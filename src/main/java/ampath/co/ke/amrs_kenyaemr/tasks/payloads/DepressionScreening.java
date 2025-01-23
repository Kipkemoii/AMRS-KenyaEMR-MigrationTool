package ampath.co.ke.amrs_kenyaemr.tasks.payloads;

import ampath.co.ke.amrs_kenyaemr.methods.AMRSTranslater;
import ampath.co.ke.amrs_kenyaemr.models.AMRSDepressionScreening;
import ampath.co.ke.amrs_kenyaemr.models.AMRSTbScreening;
import ampath.co.ke.amrs_kenyaemr.service.AMRSDepressionScreeningService;
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

public class DepressionScreening {
    public static void processDepressionScreening(AMRSDepressionScreeningService amrsDepressionScreeningService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth) throws JSONException, IOException {
        List<AMRSDepressionScreening> amrsDepressionScreenings = amrsDepressionScreeningService.findByResponseCodeIsNull();
        if (amrsDepressionScreenings.size() > 0) {
            // Use a Set to store unique encounter IDs
            Set<String> visistIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

            // Loop through the list
            for (AMRSDepressionScreening depressionScreening : amrsDepressionScreenings) {
                if (depressionScreening.getResponseCode() == null) {
                    String visitId = depressionScreening.getVisitId();
                    // Add to the result list only if it hasn't been added already
                    if (visistIdSet.add(visitId)) {
                        distinctVisitIds.add(visitId);
                    }
                }
            }

            for (String visitId : distinctVisitIds) {
                System.out.println("VisitId ID for GreenCard " + visitId);
                List<AMRSDepressionScreening> amrsDepressionScreeningsEncounters = amrsDepressionScreeningService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                String patientuuid = "";
                String formuuid = "";
                String encounteruuid = "";
                String encounterDatetime = "";
                String obsDatetime = "";
                String visituuid = amrsTranslater.kenyaemrVisitUuid(visitId);
                // String locationuuid= amrsTranslater.location()


                for (int x = 0; x < amrsDepressionScreeningsEncounters.size(); x++) {
                    String kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsDepressionScreeningsEncounters.get(x).getPatientId());
                    JSONObject jsonObservation = new JSONObject();
                    String value = amrsDepressionScreeningsEncounters.get(x).getKenyaEmrValue();
                    String amrsValue = amrsDepressionScreeningsEncounters.get(x).getValue();
                    obsDatetime = amrsDepressionScreeningsEncounters.get(x).getObsDateTime();
                    jsonObservation.put("person", kenyaemrPatientUuid);
                    jsonObservation.put("concept", amrsDepressionScreeningsEncounters.get(x).getKenyaEmrConceptUuid());
                    jsonObservation.put("obsDatetime", obsDatetime);
                    jsonObservation.put("value", value);
                    jsonObservation.put("location", "37f6bd8d-586a-4169-95fa-5781f987fe62");
                    jsonObservations.put(jsonObservation);

                    patientuuid = amrsTranslater.KenyaemrPatientUuid(amrsDepressionScreeningsEncounters.get(x).getPatientId());
                    formuuid = amrsDepressionScreeningsEncounters.get(x).getKenyaemrFormUuid();
                    encounteruuid = amrsDepressionScreeningsEncounters.get(x).getKenyaemrEncounterTypeUuid();
                    encounterDatetime = amrsDepressionScreeningsEncounters.get(x).getKenyaEmrEncounterDateTime();


                }


                //Publish the data to KenyaEMR
                if (!Objects.equals(visituuid, "")) {
                    JSONObject jsonEncounter = new JSONObject();
                    jsonEncounter.put("form", formuuid);
                    jsonEncounter.put("patient", patientuuid);
                    jsonEncounter.put("encounterDatetime", encounterDatetime);
                    jsonEncounter.put("encounterType", encounteruuid);
                    jsonEncounter.put("location", "37f6bd8d-586a-4169-95fa-5781f987fe62");
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
                        for (int x = 0; x < amrsDepressionScreeningsEncounters.size(); x++) {
                            AMRSDepressionScreening at = amrsDepressionScreeningsEncounters.get(x);
                            at.setResponseCode(String.valueOf(rescode));
                            at.setResponseCode("201");
                            System.out.println("Imefika Hapa na data " + rescode);
                            amrsDepressionScreeningService.save(at);
                        }
                    }
                }
            }
        }
    }
}
