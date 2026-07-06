document.addEventListener('DOMContentLoaded', () => {
    // Merch products database
    const PRODUCTS = {
        tshirt: { name: 'Premium Erp T-Shirt', price: 24.99, image: 'merch_tshirt.png' },
        hoodie: { name: 'Challenger Hoodie', price: 49.99, image: 'merch_hoodie.png' },
        mug: { name: 'Glossy Ceramic Mug', price: 14.99, image: 'merch_mug.png' },
        stickers: { name: 'Holographic Sticker Sheet', price: 5.99, image: 'merch_stickers.png' }
    };

    // State
    let cart = JSON.parse(localStorage.getItem('erpsmp_cart')) || [];

    // DOM Elements
    const btnCartToggle = document.getElementById('btnCartToggle');
    const btnCartClose = document.getElementById('btnCartClose');
    const cartDrawer = document.getElementById('cartDrawer');
    const cartOverlay = document.getElementById('cartOverlay');
    const cartCountBadge = document.getElementById('cartCountBadge');
    const cartItemsList = document.getElementById('cartItemsList');
    const cartSubtotal = document.getElementById('cartSubtotal');
    const btnCheckout = document.getElementById('btnCheckout');
    
    const checkoutModal = document.getElementById('checkoutModal');
    const btnModalClose = document.getElementById('btnModalClose');
    const modalOrderSummary = document.getElementById('modalOrderSummary');
    const modalTotal = document.getElementById('modalTotal');
    const btnPayNow = document.getElementById('btnPayNow');
    const toast = document.getElementById('copyToast');

    // UI Updates
    const saveCart = () => {
        localStorage.setItem('erpsmp_cart', JSON.stringify(cart));
    };

    const updateCartUI = () => {
        saveCart();
        
        // Update Count Badges
        const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);
        cartCountBadge.textContent = totalItems;

        // Render List
        if (cart.length === 0) {
            cartItemsList.innerHTML = `
                <div class="cart-empty-state">
                    <i class="fa-solid fa-basket-shopping"></i>
                    <p>Your cart is empty.</p>
                </div>
            `;
            cartSubtotal.textContent = '$0.00';
            btnCheckout.disabled = true;
            return;
        }

        btnCheckout.disabled = false;
        let subtotal = 0;
        cartItemsList.innerHTML = '';

        cart.forEach(item => {
            const product = PRODUCTS[item.id];
            const itemTotal = product.price * item.quantity;
            subtotal += itemTotal;

            const itemEl = document.createElement('div');
            itemEl.className = 'cart-item';
            itemEl.innerHTML = `
                <img src="${product.image}" alt="${product.name}" class="cart-item-img">
                <div class="cart-item-info">
                    <h4>${product.name}</h4>
                    <span class="cart-item-price">$${product.price.toFixed(2)}</span>
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
            cartItemsList.appendChild(itemEl);
        });

        cartSubtotal.textContent = `$${subtotal.toFixed(2)}`;
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
        }
    };

    const removeFromCart = (productId) => {
        cart = cart.filter(item => item.id !== productId);
        updateCartUI();
    };

    const toggleCart = () => {
        cartDrawer.classList.toggle('active');
        cartOverlay.classList.toggle('active');
    };

    const showToast = () => {
        toast.classList.add('show');
        setTimeout(() => {
            toast.classList.remove('show');
        }, 2000);
    };

    // Checkout Simulation
    const openCheckoutModal = () => {
        toggleCart(); // Close drawer
        
        // Populate Summary
        let total = 0;
        modalOrderSummary.innerHTML = '';
        
        cart.forEach(item => {
            const product = PRODUCTS[item.id];
            const itemTotal = product.price * item.quantity;
            total += itemTotal;

            const row = document.createElement('div');
            row.className = 'summary-item-row';
            row.innerHTML = `
                <span>${product.name} (x${item.quantity})</span>
                <span>$${itemTotal.toFixed(2)}</span>
            `;
            modalOrderSummary.appendChild(row);
        });

        modalTotal.textContent = `$${total.toFixed(2)}`;
        checkoutModal.classList.add('active');
    };

    const closeCheckoutModal = () => {
        checkoutModal.classList.remove('active');
    };

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
    
    if (btnPayNow) {
        btnPayNow.addEventListener('click', () => {
            alert('🎉 Thank you for your order! Payment simulated successfully.');
            cart = [];
            updateCartUI();
            closeCheckoutModal();
        });
    }

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
