package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.model.person.Person;

/**
 * Represents the in-memory model of the address book data.
 */
public class ModelManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);
    //used to compare name for sort
    private static final Comparator<Person> NAME_ASC =
            Comparator.comparing((Person p) -> p.getName().toString(), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(p -> p.getName().toString());
    private static final Comparator<Person> ADDRESS_ASC =
            Comparator.comparing((
                                    Person p) -> p.getAddress() == null ? "" : tryGetAddressValue(p),
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(p -> p.getAddress() == null ? "" : tryGetAddressValue(p));
    private final AddressBook addressBook;
    private final UserPrefs userPrefs;
    private final FilteredList<Person> filteredPersons;
    private final List<String> storageWarnings = new ArrayList<>();

    /**
     * Initializes a ModelManager with the given addressBook and userPrefs.
     */
    public ModelManager(ReadOnlyAddressBook addressBook, ReadOnlyUserPrefs userPrefs) {
        requireAllNonNull(addressBook, userPrefs);

        logger.fine("Initializing with address book: " + addressBook + " and user prefs " + userPrefs);

        this.addressBook = new AddressBook(addressBook);
        this.userPrefs = new UserPrefs(userPrefs);
        filteredPersons = new FilteredList<>(this.addressBook.getPersonList());
    }

    public ModelManager() {
        this(new AddressBook(), new UserPrefs());
    }

    private static String tryGetAddressValue(Person p) {
        // If your Address class exposes a 'value' field, use it:
        // return p.getAddress().value;
        // Otherwise, using toString() is safe:
        return p.getAddress().toString();
    }
    //=========== UserPrefs ==================================================================================

    @Override
    public void setUserPrefs(ReadOnlyUserPrefs userPrefs) {
        requireNonNull(userPrefs);
        this.userPrefs.resetData(userPrefs);
    }

    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        userPrefs.setGuiSettings(guiSettings);
    }

    @Override
    public Path getAddressBookFilePath() {
        return userPrefs.getAddressBookFilePath();
    }

    @Override
    public void setAddressBookFilePath(Path addressBookFilePath) {
        requireNonNull(addressBookFilePath);
        userPrefs.setAddressBookFilePath(addressBookFilePath);
    }

    //=========== AddressBook ================================================================================

    @Override
    public void setAddressBook(ReadOnlyAddressBook addressBook) {
        this.addressBook.resetData(addressBook);
    }

    @Override
    public ReadOnlyAddressBook getAddressBook() {
        return addressBook;
    }

    @Override
    public boolean hasPerson(Person person) {
        requireNonNull(person);
        return addressBook.hasPerson(person);
    }

    @Override
    public void deletePerson(Person target) {
        addressBook.removePerson(target);
    }

    @Override
    public void addPerson(Person person) {
        addressBook.addPerson(person);
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
    }

    @Override
    public void setPerson(Person target, Person editedPerson) {
        requireAllNonNull(target, editedPerson);

        addressBook.setPerson(target, editedPerson);
    }

    //method to sort elderly by name
    @Override
    public void sortPersonsByName(boolean ascending) {
        if (ascending) {
            addressBook.sortPersons(NAME_ASC);
        } else {
            addressBook.sortPersons(NAME_ASC.reversed());
        }
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
    }

    //method to sort elderly by address
    @Override
    public void sortPersonsByAddress(boolean ascending) {
        if (ascending) {
            addressBook.sortPersons(ADDRESS_ASC);
        } else {
            addressBook.sortPersons(ADDRESS_ASC.reversed());
        }
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
    }

    //=========== Filtered Person List Accessors =============================================================

    /**
     * Returns an unmodifiable view of the list of {@code Person} backed by the internal list of
     * {@code versionedAddressBook}
     */
    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return filteredPersons;
    }

    @Override
    public void updateFilteredPersonList(Predicate<Person> predicate) {
        requireNonNull(predicate);
        filteredPersons.setPredicate(predicate);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ModelManager)) {
            return false;
        }

        ModelManager otherModelManager = (ModelManager) other;
        return addressBook.equals(otherModelManager.addressBook)
                && userPrefs.equals(otherModelManager.userPrefs)
                && filteredPersons.equals(otherModelManager.filteredPersons);
    }

    /**
     * Sets storage warnings from data loading process
     */
    public void setStorageWarnings(List<String> warnings) {
        storageWarnings.clear();
        storageWarnings.addAll(warnings);
    }

    /**
     * Gets storage warnings from data loading process
     */
    public List<String> getStorageWarnings() {
        return new ArrayList<>(storageWarnings);
    }

    /**
     * Clears storage warnings after they've been displayed
     */
    public void clearStorageWarnings() {
        storageWarnings.clear();
    }

}
