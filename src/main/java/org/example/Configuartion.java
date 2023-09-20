package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Configuartion {
    private Properties prop;
    private static final Logger logger = AppLogger.getLogger(FileUploadController.class.getName());

    public Configuartion() {
        this.prop = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) { // Pfad zur config.properties-Datei
            prop.load(input);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Fehler beim Laden der config.properties Datei", ex);
        }
    }

    public String getExcludedNetworkAdapter() {
        return prop.getProperty("excludedNetworkAdapter", "Vmxnet3"); // "Vmxnet3" ist der Standardwert
    }
}
