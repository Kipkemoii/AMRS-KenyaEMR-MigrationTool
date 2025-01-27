package ampath.co.ke.amrs_kenyaemr.tasks.payloads;

import ampath.co.ke.amrs_kenyaemr.methods.AMRSTranslater;
import ampath.co.ke.amrs_kenyaemr.models.AMRSHeiOutcome;
import ampath.co.ke.amrs_kenyaemr.service.AMRSHeiOutcomeService;
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

public class HeiOutcomePayload {
    public static void processHeiOutcome(AMRSHeiOutcomeService amrsHeiOutcomeService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String KenyaEMRlocationUuid, String url, String auth) throws JSONException, IOException {
        List<AMRSHeiOutcome> amrsHeiOutcomes = amrsHeiOutcomeService.findByResponseCodeIsNull();
        if (amrsHeiOutcomes.size() > 0) {
// Use a Set to store unique encounter IDs
            Set<String> visistIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

// Loop through the list
            for (AMRSHeiOutcome heiOutcome : amrsHeiOutcomes) {
                if (heiOutcome.getResponseCode() == null) {
                    String visitId = heiOutcome.getVisitId();
// Add to the result list only if it hasn't been added already
                    if (visistIdSet.add(visitId)) {
                        distinctVisitIds.add(visitId);
                    }
                }
            }

            for (String visitId : distinctVisitIds) {
                System.out.println("VisitId ID for GreenCard " + visitId);
                List<AMRSHeiOutcome> amrsHeiOutcomesEncounters = amrsHeiOutcomeService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                String patientuuid = "";
                String formuuid = "";
                String encounteruuid = "";
                String encounterDatetime = "";
                String obsDatetime = "";
                String visituuid = amrsTranslater.kenyaemrVisitUuid(visitId);
// String locationuuid= amrsTranslater.location()

                for (int x = 0; x < amrsHeiOutcomesEncounters.size(); x++) {
                    String kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsHeiOutcomesEncounters.get(x).getPatientId());
                    JSONObject jsonObservation = new JSONObject();
                    String value = amrsHeiOutcomesEncounters.get(x).getKenyaEmrValue();
                    obsDatetime = amrsHeiOutcomesEncounters.get(x).getObsDateTime();
                    jsonObservation.put("person", kenyaemrPatientUuid);
                    jsonObservation.put("concept", amrsHeiOutcomesEncounters.get(x).getKenyaEmrConceptUuid());
                    jsonObservation.put("obsDatetime", obsDatetime);
                    jsonObservation.put("value", value);
                    jsonObservation.put("location", KenyaEMRlocationUuid);

                    patientuuid = amrsTranslater.KenyaemrPatientUuid(amrsHeiOutcomesEncounters.get(x).getPatientId());
                    formuuid = amrsHeiOutcomesEncounters.get(x).getKenyaemrFormUuid();
                    encounteruuid = amrsHeiOutcomesEncounters.get(x).getKenyaemrEncounterTypeUuid();
                    encounterDatetime = amrsHeiOutcomesEncounters.get(x).getKenyaEmrEncounterDateTime();

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
                        for (int x = 0; x < amrsHeiOutcomesEncounters.size(); x++) {
                            AMRSHeiOutcome at = amrsHeiOutcomesEncounters.get(x);
                            at.setResponseCode(String.valueOf(rescode));
                            at.setResponseCode("201");
                            System.out.println("Imefika Hapa na data " + rescode);
                            amrsHeiOutcomeService.save(at);
                        }
                    }else{
                        for (int x = 0; x < amrsHeiOutcomesEncounters.size(); x++) {
                            AMRSHeiOutcome at = amrsHeiOutcomesEncounters.get(x);
                            at.setResponseCode(String.valueOf(rescode));
                            at.setResponseCode("400");
                            System.out.println("Imefika Hapa na data " + rescode);
                            amrsHeiOutcomeService.save(at);
                        }
                    }
                }
            }
        }
    }
}

