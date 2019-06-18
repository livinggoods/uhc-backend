/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.goods.living.nhif.uhc.data.nhifservice;

/**
 *
 * @author ernestmurimi
 */

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.imageio.ImageIO;

import org.goods.living.nhif.uhc.data.utils.Props;
import org.goods.living.nhif.uhc.data.database.Database;
import org.goods.living.nhif.uhc.data.utils.Logging;
import org.goods.living.nhif.uhc.data.utils.Utilities;

/**
 *
 * @author ernestmurimi
 */
public class ProcessImage {

    /**
     * @param args the command line arguments
     */
    private Database database;
    private Logging logging;

    private static final int IMG_WIDTH = 300;
    private static final int IMG_HEIGHT = 300;

    public static final String LOG_PATH = Props.getLOG_PATH();

    public void prepareResize(String request_id, String image_path, String image_name, String _URI, int loop_count, String member_type) {

        logging = new Logging(LOG_PATH);
        String output_path = Props.getImageLocation();
        int type = 0;
        try {

            database = new Database(logging);
            try {
                BufferedImage originalImage = ImageIO.read(new File(image_path));

                type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();

                BufferedImage resizeImageJpg = resizeImage(originalImage, type);

                ImageIO.write(resizeImageJpg, "jpg", new File(output_path + image_name));
                //save entry to Database

                logging.applicationLog(Utilities.logPreString() + "Image Created" + output_path + image_name, "", "1");
                
                database.updateUhcImagesPut(request_id,"1");
                
                database.saveUhcImage(request_id, _URI, image_name, loop_count, member_type, 1);

            } catch (Exception e) {

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);

                logging.applicationLog(Utilities.logPreString() + "Exception Image not Created " + image_name + " - " + sw.toString(), "", "2");
                
                database.updateUhcImagesPut(request_id,"2");
                
                database.saveUhcImage(request_id, _URI, "Exception Image not Created " + image_name, loop_count, member_type, 2);

            }

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString();
            database.saveUhcImage(request_id, _URI, "Exception Image not Created " + image_name, loop_count, member_type, 2);
            database.updateUhcImagesPut(request_id,"2");
            
            
            logging.applicationLog(Utilities.logPreString() + "Exception Image not Created " + image_name + " - " + sStackTrace, "", "1");
        }

    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int type) {
        BufferedImage resizedImage = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, type);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
        g.dispose();

        return resizedImage;
    }

    public void createImageFromBlob(String request_id, String _URI, String member_type, int loop_count) {

        logging = new Logging(LOG_PATH);
        database = new Database(logging);

        String fileName = "";

        switch (member_type) {
            case "member":

                fileName = "member_" + _URI + ".jpg";
                break;
            case "spouse":

                fileName = "spouse_" + _URI + ".jpg";
                break;
            case "child":
                fileName = "child_" + loop_count + "_" + _URI + ".jpg";
                break;
            default:

                break;
        }

        fileName = fileName.replace("uuid:", "");

        String destName = "/var/lib/mysql-files/" + fileName;

        //createFileFromBlob
        try {
            database.createFileFromBlob(request_id,_URI, member_type, fileName);
            
            prepareResize(request_id, destName, fileName, _URI, loop_count, member_type);

        } catch (Exception ex) {

            logging.applicationLog(Utilities.logPreString() + "Exception fetchImage ::: " + ex.getMessage(), "", "2");
            
            database.updateUhcImagesPut(request_id,"2");
            
            database.saveUhcImage(request_id, _URI, destName, loop_count, member_type, 0);
        }

    }

}
