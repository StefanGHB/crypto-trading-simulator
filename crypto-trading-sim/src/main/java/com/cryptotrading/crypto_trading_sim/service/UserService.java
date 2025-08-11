package com.cryptotrading.crypto_trading_sim.service;

import com.cryptotrading.crypto_trading_sim.dao.UserDao;
import com.cryptotrading.crypto_trading_sim.dao.UserHoldingDao;
import com.cryptotrading.crypto_trading_sim.dao.TransactionDao;
import com.cryptotrading.crypto_trading_sim.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for User-related business logic
 */
@Service
@Transactional
public class UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserHoldingDao userHoldingDao;

    @Autowired
    private TransactionDao transactionDao;

    @Value("${trading.default.initial.balance:10000.00}")
    private BigDecimal defaultInitialBalance;

    // ===============================================
    // USER MANAGEMENT
    // ===============================================

    /**
     * Create a new user with default initial balance
     */
    public User createUser(String username, String email) {
        validateUserInput(username, email);

        if (userDao.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        if (userDao.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        User newUser = new User(username, email, defaultInitialBalance);
        return userDao.save(newUser);
    }

    /**
     * Create a new user with custom initial balance
     */
    public User createUser(String username, String email, BigDecimal initialBalance) {
        validateUserInput(username, email);
        validateInitialBalance(initialBalance);

        if (userDao.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        if (userDao.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        User newUser = new User(username, email, initialBalance);
        return userDao.save(newUser);
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long userId) {
        return userDao.findById(userId);
    }

    /**
     * Get user by username
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userDao.findByUsername(username);
    }

    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userDao.findByEmail(email);
    }

    /**
     * Get all active users
     */
    @Transactional(readOnly = true)
    public List<User> getAllActiveUsers() {
        return userDao.findAllActive();
    }

    /**
     * Update user information
     */
    public User updateUser(User user) {
        validateUserForUpdate(user);

        Optional<User> existingUser = userDao.findById(user.getId());
        if (existingUser.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + user.getId());
        }

        // Check username uniqueness (if changed)
        if (!existingUser.get().getUsername().equals(user.getUsername())) {
            if (userDao.existsByUsername(user.getUsername())) {
                throw new IllegalArgumentException("Username already exists: " + user.getUsername());
            }
        }

        // Check email uniqueness (if changed)
        if (!existingUser.get().getEmail().equals(user.getEmail())) {
            if (userDao.existsByEmail(user.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + user.getEmail());
            }
        }

        boolean updated = userDao.update(user);
        if (!updated) {
            throw new RuntimeException("Failed to update user with ID: " + user.getId());
        }

        return userDao.findById(user.getId()).orElseThrow();
    }

    // ===============================================
    // BALANCE MANAGEMENT
    // ===============================================

    /**
     * Get current balance for user
     */
    @Transactional(readOnly = true)
    public BigDecimal getCurrentBalance(Long userId) {
        validateUserId(userId);
        return userDao.getCurrentBalance(userId);
    }

    /**
     * Check if user can afford a specific amount
     */
    @Transactional(readOnly = true)
    public boolean canAfford(Long userId, BigDecimal amount) {
        validateUserId(userId);
        validateAmount(amount);
        return userDao.canAfford(userId, amount);
    }

    /**
     * Update user balance (internal method for trading operations)
     */
    protected boolean updateBalance(Long userId, BigDecimal newBalance) {
        validateUserId(userId);
        validateAmount(newBalance);
        return userDao.updateBalance(userId, newBalance);
    }

    /**
     * Deduct amount from user balance
     */
    protected boolean deductFromBalance(Long userId, BigDecimal amount) {
        validateUserId(userId);
        validateAmount(amount);

        BigDecimal currentBalance = getCurrentBalance(userId);
        if (currentBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Current: " + currentBalance + ", Required: " + amount);
        }

        BigDecimal newBalance = currentBalance.subtract(amount);
        return updateBalance(userId, newBalance);
    }

    /**
     * Add amount to user balance
     */
    protected boolean addToBalance(Long userId, BigDecimal amount) {
        validateUserId(userId);
        validateAmount(amount);

        BigDecimal currentBalance = getCurrentBalance(userId);
        BigDecimal newBalance = currentBalance.add(amount);
        return updateBalance(userId, newBalance);
    }

    // ===============================================
    // ACCOUNT RESET
    // ===============================================

    /**
     * Reset user account to initial state
     */
    public boolean resetUserAccount(Long userId) {
        validateUserId(userId);

        try {
            // Delete all holdings
            userHoldingDao.deleteAllUserHoldings(userId);

            // Delete all transactions
            transactionDao.deleteAllUserTransactions(userId);

            // Reset balance and statistics
            boolean resetSuccessful = userDao.resetAccount(userId);

            if (!resetSuccessful) {
                throw new RuntimeException("Failed to reset user account");
            }

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Error resetting user account: " + e.getMessage(), e);
        }
    }

    // ===============================================
    // USER STATISTICS
    // ===============================================

    /**
     * Get comprehensive user statistics
     */
    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics(Long userId) {
        validateUserId(userId);

        Optional<User> userOpt = getUserById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }

        User user = userOpt.get();
        BigDecimal portfolioValue = userHoldingDao.getTotalPortfolioValue(userId);
        BigDecimal totalInvested = userHoldingDao.getTotalInvestedByUser(userId);
        BigDecimal unrealizedPnL = userHoldingDao.getTotalUnrealizedProfitLoss(userId);
        BigDecimal realizedPnL = transactionDao.getTotalRealizedProfitLoss(userId);
        long transactionCount = transactionDao.getUserTransactionCount(userId);
        long holdingCount = userHoldingDao.getUserHoldingCount(userId);

        return new UserStatistics(
                user.getCurrentBalance(),
                portfolioValue,
                user.getCurrentBalance().add(portfolioValue), // Total account value
                totalInvested,
                unrealizedPnL,
                realizedPnL,
                unrealizedPnL.add(realizedPnL), // Total P&L
                transactionCount,
                holdingCount
        );
    }

    /**
     * Get user portfolio summary
     */
    @Transactional(readOnly = true)
    public UserPortfolioSummary getUserPortfolioSummary(Long userId) {
        validateUserId(userId);

        UserStatistics stats = getUserStatistics(userId);
        BigDecimal totalAccountValue = stats.getTotalAccountValue();
        BigDecimal initialBalance = userDao.findById(userId).get().getInitialBalance();
        BigDecimal overallPnL = totalAccountValue.subtract(initialBalance);

        return new UserPortfolioSummary(
                stats.getCashBalance(),
                stats.getPortfolioValue(),
                totalAccountValue,
                initialBalance,
                overallPnL,
                stats.getHoldingCount(),
                stats.getTransactionCount()
        );
    }

    // ===============================================
    // USER VALIDATION
    // ===============================================

    /**
     * Deactivate user account (soft delete)
     */
    public boolean deactivateUser(Long userId) {
        validateUserId(userId);
        return userDao.deactivate(userId);
    }

    /**
     * Check if user exists and is active
     */
    @Transactional(readOnly = true)
    public boolean isUserActiveById(Long userId) {
        Optional<User> user = userDao.findById(userId);
        return user.isPresent() && user.get().isActive();
    }

    /**
     * Check if username exists
     */
    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return userDao.existsByUsername(username);
    }

    /**
     * Check if email exists
     */
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userDao.existsByEmail(email);
    }

    // ===============================================
    // VALIDATION METHODS
    // ===============================================

    private void validateUserInput(String username, String email) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private void validateUserForUpdate(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID cannot be null for update");
        }

        validateUserInput(user.getUsername(), user.getEmail());
        validateInitialBalance(user.getInitialBalance());
        validateAmount(user.getCurrentBalance());
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID: " + userId);
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }
    }

    private void validateInitialBalance(BigDecimal initialBalance) {
        if (initialBalance == null) {
            throw new IllegalArgumentException("Initial balance cannot be null");
        }

        if (initialBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Initial balance must be positive: " + initialBalance);
        }

        if (initialBalance.compareTo(new BigDecimal("1000000")) > 0) {
            throw new IllegalArgumentException("Initial balance cannot exceed 1,000,000");
        }
    }

    // ===============================================
    // INNER CLASSES FOR STATISTICS
    // ===============================================

    public static class UserStatistics {
        private final BigDecimal cashBalance;
        private final BigDecimal portfolioValue;
        private final BigDecimal totalAccountValue;
        private final BigDecimal totalInvested;
        private final BigDecimal unrealizedPnL;
        private final BigDecimal realizedPnL;
        private final BigDecimal totalPnL;
        private final long transactionCount;
        private final long holdingCount;

        public UserStatistics(BigDecimal cashBalance, BigDecimal portfolioValue, BigDecimal totalAccountValue,
                              BigDecimal totalInvested, BigDecimal unrealizedPnL, BigDecimal realizedPnL,
                              BigDecimal totalPnL, long transactionCount, long holdingCount) {
            this.cashBalance = cashBalance;
            this.portfolioValue = portfolioValue;
            this.totalAccountValue = totalAccountValue;
            this.totalInvested = totalInvested;
            this.unrealizedPnL = unrealizedPnL;
            this.realizedPnL = realizedPnL;
            this.totalPnL = totalPnL;
            this.transactionCount = transactionCount;
            this.holdingCount = holdingCount;
        }

        // Getters
        public BigDecimal getCashBalance() { return cashBalance; }
        public BigDecimal getPortfolioValue() { return portfolioValue; }
        public BigDecimal getTotalAccountValue() { return totalAccountValue; }
        public BigDecimal getTotalInvested() { return totalInvested; }
        public BigDecimal getUnrealizedPnL() { return unrealizedPnL; }
        public BigDecimal getRealizedPnL() { return realizedPnL; }
        public BigDecimal getTotalPnL() { return totalPnL; }
        public long getTransactionCount() { return transactionCount; }
        public long getHoldingCount() { return holdingCount; }
    }

    public static class UserPortfolioSummary {
        private final BigDecimal cashBalance;
        private final BigDecimal portfolioValue;
        private final BigDecimal totalAccountValue;
        private final BigDecimal initialBalance;
        private final BigDecimal overallPnL;
        private final long holdingCount;
        private final long transactionCount;

        public UserPortfolioSummary(BigDecimal cashBalance, BigDecimal portfolioValue, BigDecimal totalAccountValue,
                                    BigDecimal initialBalance, BigDecimal overallPnL, long holdingCount, long transactionCount) {
            this.cashBalance = cashBalance;
            this.portfolioValue = portfolioValue;
            this.totalAccountValue = totalAccountValue;
            this.initialBalance = initialBalance;
            this.overallPnL = overallPnL;
            this.holdingCount = holdingCount;
            this.transactionCount = transactionCount;
        }

        // Getters
        public BigDecimal getCashBalance() { return cashBalance; }
        public BigDecimal getPortfolioValue() { return portfolioValue; }
        public BigDecimal getTotalAccountValue() { return totalAccountValue; }
        public BigDecimal getInitialBalance() { return initialBalance; }
        public BigDecimal getOverallPnL() { return overallPnL; }
        public long getHoldingCount() { return holdingCount; }
        public long getTransactionCount() { return transactionCount; }
    }
}