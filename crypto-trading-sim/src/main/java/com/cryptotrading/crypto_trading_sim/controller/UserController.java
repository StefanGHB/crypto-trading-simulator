package com.cryptotrading.crypto_trading_sim.controller;

import com.cryptotrading.crypto_trading_sim.model.User;
import com.cryptotrading.crypto_trading_sim.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for User-related operations
 * Clean and simple endpoints that work directly with Service layer
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // ===============================================
    // USER MANAGEMENT ENDPOINTS
    // ===============================================

    /**
     * Create a new user with default balance
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String email = request.get("email");
            User newUser = userService.createUser(username, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create user: " + e.getMessage()));
        }
    }

    /**
     * Create a new user with custom initial balance
     * POST /api/users/custom-balance
     */
    @PostMapping("/custom-balance")
    public ResponseEntity<?> createUserWithCustomBalance(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            String email = (String) request.get("email");
            BigDecimal initialBalance = new BigDecimal(request.get("initialBalance").toString());

            User newUser = userService.createUser(username, email, initialBalance);
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create user: " + e.getMessage()));
        }
    }

    /**
     * Get user by ID
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            Optional<User> user = userService.getUserById(id);
            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get user: " + e.getMessage()));
        }
    }

    /**
     * Get user by username
     * GET /api/users/username/{username}
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            Optional<User> user = userService.getUserByUsername(username);
            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get user: " + e.getMessage()));
        }
    }

    /**
     * Get all active users
     * GET /api/users/active
     */
    @GetMapping("/active")
    public ResponseEntity<?> getAllActiveUsers() {
        try {
            List<User> activeUsers = userService.getAllActiveUsers();
            return ResponseEntity.ok(activeUsers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get active users: " + e.getMessage()));
        }
    }

    /**
     * Update user information
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            Optional<User> existingUserOpt = userService.getUserById(id);
            if (existingUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }

            User existingUser = existingUserOpt.get();
            existingUser.setUsername(request.get("username"));
            existingUser.setEmail(request.get("email"));

            User updatedUser = userService.updateUser(existingUser);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update user: " + e.getMessage()));
        }
    }

    // ===============================================
    // ACCOUNT BALANCE ENDPOINTS
    // ===============================================

    /**
     * Get current balance for user
     * GET /api/users/{id}/balance
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<?> getCurrentBalance(@PathVariable Long id) {
        try {
            BigDecimal balance = userService.getCurrentBalance(id);
            return ResponseEntity.ok(Map.of(
                    "userId", id,
                    "currentBalance", balance
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get balance: " + e.getMessage()));
        }
    }

    /**
     * Check if user can afford specific amount
     * GET /api/users/{id}/can-afford?amount={amount}
     */
    @GetMapping("/{id}/can-afford")
    public ResponseEntity<?> canAfford(@PathVariable Long id, @RequestParam BigDecimal amount) {
        try {
            boolean canAfford = userService.canAfford(id, amount);
            BigDecimal currentBalance = userService.getCurrentBalance(id);

            return ResponseEntity.ok(Map.of(
                    "userId", id,
                    "requestedAmount", amount,
                    "currentBalance", currentBalance,
                    "canAfford", canAfford
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check affordability: " + e.getMessage()));
        }
    }

    // ===============================================
    // ACCOUNT RESET ENDPOINT
    // ===============================================

    /**
     * Reset user account to initial state
     * POST /api/users/{id}/reset
     */
    @PostMapping("/{id}/reset")
    public ResponseEntity<?> resetUserAccount(@PathVariable Long id) {
        try {
            boolean resetSuccessful = userService.resetUserAccount(id);
            if (resetSuccessful) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Account reset successfully"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to reset account"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reset account: " + e.getMessage()));
        }
    }

    // ===============================================
    // USER STATISTICS ENDPOINTS
    // ===============================================

    /**
     * Get comprehensive user statistics
     * GET /api/users/{id}/statistics
     */
    @GetMapping("/{id}/statistics")
    public ResponseEntity<?> getUserStatistics(@PathVariable Long id) {
        try {
            UserService.UserStatistics statistics = userService.getUserStatistics(id);
            return ResponseEntity.ok(statistics);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
    }

    /**
     * Get user portfolio summary
     * GET /api/users/{id}/portfolio-summary
     */
    @GetMapping("/{id}/portfolio-summary")
    public ResponseEntity<?> getUserPortfolioSummary(@PathVariable Long id) {
        try {
            UserService.UserPortfolioSummary summary = userService.getUserPortfolioSummary(id);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get portfolio summary: " + e.getMessage()));
        }
    }

    // ===============================================
    // USER VALIDATION ENDPOINTS
    // ===============================================

    /**
     * Check if user exists and is active
     * GET /api/users/{id}/active-status
     */
    @GetMapping("/{id}/active-status")
    public ResponseEntity<?> getUserActiveStatus(@PathVariable Long id) {
        try {
            boolean isActive = userService.isUserActiveById(id);
            return ResponseEntity.ok(Map.of(
                    "userId", id,
                    "isActive", isActive
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check user status: " + e.getMessage()));
        }
    }

    /**
     * Check if username exists
     * GET /api/users/check-username?username={username}
     */
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsernameAvailability(@RequestParam String username) {
        try {
            boolean exists = userService.usernameExists(username);
            return ResponseEntity.ok(Map.of(
                    "username", username,
                    "exists", exists,
                    "available", !exists
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check username: " + e.getMessage()));
        }
    }

    /**
     * Check if email exists
     * GET /api/users/check-email?email={email}
     */
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmailAvailability(@RequestParam String email) {
        try {
            boolean exists = userService.emailExists(email);
            return ResponseEntity.ok(Map.of(
                    "email", email,
                    "exists", exists,
                    "available", !exists
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check email: " + e.getMessage()));
        }
    }

    /**
     * Deactivate user account
     * POST /api/users/{id}/deactivate
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        try {
            boolean deactivated = userService.deactivateUser(id);
            if (deactivated) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "User deactivated successfully"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to deactivate user"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to deactivate user: " + e.getMessage()));
        }
    }
}