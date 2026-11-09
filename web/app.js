/* =========================================================================
   LILT — WEBSITE v3 · interaction & motion
   GSAP + ScrollTrigger + Lenis (progressive enhancement: the page works
   and stays fully readable if the CDN scripts fail to load).
   ========================================================================= */

(() => {
  "use strict";

  const hasGSAP = typeof window.gsap !== "undefined" && typeof window.ScrollTrigger !== "undefined";
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const finePointer = window.matchMedia("(pointer: fine)").matches;
  const animate = hasGSAP && !reduceMotion;

  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  const clamp = (v, a, b) => Math.min(b, Math.max(a, v));

  const REPO = "https://github.com/shashankatthaluri/Lilt";
  const WIN_URL = `${REPO}/releases/latest/download/Lilt-Setup-x64.exe`;
  const APK_URL = `${REPO}/releases/latest/download/Lilt-v1.1-Release.apk`;

  if (animate) {
    document.documentElement.classList.add("anim-ready");
    gsap.registerPlugin(ScrollTrigger);
  }
  document.documentElement.classList.remove("js-loading");

  let lenis = null;

  /* =====================================================================
     WAVEFORM RENDERER (shared by every pill on the page)
     ===================================================================== */
  const waves = [];
  function makeWave(canvas, { bars = 16, color = "#f4f2ed", active = "#ff3b1f" } = {}) {
    if (!canvas) return { set() {} };
    const ctx = canvas.getContext("2d");
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const w = canvas.width, h = canvas.height;
    canvas.width = w * dpr; canvas.height = h * dpr;
    canvas.style.width = w + "px"; canvas.style.height = h + "px";
    ctx.scale(dpr, dpr);
    const state = { level: 0.15, target: 0.15, hot: false, heights: new Array(bars).fill(2) };
    const wave = {
      set(level, hot = false) { state.target = level; state.hot = hot; },
      draw(t) {
        state.level += (state.target - state.level) * 0.08;
        ctx.clearRect(0, 0, w, h);
        const gap = w / bars;
        const bw = Math.max(2, gap * 0.45);
        for (let i = 0; i < bars; i++) {
          const n = Math.sin(t * 0.006 + i * 0.7) * 0.5 + Math.sin(t * 0.011 + i * 1.9) * 0.35 + Math.sin(t * 0.017 + i * 0.3) * 0.15;
          const env = Math.sin((i / (bars - 1)) * Math.PI) * 0.7 + 0.3;
          const target = 2 + Math.abs(n) * env * (h - 3) * state.level;
          state.heights[i] += (target - state.heights[i]) * 0.3;
          const bh = state.heights[i];
          ctx.fillStyle = state.hot ? (i % 4 === 0 ? "#ffb547" : active) : color;
          ctx.globalAlpha = state.hot ? 1 : 0.55;
          const x = i * gap + (gap - bw) / 2, y = (h - bh) / 2;
          if (ctx.roundRect) { ctx.beginPath(); ctx.roundRect(x, y, bw, bh, bw / 2); ctx.fill(); }
          else ctx.fillRect(x, y, bw, bh);
        }
      }
    };
    waves.push(wave);
    return wave;
  }
  (function loop(t) {
    waves.forEach((w) => w.draw(t));
    requestAnimationFrame(loop);
  })(0);

  /* =====================================================================
     OS DETECTION + QR
     ===================================================================== */
  function initOS() {
    const ua = navigator.userAgent || "";
    const isAndroid = /android/i.test(ua);
    const isIOS = /iphone|ipad|ipod/i.test(ua);
    const isMac = !isIOS && /macintosh|mac os x/i.test(ua);
    const isWindows = !isAndroid && /windows/i.test(ua);

    const label = $("#hero-download-label");
    const icon = $("#hero-os-icon");
    const heroBtn = $("#hero-download");
    const detected = $("#os-detected");

    if (isAndroid) {
      label.textContent = "Download for Android (Universal APK)";
      icon.innerHTML = '<img src="assets/icons/android.svg" alt="" />';
      heroBtn.href = APK_URL;
      $("#dl-android").classList.add("is-recommended");
      detected.textContent = "Detected Android — works on Samsung, Pixel, OnePlus, Xiaomi, Nothing & all Android 10+ phones.";
    } else if (isWindows) {
      label.textContent = "Download for Windows";
      icon.innerHTML = '<img src="assets/icons/windows.svg" alt="" />';
      heroBtn.href = WIN_URL;
      $("#dl-windows").classList.add("is-recommended");
      detected.textContent = "Detected Windows — your native Liquid Glass overlay installer is ready.";
    } else if (isIOS || isMac) {
      const iosCard = $("#dl-ios");
      if (iosCard) iosCard.classList.add("is-recommended");
      detected.textContent = "Detected Apple device — iOS & macOS are coming soon! Available today for Windows and Android.";
    } else {
      detected.textContent = "Free & open source for Windows and Android. iOS & macOS coming soon.";
    }

    const qrBox = $("#qr-box");
    const img = new Image();
    img.alt = "QR code to download the Lilt Android APK";
    img.loading = "lazy";
    img.src = `https://api.qrserver.com/v1/create-qr-code/?size=220x220&margin=0&data=${encodeURIComponent(APK_URL)}`;
    qrBox.appendChild(img);

    $("#qr-toggle").addEventListener("click", () => {
      $("#qr-panel").classList.toggle("is-open");
      setTimeout(() => hasGSAP && ScrollTrigger.refresh(), 520);
    });
  }

  /* =====================================================================
     SMOOTH SCROLL (Lenis) + anchor links
     ===================================================================== */
  function initScroll() {
    if (animate && typeof window.Lenis !== "undefined") {
      lenis = new Lenis({ lerp: 0.09, smoothWheel: true });
      lenis.on("scroll", ScrollTrigger.update);
      gsap.ticker.add((t) => lenis.raf(t * 1000));
      gsap.ticker.lagSmoothing(0);
    }

    $$('a[href^="#"]').forEach((a) => {
      a.addEventListener("click", (e) => {
        const id = a.getAttribute("href");
        if (id.length < 2) return;
        const target = $(id);
        if (!target) return;
        e.preventDefault();
        if (lenis) lenis.scrollTo(target, { offset: id === "#top" ? 0 : -10, duration: 1.4 });
        else target.scrollIntoView({ behavior: reduceMotion ? "auto" : "smooth" });
      });
    });
  }

  /* =====================================================================
     NAV — stays fixed at top always, deepens glass on scroll
     ===================================================================== */
  function initNav() {
    const nav = $("#nav");
    const onScroll = () => {
      nav.classList.toggle("is-scrolled", window.scrollY > 24);
    };
    window.addEventListener("scroll", onScroll, { passive: true });
    onScroll();
  }

  /* =====================================================================
     CUSTOM CURSOR + MAGNETIC BUTTONS + CARD SPOTLIGHT
     ===================================================================== */
  function initPointer() {
    if (!finePointer) return;
    const dot = $("#cursor-dot");
    const cursor = $("#cursor");
    const label = $("#cursor-label");
    let x = -100, y = -100, cx = -100, cy = -100;
    let movedOnce = false;

    window.addEventListener("pointermove", (e) => {
      x = e.clientX;
      y = e.clientY;
      if (!movedOnce) {
        movedOnce = true;
        cx = x;
        cy = y;
        dot.style.opacity = "1";
        cursor.style.opacity = "1";
      }
      if (dot) dot.style.transform = `translate3d(${x}px, ${y}px, 0)`;
    }, { passive: true });

    let lastTime = performance.now();
    (function follow(now) {
      const dt = Math.min(32, now - lastTime);
      lastTime = now;
      // Frame-rate independent spring lerp (~60-144Hz smooth)
      const alpha = 1 - Math.pow(0.0006, dt / 1000);
      cx += (x - cx) * alpha;
      cy += (y - cy) * alpha;
      cursor.style.transform = `translate3d(${cx.toFixed(2)}px, ${cy.toFixed(2)}px, 0)`;
      requestAnimationFrame(follow);
    })(lastTime);

    document.addEventListener("mouseover", (e) => {
      const lbl = e.target.closest("[data-cursor]");
      const hov = e.target.closest("a, button, input, summary, label");
      cursor.classList.toggle("has-label", !!lbl);
      cursor.classList.toggle("is-hover", !lbl && !!hov);
      if (dot) dot.classList.toggle("is-hidden-dot", !!lbl);
      label.textContent = lbl ? lbl.getAttribute("data-cursor") : "";
    });
    document.addEventListener("mouseleave", () => {
      cursor.style.opacity = "0";
      if (dot) dot.style.opacity = "0";
    });
    document.addEventListener("mouseenter", () => {
      if (movedOnce) {
        cursor.style.opacity = "1";
        if (dot) dot.style.opacity = "1";
      }
    });

    // Magnetic buttons
    $$(".magnetic").forEach((el) => {
      el.addEventListener("mousemove", (e) => {
        const r = el.getBoundingClientRect();
        const dx = e.clientX - (r.left + r.width / 2);
        const dy = e.clientY - (r.top + r.height / 2);
        el.style.transform = `translate(${dx * 0.22}px, ${dy * 0.3}px)`;
      });
      el.addEventListener("mouseleave", () => {
        el.style.transition = "transform 0.6s cubic-bezier(0.16,1,0.3,1), background 0.3s, box-shadow 0.3s";
        el.style.transform = "";
        setTimeout(() => (el.style.transition = ""), 600);
      });
    });

    // Feature card spotlight
    $$(".f-card").forEach((c) => {
      c.addEventListener("mousemove", (e) => {
        const r = c.getBoundingClientRect();
        c.style.setProperty("--mx", `${e.clientX - r.left}px`);
        c.style.setProperty("--my", `${e.clientY - r.top}px`);
      });
    });

    // Hero glow follows the mouse
    const glow = $("#hero-glow");
    const hero = $(".hero");
    let gx = 0, gy = 0, tx = 0, ty = 0;
    hero.addEventListener("mousemove", (e) => {
      const r = hero.getBoundingClientRect();
      tx = (e.clientX - r.width / 2) * 0.25;
      ty = (e.clientY - r.top - r.height * 0.4) * 0.25;
    });
    (function drift() {
      gx += (tx - gx) * 0.05; gy += (ty - gy) * 0.05;
      glow.style.transform = `translate(calc(-50% + ${gx}px), calc(-50% + ${gy}px))`;
      requestAnimationFrame(drift);
    })();
  }

  /* =====================================================================
     HERO — live "messy → clean" loop
     ===================================================================== */
  const HERO_PHRASES = [
    { raw: ["~so", "~um", "hey", "can", "we", "push", "the", "call", "to", "~Friday", "~no", "~wait", "~scratch", "~that", "Monday", "morning"], clean: "Hey, can we push the call to Monday morning?" },
    { raw: ["~Romanized", "~Telugu", "repu", "morning", "ten", "ki", "team", "meeting", "undi"], clean: "Repu morning 10:00 ki team meeting undi." },
    { raw: ["~uh", "send", "five", "thousand", "rupees", "to", "ravi", "at", "gmail", "dot", "com"], clean: "Send ₹5,000 to ravi@gmail.com." },
    { raw: ["~Romanized", "~Hindi", "kal", "shaam", "paanch", "baje", "call", "karte", "hain"], clean: "Kal shaam 5:00 baje call karte hain." },
    { raw: ["~like", "I", "~I", "~I", "think", "the", "new", "build", "is", "ready", "~you", "~know"], clean: "I think the new build is ready." }
  ];

  function initHeroDemo() {
    const rawEl = $("#hero-raw");
    const cleanEl = $("#hero-clean");
    const pillStack = $("#hero-pill");
    const winPill = $("#hero-win-pill");
    const winDot = $("#hero-win-dot");
    const status = $("#hero-pill-status");
    let gen = 0;
    let idx = 0;

    function setHeroState(phase) {
      if (!winPill || !winDot || !status) return;
      winDot.className = "lilt-status-dot";
      if (phase === "idle") {
        winPill.classList.add("is-idle");
        winDot.classList.add("dot-white");
        status.textContent = "";
      } else if (phase === "listening") {
        winPill.classList.remove("is-idle");
        winDot.classList.add("dot-red");
        status.textContent = "Listening…";
      } else if (phase === "transcribing") {
        winPill.classList.remove("is-idle");
        winDot.classList.add("dot-yellow");
        status.textContent = "Transcribing…";
      } else if (phase === "copied") {
        winPill.classList.remove("is-idle");
        winDot.classList.add("dot-green");
        status.textContent = "Copied";
      }
    }

    if (reduceMotion) {
      const p = HERO_PHRASES[0];
      rawEl.innerHTML = p.raw.map((w) => `<span class="w in ${w[0] === "~" ? "cut" : ""}">${w.replace("~", "")}</span>`).join("");
      cleanEl.textContent = p.clean;
      setHeroState("copied");
      return;
    }

    async function run(g) {
      while (g === gen) {
        const p = HERO_PHRASES[idx % HERO_PHRASES.length];
        idx++;

        // 0 — brief idle circle (White dot)
        setHeroState("idle");
        await sleep(550);
        if (g !== gen) return;

        // 1 — listening (Red dot + "Listening…")
        setHeroState("listening");
        cleanEl.innerHTML = "";
        rawEl.innerHTML = p.raw.map((w) => `<span class="w${w[0] === "~" ? " x" : ""}">${w.replace("~", "")}</span>`).join("");
        const words = $$(".w", rawEl);
        for (const w of words) {
          if (g !== gen) return;
          w.classList.add("in");
          await sleep(115 + Math.random() * 70);
        }
        await sleep(350);
        if (g !== gen) return;

        // 2 — transcribing / cleaning (Yellow dot + "Transcribing…")
        setHeroState("transcribing");
        const cuts = $$(".w.x", rawEl);
        for (const c of cuts) {
          if (g !== gen) return;
          c.classList.add("cut");
          await sleep(85);
        }
        await sleep(650);
        cuts.forEach((c) => c.classList.add("gone"));
        await sleep(420);
        if (g !== gen) return;

        // 3 — copied / inserted (Green dot + "Copied")
        setHeroState("copied");
        cleanEl.innerHTML = p.clean.split(/(\s+)/).map((t) => `<span class="cw">${t}</span>`).join("");
        const cws = $$(".cw", cleanEl);
        for (const c of cws) {
          if (g !== gen) return;
          c.classList.add("in");
          await sleep(38);
        }
        await sleep(2300);
        if (g !== gen) return;
        cws.forEach((c) => c.classList.remove("in"));
        $$(".w", rawEl).forEach((w) => w.classList.remove("in"));
      }
    }

    pillStack.addEventListener("click", () => { gen++; run(gen); });
    window.addEventListener("keydown", (e) => {
      if (e.code !== "Space" || e.repeat) return;
      const tag = (e.target.tagName || "").toLowerCase();
      if (["input", "textarea", "button", "summary", "a"].includes(tag)) return;
      if (window.scrollY > innerHeight * 0.8) return;
      e.preventDefault();
      gen++; run(gen);
    });

    // start after preloader
    return () => { gen++; run(gen); };
  }

  /* =====================================================================
     PRELOADER + HERO INTRO
     ===================================================================== */
  function initPreloader(onDone) {
    const pre = $("#preloader");
    const count = $("#pre-count");
    const start = performance.now();
    const dur = reduceMotion ? 200 : 1100;
    (function tick(now) {
      const p = clamp((now - start) / dur, 0, 1);
      const eased = 1 - Math.pow(1 - p, 3);
      count.textContent = Math.round(eased * 100);
      if (p < 1) requestAnimationFrame(tick);
      else {
        pre.classList.add("is-done");
        onDone();
      }
    })(start);
  }

  function heroIntro() {
    if (!animate) return;
    const tl = gsap.timeline({ defaults: { ease: "expo.out" } });
    tl.fromTo(".hero-title .line-inner", { y: 0, yPercent: 110 }, { y: 0, yPercent: 0, duration: 1.4, stagger: 0.12 })
      .fromTo(".intro-item", { y: 24, opacity: 0 }, { y: 0, opacity: 1, duration: 1.2, stagger: 0.09 }, 0.35)
      .fromTo(".hero-grid", { opacity: 0 }, { opacity: 1, duration: 2 }, 0)
      .fromTo(".hero-glow", { opacity: 0, scale: 0.7 }, { opacity: 1, scale: 1, duration: 2.4 }, 0);
  }

  /* =====================================================================
     SCROLL ANIMATIONS
     ===================================================================== */
  function initScrollFX() {
    if (!animate) return;

    // Hero parallax out
    gsap.to(".hero-inner", {
      yPercent: -12, opacity: 0.15, ease: "none",
      scrollTrigger: { trigger: ".hero", start: "top top", end: "bottom top", scrub: true }
    });
    gsap.to(".hero-grid", {
      yPercent: 25, ease: "none",
      scrollTrigger: { trigger: ".hero", start: "top top", end: "bottom top", scrub: true }
    });

    // Generic reveals
    ScrollTrigger.batch(".reveal", {
      start: "top 88%",
      once: true,
      onEnter: (batch) => gsap.to(batch, { opacity: 1, y: 0, duration: 1.1, ease: "expo.out", stagger: 0.09, overwrite: true })
    });

    // Eyebrows slide
    $$(".eyebrow").forEach((el) => {
      gsap.from(el, { x: -20, opacity: 0, duration: 1, ease: "expo.out", scrollTrigger: { trigger: el, start: "top 90%", once: true } });
    });

    // Desk scales up into place
    gsap.fromTo("#desk", { scale: 0.9, rotateX: 8, transformPerspective: 1400 }, {
      scale: 1, rotateX: 0, ease: "none",
      scrollTrigger: { trigger: "#desk", start: "top 95%", end: "top 35%", scrub: true }
    });

    // Phone floats in
    gsap.fromTo("#alarm-phone", { y: 120, rotate: 7 }, {
      y: 0, rotate: 0, ease: "none",
      scrollTrigger: { trigger: ".alarms", start: "top bottom", end: "center center", scrub: true }
    });

    // Notes app tilt
    gsap.fromTo("#notes-app", { rotate: -3, y: 60 }, {
      rotate: 0, y: 0, ease: "none",
      scrollTrigger: { trigger: ".notes", start: "top bottom", end: "center center", scrub: true }
    });

    // Download cards
    gsap.fromTo(".dl-card", { y: 80 }, {
      y: 0, stagger: 0.1, ease: "none",
      scrollTrigger: { trigger: ".dl-grid", start: "top bottom", end: "top 50%", scrub: true }
    });

    // Final title scale
    gsap.fromTo(".final-title", { scale: 0.86 }, {
      scale: 1, ease: "none",
      scrollTrigger: { trigger: ".final", start: "top bottom", end: "center center", scrub: true }
    });

    // Giant footer wordmark rises
    gsap.fromTo("#footer-word", { yPercent: 40, opacity: 0.2 }, {
      yPercent: 0, opacity: 1, ease: "none",
      scrollTrigger: { trigger: ".footer", start: "top bottom", end: "bottom bottom", scrub: true }
    });
  }

  /* =====================================================================
     MANIFESTO — words light up as you scroll
     ===================================================================== */
  function initManifesto() {
    const el = $("#manifesto-text");
    const serifWords = new Set(["friend", "meant,", "said."]);
    const words = el.textContent.trim().split(/\s+/);
    el.innerHTML = words.map((w) => `<span class="mw${serifWords.has(w) ? " serif-word" : ""}">${w}</span>`).join(" ");
    const spans = $$(".mw", el);

    if (!animate) { spans.forEach((s) => (s.style.color = "var(--text)")); return; }

    ScrollTrigger.create({
      trigger: "#manifesto",
      start: "top 70%",
      end: "bottom 60%",
      scrub: true,
      onUpdate: (self) => {
        const lit = Math.floor(self.progress * spans.length * 1.05);
        spans.forEach((s, i) => {
          s.style.color = i < lit ? (s.classList.contains("serif-word") ? "#ff6a4d" : "var(--text)") : "";
        });
      }
    });
  }

  /* =====================================================================
     HOW IT WORKS — pinned scrollytelling
     ===================================================================== */
  function initHow() {
    const toks = $$("#how-transcript .tok");
    const fillers = toks.filter((t) => t.classList.contains("f"));
    const backs = toks.filter((t) => t.classList.contains("b"));
    const steps = $$("#how-steps .how-step");
    const bar = $("#how-progress-bar");
    const out = $("#how-out");
    const legend = $("#how-legend");
    const chatSub = $("#chat-sub");
    const inputText = $("#chat-input-text");
    const howMobDot = $("#how-mob-dot");

    out.style.transition = "opacity .5s cubic-bezier(.16,1,.3,1), transform .6s cubic-bezier(.16,1,.3,1)";
    legend.style.transition = "opacity .4s";

    let lastStep = -1;
    function update(p) {
      const step = p < 0.36 ? 0 : p < 0.68 ? 1 : 2;
      if (step !== lastStep) {
        steps.forEach((s, i) => s.classList.toggle("is-active", i === step));
        lastStep = step;
      }
      bar.style.width = `${p * 100}%`;

      // Phase 1: words appear
      const shown = Math.floor(clamp((p - 0.03) / 0.33, 0, 1) * toks.length);
      toks.forEach((t, i) => (t.style.opacity = i < shown ? 1 : 0.1));

      // Phase 2: fillers / backtracks get struck
      const cutF = p > 0.42, cutB = p > 0.5, fade = p > 0.6;
      fillers.forEach((t) => { t.classList.toggle("cut-f", cutF); if (fade) t.style.opacity = 0.3; });
      backs.forEach((t) => { t.classList.toggle("cut-b", cutB); if (fade) t.style.opacity = 0.3; });
      legend.style.opacity = cutF ? 1 : 0;

      // Phase 3: clean message sent
      const sent = p > 0.72;
      out.style.opacity = sent ? 1 : 0;
      out.style.transform = sent ? "none" : "translateY(18px) scale(.94)";

      const listening = p > 0.03 && p < 0.37;
      if (howMobDot) {
        howMobDot.className = "lilt-status-dot " + (listening ? "dot-red" : step === 1 ? "dot-yellow" : sent ? "dot-green" : "dot-white");
      }
      chatSub.textContent = listening ? "Listening (red dot)…" : step === 1 ? "Transcribing (amber dot)…" : sent ? "Copied & sent (green dot)" : "online";
      inputText.textContent = listening ? "Listening…" : step === 1 ? "Transcribing…" : sent ? "Sent ✓" : "Message";
      chatSub.style.color = listening ? "var(--red)" : step === 1 ? "var(--amber)" : sent ? "var(--green)" : "";
    }

    if (!animate) { update(1); return; }
    update(0);

    ScrollTrigger.create({
      trigger: "#how-pin",
      start: "top top",
      end: () => "+=" + innerHeight * 2.6,
      pin: true,
      scrub: true,
      anticipatePin: 1,
      onUpdate: (self) => update(self.progress)
    });
  }

  /* =====================================================================
     FEATURES — horizontal scroll gallery
     ===================================================================== */
  function initFeatures() {
    if (!animate) return;
    const track = $("#h-track");
    const mm = gsap.matchMedia();
    mm.add("(min-width: 761px)", () => {
      const dist = () => Math.max(0, track.scrollWidth - innerWidth);
      const tween = gsap.to(track, {
        x: () => -dist(),
        ease: "none",
        scrollTrigger: {
          trigger: "#features-pin",
          start: "top top",
          end: () => "+=" + dist(),
          pin: true,
          scrub: 0.8,
          invalidateOnRefresh: true,
          anticipatePin: 1
        }
      });
      // cards gently rise as they enter horizontally
      $$(".f-card", track).forEach((card) => {
        gsap.fromTo(card, { y: 60, rotate: 2 }, {
          y: 0, rotate: 0, ease: "none",
          scrollTrigger: { trigger: card, containerAnimation: tween, start: "left 100%", end: "left 60%", scrub: true }
        });
      });
    });
  }

  /* =====================================================================
     FLOATING PILL — live automatic loop + draggable Windows Pill & Mobile Circle
     ===================================================================== */
  function initFloatPill() {
    const screen = $("#desk-screen");
    const pill = $("#float-pill");
    const dot = $("#float-dot");
    const status = $("#float-status");
    const posEl = $("#float-pos");
    const tabs = $$(".desk-tab");
    const views = $$(".app-view");
    const modeBtns = $$("#pill-mode-tabs .sit-mode");
    const stateBtns = $$("#sit-state-btns .sit-chip");

    const APP_ORDER = ["mail", "slack", "code"];
    const TEXTS = {
      mail: "The launch is moving to Monday. I’ll share the final deck by 10 AM.",
      slack: "Yes — build 1.1 passed QA. Shipping the universal APK tonight 🚀",
      code: "// TODO: retry the payment request if the gateway times out"
    };
    const BASE = {};
    views.forEach((v) => (BASE[v.dataset.view] = $("[data-target]", v).innerHTML));

    let app = "mail";
    let mode = "windows"; // "windows" (circle -> text pill) | "mobile" (circle only)
    let currentPhase = "idle";
    let loopGen = 0;

    function applyPillState(phase) {
      currentPhase = phase;
      stateBtns.forEach((b) => b.classList.toggle("is-active", b.dataset.state === phase));
      dot.className = "lilt-status-dot";
      pill.classList.toggle("is-mobile-circle", mode === "mobile");

      if (phase === "idle") {
        pill.classList.add("is-idle");
        dot.classList.add("dot-white");
        status.textContent = "";
      } else if (phase === "listening") {
        pill.classList.toggle("is-idle", mode === "mobile");
        dot.classList.add("dot-red");
        status.textContent = mode === "windows" ? "Listening…" : "";
      } else if (phase === "transcribing") {
        pill.classList.toggle("is-idle", mode === "mobile");
        dot.classList.add("dot-yellow");
        status.textContent = mode === "windows" ? "Transcribing…" : "";
      } else if (phase === "copied") {
        pill.classList.toggle("is-idle", mode === "mobile");
        dot.classList.add("dot-green");
        status.textContent = mode === "windows" ? "Copied" : "";
      }
    }

    function selectAppTab(nextApp) {
      app = nextApp;
      tabs.forEach((x) => x.classList.toggle("is-active", x.dataset.app === app));
      views.forEach((v) => v.classList.toggle("is-active", v.dataset.view === app));
    }

    async function runAutoLoop(gen, startWaitMs = 0) {
      if (startWaitMs > 0) {
        await sleep(startWaitMs);
        if (gen !== loopGen) return;
      }
      while (gen === loopGen) {
        const view = views.find((v) => v.dataset.view === app);
        const target = $("[data-target]", view);
        const thisApp = app;

        // 1. Idle / Ready (Translucent circle with White dot)
        applyPillState("idle");
        target.innerHTML = BASE[thisApp] + '<span class="caret"></span>';
        await sleep(950);
        if (gen !== loopGen) return;

        // 2. Listening… (Red dot + expands to "Listening…" on Windows / pulses on Mobile)
        applyPillState("listening");
        await sleep(1450);
        if (gen !== loopGen) return;

        // 3. Transcribing… (Yellow dot + "Transcribing…")
        applyPillState("transcribing");
        await sleep(850);
        if (gen !== loopGen) return;

        // 4. Copied ✓ (Green dot + "Copied" + live typewriter insertion)
        applyPillState("copied");
        const text = TEXTS[thisApp];
        for (let i = 1; i <= text.length; i++) {
          if (gen !== loopGen || app !== thisApp) return;
          target.innerHTML = BASE[thisApp] + `<span class="typed">${text.slice(0, i)}</span><span class="caret"></span>`;
          await sleep(18);
        }
        await sleep(1800);
        if (gen !== loopGen) return;

        // Advance automatically to next app tab (Gmail -> Slack -> VS Code)
        const nextIdx = (APP_ORDER.indexOf(thisApp) + 1) % APP_ORDER.length;
        selectAppTab(APP_ORDER[nextIdx]);
      }
    }

    modeBtns.forEach((btn) => {
      btn.addEventListener("click", () => {
        modeBtns.forEach((b) => b.classList.toggle("is-active", b === btn));
        mode = btn.dataset.mode;
        applyPillState(currentPhase);
      });
    });

    stateBtns.forEach((btn) => {
      btn.addEventListener("click", () => {
        loopGen++;
        applyPillState(btn.dataset.state);
        // Resume automatic live cycle after 3.2s so it never stays frozen
        runAutoLoop(loopGen, 3200);
      });
    });

    function setPos(px, py) {
      const r = screen.getBoundingClientRect();
      const pr = pill.getBoundingClientRect();
      const x = clamp(px, pr.width / 2 + 8, r.width - pr.width / 2 - 8);
      const y = clamp(py, pr.height / 2 + 8, r.height - pr.height / 2 - 8);
      pill.style.left = `${(x / r.width) * 100}%`;
      pill.style.top = `${(y / r.height) * 100}%`;
      posEl.textContent = `x ${Math.round((x / r.width) * 100)}% · y ${Math.round((y / r.height) * 100)}% · ${mode === "windows" ? "Windows pill" : "Mobile circle"} (live auto)`;
    }

    let dragging = false, moved = 0, sx = 0, sy = 0, ox = 0, oy = 0;
    pill.addEventListener("pointerdown", (e) => {
      dragging = true; moved = 0; sx = e.clientX; sy = e.clientY;
      const pr = pill.getBoundingClientRect();
      ox = e.clientX - (pr.left + pr.width / 2);
      oy = e.clientY - (pr.top + pr.height / 2);
      pill.setPointerCapture(e.pointerId);
      pill.classList.add("is-dragging");
    });
    pill.addEventListener("pointermove", (e) => {
      if (!dragging) return;
      moved = Math.max(moved, Math.hypot(e.clientX - sx, e.clientY - sy));
      const r = screen.getBoundingClientRect();
      setPos(e.clientX - r.left - ox, e.clientY - r.top - oy);
    });
    const end = () => {
      if (!dragging) return;
      dragging = false;
      pill.classList.remove("is-dragging");
      if (moved < 6) {
        loopGen++;
        runAutoLoop(loopGen, 0);
      }
    };
    pill.addEventListener("pointerup", end);
    pill.addEventListener("pointercancel", () => { dragging = false; pill.classList.remove("is-dragging"); });

    tabs.forEach((t) =>
      t.addEventListener("click", () => {
        selectAppTab(t.dataset.app);
        loopGen++;
        runAutoLoop(loopGen, 0);
      })
    );

    $("#btn-ghost").addEventListener("click", (e) => {
      const on = pill.classList.toggle("is-ghost");
      e.currentTarget.classList.toggle("is-active", on);
    });
    $("#btn-snap").addEventListener("click", () => {
      const r = screen.getBoundingClientRect();
      pill.style.transition = "left .6s cubic-bezier(.16,1,.3,1), top .6s cubic-bezier(.16,1,.3,1), opacity .4s";
      setPos(r.width, parseFloat(pill.style.top || "70") / 100 * r.height);
    });

    // Start the continuous live automatic loop immediately
    loopGen++;
    runAutoLoop(loopGen, 200);
  }

  /* =====================================================================
     SILENT ALARM — phone lockscreen demo
     ===================================================================== */
  function initAlarm() {
    const phone = $("#alarm-phone");
    const sw = $("#silent-switch");
    const title = $("#silent-title");
    const desc = $("#silent-desc");
    const mode = $("#alarm-mode");
    const card = $("#alarm-card");
    const aTitle = $("#alarm-title");
    const aSub = $("#alarm-sub");
    const val = $("#snooze-val");
    const snoozeBtn = $("#btn-snooze");
    const presets = $$("#snooze-presets button");
    let mins = 10;

    const fmt = (m) => (m >= 60 ? (m % 60 ? `${Math.floor(m / 60)}h ${m % 60}m` : `${m / 60} hr`) : `${m} min`);
    const pad = (n) => String(n).padStart(2, "0");

    // live clock
    const lockTime = $("#lock-time"), lockDate = $("#lock-date");
    const tick = () => {
      const d = new Date();
      lockTime.textContent = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
      lockDate.textContent = d.toLocaleDateString("en-IN", { weekday: "long", day: "numeric", month: "short" }).toUpperCase();
    };
    tick(); setInterval(tick, 15000);

    function syncSnooze() {
      val.textContent = fmt(mins);
      snoozeBtn.textContent = `Snooze ${fmt(mins)}`;
      presets.forEach((b) => b.classList.toggle("is-active", +b.dataset.m === mins));
      if (hasGSAP) gsap.fromTo(val, { y: -6, opacity: 0.3 }, { y: 0, opacity: 1, duration: 0.35, ease: "expo.out" });
    }

    function syncSilent() {
      const silent = sw.checked;
      title.textContent = silent ? "Silent alarm is on" : "Silent alarm is off";
      desc.textContent = silent ? "Shows on screen until you act — no sound." : "Shows on screen and rings until you act.";
      mode.textContent = silent ? "🔕 SILENT" : "🔔 RINGING";
      mode.classList.toggle("ringing", !silent);
      phone.classList.toggle("is-ringing", !silent);
      if (!silent) {
        phone.classList.add("is-shaking");
        setTimeout(() => phone.classList.remove("is-shaking"), 1500);
      }
    }

    $("#snooze-minus").addEventListener("click", () => { mins = Math.max(5, mins - 5); syncSnooze(); });
    $("#snooze-plus").addEventListener("click", () => { mins = Math.min(180, mins + 5); syncSnooze(); });
    presets.forEach((b) => b.addEventListener("click", () => { mins = +b.dataset.m; syncSnooze(); }));
    sw.addEventListener("change", syncSilent);

    async function dismiss(t, s) {
      card.classList.add("is-leaving");
      phone.classList.remove("is-ringing");
      await sleep(450);
      aTitle.textContent = t;
      aSub.textContent = s;
      card.classList.remove("is-leaving");
      await sleep(2400);
      card.classList.add("is-leaving");
      await sleep(450);
      aTitle.textContent = "Call Rajesh about the Q4 deck";
      aSub.textContent = "Set by voice · stays until you act";
      card.classList.remove("is-leaving");
      syncSilent();
    }

    snoozeBtn.addEventListener("click", () => {
      const d = new Date(Date.now() + mins * 60000);
      dismiss(`Snoozed for ${fmt(mins)}`, `Back on screen at ${pad(d.getHours())}:${pad(d.getMinutes())}`);
    });
    $("#btn-done").addEventListener("click", () => dismiss("✓ Done. Nice one.", "Marked complete in your Reminders"));

    syncSilent();
  }

  /* =====================================================================
     NOTES — search, filters, pin, checklists
     ===================================================================== */
  function initNotes() {
    const items = [
      { id: 1, type: "note", title: "Weekend groceries", time: "Today · 9:12 AM", pinned: true, checks: [["Milk", true], ["Eggs", false], ["Phone charger", false], ["Coffee beans", false]] },
      { id: 2, type: "alert", title: "Call Rajesh about the Q4 deck", time: "Today · 6:40 PM", pinned: false, text: "Silent alarm · snooze 10 min" },
      { id: 3, type: "text", title: "Message to Priya", time: "Today · 11:03 AM", pinned: false, text: "Hey Priya, can we move it to Monday at 10:00? I’ll send the deck." },
      { id: 4, type: "note", title: "Goa trip", time: "Yesterday", pinned: false, checks: [["Book flights", false], ["Pay ₹4,500 hotel advance", true], ["Pack sunscreen", false]] },
      { id: 5, type: "alert", title: "Pay electricity bill ₹1,250", time: "Tomorrow · 10:00 AM", pinned: false, text: "Reminder · rings normally" },
      { id: 6, type: "text", title: "Email to team", time: "Mon", pinned: false, text: "The launch is moving to Monday. Final deck by 10 AM." }
    ];
    const LABEL = { note: "NOTE", alert: "REMINDER", text: "DICTATION" };

    const list = $("#notes-list");
    const input = $("#notes-search");
    const count = $("#notes-count");
    const filters = $$("#notes-filters button");
    let filter = "all";
    let flip = null;

    const esc = (s) => s.replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
    const hl = (s, q) => {
      const safe = esc(s);
      if (!q) return safe;
      const rx = new RegExp(`(${esc(q).replace(/[.*+?^${}()|[\]\\]/g, "\\$&")})`, "gi");
      return safe.replace(rx, "<mark>$1</mark>");
    };

    function render() {
      const q = input.value.trim();
      const ql = q.toLowerCase();
      const shown = items
        .filter((i) => filter === "all" || i.type === filter)
        .filter((i) => !ql || [i.title, i.text || "", ...(i.checks || []).map((c) => c[0])].join(" ").toLowerCase().includes(ql))
        .sort((a, b) => b.pinned - a.pinned || a.id - b.id);

      count.textContent = shown.length;
      if (!shown.length) {
        list.innerHTML = `<div class="n-empty">Nothing matches “${esc(q)}”.<br/>Try “milk”, “rajesh” or “₹”.</div>`;
        return;
      }
      list.innerHTML = shown.map((i) => `
        <div class="n-item${i.pinned ? " is-pinned" : ""}${flip === i.id ? " flip-in" : ""}">
          <div class="n-head">
            <div>
              <div class="n-type mono ${i.type}">${i.pinned ? "📌 PINNED · " : ""}${LABEL[i.type]} · ${esc(i.time)}</div>
              <div class="n-title">${hl(i.title, q)}</div>
            </div>
            <button type="button" class="n-pin${i.pinned ? " on" : ""}" data-pin="${i.id}" aria-label="${i.pinned ? "Unpin" : "Pin to top"}">📌</button>
          </div>
          ${i.text ? `<div class="n-text">${hl(i.text, q)}</div>` : ""}
          ${i.checks ? `<div class="n-checks">${i.checks.map((c, k) => `
            <button type="button" class="n-check${c[1] ? " done" : ""}" data-check="${i.id}:${k}">
              <span class="n-box">${c[1] ? "✓" : ""}</span><span>${hl(c[0], q)}</span>
            </button>`).join("")}</div>` : ""}
        </div>`).join("");
      flip = null;
    }

    list.addEventListener("click", (e) => {
      const pin = e.target.closest("[data-pin]");
      const chk = e.target.closest("[data-check]");
      if (pin) {
        const it = items.find((i) => i.id === +pin.dataset.pin);
        it.pinned = !it.pinned;
        flip = it.id;
        render();
      } else if (chk) {
        const [id, k] = chk.dataset.check.split(":").map(Number);
        const it = items.find((i) => i.id === id);
        it.checks[k][1] = !it.checks[k][1];
        render();
      }
    });
    input.addEventListener("input", render);
    filters.forEach((b) => b.addEventListener("click", () => {
      filter = b.dataset.f;
      filters.forEach((x) => x.classList.toggle("is-active", x === b));
      render();
    }));
    $$(".notes-try button").forEach((b) => b.addEventListener("click", () => {
      input.value = b.dataset.q;
      render();
      input.focus({ preventScroll: true });
    }));

    render();
  }

  /* =====================================================================
     COUNTERS
     ===================================================================== */
  function initCounters() {
    const els = $$("[data-count]");
    const run = (el) => {
      const to = +el.dataset.count;
      const from = to === 0 ? 99 : 0;
      const start = performance.now();
      const dur = 1600;
      (function step(now) {
        const p = clamp((now - start) / dur, 0, 1);
        const e = 1 - Math.pow(1 - p, 4);
        el.textContent = Math.round(from + (to - from) * e);
        if (p < 1) requestAnimationFrame(step);
      })(start);
    };
    if (!hasGSAP) { els.forEach((el) => (el.textContent = el.dataset.count)); return; }
    els.forEach((el) => ScrollTrigger.create({ trigger: el, start: "top 90%", once: true, onEnter: () => run(el) }));
  }

  /* =====================================================================
     FAQ — single-open accordion
     ===================================================================== */
  function initFAQ() {
    const items = $$(".faq details");
    items.forEach((d) => d.addEventListener("toggle", () => {
      if (d.open) items.forEach((o) => o !== d && (o.open = false));
      if (hasGSAP) {
        if (d.open) gsap.fromTo($("p", d), { opacity: 0, y: -8 }, { opacity: 1, y: 0, duration: 0.5, ease: "expo.out" });
        setTimeout(() => ScrollTrigger.refresh(), 60);
      }
    }));
  }

  /* =====================================================================
     BOOT
     ===================================================================== */
  initOS();
  initScroll();
  initNav();
  initPointer();
  const startHero = initHeroDemo();
  initManifesto();
  initHow();
  initFeatures();
  initFloatPill();
  initAlarm();
  initNotes();
  initCounters();
  initFAQ();
  initScrollFX();

  initPreloader(() => {
    heroIntro();
    if (startHero) setTimeout(startHero, animate ? 900 : 0);
  });

  if (hasGSAP) {
    if (document.fonts && document.fonts.ready) document.fonts.ready.then(() => ScrollTrigger.refresh());
    window.addEventListener("load", () => ScrollTrigger.refresh());
  }
})();
