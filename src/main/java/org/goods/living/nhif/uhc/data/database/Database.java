/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.goods.living.nhif.uhc.data.database;

/**
 *
 * @author ernestmurimi
 */
import org.goods.living.nhif.uhc.data.utils.Logging;
import org.goods.living.nhif.uhc.data.utils.Utilities;
import org.goods.living.nhif.uhc.data.utils.Props;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.json.JSONObject;

public final class Database {

    private DataSource dataSource;
    private InitialContext initialContext;

    private final transient Logging logging;

    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

    public Database(Logging logging) {
        this.logging = logging;
    }

    public Connection getDatabaseConnection() throws Exception {
        Connection conn = null;
        try {
            initialContext = new InitialContext();

            dataSource = (DataSource) initialContext.lookup(Props.getDATABASE_CONTEXT_URL());

            conn = dataSource.getConnection();
            logging.applicationLog(Utilities.logPreString() + "Connection initialization completed.....", "", "1");
        } catch (NamingException ex) {
            logging.applicationLog(Utilities.logPreString() + Utilities.logPreString()
                    + "Context Initialization failed. Review: "
                    + "Context URL or Datasource: " + ex.getMessage(), "", "3");
        } catch (SQLException ex) {
            logging.applicationLog(Utilities.logPreString() + Utilities.logPreString()
                    + "Context Initialization failed. Review: "
                    + "SQL Connectivity: " + ex.getMessage(), "", "3");
        }
        return conn;
    }

    public String getNhifImages(int request_id, String child_count, String member_type) {

        String query = "SELECT IMAGE_NAME FROM TBREGIMAGES_PHASE2 WHERE RECORD_ID = ? AND MEMBER_TYPE= ? AND CHILD_COUNT= ?";

        String image_name = "";
        String image_prefix_path = Props.getFinalImageUrl();

        try (Connection conn = getDatabaseConnection(); PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setInt(1, request_id);
            preparedStatement.setString(2, member_type);
            preparedStatement.setString(3, child_count);

            try (ResultSet rs = preparedStatement.executeQuery()) {

                while (rs.next()) {
                    image_name = image_prefix_path + rs.getString("IMAGE_NAME");
                }
            }

        } catch (Exception ex) {
            System.out.print(ex.getMessage());
        }
        
        return image_name;
    }
    
    public String searchMemberDb(String member_id) {

        String _URI_COUNT="0";
        
            logging.applicationLog(Utilities.logPreString() + "facility_name Exception:- " + member_id, "", "3");
        
        String query = "SELECT COUNT(_URI) AS _URI_COUNT FROM UHC_REGISTRATION_CORE WHERE MEMBER_MEMBER_SECA_MEMBER_ID = ?";

        try (Connection conn = getDatabaseConnection(); PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setString(1, member_id);

            try (ResultSet rs = preparedStatement.executeQuery()) {

                while (rs.next()) {
                    _URI_COUNT = rs.getString("_URI_COUNT");
                }
            }

        } catch (Exception ex) {
           logging.applicationLog(Utilities.logPreString() + "facility_name Exception:- " + ex.getMessage(), "", "3");
        }
        
        return _URI_COUNT;
    }
    
    public void saveToAllRequests(int request_id, String _URI, String member_id, String message) {

        String query = "INSERT INTO UHC_ALLREQUESTS_HISTORY_V2_SYNC (REQUEST_ID,_URI,MEMBER_NATIONAL_ID,DATE_CREATED, MESSAGE) VALUES (?,?,?,?,?)";

        try (Connection conn = getDatabaseConnection(); PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            
            preparedStatement.setInt(1, request_id);
            preparedStatement.setString(2, _URI);
            preparedStatement.setString(3, member_id);
            preparedStatement.setTimestamp(4, timestamp);
            preparedStatement.setString(5, message);

            preparedStatement.executeUpdate();

        } catch (Exception ex) {
            System.out.print(ex.getMessage());
        }
    }

    public void saveNhifRequest(String _URI, JSONObject dataObject, String member_id, String REQUEST_TYPE) {

        String query = "INSERT INTO UHC_REGISTRATION_HISTORY_NHIF (PARENT_URI,NHIF_STATUS,PROCESSED_DATE,NHIF_REQUEST,MEMBER_ID,REQUEST_TYPE) VALUES (?,?,?,?,?,?)";

        try (Connection conn = getDatabaseConnection(); PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setString(1, _URI);
            preparedStatement.setInt(2, 0);
            preparedStatement.setTimestamp(3, timestamp);
            preparedStatement.setString(4, dataObject.toString());
            preparedStatement.setString(5, member_id);
            preparedStatement.setString(6, REQUEST_TYPE);

            preparedStatement.executeUpdate();

        } catch (Exception ex) {
            System.out.print(ex.getMessage());
        }
    }

    public void saveNhifFullRequest(String _URI, String dataObject, String member_id, String member_number, String REQUEST_TYPE, String nhif_response, String response_code, String error_description) {

        String query = "INSERT INTO UHC_REGISTRATION_HISTORY_NHIF (PARENT_URI,NHIF_STATUS,PROCESSED_DATE,NHIF_REQUEST,MEMBER_ID,NHIF_MEMBER_NO,REQUEST_TYPE,NHIF_RESPONSE,NHIF_RESPONSE_CODE,ERROR_DESCRIPTION) VALUES (?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = getDatabaseConnection(); PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setString(1, _URI);
            preparedStatement.setInt(2, 1);
            preparedStatement.setTimestamp(3, timestamp);
            preparedStatement.setString(4, dataObject);
            preparedStatement.setString(5, member_id);
            preparedStatement.setString(6, member_number);
            preparedStatement.setString(7, REQUEST_TYPE);
            preparedStatement.setString(8, nhif_response);
            preparedStatement.setString(9, response_code);
            preparedStatement.setString(10, error_description);

            preparedStatement.executeUpdate();

        } catch (Exception ex) {
            System.out.print(ex.getMessage());
        }
    }

    public void saveNhifResponse(String respDescription, String mem_no, String nhifjsonresp, String respCode, String _URI) {

        String updateSql = "UPDATE odk_prod.UHC_REGISTRATION_HISTORY_NHIF SET NHIF_STATUS= ?, NHIF_RESPONSE= ?, NHIF_RESPONSE_CODE= ?, NHIF_MEMBER_NO=?, ERROR_DESCRIPTION= ? WHERE PARENT_URI = ? ";
        //NHIF_RESPONSE='" + nhifjsonresp +
        try (Connection conn = getDatabaseConnection(); PreparedStatement preparedUpdateStatement = conn.prepareStatement(updateSql)) {

            preparedUpdateStatement.setInt(1, 1);
            preparedUpdateStatement.setString(2, nhifjsonresp);
            preparedUpdateStatement.setString(3, respCode);
            preparedUpdateStatement.setString(4, mem_no);
            preparedUpdateStatement.setString(5, respDescription);
            preparedUpdateStatement.setString(6, _URI);

            preparedUpdateStatement.executeUpdate();

        } catch (Exception ex) {
            System.out.print("Error updating entry " + ex.getMessage());
        }

    }

    public void saveUhcRequest(String _URI, String dataObject, String member_id, String member_number, int record_id) {

        String query = "INSERT INTO TBUHC_REGISTRATION_HISTORY_PHASE2 (_URI,STATUS,PROCESSED_DATE,UHC_REQUEST,MEMBER_ID,MEMBER_NUMBER,RECORD_ID) VALUES (?,?,?,?,?,?,?)";

        try (Connection conn = getDatabaseConnection(); PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setString(1, _URI);
            preparedStatement.setInt(2, 0);
            preparedStatement.setTimestamp(3, timestamp);
            preparedStatement.setString(4, dataObject);
            preparedStatement.setString(5, member_id);
            preparedStatement.setString(6, member_number);
            preparedStatement.setInt(7, record_id);

            preparedStatement.executeUpdate();

        } catch (Exception ex) {
            System.out.print(ex.getMessage());
        }
    }

    public void updateUhcRequest(String _URI, String dataObject, String uhc_number, int record_id) {

        String query = "UPDATE TBUHC_REGISTRATION_HISTORY_PHASE2 SET UHC_RESPONSE=?,STATUS=?,UHC_NUMBER=? WHERE RECORD_ID =?";

        try (Connection conn = getDatabaseConnection(); PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setString(1, dataObject);
            preparedStatement.setInt(2, 1);
            preparedStatement.setString(3, uhc_number);
            preparedStatement.setInt(4, record_id);

            preparedStatement.executeUpdate();

        } catch (Exception ex) {
            System.out.print(ex.getMessage());
        }
    }
    
    
     public String getFacilityCode(String facility_name){
        
        String facilityCode="";
        
            logging.applicationLog(Utilities.logPreString() + "facility_name Exception:- " + facility_name, "", "3");
        
        String query = "SELECT FACILITY_CODE FROM UHC_ISIOLO_FACILITIES_CLEAN WHERE FACILITY_RAW_NAME = ?";

        try (Connection conn = getDatabaseConnection(); PreparedStatement preparedStatement = conn.prepareStatement(query)) {

            preparedStatement.setString(1, facility_name);

            try (ResultSet rs = preparedStatement.executeQuery()) {

                while (rs.next()) {
                    facilityCode = rs.getString("Facility_code");
                }
            }

        } catch (Exception ex) {
           logging.applicationLog(Utilities.logPreString() + "facility_name Exception:- " + ex.getMessage(), "", "3");
        }
        
        return facilityCode;
    }

}
