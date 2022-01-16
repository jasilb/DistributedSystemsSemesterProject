package edu.yu.cs.com3800;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public interface LoggingServer {

      default Logger initializeLogging(String s){

          File file = new File(System.getProperty("user.dir")+ "/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-kk_mm")));
          boolean dir=file.mkdir();
         // System.out.println(dir);
          //System.out.println(file.toString());
        Logger logger = Logger.getLogger(file +"/"+ s+".log");
        logger.setUseParentHandlers(false);
        FileHandler fileHandler;
        try {
            fileHandler = new FileHandler(file + "/" + s+".log");
            logger.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
          //System.out.println(logger.getName());
        return logger;
    }
}
