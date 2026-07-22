document.addEventListener('DOMContentLoaded', () => {
    // 1. Copy IP functionality
    const ipAddress = 'play.theerpsmp.net';
    const btnCopyIpNav = document.getElementById('btnCopyIpNav');
    const btnCopyIpMain = document.getElementById('btnCopyIpMain');
    const toast = document.getElementById('copyToast');

    const copyIpToClipboard = () => {
        navigator.clipboard.writeText(ipAddress).then(() => {
            // Show toast notification
            toast.classList.add('show');
            setTimeout(() => {
                toast.classList.remove('show');
            }, 3000);
        }).catch(err => {
            console.error('Failed to copy IP address: ', err);
        });
    };

    if (btnCopyIpNav) btnCopyIpNav.addEventListener('click', copyIpToClipboard);
    if (btnCopyIpMain) btnCopyIpMain.addEventListener('click', copyIpToClipboard);

    // 2. Minecraft Server Status API
    const playerCountText = document.getElementById('playerCountText');
    const statusPlayers = document.getElementById('statusPlayers');
    const progressBar = document.getElementById('progressBar');
    const pingBadge = document.getElementById('pingBadge');

    const fetchServerStatus = async () => {
        try {
            // Using public mcsrvstat API for real-time status
            const response = await fetch(`https://api.mcsrvstat.us/3/${ipAddress}`);
            if (!response.ok) throw new Error('API fetch error');
            const data = await response.json();

            if (data.online) {
                const onlineCount = data.players?.online || 0;
                const maxCount = 40000;
                
                playerCountText.textContent = `${onlineCount.toLocaleString()} Players Online`;
                statusPlayers.textContent = `${onlineCount.toLocaleString()} / ${maxCount.toLocaleString()}`;
                
                // Calculate percentage for progress bar
                const percent = Math.min((onlineCount / maxCount) * 100, 100);
                progressBar.style.width = `${percent}%`;

                // Set ping dynamically (using fake randomized variation or API default)
                const mockPing = Math.floor(Math.random() * 8) + 12; // 12-20ms
                pingBadge.innerHTML = `<i class="fa-solid fa-wifi"></i> ${mockPing}ms`;
            } else {
                setOfflineStatus();
            }
        } catch (error) {
            console.error('Error fetching Minecraft server status:', error);
            setOfflineStatus();
        }
    };

    const setOfflineStatus = () => {
        playerCountText.textContent = 'Server Offline';
        statusPlayers.textContent = 'Offline';
        progressBar.style.width = '0%';
        pingBadge.innerHTML = `<i class="fa-solid fa-wifi-slash"></i> Offline`;
        
        const dot = document.querySelector('.status-dot');
        if (dot) {
            dot.classList.remove('online');
            dot.classList.add('offline');
        }
    };

    // Initial fetch & set poll timer every 60 seconds
    fetchServerStatus();
    setInterval(fetchServerStatus, 60000);

    // 3. Command list Search filter
    const commandSearchInput = document.getElementById('commandSearchInput');
    const commandRows = document.querySelectorAll('.command-row');

    if (commandSearchInput) {
        commandSearchInput.addEventListener('input', (e) => {
            const query = e.target.value.toLowerCase().trim();
            commandRows.forEach(row => {
                const keywords = row.getAttribute('data-name').toLowerCase();
                const syntax = row.querySelector('.command-syntax').textContent.toLowerCase();
                const desc = row.querySelector('.command-desc').textContent.toLowerCase();
                
                if (keywords.includes(query) || syntax.includes(query) || desc.includes(query)) {
                    row.style.display = 'grid';
                } else {
                    row.style.display = 'none';
                }
            });
        });
    }

    // 4. Mobile Menu toggle
    const mobileNavToggle = document.getElementById('mobileNavToggle');
    const navLinks = document.getElementById('navLinks');
    const navLinkItems = document.querySelectorAll('.nav-link');

    if (mobileNavToggle && navLinks) {
        mobileNavToggle.addEventListener('click', () => {
            navLinks.classList.toggle('active');
            const icon = mobileNavToggle.querySelector('i');
            if (navLinks.classList.contains('active')) {
                icon.classList.remove('fa-bars');
                icon.classList.add('fa-xmark');
            } else {
                icon.classList.remove('fa-xmark');
                icon.classList.add('fa-bars');
            }
        });

        // Close menu when clicking a link
        navLinkItems.forEach(link => {
            link.addEventListener('click', () => {
                navLinks.classList.remove('active');
                const icon = mobileNavToggle.querySelector('i');
                icon.classList.remove('fa-xmark');
                icon.classList.add('fa-bars');
            });
        });
    }

    // 5. Scroll Active Navigation Link Highlight
    const sections = document.querySelectorAll('section');
    window.addEventListener('scroll', () => {
        let current = '';
        sections.forEach(section => {
            const sectionTop = section.offsetTop;
            const sectionHeight = section.clientHeight;
            if (pageYOffset >= (sectionTop - 150)) {
                current = section.getAttribute('id');
            }
        });

        navLinkItems.forEach(link => {
            link.classList.remove('active');
            if (link.getAttribute('href') === `#${current}`) {
                link.classList.add('active');
            }
        });
    });

    // 6. Launch Countdown Timer
    // Target Date: August 11, 2026 4:00 PM
    const targetDate = new Date('August 11, 2026 16:00:00').getTime();

    const daysVal = document.getElementById('days');
    const hoursVal = document.getElementById('hours');
    const minutesVal = document.getElementById('minutes');
    const secondsVal = document.getElementById('seconds');

    const updateUnit = (element, newValue) => {
        const oldValue = element.textContent;
        const formattedValue = String(newValue).padStart(2, '0');
        
        if (oldValue !== formattedValue) {
            element.textContent = formattedValue;
            element.classList.remove('val-changed');
            void element.offsetWidth; // Trigger reflow to restart CSS animation
            element.classList.add('val-changed');
        }
    };

    const updateCountdown = () => {
        const now = new Date().getTime();
        const difference = targetDate - now;

        if (difference <= 0) {
            if (daysVal) updateUnit(daysVal, 0);
            if (hoursVal) updateUnit(hoursVal, 0);
            if (minutesVal) updateUnit(minutesVal, 0);
            if (secondsVal) updateUnit(secondsVal, 0);
            
            const titleEl = document.querySelector('.countdown-title');
            if (titleEl) {
                titleEl.innerHTML = '<i class="fa-solid fa-circle-check text-green"></i> We Are Live!';
            }
            return;
        }

        const days = Math.floor(difference / (1000 * 60 * 60 * 24));
        const hours = Math.floor((difference % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((difference % (1000 * 60)) / 1000);

        if (daysVal) updateUnit(daysVal, days);
        if (hoursVal) updateUnit(hoursVal, hours);
        if (minutesVal) updateUnit(minutesVal, minutes);
        if (secondsVal) updateUnit(secondsVal, seconds);
    };

    updateCountdown();
    setInterval(updateCountdown, 1000);
});
