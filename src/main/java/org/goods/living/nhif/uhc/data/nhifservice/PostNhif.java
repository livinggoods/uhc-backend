/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.goods.living.nhif.uhc.data.nhifservice;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.HttpsURLConnection;
import org.goods.living.nhif.uhc.data.database.Database;

import org.goods.living.nhif.uhc.data.utils.Logging;
import org.goods.living.nhif.uhc.data.utils.Props;

import org.goods.living.nhif.uhc.data.utils.RelaxedSSLContext;
import org.goods.living.nhif.uhc.data.utils.Utilities;

/**
 *
 * @author ernestmurimi
 */
public class PostNhif {

    private Logging logging;
    private String base_url;
    private String nhif_url;
    private String nhif_ping_url;
    private String nhif_url_member_id;
    private String nhif_uhc_url;
    private String authKey;
    private String apiEnvironment;

    private Database database;

    public void initialize() {
        try {

            Props.loadProperties();
            
            logging = new Logging(Props.getLOG_PATH());
            
            apiEnvironment = Props.getApiMode();
            
            base_url = Props.getNhifEndpoint();
            //https://api.nhif.or.ke:8443/api/agents
            
            if("TEST".equals(apiEnvironment)){
                nhif_url = base_url+".test/v1/members";
                nhif_ping_url = base_url+".test/v1/ping";
                nhif_url_member_id = base_url+".test/v1/members-by-id/";
                nhif_uhc_url = base_url+".test/v1/uhcregistrations";
                
            }else if("LIVE".equals(apiEnvironment)){
                nhif_url = base_url+"/v1/members";
                nhif_ping_url = base_url+"/v1/ping";
                nhif_url_member_id = base_url+"/v1/members-by-id/";
                nhif_uhc_url = base_url+"/v1/uhcregistrations";
            }
            
            
            //Modify endpoints to Prod or Test
            //.test/v1/member
            authKey = Props.getAuthKey();

            logging.applicationLog(Utilities.logPreString() + "Logging Props Initialized Initialization complete", "", "1");

        } catch (Exception ex) {
            logging.applicationLog(Utilities.logPreString() + "Error Innitialize Props::: " + ex.getMessage() + "\n" + ex.toString(), "", "1");
        }

    }

    public String postNhifPayload(JSONObject nhifPayload) {

        initialize();

        StringBuilder response = new StringBuilder();

        try {
            URL url = new URL(nhif_url);
            HttpsURLConnection httpConnection = (HttpsURLConnection) url.openConnection();
            httpConnection.setDoOutput(true);
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.setRequestProperty("Accept", "application/json");
            httpConnection.setRequestProperty("Authorization", "Basic " + authKey);

            httpConnection.setSSLSocketFactory(RelaxedSSLContext.getInstance().getSocketFactory());
            httpConnection.setHostnameVerifier(RelaxedSSLContext.localhostValid);

            DataOutputStream wr = new DataOutputStream(httpConnection.getOutputStream());
            wr.write(nhifPayload.toString().getBytes());
            Integer responseCode = httpConnection.getResponseCode();

            BufferedReader bufferedReader;
            // Creates a reader buffer
            if (responseCode > 199 && responseCode < 300) {
                bufferedReader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
            } else {
                bufferedReader = new BufferedReader(new InputStreamReader(httpConnection.getErrorStream()));
            }
            // To receive the response
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line).append("\n");
            }
            bufferedReader.close();

            logging.applicationLog(Utilities.logPreString() + "NHIF JSONResponse:- " + response.toString(), "", "5");
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException e) {

            logging.applicationLog(Utilities.logPreString() + "JSONException:- " + e.getMessage(), "", "5");

        }

        return response.toString();
    }

    public String postUhcPayload(JSONObject uhcPayload, String _URI, int request_id) {

        initialize();

        StringBuilder response = new StringBuilder();
        
        
        try {
            URL url = new URL(nhif_uhc_url);
            HttpsURLConnection httpConnection = (HttpsURLConnection) url.openConnection();
            httpConnection.setDoOutput(true);
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.setRequestProperty("Accept", "application/json");
            httpConnection.setRequestProperty("Authorization", "Basic " + authKey);

            httpConnection.setSSLSocketFactory(RelaxedSSLContext.getInstance().getSocketFactory());
            httpConnection.setHostnameVerifier(RelaxedSSLContext.localhostValid);

            DataOutputStream wr = new DataOutputStream(httpConnection.getOutputStream());
            wr.write(uhcPayload.toString().getBytes());
            Integer responseCode = httpConnection.getResponseCode();

            BufferedReader bufferedReader;
            // Creates a reader buffer
            if (responseCode > 199 && responseCode < 300) {
                bufferedReader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
            } else {
                bufferedReader = new BufferedReader(new InputStreamReader(httpConnection.getErrorStream()));
            }
            // To receive the response
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line).append("\n");
            }
            bufferedReader.close();

            logging.applicationLog(Utilities.logPreString() + "UHC JSONResponse:- " + response.toString(), "", "2");
            
            processUhcResponse(response.toString(),_URI,request_id);
            
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException e) {

            logging.applicationLog(Utilities.logPreString() + "JSONException:- " + e.getMessage(), "", "5");

        }

        return response.toString();
    }

    public int pingNhifEndpoint() throws KeyManagementException, NoSuchAlgorithmException {

        initialize();

        int responseCode = 0;
        try {
            URL ping_url = new URL(nhif_ping_url);

            HttpsURLConnection httpConnection = (HttpsURLConnection) ping_url.openConnection();
            httpConnection.setDoOutput(true);
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty("Authorization", "Basic " + authKey);

            httpConnection.setSSLSocketFactory(RelaxedSSLContext.getInstance().getSocketFactory());
            httpConnection.setHostnameVerifier(RelaxedSSLContext.localhostValid);

            responseCode = httpConnection.getResponseCode();

            logging.applicationLog(Utilities.logPreString() + "NHIF API::- " + nhif_ping_url + " returned " + responseCode + " - " + httpConnection.getResponseMessage() + "\n\n", "", "2");

        } catch (IOException exception) {
            //return false;
            exception.printStackTrace();
            logging.applicationLog(Utilities.logPreString() + "NHIF PING API EXCEPTION ::- " + exception.getMessage() + "\n\n", "", "3");

        }
        return responseCode;
    }
    
    public boolean checkMemberinPhase1(String member_national_id){
        boolean member_exists = false;
        initialize();
        
        
        database = new Database(logging);
        
       String member_count = database.searchMemberDb(member_national_id);
        
       if(!"0".equals(member_count)){
           member_exists = true;
       }
        
        return member_exists;
    }

    public String getMemberById(int request_id, String member_national_id, String _URI) {

        initialize();
        
        //log request to all requests table
        
        database = new Database(logging);
        database.saveToAllRequests(request_id,_URI, member_national_id,"");

        StringBuffer response = new StringBuffer();
        
        String member_number = "0";

        try {
            URL url = new URL(nhif_url_member_id + member_national_id);
            HttpsURLConnection httpConnection = (HttpsURLConnection) url.openConnection();
            httpConnection.setDoOutput(true);
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.setRequestProperty("Accept", "application/json");
            httpConnection.setRequestProperty("Authorization", "Basic " + authKey);

            httpConnection.setSSLSocketFactory(RelaxedSSLContext.getInstance().getSocketFactory());
            httpConnection.setHostnameVerifier(RelaxedSSLContext.localhostValid);

            Integer responseCode = httpConnection.getResponseCode();
            BufferedReader bufferedReader;

            if (responseCode > 199 && responseCode < 300) {
                bufferedReader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
            } else {
                bufferedReader = new BufferedReader(new InputStreamReader(httpConnection.getErrorStream()));
            }

            String inputLine;
            response = new StringBuffer();

            while ((inputLine = bufferedReader.readLine()) != null) {
                response.append(inputLine);
            }
            bufferedReader.close();

            //if member exists, save details to DB
            
            logging.applicationLog(Utilities.logPreString() + "NHIF getMemberById JSONResponse:- " + response.toString(), "", "2");
            
            
            member_number = processMemberResponse(response.toString(), member_national_id, _URI);

        } catch (IOException | KeyManagementException | NoSuchAlgorithmException e) {

            logging.applicationLog(Utilities.logPreString() + "JSONException:- " + e.getMessage(), "", "5");

        }

        return member_number;
    }

    public String processMemberResponse(String response, String member_national_id, String _URI) {

        JSONObject jsonResponseRaw = new JSONObject(response);
        JSONObject jsonResponse;

        String nhif_response_json;
        String nhif_response_string;
        String nhif_response_code;
        String member_number = "0";

        database = new Database(logging);

        if (jsonResponseRaw.has("errors")) {

            jsonResponse = new JSONObject(response.replace("[", "").replace("]", ""));

            JSONObject jsonChildObject = (JSONObject) jsonResponse.get("errors");

            nhif_response_string = jsonChildObject.get("detail").toString();
            nhif_response_json = jsonChildObject.toString();
            nhif_response_code = jsonChildObject.get("status").toString();

            //We are not saving this information into the Database, need to figure out how to store it
        } else {

            JSONObject dataObject = (JSONObject) jsonResponseRaw.get("data");
            JSONObject attributesJson = (JSONObject) dataObject.get("attributes");
            //Save attributesJson to DB
            nhif_response_json = attributesJson.toString();
            nhif_response_string = "Success";
            nhif_response_code = "200";
            member_number = attributesJson.get("mem_no").toString();

            database.saveNhifFullRequest(_URI, "", member_national_id, member_number, "NHIF_REGISTRATION", nhif_response_json, nhif_response_code, "");

        }
        return member_number;

    }
    
    public void processUhcResponse(String uhcResponse, String _URI, int request_id){
        
        JSONObject uhc_jsonResponse = new JSONObject(uhcResponse);

                        String uhc_response_string;
                        String uhc_response_json;
                        String uhc_response_code;
                        String uhc_number;

                        if (uhc_jsonResponse.has("errors")) {

                            JSONObject errjsonResponse = new JSONObject(uhcResponse.replace("[", "").replace("]", ""));

                            JSONObject jsonChildObject = (JSONObject) errjsonResponse.get("errors");

                            uhc_response_string = jsonChildObject.get("detail").toString();
                            uhc_response_json = jsonChildObject.toString();
                            uhc_response_code = jsonChildObject.get("status").toString();
                            uhc_number = "";

                        } else {

                            JSONObject dataObject = (JSONObject) uhc_jsonResponse.get("data");
                            JSONObject attributesJson = (JSONObject) dataObject.get("attributes");

                            uhc_response_json = dataObject.toString();
                            uhc_response_string = "Success";
                            uhc_response_code = "200";
                            uhc_number = attributesJson.get("mem_no").toString();
                        }

                        //Save UHC Response
                        database.updateUhcRequest(_URI, uhc_response_json, uhc_number,request_id);
    }

}
