package org.goods.living.nhif.uhc.data.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;

/**
 * Created by Ernest
 */
public final class Logging {

    private final String LOGS_PATH;

    public Logging(String LOGS_PATH) {
        this.LOGS_PATH = LOGS_PATH;
    }

    public void applicationLog(String details, String uniqueId, String logLevel) {
        Date now = new Date();

        String typeOfLog = "";
        switch (logLevel) {
            case "1":
                typeOfLog = "StartUp";
                break;
            case "2":
                typeOfLog = "Data";
                break;
            case "3":
                typeOfLog = "Exceptions";
                break;
            case "4":
                typeOfLog = "SMSApi";
                break;
            case "5":
                typeOfLog = "NHIF_Payload";
                break;
            default:
                typeOfLog = "Others";
                break;
        }

        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String LogDate = formatter.format(today);
        SimpleDateFormat LogTimeformatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String LogTime = LogTimeformatter.format(today);
        File dir = new File(LOGS_PATH + "/" + typeOfLog + "/" + LogDate);
        BufferedWriter writer = null;
        if (dir.exists()) {
            dir.setWritable(true);
        } else {
            dir.mkdirs();
            dir.setWritable(true);
        }

        String randomNum = "";
        int maximum = 100000;

        try {
            if (uniqueId.equals("")) {
                String minimum = "5";
                randomNum = minimum + (int) (Math.random() * maximum);
                uniqueId = randomNum;

            } else {
                randomNum = "10" + (int) (Math.random() * maximum);
            }

            String fileName = "";

            File[] fileList = dir.listFiles();

            if (fileList.length > 0) {
                for (int i = 0; i < fileList.length; i++) {
                    if (fileList[i].length() < 25000000) {
                        fileName = "/" + fileList[i].getName();
                    } else {
                        fileName = "/" + Utilities.dateFormat.get().format(now) + "-" + uniqueId + ".log";
                    }
                }
            } else {
                fileName = "/" + Utilities.dateFormat.get().format(now) + "-" + randomNum + ".log";
            }

            writer = new BufferedWriter(new FileWriter(dir + fileName, true));
            writer.write(LogTime + " ~ " + details);
            writer.newLine();
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(Logging.class.getSimpleName()).log(Level.SEVERE, "Exception Occurred:- " + e.getMessage(), e);
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(Logging.class.getSimpleName()).log(Level.SEVERE, "IOException Occurred:- " + ex.getMessage(), ex);
            }
        }
    }
}
