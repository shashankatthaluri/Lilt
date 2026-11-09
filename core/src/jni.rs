use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;

use crate::dictionary::{Replacement, Snippet};
use crate::pipeline::{self, TextOptions};

#[no_mangle]
pub extern "system" fn Java_com_lilt_mobile_core_LiltCore_cleanText(
    mut env: JNIEnv,
    _class: JClass,
    raw_text: JString,
    cleanup_level: JString,
    numbers_as_digits: bool,
    number_symbols: bool,
    spoken_emoji: bool,
    auto_capitalize: bool,
    trailing_space: bool,
) -> jstring {
    let raw: String = match env.get_string(&raw_text) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };
    let level: String = match env.get_string(&cleanup_level) {
        Ok(s) => s.into(),
        Err(_) => "medium".into(),
    };

    let cleanup_enabled = level != "none";
    let options = TextOptions {
        language: Some("en"),
        cleanup: cleanup_enabled,
        vocabulary: &[],
        replacements: &[],
        snippets: &[],
        numbers: numbers_as_digits,
        symbols: number_symbols,
        emoji: spoken_emoji,
        auto_capitalize,
        trailing_space,
    };

    let result = pipeline::process(&raw, &options);
    match env.new_string(result) {
        Ok(js) => js.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_lilt_mobile_core_LiltCore_applyReplacements(
    mut env: JNIEnv,
    _class: JClass,
    text_str: JString,
    replacements_json: JString,
) -> jstring {
    let text: String = match env.get_string(&text_str) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };
    let json_str: String = match env.get_string(&replacements_json) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };

    let replacements: Vec<Replacement> = serde_json::from_str(&json_str).unwrap_or_default();
    let corrected = crate::dictionary::correct(&text, &[], &replacements);
    match env.new_string(corrected.text) {
        Ok(js) => js.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_lilt_mobile_core_LiltCore_expandSnippets(
    mut env: JNIEnv,
    _class: JClass,
    text_str: JString,
    snippets_json: JString,
) -> jstring {
    let text: String = match env.get_string(&text_str) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };
    let json_str: String = match env.get_string(&snippets_json) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };

    let snippets: Vec<Snippet> = serde_json::from_str(&json_str).unwrap_or_default();
    let hits = crate::dictionary::snippet_hits(&text, &snippets);
    if hits.is_empty() {
        return match env.new_string(&text) {
            Ok(js) => js.into_raw(),
            Err(_) => std::ptr::null_mut(),
        };
    }

    let mut result = text.clone();
    // Apply expansions in reverse order of position so indices stay valid
    for (start, end, expansion) in hits.iter().rev() {
        if *start <= result.len() && *end <= result.len() && start <= end {
            result.replace_range(*start..*end, expansion);
        }
    }

    match env.new_string(result) {
        Ok(js) => js.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}
