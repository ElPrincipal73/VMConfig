package de.comline;

import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class AppLogger {
    private static FileHandler fh = null;
    private static FileHandler GetFileHandler() {
        if(fh == null) {
            try {
                fh = new FileHandler("LogFile.log", true);
                SimpleFormatter formatter = new SimpleFormatter();
                fh.setFormatter(formatter);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        return fh;
    }
    public static Logger getLogger(String className) {
        System.out.println("Create Logger: " + className);
        Logger logger = Logger.getLogger(className);
        try {
            FileHandler file = GetFileHandler();

            boolean alreadyHasFileHandler = Arrays.stream(logger.getHandlers())
                    .anyMatch(handler -> handler instanceof FileHandler);

            if (!alreadyHasFileHandler) {
                logger.addHandler(file);
            }
            logger.setLevel(Level.ALL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return logger;
    }

}
