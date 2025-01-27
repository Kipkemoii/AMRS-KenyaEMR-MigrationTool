package ampath.co.ke.amrs_kenyaemr.tasks.payloads;

import ampath.co.ke.amrs_kenyaemr.methods.AMRSTranslater;
import ampath.co.ke.amrs_kenyaemr.models.*;
import ampath.co.ke.amrs_kenyaemr.service.*;
import ampath.co.ke.amrs_kenyaemr.tasks.Mappers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class CareOpenMRSPayload {
    public static void programs(AMRSProgramService amrsProgramService, AMRSTranslater amrsTranslater,String KenyaEMRlocationUuid, String url, String auth) throws JSONException, IOException {
        //List<AMRSPrograms> amrsProgramsList = amrsProgramService.findByParentLocationUuid(parentUUID);
        List<AMRSPrograms> amrsProgramsList = amrsProgramService.findByResponseCodeIsNull();
        if (!amrsProgramsList.isEmpty()) {
            JSONObject jsonProgram = new JSONObject();
            for (int x = 0; x < amrsProgramsList.size(); x++) {

                String programms = Mappers.programs(String.valueOf(amrsProgramsList.get(x).getProgramUUID()));
                System.out.println("Program UUID is here " + programms + " amrs UUID " + amrsProgramsList.get(x).getProgramUUID());
                int pid = amrsProgramsList.get(x).getProgramID();
                AMRSPrograms ap = amrsProgramsList.get(x);

                if (!programms.isEmpty() || amrsTranslater.KenyaemrPatientUuid(ap.getPatientId()) != null) {

                    jsonProgram.put("patient", amrsTranslater.KenyaemrPatientUuid(ap.getPatientId()));
                    jsonProgram.put("program", ap.getKenyaemrProgramUuid());
                    jsonProgram.put("location",KenyaEMRlocationUuid);
                    jsonProgram.put("dateEnrolled", ap.getDateEnrolled());
                    //jsonProgram.put("location", ap.getDateEnrolled());
                    if (pid == 1 || pid == 3 || pid == 9 || pid == 20) {

                    } else {
                        jsonProgram.put("dateCompleted", ap.getDateCompleted());
                    }
                    System.out.println("Payload for Programs is here " + jsonProgram.toString());
                    OkHttpClient client = new OkHttpClient();
                    MediaType mediaType = MediaType.parse("application/json");
                    okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonProgram.toString());
                    //RequestBody body = RequestBody.create(mediaType, jsonEncounter.toString());
                    Request request = new Request.Builder()
                            .url(url + "programenrollment")
                            .method("POST", body)
                            .addHeader("Authorization", "Basic " + auth)
                            .addHeader("Content-Type", "application/json")
                            .build();
                    Response response = client.newCall(request).execute();
                    String responseBody = response.body().string(); // Get the response as a string
                    System.out.println("Response ndo hii " + responseBody + " More message " + response.message());

                    // String resBody = response.request().toString();
                    int rescode = response.code();
                    ap.setResponseCode(String.valueOf(rescode));
                   /* if(rescode==201) {
                        ap.setResponseCode(String.valueOf(rescode));
                    }else{
                        ap.setResponseCode(String.valueOf(rescode));
                    }*/
                    System.out.println("Imefika Hapa na data " + rescode);
                }
                amrsProgramService.save(ap);
            }

        }
    }

    public static void triage( String KenyaEMRLocationUuid,AMRSTranslater amrsTranslater,AMRSTriageService amrsTriageService, AMRSPatientServices amrsPatientServices, AMRSEncounterService amrsEncounterService,AMRSVisitService amrsVisitService, String url, String auth) throws JSONException, IOException {

        List<AMRSTriage> amrsTriages = amrsTriageService.findByResponseCodeIsNull();
        if (!amrsTriages.isEmpty()) {
            // Use a Set to store unique encounter IDs
            Set<String> visistIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();
            // Loop through the list
            for (AMRSTriage triage : amrsTriages) {
                if (triage.getResponseCode() == null) {
                    String visitId = triage.getVisitId();
                    // Add to the result list only if it hasn't been added already
                    if (visistIdSet.add(visitId)) {
                        distinctVisitIds.add(visitId);
                    }
                }
            }

            for (String visitId : distinctVisitIds) {
                System.out.println("VisitId ID for Vitals " + visitId);
                List<AMRSTriage> amrsTriageEncounters = amrsTriageService.findByVisitId(visitId);
                //AMRSTriage at = amrsTriageEncounters.get(0);
                JSONArray jsonObservations = new JSONArray();
                String patientuuid = "";
                for (int x = 0; x < amrsTriageEncounters.size(); x++) {

                    JSONObject jsonObservation = new JSONObject();
                    String value = amrsTriageEncounters.get(x).getValue();
                    jsonObservation.put("person", amrsTranslater.KenyaemrPatientUuid(amrsTriageEncounters.get(x).getPatientId()));///String.valueOf(conceptsetId));
                    jsonObservation.put("concept", amrsTriageEncounters.get(x).getKenyaemConceptId());///String.valueOf(conceptsetId));
                    jsonObservation.put("obsDatetime", amrsTriageEncounters.get(x).getObsDateTime());///String.valueOf(conceptsetId));
                    jsonObservation.put("value", amrsTriageEncounters.get(x).getValue());
                    jsonObservations.put(jsonObservation);
                    patientuuid =  amrsTranslater.KenyaemrPatientUuid(amrsTriageEncounters.get(x).getPatientId());
                }


                if (amrsTriageEncounters.get(0).getVisitId()==null) {

                } else {
                    List<AMRSVisits> amrsVisits = amrsVisitService.findByVisitID(amrsTriageEncounters.get(0).getVisitId());

                    if (!amrsVisits.isEmpty()) {
                        JSONObject jsonEncounter = new JSONObject();
                        jsonEncounter.put("form", "37f6bd8d-586a-4169-95fa-5781f987fe62");
                        jsonEncounter.put("patient", patientuuid);
                        jsonEncounter.put("encounterDatetime", amrsTriageEncounters.get(0).getEncounterDateTime());
                        jsonEncounter.put("encounterType", "d1059fb9-a079-4feb-a749-eedd709ae542");
                        jsonEncounter.put("location", KenyaEMRLocationUuid);
                        jsonEncounter.put("visit", amrsVisits.get(0).getKenyaemrVisitUuid());
                        jsonEncounter.put("obs", jsonObservations);
                        System.out.println("Payload for is here " + jsonEncounter.toString());
                        System.out.println("URL is here " + url + "encounter/" + amrsTriageEncounters.get(0).getKenyaemrEncounterUuid());

                        System.out.println("Payload for is here " + jsonEncounter.toString());

                        OkHttpClient client = new OkHttpClient();
                        MediaType mediaType = MediaType.parse("application/json");
                        okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());
                        //RequestBody body = RequestBody.create(mediaType, jsonEncounter.toString());
                        Request request = new Request.Builder()
                                // .url(url + "encounter/" + amrsEncounters.get(0).getKenyaemrEncounterUuid())
                                .url(url + "encounter")
                                .method("POST", body)
                                .addHeader("Authorization", "Basic " + auth)
                                .addHeader("Content-Type", "application/json")
                                .build();

                        Response response = client.newCall(request).execute();
                        String responseBody = response.body().string(); // Get the response as a string
                        System.out.println("Response ndo hii " + responseBody + " More message " + response.message());

                        String resBody = response.request().toString();
                        int rescode = response.code();
                        System.out.println("Response Code Hapa " + rescode);

                        if (rescode == 201) {
                            for (int x = 0; x < amrsTriageEncounters.size(); x++) {
                                AMRSTriage at = amrsTriageEncounters.get(x);
                                at.setResponseCode(String.valueOf(rescode));
                                at.setResponseCode("201");
                                at.setKenyaemrEncounterUuid(amrsTriageEncounters.get(0).getKenyaemrEncounterUuid());
                                System.out.println("Imefika Hapa na data " + rescode);
                                amrsTriageService.save(at);
                            }
                        }else{
                            for (int x = 0; x < amrsTriageEncounters.size(); x++) {
                                AMRSTriage at = amrsTriageEncounters.get(x);
                                at.setResponseCode(String.valueOf(rescode));
                                at.setResponseCode("400");
                                at.setKenyaemrEncounterUuid(amrsTriageEncounters.get(0).getKenyaemrEncounterUuid());
                                System.out.println("Imefika Hapa na data " + rescode);
                                amrsTriageService.save(at);
                            }
                        }
                    }
                }
            }
        }



           /* JSONArray jsonObservations = new JSONArray();
            for (int x = 0; x < amrsTriages.size(); x++) {
                AMRSTriage at = amrsTriages.get(x);
                List<AMRSPatients> amrsPatients = amrsPatientServices.getByPatientID(at.getPatientId());
                List<AMRSEncounters> amrsEncounters = amrsEncounterService.findByEncounterId(at.getEncounterId());
                System.out.println("Patient ID " + amrsPatients.get(0).getPersonId() +" Encounter Id "+ at.getEncounterId());
                //Height
                //wight,tempdbp,sbp,bmi,pulse,rr,sop,
                JSONObject jsonEncounter = new JSONObject();
                jsonEncounter.put("form",at.getKenyaemrFormUuid());

              /*  JSONObject jsonObservationBMI = new JSONObject();
                jsonObservationBMI.put("person",amrsPatients.get(0).getKenyaemrpatientUUID());
                jsonObservationBMI.put("concept",conceptuuid);///String.valueOf(conceptsetId));
                jsonObservationBMI.put("value", at.getBmi());
                */
               /* JSONObject jsonEncounter = new JSONObject();
                jsonEncounter.put("form",at.getKenyaemrFormUuid());

                JSONObject jsonObservationTEMP = new JSONObject();
                jsonObservationTEMP.put("concept","5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");///String.valueOf(conceptsetId));
                jsonObservationTEMP.put("value", at.getTemperature());
                jsonObservations.put(jsonObservationTEMP);

                JSONObject jsonObservationW = new JSONObject();
               // jsonObservationW.put("person",amrsPatients.get(0).getKenyaemrpatientUUID());
                jsonObservationW.put("concept","5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");///String.valueOf(conceptsetId));
                jsonObservationW.put("value", at.getWeight());
                jsonObservations.put(jsonObservationW);

                JSONObject jsonObservationH = new JSONObject();
                jsonObservationH.put("concept","5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");///String.valueOf(conceptsetId));
                jsonObservationH.put("value", at.getHeight());
                jsonObservations.put(jsonObservationH);
                */

               /* jsonEncounter.put("obs",jsonObservations);


                System.out.println("Payload for is here " + jsonEncounter.toString());
                System.out.println("URL is here " + url + "encounter/"+amrsEncounters.get(0).getKenyaemrEncounterUuid());
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());
                //RequestBody body = RequestBody.create(mediaType, jsonEncounter.toString());
                Request request = new Request.Builder()
                        .url(url + "encounter/"+amrsEncounters.get(0).getKenyaemrEncounterUuid())
                        .method("POST", body)
                        .addHeader("Authorization", "Basic " + auth)
                        .addHeader("Content-Type", "application/json")
                        .build();
                */
            // Response response = client.newCall(request).execute();
            // String responseBody = response.body().string(); // Get the response as a string
            // System.out.println("Response ndo hii " + responseBody + " More message " + response.message());

            // String resBody = response.request().toString();
            //  int rescode = response.code();
            // at.setResponseCode(String.valueOf(rescode));
            //  System.out.println("Imefika Hapa na data " + rescode);

            // amrsTriageService.save(at);



           /*     if(amrsEncounters.size()>0) {
                    System.out.println("Encounter ID " + amrsEncounters.get(0).getEncounterId() + " Visit ID " + at.getVisitId());
                }
            }
            // List<AMRSPrograms> amrsProgramsList = amrsTriageService.findByParentLocationUuid(parentUUID);

     /* JSONArray jsonObservations = new JSONArray();
      JSONObject jsonObservation = new JSONObject();
      AMRSTriage amrsTriage = amrsTriageService.
      // String puuid="a32d8e5e-fb75-419a-88aa-c2d5f37dd244";
      String pruuid="901218e9-dfc7-4afb-94cb-b32e551e4f76";
      String conceptuuid="1246AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
      jsonObservation.put("person",puuid);//"60168b73-60f1-4044-9dc6-84fdcbc1962c");
      jsonObservation.put("concept",conceptuuid);///String.valueOf(conceptsetId));
      jsonObservation.put("value", value);

      */
        }


    public static boolean isDecimal(String value) {
        try {
            Double.parseDouble(value); // Convert to double
            return value.contains("."); // Check for decimal point
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void amrsRegimenSwitch(AMRSRegimenSwitchService amrsRegimenSwitchService, AMRSTranslater amrsTranslater, String KenyaEMRlocationUuid, String auth, String url) throws JSONException, IOException {
        List<AMRSRegimenSwitch> amrsRegimenSwitchList = amrsRegimenSwitchService.findByResponseCodeIsNull();

        if(!amrsRegimenSwitchList.isEmpty()) {
            Set<String> regimenSwitchIdSet = new HashSet<>();
            List<String> distinctRegimenSwitchIds= new ArrayList<>();

            for( AMRSRegimenSwitch regimenSwitches: amrsRegimenSwitchList) {
                if(regimenSwitches.getResponseCode() == null) {
                    String encounterId = regimenSwitches.getPatientId();
                    if (regimenSwitchIdSet.add(encounterId)) {
                        distinctRegimenSwitchIds.add(encounterId);
                    }
                }
            }

            for(String encounterId: distinctRegimenSwitchIds) {
                System.out.println("Patient ID for Switches " + encounterId);

                List<AMRSRegimenSwitch> regimenSwitchList = amrsRegimenSwitchService.findByPatientId(encounterId);

                for( int x=0; x < regimenSwitchList.size(); x++ ) {

               String patient = amrsTranslater.KenyaemrPatientUuid(encounterId);
               String visitId = amrsTranslater.kenyaemrVisitUuid(regimenSwitchList.get(x).getVisitId());
                    //List<AMRSPatients> amrsPatients = patient;
                    if (!Objects.equals(visitId, "")) {

                        //PLAN 1255AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
                        //START 1256AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
                        //CHANGE 1259AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
                        //STOP DATE 1191AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
                        //Reason for stoping 1252AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA

                        JSONArray jsonObservations = new JSONArray();
                        JSONObject jsonObservationPType = new JSONObject();
                        jsonObservationPType.put("person", patient);
                        jsonObservationPType.put("concept", "1255AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        jsonObservationPType.put("value", "1256AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        jsonObservationPType.put("location", KenyaEMRlocationUuid);
                        jsonObservationPType.put("obsDatetime", regimenSwitchList.get(x).getEncounterDatetime());
                        jsonObservations.put(jsonObservationPType);


                        JSONObject jsonObservation = new JSONObject();
                        jsonObservation.put("person", patient);
                        jsonObservation.put("concept", regimenSwitchList.get(x).getKenyaemrConceptUuid());
                        jsonObservationPType.put("location", KenyaEMRlocationUuid);
                        jsonObservation.put("value", regimenSwitchList.get(x).getKenyaemrValue());
                        jsonObservationPType.put("obsDatetime", regimenSwitchList.get(x).getEncounterDatetime());
                        jsonObservations.put(jsonObservation);


                        JSONObject jsonRegimenSwitchEncouter = new JSONObject();
                        jsonRegimenSwitchEncouter.put("form", "da687480-e197-11e8-9f32-f2801f1b9fd1");
                        jsonRegimenSwitchEncouter.put("encounterType", "7dffc392-13e7-11e9-ab14-d663bd873d93");
                        jsonRegimenSwitchEncouter.put("location", KenyaEMRlocationUuid);
                        jsonRegimenSwitchEncouter.put("patient", patient);
                        jsonRegimenSwitchEncouter.put("encounterDatetime", regimenSwitchList.get(x).getEncounterDatetime());
                        jsonRegimenSwitchEncouter.put("obs", jsonObservations);
                        jsonRegimenSwitchEncouter.put("visit", visitId);

                        System.out.println("Payload for is here " + jsonRegimenSwitchEncouter.toString());
                        // System.out.println("URL is here " + url + "encpunter/" + regimenSwitchList.get(0).getKenyaemrEncounterUuid());

                        OkHttpClient client = new OkHttpClient();
                        MediaType mediaType = MediaType.parse("application/json");
                        okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonRegimenSwitchEncouter.toString());

                        Request request = new Request.Builder()
                                .url(url + "encounter")
                                .method("POST", body)
                                .addHeader("Authorization", "Basic " + auth)
                                .addHeader("Content-Type", "application/json")
                                .build();
                        Response response = client.newCall(request).execute();
                        String responseBody = response.body().string();
                        System.out.println("Response ndo hii " + responseBody + "More message" + response.message());

                        int resCode = response.code();
                        if (resCode == 200) {
                            for (int y = 0; y < regimenSwitchList.size(); y++) {
                                AMRSRegimenSwitch rs = regimenSwitchList.get(y);
                                rs.setResponseCode("201");
                                System.out.println("Imefika Hapa na data " + resCode);
                                amrsRegimenSwitchService.save(rs);
                            }
                        }else{
                            for (int y = 0; y < regimenSwitchList.size(); y++) {
                                AMRSRegimenSwitch rs = regimenSwitchList.get(y);
                                rs.setResponseCode("400");
                                System.out.println("Imefika Hapa na data " + resCode);
                                amrsRegimenSwitchService.save(rs);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void patientStatus(AMRSPatientStatusService amrsPatientStatusService, String auth, String url) throws JSONException, IOException {
        List<AMRSPatientStatus> amrsPatientStatusList = amrsPatientStatusService.findByResponseCodeIsNull();
        if (amrsPatientStatusList.size() > 0) {

            // Use a Set to store unique encounter IDs
            Set<String> patientIdSet = new HashSet<>();
            List<String> distinctpatientIds = new ArrayList<>();

            // Loop through the list
            for (AMRSPatientStatus patients : amrsPatientStatusList) {
                if (patients.getResponseCode() == null) {
                    String patientId = patients.getPersonId();
                    // Add to the result list only if it hasn't been added already
                    if (patientIdSet.add(patientId)) {
                        distinctpatientIds.add(patientId);
                    }
                }
            }

            for (String patientId : distinctpatientIds) {
                System.out.println("Encounter ID for Vitals " + patientId);
                List<AMRSPatientStatus> patientStatusList = amrsPatientStatusService.findByPersonId(patientId);
                JSONArray jsonObservations = new JSONArray();
                String kenyaemrPatientUuid="";
                for (int x = 0; x < patientStatusList.size(); x++) {
                    JSONObject jsonObservation = new JSONObject();
                    jsonObservation.put("person", patientStatusList.get(x).getKenyaPatientUuid());///String.valueOf(conceptsetId));
                    jsonObservation.put("concept", patientStatusList.get(x).getKenyaEmrConceptUuid());///String.valueOf(conceptsetId));
                    jsonObservation.put("value", patientStatusList.get(x).getKenyaEmrValueUuid());
                    jsonObservation.put("obsDatetime", patientStatusList.get(x).getObsDateTime());
                    jsonObservations.put(jsonObservation);
                    kenyaemrPatientUuid= patientStatusList.get(x).getKenyaPatientUuid();
                }

                JSONObject jsonEncounter = new JSONObject();
                jsonEncounter.put("form", "add7abdc-59d1-11e8-9c2d-fa7ae01bbebc");
                jsonEncounter.put("encounterType", "de1f9d67-b73e-4e1b-90d0-036166fc6995");
                jsonEncounter.put("obs", jsonObservations);
                jsonEncounter.put("patient", kenyaemrPatientUuid);


                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());
                //RequestBody body = RequestBody.create(mediaType, jsonEncounter.toString());
                Request request = new Request.Builder()
                        .url(url + "encounter")
                        .method("POST", body)
                        .addHeader("Authorization", "Basic " + auth)
                        .addHeader("Content-Type", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string(); // Get the response as a string
                System.out.println("Response ndo hii " + responseBody + " More message " + response.message());

                String resBody = response.request().toString();
                int rescode = response.code();
                if (rescode == 201) {
                    for (int x = 0; x < patientStatusList.size(); x++) {
                        AMRSPatientStatus at = patientStatusList.get(x);
                        at.setResponseCode(String.valueOf(rescode));
                        at.setResponseCode("201");
                        System.out.println("Imefika Hapa na data " + rescode);
                        amrsPatientStatusService.save(at);
                    }
                }

            }


        }
    }
    public void ProgramChecker(String patientUUID) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url("/openmrs/ws/rest/v1/programenrollment?patient=:uuid")
                .method("GET", null)
                .addHeader("Authorization", "Basic YWRtaW46QWRtaW4xMjM=")
                .addHeader("Cookie", "JSESSIONID=DF385B2E6E39E0BB49BB7E079BF31C44")
                .build();
        Response response = client.newCall(request).execute();

    }
    public static void DeleteProgram(String programuuid,String url,String auth){
        OkHttpClient clientPrograms = new OkHttpClient();
        MediaType mediaTypePrograms = MediaType.parse("application/json");
        Request requestPrograms = new Request.Builder()
                .url(url + "programenrollment/"+programuuid)
                .method("DELETE" ,null)
                .addHeader("Authorization", "Basic " + auth)
                .addHeader("Content-Type", "application/json")
                .build();
        try (Response responsePrograms = clientPrograms.newCall(requestPrograms).execute()) {
            String responseBodyPrograms = responsePrograms.body().string();
            int responseCodePrograms = responsePrograms.code();
            System.out.println("Response  Status Code: " + responseCodePrograms);
        } catch (Exception e) {
            System.err.println("Error processing Patient ID: " + e.getMessage());
        }
    }
    public static void hivEnrollment(
            AMRSHIVEnrollmentService amrshivEnrollmentService,
            AMRSTranslater amrsTranslater,
            String KenyaEMRlocationUuid,
            String url,
            String auth
    ) throws JSONException, IOException {

        List<AMRSHIVEnrollment> amrshivEnrollments = amrshivEnrollmentService.findByResponseCodeIsNull();

        if (!amrshivEnrollments.isEmpty()) {
            // Use a Set to store unique patient IDs
            Set<String> patientIdSet = new HashSet<>();
            List<String> distinctPatientIds = new ArrayList<>();

            // Collect unique patient IDs
            for (AMRSHIVEnrollment enrollment : amrshivEnrollments) {
                if (enrollment.getResponseCode() == null && patientIdSet.add(enrollment.getPatientId())) {
                    distinctPatientIds.add(enrollment.getPatientId());
                }
            }

            System.out .println("list of distinct clients "+ distinctPatientIds.toString() );

            for (String patientId : distinctPatientIds) {
                System.out.println("Processing Patient ID: " + patientId);

                String kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(patientId);

                List<AMRSHIVEnrollment> amrshivEnrollmentList = amrshivEnrollmentService.findByPatientId(patientId);
                String  visituuid =amrsTranslater.kenyaemrVisitUuid(amrshivEnrollmentList.get(0).getVisitId());
                String obsDateTime = "";
                // Prepare JSON observations

                JSONArray jsonObservations = new JSONArray();
                //Patient Type
                JSONObject jsonObservationPType = new JSONObject();
                jsonObservationPType.put("person", kenyaemrPatientUuid);
                jsonObservationPType.put("concept", "423c034e-14ac-4243-ae75-80d1daddce55");
                jsonObservationPType.put("value", "164144AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                jsonObservationPType.put("obsDatetime", amrshivEnrollmentList.get(0).getObsDateTime());
                jsonObservations.put(jsonObservationPType);

                 obsDateTime = amrshivEnrollmentList.get(0).getObsDateTime();

                int checkEntry=0;
                for (AMRSHIVEnrollment enrollment : amrshivEnrollmentList) {

                    if(!Objects.equals(enrollment.getConceptId(), "2155")) {
                        JSONObject jsonObservation = new JSONObject();
                        jsonObservation.put("person", kenyaemrPatientUuid);
                        jsonObservation.put("concept", enrollment.getKenyaemrConceptUuid());
                        jsonObservation.put("value", enrollment.getKenyaemrValue());
                        jsonObservation.put("obsDatetime", obsDateTime);
                      //  obsDateTime = enrollment.getObsDateTime();
                        if (!Objects.equals(enrollment.getKenyaemrConceptUuid(), "") && !Objects.equals(enrollment.getKenyaemrValue(), "")) {
                            jsonObservations.put(jsonObservation);
                        }
                        if(Objects.equals(enrollment.getConceptId(), "6749")){
                            checkEntry=1;
                        }
                    }
                }
                if(checkEntry==0){
                    //Entry Point
                    JSONObject jsonObservationEntry = new JSONObject();
                    jsonObservationEntry.put("person", kenyaemrPatientUuid);
                    jsonObservationEntry.put("concept", "160540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                    jsonObservationEntry.put("value", "162050AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                    jsonObservationEntry.put("obsDatetime", amrshivEnrollmentList.get(0).getObsDateTime());
                    jsonObservations.put(jsonObservationEntry);
                }

                // Prepare JSON encounter
                JSONObject jsonEncounter = new JSONObject();
                jsonEncounter.put("form", "e4b506c1-7379-42b6-a374-284469cba8da");
                jsonEncounter.put("encounterType", "de78a6be-bfc5-4634-adc3-5f1a280455cc");
                jsonEncounter.put("obs", jsonObservations);
                jsonEncounter.put("patient", kenyaemrPatientUuid);
                jsonEncounter.put("visit", visituuid);
                jsonEncounter.put("encounterDatetime", obsDateTime);
                jsonEncounter.put("location", KenyaEMRlocationUuid );

                //Check if the Client is enrolled to HIV If not Enroll 1st
                //List Programs Lists
                OkHttpClient clientPrograms = new OkHttpClient();
                MediaType mediaTypePrograms = MediaType.parse("application/json");
               // okhttp3.RequestBody bodyy = okhttp3.RequestBody.create(mediaTypePrograms, jsonProgram.toString());

                Request requestPrograms = new Request.Builder()
                        .url(url + "programenrollment?patient="+kenyaemrPatientUuid)
                        //.method("GET")
                        .addHeader("Authorization", "Basic " + auth)
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response responsePrograms = clientPrograms.newCall(requestPrograms).execute()) {
                    String responseBodyPrograms = responsePrograms.body().string();
                    int responseCodePrograms = responsePrograms.code();
                    JSONObject jsonObjectPrograms = new JSONObject(responseBodyPrograms);
                    // String encounterUUID = jsonObjectt.getString("uuid");
                    System.out.println("Response: " + responseBodyPrograms + " | Status Code: " + responseCodePrograms);
                    JSONArray results = jsonObjectPrograms.getJSONArray("results");

                    // Extract and display UUIDs
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        String uuid = result.getString("uuid");
                        String Programs = result.getString("display");
                        if(Programs.equals("HIV")){
                            DeleteProgram(uuid,url,auth);
                        }
                        System.out.println("UUID: " + uuid+ " Name "+ Programs);
                    }

                } catch (Exception e) {
                    System.err.println("Error processing Patient ID: " + patientId + " | " + e.getMessage());
                }

                //end of Programs Lists

//Create New
                JSONObject jsonProgram = new JSONObject();
                jsonProgram.put("patient", kenyaemrPatientUuid);
                jsonProgram.put("program", "dfdc6d40-2f2f-463d-ba90-cc97350441a8");
                jsonProgram.put("location",KenyaEMRlocationUuid);
                jsonProgram.put("dateEnrolled", obsDateTime);

                OkHttpClient clientt = new OkHttpClient();
                MediaType mediaTypee = MediaType.parse("application/json");
                okhttp3.RequestBody bodyy = okhttp3.RequestBody.create(mediaTypee, jsonProgram.toString());

                Request requestt = new Request.Builder()
                        .url(url + "programenrollment")
                        .method("POST", bodyy)
                        .addHeader("Authorization", "Basic " + auth)
                        .addHeader("Content-Type", "application/json")
                        .build();

                System.out.println("Payload is Here for Program Enrollments "+ jsonProgram.toString());
                try (Response responsee = clientt.newCall(requestt).execute()) {
                    String responseBodyy = responsee.body().string();
                    int responseCodee = responsee.code();
                    JSONObject jsonObjectt = new JSONObject(responseBodyy);
                   // String encounterUUID = jsonObjectt.getString("uuid");
                    System.out.println("Response: " + responseBodyy + " | Status Code: " + responseCodee);
                } catch (Exception e) {
                    System.err.println("Error processing Patient ID: " + patientId + " | " + e.getMessage());
                }
                //end of Create New

                // End of Check if the Client is enrolled to HIV If not Enroll 1st

                // Send API request
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());

                Request request = new Request.Builder()
                        .url(url + "encounter")
                        .method("POST", body)
                        .addHeader("Authorization", "Basic " + auth)
                        .addHeader("Content-Type", "application/json")
                        .build();

                System.out.println("Payload is Here "+ jsonEncounter.toString() );

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    int responseCode = response.code();
                    System.out.println("Response: " + responseBody + " | Status Code: " + responseCode);
                    // Update response code for successful submissions
                    if (responseCode == 201) {
                        for (AMRSHIVEnrollment enrollment : amrshivEnrollmentList) {
                            enrollment.setResponseCode("201");
                            amrshivEnrollmentService.save(enrollment);
                        }
                    } else {
                        for (AMRSHIVEnrollment enrollment : amrshivEnrollmentList) {
                            enrollment.setResponseCode("400");
                            amrshivEnrollmentService.save(enrollment);
                        }
                        System.err.println("Failed to process Patient ID: " + patientId + " | Status Code: " + responseCode);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing Patient ID: " + patientId + " | " + e.getMessage());
                }

            }

        } else {
            System.out.println("No enrollments found with null response codes.");
        }
    }


    public static void ordersResults(AMRSOrdersResultsService amrsOrdersResultsService, String parentUUID, String locations, String auth, String url)throws
            JSONException, IOException{
    }

    public static void artRefill(AMRSArtRefillService amrsArtRefillService,  AMRSTranslater amrsTranslater, String KenyaEMRlocationUuid, String url, String auth) throws JSONException, IOException {

        List<AMRSArtRefill> artRefills = amrsArtRefillService.findByResponseCodeIsNull();

        if (!artRefills.isEmpty()) {
            // Use a Set to store unique patient IDs
            Set<String> visitIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

            // Collect unique patient IDs
            for (AMRSArtRefill artRefill : artRefills) {
                if (artRefill.getResponseCode() == null && visitIdSet.add(artRefill.getVisitId())) {
                    distinctVisitIds.add(artRefill.getVisitId());
                }
            }

            System.out.println("list of distinct clients " + distinctVisitIds);

            for (String visitId : distinctVisitIds) {
                System.out.println("Processing Visit ID: " + visitId);

                //Fetch all obs per visitID

                List<AMRSArtRefill> artRefillList = amrsArtRefillService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                String kenyaemrPatientUuid="";
                String kenyaemrvisitUuid="";
                String obsDateTime ="";
                for (int x =0 ;x<artRefillList.size();x++){

                    System.out.println("Loop Visits "+ visitId + " index " +x);
                    kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(artRefillList.get(x).getPatientId());
                    kenyaemrvisitUuid =amrsTranslater.kenyaemrVisitUuid(visitId);
                    obsDateTime = artRefillList.get(x).getObsDateTime();
                    JSONObject jsonObservationD = new JSONObject();
                    jsonObservationD.put("person", kenyaemrPatientUuid);
                    jsonObservationD.put("concept", artRefillList.get(x).getKenyaEmrConceptUuid());
                    jsonObservationD.put("value", artRefillList.get(x).getKenyaEmrValue());
                    jsonObservationD.put("location", KenyaEMRlocationUuid);
                    jsonObservationD.put("obsDatetime", artRefillList.get(x).getObsDateTime());
                    if(!Objects.equals(artRefillList.get(x).getKenyaEmrConceptUuid(), "") && !Objects.equals(artRefillList.get(x).getKenyaEmrValue(), "") ) {
                        jsonObservations.put(jsonObservationD);
                    }
                }

                JSONObject jsonEncounter = new JSONObject();
                jsonEncounter.put("form", "83fb6ab2-faec-4d87-a714-93e77a28a201");
                jsonEncounter.put("patient", kenyaemrPatientUuid);
                jsonEncounter.put("obs", jsonObservations);
                jsonEncounter.put("visit", kenyaemrvisitUuid);
                jsonEncounter.put("encounterDatetime", obsDateTime);
                jsonEncounter.put("encounterType", "e87aa2ad-6886-422e-9dfd-064e3bfe3aad");
                jsonEncounter.put("location", KenyaEMRlocationUuid);


                // Send API request
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());

                System.out.println("SYSTEM URL IS: " + url);

                Request request = new Request.Builder()
                        .url(url + "encounter")
                        .method("POST", body)
                        .addHeader("Authorization", "Basic " + auth)
                        .addHeader("Content-Type", "application/json")
                        .build();

                System.out.println("Payload is Here "+ jsonEncounter.toString() );

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    int responseCode = response.code();

                    System.out.println("Response: " + responseBody + " | Status Code: " + responseCode);

                    // Update response code for successful submissions
                    if (responseCode == 201) {
                        for (AMRSArtRefill artRefill : artRefillList) {
                            artRefill.setResponseCode("201");
                            amrsArtRefillService.save(artRefill);
                        }
                    } else {
                        System.err.println("Failed to process Visit ID: " + visitId + " | Status Code: " + responseCode);
                        for (AMRSArtRefill artRefill : artRefillList) {
                            artRefill.setResponseCode("400");
                            amrsArtRefillService.save(artRefill);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error processing Visit ID: " + visitId + " | " + e.getMessage());
                }
            }

    }
}

public static void defaulterTracing(AMRSDefaulterTracingService amrsDefaulterTracingService, AMRSTranslater amrsTranslater, String KenyaEMRlocationUuid, String url, String auth) throws  IOException, JSONException {
    List<AMRSDefaulterTracing> amrsDefaulterTracings = amrsDefaulterTracingService.findByResponseCodeIsNull();

    if (!amrsDefaulterTracings.isEmpty()) {
        // Use a Set to store unique patient IDs
        Set<String> visitIdSet = new HashSet<>();
        List<String> distinctVisitIds = new ArrayList<>();

        // Collect unique patient IDs
        for (AMRSDefaulterTracing defaulterTracing : amrsDefaulterTracings) {
            if (defaulterTracing.getResponseCode() == null && visitIdSet.add(defaulterTracing.getVisitId())) {
                distinctVisitIds.add(defaulterTracing.getVisitId());
            }
        }
        System.out.println("list of distinct clients " + distinctVisitIds);

        for (String visitId : distinctVisitIds) {
            System.out.println("Processing Visit ID: " + visitId);

            List<AMRSDefaulterTracing> amrsDefaulterTracingList = amrsDefaulterTracingService.findByVisitId(visitId);
            JSONArray jsonObservations = new JSONArray();
            String kenyaemrPatientUuid = "";
            String kenyaemrVisitUuid = "";
            String obsDatetime = "";

            // Prepare JSON observations
            for(int x=0; x<amrsDefaulterTracingList.size(); x++) {
                JSONObject jsonObservationD = new JSONObject();
                kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsDefaulterTracingList.get(x).getPatientId());
                kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
                obsDatetime = amrsDefaulterTracingList.get(x).getObsDatetime();
                jsonObservationD.put("person", kenyaemrPatientUuid);
                jsonObservationD.put("concept", amrsDefaulterTracingList.get(x).getKenyaEmrConceptUuid());
                jsonObservationD.put("value", amrsDefaulterTracingList.get(x).getKenyaEmrValueUuid());
                jsonObservationD.put("obsDatetime", amrsDefaulterTracingList.get(x).getObsDatetime());
                jsonObservationD.put("location", KenyaEMRlocationUuid);
                if(!Objects.equals(amrsDefaulterTracingList.get(x).getKenyaEmrConceptUuid(), "") && !Objects.equals(amrsDefaulterTracingList.get(x).getKenyaEmrValueUuid(), "") ) {
                    jsonObservations.put(jsonObservationD);
                }
            }

            JSONObject jsonEncounter = new JSONObject();

//            if(!Objects.equals(kenyaemrVisitUuid, "")) {
                jsonEncounter.put("form", "a1a62d1e-2def-11e9-b210-d663bd873d93");
                jsonEncounter.put("patient", kenyaemrPatientUuid);
                jsonEncounter.put("visit", kenyaemrVisitUuid);
                jsonEncounter.put("encounterDatetime", obsDatetime);
                jsonEncounter.put("obs", jsonObservations);
                jsonEncounter.put("encounterType", "1495edf8-2df2-11e9-b210-d663bd873d93");
                jsonEncounter.put("location", KenyaEMRlocationUuid);
//            }

            // Send API request
            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("application/json");
            okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());


            Request request = new Request.Builder()
                    .url(url + "encounter")
                    .method("POST", body)
                    .addHeader("Authorization", "Basic " + auth)
                    .addHeader("Content-Type", "application/json")
                    .build();

            System.out.println("Payload is Here " + jsonEncounter);

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                int responseCode = response.code();

                System.out.println("Response: " + responseBody + " | Status Code: " + responseCode);

                // Update response code for successful submissions
                if (responseCode == 201) {
                    for (AMRSDefaulterTracing amrsDefaulterTracing : amrsDefaulterTracingList) {
                        amrsDefaulterTracing.setResponseCode("201");
                        amrsDefaulterTracingService.save(amrsDefaulterTracing);
                    }
                } else {
                    System.err.println("Failed to process visit ID: " + visitId + " | Status Code: " + responseCode);
                }
            } catch (Exception e) {
                System.err.println("Error processing visit ID: " + visitId + " | " + e.getMessage());
            }
        }

    }
}

public static void ovc(AMRSOvcService amrsOvcService, AMRSTranslater amrsTranslater, String KenyaEMRlocationUuid, String url, String auth) throws JSONException, IOException, SQLException {
    List<AMRSOvc> amrsOvcs = amrsOvcService.findByResponseCodeIsNull();

    if (!amrsOvcs.isEmpty()) {
        // Use a Set to store unique patient IDs
        Set<String> visitIdSet = new HashSet<>();
        List<String> distinctVisitIds = new ArrayList<>();

        // Collect unique patient IDs
        for (AMRSOvc amrsOvc : amrsOvcs) {
            if (amrsOvc.getResponseCode() == null && visitIdSet.add(amrsOvc.getPatientId())) {
                distinctVisitIds.add(amrsOvc.getPatientId());
            }
        }

        System.out.println("list of distinct clients " + distinctVisitIds);

        for (String visitId : distinctVisitIds) {
            System.out.println("Processing visit ID: " + visitId);

            List<AMRSOvc> amrsOvcList = amrsOvcService.findByVisitId(visitId);
            JSONArray jsonObservations = new JSONArray();
            String kenyaemrPatientUuid = "";
            String kenyaemrVisitUuid = "";
            String obsDatetime = "";

            // Prepare JSON observations
            for (int x = 0; x < amrsOvcList.size(); x++) {
                JSONObject jsonObservationD = new JSONObject();
                kenyaemrPatientUuid = amrsTranslater.translater(amrsOvcList.get(x).getPatientId());
                kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
                obsDatetime = amrsOvcList.get(x).getObsDateTime();
                jsonObservationD.put("person", kenyaemrPatientUuid);
                jsonObservationD.put("concept", amrsOvcList.get(0).getKenyaEmrConceptUuid());
                jsonObservationD.put("value", amrsOvcList.get(0).getKenyaEmrValue());
                jsonObservationD.put("location", KenyaEMRlocationUuid);
                jsonObservationD.put("obsDatetime", amrsOvcList.get(0).getObsDateTime());
                if (!Objects.equals(amrsOvcList.get(0).getKenyaEmrValue(), "") && !Objects.equals(amrsOvcList.get(0).getKenyaEmrConceptUuid(), "")) {
                    jsonObservations.put(jsonObservationD);
                }
            }


            JSONObject jsonEncounter = new JSONObject();

            if (!Objects.equals(amrsOvcList.get(0).getKenyaemrVisitUuid(), "")) {
                jsonEncounter.put("form", "5cf013e8-09da-11ea-8d71-362b9e155667");
                jsonEncounter.put("patient", kenyaemrPatientUuid);
                jsonEncounter.put("obs", jsonObservations);
                jsonEncounter.put("visit", kenyaemrVisitUuid);
                jsonEncounter.put("encounterDatetime", obsDatetime);
                jsonEncounter.put("encounterType", "5cf00d9e-09da-11ea-8d71-362b9e155667");
                jsonEncounter.put("location", KenyaEMRlocationUuid);
            }


            // Send API request
            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("application/json");
            okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());

            System.out.println("SYSTEM URL IS: " + url);

            Request request = new Request.Builder()
                    .url(url + "encounter")
                    .method("POST", body)
                    .addHeader("Authorization", "Basic " + auth)
                    .addHeader("Content-Type", "application/json")
                    .build();

            System.out.println("Payload is Here " + jsonEncounter.toString());

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                int responseCode = response.code();

                System.out.println("Response: " + responseBody + " | Status Code: " + responseCode);

                // Update response code for successful submissions
                if (responseCode == 201) {
                    for (AMRSOvc amrsOvc : amrsOvcList) {
                        amrsOvc.setResponseCode("201");
                        amrsOvcService.save(amrsOvc);
                    }
                } else {
                    for (AMRSOvc amrsOvc : amrsOvcList) {
                        amrsOvc.setResponseCode("400");
                        amrsOvcService.save(amrsOvc);
                    }
                    System.err.println("Failed to process visit ID: " + visitId + " | Status Code: " + responseCode);
                }
            } catch (Exception e) {
                System.err.println("Error processing visit ID: " + visitId + " | " + e.getMessage());
            }
        }

    }
}
    public static void prepInitial(AMRSPrepInitialService amrsPrepInitialService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String KenyaEMRlocationUuid,String url, String auth) throws JSONException, IOException, SQLException {

        List<AMRSPrepInitial> amrsPrepInitials = amrsPrepInitialService.findByResponseCodeIsNull();

        if (!amrsPrepInitials.isEmpty()) {
            // Use a Set to store unique patient IDs
            Set<String> visitIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

            // Collect unique patient IDs
            for (AMRSPrepInitial amrsPrepInitial : amrsPrepInitials) {
                if (amrsPrepInitial.getResponseCode() == null && visitIdSet.add(amrsPrepInitial.getVisitId())) {
                    distinctVisitIds.add(amrsPrepInitial.getVisitId());
                }
            }

            System.out.println("list of distinct clients " + distinctVisitIds);
            for (String visitId : distinctVisitIds) {
                System.out.println("Processing visit ID: " + visitId);

                List<AMRSPrepInitial> amrsPrepInitialList = amrsPrepInitialService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                Set<String> uniqueObservations = new HashSet<>();

                String kenyaemrPatientUuid = "";
                String kenyaemrVisitUuid = "";
                String obsDatetime = "";

                // Prepare JSON observations
                for (int x = 0; x < amrsPrepInitialList.size(); x++) {
                    kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsPrepInitialList.get(x).getPatientId());
                    kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(amrsPrepInitialList.get(x).getVisitId());
                    obsDatetime = amrsPrepInitialList.get(x).getObsDateTime();

                    JSONObject jsonObservationD = new JSONObject();
                    jsonObservationD.put("person", kenyaemrPatientUuid);
                    jsonObservationD.put("concept", amrsPrepInitialList.get(x).getKenyaEmrConceptUuid());
                    jsonObservationD.put("value", amrsPrepInitialList.get(x).getKenyaEmrValue());
                    jsonObservationD.put("obsDatetime", amrsPrepInitialList.get(x).getObsDateTime());
                    jsonObservationD.put("location", KenyaEMRlocationUuid);


                    if (!Objects.equals(amrsPrepInitialList.get(x).getKenyaEmrValue(), "") && !Objects.equals(amrsPrepInitialList.get(x).getKenyaEmrConceptUuid(), "")) {
                        // Convert JSON object to string to ensure uniqueness
                        String jsonString = jsonObservationD.toString();
                        if (!uniqueObservations.contains(jsonString)) {
                            uniqueObservations.add(jsonString); // Add to the set
                            System.out.println("JSON OBSERVATION IS :" + x + jsonString);
                            if (!Objects.equals(amrsPrepInitialList.get(x).getKenyaEmrConceptUuid(), "152370AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {

                                jsonObservations.put(jsonObservationD); // Add to the JSON array
                            }
                        }
                    }
                }

                JSONObject jsonEncounter = new JSONObject();
                jsonEncounter.put("form", "1bfb09fc-56d7-4108-bd59-b2765fd312b8");
                jsonEncounter.put("patient", kenyaemrPatientUuid);
                jsonEncounter.put("obs", jsonObservations);
                jsonEncounter.put("visit", kenyaemrVisitUuid);
                jsonEncounter.put("encounterDatetime", obsDatetime);
                jsonEncounter.put("encounterType", "706a8b12-c4ce-40e4-aec3-258b989bf6d3");
                jsonEncounter.put("location", KenyaEMRlocationUuid);


                // Send API request
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());


                Request request = new Request.Builder()
                        .url(url + "encounter")
                        .method("POST", body)
                        .addHeader("Authorization", "Basic " + auth)
                        .addHeader("Content-Type", "application/json")
                        .build();

                System.out.println("Payload is Here " + jsonEncounter);

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    int responseCode = response.code();

                    System.out.println("Response: " + responseBody + " | Status Code: " + responseCode);

                    // Update response code for successful submissions
                    if (responseCode == 201) {
                        for (AMRSPrepInitial amrsPrepInitial : amrsPrepInitialList) {
                            amrsPrepInitial.setResponseCode("201");
                            amrsPrepInitialService.save(amrsPrepInitial);
                        }
                    } else {
                        for (AMRSPrepInitial amrsPrepInitial : amrsPrepInitialList) {
                            amrsPrepInitial.setResponseCode("400");
                            amrsPrepInitialService.save(amrsPrepInitial);
                        }
                        System.err.println("Failed to process visit ID: " + visitId + " | Status Code: " + responseCode);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing visit ID: " + visitId + " | " + e.getMessage());
                }
            }

        }
    }


    public static void prepFollowUp(AMRSPrepFollowUpService amrsPrepFollowUpService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth) throws JSONException, IOException, SQLException {
        List<AMRSPrepFollowUp> amrsPrepFollowUps = amrsPrepFollowUpService.findByResponseCodeIsNull();

        if (!amrsPrepFollowUps.isEmpty()) {
            // Use a Set to store unique patient IDs
            Set<String> visitIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

            // Collect unique patient IDs
            for (AMRSPrepFollowUp amrsPrepFollowUp : amrsPrepFollowUps) {
                if (amrsPrepFollowUp.getResponseCode() == null && visitIdSet.add(amrsPrepFollowUp.getVisitId())) {
                    distinctVisitIds.add(amrsPrepFollowUp.getVisitId());
                }
            }

            System.out.println("list of distinct clients " + distinctVisitIds);
            for (String visitId : distinctVisitIds) {
                System.out.println("Processing visit ID: " + visitId);

                List<AMRSPrepFollowUp> amrsPrepFollowUpList = amrsPrepFollowUpService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                Set<String> uniqueObservations = new HashSet<>();
                String kenyaemrPatientUuid = "";
                String kenyaemrVisitUuid = "";
                String obsDatetime = "";

                // Prepare JSON observations
                for (int x = 0; x < amrsPrepFollowUpList.size(); x++) {
                    kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsPrepFollowUpList.get(x).getPatientId());
                    kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
                    obsDatetime = amrsPrepFollowUpList.get(x).getObsDateTime();

                    JSONObject jsonObservationD = new JSONObject();
                    jsonObservationD.put("person", kenyaemrPatientUuid);
                    jsonObservationD.put("concept", amrsPrepFollowUpList.get(x).getKenyaEmrConceptUuid());
                    jsonObservationD.put("value", amrsPrepFollowUpList.get(x).getKenyaEmrValue());
                    jsonObservationD.put("obsDatetime", amrsPrepFollowUpList.get(x).getObsDateTime());
                    if (!Objects.equals(amrsPrepFollowUpList.get(x).getKenyaEmrValue(), "") && !Objects.equals(amrsPrepFollowUpList.get(x).getKenyaEmrConceptUuid(), "")) {
                        // Convert JSON object to string to ensure uniqueness
                        String jsonString = jsonObservationD.toString();
                        if (!uniqueObservations.contains(jsonString)) {
                            uniqueObservations.add(jsonString); // Add to the set
                            System.out.println("JSON OBSERVATION IS :" + x + jsonString);
                            if (!Objects.equals(amrsPrepFollowUpList.get(x).getKenyaEmrConceptUuid(), "152370AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {

                                jsonObservations.put(jsonObservationD); // Add to the JSON array
                            } // Add to the JSON array
                        }
                    }

                }

                JSONObject jsonEncounter = new JSONObject();

                jsonEncounter.put("form", "ee3e2017-52c0-4a54-99ab-ebb542fb8984");
                jsonEncounter.put("patient", kenyaemrPatientUuid);
                jsonEncounter.put("obs", jsonObservations);
                jsonEncounter.put("visit", kenyaemrVisitUuid);
                jsonEncounter.put("encounterDatetime", obsDatetime);
                jsonEncounter.put("encounterType", "c4a2be28-6673-4c36-b886-ea89b0a42116");
                jsonEncounter.put("location", "37f6bd8d-586a-4169-95fa-5781f987fe62");


                // Send API request
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());


                Request request = new Request.Builder()
                        .url(url + "encounter")
                        .method("POST", body)
                        .addHeader("Authorization", "Basic " + auth)
                        .addHeader("Content-Type", "application/json")
                        .build();

                System.out.println("Payload is Here " + jsonEncounter);

                try (Response response = client.newCall(request).execute()) {
                    assert response.body() != null;
                    String responseBody = response.body().string();
                    int responseCode = response.code();

                    System.out.println("Response: " + responseBody + " | Status Code: " + responseCode);

                    // Update response code for successful submissions
                    if (responseCode == 201) {
                        for (AMRSPrepFollowUp amrsPrepFollowUp : amrsPrepFollowUpList) {
                            amrsPrepFollowUp.setResponseCode("201");
                            amrsPrepFollowUpService.save(amrsPrepFollowUp);
                        }
                    } else {
                        System.err.println("Failed to process visit ID: " + visitId + " | Status Code: " + responseCode);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing visit ID: " + visitId + " | " + e.getMessage());
                }
            }

        }
    }

    public static void prepMonthlyRefill(AMRSPrepMonthlyRefillService amrsPrepMonthlyRefillService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String KenyaEMRlocationUuid, String url, String auth) throws JSONException, IOException, SQLException {
        List<AMRSPrepMonthlyRefill> amrsPrepMonthlyRefills = amrsPrepMonthlyRefillService.findByResponseCodeIsNull();

        if (!amrsPrepMonthlyRefills.isEmpty()) {
            // Use a Set to store unique patient IDs
            Set<String> visitIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

            // Collect unique patient IDs
            for (AMRSPrepMonthlyRefill amrsPrepMonthlyRefill : amrsPrepMonthlyRefills) {
                if (amrsPrepMonthlyRefill.getResponseCode() == null && visitIdSet.add(amrsPrepMonthlyRefill.getVisitId())) {
                    distinctVisitIds.add(amrsPrepMonthlyRefill.getVisitId());
                }
            }

            System.out.println("list of distinct clients " + distinctVisitIds);
            for (String visitId : distinctVisitIds) {
                System.out.println("Processing visit ID: " + visitId);

                List<AMRSPrepMonthlyRefill> amrsPrepMonthlyRefillList = amrsPrepMonthlyRefillService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                Set<String> uniqueObservations = new HashSet<>();
                String kenyaemrPatientUuid = "";
                String kenyaemrVisitUuid = "";
                String obsDatetime = "";

                // Prepare JSON observations
                for (int x = 0; x < amrsPrepMonthlyRefillList.size(); x++) {
                    kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsPrepMonthlyRefillList.get(x).getPatientId());
                    kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
                    obsDatetime = amrsPrepMonthlyRefillList.get(x).getObsDateTime();

                    JSONObject jsonObservationD = new JSONObject();
                    jsonObservationD.put("person", kenyaemrPatientUuid);
                    jsonObservationD.put("concept", amrsPrepMonthlyRefillList.get(x).getKenyaEmrConceptUuid());
                    jsonObservationD.put("value", amrsPrepMonthlyRefillList.get(x).getKenyaEmrValue());
                    jsonObservationD.put("obsDatetime", amrsPrepMonthlyRefillList.get(x).getObsDateTime());

                    if (!Objects.equals(amrsPrepMonthlyRefillList.get(0).getKenyaEmrValue(), "") && !Objects.equals(amrsPrepMonthlyRefillList.get(0).getKenyaEmrConceptUuid(), "")) {
                        // Convert JSON object to string to ensure uniqueness
                        String jsonString = jsonObservationD.toString();
                        if (!uniqueObservations.contains(jsonString)) {
                            uniqueObservations.add(jsonString); // Add to the set
                            System.out.println("JSON OBSERVATION IS :" + x + jsonString);
                            if (!Objects.equals(amrsPrepMonthlyRefillList.get(x).getKenyaEmrConceptUuid(), "152370AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {

                                jsonObservations.put(jsonObservationD); // Add to the JSON array
                            } // Add to the JSON array
                        }
                    }
                }


                JSONObject jsonEncounter = new JSONObject();

                if (!Objects.equals(amrsPrepMonthlyRefillList.get(0).getKenyaEmrVisitUuid(), "")) {
                    jsonEncounter.put("form", "ee3e2017-52c0-4a54-99ab-ebb542fb8984");
                    jsonEncounter.put("patient", kenyaemrPatientUuid);
                    jsonEncounter.put("obs", jsonObservations);
                    jsonEncounter.put("visit", kenyaemrVisitUuid);
                    jsonEncounter.put("encounterDatetime", obsDatetime);
                    jsonEncounter.put("encounterType", "c4a2be28-6673-4c36-b886-ea89b0a42116");
                    jsonEncounter.put("location", KenyaEMRlocationUuid);
                }

                // Send API request
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());


                Request request = new Request.Builder()
                        .url(url + "encounter")
                        .method("POST", body)
                        .addHeader("Authorization", "Basic " + auth)
                        .addHeader("Content-Type", "application/json")
                        .build();

                System.out.println("Payload is Here " + jsonEncounter);

                try (Response response = client.newCall(request).execute()) {
                    assert response.body() != null;
                    String responseBody = response.body().string();
                    int responseCode = response.code();

                    System.out.println("Response: " + responseBody + " | Status Code: " + responseCode);

                    // Update response code for successful submissions
                    if (responseCode == 201) {
                        for (AMRSPrepMonthlyRefill amrsPrepMonthlyRefill : amrsPrepMonthlyRefillList) {
                            amrsPrepMonthlyRefill.setResponseCode("201");
                            amrsPrepMonthlyRefillService.save(amrsPrepMonthlyRefill);
                        }
                    } else {
                        System.err.println("Failed to process visit ID: " + visitId + " | Status Code: " + responseCode);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing visit ID: " + visitId + " | " + e.getMessage());
                }
            }

        }
    }

    public static void processCovid(AMRSCovidService amrsCovidService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String KenyaEMRlocationUuid, String url, String auth) throws JSONException, IOException {
        List<AMRSCovid> amrsCovidList = amrsCovidService.findByResponseCodeIsNull();
        if (!amrsCovidList.isEmpty()) {
            // Use a Set to store unique encounter IDs
            Set<String> visistIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

            // Loop through the list
            for (AMRSCovid amrsCovid : amrsCovidList) {
                if (amrsCovid.getResponseCode() == null) {
                    String visitId = amrsCovid.getVisitId();
                    // Add to the result list only if it hasn't been added already
                    if (visistIdSet.add(visitId)) {
                        distinctVisitIds.add(visitId);
                    }
                }
            }

            for (String visitId : distinctVisitIds) {
                List<AMRSCovid> amrsCovidEncounters = amrsCovidService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                String patientuuid = "";
                String formuuid = "";
                String encounteruuid = "";
                String encounterDatetime = "";
                String obsDatetime = "";
                String visituuid = amrsTranslater.kenyaemrVisitUuid(visitId);

                for (int x = 0; x < amrsCovidEncounters.size(); x++) {
                    String kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsCovidEncounters.get(x).getPatientId());
                    JSONObject jsonObservation = new JSONObject();
                    String value = amrsCovidEncounters.get(x).getKenyaEmrValue();
                    obsDatetime = amrsCovidEncounters.get(x).getObsDateTime();
                    jsonObservation.put("person", kenyaemrPatientUuid);
                    jsonObservation.put("concept", amrsCovidEncounters.get(x).getKenyaEmrConceptUuid());
                    jsonObservation.put("obsDatetime", obsDatetime);
                    jsonObservation.put("value", value);
                    jsonObservation.put("location", KenyaEMRlocationUuid);

                    patientuuid = amrsTranslater.KenyaemrPatientUuid(amrsCovidEncounters.get(x).getPatientId());
                    formuuid = amrsCovidEncounters.get(x).getKenyaemrFormUuid();
                    encounteruuid = amrsCovidEncounters.get(x).getKenyaemrEncounterTypeUuid();
                    encounterDatetime = amrsCovidEncounters.get(x).getKenyaEmrEncounterDateTime();

                    if (!Objects.equals(amrsCovidEncounters.get(0).getKenyaEmrValue(), "") && !Objects.equals(amrsCovidEncounters.get(0).getKenyaEmrConceptUuid(), "")) {
                        jsonObservations.put(jsonObservation);
                    }
                }

                //Publish the data to KenyaEMR
                if (!Objects.equals(visituuid, "")) {
                    JSONObject jsonEncounter = new JSONObject();
                    jsonEncounter.put("form", formuuid);
                    jsonEncounter.put("patient", patientuuid);
                    jsonEncounter.put("encounterDatetime", encounterDatetime);
                    jsonEncounter.put("encounterType", encounteruuid);
                    jsonEncounter.put("location", KenyaEMRlocationUuid);
                    jsonEncounter.put("visit", visituuid);
                    jsonEncounter.put("obs", jsonObservations);
                    System.out.println("Payload for is here " + jsonEncounter.toString());

                    OkHttpClient client = new OkHttpClient();
                    MediaType mediaType = MediaType.parse("application/json");
                    okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());
                    Request request = new Request.Builder()
                            .url(url + "encounter")
                            .method("POST", body)
                            .addHeader("Authorization", "Basic " + auth)
                            .addHeader("Content-Type", "application/json")
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseBody = response.body().string(); // Get the response as a string
                    System.out.println("Response ndo hii " + responseBody + " More message " + response.message());

                    String resBody = response.request().toString();
                    int rescode = response.code();
                    System.out.println("Response Code Hapa " + rescode);

                    if (rescode == 201) {
                        for (int x = 0; x < amrsCovidEncounters.size(); x++) {
                            AMRSCovid at = amrsCovidEncounters.get(x);
                            at.setResponseCode(String.valueOf(rescode));
                            at.setResponseCode("201");
                            System.out.println("Imefika Hapa na data " + rescode);
                            amrsCovidService.save(at);
                        }
                    }else{
                        for (int x = 0; x < amrsCovidEncounters.size(); x++) {
                            AMRSCovid at = amrsCovidEncounters.get(x);
                            at.setResponseCode(String.valueOf(rescode));
                            at.setResponseCode("400");
                            System.out.println("Imefika Hapa na data " + rescode);
                            amrsCovidService.save(at);
                        }
                    }
                }
            }
        }
    }

    public static void processAlcohol(AMRSAlcoholService amrsAlcoholService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String KenyaEMRlocationUuid, String url, String auth) throws JSONException, IOException {
    List<AMRSAlcohol> amrsModelList = amrsAlcoholService.findByResponseCodeIsNull();
    if (!amrsModelList.isEmpty()) {
      // Use a Set to store unique encounter IDs
      Set<String> visistIdSet = new HashSet<>();
      List<String> distinctVisitIds = new ArrayList<>();

      // Loop through the list
      for (AMRSAlcohol amrsAlcohol : amrsModelList) {
        if (amrsAlcohol.getResponseCode() == null) {
          String visitId = amrsAlcohol.getVisitId();
          // Add to the result list only if it hasn't been added already
          if (visistIdSet.add(visitId)) {
            distinctVisitIds.add(visitId);
          }
        }
      }

      for (String visitId : distinctVisitIds) {
        List<AMRSAlcohol> amrsModelEncounters = amrsAlcoholService.findByVisitId(visitId);
        JSONArray jsonObservations = new JSONArray();
        String patientuuid = "";
        String formuuid = "";
        String encounteruuid = "";
        String encounterDatetime = "";
        String obsDatetime = "";
        String visituuid = amrsTranslater.kenyaemrVisitUuid(visitId);

        for (int x = 0; x < amrsModelEncounters.size(); x++) {
          String kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsModelEncounters.get(x).getPatientId());
          JSONObject jsonObservation = new JSONObject();
          String value = amrsModelEncounters.get(x).getKenyaEmrValue();
          obsDatetime = amrsModelEncounters.get(x).getObsDateTime();
          jsonObservation.put("person", kenyaemrPatientUuid);
          jsonObservation.put("concept", amrsModelEncounters.get(x).getKenyaEmrConceptUuid());
          jsonObservation.put("obsDatetime", obsDatetime);
          jsonObservation.put("value", value);
          jsonObservation.put("location", KenyaEMRlocationUuid);

          patientuuid = amrsTranslater.KenyaemrPatientUuid(amrsModelEncounters.get(x).getPatientId());
          formuuid = amrsModelEncounters.get(x).getKenyaemrFormUuid();
          encounteruuid = amrsModelEncounters.get(x).getKenyaemrEncounterTypeUuid();
          encounterDatetime = amrsModelEncounters.get(x).getKenyaEmrEncounterDateTime();

          if (!Objects.equals(amrsModelEncounters.get(0).getKenyaEmrValue(), "") && !Objects.equals(amrsModelEncounters.get(0).getKenyaEmrConceptUuid(), "")) {
            jsonObservations.put(jsonObservation);
          }
        }

        //Publish the data to KenyaEMR
        if (!Objects.equals(visituuid, "")) {
          JSONObject jsonEncounter = new JSONObject();
          jsonEncounter.put("form", formuuid);
          jsonEncounter.put("patient", patientuuid);
          jsonEncounter.put("encounterDatetime", encounterDatetime);
          jsonEncounter.put("encounterType", encounteruuid);
          jsonEncounter.put("location", KenyaEMRlocationUuid);
          jsonEncounter.put("visit", visituuid);
          jsonEncounter.put("obs", jsonObservations);
          System.out.println("Payload for is here " + jsonEncounter.toString());

          OkHttpClient client = new OkHttpClient();
          MediaType mediaType = MediaType.parse("application/json");
          okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());
          Request request = new Request.Builder()
            .url(url + "encounter")
            .method("POST", body)
            .addHeader("Authorization", "Basic " + auth)
            .addHeader("Content-Type", "application/json")
            .build();

          Response response = client.newCall(request).execute();
          String responseBody = response.body().string(); // Get the response as a string
          System.out.println("Response ndo hii " + responseBody + " More message " + response.message());

          String resBody = response.request().toString();
          int rescode = response.code();
          System.out.println("Response Code Hapa " + rescode);

          if (rescode == 201) {
            for (int x = 0; x < amrsModelEncounters.size(); x++) {
              AMRSAlcohol at = amrsModelEncounters.get(x);
              at.setResponseCode(String.valueOf(rescode));
              at.setResponseCode("201");
              System.out.println("Imefika Hapa na data " + rescode);
              amrsAlcoholService.save(at);
            }
          }else{
            for (int x = 0; x < amrsModelEncounters.size(); x++) {
              AMRSAlcohol at = amrsModelEncounters.get(x);
              at.setResponseCode(String.valueOf(rescode));
              at.setResponseCode("400");
              System.out.println("Imefika Hapa na data " + rescode);
              amrsAlcoholService.save(at);
            }
          }
        }
      }
    }
  }
    public static void mchEnrollment(AMRSMchEnrollmentService amrsMchEnrollmentService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth)throws JSONException, IOException, SQLException {
        List<AMRSMchEnrollment> amrsMchEnrollments = amrsMchEnrollmentService.findByResponseCodeIsNull();

        if (!amrsMchEnrollments.isEmpty()) {
            // Use a Set to store unique patient IDs
            Set<String> visitIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

            // Collect unique patient IDs
            for (AMRSMchEnrollment amrsMchEnrollment : amrsMchEnrollments) {
                if (amrsMchEnrollment.getResponseCode() == null && visitIdSet.add(amrsMchEnrollment.getVisitId())) {
                    distinctVisitIds.add(amrsMchEnrollment.getVisitId());
                }
            }

            System.out.println("list of distinct clients " + distinctVisitIds);
            for (String visitId : distinctVisitIds) {
                System.out.println("Processing visit ID: " + visitId);

                List<AMRSMchEnrollment> amrsMchEnrollmentList = amrsMchEnrollmentService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                Set<String> uniqueObservations = new HashSet<>();
                String kenyaemrPatientUuid = "";
                String kenyaemrVisitUuid = "";
                String obsDatetime = "";

                // Prepare JSON observations
                for(int x = 0; x< amrsMchEnrollmentList.size(); x++) {
                    kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsMchEnrollmentList.get(x).getPatientId());
                    kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
                    obsDatetime = amrsMchEnrollmentList.get(x).getObsDateTime();

                    JSONObject jsonObservationD = new JSONObject();
                    jsonObservationD.put("person", kenyaemrPatientUuid);
                    jsonObservationD.put("concept", amrsMchEnrollmentList.get(x).getKenyaEmrConceptUuid());
                    jsonObservationD.put("value", amrsMchEnrollmentList.get(x).getKenyaEmrValue());
                    jsonObservationD.put("obsDatetime", amrsMchEnrollmentList.get(x).getObsDateTime());
                    if(!Objects.equals(amrsMchEnrollmentList.get(x).getKenyaEmrValue(), "") && !Objects.equals(amrsMchEnrollmentList.get(x).getKenyaEmrConceptUuid(), "") ) {
                        // Convert JSON object to string to ensure uniqueness
                        String jsonString = jsonObservationD.toString();
                        if (!uniqueObservations.contains(jsonString)) {
                            uniqueObservations.add(jsonString); // Add to the set
                            System.out.println("JSON OBSERVATION IS :" + x + jsonString);

                            jsonObservations.put(jsonObservationD); // Add to the JSON array
                            // Add to the JSON array
                        }
                    }

                }

                JSONObject jsonEncounter = new JSONObject();

                jsonEncounter.put("form", "90a18f0c-17cd-4eec-8204-5af52e8d77cf");
                jsonEncounter.put("patient", kenyaemrPatientUuid);
                jsonEncounter.put("obs", jsonObservations);
                jsonEncounter.put("visit", kenyaemrVisitUuid);
                jsonEncounter.put("encounterDatetime", obsDatetime);
                jsonEncounter.put("encounterType", "3ee036d8-7c13-4393-b5d6-036f2fe45126");
                jsonEncounter.put("location", "37f6bd8d-586a-4169-95fa-5781f987fe62");




                // Send API request
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());


                Request request = new Request.Builder()
                        .url(url + "encounter")
                        .method("POST", body)
                        .addHeader("Authorization", "Basic " + auth)
                        .addHeader("Content-Type", "application/json")
                        .build();

                System.out.println("Payload is Here "+ jsonEncounter );

                try (Response response = client.newCall(request).execute()) {
                    assert response.body() != null;
                    String responseBody = response.body().string();
                    int responseCode = response.code();

                    System.out.println("Response: " + responseBody + " | Status Code: " + responseCode);

                    // Update response code for successful submissions
                    if (responseCode == 201) {
                        for (AMRSMchEnrollment amrsMchEnrollment : amrsMchEnrollmentList) {
                            amrsMchEnrollment.setResponseCode("201");
                            amrsMchEnrollmentService.save(amrsMchEnrollment);
                        }
                    } else {
                        System.err.println("Failed to process visit ID: " + visitId + " | Status Code: " + responseCode);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing visit ID: " + visitId + " | " + e.getMessage());
                }
            }

        }
    }

    public static void mchAntenatal(AMRSMchAntenatalService amrsMchChildEnrollmentService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth)throws JSONException, IOException, SQLException {
        List<AMRSMchAntenatal> amrsMchAntenatals = amrsMchChildEnrollmentService.findByResponseCodeIsNull();

        if (!amrsMchAntenatals.isEmpty()) {
            // Use a Set to store unique patient IDs
            Set<String> visitIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

            // Collect unique patient IDs
            for (AMRSMchAntenatal amrsMchAntenatal : amrsMchAntenatals) {
                if (amrsMchAntenatal.getResponseCode() == null && visitIdSet.add(amrsMchAntenatal.getVisitId())) {
                    distinctVisitIds.add(amrsMchAntenatal.getVisitId());
                }
            }

            System.out.println("list of distinct clients " + distinctVisitIds);
            for (String visitId : distinctVisitIds) {
                System.out.println("Processing visit ID: " + visitId);

                List<AMRSMchAntenatal> amrsMchAntenatalList = amrsMchChildEnrollmentService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                Set<String> uniqueObservations = new HashSet<>();
                String kenyaemrPatientUuid = "";
                String kenyaemrVisitUuid = "";
                String obsDatetime = "";

                // Prepare JSON observations
                for(int x = 0; x< amrsMchAntenatalList.size(); x++) {
                    kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsMchAntenatalList.get(x).getPatientId());
                    kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
                    obsDatetime = amrsMchAntenatalList.get(x).getObsDateTime();

                    JSONObject jsonObservationD = new JSONObject();
                    jsonObservationD.put("person", kenyaemrPatientUuid);
                    jsonObservationD.put("concept", amrsMchAntenatalList.get(x).getKenyaEmrConceptUuid());
                    jsonObservationD.put("value", amrsMchAntenatalList.get(x).getKenyaEmrValue());
                    jsonObservationD.put("obsDatetime", amrsMchAntenatalList.get(x).getObsDateTime());
                    if(!Objects.equals(amrsMchAntenatalList.get(x).getKenyaEmrValue(), "") && !Objects.equals(amrsMchAntenatalList.get(x).getKenyaEmrConceptUuid(), "") ) {
                        // Convert JSON object to string to ensure uniqueness
                        String jsonString = jsonObservationD.toString();
                        if (!uniqueObservations.contains(jsonString)) {
                            uniqueObservations.add(jsonString); // Add to the set
                            System.out.println("JSON OBSERVATION IS :" + x + jsonString);

                            jsonObservations.put(jsonObservationD); // Add to the JSON array
                            // Add to the JSON array
                        }
                    }

                }

                JSONObject jsonEncounter = new JSONObject();

                jsonEncounter.put("form", "8553d869-bdc8-4287-8505-910c7c998aff");
                jsonEncounter.put("patient", kenyaemrPatientUuid);
                jsonEncounter.put("obs", jsonObservations);
                jsonEncounter.put("visit", kenyaemrVisitUuid);
                jsonEncounter.put("encounterDatetime", obsDatetime);
                jsonEncounter.put("encounterType", "415f5136-ca4a-49a8-8db3-f994187c3af6");
                jsonEncounter.put("location", "37f6bd8d-586a-4169-95fa-5781f987fe62");




                // Send API request
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());


                Request request = new Request.Builder()
                        .url(url + "encounter")
                        .method("POST", body)
                        .addHeader("Authorization", "Basic " + auth)
                        .addHeader("Content-Type", "application/json")
                        .build();

                System.out.println("Payload is Here "+ jsonEncounter );

                try (Response response = client.newCall(request).execute()) {
                    assert response.body() != null;
                    String responseBody = response.body().string();
                    int responseCode = response.code();

                    System.out.println("Response: " + responseBody + " | Status Code: " + responseCode);

                    // Update response code for successful submissions
                    if (responseCode == 201) {
                        for (AMRSMchAntenatal amrsMchAntenatal : amrsMchAntenatalList) {
                            amrsMchAntenatal.setResponseCode("201");
                            amrsMchChildEnrollmentService.save(amrsMchAntenatal);
                        }
                    } else {
                        System.err.println("Failed to process visit ID: " + visitId + " | Status Code: " + responseCode);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing visit ID: " + visitId + " | " + e.getMessage());
                }
            }

        }
    }

    public static void mchDischargeAndReferral(AMRSMchDischargeAndReferralService amrsMchDischargeAndReferralService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth)throws JSONException, IOException, SQLException {
        List<AMRSMchDischargeAndReferral> amrsMchDischargeAndReferrals = amrsMchDischargeAndReferralService.findByResponseCodeIsNull();

        if (!amrsMchDischargeAndReferrals.isEmpty()) {
            // Use a Set to store unique patient IDs
            Set<String> visitIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

            // Collect unique patient IDs
            for (AMRSMchDischargeAndReferral amrsMchDischargeAndReferral : amrsMchDischargeAndReferrals) {
                if (amrsMchDischargeAndReferral.getResponseCode() == null && visitIdSet.add(amrsMchDischargeAndReferral.getVisitId())) {
                    distinctVisitIds.add(amrsMchDischargeAndReferral.getVisitId());
                }
            }

            System.out.println("list of distinct clients " + distinctVisitIds);
            for (String visitId : distinctVisitIds) {
                System.out.println("Processing visit ID: " + visitId);

                List<AMRSMchDischargeAndReferral> amrsMchDischargeAndReferralList = amrsMchDischargeAndReferralService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                Set<String> uniqueObservations = new HashSet<>();
                String kenyaemrPatientUuid = "";
                String kenyaemrVisitUuid = "";
                String obsDatetime = "";

                // Prepare JSON observations
                for(int x = 0; x< amrsMchDischargeAndReferralList.size(); x++) {
                    kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsMchDischargeAndReferralList.get(x).getPatientId());
                    kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
                    obsDatetime = amrsMchDischargeAndReferralList.get(x).getObsDateTime();

                    JSONObject jsonObservationD = new JSONObject();
                    jsonObservationD.put("person", kenyaemrPatientUuid);
                    jsonObservationD.put("concept", amrsMchDischargeAndReferralList.get(x).getKenyaEmrConceptUuid());
                    jsonObservationD.put("value", amrsMchDischargeAndReferralList.get(x).getKenyaEmrValue());
                    jsonObservationD.put("obsDatetime", amrsMchDischargeAndReferralList.get(x).getObsDateTime());
                    if(!Objects.equals(amrsMchDischargeAndReferralList.get(x).getKenyaEmrValue(), "") && !Objects.equals(amrsMchDischargeAndReferralList.get(x).getKenyaEmrConceptUuid(), "") ) {
                        // Convert JSON object to string to ensure uniqueness
                        String jsonString = jsonObservationD.toString();
                        if (!uniqueObservations.contains(jsonString)) {
                            uniqueObservations.add(jsonString); // Add to the set
                            System.out.println("JSON OBSERVATION IS :" + x + jsonString);

                            jsonObservations.put(jsonObservationD); // Add to the JSON array
                            // Add to the JSON array
                        }
                    }

                }

                JSONObject jsonEncounter = new JSONObject();

                jsonEncounter.put("form", "af273344-a5f9-11e8-98d0-529269fb1459");
                jsonEncounter.put("patient", kenyaemrPatientUuid);
                jsonEncounter.put("obs", jsonObservations);
                jsonEncounter.put("visit", kenyaemrVisitUuid);
                jsonEncounter.put("encounterDatetime", obsDatetime);
                jsonEncounter.put("encounterType", "c6d09e05-1f25-4164-8860-9f32c5a02df0");
                jsonEncounter.put("location", "37f6bd8d-586a-4169-95fa-5781f987fe62");




                // Send API request
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());


                Request request = new Request.Builder()
                        .url(url + "encounter")
                        .method("POST", body)
                        .addHeader("Authorization", "Basic " + auth)
                        .addHeader("Content-Type", "application/json")
                        .build();

                System.out.println("Payload is Here "+ jsonEncounter );

                try (Response response = client.newCall(request).execute()) {
                    assert response.body() != null;
                    String responseBody = response.body().string();
                    int responseCode = response.code();

                    System.out.println("Response: " + responseBody + " | Status Code: " + responseCode);

                    // Update response code for successful submissions
                    if (responseCode == 201) {
                        for (AMRSMchDischargeAndReferral amrsMchDischargeAndReferral : amrsMchDischargeAndReferralList) {
                            amrsMchDischargeAndReferral.setResponseCode("201");
                            amrsMchDischargeAndReferralService.save(amrsMchDischargeAndReferral);
                        }
                    } else {
                        System.err.println("Failed to process visit ID: " + visitId + " | Status Code: " + responseCode);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing visit ID: " + visitId + " | " + e.getMessage());
                }
            }

        }
    }


    public static void mchPostnatal(AMRSMchPostnatalService amrsMchPostnatalService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth)throws JSONException, IOException, SQLException {
        List<AMRSMchPostnatal> amrsMchPostnatals = amrsMchPostnatalService.findByResponseCodeIsNull();

        if (!amrsMchPostnatals.isEmpty()) {
            // Use a Set to store unique patient IDs
            Set<String> visitIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

            // Collect unique patient IDs
            for (AMRSMchPostnatal amrsMchPostnatal : amrsMchPostnatals) {
                if (amrsMchPostnatal.getResponseCode() == null && visitIdSet.add(amrsMchPostnatal.getVisitId())) {
                    distinctVisitIds.add(amrsMchPostnatal.getVisitId());
                }
            }

            System.out.println("list of distinct clients " + distinctVisitIds);
            for (String visitId : distinctVisitIds) {
                System.out.println("Processing visit ID: " + visitId);

                List<AMRSMchPostnatal> amrsMchPostnatalList = amrsMchPostnatalService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                Set<String> uniqueObservations = new HashSet<>();
                String kenyaemrPatientUuid = "";
                String kenyaemrVisitUuid = "";
                String obsDatetime = "";

                // Prepare JSON observations
                for(int x = 0; x< amrsMchPostnatalList.size(); x++) {
                    kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsMchPostnatalList.get(x).getPatientId());
                    kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
                    obsDatetime = amrsMchPostnatalList.get(x).getObsDateTime();

                    JSONObject jsonObservationD = new JSONObject();
                    jsonObservationD.put("person", kenyaemrPatientUuid);
                    jsonObservationD.put("concept", amrsMchPostnatalList.get(x).getKenyaEmrConceptUuid());
                    jsonObservationD.put("value", amrsMchPostnatalList.get(x).getKenyaEmrValue());
                    jsonObservationD.put("obsDatetime", amrsMchPostnatalList.get(x).getObsDateTime());
                    if(!Objects.equals(amrsMchPostnatalList.get(x).getKenyaEmrValue(), "") && !Objects.equals(amrsMchPostnatalList.get(x).getKenyaEmrConceptUuid(), "") ) {
                        // Convert JSON object to string to ensure uniqueness
                        String jsonString = jsonObservationD.toString();
                        if (!uniqueObservations.contains(jsonString)) {
                            uniqueObservations.add(jsonString); // Add to the set
                            System.out.println("JSON OBSERVATION IS :" + x + jsonString);

                            jsonObservations.put(jsonObservationD); // Add to the JSON array
                            // Add to the JSON array
                        }
                    }

                }

                JSONObject jsonEncounter = new JSONObject();

                jsonEncounter.put("form", "72aa78e0-ee4b-47c3-9073-26f3b9ecc4a7");
                jsonEncounter.put("patient", kenyaemrPatientUuid);
                jsonEncounter.put("obs", jsonObservations);
                jsonEncounter.put("visit", kenyaemrVisitUuid);
                jsonEncounter.put("encounterDatetime", obsDatetime);
                jsonEncounter.put("encounterType", "c6d09e05-1f25-4164-8860-9f32c5a02df0");
                jsonEncounter.put("location", "37f6bd8d-586a-4169-95fa-5781f987fe62");




                // Send API request
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());


                Request request = new Request.Builder()
                        .url(url + "encounter")
                        .method("POST", body)
                        .addHeader("Authorization", "Basic " + auth)
                        .addHeader("Content-Type", "application/json")
                        .build();

                System.out.println("Payload is Here "+ jsonEncounter );

                try (Response response = client.newCall(request).execute()) {
                    assert response.body() != null;
                    String responseBody = response.body().string();
                    int responseCode = response.code();

                    System.out.println("Response: " + responseBody + " | Status Code: " + responseCode);

                    // Update response code for successful submissions
                    if (responseCode == 201) {
                        for (AMRSMchPostnatal amrsMchPostnatal : amrsMchPostnatalList) {
                            amrsMchPostnatal.setResponseCode("201");
                            amrsMchPostnatalService.save(amrsMchPostnatal);
                        }
                    } else {
                        System.err.println("Failed to process visit ID: " + visitId + " | Status Code: " + responseCode);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing visit ID: " + visitId + " | " + e.getMessage());
                }
            }

        }
    }

    public static void mchDelivery(AMRSMchDeliveryService amrsMchDeliveryService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth)throws JSONException, IOException, SQLException {
        List<AMRSMchDelivery> amrsMchDeliveries = amrsMchDeliveryService.findByResponseCodeIsNull();

        if (!amrsMchDeliveries.isEmpty()) {
            // Use a Set to store unique patient IDs
            Set<String> visitIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

            // Collect unique patient IDs
            for (AMRSMchDelivery amrsMchDelivery : amrsMchDeliveries) {
                if (amrsMchDelivery.getResponseCode() == null && visitIdSet.add(amrsMchDelivery.getVisitId())) {
                    distinctVisitIds.add(amrsMchDelivery.getVisitId());
                }
            }

            System.out.println("list of distinct clients " + distinctVisitIds);
            for (String visitId : distinctVisitIds) {
                System.out.println("Processing visit ID: " + visitId);

                List<AMRSMchDelivery> amrsMchDeliveryList = amrsMchDeliveryService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                Set<String> uniqueObservations = new HashSet<>();
                String kenyaemrPatientUuid = "";
                String kenyaemrVisitUuid = "";
                String obsDatetime = "";

                // Prepare JSON observations
                for(int x = 0; x< amrsMchDeliveryList.size(); x++) {
                    kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsMchDeliveryList.get(x).getPatientId());
                    kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
                    obsDatetime = amrsMchDeliveryList.get(x).getObsDateTime();

                    JSONObject jsonObservationD = new JSONObject();
                    jsonObservationD.put("person", kenyaemrPatientUuid);
                    jsonObservationD.put("concept", amrsMchDeliveryList.get(x).getKenyaEmrConceptUuid());
                    jsonObservationD.put("value", amrsMchDeliveryList.get(x).getKenyaEmrValue());
                    jsonObservationD.put("obsDatetime", amrsMchDeliveryList.get(x).getObsDateTime());
                    if(!Objects.equals(amrsMchDeliveryList.get(x).getKenyaEmrValue(), "") && !Objects.equals(amrsMchDeliveryList.get(x).getKenyaEmrConceptUuid(), "") ) {
                        // Convert JSON object to string to ensure uniqueness
                        String jsonString = jsonObservationD.toString();
                        if (!uniqueObservations.contains(jsonString)) {
                            uniqueObservations.add(jsonString); // Add to the set
                            System.out.println("JSON OBSERVATION IS :" + x + jsonString);

                            jsonObservations.put(jsonObservationD); // Add to the JSON array
                            // Add to the JSON array
                        }
                    }

                }

                JSONObject jsonEncounter = new JSONObject();

                jsonEncounter.put("form", "496c7cc3-0eea-4e84-a04c-2292949e2f7f");
                jsonEncounter.put("patient", kenyaemrPatientUuid);
                jsonEncounter.put("obs", jsonObservations);
                jsonEncounter.put("visit", kenyaemrVisitUuid);
                jsonEncounter.put("encounterDatetime", obsDatetime);
                jsonEncounter.put("encounterType", "c6d09e05-1f25-4164-8860-9f32c5a02df0");
                jsonEncounter.put("location", "37f6bd8d-586a-4169-95fa-5781f987fe62");




                // Send API request
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());


                Request request = new Request.Builder()
                        .url(url + "encounter")
                        .method("POST", body)
                        .addHeader("Authorization", "Basic " + auth)
                        .addHeader("Content-Type", "application/json")
                        .build();

                System.out.println("Payload is Here "+ jsonEncounter );

                try (Response response = client.newCall(request).execute()) {
                    assert response.body() != null;
                    String responseBody = response.body().string();
                    int responseCode = response.code();

                    System.out.println("Response: " + responseBody + " | Status Code: " + responseCode);

                    // Update response code for successful submissions
                    if (responseCode == 201) {
                        for (AMRSMchDelivery amrsMchDelivery : amrsMchDeliveryList) {
                            amrsMchDelivery.setResponseCode("201");
                            amrsMchDeliveryService.save(amrsMchDelivery);
                        }
                    } else {
                        System.err.println("Failed to process visit ID: " + visitId + " | Status Code: " + responseCode);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing visit ID: " + visitId + " | " + e.getMessage());
                }
            }

        }
    }

    public static void processEac(AMRSEacService amrsEacService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String KenyaEMRlocationUuid, String url, String auth) throws JSONException, IOException {
      List<AMRSEac> amrsModelList = amrsEacService.findByResponseCodeIsNull();
      if (!amrsModelList.isEmpty()) {
        // Use a Set to store unique encounter IDs
        Set<String> visistIdSet = new HashSet<>();
        List<String> distinctVisitIds = new ArrayList<>();

        // Loop through the list
        for (AMRSEac amrsEac : amrsModelList) {
          if (amrsEac.getResponseCode() == null) {
            String visitId = amrsEac.getVisitId();
            // Add to the result list only if it hasn't been added already
            if (visistIdSet.add(visitId)) {
              distinctVisitIds.add(visitId);
            }
          }
        }

        for (String visitId : distinctVisitIds) {
          List<AMRSEac> amrsModelEncounters = amrsEacService.findByVisitId(visitId);
          JSONArray jsonObservations = new JSONArray();
          String patientuuid = "";
          String formuuid = "";
          String encounteruuid = "";
          String encounterDatetime = "";
          String obsDatetime = "";
          String visituuid = amrsTranslater.kenyaemrVisitUuid(visitId);

          for (int x = 0; x < amrsModelEncounters.size(); x++) {
            String kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsModelEncounters.get(x).getPatientId());
            JSONObject jsonObservation = new JSONObject();
            String value = amrsModelEncounters.get(x).getKenyaEmrValue();
            obsDatetime = amrsModelEncounters.get(x).getObsDateTime();
            jsonObservation.put("person", kenyaemrPatientUuid);
            jsonObservation.put("concept", amrsModelEncounters.get(x).getKenyaEmrConceptUuid());
            jsonObservation.put("obsDatetime", obsDatetime);
            jsonObservation.put("value", value);
            jsonObservation.put("location", KenyaEMRlocationUuid);

            patientuuid = amrsTranslater.KenyaemrPatientUuid(amrsModelEncounters.get(x).getPatientId());
            formuuid = amrsModelEncounters.get(x).getKenyaemrFormUuid();
            encounteruuid = amrsModelEncounters.get(x).getKenyaemrEncounterTypeUuid();
            encounterDatetime = amrsModelEncounters.get(x).getKenyaEmrEncounterDateTime();

            if (!Objects.equals(amrsModelEncounters.get(0).getKenyaEmrValue(), "") && !Objects.equals(amrsModelEncounters.get(0).getKenyaEmrConceptUuid(), "")) {
              jsonObservations.put(jsonObservation);
            }
          }

          //Publish the data to KenyaEMR
          if (!Objects.equals(visituuid, "")) {
            JSONObject jsonEncounter = new JSONObject();
            jsonEncounter.put("form", formuuid);
            jsonEncounter.put("patient", patientuuid);
            jsonEncounter.put("encounterDatetime", encounterDatetime);
            jsonEncounter.put("encounterType", encounteruuid);
            jsonEncounter.put("location", KenyaEMRlocationUuid);
            jsonEncounter.put("visit", visituuid);
            jsonEncounter.put("obs", jsonObservations);
            System.out.println("Payload for is here " + jsonEncounter.toString());

            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("application/json");
            okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, jsonEncounter.toString());
            Request request = new Request.Builder()
              .url(url + "encounter")
              .method("POST", body)
              .addHeader("Authorization", "Basic " + auth)
              .addHeader("Content-Type", "application/json")
              .build();

            Response response = client.newCall(request).execute();
            String responseBody = response.body().string(); // Get the response as a string
            System.out.println("Response ndo hii " + responseBody + " More message " + response.message());

            String resBody = response.request().toString();
            int rescode = response.code();
            System.out.println("Response Code Hapa " + rescode);

            if (rescode == 201) {
              for (int x = 0; x < amrsModelEncounters.size(); x++) {
                AMRSEac at = amrsModelEncounters.get(x);
                at.setResponseCode(String.valueOf(rescode));
                at.setResponseCode("201");
                System.out.println("Imefika Hapa na data " + rescode);
                amrsEacService.save(at);
              }
            } else {
              for (int x = 0; x < amrsModelEncounters.size(); x++) {
                AMRSEac at = amrsModelEncounters.get(x);
                at.setResponseCode(String.valueOf(rescode));
                at.setResponseCode("400");
                System.out.println("Imefika Hapa na data " + rescode);
                amrsEacService.save(at);
              }
            }
          }
        }
      }
    }
}

