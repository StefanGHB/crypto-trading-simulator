package com.cryptotrading.crypto_trading_sim.service;

import com.cryptotrading.crypto_trading_sim.dao.UserDao;
import com.cryptotrading.crypto_trading_sim.dao.UserHoldingDao;
import com.cryptotrading.crypto_trading_sim.dao.TransactionDao;
import com.cryptotrading.crypto_trading_sim.model.User;
import com.cryptotrading.crypto_trading_sim.utils.TestConstants;
import com.cryptotrading.crypto_trading_sim.utils.TestDataBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private UserHoldingDao userHoldingDao;

    @Mock
    private TransactionDao transactionDao;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.createTestUserWithId(TestConstants.TEST_USER_ID);
    }

    // ===============================================
    // USER CREATION TESTS
    // ===============================================

    @Test
    void testCreateUser_Success() {
        // Given
        when(userDao.existsByUsername(TestConstants.TEST_USERNAME)).thenReturn(false);
        when(userDao.existsByEmail(TestConstants.TEST_EMAIL)).thenReturn(false);
        when(userDao.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.createUser(TestConstants.TEST_USERNAME, TestConstants.TEST_EMAIL);

        // Then
        assertNotNull(result);
        assertEquals(TestConstants.TEST_USERNAME, result.getUsername());
        assertEquals(TestConstants.TEST_EMAIL, result.getEmail());
        assertEquals(TestConstants.DEFAULT_INITIAL_BALANCE, result.getCurrentBalance());
        assertTrue(result.isActive());
        verify(userDao).save(any(User.class));
    }

    @Test
    void testCreateUser_DuplicateUsername() {
        // Given
        when(userDao.existsByUsername(TestConstants.TEST_USERNAME)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userService.createUser(TestConstants.TEST_USERNAME, TestConstants.TEST_EMAIL));

        assertTrue(exception.getMessage().contains("Username already exists"));
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_DuplicateEmail() {
        // Given
        when(userDao.existsByUsername(TestConstants.TEST_USERNAME)).thenReturn(false);
        when(userDao.existsByEmail(TestConstants.TEST_EMAIL)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userService.createUser(TestConstants.TEST_USERNAME, TestConstants.TEST_EMAIL));

        assertTrue(exception.getMessage().contains("Email already exists"));
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void testCreateUserWithCustomBalance_Success() {
        // Given
        BigDecimal customBalance = new BigDecimal("5000.00");
        when(userDao.existsByUsername(TestConstants.TEST_USERNAME)).thenReturn(false);
        when(userDao.existsByEmail(TestConstants.TEST_EMAIL)).thenReturn(false);
        when(userDao.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.createUser(TestConstants.TEST_USERNAME, TestConstants.TEST_EMAIL, customBalance);

        // Then
        assertNotNull(result);
        verify(userDao).save(argThat(user ->
                user.getInitialBalance().equals(customBalance) &&
                        user.getCurrentBalance().equals(customBalance)));
    }

    // ===============================================
    // BALANCE MANAGEMENT TESTS
    // ===============================================

    @Test
    void testGetCurrentBalance_Success() {
        // Given
        when(userDao.getCurrentBalance(TestConstants.TEST_USER_ID)).thenReturn(TestConstants.DEFAULT_INITIAL_BALANCE);

        // When
        BigDecimal balance = userService.getCurrentBalance(TestConstants.TEST_USER_ID);

        // Then
        assertEquals(TestConstants.DEFAULT_INITIAL_BALANCE, balance);
        verify(userDao).getCurrentBalance(TestConstants.TEST_USER_ID);
    }

    @Test
    void testCanAfford_True() {
        // Given
        BigDecimal requestedAmount = new BigDecimal("5000.00");
        when(userDao.canAfford(TestConstants.TEST_USER_ID, requestedAmount)).thenReturn(true);

        // When
        boolean result = userService.canAfford(TestConstants.TEST_USER_ID, requestedAmount);

        // Then
        assertTrue(result);
        verify(userDao).canAfford(TestConstants.TEST_USER_ID, requestedAmount);
    }

    @Test
    void testCanAfford_False() {
        // Given
        BigDecimal requestedAmount = new BigDecimal("15000.00"); // More than default balance
        when(userDao.canAfford(TestConstants.TEST_USER_ID, requestedAmount)).thenReturn(false);

        // When
        boolean result = userService.canAfford(TestConstants.TEST_USER_ID, requestedAmount);

        // Then
        assertFalse(result);
        verify(userDao).canAfford(TestConstants.TEST_USER_ID, requestedAmount);
    }

    @Test
    void testUpdateUser_Success() {
        // Given
        User updatedUser = TestDataBuilder.createTestUserWithId(TestConstants.TEST_USER_ID);
        updatedUser.setUsername("newusername");
        updatedUser.setEmail("newemail@example.com");

        when(userDao.findById(TestConstants.TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userDao.existsByUsername("newusername")).thenReturn(false);
        when(userDao.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userDao.update(updatedUser)).thenReturn(true);
        // 🔧 FIXED: Return updated user after update
        when(userDao.findById(TestConstants.TEST_USER_ID))
                .thenReturn(Optional.of(testUser))  // First call for validation
                .thenReturn(Optional.of(updatedUser)); // Second call after update

        // When
        User result = userService.updateUser(updatedUser);

        // Then
        assertNotNull(result);
        verify(userDao).update(updatedUser);
    }

    // ===============================================
    // ACCOUNT RESET TESTS
    // ===============================================

    @Test
    void testResetAccount_Success() {
        // Given
        when(userHoldingDao.deleteAllUserHoldings(TestConstants.TEST_USER_ID)).thenReturn(true);
        when(transactionDao.deleteAllUserTransactions(TestConstants.TEST_USER_ID)).thenReturn(true);
        when(userDao.resetAccount(TestConstants.TEST_USER_ID)).thenReturn(true);

        // When
        boolean result = userService.resetUserAccount(TestConstants.TEST_USER_ID);

        // Then
        assertTrue(result);
        verify(userHoldingDao).deleteAllUserHoldings(TestConstants.TEST_USER_ID);
        verify(transactionDao).deleteAllUserTransactions(TestConstants.TEST_USER_ID);
        verify(userDao).resetAccount(TestConstants.TEST_USER_ID);
    }

    @Test
    void testResetAccount_Failure() {
        // Given
        when(userHoldingDao.deleteAllUserHoldings(TestConstants.TEST_USER_ID)).thenReturn(true);
        when(transactionDao.deleteAllUserTransactions(TestConstants.TEST_USER_ID)).thenReturn(true);
        when(userDao.resetAccount(TestConstants.TEST_USER_ID)).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userService.resetUserAccount(TestConstants.TEST_USER_ID));

        assertTrue(exception.getMessage().contains("Failed to reset user account"));
    }

    // ===============================================
    // USER STATISTICS TESTS
    // ===============================================

    @Test
    void testGetUserStatistics_Success() {
        // Given
        when(userDao.findById(TestConstants.TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userHoldingDao.getTotalPortfolioValue(TestConstants.TEST_USER_ID)).thenReturn(TestConstants.PORTFOLIO_VALUE);
        when(userHoldingDao.getTotalInvestedByUser(TestConstants.TEST_USER_ID)).thenReturn(TestConstants.TOTAL_INVESTED);
        when(userHoldingDao.getTotalUnrealizedProfitLoss(TestConstants.TEST_USER_ID)).thenReturn(TestConstants.UNREALIZED_PNL);
        when(transactionDao.getTotalRealizedProfitLoss(TestConstants.TEST_USER_ID)).thenReturn(TestConstants.REALIZED_PNL);
        when(transactionDao.getUserTransactionCount(TestConstants.TEST_USER_ID)).thenReturn(5L);
        when(userHoldingDao.getUserHoldingCount(TestConstants.TEST_USER_ID)).thenReturn(3L);

        // When
        UserService.UserStatistics stats = userService.getUserStatistics(TestConstants.TEST_USER_ID);

        // Then
        assertNotNull(stats);
        assertEquals(testUser.getCurrentBalance(), stats.getCashBalance());
        assertEquals(TestConstants.PORTFOLIO_VALUE, stats.getPortfolioValue());
        assertEquals(TestConstants.UNREALIZED_PNL, stats.getUnrealizedPnL());
        assertEquals(TestConstants.REALIZED_PNL, stats.getRealizedPnL());
        assertEquals(5L, stats.getTransactionCount());
        assertEquals(3L, stats.getHoldingCount());
    }

    // ===============================================
    // VALIDATION TESTS
    // ===============================================

    @Test
    void testInvalidUserData_ThrowsExceptions() {
        // Test null username
        assertThrows(IllegalArgumentException.class, () ->
                userService.createUser(null, TestConstants.TEST_EMAIL));

        // Test empty username
        assertThrows(IllegalArgumentException.class, () ->
                userService.createUser("", TestConstants.TEST_EMAIL));

        // Test short username
        assertThrows(IllegalArgumentException.class, () ->
                userService.createUser("ab", TestConstants.TEST_EMAIL));

        // Test long username
        assertThrows(IllegalArgumentException.class, () ->
                userService.createUser("a".repeat(51), TestConstants.TEST_EMAIL));

        // Test null email
        assertThrows(IllegalArgumentException.class, () ->
                userService.createUser(TestConstants.TEST_USERNAME, null));

        // Test invalid email format
        assertThrows(IllegalArgumentException.class, () ->
                userService.createUser(TestConstants.TEST_USERNAME, "invalid-email"));

        // Test invalid user ID
        assertThrows(IllegalArgumentException.class, () ->
                userService.getCurrentBalance(null));

        assertThrows(IllegalArgumentException.class, () ->
                userService.getCurrentBalance(-1L));

        // Test negative initial balance
        assertThrows(IllegalArgumentException.class, () ->
                userService.createUser(TestConstants.TEST_USERNAME, TestConstants.TEST_EMAIL, TestConstants.NEGATIVE_AMOUNT));

        // Test zero initial balance
        assertThrows(IllegalArgumentException.class, () ->
                userService.createUser(TestConstants.TEST_USERNAME, TestConstants.TEST_EMAIL, TestConstants.ZERO_AMOUNT));
    }

    @Test
    void testUserValidation_Methods() {
        // Test username exists
        when(userDao.existsByUsername(TestConstants.TEST_USERNAME)).thenReturn(true);
        assertTrue(userService.usernameExists(TestConstants.TEST_USERNAME));

        // Test email exists
        when(userDao.existsByEmail(TestConstants.TEST_EMAIL)).thenReturn(true);
        assertTrue(userService.emailExists(TestConstants.TEST_EMAIL));

        // Test user is active
        when(userDao.findById(TestConstants.TEST_USER_ID)).thenReturn(Optional.of(testUser));
        assertTrue(userService.isUserActiveById(TestConstants.TEST_USER_ID));

        // Test user not found
        when(userDao.findById(999L)).thenReturn(Optional.empty());
        assertFalse(userService.isUserActiveById(999L));
    }
}