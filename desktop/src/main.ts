import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import "./style.css";
import sidebarMark from "./assets/voca-logo.svg?raw";
import dictateIdle from "./assets/vocawin-dictate-idle.svg?raw";
import dictateListening from "./assets/vocawin-dictate-listening.svg?raw";
import familyLogo from "../web/assets/brand/voca-logo-512.png";
import liltLockup from "./assets/lilt-brand-lockup.png";
import hqMarkRaw from "../web/assets/brand/vocahq/voca-mark.svg?raw";
import linuxMarkRaw from "../web/assets/brand/promo/cards/platform/linux.svg?raw";
import appleMarkRaw from "../web/assets/brand/promo/cards/platform/apple.svg?raw";
import androidMarkRaw from "../web/assets/brand/promo/cards/platform/android.svg?raw";
import gatewayMarkRaw from "../web/assets/brand/vocagateway/vocagateway-1u.svg?raw";
import discordMark from "../web/assets/brand/vocahq/social/discord.svg?raw";
import githubMark from "../web/assets/brand/vocahq/social/github.svg?raw";
import mailMark from "../web/assets/brand/vocahq/social/mail.svg?raw";
import xMark from "../web/assets/brand/vocahq/social/x.svg?raw";

type Model = { id: string; name: string; engine: string; size: string; languages: string; acceleration: string; description: string };
type Settings = {
  hotkey: string;
  activationMode: string;
  language: string;
  silenceSeconds: number;
  maxRecordingSeconds: number;
  launchAtLogin: boolean;
  soundEffects: boolean;
  soundTheme: string;
  appendTrailingSpace: boolean;
  autoCapitalize: boolean;
  selectedModel: string;
  inputDevice: string;
  autoPauseEnabled: boolean;
  autoPauseApps: string;
  idleUnloadEnabled: boolean;
  idleUnloadSeconds: number;
  welcomeDismissed: boolean;
  historyEnabled: boolean;
  debugLogging: boolean;
  customVocabulary: string;
  copyToClipboard: boolean;
  cleanupLevel: string;
  numbersAsDigits: boolean;
  numberSymbols: boolean;
  spokenEmoji: boolean;
  replacements: Replacement[];
  snippets: Snippet[];
  escapeCancels: boolean;
  handsFreeHotkey: string;
  pasteLastHotkey: string;
  mouseButton: string;
  overlayStyle: string;
  overlayPosition: string;
  overlayCustomX?: number | null;
  overlayCustomY?: number | null;
  readyPill: boolean;
  skipSilence: boolean;
  livePreview: boolean;
  muteOtherAudio: boolean;
  historyRetentionDays: number;
  historyKeepAudio: boolean;
  insertionMode: string;
  pasteApps: string;
  hybridEnabled: boolean;
  hybridProvider: string;
  hybridApiKey: string;
  hybridModel: string;
  hybridBaseUrl: string;
  hybridStyle: string;
  hybridCustomPrompt: string;
};
type Replacement = { heard: string; replacement: string };
type Snippet = { trigger: string; expansion: string };
type View = "dictation" | "shortcuts" | "models" | "audio" | "hybrid" | "formatting" | "dictionary" | "snippets" | "history" | "stats" | "settings" | "power" | "debug" | "about";
type HybridTestResult = {
  ok: boolean;
  provider: string;
  model: string;
  latencyMs: number;
  message: string;
  sampleOutput: string;
};
type HybridPreviewResult = {
  localText: string;
  polishedText: string;
  latencyMs: number;
  provider: string;
  model: string;
  usedHybrid: boolean;
  error?: string;
};
type HistoryEntry = {
  id: number;
  text: string;
  modelId: string;
  createdAtMs: number;
  audioFile?: string;
  durationMs?: number;
  status?: string;
  error?: string;
};
type BreakdownRow = { name: string; dictations: number; words: number; percent: number };
type DayTotals = { dictations: number; words: number };
type StatsSummary = {
  dictations: number;
  words: number;
  characters: number;
  audioMinutes: number;
  wordsPerMinute: number;
  timeSavedMinutes: number;
  wordsToday: number;
  wordsThisWeek: number;
  currentStreak: number;
  longestStreak: number;
  activeDays: number;
  wordsCorrected: number;
  dictionaryFixes: number;
  fixesTotal: number;
  hesitationsRemoved: number;
  stuttersCollapsed: number;
  uniqueWordsCount: number;
  avgTakeSeconds: number;
  avgWordsPerTake: number;
  apps: BreakdownRow[];
  models: BreakdownRow[];
  days: Record<string, DayTotals>;
  recent: Array<{ day: string; words: number }>;
};
/** Which shortcut the Record button is capturing. */
type CaptureTarget = "hotkey" | "handsFreeHotkey" | "pasteLastHotkey";
type ModelStatus = { installed: boolean; downloadable: boolean; downloading: boolean; progress: number; message?: string; bytesOnDisk?: number };
type HotkeyPreset = { id: string; label: string };
type GpuStatus = {
  available: boolean;
  name: string;
  backend: string;
  detail: string;
  deviceIndex: number;
  discrete: boolean;
  vramMb: number;
};
type InputDevice = { name: string; isDefault: boolean };
type ModelRecommendation = { modelId: string; modelName: string; reason: string; vramMb: number; gpu: GpuStatus };
type RuntimeStatus = {
  status: string;
  recording: boolean;
  paused: boolean;
  modelLoaded: boolean;
  parkKind: string;
  parkDetail: string;
  hotkey: string;
  inputDevice: string;
  gpuName: string;
  gpuBackend: string;
  gpuDetail?: string;
};
type LogLine = { level: string; text: string };
type DebugReport = {
  version: string;
  os: string;
  cpu: string;
  ram: string;
  gpu: GpuStatus;
  debugLogging: boolean;
  text: string;
};
type RunningApp = { name: string; label: string };
type EngineFilter = "all" | "whisper" | "onnx";
type LanguageFilter = "any" | "english" | "multilingual";

/** One searchable settings row. `page` is the sidebar page it lives on;
 *  `card` groups rows inside that page. */
type SettingsItem = {
  page: View;
  card: string;
  title: string;
  subtitle: string;
  keywords: string;
  html: string;
  /** Control sits under the text instead of beside it. */
  wide?: boolean;
  /** Extra markup under the row (the watched-app chips). */
  after?: string;
};

/** Sidebar sections, in VocaMac's order: Dictation, Writing, Activity, App. */
const NAV_ICONS: Record<View, string> = {
  dictation: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" x2="12" y1="19" y2="22"/></svg>`,
  shortcuts: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect width="20" height="16" x="2" y="4" rx="2"/><path d="M6 8h.001M10 8h.001M14 8h.001M18 8h.001M8 12h.001M12 12h.001M16 12h.001M7 16h10"/></svg>`,
  models: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>`,
  audio: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M2 10v3"/><path d="M6 6v11"/><path d="M10 3v18"/><path d="M14 8v7"/><path d="M18 5v13"/><path d="M22 10v3"/></svg>`,
  hybrid: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l1.9 5.8a2 2 0 0 0 1.3 1.3L21 12l-5.8 1.9a2 2 0 0 0-1.3 1.3L12 21l-1.9-5.8a2 2 0 0 0-1.3-1.3L3 12l5.8-1.9a2 2 0 0 0 1.3-1.3L12 3z"/></svg>`,
  formatting: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17 6.1H3"/><path d="M21 12.1H3"/><path d="M15.1 18H3"/></svg>`,
  dictionary: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"/></svg>`,
  snippets: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>`,
  history: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>`,
  stats: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect width="7" height="9" x="3" y="3" rx="1"/><rect width="7" height="5" x="14" y="3" rx="1"/><rect width="7" height="9" x="14" y="12" rx="1"/><rect width="7" height="5" x="3" y="16" rx="1"/></svg>`,
  settings: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><line x1="4" x2="20" y1="12" y2="12"/><line x1="4" x2="20" y1="6" y2="6"/><line x1="4" x2="20" y1="18" y2="18"/><circle cx="8" cy="6" r="2" fill="currentColor"/><circle cx="16" cy="12" r="2" fill="currentColor"/><circle cx="10" cy="18" r="2" fill="currentColor"/></svg>`,
  power: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18.36 6.64a9 9 0 1 1-12.73 0"/><line x1="12" x2="12" y1="2" y2="12"/></svg>`,
  debug: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="4 17 10 11 4 5"/><line x1="12" x2="20" y1="19" y2="19"/></svg>`,
  about: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>`,
};
const NAV_SECTIONS: Array<{ label: string; items: Array<[View, string, string]> }> = [
  { label: "Dictation", items: [["dictation", "Dictation", NAV_ICONS.dictation], ["shortcuts", "Shortcuts", NAV_ICONS.shortcuts], ["models", "Models", NAV_ICONS.models], ["audio", "Audio", NAV_ICONS.audio]] },
  { label: "Writing", items: [["hybrid", "Hybrid", NAV_ICONS.hybrid], ["formatting", "Formatting", NAV_ICONS.formatting], ["dictionary", "Dictionary", NAV_ICONS.dictionary], ["snippets", "Snippets", NAV_ICONS.snippets]] },
  { label: "Activity", items: [["history", "History", NAV_ICONS.history], ["stats", "Insights", NAV_ICONS.stats]] },
  { label: "App", items: [["settings", "General", NAV_ICONS.settings], ["power", "Power", NAV_ICONS.power], ["debug", "Debug", NAV_ICONS.debug], ["about", "About", NAV_ICONS.about]] },
];
const ALL_VIEWS: View[] = NAV_SECTIONS.flatMap(section => section.items.map(([id]) => id));

/** Shortcut choices for hands-free and paste-last. Lone modifiers are hold
 *  keys, so they are left to the main hotkey. */
const EXTRA_SHORTCUTS: Array<[string, string]> = [
  ["", "Off"],
  ["Ctrl+Space", "Ctrl+Space"],
  ["Alt+Space", "Alt+Space"],
  ["F7", "F7"],
  ["F8", "F8"],
  ["F9", "F9"],
  ["F10", "F10"],
  ["Ctrl+Alt+Space", "Ctrl+Alt+Space"],
  ["Ctrl+Shift+Space", "Ctrl+Shift+Space"],
  ["Ctrl+Alt+V", "Ctrl+Alt+V"],
  ["Ctrl+Shift+Alt+V", "Ctrl+Shift+Alt+V"],
];
const MOUSE_BUTTONS: Array<[string, string]> = [
  ["", "Off"],
  ["middle", "Middle button"],
  ["x1", "Back side button"],
  ["x2", "Forward side button"],
];
const RETENTION_CHOICES: Array<[number, string]> = [
  [1, "1 day"],
  [7, "7 days"],
  [30, "30 days"],
  [0, "Forever"],
];

const ENGINE_FILTERS: Array<[EngineFilter, string]> = [
  ["all", "All engines"],
  ["whisper", "Whisper"],
  ["onnx", "ONNX"],
];
const LANGUAGE_FILTERS: Array<[LanguageFilter, string]> = [
  ["any", "Any language"],
  ["english", "English"],
  ["multilingual", "Multilingual"],
];
const IDLE_PRESETS: Array<[number, string]> = [
  [0, "Never"],
  [300, "5 minutes"],
  [900, "15 minutes"],
  [1800, "30 minutes"],
  [3600, "1 hour"],
];

const SOUND_THEMES: Array<[string, string]> = [
  ["lift", "Lift"],
  ["flick", "Flick"],
  ["ember", "Ember"],
  ["step", "Step"],
  ["voca", "Lilt Signature"],
  ["soft", "Soft"],
  ["chirp", "Chirp"],
  ["scale", "Scale"],
  ["drop", "Drop"],
  ["glass", "Glass"],
  ["off", "Off"],
];

const LANGUAGE_CORE = [
  "Spanish",
  "French",
  "German",
  "Italian",
  "Portuguese",
  "Dutch",
  "Russian",
  "Japanese",
  "Chinese",
  "Korean",
  "Arabic",
  "Hindi",
  "Turkish",
  "Polish",
  "Ukrainian",
  "Swedish",
  "Norwegian",
  "Danish",
  "Finnish",
  "Czech",
  "Greek",
  "Hebrew",
  "Indonesian",
  "Vietnamese",
  "Thai",
  "Romanian",
  "Hungarian",
  "Bulgarian",
  "Croatian",
  "Estonian",
  "Latvian",
  "Lithuanian",
  "Maltese",
  "Slovak",
  "Slovenian",
  "Catalan",
];

const LANGUAGE_CHOICES = ["Auto-detect", "English", ...[...LANGUAGE_CORE].sort((a, b) => a.localeCompare(b))];

const ICON_DOWNLOAD = `<svg width="15" height="15" viewBox="0 0 16 16" aria-hidden="true"><path fill="currentColor" d="M8 1.5a.75.75 0 0 1 .75.75v6.19l1.72-1.72a.75.75 0 1 1 1.06 1.06l-3 3a.75.75 0 0 1-1.06 0l-3-3a.75.75 0 0 1 1.06-1.06l1.72 1.72V2.25A.75.75 0 0 1 8 1.5Zm-4.5 9a.75.75 0 0 1 .75.75v1.5h7.5v-1.5a.75.75 0 0 1 1.5 0v2.25c0 .41-.34.75-.75.75h-9a.75.75 0 0 1-.75-.75V11.25A.75.75 0 0 1 3.5 10.5Z"/></svg>`;
const ICON_TRASH = `<svg width="15" height="15" viewBox="0 0 16 16" aria-hidden="true"><path fill="currentColor" d="M6.2 1.75A1.25 1.25 0 0 1 7.4.75h1.2c.55 0 1.03.36 1.2.88l.2.62h2.7a.75.75 0 0 1 0 1.5h-.3l-.55 8.08A1.75 1.75 0 0 1 10.11 13.5H5.89A1.75 1.75 0 0 1 4.15 11.83L3.6 3.75h-.35a.75.75 0 0 1 0-1.5h2.7l.2-.62c.17-.52.65-.88 1.2-.88Zm.55 1.5-.1.37h2.7l-.1-.37-.05-.13H6.8l-.05.13ZM5.1 3.75l.54 8.02a.25.25 0 0 0 .25.23h4.22a.25.25 0 0 0 .25-.23l.54-8.02H5.1Z"/></svg>`;
const ICON_CHECK = `<svg width="13" height="13" viewBox="0 0 16 16" aria-hidden="true"><path fill="currentColor" d="M6.4 10.3 3.85 7.74a.75.75 0 0 0-1.06 1.06l3.1 3.1a.75.75 0 0 0 1.08-.02l6.2-6.6A.75.75 0 0 0 12.1 4.2l-5.7 6.1Z"/></svg>`;
const ICON_PLAY = `<svg width="14" height="14" viewBox="0 0 16 16" aria-hidden="true"><path fill="currentColor" stroke="currentColor" stroke-width="1" stroke-linejoin="round" d="M4.2 2.4v11.2L13.6 8z"/></svg>`;
const ICON_STOP = `<svg width="14" height="14" viewBox="0 0 16 16" aria-hidden="true"><path fill="currentColor" stroke="currentColor" stroke-width="1" d="M4 4h8v8H4z"/></svg>`;
const WARN_ICON = `<svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true" style="vertical-align:-2px; margin-right:5px;"><path d="M8 2 15 14H1z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="M8 6.5v3.5M8 12v.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>`;

function themeBrandSvg(svg: string, opts: { dropPlate?: boolean } = {}) {
  let out = svg;
  if (opts.dropPlate) {
    out = out.replace(/<rect width="1024" height="1024"[^>]*\/?>\s*/i, "");
    out = out.replace(/viewBox="0 0 1024 1024"/i, 'viewBox="80 350 864 330"');
  }
  // Kit marks bake HQ ink (#0B1A15), plate green (#0F6B57), and paper (#F2F6F2).
  // Swap those fills so About follows the row's currentColor on light and dark.
  out = out
    .replace(/fill="#0[Bb]1[Aa]15"/g, 'fill="currentColor"')
    .replace(/stroke="#0[Bb]1[Aa]15"/g, 'stroke="currentColor"')
    .replace(/fill="#0[Ff]6[Bb]57"/g, 'fill="currentColor"')
    .replace(/stroke="#0[Ff]6[Bb]57"/g, 'stroke="currentColor"')
    .replace(/fill="#F2F6F2"/gi, 'fill="currentColor" fill-opacity="0.16"')
    .replace(/stroke="#F2F6F2"/gi, 'stroke="currentColor"');
  const openTagEnd = out.indexOf(">");
  const openTag = openTagEnd >= 0 ? out.slice(0, openTagEnd) : out;
  if (!/\sfill=/i.test(openTag)) {
    out = out.replace(/<svg\b/i, '<svg fill="currentColor"');
  }
  if (!/aria-hidden=/i.test(out)) {
    out = out.replace(/<svg\b/i, '<svg aria-hidden="true"');
  }
  return out;
}

const hqMark = themeBrandSvg(hqMarkRaw);
const linuxMark = themeBrandSvg(linuxMarkRaw);
const appleMark = themeBrandSvg(appleMarkRaw);
const androidMark = themeBrandSvg(androidMarkRaw);
const gatewayMark = themeBrandSvg(gatewayMarkRaw, { dropPlate: true });

const app = document.querySelector<HTMLDivElement>("#app")!;
let models: Model[] = [];
let statuses: Record<string, ModelStatus> = {};
let history: HistoryEntry[] = [];
let historyQuery = "";
let stats: StatsSummary | null = null;
let insightsTab: "usage" | "voice" = "usage";
let insightsMonthOffset = 0;
let retrying = new Set<number>();
let playingId: number | null = null;
let tryText = "";
let tryResult = "";
let hybridShowKey = false;
let hybridTesting = false;
let hybridTestStatus: { ok: boolean; text: string } | null = null;
let hybridTryText = "um hey team let's push the new lilt hinglish update tomorrow, oh no, friday at 4 pm and make sure the right alt shortcut works smoothly";
let hybridPreviewLoading = false;
let hybridPreviewResult: HybridPreviewResult | null = null;

const HYBRID_PROVIDERS: Array<[string, string]> = [
  ["openai", "OpenAI"],
  ["groq", "Groq (Ultra-Fast LPU)"],
  ["gemini", "Google Gemini"],
  ["anthropic", "Anthropic Claude"],
  ["custom", "Custom (OpenAI-Compatible)"],
];

const HYBRID_MODELS_BY_PROVIDER: Record<string, Array<[string, string]>> = {
  openai: [
    ["gpt-4o-mini", "GPT-4o mini (Fast & Recommended)"],
    ["gpt-4.1-mini", "GPT-4.1 mini"],
    ["gpt-4.1-nano", "GPT-4.1 nano (Ultra-Low Latency)"],
    ["gpt-4o", "GPT-4o (High Intelligence)"],
  ],
  groq: [
    ["qwen/qwen3.8-27b", "Qwen 3.8 27B (Ultra-Fast & Smart)"],
    ["openai/gpt-oss-20b", "GPT-OSS 20B"],
    ["llama-3.3-70b-versatile", "Llama 3.3 70B Versatile"],
    ["llama-3.1-8b-instant", "Llama 3.1 8B Instant (~90ms)"],
  ],
  gemini: [
    ["gemini-2.5-flash", "Gemini 2.5 Flash (Recommended)"],
    ["gemini-2.0-flash", "Gemini 2.0 Flash"],
    ["gemini-2.5-flash-lite", "Gemini 2.5 Flash-Lite"],
  ],
  anthropic: [
    ["claude-3-5-haiku-latest", "Claude 3.5 Haiku (Fast & Recommended)"],
    ["claude-sonnet-4-20250514", "Claude Sonnet 4"],
  ],
  custom: [
    ["gpt-4o-mini", "gpt-4o-mini"],
    ["llama3.2", "llama3.2 (Ollama)"],
    ["deepseek-chat", "deepseek-chat"],
  ],
};

const HYBRID_STYLES: Array<[string, string]> = [
  ["smart_polish", "Smart Polish (Default — Grammar & punctuation, preserves your voice & Hinglish)"],
  ["natural", "Natural & Articulate (Smooths awkward spoken phrasing)"],
  ["professional", "Professional & Email (Clean, business-ready formatting)"],
  ["concise", "Concise & Direct (Removes fluff, crisp statements)"],
  ["bullets", "Structured Bullet Points (Organizes spoken thoughts into lists)"],
  ["prompt", "AI Prompt & Technical (Optimized for coding & AI assistants)"],
];
/** Page to return to when the sidebar search is cleared. */
let pageBeforeSearch: View | null = null;
let onboardingStep = 0;
let onboardingModel = "";
let onboardingTryText = "";
let settings: Settings;
let presets: HotkeyPreset[] = [];
let gpu: GpuStatus = {
  available: false,
  name: "Checking…",
  backend: "CPU",
  detail: "",
  deviceIndex: -1,
  discrete: false,
  vramMb: 0,
};
let devices: InputDevice[] = [];
let recommendation: ModelRecommendation | null = null;
let runtime: RuntimeStatus = {
  status: "Ready",
  recording: false,
  paused: false,
  modelLoaded: false,
  parkKind: "",
  parkDetail: "",
  hotkey: "",
  inputDevice: "Default microphone",
  gpuName: "",
  gpuBackend: "",
};
let recording = false;
let recordingHotkey: CaptureTarget | null = null;
/** The keyboard hook is recording the shortcut (Windows), not this page. */
let nativeCapture = false;
let testingDictation = false;
let testListening = false;
let testResult = "";
let micTesting = false;
let micLevel = 0;
let micMeterTimer: number | null = null;
let settingsQuery = "";
let modelQuery = "";
let engineFilter: EngineFilter = "all";
let languageFilter: LanguageFilter = "any";
let view: View = "dictation";
let toastText = "";
let toastTimer: number | null = null;
let logLines: LogLine[] = [];
let debugReport: DebugReport | null = null;
let previewStartNext = true;
let runningApps: RunningApp[] = [];
let focusRestore: { id: string; start: number; end: number } | null = null;
let paneScroll = 0;
let resetPaneScroll = false;
let micPeak = 0;

const escape = (value: string) => value.replace(/[&<>"]/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]!));
const selected = () => models.find(model => model.id === settings.selectedModel);
const modelInstalled = () => !!statuses[settings.selectedModel]?.installed;
const formatBytes = (bytes?: number) => {
  if (!bytes) return "";
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB on disk`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(bytes >= 100 * 1024 * 1024 ? 0 : 1)} MB on disk`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB on disk`;
};
const nav = (id: View, label: string, icon: string, count = 0) => `<button class="nav ${view === id ? "active" : ""}" data-view="${id}"><span class="nav-icon">${icon}</span>${label}${count ? `<span class="nav-count" aria-label="${count} matching settings">${count}</span>` : ""}</button>`;

function emptySpeechMessage() {
  return modelInstalled()
    ? "No speech was recognized."
    : "Install a speech model first (Models tab).";
}

function modelIsEnglishOnly(model: Model) {
  return model.languages.trim().toLowerCase() === "english";
}

function matchesEngine(model: Model, filter: EngineFilter) {
  if (filter === "all") return true;
  if (filter === "whisper") return model.engine === "whisper.cpp";
  return model.engine === "ONNX Runtime";
}

function matchesLanguage(model: Model, filter: LanguageFilter) {
  if (filter === "any") return true;
  if (filter === "english") return modelIsEnglishOnly(model);
  return !modelIsEnglishOnly(model);
}

function matchesModelQuery(model: Model, query: string) {
  if (!query) return true;
  const hay = `${model.name} ${model.id} ${model.engine} ${model.languages} ${model.description}`.toLowerCase();
  return hay.includes(query);
}

function filteredModels() {
  const query = modelQuery.trim().toLowerCase();
  return models.filter(model =>
    matchesEngine(model, engineFilter)
    && matchesLanguage(model, languageFilter)
    && matchesModelQuery(model, query)
  );
}

function paintToast(text: string) {
  toastText = text;
  const node = document.querySelector<HTMLElement>("#sidebar-toast");
  if (!node) return;
  if (!text) {
    node.hidden = true;
    node.innerHTML = "";
    return;
  }
  node.hidden = false;
  if (text === "Dictation failed") {
    node.innerHTML = `${WARN_ICON}Dictation failed`;
  } else {
    node.textContent = text;
  }
}

function sidebarStatusLabel() {
  if (recording) return "Listening";
  if (testingDictation) return "Testing…";
  if (runtime.parkKind === "idle") return "Unloaded";
  if (runtime.paused || runtime.parkKind === "autopause") return "Paused";
  return "Ready";
}

function paintSidebarStatus() {
  const node = document.querySelector(".sidebar-status");
  if (!node) return;
  const parked = !!runtime.parkKind && !recording;
  node.classList.toggle("parked", parked);
  node.classList.toggle("live", recording);
  const strong = node.querySelector("strong");
  if (strong) strong.textContent = sidebarStatusLabel();
  const small = node.querySelector("small");
  if (small) {
    small.textContent = runtime.parkDetail && !recording
      ? runtime.parkDetail
      : runtime.inputDevice;
  }
  const test = document.querySelector<HTMLButtonElement>("#test-dictation");
  if (test) test.disabled = runtime.paused || micTesting;
}

function sessionChromeChanged(before: { recording: boolean; parked: boolean; status: string; parkKind: string; paused: boolean }) {
  const parked = !!runtime.parkKind && !recording;
  return before.recording !== recording
    || before.parked !== parked
    || before.status !== runtime.status
    || before.parkKind !== runtime.parkKind
    || before.paused !== runtime.paused;
}

function applyRuntimeStatus(payload: RuntimeStatus | string) {
  const before = {
    recording,
    parked: !!runtime.parkKind && !recording,
    status: runtime.status,
    parkKind: runtime.parkKind,
    paused: runtime.paused,
  };
  if (typeof payload === "string") {
    runtime = { ...runtime, status: payload, paused: payload === "Paused" };
  } else {
    runtime = { ...runtime, ...payload };
  }
  recording = !!runtime.recording;
  if (!runtime.recording) testListening = false;
  if (sessionChromeChanged(before)) {
    render();
    return;
  }
  paintSidebarStatus();
  paintToast(toastText);
}

function showToast(text: string) {
  if (toastTimer) window.clearTimeout(toastTimer);
  paintToast(text);
  toastTimer = window.setTimeout(() => {
    toastTimer = null;
    paintToast("");
  }, 1800);
}

function visibleLogs() {
  if (settings?.debugLogging) return logLines;
  return logLines.filter(line => line.level === "warn" || line.level === "error");
}

function watchedApps() {
  return settings.autoPauseApps
    .split(/[\n,;]+/)
    .map(part => part.trim())
    .filter(Boolean);
}

function iconButton(action: string, id: string, label: string, icon: string, extra = "") {
  return `<button type="button" class="icon-button ${extra}" data-${action}="${escape(id)}" title="${escape(label)}" aria-label="${escape(label)}">${icon}</button>`;
}

function modelAction(model: Model, status?: ModelStatus) {
  if (status?.downloading) {
    return `<span class="model-progress">Downloading ${status.progress}%</span>`;
  }
  if (status?.installed) {
    return iconButton("delete", model.id, "Remove model", ICON_TRASH, "danger");
  }
  return iconButton("download", model.id, "Download model", ICON_DOWNLOAD, "brand");
}

function modelCards() {
  const list = filteredModels();
  if (!list.length) {
    return `<div class="empty-history">No models match. Clear the search or a filter.</div>`;
  }
  return list.map(model => {
    const status = statuses[model.id];
    const failed = !status?.installed && !status?.downloading && !!status?.message
      && status.message !== "Installed";
    const state = status?.downloading
      ? `Downloading ${status.progress}%`
      : status?.installed
        ? status.bytesOnDisk
          ? `Downloaded · ${formatBytes(status.bytesOnDisk)}`
          : "Downloaded"
        : failed
          ? `Failed: ${status?.message}`
          : "Not on this PC";
    const isSelected = model.id === settings.selectedModel;
    const recommended = recommendation?.modelId === model.id;
    return `<article class="model-card compact ${isSelected ? "selected" : ""}" data-model="${model.id}">
      <label class="model-activate">
        <input type="checkbox" data-activate="${model.id}" ${isSelected ? "checked" : ""} />
        <span class="model-check" aria-hidden="true">${isSelected ? ICON_CHECK : ""}</span>
      </label>
      <div class="model-copy">
        <b>${escape(model.name)}</b>
        <span class="engine">${escape(model.engine)}${recommended ? " · Suggested start" : ""}</span>
        <ul class="model-meta">
          <li>${escape(model.size)}</li>
          <li>${escape(model.languages)}</li>
          <li class="install-state ${failed ? "failed" : ""}">${escape(state)}</li>
        </ul>
      </div>
      <div class="model-actions">${modelAction(model, status)}</div>
    </article>`;
  }).join("");
}

const SIDE_LABELS: Record<string, string> = {
  ControlRight: "Right Ctrl", ControlLeft: "Left Ctrl",
  AltRight: "Right Alt", AltLeft: "Left Alt",
  ShiftRight: "Right Shift", ShiftLeft: "Left Shift",
};

function hotkeyLabel(spec = settings.hotkey) {
  const preset = presets.find(entry => entry.id === spec);
  if (preset) return preset.label;
  return SIDE_LABELS[spec] ?? (spec || "your hotkey");
}

function hotkeyOptions() {
  if (recordingHotkey === "hotkey") {
    return `<option value="${escape(settings.hotkey)}" selected disabled>Press shortcut keys… (e.g. Ctrl+Space)</option>`;
  }
  const known = new Set(presets.map(preset => preset.id));
  const options = presets.map(preset => `<option value="${escape(preset.id)}" ${settings.hotkey === preset.id ? "selected" : ""}>${escape(preset.label)}</option>`).join("");
  const custom = known.has(settings.hotkey) ? "" : `<option value="${escape(settings.hotkey)}" selected>${escape(hotkeyLabel(settings.hotkey))}</option>`;
  return options + custom;
}

function soundThemeOptions() {
  const selectedTheme = SOUND_THEMES.some(([id]) => id === settings.soundTheme)
    ? settings.soundTheme
    : "voca";
  return SOUND_THEMES.map(([id, label]) =>
    `<option value="${id}" ${id === selectedTheme ? "selected" : ""}>${label}</option>`
  ).join("");
}

function deviceOptions() {
  const options = [`<option value="" ${settings.inputDevice === "" ? "selected" : ""}>Default microphone</option>`];
  for (const device of devices) {
    options.push(`<option value="${escape(device.name)}" ${settings.inputDevice === device.name ? "selected" : ""}>${escape(device.name)}${device.isDefault ? " (default)" : ""}</option>`);
  }
  if (settings.inputDevice && !devices.some(device => device.name === settings.inputDevice)) {
    options.push(`<option value="${escape(settings.inputDevice)}" selected>${escape(settings.inputDevice)} (saved)</option>`);
  }
  return options.join("");
}

function languageOptions() {
  const selectedLanguage = settings.language || "Auto-detect";
  const extras = selectedLanguage && !LANGUAGE_CHOICES.includes(selectedLanguage)
    ? [[selectedLanguage, selectedLanguage] as [string, string]]
    : [];
  return extras.concat(LANGUAGE_CHOICES.map(language => [language, language]));
}

function languageControl() {
  const current = settings.language || "Auto-detect";
  return `<div class="language-picker">${selectControl("language", languageOptions(), current)}</div>`;
}

function dictateMark(kind: "idle" | "listening") {
  return kind === "listening" ? dictateListening : dictateIdle;
}

function dictationParkBanner() {
  if (recording) return "";
  if (!runtime.parkKind || !runtime.parkDetail) return "";
  const label = runtime.parkKind === "idle" ? "Model unloaded" : "Dictation paused";
  return `<div class="park-banner ${runtime.parkKind}"><strong>${label}</strong><p>${escape(runtime.parkDetail)}</p></div>`;
}

function dictationPage() {
  const model = selected();
  const installed = modelInstalled();
  const holdMode = settings.activationMode !== "toggle";
  const keycap = `<kbd>${escape(hotkeyLabel())}</kbd>`;
  const parked = !!runtime.parkKind;
  const heading = recording
    ? (testListening ? "Practice take" : "Listening…")
    : runtime.parkKind === "autopause"
      ? "Paused for a watched app"
      : runtime.parkKind === "idle"
        ? "Model unloaded to save RAM"
        : "Ready when you are";
  const statusHint = recording
    ? (testListening
      ? "This take stays inside Lilt. Stop it from the sidebar Test control."
      : `Speak now. Text lands at the caret when you finish.${settings.escapeCancels ? " Esc cancels." : ""}`)
    : "Text lands at the caret in the focused app.";
  const hotkeyLabelText = recording
    ? (testListening ? "PRACTICE" : "LISTENING")
    : (holdMode ? "HOLD TO TALK" : "TAP TO TOGGLE");
  const hotkeyHint = recording
    ? (testListening
      ? "Use Stop test in the sidebar. This take does not type into other apps."
      : holdMode
        ? "Release it when you finish speaking."
        : "Tap it again to stop and type.")
    : (holdMode
      ? "Hold it in any app."
      : "Tap it in any app to start, tap again to finish.");
  // Escape hatch for hotkey/window takes only. Sidebar Test uses its own Stop
  // and must not call inject_text via #record.
  const finishControl = recording && !testListening
    ? `<button type="button" class="quiet-button" id="record" ${runtime.paused ? "disabled" : ""}>Stop &amp; type</button>`
    : "";
  const modelState = installed
    ? `${escape(model?.engine ?? "")} · ${escape(model?.languages ?? "")} · On this PC`
    : "Not on this PC yet. Open Models to download it.";
  const modelAction = installed ? "Change model" : "Download this model";
  const stateLabel = recording ? "Listening" : parked ? (runtime.parkKind === "idle" ? "Unloaded" : "Paused") : "Ready";
  return `<header><div><p class="overline">VOICE DICTATION</p><h1>Speak naturally.<br><em>Keep it private.</em></h1><p class="lede">Lilt turns your voice into text on your own computer, with uncompromising speed and privacy.</p></div><span class="state ${parked ? "parked" : ""}"><i></i>${stateLabel}</span></header>
  ${dictationParkBanner()}
  <div class="dictation-bento">
    <section class="record-panel" aria-live="polite">
      <div class="pill-hero-wrapper">
        <button type="button" id="hero-dictate-btn" class="glowing-pill-btn ${recording ? "listening" : ""}" title="${recording ? "Click to stop dictation" : "Click to dictate"}">
          <canvas class="pill-wave-canvas" width="100" height="30"></canvas>
        </button>
      </div>
      <div class="record-copy">
        <h2>${heading}</h2>
        <p class="record-hint">${statusHint}</p>
      </div>
    </section>
    <section class="hotkey-panel">
      <p class="card-label">${hotkeyLabelText}</p>
      <p class="hotkey-hero">${keycap}</p>
      <p class="record-hint">${hotkeyHint}</p>
      <p class="record-actions"><button type="button" class="text-button" data-go="settings">Change shortcut</button>${finishControl ? ` · ${finishControl}` : ""}</p>
      <small>Practice anytime via sidebar Test. That take stays inside Lilt.</small>
    </section>
  </div>
  <section class="overview"><button class="info-card model-cta ${installed ? "" : "needs-download"}" data-go="models" type="button"><p class="card-label">ACTIVE MODEL</p><strong>${escape(model?.name ?? "Choose a model")}</strong><span>${modelState}</span><span class="text-button">${modelAction}</span></button></section>`;
}

function filterLabel<T extends string>(options: Array<[T, string]>, value: T) {
  return options.find(([id]) => id === value)?.[1] ?? options[0][1];
}

function filterDatalist(id: string, options: Array<[string, string]>) {
  return `<datalist id="${id}">${options.map(([, label]) => `<option value="${escape(label)}"></option>`).join("")}</datalist>`;
}

function modelsPage() {
  const tip = recommendation
    ? `<p class="hw-tip"><strong>Starting size:</strong> ${escape(recommendation.modelName)}. ${escape(recommendation.reason)}</p>`
    : "";
  const header = `<header><div><p class="overline">ON-DEVICE MODELS</p><h1>Choose your <em>engine.</em></h1><p class="lede">Models stay on your PC. Pick the trade-off between speed, accuracy, and language coverage.</p></div></header>`;
  if (searchQuery()) return `${header}${settingsCards("models")}`;
  return `${header}
  ${tip}
  ${settingsCards("models")}
  <div class="model-filters">
    <input id="model-search" type="search" placeholder="Search models" value="${escape(modelQuery)}" />
    <label class="filter-combo"><span class="vh">Engine</span>
      ${selectControl("engine-filter", ENGINE_FILTERS, engineFilter)}
    </label>
    <label class="filter-combo"><span class="vh">Language</span>
      ${selectControl("language-filter", LANGUAGE_FILTERS, languageFilter)}
    </label>
  </div>
  <div class="model-grid compact">${modelCards()}</div>`;
}

function chipLabel(name: string) {
  return runningApps.find(app => app.name.toLowerCase() === name.toLowerCase())?.label
    ?? name.replace(/\.exe$/i, "");
}

function appPickerOptions(chosen: string[]) {
  const taken = new Set(chosen.map(name => name.toLowerCase()));
  const options = runningApps
    .filter(app => !taken.has(app.name.toLowerCase()))
    .map(app => `<option value="${escape(app.name)}">${escape(app.label)}</option>`);
  if (!options.length) {
    return `<option value="">No other running apps right now</option>`;
  }
  return `<option value="">Add a running app</option>${options.join("")}`;
}

function runningAppOptions() {
  return appPickerOptions(watchedApps());
}

function appChips(apps: string[], action: string) {
  if (!apps.length) return "";
  return `<ul class="app-chips">${apps.map(name => `<li class="app-chip"><span>${escape(chipLabel(name))}</span><button type="button" data-${action}="${escape(name)}" title="Remove ${escape(chipLabel(name))}" aria-label="Remove ${escape(chipLabel(name))}">×</button></li>`).join("")}</ul>`;
}

function watchedAppChips() {
  return appChips(watchedApps(), "unwatch");
}

function pasteApps() {
  return (settings.pasteApps ?? "")
    .split(/[\n,;]+/)
    .map(part => part.trim())
    .filter(Boolean);
}

function idleUnloadValue() {
  if (!settings.idleUnloadEnabled) return 0;
  const seconds = settings.idleUnloadSeconds;
  return [300, 900, 1800, 3600].reduce((best, value) =>
    Math.abs(value - seconds) < Math.abs(best - seconds) ? value : best, 300);
}

function idleUnloadOptions() {
  const current = idleUnloadValue();
  return IDLE_PRESETS.map(([value, label]) =>
    `<option value="${value}" ${value === current ? "selected" : ""}>${label}</option>`
  ).join("");
}

function previewSoundControl() {
  const label = previewStartNext ? "Preview start" : "Preview end";
  const icon = previewStartNext ? ICON_PLAY : ICON_STOP;
  const off = settings.soundTheme === "off";
  return `<button type="button" class="quiet-button preview-sound" id="preview-sound" ${off ? "disabled " : ""}aria-label="${label}" title="${label}">${icon}</button>`;
}

function switchControl(id: string, checked: boolean, disabled = false) {
  return `<label class="switch"><input id="${id}" type="checkbox" ${checked ? "checked" : ""} ${disabled ? "disabled" : ""}/><span></span></label>`;
}

function selectControl(id: string, options: Array<[string, string]>, current: string) {
  return `<select id="${id}" class="themed-select">${options.map(([value, label]) => `<option value="${escape(value)}" ${value === current ? "selected" : ""}>${escape(label)}</option>`).join("")}</select>`;
}

/** Hands-free and paste-last: presets, plus Record for any combo. */
function extraShortcutControl(target: CaptureTarget) {
  const current = settings[target] as string;
  const isRecording = recordingHotkey === target;
  const options: Array<[string, string]> = isRecording
    ? [[current, "Press shortcut keys… (e.g. Ctrl+Space)"]]
    : EXTRA_SHORTCUTS.some(([value]) => value === current)
      ? EXTRA_SHORTCUTS
      : [...EXTRA_SHORTCUTS, [current, hotkeyLabel(current)] as [string, string]];
  const id = target === "handsFreeHotkey" ? "hands-free-hotkey" : "paste-last-hotkey";
  return `<div class="hotkey-controls ${isRecording ? "recording" : ""}">${selectControl(id, options, current)}
    <button type="button" class="quiet-button ${isRecording ? "recording-btn" : ""}" data-capture="${target}">${isRecording ? "Cancel" : "Record"}</button></div>`;
}

function replacementsEditor() {
  const rows = settings.replacements.map((entry, index) => `<li class="pair-row">
      <span class="pair-from">${escape(entry.heard)}</span><span class="pair-arrow" aria-hidden="true">→</span><span class="pair-to">${escape(entry.replacement)}</span>
      <button type="button" class="pair-remove" data-remove-replacement="${index}" title="Remove" aria-label="Remove ${escape(entry.heard)}">×</button>
    </li>`).join("");
  return `<div class="pair-editor">
    ${rows ? `<ul class="pair-list">${rows}</ul>` : `<p class="pair-empty">No replacements yet.</p>`}
    <div class="pair-form">
      <input id="replacement-heard" class="draft" type="text" placeholder="Heard, e.g. get hub" autocomplete="off" />
      <input id="replacement-to" class="draft" type="text" placeholder="Type instead, e.g. GitHub" autocomplete="off" />
      <button type="button" class="quiet-button" id="add-replacement">Add</button>
    </div>
  </div>`;
}

function snippetsEditor() {
  const rows = settings.snippets.map((entry, index) => `<li class="pair-row snippet">
      <span class="pair-from">${escape(entry.trigger)}</span><span class="pair-arrow" aria-hidden="true">→</span><span class="pair-to">${escape(entry.expansion)}</span>
      <button type="button" class="pair-remove" data-remove-snippet="${index}" title="Remove" aria-label="Remove ${escape(entry.trigger)}">×</button>
    </li>`).join("");
  return `<div class="pair-editor">
    ${rows ? `<ul class="pair-list">${rows}</ul>` : `<p class="pair-empty">No snippets yet.</p>`}
    <div class="pair-form snippet">
      <input id="snippet-trigger" class="draft" type="text" placeholder="Say, e.g. my address" autocomplete="off" />
      <textarea id="snippet-expansion" class="draft" rows="3" placeholder="Type, e.g. 221B Baker Street, London"></textarea>
      <button type="button" class="quiet-button" id="add-snippet">Add</button>
    </div>
  </div>`;
}

function hybridProviderLabel(provider: string): string {
  return HYBRID_PROVIDERS.find(([id]) => id === provider)?.[1] ?? "OpenAI";
}

function hybridDefaultModel(provider: string): string {
  return HYBRID_MODELS_BY_PROVIDER[provider]?.[0]?.[0] ?? "gpt-4o-mini";
}

function hybridModelControls(): string {
  const provider = settings.hybridProvider || "openai";
  const presets = HYBRID_MODELS_BY_PROVIDER[provider] ?? HYBRID_MODELS_BY_PROVIDER.openai;
  const current = settings.hybridModel || hybridDefaultModel(provider);
  const inPresets = presets.some(([id]) => id === current);
  const options: Array<[string, string]> = inPresets
    ? [...presets, ["__custom__", "Custom model ID…"]]
    : [...presets, [current, `Custom: ${current}`], ["__custom__", "Custom model ID…"]];
  return `<div class="hybrid-model-control-wrap">
    ${selectControl("hybrid-model-select", options, current)}
    <input id="hybrid-model-input" type="text" class="hybrid-model-custom-input" value="${escape(current)}" placeholder="Enter model ID, e.g. gpt-4o-mini" autocomplete="off" spellcheck="false" />
  </div>`;
}

function hybridKeyPlaceholder(provider: string): string {
  switch (provider) {
    case "groq": return "gsk_...";
    case "gemini": return "AIzaSy...";
    case "anthropic": return "sk-ant-...";
    case "custom": return "Bearer API key (optional for local Ollama)";
    default: return "sk-proj-...";
  }
}

function hybridApiKeyControls(): string {
  const hasKey = !!(settings.hybridApiKey && settings.hybridApiKey.trim());
  const statusBadge = hasKey
    ? `<span class="hybrid-key-badge configured">Key saved locally</span>`
    : `<span class="hybrid-key-badge missing">No key set</span>`;
  const testResultBanner = hybridTestStatus
    ? `<div class="hybrid-test-banner ${hybridTestStatus.ok ? "ok" : "err"}">${escape(hybridTestStatus.text)}</div>`
    : "";
  return `<div class="hybrid-key-editor">
    <div class="hybrid-key-row">
      <input
        id="hybrid-api-key"
        type="${hybridShowKey ? "text" : "password"}"
        value="${escape(settings.hybridApiKey ?? "")}"
        placeholder="${escape(hybridKeyPlaceholder(settings.hybridProvider))}"
        autocomplete="off"
        spellcheck="false"
      />
      <button type="button" class="quiet-button" id="hybrid-toggle-key">${hybridShowKey ? "Hide" : "Show"}</button>
      <button type="button" class="quiet-button" id="hybrid-test-btn" ${hybridTesting ? "disabled" : ""}>${hybridTesting ? "Testing…" : "Test connection"}</button>
    </div>
    <div class="hybrid-key-meta">
      ${statusBadge}
      <span class="hybrid-key-note">Stored only on this PC and excluded from exported settings backups.</span>
    </div>
    ${testResultBanner}
  </div>`;
}

function settingsItems(): SettingsItem[] {
  const levelPct = Math.min(100, Math.round(micLevel * 140));
  return [
    {
      page: "shortcuts",
      card: "Dictation key",
      title: "Activation hotkey",
      subtitle: "Pick a preset or press Record. New installs default to Right Alt (Option) for effortless one-thumb activation. AltGr (Ctrl+Right Alt) is not consumed, so layout characters still type. The live listener pauses while recording.",
      keywords: "hotkey shortcut keyboard record preset right alt altright",
      html: `<div class="hotkey-controls ${recordingHotkey === "hotkey" ? "recording" : ""}"><select id="hotkey-preset" class="themed-select">${hotkeyOptions()}</select>
    <button type="button" class="quiet-button ${recordingHotkey === "hotkey" ? "recording-btn" : ""}" data-capture="hotkey">${recordingHotkey === "hotkey" ? "Cancel" : "Record"}</button></div>`,
    },
    {
      page: "shortcuts",
      card: "Dictation key",
      title: "Activation style",
      subtitle: "Hold to talk, or tap to start and tap again to stop. Toggle also stops after the silence set on the Audio page.",
      keywords: "push to talk toggle mode tap",
      html: `<select id="activation" class="themed-select"><option value="pushToTalk">Push to talk</option><option value="toggle">Toggle</option></select>`,
    },
    {
      page: "shortcuts",
      card: "More ways to dictate",
      title: "Hands-free shortcut",
      subtitle: "Press once to start and again to stop, without holding a key. Silence does not end it; Max recording still does.",
      keywords: "hands free handsfree toggle shortcut start stop long dictation",
      html: extraShortcutControl("handsFreeHotkey"),
    },
    {
      page: "shortcuts",
      card: "More ways to dictate",
      title: "Mouse button",
      subtitle: "Dictate with the middle or a side mouse button, held or tapped like the hotkey. That button's normal click goes to Lilt while this is on.",
      keywords: "mouse button middle side back forward xbutton",
      html: selectControl("mouse-button", MOUSE_BUTTONS, settings.mouseButton),
    },
    {
      page: "shortcuts",
      card: "More ways to dictate",
      title: "Escape cancels dictation",
      subtitle: "Escape throws a take away while it records, or keeps a transcription from being typed. At other times Escape reaches your apps as usual.",
      keywords: "escape esc cancel discard throw away",
      html: switchControl("escape-cancels", settings.escapeCancels),
    },
    {
      page: "shortcuts",
      card: "More ways to dictate",
      title: "Paste last dictation",
      subtitle: "Types your last dictation again at the caret, even with history off. Ctrl+Alt shortcuts can clash with AltGr keyboard layouts.",
      keywords: "paste last again repeat retype shortcut",
      html: extraShortcutControl("pasteLastHotkey"),
    },
    {
      page: "models",
      card: "Language",
      title: "Dictation language",
      subtitle: "One list. Auto-detect is first, then English, then A to Z.",
      keywords: "language locale english auto detect",
      html: languageControl(),
    },
    {
      page: "audio",
      card: "Microphone",
      title: "Microphone",
      subtitle: "WASAPI capture device used for dictation.",
      keywords: "mic microphone device wasapi input",
      html: `<select id="input-device" class="themed-select">${deviceOptions()}</select>`,
    },
    {
      page: "audio",
      card: "Microphone",
      title: "Mic Test",
      subtitle: "Level meter only. Does not recognize or inject text.",
      keywords: "mic test level meter volume",
      html: `<div class="mic-test"><button type="button" class="quiet-button" id="mic-test" ${recording || testListening ? "disabled" : ""}>${micTesting ? "Stop Mic Test" : "Mic Test"}</button>
      <div class="glowing-pill-btn ${micTesting ? "listening" : ""}" style="display:inline-flex;">
        <canvas class="pill-wave-canvas" width="100" height="30"></canvas>
      </div></div>`,
    },
    {
      page: "audio",
      card: "Recording",
      title: "Silence auto-stop",
      subtitle: "Seconds of quiet before a toggled take ends. Push-to-talk and the hands-free shortcut ignore this.",
      keywords: "vad silence timeout toggle",
      html: `<input id="silence" type="number" min="0.3" max="10" step="0.1" value="${settings.silenceSeconds}" />`,
    },
    {
      page: "audio",
      card: "Recording",
      title: "Max recording",
      subtitle: "Hard stop so a stuck session cannot run forever.",
      keywords: "duration limit max",
      html: `<input id="max-recording" type="number" min="3" max="300" step="1" value="${settings.maxRecordingSeconds}" />`,
    },
    {
      page: "audio",
      card: "Recording",
      title: "Skip silence before transcribing",
      subtitle: "Cuts quiet stretches so the model only hears speech. Faster, and Whisper has no silence to invent words over.",
      keywords: "silence skip trim vad quiet hallucination",
      html: switchControl("skip-silence", settings.skipSilence),
    },
    {
      page: "audio",
      card: "Recording",
      title: "Mute other audio while dictating",
      subtitle: "Mutes apps that are playing sound while you record, then unmutes exactly those. Apps you muted yourself stay muted.",
      keywords: "mute duck music video audio other apps sound",
      html: switchControl("mute-other-audio", settings.muteOtherAudio),
    },
    {
      page: "audio",
      card: "Sounds",
      title: "Dictation sounds",
      subtitle: "These play when listening starts and stops. Preview is two clicks: start tone, then end tone.",
      keywords: "sound beep audio cue",
      html: `<div class="sound-theme-controls"><select id="sound-theme" class="themed-select">${soundThemeOptions()}</select>
      ${previewSoundControl()}</div>`,
    },
    {
      page: "hybrid",
      card: "Hybrid AI Mode",
      title: "Enable Hybrid AI Polish",
      subtitle: "Audio is always recorded and transcribed locally on your PC first. When Hybrid Mode is on, the transcript is sent to your chosen AI model to polish grammar, phrasing, and formatting before typing. Falls back to local rules automatically if offline.",
      keywords: "hybrid ai polish llm cloud openai groq gemini anthropic claude byok",
      html: switchControl("hybrid-enabled", settings.hybridEnabled),
    },
    {
      page: "hybrid",
      card: "Bring Your Own Key (BYOK)",
      title: "AI Provider",
      subtitle: "Choose OpenAI, Groq (ultra-low latency), Google Gemini, Anthropic Claude, or any OpenAI-compatible endpoint.",
      keywords: "provider openai groq gemini anthropic claude openrouter ollama custom",
      html: selectControl("hybrid-provider", HYBRID_PROVIDERS, settings.hybridProvider || "openai"),
    },
    {
      page: "hybrid",
      card: "Bring Your Own Key (BYOK)",
      wide: true,
      title: "API Key",
      subtitle: "Bring your own key from your provider dashboard. Lilt calls the provider directly from your PC—no middleman backend.",
      keywords: "api key secret token byok bring your own key openai groq gemini anthropic",
      html: hybridApiKeyControls(),
    },
    {
      page: "hybrid",
      card: "Bring Your Own Key (BYOK)",
      wide: true,
      title: "Model",
      subtitle: "Pick a fast recommended model or enter any model ID supported by your provider.",
      keywords: "model gpt-4o-mini gpt-4.1 llama-3.3 gemini-2.5-flash claude haiku sonnet",
      html: hybridModelControls(),
    },
    ...(settings.hybridProvider === "custom" ? [{
      page: "hybrid" as View,
      card: "Bring Your Own Key (BYOK)",
      wide: true,
      title: "Custom Base URL",
      subtitle: "OpenAI-compatible base URL (e.g. https://openrouter.ai/api/v1 or http://localhost:11434/v1 for local Ollama).",
      keywords: "base url endpoint custom openrouter ollama lm studio",
      html: `<input id="hybrid-base-url" type="text" value="${escape(settings.hybridBaseUrl ?? "")}" placeholder="https://api.openai.com/v1" autocomplete="off" spellcheck="false" />`,
    }] : []),
    {
      page: "hybrid",
      card: "Formatting & Tone",
      wide: true,
      title: "Writing style preset",
      subtitle: "Controls how the AI formats and refines your dictated speech. Smart Polish preserves your natural voice and Hinglish while fixing grammar and punctuation.",
      keywords: "style tone formatting smart polish professional concise bullet points prompt",
      html: selectControl("hybrid-style", HYBRID_STYLES, settings.hybridStyle || "smart_polish"),
    },
    {
      page: "hybrid",
      card: "Formatting & Tone",
      wide: true,
      title: "Custom instructions (optional)",
      subtitle: "Add personal rules for the AI formatter—for example: “Keep Hinglish words in Roman script, use casual tone, never use em-dashes.”",
      keywords: "custom prompt instructions system rules hinglish tone",
      html: `<textarea id="hybrid-custom-prompt" rows="3" placeholder="e.g. Keep Hinglish words in Roman script. Keep tone warm and concise.">${escape(settings.hybridCustomPrompt ?? "")}</textarea>`,
    },
    {
      page: "formatting",
      card: "Cleanup",
      title: "Cleanup",
      subtitle: "Medium removes “um” and “uh”, a letter said three times (“I I I”), and keeps only the fix when you correct a day, month, number, or time (“tomorrow, no, Wednesday” → “Wednesday”). None types every word as heard.",
      keywords: "cleanup filler um uh hesitation stutter correction",
      html: selectControl("cleanup-level", [["medium", "Medium"], ["none", "None"]], settings.cleanupLevel),
    },
    {
      page: "formatting",
      card: "Formatting",
      title: "Auto-capitalize",
      subtitle: "Capitalize the start of sentences.",
      keywords: "capitalize formatting output",
      html: switchControl("auto-cap", settings.autoCapitalize),
    },
    {
      page: "formatting",
      card: "Formatting",
      title: "Trailing space",
      subtitle: "Append a space after each utterance.",
      keywords: "space formatting output",
      html: switchControl("trailing-space", settings.appendTrailingSpace),
    },
    {
      page: "formatting",
      card: "Spoken forms",
      title: "Write numbers as digits",
      subtitle: "“twenty three” becomes “23”, “seven thirty pm” becomes “7:30 pm”, “my number is nine eight seven…” becomes digits. English only. “High five” and “no one” keep their words.",
      keywords: "numbers digits spoken number phone time year",
      html: switchControl("numbers-as-digits", settings.numbersAsDigits),
    },
    {
      page: "formatting",
      card: "Spoken forms",
      title: "Use symbols and ordinals",
      subtitle: "With digits on: “fifty percent” → “50%”, “five dollars and fifty cents” → “$5.50”, “the twenty first” → “21st”, “June twenty second” → “June 22”.",
      keywords: "percent dollar currency ordinal date symbols",
      html: switchControl("number-symbols", settings.numberSymbols, !settings.numbersAsDigits),
    },
    {
      page: "formatting",
      card: "Spoken forms",
      title: "Spoken emoji",
      subtitle: "Say “party emoji” for 🎉 or “three fire emojis” for 🔥🔥🔥. Talking about one (“send a fire emoji”) keeps the words.",
      keywords: "emoji emoticon smiley",
      html: switchControl("spoken-emoji", settings.spokenEmoji),
    },
    {
      page: "formatting",
      card: "Output",
      title: "How text goes in",
      subtitle: "Typing works in almost every app and leaves your clipboard alone. Paste is faster for long text and suits apps that drop typed characters; your clipboard is put back afterwards.",
      keywords: "type paste insert injection sendinput clipboard method",
      html: selectControl("insertion-mode", [["type", "Type at the caret"], ["paste", "Paste"]], settings.insertionMode),
    },
    {
      page: "formatting",
      card: "Output",
      title: "Always paste in these apps",
      subtitle: "For an app where typed text comes out wrong or incomplete. Notepad and WordPad always paste.",
      keywords: "paste apps dropped characters remote desktop terminal electron",
      html: `<select id="paste-app" class="themed-select power-combo">${appPickerOptions(pasteApps())}</select>`,
      after: `<div class="power-chips"><div id="paste-app-chips">${appChips(pasteApps(), "unpaste")}</div><p class="power-note">Empty list means type everywhere (unless Paste is chosen above).</p></div>`,
    },
    {
      page: "formatting",
      card: "Output",
      title: "Copy to clipboard",
      subtitle: "Leave recognized text on the clipboard after each take. Off by default so dictation does not replace what you already copied.",
      keywords: "clipboard paste preserve copy output",
      html: switchControl("copy-to-clipboard", settings.copyToClipboard),
    },
    {
      page: "dictionary",
      card: "Vocabulary",
      wide: true,
      title: "Vocabulary",
      subtitle: "Names and jargon spelled your way with every model: “lilt ai” becomes “Lilt AI”. Whisper also gets them as a recognition hint. One per line, or comma-separated.",
      keywords: "custom vocabulary dictionary glossary names jargon initial prompt whisper spelling",
      html: `<textarea id="custom-vocabulary" rows="5" placeholder="Lilt, Anthropic, kubectl, PostgreSQL, Grafana">${escape(settings.customVocabulary)}</textarea>`,
    },
    {
      page: "dictionary",
      card: "Replacements",
      wide: true,
      title: "Replacements",
      subtitle: "Type something else when a model gets a word wrong: “get hub” → “GitHub”. Separate several spoken forms with commas. Matching ignores case.",
      keywords: "replacements replace correct misheard dictionary",
      html: replacementsEditor(),
    },
    {
      page: "snippets",
      card: "Snippets",
      wide: true,
      title: "Custom snippets",
      subtitle: "Say a trigger and Lilt types your saved text exactly as written, never re-cased: “my address” → your full address.",
      keywords: "snippets trigger expansion text shortcut template",
      html: snippetsEditor(),
    },
    {
      page: "history",
      card: "History settings",
      title: "Keep dictation history",
      subtitle: "When this is off, new takes are not added to History. Older entries stay until they expire or you clear them.",
      keywords: "history transcript save local",
      html: switchControl("history-enabled", settings.historyEnabled),
    },
    {
      page: "history",
      card: "History settings",
      title: "Keep history for",
      subtitle: "Older dictations and their audio are deleted from this PC.",
      keywords: "history retention delete days forever expire",
      html: selectControl("history-retention", RETENTION_CHOICES.map(([value, label]) => [String(value), label]), String(settings.historyRetentionDays)),
    },
    {
      page: "history",
      card: "History settings",
      title: "History storage content",
      subtitle: "Choose whether to store both audio and text, or text only. Text only uses zero audio disk space and ensures maximum voice privacy.",
      keywords: "history audio replay retry recording save crash storage privacy text only",
      html: selectControl(
        "history-storage-mode",
        [
          ["audio_and_text", "Audio and text (Default)"],
          ["text_only", "Text only (No audio saved)"],
        ],
        settings.historyKeepAudio ? "audio_and_text" : "text_only"
      ),
    },
    {
      page: "settings",
      card: "Startup",
      title: "Launch at login",
      subtitle: "Start Lilt automatically with Windows (starts minimized to tray).",
      keywords: "startup autostart login",
      html: switchControl("launch-login", settings.launchAtLogin),
    },
    {
      page: "settings",
      card: "Startup",
      title: "Ready pill at launch",
      subtitle: "Displays the floating Lilt status pill on screen at launch, since Windows often hides new tray icons.",
      keywords: "ready pill startup notification started running launch indicator",
      html: switchControl("ready-pill", settings.readyPill),
    },
    {
      page: "settings",
      card: "Recording overlay",
      title: "Recording overlay",
      subtitle: "Button-sized pill with glowing borders that stays straight when quiet and flows into waves while you speak.",
      keywords: "overlay pill indicator hud waveform recording",
      html: `<div style="display:flex; align-items:center; gap:16px;">
        ${selectControl("overlay-style", [["minimal", "Minimal pill"], ["off", "Off"]], settings.overlayStyle)}
        <div class="glowing-pill-btn listening" style="display:inline-flex;">
          <canvas class="pill-wave-canvas" width="100" height="30"></canvas>
        </div>
      </div>`,
    },
    {
      page: "settings",
      card: "Recording overlay",
      title: "Show text while you speak",
      subtitle: "The pill shows what the model hears so far, about once a second. Uses more CPU while you dictate; the typed text is decoded as before.",
      keywords: "live preview streaming partial text while speaking realtime",
      html: switchControl("live-preview", settings.livePreview),
    },
    {
      page: "settings",
      card: "Recording overlay",
      title: "Overlay position",
      subtitle: "Bottom, top, or drag the pill anywhere on your screen to place it freely.",
      keywords: "overlay position top bottom screen drag custom",
      html: selectControl("overlay-position", [["bottom", "Bottom of screen"], ["top", "Top of screen"], ["custom", "Custom (drag anywhere)"]], settings.overlayPosition),
    },
    {
      page: "settings",
      card: "Backup",
      title: "Settings backup",
      subtitle: "Export writes your preferences, dictionary, and snippets to a JSON file in Downloads. Import reads one back. No audio, history, or models.",
      keywords: "export import backup restore settings move",
      html: `<div class="button-pair"><button type="button" class="quiet-button" id="export-settings">Export</button><button type="button" class="quiet-button" id="import-settings">Import</button><input id="import-file" class="draft" type="file" accept=".json,application/json" hidden /></div>`,
    },
    {
      page: "settings",
      card: "Setup",
      title: "Setup guide",
      subtitle: "Walk through language, model, and a first dictation again.",
      keywords: "setup onboarding welcome wizard guide",
      html: `<button type="button" class="quiet-button" id="run-setup">Run setup</button>`,
    },
    {
      page: "power",
      card: "Power",
      title: "Pause while these apps are running",
      subtitle: "Lilt pauses automatically so those apps have exclusive microphone access.",
      keywords: "power pause while apps running autopause game mic",
      html: `<select id="auto-pause-app" class="themed-select power-combo">${runningAppOptions()}</select>`,
      after: `<div class="power-chips"><div id="watched-app-chips">${watchedAppChips()}</div><p class="power-note">Empty list means off. Each chip removes that app.</p></div>`,
    },
    {
      page: "power",
      card: "Power",
      title: "Unload the model after idle",
      subtitle: "Frees RAM. Next dictation loads it again.",
      keywords: "power unload model idle ram memory minutes hour",
      html: `<select id="idle-unload" class="themed-select power-combo">${idleUnloadOptions()}</select>`,
    },
  ];
}

function itemMatches(item: SettingsItem, query: string) {
  return matchScore(item, query) > 0;
}

/** 3 when every word is in the title, 2 in the title or keywords, 1 anywhere
 *  (the subtitle included), 0 for no match. Search jumps to the best page. */
function matchScore(item: SettingsItem, query: string) {
  const parts = query.split(/\s+/).filter(Boolean);
  if (!parts.length) return 1;
  const within = (text: string) => parts.every(part => text.toLowerCase().includes(part));
  if (within(item.title)) return 3;
  if (within(`${item.title} ${item.keywords}`)) return 2;
  return within(`${item.card} ${item.title} ${item.subtitle} ${item.keywords}`) ? 1 : 0;
}

function searchQuery() {
  return settingsQuery.trim().toLowerCase();
}

/** Matching settings per page while the sidebar search has text. */
function pageMatchCounts() {
  const counts = new Map<View, number>();
  const query = searchQuery();
  if (!query) return counts;
  for (const item of settingsItems()) {
    if (itemMatches(item, query)) counts.set(item.page, (counts.get(item.page) ?? 0) + 1);
  }
  return counts;
}

function settingRow(item: SettingsItem) {
  const text = `<div><strong>${escape(item.title)}</strong><p>${escape(item.subtitle)}</p></div>`;
  return `<div class="setting-row ${item.wide ? "wide" : ""}">${text}${item.html}</div>${item.after ?? ""}`;
}

/** The page's settings, grouped into cards, filtered by the sidebar search. */
function settingsCards(page: View) {
  const query = searchQuery();
  const items = settingsItems().filter(item => item.page === page && itemMatches(item, query));
  const cards: string[] = [];
  for (const card of [...new Set(items.map(item => item.card))]) {
    const rows = items.filter(item => item.card === card);
    cards.push(`<section class="settings-card" data-settings-group="${escape(card)}"><p class="settings-group">${escape(card)}</p>${rows.map(settingRow).join("")}</section>`);
  }
  return cards.join("");
}

function pageHeader(overline: string, title: string, lede: string, extra = "") {
  return `<header><div><p class="overline">${overline}</p><h1>${title}</h1><p class="lede">${lede}</p></div>${extra}</header>`;
}

function captureHint() {
  return recordingHotkey ? `<p class="recording-hint">Press a key combo such as Ctrl+Space, or one side key such as Right Alt. Escape cancels.</p>` : "";
}

function shortcutsPage() {
  return `${pageHeader("SHORTCUTS", "Dictate <em>your way.</em>", "Hold a key, tap a shortcut, or use a mouse button. Escape throws a take away.")}
  ${settingsCards("shortcuts")}${captureHint()}`;
}

function audioPage() {
  return `${pageHeader("AUDIO", "Hear you <em>clearly.</em>", "Microphone, when a take ends, and what happens to other sound while you speak.")}
  ${settingsCards("audio")}`;
}

function hybridPage() {
  const localModelName = selected()?.name ?? settings.selectedModel;
  const providerName = hybridProviderLabel(settings.hybridProvider);
  const activeModel = settings.hybridModel || hybridDefaultModel(settings.hybridProvider);
  const hasKey = !!(settings.hybridApiKey && settings.hybridApiKey.trim()) || settings.hybridProvider === "custom";
  const statusPill = settings.hybridEnabled
    ? (hasKey
      ? `<span class="hybrid-status-pill active">● Hybrid Active · ${escape(providerName)} (${escape(activeModel)})</span>`
      : `<span class="hybrid-status-pill warn">● Add API Key below to activate ${escape(providerName)}</span>`)
    : `<span class="hybrid-status-pill">○ Hybrid Off · Using Local Rules Only</span>`;

  const pipelineBanner = searchQuery() ? "" : `
    <section class="settings-card hybrid-pipeline-card">
      <div class="hybrid-pipeline-header">
        <p class="settings-group" style="margin:0;">Hybrid Architecture</p>
        ${statusPill}
      </div>
      <div class="hybrid-pipeline-steps">
        <div class="hybrid-step">
          <span class="hybrid-step-num">1</span>
          <div>
            <strong>Local Audio → Text</strong>
            <p>${escape(localModelName)} runs 100% offline on your PC. Audio never leaves your device.</p>
          </div>
        </div>
        <span class="hybrid-step-arrow">→</span>
        <div class="hybrid-step">
          <span class="hybrid-step-num">2</span>
          <div>
            <strong>Dictionary & Snippets</strong>
            <p>Your custom vocabulary (${settings.replacements.length} replacements, ${settings.snippets.length} snippets) is applied locally.</p>
          </div>
        </div>
        <span class="hybrid-step-arrow">→</span>
        <div class="hybrid-step ${settings.hybridEnabled ? "active" : ""}">
          <span class="hybrid-step-num">3</span>
          <div>
            <strong>BYOK Cloud Polish</strong>
            <p>${escape(providerName)} (${escape(activeModel)}) refines grammar, structure, and formatting with automatic local fallback.</p>
          </div>
        </div>
      </div>
    </section>
  `;

  const playgroundCard = searchQuery() ? "" : `
    <section class="settings-card try-card">
      <div class="hybrid-playground-header">
        <p class="settings-group" style="margin:0;">Live Hybrid Playground</p>
        ${hybridPreviewResult ? `<span class="hybrid-latency-badge">${hybridPreviewResult.usedHybrid ? `${escape(hybridPreviewResult.provider)} · ${escape(hybridPreviewResult.model)} · ${hybridPreviewResult.latencyMs} ms` : "Local Fallback"}</span>` : ""}
      </div>
      <div class="try-box">
        <div class="hybrid-voice-command-chips" style="display:flex; gap:6px; margin-bottom:10px; flex-wrap:wrap;">
          <button type="button" class="text-button" data-fill-command="Write the email to Alex saying the demo is postponed to Thursday at 2 PM">✉️ Email Command</button>
          <button type="button" class="text-button" data-fill-command="Write it raw honestly the server was bugging out yesterday but we fixed it">📝 Raw Command</button>
          <button type="button" class="text-button" data-fill-command="Make a list buy organic milk finish mobile build deploy to prod">📋 List Command</button>
          <button type="button" class="text-button" data-fill-command="Meeting notes team aligned on Q4 goals Shiva is leading Android launch by Friday">💼 Meeting Notes</button>
        </div>
        <textarea id="hybrid-try-input" class="draft" rows="3" placeholder="Type or dictate a raw spoken sentence to compare Local vs. Hybrid AI Polish…">${escape(hybridTryText)}</textarea>
        <div class="hybrid-try-actions">
          <button type="button" class="quiet-button" id="hybrid-try-run" ${hybridPreviewLoading ? "disabled" : ""}>${hybridPreviewLoading ? "Polishing with AI…" : "Test Hybrid Polish"}</button>
        </div>
        ${hybridPreviewResult ? `
          ${hybridPreviewResult.error ? `<div class="hybrid-test-banner err">${escape(hybridPreviewResult.error)}</div>` : ""}
          <div class="hybrid-compare-grid">
            <div class="hybrid-compare-col">
              <span class="hybrid-compare-label">STAGE 1+2 · LOCAL RULES ONLY</span>
              <p class="hybrid-compare-box">${escape(hybridPreviewResult.localText || "(empty)")}</p>
            </div>
            <div class="hybrid-compare-col polished">
              <span class="hybrid-compare-label">STAGE 3 · HYBRID AI POLISHED</span>
              <p class="hybrid-compare-box">${escape(hybridPreviewResult.polishedText || "(empty)")}</p>
            </div>
          </div>
        ` : `<p class="try-result muted">Click "Test Hybrid Polish" to compare local rule-based formatting against your configured AI model side by side.</p>`}
      </div>
    </section>
  `;

  return `${pageHeader("HYBRID AI · BYOK", "Local audio, <em>cloud polish.</em>", "Record and transcribe locally on this PC, then use your own API key and chosen LLM to polish grammar, phrasing, and formatting.")}
  ${pipelineBanner}${settingsCards("hybrid")}${playgroundCard}`;
}

function formattingPage() {
  const tryCard = searchQuery() ? "" : `<section class="settings-card try-card"><p class="settings-group">Try it</p>
    <div class="try-box">
      <textarea id="try-input" class="draft" rows="3" placeholder="Type what a model might hear, e.g. um meet me at seven thirty pm, oh no, eight pm party emoji">${escape(tryText)}</textarea>
      <button type="button" class="quiet-button" id="try-run">Try</button>
      <p class="try-result ${tryResult ? "" : "muted"}">${tryResult ? escape(tryResult) : "The result appears here, formatted with your current settings. Nothing is typed."}</p>
    </div></section>`;
  return `${pageHeader("FORMATTING", "Text that <em>reads right.</em>", "Rules that run on this PC after every take, with every model. No language model is involved.")}
  ${settingsCards("formatting")}${tryCard}`;
}

function dictionaryPage() {
  return `${pageHeader("DICTIONARY", "Words <em>your way.</em>", "Your spellings and fixes apply to every speech model, after transcription, on this PC.")}
  ${settingsCards("dictionary")}`;
}

function snippetsPage() {
  return `${pageHeader("SNIPPETS", "Say less, <em>type more.</em>", "Triggers expand into text you saved. They are matched before any other formatting.")}
  ${settingsCards("snippets")}`;
}

function historyStatus(entry: HistoryEntry) {
  switch (entry.status) {
    case "pending": return "Unfinished";
    case "failed": return "Failed";
    case "cancelled": return "Cancelled, not typed";
    default: return "";
  }
}

function historyEntryMarkup(entry: HistoryEntry) {
  const model = models.find(item => item.id === entry.modelId)?.name ?? entry.modelId;
  const seconds = entry.durationMs ? ` · ${Math.max(1, Math.round(entry.durationMs / 1000))}s` : "";
  const status = historyStatus(entry);
  const body = entry.text
    ? `<p>${escape(entry.text)}</p>`
    : `<p class="history-missing">${escape(entry.error || (entry.status === "pending" ? "This take did not finish." : "No text."))}</p>`;
  const id = String(entry.id);
  const busy = retrying.has(entry.id);
  const actions = [
    entry.text ? `<button type="button" class="text-button" data-copy-history="${id}">Copy</button>` : "",
    entry.audioFile ? `<button type="button" class="text-button" data-play-history="${id}">${playingId === entry.id ? "Stop" : "Play"}</button>` : "",
    entry.audioFile ? `<button type="button" class="text-button" data-retry-history="${id}" ${busy ? "disabled" : ""}>${busy ? "Retrying…" : "Retry"}</button>` : "",
    `<button type="button" class="text-button danger" data-delete-history="${id}">Delete</button>`,
  ].filter(Boolean).join("");
  return `<article class="history-entry ${entry.status && entry.status !== "ok" ? `status-${escape(entry.status)}` : ""}">${body}
    <footer><span>${escape(model)} · ${new Date(entry.createdAtMs).toLocaleString()}${seconds}${status ? ` · <b>${escape(status)}</b>` : ""}${entry.audioFile ? "" : " · <span style=\"opacity:0.65; font-size:11px; font-weight:500;\">Text only</span>"}</span><span class="history-actions">${actions}</span></footer></article>`;
}

function historyPage() {
  const cards = settingsCards("history");
  if (searchQuery()) {
    return `${pageHeader("LOCAL HISTORY", "Your recent <em>dictation.</em>", "History settings that match your search.")}${cards}`;
  }
  const query = historyQuery.trim().toLowerCase();
  const list = query ? history.filter(entry => entry.text.toLowerCase().includes(query)) : history;
  const entries = list.length
    ? list.map(historyEntryMarkup).join("")
    : `<div class="empty-history">${history.length
      ? `No dictation matches “${escape(historyQuery)}”.`
      : settings.historyEnabled ? "Your local transcription history will appear here." : "Nothing is saved yet. Turn history back on below if you want new takes kept on this PC."}</div>`;
  const lede = settings.historyEnabled
    ? "Stored only on this computer. Search, copy, replay, or retry a take with another model."
    : "New takes are not being saved. Older entries stay on this PC until they expire or you clear them.";
  const clear = history.length ? `<button class="quiet-button" id="clear-history">Clear history</button>` : "";
  return `${pageHeader("LOCAL HISTORY", "Your recent <em>dictation.</em>", lede, clear)}
    ${history.length ? `<div class="history-search"><input id="history-search" class="draft" type="search" placeholder="Search history" value="${escape(historyQuery)}" /></div>` : ""}
    <section class="history-list">${entries}</section>
    ${cards}`;
}

function formatMinutes(minutes: number) {
  if (minutes < 1) return "under a minute";
  if (minutes < 60) return `${Math.round(minutes)} min`;
  const hours = Math.floor(minutes / 60);
  const rest = Math.round(minutes % 60);
  return rest ? `${hours} h ${rest} min` : `${hours} h`;
}

function formatLocalYmd(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function insightsPage() {
  const summary = stats;
  const hasTakes = !!(summary && summary.dictations > 0);

  // Strictly real user telemetry — zero fake or static placeholder numbers
  const displayWpm = summary?.wordsPerMinute ?? 0;
  const totalWords = summary?.words ?? 0;
  const totalTakes = summary?.dictations ?? 0;
  const totalChars = summary?.characters ?? 0;
  const timeSavedMin = summary?.timeSavedMinutes ?? 0;
  const displayStreak = summary?.currentStreak ?? 0;
  const longestStreak = summary?.longestStreak ?? 0;

  const fixesTotal = summary?.fixesTotal ?? 0;
  const wordsCorrected = summary?.wordsCorrected ?? 0;
  const dictFixes = summary?.dictionaryFixes ?? 0;
  const hesitationsRemoved = summary?.hesitationsRemoved ?? 0;
  const stuttersCollapsed = summary?.stuttersCollapsed ?? 0;
  const uniqueWordsCount = summary?.uniqueWordsCount ?? 0;
  const avgTakeSeconds = summary?.avgTakeSeconds ?? 0;
  const avgWordsPerTake = summary?.avgWordsPerTake ?? 0;
  const audioMinutes = summary?.audioMinutes ?? 0;

  const apps = summary?.apps ?? [];
  const modelsUsed = summary?.models ?? [];
  const daysMap = summary?.days ?? {};

  // Arc Gauge (Speedometer): Radius = 60, circumference = PI * 60 = 188.495
  const wpmRatio = displayWpm > 0 ? Math.min(1, Math.max(0.05, displayWpm / 200)) : 0;
  const dashOffset = Math.round(188.5 * (1 - wpmRatio));
  const speedMultiplier = displayWpm > 0 ? `${(displayWpm / 40).toFixed(1)}×` : "—";
  const speedTopLabel = displayWpm > 0 ? "vs typing" : "Pace";

  const APP_ICON = `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="4" width="18" height="16" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/></svg>`;
  const MODEL_ICON = `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>`;

  // Real 16-week (112-day) Sunday-aligned calendar heatmap ending on the current week
  // (shifted by insightsMonthOffset * 28 days when navigating months)
  const now = new Date();
  const todayMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const todayKey = formatLocalYmd(todayMidnight);

  // Align grid end to the Saturday of the target week
  const endAnchor = new Date(todayMidnight);
  endAnchor.setDate(endAnchor.getDate() + insightsMonthOffset * 28);
  const endSaturday = new Date(endAnchor);
  endSaturday.setDate(endSaturday.getDate() + (6 - endSaturday.getDay()));

  const startSunday = new Date(endSaturday);
  startSunday.setDate(startSunday.getDate() - 111);

  // Determine which dates belong to the active current streak
  const streakDates = new Set<string>();
  if (displayStreak > 0) {
    const todayHasTake = (daysMap[todayKey]?.dictations ?? 0) > 0;
    const cursor = new Date(todayMidnight);
    if (!todayHasTake) {
      cursor.setDate(cursor.getDate() - 1);
    }
    for (let i = 0; i < displayStreak; i++) {
      streakDates.add(formatLocalYmd(cursor));
      cursor.setDate(cursor.getDate() - 1);
    }
  }

  // Build 7 rows (Sun..Sat) x 16 columns
  const cells: string[] = [];
  const SHORT_MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
  for (let row = 0; row < 7; row++) {
    for (let col = 0; col < 16; col++) {
      const offsetDays = col * 7 + row;
      const cellDate = new Date(startSunday);
      cellDate.setDate(startSunday.getDate() + offsetDays);
      const key = formatLocalYmd(cellDate);
      const dayStat = daysMap[key];
      const dayWords = dayStat?.words ?? 0;
      const dayTakes = dayStat?.dictations ?? 0;
      const isFuture = cellDate > todayMidnight;

      let level = 0;
      if (!isFuture && dayTakes > 0) {
        level = dayWords >= 500 ? 4 : dayWords >= 200 ? 3 : dayWords >= 50 ? 2 : 1;
      }
      const inStreak = streakDates.has(key);
      const classes = ["insights-cell", `level-${level}`];
      if (inStreak) classes.push("in-streak");
      const dateLabel = `${SHORT_MONTHS[cellDate.getMonth()]} ${cellDate.getDate()}, ${cellDate.getFullYear()}`;
      const tooltip = isFuture
        ? `${dateLabel}: Upcoming`
        : dayTakes > 0
          ? `${dateLabel}: ${dayWords.toLocaleString()} words (${dayTakes} ${dayTakes === 1 ? "take" : "takes"})`
          : `${dateLabel}: 0 words`;
      cells.push(`<div class="${classes.join(" ")}" data-date="${key}" title="${escape(tooltip)}"></div>`);
    }
  }

  // Compute the 4 distinct month labels across the 16-week window
  const monthLabels: string[] = [];
  for (let m = 3; m >= 0; m--) {
    const d = new Date(endAnchor.getFullYear(), endAnchor.getMonth() - m, 1);
    monthLabels.push(`${SHORT_MONTHS[d.getMonth()]} ${String(d.getFullYear()).slice(-2)}`);
  }

  const desktopUsageRows = apps.length > 0
    ? apps.slice(0, 6).map(appRow => `
        <div class="insights-usage-row">
          <span class="insights-usage-icon">${APP_ICON}</span>
          <div class="insights-usage-bar-track">
            <div class="insights-usage-bar-fill" style="width: ${Math.max(14, appRow.percent)}%">
              <span class="insights-usage-bar-label">${appRow.percent}%</span>
            </div>
          </div>
          <span class="insights-usage-text">${escape(appRow.name.toUpperCase())} · ${appRow.words.toLocaleString()} WORDS (${appRow.dictations})</span>
        </div>
      `).join("")
    : `<p class="insights-voice-note" style="margin: 12px 0;">No dictations recorded yet. Hold or tap <strong>${escape(hotkeyLabel())}</strong> in any app—or click <strong>Test dictation</strong> in the sidebar—to see live per-app usage.</p>`;

  const modelUsageRows = modelsUsed.length > 0
    ? modelsUsed.map(modelRow => {
        const readableName = models.find(m => m.id === modelRow.name)?.name ?? modelRow.name;
        return `
          <div class="insights-usage-row">
            <span class="insights-usage-icon">${MODEL_ICON}</span>
            <div class="insights-usage-bar-track">
              <div class="insights-usage-bar-fill" style="width: ${Math.max(14, modelRow.percent)}%">
                <span class="insights-usage-bar-label">${modelRow.percent}%</span>
              </div>
            </div>
            <span class="insights-usage-text">${escape(readableName.toUpperCase())} · ${modelRow.words.toLocaleString()} WORDS (${modelRow.dictations})</span>
          </div>
        `;
      }).join("")
    : `<p class="insights-voice-note" style="margin: 12px 0;">No speech models used yet. Active model: <strong>${escape(selected()?.name ?? settings.selectedModel)}</strong>.</p>`;

  const vocabTermsCount = settings.customVocabulary
    .split(/[\n,]+/)
    .map(s => s.trim())
    .filter(Boolean).length;
  const activeRulesCount = settings.replacements.length + settings.snippets.length + vocabTermsCount;

  const usageTabContent = `
    <div class="insights-bento-grid">
      <!-- Card 1: WPM Gauge -->
      <div class="insights-card insights-card-wpm">
        <div class="insights-card-num">${displayWpm}</div>
        <div class="insights-card-label">
          WORDS PER MINUTE
          <span class="insights-info-icon" title="Live speaking pace measured across your recorded dictations">ⓘ</span>
        </div>
        <div class="wpm-gauge-container">
          <svg class="wpm-gauge-svg" viewBox="0 0 160 90">
            <path d="M 20 80 A 60 60 0 0 1 140 80" fill="none" stroke="#222226" stroke-width="12" stroke-linecap="round"/>
            <path d="M 20 80 A 60 60 0 0 1 140 80" fill="none" stroke="#ffffff" stroke-width="12" stroke-linecap="round"
                  stroke-dasharray="188.5" stroke-dashoffset="${dashOffset}" style="filter: drop-shadow(0 0 6px rgba(255,255,255,0.45));"/>
          </svg>
          <div class="wpm-gauge-center">
            <span class="wpm-gauge-top">${speedTopLabel}</span>
            <span class="wpm-gauge-percentile">${speedMultiplier}</span>
          </div>
        </div>
      </div>

      <!-- Card 2: Fixes -->
      <div class="insights-card insights-card-fixes">
        <div class="insights-card-num">${fixesTotal.toLocaleString()}</div>
        <div class="insights-card-label">FIXES MADE BY LILT</div>
        <div class="insights-card-divider"></div>
        <div class="insights-fixes-list">
          <div class="insights-fix-row">
            <span>${wordsCorrected.toLocaleString()} words corrected</span>
            <span class="insights-info-icon" title="Filler removals, stutter cleanups, spoken numbers/emojis, and sentence capitalizations applied">ⓘ</span>
          </div>
          <div class="insights-fix-row">
            <span>${dictFixes.toLocaleString()} dictionary fixes</span>
            <span class="insights-info-icon" title="Custom vocabulary spellings, replacements, and snippet expansions matched">ⓘ</span>
          </div>
        </div>
      </div>

      <!-- Card 3: Words Dictated -->
      <div class="insights-card insights-card-words">
        <div class="insights-card-num">${totalWords.toLocaleString()}</div>
        <div class="insights-card-label">TOTAL WORDS DICTATED</div>
        <div class="insights-card-divider"></div>
        <div class="insights-words-footer">
          <div class="insights-platform-stat">
            <div class="insights-desktop-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="3" width="20" height="14" rx="2"/>
                <line x1="8" y1="21" x2="16" y2="21"/>
                <line x1="12" y1="17" x2="12" y2="21"/>
              </svg>
            </div>
            <div class="insights-platform-meta">
              <span class="insights-platform-name">${totalTakes.toLocaleString()} ${totalTakes === 1 ? "take" : "takes"} · ${totalChars.toLocaleString()} chars</span>
              <span class="insights-platform-count">${(summary?.wordsToday ?? 0).toLocaleString()} words today</span>
            </div>
          </div>
          <span class="insights-badge-pill">SAVED | ${timeSavedMin > 0 ? formatMinutes(timeSavedMin).toUpperCase() : "0 MIN"}</span>
        </div>
      </div>

      <!-- Card 4: Desktop Usage Breakdown -->
      <div class="insights-card insights-card-desktop">
        <div class="insights-card-top-header">
          <h3 class="insights-card-heading">Desktop usage</h3>
          <span class="insights-badge-pill">TOTAL APPS USED | ${apps.length}</span>
        </div>
        <div class="insights-usage-list">
          ${desktopUsageRows}
        </div>
      </div>

      <!-- Card 5: Streak & Calendar Heatmap -->
      <div class="insights-card insights-card-streak">
        <div class="insights-card-top-header">
          <h3 class="insights-card-heading">${displayStreak} day streak</h3>
          <span class="insights-badge-pill">LONGEST STREAK | ${longestStreak} ${longestStreak === 1 ? "DAY" : "DAYS"}</span>
        </div>
        <div class="insights-months-nav">
          <button type="button" class="insights-month-chevron" id="streak-prev" title="Previous 4 weeks">‹</button>
          <div class="insights-months-list">
            ${monthLabels.map((m, idx) => `<span class="insights-month-label ${idx === 3 ? "active" : ""}">${escape(m)}</span>`).join("")}
          </div>
          <button type="button" class="insights-month-chevron" id="streak-next" title="Next 4 weeks" ${insightsMonthOffset >= 0 ? "disabled" : ""}>›</button>
        </div>
        <div class="insights-heatmap-wrapper">
          <div class="insights-heatmap-weekdays">
            <span>Sun</span>
            <span>Mon</span>
            <span>Tue</span>
            <span>Wed</span>
            <span>Thu</span>
            <span>Fri</span>
            <span>Sat</span>
          </div>
          <div class="insights-heatmap-grid">
            ${cells.join("")}
          </div>
        </div>
        <div class="insights-heatmap-footer">
          <div class="insights-legend-scale">
            <span class="insights-legend-text">More</span>
            <span class="insights-legend-cell level-4"></span>
            <span class="insights-legend-cell level-3"></span>
            <span class="insights-legend-cell level-2"></span>
            <span class="insights-legend-cell level-1"></span>
            <span class="insights-legend-text">Less</span>
          </div>
          <div class="insights-streak-legend">
            <span class="insights-streak-indicator-box"></span>
            <span class="insights-streak-legend-text">Current streak (${summary?.activeDays ?? 0} active ${(summary?.activeDays ?? 0) === 1 ? "day" : "days"})</span>
          </div>
        </div>
      </div>
    </div>
  `;

  const voiceTabContent = `
    <div class="insights-bento-grid">
      <div class="insights-card insights-card-wpm">
        <div class="insights-card-num">${speedMultiplier}</div>
        <div class="insights-card-label">
          SPEED VS. TYPING (40 WPM)
          <span class="insights-info-icon" title="Your measured speaking speed compared against a 40 WPM keyboard typing baseline">ⓘ</span>
        </div>
        <div class="wpm-gauge-container">
          <svg class="wpm-gauge-svg" viewBox="0 0 160 90">
            <path d="M 20 80 A 60 60 0 0 1 140 80" fill="none" stroke="#222226" stroke-width="12" stroke-linecap="round"/>
            <path d="M 20 80 A 60 60 0 0 1 140 80" fill="none" stroke="#ffffff" stroke-width="12" stroke-linecap="round"
                  stroke-dasharray="188.5" stroke-dashoffset="${dashOffset}" style="filter: drop-shadow(0 0 6px rgba(255,255,255,0.45));"/>
          </svg>
          <div class="wpm-gauge-center">
            <span class="wpm-gauge-top">Pace</span>
            <span class="wpm-gauge-percentile">${displayWpm} WPM</span>
          </div>
        </div>
      </div>

      <div class="insights-card insights-card-fixes">
        <div class="insights-card-num">${(hesitationsRemoved + stuttersCollapsed).toLocaleString()}</div>
        <div class="insights-card-label">HESITATIONS & STUTTERS FILTERED</div>
        <div class="insights-card-divider"></div>
        <div class="insights-fixes-list">
          <div class="insights-fix-row">
            <span>${hesitationsRemoved.toLocaleString()} "ums" & "uhs" removed</span>
            <span class="insights-info-icon" title="Hesitation syllables automatically purged during your dictations">ⓘ</span>
          </div>
          <div class="insights-fix-row">
            <span>${stuttersCollapsed.toLocaleString()} stutters & self-corrections resolved</span>
            <span class="insights-info-icon" title="Repeated letters and spoken corrections consolidated">ⓘ</span>
          </div>
        </div>
      </div>

      <div class="insights-card insights-card-words">
        <div class="insights-card-num">${avgTakeSeconds.toFixed(1)}s</div>
        <div class="insights-card-label">AVERAGE TAKE DURATION</div>
        <div class="insights-card-divider"></div>
        <div class="insights-words-footer">
          <div class="insights-platform-stat">
            <div class="insights-desktop-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
            </div>
            <div class="insights-platform-meta">
              <span class="insights-platform-name">Total Speech Recorded</span>
              <span class="insights-platform-count">${audioMinutes > 0 ? `${(audioMinutes * 60).toFixed(0)}s (${formatMinutes(audioMinutes)})` : "0s"}</span>
            </div>
          </div>
          <span class="insights-badge-pill">AVG ${avgWordsPerTake.toFixed(1)} WORDS / TAKE</span>
        </div>
      </div>

      <div class="insights-card insights-card-desktop">
        <div class="insights-card-top-header">
          <h3 class="insights-card-heading">Models & Lexicon</h3>
          <span class="insights-badge-pill">UNIQUE VOCABULARY | ${uniqueWordsCount.toLocaleString()} WORDS</span>
        </div>
        <div class="insights-usage-list">
          ${modelUsageRows}
        </div>
      </div>

      <div class="insights-card insights-card-streak">
        <div class="insights-card-top-header">
          <h3 class="insights-card-heading">Smart Transformations</h3>
          <span class="insights-badge-pill">${activeRulesCount} CUSTOM ${activeRulesCount === 1 ? "ENTRY" : "ENTRIES"}</span>
        </div>
        <div class="insights-voice-transformations">
          <p class="insights-voice-note">Live formatting and personal dictionary rules currently active on your PC:</p>
          <div class="insights-voice-pills">
            <span class="insights-vp">Auto Capitalization: <strong>${settings.autoCapitalize ? "On" : "Off"}</strong></span>
            <span class="insights-vp">Numbers as Digits: <strong>${settings.numbersAsDigits ? "On" : "Off"}</strong></span>
            <span class="insights-vp">Spoken Emoji: <strong>${settings.spokenEmoji ? "On" : "Off"}</strong></span>
            <span class="insights-vp">Cleanup Level: <strong>${escape(settings.cleanupLevel)}</strong></span>
            <span class="insights-vp">Vocabulary Terms: <strong>${vocabTermsCount}</strong></span>
            <span class="insights-vp">Replacements: <strong>${settings.replacements.length}</strong></span>
            <span class="insights-vp">Snippets: <strong>${settings.snippets.length}</strong></span>
          </div>
          <div class="insights-words-footer" style="margin-top: 24px;">
            <span class="insights-platform-name">100% of speech recognition and text formatting runs locally on your device.</span>
          </div>
        </div>
      </div>
    </div>
  `;

  return `
    <div class="insights-container">
      <div class="insights-header-row">
        <div class="insights-title-group">
          <h1 class="insights-title">Insights</h1>
        </div>
        <div class="insights-header-actions">
          ${hasTakes ? `<button type="button" class="quiet-button insights-reset-btn" id="reset-stats">Reset</button>` : ""}
          <button type="button" class="insights-share-circle" id="share-insights" title="Copy Live Insights">
            <svg class="insights-share-svg-text" viewBox="0 0 100 100">
              <path id="sharePath" d="M 50, 50 m -37, 0 a 37,37 0 1,1 74,0 a 37,37 0 1,1 -74,0" fill="none"/>
              <text font-size="9.5" font-family="'EB Garamond', serif" font-weight="700" letter-spacing="3" fill="#a1a1aa">
                <textPath href="#sharePath" startOffset="0%">SHARE • SHARE • </textPath>
              </text>
            </svg>
            <div class="insights-share-icon-inner">
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/>
                <polyline points="16 6 12 2 8 6"/>
                <line x1="12" y1="2" x2="12" y2="15"/>
              </svg>
            </div>
          </button>
        </div>
      </div>

      <div class="insights-tab-bar">
        <button type="button" class="insights-tab-btn ${insightsTab === "usage" ? "active" : ""}" data-insights-tab="usage">Your Usage</button>
        <button type="button" class="insights-tab-btn ${insightsTab === "voice" ? "active" : ""}" data-insights-tab="voice">Your Voice</button>
      </div>

      ${insightsTab === "usage" ? usageTabContent : voiceTabContent}
    </div>
  `;
}

function generalPage() {
  return `${pageHeader("GENERAL", "Make it <em>yours.</em>", "Lilt stores all preferences strictly locally on this PC. Each change is saved as you make it.")}
  ${settingsCards("settings")}`;
}

function powerPage() {
  return `${pageHeader("POWER", "Stay out of <em>the way.</em>", "Pause for apps that need the microphone, and give memory back when you are not dictating.")}
  ${settingsCards("power")}`;
}

function searchEmptyState() {
  return `${pageHeader("SEARCH", "Nothing <em>found.</em>", `No settings match “${escape(settingsQuery)}”. Try another word, or clear the search.`)}`;
}

function debugFact(label: string, value: string) {
  return `<div><dt>${escape(label)}</dt><dd>${escape(value)}</dd></div>`;
}

function debugPage() {
  const lines = visibleLogs();
  const body = lines.length
    ? lines.map(line => `<div class="log-line level-${escape(line.level)}"><span class="log-level">${escape(line.level)}</span>${escape(line.text)}</div>`).join("")
    : `<div class="empty-history">No warning or error lines yet.${settings.debugLogging ? "" : " Turn on debug logging to see the quieter chatter."}</div>`;
  const report = debugReport;
  const gpuName = report?.gpu.name ?? gpu.name;
  const gpuBackend = report?.gpu.backend ?? gpu.backend;
  const gpuDetail = report?.gpu.detail ?? gpu.detail;
  const gpuDiscrete = report?.gpu.discrete ?? gpu.discrete;
  const gpuVram = report?.gpu.vramMb ?? gpu.vramMb;
  const gpuValue = [
    gpuName,
    gpuDiscrete ? "discrete" : "",
    gpuVram ? `~${gpuVram} MB` : "",
    gpuBackend,
  ].filter(Boolean).join(" · ");
  return `<header><div><p class="overline">DEBUG</p><h1>System & <em>Hardware.</em></h1><p class="lede">Hardware acceleration metrics, system resources, and runtime diagnostics. Audio and logs remain stored strictly on this device.</p></div></header>
    <section class="settings-card"><p class="settings-group">This PC</p>
      <dl class="machine-facts">
        ${debugFact("Version", `Lilt 1.0.0`)}
        ${debugFact("OS", report?.os ?? "…")}
        ${debugFact("CPU", report?.cpu ?? "…")}
        ${debugFact("RAM", report?.ram ?? "…")}
        ${debugFact("GPU", gpuValue)}
      </dl>
      <p class="machine-gpu-detail">${escape(gpuDetail || gpuBackend)}</p>
    </section>
    <section class="settings-card"><p class="settings-group">Logs</p>
      <div class="setting-row"><div><strong>Debug logging</strong><p>Off shows warning and error. On also shows debug and info.</p></div>
        <label class="switch"><input id="debug-logging" type="checkbox" ${settings.debugLogging ? "checked" : ""}/><span></span></label>
      </div>
      <div class="log-toolbar">
        <button type="button" class="quiet-button" id="copy-logs">Copy report</button>
        <button type="button" class="quiet-button" id="clear-logs">Clear</button>
      </div>
      <section class="log-panel">${body}</section>
    </section>`;
}

function familyRow(href: string, markClass: string, mark: string, name: string, host: string) {
  return `<li><button type="button" class="about-family-btn" data-open="${href}">
      <span class="about-family-mark ${markClass}">${mark}</span>
      <span class="about-family-copy"><strong>${name}</strong><span>${host}</span></span>
    </button></li>`;
}

function aboutPage() {
  const installedCount = Object.values(statuses).filter(s => s.installed).length;
  const totalDiskBytes = Object.values(statuses).reduce((sum, s) => sum + (s.bytesOnDisk || 0), 0);
  const diskSummary = totalDiskBytes > 0 ? formatBytes(totalDiskBytes) : "0 MB on disk";
  const activeModel = selected()?.name ?? settings.selectedModel;
  const activeEngine = selected()?.engine ?? "On-device";
  return `<header><div><p class="overline">ABOUT</p><h1>Lilt</h1></div></header>
    <section class="about-hero">
      <img class="about-lockup-hero" src="${liltLockup}" width="280" alt="Lilt" />
      <p class="about-tagline">On-Device Voice Dictation & Formatting Engine</p>
      <span class="brand-tag">Version 1.0.0 · Standalone Edition</span>
    </section>
    <section class="settings-card">
      <p class="settings-group">Live Runtime Configuration</p>
      <div class="about-copy">
        <p><strong>Active Model:</strong> ${escape(activeModel)} (${escape(activeEngine)}) · ${installedCount} of ${models.length} models installed (${escape(diskSummary)}).</p>
        <p><strong>Hardware Acceleration:</strong> ${escape(gpu.name)} (${escape(gpu.backend)}) · <strong>Shortcut:</strong> ${escape(hotkeyLabel())} (${settings.activationMode === "toggle" ? "Toggle" : "Push to talk"}).</p>
      </div>
    </section>
    <section class="settings-card">
      <p class="settings-group">Architecture & Privacy</p>
      <div class="about-copy">
        <p>Lilt runs 100% locally on your Windows PC using whisper.cpp and ONNX Runtime. Your voice recordings, transcription history, and usage insights never leave this machine.</p>
        <p>Built-in text processing automatically removes filler words ("um", "uh"), collapses stutters, resolves spoken self-corrections, and applies your personal vocabulary, replacements, and snippets in real time.</p>
      </div>
    </section>`;
}

const PARAKEET_LANGUAGES = ["English", "Spanish", "French", "German", "Italian", "Portuguese", "Dutch", "Russian", "Polish", "Ukrainian", "Swedish", "Danish", "Finnish", "Czech", "Greek", "Romanian", "Hungarian", "Bulgarian", "Croatian", "Estonian", "Latvian", "Lithuanian", "Maltese", "Slovak", "Slovenian"];

function modelSpeaks(model: Model, language: string) {
  const multilingual = (list: string[]) => language === "Auto-detect" ? list.length > 1 : list.includes(language);
  // Canary and Cohere cannot detect the language; Auto-detect gives them English.
  const told = (list: string[]) => language !== "Auto-detect" && list.includes(language);
  if (model.id === "voca-hinglish") return multilingual(["Hindi", "English"]);
  if (model.engine === "whisper.cpp") return modelIsEnglishOnly(model) ? language === "English" : true;
  switch (model.id) {
    case "parakeet-tdt-0.6b-v3": return multilingual(PARAKEET_LANGUAGES);
    case "sensevoice-small": return multilingual(["Chinese", "Japanese", "Korean", "English"]);
    case "canary-180m": return told(["English", "Spanish", "German", "French"]);
    case "canary-1b-v2": return told(PARAKEET_LANGUAGES);
    case "cohere-transcribe": return told(["English", "German", "French", "Italian", "Spanish", "Portuguese", "Greek", "Dutch", "Polish", "Arabic", "Vietnamese", "Chinese", "Japanese", "Korean"]);
    case "gigaam-v3": return language === "Russian";
    default: return language === "English" && modelIsEnglishOnly(model);
  }
}

/** Up to three models for the setup guide: this PC's suggested size first
 *  when it fits, then specialists for the language, then the rest. */
function suggestedModels(language: string) {
  const fits = models.filter(model => modelSpeaks(model, language));
  const preferred: string[] = [];
  if (recommendation && fits.some(model => model.id === recommendation!.modelId)) preferred.push(recommendation.modelId);
  if (language === "English") preferred.push("parakeet-tdt-0.6b-v3", "moonshine-base");
  else if (language === "Russian") preferred.push("gigaam-v3", "parakeet-tdt-0.6b-v3");
  else if (language === "Hindi") preferred.push("voca-hinglish");
  else if (["Chinese", "Japanese", "Korean"].includes(language)) preferred.push("sensevoice-small");
  else if (PARAKEET_LANGUAGES.includes(language)) preferred.push("parakeet-tdt-0.6b-v3");
  preferred.push("whisper-small", "whisper-base");
  const ordered = [...preferred.map(id => fits.find(model => model.id === id)), ...fits]
    .filter((model): model is Model => !!model);
  return [...new Map(ordered.map(model => [model.id, model])).values()].slice(0, 3);
}

const ONBOARDING_STEPS = ["Welcome", "Language", "Model", "Try it", "Done"];

function onboardingOverlay() {
  if (settings.welcomeDismissed) return "";
  const dots = ONBOARDING_STEPS.map((label, index) =>
    `<li class="${index === onboardingStep ? "current" : index < onboardingStep ? "done" : ""}"><span>${escape(label)}</span></li>`).join("");
  const key = `<kbd>${escape(hotkeyLabel())}</kbd>`;
  const hold = settings.activationMode === "toggle" ? `Tap ${key}, speak, then tap it again.` : `Hold ${key}, speak, then let go.`;
  let body = "";
  let actions = "";
  switch (onboardingStep) {
    case 0:
      body = `<h2 id="welcome-title">Private voice typing for Windows</h2>
        <ul class="onboarding-points">
          <li><strong>Talk anywhere.</strong> ${hold} Your words appear at the caret in the app you are using.</li>
          <li><strong>Stays on this PC.</strong> After a model downloads, audio and text never leave this computer.</li>
          <li><strong>Lives in the tray.</strong> Closing this window keeps Lilt running quietly in the system tray. A small pill shows when it is listening.</li>
        </ul>`;
      actions = `<button type="button" class="text-button" id="onboarding-skip">Skip setup</button><button type="button" class="primary" data-onboarding-next>Get started</button>`;
      break;
    case 1:
      body = `<h2 id="welcome-title">What do you speak?</h2>
        <p>Lilt recommends neural engines tailored to your language. Pick Auto-detect if you switch between languages.</p>
        <select id="onboarding-language" class="themed-select">${LANGUAGE_CHOICES.map(language => `<option value="${escape(language)}" ${language === (settings.language || "Auto-detect") ? "selected" : ""}>${escape(language === "Auto-detect" ? "Auto-detect (several languages)" : language)}</option>`).join("")}</select>`;
      actions = `<button type="button" class="text-button" data-onboarding-back>Back</button><button type="button" class="primary" data-onboarding-next>Next</button>`;
      break;
    case 2: {
      const suggestions = suggestedModels(settings.language || "Auto-detect");
      if (!onboardingModel || !suggestions.some(model => model.id === onboardingModel)) {
        onboardingModel = suggestions.find(model => statuses[model.id]?.installed)?.id ?? suggestions[0]?.id ?? settings.selectedModel;
      }
      const chosen = statuses[onboardingModel];
      const rows = suggestions.map(model => {
        const status = statuses[model.id];
        const state = status?.downloading ? `Downloading ${status.progress}%` : status?.installed ? "On this PC" : model.size;
        const note = recommendation?.modelId === model.id ? " · Suggested for this PC" : "";
        return `<label class="onboarding-model ${model.id === onboardingModel ? "selected" : ""}">
          <input type="radio" name="onboarding-model" value="${escape(model.id)}" ${model.id === onboardingModel ? "checked" : ""} />
          <span><strong>${escape(model.name)}</strong><small>${escape(model.description)}${escape(note)}</small></span>
          <em>${escape(state)}</em></label>`;
      }).join("");
      body = `<h2 id="welcome-title">Pick a speech model</h2>
        <p>Models run on this PC. The download happens once; after that Lilt works completely offline.</p>
        <div class="onboarding-models">${rows || `<p>No model in the catalog covers that language yet. Go back and pick Auto-detect.</p>`}</div>`;
      const label = chosen?.downloading ? `Downloading ${chosen.progress}%` : chosen?.installed ? "Use this model" : "Download and use";
      actions = `<button type="button" class="text-button" data-onboarding-back>Back</button><button type="button" class="primary" id="onboarding-model-go" ${chosen?.downloading || !rows ? "disabled" : ""}>${label}</button>`;
      break;
    }
    case 3:
      body = `<h2 id="welcome-title">Try your first dictation</h2>
        <p>Click in the box, then ${hold.charAt(0).toLowerCase()}${hold.slice(1)} Press Escape while speaking to throw a take away.</p>
        <textarea id="onboarding-try" class="draft" rows="4" placeholder="Your words appear here.">${escape(onboardingTryText)}</textarea>
        <p class="onboarding-note">${recording ? "Listening…" : "It works the same in any app: the text goes where the caret is."}</p>`;
      actions = `<button type="button" class="text-button" data-onboarding-back>Back</button><button type="button" class="primary" data-onboarding-next>${onboardingTryText.trim() ? "Next" : "Skip for now"}</button>`;
      break;
    default:
      body = `<h2 id="welcome-title">You are set</h2>
        <ul class="onboarding-points">
          <li><strong>${escape(hotkeyLabel())}</strong> dictates in any app. Change it, or add a hands-free shortcut or mouse button, on the Shortcuts page.</li>
          <li><strong>Formatting</strong> has spoken numbers, emoji, and cleanup. <strong>Dictionary</strong> fixes names a model gets wrong.</li>
          <li>Lilt lives in the system tray. Closing this window keeps it running.</li>
        </ul>
        <label class="onboarding-login">${switchControl("onboarding-login", settings.launchAtLogin)}<span>Start Lilt when I sign in to Windows</span></label>`;
      actions = `<button type="button" class="text-button" data-onboarding-back>Back</button><button type="button" class="primary" id="welcome-dismiss">Finish</button>`;
  }
  return `<div class="welcome-overlay" role="dialog" aria-modal="true" aria-labelledby="welcome-title">
    <div class="welcome-card onboarding">
      <ol class="onboarding-steps" aria-label="Setup steps">${dots}</ol>
      ${body}
      <div class="onboarding-actions">${actions}</div>
    </div>
  </div>`;
}

function onboardingTrying() {
  return !settings.welcomeDismissed && onboardingStep === 3;
}

function sidebarFooter() {
  const parked = !!runtime.parkKind && !recording;
  const statusLabel = sidebarStatusLabel();
  const result = testResult
    ? `<p class="sidebar-result">${escape(testResult)}</p>`
    : `<p class="sidebar-result muted">Practice here. This take does not type into other apps.</p>`;
  return `<div class="sidebar-footer">
    <div class="sidebar-status ${parked ? "parked" : recording ? "live" : ""}"><i></i><div><strong>${statusLabel}</strong>${runtime.parkDetail && !recording ? `<small>${escape(runtime.parkDetail)}</small>` : `<small>${escape(runtime.inputDevice)}</small>`}</div></div>
    ${toastMarkup()}
    <button type="button" class="quiet-button sidebar-test" id="test-dictation" ${runtime.paused || micTesting ? "disabled" : ""} aria-label="${testListening ? "Stop test dictation" : "Test dictation inside Lilt"}">${testListening ? "Stop test" : testingDictation ? "Testing…" : "Test dictation"}</button>
    ${result}
  </div>`;
}

function toastMarkup() {
  const content = toastText === "Dictation failed"
    ? `${WARN_ICON}Dictation failed`
    : escape(toastText);
  return `<p class="toast" id="sidebar-toast" role="status"${toastText ? "" : " hidden"}>${content}</p>`;
}

function captureChrome() {
  const main = document.querySelector("main");
  if (main && !resetPaneScroll) paneScroll = main.scrollTop;
  const active = document.activeElement;
  if (active instanceof HTMLElement && active.id && !focusRestore) {
    if (active instanceof HTMLInputElement || active instanceof HTMLTextAreaElement) {
      focusRestore = { id: active.id, start: active.selectionStart ?? 0, end: active.selectionEnd ?? 0 };
    } else {
      focusRestore = { id: active.id, start: 0, end: 0 };
    }
  }
}

function restorePaneScroll() {
  const main = document.querySelector("main");
  if (!main) return;
  main.scrollTop = resetPaneScroll ? 0 : paneScroll;
  resetPaneScroll = false;
}

function sidebarNav() {
  const counts = pageMatchCounts();
  const searching = !!searchQuery();
  return NAV_SECTIONS.map(section => {
    const items = section.items
      .filter(([id]) => !searching || counts.has(id))
      .map(([id, label, icon]) => nav(id, label, icon, searching ? counts.get(id) ?? 0 : 0))
      .join("");
    return items ? `<p class="nav-section">${escape(section.label)}</p>${items}` : "";
  }).join("") || `<p class="nav-empty">No settings match.</p>`;
}

/** VocaMac's search contract: filter pages, badge counts, jump to the first
 *  page with matches, and restore the previous page when the search clears. */
function applySettingsSearch(value: string) {
  const was = searchQuery();
  settingsQuery = value;
  const query = searchQuery();
  if (query && !was) pageBeforeSearch = view;
  if (!query) {
    if (pageBeforeSearch) view = pageBeforeSearch;
    pageBeforeSearch = null;
    return;
  }
  // Land on the page whose setting best matches, not merely the first
  // page that mentions the word somewhere.
  const best = new Map<View, number>();
  for (const item of settingsItems()) {
    const score = matchScore(item, query);
    if (score > (best.get(item.page) ?? 0)) best.set(item.page, score);
  }
  const top = Math.max(0, ...best.values());
  if ((best.get(view) ?? 0) < top) {
    const first = ALL_VIEWS.find(id => best.get(id) === top);
    if (first) view = first;
  }
}

function render() {
  captureChrome();
  const pages: Record<View, () => string> = {
    dictation: dictationPage,
    shortcuts: shortcutsPage,
    models: modelsPage,
    audio: audioPage,
    hybrid: hybridPage,
    formatting: formattingPage,
    dictionary: dictionaryPage,
    snippets: snippetsPage,
    history: historyPage,
    stats: insightsPage,
    settings: generalPage,
    power: powerPage,
    debug: debugPage,
    about: aboutPage,
  };
  const noMatches = !!searchQuery() && pageMatchCounts().size === 0;
  app.innerHTML = `<aside>
    <div class="brand"><img class="brand-lockup-img" src="${liltLockup}" alt="Lilt" /></div>
    <div class="sidebar-search"><input id="settings-search" type="search" placeholder="Search settings" aria-label="Search settings" value="${escape(settingsQuery)}" /></div>
    <nav>${sidebarNav()}</nav>
    ${sidebarFooter()}
  </aside>
  <main>
    ${noMatches ? searchEmptyState() : pages[view]()}
    ${onboardingOverlay()}
  </main>`;
  bindChrome();
  restoreFocusedField();
  restorePaneScroll();
  setupWaveCanvases();
}

let waveAnimationId: number | null = null;
let wavePhaseTime = 0;
let mainSmoothedLevel = 0;
let mainMicPoller: number | null = null;

function startMainMicPolling() {
  if (mainMicPoller !== null) return;
  mainMicPoller = window.setInterval(async () => {
    try {
      if (recording || testListening || micTesting) {
        const lvl = await invoke<number>("get_mic_level");
        mainSmoothedLevel = Math.max(lvl, mainSmoothedLevel * 0.82);
      } else {
        mainSmoothedLevel = Math.max(0, mainSmoothedLevel * 0.82);
      }
    } catch {
      mainSmoothedLevel = 0;
    }
  }, 40);
}

function setupWaveCanvases() {
  startMainMicPolling();

  if (waveAnimationId !== null) {
    cancelAnimationFrame(waveAnimationId);
    waveAnimationId = null;
  }

  const canvases = Array.from(document.querySelectorAll<HTMLCanvasElement>(".pill-wave-canvas"));
  if (canvases.length === 0) return;

  const dpr = window.devicePixelRatio || 1;
  const STRANDS = 11;

  canvases.forEach(canvas => {
    const w = canvas.clientWidth || 100;
    const h = canvas.clientHeight || 30;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
  });

  function drawFrame() {
    const activeCanvases = Array.from(document.querySelectorAll<HTMLCanvasElement>(".pill-wave-canvas"));
    if (activeCanvases.length === 0) {
      waveAnimationId = null;
      return;
    }

    const loudness = Math.min(1.0, mainSmoothedLevel * 4.0);
    const active = Math.max(0, Math.min(1.0, (loudness - 0.04) / 0.15));

    const speed = active * (0.048 + loudness * 0.055);
    wavePhaseTime += speed;
    const totalAmp = active * (2.8 + 8.5 * loudness);

    activeCanvases.forEach(canvas => {
      const c = canvas.getContext("2d");
      if (!c) return;
      const w = 100;
      const h = 30;
      const midY = h / 2;

      c.setTransform(dpr, 0, 0, dpr, 0, 0);
      c.clearRect(0, 0, w, h);

      c.save();
      c.globalCompositeOperation = "screen";

      const gradWarm = c.createLinearGradient(0, 0, w, 0);
      gradWarm.addColorStop(0.0, "rgba(255, 69, 0, 0.15)");
      gradWarm.addColorStop(0.18, "rgba(255, 90, 0, 0.85)");
      gradWarm.addColorStop(0.5, "rgba(255, 175, 20, 0.95)");
      gradWarm.addColorStop(0.82, "rgba(255, 45, 85, 0.85)");
      gradWarm.addColorStop(1.0, "rgba(255, 69, 0, 0.15)");

      const gradCool = c.createLinearGradient(0, 0, w, 0);
      gradCool.addColorStop(0.0, "rgba(56, 189, 248, 0.15)");
      gradCool.addColorStop(0.2, "rgba(99, 102, 241, 0.9)");
      gradCool.addColorStop(0.5, "rgba(168, 85, 247, 0.95)");
      gradCool.addColorStop(0.8, "rgba(236, 72, 153, 0.9)");
      gradCool.addColorStop(1.0, "rgba(168, 85, 247, 0.15)");

      const gradCore = c.createLinearGradient(0, 0, w, 0);
      gradCore.addColorStop(0.0, "rgba(255, 255, 255, 0)");
      gradCore.addColorStop(0.25, "rgba(255, 210, 255, 0.85)");
      gradCore.addColorStop(0.5, "rgba(255, 255, 255, 1.0)");
      gradCore.addColorStop(0.75, "rgba(255, 190, 240, 0.85)");
      gradCore.addColorStop(1.0, "rgba(255, 255, 255, 0)");

      for (let s = 0; s < STRANDS; s++) {
        const normIdx = (s - (STRANDS - 1) / 2) / ((STRANDS - 1) / 2);
        const phaseOffset = s * 0.28;
        const isWarm = s % 2 === 1;
        const isCore = s === Math.floor(STRANDS / 2);

        let strokeGrad = isWarm ? gradWarm : gradCool;
        if (isCore && (loudness > 0.15 || active === 0)) {
          strokeGrad = gradCore;
        }

        c.strokeStyle = strokeGrad;
        c.lineWidth = isCore ? 1.9 : 1.25 + 0.3 * (1 - Math.abs(normIdx));
        c.shadowBlur = isCore ? 12 + active * 6 : 6 + active * 6;
        c.shadowColor = isWarm ? "rgba(255, 110, 20, 0.75)" : "rgba(147, 51, 234, 0.75)";

        c.beginPath();
        for (let x = 0; x <= w; x += 2) {
          const p = x / w;
          const env = Math.pow(Math.sin(Math.PI * p), 1.85);
          const h1 = Math.sin(p * Math.PI * 4.2 - wavePhaseTime * 1.4 + phaseOffset);
          const h2 = Math.sin(p * Math.PI * 7.6 + wavePhaseTime * 1.9 - phaseOffset * 1.3);
          const h3 = Math.cos(p * Math.PI * 11.2 - wavePhaseTime * 2.6 + normIdx * 1.8);
          const strandSpread = normIdx * 3.6 * active * (0.3 + loudness * 0.7);
          const y = midY + env * ((h1 * 0.64 + h2 * 0.26 + h3 * 0.10) * totalAmp + strandSpread);

          if (x === 0) c.moveTo(x, y);
          else c.lineTo(x, y);
        }
        c.stroke();
      }
      c.restore();
    });

    waveAnimationId = requestAnimationFrame(drawFrame);
  }

  drawFrame();
}

function openView(next: View) {
  if (next !== view) cancelHotkeyRecording();
  view = next;
  resetPaneScroll = true;
  if (searchQuery() && !pageMatchCounts().has(next)) {
    settingsQuery = "";
    pageBeforeSearch = null;
  }
  if (next === "power" || next === "formatting") {
    void Promise.all([refreshRunningApps(), refreshRuntime()]).then(render);
    return;
  }
  if (next === "debug") {
    void Promise.all([refreshLogs(), refreshDebugReport()]).then(render);
    return;
  }
  if (next === "stats") {
    void refreshStats().then(render);
    return;
  }
  if (next === "history") {
    void refreshHistory().then(render).catch(() => render());
    return;
  }
  render();
}

function bindChrome() {
  document.querySelectorAll<HTMLButtonElement>("[data-view]").forEach(button => button.addEventListener("click", () => {
    openView(button.dataset.view as View);
  }));
  document.querySelectorAll<HTMLButtonElement>("[data-go]").forEach(button => button.addEventListener("click", () => {
    openView(button.dataset.go as View);
  }));
  document.querySelectorAll<HTMLButtonElement>("[data-open]").forEach(button => button.addEventListener("click", () => {
    void openExternal(button.dataset.open!);
  }));
  document.querySelector("#record")?.addEventListener("click", toggleRecording);
  document.querySelector("#hero-dictate-btn")?.addEventListener("click", toggleRecording);
  document.querySelector("#clear-history")?.addEventListener("click", clearHistory);
  document.querySelector("#test-dictation")?.addEventListener("click", testDictation);
  document.querySelector("#mic-test")?.addEventListener("click", toggleMicTest);
  document.querySelector("#welcome-dismiss")?.addEventListener("click", dismissWelcome);
  document.querySelector("#copy-logs")?.addEventListener("click", copyLogs);
  document.querySelector("#clear-logs")?.addEventListener("click", clearLogs);
  document.querySelector("#auto-pause-app")?.addEventListener("change", () => {
    void addWatchedApp();
  });
  bindWatchedAppChips();
  bindFilterCombo("#engine-filter", ENGINE_FILTERS, value => { engineFilter = value as EngineFilter; });
  bindFilterCombo("#language-filter", LANGUAGE_FILTERS, value => { languageFilter = value as LanguageFilter; });
  bindLiveSearch("#settings-search", applySettingsSearch);
  bindLiveSearch("#model-search", value => { modelQuery = value; });
  bindLiveSearch("#history-search", value => { historyQuery = value; });
  document.querySelectorAll<HTMLButtonElement>("[data-capture]").forEach(button => button.addEventListener("click", () => {
    void toggleHotkeyRecording(button.dataset.capture as CaptureTarget);
  }));
  bindPageActions();
  bindOnboarding();
  document.querySelector<HTMLSelectElement>("#paste-app")?.addEventListener("change", event => {
    const name = (event.target as HTMLSelectElement).value.trim();
    if (!name || pasteApps().some(app => app.toLowerCase() === name.toLowerCase())) return;
    settings.pasteApps = [...pasteApps(), name].join("\n");
    void persistSettings(true, true).then(render);
  });
  document.querySelectorAll<HTMLButtonElement>("[data-unpaste]").forEach(button => button.addEventListener("click", () => {
    settings.pasteApps = pasteApps().filter(app => app !== button.dataset.unpaste).join("\n");
    void persistSettings(true, true).then(render);
  }));
  const soundTheme = document.querySelector<HTMLSelectElement>("#sound-theme");
  const previewSound = document.querySelector<HTMLButtonElement>("#preview-sound");
  previewSound?.addEventListener("click", async () => {
    const theme = soundTheme?.value ?? settings.soundTheme;
    if (theme === "off") return;
    try {
      await invoke("preview_sound", { theme, start: previewStartNext });
      previewStartNext = !previewStartNext;
      if (previewSound) {
        const label = previewStartNext ? "Preview start" : "Preview end";
        previewSound.innerHTML = previewStartNext ? ICON_PLAY : ICON_STOP;
        previewSound.setAttribute("aria-label", label);
        previewSound.title = label;
      }
    } catch (error) {
      showToast(String(error));
      render();
    }
  });
  const language = document.querySelector<HTMLSelectElement | HTMLInputElement>("#language");
  if (language) language.value = settings.language;
  const activation = document.querySelector<HTMLSelectElement>("#activation");
  if (activation) activation.value = settings.activationMode;
  document.querySelectorAll<HTMLInputElement>("[data-activate]").forEach(box => {
    box.addEventListener("click", event => event.stopPropagation());
    box.addEventListener("change", () => {
      if (box.checked) void selectModel(box.dataset.activate!);
      else {
        box.checked = true;
      }
    });
  });
  document.querySelectorAll<HTMLButtonElement>("[data-download]").forEach(button => button.addEventListener("click", event => {
    event.stopPropagation();
    downloadModel(button.dataset.download!);
  }));
  document.querySelectorAll<HTMLButtonElement>("[data-delete]").forEach(button => button.addEventListener("click", event => {
    event.stopPropagation();
    deleteModel(button.dataset.delete!);
  }));
  bindAutosave();
}

function bindLiveSearch(selector: string, assign: (value: string) => void) {
  document.querySelector(selector)?.addEventListener("input", event => {
    const input = event.target as HTMLInputElement;
    assign(input.value);
    focusRestore = { id: input.id, start: input.selectionStart ?? input.value.length, end: input.selectionEnd ?? input.value.length };
    render();
  });
}

function restoreFocusedField() {
  if (!focusRestore) return;
  const field = document.querySelector<HTMLElement>(`#${focusRestore.id}`);
  if (field instanceof HTMLInputElement || field instanceof HTMLTextAreaElement) {
    field.focus();
    try { field.setSelectionRange(focusRestore.start, focusRestore.end); } catch { /* ignore */ }
  } else if (field instanceof HTMLSelectElement) {
    field.focus();
  }
  focusRestore = null;
}

function bindFilterCombo(selector: string, options: Array<[string, string]>, assign: (value: string) => void) {
  const input = document.querySelector<HTMLSelectElement | HTMLInputElement>(selector);
  if (!input) return;
  const apply = () => {
    const val = input.value.trim();
    const match = options.find(([id, label]) => id === val || label.toLowerCase() === val.toLowerCase());
    if (!match) return;
    assign(match[0]);
    render();
  };
  input.addEventListener("change", apply);
}

function bindAutosave() {
  const persistFromEvent = (event: Event) => {
    const target = event.target as HTMLElement;
    if (target.id === "settings-search" || target.id === "model-search" || target.id === "engine-filter" || target.id === "language-filter" || target.id === "auto-pause-app" || target.id === "paste-app") return;
    if (target.classList.contains("draft")) return;
    void persistSettings();
  };
  document.querySelectorAll<HTMLElement>(".setting-row input, .setting-row select, .setting-row textarea, #debug-logging, #idle-unload").forEach(node => {
    if (
      node.id === "custom-vocabulary" ||
      node.id === "language" ||
      node.id === "hybrid-api-key" ||
      node.id === "hybrid-model-input" ||
      node.id === "hybrid-base-url" ||
      node.id === "hybrid-custom-prompt"
    ) {
      node.addEventListener("change", persistFromEvent);
      node.addEventListener("blur", persistFromEvent);
      return;
    }
    node.addEventListener("change", persistFromEvent);
    if (node instanceof HTMLInputElement && (node.type === "number" || node.type === "search")) {
      node.addEventListener("blur", persistFromEvent);
    }
  });
}

function collectSettingsFromDom() {
  const preset = document.querySelector<HTMLSelectElement>("#hotkey-preset");
  if (preset) settings.hotkey = preset.value;
  const language = document.querySelector<HTMLSelectElement | HTMLInputElement>("#language");
  if (language) {
    const typed = language.value.trim();
    const match = LANGUAGE_CHOICES.find(item => item.toLowerCase() === typed.toLowerCase())
      ?? (typed && !LANGUAGE_CHOICES.includes(typed) ? typed : "");
    if (match) settings.language = match;
  }
  const activation = document.querySelector<HTMLSelectElement>("#activation");
  if (activation) settings.activationMode = activation.value;
  const silence = document.querySelector<HTMLInputElement>("#silence");
  if (silence) settings.silenceSeconds = Number(silence.value) || 1.5;
  const maxRecording = document.querySelector<HTMLInputElement>("#max-recording");
  if (maxRecording) settings.maxRecordingSeconds = Number(maxRecording.value) || 60;
  const soundTheme = document.querySelector<HTMLSelectElement>("#sound-theme");
  if (soundTheme) {
    settings.soundTheme = soundTheme.value;
    settings.soundEffects = soundTheme.value !== "off";
    previewStartNext = true;
  }
  const autoCap = document.querySelector<HTMLInputElement>("#auto-cap");
  if (autoCap) settings.autoCapitalize = autoCap.checked;
  const trailing = document.querySelector<HTMLInputElement>("#trailing-space");
  if (trailing) settings.appendTrailingSpace = trailing.checked;
  const copyToClipboard = document.querySelector<HTMLInputElement>("#copy-to-clipboard");
  if (copyToClipboard) settings.copyToClipboard = copyToClipboard.checked;
  const launch = document.querySelector<HTMLInputElement>("#launch-login");
  if (launch) settings.launchAtLogin = launch.checked;
  const inputDevice = document.querySelector<HTMLSelectElement>("#input-device");
  if (inputDevice) settings.inputDevice = inputDevice.value;
  const idleUnload = document.querySelector<HTMLSelectElement>("#idle-unload");
  if (idleUnload) {
    const seconds = Number(idleUnload.value);
    settings.idleUnloadEnabled = seconds > 0;
    if (seconds > 0) settings.idleUnloadSeconds = seconds;
  }
  const historyEnabled = document.querySelector<HTMLInputElement>("#history-enabled");
  if (historyEnabled) settings.historyEnabled = historyEnabled.checked;
  const debugLogging = document.querySelector<HTMLInputElement>("#debug-logging");
  if (debugLogging) settings.debugLogging = debugLogging.checked;
  const customVocabulary = document.querySelector<HTMLTextAreaElement>("#custom-vocabulary");
  if (customVocabulary) settings.customVocabulary = customVocabulary.value;
  const pick = (id: string) => document.querySelector<HTMLSelectElement>(`#${id}`)?.value;
  settings.insertionMode = pick("insertion-mode") ?? settings.insertionMode;
  const checked = (id: string) => document.querySelector<HTMLInputElement>(`#${id}`)?.checked;
  settings.handsFreeHotkey = pick("hands-free-hotkey") ?? settings.handsFreeHotkey;
  settings.pasteLastHotkey = pick("paste-last-hotkey") ?? settings.pasteLastHotkey;
  settings.mouseButton = pick("mouse-button") ?? settings.mouseButton;
  settings.cleanupLevel = pick("cleanup-level") ?? settings.cleanupLevel;
  settings.overlayStyle = pick("overlay-style") ?? settings.overlayStyle;
  settings.overlayPosition = pick("overlay-position") ?? settings.overlayPosition;
  const retention = pick("history-retention");
  if (retention !== undefined) settings.historyRetentionDays = Number(retention);
  settings.escapeCancels = checked("escape-cancels") ?? settings.escapeCancels;
  settings.skipSilence = checked("skip-silence") ?? settings.skipSilence;
  settings.livePreview = checked("live-preview") ?? settings.livePreview;
  settings.muteOtherAudio = checked("mute-other-audio") ?? settings.muteOtherAudio;
  settings.numbersAsDigits = checked("numbers-as-digits") ?? settings.numbersAsDigits;
  settings.numberSymbols = checked("number-symbols") ?? settings.numberSymbols;
  settings.spokenEmoji = checked("spoken-emoji") ?? settings.spokenEmoji;
  const storageMode = pick("history-storage-mode");
  if (storageMode) {
    settings.historyKeepAudio = storageMode === "audio_and_text";
  } else {
    settings.historyKeepAudio = checked("history-keep-audio") ?? settings.historyKeepAudio;
  }
  settings.readyPill = checked("ready-pill") ?? settings.readyPill;
  settings.hybridEnabled = checked("hybrid-enabled") ?? settings.hybridEnabled;
  const providerSelect = pick("hybrid-provider");
  if (providerSelect && providerSelect !== settings.hybridProvider) {
    settings.hybridProvider = providerSelect;
    settings.hybridModel = hybridDefaultModel(providerSelect);
    hybridTestStatus = null;
  } else if (providerSelect) {
    settings.hybridProvider = providerSelect;
    const modelSelect = pick("hybrid-model-select");
    const modelInput = document.querySelector<HTMLInputElement>("#hybrid-model-input");
    if (modelSelect && modelSelect !== "__custom__" && modelSelect !== settings.hybridModel) {
      settings.hybridModel = modelSelect;
      if (modelInput) modelInput.value = modelSelect;
    } else if (modelInput && modelInput.value.trim()) {
      settings.hybridModel = modelInput.value.trim();
    }
  }
  const apiKeyInput = document.querySelector<HTMLInputElement>("#hybrid-api-key");
  if (apiKeyInput) settings.hybridApiKey = apiKeyInput.value.trim();
  const baseUrlInput = document.querySelector<HTMLInputElement>("#hybrid-base-url");
  if (baseUrlInput) settings.hybridBaseUrl = baseUrlInput.value.trim();
  settings.hybridStyle = pick("hybrid-style") ?? settings.hybridStyle;
  const customPromptInput = document.querySelector<HTMLTextAreaElement>("#hybrid-custom-prompt");
  if (customPromptInput) settings.hybridCustomPrompt = customPromptInput.value;
}

async function persistSettings(silent = false, skipCollect = false) {
  if (!skipCollect) collectSettingsFromDom();
  try {
    await invoke("save_settings", { settings });
    settings = await invoke<Settings>("get_settings");
    await refreshRuntime();
    if (view === "debug") {
      await Promise.all([refreshLogs(), refreshDebugReport()]);
      render();
      if (!silent) showToast("Settings saved");
      return;
    }
    syncSettingsControls();
    // Some rows enable others (symbols need digits); redraw those pages.
    if (view === "formatting" || view === "shortcuts" || view === "hybrid") render();
    if (!silent) showToast("Settings saved");
  } catch (error) {
    // Keep the window honest: show what was actually saved.
    try { settings = await invoke<Settings>("get_settings"); } catch { /* keep local copy */ }
    render();
    showToast(String(error));
  }
}

function syncSettingsControls() {
  const preset = document.querySelector<HTMLSelectElement>("#hotkey-preset");
  if (preset && document.activeElement !== preset) preset.value = settings.hotkey;
  const language = document.querySelector<HTMLSelectElement | HTMLInputElement>("#language");
  if (language && document.activeElement !== language) language.value = settings.language;
  const activation = document.querySelector<HTMLSelectElement>("#activation");
  if (activation && document.activeElement !== activation) activation.value = settings.activationMode;
  const soundTheme = document.querySelector<HTMLSelectElement>("#sound-theme");
  if (soundTheme && document.activeElement !== soundTheme) soundTheme.value = settings.soundTheme;
  const inputDevice = document.querySelector<HTMLSelectElement>("#input-device");
  if (inputDevice && document.activeElement !== inputDevice) inputDevice.value = settings.inputDevice;
  const idleUnload = document.querySelector<HTMLSelectElement>("#idle-unload");
  if (idleUnload && document.activeElement !== idleUnload) idleUnload.value = String(idleUnloadValue());
}

function bindWatchedAppChips() {
  document.querySelectorAll<HTMLButtonElement>("[data-unwatch]").forEach(button => {
    button.addEventListener("click", () => void removeWatchedApp(button.dataset.unwatch!));
  });
}

function paintPowerApps() {
  const select = document.querySelector<HTMLSelectElement>("#auto-pause-app");
  if (select) {
    select.innerHTML = runningAppOptions();
    select.value = "";
  }
  const chips = document.querySelector("#watched-app-chips");
  if (chips) {
    chips.innerHTML = watchedAppChips();
    bindWatchedAppChips();
  }
}

async function addWatchedApp() {
  const select = document.querySelector<HTMLSelectElement>("#auto-pause-app");
  const name = (select?.value || "").trim();
  if (!name) return;
  const current = watchedApps();
  if (current.some(entry => entry.toLowerCase() === name.toLowerCase())) {
    if (select) select.value = "";
    return;
  }
  current.push(name);
  settings.autoPauseApps = current.join("\n");
  settings.autoPauseEnabled = true;
  await persistSettings();
  paintPowerApps();
}

async function removeWatchedApp(name: string) {
  const remaining = watchedApps().filter(entry => entry !== name);
  settings.autoPauseApps = remaining.join("\n");
  settings.autoPauseEnabled = remaining.length > 0;
  await persistSettings();
  paintPowerApps();
}

async function openExternal(url: string) {
  try {
    await invoke("open_external", { url });
  } catch (error) {
    showToast(String(error));
    render();
  }
}

function codeToHotkeyPart(code: string, key: string): string | null {
  const map: Record<string, string> = {
    Space: "Space",
    F7: "F7",
    F8: "F8",
    F9: "F9",
    F10: "F10",
    ControlRight: "ControlRight",
    ControlLeft: "ControlLeft",
    AltRight: "AltRight",
    AltLeft: "AltLeft",
    ShiftRight: "ShiftRight",
    ShiftLeft: "ShiftLeft",
  };
  if (map[code]) return map[code];
  if (/^Key[A-Z]$/.test(code)) return code.slice(3);
  if (/^Digit[0-9]$/.test(code)) return code.slice(5);
  if (key.length === 1) return key.toUpperCase();
  return null;
}

let webCaptureSawNonModifier = false;
let webCaptureModifiers = new Set<string>();

async function toggleHotkeyRecording(target: CaptureTarget) {
  if (recordingHotkey) {
    const same = recordingHotkey === target;
    recordingHotkey = null;
    nativeCapture = false;
    webCaptureSawNonModifier = false;
    webCaptureModifiers.clear();
    try { await invoke("stop_hotkey_capture"); } catch { /* ignore */ }
    if (same) {
      showToast("Shortcut recording cancelled.");
      render();
      return;
    }
  }
  // The hook sees keys an IME or this webview would take (Ctrl+Space);
  // elsewhere the page records them itself. The target is set first so a
  // result that arrives before the command returns is not dropped.
  recordingHotkey = target;
  nativeCapture = true;
  webCaptureSawNonModifier = false;
  webCaptureModifiers.clear();
  render();
  (document.activeElement as HTMLElement | null)?.blur?.();
  try { nativeCapture = await invoke<boolean>("start_hotkey_capture"); } catch { nativeCapture = false; }
  if (!nativeCapture && recordingHotkey === target) {
    try { await invoke("pause_hotkey_listener"); } catch { /* ignore */ }
  }
}

/** Leaving the page mid-recording must not keep capturing keys. */
function cancelHotkeyRecording() {
  if (!recordingHotkey) return;
  recordingHotkey = null;
  nativeCapture = false;
  webCaptureSawNonModifier = false;
  webCaptureModifiers.clear();
  void invoke("stop_hotkey_capture").catch(() => undefined);
}

function finishHotkeyCapture(spec: string, label: string) {
  if (!recordingHotkey) return;
  const target = recordingHotkey;
  settings[target] = spec;
  recordingHotkey = null;
  nativeCapture = false;
  webCaptureSawNonModifier = false;
  webCaptureModifiers.clear();
  void invoke("stop_hotkey_capture").catch(() => undefined);
  render();
  void persistSettings(true, true).then(() => {
    render();
    syncSettingsControls();
    if (settings[target] === spec) showToast(`Shortcut set to ${hotkeyLabel(spec)}.`);
  });
}

function isSafeLoneWebKey(part: string): boolean {
  return /^F([1-9]|1[0-9]|2[0-4])$/.test(part)
    || part === "Insert"
    || part === "Pause"
    || part === "ScrollLock";
}

function onGlobalKeyDown(event: KeyboardEvent) {
  if (!recordingHotkey) return;
  event.preventDefault();
  event.stopPropagation();
  if (event.key === "Escape" && !event.ctrlKey && !event.altKey && !event.shiftKey) {
    recordingHotkey = null;
    nativeCapture = false;
    webCaptureSawNonModifier = false;
    webCaptureModifiers.clear();
    showToast("Shortcut recording cancelled.");
    void invoke("stop_hotkey_capture").catch(() => undefined);
    render();
    return;
  }
  if (event.key === "Meta" || event.code.startsWith("Meta") || event.code === "OSLeft" || event.code === "OSRight") {
    showToast("Win/Super is reserved on Windows. Pick another key.");
    render();
    return;
  }
  const isModifier = ["Control", "Alt", "Shift", "Meta"].includes(event.key);
  if (isModifier) {
    if (event.code) webCaptureModifiers.add(event.code);
    return;
  }
  webCaptureSawNonModifier = true;
  const parts: string[] = [];
  if (event.ctrlKey) parts.push("Ctrl");
  if (event.altKey) parts.push("Alt");
  if (event.shiftKey) parts.push("Shift");
  const keyPart = codeToHotkeyPart(event.code, event.key);
  if (!keyPart) return;
  if (keyPart === "ControlRight" || keyPart === "AltRight" || keyPart === "ShiftRight"
    || keyPart === "ControlLeft" || keyPart === "AltLeft" || keyPart === "ShiftLeft") {
    finishHotkeyCapture(keyPart, hotkeyLabel(keyPart));
    return;
  }
  if (!event.ctrlKey && !event.altKey && !isSafeLoneWebKey(keyPart)) {
    showToast(`${keyPart} types or moves the caret. Hold Ctrl or Alt with it, for example Ctrl+${keyPart}.`);
    render();
    return;
  }
  parts.push(keyPart);
  const combo = parts.join("+");
  finishHotkeyCapture(combo, hotkeyLabel(combo));
}

function onGlobalKeyUp(event: KeyboardEvent) {
  if (!recordingHotkey) return;
  const sideModifiers: Record<string, string> = {
    ControlRight: "Right Ctrl",
    AltRight: "Right Alt",
    ShiftRight: "Right Shift",
    ControlLeft: "Left Ctrl",
    AltLeft: "Left Alt",
    ShiftLeft: "Left Shift",
  };
  if (sideModifiers[event.code] && !webCaptureSawNonModifier) {
    event.preventDefault();
    event.stopPropagation();
    if (webCaptureModifiers.size <= 1) {
      finishHotkeyCapture(event.code, sideModifiers[event.code]);
    } else {
      webCaptureModifiers.clear();
    }
  }
}

type CaptureOutcome =
  | { kind: "shortcut"; value: string }
  | { kind: "refused"; value: string }
  | { kind: "cancelled" };

listen<CaptureOutcome>("hotkey-captured", event => {
  if (!recordingHotkey) return;
  const outcome = event.payload;
  if (outcome.kind === "shortcut") {
    finishHotkeyCapture(outcome.value, hotkeyLabel(outcome.value));
  } else if (outcome.kind === "refused") {
    // Still recording: the hook waits for another try.
    showToast(outcome.value);
    render();
  } else {
    recordingHotkey = null;
    nativeCapture = false;
    webCaptureSawNonModifier = false;
    webCaptureModifiers.clear();
    void invoke("stop_hotkey_capture").catch(() => undefined);
    showToast("Shortcut recording cancelled.");
    render();
  }
}).catch(() => undefined);

async function refreshStatuses() { statuses = await invoke<Record<string, ModelStatus>>("get_model_statuses"); }
async function refreshHistory() { history = await invoke<HistoryEntry[]>("get_history"); }
async function refreshRuntime() {
  try {
    runtime = await invoke<RuntimeStatus>("get_runtime_status");
    if (typeof runtime.recording === "boolean") {
      recording = runtime.recording;
      if (!runtime.recording) testListening = false;
    }
  } catch { /* ignore */ }
}
async function refreshLogs() {
  try { logLines = await invoke<LogLine[]>("get_log_lines"); } catch { logLines = []; }
}
async function refreshDebugReport() {
  try { debugReport = await invoke<DebugReport>("get_debug_report"); } catch { debugReport = null; }
}
async function refreshRunningApps() {
  try { runningApps = await invoke<RunningApp[]>("list_running_apps"); } catch { runningApps = []; }
}
async function clearHistory() {
  try { await invoke("clear_history"); history = []; showToast("History cleared."); } catch (error) { showToast(String(error)); }
  render();
}
async function copyLogs() {
  let text = "";
  try {
    const report = await invoke<DebugReport>("get_debug_report");
    debugReport = report;
    text = report.text;
  } catch {
    showToast("Could not build the debug report.");
    render();
    return;
  }
  if (!text.trim()) {
    showToast("Nothing to copy.");
    render();
    return;
  }
  try {
    await invoke("copy_text", { text });
    showToast("Debug report copied.");
  } catch {
    try {
      await navigator.clipboard.writeText(text);
      showToast("Debug report copied.");
    } catch {
      showToast("Could not copy the debug report.");
    }
  }
}
async function clearLogs() {
  if (!window.confirm("Clear the log buffer for this session? This does not delete files on disk.")) {
    return;
  }
  try {
    await invoke("clear_log_lines");
    logLines = [];
    showToast("Logs cleared.");
  } catch (error) {
    showToast(String(error));
  }
  render();
}
async function dismissWelcome() {
  try {
    const login = document.querySelector<HTMLInputElement>("#onboarding-login");
    if (login && login.checked !== settings.launchAtLogin) {
      settings.launchAtLogin = login.checked;
      await persistSettings(true, true);
    }
    await invoke("dismiss_welcome");
    settings.welcomeDismissed = true;
    onboardingStep = 0;
  } catch (error) {
    showToast(String(error));
  }
  render();
}

async function refreshStats() {
  try { stats = await invoke<StatsSummary>("get_stats"); } catch { stats = null; }
}

function historyId(button: HTMLElement, key: string) {
  return Number(button.dataset[key]);
}

function bindPageActions() {
  document.querySelectorAll<HTMLButtonElement>("[data-copy-history]").forEach(button => button.addEventListener("click", async () => {
    const entry = history.find(item => item.id === historyId(button, "copyHistory"));
    if (!entry?.text) return;
    try { await invoke("copy_text", { text: entry.text }); showToast("Copied."); } catch (error) { showToast(String(error)); }
  }));
  document.querySelectorAll<HTMLButtonElement>("[data-play-history]").forEach(button => button.addEventListener("click", async () => {
    const id = historyId(button, "playHistory");
    try {
      if (playingId === id) {
        await invoke("stop_history_audio");
        playingId = null;
      } else {
        await invoke("play_history_audio", { id });
        playingId = id;
        const entry = history.find(item => item.id === id);
        const length = Math.max(1000, entry?.durationMs ?? 5000);
        window.setTimeout(() => { if (playingId === id) { playingId = null; render(); } }, length + 300);
      }
    } catch (error) { playingId = null; showToast(String(error)); }
    render();
  }));
  document.querySelectorAll<HTMLButtonElement>("[data-retry-history]").forEach(button => button.addEventListener("click", async () => {
    const id = historyId(button, "retryHistory");
    retrying.add(id);
    render();
    try {
      const text = await invoke<string>("retry_history_entry", { id });
      showToast(text ? "Retried with the current model." : "No speech was recognized.");
    } catch (error) { showToast(String(error)); }
    retrying.delete(id);
    await refreshHistory().catch(() => undefined);
    render();
  }));
  document.querySelectorAll<HTMLButtonElement>("[data-delete-history]").forEach(button => button.addEventListener("click", async () => {
    const id = historyId(button, "deleteHistory");
    try {
      await invoke("delete_history_entry", { id });
      if (playingId === id) playingId = null;
      history = history.filter(item => item.id !== id);
    } catch (error) { showToast(String(error)); }
    render();
  }));
  document.querySelector("#reset-stats")?.addEventListener("click", async () => {
    if (!window.confirm("Reset all insights and statistics on this PC? History is not affected.")) return;
    try { await invoke("reset_stats"); await refreshStats(); showToast("Insights reset."); } catch (error) { showToast(String(error)); }
    render();
  });
  document.querySelectorAll<HTMLButtonElement>("[data-insights-tab]").forEach(button => button.addEventListener("click", () => {
    const tab = button.dataset.insightsTab as "usage" | "voice";
    if (tab && tab !== insightsTab) {
      insightsTab = tab;
      render();
    }
  }));
  document.querySelector("#share-insights")?.addEventListener("click", () => {
    const s = stats;
    const words = (s?.words ?? 0).toLocaleString();
    const takes = (s?.dictations ?? 0).toLocaleString();
    const wpm = s?.wordsPerMinute ?? 0;
    const streak = s?.currentStreak ?? 0;
    const fixes = (s?.fixesTotal ?? 0).toLocaleString();
    const shareText = `Lilt Live Insights\nWords Dictated: ${words} (${takes} takes)\nSpeaking Pace: ${wpm} WPM\nFixes Applied: ${fixes}\nCurrent Streak: ${streak} ${streak === 1 ? "day" : "days"}\nDictated locally on Windows with Lilt.`;
    navigator.clipboard.writeText(shareText).then(() => {
      showToast("Live insights copied to clipboard!");
    }).catch(() => {
      showToast("Could not copy insights.");
    });
  });
  document.querySelector("#streak-prev")?.addEventListener("click", () => {
    insightsMonthOffset -= 1;
    render();
  });
  document.querySelector("#streak-next")?.addEventListener("click", () => {
    if (insightsMonthOffset < 0) {
      insightsMonthOffset += 1;
      render();
    }
  });
  document.querySelector("#add-replacement")?.addEventListener("click", () => {
    const heard = document.querySelector<HTMLInputElement>("#replacement-heard")?.value.trim() ?? "";
    const replacement = document.querySelector<HTMLInputElement>("#replacement-to")?.value.trim() ?? "";
    if (!heard || !replacement) { showToast("Fill in what is heard and what to type."); return; }
    settings.replacements = [...settings.replacements, { heard, replacement }];
    void persistSettings(true, true).then(render);
  });
  document.querySelectorAll<HTMLButtonElement>("[data-remove-replacement]").forEach(button => button.addEventListener("click", () => {
    const index = Number(button.dataset.removeReplacement);
    settings.replacements = settings.replacements.filter((_, position) => position !== index);
    void persistSettings(true, true).then(render);
  }));
  document.querySelector("#add-snippet")?.addEventListener("click", () => {
    const trigger = document.querySelector<HTMLInputElement>("#snippet-trigger")?.value.trim() ?? "";
    const expansion = document.querySelector<HTMLTextAreaElement>("#snippet-expansion")?.value ?? "";
    if (!trigger || !expansion.trim()) { showToast("Fill in the trigger and the text to type."); return; }
    settings.snippets = [...settings.snippets, { trigger, expansion }];
    void persistSettings(true, true).then(render);
  });
  document.querySelectorAll<HTMLButtonElement>("[data-remove-snippet]").forEach(button => button.addEventListener("click", () => {
    const index = Number(button.dataset.removeSnippet);
    settings.snippets = settings.snippets.filter((_, position) => position !== index);
    void persistSettings(true, true).then(render);
  }));
  const tryInput = document.querySelector<HTMLTextAreaElement>("#try-input");
  tryInput?.addEventListener("input", () => { tryText = tryInput.value; });
  document.querySelector("#try-run")?.addEventListener("click", async () => {
    try { tryResult = (await invoke<string>("preview_text", { text: tryText })) || "(nothing would be typed)"; } catch (error) { tryResult = String(error); }
    render();
  });
  document.querySelector("#hybrid-toggle-key")?.addEventListener("click", () => {
    hybridShowKey = !hybridShowKey;
    render();
  });
  document.querySelector("#hybrid-test-btn")?.addEventListener("click", async () => {
    collectSettingsFromDom();
    await persistSettings(true, true);
    hybridTesting = true;
    hybridTestStatus = null;
    render();
    try {
      const res = await invoke<HybridTestResult>("test_hybrid_connection", {
        config: {
          enabled: settings.hybridEnabled,
          provider: settings.hybridProvider,
          apiKey: settings.hybridApiKey,
          model: settings.hybridModel,
          baseUrl: settings.hybridBaseUrl,
          style: settings.hybridStyle,
          customPrompt: settings.hybridCustomPrompt,
          vocabularyHint: settings.customVocabulary,
        },
      });
      hybridTestStatus = {
        ok: true,
        text: `✓ ${res.message} · Sample: “${res.sampleOutput}”`,
      };
      showToast(`Connected in ${res.latencyMs} ms`);
    } catch (error) {
      hybridTestStatus = {
        ok: false,
        text: `✕ ${String(error)}`,
      };
      showToast("Connection failed");
    }
    hybridTesting = false;
    render();
  });
  const hybridTryInput = document.querySelector<HTMLTextAreaElement>("#hybrid-try-input");
  hybridTryInput?.addEventListener("input", () => {
    hybridTryText = hybridTryInput.value;
  });
  document.querySelectorAll<HTMLButtonElement>("[data-fill-command]").forEach(btn => {
    btn.addEventListener("click", () => {
      hybridTryText = btn.dataset.fillCommand || "";
      if (hybridTryInput) hybridTryInput.value = hybridTryText;
      hybridPreviewResult = null;
      render();
    });
  });
  document.querySelector("#hybrid-try-run")?.addEventListener("click", async () => {
    collectSettingsFromDom();
    await persistSettings(true, true);
    hybridPreviewLoading = true;
    render();
    try {
      hybridPreviewResult = await invoke<HybridPreviewResult>("preview_hybrid_text", {
        text: hybridTryText,
      });
    } catch (error) {
      hybridPreviewResult = {
        localText: hybridTryText,
        polishedText: hybridTryText,
        latencyMs: 0,
        provider: hybridProviderLabel(settings.hybridProvider),
        model: settings.hybridModel,
        usedHybrid: false,
        error: String(error),
      };
    }
    hybridPreviewLoading = false;
    render();
  });
  document.querySelector("#export-settings")?.addEventListener("click", async () => {
    try { const path = await invoke<string>("export_settings"); showToast(`Saved to ${path}`); } catch (error) { showToast(String(error)); }
  });
  const importFile = document.querySelector<HTMLInputElement>("#import-file");
  document.querySelector("#import-settings")?.addEventListener("click", () => importFile?.click());
  importFile?.addEventListener("change", async () => {
    const file = importFile.files?.[0];
    if (!file) return;
    try {
      const contents = await file.text();
      settings = await invoke<Settings>("parse_settings_backup", { contents });
      await persistSettings(true, true);
      showToast("Settings imported.");
    } catch (error) { showToast(String(error)); }
    render();
  });
  document.querySelector("#run-setup")?.addEventListener("click", async () => {
    settings.welcomeDismissed = false;
    onboardingStep = 0;
    onboardingTryText = "";
    await persistSettings(true, true);
    render();
  });
}

function bindOnboarding() {
  document.querySelector("#onboarding-skip")?.addEventListener("click", dismissWelcome);
  document.querySelectorAll<HTMLButtonElement>("[data-onboarding-next]").forEach(button => button.addEventListener("click", async () => {
    if (onboardingStep === 1) {
      const language = document.querySelector<HTMLSelectElement>("#onboarding-language")?.value;
      if (language && language !== settings.language) {
        settings.language = language;
        await persistSettings(true, true);
      }
    }
    onboardingStep = Math.min(ONBOARDING_STEPS.length - 1, onboardingStep + 1);
    render();
    if (onboardingStep === 3) document.querySelector<HTMLTextAreaElement>("#onboarding-try")?.focus();
  }));
  document.querySelectorAll<HTMLButtonElement>("[data-onboarding-back]").forEach(button => button.addEventListener("click", () => {
    onboardingStep = Math.max(0, onboardingStep - 1);
    render();
  }));
  document.querySelectorAll<HTMLInputElement>('input[name="onboarding-model"]').forEach(radio => radio.addEventListener("change", () => {
    onboardingModel = radio.value;
    render();
  }));
  document.querySelector("#onboarding-model-go")?.addEventListener("click", async () => {
    const id = onboardingModel;
    if (!id) return;
    if (!statuses[id]?.installed) {
      await downloadModel(id);
      if (!statuses[id]?.installed) return;
    }
    await selectModel(id);
    onboardingStep = 3;
    render();
    document.querySelector<HTMLTextAreaElement>("#onboarding-try")?.focus();
  });
  const tryBox = document.querySelector<HTMLTextAreaElement>("#onboarding-try");
  tryBox?.addEventListener("input", () => {
    const hadText = !!onboardingTryText.trim();
    onboardingTryText = tryBox.value;
    // The first words turn "Skip for now" into "Next".
    if (hadText !== !!onboardingTryText.trim()) {
      const next = document.querySelector<HTMLButtonElement>("[data-onboarding-next]");
      if (next) next.textContent = onboardingTryText.trim() ? "Next" : "Skip for now";
    }
  });
}
async function downloadModel(id: string) {
  try {
    statuses[id] = { ...(statuses[id] ?? { installed: false, downloadable: true }), downloading: true, progress: 0, message: "Connecting…" };
    showToast(`Downloading ${models.find(model => model.id === id)?.name ?? "model"}…`);
    render();
    const timer = window.setInterval(() => refreshStatuses().then(render).catch(() => undefined), 500);
    await invoke("download_model", { modelId: id });
    window.clearInterval(timer); await refreshStatuses(); showToast("Model downloaded.");
  } catch (error) {
    await refreshStatuses().catch(() => undefined);
    const status = statuses[id];
    showToast(status?.message ? String(status.message) : String(error));
  }
  render();
}
async function deleteModel(id: string) {
  try { await invoke("delete_model", { modelId: id }); await refreshStatuses(); showToast("Model removed."); } catch (error) { showToast(String(error)); }
  render();
}
async function selectModel(id: string) {
  settings.selectedModel = id;
  try { await invoke("save_settings", { settings }); showToast(`${selected()?.name ?? "Model"} is active.`); render(); } catch (error) { showToast(String(error)); render(); }
}
async function toggleRecording() {
  if (testListening) return;
  try {
    if (!recording) {
      if (!modelInstalled()) {
        showToast(emptySpeechMessage());
        render();
        return;
      }
      await invoke("start_recording");
      recording = true;
      await refreshRuntime();
      render();
      return;
    }
    recording = false; render();
    const text = await invoke<string>("stop_and_transcribe");
    if (!text) { showToast(emptySpeechMessage()); render(); return; }
    await invoke("inject_text", { text });
    await Promise.all([refreshHistory(), refreshStats()]);
  } catch (_error) { recording = false; showToast("Dictation failed"); }
  await refreshRuntime();
  render();
}
async function toggleMicTest() {
  try {
    if (!micTesting) {
      await invoke("start_mic_test");
      micTesting = true;
      micPeak = 0;
      if (micMeterTimer) window.clearInterval(micMeterTimer);
      micMeterTimer = window.setInterval(async () => {
        try {
          micLevel = await invoke<number>("get_mic_level");
          micPeak = Math.max(micPeak, micLevel);
          document.querySelectorAll<HTMLElement>(".level-meter span").forEach(bar => {
            bar.style.width = `${Math.min(100, Math.round(micLevel * 140))}%`;
          });
        } catch { /* ignore */ }
      }, 80);
      render();
      return;
    }
    await invoke("stop_mic_test");
    micTesting = false;
    micLevel = 0;
    if (micMeterTimer) { window.clearInterval(micMeterTimer); micMeterTimer = null; }
  } catch (error) {
    const emptyCapture = String(error).includes("No microphone audio was captured");
    micTesting = false;
    micLevel = 0;
    if (micMeterTimer) { window.clearInterval(micMeterTimer); micMeterTimer = null; }
    if (!(emptyCapture && micPeak > 0)) {
      showToast(String(error));
    }
  }
  micPeak = 0;
  render();
}
async function testDictation() {
  if (runtime.paused || micTesting) return;
  try {
    if (!testListening) {
      if (!modelInstalled()) {
        showToast(emptySpeechMessage());
        render();
        return;
      }
      testingDictation = true;
      testListening = true;
      recording = true;
      testResult = "";
      render();
      await invoke("start_recording", { noInject: true });
      testingDictation = false;
      await refreshRuntime();
      render();
      return;
    }
    testingDictation = true;
    recording = false;
    testListening = false;
    render();
    const text = await invoke<string>("stop_and_transcribe");
    testResult = text || emptySpeechMessage();
    await Promise.all([refreshHistory(), refreshStats()]);
  } catch (_error) {
    recording = false;
    testListening = false;
    testResult = "Dictation failed";
    try { await invoke("stop_and_transcribe"); } catch { /* leftover session is cleared in rust */ }
  }
  testingDictation = false;
  await refreshRuntime();
  render();
}

window.addEventListener("keydown", onGlobalKeyDown, true);
window.addEventListener("keyup", onGlobalKeyUp, true);

/** While the setup guide's "Try it" box is up, dictation types into it; a
 *  full redraw mid-typing would drop keystrokes, so only the note updates. */
function paintOnboardingNote() {
  const note = document.querySelector(".onboarding-note");
  if (note) note.textContent = recording ? "Listening…" : "It works the same in any app: the text goes where the caret is.";
  paintSidebarStatus();
}

listen<boolean>("recording-changed", event => {
  recording = event.payload;
  if (!event.payload) testListening = false;
  if (onboardingTrying()) {
    paintOnboardingNote();
    void refreshRuntime();
    return;
  }
  refreshRuntime().then(render).catch(() => render());
}).catch(() => undefined);
listen<string>("dictation-finished", async () => {
  recording = false;
  await Promise.all([refreshHistory(), refreshStats(), refreshRuntime()]).catch(() => undefined);
  if (onboardingTrying()) {
    paintOnboardingNote();
    return;
  }
  render();
}).catch(() => undefined);
listen("dictation-cancelled", () => {
  if (!onboardingTrying()) showToast("Cancelled. Nothing was typed.");
}).catch(() => undefined);
listen("history-changed", async () => {
  if (onboardingTrying()) return;
  await Promise.all([refreshHistory(), refreshStats()]).catch(() => undefined);
  if (view === "history" || view === "stats") render();
}).catch(() => undefined);
listen<string>("test-dictation-finished", async event => {
  recording = false;
  testListening = false;
  testResult = event.payload || emptySpeechMessage();
  await Promise.all([refreshHistory(), refreshStats(), refreshRuntime()]).catch(() => undefined);
  render();
}).catch(() => undefined);
listen<string>("dictation-error", _event => {
  recording = false;
  showToast("Dictation failed");
  refreshRuntime().then(render).catch(() => render());
}).catch(() => undefined);
listen<boolean>("recording-changed", event => {
  recording = !!event.payload;
  render();
}).catch(() => undefined);
listen<RuntimeStatus>("runtime-status", event => {
  applyRuntimeStatus(event.payload);
}).catch(() => undefined);
listen<Settings>("settings-changed", event => {
  settings = { ...settings, ...event.payload };
  render();
}).catch(() => undefined);
listen<string>("navigate", event => {
  if (ALL_VIEWS.includes(event.payload as View)) openView(event.payload as View);
}).catch(() => undefined);
/** Adds a live log line the way `logbuf::append` does: at 500 lines the
 *  oldest debug line goes first, so the pane keeps the same warnings and
 *  errors as a copied report. */
function appendLogLine(lines: LogLine[], line: LogLine): LogLine[] {
  if (lines.length < 500) return [...lines, line];
  const oldestDebug = lines.findIndex(entry => entry.level === "debug");
  const drop = oldestDebug === -1 ? 0 : oldestDebug;
  return [...lines.slice(0, drop), ...lines.slice(drop + 1), line];
}

listen<LogLine>("log-line", event => {
  const line = event.payload;
  if (line && typeof line === "object" && "text" in line) {
    logLines = appendLogLine(logLines, line);
  }
  if (view === "debug") render();
}).catch(() => undefined);

Promise.all([
  invoke<Model[]>("get_models"),
  invoke<Settings>("get_settings"),
  invoke<Record<string, ModelStatus>>("get_model_statuses"),
  invoke<HistoryEntry[]>("get_history"),
  invoke<HotkeyPreset[]>("get_hotkey_presets"),
  invoke<GpuStatus>("get_gpu_status"),
  invoke<InputDevice[]>("list_input_devices").catch(() => [] as InputDevice[]),
  invoke<ModelRecommendation>("recommend_model").catch(() => null),
  invoke<RuntimeStatus>("get_runtime_status").catch(() => runtime),
  invoke<LogLine[]>("get_log_lines").catch(() => [] as LogLine[]),
  invoke<StatsSummary>("get_stats").catch(() => null),
]).then(([catalog, saved, installs, entries, hotkeyPresets, gpuStatus, inputDevices, modelRec, runtimeStatus, logs, initialStats]) => {
  models = catalog;
  stats = initialStats;
  settings = {
    ...saved,
    soundTheme: saved.soundTheme || (saved.soundEffects === false ? "off" : "voca"),
    soundEffects: saved.soundEffects ?? true,
    maxRecordingSeconds: saved.maxRecordingSeconds ?? 60,
    appendTrailingSpace: saved.appendTrailingSpace ?? true,
    autoCapitalize: saved.autoCapitalize ?? true,
    inputDevice: saved.inputDevice ?? "",
    autoPauseEnabled: saved.autoPauseEnabled ?? false,
    autoPauseApps: saved.autoPauseApps ?? "",
    idleUnloadEnabled: saved.idleUnloadEnabled ?? false,
    idleUnloadSeconds: saved.idleUnloadSeconds ?? 300,
    welcomeDismissed: saved.welcomeDismissed ?? false,
    historyEnabled: saved.historyEnabled ?? true,
    debugLogging: saved.debugLogging ?? false,
    customVocabulary: saved.customVocabulary ?? "",
    copyToClipboard: saved.copyToClipboard ?? false,
    cleanupLevel: saved.cleanupLevel ?? "medium",
    numbersAsDigits: saved.numbersAsDigits ?? false,
    numberSymbols: saved.numberSymbols ?? false,
    spokenEmoji: saved.spokenEmoji ?? false,
    replacements: saved.replacements ?? [],
    snippets: saved.snippets ?? [],
    escapeCancels: saved.escapeCancels ?? true,
    handsFreeHotkey: saved.handsFreeHotkey ?? "",
    pasteLastHotkey: saved.pasteLastHotkey ?? "",
    mouseButton: saved.mouseButton ?? "",
    overlayStyle: saved.overlayStyle ?? "minimal",
    overlayPosition: saved.overlayPosition ?? "bottom",
    overlayCustomX: saved.overlayCustomX ?? null,
    overlayCustomY: saved.overlayCustomY ?? null,
    readyPill: saved.readyPill ?? true,
    skipSilence: saved.skipSilence ?? true,
    livePreview: saved.livePreview ?? false,
    muteOtherAudio: saved.muteOtherAudio ?? false,
    historyRetentionDays: saved.historyRetentionDays ?? 30,
    historyKeepAudio: saved.historyKeepAudio ?? true,
    insertionMode: saved.insertionMode ?? "type",
    pasteApps: saved.pasteApps ?? "",
    hybridEnabled: saved.hybridEnabled ?? false,
    hybridProvider: saved.hybridProvider || "openai",
    hybridApiKey: saved.hybridApiKey ?? "",
    hybridModel: saved.hybridModel || "gpt-4o-mini",
    hybridBaseUrl: saved.hybridBaseUrl ?? "",
    hybridStyle: saved.hybridStyle || "smart_polish",
    hybridCustomPrompt: saved.hybridCustomPrompt ?? "",
  };
  statuses = installs;
  history = entries;
  presets = hotkeyPresets;
  gpu = {
    ...gpuStatus,
    deviceIndex: gpuStatus.deviceIndex ?? -1,
    discrete: gpuStatus.discrete ?? false,
    vramMb: gpuStatus.vramMb ?? 0,
    detail: gpuStatus.detail ?? "",
  };
  devices = inputDevices;
  recommendation = modelRec;
  runtime = runtimeStatus;
  logLines = logs;
  render();
}).catch(error => { app.textContent = `Could not start Lilt: ${error}`; });
