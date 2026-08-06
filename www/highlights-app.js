// ============================================================
//  ErpSMP World Highlights — Polished App Logic
// ============================================================

document.addEventListener('DOMContentLoaded', () => {

    /* ---- Camera Configuration ---- */
    const cameras = [
        {
            key: 'spawn',
            id: 'CAM-01',
            name: 'Spawn Square',
            coords: 'XYZ: -15 / 89 / 0',
            resolution: '1080p60 ULTRA',
            grade: 'grade-spawn',
            image: 'assets/spawn_cam.png'
        },
        {
            key: 'duel',
            id: 'CAM-02',
            name: 'Duel Arena',
            coords: 'XYZ: 0 / 72 / 0',
            resolution: '1080p60 ULTRA',
            grade: 'grade-duel',
            image: 'assets/duel_arena_cam.png'
        },
        {
            key: 'echo',
            id: 'CAM-03',
            name: 'Echo Valley',
            coords: 'XYZ: 142 / 45 / -89',
            resolution: '4K30 ULTRA',
            grade: 'grade-echo',
            image: 'assets/echo_valley_cam.png'
        },
        {
            key: 'afk',
            id: 'CAM-04',
            name: 'AFK & Bank Vault',
            coords: 'XYZ: -320 / 64 / 112',
            resolution: '1080p60 ULTRA',
            grade: 'grade-afk',
            image: 'assets/bank_afk_cam.png'
        }
    ];

    let activeCamIdx = 0;
    let autoCycleTimer = null;
    const AUTO_CYCLE_MS = 18000; // auto-switch every 18s

    /* ---- DOM Refs ---- */
    const streamViewport   = document.getElementById('streamViewport');
    const streamBgImg      = document.getElementById('streamBgImg');
    const hudCamBadge      = document.getElementById('hudCamBadge');
    const hudCamName       = document.getElementById('hudCamName');
    const hudCoords        = document.getElementById('hudCoords');
    const hudFps           = document.getElementById('hudFps');
    const hudResolution    = document.getElementById('hudResolution');
    const spectatorCount   = document.getElementById('spectatorCount');
    const eventsFeedList   = document.getElementById('eventsFeedList');
    const chatForm         = document.getElementById('chatForm');
    const chatInput        = document.getElementById('chatInput');
    const videoModal       = document.getElementById('videoModal');
    const modalTitle       = document.getElementById('modalTitle');
    const modalCloseBtn    = document.getElementById('modalCloseBtn');
    const onlineCounter    = document.getElementById('onlineCounter');
    const duelsCounter     = document.getElementById('duelsCounter');
    const cratesCounter    = document.getElementById('cratesCounter');
    const killsCounter     = document.getElementById('killsCounter');

    /* ---- Animated Counter Util ---- */
    function animateCounter(el, target, duration = 1200) {
        if (!el) return;
        const start = performance.now();
        const from = parseInt(el.textContent.replace(/[^0-9]/g, '')) || 0;
        function tick(now) {
            const progress = Math.min((now - start) / duration, 1);
            const ease = 1 - Math.pow(1 - progress, 3);
            el.textContent = Math.round(from + (target - from) * ease).toLocaleString();
            if (progress < 1) requestAnimationFrame(tick);
        }
        requestAnimationFrame(tick);
    }

    // Initial counters
    animateCounter(onlineCounter, 24);
    animateCounter(duelsCounter, 7);
    animateCounter(cratesCounter, 312);
    animateCounter(killsCounter, 4891);

    /* ---- Camera Switching ---- */
    function switchCamera(idx) {
        activeCamIdx = idx;
        const cam = cameras[idx];

        // Fade the image
        if (streamBgImg) {
            streamBgImg.style.opacity = '0';
            setTimeout(() => {
                streamBgImg.src = cam.image;
                streamBgImg.onload = () => { streamBgImg.style.opacity = '1'; };
                // fallback in case onload doesn't fire
                setTimeout(() => { streamBgImg.style.opacity = '1'; }, 300);
            }, 250);
        }

        // Update viewport colour grade
        if (streamViewport) {
            streamViewport.className = 'stream-viewport ' + cam.grade;
        }

        // Update HUD
        if (hudCamBadge) hudCamBadge.innerHTML = `<i class="fa-solid fa-video"></i> ${cam.id}`;
        if (hudCamName)  hudCamName.textContent = cam.name;
        if (hudCoords)   hudCoords.textContent  = cam.coords;
        if (hudResolution) hudResolution.textContent = cam.resolution;

        // Update cam buttons
        document.querySelectorAll('.cam-btn').forEach((btn, i) => {
            btn.classList.toggle('active', i === idx);
        });

        resetAutoCycle();
    }

    document.querySelectorAll('.cam-btn').forEach((btn, i) => {
        btn.addEventListener('click', () => switchCamera(i));
    });

    function startAutoCycle() {
        autoCycleTimer = setInterval(() => {
            switchCamera((activeCamIdx + 1) % cameras.length);
        }, AUTO_CYCLE_MS);
    }

    function resetAutoCycle() {
        clearInterval(autoCycleTimer);
        startAutoCycle();
    }

    // Init first camera & start cycling
    switchCamera(0);
    startAutoCycle();

    /* ---- Mute / Fullscreen Controls ---- */
    const btnMute       = document.getElementById('btnMuteToggle');
    const btnFullscreen = document.getElementById('btnFullscreen');
    let muted = false;

    if (btnMute) {
        btnMute.addEventListener('click', () => {
            muted = !muted;
            btnMute.innerHTML = muted
                ? '<i class="fa-solid fa-volume-xmark"></i>'
                : '<i class="fa-solid fa-volume-high"></i>';
        });
    }

    if (btnFullscreen && streamViewport) {
        btnFullscreen.addEventListener('click', () => {
            if (!document.fullscreenElement) {
                streamViewport.requestFullscreen?.();
            } else {
                document.exitFullscreen?.();
            }
        });
    }

    /* ---- Spectator Count Simulation ---- */
    let spectators = 64;
    setInterval(() => {
        spectators += Math.floor(Math.random() * 7) - 3;
        spectators = Math.max(38, Math.min(120, spectators));
        if (spectatorCount) spectatorCount.textContent = `${spectators} Watching`;
    }, 5000);

    /* ---- Live Server Event Feed ---- */
    const allEvents = [
        { cls: 'duel',  icon: 'fa-swords',    text: () => `<span class="highlight-name">.RedToppat208</span> won a duel against <span class="highlight-name">blanking</span>! 🏆` },
        { cls: 'crate', icon: 'fa-box-open',  text: () => `<span class="highlight-name">.Boreas4052</span> opened an <span style="color:var(--accent-purple);font-weight:700;">[Echo Crate]</span> jackpot!` },
        { cls: 'kill',  icon: 'fa-skull',     text: () => `<span class="highlight-name">blanking</span> was slain by <span class="highlight-name">.RedToppat208</span> using ⚔️ Echo Sword!` },
        { cls: 'join',  icon: 'fa-user-plus', text: () => `<span class="highlight-name">ShadowMiner</span> joined ErpSMP — Welcome! 🎉` },
        { cls: 'crate', icon: 'fa-key',       text: () => `<span class="highlight-name">CraftGod99</span> received <span style="color:var(--accent-gold);font-weight:700;">1x Crimson Key</span>! 🔑` },
        { cls: 'duel',  icon: 'fa-trophy',    text: () => `<span class="highlight-name">.Boreas4052</span> reached <span style="color:var(--accent-gold);font-weight:700;">Top #1 Kills</span> on the leaderboard!` },
        { cls: 'kill',  icon: 'fa-skull',     text: () => `<span class="highlight-name">PixelKnight</span> was blown up by a Creeper near Spawn.` },
        { cls: 'crate', icon: 'fa-gem',       text: () => `<span class="highlight-name">DragonBorn</span> found <span style="color:#38bdf8;font-weight:700;">5x Netherite Ingots</span> in a crate!` },
        { cls: 'join',  icon: 'fa-door-open', text: () => `<span class="highlight-name">NightRunner</span> left the game.` },
        { cls: 'duel',  icon: 'fa-swords',    text: () => `<span class="highlight-name">IronFist</span> challenged <span class="highlight-name">SkyWalker</span> to a duel!` }
    ];

    function timeAgo(seconds) {
        if (seconds < 60) return `${seconds}s ago`;
        return `${Math.floor(seconds / 60)}m ago`;
    }

    function addEvent(evtObj, ago = 0) {
        if (!eventsFeedList) return;
        const item = document.createElement('div');
        item.className = 'event-item';
        item.innerHTML = `
            <div class="event-icon ${evtObj.cls}">
                <i class="fa-solid ${evtObj.icon}"></i>
            </div>
            <div class="event-content">
                <div class="event-text">${evtObj.text()}</div>
                <div class="event-time">${ago === 0 ? 'Just now' : timeAgo(ago)}</div>
            </div>`;
        eventsFeedList.insertBefore(item, eventsFeedList.firstChild);
        while (eventsFeedList.children.length > 18) {
            eventsFeedList.removeChild(eventsFeedList.lastChild);
        }
    }

    // Seed initial events
    const seeds = [2, 5, 7, 15, 32, 74];
    [...seeds].reverse().forEach((ago, i) => {
        addEvent(allEvents[i % allEvents.length], ago);
    });

    // Auto-generate new events
    setInterval(() => {
        const evt = allEvents[Math.floor(Math.random() * allEvents.length)];
        addEvent(evt);
    }, 7000);

    /* ---- Chat Input ---- */
    if (chatForm) {
        chatForm.addEventListener('submit', e => {
            e.preventDefault();
            const msg = chatInput.value.trim();
            if (!msg) return;
            addEvent({
                cls: 'join',
                icon: 'fa-comment',
                text: () => `<span class="highlight-name" style="color:var(--primary)">Visitor</span>: ${escHtml(msg)}`
            });
            chatInput.value = '';
        });
    }

    function escHtml(s) {
        return s.replace(/[&<>"']/g, c => ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;' }[c]));
    }

    /* ---- Clip Modal ---- */
    document.querySelectorAll('.clip-card').forEach(card => {
        card.addEventListener('click', () => {
            if (modalTitle) modalTitle.textContent = card.dataset.title || 'ErpSMP Highlight';
            if (videoModal && videoModal.showModal) videoModal.showModal();
        });
    });

    if (modalCloseBtn && videoModal) {
        modalCloseBtn.addEventListener('click', () => videoModal.close());
    }

    // Light-dismiss fallback (per modern-web-guidance closedBy support check)
    if (videoModal && !('closedBy' in HTMLDialogElement.prototype)) {
        videoModal.addEventListener('click', e => {
            if (e.target !== videoModal) return;
            const r = videoModal.getBoundingClientRect();
            const inside = r.top <= e.clientY && e.clientY <= r.bottom &&
                           r.left <= e.clientX && e.clientX <= r.right;
            if (!inside) videoModal.close();
        });
    }

    /* ---- Intersection Observer for clip cards entrance ---- */
    const observer = new IntersectionObserver(entries => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.1 });

    document.querySelectorAll('.clip-card, .stat-card').forEach(el => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(20px)';
        el.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
        observer.observe(el);
    });

});
