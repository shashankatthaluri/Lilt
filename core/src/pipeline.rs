//! Transcript → text to type. The stages that need no model, in VocaMac's
//! `DictationOutputPipeline` order:
//!
//! 1. Cleanup (optional): hesitations, single-letter stutters, spoken
//!    corrections.
//! 2. Personal dictionary: replacements and vocabulary spellings.
//! 3. Snippets: triggers become the user's saved text.
//! 4. Spoken emoji, then spoken numbers (both opt-in).
//! 5. Formatting: auto-capitalize and trailing space.
//!
//! Snippet expansions, protected dictionary terms and emoji glyphs are masked
//! with private-use placeholders from stage 3 on, so formatting never re-cases
//! text the user authored (`iPhone` stays `iPhone` at a sentence start).

use crate::{cleanup, dictionary, spoken_emoji, spoken_numbers};

/// First placeholder. The emoji stage masks its own spans at U+F8FE/U+F8FF,
/// so the lane stops well before that.
const PLACEHOLDER_BASE: u32 = 0xE000;
const PLACEHOLDER_LIMIT: u32 = 0xF800;

pub struct TextOptions<'a> {
    /// Engine language code ("en", "de"), or `None` for auto-detect.
    pub language: Option<&'a str>,
    pub cleanup: bool,
    pub vocabulary: &'a [String],
    pub replacements: &'a [dictionary::Replacement],
    pub snippets: &'a [dictionary::Snippet],
    pub numbers: bool,
    pub symbols: bool,
    pub emoji: bool,
    pub auto_capitalize: bool,
    pub trailing_space: bool,
}

struct Masked {
    text: String,
    values: Vec<String>,
}

impl Masked {
    /// A placeholder for `value`, or the value itself once the lane is full.
    fn push(&mut self, value: String) -> String {
        let code = PLACEHOLDER_BASE + self.values.len() as u32;
        match char::from_u32(code).filter(|_| code < PLACEHOLDER_LIMIT) {
            Some(placeholder) => {
                self.values.push(value);
                placeholder.to_string()
            }
            None => value,
        }
    }

    fn restore(&self, text: &str) -> String {
        text.chars()
            .map(|c| {
                let code = c as u32;
                if (PLACEHOLDER_BASE..PLACEHOLDER_LIMIT).contains(&code) {
                    if let Some(value) = self.values.get((code - PLACEHOLDER_BASE) as usize) {
                        return value.clone();
                    }
                }
                c.to_string()
            })
            .collect()
    }
}

/// Words that are common in English and rare elsewhere, for transcripts whose
/// engine did not say which language it heard.
const COMMON_ENGLISH: &[&str] = &[
    "the", "a", "an", "and", "to", "i", "you", "it", "is", "of", "that", "we", "this", "in", "for",
    "on", "my", "so", "but", "what", "can", "be", "are", "was", "with", "have", "do", "me", "your",
    "not", "just", "how", "okay", "ok", "yes", "hello", "hi", "thanks", "please", "let's", "i'm",
];

/// Whether unlabelled text reads as English: Latin letters, and either a
/// common English word or too short to tell.
pub fn likely_english(text: &str) -> bool {
    let letters: Vec<char> = text.chars().filter(|c| c.is_alphabetic()).collect();
    if letters.is_empty() {
        return false;
    }
    let ascii = letters.iter().filter(|c| c.is_ascii()).count();
    if ascii * 10 < letters.len() * 9 {
        return false;
    }
    let words: Vec<String> = text
        .split(|c: char| !(c.is_alphanumeric() || c == '\''))
        .filter(|w| !w.is_empty())
        .map(str::to_lowercase)
        .collect();
    words.len() <= 2 || words.iter().any(|w| COMMON_ENGLISH.contains(&w.as_str()))
}

#[derive(Clone, Copy, Debug, Default, PartialEq, Eq)]
pub struct PipelineMetrics {
    pub hesitations_removed: u64,
    pub stutters_collapsed: u64,
    pub words_corrected: u64,
    pub dictionary_fixes: u64,
}

fn count_words(text: &str) -> u64 {
    text.split_whitespace()
        .filter(|w| w.chars().any(char::is_alphanumeric))
        .count() as u64
}

fn count_case_diffs(before: &str, after: &str) -> u64 {
    before
        .chars()
        .zip(after.chars())
        .filter(|(a, b)| a != b && a.eq_ignore_ascii_case(b))
        .count() as u64
}

pub fn process(raw: &str, options: &TextOptions) -> String {
    process_with_metrics(raw, options).0
}

pub fn process_with_metrics(raw: &str, options: &TextOptions) -> (String, PipelineMetrics) {
    let mut metrics = PipelineMetrics::default();
    let mut input = raw.trim().to_string();
    if input.is_empty() {
        return (String::new(), metrics);
    }

    if options.cleanup {
        // Without a language from the setting or the model, a confident
        // detection decides; the English-word heuristic only when unsure.
        let detected = options.language.or_else(|| crate::lang_id::detect(&input));
        let english = match detected {
            Some(code) => code.split('-').next() == Some("en"),
            None => likely_english(&input),
        };
        let words_before = count_words(&input);
        let (text, removed) = if english {
            cleanup::remove_hesitations(&input)
        } else {
            cleanup::remove_other_language_hesitations(&input, detected)
        };
        if removed {
            let words_after = count_words(&text);
            metrics.hesitations_removed += words_before.saturating_sub(words_after).max(1);
        }
        input = text;
        if removed && input.trim().is_empty() {
            metrics.words_corrected = metrics.hesitations_removed;
            return (String::new(), metrics);
        }
        let (stutter_text, stutter_count) = cleanup::collapse_stutters(&input);
        metrics.stutters_collapsed += stutter_count as u64;
        input = stutter_text;

        let (corr_text, corr_count) = cleanup::resolve_corrections(&input);
        metrics.stutters_collapsed += corr_count as u64;
        input = corr_text;
    }

    let correction = dictionary::correct(&input, options.vocabulary, options.replacements);
    if correction.text != input {
        metrics.dictionary_fixes += (correction.protected_terms.len() as u64).max(1);
    }
    let user_snippet_hits = dictionary::snippet_hits(&correction.text, options.snippets).len() as u64;
    metrics.dictionary_fixes += user_snippet_hits;

    let mut snippets = options.snippets.to_vec();
    snippets.extend(
        correction
            .protected_terms
            .iter()
            .map(|term| dictionary::Snippet {
                trigger: term.clone(),
                expansion: term.clone(),
            }),
    );

    let mut masked = Masked {
        text: String::new(),
        values: Vec::new(),
    };
    let source = correction.text;
    let mut copied = 0;
    for (start, end, expansion) in dictionary::snippet_hits(&source, &snippets) {
        if start < copied {
            continue;
        }
        masked.text.push_str(&source[copied..start]);
        let placeholder = masked.push(expansion);
        masked.text.push_str(&placeholder);
        copied = end;
    }
    masked.text.push_str(&source[copied..]);

    let mut emoji_conversions = 0u64;
    if options.emoji {
        let text = std::mem::take(&mut masked.text);
        masked.text = spoken_emoji::glyphs(&text, &mut |glyph| {
            emoji_conversions += 1;
            masked.push(glyph)
        });
    }
    let mut number_conversions = 0u64;
    if options.numbers {
        let before_nums = masked.text.clone();
        masked.text = spoken_numbers::digits(&masked.text, options.symbols);
        if masked.text != before_nums {
            number_conversions = count_words(&before_nums)
                .abs_diff(count_words(&masked.text))
                .max(1);
        }
    }
    let before_polish = masked.text.trim().to_string();
    let formatted = apply_output_polish(
        &masked.text,
        options.auto_capitalize,
        options.trailing_space,
    );
    let cap_fixes = count_case_diffs(&before_polish, formatted.trim());
    metrics.words_corrected = metrics.hesitations_removed
        + metrics.stutters_collapsed
        + emoji_conversions
        + number_conversions
        + cap_fixes;

    (masked.restore(&formatted), metrics)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::dictionary::{Replacement, Snippet};

    fn options<'a>() -> TextOptions<'a> {
        TextOptions {
            language: None,
            cleanup: true,
            vocabulary: &[],
            replacements: &[],
            snippets: &[],
            numbers: false,
            symbols: false,
            emoji: false,
            auto_capitalize: true,
            trailing_space: false,
        }
    }

    #[test]
    fn default_pipeline_cleans_and_formats() {
        assert_eq!(
            process("um let's do it tomorrow, oh, no, Wednesday", &options()),
            "Let's do it Wednesday"
        );
        assert_eq!(process("Uh", &options()), "");
        assert_eq!(process("  hello there  ", &options()), "Hello there");
    }

    #[test]
    fn cleanup_off_keeps_every_word() {
        let mut opts = options();
        opts.cleanup = false;
        assert_eq!(process("um hello", &opts), "Um hello");
    }

    #[test]
    fn german_keeps_um() {
        let mut opts = options();
        opts.language = Some("de");
        assert_eq!(
            process("wir treffen uns um 5 Uhr", &opts),
            "Wir treffen uns um 5 Uhr"
        );
    }

    #[test]
    fn dictionary_terms_keep_their_case_at_a_sentence_start() {
        let vocabulary = vec!["iPhone".to_string()];
        let mut opts = options();
        opts.vocabulary = &vocabulary;
        assert_eq!(process("i phone is here", &opts), "iPhone is here");
    }

    #[test]
    fn snippets_expand_and_are_not_recased() {
        let snippets = vec![Snippet {
            trigger: "my email".into(),
            expansion: "me@example.com".into(),
        }];
        let replacements = vec![Replacement {
            heard: "get hub".into(),
            replacement: "GitHub".into(),
        }];
        let mut opts = options();
        opts.snippets = &snippets;
        opts.replacements = &replacements;
        assert_eq!(process("my email", &opts), "me@example.com");
        assert_eq!(
            process("send get hub link to my email", &opts),
            "Send GitHub link to me@example.com"
        );
    }

    #[test]
    fn spoken_forms_are_opt_in() {
        let mut opts = options();
        assert_eq!(process("three fire emojis", &opts), "Three fire emojis");
        opts.emoji = true;
        assert_eq!(process("three fire emojis", &opts), "🔥🔥🔥");
        opts.numbers = true;
        opts.symbols = true;
        assert_eq!(
            process("it costs five dollars, party emoji", &opts),
            "It costs $5, 🎉"
        );
    }

    #[test]
    fn trailing_space_is_added_after_restoring() {
        let mut opts = options();
        opts.trailing_space = true;
        assert_eq!(process("hello", &opts), "Hello ");
    }

    #[test]
    fn english_detection_is_conservative() {
        assert!(likely_english("send it to me"));
        assert!(likely_english("Uh"));
        assert!(!likely_english("wir treffen uns um fünf Uhr heute"));
        assert!(!likely_english("मुझे तीन कॉपी चाहिए"));
    }

    #[test]
    fn detected_german_keeps_its_um() {
        let opts = options();
        // "ok" made the old heuristic call this English and delete "um".
        let german = "Ich habe um fünf Uhr ein Meeting mit dem ganzen Team, ok";
        assert!(process(german, &opts).contains(" um fünf"));
        assert_eq!(
            process("So um I think we should ship it on Friday", &opts),
            "So I think we should ship it on Friday"
        );
    }

    #[test]
    fn detected_portuguese_keeps_um_and_french_loses_euh() {
        let opts = options();
        // "um" is Portuguese for "a"; English cleanup would delete it.
        let portuguese = "Eu comprei um carro novo para a minha família ontem, ok";
        assert!(process(portuguese, &opts).contains(" um carro"));
        // Detected French gets its own filler words.
        let french = process("Je voudrais euh réserver une table pour quatre personnes ce soir", &opts);
        assert!(!french.contains("euh"), "{french}");
        assert!(french.contains("réserver une table"), "{french}");
    }
}

pub fn append_trailing_space(text: &str) -> String {
    if text.is_empty() {
        return String::new();
    }
    if text.ends_with(char::is_whitespace) {
        return text.to_string();
    }
    format!("{text} ")
}

pub fn capitalize_sentences(text: &str) -> String {
    if text.is_empty() {
        return String::new();
    }
    let mut out = String::with_capacity(text.len());
    let mut capitalize_next = true;
    for ch in text.chars() {
        if capitalize_next && ch.is_ascii_lowercase() {
            out.push(ch.to_ascii_uppercase());
            capitalize_next = false;
            continue;
        }
        out.push(ch);
        if matches!(ch, '.' | '!' | '?') {
            capitalize_next = true;
        } else if !ch.is_whitespace() {
            capitalize_next = false;
        }
    }
    out
}

pub fn apply_output_polish(text: &str, auto_capitalize: bool, trailing_space: bool) -> String {
    let trimmed = text.trim();
    if trimmed.is_empty() {
        return String::new();
    }
    let mut result = trimmed.to_string();
    if auto_capitalize {
        result = capitalize_sentences(&result);
    }
    if trailing_space {
        result = append_trailing_space(&result);
    }
    result
}
