/**
 * LILT — AWWWARDS-CALIBER INTERACTIVE SHOWCASE ENGINE (v2.0)
 * Inspired by Wispr Flow, Granola, Linear, Raycast & Nothing OS
 */

document.addEventListener("DOMContentLoaded", () => {
  initOSDetectionAndQR();
  initCursorSpecularGlow();
  initScrollRevealAndScramble();
  initHeroWorkspaceStage();
  initDraggablePillSandbox();
  initNothingSilentAlarmWidget();
  initGranolaAutoNotesWidget();
  initBacktrackingPipeline();
  initStickyBottomDock();
});

/* ==========================================================================
   1. SMART OS AUTO-DETECTION & PHONE QR DRAWER
   ========================================================================== */
function initOSDetectionAndQR() {
  const ua = navigator.userAgent || navigator.vendor || window.opera;
  const platform = navigator.platform || "";

  const REPO_URL = "https://github.com/shashankatthaluri/Lilt";
  const WIN_EXE_URL = `${REPO_URL}/releases/latest/download/Lilt-Setup-x64.exe`;
  const ANDROID_APK_URL = `${REPO_URL}/releases/latest/download/Lilt-v1.1-Release.apk`;

  const isAndroid = /android/i.test(ua);
  const isWindows = /win/i.test(platform) || /windows/i.test(ua);
  const isMac = /mac/i.test(platform) && !isAndroid;

  const primaryDlBtn = document.getElementById("hero-primary-dl");
  const primaryLabel = document.getElementById("hero-dl-label");
  const osChip = document.getElementById("hero-os-chip");
  const deviceSummary = document.getElementById("detected-device-summary");
  const navQuickDl = document.getElementById("nav-quick-dl");

  const winCard = document.getElementById("dl-card-windows");
  const andCard = document.getElementById("dl-card-android");
  const winRecBadge = document.getElementById("win-rec-badge");
  const andRecBadge = document.getElementById("and-rec-badge");

  if (isAndroid) {
    if (primaryDlBtn) primaryDlBtn.href = ANDROID_APK_URL;
    if (primaryLabel) primaryLabel.textContent = "Download Lilt for Android";
    if (osChip) osChip.textContent = "APK • v1.1";
    if (deviceSummary) deviceSummary.textContent = "✦ Detected Android — Direct ARM64 APK Ready";
    if (navQuickDl) {
      navQuickDl.textContent = "Get APK";
      navQuickDl.href = ANDROID_APK_URL;
    }
    if (andCard) andCard.classList.add("recommended-os");
    if (andRecBadge) {
      andRecBadge.textContent = "★ RECOMMENDED FOR YOUR PHONE";
      andRecBadge.style.color = "#FFB800";
    }
  } else if (isWindows) {
    if (primaryDlBtn) primaryDlBtn.href = WIN_EXE_URL;
    if (primaryLabel) primaryLabel.textContent = "Download Lilt for Windows";
    if (osChip) osChip.textContent = "WIN 10/11 • X64";
    if (deviceSummary) deviceSummary.textContent = "✦ Detected Windows (64-bit) — Native Rust Overlay Ready";
    if (navQuickDl) {
      navQuickDl.textContent = "Download .exe";
      navQuickDl.href = WIN_EXE_URL;
    }
    if (winCard) winCard.classList.add("recommended-os");
    if (winRecBadge) {
      winRecBadge.textContent = "★ DETECTED OS";
      winRecBadge.style.color = "#FFB800";
    }
  } else {
    if (primaryDlBtn) primaryDlBtn.href = `${REPO_URL}/releases`;
    if (primaryLabel) primaryLabel.textContent = "Download Lilt Releases";
    if (osChip) osChip.textContent = isMac ? "WIN & ANDROID" : "ALL PLATFORMS";
    if (deviceSummary) deviceSummary.textContent = "✦ Available for Windows 10/11 & Android 10+";
  }

  // Phone Companion QR Toggle
  const toggleQrBtn = document.getElementById("toggle-qr-btn");
  const qrDrawer = document.getElementById("hero-qr-drawer");
  const qrBox = document.getElementById("hero-qr-box");

  if (qrBox) {
    const qrImg = document.createElement("img");
    qrImg.alt = "Scan to download Lilt Android APK";
    qrImg.width = 78;
    qrImg.height = 78;
    qrImg.src = `https://api.qrserver.com/v1/create-qr-code/?size=160x160&color=08080A&bgcolor=FFFFFF&data=${encodeURIComponent(ANDROID_APK_URL)}`;
    qrBox.appendChild(qrImg);
  }

  if (toggleQrBtn && qrDrawer) {
    toggleQrBtn.addEventListener("click", () => {
      qrDrawer.classList.toggle("is-open");
    });
  }
}

/* ==========================================================================
   2. LINEAR / RAYCAST CURSOR-TRACKED SPECULAR GLOW
   ========================================================================== */
function initCursorSpecularGlow() {
  const cards = document.querySelectorAll(".bento-card, .dl-platform-card");
  cards.forEach((card) => {
    card.addEventListener("mousemove", (e) => {
      const rect = card.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      card.style.setProperty("--mouse-x", `${x}px`);
      card.style.setProperty("--mouse-y", `${y}px`);
    });
  });
}

/* ==========================================================================
   3. SCROLL REVEAL & NOTHING OS DOT-MATRIX TEXT SCRAMBLE
   ========================================================================== */
function initScrollRevealAndScramble() {
  const GLYPHS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ•/:⚡";

  function scrambleElement(el) {
    const target = el.getAttribute("data-scramble") || el.textContent;
    let iteration = 0;
    const totalIterations = target.length;

    const interval = setInterval(() => {
      el.textContent = target
        .split("")
        .map((char, idx) => {
          if (char === " ") return " ";
          if (idx < iteration) return target[idx];
          return GLYPHS[Math.floor(Math.random() * GLYPHS.length)];
        })
        .join("");

      iteration += Math.max(0.5, totalIterations / 18);
      if (iteration >= totalIterations) {
        el.textContent = target;
        clearInterval(interval);
      }
    }, 24);
  }

  const revealEls = document.querySelectorAll(".reveal-up");
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("in-view");
          const scrambleTargets = entry.target.querySelectorAll("[data-scramble]");
          scrambleTargets.forEach((el) => {
            if (!el.dataset.scrambledOnce) {
              el.dataset.scrambledOnce = "true";
              scrambleElement(el);
            }
          });
        }
      });
    },
    { threshold: 0.14 }
  );

  revealEls.forEach((el) => observer.observe(el));

  // Scramble hero eyebrow on load
  const heroEyebrow = document.querySelector(".hero [data-scramble]");
  if (heroEyebrow) {
    setTimeout(() => scrambleElement(heroEyebrow), 260);
  }
}

/* ==========================================================================
   4. HERO MULTI-APP WORKSPACE STAGE + 60FPS SPECTRUM PILL + [SPACE] TRIGGER
   ========================================================================== */
function initHeroWorkspaceStage() {
  const WORKSPACE_SCENARIOS = {
    whatsapp: {
      avatar: "VK",
      title: "Venkatesh • Gachibowli",
      sub: "WhatsApp Direct • Indian Contact & Tenglish Bias Active",
      badge: "✓ ZERO-CONFLICT OVERLAY",
      rawHtml: `
        <span>Raw Stream:</span>
        <span style="color: var(--text-secondary);">"Hey Venkatesh let's meet at Banjara Hills at 4 PM..."</span>
        <span class="struck-disfluency">kaadu kaadu</span>
        <span class="backtrack-badge">↺ BACKTRACKED</span>
        <span style="color: var(--text-pure);">"make it Gachibowli at 5:30 PM and bring five thousand rupees"</span>
      `,
      finalOutput: "Hey Venkatesh, let's meet at Gachibowli at 5:30 PM and bring ₹5,000."
    },
    slack: {
      avatar: "#P",
      title: "#product-launch • Slack",
      sub: "Desktop Overlay • Mid-Sentence Self-Correction",
      badge: "⚡ 3.8ms RUST CLEANUP",
      rawHtml: `
        <span>Raw Stream:</span>
        <span style="color: var(--text-secondary);">"Team we are shipping v1.1 on..."</span>
        <span class="struck-disfluency">Friday afternoon umm no wait scratch that</span>
        <span class="backtrack-badge">↺ BACKTRACKED</span>
        <span style="color: var(--text-pure);">"Thursday at 9 AM IST after passing QA."</span>
      `,
      finalOutput: "Team, we are shipping v1.1 on Thursday at 9:00 AM IST after passing QA."
    },
    cursor: {
      avatar: "RS",
      title: "Cursor IDE • speech_engine.rs",
      sub: "Developer Dictation • Technical Symbol & Snake_Case Aware",
      badge: "🦀 NATIVE TAURI HOOK",
      rawHtml: `
        <span>Raw Stream:</span>
        <span style="color: var(--text-secondary);">"Refactor parse temporal intent..."</span>
        <span class="struck-disfluency">to return a boolean... wait no</span>
        <span class="backtrack-badge">↺ BACKTRACKED</span>
        <span style="color: var(--text-pure);">"to trigger silent lockscreen alarm when snooze stepper is set."</span>
      `,
      finalOutput: "// TODO: Refactor `parse_temporal_intent` to trigger silent lockscreen alarm when snooze stepper is set."
    },
    notes: {
      avatar: "📌",
      title: "Lilt Auto Note Taker",
      sub: "Voice Directive ('Take a note...') • Auto-Checklist & Pin",
      badge: "📝 AUTO-STRUCTURED",
      rawHtml: `
        <span>Raw Stream:</span>
        <span style="color: var(--text-secondary);">"Take a note for weekend trip:"</span>
        <span class="struck-disfluency">first buy milk no wait</span>
        <span class="backtrack-badge">✦ LIST EXTRACTED</span>
        <span style="color: var(--text-pure);">"first book flight to Hyderabad, second pack Nothing ear charger, third call Rajesh."</span>
      `,
      finalOutput: "☑ Book flight to Hyderabad  •  ☐ Pack Nothing Ear charger  •  ☐ Call Rajesh"
    }
  };

  let activeAppKey = "whatsapp";
  let isDictating = false;
  let typewriterTimer = null;

  const tabs = document.querySelectorAll(".context-tab");
  const avatarEl = document.getElementById("stage-avatar");
  const titleEl = document.getElementById("stage-app-title");
  const subEl = document.getElementById("stage-app-sub");
  const badgeEl = document.getElementById("stage-mode-badge");
  const rawStreamEl = document.getElementById("stage-raw-stream");
  const cleanEditorEl = document.getElementById("stage-clean-editor");

  const magicPill = document.getElementById("hero-magic-pill");
  const pillStatus = document.getElementById("hero-pill-status");
  const stickyNarration = document.getElementById("sticky-pill-narration");
  const canvas = document.getElementById("pill-spectrum");

  function renderScenarioInstant(appKey) {
    const sc = WORKSPACE_SCENARIOS[appKey];
    if (!sc) return;
    if (typewriterTimer) clearInterval(typewriterTimer);

    avatarEl.textContent = sc.avatar;
    titleEl.textContent = sc.title;
    subEl.textContent = sc.sub;
    badgeEl.textContent = sc.badge;
    rawStreamEl.innerHTML = sc.rawHtml;
    cleanEditorEl.innerHTML = `${sc.finalOutput}<span class="blinking-cursor"></span>`;
  }

  function runLiveDictationSimulation() {
    if (isDictating) return;
    isDictating = true;

    const sc = WORKSPACE_SCENARIOS[activeAppKey];
    magicPill.classList.add("is-recording");
    pillStatus.textContent = "LISTENING • SPEAK MESSY...";
    if (stickyNarration) stickyNarration.textContent = "🔴 LIVE SIMULATION • CAPTURING & BACKTRACKING...";

    rawStreamEl.innerHTML = `<span>🎙️ Capturing live voice stream...</span> <span style="color: var(--nothing-red);">● REC</span>`;
    cleanEditorEl.innerHTML = `<span style="color: var(--text-muted);">Waiting for speech completion...</span><span class="blinking-cursor"></span>`;

    setTimeout(() => {
      rawStreamEl.innerHTML = sc.rawHtml;
      pillStatus.textContent = "🦀 STRIPPING FALSE STARTS (3.8ms)...";
    }, 900);

    setTimeout(() => {
      magicPill.classList.remove("is-recording");
      pillStatus.textContent = "✓ CLEANED & INSERTED • CLICK OR [SPACE]";
      if (stickyNarration) stickyNarration.textContent = "✓ CLEANED IN 3.8MS • HOLD [SPACE] TO TEST AGAIN";

      // Typewriter injection
      cleanEditorEl.innerHTML = "";
      let idx = 0;
      const text = sc.finalOutput;
      typewriterTimer = setInterval(() => {
        idx += 2;
        cleanEditorEl.innerHTML = `${text.slice(0, idx)}<span class="blinking-cursor"></span>`;
        if (idx >= text.length) {
          clearInterval(typewriterTimer);
          isDictating = false;
        }
      }, 16);
    }, 1650);
  }

  tabs.forEach((tab) => {
    tab.addEventListener("click", () => {
      tabs.forEach((t) => t.classList.remove("active"));
      tab.classList.add("active");
      activeAppKey = tab.getAttribute("data-app");
      isDictating = false;
      magicPill.classList.remove("is-recording");
      pillStatus.textContent = "CLICK PILL OR HOLD [SPACE]";
      renderScenarioInstant(activeAppKey);
      runLiveDictationSimulation();
    });
  });

  if (magicPill) {
    magicPill.addEventListener("click", () => {
      runLiveDictationSimulation();
    });
  }

  // Global [Spacebar] shortcut (unless typing in search input)
  window.addEventListener("keydown", (e) => {
    const tag = (e.target && e.target.tagName) || "";
    if (tag === "INPUT" || tag === "TEXTAREA") return;
    if (e.code === "Space" && !e.repeat) {
      e.preventDefault();
      runLiveDictationSimulation();
    }
  });

  // 60FPS Audio Spectrum Canvas inside the Floating Pill
  if (canvas) {
    const ctx = canvas.getContext("2d");
    const barCount = 18;
    let phase = 0;

    function drawSpectrum() {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      const barWidth = 3;
      const gap = 3;
      const centerY = canvas.height / 2;

      for (let i = 0; i < barCount; i++) {
        const amp = isDictating
          ? 4 + Math.abs(Math.sin(phase * 2.4 + i * 0.55)) * 18
          : 2.5 + Math.abs(Math.sin(phase + i * 0.45)) * 7;

        const x = i * (barWidth + gap) + 2;
        const y = centerY - amp / 2;

        ctx.fillStyle = isDictating
          ? i % 3 === 0
            ? "#FF2800"
            : "#FFB800"
          : "rgba(244, 244, 246, 0.55)";

        ctx.beginPath();
        ctx.roundRect(x, y, barWidth, amp, 2);
        ctx.fill();
      }

      phase += isDictating ? 0.16 : 0.05;
      requestAnimationFrame(drawSpectrum);
    }

    drawSpectrum();
  }
}

/* ==========================================================================
   5. BENTO 1 — PHYSICALLY DRAGGABLE FLOATING PILL SANDBOX
   ========================================================================== */
function initDraggablePillSandbox() {
  const sandbox = document.getElementById("pill-sandbox");
  const pill = document.getElementById("draggable-pill");
  const pillText = document.getElementById("drag-pill-text");
  const coordsReadout = document.getElementById("drag-coords-readout");
  const injectedTarget = document.getElementById("sandbox-injected-target");
  const btnGhost = document.getElementById("btn-toggle-ghost");
  const btnDock = document.getElementById("btn-dock-edge");

  if (!sandbox || !pill) return;

  let dragging = false;
  let movedDistance = 0;
  let startX = 0;
  let startY = 0;
  let isGhost = false;
  let snippetIndex = 0;

  const CODE_SNIPPETS = [
    `  // Dictated: "Refactor auth middleware before Monday standup"`,
    `  // Dictated: "Ensure silent alarm wakes lockscreen without audio"`,
    `  // Dictated: "Bias Whisper prompt with Venkatesh and Gachibowli"`,
    `  // Dictated: "Auto-copy cleaned transcript to system clipboard"`
  ];

  function setPillPosition(xPx, yPx) {
    const rect = sandbox.getBoundingClientRect();
    const pillRect = pill.getBoundingClientRect();

    const minX = pillRect.width / 2 + 10;
    const maxX = rect.width - pillRect.width / 2 - 10;
    const minY = pillRect.height / 2 + 10;
    const maxY = rect.height - pillRect.height / 2 - 36;

    const clampedX = Math.max(minX, Math.min(maxX, xPx));
    const clampedY = Math.max(minY, Math.min(maxY, yPx));

    const pctX = Math.round((clampedX / rect.width) * 100);
    const pctY = Math.round((clampedY / rect.height) * 100);

    pill.style.left = `${pctX}%`;
    pill.style.top = `${pctY}%`;
    if (coordsReadout) {
      coordsReadout.textContent = `POS: X: ${pctX}% • Y: ${pctY}%`;
    }
  }

  pill.addEventListener("pointerdown", (e) => {
    dragging = true;
    movedDistance = 0;
    startX = e.clientX;
    startY = e.clientY;
    pill.setPointerCapture(e.pointerId);
    pill.style.transition = "none";
  });

  pill.addEventListener("pointermove", (e) => {
    if (!dragging) return;
    const dx = e.clientX - startX;
    const dy = e.clientY - startY;
    movedDistance = Math.hypot(dx, dy);

    const rect = sandbox.getBoundingClientRect();
    setPillPosition(e.clientX - rect.left, e.clientY - rect.top);
  });

  pill.addEventListener("pointerup", (e) => {
    if (!dragging) return;
    dragging = false;
    pill.releasePointerCapture(e.pointerId);
    pill.style.transition = "all 0.25s cubic-bezier(0.16, 1, 0.3, 1)";

    // If it was a tap rather than a drag, trigger live code injection
    if (movedDistance < 6) {
      snippetIndex = (snippetIndex + 1) % CODE_SNIPPETS.length;
      if (pillText) pillText.textContent = "✓ INJECTED INTO IDE";
      if (injectedTarget) {
        injectedTarget.textContent = CODE_SNIPPETS[snippetIndex];
        injectedTarget.style.color = "#FFB800";
        setTimeout(() => {
          injectedTarget.style.color = "#4ADE80";
        }, 450);
      }
      setTimeout(() => {
        if (pillText) pillText.textContent = "DRAG ME • TAP TO DICTATE";
      }, 1300);
    }
  });

  if (btnGhost) {
    btnGhost.addEventListener("click", () => {
      isGhost = !isGhost;
      pill.style.opacity = isGhost ? "0.38" : "1";
      btnGhost.textContent = isGhost ? "Ghost: 38%" : "Ghost Opacity";
    });
  }

  if (btnDock) {
    btnDock.addEventListener("click", () => {
      pill.style.transition = "all 0.35s cubic-bezier(0.16, 1, 0.3, 1)";
      pill.style.left = "82%";
      pill.style.top = "36%";
      if (coordsReadout) coordsReadout.textContent = "POS: DOCKED RIGHT EDGE (82%, 36%)";
    });
  }
}

/* ==========================================================================
   6. BENTO 2 — NOTHING OS SILENT ALARM & TACTILE 5-MIN STEPPER
   ========================================================================== */
function initNothingSilentAlarmWidget() {
  let snoozeMinutes = 10;
  let isSilentMode = true;

  const badgePill = document.getElementById("alarm-badge-pill");
  const btnToggleSilent = document.getElementById("btn-toggle-silent-mode");
  const titleEl = document.getElementById("alarm-demo-title");
  const subEl = document.getElementById("alarm-demo-sub");
  const valDisplay = document.getElementById("stepper-val-display");
  const btnMinus = document.getElementById("stepper-minus");
  const btnPlus = document.getElementById("stepper-plus");
  const presetChips = document.querySelectorAll(".preset-chip");
  const btnSnooze = document.getElementById("btn-demo-snooze");
  const btnAck = document.getElementById("btn-demo-ack");

  function formatDuration(mins) {
    if (mins >= 60) {
      const h = Math.floor(mins / 60);
      const rem = mins % 60;
      return rem === 0 ? `${h}h` : `${h}h ${rem}m`;
    }
    return `${mins}m`;
  }

  function syncStepperUI() {
    const label = formatDuration(snoozeMinutes);
    if (valDisplay) valDisplay.textContent = label;
    if (btnSnooze) btnSnooze.textContent = `SNOOZE (+${label})`;

    presetChips.forEach((chip) => {
      const chipMins = parseInt(chip.getAttribute("data-mins"), 10);
      chip.classList.toggle("active", chipMins === snoozeMinutes);
    });
  }

  if (btnToggleSilent) {
    btnToggleSilent.addEventListener("click", () => {
      isSilentMode = !isSilentMode;
      btnToggleSilent.classList.toggle("active", isSilentMode);
      btnToggleSilent.textContent = isSilentMode ? "Silent Mode: ON" : "Silent Mode: OFF";

      if (badgePill) {
        badgePill.textContent = isSilentMode
          ? "🔕 SILENT ALARM (VISUAL)"
          : "🔴 AUDIBLE ALARM (RINGING)";
        badgePill.style.color = isSilentMode ? "#FFB800" : "#FF2800";
        badgePill.style.borderColor = isSilentMode
          ? "rgba(255, 184, 0, 0.35)"
          : "rgba(255, 40, 0, 0.45)";
      }

      if (subEl) {
        subEl.textContent = isSilentMode
          ? "Wakes screen & demands action with zero audio ringtone"
          : "Full-screen lockscreen wakeup + escalating Nothing OS alarm tone";
      }
    });
  }

  if (btnMinus) {
    btnMinus.addEventListener("click", () => {
      snoozeMinutes = Math.max(5, snoozeMinutes - 5);
      syncStepperUI();
    });
  }

  if (btnPlus) {
    btnPlus.addEventListener("click", () => {
      snoozeMinutes = Math.min(180, snoozeMinutes + 5);
      syncStepperUI();
    });
  }

  presetChips.forEach((chip) => {
    chip.addEventListener("click", () => {
      snoozeMinutes = parseInt(chip.getAttribute("data-mins"), 10) || 10;
      syncStepperUI();
    });
  });

  if (btnSnooze) {
    btnSnooze.addEventListener("click", () => {
      const formatted = formatDuration(snoozeMinutes);
      if (titleEl) titleEl.textContent = `⏳ Snoozed for ${formatted}`;
      if (subEl) subEl.textContent = `Alarm will re-wake screen in ${formatted} unless acknowledged`;
      setTimeout(() => {
        if (titleEl) titleEl.textContent = "Call Rajesh regarding Q4 deck";
        if (subEl) {
          subEl.textContent = isSilentMode
            ? "Wakes screen & demands action with zero audio ringtone"
            : "Full-screen lockscreen wakeup + escalating Nothing OS alarm tone";
        }
      }, 2000);
    });
  }

  if (btnAck) {
    btnAck.addEventListener("click", () => {
      if (titleEl) titleEl.textContent = "✓ Alarm Acknowledged & Cleared";
      if (subEl) subEl.textContent = "Marked Done in Lilt Alerts Tab • Next reminder queued";
      setTimeout(() => {
        if (titleEl) titleEl.textContent = "Call Rajesh regarding Q4 deck";
        if (subEl) {
          subEl.textContent = isSilentMode
            ? "Wakes screen & demands action with zero audio ringtone"
            : "Full-screen lockscreen wakeup + escalating Nothing OS alarm tone";
        }
      }, 2000);
    });
  }
}

/* ==========================================================================
   7. BENTO 3 — GRANOLA-STYLE AUTO NOTE TAKER + PIN + AMBER SEARCH
   ========================================================================== */
function initGranolaAutoNotesWidget() {
  const notesData = [
    {
      id: "note-1",
      title: "Hyderabad Sprint & Groceries",
      time: "JUST NOW • VOICE NOTE",
      isPinned: true,
      items: [
        { text: "Buy organic almond milk and cold brew", done: true },
        { text: "Send ₹5,000 advance to Venkatesh in Gachibowli", done: false },
        { text: "Review silent alarm lockscreen PR", done: false }
      ]
    },
    {
      id: "note-2",
      title: "Flight & Release Checklist",
      time: "14 MINS AGO • AUTO LIST",
      isPinned: false,
      items: [
        { text: "Book morning flight to Bengaluru (BLR)", done: true },
        { text: "Publish Lilt v1.1 APK & Windows installer on GitHub", done: true },
        { text: "Test Tenglish kaadu kaadu backtracking", done: false }
      ]
    },
    {
      id: "note-3",
      title: "Quick Contact Capture",
      time: "1 HR AGO • PHONE EXTRACT",
      isPinned: false,
      items: [
        { text: "Save Rajeshwar Rao (+91 98480 22338) to Contacts", done: false }
      ]
    }
  ];

  const searchInput = document.getElementById("bento-note-search");
  const container = document.getElementById("bento-notes-container");
  if (!container) return;

  function highlightQuery(text, query) {
    if (!query) return text;
    const escaped = query.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const regex = new RegExp(`(${escaped})`, "gi");
    return text.replace(regex, `<mark class="amber-hl">$1</mark>`);
  }

  function renderNotes() {
    const query = (searchInput ? searchInput.value : "").trim();
    const sorted = [...notesData].sort((a, b) => Number(b.isPinned) - Number(a.isPinned));

    const filtered = sorted.filter((note) => {
      if (!query) return true;
      const q = query.toLowerCase();
      const inTitle = note.title.toLowerCase().includes(q);
      const inItems = note.items.some((it) => it.text.toLowerCase().includes(q));
      return inTitle || inItems;
    });

    if (filtered.length === 0) {
      container.innerHTML = `
        <div style="text-align:center; padding: 24px 12px; color: var(--text-muted); font-family: var(--font-mono); font-size: 12px;">
          No notes matching "${query}". Try "milk", "flight", or "Venkatesh".
        </div>
      `;
      return;
    }

    container.innerHTML = filtered
      .map(
        (note) => `
        <div class="mini-note-item ${note.isPinned ? "is-pinned" : ""}" data-id="${note.id}">
          <div class="note-item-head">
            <div>
              <strong style="font-size: 13px; color: #FFF;">${highlightQuery(note.title, query)}</strong>
              <span style="display:block; font-family: var(--font-mono); font-size: 9.5px; color: var(--text-muted); margin-top: 2px;">
                ${note.isPinned ? "📌 PINNED TO TOP • " : ""}${note.time}
              </span>
            </div>
            <button type="button" class="pin-toggle-btn ${note.isPinned ? "pinned" : ""}" data-pin-id="${note.id}" title="Toggle Pin to Top">
              📌
            </button>
          </div>
          <div class="note-checklist">
            ${note.items
              .map(
                (item, idx) => `
              <div class="check-bullet-row ${item.done ? "checked" : ""}" data-note-id="${note.id}" data-item-idx="${idx}">
                <span class="custom-checkbox">${item.done ? "✓" : ""}</span>
                <span>${highlightQuery(item.text, query)}</span>
              </div>
            `
              )
              .join("")}
          </div>
        </div>
      `
      )
      .join("");

    // Bind Pin buttons
    container.querySelectorAll("[data-pin-id]").forEach((btn) => {
      btn.addEventListener("click", (e) => {
        e.stopPropagation();
        const id = btn.getAttribute("data-pin-id");
        const target = notesData.find((n) => n.id === id);
        if (target) {
          target.isPinned = !target.isPinned;
          renderNotes();
        }
      });
    });

    // Bind Checklist toggles
    container.querySelectorAll(".check-bullet-row").forEach((row) => {
      row.addEventListener("click", () => {
        const noteId = row.getAttribute("data-note-id");
        const itemIdx = parseInt(row.getAttribute("data-item-idx"), 10);
        const target = notesData.find((n) => n.id === noteId);
        if (target && target.items[itemIdx]) {
          target.items[itemIdx].done = !target.items[itemIdx].done;
          renderNotes();
        }
      });
    });
  }

  if (searchInput) {
    searchInput.addEventListener("input", renderNotes);
  }

  renderNotes();
}

/* ==========================================================================
   8. BENTO 4 — KINETIC BACKTRACKING & TENGLISH PIPELINE SWITCHER
   ========================================================================== */
function initBacktrackingPipeline() {
  const PIPELINE_CASES = {
    backtrack: {
      raw: `"Send the client contract by <span style="color:#F87171;text-decoration:line-through;">Friday evening... no wait, scratch that,</span> Monday 10 AM."`,
      clean: `"Send the client contract by Monday 10:00 AM."`
    },
    tenglish: {
      raw: `"Mama let's meet at <span style="color:#F87171;text-decoration:line-through;">Jubilee Hills... kaadu kaadu,</span> Gachibowli DLF gate near 7 PM."`,
      clean: `"Mama, let's meet at Gachibowli DLF Gate near 7:00 PM."`
    },
    currency: {
      raw: `"Transfer <span style="color:#FFB800;">forty five thousand five hundred rupees</span> to <span style="color:#FFB800;">shashank dot dev at gmail dot com</span>"`,
      clean: `"Transfer ₹45,500 to shashank.dev@gmail.com."`
    },
    stutter: {
      raw: `"<span style="color:#F87171;text-decoration:line-through;">I I I think we should umm</span> deploy the Android release APK right now."`,
      clean: `"I think we should deploy the Android release APK right now."`
    }
  };

  const buttons = document.querySelectorAll(".scenario-pill");
  const rawBox = document.getElementById("pipe-raw-text");
  const cleanBox = document.getElementById("pipe-clean-text");

  buttons.forEach((btn) => {
    btn.addEventListener("click", () => {
      buttons.forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      const key = btn.getAttribute("data-sc");
      const data = PIPELINE_CASES[key];
      if (data && rawBox && cleanBox) {
        rawBox.innerHTML = data.raw;
        cleanBox.innerHTML = data.clean;
      }
    });
  });
}

/* ==========================================================================
   9. STICKY BOTTOM FLOATING PILL DOCK (SCROLL-AWARE NARRATION)
   ========================================================================== */
function initStickyBottomDock() {
  const dock = document.getElementById("sticky-bottom-dock");
  const narration = document.getElementById("sticky-pill-narration");
  if (!dock) return;

  window.addEventListener("scroll", () => {
    const y = window.scrollY;
    if (y > 420) {
      dock.classList.add("visible");
    } else {
      dock.classList.remove("visible");
    }

    if (!narration) return;
    if (y > 1900) {
      narration.textContent = "✦ READY TO INSTALL • WINDOWS (.EXE) & ANDROID (.APK)";
    } else if (y > 1200) {
      narration.textContent = "📌 AUTO NOTE TAKER • PIN TO TOP & AMBER SEARCH";
    } else if (y > 650) {
      narration.textContent = "🔕 PERSISTENT SILENT ALARMS • 5M TACTILE STEPPER";
    } else {
      narration.textContent = "LILT OVERLAY READY • HOLD [SPACE] TO TEST";
    }
  });
}
