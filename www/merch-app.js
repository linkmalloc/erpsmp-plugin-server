document.addEventListener('DOMContentLoaded', () => {
    // Merch products database
    const PRODUCTS = {
        tshirt: { name: 'Premium Erp T-Shirt', priceUSD: 24.99, pricePHP: 499.95, image: 'merch_tshirt.png' },
        hoodie: { name: 'Challenger Hoodie', priceUSD: 49.99, pricePHP: 1499.95, image: 'merch_hoodie.png' },
        mug: { name: 'Glossy Ceramic Mug', priceUSD: 14.99, pricePHP: 399.95, image: 'merch_mug.png' },
        stickers: { name: 'Holographic Sticker Sheet', priceUSD: 5.99, pricePHP: 199.95, image: 'merch_stickers.png' }
    };

    // State
    let cart = JSON.parse(localStorage.getItem('erpsmp_cart')) || [];
    let checkoutPlatform = 'java';
    let checkoutUsername = '';
    let selectedCurrency = 'PHP'; // Default to PHP matching store cards

    // DOM Elements
    const btnCartToggle = document.getElementById('btnCartToggle');
    const btnCartClose = document.getElementById('btnCartClose');
    const cartDrawer = document.getElementById('cartDrawer');
    const cartOverlay = document.getElementById('cartOverlay');
    const cartCountBadge = document.getElementById('cartCountBadge');
    const cartItemsList = document.getElementById('cartItemsList');
    const cartSubtotal = document.getElementById('cartSubtotal');
    const btnCheckout = document.getElementById('btnCheckout');
    
    // Checkout Multi-Step elements
    const checkoutModal = document.getElementById('checkoutModal');
    const checkoutModalCard = document.getElementById('checkoutModalCard');
    const btnModalClose = document.getElementById('btnModalClose');
    const checkoutStep1 = document.getElementById('checkoutStep1');
    const checkoutStep2 = document.getElementById('checkoutStep2');
    
    const btnPlatformJava = document.getElementById('btnPlatformJava');
    const btnPlatformBedrock = document.getElementById('btnPlatformBedrock');
    const checkoutUsernameInput = document.getElementById('checkoutUsernameInput');
    const btnCheckoutContinue = document.getElementById('btnCheckoutContinue');
    
    const checkoutTotalText = document.getElementById('checkoutTotalText');
    const checkoutCurrencySelect = document.getElementById('checkoutCurrencySelect');
    const checkoutUserAvatar = document.getElementById('checkoutUserAvatar');
    const checkoutUsernameDisplay = document.getElementById('checkoutUsernameDisplay');
    const btnSwitchAccount = document.getElementById('btnSwitchAccount');
    const checkoutTableBody = document.getElementById('checkoutTableBody');
    const btnProceedCheckout = document.getElementById('btnProceedCheckout');
    const toast = document.getElementById('copyToast');

    // Step 3 (Payment) DOM elements
    const checkoutStep3 = document.getElementById('checkoutStep3');
    const paymentSuccessView = document.getElementById('paymentSuccessView');
    const btnPaymentBack = document.getElementById('btnPaymentBack');
    const btnPaymentSubmit = document.getElementById('btnPaymentSubmit');
    const btnSuccessClose = document.getElementById('btnSuccessClose');
    const successAccountDisplay = document.getElementById('successAccountDisplay');
    
    const panelGCash = document.getElementById('panelGCash');
    const panelUSDT = document.getElementById('panelUSDT');
    const panelBitcoin = document.getElementById('panelBitcoin');
    
    const gcashRefInput = document.getElementById('gcashRefInput');
    const usdtHashInput = document.getElementById('usdtHashInput');
    const btcHashInput = document.getElementById('btcHashInput');
    
    const paymentMethodBtns = document.querySelectorAll('.payment-method-btn');
    const paymentDetailPanels = document.querySelectorAll('.payment-detail-panel');

    // UI Updates
    const saveCart = () => {
        localStorage.setItem('erpsmp_cart', JSON.stringify(cart));
    };

    const formatCurrency = (amount, currency) => {
        if (currency === 'PHP') {
            return `₱ ${amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
        }
        return `$${amount.toFixed(2)}`;
    };

    const updateCartUI = () => {
        saveCart();
        
        // Update Count Badges
        const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);
        if (cartCountBadge) cartCountBadge.textContent = totalItems;

        // Render List
        if (cart.length === 0) {
            if (cartItemsList) {
                cartItemsList.innerHTML = `
                    <div class="cart-empty-state">
                        <i class="fa-solid fa-basket-shopping"></i>
                        <p>Your cart is empty.</p>
                    </div>
                `;
            }
            if (cartSubtotal) cartSubtotal.textContent = formatCurrency(0, 'PHP');
            if (btnCheckout) btnCheckout.disabled = true;
            return;
        }

        if (btnCheckout) btnCheckout.disabled = false;
        let subtotalPHP = 0;
        if (cartItemsList) cartItemsList.innerHTML = '';

        cart.forEach(item => {
            const product = PRODUCTS[item.id];
            if (!product) return;
            const itemTotalPHP = product.pricePHP * item.quantity;
            subtotalPHP += itemTotalPHP;

            const itemEl = document.createElement('div');
            itemEl.className = 'cart-item';
            itemEl.innerHTML = `
                <img src="${product.image}" alt="${product.name}" class="cart-item-img">
                <div class="cart-item-info">
                    <h4>${product.name}</h4>
                    <span class="cart-item-price">${formatCurrency(product.pricePHP, 'PHP')}</span>
                </div>
                <div class="cart-item-controls">
                    <div class="quantity-stepper">
                        <button class="btn-qty-minus" data-id="${item.id}">-</button>
                        <span>${item.quantity}</span>
                        <button class="btn-qty-plus" data-id="${item.id}">+</button>
                    </div>
                    <button class="btn-remove-item" data-id="${item.id}">Remove</button>
                </div>
            `;
            if (cartItemsList) cartItemsList.appendChild(itemEl);
        });

        if (cartSubtotal) cartSubtotal.textContent = formatCurrency(subtotalPHP, 'PHP');
    };

    // Actions
    const addToCart = (productId) => {
        const existing = cart.find(item => item.id === productId);
        if (existing) {
            existing.quantity += 1;
        } else {
            cart.push({ id: productId, quantity: 1 });
        }
        updateCartUI();
        showToast();
    };

    const changeQuantity = (productId, delta) => {
        const item = cart.find(item => item.id === productId);
        if (item) {
            item.quantity += delta;
            if (item.quantity <= 0) {
                cart = cart.filter(i => i.id !== productId);
            }
            updateCartUI();
            if (checkoutModal && checkoutModal.classList.contains('active')) {
                renderCheckoutTable();
            }
        }
    };

    const removeFromCart = (productId) => {
        cart = cart.filter(item => item.id !== productId);
        updateCartUI();
        if (checkoutModal && checkoutModal.classList.contains('active')) {
            renderCheckoutTable();
        }
    };

    const toggleCart = () => {
        if (cartDrawer) cartDrawer.classList.toggle('active');
        if (cartOverlay) cartOverlay.classList.toggle('active');
    };

    const showToast = () => {
        if (toast) {
            toast.classList.add('show');
            setTimeout(() => {
                toast.classList.remove('show');
            }, 2000);
        }
    };

    // Render step 2 table
    const renderCheckoutTable = () => {
        if (cart.length === 0) {
            checkoutTableBody.innerHTML = `
                <tr>
                    <td colspan="3" style="text-align: center; color: var(--text-secondary);">Your cart is empty.</td>
                </tr>
            `;
            checkoutTotalText.textContent = formatCurrency(0, selectedCurrency);
            return;
        }

        checkoutTableBody.innerHTML = '';
        let total = 0;

        cart.forEach(item => {
            const product = PRODUCTS[item.id];
            if (!product) return;
            const price = selectedCurrency === 'PHP' ? product.pricePHP : product.priceUSD;
            const rowTotal = price * item.quantity;
            total += rowTotal;

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>
                    <div class="checkout-item-name">${product.name}</div>
                </td>
                <td>
                    <div class="checkout-item-price">${formatCurrency(price, selectedCurrency)}</div>
                </td>
                <td>
                    <div class="checkout-qty-cell">
                        <input type="number" class="checkout-qty-input" data-id="${item.id}" value="${item.quantity}" min="1">
                        <div class="checkout-row-actions">
                            <button class="btn-row-action update-qty" data-id="${item.id}"><i class="fa-solid fa-arrows-rotate"></i></button>
                            <button class="btn-row-action info-item" data-id="${item.id}"><i class="fa-solid fa-circle-info"></i></button>
                            <button class="btn-row-action delete delete-item" data-id="${item.id}"><i class="fa-solid fa-xmark"></i></button>
                        </div>
                    </div>
                </td>
            `;
            checkoutTableBody.appendChild(tr);
        });

        checkoutTotalText.textContent = formatCurrency(total, selectedCurrency);
    };

    // Checkout Simulation
    const openCheckoutModal = () => {
        toggleCart(); // Close drawer
        
        // Reset to step 1
        checkoutStep1.style.display = 'flex';
        checkoutStep2.style.display = 'none';
        if (checkoutStep3) checkoutStep3.style.display = 'none';
        if (paymentSuccessView) paymentSuccessView.style.display = 'none';
        if (checkoutModalCard) checkoutModalCard.classList.remove('step-2-active');
        
        // Reset inputs
        if (gcashRefInput) gcashRefInput.value = '';
        if (usdtHashInput) usdtHashInput.value = '';
        if (btcHashInput) btcHashInput.value = '';
        
        if (checkoutModal) checkoutModal.classList.add('active');
    };

    const closeCheckoutModal = () => {
        if (checkoutModal) checkoutModal.classList.remove('active');
    };

    // Platform Tab toggles
    if (btnPlatformJava) {
        btnPlatformJava.addEventListener('click', () => {
            checkoutPlatform = 'java';
            btnPlatformJava.classList.add('active');
            if (btnPlatformBedrock) btnPlatformBedrock.classList.remove('active');
        });
    }

    if (btnPlatformBedrock) {
        btnPlatformBedrock.addEventListener('click', () => {
            checkoutPlatform = 'bedrock';
            btnPlatformBedrock.classList.add('active');
            if (btnPlatformJava) btnPlatformJava.classList.remove('active');
        });
    }

    // Username Continue
    if (btnCheckoutContinue) {
        btnCheckoutContinue.addEventListener('click', () => {
            const username = checkoutUsernameInput.value.trim();
            if (!username) {
                alert('Please enter your Minecraft username to continue.');
                return;
            }
            checkoutUsername = username;

            // Set loading state
            const originalText = btnCheckoutContinue.innerHTML;
            btnCheckoutContinue.disabled = true;
            btnCheckoutContinue.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Fetching...';

            const transitionToStep2 = (finalUsername, avatarUrl) => {
                checkoutUsernameDisplay.textContent = finalUsername;
                checkoutUserAvatar.src = avatarUrl;

                // Transition to Step 2
                checkoutStep1.style.display = 'none';
                checkoutStep2.style.display = 'flex';
                if (checkoutModalCard) checkoutModalCard.classList.add('step-2-active');
                
                renderCheckoutTable();
                
                // Restore button
                btnCheckoutContinue.disabled = false;
                btnCheckoutContinue.innerHTML = originalText;
            };

            const fallback = () => {
                const displayUsername = checkoutPlatform === 'bedrock' && !username.startsWith('.') ? '.' + username : username;
                const fetchName = username.startsWith('.') ? username.substring(1) : username;
                const avatar = checkoutPlatform === 'bedrock' 
                    ? `https://mc-heads.net/avatar/steve/64`
                    : `https://mc-heads.net/avatar/${fetchName}/64`;
                transitionToStep2(displayUsername, avatar);
            };

            if (checkoutPlatform === 'java') {
                fetch(`https://playerdb.co/api/player/minecraft/${username}`)
                    .then(res => res.json())
                    .then(data => {
                        if (data.success && data.data && data.data.player) {
                            const exactName = data.data.player.username;
                            const avatar = data.data.player.avatar || `https://mc-heads.net/avatar/${data.data.player.raw_id}/64`;
                            transitionToStep2(exactName, avatar);
                        } else {
                            fallback();
                        }
                    })
                    .catch(() => {
                        fallback();
                    });
            } else {
                // Bedrock / Xbox lookup
                const cleanXboxName = username.startsWith('.') ? username.substring(1) : username;
                fetch(`https://playerdb.co/api/player/xbox/${cleanXboxName}`)
                    .then(res => res.json())
                    .then(data => {
                        if (data.success && data.data && data.data.player) {
                            const exactName = '.' + data.data.player.username;
                            const avatar = data.data.player.avatar || `https://mc-heads.net/avatar/steve/64`;
                            transitionToStep2(exactName, avatar);
                        } else {
                            fallback();
                        }
                    })
                    .catch(() => {
                        fallback();
                    });
            }
        });
    }

    // Switch Account back to step 1
    if (btnSwitchAccount) {
        btnSwitchAccount.addEventListener('click', () => {
            checkoutStep2.style.display = 'none';
            checkoutStep1.style.display = 'flex';
            if (checkoutModalCard) checkoutModalCard.classList.remove('step-2-active');
        });
    }

    // Currency Switcher
    if (checkoutCurrencySelect) {
        checkoutCurrencySelect.addEventListener('change', (e) => {
            selectedCurrency = e.target.value;
            renderCheckoutTable();
        });
    }

    // Checkout table event delegations
    if (checkoutTableBody) {
        checkoutTableBody.addEventListener('change', (e) => {
            if (e.target.classList.contains('checkout-qty-input')) {
                const id = e.target.getAttribute('data-id');
                const val = parseInt(e.target.value);
                if (!isNaN(val) && val > 0) {
                    const item = cart.find(i => i.id === id);
                    if (item) {
                        item.quantity = val;
                        updateCartUI();
                        renderCheckoutTable();
                    }
                } else {
                    e.target.value = 1;
                }
            }
        });

        checkoutTableBody.addEventListener('click', (e) => {
            const btn = e.target.closest('button');
            if (!btn) return;
            const id = btn.getAttribute('data-id');

            if (btn.classList.contains('delete-item')) {
                removeFromCart(id);
            } else if (btn.classList.contains('update-qty')) {
                const input = btn.closest('td').querySelector('.checkout-qty-input');
                const val = parseInt(input.value);
                if (!isNaN(val) && val > 0) {
                    const item = cart.find(i => i.id === id);
                    if (item) {
                        item.quantity = val;
                        updateCartUI();
                        renderCheckoutTable();
                        alert('Quantity updated successfully!');
                    }
                }
            } else if (btn.classList.contains('info-item')) {
                const product = PRODUCTS[id];
                if (product) {
                    alert(`${product.name}\n\nSelected platform: ${checkoutPlatform.toUpperCase()}\nUsername: ${checkoutUsername}`);
                }
            }
        });
    }

    // Proceed to payment page (Step 3)
    if (btnProceedCheckout) {
        btnProceedCheckout.addEventListener('click', () => {
            if (checkoutStep2) checkoutStep2.style.display = 'none';
            if (checkoutStep3) checkoutStep3.style.display = 'flex';
        });
    }

    // Back from payment to checkout table (Step 3 -> Step 2)
    if (btnPaymentBack) {
        btnPaymentBack.addEventListener('click', () => {
            if (checkoutStep3) checkoutStep3.style.display = 'none';
            if (checkoutStep2) checkoutStep2.style.display = 'flex';
        });
    }

    // Payment method selector tab switching
    paymentMethodBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            paymentMethodBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            
            const method = btn.getAttribute('data-method');
            paymentDetailPanels.forEach(panel => {
                panel.classList.remove('active');
            });
            
            if (method === 'gcash') {
                document.getElementById('panelGCash').classList.add('active');
            } else if (method === 'usdt') {
                document.getElementById('panelUSDT').classList.add('active');
            } else if (method === 'bitcoin') {
                document.getElementById('panelBitcoin').classList.add('active');
            }
        });
    });

    // Copy to clipboard for payment addresses/details
    document.querySelectorAll('.btn-copy-address').forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-copy-target');
            const targetEl = document.getElementById(targetId);
            if (targetEl) {
                const textToCopy = targetEl.textContent.trim();
                navigator.clipboard.writeText(textToCopy).then(() => {
                    const originalIcon = btn.innerHTML;
                    btn.innerHTML = '<i class="fa-solid fa-check" style="color: var(--primary);"></i>';
                    setTimeout(() => {
                        btn.innerHTML = originalIcon;
                    }, 2000);
                }).catch(err => {
                    console.error('Failed to copy text: ', err);
                });
            }
        });
    });

    // Submit payment verification proof
    if (btnPaymentSubmit) {
        btnPaymentSubmit.addEventListener('click', () => {
            // Find active payment method
            const activeMethodBtn = document.querySelector('.payment-method-btn.active');
            const method = activeMethodBtn ? activeMethodBtn.getAttribute('data-method') : 'gcash';
            
            let verificationValue = '';
            if (method === 'gcash') {
                verificationValue = gcashRefInput ? gcashRefInput.value.trim() : '';
                if (!verificationValue) {
                    alert('Please enter your 13-digit GCash Reference Number.');
                    return;
                }
                if (!/^\d{13}$/.test(verificationValue)) {
                    alert('Please enter a valid 13-digit GCash Reference Number containing only numbers.');
                    return;
                }
            } else if (method === 'usdt') {
                verificationValue = usdtHashInput ? usdtHashInput.value.trim() : '';
                if (!verificationValue) {
                    alert('Please enter the USDT transaction hash (TXID).');
                    return;
                }
                if (verificationValue.length < 10) {
                    alert('Please enter a valid USDT transaction hash.');
                    return;
                }
            } else if (method === 'bitcoin') {
                verificationValue = btcHashInput ? btcHashInput.value.trim() : '';
                if (!verificationValue) {
                    alert('Please enter the Bitcoin transaction ID (TXID).');
                    return;
                }
                if (verificationValue.length < 10) {
                    alert('Please enter a valid Bitcoin transaction ID.');
                    return;
                }
            }

            // Show submitting state
            const originalBtnContent = btnPaymentSubmit.innerHTML;
            btnPaymentSubmit.disabled = true;
            btnPaymentSubmit.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Submitting...';

            setTimeout(() => {
                // Success: Transition to Step 4 (Success Screen)
                btnPaymentSubmit.disabled = false;
                btnPaymentSubmit.innerHTML = originalBtnContent;
                
                if (checkoutStep3) checkoutStep3.style.display = 'none';
                if (paymentSuccessView) paymentSuccessView.style.display = 'flex';
                if (successAccountDisplay) {
                    successAccountDisplay.textContent = `${checkoutUsername} (${checkoutPlatform.toUpperCase()})`;
                }
                
                // Clear cart state
                cart = [];
                updateCartUI();
            }, 1200);
        });
    }

    // Success Close Button
    if (btnSuccessClose) {
        btnSuccessClose.addEventListener('click', () => {
            closeCheckoutModal();
        });
    }

    // Event Listeners for Cart Drawer
    if (btnCartToggle) btnCartToggle.addEventListener('click', toggleCart);
    if (btnCartClose) btnCartClose.addEventListener('click', toggleCart);
    if (cartOverlay) cartOverlay.addEventListener('click', toggleCart);

    // Add to Cart buttons
    document.querySelectorAll('.btn-add-cart').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const card = e.target.closest('.merch-card');
            const id = card.getAttribute('data-id');
            addToCart(id);
        });
    });

    // Delegated controls inside Cart Drawer
    cartItemsList.addEventListener('click', (e) => {
        const target = e.target;
        if (target.classList.contains('btn-qty-plus')) {
            changeQuantity(target.getAttribute('data-id'), 1);
        } else if (target.classList.contains('btn-qty-minus')) {
            changeQuantity(target.getAttribute('data-id'), -1);
        } else if (target.classList.contains('btn-remove-item')) {
            removeFromCart(target.getAttribute('data-id'));
        }
    });

    // Checkout triggers
    if (btnCheckout) btnCheckout.addEventListener('click', openCheckoutModal);
    if (btnModalClose) btnModalClose.addEventListener('click', closeCheckoutModal);

    // Filters & Search
    const merchSearch = document.getElementById('merchSearch');
    const filterBtns = document.querySelectorAll('.filter-btn');
    const productCards = document.querySelectorAll('.merch-card');

    const filterProducts = () => {
        const query = merchSearch ? merchSearch.value.toLowerCase().trim() : '';
        const activeFilterBtn = document.querySelector('.filter-btn.active');
        const category = activeFilterBtn ? activeFilterBtn.getAttribute('data-filter') : 'all';

        productCards.forEach(card => {
            const name = card.getAttribute('data-name').toLowerCase();
            const cardCategory = card.getAttribute('data-category');
            
            const matchesSearch = name.includes(query);
            const matchesCategory = category === 'all' || cardCategory === category;

            if (matchesSearch && matchesCategory) {
                card.style.display = 'flex';
            } else {
                card.style.display = 'none';
            }
        });
    };

    if (merchSearch) merchSearch.addEventListener('input', filterProducts);
    
    filterBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            filterBtns.forEach(b => b.classList.remove('active'));
            e.target.classList.add('active');
            filterProducts();
        });
    });

    // Mobile Navigation Toggle
    const mobileNavToggle = document.getElementById('mobileNavToggle');
    const navLinks = document.getElementById('navLinks');

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
    }

    // Initialize
    updateCartUI();
});
