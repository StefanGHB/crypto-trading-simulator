
class CryptoTradingApp {
    constructor() {
        this.config = {
            apiBaseUrl: '',
            websocketUrl: '/ws',
            reconnectInterval: 3000,

        };

        // Application State
        this.state = {
            currentUser: null,
            cryptocurrencies: new Map(),
            transactions: [],
            balance: 0,
            isLoading: true,
            connectionStatus: 'disconnected',
            selectedCrypto: null,
            tradeType: 'buy',
            availableCryptoQuantity: 0
        };

        // WebSocket connection
        this.stompClient = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.shouldReconnect = true; // 🛑 NEW: Control flag for reconnection
        this.reconnectTimer = null; // 🛑 NEW: Timer reference for cleanup

        // DOM element cache
        this.elements = {};

        // ⚡ PERFORMANCE COUNTERS
        this.stats = {
            totalUpdatesReceived: 0,
            totalUpdatesProcessed: 0,
            lastUpdateTime: 0
        };

        // ⚡ PERFORMANCE OPTIMIZATIONS
        this.performanceOptimizations = {
            // DOM Fragment for batch updates
            domFragment: null,

            // Throttled functions
            throttledPriceUpdate: this.throttle(this.updatePriceInTableInstant.bind(this), 16), // 60fps
            throttledLastUpdate: this.throttle(this.updateLastUpdateTimeInstant.bind(this), 1000), // 1sec

            // Intersection Observer for lazy loading
            intersectionObserver: null,

            // RequestAnimationFrame for smooth updates
            rafId: null,
            pendingUpdates: new Map(),

            // Memory pool for reusing DOM elements
            elementPool: new Map()
        };

        // ⚡ SCROLL PERFORMANCE OPTIMIZATIONS
        this.scrollOptimizations = {
            // Passive scroll listeners for better performance
            passiveScrollOptions: { passive: true, capture: false },

            // Debounced scroll handler
            debouncedScrollHandler: this.debounce(this.handleScroll.bind(this), 10),

            // RAF for scroll-triggered updates
            scrollRafId: null,

            // Viewport tracking
            viewportHeight: window.innerHeight,
            lastScrollTop: 0,

            // Smooth scroll settings
            smoothScrollEnabled: true
        };

        // Initialize application
        this.init();
    }

    // ===============================================
    // ⚡ SCROLL PERFORMANCE UTILITIES
    // ===============================================

    /**
     * ⚡ Setup smooth scroll optimizations
     */
    setupScrollOptimizations() {
        // Set CSS scroll behavior
        document.documentElement.style.scrollBehavior = 'smooth';
        document.body.style.scrollBehavior = 'smooth';

        // Optimize viewport
        this.scrollOptimizations.viewportHeight = window.innerHeight;

        // Add passive scroll listeners
        window.addEventListener('scroll', this.scrollOptimizations.debouncedScrollHandler,
            this.scrollOptimizations.passiveScrollOptions);

        // Add resize listener
        window.addEventListener('resize', () => {
            this.scrollOptimizations.viewportHeight = window.innerHeight;
        }, this.scrollOptimizations.passiveScrollOptions);

        console.log('⚡ Scroll optimizations enabled');
    }

    /**
     * ⚡ Handle scroll events with performance optimizations
     */
    handleScroll() {
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;

        // Cancel previous RAF if pending
        if (this.scrollOptimizations.scrollRafId) {
            cancelAnimationFrame(this.scrollOptimizations.scrollRafId);
        }

        // Schedule scroll updates in RAF
        this.scrollOptimizations.scrollRafId = requestAnimationFrame(() => {
            this.processScrollUpdates(scrollTop);
            this.scrollOptimizations.scrollRafId = null;
        });

        this.scrollOptimizations.lastScrollTop = scrollTop;
    }

    /**
     * ⚡ Process scroll updates efficiently
     */
    processScrollUpdates(scrollTop) {
        // Scroll-based optimizations can be added here
        // For now, just ensure smooth performance

        // Update any scroll-dependent UI elements
        if (this.elements.header && scrollTop > 100) {
            // Add header shadow on scroll
            this.elements.header.style.boxShadow = '0 4px 20px rgba(0, 0, 0, 0.3)';
        } else if (this.elements.header) {
            this.elements.header.style.boxShadow = 'none';
        }
    }

    /**
     * ⚡ Force smooth scroll behavior on all elements
     */
    forceSmootScroll() {
        // Apply smooth scroll to all scrollable elements
        const scrollableElements = document.querySelectorAll('[overflow="auto"], [overflow="scroll"]');

        scrollableElements.forEach(element => {
            element.style.scrollBehavior = 'smooth';
            element.style.webkitOverflowScrolling = 'touch';
        });
    }

    /**
     * Throttle function to limit execution frequency
     */
    throttle(func, limit) {
        let inThrottle;
        return function() {
            const args = arguments;
            const context = this;
            if (!inThrottle) {
                func.apply(context, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        }
    }

    /**
     * Debounce function for delayed execution
     */
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    /**
     * ⚡ Batch DOM updates using DocumentFragment
     */
    createDocumentFragment() {
        this.performanceOptimizations.domFragment = document.createDocumentFragment();
        return this.performanceOptimizations.domFragment;
    }

    /**
     * ⚡ RequestAnimationFrame batched updates
     */
    scheduleUpdate(symbol, updateData) {
        this.performanceOptimizations.pendingUpdates.set(symbol, updateData);

        if (!this.performanceOptimizations.rafId) {
            this.performanceOptimizations.rafId = requestAnimationFrame(() => {
                this.processBatchedUpdates();
                this.performanceOptimizations.rafId = null;
            });
        }
    }

    /**
     * ⚡ Process all pending updates in a single frame
     */
    processBatchedUpdates() {
        const updates = this.performanceOptimizations.pendingUpdates;

        updates.forEach((updateData, symbol) => {
            this.performanceOptimizations.throttledPriceUpdate(symbol, updateData.price, updateData.changePercent24h, updateData.priceDirection);
        });

        updates.clear();
    }

    /**
     * ⚡ SIMPLIFIED: Setup basic intersection observer (optional)
     * 🎯 REMOVED: Complex lazy loading - not needed anymore
     */
    setupIntersectionObserver() {
        // ⚡ SIMPLIFIED: Basic observer for future enhancements only
        if ('IntersectionObserver' in window) {
            this.performanceOptimizations.intersectionObserver = new IntersectionObserver(
                (entries) => {
                    // Optional: Can be used for future optimizations
                    entries.forEach(entry => {
                        if (entry.isIntersecting && entry.target.dataset.futureOptimization) {
                            // Future optimization placeholder
                        }
                    });
                },
                {
                    rootMargin: '50px',
                    threshold: 0.1
                }
            );
        }
    }

    /**
     * ⚡ SIMPLIFIED: Basic lazy content loading (not used for transactions anymore)
     */
    loadLazyContent(element) {
        // ⚡ SIMPLIFIED: Basic implementation for future use
        if (element.dataset.lazyLoad === 'future-content') {
            // Future lazy loading implementation
            element.dataset.lazyLoad = 'loaded';
        }
    }

    // ===============================================
    // INITIALIZATION
    // ===============================================

    async init() {
        try {
            console.log('⚡ Initializing INSTANT Crypto Trading Simulator...');

            // Cache DOM elements
            this.cacheElements();

            // ⚡ Setup scroll optimizations FIRST
            this.setupScrollOptimizations();

            // Setup performance optimizations
            this.setupIntersectionObserver();

            // Force smooth scroll behavior
            this.forceSmootScroll();

            // Setup event listeners
            this.setupEventListeners();

            // Load initial data
            await this.loadInitialData();

            // Setup WebSocket connection
            this.setupWebSocket();

            // Hide loading overlay
            this.hideLoading();

            console.log('✅ INSTANT application initialized successfully');
        } catch (error) {
            console.error('❌ Failed to initialize application:', error);
            this.showError('Failed to initialize application. Please refresh the page.');
            this.hideLoading();
        }
    }

    cacheElements() {
        // Header elements
        this.elements.userBalance = document.getElementById('user-balance');
        this.elements.connectionStatus = document.getElementById('connection-status');
        this.elements.resetAccountBtn = document.getElementById('reset-account-btn');
        this.elements.lastUpdate = document.getElementById('last-update');
        this.elements.reconnectBtn = document.getElementById('reconnect-btn'); // 🛑 NEW: Manual reconnect button

        // Market elements
        this.elements.cryptoTableBody = document.getElementById('crypto-table-body');
        this.elements.cryptoTableLoading = document.getElementById('crypto-table-loading');
        this.elements.cryptoGrid = document.getElementById('crypto-grid'); // 📱 NEW: Mobile crypto grid

        // Transaction elements
        this.elements.transactionList = document.getElementById('transaction-list');
        this.elements.emptyHistory = document.getElementById('empty-history');
        this.elements.historyLoading = document.getElementById('history-loading');
        this.elements.refreshHistoryBtn = document.getElementById('refresh-history-btn');

        // Modal elements
        this.elements.tradingModal = document.getElementById('trading-modal');
        this.elements.modalTitle = document.getElementById('modal-title');
        this.elements.cryptoSymbolAvatar = document.getElementById('crypto-symbol-avatar');
        this.elements.cryptoName = document.getElementById('crypto-name');
        this.elements.cryptoPrice = document.getElementById('crypto-price');
        this.elements.buyTab = document.getElementById('buy-tab');
        this.elements.sellTab = document.getElementById('sell-tab');
        this.elements.amountInput = document.getElementById('amount-input');
        this.elements.amountLabel = document.getElementById('amount-label');
        this.elements.availableBalance = document.getElementById('available-balance');
        this.elements.previewQuantity = document.getElementById('preview-quantity');
        this.elements.previewFee = document.getElementById('preview-fee');
        this.elements.executeTradeBtn = document.getElementById('execute-trade-btn');
        this.elements.closeModalBtn = document.getElementById('close-modal-btn');
        this.elements.cancelTradeBtn = document.getElementById('cancel-trade-btn');
        this.elements.amountError = document.getElementById('amount-error');
        this.elements.tradeForm = document.getElementById('trade-form');

        // Confirmation modal
        this.elements.confirmationModal = document.getElementById('confirmation-modal');
        this.elements.confirmTitle = document.getElementById('confirm-title');
        this.elements.confirmMessage = document.getElementById('confirm-message');
        this.elements.confirmOkBtn = document.getElementById('confirm-ok-btn');
        this.elements.confirmCancelBtn = document.getElementById('confirm-cancel-btn');

        // Toast container
        this.elements.toastContainer = document.getElementById('toast-container');

        // Loading overlay
        this.elements.loadingOverlay = document.getElementById('loading-overlay');
    }

    setupEventListeners() {
        // Reset account
        this.elements.resetAccountBtn?.addEventListener('click', () => this.showResetConfirmation());

        // 🛑 NEW: Manual reconnect button
        this.elements.reconnectBtn?.addEventListener('click', () => this.attemptManualReconnection());

        // Refresh history
        this.elements.refreshHistoryBtn?.addEventListener('click', () => this.loadTransactionHistory());

        // Modal controls
        this.elements.closeModalBtn?.addEventListener('click', () => this.closeTradingModal());
        this.elements.cancelTradeBtn?.addEventListener('click', () => this.closeTradingModal());
        this.elements.tradingModal?.addEventListener('click', (e) => {
            if (e.target === this.elements.tradingModal) {
                this.closeTradingModal();
            }
        });

        // Trade tabs
        this.elements.buyTab?.addEventListener('click', () => this.switchTradeType('buy'));
        this.elements.sellTab?.addEventListener('click', () => this.switchTradeType('sell'));

        // Amount input
        this.elements.amountInput?.addEventListener('input', () => this.updateTradePreview());
        this.elements.amountInput?.addEventListener('blur', () => this.validateAmount());

        // Trade form
        this.elements.tradeForm?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.executeTrade();
        });

        // Confirmation modal
        this.elements.confirmCancelBtn?.addEventListener('click', () => this.hideConfirmationModal());

        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                this.closeTradingModal();
                this.hideConfirmationModal();
            }
        });
    }

    // ===============================================
    // DATA LOADING
    // ===============================================

    async loadInitialData() {
        console.log('📊 Loading initial data...');

        try {
            // Load user data
            await this.loadUserData();

            // Load cryptocurrencies
            await this.loadCryptocurrencies();

            // Load transaction history
            await this.loadTransactionHistory();

        } catch (error) {
            console.error('❌ Failed to load initial data:', error);
            throw error;
        }
    }

    async loadUserData() {
        try {
            // Get first active user
            const usersResponse = await this.apiCall('/api/users/active');
            if (!usersResponse.length) {
                throw new Error('No active users found');
            }

            this.state.currentUser = usersResponse[0];
            console.log('👤 Current user:', this.state.currentUser.username);

            // Load user balance
            await this.updateUserBalance();

        } catch (error) {
            console.error('❌ Failed to load user data:', error);
            throw error;
        }
    }

    async updateUserBalance() {
        try {
            const balanceResponse = await this.apiCall(`/api/users/${this.state.currentUser.id}/balance`);
            this.state.balance = balanceResponse.currentBalance;
            this.updateBalanceDisplay();
        } catch (error) {
            console.error('❌ Failed to update balance:', error);
        }
    }

    async loadCryptocurrencies() {
        try {
            console.log('🪙 Loading cryptocurrencies...');
            this.showTableLoading(true);

            const cryptosResponse = await this.apiCall('/api/cryptocurrencies/top?limit=20');

            // Store in state
            this.state.cryptocurrencies.clear();
            cryptosResponse.forEach(crypto => {
                this.state.cryptocurrencies.set(crypto.symbol, crypto);
            });

            // ⚡ OPTIMIZED: Render table with batch updates
            this.renderCryptocurrencyTableOptimized();
            this.showTableLoading(false);

            console.log(`✅ Loaded ${cryptosResponse.length} cryptocurrencies`);
        } catch (error) {
            console.error('❌ Failed to load cryptocurrencies:', error);
            this.showTableLoading(false);
            this.showError('Failed to load cryptocurrency data');
        }
    }

    async loadTransactionHistory() {
        try {
            console.log('📋 Loading transaction history...');
            this.showHistoryLoading(true);

            const transactionsResponse = await this.apiCall(`/api/transactions/user/${this.state.currentUser.id}/export`);
            this.state.transactions = transactionsResponse;

            // ⚡ OPTIMIZED: Render transaction history with lazy loading
            this.renderTransactionHistoryOptimized();
            this.showHistoryLoading(false);

            console.log(`✅ Loaded ${transactionsResponse.length} transactions`);
        } catch (error) {
            console.error('❌ Failed to load transaction history:', error);
            this.showHistoryLoading(false);
        }
    }

    // ===============================================
    // 🎯 NEW: LOAD CRYPTO QUANTITY FOR SELL OPERATIONS
    // ===============================================

    async loadCryptoQuantity(cryptoSymbol) {
        try {
            console.log(`🔍 Loading quantity for ${cryptoSymbol}...`);

            const quantityResponse = await this.apiCall(`/api/portfolio/user/${this.state.currentUser.id}/quantity/${cryptoSymbol}`);
            this.state.availableCryptoQuantity = quantityResponse.quantity || 0;

            console.log(`✅ Available ${cryptoSymbol}: ${this.state.availableCryptoQuantity}`);
            return this.state.availableCryptoQuantity;
        } catch (error) {
            console.error(`❌ Failed to load quantity for ${cryptoSymbol}:`, error);
            this.state.availableCryptoQuantity = 0;
            return 0;
        }
    }

    // ===============================================
    // ⚡ ENHANCED WEBSOCKET CONNECTION WITH SERVER SHUTDOWN DETECTION
    // ===============================================

    setupWebSocket() {
        try {
            console.log('⚡ Setting up INSTANT WebSocket connection...');

            const socket = new SockJS(this.config.websocketUrl);
            this.stompClient = Stomp.over(socket);

            // Disable debug logging in production
            this.stompClient.debug = null;

            this.stompClient.connect({},
                (frame) => this.onWebSocketConnected(frame),
                (error) => this.onWebSocketError(error)
            );

        } catch (error) {
            console.error('❌ Failed to setup WebSocket:', error);
            this.updateConnectionStatus('error');
        }
    }

    onWebSocketConnected(frame) {
        console.log('⚡ INSTANT WebSocket connected:', frame);
        this.updateConnectionStatus('connected');
        this.reconnectAttempts = 0;
        this.shouldReconnect = true; // 🛑 NEW: Reset reconnection flag on successful connection

        // Subscribe to price updates with INSTANT handling
        this.stompClient.subscribe('/topic/prices', (message) => {
            try {
                const priceData = JSON.parse(message.body);
                this.handlePriceUpdateInstant(priceData);
            } catch (error) {
                console.error('❌ Failed to parse price update:', error);
            }
        });

        // Subscribe to status updates
        this.stompClient.subscribe('/topic/status', (message) => {
            try {
                const statusData = JSON.parse(message.body);
                console.log('📊 Status update:', statusData);

                // Handle different status types
                if (statusData.type === 'refresh_complete') {
                    this.showSuccess('Price data refreshed successfully');
                } else if (statusData.type === 'error') {
                    this.showError(statusData.message || 'WebSocket error occurred');
                }
            } catch (error) {
                console.error('❌ Failed to parse status update:', error);
            }
        });

        // Subscribe to heartbeat
        this.stompClient.subscribe('/topic/heartbeat', (message) => {
            // Connection is alive - update connection status if needed
            if (this.state.connectionStatus !== 'connected') {
                this.updateConnectionStatus('connected');
            }
        });

        // Request initial price subscription
        try {
            this.stompClient.send('/app/subscribe-prices', {}, JSON.stringify({}));
            console.log('📡 Requested INSTANT price subscription');
        } catch (error) {
            console.error('❌ Failed to request price subscription:', error);
        }
    }

    // 🛑 ENHANCED: WebSocket error handling with server shutdown detection
    onWebSocketError(error) {
        // 🛑 FIXED: Detect server shutdown without showing technical errors
        const isServerDown = this.detectServerShutdown(error);

        if (isServerDown) {
            console.log('🔌 Connection to trading server lost');
            this.handleServerShutdown();
        } else {
            console.log('⚠️ Temporary connection issue detected');
            this.handleTemporaryError(error);
        }
    }

    // 🛑 NEW: Detect if server is shut down
    detectServerShutdown(error) {
        const errorString = error.toString().toLowerCase();

        // Check for connection refused patterns that indicate server shutdown
        return errorString.includes('connection_refused') ||
               errorString.includes('lost connection') ||
               errorString.includes('net::err_connection_refused') ||
               errorString.includes('connection refused') ||
               errorString.includes('econnrefused') ||
               errorString.includes('err_connection_reset') ||
               errorString.includes('connection_reset') ||
               errorString.includes('websocket connection') ||
               errorString.includes('xhr_streaming');
    }

    // 🛑 NEW: Handle server shutdown gracefully
    handleServerShutdown() {
        console.log('🛑 Server appears to be offline - stopping reconnection attempts');
        this.updateConnectionStatus('server_offline');
        this.stopAllReconnectionAttempts();
        this.showFriendlyOfflineMessage(); // 🛑 FIXED: Friendly message
    }

    // 🛑 NEW: Handle temporary errors (network issues, etc.)
    handleTemporaryError(error) {
        console.log('🔄 Temporary connection error - will attempt reconnection');
        this.updateConnectionStatus('error');
        this.scheduleReconnect();
    }

    // 🛑 NEW: Stop all reconnection attempts
    stopAllReconnectionAttempts() {
        this.shouldReconnect = false;

        // Clear any pending reconnection timer
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }

        // Properly disconnect STOMP client
        if (this.stompClient && this.stompClient.connected) {
            try {
                this.stompClient.disconnect();
            } catch (e) {
                console.log('Note: STOMP disconnect threw error (expected when server is down):', e.message);
            }
        }

        this.stompClient = null;
        console.log('🧹 All reconnection attempts stopped and connections cleaned up');
    }

    // 🛑 FIXED: Show friendly offline message
    showFriendlyOfflineMessage() {
        this.showError(
            '⚡ Live market data reconnecting... Stand by!',
            8000 // Show for 8 seconds
        );
    }

    // 🛑 NEW: Manual reconnection attempt
    attemptManualReconnection() {
        console.log('🔄 Manual reconnection attempt requested by user');

        // Reset connection state
        this.shouldReconnect = true;
        this.reconnectAttempts = 0;
        this.updateConnectionStatus('connecting');

        // Clear any existing connections
        if (this.stompClient) {
            try {
                this.stompClient.disconnect();
            } catch (e) {
                // Ignore disconnect errors
            }
            this.stompClient = null;
        }

        // Attempt new connection
        this.setupWebSocket();
    }

    // 🛑 ENHANCED: Smart reconnection scheduling
    scheduleReconnect() {
        // 🛑 NEW: Check if we should stop reconnecting (server offline)
        if (!this.shouldReconnect || this.state.connectionStatus === 'server_offline') {
            console.log('🚫 Reconnection blocked - server appears to be offline');
            return;
        }

        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('❌ Max reconnection attempts reached');
            this.updateConnectionStatus('failed');
            this.showError('Connection failed. Please refresh the page.');
            return;
        }

        this.reconnectAttempts++;
        this.updateConnectionStatus('reconnecting');

        // Fast reconnect for real-time performance
        let delay = Math.min(this.config.reconnectInterval * this.reconnectAttempts, 10000);
        delay = Math.max(delay, 1000);

        console.log(`🔄 Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`);

        // 🛑 NEW: Store timer reference for cleanup
        this.reconnectTimer = setTimeout(() => {
            if (this.shouldReconnect && this.state.connectionStatus !== 'server_offline') {
                this.setupWebSocket();
            }
        }, delay);
    }

    // ===============================================
    // ⚡ INSTANT PRICE UPDATE HANDLING - ZERO DELAYS WITH OPTIMIZATIONS
    // ===============================================

    handlePriceUpdateInstant(priceData) {
        try {
            // Update statistics
            this.stats.totalUpdatesReceived++;
            this.stats.lastUpdateTime = Date.now();

            console.log('⚡ INSTANT price update received:', priceData);

            // Handle different update formats
            if (priceData.type === 'batch_price_update') {
                // Handle batch updates
                if (priceData.updates && Array.isArray(priceData.updates)) {
                    console.log(`📦 Processing INSTANT batch with ${priceData.updates.length} items`);
                    priceData.updates.forEach(update => {
                        this.processIndividualPriceUpdateInstant(update);
                    });
                }
            } else if (priceData.type === 'initial_prices') {
                // Handle initial price data on connection
                console.log('🎯 Processing INSTANT initial prices');
                if (priceData.data) {
                    Object.entries(priceData.data).forEach(([symbol, price]) => {
                        this.processIndividualPriceUpdateInstant({ symbol, price });
                    });
                }
            } else if (priceData.symbol && priceData.price !== undefined) {
                // ⚡ Handle INSTANT individual price updates (OPTIMIZED PATH)
                console.log(`⚡ INSTANT update for ${priceData.symbol}: ${this.formatPrice(priceData.price)}`);
                this.processIndividualPriceUpdateInstant(priceData);
            } else {
                console.log('📊 Other update:', priceData);
                return;
            }

            // ⚡ OPTIMIZED: Throttled timestamp update
            this.performanceOptimizations.throttledLastUpdate();

        } catch (error) {
            console.error('❌ Failed to handle INSTANT price update:', error);
        }
    }

    // ⚡ Process individual price update with BATCHED OPTIMIZATIONS AND PRICE DIRECTION
    processIndividualPriceUpdateInstant(updateData) {
        try {
            const { symbol, price, change24h, changePercent24h } = updateData;

            if (!symbol || price === undefined) {
                console.warn('⚠️ Invalid price update data:', updateData);
                return;
            }

            // 🎯 TRACK PRICE CHANGES FOR COLOR INDICATION
            let priceDirection = 'neutral';

            // Update state INSTANTLY
            if (this.state.cryptocurrencies.has(symbol)) {
                const crypto = this.state.cryptocurrencies.get(symbol);
                const oldPrice = crypto.currentPrice;
                const newPrice = price;

                // 🔥 DETERMINE PRICE DIRECTION
                if (newPrice > oldPrice) {
                    priceDirection = 'up';
                } else if (newPrice < oldPrice) {
                    priceDirection = 'down';
                } else {
                    priceDirection = 'neutral';
                }

                crypto.currentPrice = price;
                if (change24h !== undefined) crypto.priceChange24h = change24h;
                if (changePercent24h !== undefined) crypto.priceChangePercent24h = changePercent24h;

                // 🔧 ПОКАЗВА ТОЧНИТЕ ПРОМЕНИ С 5 ЗНАКА
                console.log(`💰 INSTANT update ${symbol}: ${this.formatPrice(price)} (was: ${this.formatPrice(oldPrice)}) - Direction: ${priceDirection}`);
            } else {
                console.warn(`⚠️ Received update for unknown crypto: ${symbol}`);
                return;
            }

            // ⚡ OPTIMIZED: Schedule batched UI update with price direction
            this.scheduleUpdate(symbol, {
                price,
                changePercent24h: changePercent24h !== undefined ? changePercent24h : null,
                priceDirection // 🆕 Pass direction to UI
            });

            // ⚡ Update modal INSTANTLY if open for this crypto
            if (this.state.selectedCrypto && this.state.selectedCrypto.symbol === symbol) {
                this.elements.cryptoPrice.textContent = this.formatPrice(price);
                this.updateTradePreview();
            }

            // Update statistics
            this.stats.totalUpdatesProcessed++;

        } catch (error) {
            console.error(`❌ Failed to process INSTANT price update for ${updateData.symbol}:`, error);
        }
    }

    // ===============================================
    // ⚡ OPTIMIZED UI RENDERING WITH RESPONSIVE SUPPORT
    // ===============================================

    /**
     * ⚡ OPTIMIZED: Render cryptocurrency table AND cards with batch DOM updates
     * 📱 RESPONSIVE: Now renders both desktop table and mobile/tablet cards
     */
    renderCryptocurrencyTableOptimized() {
        const tbody = this.elements.cryptoTableBody;
        const cryptoGrid = this.elements.cryptoGrid;

        if (!tbody && !cryptoGrid) return;

        const cryptos = Array.from(this.state.cryptocurrencies.values())
            .sort((a, b) => a.marketCapRank - b.marketCapRank);

        // 🖥️ DESKTOP: Render table rows
        if (tbody) {
            const tableFragment = this.createDocumentFragment();
            cryptos.forEach((crypto, index) => {
                const row = this.createCryptoRow(crypto, index + 1);
                tableFragment.appendChild(row);
            });
            tbody.innerHTML = '';
            tbody.appendChild(tableFragment);
            console.log(`🖥️ Desktop table rendered with ${cryptos.length} rows`);
        }

        // 📱 MOBILE/TABLET: Render cards
        if (cryptoGrid) {
            const cardFragment = this.createDocumentFragment();
            cryptos.forEach((crypto, index) => {
                const card = this.createCryptoCard(crypto, index + 1);
                cardFragment.appendChild(card);
            });
            cryptoGrid.innerHTML = '';
            cryptoGrid.appendChild(cardFragment);
            console.log(`📱 Mobile/tablet cards rendered with ${cryptos.length} cards`);
        }
    }

    /**
     * ⚡ OPTIMIZED: Render ALL transactions at once - NO LAZY LOADING
     * 🎯 FIXED: Load everything immediately for smooth scrolling
     */
    renderTransactionHistoryOptimized() {
        const container = this.elements.transactionList;
        const emptyState = this.elements.emptyHistory;

        if (!container || !emptyState) return;

        if (this.state.transactions.length === 0) {
            container.style.display = 'none';
            emptyState.style.display = 'block';
            return;
        }

        container.style.display = 'block';
        emptyState.style.display = 'none';

        // ⚡ Use DocumentFragment for batch DOM operations
        const fragment = this.createDocumentFragment();

        // Sort by date (newest first)
        const sortedTransactions = [...this.state.transactions]
            .sort((a, b) => new Date(b.dateTime) - new Date(a.dateTime));

        // 🎯 FIXED: Render ALL transactions at once
        console.log(`⚡ Rendering ALL ${sortedTransactions.length} transactions immediately`);

        sortedTransactions.forEach(transaction => {
            const item = this.createTransactionItem(transaction);
            fragment.appendChild(item);
        });

        // ⚡ Single DOM operation - ALL transactions loaded
        container.innerHTML = '';
        container.appendChild(fragment);

        console.log(`✅ ALL ${sortedTransactions.length} transactions rendered successfully`);
    }

    createCryptoRow(crypto, rank) {
        const row = document.createElement('tr');
        row.className = 'crypto-row';
        row.setAttribute('data-crypto-symbol', crypto.symbol);

        const changePercent = crypto.priceChangePercent24h || 0;
        const changeClass = changePercent >= 0 ? 'positive' : 'negative';
        const changeIcon = changePercent >= 0 ? '↗' : '↘';

        row.innerHTML = `
            <td class="col-rank">${rank}</td>
            <td class="col-name">
                <div class="crypto-info">
                    <div class="crypto-avatar">
                        <span class="crypto-symbol">${crypto.symbol}</span>
                    </div>
                    <div class="crypto-details">
                        <div class="crypto-name">${crypto.name}</div>
                        <div class="crypto-symbol-text">${crypto.symbol}</div>
                    </div>
                </div>
            </td>
            <td class="col-price">
                <div class="price-container">
                    <span class="price-value" data-price="${crypto.currentPrice}">
                        ${this.formatPrice(crypto.currentPrice)}
                    </span>
                </div>
            </td>
            <td class="col-change">
                <div class="change-container ${changeClass}">
                    <span class="change-icon">${changeIcon}</span>
                    <span class="change-percent">${this.formatPercent(changePercent)}</span>
                </div>
            </td>
            <td class="col-actions">
                <div class="action-buttons">
                    <button class="btn btn-buy btn-sm" onclick="app.openTradingModal('${crypto.symbol}', 'buy')">
                        <svg width="12" height="12" fill="currentColor" viewBox="0 0 16 16">
                            <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"/>
                        </svg>
                        Buy
                    </button>
                    <button class="btn btn-sell btn-sm" onclick="app.openTradingModal('${crypto.symbol}', 'sell')">
                        <svg width="12" height="12" fill="currentColor" viewBox="0 0 16 16">
                            <path d="M4 8a.5.5 0 0 1 .5-.5h7a.5.5 0 0 1 0 1h-7A.5.5 0 0 1 4 8z"/>
                        </svg>
                        Sell
                    </button>
                </div>
            </td>
        `;

        return row;
    }


    createCryptoCard(crypto, rank) {
        const card = document.createElement('div');
        card.className = 'crypto-card';
        card.setAttribute('data-crypto-symbol', crypto.symbol);

        const changePercent = crypto.priceChangePercent24h || 0;
        const changeClass = changePercent >= 0 ? 'positive' : 'negative';
        const changeIcon = changePercent >= 0 ? '↗' : '↘';

        card.innerHTML = `
            <div class="crypto-card-header">
                <div class="crypto-card-info">
                    <div class="crypto-card-rank">#${rank}</div>
                    <div class="crypto-card-avatar">
                        <span class="crypto-symbol">${crypto.symbol}</span>
                    </div>
                    <div class="crypto-card-details">
                        <div class="crypto-card-name">${crypto.name}</div>
                        <div class="crypto-card-symbol">${crypto.symbol}</div>
                    </div>
                </div>
                <div class="crypto-card-price">
                    <div class="crypto-card-price-value" data-price="${crypto.currentPrice}">
                        ${this.formatPrice(crypto.currentPrice)}
                    </div>
                    <div class="crypto-card-change ${changeClass}">
                        <span class="change-icon">${changeIcon}</span>
                        <span class="change-percent">${this.formatPercent(changePercent)}</span>
                    </div>
                </div>
            </div>

            <div class="crypto-card-price-section">
                <div class="crypto-card-price-value" data-price="${crypto.currentPrice}">
                    ${this.formatPrice(crypto.currentPrice)}
                </div>
            </div>

            <div class="crypto-card-actions">
                <button class="btn btn-buy" onclick="app.openTradingModal('${crypto.symbol}', 'buy')">
                    <svg width="12" height="12" fill="currentColor" viewBox="0 0 16 16">
                        <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"/>
                    </svg>
                    Buy
                </button>
                <button class="btn btn-sell" onclick="app.openTradingModal('${crypto.symbol}', 'sell')">
                    <svg width="12" height="12" fill="currentColor" viewBox="0 0 16 16">
                        <path d="M4 8a.5.5 0 0 1 .5-.5h7a.5.5 0 0 1 0 1h-7A.5.5 0 0 1 4 8z"/>
                    </svg>
                    Sell
                </button>
            </div>
        `;

        return card;
    }

    // ⚡ ENHANCED: INSTANT price update in table AND cards with GPU optimization AND PRICE COLOR INDICATION
    updatePriceInTableInstant(symbol, price, changePercent, priceDirection = 'neutral') {
        // 🖥️ Update desktop table row
        const row = document.querySelector(`tr[data-crypto-symbol="${symbol}"]`);
        if (row) {
            const priceElement = row.querySelector('.price-value');
            const changeContainer = row.querySelector('.change-container');

            if (priceElement) {

                priceElement.classList.remove('price-up', 'price-down', 'price-neutral');

                if (priceDirection === 'up') {
                    priceElement.classList.add('price-up');
                } else if (priceDirection === 'down') {
                    priceElement.classList.add('price-down');
                } else {
                    priceElement.classList.add('price-neutral');
                }

                // ⚡ Use transform for GPU-accelerated animation
                priceElement.style.transform = 'scale(1.02)';
                priceElement.textContent = this.formatPrice(price);
                priceElement.setAttribute('data-price', price);

                // ⚡ OPTIMIZED: Faster animation reset with GPU acceleration
                requestAnimationFrame(() => {
                    priceElement.style.transform = 'scale(1)';
                    priceElement.style.transition = 'transform 0.15s ease-out';

                    setTimeout(() => {
                        priceElement.style.transition = '';
                    }, 150);
                });


                setTimeout(() => {
                    priceElement.classList.remove('price-up', 'price-down', 'price-neutral');
                }, 800);
            }

            if (changeContainer && changePercent !== undefined && changePercent !== null) {
                const isPositive = changePercent >= 0;
                changeContainer.className = `change-container ${isPositive ? 'positive' : 'negative'}`;

                const changeIcon = changeContainer.querySelector('.change-icon');
                const changePercentElement = changeContainer.querySelector('.change-percent');

                if (changeIcon) changeIcon.textContent = isPositive ? '↗' : '↘';
                if (changePercentElement) changePercentElement.textContent = this.formatPercent(changePercent);
            }
        }

        // 📱 Update mobile/tablet card
        const card = document.querySelector(`.crypto-card[data-crypto-symbol="${symbol}"]`);
        if (card) {
            const priceElements = card.querySelectorAll('.crypto-card-price-value');
            const changeContainer = card.querySelector('.crypto-card-change');

            priceElements.forEach(priceElement => {

                priceElement.classList.remove('price-up', 'price-down', 'price-neutral');

                if (priceDirection === 'up') {
                    priceElement.classList.add('price-up');
                } else if (priceDirection === 'down') {
                    priceElement.classList.add('price-down');
                } else {
                    priceElement.classList.add('price-neutral');
                }

                // ⚡ Use transform for GPU-accelerated animation
                priceElement.style.transform = 'scale(1.02)';
                priceElement.textContent = this.formatPrice(price);
                priceElement.setAttribute('data-price', price);

                // ⚡ OPTIMIZED: Faster animation reset with GPU acceleration
                requestAnimationFrame(() => {
                    priceElement.style.transform = 'scale(1)';
                    priceElement.style.transition = 'transform 0.15s ease-out';

                    setTimeout(() => {
                        priceElement.style.transition = '';
                    }, 150);
                });


                setTimeout(() => {
                    priceElement.classList.remove('price-up', 'price-down', 'price-neutral');
                }, 800);
            });

            if (changeContainer && changePercent !== undefined && changePercent !== null) {
                const isPositive = changePercent >= 0;
                changeContainer.className = `crypto-card-change ${isPositive ? 'positive' : 'negative'}`;

                const changeIcon = changeContainer.querySelector('.change-icon');
                const changePercentElement = changeContainer.querySelector('.change-percent');

                if (changeIcon) changeIcon.textContent = isPositive ? '↗' : '↘';
                if (changePercentElement) changePercentElement.textContent = this.formatPercent(changePercent);
            }
        }
    }

    createTransactionItem(transaction) {
        const item = document.createElement('div');
        item.className = 'transaction-item';

        const isBuy = transaction.type === 'BUY';
        const typeClass = isBuy ? 'buy' : 'sell';
        const typeIcon = isBuy ?
            '<path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"/>' :
            '<path d="M4 8a.5.5 0 0 1 .5-.5h7a.5.5 0 0 1 0 1h-7A.5.5 0 0 1 4 8z"/>';

        // Format profit/loss
        let profitLossHtml = '';
        if (transaction.realizedPnL && transaction.realizedPnL !== 0) {
            const isProfit = transaction.realizedPnL > 0;
            const profitClass = isProfit ? 'profit' : 'loss';
            const profitSign = isProfit ? '+' : '';
            profitLossHtml = `
                <div class="transaction-pnl ${profitClass}">
                    ${profitSign}${this.formatCurrency(transaction.realizedPnL)}
                </div>
            `;
        }

        item.innerHTML = `
            <div class="transaction-icon ${typeClass}">
                <svg width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
                    ${typeIcon}
                </svg>
            </div>
            <div class="transaction-details">
                <div class="transaction-main">
                    <span class="transaction-type">${transaction.type}</span>
                    <span class="transaction-crypto">${transaction.cryptoSymbol}</span>
                    <span class="transaction-amount">${this.formatNumber(transaction.quantity)} @ ${this.formatPrice(transaction.pricePerUnit)}</span>
                </div>
                <div class="transaction-meta">
                    <span class="transaction-date">${this.formatDate(transaction.dateTime)}</span>
                    <span class="transaction-total">${this.formatCurrency(transaction.totalAmount)}</span>
                </div>
            </div>
            <div class="transaction-result">
                ${profitLossHtml}
                <div class="transaction-balance">
                    Balance: ${this.formatCurrency(transaction.balanceAfter)}
                </div>
            </div>
        `;

        return item;
    }

    // ===============================================
    // TRADING MODAL
    // ===============================================

    async openTradingModal(cryptoSymbol, tradeType = 'buy') {
        const crypto = this.state.cryptocurrencies.get(cryptoSymbol);
        if (!crypto) {
            this.showError('Cryptocurrency not found');
            return;
        }

        this.state.selectedCrypto = crypto;
        this.state.tradeType = tradeType;

        // Update modal content
        this.elements.modalTitle.textContent = `${tradeType === 'buy' ? 'Buy' : 'Sell'} ${crypto.name}`;
        this.elements.cryptoSymbolAvatar.textContent = crypto.symbol;
        this.elements.cryptoName.textContent = crypto.name;
        this.elements.cryptoPrice.textContent = this.formatPrice(crypto.currentPrice);

        // 🎯 FIXED: Load crypto quantity for SELL operations
        if (tradeType === 'sell') {
            await this.loadCryptoQuantity(cryptoSymbol);
        }

        // Switch trade type
        await this.switchTradeType(tradeType);

        // Reset form
        this.resetTradeForm();

        // Show modal
        this.elements.tradingModal.style.display = 'flex';
        this.elements.amountInput.focus();
    }

    closeTradingModal() {
        this.elements.tradingModal.style.display = 'none';
        this.state.selectedCrypto = null;
        this.state.availableCryptoQuantity = 0; // 🎯 FIXED: Reset available quantity
        this.resetTradeForm();
    }


    async switchTradeType(type) {
        this.state.tradeType = type;

        // Update tab active state
        this.elements.buyTab.classList.toggle('active', type === 'buy');
        this.elements.sellTab.classList.toggle('active', type === 'sell');

        // 🎯 NEW: Update modal title when switching trade type
        if (this.state.selectedCrypto) {
            this.elements.modalTitle.textContent = `${type === 'buy' ? 'Buy' : 'Sell'} ${this.state.selectedCrypto.name}`;
        }


        if (type === 'sell' && this.state.selectedCrypto) {
            await this.loadCryptoQuantity(this.state.selectedCrypto.symbol);
        }

        // 🎯 FIXED: Update form labels and available amounts with proper logic
        if (type === 'buy') {
            this.elements.amountLabel.textContent = 'Amount (USD)';
            this.elements.availableBalance.textContent = `Available: ${this.formatCurrency(this.state.balance)}`;
            // 🎯 FIXED: Update preview label text
            const previewLabel = document.querySelector('.preview-row span:first-child');
            if (previewLabel) previewLabel.textContent = 'You will get:';
        } else {
            this.elements.amountLabel.textContent = 'Amount (USD)';
            // 🎯 FIXED: Show available crypto quantity for SELL
            this.elements.availableBalance.textContent = `Available: ${this.formatNumber(this.state.availableCryptoQuantity)} ${this.state.selectedCrypto?.symbol || ''}`;
            // 🎯 FIXED: Update preview label text to SELL
            const previewLabel = document.querySelector('.preview-row span:first-child');
            if (previewLabel) previewLabel.textContent = 'You will sell:';
        }

        // Update execute button text
        this.elements.executeTradeBtn.querySelector('.btn-text').textContent =
            `${type === 'buy' ? 'Buy' : 'Sell'} ${this.state.selectedCrypto?.symbol || ''}`;

        // Clear form and update preview
        this.elements.amountInput.value = '';
        this.updateTradePreview();
    }

    resetTradeForm() {
        this.elements.amountInput.value = '';
        this.elements.amountError.textContent = '';
        this.elements.executeTradeBtn.disabled = true;
        this.updateTradePreview();
    }

    updateTradePreview() {
       const amount = parseFloat(this.elements.amountInput.value) || 0;
       const crypto = this.state.selectedCrypto;

       if (!crypto || amount <= 0) {
           this.elements.previewQuantity.textContent = `0.00000 ${crypto?.symbol || ''}`;
           this.elements.previewFee.textContent = '$0.00000';
           this.elements.executeTradeBtn.disabled = true;
           return;
       }

       const price = crypto.currentPrice;
       const fee = amount * 0.001; // 0.1% fee (adjust as needed)

       let quantity, isValidAmount;

       if (this.state.tradeType === 'buy') {
           quantity = (amount - fee) / price;
           isValidAmount = amount > 0 && amount <= this.state.balance;
       } else {
           // 🎯 FIXED: For SELL, calculate quantity from USD amount and validate against available crypto
           quantity = amount / price;
           const maxSellableUSD = this.state.availableCryptoQuantity * price;
           isValidAmount = amount > 0 && amount <= maxSellableUSD;
       }

       this.elements.previewQuantity.textContent = `${this.formatNumber(quantity)} ${crypto.symbol}`;

       this.elements.previewFee.textContent = this.formatCurrency(fee);

       // Enable/disable execute button
       this.elements.executeTradeBtn.disabled = !isValidAmount;

       // 🎯 FIXED: Update error message for SELL operations
       if (this.state.tradeType === 'buy') {
           if (amount > this.state.balance) {
               this.elements.amountError.textContent = 'Insufficient balance';
           } else {
               this.elements.amountError.textContent = '';
           }
       } else {
           const maxSellableUSD = this.state.availableCryptoQuantity * price;
           if (amount > maxSellableUSD) {
               this.elements.amountError.textContent = `You don't have enough ${crypto.symbol} to sell this amount!`;
           } else {
               this.elements.amountError.textContent = '';
           }
       }
    }

    validateAmount() {
       const amount = parseFloat(this.elements.amountInput.value) || 0;

       // Показва error само ако има въведена стойност и тя е <= 0
       if (this.elements.amountInput.value.trim() !== '' && amount <= 0) {
           this.elements.amountError.textContent = 'Amount must be greater than 0';
           return false;
       }

       // 🎯 FIXED: Different validation for BUY vs SELL
       if (this.state.tradeType === 'buy') {
           if (amount > this.state.balance) {
               this.elements.amountError.textContent = 'Insufficient balance';
               return false;
           }
       } else {
           const crypto = this.state.selectedCrypto;
           const maxSellableUSD = this.state.availableCryptoQuantity * crypto.currentPrice;
           if (amount > maxSellableUSD) {
               this.elements.amountError.textContent = `You don't have enough ${crypto.symbol} to sell this amount!`;
               return false;
           }
       }

       this.elements.amountError.textContent = '';
       return true;
    }

    async executeTrade() {
        if (!this.validateAmount()) return;

        const amount = parseFloat(this.elements.amountInput.value);
        const crypto = this.state.selectedCrypto;
        const tradeType = this.state.tradeType;

        try {
            // Show loading state
            this.setTradeButtonLoading(true);

            // Prepare API request
            const endpoint = tradeType === 'buy' ?
                '/api/trading/buy-by-amount' :
                '/api/trading/sell-by-amount';

            const requestBody = {
                userId: this.state.currentUser.id,
                cryptoSymbol: crypto.symbol,
                [tradeType === 'buy' ? 'amountToSpend' : 'amountToReceive']: amount
            };

            // Execute trade
            const result = await this.apiCall(endpoint, 'POST', requestBody);

            if (result.success) {
                // Show success message
                this.showSuccess(`${tradeType === 'buy' ? 'Bought' : 'Sold'} ${crypto.symbol} successfully!`);

                // Close modal
                this.closeTradingModal();

                // Refresh data
                await this.updateUserBalance();
                await this.loadTransactionHistory();

            } else {
                this.showError(result.message || 'Trade failed');
            }

        } catch (error) {
            console.error('❌ Trade execution failed:', error);
            this.showError('Trade execution failed. Please try again.');
        } finally {
            this.setTradeButtonLoading(false);
        }
    }

    setTradeButtonLoading(isLoading) {
        const btn = this.elements.executeTradeBtn;
        const btnText = btn.querySelector('.btn-text');
        const btnLoader = btn.querySelector('.btn-loader');

        if (isLoading) {
            btn.disabled = true;
            btnText.style.display = 'none';
            btnLoader.style.display = 'block';
        } else {
            btn.disabled = false;
            btnText.style.display = 'block';
            btnLoader.style.display = 'none';
        }
    }

    // ===============================================
    // ACCOUNT RESET
    // ===============================================

    showResetConfirmation() {
        this.elements.confirmTitle.textContent = 'Reset Account';
        this.elements.confirmMessage.textContent = 'Are you sure you want to reset your account? This will restore your balance to $10,000 and delete all transactions and holdings. This action cannot be undone.';

        // Update confirm button
        this.elements.confirmOkBtn.textContent = 'Reset Account';
        this.elements.confirmOkBtn.className = 'btn btn-danger';

        // Set callback
        this.elements.confirmOkBtn.onclick = () => this.executeAccountReset();

        this.showConfirmationModal();
    }

    async executeAccountReset() {
        try {
            this.hideConfirmationModal();

            const result = await this.apiCall(`/api/users/${this.state.currentUser.id}/reset`, 'POST');

            if (result.success) {
                this.showSuccess('Account reset successfully!');

                // Refresh all data
                await this.updateUserBalance();
                await this.loadTransactionHistory();

            } else {
                this.showError('Failed to reset account');
            }

        } catch (error) {
            console.error('❌ Account reset failed:', error);
            this.showError('Account reset failed. Please try again.');
        }
    }

    // ===============================================
    // UTILITY METHODS
    // ===============================================

    async apiCall(endpoint, method = 'GET', body = null) {
        try {
            const options = {
                method,
                headers: {
                    'Content-Type': 'application/json',
                },
            };

            if (body && method !== 'GET') {
                options.body = JSON.stringify(body);
            }

            const response = await fetch(this.config.apiBaseUrl + endpoint, options);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            return await response.json();

        } catch (error) {
            console.error(`❌ API call failed [${method} ${endpoint}]:`, error);
            throw error;
        }
    }

    // UI State Management
    showTableLoading(show) {
        if (this.elements.cryptoTableLoading) {
            this.elements.cryptoTableLoading.style.display = show ? 'block' : 'none';
        }
    }

    showHistoryLoading(show) {
        if (this.elements.historyLoading) {
            this.elements.historyLoading.style.display = show ? 'block' : 'none';
        }
    }

    hideLoading() {
        if (this.elements.loadingOverlay) {
            this.elements.loadingOverlay.style.display = 'none';
        }
        this.state.isLoading = false;
    }

    // 🛑 FIXED: Connection status with friendly messages
    updateConnectionStatus(status) {
        this.state.connectionStatus = status;

        const statusElement = this.elements.connectionStatus;
        if (!statusElement) return;

        const indicator = statusElement.querySelector('.status-indicator');
        const text = statusElement.querySelector('.status-text');

        statusElement.className = `connection-status ${status}`;

        // 🛑 FIXED: Friendly status messages
        const statusTexts = {
            'connected': '⚡ Live Data Connected',
            'connecting': 'Connecting to market data...',
            'reconnecting': 'Reconnecting...',
            'server_offline': '📡 Service temporarily unavailable',
            'error': 'Checking connection...',
            'failed': 'Please refresh to reconnect'
        };

        if (text) text.textContent = statusTexts[status] || 'Checking connection...';

        // 🛑 NEW: Show/hide manual reconnect button
        if (this.elements.reconnectBtn) {
            if (status === 'server_offline' || status === 'failed') {
                this.elements.reconnectBtn.style.display = 'inline-flex';
            } else {
                this.elements.reconnectBtn.style.display = 'none';
            }
        }
    }

    updateBalanceDisplay() {
        if (this.elements.userBalance) {
            this.elements.userBalance.textContent = this.formatCurrency(this.state.balance);
        }
    }

    // ⚡ OPTIMIZED: Throttled last update time display
    updateLastUpdateTimeInstant() {
        if (this.elements.lastUpdate) {
            this.elements.lastUpdate.textContent = `⚡ Last updated: ${new Date().toLocaleTimeString()} (${this.stats.totalUpdatesProcessed} updates)`;
        }
    }

    // Modal Management
    showConfirmationModal() {
        this.elements.confirmationModal.style.display = 'flex';
    }

    hideConfirmationModal() {
        this.elements.confirmationModal.style.display = 'none';
    }

    // Toast Notifications
    showSuccess(message) {
        this.showToast(message, 'success');
    }

    showError(message, duration = 5000) { // 🛑 NEW: Added duration parameter
        // 🛑 ФИЛТРИРАМЕ SockJS технически съобщения
        const msg = message.toLowerCase();

        if (msg.includes('server is currently offline') ||
            msg.includes('spring boot application') ||
            msg.includes('try reconnecting') ||
            msg.includes('whoops! lost connection') ||
            msg.includes('connection refused') ||
            msg.includes('websocket error')) {

            // 🎯 Заменяме с яко съобщение
            message = '⚡ Live market data reconnecting... Stand by!';
            duration = 6000; // Малко по-дълго за важни съобщения
        }

        this.showToast(message, 'error', duration);
    }

    showToast(message, type = 'info', duration = 5000) {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;

        const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';

        toast.innerHTML = `
            <div class="toast-content">
                <span class="toast-icon">${icon}</span>
                <span class="toast-message">${message}</span>
            </div>
            <button class="toast-close">&times;</button>
        `;

        // Add event listener for close button
        toast.querySelector('.toast-close').addEventListener('click', () => {
            this.removeToast(toast);
        });

        // Add to container
        this.elements.toastContainer.appendChild(toast);

        // Auto remove after duration
        setTimeout(() => {
            this.removeToast(toast);
        }, duration);
    }

    removeToast(toast) {
        if (toast && toast.parentNode) {
            toast.style.animation = 'slideOut 0.3s ease-in';
            setTimeout(() => {
                toast.parentNode.removeChild(toast);
            }, 300);
        }
    }

    formatPrice(price) {
        if (price == null) return '$0.00000';
        const numPrice = Number(price);


        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD',
            minimumFractionDigits: 5,
            maximumFractionDigits: 5
        }).format(numPrice);
    }

    formatCurrency(amount) {
        if (amount == null) return '$0.00000';

        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD',
            minimumFractionDigits: 5,
            maximumFractionDigits: 5
        }).format(Number(amount));
    }

    formatNumber(number, decimals = 8) {
        if (number == null) return '0.00000';
        return new Intl.NumberFormat('en-US', {
            minimumFractionDigits: 5,
            maximumFractionDigits: decimals
        }).format(Number(number));
    }

    formatPercent(percent) {
        if (percent == null) return '0.00%';
        const sign = percent >= 0 ? '+' : '';
        return `${sign}${Number(percent).toFixed(2)}%`;
    }

    formatDate(dateString) {

        const date = new Date(dateString);
        const correctedDate = new Date(date.getTime() - (3 * 60 * 60 * 1000));

        return correctedDate.toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }
}

// ===============================================
// APPLICATION INITIALIZATION
// ===============================================

// Global app instance
let app;

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    console.log('⚡ DOM loaded, initializing INSTANT Crypto Trading App...');
    app = new CryptoTradingApp();
});

// 🛑 ENHANCED: Cleanup on page unload
window.addEventListener('beforeunload', () => {
    if (app && app.stompClient) {
        app.shouldReconnect = false; // 🛑 NEW: Prevent reconnection attempts during page unload
        try {
            app.stompClient.disconnect();
        } catch (e) {
            // Ignore disconnect errors during page unload
        }
    }

        // ⚡ CLEANUP: Remove scroll listeners
        if (app && app.scrollOptimizations) {
            window.removeEventListener('scroll', app.scrollOptimizations.debouncedScrollHandler);

            if (app.scrollOptimizations.scrollRafId) {
                cancelAnimationFrame(app.scrollOptimizations.scrollRafId);
            }
        }

    // ⚡ CLEANUP: Disconnect intersection observer
    if (app && app.performanceOptimizations.intersectionObserver) {
        app.performanceOptimizations.intersectionObserver.disconnect();
    }
});

// ⚡ PERFORMANCE MONITORING
setInterval(() => {
    if (app && app.stats) {
        console.log(`⚡ PERFORMANCE: ${app.stats.totalUpdatesProcessed}/${app.stats.totalUpdatesReceived} updates processed (${((app.stats.totalUpdatesProcessed/app.stats.totalUpdatesReceived)*100).toFixed(1)}% success rate)`);
    }
}, 30000);