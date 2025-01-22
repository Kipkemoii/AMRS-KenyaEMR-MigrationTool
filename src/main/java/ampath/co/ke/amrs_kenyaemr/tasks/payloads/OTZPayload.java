package ampath.co.ke.amrs_kenyaemr.tasks.payloads;

import ampath.co.ke.amrs_kenyaemr.methods.AMRSTranslater;
import ampath.co.ke.amrs_kenyaemr.models.AMRSOtzActivity;
import ampath.co.ke.amrs_kenyaemr.models.AMRSOtzDiscontinuation;
import ampath.co.ke.amrs_kenyaemr.models.AMRSOtzEnrollment;
import ampath.co.ke.amrs_kenyaemr.service.AMRSOtzActivityService;
import ampath.co.ke.amrs_kenyaemr.service.AMRSOtzDiscontinuationService;
import ampath.co.ke.amrs_kenyaemr.service.AMRSOtzEnrollmentService;
import ampath.co.ke.amrs_kenyaemr.service.AMRSPatientServices;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class OTZPayload {
    public static void processOTZActivity(AMRSOtzActivityService amrsOtzActivityService, AMRSTranslater amrsTranslater,String KenyaEMRlocationUuid, String url, String auth) throws JSONException, IOException {
        List<AMRSOtzActivity> amrsOtzActivities = amrsOtzActivityService.findByResponseCodeIsNull();
        if (amrsOtzActivities.size() > 0) {
            // Use a Set to store unique encounter IDs
            Set<String> visistIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

            // Loop through the list
            for (AMRSOtzActivity otzActivity : amrsOtzActivities) {
                if (otzActivity.getResponseCode() == null) {
                    String visitId = otzActivity.getVisitId();
                    // Add to the result list only if it hasn't been added already
                    if (visistIdSet.add(visitId)) {
                        distinctVisitIds.add(visitId);
                    }
                }
            }

            for (String visitId : distinctVisitIds) {
                System.out.println("VisitId ID for GreenCard " + visitId);
                List<AMRSOtzActivity> amrsOtzActivityEncounters = amrsOtzActivityService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                String patientuuid = "";
                String formuuid = "";
                String encounteruuid = "";
                String encounterDatetime = "";
                String obsDatetime = "";
                String visituuid = amrsTranslater.kenyaemrVisitUuid(visitId);
                // String locationuuid= amrsTranslater.location()


                for (int x = 0; x < amrsOtzActivityEncounters.size(); x++) {
                    String kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsOtzActivityEncounters.get(x).getPatientId());
                    JSONObject jsonObservation = new JSONObject();
                    String value = amrsOtzActivityEncounters.get(x).getKenyaEmrValue();
                    obsDatetime = amrsOtzActivityEncounters.get(x).getObsDateTime();
                    jsonObservation.put("person", kenyaemrPatientUuid);
                    jsonObservation.put("concept", amrsOtzActivityEncounters.get(x).getKenyaEmrConceptUuid());
                    jsonObservation.put("obsDatetime", obsDatetime);
                    jsonObservation.put("value", value);
                    jsonObservation.put("location", KenyaEMRlocationUuid);

                    patientuuid = amrsTranslater.KenyaemrPatientUuid(amrsOtzActivityEncounters.get(x).getPatientId());
                    formuuid = amrsOtzActivityEncounters.get(x).getKenyaemrFormUuid();
                    encounteruuid = amrsOtzActivityEncounters.get(x).getKenyaemrEncounterTypeUuid();
                    encounterDatetime = amrsOtzActivityEncounters.get(x).getKenyaEmrEncounterDateTime();

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
                        for (int x = 0; x < amrsOtzActivityEncounters.size(); x++) {
                            AMRSOtzActivity at = amrsOtzActivityEncounters.get(x);
                            at.setResponseCode(String.valueOf(rescode));
                            at.setResponseCode("201");
                            System.out.println("Imefika Hapa na data " + rescode);
                            amrsOtzActivityService.save(at);
                        }
                    }else{
                        for (int x = 0; x < amrsOtzActivityEncounters.size(); x++) {
                            AMRSOtzActivity at = amrsOtzActivityEncounters.get(x);
                            at.setResponseCode(String.valueOf(rescode));
                            at.setResponseCode("400");
                            System.out.println("Imefika Hapa na data " + rescode);
                            amrsOtzActivityService.save(at);
                        }
                    }
                }
            }
        }
    }
    public static void processOTZDiscontinuation(AMRSOtzDiscontinuationService amrsOtzDiscontinuationService, AMRSTranslater amrsTranslater, String KenyaEMRlocationUuid, String url, String auth) throws JSONException, IOException {
        List<AMRSOtzDiscontinuation> amrsOtzDiscontinuations = amrsOtzDiscontinuationService.findByResponseCodeIsNull();
        if (!amrsOtzDiscontinuations.isEmpty()) {
            // Use a Set to store unique encounter IDs
            Set<String> visistIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

            // Loop through the list
            for (AMRSOtzDiscontinuation otzDiscontinuation : amrsOtzDiscontinuations) {
                if (otzDiscontinuation.getResponseCode() == null) {
                    String visitId = otzDiscontinuation.getVisitId();
                    // Add to the result list only if it hasn't been added already
                    if (visistIdSet.add(visitId)) {
                        distinctVisitIds.add(visitId);
                    }
                }
            }

            for (String visitId : distinctVisitIds) {
                System.out.println("VisitId ID for GreenCard " + visitId);
                List<AMRSOtzDiscontinuation> amrsOtzDiscontinuationEncounters = amrsOtzDiscontinuationService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                String patientuuid = "";
                String formuuid = "";
                String encounteruuid = "";
                String encounterDatetime = "";
                String obsDatetime = "";
                String visituuid = amrsTranslater.kenyaemrVisitUuid(visitId);
                // String locationuuid= amrsTranslater.location()


                for (int x = 0; x < amrsOtzDiscontinuationEncounters.size(); x++) {
                    String kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsOtzDiscontinuationEncounters.get(x).getPatientId());
                    JSONObject jsonObservation = new JSONObject();
                    String value = amrsOtzDiscontinuationEncounters.get(x).getKenyaEmrValue();
                    obsDatetime = amrsOtzDiscontinuationEncounters.get(x).getObsDateTime();
                    jsonObservation.put("person", kenyaemrPatientUuid);
                    jsonObservation.put("concept", amrsOtzDiscontinuationEncounters.get(x).getKenyaEmrConceptUuid());
                    jsonObservation.put("obsDatetime", obsDatetime);
                    jsonObservation.put("value", value);
                    jsonObservation.put("location", KenyaEMRlocationUuid);

                    patientuuid = amrsTranslater.KenyaemrPatientUuid(amrsOtzDiscontinuationEncounters.get(x).getPatientId());
                    formuuid = amrsOtzDiscontinuationEncounters.get(x).getKenyaemrFormUuid();
                    encounteruuid = amrsOtzDiscontinuationEncounters.get(x).getKenyaemrEncounterTypeUuid();
                    encounterDatetime = amrsOtzDiscontinuationEncounters.get(x).getKenyaEmrEncounterDateTime();

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
                        for (int x = 0; x < amrsOtzDiscontinuationEncounters.size(); x++) {
                            AMRSOtzDiscontinuation at = amrsOtzDiscontinuationEncounters.get(x);
                            at.setResponseCode(String.valueOf(rescode));
                            at.setResponseCode("201");
                            System.out.println("Imefika Hapa na data " + rescode);
                            amrsOtzDiscontinuationService.save(at);
                        }
                    }else{
                        for (int x = 0; x < amrsOtzDiscontinuationEncounters.size(); x++) {
                            AMRSOtzDiscontinuation at = amrsOtzDiscontinuationEncounters.get(x);
                            at.setResponseCode(String.valueOf(rescode));
                            at.setResponseCode("400");
                            System.out.println("Imefika Hapa na data " + rescode);
                            amrsOtzDiscontinuationService.save(at);
                        }
                    }
                }
            }
        }
    }
    public static void processOTZEnrollment(AMRSOtzEnrollmentService amrsOtzEnrollmentService, AMRSTranslater amrsTranslater, String KenyaEMRlocationUuid, String url, String auth) throws JSONException, IOException {
        List<AMRSOtzEnrollment> amrsOtzEnrollments = amrsOtzEnrollmentService.findByResponseCodeIsNull();
        if (!amrsOtzEnrollments.isEmpty()) {
            // Use a Set to store unique encounter IDs
            Set<String> visistIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

            // Loop through the list
            for (AMRSOtzEnrollment otzEnrollment : amrsOtzEnrollments) {
                if (otzEnrollment.getResponseCode() == null) {
                    String visitId = otzEnrollment.getVisitId();
                    // Add to the result list only if it hasn't been added already
                    if (visistIdSet.add(visitId)) {
                        distinctVisitIds.add(visitId);
                    }
                }
            }

            for (String visitId : distinctVisitIds) {
                System.out.println("VisitId ID for GreenCard " + visitId);
                List<AMRSOtzEnrollment> amrsOtzEnrollmentEncounters = amrsOtzEnrollmentService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                String patientuuid = "";
                String formuuid = "";
                String encounteruuid = "";
                String encounterDatetime = "";
                String obsDatetime = "";
                String visituuid = amrsTranslater.kenyaemrVisitUuid(visitId);
                // String locationuuid= amrsTranslater.location()


                for (int x = 0; x < amrsOtzEnrollmentEncounters.size(); x++) {
                    String kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsOtzEnrollmentEncounters.get(x).getPatientId());
                    JSONObject jsonObservation = new JSONObject();
                    String value = amrsOtzEnrollmentEncounters.get(x).getKenyaEmrValue();
                    obsDatetime = amrsOtzEnrollmentEncounters.get(x).getObsDateTime();
                    jsonObservation.put("person", kenyaemrPatientUuid);
                    jsonObservation.put("concept", amrsOtzEnrollmentEncounters.get(x).getKenyaEmrConceptUuid());
                    jsonObservation.put("obsDatetime", obsDatetime);
                    jsonObservation.put("value", value);
                    jsonObservation.put("location", KenyaEMRlocationUuid);

                    patientuuid = amrsTranslater.KenyaemrPatientUuid(amrsOtzEnrollmentEncounters.get(x).getPatientId());
                    formuuid = amrsOtzEnrollmentEncounters.get(x).getKenyaemrFormUuid();
                    encounteruuid = amrsOtzEnrollmentEncounters.get(x).getKenyaemrEncounterTypeUuid();
                    encounterDatetime = amrsOtzEnrollmentEncounters.get(x).getKenyaEmrEncounterDateTime();

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
                        for (int x = 0; x < amrsOtzEnrollmentEncounters.size(); x++) {
                            AMRSOtzEnrollment at = amrsOtzEnrollmentEncounters.get(x);
                            at.setResponseCode(String.valueOf(rescode));
                            at.setResponseCode("201");
                            System.out.println("Imefika Hapa na data " + rescode);
                            amrsOtzEnrollmentService.save(at);
                        }
                    }else{
                        for (int x = 0; x < amrsOtzEnrollmentEncounters.size(); x++) {
                            AMRSOtzEnrollment at = amrsOtzEnrollmentEncounters.get(x);
                            at.setResponseCode(String.valueOf(rescode));
                            at.setResponseCode("400");
                            System.out.println("Imefika Hapa na data " + rescode);
                            amrsOtzEnrollmentService.save(at);
                        }
                    }
                }
            }
        }
    }
}
