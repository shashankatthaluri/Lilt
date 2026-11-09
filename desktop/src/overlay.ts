import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import "./overlay.css";

type Payload = { phase: string; title: string; detail: string };

const pill = document.querySelector<HTMLDivElement>("#pill")!;
const SHADOW_ROOM = 40;
const IDLE_SIZE = 40;

// Create persistent child elements once so transitions between states never tear down the DOM
const dotEl = document.createElement("span");
dotEl.className = "status-dot dot-white";
const labelEl = document.createElement("span");
labelEl.className = "status-label";
const closeBtn = document.createElement("button");
closeBtn.className = "pill-close-btn";
closeBtn.type = "button";
closeBtn.title = "Hide overlay";
closeBtn.setAttribute("aria-label", "Hide overlay");
closeBtn.innerHTML = `
  <svg viewBox="0 0 16 16" width="9" height="9" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" fill="none">
    <line x1="3.5" y1="3.5" x2="12.5" y2="12.5" />
    <line x1="12.5" y1="3.5" x2="3.5" y2="12.5" />
  </svg>
`;
pill.replaceChildren(dotEl, labelEl, closeBtn);

closeBtn.addEventListener("pointerdown", (e) => {
  e.stopPropagation();
});

closeBtn.addEventListener("click", (e) => {
  e.preventDefault();
  e.stopPropagation();
  pill.hidden = true;
  void invoke("hide_overlay").catch(() => undefined);
});

let micPollTimer: number | null = null;
let smoothedLevel = 0;
let lastPhase = "hidden";

function stopMicPolling() {
  if (micPollTimer !== null) {
    clearInterval(micPollTimer);
    micPollTimer = null;
  }
  smoothedLevel = 0;
  dotEl.style.transform = "scale(1)";
}

function startMicPolling() {
  stopMicPolling();
  micPollTimer = window.setInterval(async () => {
    try {
      const level = await invoke<number>("get_mic_level");
      smoothedLevel = Math.max(level, smoothedLevel * 0.78);
      const scale = 1.0 + Math.min(0.38, smoothedLevel * 1.5);
      dotEl.style.transform = `scale(${scale.toFixed(3)})`;
    } catch {
      // Window closing or inactive
    }
  }, 40);
}

function applySmoothState(
  pillClasses: string,
  dotClasses: string,
  labelText: string,
  ariaLabel: string,
  isIdle: boolean
) {
  const wasHidden = pill.hidden || lastPhase === "hidden";
  const prevClasses = pill.className || "pill idle";
  const currentWidth = wasHidden
    ? IDLE_SIZE
    : Math.round(pill.getBoundingClientRect().width) || IDLE_SIZE;
  const prevLabel = labelEl.textContent || "";

  pill.hidden = false;
  pill.setAttribute("aria-label", ariaLabel);

  // 1. Temporarily disable transitions to measure the exact target width
  pill.style.transition = "none";
  labelEl.textContent = isIdle ? "" : labelText;
  pill.className = pillClasses;

  let targetWidth = IDLE_SIZE;
  if (!isIdle) {
    pill.style.width = "max-content";
    targetWidth = Math.max(128, Math.min(580, Math.ceil(pill.getBoundingClientRect().width)));
  }

  // Ensure the host overlay canvas is wide enough (at least 120 so idle hover expansion never clips)
  void invoke("set_overlay_width", { width: Math.max(120, targetWidth + SHADOW_ROOM) }).catch(() => undefined);

  if (wasHidden) {
    if (isIdle) {
      labelEl.textContent = "";
    }
    dotEl.className = dotClasses;
    pill.style.width = `${targetWidth}px`;
    void pill.offsetWidth;
    pill.style.transition = "";
    return;
  }

  // 2. Restore exact previous visual state & width and flush layout
  pill.className = prevClasses;
  pill.style.width = `${currentWidth}px`;
  void pill.offsetWidth;

  // 3. Re-enable transitions and smoothly animate all properties (width, padding, opacity, glass tint, dot) together
  pill.style.transition = "";
  pill.className = pillClasses;
  dotEl.className = dotClasses;
  pill.style.width = `${targetWidth}px`;

  // Smoothly cross-fade label when switching between two active states (e.g., Listening… -> Transcribing…)
  if (!isIdle && prevLabel && prevLabel !== labelText && lastPhase !== "idle" && lastPhase !== "ready") {
    if (typeof labelEl.animate === "function") {
      labelEl.animate(
        [
          { opacity: 0.2, transform: "translateY(2px)" },
          { opacity: 1, transform: "translateY(0)" },
        ],
        { duration: 200, easing: "cubic-bezier(0.22, 1, 0.36, 1)" }
      );
    }
  }
}

function render(payload: Payload) {
  stopMicPolling();

  const phase = (payload.phase || "idle").toLowerCase();

  if (phase === "hidden") {
    pill.hidden = true;
    pill.className = "pill hidden";
    lastPhase = "hidden";
    return;
  }

  switch (phase) {
    case "idle":
    case "ready": {
      applySmoothState(
        "pill idle",
        "status-dot dot-white",
        "",
        "Lilt Ready. Click to dictate.",
        true
      );
      break;
    }

    case "listening": {
      applySmoothState(
        "pill active listening",
        "status-dot dot-red",
        "Listening…",
        "Listening…",
        false
      );
      startMicPolling();
      break;
    }

    case "processing": {
      applySmoothState(
        "pill active transcribing",
        "status-dot dot-yellow",
        "Transcribing…",
        "Transcribing…",
        false
      );
      break;
    }

    case "copied": {
      applySmoothState(
        "pill active copied",
        "status-dot dot-green",
        "Copied",
        "Copied to clipboard.",
        false
      );
      break;
    }

    case "cancelled": {
      applySmoothState(
        "pill idle",
        "status-dot dot-white",
        "",
        "Dictation cancelled.",
        true
      );
      break;
    }

    case "error":
    default: {
      const errorMsg = payload.title && payload.title !== "Dictation failed" ? payload.title : "Try again";
      applySmoothState(
        "pill active error",
        "status-dot dot-red",
        errorMsg,
        errorMsg,
        false
      );
      break;
    }
  }

  lastPhase = phase;
}

let dragStartScreenX = 0;
let dragStartScreenY = 0;
let isDragging = false;
let pointerDownOnPill = false;
let suppressClickUntil = 0;

pill.addEventListener("pointerdown", (e) => {
  if (e.button !== 0) return;
  pointerDownOnPill = true;
  dragStartScreenX = e.screenX;
  dragStartScreenY = e.screenY;
  isDragging = false;
});

window.addEventListener("pointermove", (e) => {
  if (!pointerDownOnPill) return;
  if (!isDragging) {
    const dist = Math.hypot(e.screenX - dragStartScreenX, e.screenY - dragStartScreenY);
    if (dist > 5) {
      isDragging = true;
      pill.classList.add("dragging");
      void invoke("start_overlay_drag").catch(() => undefined);
    }
  }
});

function endDrag() {
  if (isDragging) {
    suppressClickUntil = Date.now() + 300;
    isDragging = false;
    pill.classList.remove("dragging");
  }
  pointerDownOnPill = false;
}

window.addEventListener("pointerup", endDrag);
window.addEventListener("pointercancel", endDrag);

listen("overlay-drag-ended", () => {
  endDrag();
}).catch(() => undefined);

// User click on control triggers state machine transition (only if not dragging)
pill.addEventListener("click", (e) => {
  if (Date.now() < suppressClickUntil || isDragging) {
    e.preventDefault();
    e.stopPropagation();
    return;
  }
  void invoke("dismiss_overlay").catch(() => undefined);
});

listen<Payload>("overlay-phase", event => render(event.payload)).catch(() => undefined);
invoke<Payload>("get_overlay_phase").then(render).catch(() => undefined);
