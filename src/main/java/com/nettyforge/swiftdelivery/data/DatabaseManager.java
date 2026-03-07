package com.nettyforge.swiftdelivery.data;

import com.nettyforge.swiftdelivery.service.ActiveEvent;
import com.nettyforge.swiftdelivery.model.Winner;

import com.nettyforge.swiftdelivery.model.Mail;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for database operations.
 * Supports saving and loading event data and leaderboards.
 */
public interface DatabaseManager {

    /**
     * Initializes the database connection and creates necessary tables.
     *
     * @return true if successful, false otherwise
     */
    boolean connect();

    /**
     * Closes the database connection pool.
     */
    void disconnect();

    /**
     * Saves an active event and all its participant data.
     *
     * @param event The event to save
     * @return A CompletableFuture representing the async operation
     */
    CompletableFuture<Void> saveActiveEvent(ActiveEvent event);

    /**
     * Deletes an active event and all its participant data.
     * This is called when an event ends normally or is manually cancelled.
     *
     * @param deliveryName The name of the delivery event to delete
     * @return A CompletableFuture representing the async operation
     */
    CompletableFuture<Void> deleteActiveEvent(String deliveryName);

    /**
     * Loads all currently active events from the database.
     * Used for crash recovery and cross-server syncing.
     *
     * @return A CompletableFuture containing a list of loaded events
     */
    CompletableFuture<List<ActiveEvent>> loadAllActiveEvents();

    /**
     * Saves the final results of a completed event to the permanent record table.
     *
     * @param deliveryName The name of the delivery
     * @param winners The list of winners
     * @param totalParticipants The total number of players who participated
     * @param totalItems The total number of items delivered
     * @return A CompletableFuture representing the async operation
     */
    CompletableFuture<Void> saveEventHistory(String deliveryName, List<Winner> winners, int totalParticipants, int totalItems);

    /**
     * Gets the all-time leaderboard for a specific delivery event.
     *
     * @param deliveryName The name of the delivery type
     * @param limit The maximum number of results to return
     * @return A CompletableFuture containing a map of player UUID -> total deliveries
     */
    CompletableFuture<Map<UUID, Integer>> getLeaderboard(String deliveryName, int limit);
    
    /**
     * Gets the all-time global leaderboard across all delivery types.
     *
     * @param limit The maximum number of results to return
     * @return A CompletableFuture containing a map of player UUID -> total deliveries
     */
    CompletableFuture<Map<UUID, Integer>> getGlobalLeaderboard(int limit);

    // --- P2P Mail Operations ---
    CompletableFuture<Void> saveMail(Mail mail);
    CompletableFuture<List<Mail>> getMail(UUID receiver);
    CompletableFuture<Void> markMailClaimed(int mailId);

    // --- Tier 1 Security & RPG Operations ---
    CompletableFuture<Void> logTransaction(String transactionId, UUID sender, UUID receiver, String itemSummary, String action);
    CompletableFuture<List<String>> getPlayerTransactions(UUID playerUuid, int limit);
    
    CompletableFuture<Void> addCourierXp(UUID playerUuid, int amount);
    CompletableFuture<Integer> getCourierXp(UUID playerUuid);
    CompletableFuture<Integer> getCourierLevel(UUID playerUuid);
}
