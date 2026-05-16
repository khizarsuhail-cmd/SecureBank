package com.banking.security;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Module 5 - Audit Layer.
 * Records every significant system event to an in-memory log.
 * Account numbers are automatically masked before storage.
 */
public class AuditLog {

    /** Singleton instance */
    private static final AuditLog INSTANCE = new AuditLog();

    private final List<String> entries = new ArrayList<>();
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AuditLog() {}

    public static AuditLog getInstance() { return INSTANCE; }

    // -- Logging methods -------------------------------------------------------

    /** Log a general action by a user. */
    public void log(String userId, String action, String details) {
        String masked = SecurityService.maskAccountNumber(details);
        String entry  = String.format("[%s] USER=%s | ACTION=%s | DETAIL=%s",
                LocalDateTime.now().format(FMT), userId, action, masked);
        entries.add(entry);
        System.out.println("[AUDIT] " + entry);
    }

    /** Log a transaction event. */
    public void logTransaction(String userId, String txnId,
                               String accountNumber, String description) {
        String masked = SecurityService.maskAccountNumber(accountNumber);
        String entry  = String.format("[%s] USER=%s | TXN=%s | ACCT=%s | %s",
                LocalDateTime.now().format(FMT), userId, txnId, masked, description);
        entries.add(entry);
        System.out.println("[AUDIT] " + entry);
    }

    /** Log a security event (login, lockout, password reset). */
    public void logSecurity(String userId, String event) {
        String entry = String.format("[%s] SECURITY | USER=%s | EVENT=%s",
                LocalDateTime.now().format(FMT), userId, event);
        entries.add(entry);
        System.out.println("[AUDIT] " + entry);
    }

    /** Log a flagged / suspicious transaction. */
    public void flagTransaction(String userId, String reason, String accountNumber) {
        String masked = SecurityService.maskAccountNumber(accountNumber);
        String entry  = String.format("[%s] [!] FLAG | USER=%s | ACCT=%s | REASON=%s",
                LocalDateTime.now().format(FMT), userId, masked, reason);
        entries.add(entry);
        System.out.println("[AUDIT][FLAG] " + entry);
    }

    // -- Retrieval -------------------------------------------------------------

    public List<String> getAllEntries() { return new ArrayList<>(entries); }

    /** Returns only entries that contain the given keyword (case-insensitive). */
    public List<String> search(String keyword) {
        String lower = keyword.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String e : entries)
            if (e.toLowerCase().contains(lower)) result.add(e);
        return result;
    }

    public int getFlaggedCount() {
        return (int) entries.stream().filter(e -> e.contains("[!] FLAG")).count();
    }

    public void printAll() {
        System.out.println("\n========== AUDIT LOG ==========");
        entries.forEach(System.out::println);
        System.out.println("==============================");
    }
}
