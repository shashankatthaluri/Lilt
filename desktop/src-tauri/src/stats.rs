//! Usage stats, kept only on this PC (`stats.json`), like VocaMac's Stats
//! page: totals, speaking pace, streaks, time saved over typing, per-app and
//! per-model usage breakdown, and live voice/formatting metrics.

use crate::history::{HistoryEntry, STATUS_OK};
use crate::pipeline::PipelineMetrics;
use chrono::{Datelike, Duration, Local, NaiveDate, TimeZone};
use serde::{Deserialize, Serialize};
use std::collections::{BTreeMap, BTreeSet};
use std::fs;
use std::path::Path;

/// Typing speed used for "time saved", the figure VocaMac uses.
const TYPING_WORDS_PER_MINUTE: f64 = 40.0;

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DayTotals {
    pub dictations: u64,
    pub words: u64,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Stats {
    pub dictations: u64,
    pub words: u64,
    pub characters: u64,
    /// Milliseconds of speech that produced typed text.
    pub audio_ms: u64,
    /// Words corrected by cleanup, spoken numbers/emoji, and auto-capitalization.
    #[serde(default)]
    pub words_corrected: u64,
    /// Custom dictionary replacements, vocabulary matches, and snippet expansions.
    #[serde(default)]
    pub dictionary_fixes: u64,
    /// Filler words ("um", "uh", etc.) purged by cleanup.
    #[serde(default)]
    pub hesitations_removed: u64,
    /// Stutters and spoken self-corrections consolidated.
    #[serde(default)]
    pub stutters_collapsed: u64,
    /// Distinct lowercase words dictated by the user.
    #[serde(default)]
    pub unique_words: BTreeSet<String>,
    /// Target desktop app name → { dictations, words }.
    #[serde(default)]
    pub apps: BTreeMap<String, DayTotals>,
    /// Speech model ID → { dictations, words }.
    #[serde(default)]
    pub models: BTreeMap<String, DayTotals>,
    /// Local calendar day (`YYYY-MM-DD`) → totals.
    #[serde(default)]
    pub days: BTreeMap<String, DayTotals>,
    /// History entry IDs already counted, to avoid double-counting during backfill.
    #[serde(default)]
    pub counted_ids: BTreeSet<u128>,
    /// Timestamp (ms) when the user last clicked Reset on Insights.
    #[serde(default)]
    pub reset_at_ms: u128,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DayPoint {
    pub day: String,
    pub words: u64,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct BreakdownRow {
    pub name: String,
    pub dictations: u64,
    pub words: u64,
    pub percent: u64,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct Summary {
    pub dictations: u64,
    pub words: u64,
    pub characters: u64,
    pub audio_minutes: f64,
    pub words_per_minute: u64,
    pub time_saved_minutes: u64,
    pub words_today: u64,
    pub words_this_week: u64,
    pub current_streak: u32,
    pub longest_streak: u32,
    pub active_days: u32,
    pub words_corrected: u64,
    pub dictionary_fixes: u64,
    pub fixes_total: u64,
    pub hesitations_removed: u64,
    pub stutters_collapsed: u64,
    pub unique_words_count: u64,
    pub avg_take_seconds: f64,
    pub avg_words_per_take: f64,
    pub apps: Vec<BreakdownRow>,
    pub models: Vec<BreakdownRow>,
    pub days: BTreeMap<String, DayTotals>,
    /// The last 14 days, oldest first, for the chart.
    pub recent: Vec<DayPoint>,
}

pub struct RecordDetails<'a> {
    pub text: &'a str,
    pub audio_ms: u64,
    pub app_name: &'a str,
    pub model_id: &'a str,
    pub metrics: PipelineMetrics,
    pub history_id: Option<u128>,
}

pub fn load(path: &Path) -> Stats {
    fs::read_to_string(path)
        .ok()
        .and_then(|contents| serde_json::from_str(&contents).ok())
        .unwrap_or_default()
}

fn save(path: &Path, stats: &Stats) -> Result<(), String> {
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent).map_err(|error| format!("Could not save stats: {error}"))?;
    }
    let serialized = serde_json::to_vec_pretty(stats)
        .map_err(|error| format!("Could not save stats: {error}"))?;
    fs::write(path, serialized).map_err(|error| format!("Could not save stats: {error}"))
}

pub fn word_count(text: &str) -> u64 {
    text.split_whitespace()
        .filter(|word| word.chars().any(char::is_alphanumeric))
        .count() as u64
}

fn extract_unique_words(text: &str) -> impl Iterator<Item = String> + '_ {
    text.split(|c: char| !(c.is_alphanumeric() || c == '\''))
        .filter(|w| w.chars().any(char::is_alphabetic))
        .map(str::to_lowercase)
}

fn day_key(date: NaiveDate) -> String {
    date.format("%Y-%m-%d").to_string()
}

pub fn add(stats: &mut Stats, text: &str, audio_ms: u64, today: NaiveDate) {
    add_detailed(
        stats,
        &RecordDetails {
            text,
            audio_ms,
            app_name: "Desktop",
            model_id: "whisper-tiny",
            metrics: PipelineMetrics::default(),
            history_id: None,
        },
        today,
    );
}

pub fn add_detailed(stats: &mut Stats, details: &RecordDetails<'_>, today: NaiveDate) {
    if let Some(id) = details.history_id {
        if !stats.counted_ids.insert(id) {
            // Already recorded for this history entry.
            return;
        }
    }
    let words = word_count(details.text);
    stats.dictations += 1;
    stats.words += words;
    stats.characters += details.text.trim().chars().count() as u64;
    stats.audio_ms += details.audio_ms;
    stats.words_corrected += details.metrics.words_corrected;
    stats.dictionary_fixes += details.metrics.dictionary_fixes;
    stats.hesitations_removed += details.metrics.hesitations_removed;
    stats.stutters_collapsed += details.metrics.stutters_collapsed;

    for word in extract_unique_words(details.text) {
        stats.unique_words.insert(word);
    }

    if !details.app_name.trim().is_empty() {
        let app_entry = stats.apps.entry(details.app_name.trim().to_string()).or_default();
        app_entry.dictations += 1;
        app_entry.words += words;
    }

    if !details.model_id.trim().is_empty() {
        let model_entry = stats
            .models
            .entry(details.model_id.trim().to_string())
            .or_default();
        model_entry.dictations += 1;
        model_entry.words += words;
    }

    let day = stats.days.entry(day_key(today)).or_default();
    day.dictations += 1;
    day.words += words;
}

pub fn record(path: &Path, text: &str, audio_ms: u64) -> Result<(), String> {
    let mut stats = load(path);
    add(&mut stats, text, audio_ms, Local::now().date_naive());
    save(path, &stats)
}

pub fn record_detailed(path: &Path, details: &RecordDetails<'_>) -> Result<(), String> {
    let mut stats = load(path);
    add_detailed(&mut stats, details, Local::now().date_naive());
    save(path, &stats)
}

pub fn reset(path: &Path) -> Result<(), String> {
    let now = crate::history::now_ms();
    let fresh = Stats {
        reset_at_ms: now,
        ..Default::default()
    };
    save(path, &fresh)
}

fn date_from_ms(ms: u128) -> NaiveDate {
    Local
        .timestamp_millis_opt(ms as i64)
        .single()
        .map(|dt| dt.date_naive())
        .unwrap_or_else(|| Local::now().date_naive())
}

fn estimate_sentence_caps(text: &str) -> u64 {
    let mut count = 0u64;
    let mut at_start = true;
    for ch in text.chars() {
        if at_start && ch.is_alphabetic() {
            if ch.is_uppercase() {
                count += 1;
            }
            at_start = false;
        } else if matches!(ch, '.' | '!' | '?') {
            at_start = true;
        } else if !ch.is_whitespace() {
            at_start = false;
        }
    }
    count
}

/// Ensures any completed (`ok`) entries in `history.json` created after the last
/// stats reset are reflected in `stats.json`.
pub fn sync_with_history(path: &Path, history: &[HistoryEntry]) -> Stats {
    let mut stats = load(path);
    let mut changed = false;

    for entry in history.iter().rev() {
        if entry.status != STATUS_OK || entry.text.trim().is_empty() {
            continue;
        }
        if entry.created_at_ms <= stats.reset_at_ms {
            continue;
        }
        if stats.counted_ids.contains(&entry.id) {
            continue;
        }
        let date = date_from_ms(entry.created_at_ms);
        let cap_fixes = estimate_sentence_caps(&entry.text);
        add_detailed(
            &mut stats,
            &RecordDetails {
                text: &entry.text,
                audio_ms: entry.duration_ms,
                app_name: "Lilt",
                model_id: &entry.model_id,
                metrics: PipelineMetrics {
                    words_corrected: cap_fixes,
                    ..Default::default()
                },
                history_id: Some(entry.id),
            },
            date,
        );
        changed = true;
    }

    if changed {
        let _ = save(path, &stats);
    }
    stats
}

fn build_breakdown(map: &BTreeMap<String, DayTotals>) -> Vec<BreakdownRow> {
    let total_words: u64 = map.values().map(|t| t.words).sum();
    let total_takes: u64 = map.values().map(|t| t.dictations).sum();
    let mut rows: Vec<BreakdownRow> = map
        .iter()
        .filter(|(_, t)| t.dictations > 0)
        .map(|(name, t)| {
            let pct = if total_words > 0 {
                ((t.words as f64 / total_words as f64) * 100.0).round() as u64
            } else if total_takes > 0 {
                ((t.dictations as f64 / total_takes as f64) * 100.0).round() as u64
            } else {
                0
            };
            BreakdownRow {
                name: name.clone(),
                dictations: t.dictations,
                words: t.words,
                percent: pct.min(100),
            }
        })
        .collect();
    rows.sort_by(|a, b| b.words.cmp(&a.words).then_with(|| b.dictations.cmp(&a.dictations)));
    rows
}

pub fn summarize(stats: &Stats, today: NaiveDate) -> Summary {
    let active: Vec<NaiveDate> = stats
        .days
        .iter()
        .filter(|(_, totals)| totals.dictations > 0)
        .filter_map(|(day, _)| NaiveDate::parse_from_str(day, "%Y-%m-%d").ok())
        .collect();

    // Longest run of consecutive days.
    let mut longest = 0u32;
    let mut run = 0u32;
    let mut previous: Option<NaiveDate> = None;
    for day in &active {
        run = match previous {
            Some(before) if *day - before == Duration::days(1) => run + 1,
            _ => 1,
        };
        longest = longest.max(run);
        previous = Some(*day);
    }

    // Current streak: ends today, or yesterday when today has none yet.
    let has = |day: NaiveDate| {
        stats
            .days
            .get(&day_key(day))
            .is_some_and(|t| t.dictations > 0)
    };
    let mut cursor = if has(today) {
        today
    } else {
        today - Duration::days(1)
    };
    let mut current = 0u32;
    while has(cursor) {
        current += 1;
        cursor -= Duration::days(1);
    }

    let words_on = |day: NaiveDate| stats.days.get(&day_key(day)).map_or(0, |t| t.words);
    let week_start = today - Duration::days(today.weekday().num_days_from_monday() as i64);
    let mut words_this_week = 0;
    let mut day = week_start;
    while day <= today {
        words_this_week += words_on(day);
        day += Duration::days(1);
    }
    let recent = (0..14)
        .rev()
        .map(|back| {
            let day = today - Duration::days(back);
            DayPoint {
                day: day_key(day),
                words: words_on(day),
            }
        })
        .collect();

    let audio_minutes = stats.audio_ms as f64 / 60_000.0;
    let words_per_minute = if stats.audio_ms >= 500 && stats.words > 0 {
        (stats.words as f64 / (stats.audio_ms as f64 / 60_000.0)).round() as u64
    } else {
        0
    };
    let typing_minutes = stats.words as f64 / TYPING_WORDS_PER_MINUTE;
    let time_saved_minutes = (typing_minutes - audio_minutes).max(0.0).round() as u64;

    let avg_take_seconds = if stats.dictations > 0 {
        (stats.audio_ms as f64 / 1000.0) / (stats.dictations as f64)
    } else {
        0.0
    };
    let avg_words_per_take = if stats.dictations > 0 {
        (stats.words as f64) / (stats.dictations as f64)
    } else {
        0.0
    };

    Summary {
        dictations: stats.dictations,
        words: stats.words,
        characters: stats.characters,
        audio_minutes,
        words_per_minute,
        time_saved_minutes,
        words_today: words_on(today),
        words_this_week,
        current_streak: current,
        longest_streak: longest,
        active_days: active.len() as u32,
        words_corrected: stats.words_corrected,
        dictionary_fixes: stats.dictionary_fixes,
        fixes_total: stats.words_corrected + stats.dictionary_fixes,
        hesitations_removed: stats.hesitations_removed,
        stutters_collapsed: stats.stutters_collapsed,
        unique_words_count: stats.unique_words.len() as u64,
        avg_take_seconds,
        avg_words_per_take,
        apps: build_breakdown(&stats.apps),
        models: build_breakdown(&stats.models),
        days: stats.days.clone(),
        recent,
    }
}

pub fn summary(path: &Path) -> Summary {
    summarize(&load(path), Local::now().date_naive())
}

pub fn summary_with_history(path: &Path, history: &[HistoryEntry]) -> Summary {
    let stats = sync_with_history(path, history);
    summarize(&stats, Local::now().date_naive())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn date(text: &str) -> NaiveDate {
        NaiveDate::parse_from_str(text, "%Y-%m-%d").unwrap()
    }

    #[test]
    fn words_count_only_real_words() {
        assert_eq!(word_count("Hello there, friend!"), 3);
        assert_eq!(word_count("  — 🎉 "), 0);
        assert_eq!(word_count("It costs $5"), 3);
    }

    #[test]
    fn totals_pace_and_time_saved() {
        let mut stats = Stats::default();
        let today = date("2026-09-25");
        // 120 words over one minute of speech.
        add(&mut stats, &"word ".repeat(120), 60_000, today);
        let summary = summarize(&stats, today);
        assert_eq!(summary.dictations, 1);
        assert_eq!(summary.words, 120);
        assert_eq!(summary.words_per_minute, 120);
        // Typing 120 words at 40 wpm is 3 minutes; speaking took 1.
        assert_eq!(summary.time_saved_minutes, 2);
        assert_eq!(summary.words_today, 120);
        assert_eq!(summary.recent.len(), 14);
        assert_eq!(summary.recent.last().unwrap().words, 120);
        assert_eq!(summary.unique_words_count, 1);
    }

    #[test]
    fn streaks_follow_consecutive_days() {
        let mut stats = Stats::default();
        for day in [
            "2026-09-01",
            "2026-09-02",
            "2026-09-03",
            "2026-09-10",
            "2026-09-23",
            "2026-09-24",
        ] {
            add(&mut stats, "hello world", 1_000, date(day));
        }
        // Today has nothing yet: the streak that ended yesterday still counts.
        let summary = summarize(&stats, date("2026-09-25"));
        assert_eq!(summary.current_streak, 2);
        assert_eq!(summary.longest_streak, 3);
        assert_eq!(summary.active_days, 6);
        // A gap of a day ends it.
        assert_eq!(summarize(&stats, date("2026-09-26")).current_streak, 0);
    }

    #[test]
    fn week_starts_on_monday() {
        let mut stats = Stats::default();
        add(&mut stats, "one two", 1_000, date("2026-09-20")); // Sunday
        add(&mut stats, "three four five", 1_000, date("2026-09-21")); // Monday
        let summary = summarize(&stats, date("2026-09-25"));
        assert_eq!(summary.words_this_week, 3);
    }
}
