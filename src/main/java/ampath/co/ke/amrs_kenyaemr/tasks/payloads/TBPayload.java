package ampath.co.ke.amrs_kenyaemr.tasks.payloads;

import ampath.co.ke.amrs_kenyaemr.methods.AMRSTranslater;
import ampath.co.ke.amrs_kenyaemr.models.AMRSTbScreening;
import ampath.co.ke.amrs_kenyaemr.service.AMRSPatientServices;
import ampath.co.ke.amrs_kenyaemr.service.AMRSTbScreeningService;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class TBPayload {
    public static void processTBScreening(AMRSTbScreeningService amrsTbScreeningService, AMRSTranslater amrsTranslater, String KenyaEMRlocationUuid, String url, String auth) throws JSONException, IOException {
        List<AMRSTbScreening> amrsTbScreenings = amrsTbScreeningService.findByResponseCodeIsNull();
        if (amrsTbScreenings.size() > 0) {
            // Use a Set to store unique encounter IDs
            Set<String> visistIdSet = new HashSet<>();
            List<String> distinctVisitIds = new ArrayList<>();

            // Loop through the list
            for (AMRSTbScreening tbScreening : amrsTbScreenings) {
                if (tbScreening.getResponseCode() == null) {
                    String visitId = tbScreening.getVisitId();
                    // Add to the result list only if it hasn't been added already
                    if (visistIdSet.add(visitId)) {
                        distinctVisitIds.add(visitId);
                    }
                }
            }

            for (String visitId : distinctVisitIds) {
                System.out.println("VisitId ID for GreenCard " + visitId);
                List<AMRSTbScreening> amrsTbScreeningEncounters = amrsTbScreeningService.findByVisitId(visitId);
                JSONArray jsonObservations = new JSONArray();
                String patientuuid = "";
                String formuuid = "";
                String encounteruuid = "";
                String encounterDatetime = "";
                String obsDatetime = "";
                String visituuid = amrsTranslater.kenyaemrVisitUuid(visitId);
                // String locationuuid= amrsTranslater.location()


                for (int x = 0; x < amrsTbScreeningEncounters.size(); x++) {
                    String kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(amrsTbScreeningEncounters.get(x).getPatientId());
                    JSONObject jsonObservation = new JSONObject();
                    String value = amrsTbScreeningEncounters.get(x).getKenyaEmrValue();
                    String amrsValue = amrsTbScreeningEncounters.get(x).getValue();
                    obsDatetime = amrsTbScreeningEncounters.get(x).getObsDateTime();
                    jsonObservation.put("person", kenyaemrPatientUuid);
                    jsonObservation.put("concept", amrsTbScreeningEncounters.get(x).getKenyaEmrConceptUuid());
                    jsonObservation.put("obsDatetime", obsDatetime);
                    jsonObservation.put("value", value);
                    jsonObservation.put("location", KenyaEMRlocationUuid);
                    jsonObservations.put(jsonObservation);

                    patientuuid = amrsTranslater.KenyaemrPatientUuid(amrsTbScreeningEncounters.get(x).getPatientId());
                    formuuid = amrsTbScreeningEncounters.get(x).getKenyaemrFormUuid();
                    encounteruuid = amrsTbScreeningEncounters.get(x).getKenyaemrEncounterTypeUuid();
                    encounterDatetime = amrsTbScreeningEncounters.get(x).getKenyaEmrEncounterDateTime();


                }

                //default screening questions to NO
                // cough of any duration
                JSONObject jsonObservationEntry = new JSONObject();
                jsonObservationEntry.put("person", patientuuid);
                jsonObservationEntry.put("concept", "1729AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                jsonObservationEntry.put("value", "1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                jsonObservationEntry.put("location", KenyaEMRlocationUuid);
                jsonObservationEntry.put("obsDatetime", obsDatetime);
                jsonObservations.put(jsonObservationEntry);

                // fever No
                JSONObject jsonObservationFever = new JSONObject();
                jsonObservationFever.put("person", patientuuid);
                jsonObservationFever.put("concept", "1729AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                jsonObservationFever.put("value", "1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                jsonObservationFever.put("location", KenyaEMRlocationUuid);
                jsonObservationFever.put("obsDatetime", obsDatetime);
                jsonObservations.put(jsonObservationFever);

                // noticeable weight loss NO
                JSONObject jsonObservationWeightLoss = new JSONObject();
                jsonObservationWeightLoss.put("person", patientuuid);
                jsonObservationWeightLoss.put("concept", "1729AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                jsonObservationWeightLoss.put("value", "1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                jsonObservationWeightLoss.put("location", KenyaEMRlocationUuid);
                jsonObservationWeightLoss.put("obsDatetime", obsDatetime);
                jsonObservations.put(jsonObservationWeightLoss);

                // noticeable night sweats NO
                JSONObject jsonObservationNightSweats = new JSONObject();
                jsonObservationNightSweats.put("person", patientuuid);
                jsonObservationNightSweats.put("concept", "1729AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                jsonObservationNightSweats.put("value", "1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                jsonObservationNightSweats.put("location", KenyaEMRlocationUuid);
                jsonObservationNightSweats.put("obsDatetime", obsDatetime);
                jsonObservations.put(jsonObservationNightSweats);


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
                        for (int x = 0; x < amrsTbScreeningEncounters.size(); x++) {
                            AMRSTbScreening at = amrsTbScreeningEncounters.get(x);
                            at.setResponseCode(String.valueOf(rescode));
                            at.setResponseCode("201");
                            System.out.println("Imefika Hapa na data " + rescode);
                            amrsTbScreeningService.save(at);
                        }
                    }else{
                        for (int x = 0; x < amrsTbScreeningEncounters.size(); x++) {
                            AMRSTbScreening at = amrsTbScreeningEncounters.get(x);
                            at.setResponseCode(String.valueOf(rescode));
                            at.setResponseCode("400");
                            System.out.println("Imefika Hapa na data " + rescode);
                            amrsTbScreeningService.save(at);
                        }
                    }
                }
            }
        }
    }


}
