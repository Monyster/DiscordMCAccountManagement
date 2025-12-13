package com.example.loginplugin;

import java.io.File;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class LoginManager {

	private final Set<UUID> loggedInPlayers = new HashSet<>();
	private final Logger logger;

	private Connection connection;

	public LoginManager(Logger logger) {
		this.logger = logger;
		initializeDatabase();
	}

	// ---------------- LOGIN STATE ----------------

	public boolean isLoggedIn(UUID uuid) {
		return loggedInPlayers.contains(uuid);
	}

	public void setLoggedIn(UUID uuid) {
		loggedInPlayers.add(uuid);
		logger.info("Player " + uuid + " logged in.");
	}

	public void remove(UUID uuid) {
		if (loggedInPlayers.remove(uuid)) {
			logger.info("Login state cleared for " + uuid);
		}
	}

	// ---------------- SQLITE DATABASE ----------------

	private void initializeDatabase() {
		try {
			File dbFile = new File("./database/superdb.db");
			connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
			logger.info("SQLite database connected.");
		} catch (Exception e) {
			logger.severe("Failed to initialize database: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// ---------------- CODE MANAGEMENT ----------------

	/**
	 * Authorize player using the code
	 */
	public boolean authorizeWithCode(String username, UUID uuid, String code) {
		if (!code.matches("^[0-9]+$")) {
			logger.info("Not matches");
			return false;
		}

		String sql = "SELECT * FROM link_codes WHERE mc_username = ? AND code = ? AND expires_at > ?";

		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, username.toLowerCase()); // Problem
			stmt.setString(2, code);
			stmt.setLong(3, System.currentTimeMillis());

			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				// Valid code, mark player as logged in
				setLoggedIn(uuid);

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

				logger.info("Player " + username + " authorized with code " + code);
				return true;
			} else {
				logger.info("Invalid or expired code for player " + username + ": " + code);
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
