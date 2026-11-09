//! Language detection for transcript cleanup.

use whatlang::Lang;

const MIN_CONFIDENCE: f64 = 0.9;

/// The ISO 639-1 code of `text`'s language, or `None` when unsure.
pub fn detect(text: &str) -> Option<&'static str> {
    let info = whatlang::detect(text)?;
    if !info.is_reliable() || info.confidence() < MIN_CONFIDENCE {
        return None;
    }
    iso_639_1(info.lang())
}

/// The two-letter code for languages.
fn iso_639_1(lang: Lang) -> Option<&'static str> {
    Some(match lang {
        Lang::Eng => "en",
        Lang::Spa => "es",
        Lang::Fra => "fr",
        Lang::Deu => "de",
        Lang::Ita => "it",
        Lang::Por => "pt",
        Lang::Nld => "nl",
        Lang::Rus => "ru",
        Lang::Jpn => "ja",
        Lang::Cmn => "zh",
        Lang::Kor => "ko",
        Lang::Ara => "ar",
        Lang::Hin => "hi",
        Lang::Tur => "tr",
        Lang::Pol => "pl",
        Lang::Ukr => "uk",
        Lang::Swe => "sv",
        Lang::Nob => "no",
        Lang::Dan => "da",
        Lang::Fin => "fi",
        Lang::Ces => "cs",
        Lang::Ell => "el",
        Lang::Heb => "he",
        Lang::Ind => "id",
        Lang::Vie => "vi",
        Lang::Tha => "th",
        Lang::Ron => "ro",
        Lang::Hun => "hu",
        Lang::Cat => "ca",
        Lang::Bul => "bg",
        Lang::Hrv => "hr",
        Lang::Est => "et",
        Lang::Lav => "lv",
        Lang::Lit => "lt",
        Lang::Slk => "sk",
        Lang::Slv => "sl",
        _ => return None,
    })
}

#[cfg(test)]
mod tests {
    use super::detect;

    #[test]
    fn clear_sentences_are_detected() {
        assert_eq!(
            detect("Ich habe um fünf Uhr ein Meeting mit dem ganzen Team, ok"),
            Some("de")
        );
        assert_eq!(
            detect("Please send the report to the whole team before Friday afternoon."),
            Some("en")
        );
        assert_eq!(
            detect("Je voudrais réserver une table pour quatre personnes ce soir."),
            Some("fr")
        );
    }

    #[test]
    fn short_or_unclear_text_is_left_undecided() {
        assert_eq!(detect("ok"), None);
        assert_eq!(detect("um"), None);
        assert_eq!(detect(""), None);
    }
}
