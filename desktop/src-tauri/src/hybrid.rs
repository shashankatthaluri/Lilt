//! Hybrid AI Polish (Bring Your Own Key — BYOK).
//!
//! Stage 1 (Local Audio -> Text) runs offline via whisper.cpp or ONNX Runtime.
//! Stage 2 (Local Rules) applies personal dictionary replacements, vocabulary, and snippets.
//! Stage 3 (Hybrid AI Polish, optional) sends the transcript to the user's configured
//! LLM provider & model to refine grammar, punctuation, structure, and tone.
//! If the network or API fails, Lilt gracefully falls back to the local output.

use serde::{Deserialize, Serialize};
use std::sync::OnceLock;
use std::time::{Duration, Instant};

const REQUEST_TIMEOUT_SECS: u64 = 8;

static HTTP_CLIENT: OnceLock<reqwest::Client> = OnceLock::new();

fn get_http_client() -> &'static reqwest::Client {
    HTTP_CLIENT.get_or_init(|| {
        reqwest::Client::builder()
            .timeout(Duration::from_secs(REQUEST_TIMEOUT_SECS))
            .connect_timeout(Duration::from_secs(4))
            .tcp_nodelay(true)
            .tcp_keepalive(Duration::from_secs(30))
            .pool_max_idle_per_host(8)
            .pool_idle_timeout(Duration::from_secs(90))
            .user_agent("Lilt/1.0 (Windows NT 10.0; Win64; x64)")
            .build()
            .unwrap_or_else(|_| reqwest::Client::new())
    })
}

/// Pre-warms the HTTP/2 and TLS connection in the background while the user is still speaking.
pub fn prewarm_connection(config: &HybridConfig) {
    if !config.enabled {
        return;
    }
    let endpoint = chat_completions_endpoint(&config.provider, &config.base_url);
    let client = get_http_client().clone();
    tauri::async_runtime::spawn(async move {
        let _ = client.request(reqwest::Method::OPTIONS, &endpoint).send().await;
    });
}

#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HybridConfig {
    pub enabled: bool,
    pub provider: String,
    pub api_key: String,
    pub model: String,
    pub base_url: String,
    pub style: String,
    pub custom_prompt: String,
    pub vocabulary_hint: String,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HybridTestResult {
    pub ok: bool,
    pub provider: String,
    pub model: String,
    pub latency_ms: u64,
    pub message: String,
    pub sample_output: String,
}

#[derive(Clone, Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HybridPreviewResult {
    pub local_text: String,
    pub polished_text: String,
    pub latency_ms: u64,
    pub provider: String,
    pub model: String,
    pub used_hybrid: bool,
    pub error: Option<String>,
}

pub fn default_model_for_provider(provider: &str) -> &'static str {
    match provider.trim().to_ascii_lowercase().as_str() {
        "groq" => "qwen/qwen3.8-27b",
        "gemini" => "gemini-2.5-flash",
        "anthropic" => "claude-3-5-haiku-latest",
        "custom" => "gpt-4o-mini",
        _ => "gpt-4o-mini",
    }
}

pub fn provider_display_name(provider: &str) -> &'static str {
    match provider.trim().to_ascii_lowercase().as_str() {
        "groq" => "Groq",
        "gemini" => "Google Gemini",
        "anthropic" => "Anthropic",
        "custom" => "Custom Endpoint",
        _ => "OpenAI",
    }
}

fn chat_completions_endpoint(provider: &str, custom_base_url: &str) -> String {
    match provider.trim().to_ascii_lowercase().as_str() {
        "groq" => "https://api.groq.com/openai/v1/chat/completions".to_string(),
        "gemini" => "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions".to_string(),
        "anthropic" => "https://api.anthropic.com/v1/messages".to_string(),
        "custom" => {
            let trimmed = custom_base_url.trim().trim_end_matches('/');
            if trimmed.is_empty() {
                "https://api.openai.com/v1/chat/completions".to_string()
            } else if trimmed.ends_with("/chat/completions") || trimmed.ends_with("/messages") {
                trimmed.to_string()
            } else if trimmed.ends_with("/v1") || trimmed.ends_with("/openai") {
                format!("{trimmed}/chat/completions")
            } else {
                format!("{trimmed}/v1/chat/completions")
            }
        }
        _ => "https://api.openai.com/v1/chat/completions".to_string(),
    }
}

fn style_instructions(style: &str) -> &'static str {
    match style.trim().to_ascii_lowercase().as_str() {
        "natural" => {
            "Smooth out awkward spoken phrasing, false starts, and repetitions so the text reads naturally and articulately, while keeping the speaker's authentic voice and intent."
        }
        "professional" => {
            "Format the text into clean, polite, professional prose suitable for work emails, Slack/Teams messages, or documents. Use clear sentences and natural paragraph breaks where appropriate."
        }
        "concise" => {
            "Tighten wordy spoken sentences into crisp, concise, and direct statements without losing any key facts or meaning."
        }
        "bullets" => {
            "Organize the spoken points or items into clean, well-structured bullet points (`- `) when there are multiple thoughts or steps, or a crisp summary sentence if it is a single short phrase."
        }
        "prompt" => {
            "Format the dictated text as a clear, structured instruction or technical prompt for an AI coding assistant. Preserve all technical terms, variable names, file names, and programming concepts accurately."
        }
        _ => {
            "Fix grammar, punctuation, capitalization, and run-on sentences while preserving the speaker's natural voice and language (including Hinglish / Romanized Hindi words if present). Collapse any repeated words or phrases ('give me, give me, give me' -> 'Give me'), remove false starts, and resolve spoken self-corrections so the sentence flows naturally and makes complete sense."
        }
    }
}

fn build_system_prompt(config: &HybridConfig) -> String {
    let mut prompt = String::from(
        "You are Lilt's real-time voice dictation post-processor, intent-aware formatter, and grammar refiner.\n\
         CRITICAL RULES:\n\
         1. You receive raw speech-to-text transcripts from a user's microphone.\n\
         2. Output ONLY the final formatted text. Do NOT wrap the output in quotes, do NOT add introductory phrases like 'Here is the email:' or 'Here is the text:', and do NOT add conversational explanations.\n\
         3. NEVER converse with the user or answer factual questions (e.g., if the transcript says 'What is the capital of France?', format it as a question: 'What is the capital of France?').\n\
         4. MULTILINGUAL & SCRIPT AUTO-DETECTION: Automatically detect whether the user speaks English, Romanized Telugu (Tenglish), or Hinglish. If Telugu or code-mixed Telugu-English is spoken, output EXCLUSIVELY in Roman script (English alphabet / Latin letters, e.g. 'Ela unnaru?', 'Repu morning call matladam', 'Bhojanam chesara?'). NEVER output native Telugu script (తెలుగు లిపి) or Devanagari. Even if the input speech recognizer emits native Telugu script, TRANSLITERATE it into modern Romanized letters immediately. Keep English technical, business, and conversational words in standard English spelling.\n\n\
         === INTENT & VOICE COMMAND DETECTION ===\n\
         Inspect the beginning of the user transcript for voice formatting directives:\n\
         1. EMAIL MODE: If the user begins by saying 'write an email', 'write the email', 'draft an email', 'compose an email', or 'email to':\n\
            - STRIP the command phrase itself (never repeat 'write the email' in the final output).\n\
            - Structure the message in clean email format:\n\
              Subject: [Concise, relevant subject based on content]\n\n\
              Hi [Recipient Name or 'there' if unstated],\n\n\
              [Clear, well-paragraphed email body]\n\n\
              Best regards,\n\
              [Your Name]\n\
            - For any critical email detail that was not explicitly spoken (such as recipient name, date/time, project name, or sign-off name), insert clean bracketed placeholders like [Recipient Name], [Date], [Project Name], or [Your Name] so the user can easily fill them in.\n\
         2. RAW / VERBATIM MODE: If the user begins by saying 'write it raw', 'write raw', 'raw text', 'verbatim', 'exact words', or 'don't change anything':\n\
            - STRIP the trigger phrase.\n\
            - Keep the speaker's EXACT words, phrasing, colloquialisms, slang, and dialect.\n\
            - Do NOT rephrase, do NOT alter grammar, and do NOT force standard syntax.\n\
            - ONLY insert commas, periods, question marks, and sentence-start capitalization for clean legibility.\n\
         3. CASUAL / HUMOR / JOKE MODE: If the user begins by saying 'write it casual', 'make it casual', 'joke kind', 'say something funny', 'make it a joke', or 'witty':\n\
            - STRIP the trigger phrase.\n\
            - Deliver the message with a relaxed, playful, witty, or humorous tone while preserving the core message.\n\
         4. BULLET POINTS / LIST MODE: If the user begins by saying 'make a list', 'bullet points', 'bullet this', or 'list of':\n\
            - STRIP the trigger phrase.\n\
            - Format the output as clean bullet points starting with '• '.\n\
         5. DEFAULT MODE (No voice command detected):\n\
            - Remove spoken junk, stutters, repeated words or phrases (e.g., 'give me, give me, give me' -> 'Give me'), false starts, and mid-sentence restarts.\n\
            - When the speaker corrects themselves mid-thought (e.g., 'send it to Rahul, no wait, Priya' or 'let us meet at 4, sorry 5'), keep ONLY the corrected intended thought so the resulting sentence reads smoothly and makes complete sense.\n\
            - Convert numbers to digits ('forty two' -> '42', 'three pm' -> '3 PM') and add proper punctuation.\n\n",
    );

    prompt.push_str("FORMATTING STYLE:\n");
    prompt.push_str(style_instructions(&config.style));
    prompt.push('\n');

    let vocab = config.vocabulary_hint.trim();
    if !vocab.is_empty() {
        prompt.push_str("\nPREFERRED SPELLINGS & VOCABULARY:\n");
        prompt.push_str(vocab);
        prompt.push('\n');
    }

    let custom = config.custom_prompt.trim();
    if !custom.is_empty() {
        prompt.push_str("\nUSER CUSTOM INSTRUCTIONS:\n");
        prompt.push_str(custom);
        prompt.push('\n');
    }

    prompt
}

/// Calls the configured LLM provider and returns `(polished_text, latency_ms)`.
pub fn polish_transcript(config: &HybridConfig, transcript: &str) -> Result<(String, u64), String> {
    let trimmed = transcript.trim();
    if trimmed.is_empty() {
        return Ok((String::new(), 0));
    }
    let api_key = config.api_key.trim();
    let is_local_custom = config.provider.eq_ignore_ascii_case("custom")
        && (config.base_url.contains("localhost") || config.base_url.contains("127.0.0.1"));
    if api_key.is_empty() && !is_local_custom {
        return Err("No API key is configured for Hybrid AI mode.".into());
    }

    let model = if config.model.trim().is_empty() {
        default_model_for_provider(&config.provider).to_string()
    } else {
        config.model.trim().to_string()
    };

    let system_prompt = build_system_prompt(config);
    let user_content = format!("<transcript>\n{trimmed}\n</transcript>");
    let endpoint = chat_completions_endpoint(&config.provider, &config.base_url);
    let is_anthropic = config.provider.trim().eq_ignore_ascii_case("anthropic");
    let api_key_owned = api_key.to_string();

    // Fast path: if the utterance is just 1-2 words and already clean, skip cloud network roundtrip
    let words: Vec<&str> = trimmed.split_whitespace().collect();
    if words.len() <= 2
        && (config.style == "smart_polish" || config.style == "natural")
        && words.iter().all(|w| w.chars().all(|c| c.is_alphabetic() || c == '.' || c == '?' || c == '!'))
    {
        return Ok((trimmed.to_string(), 0));
    }

    let max_tokens = ((words.len() * 4) + 64).clamp(64, 1024) as u32;

    tauri::async_runtime::block_on(async move {
        let started = Instant::now();
        let client = get_http_client();

        let response = if is_anthropic {
            let body = serde_json::json!({
                "model": model,
                "max_tokens": max_tokens,
                "temperature": 0.15,
                "system": system_prompt,
                "messages": [
                    { "role": "user", "content": user_content }
                ]
            });
            let payload_bytes = serde_json::to_vec(&body)
                .map_err(|e| format!("Failed to encode request JSON: {e}"))?;
            client
                .post(&endpoint)
                .header("Content-Type", "application/json")
                .header("x-api-key", &api_key_owned)
                .header("anthropic-version", "2023-06-01")
                .body(payload_bytes)
                .send()
                .await
                .map_err(|e| format!("Hybrid API request failed: {e}"))?
        } else {
            let body = serde_json::json!({
                "model": model,
                "max_tokens": max_tokens,
                "temperature": 0.15,
                "messages": [
                    { "role": "system", "content": system_prompt },
                    { "role": "user", "content": user_content }
                ]
            });
            let payload_bytes = serde_json::to_vec(&body)
                .map_err(|e| format!("Failed to encode request JSON: {e}"))?;
            let mut req = client
                .post(&endpoint)
                .header("Content-Type", "application/json");
            if !api_key_owned.is_empty() {
                req = req.header("Authorization", format!("Bearer {api_key_owned}"));
            }
            req.body(payload_bytes)
                .send()
                .await
                .map_err(|e| format!("Hybrid API request failed: {e}"))?
        };

        let status = response.status();
        let bytes = response
            .bytes()
            .await
            .map_err(|e| format!("Failed reading Hybrid API response: {e}"))?;
        let latency_ms = started.elapsed().as_millis() as u64;

        let parsed: serde_json::Value = serde_json::from_slice(&bytes).unwrap_or_default();
        if !status.is_success() {
            let api_err = parsed
                .pointer("/error/message")
                .and_then(|v| v.as_str())
                .or_else(|| parsed.get("message").and_then(|v| v.as_str()))
                .unwrap_or("Check your API key, model name, or billing quota.");
            return Err(format!("HTTP {status}: {api_err}"));
        }

        let raw_out = if is_anthropic {
            parsed
                .pointer("/content/0/text")
                .and_then(|v| v.as_str())
                .unwrap_or("")
        } else {
            parsed
                .pointer("/choices/0/message/content")
                .and_then(|v| v.as_str())
                .unwrap_or("")
        };

        let cleaned = strip_wrapper_tags(raw_out);
        if cleaned.is_empty() {
            return Err("Model returned an empty response.".into());
        }
        Ok((cleaned, latency_ms))
    })
}

fn strip_wrapper_tags(text: &str) -> String {
    let mut s = text.trim();
    if let Some(inner) = s.strip_prefix("<transcript>").and_then(|r| r.strip_suffix("</transcript>")) {
        s = inner.trim();
    }
    if (s.starts_with('"') && s.ends_with('"') && s.len() >= 2)
        && !s[1..s.len() - 1].contains('"')
    {
        s = &s[1..s.len() - 1];
    }
    s.trim().to_string()
}

/// Counts approximate word-level improvements between local and hybrid-polished text.
pub fn count_word_improvements(before: &str, after: &str) -> u64 {
    let before_words: Vec<&str> = before.split_whitespace().collect();
    let after_words: Vec<&str> = after.split_whitespace().collect();
    if before_words == after_words {
        return 0;
    }
    let min_len = before_words.len().min(after_words.len());
    let mut diffs = before_words.len().abs_diff(after_words.len()) as u64;
    for i in 0..min_len {
        if before_words[i] != after_words[i] {
            diffs += 1;
        }
    }
    diffs.max(1)
}

pub fn test_connection(config: &HybridConfig) -> Result<HybridTestResult, String> {
    let sample = "um hey team let's meet tomorrow at 4 pm to review the new lilt hybrid update";
    let model = if config.model.trim().is_empty() {
        default_model_for_provider(&config.provider).to_string()
    } else {
        config.model.trim().to_string()
    };
    let provider_label = provider_display_name(&config.provider).to_string();
    let (output, latency_ms) = polish_transcript(config, sample)?;
    Ok(HybridTestResult {
        ok: true,
        provider: provider_label.clone(),
        model: model.clone(),
        latency_ms,
        message: format!("Connected to {provider_label} ({model}) in {latency_ms} ms"),
        sample_output: output,
    })
}


fn audio_transcriptions_endpoint(provider: &str, custom_base_url: &str) -> Option<(String, &'static str)> {
    match provider.trim().to_ascii_lowercase().as_str() {
        "groq" => Some(("https://api.groq.com/openai/v1/audio/transcriptions".to_string(), "whisper-large-v3-turbo")),
        "openai" => Some(("https://api.openai.com/v1/audio/transcriptions".to_string(), "whisper-1")),
        "custom" => {
            let trimmed = custom_base_url.trim().trim_end_matches('/');
            if trimmed.is_empty() {
                Some(("https://api.openai.com/v1/audio/transcriptions".to_string(), "whisper-1"))
            } else if trimmed.ends_with("/audio/transcriptions") {
                Some((trimmed.to_string(), "whisper-1"))
            } else if trimmed.ends_with("/v1") || trimmed.ends_with("/openai") {
                Some((format!("{trimmed}/audio/transcriptions"), "whisper-1"))
            } else {
                Some((format!("{trimmed}/v1/audio/transcriptions"), "whisper-1"))
            }
        }
        _ => None,
    }
}

pub fn encode_pcm_to_wav(pcm: &[f32]) -> Result<Vec<u8>, String> {
    let spec = hound::WavSpec {
        channels: 1,
        sample_rate: 16_000,
        bits_per_sample: 16,
        sample_format: hound::SampleFormat::Int,
    };
    let mut cursor = std::io::Cursor::new(Vec::new());
    let mut writer = hound::WavWriter::new(&mut cursor, spec)
        .map_err(|e| format!("Could not initialize WAV encoder: {e}"))?;
    for &sample in pcm {
        writer
            .write_sample((sample.clamp(-1.0, 1.0) * i16::MAX as f32) as i16)
            .map_err(|e| format!("WAV write sample failed: {e}"))?;
    }
    writer.finalize().map_err(|e| format!("WAV finalize failed: {e}"))?;
    Ok(cursor.into_inner())
}

fn build_multipart_audio(
    wav_bytes: &[u8],
    model: &str,
    language: Option<&str>,
    prompt: Option<&str>,
) -> (Vec<u8>, String) {
    let boundary = format!("----LiltBoundary{:x}", std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_nanos());
    let mut body = Vec::new();

    // field: model
    body.extend_from_slice(format!("--{boundary}\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\n{model}\r\n").as_bytes());

    // field: response_format
    body.extend_from_slice(format!("--{boundary}\r\nContent-Disposition: form-data; name=\"response_format\"\r\n\r\njson\r\n").as_bytes());

    // field: language (optional)
    if let Some(lang) = language {
        if !lang.trim().is_empty() && lang != "auto" {
            body.extend_from_slice(format!("--{boundary}\r\nContent-Disposition: form-data; name=\"language\"\r\n\r\n{lang}\r\n").as_bytes());
        }
    }

    // field: prompt (optional)
    if let Some(p) = prompt {
        let trimmed_p = p.trim();
        if !trimmed_p.is_empty() {
            body.extend_from_slice(format!("--{boundary}\r\nContent-Disposition: form-data; name=\"prompt\"\r\n\r\n{trimmed_p}\r\n").as_bytes());
        }
    }

    // field: file
    body.extend_from_slice(format!("--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\nContent-Type: audio/wav\r\n\r\n").as_bytes());
    body.extend_from_slice(wav_bytes);
    body.extend_from_slice(b"\r\n");

    // end
    body.extend_from_slice(format!("--{boundary}--\r\n").as_bytes());

    (body, format!("multipart/form-data; boundary={boundary}"))
}

pub fn transcribe_cloud_audio(
    config: &HybridConfig,
    pcm: &[f32],
    language: Option<&str>,
) -> Result<(String, u64), String> {
    if !config.enabled {
        return Err("Cloud AI is disabled.".into());
    }
    let (endpoint, default_model) = audio_transcriptions_endpoint(&config.provider, &config.base_url)
        .ok_or_else(|| format!("Provider {} does not support direct cloud audio transcription.", config.provider))?;

    let api_key = config.api_key.trim();
    if api_key.is_empty() {
        return Err("Cloud API key is missing.".into());
    }

    let wav_bytes = encode_pcm_to_wav(pcm)?;
    let prompt = if !config.vocabulary_hint.trim().is_empty() {
        Some(config.vocabulary_hint.as_str())
    } else {
        None
    };

    let (payload, content_type) = build_multipart_audio(&wav_bytes, default_model, language, prompt);
    let api_key_owned = api_key.to_string();

    tauri::async_runtime::block_on(async move {
        let started = Instant::now();
        let client = get_http_client();
        let resp = client
            .post(&endpoint)
            .header("Content-Type", content_type)
            .header("Authorization", format!("Bearer {api_key_owned}"))
            .body(payload)
            .send()
            .await
            .map_err(|e| format!("Cloud ASR request failed: {e}"))?;

        let status = resp.status();
        let latency_ms = started.elapsed().as_millis() as u64;
        let body_bytes = resp
            .bytes()
            .await
            .map_err(|e| format!("Failed to read Cloud ASR response: {e}"))?;

        let parsed: serde_json::Value = serde_json::from_slice(&body_bytes)
            .map_err(|e| format!("Invalid JSON from Cloud ASR: {e}"))?;

        if !status.is_success() {
            let err_msg = parsed
                .pointer("/error/message")
                .and_then(|v| v.as_str())
                .unwrap_or("Cloud ASR transcription error.");
            return Err(format!("Cloud ASR HTTP {status}: {err_msg}"));
        }

        let text = parsed
            .get("text")
            .and_then(|v| v.as_str())
            .unwrap_or("")
            .trim();

        if text.is_empty() {
            return Err("Cloud ASR returned empty transcription.".into());
        }

        Ok((text.to_string(), latency_ms))
    })
}
