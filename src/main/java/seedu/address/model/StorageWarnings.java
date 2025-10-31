package seedu.address.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to manage storage warnings that need to be displayed in UI
 */
public class StorageWarnings {
    private static List<String> pendingWarnings = new ArrayList<>();

    /**
     * Sets pending warnings to be displayed
     */
    public static void setPendingWarnings(List<String> warnings) {
        pendingWarnings.clear();
        if (warnings != null) {
            pendingWarnings.addAll(warnings);
        }
    }

    /**
     * Gets and clears pending warnings
     */
    public static List<String> getAndClearPendingWarnings() {
        List<String> warnings = new ArrayList<>(pendingWarnings);
        pendingWarnings.clear();
        return warnings;
    }

    /**
     * Checks if there are pending warnings
     */
    public static boolean hasPendingWarnings() {
        return !pendingWarnings.isEmpty();
    }
}
