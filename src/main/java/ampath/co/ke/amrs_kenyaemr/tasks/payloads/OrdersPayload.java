package ampath.co.ke.amrs_kenyaemr.tasks.payloads;

import ampath.co.ke.amrs_kenyaemr.methods.AMRSTranslater;
import ampath.co.ke.amrs_kenyaemr.models.AMRSOrders;
import ampath.co.ke.amrs_kenyaemr.models.AMRSPatients;
import ampath.co.ke.amrs_kenyaemr.models.AMRSVisits;
import ampath.co.ke.amrs_kenyaemr.service.AMRSOrderService;
import ampath.co.ke.amrs_kenyaemr.service.AMRSPatientServices;
import ampath.co.ke.amrs_kenyaemr.service.AMRSVisitService;
import okhttp3.*;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;

public class OrdersPayload {
    public static void orders(String KenyaEMRlocationUuid,AMRSOrderService amrsOrderService, AMRSPatientServices amrsPatientServices, AMRSVisitService amrsVisitService, AMRSTranslater amrsTranslater, String url, String auth) throws JSONException, IOException {
        List<AMRSOrders> amrsOrders = amrsOrderService.findByResponseCodeIsNull();

        if(!amrsOrders.isEmpty()) {

            System.out.println("Size ndo hii "+ amrsOrders.size());

            for (int x = 0; x < amrsOrders.size(); x++) {
                AMRSOrders amrsOrders1 = amrsOrders.get(x);
                if(amrsOrders1.getResponseCode() == null) {

                    List<AMRSPatients> patientsList = amrsPatientServices.getByPatientID(amrsOrders1.getPatientId());
                    System.out.println(" Patient Iko  "+ patientsList.size()+ " Patient ID "+ amrsOrders1.getPatientId());
                    if (!patientsList.isEmpty()) {
                        //String pid = patientsList.get(0).getKenyaemrpatientUUID();
                        String patintUUID = amrsTranslater.KenyaemrPatientUuid(amrsOrders1.getPatientId());
                        Integer kenyaemrOrderId = amrsOrders.get(x).getKenyaemrOrderId();
                        String orderNumber = amrsOrders.get(x).getOrderNumber();
                        Integer orderType = amrsOrders.get(x).getOrderTypeId();
                        String urgency = amrsOrders.get(x).getUrgency();
                        String orderReason = amrsOrders.get(x).getOrderReason();
                        String concept_uuid = amrsOrders.get(x).getKenyaemrConceptUuid();
                        String visitid = amrsOrders.get(x).getVisitId();
                        String dateOrder = amrsOrders.get(x).getDateOrdered();
                        String etypeuuid = "e1406e88-e9a9-11e8-9f32-f2801f1b9fd1";
                        String formuuid = "2cdeded1-3f69-3bda-beff-1ed2ead94eaf";
                        String visituuid = amrsTranslater.kenyaemrVisitUuid(visitid);
                        List<AMRSVisits> amrsVisits = amrsVisitService.findByVisitID(visitid);
                        if (amrsVisits.size() > 0) {


                        ///////////////////////////////////////////////////////
                        JSONObject jsonEncounter = new JSONObject();
                        jsonEncounter.put("form", formuuid);
                        jsonEncounter.put("patient", patintUUID);
                        jsonEncounter.put("encounterDatetime", amrsVisits.get(0).getDateStarted());
                        jsonEncounter.put("encounterType", etypeuuid);
                        jsonEncounter.put("location", KenyaEMRlocationUuid);
                        jsonEncounter.put("visit", visituuid);
                        if (patintUUID != null && concept_uuid != null && concept_uuid !="12AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") {
                            System.out.println("Payload for is here " + jsonEncounter.toString());
                            OkHttpClient clientt = new OkHttpClient();
                            MediaType mediaTypee = MediaType.parse("application/json");
                            RequestBody bodyy = RequestBody.create(mediaTypee, jsonEncounter.toString());
                            Request requestt = new Request.Builder()
                                    .url(url + "encounter")
                                    .method("POST", bodyy)
                                    .addHeader("Authorization", "Basic " + auth)
                                    .addHeader("Content-Type", "application/json")
                                    .build();

                            Response responsee = clientt.newCall(requestt).execute();
                            String responseBodyy = responsee.body().string(); // Get the response as a string
                            System.out.println("Response ndo hii " + responseBodyy + " More message " + responsee.message());
                            JSONObject jsonObjectt = new JSONObject(responseBodyy);
                            String encounterUUID = jsonObjectt.getString("uuid");
                            String resBodyy = responsee.request().toString();
                            int rescodee = responsee.code();

                            // String encounterUUID = "";

                            //////////////////////////////////////////////////////

                            String result = amrsOrders.get(x).getFinalOrderResult();
                            // int result = (int) Double.parseDouble(amrsOrders.get(x).getFinalOrderResult());
                            String resultsDate = amrsOrders.get(x).getDateOrdered();

                            String orderAction = amrsOrders.get(x).getOrderAction();

                            if (concept_uuid != null && concept_uuid != "12AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") {
                                JSONObject jsonOrder = new JSONObject();
                                jsonOrder.put("urgency", urgency);
                                jsonOrder.put("orderer", "ae01b8ff-a4cc-4012-bcf7-72359e852e14");
                                jsonOrder.put("orderReason", orderReason);
                                jsonOrder.put("careSetting", "OUTPATIENT");
                                jsonOrder.put("patient", patintUUID);
                                jsonOrder.put("concept", concept_uuid);
                                jsonOrder.put("encounter", encounterUUID);
                                jsonOrder.put("action", orderAction);
                                jsonOrder.put("dateActivated", resultsDate);
                                jsonOrder.put("type", "testorder");
//                            jsonOrder.put("dateStopped",resultsDate);
                                System.out.println("---------------------------------------------");
                                System.out.println(jsonOrder.toString());
                                System.out.println("----------------------------------------------");
                                if (encounterUUID != null) {
                                    System.out.println("XXXXXXXXXXXXXXXXXXXXXXXX---------------" + amrsTranslater.KenyaemrPatientUuid(amrsOrders.get(0).getPatientId()));
                                } else {
                                    System.out.println("XXXXXXXXXXXXXXXXXXXXXXXX- Missing Endcounter--------------" + amrsTranslater.KenyaemrPatientUuid(amrsOrders.get(0).getPatientId()));

                                }
                                    OkHttpClient client = new OkHttpClient();
                                    MediaType mediaType = MediaType.parse("application/json");
                                    RequestBody body = RequestBody.create(mediaType, jsonOrder.toString());
                                    String bodyString = body.toString();
                                    Request request = new Request.Builder()
                                            .url(url + "order")
                                            .method("POST", body)
                                            .addHeader("Authorization", "Basic " + auth)
                                            .addHeader("Content-Type", "application/json")
                                            .build();

                                    Response response = client.newCall(request).execute();
                                    String responseBody = response.body().string(); // Get the response as a string
//                        System.out.println("Response ndo hii " + responseBody + " More message " + response.message() + " reponse code " + response.code());
                                    JSONObject jsonObject = new JSONObject(responseBody);
                                    System.out.println("Response ndo hii " + response.message());
//                        System.out.println("Response Payload " + jsonOrder.toString());
                                    if (response.code() == 201) {
                                        String orderUuid = jsonObject.getString("uuid");
                                        JSONObject orderResultsObj = new JSONObject();
                                        orderResultsObj.put("person", amrsTranslater.KenyaemrPatientUuid(amrsOrders.get(x).getPatientId()));
                                        orderResultsObj.put("concept", concept_uuid);
                                        orderResultsObj.put("encounter", encounterUUID);
                                        orderResultsObj.put("obsDatetime", resultsDate);
                                        orderResultsObj.put("value", result);
                                        orderResultsObj.put("order", orderUuid);
                                        amrsOrders1.setResponseCode("201");
                                        amrsOrderService.save(amrsOrders1);

                                        if (result != null) {
                                            if (ordersResults(orderResultsObj, amrsOrders1, amrsOrderService, url, auth)) {
                                                System.out.println("Results Object is here " + orderResultsObj.toString());
                                                // amrsOrders1.setKenyaemrOrderUuid(orderUuid);
                                                //amrsOrders1.setResponseCodeResults(String.valueOf(response.code()));
                                                amrsOrderService.save(amrsOrders1);
                                            }
                                        }

                                    } else {
                                        System.out.println("Response ndo hii " + response.code());
                                    }

                            }
                        }
                        else{
                            System.out.println("Missing Visit ID");
                        }
                        } /*else if (patintUUID != "") { */

                            /*

                            System.out.println("Payload for is here " + jsonEncounter.toString());
                            OkHttpClient clientt = new OkHttpClient();
                            MediaType mediaTypee = MediaType.parse("application/json");
                            RequestBody bodyy = RequestBody.create(mediaTypee, jsonEncounter.toString());
                            Request requestt = new Request.Builder()
                                    .url(url + "encounter")
                                    .method("POST", bodyy)
                                    .addHeader("Authorization", "Basic " + auth)
                                    .addHeader("Content-Type", "application/json")
                                    .build();

                            Response responsee = clientt.newCall(requestt).execute();
                            String responseBodyy = responsee.body().string(); // Get the response as a string
                            System.out.println("Response ndo hii " + responseBodyy + " More message " + responsee.message());
                            JSONObject jsonObjectt = new JSONObject(responseBodyy);
                            String encounterUUID = jsonObjectt.getString("uuid");
                            String resBodyy = responsee.request().toString();
                            int rescodee = responsee.code();

                            // String encounterUUID = "";

                            //////////////////////////////////////////////////////

                            String result = amrsOrders.get(x).getFinalOrderResult();
                            // int result = (int) Double.parseDouble(amrsOrders.get(x).getFinalOrderResult());
                            String resultsDate = amrsOrders.get(x).getDateOrdered();

                            String orderAction = amrsOrders.get(x).getOrderAction();
                            if (concept_uuid != null) {
                                JSONObject jsonOrder = new JSONObject();
                                jsonOrder.put("urgency", urgency);
                                jsonOrder.put("orderer", "ae01b8ff-a4cc-4012-bcf7-72359e852e14");
                                jsonOrder.put("orderReason", orderReason);
                                jsonOrder.put("careSetting", "OUTPATIENT");
                                jsonOrder.put("patient", patintUUID);
                                jsonOrder.put("concept", concept_uuid);
                                jsonOrder.put("encounter", encounterUUID);
                                jsonOrder.put("action", orderAction);
                                jsonOrder.put("dateActivated", resultsDate);
                                jsonOrder.put("type", "testorder");
//                            jsonOrder.put("dateStopped",resultsDate);
                                System.out.println("---------------------------------------------");
                                System.out.println(jsonOrder.toString());
                                System.out.println("----------------------------------------------");
                                if (encounterUUID != null) {
                                    System.out.println("XXXXXXXXXXXXXXXXXXXXXXXX---------------" + amrsOrders.get(0).getKenyaemrPatientUuid());
                                } else {
                                    System.out.println("XXXXXXXXXXXXXXXXXXXXXXXX- Missing Endcounter--------------" + amrsOrders.get(0).getKenyaemrPatientUuid());

                                }
                                OkHttpClient client = new OkHttpClient();
                                MediaType mediaType = MediaType.parse("application/json");
                                RequestBody body = RequestBody.create(mediaType, jsonOrder.toString());
                                String bodyString = body.toString();
                                Request request = new Request.Builder()
                                        .url(url + "order")
                                        .method("POST", body)
                                        .addHeader("Authorization", "Basic " + auth)
                                        .addHeader("Content-Type", "application/json")
                                        .build();
                                Response response = client.newCall(request).execute();
                                String responseBody = response.body().string(); // Get the response as a string
//                        System.out.println("Response ndo hii " + responseBody + " More message " + response.message() + " reponse code " + response.code());
                                JSONObject jsonObject = new JSONObject(responseBody);
                                System.out.println("Response ndo hii " + response);
//                        System.out.println("Response Payload " + jsonOrder.toString());
                                if (response.code() == 201) {
                                    String orderUuid = jsonObject.getString("uuid");
                                    JSONObject orderResultsObj = new JSONObject();
                                    orderResultsObj.put("person", amrsTranslater.KenyaemrPatientUuid(amrsOrders.get(x).getPatientId()));
                                    orderResultsObj.put("concept", concept_uuid);
                                    orderResultsObj.put("encounter", encounterUUID);
                                    orderResultsObj.put("obsDatetime", resultsDate);
                                    orderResultsObj.put("value", result);
                                    orderResultsObj.put("order", orderUuid);
                                    amrsOrders1.setResponseCode("201");
                                    amrsOrderService.save(amrsOrders1);

                                    if (result == null) {
                                        if (ordersResults(orderResultsObj, amrsOrders1, amrsOrderService, url, auth)) {
                                            System.out.println("Results Object is here " + orderResultsObj.toString());
                                            // amrsOrders1.setKenyaemrOrderUuid(orderUuid);
                                            //amrsOrders1.setResponseCodeResults(String.valueOf(response.code()));
                                            amrsOrderService.save(amrsOrders1);
                                        }
                                    }

                                } else {
                                    System.out.println("Response ndo hii " + response.code());
                                }
                            }

                            */
                        //}
                    } else {
                        System.out.println("Order not saved");
                    }

                }
            }
        }
    }

    public static boolean ordersResults(JSONObject orderResultObj, AMRSOrders a,AMRSOrderService as, String url, String auth) throws JSONException, IOException {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, orderResultObj.toString());
        String bodyString = body.toString();
        Request request = new Request.Builder()
                .url(url + "obs")
                .method("POST", body)
                .addHeader("Authorization", "Basic " + auth)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        String responseBody = response.body().string(); // Get the response as a string

        System.out.println("Obs Body ndo Hii "+ bodyString);
        JSONObject jsonObject = new JSONObject(responseBody);
        if (response.code() == 201) {
            a.setResponseCodeResults("201");
            as.save(a);
            return true;
        } else {
            System.out.println("failed saving observations " + response.code() + " response Body" + responseBody);
            return false;
        }

    }



}
