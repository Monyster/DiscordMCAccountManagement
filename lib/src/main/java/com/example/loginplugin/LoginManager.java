package com.example.loginplugin;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;

public class LoginManager {

	private final Logger logger;

	private Connection connection;

	public LoginManager(Logger logger) {
		this.logger = logger;
		initializeDatabase();
	}

	// ---------------- SQLITE DATABASE ----------------

	private void initializeDatabase() {
		try {
			File dbFile = new File("./database/superdb.db");

			// Check if database file exists
			if (!dbFile.exists()) {
				logger.severe("Database file not found at: " + dbFile.getAbsolutePath());
				logger.severe("Please initialize the database schema before starting the plugin!");
				return;
			}

			connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
			logger.info("SQLite database connected at: " + dbFile.getAbsolutePath());
		} catch (Exception e) {
			logger.severe("Failed to initialize database: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Close database connection - call this on plugin disable
	 */
	public void closeDatabase() {
		if (connection != null) {
			try {
				connection.close();
				logger.info("Database connection closed.");
			} catch (SQLException e) {
				logger.severe("Error closing database connection: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Check if database connection is valid and available
	 */
	private boolean isDatabaseAvailable() {
		try {
			return connection != null && !connection.isClosed();
		} catch (SQLException e) {
			logger.severe("Error checking database connection: " + e.getMessage());
			return false;
		}
	}

	// ---------------- CODE MANAGEMENT ----------------

	/**
	 * Authorize player using the code
	 */
	public boolean authorizeWithCode(String username, UUID uuid, String code) {
		// Error boundary: Check database availability
		if (!isDatabaseAvailable()) {
			logger.severe("Database is not available for authorization!");
			return false;
		}

		// Validate code format: exactly 6 digits
		if (code == null || !code.matches("^[0-9]{6}$")) {
			logger.warning("Invalid code format from player " + username + ": " + (code == null ? "null" : code));
			return false;
		}

		String sql = "SELECT * FROM link_codes WHERE LOWER(mc_username) = LOWER(?) AND code = ? AND expires_at > ?";

		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, username);
			stmt.setString(2, code);
			stmt.setLong(3, System.currentTimeMillis());

			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				// Valid code found
				logger.info("Player " + username + " authorized successfully with code");

				// Update mc_uuid in database
				try (PreparedStatement update = connection
						.prepareStatement("UPDATE link_codes SET mc_uuid = ? WHERE code = ?")) {
					update.setString(1, uuid.toString());
					update.setString(2, code);
					update.executeUpdate();
				}

				// Delete the code after usage
				try (PreparedStatement delete = connection.prepareStatement("DELETE FROM link_codes WHERE code = ?")) {
					delete.setString(1, code);
					delete.executeUpdate();
				}

				return true;
			} else {
				logger.info("Invalid or expired code for player " + username);
				return false;
			}
		} catch (SQLException e) {
			logger.severe("Database error during authorization: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Check if a Minecraft username is registered in the accounts table.
	 */
	public boolean isRegistered(String username) {
		// Error boundary: Check database availability
		if (!isDatabaseAvailable()) {
			logger.severe("Database is not available for registration check!");
			return false;
		}

		String sql = "SELECT 1 FROM accounts WHERE LOWER(mc_username) = LOWER(?) LIMIT 1";

		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, username);

			try (ResultSet rs = stmt.executeQuery()) {
				return rs.next(); // true if record exists
			}

		} catch (SQLException e) {
			logger.severe("Database error in isRegistered(): " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
}
