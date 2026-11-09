//! Rule-based transcript cleanup, no model needed (VocaMac "Medium" level):
//! hesitation sounds ("um", "uh"), single-letter stutters ("I I I think"), and
//! spoken self-corrections ("tomorrow, oh, no, Wednesday" → "Wednesday").
//!
//! Ported from VocaMac `WritingStyleEngine` and `SpokenCorrectionResolver`.
//! VocaMac also collapses multi-letter stutters and cut-off words, but those
//! need a spelling dictionary to tell a fragment from a word; VocaWin keeps
//! only the rules that are safe without one.

use fancy_regex::Regex;
use std::sync::OnceLock;

// MARK: - Hesitations

fn english_hesitation() -> &'static Regex {
    static EXPRESSION: OnceLock<Regex> = OnceLock::new();
    EXPRESSION.get_or_init(|| {
        Regex::new(
            r"(?i)(?<![\p{L}\p{N}'’])(u+m+|u+h+m*|e+r+m+|h+m+)(?![\p{L}\p{N}'’])([,.!?;:…]*)",
        )
        .expect("hesitation pattern")
    })
}

/// Sounds that are not a word in any language the models write. Plain "um"
/// is excluded (German "um 5 Uhr"), as are "ah", "eh", "em", "am", "mm".
const UNIVERSAL_HESITATIONS: &str =
    "u+h+m*|u+m{2,}|h+m+|e+h{2,}|e+h+m+|a+h+m+|ä+h+m*|m{3,}|х+м+|м{3,}";

fn language_hesitations(language: Option<&str>) -> &'static [&'static str] {
    match language.and_then(|code| code.split('-').next()) {
        Some("de") => &["öh"],
        Some("fr") => &["euh", "heu"],
        _ => &[],
    }
}

/// Remove hesitation sounds from English text and repair what they leave:
/// "Hello, um, how are you?" → "Hello, how are you?". Returns whether any
/// were removed.
pub fn remove_hesitations(text: &str) -> (String, bool) {
    remove_matches(english_hesitation(), text)
}

/// Other languages lose only the universal sounds, plus the language's own
/// when it is known.
pub fn remove_other_language_hesitations(text: &str, language: Option<&str>) -> (String, bool) {
    let extras: Vec<String> = language_hesitations(language)
        .iter()
        .map(|word| fancy_regex::escape(word).into_owned())
        .collect();
    let mut alternatives = vec![UNIVERSAL_HESITATIONS.to_string()];
    alternatives.extend(extras);
    let pattern = format!(
        r"(?i)(?<![\p{{L}}\p{{N}}'’])({})(?![\p{{L}}\p{{N}}'’])([,.!?;:…]*)",
        alternatives.join("|")
    );
    match Regex::new(&pattern) {
        Ok(expression) => remove_matches(&expression, text),
        Err(_) => (text.to_string(), false),
    }
}

fn remove_matches(expression: &Regex, text: &str) -> (String, bool) {
    let ranges: Vec<(usize, usize)> = expression
        .find_iter(text)
        .filter_map(Result::ok)
        .map(|found| char_range(text, found.start(), found.end()))
        .collect();
    if ranges.is_empty() {
        return (text.to_string(), false);
    }
    (remove_word_runs(&ranges, text, true), true)
}

fn char_range(text: &str, start: usize, end: usize) -> (usize, usize) {
    (text[..start].chars().count(), text[..end].chars().count())
}

fn is_horizontal_space(c: char) -> bool {
    c == ' ' || c == '\t'
}

/// Delete whole words (char ranges) and repair what they leave behind. A
/// sentence end the words carried moves to the word before ("hello, um." →
/// "hello."), and a capitalized sentence opener hands its capital to the next
/// word ("Um I hope" → "I hope"). With `prose` off only the deletion happens.
fn remove_word_runs(ranges: &[(usize, usize)], text: &str, prose: bool) -> String {
    let mut result: Vec<char> = text.chars().collect();
    let mut sorted = ranges.to_vec();
    sorted.sort();
    let mut runs: Vec<(usize, usize)> = Vec::new();
    for range in sorted {
        if let Some(last) = runs.last_mut() {
            let mut gap = last.1;
            while gap < range.0 && is_horizontal_space(result[gap]) {
                gap += 1;
            }
            if gap >= range.0 {
                last.0 = last.0.min(range.0);
                last.1 = last.1.max(range.1);
                continue;
            }
        }
        runs.push(range);
    }

    for (run_start, run_end) in runs.into_iter().rev() {
        let words: Vec<char> = result[run_start..run_end].to_vec();
        let trailing: Vec<char> = words
            .iter()
            .rev()
            .take_while(|c| ",.!?;:…".contains(**c))
            .copied()
            .collect();

        let mut previous = run_start as isize - 1;
        while previous >= 0 && is_horizontal_space(result[previous as usize]) {
            previous -= 1;
        }
        let at_sentence_start = previous < 0 || ".!?…\n\r".contains(result[previous as usize]);

        let mut end = run_end;
        while end < result.len() && is_horizontal_space(result[end]) {
            end += 1;
        }
        let start = if end == run_end {
            (previous + 1) as usize
        } else {
            run_start
        };
        result.drain(start..end);

        if !prose {
            continue;
        }
        if at_sentence_start {
            if words.first().is_some_and(|c| c.is_uppercase()) {
                let mut next = start;
                while next < result.len() && is_horizontal_space(result[next]) {
                    next += 1;
                }
                if next < result.len() {
                    let upper: Vec<char> = result[next].to_uppercase().collect();
                    if upper.len() == 1 && upper[0] != result[next] {
                        result[next] = upper[0];
                    }
                }
            }
        } else if let Some(terminal) = trailing.iter().find(|c| ".!?…".contains(**c)) {
            // `trailing` is reversed, so the first terminal found is the last one said.
            if previous >= 0 {
                let mut last = previous;
                while last >= 0 && ",;:".contains(result[last as usize]) {
                    result.remove(last as usize);
                    last -= 1;
                }
                if last >= 0 && !".!?…".contains(result[last as usize]) {
                    result.insert(last as usize + 1, *terminal);
                }
            }
        }
    }
    result.into_iter().collect()
}

// MARK: - Stutters

const STUTTER_FUNCTION_WORDS: &[&str] = &[
    "the", "a", "an", "to", "in", "on", "at", "of", "for", "and", "but", "or", "is", "was", "are",
    "were", "with", "from", "by", "this", "that", "it", "we", "they", "he", "she", "you", "my",
    "your", "our", "can", "will",
];

#[derive(Clone, Debug)]
struct PhraseToken {
    byte_start: usize,
    byte_end: usize,
    raw: String,
    core: String,
    trailing_punct: String,
}

fn tokenize_for_phrases(text: &str) -> Vec<PhraseToken> {
    let mut out = Vec::new();
    let mut i = 0;
    let bytes = text.as_bytes();
    while i < bytes.len() {
        let ch = text[i..].chars().next().unwrap();
        if ch.is_whitespace() {
            i += ch.len_utf8();
            continue;
        }
        let start = i;
        while i < bytes.len() {
            let c = text[i..].chars().next().unwrap();
            if c.is_whitespace() {
                break;
            }
            i += c.len_utf8();
        }
        let raw = text[start..i].to_string();
        let trimmed = raw.trim_end_matches(|c: char| ",.!?;:…".contains(c));
        let trailing_punct = raw[trimmed.len()..].to_string();
        let core = trimmed.to_lowercase();
        out.push(PhraseToken {
            byte_start: start,
            byte_end: i,
            raw,
            core,
            trailing_punct,
        });
    }
    out
}

/// Collapse repeated multi-word phrases ("give me, give me, give me" → "give me")
/// while preserving initial capitalization and final punctuation.
fn collapse_repeated_phrases(text: &str) -> (String, usize) {
    let tokens = tokenize_for_phrases(text);
    if tokens.len() < 4 {
        return (text.to_string(), 0);
    }
    let mut out = String::with_capacity(text.len());
    let mut last_byte = 0;
    let mut collapsed_count = 0;
    let mut idx = 0;

    while idx < tokens.len() {
        let mut matched: Option<(usize, usize)> = None;
        for plen in (2..=5).rev() {
            if idx + plen * 2 > tokens.len() {
                continue;
            }
            let internal_clean = tokens[idx..idx + plen - 1]
                .iter()
                .all(|t| !t.core.is_empty() && t.trailing_punct.is_empty());
            let boundary_ok = !tokens[idx + plen - 1].core.is_empty()
                && (tokens[idx + plen - 1].trailing_punct.is_empty()
                    || tokens[idx + plen - 1].trailing_punct == ",");
            if !internal_clean || !boundary_ok {
                continue;
            }

            let mut repeats = 1usize;
            while idx + (repeats + 1) * plen <= tokens.len() {
                let base = idx + repeats * plen;
                let prev_end = &tokens[base - 1];
                if !(prev_end.trailing_punct.is_empty() || prev_end.trailing_punct == ",")
                    || text[prev_end.byte_end..tokens[base].byte_start].contains('\n')
                {
                    break;
                }
                let same_words = (0..plen).all(|k| tokens[base + k].core == tokens[idx + k].core);
                let internal_rep_clean = tokens[base..base + plen - 1]
                    .iter()
                    .all(|t| t.trailing_punct.is_empty());
                if same_words && internal_rep_clean {
                    repeats += 1;
                } else {
                    break;
                }
            }

            if repeats >= 2 {
                matched = Some((plen, repeats));
                break;
            }
        }

        if let Some((plen, repeats)) = matched {
            let first_tok = &tokens[idx];
            let last_tok = &tokens[idx + repeats * plen - 1];
            out.push_str(&text[last_byte..first_tok.byte_start]);
            let mut phrase_words: Vec<String> = tokens[idx..idx + plen]
                .iter()
                .map(|t| {
                    t.raw
                        .trim_end_matches(|c: char| ",.!?;:…".contains(c))
                        .to_string()
                })
                .collect();
            if let Some(last_word) = phrase_words.last_mut() {
                last_word.push_str(&last_tok.trailing_punct);
            }
            out.push_str(&phrase_words.join(" "));
            last_byte = last_tok.byte_end;
            collapsed_count += repeats - 1;
            idx += repeats * plen;
        } else {
            idx += 1;
        }
    }

    if collapsed_count == 0 {
        return (text.to_string(), 0);
    }
    out.push_str(&text[last_byte..]);
    (out, collapsed_count)
}

/// Collapse a single letter said three or more times ("I I I think" → "I think"),
/// repeated function words ("the the" → "the"), and repeated multi-word phrases
/// ("give me, give me, give me" → "give me"). "no no no" and "I I think" are left as spoken.
pub fn collapse_stutters(text: &str) -> (String, usize) {
    let (after_phrases, phrase_count) = collapse_repeated_phrases(text);
    let chars: Vec<char> = after_phrases.chars().collect();
    let mut tokens: Vec<(usize, usize, String)> = Vec::new();
    let mut i = 0;
    while i < chars.len() {
        if chars[i].is_whitespace() {
            i += 1;
            continue;
        }
        let start = i;
        while i < chars.len() && !chars[i].is_whitespace() {
            i += 1;
        }
        let word: String = chars[start..i].iter().collect();
        let clean = word
            .trim_end_matches(|c: char| ",;".contains(c))
            .to_lowercase();
        tokens.push((start, i, clean));
    }
    let mut removals = Vec::new();
    let mut index = 0;
    while index < tokens.len() {
        let key = &tokens[index].2;
        let mut end = index + 1;
        while end < tokens.len() && tokens[end].2 == *key {
            end += 1;
        }
        let is_single_letter = key.chars().count() == 1 && key.chars().all(char::is_alphabetic);
        let is_function_word = STUTTER_FUNCTION_WORDS.contains(&key.as_str()) && key.chars().count() > 1;
        if (end - index >= 3 && is_single_letter) || (end - index >= 2 && is_function_word) {
            removals.extend(tokens[index + 1..end].iter().map(|t| (t.0, t.1)));
        }
        index = end;
    }
    if removals.is_empty() {
        return (after_phrases, phrase_count);
    }
    let count = phrase_count + removals.len();
    (remove_word_runs(&removals, &after_phrases, false), count)
}

// MARK: - Spoken corrections

const DAYS: &[&str] = &[
    "today",
    "tomorrow",
    "yesterday",
    "tonight",
    "monday",
    "tuesday",
    "wednesday",
    "thursday",
    "friday",
    "saturday",
    "sunday",
    "weekend",
    "hoy",
    "mañana",
    "manana",
    "ayer",
    "lunes",
    "martes",
    "miércoles",
    "miercoles",
    "jueves",
    "viernes",
    "sábado",
    "sabado",
    "domingo",
    "aujourd'hui",
    "demain",
    "hier",
    "lundi",
    "mardi",
    "mercredi",
    "jeudi",
    "vendredi",
    "samedi",
    "dimanche",
    "heute",
    "morgen",
    "übermorgen",
    "gestern",
    "montag",
    "dienstag",
    "mittwoch",
    "donnerstag",
    "freitag",
    "samstag",
    "sonntag",
    "hoje",
    "amanhã",
    "amanha",
    "ontem",
    "segunda",
    "terça",
    "terca",
    "quarta",
    "quinta",
    "sexta",
    "oggi",
    "domani",
    "ieri",
    "lunedì",
    "lunedi",
    "martedì",
    "martedi",
    "mercoledì",
    "mercoledi",
    "giovedì",
    "giovedi",
    "venerdì",
    "venerdi",
    "sabato",
    "domenica",
    "aaj",
    "kal",
    "parso",
    "parson",
    "somvar",
    "somwar",
    "mangalvar",
    "mangalwar",
    "budhvar",
    "budhwar",
    "guruvar",
    "guruwar",
    "shukravar",
    "shukrawar",
    "shanivar",
    "shaniwar",
    "ravivar",
    "raviwar",
    "itvaar",
];

const MONTHS: &[&str] = &[
    "january",
    "february",
    "march",
    "april",
    "may",
    "june",
    "july",
    "august",
    "september",
    "october",
    "november",
    "december",
    "enero",
    "febrero",
    "marzo",
    "abril",
    "mayo",
    "junio",
    "julio",
    "agosto",
    "septiembre",
    "octubre",
    "noviembre",
    "diciembre",
    "janvier",
    "février",
    "fevrier",
    "mars",
    "avril",
    "mai",
    "juin",
    "juillet",
    "août",
    "aout",
    "septembre",
    "octobre",
    "novembre",
    "décembre",
    "decembre",
    "januar",
    "februar",
    "märz",
    "maerz",
    "juni",
    "juli",
    "oktober",
    "dezember",
    "janeiro",
    "fevereiro",
    "março",
    "marco",
    "maio",
    "junho",
    "julho",
    "setembro",
    "outubro",
    "novembro",
    "dezembro",
    "gennaio",
    "febbraio",
    "aprile",
    "maggio",
    "giugno",
    "luglio",
    "settembre",
    "ottobre",
    "dicembre",
];

const NUMBER_WORDS: &[&str] = &[
    "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven",
    "twelve", "fifteen", "twenty", "thirty", "forty", "fifty", "hundred", "noon", "midnight",
    "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez", "deux",
    "trois", "quatre", "cinq", "sept", "huit", "neuf", "dix", "eins", "zwei", "drei", "vier",
    "fünf", "sechs", "sieben", "acht", "neun", "zehn", "ek", "paanch", "chhe", "saat", "aath",
    "nau",
];

const ARTICLES: &[&str] = &[
    "the", "on", "at", "in", "by", "el", "la", "los", "las", "le", "der", "die", "das", "am", "um",
    "à", "il", "lo",
];

const DAY_MODIFIERS: &[&str] = &[
    "next",
    "this",
    "last",
    "coming",
    "próximo",
    "prochain",
    "nächsten",
];

const NEGATIONS: &[&str] = &[
    "not", "no", "never", "cannot", "can't", "won't", "don't", "doesn't", "didn't", "isn't",
    "aren't", "nicht", "kein", "keine", "nie", "pas", "jamais", "nunca", "não", "nao", "non",
    "nahi", "nahin", "mat",
];

/// Cue phrases, longest first so "or no" wins over "no".
const CUES: &[&str] = &[
    "no no",
    "oh no",
    "no wait",
    "or no",
    "oh sorry",
    "or rather",
    "i mean",
    "make that",
    "scratch that",
    "no sorry",
    "sorry",
    "wait",
    "actually",
    "rather",
    "correction",
    "no",
    "perdón",
    "perdon",
    "mejor dicho",
    "digo",
    "o sea",
    "non",
    "pardon",
    "je veux dire",
    "plutôt",
    "plutot",
    "enfin",
    "nein",
    "ich meine",
    "besser gesagt",
    "quatsch",
    "não",
    "nao",
    "desculpa",
    "quer dizer",
    "aliás",
    "alias",
    "scusa",
    "cioè",
    "cioe",
    "anzi",
    "nahi nahi",
    "nahi",
    "nahin",
    "mera matlab",
    "matlab",
];

/// Cues that are also everyday words ("wait 5 minutes") count only when set
/// off by punctuation on both sides.
const PUNCTUATED_CUES: &[&str] = &[
    "no", "wait", "non", "nein", "não", "nao", "nahi", "nahin", "pardon", "enfin", "alias", "digo",
    "matlab",
];

const SPACE: &str = "[ \\t]";

fn alternation(words: &[&str]) -> String {
    let mut sorted: Vec<&str> = words.to_vec();
    sorted.sort_by_key(|word| std::cmp::Reverse(word.chars().count()));
    sorted
        .iter()
        .map(|word| fancy_regex::escape(word).into_owned())
        .collect::<Vec<_>>()
        .join("|")
}

fn value_pattern(group: &str) -> String {
    let day = format!(
        "(?:(?:{}){SPACE}+)?(?:{})(?:-feira)?",
        alternation(DAY_MODIFIERS),
        alternation(DAYS)
    );
    let month = alternation(MONTHS);
    let number = format!(
        "(?:\\d+(?:[:.]\\d+)?|{})(?:{SPACE}*(?:a\\.?m\\.?|p\\.?m\\.?|o'clock|minutes?|mins?|hours?|days?|weeks?|percent|%|uhr|heures?|horas?|ore|baje))?",
        alternation(NUMBER_WORDS)
    );
    format!(
        "(?<{group}>(?:(?:{}){SPACE}+)?(?:{day}|{month}|{number}))",
        alternation(ARTICLES)
    )
}

fn cue_pattern(phrases: &[&str]) -> String {
    phrases
        .iter()
        .map(|phrase| {
            phrase
                .split(' ')
                .map(|word| fancy_regex::escape(word).into_owned())
                .collect::<Vec<_>>()
                .join("[, \\t]+")
        })
        .collect::<Vec<_>>()
        .join("|")
}

fn correction_expression() -> &'static Regex {
    static EXPRESSION: OnceLock<Regex> = OnceLock::new();
    EXPRESSION.get_or_init(|| {
        let plain_cues: Vec<&str> = CUES
            .iter()
            .copied()
            .filter(|cue| !PUNCTUATED_CUES.contains(cue))
            .collect();
        let punctuated_cues: Vec<&str> = CUES
            .iter()
            .copied()
            .filter(|cue| PUNCTUATED_CUES.contains(cue))
            .collect();
        let separators = "[,.;…—–\\- \\t]";
        let punctuation = format!("{SPACE}*[,.;…—–\\-]{separators}*");
        let interjection = "(?:oh[, \\t]+)?";
        let plain = format!(
            "{separators}*{interjection}(?:{})(?![\\p{{L}}]){separators}+",
            cue_pattern(&plain_cues)
        );
        let punctuated = format!(
            "{punctuation}{interjection}(?:{})(?![\\p{{L}}]){punctuation}",
            cue_pattern(&punctuated_cues)
        );
        let pattern = format!(
            "(?i)(?<![\\p{{L}}\\p{{N}}])(?<correction>{}(?:{plain}|{punctuated}){})(?![\\p{{L}}\\p{{N}}])",
            value_pattern("first"),
            value_pattern("replacement")
        );
        Regex::new(&pattern).expect("correction pattern")
    })
}

#[derive(PartialEq, Eq)]
enum Kind {
    Day,
    Month,
    Number,
}

fn kind(value: &str) -> Option<Kind> {
    let lower = value.to_lowercase();
    let words: Vec<&str> = lower.split(' ').filter(|w| !w.is_empty()).collect();
    let core = words
        .iter()
        .rev()
        .find(|w| !ARTICLES.contains(w) && !DAY_MODIFIERS.contains(w))?;
    if DAYS.contains(core) || DAYS.contains(&core.replace("-feira", "").as_str()) {
        return Some(Kind::Day);
    }
    if MONTHS.contains(core) {
        return Some(Kind::Month);
    }
    if words
        .iter()
        .any(|w| w.chars().next().is_some_and(|c| c.is_numeric()) || NUMBER_WORDS.contains(w))
    {
        return Some(Kind::Number);
    }
    None
}

/// A full stop between the value and its replacement. An ellipsis is a
/// hesitation, not a sentence end.
fn ends_a_sentence(gap: &str) -> bool {
    let without_ellipsis = gap.replace('…', "");
    let mut cleaned = String::new();
    let chars: Vec<char> = without_ellipsis.chars().collect();
    let mut i = 0;
    while i < chars.len() {
        if chars[i] == '.' && chars.get(i + 1) == Some(&'.') {
            while i < chars.len() && chars[i] == '.' {
                i += 1;
            }
            continue;
        }
        cleaned.push(chars[i]);
        i += 1;
    }
    cleaned.chars().any(|c| ".!?".contains(c))
}

fn contains_negation(clause: &str) -> bool {
    let normalized = clause.to_lowercase().replace('’', "'");
    normalized
        .split(|c: char| !(c.is_alphabetic() || c == '\''))
        .any(|word| NEGATIONS.contains(&word) || word.ends_with("n't"))
}

/// "at 5, no, 6" → "at 6"; "next Monday, no, Tuesday" → "next Tuesday".
fn carrying_leading_words(first: &str, replacement: &str) -> String {
    let leading = |value: &str| -> Vec<String> {
        value
            .split(' ')
            .take_while(|w| {
                let lower = w.to_lowercase();
                ARTICLES.contains(&lower.as_str()) || DAY_MODIFIERS.contains(&lower.as_str())
            })
            .map(str::to_string)
            .collect()
    };
    if !leading(replacement).is_empty() {
        return replacement.to_string();
    }
    let carried = leading(first);
    if carried.is_empty() {
        replacement.to_string()
    } else {
        let mut parts = carried;
        parts.push(replacement.to_string());
        parts.join(" ")
    }
}

const ANCHOR_WORDS: &[&str] = &[
    "the", "a", "an", "to", "in", "on", "at", "for", "with", "from", "by", "my", "your", "our",
    "this", "that",
];

const NON_REPLACEMENT_STARTERS: &[&str] = &[
    "not", "no", "never", "really", "i", "i'll", "i'm", "we", "you", "they", "he", "she", "it",
    "it's", "there", "that's",
];

fn false_start_expression() -> &'static Regex {
    static EXPRESSION: OnceLock<Regex> = OnceLock::new();
    EXPRESSION.get_or_init(|| {
        Regex::new(
            r"(?m)(?<prefix>^|[.!?][ \t]+)(?<frag>\p{L}[\p{L}'’]*(?:[ \t]+\p{L}[\p{L}'’]*){0,3})[ \t]*(?:—|--)[ \t]*(?<next>\p{L})",
        )
        .expect("false start pattern")
    })
}

fn general_correction_expression() -> &'static Regex {
    static EXPRESSION: OnceLock<Regex> = OnceLock::new();
    EXPRESSION.get_or_init(|| {
        Regex::new(
            r"(?i)(?<lead>[^\n.!?]+?)(?<sep>[ \t]*,[ \t]*(?:(?:oh[ \t]+)?(?:no[ \t]+wait|i[ \t]+mean|scratch[ \t]+that|oh[ \t]+sorry|no[ \t]+sorry|or[ \t]+rather|actually[ \t]+no|mera[ \t]+matlab)[ \t]*,?[ \t]+|sorry[ \t]*,[ \t]+)|[ \t]+(?:no[ \t]+wait|i[ \t]+mean|scratch[ \t]+that|or[ \t]+rather|actually[ \t]+no)[ \t]*,?[ \t]+)(?<after>\p{L}[\p{L}\p{N}'’-]*(?:[ \t]+\p{L}[\p{L}\p{N}'’-]*){0,3})",
        )
        .expect("general correction pattern")
    })
}

fn resolve_general_corrections(text: &str) -> (String, usize) {
    let mut result = text.to_string();
    let mut count = 0;

    // 1. Abandoned false starts separated by an em-dash ("we should—let's deploy it tonight")
    let fs_expr = false_start_expression();
    for _ in 0..4 {
        let mut patch = None;
        if let Ok(Some(caps)) = fs_expr.captures(&result) {
            if let (Some(prefix), Some(frag), Some(next), Some(whole)) = (
                caps.name("prefix"),
                caps.name("frag"),
                caps.name("next"),
                caps.get(0),
            ) {
                if !contains_negation(frag.as_str()) {
                    let frag_starts_upper = frag
                        .as_str()
                        .chars()
                        .next()
                        .is_some_and(|c| c.is_uppercase());
                    let next_ch = next.as_str().chars().next().unwrap();
                    let replacement_ch: String = if frag_starts_upper {
                        next_ch.to_uppercase().collect()
                    } else {
                        next_ch.to_string()
                    };
                    let rep = format!("{}{}", prefix.as_str(), replacement_ch);
                    patch = Some((whole.start(), whole.end(), rep));
                }
            }
        }
        if let Some((start, end, rep)) = patch {
            result.replace_range(start..end, &rep);
            count += 1;
        } else {
            break;
        }
    }

    // 2. General word / noun-phrase self-corrections across explicit correction cues
    let gen_expr = general_correction_expression();
    for _ in 0..6 {
        let mut patch = None;
        for caps in gen_expr.captures_iter(&result).filter_map(Result::ok) {
            let (Some(lead), Some(sep), Some(after)) =
                (caps.name("lead"), caps.name("sep"), caps.name("after"))
            else {
                continue;
            };
            let lead_str = lead.as_str();
            let after_str = after.as_str();
            if contains_negation(lead_str) || contains_negation(after_str) {
                continue;
            }
            let after_words: Vec<&str> = after_str.split_whitespace().collect();
            if after_words.is_empty() || after_words.len() > 4 {
                continue;
            }
            let first_after_lower = after_words[0]
                .trim_matches(|c: char| !c.is_alphanumeric() && c != '\'')
                .to_lowercase();
            if NON_REPLACEMENT_STARTERS.contains(&first_after_lower.as_str()) {
                continue;
            }

            let lead_tokens = tokenize_for_phrases(lead_str);
            if lead_tokens.is_empty() {
                continue;
            }

            // Check if `after` starts with an anchor word (e.g. "the hybrid page") that matches
            // a recent anchor word in `lead` (e.g. "open the settings page")
            let mut replace_from_byte = None;
            if ANCHOR_WORDS.contains(&first_after_lower.as_str()) {
                let search_start = lead_tokens.len().saturating_sub(4);
                for tok in lead_tokens[search_start..].iter().rev() {
                    if tok.core == first_after_lower {
                        replace_from_byte = Some(lead.start() + tok.byte_start);
                        break;
                    }
                }
            }

            // Otherwise, replace the immediately preceding word (or 2 words if `after` shares the same last word)
            if replace_from_byte.is_none() {
                if after_words.len() <= 3 {
                    let last_lead = lead_tokens.last().unwrap();
                    // Ensure the last token in `lead` had no internal clause break
                    if last_lead.trailing_punct.is_empty() && last_lead.core != first_after_lower {
                        replace_from_byte = Some(lead.start() + last_lead.byte_start);
                    }
                }
            }

            if let Some(start_byte) = replace_from_byte {
                let _ = sep;
                patch = Some((start_byte, after.end(), after_str.to_string()));
                break;
            }
        }

        if let Some((start, end, rep)) = patch {
            result.replace_range(start..end, &rep);
            count += 1;
        } else {
            break;
        }
    }

    (result, count)
}

/// Resolves explicit, compact self-corrections, returning the text and how
/// many were resolved.
pub fn resolve_corrections(text: &str) -> (String, usize) {
    let expression = correction_expression();
    let mut result = text.to_string();
    let mut count = 0;
    for _ in 0..8 {
        let mut replaced = None;
        for captures in expression.captures_iter(&result).filter_map(Result::ok) {
            let (Some(first), Some(replacement), Some(correction)) = (
                captures.name("first"),
                captures.name("replacement"),
                captures.name("correction"),
            ) else {
                continue;
            };
            let first_kind = kind(first.as_str());
            if first_kind.is_none()
                || first_kind != kind(replacement.as_str())
                || first.as_str().to_lowercase() == replacement.as_str().to_lowercase()
            {
                continue;
            }
            if ends_a_sentence(&result[first.end()..replacement.start()]) {
                continue;
            }
            let before = &result[..correction.start()];
            let clause = before
                .rsplit(|c: char| ".!?\n".contains(c))
                .next()
                .unwrap_or(before);
            if contains_negation(clause) {
                continue;
            }
            replaced = Some((
                correction.start(),
                correction.end(),
                carrying_leading_words(first.as_str(), replacement.as_str()),
            ));
            break;
        }
        let Some((start, end, value)) = replaced else {
            break;
        };
        result.replace_range(start..end, &value);
        count += 1;
    }
    let (result, gen_count) = resolve_general_corrections(&result);
    (result, count + gen_count)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn hesitation_cleanup_repairs_punctuation_and_case() {
        for (input, expected) in [
            ("Hello, um, how are you?", "Hello, how are you?"),
            (
                "How are you? Um I hope you're good.",
                "How are you? I hope you're good.",
            ),
            ("Um, so we ship Friday", "So we ship Friday"),
            ("hello world, um.", "hello world."),
            ("uh uh okay", "okay"),
            ("Umm, uhh, hmm, erm, uhm", ""),
            (
                "the summary says 5 mm and humming",
                "the summary says 5 mm and humming",
            ),
        ] {
            assert_eq!(remove_hesitations(input).0, expected, "{input}");
        }
        assert!(!remove_hesitations("nothing to remove").1);
    }

    #[test]
    fn other_languages_lose_only_universal_hesitations() {
        for (input, language, expected) in [
            (
                "Wir treffen uns um 5 Uhr, ähm, am Bahnhof",
                Some("de"),
                "Wir treffen uns um 5 Uhr, am Bahnhof",
            ),
            (
                "Eu moro em Lisboa, hmm, perto do rio",
                Some("pt"),
                "Eu moro em Lisboa, perto do rio",
            ),
            ("Alors euh on commence", Some("fr"), "Alors on commence"),
            ("Alors euh on commence", None, "Alors euh on commence"),
            ("Uhm, ja", None, "Ja"),
        ] {
            assert_eq!(
                remove_other_language_hesitations(input, language).0,
                expected,
                "{input}"
            );
        }
    }

    #[test]
    fn single_letter_stutters_collapse_but_words_stay() {
        for (input, expected) in [
            ("I I I think so", "I think so"),
            ("no no no, not that", "no no no, not that"),
            ("it is very very very good", "it is very very very good"),
            ("I I think", "I I think"),
            ("Give me, give me, give me the report.", "Give me the report."),
            ("give me, give me, give me", "give me"),
            ("I want to, I want to go home", "I want to go home"),
            ("send the the file", "send the file"),
        ] {
            assert_eq!(collapse_stutters(input).0, expected, "{input}");
        }
    }

    #[test]
    fn explicit_corrections_keep_the_replacement() {
        for (input, expected) in [
            ("Meet me at 2, actually 3 tomorrow", "Meet me at 3 tomorrow"),
            ("Use 2:00 rather 3:30", "Use 3:30"),
            (
                "let's do it tomorrow, oh, no, Wednesday",
                "let's do it Wednesday",
            ),
            (
                "let's do it tomorrow or no Wednesday",
                "let's do it Wednesday",
            ),
            ("see you on Monday, sorry, Tuesday.", "see you on Tuesday."),
            ("meet next Monday, no wait, Friday", "meet next Friday"),
            (
                "the launch is in March, I mean April",
                "the launch is in April",
            ),
            ("call me at 5, no, 6 pm", "call me at 6 pm"),
            (
                "give it 15 minutes, make that 20 minutes",
                "give it 20 minutes",
            ),
            (
                "lo hacemos mañana, no, el miércoles",
                "lo hacemos el miércoles",
            ),
            ("on se voit demain, non, mercredi", "on se voit mercredi"),
            (
                "wir treffen uns morgen, nein, Mittwoch",
                "wir treffen uns Mittwoch",
            ),
            ("vamos amanhã, não, quarta", "vamos quarta"),
            ("ci vediamo domani, anzi, giovedì", "ci vediamo giovedì"),
            (
                "meeting kal, nahi, parso rakhte hain",
                "meeting parso rakhte hain",
            ),
            ("deploy Monday, no, Tuesday", "deploy Tuesday"),
            ("Friday - wait - Saturday", "Saturday"),
            ("meet at 2, sorry, 3 pm", "meet at 3 pm"),
            ("call me at 5 p.m., no, 6 p.m.", "call me at 6 p.m."),
            (
                "let's do it tomorrow… no, Wednesday",
                "let's do it Wednesday",
            ),
            (
                "send the mail to John, sorry, Sarah",
                "send the mail to Sarah",
            ),
            (
                "send it to Rahul, sorry, Rohit",
                "send it to Rohit",
            ),
            (
                "make the button red, no wait, translucent glass",
                "make the button translucent glass",
            ),
            (
                "open the settings page, no wait, the hybrid page",
                "open the hybrid page",
            ),
            (
                "we should—let's deploy it tonight",
                "let's deploy it tonight",
            ),
        ] {
            assert_eq!(resolve_corrections(input).0, expected, "{input}");
        }
    }

    #[test]
    fn things_that_only_look_like_corrections_stay() {
        for text in [
            "meeting kal hai, nahi nahi, parso",
            "Are you coming tomorrow? No, Wednesday.",
            "I can't do Monday, no, Tuesday works",
            "Monday no problem",
            "tomorrow, no, not Wednesday",
            "No, Wednesday works for me",
            "I'll do it, no, really",
            "deploy Monday\nno, Tuesday",
            "deploy Monday, no,\nTuesday",
            "Press 1, wait 2 seconds, then press 3",
            "Boil for 10, wait 5 minutes, then drain",
            "We open Monday, no Sunday hours",
            "I work Monday, no Tuesday or Wednesday",
            "I was actually thrilled with the result",
            "We need 2. Sorry, 3 of us can't make it.",
            "Version 2. Actually 3 people asked",
            "Ship it Monday! Wait, Tuesday is a holiday",
        ] {
            assert_eq!(resolve_corrections(text).0, text, "{text}");
        }
    }
}
