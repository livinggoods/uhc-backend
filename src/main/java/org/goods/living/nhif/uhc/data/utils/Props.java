/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.goods.living.nhif.uhc.data.utils;

import java.io.FileInputStream;
import java.util.Properties;

/**
 *
 * @author ernestmurimi
 */
public final class Props {
   
    private  static final String PROPS_FILE = "nhif.properties";
   
    private static String DATABASE_CONTEXT_URL;

    private static String LOG_PATH;

    private static String SMS_SERVER_URL;

    private static String SMS_API_USER;

    private static String SMS_API_PASSWORD;

    private static String SMS_SERVICE_NAME;

    private static String TABLE_NAME;
    
    private static String IMAGE_LOCATION;
    
    private static String NHIF_ENDPOINT;
    
    private static String AUTH_KEY;
    
    private static String DOWNLOAD_IMAGE_URL;
    
    private static String DOWNLOAD_IMAGE_URL_CHILD;
    
    private static String RAW_IMAGE_LOCATION;
    
    private static String FINAL_IMAGE_URL;
    
    private static String NHIF_PING_ENDPOINT;
    
    private static String API_MODE;
    /**
     * Load properties.
     *
     */
    public static void loadProperties() {
        try {
            Properties properties = new Properties();
            try (FileInputStream fileinput = new FileInputStream(PROPS_FILE)) {
                properties.load(fileinput);
            }
            
            LOG_PATH = properties.getProperty("LOG_PATH");
            
            DATABASE_CONTEXT_URL = properties.getProperty("DATABASE_CONTEXT_URL");

            TABLE_NAME = properties.getProperty("TABLE_NAME");

            SMS_SERVICE_NAME = properties.getProperty("SMS_SERVICE_NAME");

            SMS_SERVER_URL = properties.getProperty("SMS_SERVER_URL");

            SMS_API_USER = properties.getProperty("SMS_API_USER");

            SMS_API_PASSWORD = properties.getProperty("SMS_API_PASSWORD");
            
            IMAGE_LOCATION = properties.getProperty("IMAGE_LOCATION");
            
            NHIF_ENDPOINT = properties.getProperty("NHIF_ENDPOINT");
            
            AUTH_KEY = properties.getProperty("AUTH_KEY");
            
            DOWNLOAD_IMAGE_URL = properties.getProperty("DOWNLOAD_IMAGE_URL");
            
            DOWNLOAD_IMAGE_URL_CHILD = properties.getProperty("DOWNLOAD_IMAGE_URL_CHILD");
            
            RAW_IMAGE_LOCATION = properties.getProperty("RAW_IMAGE_LOCATION");
            
            FINAL_IMAGE_URL = properties.getProperty("FINAL_IMAGE_URL");
            
            NHIF_PING_ENDPOINT = properties.getProperty("NHIF_PING_ENDPOINT");
            
            API_MODE = properties.getProperty("API_MODE");
            
        } catch (Exception ex) {
            System.err.print("ERROR: Failed to load properties file.\nCause: " + ex.getMessage());
        }
    }
    public static  String getDATABASE_CONTEXT_URL() {
        return DATABASE_CONTEXT_URL;
    }

    public static String getLOG_PATH() {
        return LOG_PATH;
    }

    public static String getSMS_SERVER_URL() {
        return SMS_SERVER_URL;
    }

    public static String getSMS_SERVER_USER() {
        return SMS_API_USER;
    }

    public static String getSMS_SERVER_PASSWORD() {
        return SMS_API_PASSWORD;
    }

    public static String getTableName() {
        return TABLE_NAME;
    }   
    
    
    public static String getSmsServiceName() {
        return SMS_SERVICE_NAME;
    }
    
    public static String getImageLocation(){
        return IMAGE_LOCATION;
    }
    
    public static String getNhifEndpoint(){
        
        return NHIF_ENDPOINT;
        
    }
    
    public static String getAuthKey(){
        return AUTH_KEY;
    }
    
    public static String getDownloadUrl(){
        return DOWNLOAD_IMAGE_URL;
    }
    
    public static String getChildImageUrl(){
        return DOWNLOAD_IMAGE_URL_CHILD;
    }
    
    public static String getRawImageLocation(){
        return RAW_IMAGE_LOCATION;
    }
    
    public static String getFinalImageUrl(){
        return FINAL_IMAGE_URL;
    }
    
    public static String getNhifPingEndpoint(){
        return NHIF_PING_ENDPOINT;
    }
    
    public static String getApiMode(){
        return API_MODE;
    }
}
