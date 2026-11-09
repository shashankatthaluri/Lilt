// Lilt Smart Platform Detection & Interactive Showcase
(function () {
  'use strict';

  // Constants & Release Links
  const REPO_OWNER = 'shashankatthaluri';
  const REPO_NAME = 'Lilt';
  const GITHUB_REPO_URL = `https://github.com/${REPO_OWNER}/${REPO_NAME}`;
  
  // Direct release download asset URLs (fallback to releases page if not yet tagged)
  const DOWNLOAD_WINDOWS_EXE = `https://github.com/${REPO_OWNER}/${REPO_NAME}/releases/latest/download/Lilt-Setup-x64.exe`;
  const DOWNLOAD_ANDROID_APK = `https://github.com/${REPO_OWNER}/${REPO_NAME}/releases/latest/download/Lilt-v1.1-Release.apk`;
  const RELEASES_PAGE = `https://github.com/${REPO_OWNER}/${REPO_NAME}/releases`;

  // Detect Operating System
  function detectOS() {
    const userAgent = window.navigator.userAgent || window.navigator.vendor || window.opera;
    const platform = window.navigator?.userAgentData?.platform || window.navigator.platform || '';

    if (/android/i.test(userAgent)) {
      return 'android';
    }
    if (/iPad|iPhone|iPod/.test(userAgent) && !window.MSStream) {
      return 'ios';
    }
    if (/Win/i.test(platform) || /windows/i.test(userAgent)) {
      return 'windows';
    }
    if (/Mac/i.test(platform) || /macintosh|mac os x/i.test(userAgent)) {
      return 'macos';
    }
    if (/Linux/i.test(platform) || /linux/i.test(userAgent)) {
      return 'linux';
    }
    return 'unknown';
  }

  // Update Hero Download Module based on detected OS
  function configureHeroDownload() {
    const os = detectOS();
    const heroBtn = document.getElementById('hero-download-btn');
    const heroBtnText = document.getElementById('hero-btn-text');
    const heroBtnSub = document.getElementById('hero-btn-sub');
    const detectedTag = document.getElementById('detected-os-name');
    const qrSection = document.getElementById('qr-section');
    const androidTipSection = document.getElementById('android-tip-section');

    if (!heroBtn || !heroBtnText) return;

    if (os === 'android') {
      detectedTag.textContent = 'Android Device Detected';
      heroBtn.href = DOWNLOAD_ANDROID_APK;
      heroBtnText.textContent = 'Download for Android (.apk)';
      if (heroBtnSub) heroBtnSub.textContent = 'v1.1 • ARM64/Universal • ~17 MB';
      if (qrSection) qrSection.style.display = 'none';
      if (androidTipSection) androidTipSection.style.display = 'block';
    } else if (os === 'windows') {
      detectedTag.textContent = 'Windows PC Detected';
      heroBtn.href = DOWNLOAD_WINDOWS_EXE;
      heroBtnText.textContent = 'Download for Windows (.exe)';
      if (heroBtnSub) heroBtnSub.textContent = 'v1.0 • x64 Installer • ~24 MB';
      if (qrSection) qrSection.style.display = 'flex';
      if (androidTipSection) androidTipSection.style.display = 'none';
    } else {
      detectedTag.textContent = os.toUpperCase() + ' Detected';
      heroBtn.href = RELEASES_PAGE;
      heroBtnText.textContent = 'View All Releases';
      if (heroBtnSub) heroBtnSub.textContent = 'Windows (.exe) & Android (.apk)';
      if (qrSection) qrSection.style.display = 'flex';
      if (androidTipSection) androidTipSection.style.display = 'none';
    }
  }

  // Interactive Floating Pill Demo
  function initFloatingPillDemo() {
    const pill = document.getElementById('interactive-pill');
    const pillText = document.getElementById('pill-live-text');
    const outputBox = document.getElementById('mockup-cleaned-output');
    if (!pill || !pillText || !outputBox) return;

    const samples = [
      {
        heard: '"Schedule team sync for 3 PM... no wait, make it tomorrow at 11 AM."',
        cleaned: 'Schedule team sync for tomorrow at 11:00 AM.'
      },
      {
        heard: '"Take a note buy almond milk eggs and two thousand rupees cash."',
        cleaned: '• Almond milk\n• Eggs\n• ₹2,000 cash'
      },
      {
        heard: '"Remind me to call Venkatesh at Gachibowli office tonight."',
        cleaned: '🔔 Alert Scheduled: Call Venkatesh at Gachibowli office tonight (Silent Alarm)'
      }
    ];

    let currentIndex = 0;
    let isAnimating = false;

    pill.addEventListener('click', () => {
      if (isAnimating) return;
      isAnimating = true;

      const sample = samples[currentIndex];
      currentIndex = (currentIndex + 1) % samples.length;

      pillText.textContent = 'LISTENING...';
      pill.style.borderColor = '#D71921';
      pill.style.boxShadow = '0 0 35px rgba(215, 25, 33, 0.4)';
      outputBox.innerHTML = '<span style="color:#71717A;">Transcribing speech...</span>';

      setTimeout(() => {
        pillText.textContent = 'CLEANING DISFLUENCIES...';
        pill.style.borderColor = '#FFB800';
        pill.style.boxShadow = '0 0 35px rgba(255, 184, 0, 0.4)';
        outputBox.innerHTML = `<span style="text-decoration: line-through; color: #F87171;">${sample.heard}</span>`;
      }, 1200);

      setTimeout(() => {
        pillText.textContent = 'READY (TAP TO DICTATE)';
        pill.style.borderColor = 'rgba(255, 255, 255, 0.18)';
        pill.style.boxShadow = '0 20px 40px rgba(0, 0, 0, 0.7)';
        outputBox.innerHTML = `
          <div style="color: #4ADE80; font-weight: 600; margin-bottom: 4px;">✓ AUTO-CLEANED & COPIED:</div>
          <div style="color: #FFFFFF; font-size: 14px; white-space: pre-line;">${sample.cleaned}</div>
        `;
        isAnimating = false;
      }, 2500);
    });
  }

  // Interactive Disfluency Showcase Tabs
  function initDisfluencyTabs() {
    const chips = document.querySelectorAll('.demo-chip');
    const bubbleBefore = document.getElementById('demo-bubble-before');
    const bubbleAfter = document.getElementById('demo-bubble-after');

    const demoData = {
      1: {
        before: '"Send the quarterly report by Friday... no wait, scratch that, Monday morning."',
        after: '"Send the quarterly report by Monday morning."'
      },
      2: {
        before: '"Meeting at 4... kaadu kaadu repu 5 PM ki pettandi."',
        after: '"Meeting repu 5:00 PM ki pettandi."'
      },
      3: {
        before: '"Transfer five thousand rupees to Venkatesh at HDFC bank."',
        after: '"Transfer ₹5,000 to Venkatesh at HDFC bank."'
      },
      4: {
        before: '"Take a note need groceries: apples bananas bread."',
        after: '• Apples\n• Bananas\n• Bread'
      }
    };

    chips.forEach(chip => {
      chip.addEventListener('click', () => {
        chips.forEach(c => c.classList.remove('active'));
        chip.classList.add('active');
        const id = chip.getAttribute('data-demo-id');
        if (demoData[id] && bubbleBefore && bubbleAfter) {
          bubbleBefore.textContent = demoData[id].before;
          bubbleAfter.innerHTML = demoData[id].after.replace(/\n/g, '<br/>');
        }
      });
    });
  }

  // Simple QR Code Generator for Android Download (via SVG)
  function renderQrCode() {
    const qrContainer = document.getElementById('qr-canvas-container');
    if (!qrContainer) return;
    
    // Using a reliable, lightweight SVG QR code API or embed
    const qrTargetUrl = encodeURIComponent(DOWNLOAD_ANDROID_APK);
    const qrImg = document.createElement('img');
    qrImg.src = `https://api.qrserver.com/v1/create-qr-code/?size=140x140&margin=4&data=${qrTargetUrl}`;
    qrImg.alt = 'Scan to download Lilt Android APK';
    qrImg.className = 'qr-code-img';
    qrContainer.appendChild(qrImg);
  }

  // Initialize on DOM Ready
  document.addEventListener('DOMContentLoaded', () => {
    configureHeroDownload();
    initFloatingPillDemo();
    initDisfluencyTabs();
    renderQrCode();
  });
})();
