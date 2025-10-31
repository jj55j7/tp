package seedu.address.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Person;

/**
 * An Immutable AddressBook that is serializable to JSON format.
 */
@JsonRootName(value = "addressbook")
class JsonSerializableAddressBook {

    public static final String MESSAGE_DUPLICATE_PERSON = "Duplicate entries found based on PHONE + NAME combination. "
            + "Entries will be automatically cleaned up.";
    public static final String MESSAGE_ENTRY_LIMIT_EXCEEDED = "Entry limit exceeded. "
            + "Only first 250 entries will be kept.";

    private static final int MAX_PERSONS_LIMIT = 250;

    private static final Logger logger = LogsCenter.getLogger(JsonSerializableAddressBook.class);

    private final List<JsonAdaptedPerson> persons = new ArrayList<>();

    /**
     * Constructs a {@code JsonSerializableAddressBook} with the given persons.
     */
    @JsonCreator
    public JsonSerializableAddressBook(@JsonProperty("persons") List<JsonAdaptedPerson> persons) {
        this.persons.addAll(persons);
    }

    /**
     * Converts a given {@code ReadOnlyAddressBook} into this class for Jackson use.
     *
     * @param source future changes to this will not affect the created {@code JsonSerializableAddressBook}.
     */
    public JsonSerializableAddressBook(ReadOnlyAddressBook source) {
        persons.addAll(source.getPersonList().stream().map(JsonAdaptedPerson::new).collect(Collectors.toList()));
    }

    /**
     * Converts this address book into the model's {@code AddressBook} object.
     *
     * @throws IllegalValueException if there were any data constraints violated.
     */
    public AddressBook toModelType() throws IllegalValueException {
        AddressBook addressBook = new AddressBook();
        int duplicateCount = 0;
        int invalidCount = 0;
        int loadedCount = 0;
        int entryNumber = 0;
        int skippedDueToLimit = 0;
        boolean limitExceeded = false;

        // Create lists to store warning messages
        List<String> warningMessages = new ArrayList<>();

        for (JsonAdaptedPerson jsonAdaptedPerson : persons) {
            entryNumber++;
            // Check if reached the limit
            if (loadedCount >= MAX_PERSONS_LIMIT) {
                skippedDueToLimit = persons.size() - MAX_PERSONS_LIMIT - invalidCount - duplicateCount;
                limitExceeded = true;
                break; // Stop processing once we reach the limit
            }

            try {
                Person person = jsonAdaptedPerson.toModelType();

                if (hasDuplicatePhoneName(addressBook, person)) {
                    duplicateCount++;
                    String warningMsg = "[Entry " + entryNumber + "] DUPLICATE SKIPPED - Name: '" + person.getName()
                            + "', Phone: '" + person.getPhone() + "' - Same phone+name combination exists";
                    logger.warning(warningMsg);
                    warningMessages.add(warningMsg);
                    continue;
                }

                addressBook.addPerson(person);
                loadedCount++;

            } catch (IllegalValueException e) {
                invalidCount++;
                String personInfo = extractPersonInfoFromError(e.getMessage());
                String reason = extractReasonFromError(e.getMessage());
                String warningMsg = "[Entry " + entryNumber + "] INVALID SKIPPED - Reason: " + reason;

                if (!personInfo.isEmpty()) {
                    warningMsg = "[Entry " + entryNumber + "] INVALID SKIPPED - Person: '" + personInfo
                            + "' - Reason: " + reason;
                }

                logger.warning(warningMsg);
                warningMessages.add(warningMsg);
            }
        }

        // Log warning if limit was exceeded
        if (limitExceeded) {
            String limitWarning = MESSAGE_ENTRY_LIMIT_EXCEEDED + " Skipped " + skippedDueToLimit + " entries beyond limit.";
            logger.warning(limitWarning);
            warningMessages.add(limitWarning);
        }

        // Store the warnings in the address book
        addressBook.setStorageWarnings(warningMessages);

        // Log comprehensive summary
        StringBuilder summary = new StringBuilder();
        summary.append("Data loading completed: ");
        summary.append(loadedCount).append(" entries loaded successfully");

        if (duplicateCount > 0) {
            summary.append(", ").append(duplicateCount).append(" duplicates skipped");
        }
        if (invalidCount > 0) {
            summary.append(", ").append(invalidCount).append(" invalid entries skipped");
        }
        if (skippedDueToLimit > 0) {
            summary.append(", ").append(skippedDueToLimit).append(" entries skipped due to limit");
        }

        logger.info(summary.toString());

        if (loadedCount == 0 && persons.size() > 0) {
            throw new IllegalValueException("No valid persons found in data file. "
                    + "All entries were skipped due to errors.");
        }

        return addressBook;
    }

    /**
     * Extracts person information from error message for better logging
     */
    private String extractPersonInfoFromError(String errorMessage) {
        if (errorMessage.contains("for person:")) {
            // Extract the person name from error message like "Invalid phone for person: John Doe"
            return errorMessage.substring(errorMessage.indexOf("for person:") + "for person:".length()).trim();
        }
        return "Unknown";
    }

    /**
     * Extracts clean reason from error message
     */
    private String extractReasonFromError(String errorMessage) {
        if (errorMessage.contains("Missing required field:")) {
            if (errorMessage.contains("name")) {
                return "Missing name field";
            }
            if (errorMessage.contains("phone")) {
                return "Missing phone field";
            }
            if (errorMessage.contains("address")) {
                return "Missing address field";
            }
            return "Missing required field";
        }
        if (errorMessage.contains("Invalid name format")) {
            return "Invalid name format";
        }
        if (errorMessage.contains("Invalid phone format")) {
            return "Invalid phone number";
        }
        if (errorMessage.contains("Invalid address format")) {
            return "Invalid address format";
        }
        if (errorMessage.contains("Invalid email format")) {
            return "Invalid email format";
        }
        return errorMessage;
    }

    /**
     * Checks if the address book already contains a person with the same PHONE + NAME combination.
     * Other fields (address, email, tags) can be different.
     */
    private boolean hasDuplicatePhoneName(AddressBook addressBook, Person person) {
        return addressBook.getPersonList().stream()
                .anyMatch(existingPerson ->
                        existingPerson.getPhone().equals(person.getPhone())
                                && existingPerson.getName().fullName.equalsIgnoreCase(person.getName().fullName));
    }

    /**
     * Returns the number of persons in the JSON data before processing.
     * Used to check if limit was exceeded.
     */
    public int getOriginalPersonCount() {
        return persons.size();
    }

}
