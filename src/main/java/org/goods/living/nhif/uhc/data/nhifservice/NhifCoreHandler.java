/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.goods.living.nhif.uhc.data.nhifservice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import org.goods.living.nhif.uhc.data.utils.Logging;
import org.goods.living.nhif.uhc.data.utils.Props;
import org.goods.living.nhif.uhc.data.utils.Utilities;
import org.goods.living.nhif.uhc.data.database.Database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import org.json.JSONException;

/**
 *
 * @author ernestmurimi
 */
@Singleton
@Startup
public class NhifCoreHandler implements Runnable {

    private Logging logging;

    private Database database;

    private Thread worker;

    private boolean isRunning = false;

    PostNhif nhifApi = new PostNhif();

    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

    public void initialize() {
        try {

            Props.loadProperties();

            logging = new Logging(Props.getLOG_PATH());

            worker = new Thread(this);

            isRunning = true;

            logging.applicationLog(Utilities.logPreString() + "Innitialization complete", "", "1");

        } catch (Exception ex) {
            logging.applicationLog(Utilities.logPreString() + "Error Innitialize ::: " + ex.getMessage() + "\n" + ex.toString(), "", "1");
        }

    }

    @PostConstruct
    private void startup() {

        try {
            initialize();
            try {
                if (isRunning) {
                    logging.applicationLog(Utilities.logPreString() + "main thread loaded ...", "", "1");
                    worker.start();
                } else {
                    logging.applicationLog(Utilities.logPreString() + "failed to load main thread", "", "1");

                }
            } catch (Exception ex) {
                logging.applicationLog(Utilities.logPreString() + "Error Innitialize ::: " + ex.getMessage() + "\n" + ex.toString(), "", "1");

            }
        } catch (Exception ex) {
            logging.applicationLog(Utilities.logPreString() + "Error startup::: " + ex.getMessage() + "\n" + ex.toString(), "", "1");

        }

    }

    @PreDestroy
    private void destroy() {
        try {
            worker.stop();
        } catch (Exception ex) {
            logging.applicationLog(Utilities.logPreString() + "Error destroy ::: " + ex.getMessage() + "\n" + ex.toString(), "", "1");

        }
    }

    @Override
    public void run() {
        try {
            while (isRunning) {
                fetchAndProcess();
                //TimeUnit.MINUTES.sleep(1);
                TimeUnit.SECONDS.sleep(5);
            }
        } catch (InterruptedException ex) {
            logging.applicationLog(Utilities.logPreString() + "Error run ::: " + ex.getMessage() + "\n" + ex.toString(), "", "1");

        } catch (Exception ex) {
            Logger.getLogger(NhifCoreHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void fetchAndProcess() throws SQLException, KeyManagementException, NoSuchAlgorithmException {

        int nhifStatusCode = nhifApi.pingNhifEndpoint();

        if (nhifStatusCode > 199 && nhifStatusCode < 300) {
            doNhif();
        } else {
            logging.applicationLog(Utilities.logPreString() + "NHIF API Response:: " + nhifStatusCode, "", "3");
        }

    }

    public void doNhif() {
        //update query to pick only already registered NHIF members
        String query_new = "SELECT * FROM odk_prod.TBREGISTRATIONPHASE2_COPY  WHERE ID NOT IN (SELECT REQUEST_ID FROM UHC_ALLREQUESTS_HISTORY_V2_SYNC) AND ID IN(SELECT RECORD_ID FROM TBREGIMAGES_PHASE2 WHERE IMAGE_STATUS =1) LIMIT 10";

        logging.applicationLog(Utilities.logPreString() + "doNhif::: " + query_new, "", "2");

        database = new Database(logging);

        try (Connection conn = database.getDatabaseConnection(); Statement statement = conn.createStatement()) {

            try (ResultSet resultSet = statement.executeQuery(query_new)) {

                ResultSetMetaData core_rsMetaData = resultSet.getMetaData();

                int resultColumns = core_rsMetaData.getColumnCount();

                while (resultSet.next()) {

                    //Check if member is registered on NHIF
                    
                    String member_id = resultSet.getString("MEMBER_MEMBER_SECA_MEMBER_ID");
                    String _URI = resultSet.getString("_URI");
                    int request_id = resultSet.getInt("ID");
                    
                    String sub_county =resultSet.getString("MEMBER_MEMBER_SECC_SUB_COUNTY");
                    String ward_name = resultSet.getString("MEMBER_MEMBER_SECC_WARD");
                    String member_facility = resultSet.getString("MEMBER_MEMBER_SECC_MEMBER_FACILITY_NAME");
                    
                    String temp[] = member_facility.split("_");
                    //String facility_code = database.getFacilityCode(member_facility);
                    
                    String facility_code = temp[0];
                               
                    String member_nhif_number = nhifApi.getMemberById(request_id,member_id, _URI);
                    

                    //if member_nhif_exists then skip registration and push UHC request
                    if (!"0".equalsIgnoreCase(member_nhif_number)) {

                        prepareUhcRequest(sub_county, member_nhif_number, ward_name, member_id, _URI, request_id, facility_code);
                        
                    } else {
                        
                        //Do not prepare NHIF request
                        int childCount = resultSet.getInt("CHILD_COUNT");

                        String NHIF_MEMBER_HAS_SPOUSE = resultSet.getString("NHIF_MEMBER_HAS_SPOUSE");

                        String member_image = database.getNhifImages(request_id, "0", "member");

                        String member_gender;

                        if ("male".equalsIgnoreCase(resultSet.getString("MEMBER_MEMBER_SECA_MEMBER_GENDER"))) {
                            member_gender = "M";
                        } else {
                            member_gender = "F";
                        }

                        JSONObject dataObject = new JSONObject();
                        JSONObject attributesObject = new JSONObject();
                        JSONObject arrayAttributes = new JSONObject();

                        attributesObject.put("type", "members");
                        String maritalStatus;
                        
                        for (int i = 1; i < resultColumns; i++) {

                            //start constructing JSONObject
                            if ("yes".equalsIgnoreCase(NHIF_MEMBER_HAS_SPOUSE)) {
                                maritalStatus = "Married";
                            } else {
                                maritalStatus = "Single";
                            }

                            arrayAttributes.put("id_number", member_id);
                            arrayAttributes.put("marital_status", maritalStatus);
                            arrayAttributes.put("birthdate", formatDob(resultSet.getString("MEMBER_MEMBER_SECA_MEMBER_DOB")));
                            arrayAttributes.put("branch_no", "");
                            arrayAttributes.put("gender", member_gender);
                            arrayAttributes.put("phone", resultSet.getString("MEMBER_MEMBER_SECB_MEMBER_MOBILE"));
                            arrayAttributes.put("emp_no", "459935");
                            arrayAttributes.put("emp_name", "Isiolo County Govt");
                            arrayAttributes.put("photo", member_image);
                            arrayAttributes.put("last_name", resultSet.getString("MEMBER_MEMBER_SECA_MEMBER_OTHER_NAMES"));
                            arrayAttributes.put("first_name", resultSet.getString("MEMBER_MEMBER_SECA_MEMBER_SURNAME"));
                            arrayAttributes.put("email", resultSet.getString("MEMBER_MEMBER_SECB_MEMBER_EMAIL"));

                        }

                        //if Member has spouse or has children then construct dependants object
                        if ("yes".equalsIgnoreCase(NHIF_MEMBER_HAS_SPOUSE) || childCount > 0) {

                            JSONArray arr = new JSONArray();

                            if ("yes".equalsIgnoreCase(NHIF_MEMBER_HAS_SPOUSE)) {
                               // String spouse_image = database.getNhifImages(request_id, "0", "spouse");

                                JSONObject spousejson = new JSONObject();

                                String spouseRelationship, spouse_gender;

                                if ("male".equalsIgnoreCase(resultSet.getString("SPOUSE_SPOUSE_SECB_SPOUSE_GENDER"))) {
                                    spouseRelationship = "Husband";
                                    spouse_gender = "M";
                                } else {
                                    spouseRelationship = "Wife";
                                    spouse_gender = "F";
                                }
                                
                                
                        //
                   
                                spousejson.put("id_number", resultSet.getString("SPOUSE_SPOUSE_SECA_SPOUSE_ID"));
                                spousejson.put("birthdate", formatDob(resultSet.getString("SPOUSE_SPOUSE_SECA_SPOUSE_DOB")));
                                spousejson.put("gender", spouse_gender);
                                spousejson.put("last_name", resultSet.getString("SPOUSE_SPOUSE_SECA_SPOUSE_OTHER_NAMES"));
                                spousejson.put("relationship", spouseRelationship);
                                //spousejson.put("photo", spouse_image);
                                spousejson.put("first_name", resultSet.getString("SPOUSE_SPOUSE_SECA_SPOUSE_SURNAME"));

                                arr.put(spousejson);
                            }
                            //Populate for Children
                            if (childCount > 0) {
                                //Add children data to dependants array object
                                arr = getChildrenDetails(request_id,_URI, childCount, arr);

                            }

                            arrayAttributes.put("dependants", arr);
                        }

                        attributesObject.put("attributes", arrayAttributes);
                        dataObject.put("data", attributesObject);

                        //Save request metadata to DB, log JSONObject
                        logging.applicationLog(Utilities.logPreString() + "Request JSONObject:- " + dataObject + " \n \n", "", "2");

                        //send object to database
                        database.saveNhifRequest(_URI, dataObject, member_id, "NHIF_REGISTRATION");

                        String stringResponse = nhifApi.postNhifPayload(dataObject);
//
                       processResponse(stringResponse, _URI, sub_county,ward_name,member_id,request_id,facility_code);

//UNCOMMENT TO PROCESS NHIF REQUESTS
                    }
                    
                }
            } catch (Exception ex) {
                logging.applicationLog(Utilities.logPreString() + "SQLException:- " + ex.getMessage(), "", "3");
            }

        } catch (Exception ex) {

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String sStackTrace = sw.toString();

            logging.applicationLog(Utilities.logPreString() + "SQLException:- " + sStackTrace, "", "3");
        }
    }

    public JSONArray getChildrenDetails(int request_id, String _URI, int childCount, JSONArray arr) throws SQLException, Exception {

        database = new Database(logging);
        //ProcessImage processimage = new ProcessImage();

        try (Connection conn = database.getDatabaseConnection(); Statement statement = conn.createStatement()) {

            String sql = "SELECT _ORDINAL_NUMBER, CHILD_AGE,CHILD_DOB,CHILD_NAME,CHILD_GENDER,CHILD_FACILITY_NAME FROM odk_prod.UHC_REGISTRATION_PHASE2_CHILD WHERE _TOP_LEVEL_AURI='" + _URI + "'";

            System.out.print(sql);

            try (ResultSet children_rs = statement.executeQuery(sql)) {

                HashMap<String, JSONObject> map = new HashMap<>();

                //int child_num = Integer.parseInt(children_rs.getString("_ORDINAL_NUMBER"));
                String child_num = children_rs.getString("_ORDINAL_NUMBER");

                while (children_rs.next()) {

                    //String child_image =  database.getNhifImages(request_id, child_num, "child");

                    //child_image = processimage.fetchImage(_URI, "child", child_num);

                    String childRelationship = "";

                    if ("male".equalsIgnoreCase(children_rs.getString("CHILD_GENDER"))) {
                        childRelationship = "Son";
                    } else {
                        childRelationship = "Daughter";
                    }

                    for (int i = 1; i < 2; i++) {

                        JSONObject json = new JSONObject();

                        json.put("relationship", childRelationship);
                        json.put("last_name", children_rs.getString("CHILD_NAME"));
                        json.put("first_name", "");
                       // json.put("photo", child_image);
                        json.put("gender", children_rs.getString("CHILD_GENDER"));
                        json.put("birthdate", formatDob(children_rs.getString("CHILD_DOB")));

                        map.put("json" + i, json);
                        arr.put(map.get("json" + i));

                    }
                }

            } catch (Exception ex) {
                logging.applicationLog(Utilities.logPreString() + "" + _URI + " Core Member Data Exception" + ex.getMessage(), "", "5");
            }
        }

        return arr;
    }

    public void processResponse(String nhifResponse, String _URI , String sub_county, String ward_name, String member_id, int request_id, String facility_code) {
        //logging.applicationLog(Utilities.logPreString() + "RAW RESPONSE JSONObject:- " + nhifResponse + "\n\n", "", "2");

        String nhif_response_json;
        String nhif_response_string;
        String nhif_response_code;
        String member_number;
        JSONObject jsonResponse;

        try {
            database = new Database(logging);

            try {

                JSONObject jsonResponseRaw = new JSONObject(nhifResponse);

                if (jsonResponseRaw.has("errors")) {

                    jsonResponse = new JSONObject(nhifResponse.replace("[", "").replace("]", ""));

                    JSONObject jsonChildObject = (JSONObject) jsonResponse.get("errors");

                    nhif_response_string = jsonChildObject.get("detail").toString();
                    nhif_response_json = jsonChildObject.toString();
                    nhif_response_code = jsonChildObject.get("status").toString();
                    member_number = "";

                } else {
                    jsonResponse = new JSONObject(nhifResponse);

                    JSONObject jsonResponseSuccess = new JSONObject(nhifResponse);
                    JSONObject dataObject = (JSONObject) jsonResponseSuccess.get("data");

                    JSONObject attributesJson = (JSONObject) dataObject.get("attributes");
                    //Save attributesJson to DB
                    nhif_response_json = attributesJson.toString();
                    nhif_response_string = "Success";
                    nhif_response_code = "200";
                    member_number = attributesJson.get("mem_no").toString();
                    
                    //
                    prepareUhcRequest(sub_county,member_number,ward_name, member_id, _URI, request_id, facility_code);
                }

                logging.applicationLog(Utilities.logPreString() + "RESPONSE JSONObject:- " + jsonResponse + "\n\n", "", "2");

                database.saveNhifResponse(nhif_response_string, member_number, nhif_response_json, nhif_response_code, _URI);

            } catch (JSONException ex) {

                logging.applicationLog(Utilities.logPreString() + "RESPONSE JSONException:- " + ex.getMessage() + nhifResponse + "\n\n", "", "3");
                database.saveNhifResponse("Error Getting HNIF Response", "", "", "500", _URI);

            }

        } catch (Exception ex) {
            logging.applicationLog(Utilities.logPreString() + "RESPONSE Exception:- " + ex.getMessage() + "\n\n", "", "3");
        }

    }
    
    
    public void prepareUhcRequest(String sub_county,String member_nhif_number,String ward_name, String member_id, String _URI, int request_id, String facility_code){
        
        database = new Database(logging);
        
                        JSONObject uhcDataObject = new JSONObject();
                        JSONObject uhcAttributesObject = new JSONObject();
                        JSONObject uhcArrayAttributes = new JSONObject();

                        String subcounty_name = sub_county;
                        String subcounty_code;

                        switch (subcounty_name) {

                            case "garbatulla":
                                subcounty_code = "1101";
                                break;
                            case "isiolo":
                                subcounty_code = "1102";
                                break;
                            case "merti":
                                subcounty_code = "1103";
                                break;
                            default:
                                subcounty_code = "11";
                                break;
                        }

                        uhcArrayAttributes.put("mem_no", member_nhif_number);
                        uhcArrayAttributes.put("county_no", "11");
                        uhcArrayAttributes.put("subcounty_no", subcounty_code);
                        uhcArrayAttributes.put("ward", ward_name);
                        uhcArrayAttributes.put("facility_no",facility_code);

                        uhcAttributesObject.put("type", "uhcregistrations");

                        uhcAttributesObject.put("attributes", uhcArrayAttributes);
                        uhcDataObject.put("data", uhcAttributesObject);
                  
 
                        //prepare and send request to UHC
                        
                        if("".equals(facility_code)){
                            logging.applicationLog(Utilities.logPreString() + "Facility not Mapped for Member Number:- " + member_nhif_number + " \n \n", "", "2");
                        
                            database.saveToAllRequests(request_id,_URI, member_id,"Facility not found or unmapped");
                        }else{
                            //save UHC request
                        logging.applicationLog(Utilities.logPreString() + "UHC REQUEST JSONObject:- " + uhcDataObject + " \n \n", "", "2");
                        
                        database.saveUhcRequest(_URI, uhcDataObject.toString(), member_id, member_nhif_number,request_id);
                        
                        nhifApi.postUhcPayload(uhcDataObject,_URI,request_id);
                        }
                        
    }
    
    public String formatDob(String dob) {

        String nhifDate = dob.substring(0, 10);

        return nhifDate;
    }

}
