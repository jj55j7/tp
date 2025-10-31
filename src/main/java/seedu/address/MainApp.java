package seedu.address;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.stage.Stage;
import seedu.address.commons.core.Config;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.core.Version;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.ConfigUtil;
import seedu.address.commons.util.StringUtil;
import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.model.UserPrefs;
import seedu.address.model.util.SampleDataUtil;
import seedu.address.storage.AddressBookStorage;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.storage.UserPrefsStorage;
import seedu.address.ui.Ui;
import seedu.address.ui.UiManager;

/**
 * Runs the application.
 */
public class MainApp extends Application {

    public static final Version VERSION = new Version(0, 2, 2, true);

    private static final Logger logger = LogsCenter.getLogger(MainApp.class);

    protected Ui ui;
    protected Logic logic;
    protected Storage storage;
    protected Model model;
    protected Config config;

    @Override
    public void init() throws Exception {
        logger.info("=============================[ Initializing ElderRing ]===========================");
        super.init();

        AppParameters appParameters = AppParameters.parse(getParameters());
        config = initConfig(appParameters.getConfigPath());
        initLogging(config);

        UserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(config.getUserPrefsFilePath());
        UserPrefs userPrefs = initPrefs(userPrefsStorage);
        AddressBookStorage addressBookStorage = new JsonAddressBookStorage(userPrefs.getAddressBookFilePath());
        storage = new StorageManager(addressBookStorage, userPrefsStorage);

        model = initModelManager(storage, userPrefs);

        logic = new LogicManager(model, storage);

        ui = new UiManager(logic);
    }

    /**
     * Returns a {@code ModelManager} with the data from {@code storage}'s address book and {@code userPrefs}. <br>
     * The data from the sample address book will be used instead if {@code storage}'s address book is not found,
     * or an empty address book will be used instead if errors occur when reading {@code storage}'s address book.
     */
    private Model initModelManager(Storage storage, ReadOnlyUserPrefs userPrefs) {
        logger.info("Using data file : " + storage.getAddressBookFilePath());

        // Ensure data directory exists first, before any read/write operations
        ensureDataDirectoryExists(storage.getAddressBookFilePath());

        Optional<ReadOnlyAddressBook> addressBookOptional;
        ReadOnlyAddressBook initialData;
        boolean shouldSaveData = false;

        try {
            addressBookOptional = storage.readAddressBook();

            if (!addressBookOptional.isPresent()) {
                // No existing file - use sample data and MUST save it
                logger.info("Creating a new data file " + storage.getAddressBookFilePath()
                        + " populated with a sample AddressBook.");
                initialData = SampleDataUtil.getSampleAddressBook();
                shouldSaveData = true;
            } else {
                // File exists - use loaded data
                initialData = addressBookOptional.get();
                shouldSaveData = true; // Save to clean any duplicates/invalid entries
            }

        } catch (DataLoadingException e) {
            logger.warning("Data file at " + storage.getAddressBookFilePath() + " could not be loaded."
                    + " Will be starting with an empty AddressBook.");
            initialData = new AddressBook();
            shouldSaveData = true;
        }

        // Save the data immediately to ensure:
        // 1. File and folder are created
        // 2. Sample data is persisted
        // 3. Duplicates/invalid entries are cleaned
        if (shouldSaveData) {
            try {
                storage.saveAddressBook(initialData);
                logger.info("Data has been saved to file: " + storage.getAddressBookFilePath());
                int personCount = initialData.getPersonList().size();
                if (personCount >= 250) {
                    logger.info("Address book is at or near capacity (" + personCount + "/250 entries)");
                }
            } catch (IOException e) {
                logger.warning("Failed to save data to file: " + StringUtil.getDetails(e));
            }
        }

        // Create model with the loaded data and its warnings
        ModelManager modelManager = new ModelManager(initialData, userPrefs);

        // Transfer warnings from AddressBook to Model
        List<String> warnings = initialData.getStorageWarnings();
        if (warnings != null && !warnings.isEmpty()) {
            modelManager.setStorageWarnings(warnings);
            logger.info("Stored " + warnings.size() + " warnings in model for UI display");
        }

        return modelManager;
    }

    //    /**
    //     * Returns a {@code ModelManager} with the data from {@code storage}'s address book and
    //     {@code userPrefs}. <br>
    //     * The data from the sample address book will be used instead if {@code storage}'s address book is not found,
    //     * or an empty address book will be used instead if errors occur when reading {@code storage}'s address book.
    //     */
    //    private Model initModelManager(Storage storage, ReadOnlyUserPrefs userPrefs) {
    //        logger.info("Using data file : " + storage.getAddressBookFilePath());
    //
    //        // Ensure data directory exists first, before any read/write operations
    //        ensureDataDirectoryExists(storage.getAddressBookFilePath());
    //
    //        Optional<ReadOnlyAddressBook> addressBookOptional;
    //        ReadOnlyAddressBook initialData;
    //        boolean shouldSaveData = false; // Track if data needs to be saved
    //        List<String> storageWarnings = new ArrayList<>(); // Store warnings locally
    //
    //        try {
    //            addressBookOptional = storage.readAddressBook();
    //
    //            if (!addressBookOptional.isPresent()) {
    //                // No existing file - use sample data and MUST save it
    //                logger.info("Creating a new data file " + storage.getAddressBookFilePath()
    //                        + " populated with a sample AddressBook.");
    //                initialData = SampleDataUtil.getSampleAddressBook();
    //                shouldSaveData = true; // MUST save to create the file
    //            } else {
    //                // File exists - use loaded data and save to clean duplicates/invalid entries
    //                initialData = addressBookOptional.get();
    //                shouldSaveData = true; // Save to clean any duplicates/invalid entries
    //
    //                // Display storage warnings in UI
    //                displayStorageWarnings(initialData.getStorageWarnings());
    //            }
    //
    //        } catch (DataLoadingException e) {
    //            logger.warning("Data file at " + storage.getAddressBookFilePath() + " could not be loaded."
    //                    + " Will be starting with an empty AddressBook.");
    //            initialData = new AddressBook();
    //            shouldSaveData = true; // Save empty address book to create valid file
    //        }
    //
    //        // Save the data immediately to ensure:
    //        // 1. File and folder are created
    //        // 2. Sample data is persisted
    //        // 3. Duplicates/invalid entries are cleaned
    //        if (shouldSaveData) {
    //            try {
    //                storage.saveAddressBook(initialData);
    //                logger.info("Data has been saved to file: " + storage.getAddressBookFilePath());
    //                int personCount = initialData.getPersonList().size();
    //                if (personCount >= 250) {
    //                    logger.info("Address book is at or near capacity (" + personCount + "/250 entries)");
    //                }
    //            } catch (IOException e) {
    //                logger.warning("Failed to save data to file: " + StringUtil.getDetails(e));
    //            }
    //        }
    //
    //        // Create model and set warnings
    //        ModelManager modelManager = new ModelManager(initialData, userPrefs);
    //        if (!storageWarnings.isEmpty()) {
    //            modelManager.setStorageWarnings(storageWarnings);
    //        }
    //
    //        return new ModelManager(initialData, userPrefs);
    //    }


    /**
     * Ensures the data directory exists. Creates it if necessary.
     * This is critical for preventing file save failures.
     */
    private void ensureDataDirectoryExists(Path filePath) {
        try {
            Path dataDir = filePath.getParent();
            if (dataDir != null && !java.nio.file.Files.exists(dataDir)) {
                java.nio.file.Files.createDirectories(dataDir);
                logger.info("Created data directory: " + dataDir);
            }
        } catch (IOException e) {
            logger.warning("Failed to create data directory: " + StringUtil.getDetails(e));
        }
    }

    private void initLogging(Config config) {
        LogsCenter.init(config);
    }

    /**
     * Returns a {@code Config} using the file at {@code configFilePath}. <br>
     * The default file path {@code Config#DEFAULT_CONFIG_FILE} will be used instead
     * if {@code configFilePath} is null.
     */
    protected Config initConfig(Path configFilePath) {
        Config initializedConfig;
        Path configFilePathUsed;

        configFilePathUsed = Config.DEFAULT_CONFIG_FILE;

        if (configFilePath != null) {
            logger.info("Custom Config file specified " + configFilePath);
            configFilePathUsed = configFilePath;
        }

        logger.info("Using config file : " + configFilePathUsed);

        try {
            Optional<Config> configOptional = ConfigUtil.readConfig(configFilePathUsed);
            if (!configOptional.isPresent()) {
                logger.info("Creating new config file " + configFilePathUsed);
            }
            initializedConfig = configOptional.orElse(new Config());
        } catch (DataLoadingException e) {
            logger.warning("Config file at " + configFilePathUsed + " could not be loaded."
                    + " Using default config properties.");
            initializedConfig = new Config();
        }

        //Update config file in case it was missing to begin with or there are new/unused fields
        try {
            ConfigUtil.saveConfig(initializedConfig, configFilePathUsed);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }
        return initializedConfig;
    }

    /**
     * Returns a {@code UserPrefs} using the file at {@code storage}'s user prefs file path,
     * or a new {@code UserPrefs} with default configuration if errors occur when
     * reading from the file.
     */
    protected UserPrefs initPrefs(UserPrefsStorage storage) {
        Path prefsFilePath = storage.getUserPrefsFilePath();
        logger.info("Using preference file : " + prefsFilePath);

        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            if (!prefsOptional.isPresent()) {
                logger.info("Creating new preference file " + prefsFilePath);
            }
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataLoadingException e) {
            logger.warning("Preference file at " + prefsFilePath + " could not be loaded."
                    + " Using default preferences.");
            initializedPrefs = new UserPrefs();
        }

        //Update prefs file in case it was missing to begin with or there are new/unused fields
        try {
            storage.saveUserPrefs(initializedPrefs);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }

        return initializedPrefs;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting AddressBook " + MainApp.VERSION);
        ui.start(primaryStage);
    }

    @Override
    public void stop() {
        logger.info("============================ [ Stopping ElderRing ] =============================");
        try {
            storage.saveUserPrefs(model.getUserPrefs());
        } catch (IOException e) {
            logger.severe("Failed to save preferences " + StringUtil.getDetails(e));
        }
    }
}
