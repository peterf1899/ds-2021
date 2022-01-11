package blockchain.node;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public abstract class DataStorage {

    private static final String PATH = "datastorage/";
    private static final String EXTENSION = ".datastorage";

    private String fileName;
    private Properties properties;

    public DataStorage(String fileName) throws Exception {
        this.fileName = fileName;

        // Use Java Properties for data storage
        this.properties = new Properties();
        this.properties.load(new FileInputStream(PATH + this.fileName + EXTENSION));
    }

    // Get value of a key
    protected String get(String key) {
        return this.properties.getProperty(key);
    }

    // Set value of a key
    protected void set(String key, String value) throws Exception {
        this.properties.setProperty(key, value);
        this.properties.store(new FileOutputStream(PATH + this.fileName + EXTENSION), null);
    }
}
