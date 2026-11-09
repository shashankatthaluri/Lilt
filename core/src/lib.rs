pub mod cleanup;
pub mod dictionary;
pub mod hinglish;
pub mod jni;
pub mod lang_id;
pub mod pipeline;
pub mod spoken_emoji;
pub mod spoken_numbers;

pub use cleanup::{collapse_stutters, remove_hesitations, resolve_corrections};
pub use dictionary::{correct, snippet_hits, Replacement, Snippet};
pub use pipeline::{process, process_with_metrics, TextOptions};

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_stutter_collapse() {
        let (out, count) = collapse_stutters("I want to, I want to change the model");
        assert_eq!(out, "I want to change the model");
        assert_eq!(count, 1);
    }

    #[test]
    fn test_correction_resolution() {
        let (out, count) = resolve_corrections("send the mail to John, sorry, Sarah");
        assert_eq!(out, "send the mail to Sarah");
        assert_eq!(count, 1);
    }

    #[test]
    fn test_full_pipeline_process() {
        let options = TextOptions {
            language: Some("en"),
            cleanup: true,
            vocabulary: &[],
            replacements: &[],
            snippets: &[],
            numbers: true,
            symbols: true,
            emoji: true,
            auto_capitalize: true,
            trailing_space: true,
        };
        let output = process("give me, give me three apples, crying emoji", &options);
        assert!(output.contains("Give me 3 apples, 😭"));
    }
}
