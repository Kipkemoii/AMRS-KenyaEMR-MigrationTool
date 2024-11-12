package ampath.co.ke.amrs_kenyaemr.tasks;

import ampath.co.ke.amrs_kenyaemr.models.AMRSIdentifiers;
import ampath.co.ke.amrs_kenyaemr.models.AMRSPatients;
import ampath.co.ke.amrs_kenyaemr.models.AMRSUsers;
import ampath.co.ke.amrs_kenyaemr.service.AMRSIdentifiersService;
import ampath.co.ke.amrs_kenyaemr.service.AMRSPatientServices;
import ampath.co.ke.amrs_kenyaemr.service.AMRSUserServices;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

public class RegisterOpenMRSPayload {
    public static void users(AMRSUsers amrsUsers, AMRSUserServices amrsUserServices,String url,String auth ) throws JSONException, ParseException, IOException {
        //OpenMRS Payload
        //Addresses
        JSONArray jsonAddressesArray = new JSONArray();
        JSONObject jsonAddresses = new JSONObject();
        jsonAddresses.put("address1", amrsUsers.getAddress1());
        jsonAddresses.put("address2", amrsUsers.getAddress4());
        jsonAddresses.put("address3", amrsUsers.getAddress4());
        jsonAddresses.put("address4", amrsUsers.getAddress4());
        jsonAddressesArray.put(jsonAddresses);
        //person
        JSONObject jsonPerson = new JSONObject();
        String startDateString = amrsUsers.getBirthdate();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
        jsonPerson.put("gender", amrsUsers.getGender());
        jsonPerson.put("birthdate", amrsUsers.getBirthdate());// sdf2.format(sdf.parse(startDateString)));
        String birthdateEstimatedc = amrsUsers.getBirthdate_estimate();
        String birthdateEstimated = "false";
        if (birthdateEstimatedc.equals("0")) {
            birthdateEstimated = "false";
        } else {
            birthdateEstimated = "true";
        }
        jsonPerson.put("birthdateEstimated", birthdateEstimated);
        String dead = amrsUsers.getDead();
        String deadcheck = "false";
        if (dead.equals("0")) {
            deadcheck = "false";
        } else {
            deadcheck = "true";
        }
        jsonPerson.put("dead", deadcheck);
        //Names
        JSONArray namearray = new JSONArray();
        JSONObject jsonNames = new JSONObject();
        jsonNames.put("givenName", amrsUsers.getGiven_name());
        jsonNames.put("familyName", amrsUsers.getFamily_name());
        jsonNames.put("middleName", amrsUsers.getMiddle_name());
        namearray.put(jsonNames);
        //Roles
        JSONArray rolesarray = new JSONArray();
        rolesarray.put("b1ad7622-9548-4c0e-a137-17ea4a2d892a");

        jsonPerson.put("names", namearray);
        jsonPerson.put("addresses", jsonAddressesArray);
        //user
        JSONObject jsonUser = new JSONObject();
        jsonUser.put("username", amrsUsers.getUsername());
        jsonUser.put("password", "Admin123");
        jsonUser.put("person", jsonPerson);
        jsonUser.put("roles", rolesarray);

        // Basic Authentication
        //String auth = "admin" + ":" + "Admin123";
        //String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
        //String url = "http://192.168.100.48:8080/openmrs/ws/rest/v1/";


        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, jsonUser.toString());
        String bodyString =body.toString();
        Request request = new Request.Builder()
                .url(url + "user")
                .method("POST", body)
                .addHeader("Authorization", "Basic " + auth)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

       // System.out.println("Response ndo hii " + jsonUser.toString());
        System.out.println("Response ndo hii " + response);
        System.out.println("Response Payload " + jsonUser.toString());
        amrsUsers.setMigrated(1);
        amrsUsers.setResponse_code(response.code());
        amrsUserServices.save(amrsUsers);

    }

    public static void patient(AMRSPatients amrsPatients, AMRSPatientServices amrsPatientServices, AMRSIdentifiersService amrsIdentifiersService,String url,String auth) throws JSONException, ParseException, IOException {
        //OpenMRS Payload
        //identifier
        List<AMRSIdentifiers> identifiers = amrsIdentifiersService.getByPatientID(amrsPatients.getPersonId());
        JSONArray Identifierarray = new JSONArray();
        String OpenMRSID = IdentifierGenerator.generateIdentifier(url,auth);

        //Add OpenmrsID
        JSONObject jsonIdentifierOpenMRSID = new JSONObject();
        jsonIdentifierOpenMRSID.put("identifier",OpenMRSID);
        jsonIdentifierOpenMRSID.put("identifierType", "dfacd928-0370-4315-99d7-6ec1c9f7ae76");
        jsonIdentifierOpenMRSID.put("location", "c55535b8-b9f2-4a97-8c6c-4ea9496256df");
        jsonIdentifierOpenMRSID.put("preferred", "true");
        Identifierarray.put(jsonIdentifierOpenMRSID);
        AMRSIdentifiers ai = new AMRSIdentifiers();
        ai.setKenyaemr_uuid("dfacd928-0370-4315-99d7-6ec1c9f7ae76");
        ai.setPatientid(amrsPatients.getPersonId());
        ai.setPreferred("true");
        ai.setIdentifier(OpenMRSID);
        ai.setLocation("c55535b8-b9f2-4a97-8c6c-4ea9496256df");
        amrsIdentifiersService.save(ai);

        for (int x = 0; x < identifiers.size(); x++) {

            String identifyType = Mappers.identifers(identifiers.get(x).getUuid());
            if(identifyType.isEmpty()){

            }else {

                JSONObject jsonIdentifier = new JSONObject();
                jsonIdentifier.put("identifier", identifiers.get(x).getIdentifier());
                jsonIdentifier.put("identifierType", identifyType);
                jsonIdentifier.put("location", "c55535b8-b9f2-4a97-8c6c-4ea9496256df");
                jsonIdentifier.put("preferred","false");// identifiers.get(x).getPreferred());
                Identifierarray.put(jsonIdentifier);
            }
        }
        //Start of Names
        JSONArray namearray = new JSONArray();
        JSONObject jsonNames = new JSONObject();
        jsonNames.put("givenName", amrsPatients.getGiven_name());
        jsonNames.put("familyName", amrsPatients.getFamily_name());
        jsonNames.put("middleName", amrsPatients.getMiddle_name());
        namearray.put(jsonNames);
        // End of dentifier
        JSONArray jsonAddressesArray = new JSONArray();
        JSONObject jsonAddresses = new JSONObject();
        jsonAddresses.put("address1", amrsPatients.getAddress1());
        jsonAddresses.put("address2", amrsPatients.getAddress1());
        jsonAddresses.put("address3", amrsPatients.getAddress4());
        jsonAddresses.put("address4", amrsPatients.getAddress4());
        jsonAddressesArray.put(jsonAddresses);

        //person
        JSONObject jsonPerson = new JSONObject();
        jsonPerson.put("gender", amrsPatients.getGender());
        jsonPerson.put("birthdate", amrsPatients.getBirthdate());// sdf2.format(sdf.parse(startDateString)));
        String birthdateEstimatedc = amrsPatients.getBirthdate_estimated();
        String birthdateEstimated = "false";
        if (birthdateEstimatedc.equals("0")) {
            birthdateEstimated = "false";
        } else {
            birthdateEstimated = "true";
        }
        jsonPerson.put("birthdateEstimated", birthdateEstimated);
        String dead = amrsPatients.getDead();
        String deadcheck = "false";
        if (dead.equals("0")) {
            deadcheck = "false";
        } else {
            deadcheck = "true";
        }
        jsonPerson.put("dead", deadcheck);
        jsonPerson.put("names", namearray);
        jsonPerson.put("addresses", jsonAddressesArray);

        JSONObject patientObject = new JSONObject();
        patientObject.put("identifiers", Identifierarray);
        patientObject.put("person", jsonPerson);

        // Basic Authentication
       // String auth = "admin" + ":" + "Admin123";
       // String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
       // String url = "http://192.168.100.48:8080/openmrs/ws/rest/v1/";

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, patientObject.toString());
        Request request = new Request.Builder()
                .url(url + "patient")
                .method("POST", body)
                .addHeader("Authorization", "Basic " + auth)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        // System.out.println("Response ndo hii " + jsonUser.toString());
        System.out.println("Response ndo hii " + response.request()+" More message "+ response.message());
        amrsPatients.setMigrated(1);
        amrsPatients.setResponse_code(response.code());
        //amrsPatients.setKenyaemrpatientUUID();
        amrsPatientServices.save(amrsPatients);
        System.out.println("identifers ndo hii "+ patientObject.toString());

    }
}
