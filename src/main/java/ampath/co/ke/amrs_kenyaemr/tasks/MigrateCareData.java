package ampath.co.ke.amrs_kenyaemr.tasks;

import ampath.co.ke.amrs_kenyaemr.methods.AMRSTranslater;
import ampath.co.ke.amrs_kenyaemr.methods.AMRSSamples;
import ampath.co.ke.amrs_kenyaemr.models.*;
import ampath.co.ke.amrs_kenyaemr.service.*;
import ampath.co.ke.amrs_kenyaemr.tasks.payloads.*;
import org.json.JSONException;

import java.io.IOException;
import java.sql.*;
import java.text.ParseException;


import java.util.List;
import java.util.Objects;
import java.util.UUID;


public class MigrateCareData {

  public static void programs(String server, String username, String password, AMRSProgramService amrsProgramService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String KenyaEMRlocationUuid, String url, String auth) throws SQLException, JSONException, ParseException, IOException {
    List<AMRSPrograms> amrsProgramss = amrsProgramService.findFirstByOrderByIdDesc();

    List<String> stringPIDsList = amrsPatientServices.getAllPatientID();
    String pid = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);
    System.out.println("PtientIDs " + pid);

    String sql = "";
    if (!amrsProgramss.isEmpty()) {
      System.out.println("Latest program " + amrsProgramss.get(0).getPatientId());
      String ppid = amrsProgramss.get(0).getAmrsPatientProgramID();
      sql = "select pp.patient_program_id, pp.patient_id, \n" +
        "       p.name,\n" +
        "       p.uuid program_uuid,\n" +
        "       pp.location_id,\n" +
        "       l.uuid location_uuid,\n" +
        "       p.concept_id,\n" +
        "       pp.date_enrolled,\n" +
        "       pp.date_completed,\n" +
        "       p.program_id\n" +
        "       from amrs.patient_program pp\n" +
        "       inner join amrs.encounter e on e.patient_id=pp.patient_id\n" +
        "       inner join amrs.program p on p.program_id=pp.program_id\n" +
        "       inner join amrs.location l on l.location_id = e.location_id\n" +
        "      and e.patient_id in (" + pid + ")  \n" + //and e. patient_id in ('1224605,1222698') // pp.patient_program_id>=" + ppid + " and
        "       group by  pp.patient_id,p.concept_id  order by pp.patient_program_id asc";

    } else {
      sql = "select pp.patient_program_id, pp.patient_id, \n" +
        "       p.name,\n" +
        "       p.uuid program_uuid,\n" +
        "       pp.location_id,\n" +
        "       l.uuid location_uuid,\n" +
        "       p.concept_id,\n" +
        "       pp.date_enrolled,\n" +
        "       pp.date_completed,\n" +
        "       p.program_id\n" +
        "       from amrs.patient_program pp\n" +
        "       inner join amrs.encounter e on e.patient_id=pp.patient_id\n" +
        "       inner join amrs.program p on p.program_id=pp.program_id\n" +
        "       inner join amrs.location l on l.location_id = e.location_id\n" +
        "       and e.patient_id in (" + pid + ") \n" + //and e. patient_id in ('1224605,1222698')
        "       group by  pp.patient_id,p.concept_id  order by pp.patient_program_id asc";
    }
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String locationId = rs.getString("location_id");
      String programName = rs.getString("name");
      String programUuid = rs.getString("program_uuid");
      String locationUuid = rs.getString("location_uuid");
      String conceptId = rs.getString("concept_id");
      String dateEnrolled = rs.getString("date_enrolled");
      String dateCompleted = rs.getString("date_completed");
      String programId = rs.getString("program_id");
      String patientProgramId = rs.getString("patient_program_id");

      // List<AMRSPrograms> patientsList = amrsProgramService.getprogramByLocation(rs.getString(2),parentUUID);
      //  List<AMRSPatients> amrsPatients = amrsPatientServices.getByPatientID(patientId);
      String person_id = patientId;
      List<AMRSPrograms> amrsPrograms = amrsProgramService.findByPatientIdAndProgramID(patientId, Integer.parseInt(programId));
      if (amrsPrograms.isEmpty()) {
        String kenyaemr_progra_uuid = Mappers.programs(programUuid);
        AMRSPrograms ae = new AMRSPrograms();
        ae.setProgramUUID(programUuid);
        ae.setPatientId(person_id);
        //ae.setParentLocationUuid(parentUUID);
        ae.setLocationId(locationId);
        ae.setUUID(String.valueOf(UUID.randomUUID()));
        ae.setProgramID(Integer.parseInt(programId));
        ae.setConceptId(conceptId);
        ae.setProgramName(programName);
        ae.setDateEnrolled(dateEnrolled);
        ae.setDateCompleted(dateCompleted);
        ae.setPatientKenyaemrUuid(amrsTranslater.KenyaemrPatientUuid(patientId));
        ae.setKenyaemrProgramUuid(Mappers.programs(programUuid));
        ae.setAmrsPatientProgramID(patientProgramId);
        amrsProgramService.save(ae);
      }

      //Migate Programs
      //  CareOpenMRSPayload.programs(amrsProgramService,amrsTranslater, url, auth);

    }

    //Migate Programs
    CareOpenMRSPayload.programs(amrsProgramService, amrsTranslater, KenyaEMRlocationUuid, url, auth);
  }

  public static void encounters(String server, String username, String password, String locations, String parentUUID, AMRSEncounterService amrsEncounterService, AMRSPatientServices amrsPatientServices, AMRSVisitService amrsVisitService, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    List<AMRSEncounters> amrsEncounters = amrsEncounterService.findFirstByOrderByIdDesc();

    List<AMRSPatients> amrsPatientsList = amrsPatientServices.getAll();
    String pidss = "";
    for (int y = 0; y < amrsPatientsList.size(); y++) {
      pidss += amrsPatientsList.get(y).getPersonId() + ",";
    }
    String pid = pidss.substring(0, pidss.length() - 1);
    System.out.println("PtientIDs " + pid);

    String sql = "";
    if (!amrsEncounters.isEmpty()) {
      String EncounterID = amrsEncounters.get(0).getEncounterId();

      sql = "select patient_id as person_id, \n" +
        " e.uuid as amrs_encounter_uuid,\n" +
        " e.encounter_id ,\n" +
        "  case\n" +
        "  when e.encounter_type = 147 then 'e24209cc-0a1d-11eb-8f2a-bb245320c623'\n" +
        "  when e.encounter_type = 55 then 'b402d094-bff3-4b31-b167-82426b4e3e28'\n" +
        "  when e.encounter_type = 146 then 'e24209cc-0a1d-11eb-8f2a-bb245320c623'\n" +
        "  when e.encounter_type = 287 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 96 then 'b402d094-bff3-4b31-b167-82426b4e3e28'\n" +
        "  when e.encounter_type = 126 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
        "  when e.encounter_type = 81 then '1495edf8-2df2-11e9-b210-d663bd873d93'\n" +
        "  when e.encounter_type = 70 then '70a0158e-98f3-400b-9c90-a13c84b72065'\n" +
        "  when e.encounter_type = 261 then '1495edf8-2df2-11e9-b210-d663bd873d93'\n" +
        "  when e.encounter_type = 247 then 'e24209cc-0a1d-11eb-8f2a-bb245320c623'\n" +
        "  when e.encounter_type = 211 then 'b402d094-bff3-4b31-b167-82426b4e3e28'\n" +
        "  when e.encounter_type = 210 then 'b402d094-bff3-4b31-b167-82426b4e3e28'\n" +
        "  when e.encounter_type = 131 then 'dfcbe5d0-1afb-48a0-8f1e-5e5988b11f15'\n" +
        "  when e.encounter_type = 212 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 260 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 86 then 'e24209cc-0a1d-11eb-8f2a-bb245320c623'\n" +
        "  when e.encounter_type = 257 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
        "  when e.encounter_type = 272 then '3ee036d8-7c13-4393-b5d6-036f2fe45126'\n" +
        "  when e.encounter_type = 276 then '1495edf8-2df2-11e9-b210-d663bd873d93'\n" +
        "  when e.encounter_type = 56 then '5c1ecaf1-ec25-46b7-9b5e-ee7fe44f03cf'\n" +
        "  when e.encounter_type = 224 then 'de1f9d67-b73e-4e1b-90d0-036166fc6995'\n" +
        "  when e.encounter_type = 213 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 275 then '84220f19-9071-4745-9045-3b2f8d3dc128'\n" +
        "  when e.encounter_type = 69 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type =110 then 'd1059fb9-a079-4feb-a749-eedd709ae542'\n" +
        "  when e.encounter_type = 270 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 23 then '54df6991-13de-4efc-a1a9-2d5ac1b72ff8'\n" +
        "  when e.encounter_type = 22 then '54df6991-13de-4efc-a1a9-2d5ac1b72ff8'\n" +
        "  when e.encounter_type = 280 then 'ec2a91e5-444a-4ca0-87f1-f71ddfaf57eb'\n" +
        "  when e.encounter_type = 1 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
        "  when e.encounter_type = 14 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 14 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 16 then 'd1059fb9-a079-4feb-a749-eedd709ae542'\n" +
        "  when e.encounter_type = 16 then '160fcc03-4ff5-413f-b582-7e944a770bed'\n" +
        "  when e.encounter_type = 2 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 265 then '3ee036d8-7c13-4393-b5d6-036f2fe45126'\n" +
        "  when e.encounter_type = 264 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 32 then '3ee036d8-7c13-4393-b5d6-036f2fe45126'\n" +
        "  when encounter_type = 33 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 242 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 294 then '5021b1a1-e7f6-44b4-ba02-da2f2bcf8718'\n" +
        "  when e.encounter_type = 13 then 'e360f35f-e496-4f01-843b-e2894e278b5b'\n" +
        "  when e.encounter_type = 138 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 252 then '86709cfc-1490-11ec-82a8-0242ac130003'\n" +
        "  when e.encounter_type = 209 then '86709cfc-1490-11ec-82a8-0242ac130003'\n" +
        "  when e.encounter_type = 208 then '86709cfc-1490-11ec-82a8-0242ac130003'\n" +
        "  when e.encounter_type = 234 then 'bcc6da85-72f2-4291-b206-789b8186a021'\n" +
        "  when e.encounter_type = 233 then '415f5136-ca4a-49a8-8db3-f994187c3af6'\n" +
        "  when e.encounter_type = 31 then '2bdada65-4c72-4a48-8730-859890e25cee'\n" +
        "  when e.encounter_type = 127 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 144 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 153 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 181 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 182 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 248 then '7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3'\n" +
        "  when e.encounter_type = 249 then '7dffc392-13e7-11e9-ab14-d663bd873d93'\n" +
        "  when e.encounter_type = 203 then '7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3'\n" +
        "  when e.encounter_type = 186 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 19 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 20 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 26 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 17 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 18 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 279 then '54df6991-13de-4efc-a1a9-2d5ac1b72ff8'\n" +
        "  when e.encounter_type = 157 then '2bdada65-4c72-4a48-8730-859890e25cee'\n" +
        "  when e.encounter_type = 250 then '9bc15e94-2794-11e8-b467-0ed5f89f718b'\n" +
        "  when e.encounter_type = 243 then '975ae894-7660-4224-b777-468c2e710a2a'\n" +
        "  when e.encounter_type = 43 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 132 then '975ae894-7660-4224-b777-468c2e710a2a'\n" +
        "  when e.encounter_type = 115 then '01894f88-dc73-42d4-97a3-0929118403fb'\n" +
        "  when e.encounter_type = 115 then '82169b8d-c945-4c41-be62-433dfd9d6c86'\n" +
        "  when e.encounter_type = 115 then '5feee3f1-aa16-4513-8bd0-5d9b27ef1208'\n" +
        "  when e.encounter_type = 115 then '415f5136-ca4a-49a8-8db3-f994187c3af6'\n" +
        "  when e.encounter_type = 115 then 'bcc6da85-72f2-4291-b206-789b8186a021'\n" +
        "  when e.encounter_type = 253 then 'bfbb5dc2-d3e6-41ea-ad86-101336e3e38f'\n" +
        "  when e.encounter_type = 129 then '54df6991-13de-4efc-a1a9-2d5ac1b72ff8'\n" +
        "  when e.encounter_type = 110 then 'd1059fb9-a079-4feb-a749-eedd709ae542'\n" +
        "  when e.encounter_type = 251 then 'e1406e88-e9a9-11e8-9f32-f2801f1b9fd1'\n" +
        "  when e.encounter_type = 227 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
        "  when e.encounter_type = 5 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
        "  when e.encounter_type = 6 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
        "  when e.encounter_type = 8 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
        "  when e.encounter_type = 9 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
        "  when e.encounter_type = 196 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 121 then 'e1406e88-e9a9-11e8-9f32-f2801f1b9fd1'\n" +
        "  when e.encounter_type = 7 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
        "  when e.encounter_type = 273 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 274 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 237 then '5feee3f1-aa16-4513-8bd0-5d9b27ef1208'\n" +
        "  when e.encounter_type = 235 then '01894f88-dc73-42d4-97a3-0929118403fb'\n" +
        "  when e.encounter_type = 236 then '82169b8d-c945-4c41-be62-433dfd9d6c86'\n" +
        "  when e.encounter_type = 239 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 240 then '7c426cfc-3b47-4481-b55f-89860c21c7de'\n" +
        "  when e.encounter_type = 238 then '3ee036d8-7c13-4393-b5d6-036f2fe45126'\n" +
        "  when e.encounter_type = 268 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 140 then '54df6991-13de-4efc-a1a9-2d5ac1b72ff8'\n" +
        "  when e.encounter_type = 114 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 120 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 168 then '160fcc03-4ff5-413f-b582-7e944a770bed'\n" +
        "  when e.encounter_type = 284 then '162386c8-0464-11ea-9a9f-362b9e155667'\n" +
        "  when e.encounter_type = 285 then '162382b8-0464-11ea-9a9f-362b9e155667'\n" +
        "  when e.encounter_type = 283 then '16238574-0464-11ea-9a9f-362b9e155667'\n" +
        "  when e.encounter_type = 21 then '1495edf8-2df2-11e9-b210-d663bd873d93'\n" +
        "  when e.encounter_type = 220 then '5cf00d9e-09da-11ea-8d71-362b9e155667'\n" +
        "  when e.encounter_type = 214 then '5cf0124e-09da-11ea-8d71-362b9e155667'\n" +
        "  when e.encounter_type = 282 then 'ec2a91e5-444a-4ca0-87f1-f71ddfaf57eb'\n" +
        "  when e.encounter_type = 3 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
        "  when e.encounter_type = 15 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 80 then '160fcc03-4ff5-413f-b582-7e944a770bed'\n" +
        "  when e.encounter_type = 4 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 67 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 162 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 10 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 44 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 125 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 11 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 47 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 46 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 266 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 267 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 111 then 'e1406e88-e9a9-11e8-9f32-f2801f1b9fd1'\n" +
        "  when e.encounter_type = 34 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type =133 then '706a8b12-c4ce-40e4-aec3-258b989bf6d3'\n" +
        "  when e.encounter_type = 263 then 'c4a2be28-6673-4c36-b886-ea89b0a42116'\n" +
        "  when e.encounter_type = 263 then '706a8b12-c4ce-40e4-aec3-258b989bf6d3'\n" +
        "  when e.encounter_type = 262 then '291c0828-a216-11e9-a2a3-2a2ae2dbcce4'\n" +
        "  when e.encounter_type = 134 then 'c4a2be28-6673-4c36-b886-ea89b0a42116'\n" +
        "  when e.encounter_type = 117 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 176 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 116 then '2bdada65-4c72-4a48-8730-859890e25cee'\n" +
        "  when e.encounter_type = 119 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 221 then 'd1059fb9-a079-4feb-a749-eedd709ae542'\n" +
        "  when e.encounter_type = 158 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 281 then 'ec2a91e5-444a-4ca0-87f1-f71ddfaf57eb'\n" +
        "  when e.encounter_type = 105 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
        "  when e.encounter_type = 106 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 163 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 137 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
        "  end kenyaemr_encounter_uuid,\n" +
        "et.encounter_type_id,\n" +
        "et.name as encounterName,\n" +
        "e.location_id,\n" +
        "e.visit_id,\n" +
        "case \n" +
        " when e.encounter_type =110 then 6\n" +
        " when e.encounter_type = 270 then 8\n" +
        " when e.encounter_type = 23 then 303\n" +
        " when e.encounter_type = 22 then 303\n" +
        " when e.encounter_type = 280 then 302\n" +
        " when e.encounter_type = 1 then 7\n" +
        " when e.encounter_type = 14 then 21\n" +
        " when e.encounter_type = 14 then 8\n" +
        " when e.encounter_type = 16 then 6\n" +
        " when e.encounter_type = 16 then 252\n" +
        " when e.encounter_type = 2 then 8\n" +
        " when e.encounter_type = 265 then 14\n" +
        " when e.encounter_type = 264 then 15\n" +
        " when e.encounter_type = 32 then 14\n" +
        " when e.encounter_type = 33 then 15\n" +
        " when e.encounter_type = 242 then 21\n" +
        " when e.encounter_type = 294 then 73\n" +
        " when e.encounter_type = 13 then 300\n" +
        " when e.encounter_type = 138 then 8\n" +
        " when e.encounter_type = 252 then 243\n" +
        " when e.encounter_type = 209 then 243\n" +
        " when e.encounter_type = 208 then 243\n" +
        " when e.encounter_type = 234 then 10\n" +
        " when e.encounter_type = 233 then 9\n" +
        " when e.encounter_type = 31 then 2\n" +
        " when e.encounter_type = 127 then 8\n" +
        " when e.encounter_type = 144 then 8\n" +
        " when e.encounter_type = 153 then 8\n" +
        " when e.encounter_type = 181 then 21\n" +
        " when e.encounter_type = 182 then 21\n" +
        " when e.encounter_type = 248 then 24\n" +
        " when e.encounter_type = 249 then 29\n" +
        " when e.encounter_type = 203 then 24\n" +
        " when e.encounter_type = 186 then 21\n" +
        " when e.encounter_type = 19 then 21\n" +
        " when e.encounter_type = 20 then 21\n" +
        " when e.encounter_type = 26 then 21\n" +
        " when e.encounter_type = 17 then 21\n" +
        " when e.encounter_type = 18 then 21\n" +
        " when e.encounter_type = 279 then 303\n" +
        " when e.encounter_type = 157 then 2\n" +
        " when e.encounter_type = 250 then 28\n" +
        " when e.encounter_type = 243 then 22\n" +
        " when e.encounter_type = 43 then 8\n" +
        " when e.encounter_type = 132 then 22\n" +
        " when e.encounter_type = 115 then 11\n" +
        " when e.encounter_type = 115 then 12\n" +
        " when e.encounter_type = 115 then 13\n" +
        "  when e.encounter_type = 115 then 9\n" +
        " when e.encounter_type = 115 then 10\n" +
        " when e.encounter_type = 253 then 306\n" +
        " when e.encounter_type = 129 then 303\n" +
        " when e.encounter_type = 110 then 6\n" +
        " when e.encounter_type = 251 then 30\n" +
        " when e.encounter_type = 227 then 4\n" +
        " when e.encounter_type = 5 then 4\n" +
        " when e.encounter_type = 6 then 4\n" +
        " when e.encounter_type = 8 then 4\n" +
        " when e.encounter_type = 9 then 4\n" +
        " when e.encounter_type = 196 then 15\n" +
        " when e.encounter_type = 121 then 30\n" +
        " when e.encounter_type = 7 then 4\n" +
        " when e.encounter_type = 273 then 15\n" +
        " when e.encounter_type = 274 then 15\n" +
        " when e.encounter_type = 237 then 13\n" +
        " when e.encounter_type = 235 then 11\n" +
        " when e.encounter_type = 236 then 12\n" +
        " when e.encounter_type = 239 then 15\n" +
        " when e.encounter_type = 240 then 16\n" +
        " when e.encounter_type = 238 then 14\n" +
        " when e.encounter_type = 268 then 15\n" +
        " when e.encounter_type = 140 then 303\n" +
        " when e.encounter_type = 114 then 8\n" +
        " when e.encounter_type = 120 then 21\n" +
        " when e.encounter_type = 168 then 252\n" +
        " when e.encounter_type = 284 then 36\n" +
        " when e.encounter_type = 285 then 35\n" +
        " when e.encounter_type = 283 then 34\n" +
        " when e.encounter_type = 21 then 31\n" +
        " when e.encounter_type = 220 then 33\n" +
        " when e.encounter_type = 214 then 32\n" +
        " when e.encounter_type = 282 then 302\n" +
        " when e.encounter_type = 3 then 7\n" +
        " when e.encounter_type = 15 then 21\n" +
        " when e.encounter_type = 80 then 252\n" +
        " when e.encounter_type = 4 then 8\n" +
        " when e.encounter_type = 67 then 8\n" +
        " when e.encounter_type = 162 then 8\n" +
        " when e.encounter_type = 10 then 15\n" +
        " when e.encounter_type = 44 then 15\n" +
        " when e.encounter_type = 125 then 15\n" +
        " when e.encounter_type = 11 then 15\n" +
        " when e.encounter_type = 47 then 15\n" +
        " when e.encounter_type = 46 then 15\n" +
        " when e.encounter_type = 266 then 15\n" +
        " when e.encounter_type = 267 then 15\n" +
        " when e.encounter_type = 111 then 30\n" +
        " when e.encounter_type = 34 then 15\n" +
        " when e.encounter_type = 133 then 51\n" +
        " when e.encounter_type = 263 then 38\n" +
        " when e.encounter_type = 263 then 51\n" +
        " when e.encounter_type = 262 then 50\n" +
        " when e.encounter_type = 134 then 38\n" +
        " when e.encounter_type = 117 then 8\n" +
        " when e.encounter_type = 176 then 8\n" +
        " when e.encounter_type = 116 then 2\n" +
        " when e.encounter_type = 119 then 8\n" +
        " when e.encounter_type = 221 then 6\n" +
        " when e.encounter_type = 158 then 8\n" +
        " when e.encounter_type = 281 then 302\n" +
        " when e.encounter_type = 105 then 7\n" +
        " when e.encounter_type = 106 then 8\n" +
        " when e.encounter_type = 163 then 8\n" +
        " when e.encounter_type = 137 then 7\n" +
        " when e.encounter_type = 69 then 8\n" +
        " when e.encounter_type = 275 then 62\n" +
        " when e.encounter_type = 213 then 8\n" +
        " when e.encounter_type = 224 then 5\n" +
        " when e.encounter_type = 56 then 270\n" +
        " when e.encounter_type = 276 then 31\n" +
        " when e.encounter_type = 272 then 14\n" +
        " when e.encounter_type = 257 then 7\n" +
        " when e.encounter_type = 86 then 247\n" +
        " when e.encounter_type = 260 then 8\n" +
        " when e.encounter_type = 212 then 8\n" +
        " when e.encounter_type = 131 then 277\n" +
        " when e.encounter_type = 211 then 278\n" +
        " when e.encounter_type = 210 then 278\n" +
        " when e.encounter_type = 147 then 247\n" +
        " when e.encounter_type = 261 then 31\n" +
        " when e.encounter_type = 70 then 255\n" +
        " when e.encounter_type = 81 then 31\n" +
        " when e.encounter_type = 126 then 7\n" +
        " when e.encounter_type = 96 then 278\n" +
        " when e.encounter_type = 287 then 8\n" +
        " when e.encounter_type = 146 then 247\n" +
        " when e.encounter_type = 55 then 278\n" +
        "  end kenyaem_encounter_id,\n" +
        " e.creator,\n" +
        " e.encounter_datetime ,\n" +
        " e.encounter_type,\n" +
        " form_id,\n" +
        "  e.voided\n" +
        " from amrs.encounter e\n" +
        " inner join amrs.encounter_type et on  e.encounter_type=et.encounter_type_id \n" +
        " inner join amrs.location l on  e.location_id=l.location_id\n" +
        " where e.voided =0 and l.uuid in (" + locations + ") and e.encounter_id>" + EncounterID + " and e.patient_id in (" + pid + ")  \n" +
        " order by e.encounter_id asc ";

    } else {

      sql = "select patient_id as person_id, \n" +
        " e.uuid as amrs_encounter_uuid,\n" +
        " encounter_id ,\n" +
        "  case\n" +
        "  when e.encounter_type = 147 then 'e24209cc-0a1d-11eb-8f2a-bb245320c623'\n" +
        "  when e.encounter_type = 55 then 'b402d094-bff3-4b31-b167-82426b4e3e28'\n" +
        "  when e.encounter_type = 146 then 'e24209cc-0a1d-11eb-8f2a-bb245320c623'\n" +
        "  when e.encounter_type = 287 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 96 then 'b402d094-bff3-4b31-b167-82426b4e3e28'\n" +
        "  when e.encounter_type = 126 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
        "  when e.encounter_type = 81 then '1495edf8-2df2-11e9-b210-d663bd873d93'\n" +
        "  when e.encounter_type = 70 then '70a0158e-98f3-400b-9c90-a13c84b72065'\n" +
        "  when e.encounter_type = 261 then '1495edf8-2df2-11e9-b210-d663bd873d93'\n" +
        "  when e.encounter_type = 247 then 'e24209cc-0a1d-11eb-8f2a-bb245320c623'\n" +
        "  when e.encounter_type = 211 then 'b402d094-bff3-4b31-b167-82426b4e3e28'\n" +
        "  when e.encounter_type = 210 then 'b402d094-bff3-4b31-b167-82426b4e3e28'\n" +
        "  when e.encounter_type = 131 then 'dfcbe5d0-1afb-48a0-8f1e-5e5988b11f15'\n" +
        "  when e.encounter_type = 212 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 260 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 86 then 'e24209cc-0a1d-11eb-8f2a-bb245320c623'\n" +
        "  when e.encounter_type = 257 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
        "  when e.encounter_type = 272 then '3ee036d8-7c13-4393-b5d6-036f2fe45126'\n" +
        "  when e.encounter_type = 276 then '1495edf8-2df2-11e9-b210-d663bd873d93'\n" +
        "  when e.encounter_type = 56 then '5c1ecaf1-ec25-46b7-9b5e-ee7fe44f03cf'\n" +
        "  when e.encounter_type = 224 then 'de1f9d67-b73e-4e1b-90d0-036166fc6995'\n" +
        "  when e.encounter_type = 213 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 275 then '84220f19-9071-4745-9045-3b2f8d3dc128'\n" +
        "  when e.encounter_type = 69 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type =110 then 'd1059fb9-a079-4feb-a749-eedd709ae542'\n" +
        "  when e.encounter_type = 270 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 23 then '54df6991-13de-4efc-a1a9-2d5ac1b72ff8'\n" +
        "  when e.encounter_type = 22 then '54df6991-13de-4efc-a1a9-2d5ac1b72ff8'\n" +
        "  when e.encounter_type = 280 then 'ec2a91e5-444a-4ca0-87f1-f71ddfaf57eb'\n" +
        "  when e.encounter_type = 1 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
        "  when e.encounter_type = 14 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 14 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 16 then 'd1059fb9-a079-4feb-a749-eedd709ae542'\n" +
        "  when e.encounter_type = 16 then '160fcc03-4ff5-413f-b582-7e944a770bed'\n" +
        "  when e.encounter_type = 2 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 265 then '3ee036d8-7c13-4393-b5d6-036f2fe45126'\n" +
        "  when e.encounter_type = 264 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 32 then '3ee036d8-7c13-4393-b5d6-036f2fe45126'\n" +
        "  when encounter_type = 33 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 242 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 294 then '5021b1a1-e7f6-44b4-ba02-da2f2bcf8718'\n" +
        "  when e.encounter_type = 13 then 'e360f35f-e496-4f01-843b-e2894e278b5b'\n" +
        "  when e.encounter_type = 138 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 252 then '86709cfc-1490-11ec-82a8-0242ac130003'\n" +
        "  when e.encounter_type = 209 then '86709cfc-1490-11ec-82a8-0242ac130003'\n" +
        "  when e.encounter_type = 208 then '86709cfc-1490-11ec-82a8-0242ac130003'\n" +
        "  when e.encounter_type = 234 then 'bcc6da85-72f2-4291-b206-789b8186a021'\n" +
        "  when e.encounter_type = 233 then '415f5136-ca4a-49a8-8db3-f994187c3af6'\n" +
        "  when e.encounter_type = 31 then '2bdada65-4c72-4a48-8730-859890e25cee'\n" +
        "  when e.encounter_type = 127 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 144 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 153 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 181 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 182 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 248 then '7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3'\n" +
        "  when e.encounter_type = 249 then '7dffc392-13e7-11e9-ab14-d663bd873d93'\n" +
        "  when e.encounter_type = 203 then '7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3'\n" +
        "  when e.encounter_type = 186 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 19 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 20 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 26 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 17 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 18 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 279 then '54df6991-13de-4efc-a1a9-2d5ac1b72ff8'\n" +
        "  when e.encounter_type = 157 then '2bdada65-4c72-4a48-8730-859890e25cee'\n" +
        "  when e.encounter_type = 250 then '9bc15e94-2794-11e8-b467-0ed5f89f718b'\n" +
        "  when e.encounter_type = 243 then '975ae894-7660-4224-b777-468c2e710a2a'\n" +
        "  when e.encounter_type = 43 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 132 then '975ae894-7660-4224-b777-468c2e710a2a'\n" +
        "  when e.encounter_type = 115 then '01894f88-dc73-42d4-97a3-0929118403fb'\n" +
        "  when e.encounter_type = 115 then '82169b8d-c945-4c41-be62-433dfd9d6c86'\n" +
        "  when e.encounter_type = 115 then '5feee3f1-aa16-4513-8bd0-5d9b27ef1208'\n" +
        "  when e.encounter_type = 115 then '415f5136-ca4a-49a8-8db3-f994187c3af6'\n" +
        "  when e.encounter_type = 115 then 'bcc6da85-72f2-4291-b206-789b8186a021'\n" +
        "  when e.encounter_type = 253 then 'bfbb5dc2-d3e6-41ea-ad86-101336e3e38f'\n" +
        "  when e.encounter_type = 129 then '54df6991-13de-4efc-a1a9-2d5ac1b72ff8'\n" +
        "  when e.encounter_type = 110 then 'd1059fb9-a079-4feb-a749-eedd709ae542'\n" +
        "  when e.encounter_type = 251 then 'e1406e88-e9a9-11e8-9f32-f2801f1b9fd1'\n" +
        "  when e.encounter_type = 227 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
        "  when e.encounter_type = 5 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
        "  when e.encounter_type = 6 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
        "  when e.encounter_type = 8 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
        "  when e.encounter_type = 9 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
        "  when e.encounter_type = 196 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 121 then 'e1406e88-e9a9-11e8-9f32-f2801f1b9fd1'\n" +
        "  when e.encounter_type = 7 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
        "  when e.encounter_type = 273 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 274 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 237 then '5feee3f1-aa16-4513-8bd0-5d9b27ef1208'\n" +
        "  when e.encounter_type = 235 then '01894f88-dc73-42d4-97a3-0929118403fb'\n" +
        "  when e.encounter_type = 236 then '82169b8d-c945-4c41-be62-433dfd9d6c86'\n" +
        "  when e.encounter_type = 239 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 240 then '7c426cfc-3b47-4481-b55f-89860c21c7de'\n" +
        "  when e.encounter_type = 238 then '3ee036d8-7c13-4393-b5d6-036f2fe45126'\n" +
        "  when e.encounter_type = 268 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 140 then '54df6991-13de-4efc-a1a9-2d5ac1b72ff8'\n" +
        "  when e.encounter_type = 114 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 120 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 168 then '160fcc03-4ff5-413f-b582-7e944a770bed'\n" +
        "  when e.encounter_type = 284 then '162386c8-0464-11ea-9a9f-362b9e155667'\n" +
        "  when e.encounter_type = 285 then '162382b8-0464-11ea-9a9f-362b9e155667'\n" +
        "  when e.encounter_type = 283 then '16238574-0464-11ea-9a9f-362b9e155667'\n" +
        "  when e.encounter_type = 21 then '1495edf8-2df2-11e9-b210-d663bd873d93'\n" +
        "  when e.encounter_type = 220 then '5cf00d9e-09da-11ea-8d71-362b9e155667'\n" +
        "  when e.encounter_type = 214 then '5cf0124e-09da-11ea-8d71-362b9e155667'\n" +
        "  when e.encounter_type = 282 then 'ec2a91e5-444a-4ca0-87f1-f71ddfaf57eb'\n" +
        "  when e.encounter_type = 3 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
        "  when e.encounter_type = 15 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
        "  when e.encounter_type = 80 then '160fcc03-4ff5-413f-b582-7e944a770bed'\n" +
        "  when e.encounter_type = 4 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 67 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 162 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 10 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 44 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 125 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 11 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 47 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 46 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 266 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 267 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type = 111 then 'e1406e88-e9a9-11e8-9f32-f2801f1b9fd1'\n" +
        "  when e.encounter_type = 34 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
        "  when e.encounter_type =133 then '706a8b12-c4ce-40e4-aec3-258b989bf6d3'\n" +
        "  when e.encounter_type = 263 then 'c4a2be28-6673-4c36-b886-ea89b0a42116'\n" +
        "  when e.encounter_type = 263 then '706a8b12-c4ce-40e4-aec3-258b989bf6d3'\n" +
        "  when e.encounter_type = 262 then '291c0828-a216-11e9-a2a3-2a2ae2dbcce4'\n" +
        "  when e.encounter_type = 134 then 'c4a2be28-6673-4c36-b886-ea89b0a42116'\n" +
        "  when e.encounter_type = 117 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 176 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 116 then '2bdada65-4c72-4a48-8730-859890e25cee'\n" +
        "  when e.encounter_type = 119 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 221 then 'd1059fb9-a079-4feb-a749-eedd709ae542'\n" +
        "  when e.encounter_type = 158 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 281 then 'ec2a91e5-444a-4ca0-87f1-f71ddfaf57eb'\n" +
        "  when e.encounter_type = 105 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
        "  when e.encounter_type = 106 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 163 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
        "  when e.encounter_type = 137 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
        "  end kenyaemr_encounter_uuid,\n" +
        "et.encounter_type_id,\n" +
        "et.name as encounterName,\n" +
        "e.location_id,\n" +
        "e.visit_id,\n" +
        "case \n" +
        " when e.encounter_type =110 then 6\n" +
        " when e.encounter_type = 270 then 8\n" +
        " when e.encounter_type = 23 then 303\n" +
        " when e.encounter_type = 22 then 303\n" +
        " when e.encounter_type = 280 then 302\n" +
        " when e.encounter_type = 1 then 7\n" +
        " when e.encounter_type = 14 then 21\n" +
        " when e.encounter_type = 14 then 8\n" +
        " when e.encounter_type = 16 then 6\n" +
        " when e.encounter_type = 16 then 252\n" +
        " when e.encounter_type = 2 then 8\n" +
        " when e.encounter_type = 265 then 14\n" +
        " when e.encounter_type = 264 then 15\n" +
        " when e.encounter_type = 32 then 14\n" +
        " when e.encounter_type = 33 then 15\n" +
        " when e.encounter_type = 242 then 21\n" +
        " when e.encounter_type = 294 then 73\n" +
        " when e.encounter_type = 13 then 300\n" +
        " when e.encounter_type = 138 then 8\n" +
        " when e.encounter_type = 252 then 243\n" +
        " when e.encounter_type = 209 then 243\n" +
        " when e.encounter_type = 208 then 243\n" +
        " when e.encounter_type = 234 then 10\n" +
        " when e.encounter_type = 233 then 9\n" +
        " when e.encounter_type = 31 then 2\n" +
        " when e.encounter_type = 127 then 8\n" +
        " when e.encounter_type = 144 then 8\n" +
        " when e.encounter_type = 153 then 8\n" +
        " when e.encounter_type = 181 then 21\n" +
        " when e.encounter_type = 182 then 21\n" +
        " when e.encounter_type = 248 then 24\n" +
        " when e.encounter_type = 249 then 29\n" +
        " when e.encounter_type = 203 then 24\n" +
        " when e.encounter_type = 186 then 21\n" +
        " when e.encounter_type = 19 then 21\n" +
        " when e.encounter_type = 20 then 21\n" +
        " when e.encounter_type = 26 then 21\n" +
        " when e.encounter_type = 17 then 21\n" +
        " when e.encounter_type = 18 then 21\n" +
        " when e.encounter_type = 279 then 303\n" +
        " when e.encounter_type = 157 then 2\n" +
        " when e.encounter_type = 250 then 28\n" +
        " when e.encounter_type = 243 then 22\n" +
        " when e.encounter_type = 43 then 8\n" +
        " when e.encounter_type = 132 then 22\n" +
        " when e.encounter_type = 115 then 11\n" +
        " when e.encounter_type = 115 then 12\n" +
        " when e.encounter_type = 115 then 13\n" +
        "  when e.encounter_type = 115 then 9\n" +
        " when e.encounter_type = 115 then 10\n" +
        " when e.encounter_type = 253 then 306\n" +
        " when e.encounter_type = 129 then 303\n" +
        " when e.encounter_type = 110 then 6\n" +
        " when e.encounter_type = 251 then 30\n" +
        " when e.encounter_type = 227 then 4\n" +
        " when e.encounter_type = 5 then 4\n" +
        " when e.encounter_type = 6 then 4\n" +
        " when e.encounter_type = 8 then 4\n" +
        " when e.encounter_type = 9 then 4\n" +
        " when e.encounter_type = 196 then 15\n" +
        " when e.encounter_type = 121 then 30\n" +
        " when e.encounter_type = 7 then 4\n" +
        " when e.encounter_type = 273 then 15\n" +
        " when e.encounter_type = 274 then 15\n" +
        " when e.encounter_type = 237 then 13\n" +
        " when e.encounter_type = 235 then 11\n" +
        " when e.encounter_type = 236 then 12\n" +
        " when e.encounter_type = 239 then 15\n" +
        " when e.encounter_type = 240 then 16\n" +
        " when e.encounter_type = 238 then 14\n" +
        " when e.encounter_type = 268 then 15\n" +
        " when e.encounter_type = 140 then 303\n" +
        " when e.encounter_type = 114 then 8\n" +
        " when e.encounter_type = 120 then 21\n" +
        " when e.encounter_type = 168 then 252\n" +
        " when e.encounter_type = 284 then 36\n" +
        " when e.encounter_type = 285 then 35\n" +
        " when e.encounter_type = 283 then 34\n" +
        " when e.encounter_type = 21 then 31\n" +
        " when e.encounter_type = 220 then 33\n" +
        " when e.encounter_type = 214 then 32\n" +
        " when e.encounter_type = 282 then 302\n" +
        " when e.encounter_type = 3 then 7\n" +
        " when e.encounter_type = 15 then 21\n" +
        " when e.encounter_type = 80 then 252\n" +
        " when e.encounter_type = 4 then 8\n" +
        " when e.encounter_type = 67 then 8\n" +
        " when e.encounter_type = 162 then 8\n" +
        " when e.encounter_type = 10 then 15\n" +
        " when e.encounter_type = 44 then 15\n" +
        " when e.encounter_type = 125 then 15\n" +
        " when e.encounter_type = 11 then 15\n" +
        " when e.encounter_type = 47 then 15\n" +
        " when e.encounter_type = 46 then 15\n" +
        " when e.encounter_type = 266 then 15\n" +
        " when e.encounter_type = 267 then 15\n" +
        " when e.encounter_type = 111 then 30\n" +
        " when e.encounter_type = 34 then 15\n" +
        " when e.encounter_type = 133 then 51\n" +
        " when e.encounter_type = 263 then 38\n" +
        " when e.encounter_type = 263 then 51\n" +
        " when e.encounter_type = 262 then 50\n" +
        " when e.encounter_type = 134 then 38\n" +
        " when e.encounter_type = 117 then 8\n" +
        " when e.encounter_type = 176 then 8\n" +
        " when e.encounter_type = 116 then 2\n" +
        " when e.encounter_type = 119 then 8\n" +
        " when e.encounter_type = 221 then 6\n" +
        " when e.encounter_type = 158 then 8\n" +
        " when e.encounter_type = 281 then 302\n" +
        " when e.encounter_type = 105 then 7\n" +
        " when e.encounter_type = 106 then 8\n" +
        " when e.encounter_type = 163 then 8\n" +
        " when e.encounter_type = 137 then 7\n" +
        " when e.encounter_type = 69 then 8\n" +
        " when e.encounter_type = 275 then 62\n" +
        " when e.encounter_type = 213 then 8\n" +
        " when e.encounter_type = 224 then 5\n" +
        " when e.encounter_type = 56 then 270\n" +
        " when e.encounter_type = 276 then 31\n" +
        " when e.encounter_type = 272 then 14\n" +
        " when e.encounter_type = 257 then 7\n" +
        " when e.encounter_type = 86 then 247\n" +
        " when e.encounter_type = 260 then 8\n" +
        " when e.encounter_type = 212 then 8\n" +
        " when e.encounter_type = 131 then 277\n" +
        " when e.encounter_type = 211 then 278\n" +
        " when e.encounter_type = 210 then 278\n" +
        " when e.encounter_type = 147 then 247\n" +
        " when e.encounter_type = 261 then 31\n" +
        " when e.encounter_type = 70 then 255\n" +
        " when e.encounter_type = 81 then 31\n" +
        " when e.encounter_type = 126 then 7\n" +
        " when e.encounter_type = 96 then 278\n" +
        " when e.encounter_type = 287 then 8\n" +
        " when e.encounter_type = 146 then 247\n" +
        " when e.encounter_type = 55 then 278\n" +
        "  end kenyaem_encounter_id,\n" +
        " e.creator,\n" +
        " e.encounter_datetime ,\n" +
        " e.encounter_type,\n" +
        " form_id,\n" +
        "  e.voided\n" +
        " from amrs.encounter e\n" +
        " inner join amrs.encounter_type et on  e.encounter_type=et.encounter_type_id\n" +
        " inner join amrs.location l on  e.location_id=l.location_id\n" +
        " where e.voided =0  and e.patient_id in (" + pid + ")  \n" + // and l.uuid in (" + locations + ")
        " order by e.encounter_id asc "
      ;
    }
    // System.out.println("Sql "+ sql);

    System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {
      String patientId = rs.getString("person_id");
      String amrsEncounterUuid = rs.getString("amrs_encounter_uuid");
      String encounterId = rs.getString("encounter_id");
      String encounterTypeId = rs.getString("encounter_type_id");
      String encounterDateTime = rs.getString("encounter_datetime");
      String encounterName = rs.getString("encounterName");
      String locationId = rs.getString("location_id");
      String visitId = rs.getString("visit_id");
      String kenyaemrEncounterTypeId = rs.getString("kenyaem_encounter_id");
      String kenyaemrEncounterUuid = rs.getString("kenyaemr_encounter_uuid");

      System.out.println("patientId " + patientId + " EncouterID " + encounterId + "encounterTypeId " + encounterTypeId + " encounterName " + encounterName + " locationId " + locationId + " visitId " + visitId);

      List<AMRSEncounters> amrsEncountersList = amrsEncounterService.findByPatientIdAndEncounterId(patientId, encounterId);
      if (amrsEncountersList.size() == 0) {
        AMRSEncounters ae = new AMRSEncounters();
        ae.setPatientId(patientId);
        ae.setEncounterId(encounterId);
        ae.setEncounterTypeId(encounterTypeId);
        ae.setEncounterName(encounterName);
        ae.setLocationId(locationId);
        ae.setVisitId(visitId);
        ae.setEncounterDateTime(encounterDateTime);
        ae.setKenyaemrEncounterTypeId(kenyaemrEncounterTypeId);
        ae.setKenyaemrEncounterTypeUuid(kenyaemrEncounterUuid);
        amrsEncounterService.save(ae);
      }
    }

    EncountersPayload.encounters(amrsEncounterService, amrsPatientServices, amrsVisitService, url, auth);

  }

  public static void enrollments(String server, String username, String password, String locations, String parentUUID, AMRSEnrollmentService amrsEnrollmentService, AMRSPatientServices amrsPatientServices, String url, String auth) throws SQLException, JSONException, ParseException, IOException {


    String sql = "select person_id,encounter_id,enrollment_date,hiv_start_date,death_date,transfer_out,transfer_out_date from etl.flat_hiv_summary_v15b  \n" +
      "where is_clinical_encounter = 1 and next_clinical_datetime_hiv is null\n" +
      "and location_id in (339,2,98,379)";
    System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {
      String patientId = rs.getString("person_id");
      String encounterId = rs.getString("encounter_id");
      String encounterDatetime = rs.getString("encounter_datetime");
      String formId = rs.getString("form_id");
      String formName = rs.getString("Form_Name");
      String patientType = rs.getString("Patient_Type");
      String entryPoint = rs.getString("Entry_point");
      String tiFacility = rs.getString("TI_Facility");
      String dateFirstEnrolledInCare = rs.getString("Date_first_enrolled_in_care");
      String transferInDate = rs.getString("Transfer_in_date");
      String dateStartedArtAtTransferringFacility = rs.getString("");
      String dateConfirmedHivPositive = rs.getString("");
      String facilityConfirmedHivPositive = rs.getString("");
      String baselineArvUse = rs.getString("");
      String purposeOfBaselineArvUse = rs.getString("");
      String baselineArvRegimen = rs.getString("");
      String baselineArvRegimenLine = rs.getString("");
      String baselineArvDateLastUsed = rs.getString("");
      String baselineWhoStage = rs.getString("");
      String baselineCd4Results = rs.getString("");
      String baselineCd4Date = rs.getString("");
      String baselineVlResults = rs.getString("");
      String baselineVlDate = rs.getString("");
      String baselineVlLdlResults = rs.getString("");
      String baselineVlLdlDate = rs.getString("");
      String baselineHbvInfected = rs.getString("");
      String baselineTbInfected = rs.getString("");
      String baselinePregnant = rs.getString("");
      String baselineBreastFeeding = rs.getString("");
      String baselineWeight = rs.getString("");
      String baselineHeight = rs.getString("");
      String baselineBMI = rs.getString("");
      String nameOfTreatmentSupporter = rs.getString("");
      String relationshipOfTreatmentSupporter = rs.getString("");
      String treatmentSupporterTelephone = rs.getString("");
      String treatmentSupporterAddress = rs.getString("");

      List<AMRSEnrollments> enrollmentsList = amrsEnrollmentService.getByPatientID(patientId);
      if (enrollmentsList.isEmpty()) {
        AMRSEnrollments ae = new AMRSEnrollments();
        ae.setPatientId(patientId);
        ae.setEncounterId(encounterId);
        ae.setEncounterDatetime(encounterDatetime);
        ae.setFormId(formId);
        ae.setFormName(formName);
        ae.setPatientType(patientType);
        ae.setEntryPoint(entryPoint);
        ae.setTiFacility(tiFacility);
        ae.setDateFirstEnrolledInCare(dateFirstEnrolledInCare);
        ae.setTransferInDate(transferInDate);
        ae.setDateStartedArtAtTransferringFacility(dateStartedArtAtTransferringFacility);
        ae.setDateConfirmedHivPositive(dateConfirmedHivPositive);
        ae.setFacilityConfirmedHivPositive(facilityConfirmedHivPositive);
        ae.setBaselineArvUse(baselineArvUse);
        ae.setPurposeOfBaselineArvUse(purposeOfBaselineArvUse);
        ae.setBaselineArvRegimen(baselineArvRegimen);
        ae.setBaselineArvRegimenLine(baselineArvRegimenLine);
        ae.setBaselineArvDateLastUsed(baselineArvDateLastUsed);
        ae.setBaselineWhoStage(baselineWhoStage);
        ae.setBaselineCd4Results(baselineCd4Results);
        ae.setBaselineCd4Date(baselineCd4Date);
        ae.setBaselineVlResults(baselineVlResults);
        ae.setBaselineVlDate(baselineVlDate);
        ae.setBaselineVlLdlResults(baselineVlLdlResults);
        ae.setBaselineVlLdlDate(baselineVlLdlDate);
        ae.setBaselineHbvInfected(baselineHbvInfected);
        ae.setBaselineTbInfected(baselineTbInfected);
        ae.setBaselinePregnant(baselinePregnant);
        ae.setBaselineBreastFeeding(baselineBreastFeeding);
        ae.setBaselineWeight(baselineWeight);
        ae.setBaselineHeight(baselineHeight);
        ae.setBaselineBMI(baselineBMI);
        ae.setNameOfTreatmentSupporter(nameOfTreatmentSupporter);
        ae.setRelationshipOfTreatmentSupporter(relationshipOfTreatmentSupporter);
        ae.setTreatmentSupporterTelephone(treatmentSupporterTelephone);
        ae.setTreatmentSupporterAddress(treatmentSupporterAddress);

        amrsEnrollmentService.save(ae);

      }


    }

    EnrollmentsPayload.encounters(url, auth);
  }

  public static void visits(String server, String username, String password, String kenyaEMRLocationUuid, AMRSVisitService amrsVisitService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth) throws SQLException, JSONException, ParseException, IOException {
    String sql = "";
    List<AMRSVisits> amrsVisitsList = amrsVisitService.findFirstByOrderByIdDesc();
    List<String> stringPIDsList = amrsPatientServices.getAllPatientID();
    String pid = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);

    System.out.println("PatientIDs " + pid);

    if (!amrsVisitsList.isEmpty()) {
      sql = "select v.visit_id,\n" +
        "       e.patient_id,\n" +
        "       visit_type_id,\n" +
        "       date_started,\n" +
        "       e.location_id,\n" +
        "       date_stopped,\n" +
        "       encounter_type,\n" +
        "       v.voided \n" +
        "              from amrs.visit v\n" +
        "              inner join  amrs.encounter e on e.visit_id = v.visit_id\n" +
        "              inner join amrs.location l on l.location_id=e.location_id\n" +
        "              where  e.patient_id in (" + pid + ") \n " + //and v.visit_id>" + amrsVisitsList.get(0).getVisitId() + "
        "              and v.voided=0\n" +
        "              group by  v.visit_id order by v.visit_id asc ";
    } else {

      sql = "select v.visit_id,\n" +
        "       e.patient_id,\n" +
        "\t     visit_type_id,\n" +
        "       date_started,\n" +
        "       e.location_id,\n" +
        "\t     date_stopped,\n" +
        "       encounter_type,\n" +
        "       v.voided \n" +
        "              from amrs.visit v\n" +
        "              inner join  amrs.encounter e on e.visit_id = v.visit_id\n" +
        "              inner join amrs.location l on l.location_id=e.location_id\n" +
        "              where  e.patient_id in (" + pid + ")\n" + //l.uuid in (" + locations + ") and
        "              and v.voided=0 \n" +
        "              group by  v.visit_id order by v.visit_id asc ";
    }


    //  System.out.println("locations " + locations + " parentUUID " + parentUUID);
    // System.out.println("locations " + sql);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String visitId = rs.getString("visit_id");
      String patientId = rs.getString("patient_id");
      String visitTypeId = rs.getString("visit_type_id");
      String dateStarted = rs.getString("date_started");
      String dateStopped = rs.getString("date_stopped");
      String locationId = rs.getString("location_id");
      String voided = rs.getString("voided");

      System.out.println("Visit_Id " + visitId);

      List<AMRSVisits> av = amrsVisitService.findByVisitID(visitId);
      if (av.isEmpty()) {
        List<AMRSPatients> amrsPatients = amrsPatientServices.getByPatientID(patientId);
        String kenyaemrpid = "Client Not Migrated";
        AMRSVisits avv = new AMRSVisits();
          kenyaemrpid = amrsTranslater.translater(amrsPatients.get(0).getPersonId());
        avv.setVisitId(visitId);
        avv.setDateStarted(dateStarted);
        avv.setPatientId(patientId);
        avv.setVisitType(visitTypeId);
        avv.setKenyaemrPatientUuid(kenyaemrpid);
        avv.setDateStop(dateStopped);
        avv.setVoided(voided);
        avv.setLocationId(locationId);
        amrsVisitService.save(avv);
      }


    }

    VisitsPayload.visits(amrsVisitService, kenyaEMRLocationUuid, amrsPatientServices, auth, url);

  }


  public static void order(String server, String username, String password, String KenyaEMRlocationUuid, AMRSOrderService amrsOrderService, AMRSPatientServices amrsPatientServices, AMRSVisitService amrsVisitService, AMRSTranslater amrsTranslater, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    List<String> stringPIDsList = amrsPatientServices.getAllPatientID();
    String samplePatientList = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);
    //String samplePatientList = AMRSSamples.getPersonIdListKapsoya();
    // String samplePatientList = pid;

    System.out.println("Data is here " + samplePatientList);

    String sql = "WITH cte_orders as ( SELECT  \n" +
            "                      t1.patient_id, \n" +
            "                      t1.order_id, \n" +
            "                      e.encounter_id, \n" +
            "                      e.visit_id,\n" +
            "                      t1.order_number AS orderNumber, \n" +
            "                      t1.concept_id AS concept_id, \n" +
            "                      case when t1.concept_id = 856 then 'VIRAL LOAD' ELSE t2.name end AS display, \n" +
            " case when ot.order_type_id = 2 then 'drugorder' when ot.order_type_id = 3 then 'testorder' ELSE NULL end AS order_type,      t3.value_numeric as order_result, \n" +
            "                      GROUP_CONCAT(DISTINCT t6.identifier) AS identifiers, \n" +
            "                      t1.date_activated AS date_ordered, \n" +
            "                           t1.urgency as urgency, \n" +
            "                           t1.order_action as order_action, \n" +
            "                           t1.order_reason as order_reason, \n" +
            "                      t4.name AS sample_drawn, \n" +
            "                      DATE(t5.obs_datetime) AS sample_collection_date \n" +
            "                       FROM amrs.encounter e \n" +
            "                          INNER JOIN amrs.patient p USING (patient_id) \n" +
            "                          INNER JOIN amrs.person per ON (per.person_id = p.patient_id) \n" +
            "                          INNER JOIN amrs.orders t1 ON(e.encounter_id = t1.encounter_id) \n" +
            " LEFT OUTER JOIN amrs.order_type ot USING(order_type_id)  \n" +
            "  LEFT OUTER JOIN amrs.obs t5 ON (t1.order_id = t5.order_id AND (t5.voided IS NULL || t5.voided = 0) AND t5.concept_id = 10189) \n" +
            "                          LEFT OUTER JOIN amrs.obs t3 ON (t1.concept_id = t3.concept_id AND (t3.voided IS NULL || t3.voided = 0) AND t1.encounter_id = t3.encounter_id)     \n" +
            "                          LEFT OUTER JOIN amrs.concept_name t4 ON (t5.value_coded = t4.concept_id) \n" +
            "                          LEFT OUTER JOIN amrs.concept_name t2 ON (t1.concept_id = t2.concept_id)  \n" +
            "                          LEFT OUTER JOIN amrs.patient_identifier t6 ON (t1.patient_id = t6.patient_id) \n" +
            "                       WHERE p.voided = 0 AND e.voided = 0 \n" +
            "                          AND (t1.voided IS NULL || t1.voided = 0) \n" +
            "                          AND t1.patient_id IN(" + samplePatientList + ") \n" +
            "                       GROUP BY t1.patient_id, t1.order_number \n" +
            "                       ORDER BY t1.patient_id, t1.date_activated desc\n" +
            "                       ), \n" +
            "                cte_vls AS( \n" +
            "                       SELECT  \n" +
            "                      f.person_id,  \n" +
            "                      f.concept_id,  \n" +
            "                      f.order_id,  \n" +
            "                      f.obs_date,  \n" +
            "                      f.order_value  \n" +
            "                       FROM ( \n" +
            "                      SELECT  \n" +
            "                          t.row_id,  \n" +
            "                          t.person_id,  \n" +
            "                          t.order_type as concept_id,  \n" +
            "                          t.encounter_id,  \n" +
            "                          CASE WHEN t.order_id IS NOT NULL AND t.concept_id = 856 THEN t.order_id \n" +
            "                          WHEN LEAD(t.order_id) OVER (ORDER BY t.row_id ASC) IS NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id THEN t.order_id \n" +
            "                          ELSE NULL END AS order_id,  \n" +
            "                          t.obs_date,  \n" +
            "                          CASE WHEN t.order_id IS NOT NULL AND t.concept_id = 856 THEN t.value_numeric \n" +
            "                          WHEN t.order_id IS NOT NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id AND t.order_type = 856 THEN LEAD(t.value_numeric) OVER (ORDER BY t.row_id ASC) \n" +
            "                          ELSE NULL END AS order_value \n" +
            "                      FROM ( \n" +
            "                          SELECT  \n" +
            "                              ROW_NUMBER() OVER (ORDER BY o.person_id, o.obs_id ASC) AS row_id,  \n" +
            "                              o.person_id,  \n" +
            "                              o.concept_id,  \n" +
            "                              o.encounter_id,  \n" +
            "                              o.order_id, \n" +
            "                              d.concept_id as order_type, \n" +
            "                              DATE(o.obs_datetime) AS obs_date,  \n" +
            "                              o.value_numeric,  \n" +
            "                              o.location_id,  \n" +
            "                              o.voided \n" +
            "                          FROM  amrs.obs o \n" +
            "                            LEFT JOIN amrs.orders d using(order_id) \n" +
            "                          WHERE  \n" +
            "                              o.person_id in(" + samplePatientList + ") AND  \n" +
            "                              o.concept_id IN (10189, 856) AND  \n" +
            "                              o.encounter_id IS NULL \n" +
            "                          GROUP BY  \n" +
            "                              o.person_id,  \n" +
            "                              o.concept_id,  \n" +
            "                              DATE(o.obs_datetime) \n" +
            "                          ORDER BY  \n" +
            "                          o.person_id, o.obs_id ASC \n" +
            "                      ) t \n" +
            "                       ) f  \n" +
            "                       WHERE f.order_id IS NOT NULL \n" +
            "                       ), \n" +
            " cte_dna_pcr AS( \n" +
            "                       SELECT  \n" +
            "                      f.person_id,  \n" +
            "                      f.concept_id,  \n" +
            "                      f.order_id,  \n" +
            "                      f.obs_date,  \n" +
            "                      f.order_value  \n" +
            "                       FROM ( \n" +
            "                      SELECT  \n" +
            "                          t.row_id,  \n" +
            "                          t.person_id,  \n" +
            "                          t.order_type as concept_id,  \n" +
            "                          t.encounter_id,  \n" +
            "                          CASE WHEN t.order_id IS NOT NULL AND t.concept_id = 1030 THEN t.order_id \n" +
            "                          WHEN LEAD(t.order_id) OVER (ORDER BY t.row_id ASC) IS NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id THEN t.order_id \n" +
            "                          ELSE NULL END AS order_id,  \n" +
            "                          t.obs_date,  \n" +
            "                          CASE WHEN t.order_id IS NOT NULL AND t.concept_id = 1030 THEN t.value_coded \n" +
            "                          WHEN t.order_id IS NOT NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id AND t.order_type = 1030 THEN LEAD(t.value_coded) OVER (ORDER BY t.row_id ASC) \n" +
            "                          ELSE NULL END AS order_value \n" +
            "                      FROM ( \n" +
            "                          SELECT  \n" +
            "                              ROW_NUMBER() OVER (ORDER BY o.person_id, o.obs_id ASC) AS row_id,  \n" +
            "                              o.person_id,  \n" +
            "                              o.concept_id,  \n" +
            "                              o.encounter_id,  \n" +
            "                              o.order_id, \n" +
            "                              d.concept_id as order_type, \n" +
            "                              DATE(o.obs_datetime) AS obs_date,  \n" +
            "                              o.value_coded,  \n" +
            "                              o.location_id,  \n" +
            "                              o.voided \n" +
            "                          FROM   amrs.obs o \n" +
            "                        LEFT JOIN amrs.orders d using(order_id) \n" +
            "                          WHERE  \n" +
            "                              o.person_id in(" + samplePatientList + ") AND  \n" +
            "                              o.concept_id IN (10189, 1030) AND  \n" +
            "                              o.encounter_id IS NULL \n" +
            "                          GROUP BY  \n" +
            "                              o.person_id,  \n" +
            "                              o.concept_id,  \n" +
            "                              DATE(o.obs_datetime) \n" +
            "                          ORDER BY  \n" +
            "                          o.person_id, o.obs_id ASC \n" +
            "                      ) t \n" +
            "                       ) f  \n" +
            "                       WHERE f.order_id IS NOT NULL \n" +
            "                       ), \n" +
            " cte_cd4 AS( \n" +
            "                       SELECT  \n" +
            "                      f.person_id,  \n" +
            "                      f.concept_id,  \n" +
            "                      f.order_id,  \n" +
            "                      f.obs_date,  \n" +
            "                      f.order_value  \n" +
            "                       FROM ( \n" +
            "                      SELECT  \n" +
            "                          t.row_id,  \n" +
            "                          t.person_id,  \n" +
            "                          t.order_type as concept_id,  \n" +
            "                          t.encounter_id,  \n" +
            "                          CASE WHEN t.order_id IS NOT NULL AND t.concept_id in(657, 5497) THEN t.order_id \n" +
            "                          WHEN LEAD(t.order_id) OVER (ORDER BY t.row_id ASC) IS NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id THEN t.order_id \n" +
            "                          ELSE NULL END AS order_id,  \n" +
            "                          t.obs_date,  \n" +
            "                          CASE WHEN t.order_id IS NOT NULL AND t.concept_id in(657, 5497) THEN t.value_numeric \n" +
            "                          WHEN t.order_id IS NOT NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id AND t.order_type in(657, 5497) THEN LEAD(t.value_numeric) OVER (ORDER BY t.row_id ASC) \n" +
            "                          ELSE NULL END AS order_value \n" +
            "                      FROM ( \n" +
            "                          SELECT  \n" +
            "                              ROW_NUMBER() OVER (ORDER BY o.person_id, o.obs_id ASC) AS row_id,  \n" +
            "                              o.person_id,  \n" +
            "                              o.concept_id,  \n" +
            "                              o.encounter_id,  \n" +
            "                              o.order_id, \n" +
            "                              d.concept_id as order_type, \n" +
            "                              DATE(o.obs_datetime) AS obs_date,  \n" +
            "                              o.value_numeric,  \n" +
            "                              o.location_id,  \n" +
            "                              o.voided \n" +
            "                          FROM  amrs.obs o \n" +
            "                          LEFT JOIN  amrs.orders d using(order_id) \n" +
            "                          WHERE  \n" +
            "                              o.person_id in(" + samplePatientList + ") AND  \n" +
            "                              o.concept_id IN (10189, 657, 5497) AND  \n" +
            "                              o.encounter_id IS NULL \n" +
            "                          GROUP BY  \n" +
            "                              o.person_id,  \n" +
            "                              o.concept_id,  \n" +
            "                              DATE(o.obs_datetime) \n" +
            "                          ORDER BY  \n" +
            "                          o.person_id, o.obs_id ASC \n" +
            "                      ) t \n" +
            "                       ) f  \n" +
            "                       WHERE f.order_id IS NOT NULL \n" +
            "                       ) \n" +
            "                       SELECT a.patient_id, identifiers, a.order_id, a.orderNumber,a.order_type, a.order_action, a.urgency,a.order_reason, a.encounter_id,a.visit_id, a.concept_id, a.display, a.date_ordered, a.sample_drawn, a.sample_collection_date,  \n" +
            "                       case when b.order_value IS NOT NULL THEN b.order_value  \n" +
            "                       when c.order_value IS NOT NULL THEN c.order_value \n" +
            "                       when d.order_value IS NOT NULL THEN d.order_value \n" +
            "                       else a.order_result END  as final_order_result FROM cte_orders a  \n" +
            "                       left join cte_vls b ON(a.order_id = b.order_id AND a.concept_id = b.concept_id) \n" +
            "                       left join cte_dna_pcr c ON(a.order_id = c.order_id AND a.concept_id = c.concept_id) \n" +
            "                       left join cte_cd4 d ON(a.order_id = d.order_id AND a.concept_id = d.concept_id) \n" +
            "                       group by a.patient_id, a.orderNumber";


    System.out.println("Sql is here " + sql);

    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {
      String patientId = rs.getString("patient_id");
      String orderId = rs.getString("order_id");
      String orderNumber = rs.getString("orderNumber");
      String encounterId = rs.getString("encounter_id");
      String visitId = rs.getString("visit_id");
      String conceptId = rs.getString("concept_id");
      String display = rs.getString("display");
      String dateOrdered = rs.getString("date_ordered");
      String sampleDrawn = rs.getString("sample_drawn");
      String sampleCollectionDate = rs.getString("sample_collection_date");
      String finalOrderResult = rs.getString("final_order_result");
      String orderAction = rs.getString("order_action");
      String orderReason = rs.getString("order_reason");
      String urgency = rs.getString("urgency");
      String orderType = rs.getString("order_type");

      System.out.println("final results : " + finalOrderResult);

      List<AMRSOrders> amrsOrders = amrsOrderService.findByOrderId(Integer.parseInt(orderId));
      if (amrsOrders.isEmpty()) {
        String kenyaemr_uuid = "";
        AMRSOrders ao = new AMRSOrders();
        ao.setConceptId(Integer.valueOf(conceptId));
        ao.setPatientId(patientId);
        ao.setOrderId(Integer.valueOf(orderId));
        ao.setEncounterId(Integer.valueOf(encounterId));
        ao.setDisplay(display);
        ao.setVisitId(visitId);
        ao.setOrderReason(orderReason);
        ao.setOrderNumber(orderNumber);
        ao.setOrderAction(orderAction);
        ao.setSampleDrawn(sampleDrawn);
        ao.setSampleCollectionDate(sampleCollectionDate);
        ao.setFinalOrderResult(finalOrderResult);
        ao.setDateOrdered(dateOrdered);
        ao.setUrgency(urgency);
        ao.setOrderType(orderType);
        ao.setKenyaemrConceptUuid(amrsTranslater.translater(conceptId));
        ao.setOrderId(Integer.parseInt(orderId));
        ao.setKenyaemrPatientUuid(amrsTranslater.translater(patientId));
        amrsOrderService.save(ao);

      } else {
        System.out.println("Order already Shipped");
      }

    }

    // process all un processed orders
   // try {
      OrdersPayload.orders(KenyaEMRlocationUuid, amrsOrderService, amrsPatientServices, amrsVisitService, amrsTranslater, url, auth);
   // } catch (Exception e) {
     // System.out.println("Error in processing orders " + e.getMessage());
    //}
  }

  public static void triage(String server, String username, String password, String locations, String KenyaemrLocationUuid, AMRSTranslater amrsTranslater, AMRSTriageService amrsTriageService, AMRSPatientServices amrsPatientServices, AMRSEncounterService amrsEncounterService, AMRSConceptMappingService amrsConceptMappingService, AMRSVisitService amrsVisitService, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    List<String> stringPIDsList = amrsPatientServices.getAllPatientID();
    String pid = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);
    // String pid = AMRSSamples.getPersonIdListKapsoya();
    System.out.println("PatientIDs " + pid);

    String sql = "";
    sql = "WITH cte_vitals_concepts as (\n" +
      "SELECT \n" +
      "    concept_id, uuid\n" +
      "FROM\n" +
      "    amrs.concept\n" +
      "WHERE\n" +
      "    uuid IN (\n" +
      "    'a8a65d5a-1350-11df-a1f1-0026b9348838', \n" +
      "        'a8a65e36-1350-11df-a1f1-0026b9348838',\n" +
      "        'a8a65f12-1350-11df-a1f1-0026b9348838',\n" +
      "        'a8a6f71a-1350-11df-a1f1-0026b9348838',\n" +
      "        'a8a65fee-1350-11df-a1f1-0026b9348838',\n" +
      "        'a8a660ca-1350-11df-a1f1-0026b9348838',\n" +
      "        'a8a6619c-1350-11df-a1f1-0026b9348838',\n" +
      "        '5099d8a8-36c1-4574-a568-9bc49c15c08c',\n" +
      "        '507f48e7-26fc-490b-a521-35d7c5aa8e9f',\n" +
      "        'a8a66354-1350-11df-a1f1-0026b9348838',\n" +
      "        'a89c60c0-1350-11df-a1f1-0026b9348838',\n" +
      "        '9061e5d5-8478-4d16-be44-bfec05b6705a',\n" +
      "        'a89c6188-1350-11df-a1f1-0026b9348838')\n" +
      ")\n" +
      "\n" +
      "SELECT \n" +
      "o.person_id,\n" +
      "o.encounter_id, \n" +
      "e.encounter_datetime,\n" +
      "e.visit_id,\n" +
      "o.location_id,\n" +
      "o.concept_id,\n" +
      "case  when o.concept_id=1342 then '1342AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
      "  when o.concept_id=1343 then '1343AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
      "  when o.concept_id=5085 then '5085AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
      "  when o.concept_id=5086 then '5086AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
      "  when o.concept_id=5087 then '5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
      "  when o.concept_id=5088 then '5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
      "  when o.concept_id=5089 then '5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
      "  when o.concept_id=5090 then '5090AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
      "  when o.concept_id=5092 then '5092AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
      "  when o.concept_id=5242 then '5242AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
      " end  kenyaemr_uuid,\n" +
      "o.obs_datetime,\n" +
      "o.value_numeric\n" +
      "FROM amrs.obs o \n" +
      "INNER JOIN amrs.encounter e using(encounter_id)\n" +
      "INNER JOIN amrs.location l on l.location_id= e.location_id\n" +
      "WHERE o.concept_id IN (SELECT concept_id FROM cte_vitals_concepts) \n" +
      // "AND l.uuid IN(" + locations + ") \n" +
      " AND o.person_id in (" + pid + ") \n" + // ( " + pid + " )
      "GROUP BY o.person_id, o.encounter_id,o.concept_id";

    // System.out.println("SQL is "+ sql);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {
      String patientId = rs.getString("person_id");
      String encounterID = rs.getString("encounter_id");
      String encounterDateTime = rs.getString("encounter_datetime");
      String visitId = rs.getString("visit_id");
      String locationId = rs.getString("location_id");
      String obsDateTime = rs.getString("obs_datetime");
      String obsValue = rs.getString("value_numeric");
      String conceptid = rs.getString("concept_id");
      String kenyaemr_uuid = rs.getString("kenyaemr_uuid");
      String kenyaemrPatientUuid = "";

               /* List<AMRSPatients> amrsPatients = amrsPatientServices.getByPatientID(patientId);
                if (!amrsPatients.isEmpty()) {
                    kenyaemrPatientUuid = amrsPatients.get(0).getKenyaemrpatientUUID();
                }
                */
      List<AMRSTriage> amrsTriageList = amrsTriageService.findByPatientIdAndEncounterIdAndConceptId(patientId, encounterID, conceptid);
      if (amrsTriageList.isEmpty()) {
        AMRSTriage at = new AMRSTriage();
        at.setPatientId(patientId);
        at.setEncounterDateTime(encounterDateTime);
        at.setVisitId(visitId);
        at.setEncounterId(encounterID);
        at.setLocationId(locationId);
        at.setObsDateTime(obsDateTime);
        at.setValue(obsValue);
        at.setConceptId(conceptid);
        at.setKenyaemrPatientUuid(kenyaemrPatientUuid);
        at.setKenyaemConceptId(kenyaemr_uuid);
        at.setKenyaemrFormUuid("37f6bd8d-586a-4169-95fa-5781f987fe62");
        amrsTriageService.save(at);
        System.out.println("Patient_id" + patientId + "Triage encounterID " + encounterID);
      } else {
        System.out.println("Existing Patient_id " + patientId + " Triage encounterID " + encounterID);

      }
    }


    CareOpenMRSPayload.triage(KenyaemrLocationUuid, amrsTranslater, amrsTriageService, amrsPatientServices, amrsEncounterService, amrsVisitService, url, auth);

  }

  public static void newObs(String server, String username, String password, String locations, String parentUUID, AMRSObsService amrsObsService, AMRSTranslater amrsTranslater, AMRSPatientServices amrsPatientServices, AMRSEncounterService amrsEncounterService, String url, String auth) throws SQLException, JSONException, ParseException, IOException {


    List<AMRSPatients> amrsPatientsList = amrsPatientServices.getAll();
    String pidss = "";
    for (int y = 0; y < amrsPatientsList.size(); y++) {
      String pid = amrsPatientsList.get(y).getPersonId();

      System.out.println("PatientIDs " + pid);
      String sql = "";
      sql = "select  e.patient_id,\n" +
        "e.encounter_type,\n" +
        "e.encounter_id,\n" +
        "e.form_id, \n" +
        "o.concept_id, \n" +
        "cn.name as question,\n" +
        "c.datatype_id,\n" +
        "o.obs_datetime, \n" +
        "o.obs_group_id,\n" +
        "et.name AS encounter_type,\n" +
        "COALESCE(cn_answer.name, '') as value_coded_name,\n" +
        "COALESCE(drug.name, '') as drug_name,\n" +
        "case when o.value_coded is not null then  o.value_coded\n" +
        "when o.value_datetime is not null then  o.value_datetime\n" +
        "when o.value_text is not null then o.value_text\n" +
        "when o.value_numeric is not null then o.value_numeric end\n" +
        "as value \n" +
        "from amrs.obs o \n" +
        "inner join amrs.encounter e on e.encounter_id =o.encounter_id\n" +
        "INNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id \n" +
        "inner join amrs.concept c on o.concept_id =c.concept_id\n" +
        "                     AND cn.locale_preferred = 1  and cn.locale='en' and cn .voided=0\n" +
        "\t\t\t\t\t\t-- AND cn.concept_name_type = 'FULLY_SPECIFIED'\n" +
        "                    LEFT JOIN amrs.encounter_type et ON e.encounter_type = et.encounter_type_id -- and et.voided=0\n" +
        "                   LEFT JOIN amrs.location l ON e.location_id = l.location_id\n" +
        "                    LEFT JOIN amrs.concept_name cn_answer ON o.value_coded = cn_answer.concept_id \n" +
        "                        AND cn_answer.locale = 'en' \n" +
        "                        AND cn_answer.concept_name_type = 'FULLY_SPECIFIED'\n" +
        "\t\t\t\tLEFT JOIN amrs.drug ON o.value_drug = drug.drug_id\n" +
        " where o.person_id in (1212603)\n" +
        " and o.voided =0  and e.encounter_type=1 ";

      System.out.println("locations " + locations + " parentUUID " + parentUUID);
      Connection con = DriverManager.getConnection(server, username, password);
      int x = 0;
      Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
      ResultSet rs = stmt.executeQuery(sql);
      rs.last();
      x = rs.getRow();
      rs.beforeFirst();
      while (rs.next()) {

        String conceptId = rs.getString("concept_id");
        String patientId = rs.getString("patient_id");
        String encounterId = rs.getString("encounter_id");
        String encounterType = rs.getString("encounter_type");
        String datatypeId = rs.getString("datatype_id");
        String question = rs.getString("question");
        String formId = rs.getString("form_id");
        String obsDatetime = rs.getString("obs_datetime");
        String value = rs.getString("value");
        String valueCodedName = rs.getString("value_coded_name");

        List<AMRSPatients> amrsPatients = amrsPatientServices.getByPatientID(patientId);
        String kenyaemrPatientUuid = "";
        String kenyaemr_concept_uuid = "";
        String kenyaemr_encounter_uuid = "";
        if (amrsPatients.size() > 0) {
          kenyaemrPatientUuid = amrsPatients.get(0).getKenyaemrpatientUUID();
        }

        kenyaemr_concept_uuid = amrsTranslater.translater(conceptId);
        String kenyaemr_value_uuid = "";
        if (datatypeId.equals("2")) {
          kenyaemr_value_uuid = amrsTranslater.translater(value);
        } else {
          kenyaemr_value_uuid = value;
        }

        List<AMRSEncounters> amrsEncounters = amrsEncounterService.findByEncounterId(encounterId);
        if (!amrsEncounters.isEmpty()) {
          kenyaemr_encounter_uuid = amrsEncounters.get(0).getKenyaemrEncounterUuid();
        }

        List<AMRSObs> amrsObsList = amrsObsService.findByPatientIdAndEncounterIDAndConceptId(patientId, encounterId, conceptId);
        if (amrsObsList.isEmpty()) {
          AMRSObs ao = new AMRSObs();
          ao.setConceptId(conceptId);
          ao.setPatientId(patientId);
          if (kenyaemr_concept_uuid != null || !kenyaemr_concept_uuid.isEmpty()) {
            ao.setKenyaemrconceptuuid(kenyaemr_concept_uuid);
          }
          ao.setEncounterId(encounterId);
          ao.setValueType(value);
          ao.setEncounterType(encounterType);
          ao.setObsDatetime(obsDatetime);
          ao.setDataType(datatypeId);
          ao.setValue(value);
          ao.setAmrsQuestion(question);
          ao.setFormId(formId);
          ao.setKenyaemrvalue(kenyaemr_value_uuid);
          ao.setValuecodedName(valueCodedName);
          ao.setKenyaemrlocationuuid("c55535b8-b9f2-4a97-8c6c-4ea9496256df");
          ao.setKenyaemrpersonuuid(kenyaemrPatientUuid);
          ao.setKenyaemrencounteruuid(kenyaemr_encounter_uuid);
          amrsObsService.save(ao);
          System.out.println("Patient_id" + patientId + "ObsID " + obsDatetime);
        } else {
          System.out.println("Existing Patient_id " + patientId);

        }
      }


      ObsPayload.newObs(amrsObsService, amrsTranslater, amrsPatientServices, amrsEncounterService, url, auth);
    }

  }

  public static void encounterMappings(String server, String username, String password, String locations, String parentUUID, AMRSEncounterMappingService amrsEncounterMappingService, AMRSPatientServices amrsPatientServices, AMRSConceptMappingService amrsConceptMappingService, String url, String auth) throws SQLException, JSONException, ParseException, IOException {
    String sql = "select \n" +
      " encounter_type_id,\n" +
      " case \n" +
      " when e.encounter_type_id =110 then 6\n" +
      " when e.encounter_type_id = 270 then 8\n" +
      " when e.encounter_type_id = 23 then 303\n" +
      " when e.encounter_type_id = 22 then 303\n" +
      " when e.encounter_type_id = 280 then 302\n" +
      " when e.encounter_type_id = 1 then 7\n" +
      " when e.encounter_type_id = 14 then 21\n" +
      " when e.encounter_type_id = 14 then 8\n" +
      " when e.encounter_type_id = 16 then 6\n" +
      " when e.encounter_type_id = 16 then 252\n" +
      " when e.encounter_type_id = 2 then 8\n" +
      " when e.encounter_type_id = 265 then 14\n" +
      " when e.encounter_type_id = 264 then 15\n" +
      " when e.encounter_type_id = 32 then 14\n" +
      " when e.encounter_type_id = 33 then 15\n" +
      " when e.encounter_type_id = 242 then 21\n" +
      " when e.encounter_type_id = 294 then 73\n" +
      " when e.encounter_type_id = 13 then 300\n" +
      " when e.encounter_type_id = 138 then 8\n" +
      " when e.encounter_type_id = 252 then 243\n" +
      " when e.encounter_type_id = 209 then 243\n" +
      " when e.encounter_type_id = 208 then 243\n" +
      " when e.encounter_type_id = 234 then 10\n" +
      " when e.encounter_type_id = 233 then 9\n" +
      " when e.encounter_type_id = 31 then 2\n" +
      " when e.encounter_type_id = 127 then 8\n" +
      " when e.encounter_type_id = 144 then 8\n" +
      " when e.encounter_type_id = 153 then 8\n" +
      " when e.encounter_type_id = 181 then 21\n" +
      " when e.encounter_type_id = 182 then 21\n" +
      " when e.encounter_type_id = 248 then 24\n" +
      " when e.encounter_type_id = 249 then 29\n" +
      " when e.encounter_type_id = 203 then 24\n" +
      " when e.encounter_type_id = 186 then 21\n" +
      " when e.encounter_type_id = 19 then 21\n" +
      " when e.encounter_type_id = 20 then 21\n" +
      " when e.encounter_type_id = 26 then 21\n" +
      " when e.encounter_type_id = 17 then 21\n" +
      " when e.encounter_type_id = 18 then 21\n" +
      " when e.encounter_type_id = 279 then 303\n" +
      " when e.encounter_type_id = 157 then 2\n" +
      " when e.encounter_type_id = 250 then 28\n" +
      " when e.encounter_type_id = 243 then 22\n" +
      " when e.encounter_type_id = 43 then 8\n" +
      " when e.encounter_type_id = 132 then 22\n" +
      " when e.encounter_type_id = 115 then 11\n" +
      " when e.encounter_type_id = 115 then 12\n" +
      " when e.encounter_type_id = 115 then 13\n" +
      " when e.encounter_type_id = 115 then 9\n" +
      " when e.encounter_type_id = 115 then 10\n" +
      " when e.encounter_type_id = 253 then 306\n" +
      " when e.encounter_type_id = 129 then 303\n" +
      " when e.encounter_type_id = 110 then 6\n" +
      " when e.encounter_type_id = 251 then 30\n" +
      " when e.encounter_type_id = 227 then 4\n" +
      " when e.encounter_type_id = 5 then 4\n" +
      " when e.encounter_type_id = 6 then 4\n" +
      " when e.encounter_type_id = 8 then 4\n" +
      " when e.encounter_type_id = 9 then 4\n" +
      " when e.encounter_type_id = 196 then 15\n" +
      " when e.encounter_type_id = 121 then 30\n" +
      " when e.encounter_type_id = 7 then 4\n" +
      " when e.encounter_type_id = 273 then 15\n" +
      " when e.encounter_type_id = 274 then 15\n" +
      " when e.encounter_type_id = 237 then 13\n" +
      " when e.encounter_type_id = 235 then 11\n" +
      " when e.encounter_type_id = 236 then 12\n" +
      " when e.encounter_type_id = 239 then 15\n" +
      " when e.encounter_type_id = 240 then 16\n" +
      " when e.encounter_type_id = 238 then 14\n" +
      " when e.encounter_type_id = 268 then 15\n" +
      " when e.encounter_type_id = 140 then 303\n" +
      " when e.encounter_type_id = 114 then 8\n" +
      " when e.encounter_type_id = 120 then 21\n" +
      " when e.encounter_type_id = 168 then 252\n" +
      " when e.encounter_type_id = 284 then 36\n" +
      " when e.encounter_type_id = 285 then 35\n" +
      " when e.encounter_type_id = 283 then 34\n" +
      " when e.encounter_type_id = 21 then 31\n" +
      " when e.encounter_type_id = 220 then 33\n" +
      " when e.encounter_type_id = 214 then 32\n" +
      " when e.encounter_type_id = 282 then 302\n" +
      " when e.encounter_type_id = 3 then 7\n" +
      " when e.encounter_type_id = 15 then 21\n" +
      " when e.encounter_type_id = 80 then 252\n" +
      " when e.encounter_type_id = 4 then 8\n" +
      " when e.encounter_type_id = 67 then 8\n" +
      " when e.encounter_type_id = 162 then 8\n" +
      " when e.encounter_type_id = 10 then 15\n" +
      " when e.encounter_type_id = 44 then 15\n" +
      " when e.encounter_type_id = 125 then 15\n" +
      " when e.encounter_type_id = 11 then 15\n" +
      " when e.encounter_type_id = 47 then 15\n" +
      " when e.encounter_type_id = 46 then 15\n" +
      " when e.encounter_type_id = 266 then 15\n" +
      " when e.encounter_type_id = 267 then 15\n" +
      " when e.encounter_type_id = 111 then 30\n" +
      " when e.encounter_type_id = 34 then 15\n" +
      " when e.encounter_type_id = 133 then 51\n" +
      " when e.encounter_type_id = 263 then 38\n" +
      " when e.encounter_type_id = 263 then 51\n" +
      " when e.encounter_type_id = 262 then 50\n" +
      " when e.encounter_type_id = 134 then 38\n" +
      " when e.encounter_type_id = 117 then 8\n" +
      " when e.encounter_type_id = 176 then 8\n" +
      " when e.encounter_type_id = 116 then 2\n" +
      " when e.encounter_type_id = 119 then 8\n" +
      " when e.encounter_type_id = 221 then 6\n" +
      " when e.encounter_type_id = 158 then 8\n" +
      " when e.encounter_type_id = 281 then 302\n" +
      " when e.encounter_type_id = 105 then 7\n" +
      " when e.encounter_type_id = 106 then 8\n" +
      " when e.encounter_type_id = 163 then 8\n" +
      " when e.encounter_type_id = 137 then 7\n" +
      "  end kenyaem_encounter_id\n" +
      " ,\n" +
      "  case \n" +
      "  when e.encounter_type_id =110 then 'd1059fb9-a079-4feb-a749-eedd709ae542'\n" +
      "  when e.encounter_type_id = 270 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 23 then '54df6991-13de-4efc-a1a9-2d5ac1b72ff8'\n" +
      "  when e.encounter_type_id = 22 then '54df6991-13de-4efc-a1a9-2d5ac1b72ff8'\n" +
      "  when e.encounter_type_id = 280 then 'ec2a91e5-444a-4ca0-87f1-f71ddfaf57eb'\n" +
      "  when e.encounter_type_id = 1 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
      "  when e.encounter_type_id = 14 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
      "  when e.encounter_type_id = 14 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 16 then 'd1059fb9-a079-4feb-a749-eedd709ae542'\n" +
      "  when e.encounter_type_id = 16 then '160fcc03-4ff5-413f-b582-7e944a770bed'\n" +
      "  when e.encounter_type_id = 2 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 265 then '3ee036d8-7c13-4393-b5d6-036f2fe45126'\n" +
      "  when e.encounter_type_id = 264 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id = 32 then '3ee036d8-7c13-4393-b5d6-036f2fe45126'\n" +
      "  when encounter_type_id = 33 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id = 242 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
      "  when e.encounter_type_id = 294 then '5021b1a1-e7f6-44b4-ba02-da2f2bcf8718'\n" +
      "  when e.encounter_type_id = 13 then 'e360f35f-e496-4f01-843b-e2894e278b5b'\n" +
      "  when e.encounter_type_id = 138 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 252 then '86709cfc-1490-11ec-82a8-0242ac130003'\n" +
      "  when e.encounter_type_id = 209 then '86709cfc-1490-11ec-82a8-0242ac130003'\n" +
      "  when e.encounter_type_id = 208 then '86709cfc-1490-11ec-82a8-0242ac130003'\n" +
      "  when e.encounter_type_id = 234 then 'bcc6da85-72f2-4291-b206-789b8186a021'\n" +
      "  when e.encounter_type_id = 233 then '415f5136-ca4a-49a8-8db3-f994187c3af6'\n" +
      "  when e.encounter_type_id = 31 then '2bdada65-4c72-4a48-8730-859890e25cee'\n" +
      "  when e.encounter_type_id = 127 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 144 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 153 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
      "  when e.encounter_type_id = 181 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
      "  when e.encounter_type_id = 182 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
      "  when e.encounter_type_id = 248 then '7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3'\n" +
      "  when e.encounter_type_id = 249 then '7dffc392-13e7-11e9-ab14-d663bd873d93'\n" +
      "  when e.encounter_type_id = 203 then '7df67b83-1b84-4fe2-b1b7-794b4e9bfcc3'\n" +
      "  when e.encounter_type_id = 186 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
      "  when e.encounter_type_id = 19 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
      "  when e.encounter_type_id = 20 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
      "  when e.encounter_type_id = 26 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
      "  when e.encounter_type_id = 17 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
      "  when e.encounter_type_id = 18 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
      "  when e.encounter_type_id = 279 then '54df6991-13de-4efc-a1a9-2d5ac1b72ff8'\n" +
      "  when e.encounter_type_id = 157 then '2bdada65-4c72-4a48-8730-859890e25cee'\n" +
      "  when e.encounter_type_id = 250 then '9bc15e94-2794-11e8-b467-0ed5f89f718b'\n" +
      "  when e.encounter_type_id = 243 then '975ae894-7660-4224-b777-468c2e710a2a'\n" +
      "  when e.encounter_type_id = 43 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 132 then '975ae894-7660-4224-b777-468c2e710a2a'\n" +
      "  when e.encounter_type_id = 115 then '01894f88-dc73-42d4-97a3-0929118403fb'\n" +
      "  when e.encounter_type_id = 115 then '82169b8d-c945-4c41-be62-433dfd9d6c86'\n" +
      "  when e.encounter_type_id = 115 then '5feee3f1-aa16-4513-8bd0-5d9b27ef1208'\n" +
      "  when e.encounter_type_id = 115 then '415f5136-ca4a-49a8-8db3-f994187c3af6'\n" +
      "  when e.encounter_type_id = 115 then 'bcc6da85-72f2-4291-b206-789b8186a021'\n" +
      "  when e.encounter_type_id = 253 then 'bfbb5dc2-d3e6-41ea-ad86-101336e3e38f'\n" +
      "  when e.encounter_type_id = 129 then '54df6991-13de-4efc-a1a9-2d5ac1b72ff8'\n" +
      "  when e.encounter_type_id = 110 then 'd1059fb9-a079-4feb-a749-eedd709ae542'\n" +
      "  when e.encounter_type_id = 251 then 'e1406e88-e9a9-11e8-9f32-f2801f1b9fd1'\n" +
      "  when e.encounter_type_id = 227 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
      "  when e.encounter_type_id = 5 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
      "  when e.encounter_type_id = 6 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
      "  when e.encounter_type_id = 8 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
      "  when e.encounter_type_id = 9 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
      "  when e.encounter_type_id = 196 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id = 121 then 'e1406e88-e9a9-11e8-9f32-f2801f1b9fd1'\n" +
      "  when e.encounter_type_id = 7 then '17a381d1-7e29-406a-b782-aa903b963c28'\n" +
      "  when e.encounter_type_id = 273 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id = 274 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id = 237 then '5feee3f1-aa16-4513-8bd0-5d9b27ef1208'\n" +
      "  when e.encounter_type_id = 235 then '01894f88-dc73-42d4-97a3-0929118403fb'\n" +
      "  when e.encounter_type_id = 236 then '82169b8d-c945-4c41-be62-433dfd9d6c86'\n" +
      "  when e.encounter_type_id = 239 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id = 240 then '7c426cfc-3b47-4481-b55f-89860c21c7de'\n" +
      "  when e.encounter_type_id = 238 then '3ee036d8-7c13-4393-b5d6-036f2fe45126'\n" +
      "  when e.encounter_type_id = 268 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id = 140 then '54df6991-13de-4efc-a1a9-2d5ac1b72ff8'\n" +
      "  when e.encounter_type_id = 114 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 120 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
      "  when e.encounter_type_id = 168 then '160fcc03-4ff5-413f-b582-7e944a770bed'\n" +
      "  when e.encounter_type_id = 284 then '162386c8-0464-11ea-9a9f-362b9e155667'\n" +
      "  when e.encounter_type_id = 285 then '162382b8-0464-11ea-9a9f-362b9e155667'\n" +
      "  when e.encounter_type_id = 283 then '16238574-0464-11ea-9a9f-362b9e155667'\n" +
      "  when e.encounter_type_id = 21 then '1495edf8-2df2-11e9-b210-d663bd873d93'\n" +
      "  when e.encounter_type_id = 220 then '5cf00d9e-09da-11ea-8d71-362b9e155667'\n" +
      "  when e.encounter_type_id = 214 then '5cf0124e-09da-11ea-8d71-362b9e155667'\n" +
      "  when e.encounter_type_id = 282 then 'ec2a91e5-444a-4ca0-87f1-f71ddfaf57eb'\n" +
      "  when e.encounter_type_id = 3 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
      "  when e.encounter_type_id = 15 then 'e87aa2ad-6886-422e-9dfd-064e3bfe3aad'\n" +
      "  when e.encounter_type_id = 80 then '160fcc03-4ff5-413f-b582-7e944a770bed'\n" +
      "  when e.encounter_type_id = 4 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 67 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 162 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 10 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id = 44 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id = 125 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id = 11 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id = 47 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id = 46 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id = 266 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id = 267 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id = 111 then 'e1406e88-e9a9-11e8-9f32-f2801f1b9fd1'\n" +
      "  when e.encounter_type_id = 34 then 'c6d09e05-1f25-4164-8860-9f32c5a02df0'\n" +
      "  when e.encounter_type_id =133 then '706a8b12-c4ce-40e4-aec3-258b989bf6d3'\n" +
      "  when e.encounter_type_id = 263 then 'c4a2be28-6673-4c36-b886-ea89b0a42116'\n" +
      "  when e.encounter_type_id = 263 then '706a8b12-c4ce-40e4-aec3-258b989bf6d3'\n" +
      "  when e.encounter_type_id = 262 then '291c0828-a216-11e9-a2a3-2a2ae2dbcce4'\n" +
      "  when e.encounter_type_id = 134 then 'c4a2be28-6673-4c36-b886-ea89b0a42116'\n" +
      "  when e.encounter_type_id = 117 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 176 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 116 then '2bdada65-4c72-4a48-8730-859890e25cee'\n" +
      "  when e.encounter_type_id = 119 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 221 then 'd1059fb9-a079-4feb-a749-eedd709ae542'\n" +
      "  when e.encounter_type_id = 158 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 281 then 'ec2a91e5-444a-4ca0-87f1-f71ddfaf57eb'\n" +
      "  when e.encounter_type_id = 105 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
      "  when e.encounter_type_id = 106 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 163 then 'a0034eee-1940-4e35-847f-97537a35d05e'\n" +
      "  when e.encounter_type_id = 137 then 'de78a6be-bfc5-4634-adc3-5f1a280455cc'\n" +
      "  end kenyaem_encounter_uuid\n" +
      " from  amrs.encounter_type e\n" +
      " -- order by e.encounter_id desc";

    System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {
      String encounterTypeId = rs.getString("encounter_type_id");
      String kenyaemEncounterId = rs.getString("kenyaem_encounter_id");
      String kenyaemEncounterUuid = rs.getString("kenyaem_encounter_uuid");

      List<AMRSEncountersMapping> amList = amrsEncounterMappingService.getByAmrsID(encounterTypeId);
      if (amList.size() > 0) {
        AMRSEncountersMapping am = amList.get(0);
        am.setAmrsEncounterTypeId(encounterTypeId);
        am.setKenyaemrEncounterTypeId(kenyaemEncounterId);
        am.setKenyaemrEncounterTypeUuid(kenyaemEncounterUuid);
        amrsEncounterMappingService.save(am);
      } else {

        AMRSEncountersMapping am = new AMRSEncountersMapping();
        am.setAmrsEncounterTypeId(encounterTypeId);
        am.setKenyaemrEncounterTypeId(kenyaemEncounterId);
        am.setKenyaemrEncounterTypeUuid(kenyaemEncounterUuid);
        amrsEncounterMappingService.save(am);
      }

    }


  }


  public static void hivenrollment(String server, String username, String password, String KenyaEMRlocationUuid, AMRSHIVEnrollmentService amrsHIVEnrollmentService, AMRSTranslater amrsTranslater, AMRSPatientServices amrsPatientServices, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    // String samplePatientList = AMRSSamples.getPersonIdList();
    //  String samplePatientList = "7315,1171851,1174041,1188232,1072350,1212684,1209134";
    List<String> stringPIDsList = amrsPatientServices.getAllPatientID();
    String samplePatientList = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);

    String sql = "";
    List<AMRSHIVEnrollment> amrshivEnrollmentLists = amrsHIVEnrollmentService.findFirstByOrderByIdDesc();
    String nextEncounterID = "";
    if (amrshivEnrollmentLists.isEmpty()) {

      sql = "SELECT   \n" +
        "                                              o.person_id,  \n" +
        "                                              e.encounter_id,  \n" +
        "                                              e.visit_id,  \n" +
        "                                              e.encounter_datetime,  \n" +
        "                                              e.encounter_type,  \n" +
        "                                              l.uuid AS location_uuid,  \n" +
        "                                              o.concept_id,  \n" +
        "                                              cn.name AS concept_name,  \n" +
        "                                               o.obs_datetime,  \n" +
        "                                              COALESCE(o.value_coded,  \n" +
        "                                                      o.value_datetime,  \n" +
        "                                                      o.value_numeric,  \n" +
        "                                                      o.value_text) AS value,  \n" +
        "                                              cd.name AS value_type,  \n" +
        "                                              c.datatype_id,  \n" +
        "                                              et.name AS encounterName,  \n" +
        "                                              e.creator AS provider_id,  \n" +
        "                                              'HIV Enrollment' AS Category  \n" +
        "                                          FROM  \n" +
        "                                              amrs.obs o  \n" +
        "                                                  INNER JOIN  \n" +
        "                                              amrs.encounter e ON (o.encounter_id = e.encounter_id)  \n" +
        "                                                  INNER JOIN  \n" +
        "                                              amrs.encounter_type et ON et.encounter_type_id = e.encounter_type  \n" +
        "                                                  INNER JOIN  \n" +
        "                                              amrs.concept c ON c.concept_id = o.concept_id  \n" +
        "                                                  INNER JOIN  \n" +
        "                                              amrs.concept_name cn ON o.concept_id = cn.concept_id  and cn.locale_preferred = 1 \n" +
        "                                                  INNER JOIN  \n" +
        "                                              amrs.concept_datatype cd ON cd.concept_datatype_id = c.datatype_id  \n" +
        "                                                  INNER JOIN  \n" +
        "                                              amrs.location l ON e.location_id = l.location_id  \n" +
        "                                          WHERE  \n" +
        "                                                  o.concept_id IN(6749,10747,10748,7013,1499,9203,6748,1633,2155,5356,966,1088,5419,10804,6032,6176,5272,5629,1174,7013,1499,9203,6748) -- 5356 \n" +
        "                                                  and e.patient_id in ("+ samplePatientList+")  \n" +
        "                                                  AND e.voided = 0 \n" +
        "                                                 group by o.concept_id,e.patient_id\n" +
        "                                          ORDER BY o.encounter_id   ASC \n" +
        "                                           ";
    } else {
      System.out.println("List" + amrshivEnrollmentLists);
      nextEncounterID = amrshivEnrollmentLists.get(0).getEncounterID();
      sql = "SELECT   \n" +
        "                                              o.person_id,  \n" +
        "                                              e.encounter_id,  \n" +
        "                                              e.visit_id,  \n" +
        "                                              e.encounter_datetime,  \n" +
        "                                              e.encounter_type,  \n" +
        "                                              l.uuid AS location_uuid,  \n" +
        "                                              o.concept_id,  \n" +
        "                                              cn.name AS concept_name,  \n" +
        "                                               o.obs_datetime,  \n" +
        "                                              COALESCE(o.value_coded,  \n" +
        "                                                      o.value_datetime,  \n" +
        "                                                      o.value_numeric,  \n" +
        "                                                      o.value_text) AS value,  \n" +
        "                                              cd.name AS value_type,  \n" +
        "                                              c.datatype_id,  \n" +
        "                                              et.name AS encounterName,  \n" +
        "                                              e.creator AS provider_id,  \n" +
        "                                              'HIV Enrollment' AS Category  \n" +
        "                                          FROM  \n" +
        "                                              amrs.obs o  \n" +
        "                                                  INNER JOIN  \n" +
        "                                              amrs.encounter e ON (o.encounter_id = e.encounter_id)  \n" +
        "                                                  INNER JOIN  \n" +
        "                                              amrs.encounter_type et ON et.encounter_type_id = e.encounter_type  \n" +
        "                                                  INNER JOIN  \n" +
        "                                              amrs.concept c ON c.concept_id = o.concept_id  \n" +
        "                                                  INNER JOIN  \n" +
        "                                              amrs.concept_name cn ON o.concept_id = cn.concept_id  and cn.locale_preferred = 1 \n" +
        "                                                  INNER JOIN  \n" +
        "                                              amrs.concept_datatype cd ON cd.concept_datatype_id = c.datatype_id  \n" +
        "                                                  INNER JOIN  \n" +
        "                                              amrs.location l ON e.location_id = l.location_id  \n" +
        "                                          WHERE  \n" +
        "                                                  o.concept_id IN(6749,10747,10748,7013,1499,9203,6748,1633,2155,5356,966,1088,5419,10804,6032,6176,5272,5629,1174,7013,1499,9203,6748) -- 5356 \n" +
        "                                                  and e.patient_id in ("+ samplePatientList+")  \n" +
        "                                                  AND e.voided = 0 \n" +
        "                                                 group by o.concept_id,e.patient_id\n" +
        "                                          ORDER BY o.encounter_id   ASC \n" +
        "                                           ";
    }
    System.out.println("sqlHivEnrollment" + sql);
    //  System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {
      String patientId = rs.getString("person_id");
      String conceptId = rs.getString("concept_id");
      String encounterID = rs.getString("encounter_id");
      String visit_Id = rs.getString("visit_id");
      String locationUuid = rs.getString("location_uuid");
      String encounterDatetime = rs.getString("encounter_datetime");
      String encounterType = rs.getString("encounter_type");
      String conceptName = rs.getString("concept_name");
      String obsDatetime = rs.getString("obs_datetime");
      String value = rs.getString("value");
      String valueType = rs.getString("value_type");
      String datatypeId = rs.getString("datatype_id");
      String provider = rs.getString("provider_id");
      String encounterName = rs.getString("encounterName");
      String category = rs.getString("Category");

      List<AMRSHIVEnrollment> amrshivEnrollmentList = amrsHIVEnrollmentService.findByPatientIdAndEncounterIDAndConceptId(patientId, encounterID, conceptId);
      if (amrshivEnrollmentList.isEmpty()) {
        AMRSHIVEnrollment ahe = new AMRSHIVEnrollment();
        ahe.setPatientId(patientId);
        ahe.setConceptId(conceptId);
        ahe.setLocationUuid(locationUuid);
        ahe.setEncounterID(encounterID);
        ahe.setEncounterDateTime(encounterDatetime);
        ahe.setVisitId(visit_Id);
        ahe.setEncounterType(encounterType);
        ahe.setConceptName(conceptName);
        ahe.setObsDateTime(obsDatetime);
        ahe.setConceptValue(value);
        ahe.setValueDataType(valueType);
        ahe.setDataTypeId(datatypeId);
        ahe.setProvider(provider);
        ahe.setEncounterName(encounterName);
        ahe.setCategory(category);
        ahe.setKenyaemrConceptUuid(amrsTranslater.translater(conceptId));
        if (datatypeId.equals("6")) { // || datatypeId.equals("2")
          ahe.setKenyaemrValue(value);
        } else if (datatypeId.equals("10")) { // || datatypeId.equals("2")
          Boolean bvalue = false;
          if (conceptId.equals("5272")) {
            ahe.setKenyaemrValue(amrsTranslater.translater(value));
          } else {
            if (value.equals("")) {
              bvalue = true;
            }
            ahe.setKenyaemrValue(String.valueOf(bvalue));
          }

        } else {
          ahe.setKenyaemrValue(amrsTranslater.translater(value));
        }
        System.out.println("Tumefika Hapa!!!" + KenyaEMRlocationUuid);
        amrsHIVEnrollmentService.save(ahe);
      }

      System.out.println("Patient_id" + patientId);
    }
    CareOpenMRSPayload.hivEnrollment(amrsHIVEnrollmentService, amrsTranslater, KenyaEMRlocationUuid, url, auth);

  }

    /*public static void obs(String server, String username, String password, String locations,
                           String parentUUID, AMRSEncounterService amrsEncounterService, AMRSObsService amrsObsService, AMRSPatientServices amrsPatientServices,
                           AMRSTranslater amrsConceptReader, String url, String auth) throws SQLException, JSONException,
            ParseException, IOException {

       List<AMRSPatients> amrsPatientsList = amrsPatientServices.getAll();
        if (amrsPatientsList.isEmpty()) {
            System.out.println("No patients found.");
            return;
        }

        List<AMRSObs> newObservations = new ArrayList<>();

        for (AMRSPatients patient : amrsPatientsList) {
            String pid = patient.getPersonId();
            String kenyaemrLocation = patient.getLocation_id();
            String kenyaemrPatientUuid = patient.getKenyaemrpatientUUID();

            if (pid == null) {
             System.out.println("Skipping Patient ID: " + pid );
                continue;
            }

            System.out.println("Processing Patient ID: " + pid);
            // Process database operations
            newObservations.addAll(processPatientObservations(server, username, password, pid, kenyaemrLocation, kenyaemrPatientUuid, amrsEncounterService,
                    amrsObsService, amrsConceptReader));
        }

        // Make a single API call with all new observations
        if (!newObservations.isEmpty()) {
            ObsPayload.obs(amrsObsService, amrsPatientServices, url, auth, newObservations);
        }
    }
    */

   /* private static List<AMRSObs> processPatientObservations(String server, String username,
                                                            String password, String patientId, String location, String patientUuid, AMRSEncounterService amrsEncounterService, AMRSObsService amrsObsService,
                                                            AMRSTranslater amrsConceptReader) throws SQLException {

        List<AMRSObs> newObservations = new ArrayList<>();
        String sql = "select  e.patient_id,\n" +
                "e.encounter_type,\n" +
                "e.form_id, \n" +
                "o.concept_id, \n" +
                "cn.name as question,\n" +
                "c.datatype_id,\n" +
                "o.obs_datetime, \n" +
                "o.obs_group_id,\n" +
                "et.name AS encounter_type,\n" +
                "COALESCE(cn_answer.name, '') as value_coded_name,\n" +
                "COALESCE(drug.name, '') as drug_name,\n" +
                "case when o.value_coded is not null then  o.value_coded\n" +
                "when o.value_datetime is not null then  o.value_datetime\n" +
                "when o.value_text is not null then o.value_text\n" +
                "when o.value_numeric is not null then o.value_numeric end\n" +
                "as value \n" +
                "from amrs.obs o \n" +
                "inner join amrs.encounter e on e.encounter_id =o.encounter_id\n" +
                "INNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id \n" +
                "inner join amrs.concept c on o.concept_id =c.concept_id\n" +
                "                     AND cn.locale_preferred = 1  and cn.locale='en' and cn .voided=0\n" +
                "\t\t\t\t\t\t-- AND cn.concept_name_type = 'FULLY_SPECIFIED'\n" +
                "                    LEFT JOIN amrs.encounter_type et ON e.encounter_type = et.encounter_type_id -- and et.voided=0\n" +
                "                   LEFT JOIN amrs.location l ON e.location_id = l.location_id\n" +
                "                    LEFT JOIN amrs.concept_name cn_answer ON o.value_coded = cn_answer.concept_id \n" +
                "                        AND cn_answer.locale = 'en' \n" +
                "                        AND cn_answer.concept_name_type = 'FULLY_SPECIFIED'\n" +
                "\t\t\t\tLEFT JOIN amrs.drug ON o.value_drug = drug.drug_id\n" +
                " where o.person_id=1169125\n" +
                " and o.voided =0";

        try (Connection con = DriverManager.getConnection(server, username, password);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            //stmt.setString(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                int x = 0;
                rs.last();
                x = rs.getRow();
                rs.beforeFirst();
                while (rs.next()) {

                    AMRSObs observation = processObservationRow(rs, amrsEncounterService, amrsObsService, amrsConceptReader, location, patientUuid);
                    if (observation != null) {

                        newObservations.add(observation);
                        amrsObsService.save(observation);
                    }
                }
            }
        }
        return newObservations;
    }
    */

   /* private static AMRSObs processObservationRow(ResultSet rs, AMRSEncounterService amrsEncounterService, AMRSObsService amrsObsService,
                                                 AMRSTranslater amrsConceptReader, String location, String patientUuid) throws SQLException {

        String conceptId = rs.getString("concept_id");
        String personId = rs.getString("person_id");
        String encounterId = rs.getString("encounter_id");
        String encounterType = rs.getString("encounter_type");
        String datatypeId = rs.getString("datatype_id");
        String question = rs.getString("question");
        String formId = rs.getString("form_id");
        String obsDatetime = rs.getString("obs_datetime");
        String value = rs.getString("value");
        String valueCodedName = rs.getString("value_coded_name");

        Map<String, AMRSObs> existingObservations = amrsObsService
                .findByPatientIdAndEncounterIDAndConceptId(personId, encounterId, conceptId)
                .stream()
                .collect(Collectors.toMap(
                        obs -> obs.getPatientId() + "_" + obs.getEncounterID() + "_" + obs.getConceptId(),
                        obs -> obs
                ));
        String kenyaemr_encounter_uuid = "";
        String kenyaemr_concept_uuid = "";


        List<AMRSEncounters> amrsEncounters = amrsEncounterService.findByEncounterId(encounterId);
        if(!amrsEncounters.isEmpty()) {
            kenyaemr_encounter_uuid = amrsEncounters.get(0).getKenyaemrEncounterUuid();
        }

        kenyaemr_concept_uuid = amrsConceptReader.translater(conceptId);

        // Check if kenyaemr_concept_uuid is valid
        if (kenyaemr_concept_uuid == null || kenyaemr_concept_uuid.isEmpty()) {
            System.out.println("Invalid kenyaemr_concept_uuid. Cannot proceed.");
            return null;
        }

        if (existingObservations.isEmpty()) {
            AMRSObs ao = new AMRSObs();
            ao.setConceptId(conceptId);
            ao.setPatientId(personId);
            ao.setKenyaemrconceptuuid(kenyaemr_concept_uuid);
            ao.setEncounterID(encounterId);
            ao.setValueType(value);
            ao.setEncounterType(encounterType);
            ao.setObsDatetime(obsDatetime);
            ao.setDataType(datatypeId);
            ao.setValue(value);
            ao.setAmrsQuestion(question);
            ao.setFormId(formId);
            ao.setValuecodedName(valueCodedName);
            ao.setKenyaemrlocationuuid(location);
            ao.setKenyaemrpersonuuid(patientUuid);
            ao.setKenyaemrencounteruuid(kenyaemr_encounter_uuid);
            System.out.println("New AMRS obs: " + ao);
            return ao;
        }

        System.out.println("Existing observation found");
        return null;
    }
    */

  public static void DrugSwitches(String server, String username, String password, String KenyaEMRlocationUuid, AMRSRegimenSwitchService amrsRegimenSwitchService, AMRSTranslater amrsTranslater, AMRSPatientServices amrsPatientServices, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    //String samplePatientList = AMRSSamples.getPersonIdList();
    //  String samplePatientList = "7315,1171851,1174041,1188232,1072350,1212684,1209134";
    List<String> stringPIDsList = amrsPatientServices.getAllPatientID();
    String samplePatientList = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);

    String sql = "";
    List<AMRSRegimenSwitch> amrsRegimenSwitchList = amrsRegimenSwitchService.findFirstByOrderByIdDesc();
    String nextEncounterID = "";
    if (amrsRegimenSwitchList.isEmpty()) {

      sql = "SELECT patient_id,MIN(regimen_data.enc_id) AS Encounter_ID,MIN(regimen_data.visit_id) AS Visit_Id, \n" +
        "concept_id,value_coded,Encounter_Date,GROUP_CONCAT(concept_name SEPARATOR \",\") as Regimen,Reason_for_Change FROM \n" +
        "(\n" +
        "\tSELECT o.person_id as patient_id,o.encounter_id as enc_id,e.visit_id,o.concept_id,o.value_coded,o.voided,e.encounter_datetime AS Encounter_Date, cn.name as concept_name  \n" +
        "\t\tfrom amrs.obs o\n" +
        "\t\tINNER JOIN amrs.concept_name cn ON o.value_coded=cn.concept_id and cn.locale='en' and cn.concept_name_type='FULLY_SPECIFIED' \n" +
        "\t\tINNER JOIN amrs.encounter e ON e.encounter_id=o.encounter_id and e.voided=0 \n" +
        "\twhere o.concept_id=1088 and o.voided=0 \n" +
        //"    and o.location_id in (339)\n" +
        "    and o.person_id in (" + samplePatientList + ") \n" + //("+  samplePatientList +")
        "\tGROUP BY patient_id, o.value_coded \n" +
        ")\n" +
        " as regimen_data\n" +
        " LEFT OUTER JOIN (\n" +
        "\t SELECT encounter_id,cn.name AS Reason_for_Change\n" +
        "\t\t FROM amrs.obs o \n" +
        "\t\t INNER JOIN amrs.concept_name cn ON o.value_coded=cn.concept_id and o.voided=0 \n" +
        "\t AND o.concept_id in (1252, 1262, 1266,1269) and cn.locale='en'  and cn.concept_name_type='FULLY_SPECIFIED' \n" +
        "\t GROUP BY encounter_id \n" +
        " ) as reg_fail \n" +
        " ON regimen_data.enc_id=reg_fail.encounter_id\n" +
        " \n" +
        " GROUP BY enc_id, patient_id ORDER BY patient_id ASC,Encounter_Date ASC \n";
    } else {
      System.out.println("List" + amrsRegimenSwitchList);
//            nextEncounterID = amrsRegimenSwitchList.get(0).getEncounterID();
      sql = "SELECT patient_id,MIN(regimen_data.enc_id) AS Encounter_ID,MIN(regimen_data.visit_id) AS Visit_Id, \n" +
        "concept_id,value_coded,Encounter_Date,GROUP_CONCAT(concept_name SEPARATOR \",\") as Regimen,Reason_for_Change FROM \n" +
        "(\n" +
        "\tSELECT o.person_id as patient_id,o.encounter_id as enc_id,e.visit_id,o.concept_id,o.value_coded,o.voided,e.encounter_datetime AS Encounter_Date, cn.name as concept_name  \n" +
        "\t\tfrom amrs.obs o\n" +
        "\t\tINNER JOIN amrs.concept_name cn ON o.value_coded=cn.concept_id and cn.locale='en' and cn.concept_name_type='FULLY_SPECIFIED' \n" +
        "\t\tINNER JOIN amrs.encounter e ON e.encounter_id=o.encounter_id and e.voided=0 \n" +
        "\twhere o.concept_id=1088 and o.voided=0 \n" +
        // "    and o.location_id in (339)\n" +
        "    and o.person_id in (" + samplePatientList + ")\n" + //"+  samplePatientList +"
        "\tGROUP BY patient_id, o.value_coded \n" +
        ")\n" +
        " as regimen_data\n" +
        " LEFT OUTER JOIN (\n" +
        "\t SELECT encounter_id,cn.name AS Reason_for_Change\n" +
        "\t\t FROM amrs.obs o \n" +
        "\t\t INNER JOIN amrs.concept_name cn ON o.value_coded=cn.concept_id and o.voided=0 \n" +
        "\t AND o.concept_id in (1252, 1262, 1266,1269) and cn.locale='en'  and cn.concept_name_type='FULLY_SPECIFIED' \n" +
        "\t GROUP BY encounter_id \n" +
        " ) as reg_fail \n" +
        " ON regimen_data.enc_id=reg_fail.encounter_id\n" +
        " \n" +
        " GROUP BY enc_id, patient_id ORDER BY patient_id ASC,Encounter_Date ASC \n";
    }
    System.out.println("regimenSwitchList" + sql);
    // System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {
      String patientId = rs.getString("patient_id");
      String encounterId = rs.getString("Encounter_ID");
      String visitId = rs.getString("visit_id");
      String conceptId = rs.getString("concept_id");
      String valueCoded = rs.getString("value_coded");
      String encounterDatetime = rs.getString("Encounter_Date");
      String regimen = rs.getString("Regimen");
      String reasonForChange = rs.getString("Reason_for_Change");
      String kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(patientId);
      String kenyaemrConceptUuid = amrsTranslater.translater("1193");
      String kenyaemrValue = amrsTranslater.translater(valueCoded);

      if (amrsRegimenSwitchList.isEmpty()) {
        AMRSRegimenSwitch ar = new AMRSRegimenSwitch();
        ar.setPatientId(patientId);
        ar.setEncounterId(encounterId);
        ar.setConceptId(conceptId);
        ar.setValueCoded(valueCoded);
        ar.setEncounterDatetime(encounterDatetime);
        ar.setRegimen(regimen);
        ar.setReasonForChange(reasonForChange);
        ar.setKenyaemrValue(kenyaemrValue);
        ar.setKenyaemrConceptUuid(kenyaemrConceptUuid);
        ar.setKenyaemrPatientUuid(kenyaemrPatientUuid);
        ar.setVisitId(visitId);
        System.out.println("Tumefika Hapa!!!" + KenyaEMRlocationUuid);
        amrsRegimenSwitchService.save(ar);
      }


      CareOpenMRSPayload.amrsRegimenSwitch(amrsRegimenSwitchService, amrsTranslater, KenyaEMRlocationUuid, auth, url);


      System.out.println("Patient_id" + patientId);
    }

    CareOpenMRSPayload.amrsRegimenSwitch(amrsRegimenSwitchService, amrsTranslater, KenyaEMRlocationUuid, auth, url);

  }

  public static void programEnrollments(String server, String username, String password, String locations, String parentUUID, AMRSEnrollmentService amrsEnrollmentService, AMRSEncounterService amrsEncounterService, AMRSConceptMappingService amrsConceptMappingService, String url, String auth) throws SQLException, JSONException, ParseException, IOException {
    String samplePatientList = AMRSSamples.getPersonIdList();
    AMRSTranslater amrsConceptReader = new AMRSTranslater();

    String sql = "";
    List<AMRSEnrollments> amrsEnrollmentsList = amrsEnrollmentService.getAll();
    String nextEncounterID = "";


    if (amrsEnrollmentsList.isEmpty()) {

      sql = "select\n" +
        "\t-- e.*,\n" +
        "\te.patient_id,\n" +
        "\te.encounter_id,\n" +
        "\te.encounter_datetime,\n" +
        "\t-- e.form_id,\n" +
        "\t-- f.name,\n" +
        "\tmax(case f.form_id when '15' then 164144 -- New\n" +
        "     when o.concept_id = 10194 then 160563 --  'Transfer-In'\n" +
        "     when f.form_id then 164931 -- Transit\n" +
        "     else NULL end) as Patient_Type,\n" +
        "\tmax(case when o.concept_id = 2051 and o.value_coded = 2047 then 160539 -- 'VCT'\n" +
        "     when o.concept_id = 2051 and o.value_coded = 2240 then 162050 -- 'ccc'\n" +
        "      when o.concept_id = 2051 and o.value_coded = 2177 then 162050 -- 'PITC Mappend to CCC'\n" +
        "     when 'Medical out patient' then 160542\n" +
        "     when o.concept_id = 2051 and o.value_coded = 1965 then 160542 -- 'OPD'\n" +
        "     when 'Inpatient Adult' then 160536\n" +
        "     when 'Inpatient Child' then 160537\n" +
        "     when o.concept_id = 2051 and o.value_coded = 1776 then 160536 -- 'PMTCT'\n" +
        "     when 'Mother Child Health' then 159937\n" +
        "     when 'TB Clinic' then 160541\n" +
        "     when 'Unknown' then 162050\n" +
        "     when 'Other' then 5622 else o.value_coded end) as Entry_point ,\n" +
        "\t-- null as TI_Facility,\n" +
        "\tmax(case when o.concept_id = 7013 then o.value_datetime else null end) Date_first_enrolled_in_care,\n" +
        "    fhs.transfer_in_date,\n" +
        "    fhs.hiv_start_date as Date_started_art_at_transferring_facility,\n" +
        "    null as Date_confirmed_hiv_positive,\n" +
        "    null as Facility_confirmed_hiv_positive,\n" +
        "    etl.get_arv_names(fhs.cur_arv_meds) as Baseline_arv_use,\n" +
        "    /*(ecase enr.Purpose_of_baseline_arv_use when 'PMTCT' then 1148\n" +
        "     when 'PEP' then 1691\n" +
        "     when 'ART' then 1181 else NULL end) as Purpose_of_baseline_arv_use,\n" +
        "    (case enr.Baseline_arv_regimen when 'AF2D (TDF + 3TC + ATV/r)' then 164512\n" +
        "     when 'AF2A (TDF + 3TC + NVP)' then 162565\n" +
        "     when 'AF2B (TDF + 3TC + EFV)' then 164505\n" +
        "     when 'AF1A (AZT + 3TC + NVP' then 1652\n" +
        "     when 'AF1B (AZT + 3TC + EFV)' then 160124\n" +
        "     when 'AF4B (ABC + 3TC + EFV)' then 162563\n" +
        "     when 'AF4A (ABC + 3TC + NVP)' then 162199\n" +
        "     when 'CF2A (ABC + 3TC + NVP)' then 162199\n" +
        "     when 'CF2D (ABC + 3TC + LPV/r)' then 162200\n" +
        "     when 'CF2B (ABC + 3TC + EFV)' then 162563 else NULL end) as Baseline_arv_regimen,*/\n" +
        "    fhs.cur_who_stage  as Baseline_arv_regimen_line,\n" +
        "    fhs.rtc_date as Baseline_arv_date_last_used,\n" +
        "    case fhs.cur_who_stage when '1' then 1204\n" +
        "     when '2' then 1205\n" +
        "     when '3' then 1206\n" +
        "     when '4' then 1207\n" +
        "     when 'Unknown' then 1067 else NULL end as Baseline_who_stage,\n" +
        "    case \n" +
        "    \twhen fhs.cd4_2 is not null then fhs.cd4_2\n" +
        "    \telse fhs.cd4_1\n" +
        "    end as Baseline_cd4_results,\n" +
        "    case \n" +
        "    \twhen fhs.cd4_2_date is not null then fhs.cd4_2_date\n" +
        "    \telse fhs.cd4_1_date\n" +
        "    end as Baseline_cd4_date,\n" +
        "    case \n" +
        "    \twhen fhs.vl_2 is not null then fhs.vl_2 \n" +
        "    \telse fhs.vl_1\n" +
        "    end as Baseline_vl_results,\n" +
        "    case \n" +
        "    \twhen fhs.vl_2_date is not null then fhs.vl_2_date\n" +
        "    \telse fhs.vl_1_date \n" +
        "    end as Baseline_vl_date,\n" +
        "    case \n" +
        "\t    when fhs.vl_2 <=200 then fhs.vl_2 \n" +
        "\t    when fhs.vl_1 <=200 then fhs.vl_1 \n" +
        "\t    else null\t\n" +
        "    end as Baseline_vl_ldl_results,\n" +
        "    case \n" +
        "\t    when fhs.vl_2_date <=200 then fhs.vl_2_date \n" +
        "\t    when fhs.vl_1_date <=200 then fhs.vl_1_date \n" +
        "\t    else null\n" +
        "    end as Baseline_vl_ldl_date,\n" +
        "    null as Baseline_HBV_Infected,\n" +
        "    case \n" +
        "    \twhen fhs.on_tb_tx = 1 then 1\n" +
        "    \telse 0\n" +
        "    end as Baseline_TB_Infected,\n" +
        "    CASE\n" +
        "    \twhen fhs.is_pregnant = 1 then 1 \n" +
        "    \telse 0\n" +
        "    END as Baseline_Pregnant,\n" +
        "    case \n" +
        "    \twhen fhs.is_mother_breastfeeding = 1 then 1\n" +
        "    \telse 0\n" +
        "    end as Baseline_Breastfeeding,\n" +
        "    fhs.weight as Baseline_Weight,\n" +
        "    fhs.height as Baseline_Height,\n" +
        "\tnull as Baseline_BMI,\n" +
        "    null as Name_of_treatment_supporter,\n" +
        "    null as Relationship_of_treatment_supporter,\n" +
        "    null as reatment_supporter_telephone,\n" +
        "    null as Treatment_supporter_address\n" +
        "from\n" +
        "\tamrs.encounter e\n" +
        "inner join etl.flat_hiv_summary_v15b fhs on (fhs.person_id = e.patient_id)\n" +
        "inner join amrs.obs o on\n" +
        "\te.encounter_id = o.encounter_id\n" +
        "inner join amrs.form f on\n" +
        "\tf.form_id = e.form_id\n" +
        "where\n" +
        "\te.encounter_type in (1 , 3, 24, 32, 105, 137, 135, 136, 265, 266)\n" +
        "\tand e.location_id in (2, 336, 98)\n" +
        "\tand e.voided = 0\n" +
        "\tand o.concept_id in (2051, 7013)\n" +
        "\tand fhs.is_clinical_encounter = 1 and e.patient_id in (" + samplePatientList + ")\n" +
        "group by\n" +
        "\te.patient_id";
    } else {
      System.out.println("List" + amrsEnrollmentsList);
      //            nextEncounterID = amrsRegimenSwitchList.get(0).getEncounterID();
      sql = "select\n" +
        "\te.patient_id,\n" +
        "\te.encounter_id,\n" +
        "\te.encounter_datetime,\n" +
        "\t-- f.name,\n" +
        "\tmax(case f.form_id when '15' then 164144 -- New\n" +
        "     when o.concept_id = 10194 then 160563 --  'Transfer-In'\n" +
        "     when f.form_id then 164931 -- Transit\n" +
        "     else NULL end) as Patient_Type,\n" +
        "\tmax(case when o.concept_id = 2051 and o.value_coded = 2047 then 160539 -- 'VCT'\n" +
        "     when o.concept_id = 2051 and o.value_coded = 2240 then 162050 -- 'ccc'\n" +
        "      when o.concept_id = 2051 and o.value_coded = 2177 then 162050 -- 'PITC Mappend to CCC'\n" +
        "     when 'Medical out patient' then 160542\n" +
        "     when o.concept_id = 2051 and o.value_coded = 1965 then 160542 -- 'OPD'\n" +
        "     when 'Inpatient Adult' then 160536\n" +
        "     when 'Inpatient Child' then 160537\n" +
        "     when o.concept_id = 2051 and o.value_coded = 1776 then 160536 -- 'PMTCT'\n" +
        "     when 'Mother Child Health' then 159937\n" +
        "     when 'TB Clinic' then 160541\n" +
        "     when 'Unknown' then 162050\n" +
        "     when 'Other' then 5622 else o.value_coded end) as Entry_point ,\n" +
        "\t-- null as TI_Facility,\n" +
        "\tmax(case when o.concept_id = 7013 then o.value_datetime else null end) Date_first_enrolled_in_care,\n" +
        "    fhs.transfer_in_date,\n" +
        "    fhs.hiv_start_date as Date_started_art_at_transferring_facility,\n" +
        "    null as Date_confirmed_hiv_positive,\n" +
        "    null as Facility_confirmed_hiv_positive,\n" +
        "    etl.get_arv_names(fhs.cur_arv_meds) as Baseline_arv_use,\n" +
        "    /*(ecase enr.Purpose_of_baseline_arv_use when 'PMTCT' then 1148\n" +
        "     when 'PEP' then 1691\n" +
        "     when 'ART' then 1181 else NULL end) as Purpose_of_baseline_arv_use,\n" +
        "    (case enr.Baseline_arv_regimen when 'AF2D (TDF + 3TC + ATV/r)' then 164512\n" +
        "     when 'AF2A (TDF + 3TC + NVP)' then 162565\n" +
        "     when 'AF2B (TDF + 3TC + EFV)' then 164505\n" +
        "     when 'AF1A (AZT + 3TC + NVP' then 1652\n" +
        "     when 'AF1B (AZT + 3TC + EFV)' then 160124\n" +
        "     when 'AF4B (ABC + 3TC + EFV)' then 162563\n" +
        "     when 'AF4A (ABC + 3TC + NVP)' then 162199\n" +
        "     when 'CF2A (ABC + 3TC + NVP)' then 162199\n" +
        "     when 'CF2D (ABC + 3TC + LPV/r)' then 162200\n" +
        "     when 'CF2B (ABC + 3TC + EFV)' then 162563 else NULL end) as Baseline_arv_regimen,*/\n" +
        "    fhs.cur_who_stage  as Baseline_arv_regimen_line,\n" +
        "    fhs.rtc_date as Baseline_arv_date_last_used,\n" +
        "    case fhs.cur_who_stage when '1' then 1204\n" +
        "     when '2' then 1205\n" +
        "     when '3' then 1206\n" +
        "     when '4' then 1207\n" +
        "     when 'Unknown' then 1067 else NULL end as Baseline_who_stage,\n" +
        "    case \n" +
        "    \twhen fhs.cd4_2 is not null then fhs.cd4_2\n" +
        "    \telse fhs.cd4_1\n" +
        "    end as Baseline_cd4_results,\n" +
        "    case \n" +
        "    \twhen fhs.cd4_2_date is not null then fhs.cd4_2_date\n" +
        "    \telse fhs.cd4_1_date\n" +
        "    end as Baseline_cd4_date,\n" +
        "    case \n" +
        "    \twhen fhs.vl_2 is not null then fhs.vl_2 \n" +
        "    \telse fhs.vl_1\n" +
        "    end as Baseline_vl_results,\n" +
        "    case \n" +
        "    \twhen fhs.vl_2_date is not null then fhs.vl_2_date\n" +
        "    \telse fhs.vl_1_date \n" +
        "    end as Baseline_vl_date,\n" +
        "    case \n" +
        "\t    when fhs.vl_2 <=200 then fhs.vl_2 \n" +
        "\t    when fhs.vl_1 <=200 then fhs.vl_1 \n" +
        "\t    else null\t\n" +
        "    end as Baseline_vl_ldl_results,\n" +
        "    case \n" +
        "\t    when fhs.vl_2_date <=200 then fhs.vl_2_date \n" +
        "\t    when fhs.vl_1_date <=200 then fhs.vl_1_date \n" +
        "\t    else null\n" +
        "    end as Baseline_vl_ldl_date,\n" +
        "    null as Baseline_HBV_Infected,\n" +
        "    case \n" +
        "    \twhen fhs.on_tb_tx = 1 then 1\n" +
        "    \telse 0\n" +
        "    end as Baseline_TB_Infected,\n" +
        "    CASE\n" +
        "    \twhen fhs.is_pregnant = 1 then 1 \n" +
        "    \telse 0\n" +
        "    END as Baseline_Pregnant,\n" +
        "    case \n" +
        "    \twhen fhs.is_mother_breastfeeding = 1 then 1\n" +
        "    \telse 0\n" +
        "    end as Baseline_Breastfeeding,\n" +
        "    fhs.weight as Baseline_Weight,\n" +
        "    fhs.height as Baseline_Height,\n" +
        "\tnull as Baseline_BMI,\n" +
        "    null as Name_of_treatment_supporter,\n" +
        "    null as Relationship_of_treatment_supporter,\n" +
        "    null as reatment_supporter_telephone,\n" +
        "    null as Treatment_supporter_address\n" +
        "from\n" +
        "\tamrs.encounter e\n" +
        "inner join etl.flat_hiv_summary_v15b fhs on (fhs.person_id = e.patient_id)\n" +
        "inner join amrs.obs o on\n" +
        "\te.encounter_id = o.encounter_id\n" +
        "inner join amrs.form f on\n" +
        "\tf.form_id = e.form_id\n" +
        "where\n" +
        "\te.encounter_type in (1 , 3, 24, 32, 105, 137, 135, 136, 265, 266)\n" +
        "\tand e.location_id in (2, 336, 98)\n" +
        "\tand e.voided = 0\n" +
        "\tand o.concept_id in (2051, 7013)\n" +
        "\tand fhs.is_clinical_encounter = 1 and e.patient_id in (" + samplePatientList + ")\n" +
        "group by\n" +
        "\te.patient_id";
    }
    System.out.println("regimenSwitchList" + sql);
    System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {
      String patientId = rs.getString("patient_id");

      if (amrsEnrollmentsList.isEmpty()) {

        AMRSEnrollments ae = new AMRSEnrollments();

        ae.setPatientId(rs.getString("patient_id"));
        ae.setEncounterId(rs.getString("encounter_id"));
        ae.setEncounterDatetime(rs.getString("encounter_datetime"));
        ae.setPatientType(rs.getString("Patient_Type"));
        ae.setEntryPoint(rs.getString("Entry_point"));
        ae.setTransferInDate(rs.getString("transfer_in_date"));
        ae.setDateFirstEnrolledInCare(rs.getString("Date_first_enrolled_in_care"));
        ae.setDateStartedArtAtTransferringFacility(rs.getString("Date_started_art_at_transferring_facility"));
        ae.setBaselineArvUse(rs.getString("Baseline_arv_use"));
        ae.setBaselineArvRegimenLine(rs.getString("Baseline_arv_regimen_line"));
        ae.setBaselineArvDateLastUsed(rs.getString("Baseline_arv_date_last_used"));
        ae.setBaselineWhoStage(rs.getString("Baseline_who_stage"));
        ae.setBaselineCd4Results(rs.getString("Baseline_cd4_results"));
        ae.setBaselineCd4Date(rs.getString("Baseline_cd4_date"));
        ae.setBaselineVlResults(rs.getString("Baseline_vl_results"));
        ae.setBaselineVlDate(rs.getString("Baseline_vl_date"));
        ae.setBaselineTbInfected(rs.getInt("Baseline_TB_Infected") == 1 ? "Yes" : "No");
        ae.setBaselinePregnant(rs.getInt("Baseline_Pregnant") == 1 ? "Yes" : "No");
        ae.setBaselineBreastFeeding(rs.getInt("Baseline_Breastfeeding") == 1 ? "Yes" : "No");
        ae.setBaselineWeight(rs.getString("Baseline_Weight"));
        ae.setBaselineHeight(rs.getString("Baseline_Height"));

        amrsEnrollmentService.save(ae);

        NewEnrollmentPayload.enrollments(amrsEnrollmentService, amrsEncounterService, url, auth);
      }

      System.out.println("Patient_id" + patientId);
    }
  }

  public static void patientStatus(String server, String username, String password, AMRSPatientStatusService amrsPatientStatusService, AMRSConceptMappingService amrsConceptMappingService, AMRSPatientServices amrsPatientServices, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    List<String> stringPIDsList = amrsPatientServices.getAllPatientID();

    String PatientList = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);


    // String samplePatientList = AMRSSamples.getPersonIdList();

    // System.out.println("Sample Clients " + samplePatientList);
    String sql = "";
    List<AMRSPatientStatus> amrsCivilStatusList = amrsPatientStatusService.findFirstByOrderByIdDesc();
    String nextEncounterID = "";
    if (amrsCivilStatusList.isEmpty()) {

      sql = "SELECT  \n" +
        "                     pa.person_id, \n" +
        "                    pt.person_attribute_type_id, \n" +
        "                    case when pt.person_attribute_type_id =5 then '1054' \n" +
        "                    when pt.person_attribute_type_id =42 then '1542' \n" +
        "                    when pt.person_attribute_type_id =73 then '1712' \n" +
        "                    end kenyaemr_concept, \n" +
        "                    case when pt.person_attribute_type_id =5 then '1054AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
        "                    when pt.person_attribute_type_id =42 then '1542AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
        "                    when pt.person_attribute_type_id =73 then '1712AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
        "                    end kenyaemr_concept_uuid, \n" +
        "                    pt.name, \n" +
        "                    cn.name as name_value,\n" +
        "                    pa.value,\n" +
        "                    case when pa.value =5555 then '159715AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
        "                    when pa.value =1966 then '1542AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
        "                    when pa.value =73 then '1712AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
        "                    when pa.value = 1059 then '1059AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1057 then '1057AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' -- never married / single\n" +
        "                    when pa.value = 1175 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 5622 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1056 then '1058AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1055 then '5555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1060 then '1060AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 5618 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6290 then '159715AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1670 then '1060AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 10479 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1058 then '1058AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 5622 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value =  8714 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1967 then '1538AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 8711 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1968 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1966 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6966 then '159466AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6284 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1971 then '1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6408 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1832 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 8589 then '159465AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6280 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1969 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1970 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6580 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 5619 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value =  6401 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1678 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 8407 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 10368 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 10369 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 11283 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1496 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 5507 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 8713 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 8710 then  '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 12263 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 12265 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 12262 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 12264 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1602 then '1714AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1107 then '1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1604 then '159785AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1600 then '1713AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6216 then '159785AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6214 then '1713AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6215 then '1714AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1601 then '1713AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 7583 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 5629 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1603 then '1714AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 7549 then '1713AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    else '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    end kenyaemr_value_uuid, \n" +
        "                    pa.date_created  \n" +
        "                     \n" +
        "                FROM \n" +
        "                    amrs.person_attribute pa \n" +
        "                        INNER JOIN \n" +
        "                    amrs.person_attribute_type pt ON pa.person_attribute_type_id = pt.person_attribute_type_id and pt.person_attribute_type_id in(42,73,5) \n" +
        "                    inner join amrs.concept c on c.concept_id = pa.value \n" +
        "                      inner join amrs.concept_name cn on c.concept_id = cn.concept_id  and cn.locale_preferred=1\n" +
        "                WHERE \n" +
        "                       pa.person_id IN (" + PatientList + ") \n" +
        "                        AND  \n" +
        " pa.voided = 0 order by  pa.person_id  asc";


    } else {
      System.out.println("List" + amrsCivilStatusList);
      sql = "SELECT  \n" +
        "                     pa.person_id, \n" +
        "                    pt.person_attribute_type_id, \n" +
        "                    case when pt.person_attribute_type_id =5 then '1054' \n" +
        "                    when pt.person_attribute_type_id =42 then '1542' \n" +
        "                    when pt.person_attribute_type_id =73 then '1712' \n" +
        "                    end kenyaemr_concept, \n" +
        "                    case when pt.person_attribute_type_id =5 then '1054AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
        "                    when pt.person_attribute_type_id =42 then '1542AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
        "                    when pt.person_attribute_type_id =73 then '1712AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
        "                    end kenyaemr_concept_uuid, \n" +
        "                    pt.name, \n" +
        "                    cn.name as name_value,\n" +
        "                    pa.value,\n" +
        "                    case when pa.value =5555 then '159715AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
        "                    when pa.value =1966 then '1542AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
        "                    when pa.value =73 then '1712AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
        "                    when pa.value = 1059 then '1059AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1057 then '1057AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' -- never married / single\n" +
        "                    when pa.value = 1175 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 5622 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1056 then '1058AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1055 then '5555AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1060 then '1060AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 5618 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6290 then '159715AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1670 then '1060AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 10479 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1058 then '1058AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 5622 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value =  8714 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1967 then '1538AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 8711 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1968 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1966 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6966 then '159466AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6284 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1971 then '1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6408 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1832 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 8589 then '159465AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6280 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1969 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1970 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6580 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 5619 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value =  6401 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1678 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 8407 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 10368 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 10369 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 11283 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1496 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 5507 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 8713 then '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 8710 then  '1540AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 12263 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 12265 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 12262 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 12264 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1602 then '1714AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1107 then '1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1604 then '159785AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1600 then '1713AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6216 then '159785AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6214 then '1713AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 6215 then '1714AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1601 then '1713AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 7583 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 5629 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 1603 then '1714AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    when pa.value = 7549 then '1713AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    else '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
        "                    end kenyaemr_value_uuid, \n" +
        "                    pa.date_created  \n" +
        "                     \n" +
        "                FROM \n" +
        "                    amrs.person_attribute pa \n" +
        "                        INNER JOIN \n" +
        "                    amrs.person_attribute_type pt ON pa.person_attribute_type_id = pt.person_attribute_type_id and pt.person_attribute_type_id in(42,73,5) \n" +
        "                    inner join amrs.concept c on c.concept_id = pa.value \n" +
        "                      inner join amrs.concept_name cn on c.concept_id = cn.concept_id  and cn.locale_preferred=1\n" +
        "                WHERE \n" +
        "                       pa.person_id IN (" + PatientList + ") \n" +
        "                        AND pa.voided=0  and   \n" +
        " pa.voided = 0 order by  pa.person_id  asc";
    }

    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {
      String personId = rs.getString("person_id");
      String personAttributeTypeId = rs.getString("person_attribute_type_id");
      String kenyaEmrConcept = rs.getString("kenyaemr_concept");
      String name = rs.getString("name");
      String value = rs.getString("value");
      String kenyaemr_concept_uuid = rs.getString("kenyaemr_concept_uuid");
      String kenyaemr_value_uuid = rs.getString("kenyaemr_value_uuid");
      String name_value = rs.getString("name_value");
      String patientid = rs.getString("person_id");
      String date_created = rs.getString("date_created");

      List<AMRSPatients> amrsPatients = amrsPatientServices.getByPatientID(patientid);
      String kenyaemrPatientUuid = "";
      if (amrsPatients.size() > 0) {
        kenyaemrPatientUuid = amrsPatients.get(0).getKenyaemrpatientUUID();
      }

      List<AMRSPatientStatus> amrsPatientStatusList = amrsPatientStatusService.findByPersonIdAndPersonAttributeTypeId(patientid, personAttributeTypeId);

      if (amrsPatientStatusList.isEmpty()) {

        AMRSPatientStatus cs = new AMRSPatientStatus();
        cs.setPersonId(personId);
        cs.setPersonAttributeTypeId(personAttributeTypeId);
        cs.setKenyaEmrConcept(kenyaEmrConcept);
        cs.setName(name);
        cs.setValue(value);
        cs.setKenyaEmrConceptUuid(kenyaemr_concept_uuid);
        cs.setKenyaEmrValueUuid(kenyaemr_value_uuid);
        cs.setValueName(name_value);
        cs.setKenyaPatientUuid(kenyaemrPatientUuid);
        cs.setObsDateTime(date_created);
        amrsPatientStatusService.save(cs);
        if ((!(kenyaemrPatientUuid == null))) {
          //    CareOpenMRSPayload.patientStatus(amrsPatientStatusService, parentUUID, locations, auth, url);
        }
      }

      System.out.println("Patient_id" + personId);
    }

    CareOpenMRSPayload.patientStatus(amrsPatientStatusService, auth, url);

  }

  public static void formsMappings(String server, String username, String password, String locations, String parentUUID, AMRSFormsMappingService amrsFormsMappingService, AMRSPatientServices amrsPatientServices, AMRSConceptMappingService amrsConceptMappingService, String url, String auth) throws SQLException, JSONException, ParseException, IOException {
    String sql = "select distinct(e.form_id), \n" +
      "e.encounter_type ,\n" +
      "e.name,\n" +
      "case \n" +
      "when e.form_id in(14,16,17,36,44,49,59,61,79,80,81,85,84,89,87,90,94,88,90,92,93,94,105,116,118,119,121,124,133,135,137,143,144,159,173,179,227,229,232,236,241,245,247,250,251,350,447,466,625,745,907,347,379,485,495,511,512,517,518,522,524,528,\n" +
      "529,534,540,544,545,550,648,651,689,718,727,728,731,761,789,783,784,800,864,911,912,914,967,968,999,1009,1045,1046,1047,1071,1072,1099,1412,1429,1447,1106,1155,1187,1378,1381,1437,1454,1455,1465,1470,1478,1480,1482,1496,1497)  then '22c68f86-bbf0-49ba-b2d1-23fa7ccf0259' -- adult ped youth return\n" +
      "when e.form_id in(15,33,34,35,37,43,50,58,60,95,98,111,120,134,136,145,146,148,157,160,226,228,230,235,253,254,357,378,481,484,510,523,530,531,535,546,547,649,650,713,725,726,730,790,801,877,1084,1107,1372,1373,1380,1436,1452,1453,1469,1477,1479,1481,1484,1501,1502,1509,1510,1511) then 'e4b506c1-7379-42b6-a374-284469cba8da' -- adult youth ped initial\n" +
      "when e.form_id in(30,22,19,18,20,21,32,521,647,822,1110,1407,1408,1425,1537) then '2cdeded1-3f69-3bda-beff-1ed2ead94eaf' -- lab\n" +
      "when e.form_id in(65,70,82,110,231,527,673,819,820,810,683,703,979,980,981,982,983,1010) then '83fb6ab2-faec-4d87-a714-93e77a28a201' -- art fast track\n" +
      "when e.form_id in(66,91,112,364,457,525,909,1093,1486,1487) then 'a1a62d1e-2def-11e9-b210-d663bd873d93' -- defaulter tracing\n" +
      "when e.form_id in(520,542,720,886,1078,1164,1165,1440,1458,1472) then 'c483f10f-d9ee-4b0d-9b8c-c24c1ec24701'  -- EAC\n" +
      "when e.form_id = 71 then '5cf013e8-09da-11ea-8d71-362b9e155667' -- OVC discontinuation\n" +
      "when e.form_id in (52,26,353,356,362,532,803,1087) then 'b8357314-0f6a-4fc9-a5b7-339f47095d62' -- Nutrition\n" +
      "when e.form_id in(27,97,180,183,242,248,344,1109,113,125,257,258,327,328,1438,1439,1534,1535) then 'e8f98494-af35-4bb8-9fc7-c409c8fed843' -- anc \n" +
      "when e.form_id in (843,933,1168,1324,537,929,1169,114,538,701,1414) then '1f76643e-2495-11e9-ab14-d663bd873d93' -- hiv discontinuation\n" +
      "when e.form_id in(224,249,348,446,519,1131,1444,1445,1446,1540,174,178,225,305,354,488, 1147,1177) then '72aa78e0-ee4b-47c3-9073-26f3b9ecc4a7' -- pnc\n" +
      "when e.form_id in(1532,782,1086,1432,1435) then '1bfb09fc-56d7-4108-bd59-b2765fd312b8' -- Prep initial\n" +
      "when e.form_id in(1499,777,1088,1431,1533,1431) then 'ee3e2017-52c0-4a54-99ab-ebb542fb8984' -- prep return\n" +
      "when e.form_id = 1434 then '291c03c8-a216-11e9-a2a3-2a2ae2dbcce4' -- Prep monthly refill\n" +
      "when e.form_id in (29,1121,1441,1442,1443) then '496c7cc3-0eea-4e84-a04c-2292949e2f7f' -- MNCH Delivery\n" +
      "when e.form_id = 1324 then '5cf013e8-09da-11ea-8d71-362b9e155667' -- OVC Disc\n" +
      "when e.form_id = 1483 then '3ae95898-0464-11ea-8d71-362b9e155667' -- OTZ enrollment\n" +
      "when e.form_id = 1484 then '3ae95d48-0464-11ea-8d71-362b9e155667' -- OTZ Activity\n" +
      "when e.form_id = 1485 then '3ae955dc-0464-11ea-8d71-362b9e155667' -- OTZ Disc\n" +
      "when e.form_id in (686,687,688,733,811,1531,536) then '755b59e6-acbb-4853-abaf-be302039f902' -- cwc\n" +
      "when e.form_id in (1356,1174) then '48f2235ca-cc77-49cb-83e6-f526d5a5f174' -- Cervical screening\n" +
      "when e.form_id in (1302,1316,1397,1424,1303,1359) then '86709f36-1490-11ec-82a8-0242ac130003' -- Covid Screening\n" +
      "when e.form_id in (1357,1385) then 'ac3152de-1728-4786-828a-7fb4db0fc384' -- Home visit\n" +
      "when e.form_id in (520,542,720,886,1078,1164,1165,1440,1458,1460,1471) then '37f6bd8d-586a-4169-95fa-5781f987fe62' -- Triage\n" +
      "when e.form_id in (101,117,239,330,368) then '59ed8e62-7f1f-40ae-a2e3-eabe350277ce' -- TB\n" +
      "when e.form_id in (1369) and e.encounter_type in (2) then '22c68f86-bbf0-49ba-b2d1-23fa7ccf0259' -- Greencard\n" +
      "when e.form_id in (1369) and e.encounter_type in (110) then '37f6bd8d-586a-4169-95fa-5781f987fe62' -- Triage\n" +
      "end kenyaemr_form_uuid\n" +
      "from \n" +
      "amrs.form e";

    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {
      String amrsFormId = rs.getString("form_id");
      String encounterType = rs.getString("encounter_type");
      String amrsFormName = rs.getString("name");
      String kenyaEmrFormUuid = rs.getString("kenyaemr_form_uuid");

      AMRSFormsMapper form = new AMRSFormsMapper();
      form.setAmrsFormId(amrsFormId);
      form.setAmrsEncounterTypeId(encounterType);
      form.setKenyaemrFormUuid(kenyaEmrFormUuid);
      form.setAmrsFormName(amrsFormName);
      form.setAmrsMigrationErrorDescription(null);
      List<AMRSFormsMapper> amrsFormsMappers = amrsFormsMappingService.findByAmrsFormIdAndEncounterTypeId(amrsFormId, encounterType);
      if (amrsFormsMappers.isEmpty()) {
        amrsFormsMappingService.save(form);
      }

    }
  }

  public static void processGreenCard(String server, String username, String password, String KenyaEMRlocationUuid, AMRSGreenCardService amrsGreenCardService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    // String samplePatientList = AMRSSamples.getPersonIdList();
    List<String> stringPIDsList = amrsPatientServices.getAllPatientID();
    String samplePatientList = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);


       /* List<AMRSPatients> amrsPatientsList = amrsPatientServices.getAll();
        String pidss = "";
        for (int y = 0; y < amrsPatientsList.size(); y++) {
            pidss += amrsPatientsList.get(y).getPersonId() + ",";
        }
        String pid = pidss.substring(0, pidss.length() - 1);

        System.out.println("Patient Id " + pid);


        System.out.println("Patient Id " + pid); */

//        String sql = "SELECT o.person_id as patient_id,e.form_id,o.concept_id,o.encounter_id, " +
//                "o.value_datetime as tca, o.obs_datetime,o.uuid  " +
//                "FROM amrs.obs o \n" +
//                "INNER JOIN amrs.concept c ON o.concept_id=c.concept_id \n" +
//                "AND o.person_id IN("+samplePatientList+")\n" +
//                "AND c.uuid in ('a8a666ba-1350-11df-a1f1-0026b9348838','318a5e8b-218c-4f66-9106-cd581dec1f95')\n" +
//                "INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0";


//        String sql = "SELECT o.person_id as patient_id,e.form_id,o.concept_id,o.encounter_id, \n" +
//                "ifnull(o.value_datetime,\"\") as value_datetime,ifnull(o.value_coded,\"\") as value_coded,ifnull(o.value_numeric,\"\") as value_numeric,\n" +
//                "ifnull(o.value_text,\"\")as value_text,o.obs_datetime,o.uuid  \n" +
//                "FROM amrs.obs o \n" +
//                "INNER JOIN amrs.concept c ON o.concept_id=c.concept_id \n" +
//                "AND o.person_id IN(59807) \n" +
//                "AND c.concept_id in (1246,1412,10653,5088,5087,5085,5086,5242,5092,5089,\n" +
//                "10805,5090,1343,9782,6578,12258,5356,6048,5219,1154,10893,10727,\n" +
//                "6176,9742,10591,6174,1271,12,1272,10761,2028,10676,7502,10677,\n" +
//                "7637,1113,1111,8292,11308,8293,10679,10785,10786,10787,\n" +
//                "10788,1266,10681,6793,2031,6968,10706,10239,6137,11679,1065,\n" +
//                "1066,1664,1193,7897,1198,1915,10707,1836,2061,5272,9736,10814,\n" +
//                "5596,12253,10708,7947,5624,5632,8355,374,6687,1119,10987,1120,\n" +
//                "10821,7343,11705,10845,1123,9467,1124,1125,1129,1126,1128,6042,\n" +
//                "7222,1109,10831,10832,10833,10834,8288,6287,6259,10726,2312,\n" +
//                "10400,9611,9609,9070,5096,1835,9605,10988,1724,10102,10103,\n" +
//                "10104,10105,10106,10107,10108,10109,5616,10381,12384,1629,\n" +
//                "6748,10984,11930,7656)\n" +
//                "INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0 \n" +
//                "and e.encounter_type in(2,4,106,176)\n" +
//                "ORDER BY patient_id ASC,encounter_id DESC";


       /* String sql = "SELECT o.person_id as patient_id,e.form_id,e.visit_id,o.concept_id,o.encounter_id,o.obs_datetime,e.encounter_datetime,\n" +
                " cn.name question,c.datatype_id,\n" +
                "case when o.value_datetime is not null then o.value_datetime\n" +
                "when o.value_coded is not null then o.value_coded\n" +
                "when o.value_numeric is not null then o.value_numeric\n" +
                "when o.value_text is not null then o.value_text end \n" +
                "as value  \n" +
                "                  FROM amrs.obs o  \n" +
                "                  INNER JOIN amrs.concept c ON o.concept_id=c.concept_id  \n" +
                "\t\t\t\tINNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id\n" +
                "                 and cn.locale_preferred=1\n" +
                "              -- LEFT JOIN amrs.concept_name cn_answer ON o.value_coded = cn_answer.concept_id and cn_answer.locale_preferred=1\n" +
                "\t\t\t   AND o.person_id IN(59807)  \n" +
                "                  AND c.concept_id in (1246,1412,10653,5242,\n" +
                "                  10805,1343,9782,6578,12258,5356,6048,5219,10893,10727, \n" +
                "                  6176,9742,10591,6174,1271,12,1272,10761,2028,10676,7502,10677, \n" +
                "                  7637,1113,1111,8292,11308,8293,10679,10785,10786,10787, \n" +
                "                  10788,1266,10681,6793,2031,6968,10706,10239,6137,11679,1065, \n" +
                "                  1066,1664,1193,7897,1198,1915,10707,1836,2061,5272,9736,10814, \n" +
                "                  5596,12253,10708,7947,5624,5632,8355,374,6687,1119,10987,1120, \n" +
                "                  10821,7343,11705,10845,1123,9467,1124,1125,1129,1126,1128,6042, \n" +
                "                  7222,1109,10831,10832,10833,10834,8288,6287,6259,10726,2312, \n" +
                "                  10400,9611,9609,9070,5096,1835,9605,10988,1724,10102,10103, \n" +
                "                  10104,10105,10106,10107,10108,10109,5616,10381,12384,1629, \n" +
                "                  6748,10984,11930,7656) -- 5088,5087,5085,5086,5089,5090,5092 \n" +
                "                  INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0  \n" +
                "                  and e.encounter_type in(2,4,106,176) and e.encounter_id in (14763811)\n" +
                "                  ORDER BY patient_id ASC,encounter_id DESC";

        System.out.println("locations " + locations + " parentUUID " + parentUUID); */
    String sql = "SELECT o.person_id as patient_id,e.form_id,e.encounter_type,e.visit_id,o.concept_id,o.encounter_id,o.obs_datetime,e.encounter_datetime,\n" +
            "                 cn.name question,c.datatype_id,\n" +
            "                case when o.value_datetime is not null then o.value_datetime\n" +
            "                when o.value_coded is not null then o.value_coded\n" +
            "                when o.value_numeric is not null then o.value_numeric\n" +
            "                when o.value_text is not null then o.value_text end \n" +
            "                as value  \n" +
            "                                  FROM amrs.obs o  \n" +
            "                                  INNER JOIN amrs.concept c ON o.concept_id=c.concept_id  \n" +
            "                INNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id\n" +
            "                                 and cn.locale_preferred=1 \n" +
            "                                  AND c.concept_id in (1246,1412,9782,10653,12285,5356,5219,10893,10727,\n" +
            "                                  6176,9742,10591,6147,1271,2028,7502,10677,7637,8292,10679,10785,10786,\n" +
            "                                  10787,10788,1266,10681,6793,6968,10706,10239,6137,11679,6137,1664,1193,\n" +
            "                                  2031,6968,10706,7897,1198,1915,10707,1836,2061,5272,9736,10814,5596,\n" +
            "                                  12253,10708,7947,5624,5632,8355,374,6687,1119,6042,7222,1109,10831,\n" +
            "                                  10832,10833,10834,8288,6287,6259,10726,2312,10400,9611,9609,9070,5096,1835,\n" +
            "                                  9605,10988,1113,1111,8292,8293,10679,10788,1266,10681,6793,2031,6968,10706,\n" +
            "                                  10239,6137,11679,6137,1664,1193,2031,6968,7897,1198,1915,1836,10845,1123) -- 5088,5087,5085,5086,5089,5090,5092 \n" +
            "                                  INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0  \n" +
            "                                  -- and e.encounter_type in(2,4,106,176) \n" +
            "                                  -- and e.encounter_id in (14763811)\n" +
            "                                  where e.patient_id in ("+ samplePatientList+")\n" +
            "                            group by  patient_id,concept_id,visit_id\n" +
            "                                  ORDER BY patient_id ASC,encounter_id DESC";

    System.out.println("SQL is here " + sql);

    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String encounterDatetime = rs.getString("encounter_datetime");
      String value = rs.getString("value");
      String question = rs.getString("question");
      String dataType = rs.getString("datatype_id");
      String visitId = rs.getString("visit_id");
      String obsDateTime = rs.getString("obs_datetime");

      String kenyaemr_uuid = "";
      List<AMRSGreenCard> amrsGreenCardExist = amrsGreenCardService.findByPatientIdAndVisitIdAndConceptId(patientId, visitId, conceptId);
      if (!amrsGreenCardExist.isEmpty()) {

      } else {
        AMRSGreenCard amrsGreenCard = new AMRSGreenCard();
        String kenyaemr_encounter_id;
        String kenyaemr_value = "";
        if (dataType.equals("2")) {
          kenyaemr_value = amrsTranslater.translater(value);
        } else if (dataType.equals("10")) {
          if (Objects.equals(conceptId, "5632")) {
            kenyaemr_value = amrsTranslater.translater(value);
          } else {
            boolean vcheck = false;
            if (value.equals("1065")) {
              vcheck = true;
            }
            kenyaemr_value = String.valueOf(vcheck);
          }
        } else {
          kenyaemr_value = value;
        }
        amrsGreenCard.setPatientId(patientId);
        amrsGreenCard.setFormId(formId);
        amrsGreenCard.setConceptId(conceptId);
        amrsGreenCard.setEncounterId(encounterId);
        amrsGreenCard.setValue(value);
        amrsGreenCard.setConceptDataTypeId(dataType);
        amrsGreenCard.setVisitId(visitId);
        amrsGreenCard.setQuestion(question);
        amrsGreenCard.setObsDateTime(obsDateTime);
        amrsGreenCard.setKenyaemrEncounterTypeUuid("a0034eee-1940-4e35-847f-97537a35d05e");
        amrsGreenCard.setKenyaemrFormUuid("22c68f86-bbf0-49ba-b2d1-23fa7ccf0259");
        amrsGreenCard.setKenyaEmrValue(kenyaemr_value);
        amrsGreenCard.setKenyaEmrEncounterDateTime(encounterDatetime);
        String kenyaemr_patient_uuid = amrsTranslater.KenyaemrPatientUuid(patientId);
        String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
        amrsGreenCard.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
        amrsGreenCard.setKenyaemrPatientUuid(kenyaemr_patient_uuid);
        System.out.println("Patient ID "+ patientId+" Kenyaemr Value "+ kenyaemr_value);
        try {
          amrsGreenCardService.save(amrsGreenCard);
        }catch (Exception e){
          System.out.println("Could not save this "+e.getMessage());
        }
      }

    }
    GreenCardPayload.processGreenCard(amrsGreenCardService, amrsPatientServices, amrsTranslater, KenyaEMRlocationUuid, url, auth);

  }

  public static void ordersResults(String server, String username, String password, String locations, String parentUUID, AMRSOrdersResultsService amrsOrdersResultsService, AMRSConceptMappingService amrsConceptMappingService, AMRSPatientServices amrsPatientServices, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    String sql = "";
    List<AMRSOrdersResults> amrsOrdersResultsList = amrsOrdersResultsService.findFirstByOrderByIdDesc();
    String nextEncounterID = "";
    String samplePatientList = AMRSSamples.getPersonIdList();
    if (amrsOrdersResultsList.isEmpty()) {

      sql = "WITH cte_patient_id as (\n" +
        "(" + samplePatientList + ")" +
        "),\n" +
        "\n" +
        "cte_orders as (\n" +
        "\tSELECT \n" +
        "    t1.patient_id,\n" +
        "    t1.order_id,\n" +
        "    t1.order_number AS orderNumber,\n" +
        "    t1.concept_id AS order_type,\n" +
        "    case when t1.concept_id = 856 then 'VIRAL LOAD' ELSE t2.name end AS display,\n" +
        "    t3.value_numeric as order_result,\n" +
        "    -- o.order_value,\n" +
        "    t1.date_activated AS date_ordered,\n" +
        "    t4.name AS sample_drawn,\n" +
        "    DATE(t5.obs_datetime) AS sample_collection_date\n" +
        "FROM\n" +
        "    amrs.encounter e\n" +
        "        INNER JOIN\n" +
        "    amrs.patient p USING (patient_id)\n" +
        "        INNER JOIN\n" +
        "    amrs.person per ON (per.person_id = p.patient_id)\n" +
        "\t\tINNER JOIN \n" +
        "\tamrs.orders t1 ON(e.encounter_id = t1.encounter_id)\n" +
        "        LEFT OUTER JOIN\n" +
        "\tamrs.obs t5 ON (t1.order_id = t5.order_id\n" +
        "        AND (t5.voided IS NULL || t5.voided = 0)\n" +
        "        AND t5.concept_id = 10189)\n" +
        "\t\tLEFT OUTER JOIN\n" +
        "    amrs.obs t3 ON (t1.concept_id = t3.concept_id\n" +
        "        AND (t3.voided IS NULL || t3.voided = 0) AND t1.encounter_id = t3.encounter_id)    \n" +
        "        LEFT OUTER JOIN\n" +
        "    amrs.concept_name t4 ON (t5.value_coded = t4.concept_id)\n" +
        "        LEFT OUTER JOIN\n" +
        "    amrs.concept_name t2 ON (t1.concept_id = t2.concept_id)\n" +
        "WHERE\n" +
        "\t\tp.voided = 0 AND e.voided = 0\n" +
        "        AND e.location_id IN (2 , 339, 98, 379) \n" +
        "        AND (t1.voided IS NULL || t1.voided = 0)\n" +
        "        AND t1.patient_id IN(SELECT patient_id FROM cte_patient_id)\n" +
        "GROUP BY t1.patient_id, t1.order_number\n" +
        "ORDER BY t1.patient_id, t1.date_activated desc\n" +
        "),\n" +
        "cte_vls AS(\n" +
        "SELECT \n" +
        "    f.person_id, \n" +
        "    f.concept_id, \n" +
        "    f.order_id, \n" +
        "    f.obs_date, \n" +
        "    f.order_value \n" +
        "FROM (\n" +
        "    SELECT \n" +
        "        t.row_id, \n" +
        "        t.person_id, \n" +
        "        t.order_type as concept_id, \n" +
        "        t.encounter_id, \n" +
        "        CASE WHEN t.order_id IS NOT NULL AND t.concept_id = 856 THEN t.order_id\n" +
        "        WHEN LEAD(t.order_id) OVER (ORDER BY t.row_id ASC) IS NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id THEN t.order_id\n" +
        "        ELSE NULL END AS order_id, \n" +
        "        t.obs_date, \n" +
        "        CASE WHEN t.order_id IS NOT NULL AND t.concept_id = 856 THEN t.value_numeric\n" +
        "        WHEN t.order_id IS NOT NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id AND t.order_type = 856 THEN LEAD(t.value_numeric) OVER (ORDER BY t.row_id ASC)\n" +
        "        ELSE NULL END AS order_value\n" +
        "    FROM (\n" +
        "        SELECT \n" +
        "            ROW_NUMBER() OVER (ORDER BY o.person_id, o.obs_id ASC) AS row_id, \n" +
        "            o.person_id, \n" +
        "            o.concept_id, \n" +
        "            o.encounter_id, \n" +
        "            o.order_id,\n" +
        "            d.concept_id as order_type,\n" +
        "            DATE(o.obs_datetime) AS obs_date, \n" +
        "            o.value_numeric, \n" +
        "            o.location_id, \n" +
        "            o.voided\n" +
        "        FROM \n" +
        "            amrs.obs o\n" +
        "\t\tLEFT JOIN \n" +
        "\t\t\tamrs.orders d using(order_id)\n" +
        "        WHERE \n" +
        "            o.person_id in(SELECT * FROM temp_patients) AND \n" +
        "            o.concept_id IN (10189, 856) AND \n" +
        "            o.encounter_id IS NULL\n" +
        "        GROUP BY \n" +
        "            o.person_id, \n" +
        "            o.concept_id, \n" +
        "            DATE(o.obs_datetime)\n" +
        "        ORDER BY \n" +
        "        o.person_id, o.obs_id ASC\n" +
        "    ) t\n" +
        ") f \n" +
        "WHERE f.order_id IS NOT NULL\n" +
        "),\n" +
        "cte_dna_pcr AS(\n" +
        "SELECT \n" +
        "    f.person_id, \n" +
        "    f.concept_id, \n" +
        "    f.order_id, \n" +
        "    f.obs_date, \n" +
        "    f.order_value \n" +
        "FROM (\n" +
        "    SELECT \n" +
        "        t.row_id, \n" +
        "        t.person_id, \n" +
        "        t.order_type as concept_id, \n" +
        "        t.encounter_id, \n" +
        "        CASE WHEN t.order_id IS NOT NULL AND t.concept_id = 1030 THEN t.order_id\n" +
        "        WHEN LEAD(t.order_id) OVER (ORDER BY t.row_id ASC) IS NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id THEN t.order_id\n" +
        "        ELSE NULL END AS order_id, \n" +
        "        t.obs_date, \n" +
        "        CASE WHEN t.order_id IS NOT NULL AND t.concept_id = 1030 THEN t.value_coded\n" +
        "        WHEN t.order_id IS NOT NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id AND t.order_type = 1030 THEN LEAD(t.value_coded) OVER (ORDER BY t.row_id ASC)\n" +
        "        ELSE NULL END AS order_value\n" +
        "    FROM (\n" +
        "        SELECT \n" +
        "            ROW_NUMBER() OVER (ORDER BY o.person_id, o.obs_id ASC) AS row_id, \n" +
        "            o.person_id, \n" +
        "            o.concept_id, \n" +
        "            o.encounter_id, \n" +
        "            o.order_id,\n" +
        "            d.concept_id as order_type,\n" +
        "            DATE(o.obs_datetime) AS obs_date, \n" +
        "            o.value_coded, \n" +
        "            o.location_id, \n" +
        "            o.voided\n" +
        "        FROM \n" +
        "            amrs.obs o\n" +
        "\t\tLEFT JOIN \n" +
        "\t\t\tamrs.orders d using(order_id)\n" +
        "        WHERE \n" +
        "            o.person_id in(SELECT * FROM temp_patients) AND \n" +
        "            o.concept_id IN (10189, 1030) AND \n" +
        "            o.encounter_id IS NULL\n" +
        "        GROUP BY \n" +
        "            o.person_id, \n" +
        "            o.concept_id, \n" +
        "            DATE(o.obs_datetime)\n" +
        "        ORDER BY \n" +
        "        o.person_id, o.obs_id ASC\n" +
        "    ) t\n" +
        ") f \n" +
        "WHERE f.order_id IS NOT NULL\n" +
        "),\n" +
        "cte_cd4 AS(\n" +
        "SELECT \n" +
        "    f.person_id, \n" +
        "    f.concept_id, \n" +
        "    f.order_id, \n" +
        "    f.obs_date, \n" +
        "    f.order_value \n" +
        "FROM (\n" +
        "    SELECT \n" +
        "        t.row_id, \n" +
        "        t.person_id, \n" +
        "        t.order_type as concept_id, \n" +
        "        t.encounter_id, \n" +
        "        CASE WHEN t.order_id IS NOT NULL AND t.concept_id in(657, 5497) THEN t.order_id\n" +
        "        WHEN LEAD(t.order_id) OVER (ORDER BY t.row_id ASC) IS NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id THEN t.order_id\n" +
        "        ELSE NULL END AS order_id, \n" +
        "        t.obs_date, \n" +
        "        CASE WHEN t.order_id IS NOT NULL AND t.concept_id in(657, 5497) THEN t.value_numeric\n" +
        "        WHEN t.order_id IS NOT NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id AND t.order_type in(657, 5497) THEN LEAD(t.value_numeric) OVER (ORDER BY t.row_id ASC)\n" +
        "        ELSE NULL END AS order_value\n" +
        "    FROM (\n" +
        "        SELECT \n" +
        "            ROW_NUMBER() OVER (ORDER BY o.person_id, o.obs_id ASC) AS row_id, \n" +
        "            o.person_id, \n" +
        "            o.concept_id, \n" +
        "            o.encounter_id, \n" +
        "            o.order_id,\n" +
        "            d.concept_id as order_type,\n" +
        "            DATE(o.obs_datetime) AS obs_date, \n" +
        "            o.value_numeric, \n" +
        "            o.location_id, \n" +
        "            o.voided\n" +
        "        FROM \n" +
        "            amrs.obs o\n" +
        "\t\tLEFT JOIN \n" +
        "\t\t\tamrs.orders d using(order_id)\n" +
        "        WHERE \n" +
        "            o.person_id in(SELECT * FROM temp_patients) AND \n" +
        "            o.concept_id IN (10189, 657, 5497) AND \n" +
        "            o.encounter_id IS NULL\n" +
        "        GROUP BY \n" +
        "            o.person_id, \n" +
        "            o.concept_id, \n" +
        "            DATE(o.obs_datetime)\n" +
        "        ORDER BY \n" +
        "        o.person_id, o.obs_id ASC\n" +
        "    ) t\n" +
        ") f \n" +
        "WHERE f.order_id IS NOT NULL\n" +
        ")\n" +
        "\n" +
        "SELECT a.patient_id, a.order_id, a.orderNumber, a.order_type, a.display, a.date_ordered, a.sample_drawn, a.sample_collection_date, \n" +
        "case when b.order_value IS NOT NULL THEN b.order_value \n" +
        "when c.order_value IS NOT NULL THEN c.order_value\n" +
        "when d.order_value IS NOT NULL THEN d.order_value\n" +
        "else a.order_result END  as final_order_result FROM cte_orders a \n" +
        "left join cte_vls b ON(a.order_id = b.order_id AND a.order_type = b.concept_id)\n" +
        "left join cte_dna_pcr c ON(a.order_id = c.order_id AND a.order_type = c.concept_id)\n" +
        "left join cte_cd4 d ON(a.order_id = d.order_id AND a.order_type = d.concept_id)\n" +
        "group by a.patient_id, a.orderNumber";


    } else {
      System.out.println("List" + amrsOrdersResultsList);
//            nextEncounterID = amrs.get(0).getEncounterID();
      sql = "WITH cte_patient_id as (\n" +
        "\tSELECT * FROM temp_patients\n" +
        "),\n" +
        "\n" +
        "cte_orders as (\n" +
        "\tSELECT \n" +
        "    t1.patient_id,\n" +
        "    t1.order_id,\n" +
        "    t1.order_number AS orderNumber,\n" +
        "    t1.concept_id AS order_type,\n" +
        "    case when t1.concept_id = 856 then 'VIRAL LOAD' ELSE t2.name end AS display,\n" +
        "    t3.value_numeric as order_result,\n" +
        "    -- o.order_value,\n" +
        "    t1.date_activated AS date_ordered,\n" +
        "    t4.name AS sample_drawn,\n" +
        "    DATE(t5.obs_datetime) AS sample_collection_date\n" +
        "FROM\n" +
        "    amrs.encounter e\n" +
        "        INNER JOIN\n" +
        "    amrs.patient p USING (patient_id)\n" +
        "        INNER JOIN\n" +
        "    amrs.person per ON (per.person_id = p.patient_id)\n" +
        "\t\tINNER JOIN \n" +
        "\tamrs.orders t1 ON(e.encounter_id = t1.encounter_id)\n" +
        "        LEFT OUTER JOIN\n" +
        "\tamrs.obs t5 ON (t1.order_id = t5.order_id\n" +
        "        AND (t5.voided IS NULL || t5.voided = 0)\n" +
        "        AND t5.concept_id = 10189)\n" +
        "\t\tLEFT OUTER JOIN\n" +
        "    amrs.obs t3 ON (t1.concept_id = t3.concept_id\n" +
        "        AND (t3.voided IS NULL || t3.voided = 0) AND t1.encounter_id = t3.encounter_id)    \n" +
        "        LEFT OUTER JOIN\n" +
        "    amrs.concept_name t4 ON (t5.value_coded = t4.concept_id)\n" +
        "        LEFT OUTER JOIN\n" +
        "    amrs.concept_name t2 ON (t1.concept_id = t2.concept_id)\n" +
        "WHERE\n" +
        "\t\tp.voided = 0 AND e.voided = 0\n" +
        "        AND e.location_id IN (2 , 339, 98, 379) \n" +
        "        AND (t1.voided IS NULL || t1.voided = 0)\n" +
        "        AND t1.patient_id IN(SELECT patient_id FROM cte_patient_id)\n" +
        "GROUP BY t1.patient_id, t1.order_number\n" +
        "ORDER BY t1.patient_id, t1.date_activated desc\n" +
        "),\n" +
        "cte_vls AS(\n" +
        "SELECT \n" +
        "    f.person_id, \n" +
        "    f.concept_id, \n" +
        "    f.order_id, \n" +
        "    f.obs_date, \n" +
        "    f.order_value \n" +
        "FROM (\n" +
        "    SELECT \n" +
        "        t.row_id, \n" +
        "        t.person_id, \n" +
        "        t.order_type as concept_id, \n" +
        "        t.encounter_id, \n" +
        "        CASE WHEN t.order_id IS NOT NULL AND t.concept_id = 856 THEN t.order_id\n" +
        "        WHEN LEAD(t.order_id) OVER (ORDER BY t.row_id ASC) IS NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id THEN t.order_id\n" +
        "        ELSE NULL END AS order_id, \n" +
        "        t.obs_date, \n" +
        "        CASE WHEN t.order_id IS NOT NULL AND t.concept_id = 856 THEN t.value_numeric\n" +
        "        WHEN t.order_id IS NOT NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id AND t.order_type = 856 THEN LEAD(t.value_numeric) OVER (ORDER BY t.row_id ASC)\n" +
        "        ELSE NULL END AS order_value\n" +
        "    FROM (\n" +
        "        SELECT \n" +
        "            ROW_NUMBER() OVER (ORDER BY o.person_id, o.obs_id ASC) AS row_id, \n" +
        "            o.person_id, \n" +
        "            o.concept_id, \n" +
        "            o.encounter_id, \n" +
        "            o.order_id,\n" +
        "            d.concept_id as order_type,\n" +
        "            DATE(o.obs_datetime) AS obs_date, \n" +
        "            o.value_numeric, \n" +
        "            o.location_id, \n" +
        "            o.voided\n" +
        "        FROM \n" +
        "            amrs.obs o\n" +
        "\t\tLEFT JOIN \n" +
        "\t\t\tamrs.orders d using(order_id)\n" +
        "        WHERE \n" +
        "            o.person_id in(SELECT * FROM temp_patients) AND \n" +
        "            o.concept_id IN (10189, 856) AND \n" +
        "            o.encounter_id IS NULL\n" +
        "        GROUP BY \n" +
        "            o.person_id, \n" +
        "            o.concept_id, \n" +
        "            DATE(o.obs_datetime)\n" +
        "        ORDER BY \n" +
        "        o.person_id, o.obs_id ASC\n" +
        "    ) t\n" +
        ") f \n" +
        "WHERE f.order_id IS NOT NULL\n" +
        "),\n" +
        "cte_dna_pcr AS(\n" +
        "SELECT \n" +
        "    f.person_id, \n" +
        "    f.concept_id, \n" +
        "    f.order_id, \n" +
        "    f.obs_date, \n" +
        "    f.order_value \n" +
        "FROM (\n" +
        "    SELECT \n" +
        "        t.row_id, \n" +
        "        t.person_id, \n" +
        "        t.order_type as concept_id, \n" +
        "        t.encounter_id, \n" +
        "        CASE WHEN t.order_id IS NOT NULL AND t.concept_id = 1030 THEN t.order_id\n" +
        "        WHEN LEAD(t.order_id) OVER (ORDER BY t.row_id ASC) IS NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id THEN t.order_id\n" +
        "        ELSE NULL END AS order_id, \n" +
        "        t.obs_date, \n" +
        "        CASE WHEN t.order_id IS NOT NULL AND t.concept_id = 1030 THEN t.value_coded\n" +
        "        WHEN t.order_id IS NOT NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id AND t.order_type = 1030 THEN LEAD(t.value_coded) OVER (ORDER BY t.row_id ASC)\n" +
        "        ELSE NULL END AS order_value\n" +
        "    FROM (\n" +
        "        SELECT \n" +
        "            ROW_NUMBER() OVER (ORDER BY o.person_id, o.obs_id ASC) AS row_id, \n" +
        "            o.person_id, \n" +
        "            o.concept_id, \n" +
        "            o.encounter_id, \n" +
        "            o.order_id,\n" +
        "            d.concept_id as order_type,\n" +
        "            DATE(o.obs_datetime) AS obs_date, \n" +
        "            o.value_coded, \n" +
        "            o.location_id, \n" +
        "            o.voided\n" +
        "        FROM \n" +
        "            amrs.obs o\n" +
        "\t\tLEFT JOIN \n" +
        "\t\t\tamrs.orders d using(order_id)\n" +
        "        WHERE \n" +
        "            o.person_id in(SELECT * FROM temp_patients) AND \n" +
        "            o.concept_id IN (10189, 1030) AND \n" +
        "            o.encounter_id IS NULL\n" +
        "        GROUP BY \n" +
        "            o.person_id, \n" +
        "            o.concept_id, \n" +
        "            DATE(o.obs_datetime)\n" +
        "        ORDER BY \n" +
        "        o.person_id, o.obs_id ASC\n" +
        "    ) t\n" +
        ") f \n" +
        "WHERE f.order_id IS NOT NULL\n" +
        "),\n" +
        "cte_cd4 AS(\n" +
        "SELECT \n" +
        "    f.person_id, \n" +
        "    f.concept_id, \n" +
        "    f.order_id, \n" +
        "    f.obs_date, \n" +
        "    f.order_value \n" +
        "FROM (\n" +
        "    SELECT \n" +
        "        t.row_id, \n" +
        "        t.person_id, \n" +
        "        t.order_type as concept_id, \n" +
        "        t.encounter_id, \n" +
        "        CASE WHEN t.order_id IS NOT NULL AND t.concept_id in(657, 5497) THEN t.order_id\n" +
        "        WHEN LEAD(t.order_id) OVER (ORDER BY t.row_id ASC) IS NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id THEN t.order_id\n" +
        "        ELSE NULL END AS order_id, \n" +
        "        t.obs_date, \n" +
        "        CASE WHEN t.order_id IS NOT NULL AND t.concept_id in(657, 5497) THEN t.value_numeric\n" +
        "        WHEN t.order_id IS NOT NULL AND LEAD(t.person_id) OVER (ORDER BY t.row_id ASC) = t.person_id AND t.order_type in(657, 5497) THEN LEAD(t.value_numeric) OVER (ORDER BY t.row_id ASC)\n" +
        "        ELSE NULL END AS order_value\n" +
        "    FROM (\n" +
        "        SELECT \n" +
        "            ROW_NUMBER() OVER (ORDER BY o.person_id, o.obs_id ASC) AS row_id, \n" +
        "            o.person_id, \n" +
        "            o.concept_id, \n" +
        "            o.encounter_id, \n" +
        "            o.order_id,\n" +
        "            d.concept_id as order_type,\n" +
        "            DATE(o.obs_datetime) AS obs_date, \n" +
        "            o.value_numeric, \n" +
        "            o.location_id, \n" +
        "            o.voided\n" +
        "        FROM \n" +
        "            amrs.obs o\n" +
        "\t\tLEFT JOIN \n" +
        "\t\t\tamrs.orders d using(order_id)\n" +
        "        WHERE \n" +
        "            o.person_id in(SELECT * FROM temp_patients) AND \n" +
        "            o.concept_id IN (10189, 657, 5497) AND \n" +
        "            o.encounter_id IS NULL\n" +
        "        GROUP BY \n" +
        "            o.person_id, \n" +
        "            o.concept_id, \n" +
        "            DATE(o.obs_datetime)\n" +
        "        ORDER BY \n" +
        "        o.person_id, o.obs_id ASC\n" +
        "    ) t\n" +
        ") f \n" +
        "WHERE f.order_id IS NOT NULL\n" +
        ")\n" +
        "\n" +
        "SELECT a.patient_id, a.order_id, a.orderNumber, a.order_type, a.display, a.date_ordered, a.sample_drawn, a.sample_collection_date, \n" +
        "case when b.order_value IS NOT NULL THEN b.order_value \n" +
        "when c.order_value IS NOT NULL THEN c.order_value\n" +
        "when d.order_value IS NOT NULL THEN d.order_value\n" +
        "else a.order_result END  as final_order_result FROM cte_orders a \n" +
        "left join cte_vls b ON(a.order_id = b.order_id AND a.order_type = b.concept_id)\n" +
        "left join cte_dna_pcr c ON(a.order_id = c.order_id AND a.order_type = c.concept_id)\n" +
        "left join cte_cd4 d ON(a.order_id = d.order_id AND a.order_type = d.concept_id)\n" +
        "group by a.patient_id, a.orderNumber";
    }

    System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {
      String patientId = rs.getString("patient_id");
      String orderId = rs.getString("order_id");
      String orderNumber = rs.getString("orderNumber");
      String orderType = rs.getString("order_type");
      String display = rs.getString("display");
      String dateOrdered = rs.getString("date_ordered");
      String sampleDrawn = rs.getString("sample_drawn");
      String sampleCollectionDate = rs.getString("sample_collection_date");
      String finalOrderResults = rs.getString("final_order_result");

      List<AMRSOrdersResults> amrsOrdersResults = amrsOrdersResultsService.findByPatientId(patientId);
      String kenyaemrPatientUuid = "";
      if (amrsOrdersResults.size() > 0) {
//                kenyaemrPatientUuid = amrsOrdersResults.get(0).getKenyaemrpatientUUID();
      }


      if (amrsOrdersResultsList.isEmpty()) {

        AMRSOrdersResults or = new AMRSOrdersResults();
        or.setPatientId(patientId);
        or.setOrderId(orderId);
        or.setOrderNumber(orderNumber);
        or.setOrderType(orderType);
        or.setDisplay(display);
        or.setDateOrdered(dateOrdered);
        or.setSampleDrawn(sampleDrawn);
        or.setSampleCollectionDate(sampleCollectionDate);
        or.setFinalOrderResults(finalOrderResults);


        System.out.println("Tumefika Hapa!!!" + parentUUID);
        amrsOrdersResultsService.save(or);

//                CareOpenMRSPayload.ordersResults(amrsOrdersResultsService, parentUUID, locations, auth, url);

      }

      System.out.println("Patient_id" + patientId);
    }

  }

  public static void EncounterFormsMappings(String server, String username, String password, String locations, String parentUUID, AMRSEncounterFormsMappingService amrsEncounterFormsMappingService, AMRSPatientServices amrsPatientServices, AMRSConceptMappingService amrsConceptMappingService, String url, String auth) throws SQLException, JSONException, ParseException, IOException {
    String sql = "select distinct(e.form_id), \n" +
      "e.encounter_id , \n" +
      "e.patient_id,\n" +
      "case \n" +
      "when e.form_id in(14,16,17,44,49,59,61,85,84,89,87,94,88,93,98,105,118,119,121,133,135,124,80,232,236,241,247,254,252,251,250)  then '22c68f86-bbf0-49ba-b2d1-23fa7ccf0259' -- adult return\n" +
      "when e.form_id in(15,34,43,50,58,35,96,111,120,134,136,230,235) then 'e4b506c1-7379-42b6-a374-284469cba8da' -- adult initial\n" +
      "when e.form_id in(30,22,19,18,20,21) then '2cdeded1-3f69-3bda-beff-1ed2ead94eaf' -- lab\n" +
      "when e.form_id in(64,65,69,70,109,110,231) then '83fb6ab2-faec-4d87-a714-93e77a28a201' -- art fast track\n" +
      "when e.form_id in(66,91,112) then 'a1a62d1e-2def-11e9-b210-d663bd873d93' -- defaulter tracing\n" +
      "when e.form_id = 68 then 'c483f10f-d9ee-4b0d-9b8c-c24c1ec24701'\n" +
      "when e.form_id = 71 then '5cf01528-09da-11ea-8d71-362b9e155667'\n" +
      "when e.form_id = 52 then 'b8357314-0f6a-4fc9-a5b7-339f47095d62'\n" +
      "when e.form_id in(97,248) then 'e8f98494-af35-4bb8-9fc7-c409c8fed843' -- anc\n" +
      "when e.form_id = 31 then '1f76643e-2495-11e9-ab14-d663bd873d93' -- discontinuation\n" +
      "when e.form_id in(101,239) then '59ed8e62-7f1f-40ae-a2e3-eabe350277ce' -- tb\n" +
      "when e.form_id in(174,240,249) then '72aa78e0-ee4b-47c3-9073-26f3b9ecc4a7' -- pnc\n" +
      "when e.form_id = 225 then 'a52c57d4-110f-4879-82ae-907b0d90add6'\n" +
      "when e.form_id = 244 then '755b59e6-acbb-4853-abaf-be302039f902' -- cwc\n" +
      "when e.form_id = 164 then '31a371c6-3cfe-431f-94db-4acadad8d209' -- oncology\n" +
      "end kenyaemr_form_uuid\n" +
      "from \n" +
      "amrs.encounter e\n" +
      "inner join\n" +
      "amrs.form f on f.form_id = e.form_id ";

    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {
      String amrsFormId = rs.getString("form_id");
      String encounterId = rs.getString("encounter_id");
      String amrsPid = rs.getString("patient_id");
      String kenyaEmrFormUuid = rs.getString("kenyaemr_form_uuid");

      AMRSEncountersFormMappings form = new AMRSEncountersFormMappings();
      form.setAmrsFormId(amrsFormId);
      form.setAmrsEncounterId(encounterId);
      form.setKenyaemrFormUuid(kenyaEmrFormUuid);
      form.setPatientId(amrsPid);
      form.setAmrsMigrationStatus(false);
      form.setResponseCode(null);
      amrsEncounterFormsMappingService.save(form);

    }
  }

  /// //////////////
  public static void artRefill(String server, String username, String password, String KenyaEMRlocationUuid, AMRSArtRefillService amrsArtRefillService, AMRSTranslater amrsTranslater, AMRSPatientServices amrsPatientServices, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    // String samplePatientList = AMRSSamples.getPersonIdList();
    List<String> stringPIDsList = amrsPatientServices.getAllPatientID();
    String samplePatientList = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);


    String sql = "SELECT o.person_id as patient_id,\n" +
      "e.form_id,\n" +
      "e.visit_id,\n" +
      "o.concept_id,\n" +
      "o.encounter_id,\n" +
      "e.encounter_datetime,\n" +
      "cn.name question,\n" +
      "case when o.value_datetime is not null then o.value_datetime\n" +
      "                when o.value_coded is not null then o.value_coded\n" +
      "                when o.value_numeric is not null then o.value_numeric\n" +
      "                when o.value_text is not null then o.value_text end \n" +
      "                as value\n" +
      "                FROM amrs.obs o\n" +
      "                INNER JOIN amrs.concept c ON o.concept_id=c.concept_id\n" +
      "                INNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id\n" +
      "                and cn.locale_preferred=1\n" +
      "                AND o.person_id IN(" + samplePatientList + ")\n" +
      "                 -- AND c.concept_id in (65,70,82,110,231,527,673,819,820,810,683,703,979,980,981,982,983,1010)\n" +
      "                INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0 \n" +
      "                and e.encounter_type in(186,242)\n" +
      "                ORDER BY patient_id ASC,encounter_id DESC";


    //System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String question = rs.getString("question");
      String value = rs.getString(("value"));
      String visitId = rs.getString(("visit_id"));
      String obsDateTime = rs.getString(("encounter_datetime"));


      List<AMRSArtRefill> artRefillList = amrsArtRefillService.findByPatientIdAndEncounterIdAndConceptId(patientId, encounterId, conceptId);
      if (artRefillList.isEmpty()) {
        AMRSArtRefill amrsArtRefill = new AMRSArtRefill();
        amrsArtRefill.setPatientId(patientId);
        amrsArtRefill.setFormId(formId);
        amrsArtRefill.setConceptId(conceptId);
        amrsArtRefill.setEncounterId(encounterId);
        amrsArtRefill.setQuestion(question);
        amrsArtRefill.setValue(value);
        amrsArtRefill.setVisitId(visitId);
        amrsArtRefill.setObsDateTime(obsDateTime);
        String kenyaemr_patient_uuid = amrsTranslater.KenyaemrPatientUuid(patientId);
        amrsArtRefill.setKenyaEmrPatientUuid(kenyaemr_patient_uuid);
        String kenyaEmrEncounterUuid = "e87aa2ad-6886-422e-9dfd-064e3bfe3aad";
        amrsArtRefill.setKenyaEmrEncounterUuid(kenyaEmrEncounterUuid);
        String kenyaEmrFormUuid = "83fb6ab2-faec-4d87-a714-93e77a28a201";
        amrsArtRefill.setKenyaEmrFormUuid(kenyaEmrFormUuid);
        String kenyaEmrConceptUuid = "";
        kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
        amrsArtRefill.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
        String kenyaEmrValueUuid = "";
        kenyaEmrValueUuid = amrsTranslater.translater(value);
        amrsArtRefill.setKenyaEmrValue(kenyaEmrValueUuid);
        amrsArtRefillService.save(amrsArtRefill);
      }

      // Call method to create and insert the payload

    }
    CareOpenMRSPayload.artRefill(amrsArtRefillService, amrsTranslater, KenyaEMRlocationUuid, url, auth);
  }


  public static void defaulterTracing(String server, String username, String password, String KenyaEMRlocationUuid, AMRSDefaulterTracingService amrsDefaulterTracingService, AMRSTranslater amrsTranslater, AMRSPatientServices amrsPatientServices, String url, String auth) throws IOException, SQLException, JSONException {

   // String samplePatientList = AMRSSamples.getPersonIdList();
    List<String> stringPIDsList = amrsPatientServices.getAllPatientID();
    String samplePatientList = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);


    String sql = "SELECT o.person_id as patient_id,\n" +
      "e.form_id,\n" +
      "o.concept_id,\n" +
      " e.visit_id,\n" +
      "o.encounter_id,\n" +
      "e.encounter_datetime,\n" +
      "cn.name question,\n" +
      "case when o.value_datetime is not null then o.value_datetime\n" +
      "                when o.value_coded is not null then o.value_coded\n" +
      "                when o.value_numeric is not null then o.value_numeric\n" +
      "                when o.value_text is not null then o.value_text end \n" +
      "                as value\n" +
      "                FROM amrs.obs o\n" +
      "                INNER JOIN amrs.concept c ON o.concept_id=c.concept_id\n" +
      "                INNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id\n" +
      "  and cn.locale_preferred=1\n" +
      "                AND o.person_id IN("+ samplePatientList +")\n" +
      "                 -- AND c.concept_id in (66,91,112,364,457,525,909,1093,1486,1487)\n" +
      "                INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0 \n" +
      "                and e.encounter_type in(21)\n" +
      "                ORDER BY patient_id ASC,encounter_id DESC";


    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String question = rs.getString("question");
      String value = rs.getString(("value"));
      String visitId = rs.getString(("visit_id"));
      String obsDatetime = rs.getString(("encounter_datetime"));


      List<AMRSDefaulterTracing> amrsDefaulterTracingList = amrsDefaulterTracingService.findByPatientIdAndEncounterIdAndConceptId(patientId, encounterId, conceptId);
      if (amrsDefaulterTracingList.isEmpty()) {
        AMRSDefaulterTracing amrsDefaulterTracing = new AMRSDefaulterTracing();
        amrsDefaulterTracing.setPatientId(patientId);
        amrsDefaulterTracing.setFormId(formId);
        amrsDefaulterTracing.setConceptId(conceptId);
        amrsDefaulterTracing.setEncounterId(encounterId);
        amrsDefaulterTracing.setQuestion(question);
        amrsDefaulterTracing.setValue(value);
        amrsDefaulterTracing.setVisitId(visitId);
        amrsDefaulterTracing.setObsDatetime(obsDatetime);

        String kenyaemrPatientUuid = "";
        kenyaemrPatientUuid = amrsTranslater.KenyaemrPatientUuid(patientId);
        amrsDefaulterTracing.setKenyaEmrPatientUuid(kenyaemrPatientUuid);
        String kenyaEmrEncounterUuid = "1495edf8-2df2-11e9-b210-d663bd873d93";
        amrsDefaulterTracing.setKenyaEmrEncounterUuid(kenyaEmrEncounterUuid);
        String kenyaEmrFormUuid = "a1a62d1e-2def-11e9-b210-d663bd873d93";
        amrsDefaulterTracing.setKenyaEmrFormUuid(kenyaEmrFormUuid);
        String kenyaEmrConceptUuid = "";
        kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
        amrsDefaulterTracing.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
        String kenyaEmrValueUuid = "";
        kenyaEmrValueUuid = amrsTranslater.translater(value);
        amrsDefaulterTracing.setKenyaEmrValueUuid(kenyaEmrValueUuid);


        amrsDefaulterTracingService.save(amrsDefaulterTracing);
      }

      // Call method to create and insert the payload
    }

    CareOpenMRSPayload.defaulterTracing(amrsDefaulterTracingService, amrsTranslater,KenyaEMRlocationUuid, url, auth);

  }

  public static void processOtzActivity(String server, String username, String password, String KenyaEMRlocationUuid, AMRSOtzActivityService amrsOtzActivityService, AMRSTranslater amrsTranslater, AMRSPatientServices amrsPatientServices, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    //String samplePatientList = AMRSSamples.getPersonIdList();
    List<String> stringPIDsList = amrsPatientServices.getAllPatientID();
    String samplePatientList = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);



    String sql = "SELECT o.person_id as patient_id,e.form_id,e.visit_id,o.concept_id,o.encounter_id,o.obs_datetime,e.encounter_datetime,  \n" +
      "                  cn.name question,c.datatype_id,  \n" +
      "                 case when o.value_datetime is not null then o.value_datetime \n" +
      "                 when o.value_coded is not null then o.value_coded  \n" +
      "                 when o.value_numeric is not null then o.value_numeric  \n" +
      "                 when o.value_text is not null then o.value_text end   \n" +
      "                 as value    \n" +
      "                                   FROM amrs.obs o    \n" +
      "                                   INNER JOIN amrs.concept c ON o.concept_id=c.concept_id    \n" +
      "                INNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id \n" +
      "                                  and cn.locale_preferred=1  \n" +
      "                               -- LEFT JOIN amrs.concept_name cn_answer ON o.value_coded = cn_answer.concept_id and cn_answer.locale_preferred=1\\n  +\n" +
      "                       AND o.person_id IN("+samplePatientList+")    \n" +
      "                                   AND c.concept_id in (11032,11037, 11033, 12300, 11034, 12272, 11035, 9302, 10984, 9467) \n" +
      "                                   INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0    \n" +
      "                                   and e.encounter_type in(284) \n" +
      "                                    and e.encounter_id in (14285867, 14287456)  \n" +
      "                                   ORDER BY patient_id ASC,\n" +
      "                                   encounter_id DESC";


    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");

      String encounterDatetime = rs.getString("encounter_datetime");
      String value = rs.getString("value");
      String question = rs.getString("question");
      String dataType = rs.getString("datatype_id");
      String visitId = rs.getString("visit_id");
      String obsDateTime = rs.getString("obs_datetime");

      // Check if record already exists
      List<AMRSOtzActivity> existingRecords = amrsOtzActivityService.findByEncounterConceptAndPatient(encounterId, conceptId, patientId);
      if (!existingRecords.isEmpty()) {
        System.out.println("Duplicate record found for encounterId: " + encounterId + ", conceptId: " + conceptId + ", patientId: " + patientId);
        continue; // Skip saving to avoid duplication
      }
      String kenyaemr_value = dataType.equals("2") ? amrsTranslater.translater(value) : value;
      AMRSOtzActivity amrsOtzActivity = new AMRSOtzActivity();
      amrsOtzActivity.setPatientId(patientId);
      amrsOtzActivity.setFormId(formId);
      amrsOtzActivity.setConceptId(conceptId);
      amrsOtzActivity.setEncounterId(encounterId);
      amrsOtzActivity.setValue(value);
      amrsOtzActivity.setConceptDataTypeId(dataType);
      amrsOtzActivity.setVisitId(visitId);
      amrsOtzActivity.setQuestion(question);
      amrsOtzActivity.setObsDateTime(obsDateTime);
      amrsOtzActivity.setKenyaemrEncounterTypeUuid("162386c8-0464-11ea-9a9f-362b9e155667");
      amrsOtzActivity.setKenyaemrFormUuid("3ae95d48-0464-11ea-8d71-362b9e155667");
      amrsOtzActivity.setKenyaEmrValue(kenyaemr_value);
      amrsOtzActivity.setKenyaEmrEncounterDateTime(encounterDatetime);
      String kenyaemr_patient_uuid = amrsTranslater.KenyaemrPatientUuid(patientId);
      String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
      amrsOtzActivity.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
      amrsOtzActivity.setKenyaemrPatientUuid(kenyaemr_patient_uuid);
      amrsOtzActivityService.save(amrsOtzActivity);
    }
    OTZPayload.processOTZActivity(amrsOtzActivityService, amrsTranslater,KenyaEMRlocationUuid, url, auth);
  }


  public static void processOtzDiscontinuation(String server, String username, String password, String KenyaEMRlocationUuid, AMRSOtzDiscontinuationService amrsOtzDiscontinuationService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

   // String samplePatientList = AMRSSamples.getPersonIdList();
    List<String> stringPIDsList = amrsPatientServices.getAllPatientID();
    String samplePatientList = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);


    String sql = "SELECT \n" +
      "    o.person_id AS patient_id,\n" +
      "    e.form_id,\n" +
      "    e.visit_id,\n" +
      "    o.concept_id,\n" +
      "    o.encounter_id,\n" +
      "    o.obs_datetime,\n" +
      "    e.encounter_datetime,\n" +
      "    cn.name question,\n" +
      "    e.location_id,\n" +
      "    c.datatype_id,\n" +
      "    CASE\n" +
      "        WHEN o.value_datetime IS NOT NULL THEN o.value_datetime\n" +
      "        WHEN o.value_coded IS NOT NULL THEN o.value_coded\n" +
      "        WHEN o.value_numeric IS NOT NULL THEN o.value_numeric\n" +
      "        WHEN o.value_text IS NOT NULL THEN o.value_text\n" +
      "    END AS value\n" +
      "FROM\n" +
      "    amrs.obs o\n" +
      "        INNER JOIN\n" +
      "    amrs.concept c ON o.concept_id = c.concept_id\n" +
      "        INNER JOIN\n" +
      "    amrs.concept_name cn ON o.concept_id = cn.concept_id\n" +
      "        AND cn.locale_preferred = 1\n" +
      "         AND o.concept_id IN (1596)\n" +
      "        INNER JOIN\n" +
      "    amrs.encounter e ON o.encounter_id = e.encounter_id\n" +
      "     AND o.person_id in (" + samplePatientList + ")\n" +
      "        AND e.voided = 0\n" +
      "        AND o.voided = 0\n" +
      "        AND e.encounter_type IN (285)\n" +
      "ORDER BY patient_id ASC , encounter_id DESC";

    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String encounterDatetime = rs.getString("encounter_datetime");
      String value = rs.getString("value");
      String question = rs.getString("question");
      String dataType = rs.getString("datatype_id");
      String visitId = rs.getString("visit_id");
      String obsDateTime = rs.getString("obs_datetime");

      // Check if record already exists
      List<AMRSOtzDiscontinuation> existingRecords = amrsOtzDiscontinuationService.findByEncounterConceptAndPatient(encounterId, conceptId, patientId);
      if (!existingRecords.isEmpty()) {
        System.out.println("Duplicate record found for encounterId: " + encounterId + ", conceptId: " + conceptId + ", patientId: " + patientId);
        continue; // Skip saving to avoid duplication
      }
      String kenyaemr_value = dataType.equals("2") ? amrsTranslater.translater(value) : value;

      AMRSOtzDiscontinuation amrsOtzDiscontinuation = new AMRSOtzDiscontinuation();
      amrsOtzDiscontinuation.setPatientId(patientId);
      amrsOtzDiscontinuation.setFormId(formId);
      amrsOtzDiscontinuation.setConceptId(conceptId);
      amrsOtzDiscontinuation.setEncounterId(encounterId);
      amrsOtzDiscontinuation.setValue(value);
      amrsOtzDiscontinuation.setConceptDataTypeId(dataType);
      amrsOtzDiscontinuation.setVisitId(visitId);
      amrsOtzDiscontinuation.setQuestion(question);
      amrsOtzDiscontinuation.setObsDateTime(obsDateTime);
      amrsOtzDiscontinuation.setKenyaemrEncounterTypeUuid("162382b8-0464-11ea-9a9f-362b9e155667");
      amrsOtzDiscontinuation.setKenyaemrFormUuid("3ae955dc-0464-11ea-8d71-362b9e155667");
      amrsOtzDiscontinuation.setKenyaEmrValue(kenyaemr_value);
      amrsOtzDiscontinuation.setKenyaEmrEncounterDateTime(encounterDatetime);
      String kenyaemr_patient_uuid = amrsTranslater.KenyaemrPatientUuid(patientId);
      String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
      amrsOtzDiscontinuation.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
      amrsOtzDiscontinuation.setKenyaemrPatientUuid(kenyaemr_patient_uuid);
      amrsOtzDiscontinuationService.save(amrsOtzDiscontinuation);
    }
    OTZPayload.processOTZDiscontinuation(amrsOtzDiscontinuationService, amrsTranslater, KenyaEMRlocationUuid, url, auth);
  }

  public static void processOtzEnrollments(String server, String username, String password, String KenyaEMRlocationUuid, AMRSOtzEnrollmentService amrsOtzEnrollmentService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    String samplePatientList = AMRSSamples.getPersonIdList();


    String sql = "SELECT \n" +
      "    o.person_id AS patient_id,\n" +
      "    e.form_id,\n" +
      "    e.visit_id,\n" +
      "    o.concept_id,\n" +
      "    o.encounter_id,\n" +
      "    o.obs_datetime,\n" +
      "    e.encounter_datetime,\n" +
      "    cn.name question,\n" +
      "    e.location_id,\n" +
      "    c.datatype_id,\n" +
      "    CASE\n" +
      "        WHEN o.value_datetime IS NOT NULL THEN o.value_datetime\n" +
      "        WHEN o.value_coded IS NOT NULL THEN o.value_coded\n" +
      "        WHEN o.value_numeric IS NOT NULL THEN o.value_numeric\n" +
      "        WHEN o.value_text IS NOT NULL THEN o.value_text\n" +
      "    END AS value\n" +
      "FROM\n" +
      "    amrs.obs o\n" +
      "        INNER JOIN\n" +
      "    amrs.concept c ON o.concept_id = c.concept_id\n" +
      "        INNER JOIN\n" +
      "    amrs.concept_name cn ON o.concept_id = cn.concept_id\n" +
      "        AND cn.locale_preferred = 1\n" +
      "         AND o.concept_id IN (10793)\n" +
      "        INNER JOIN\n" +
      "    amrs.encounter e ON o.encounter_id = e.encounter_id\n" +
      "     AND o.person_id in (" + samplePatientList + ")\n" +
      "        AND e.voided = 0\n" +
      "        AND o.voided = 0\n" +
      "        AND e.encounter_type IN (283)\n" +
      "ORDER BY patient_id ASC , encounter_id DESC";

  //  System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String encounterDatetime = rs.getString("encounter_datetime");
      String value = rs.getString("value");
      String question = rs.getString("question");
      String dataType = rs.getString("datatype_id");
      String visitId = rs.getString("visit_id");
      String obsDateTime = rs.getString("obs_datetime");

      // Check if record already exists
      List<AMRSOtzEnrollment> existingRecords = amrsOtzEnrollmentService.findByEncounterConceptAndPatient(encounterId, conceptId, patientId);
      if (!existingRecords.isEmpty()) {
        System.out.println("Duplicate record found for encounterId: " + encounterId + ", conceptId: " + conceptId + ", patientId: " + patientId);
        continue; // Skip saving to avoid duplication
      }


      String kenyaemr_value = dataType.equals("2") ? amrsTranslater.translater(value) : value;
      AMRSOtzEnrollment amrsOtzEnrollment = new AMRSOtzEnrollment();
      amrsOtzEnrollment.setPatientId(patientId);
      amrsOtzEnrollment.setFormId(formId);
      amrsOtzEnrollment.setConceptId(conceptId);
      amrsOtzEnrollment.setEncounterId(encounterId);
      amrsOtzEnrollment.setValue(value);
      amrsOtzEnrollment.setConceptDataTypeId(dataType);
      amrsOtzEnrollment.setVisitId(visitId);
      amrsOtzEnrollment.setQuestion(question);
      amrsOtzEnrollment.setObsDateTime(obsDateTime);
      amrsOtzEnrollment.setKenyaemrEncounterTypeUuid("16238574-0464-11ea-9a9f-362b9e155667");
      amrsOtzEnrollment.setKenyaemrFormUuid("3ae95898-0464-11ea-8d71-362b9e155667");
      amrsOtzEnrollment.setKenyaEmrValue(kenyaemr_value);
      amrsOtzEnrollment.setKenyaEmrEncounterDateTime(encounterDatetime);
      String kenyaemr_patient_uuid = amrsTranslater.KenyaemrPatientUuid(patientId);
      String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
      amrsOtzEnrollment.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
      amrsOtzEnrollment.setKenyaemrPatientUuid(kenyaemr_patient_uuid);
      amrsOtzEnrollmentService.save(amrsOtzEnrollment);
    }
    OTZPayload.processOTZEnrollment(amrsOtzEnrollmentService, amrsTranslater,KenyaEMRlocationUuid, url, auth);
  }

  public static void processTBScreening(String server, String username, String password, String KenyaEMRlocationUuid, AMRSTbScreeningService amrsTbScreeningService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

   // String samplePatientList = AMRSSamples.getPersonIdList();
    List<String> stringPIDsList = amrsPatientServices.getAllPatientID();
    String samplePatientList = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);



    String sql = "SELECT \n" +
      "    o.person_id AS patient_id,\n" +
      "    e.form_id,\n" +
      "    e.visit_id,\n" +
      "    o.concept_id,\n" +
      "    o.encounter_id,\n" +
      "    o.obs_datetime,\n" +
      "    e.encounter_datetime,\n" +
      "    cn.name question,\n" +
      "    e.location_id,\n" +
      "    c.datatype_id,\n" +
      "    CASE\n" +
      "        WHEN o.value_datetime IS NOT NULL THEN o.value_datetime\n" +
      "        WHEN o.value_coded IS NOT NULL THEN o.value_coded\n" +
      "        WHEN o.value_numeric IS NOT NULL THEN o.value_numeric\n" +
      "        WHEN o.value_text IS NOT NULL THEN o.value_text\n" +
      "    END AS value\n" +
      "FROM\n" +
      "    amrs.obs o\n" +
      "        INNER JOIN\n" +
      "    amrs.concept c ON o.concept_id = c.concept_id\n" +
      "        INNER JOIN\n" +
      "    amrs.concept_name cn ON o.concept_id = cn.concept_id\n" +
      "        AND cn.locale_preferred = 1\n" +
      "         AND o.concept_id IN (6174)\n" +
      "        INNER JOIN\n" +
      "    amrs.encounter e ON o.encounter_id = e.encounter_id\n" +
      "      AND o.person_id in (" + samplePatientList + ")\n" +
      "        AND e.voided = 0\n" +
      "        AND o.voided = 0\n" +
      "        AND e.encounter_type IN (1, 2, 3,4, 105, 106)\n" +
      "ORDER BY patient_id ASC";


   // System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String encounterDatetime = rs.getString("encounter_datetime");
      String value = rs.getString("value");
      String question = rs.getString("question");
      String dataType = rs.getString("datatype_id");
      String visitId = rs.getString("visit_id");
      String obsDateTime = rs.getString("obs_datetime");

      // Check if record already exists
      List<AMRSTbScreening> existingRecords = amrsTbScreeningService.findByEncounterConceptAndPatient(encounterId, conceptId, patientId);
      if (!existingRecords.isEmpty()) {
        System.out.println("Duplicate record found for encounterId: " + encounterId + ", conceptId: " + conceptId + ", patientId: " + patientId);
        continue; // Skip saving to avoid duplication
      }


      String kenyaemr_value = dataType.equals("2") ? amrsTranslater.translater(value) : value;

      AMRSTbScreening amrsTbScreening = new AMRSTbScreening();
      amrsTbScreening.setPatientId(patientId);
      amrsTbScreening.setFormId(formId);
      amrsTbScreening.setConceptId(conceptId);
      amrsTbScreening.setEncounterId(encounterId);
      amrsTbScreening.setValue(value);
      amrsTbScreening.setConceptDataTypeId(dataType);
      amrsTbScreening.setVisitId(visitId);
      amrsTbScreening.setQuestion(question);
      amrsTbScreening.setObsDateTime(obsDateTime);
      amrsTbScreening.setKenyaemrEncounterTypeUuid("ed6dacc9-0827-4c82-86be-53c0d8c449be");
      amrsTbScreening.setKenyaemrFormUuid("59ed8e62-7f1f-40ae-a2e3-eabe350277ce");
      amrsTbScreening.setKenyaEmrValue(kenyaemr_value);
      amrsTbScreening.setKenyaEmrEncounterDateTime(encounterDatetime);
      String kenyaemr_patient_uuid = amrsTranslater.KenyaemrPatientUuid(patientId);
      String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
      amrsTbScreening.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
      amrsTbScreening.setKenyaemrPatientUuid(kenyaemr_patient_uuid);
      amrsTbScreeningService.save(amrsTbScreening);
    }
    TBPayload.processTBScreening(amrsTbScreeningService, amrsTranslater,KenyaEMRlocationUuid, url, auth);
  }

  public static void ovc(String server, String username, String password, String KenyaEMRlocationUuid, AMRSOvcService amrsOvcService, AMRSTranslater amrsTranslater, AMRSPatientServices amrsPatientServices, String url, String auth) throws IOException, JSONException, SQLException {


    List<String> stringPIDsList = amrsPatientServices.getAllPatientID();
    String samplePatientList = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);

    String sql = "SELECT o.person_id as patient_id,\n" +
      "                    e.form_id,\n" +
      "                    o.concept_id,\n" +
      "                    o.encounter_id,\n" +
      "                    cn.name question,\n" +
      "                    e.visit_id,\n" +
      "                    e.encounter_datetime,\n" +
      "                    o.obs_datetime,\n" +
      "                    case when o.value_datetime is not null then o.value_datetime\n" +
      "                                    when o.value_coded is not null then o.value_coded\n" +
      "                                    when o.value_numeric is not null then o.value_numeric\n" +
      "                                    when o.value_text is not null then o.value_text end \n" +
      "                                    as value\n" +
      "                                    FROM amrs.obs o\n" +
      "                                    INNER JOIN amrs.concept c ON o.concept_id=c.concept_id\n" +
      "                                    INNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id\n" +
      "                                     and cn.locale_preferred=1\n" +
      "                                     AND o.person_id IN("+ samplePatientList +")\n" +
      "                                    -- AND c.concept_id in (1324,71)\n" +
      "                                   INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0 \n" +
      "                                  and e.encounter_type in(24,220)\n" +
      "                                  ORDER BY patient_id ASC,encounter_id DESC;";

    // System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String question = rs.getString("question");
      String value = rs.getString(("value"));
      String visitId = rs.getString("visit_id");
      String obsDatetime = rs.getString("obs_datetime");
      String encounterDatetime = rs.getString("encounter_datetime");


      List<AMRSOvc> amrsOvcList = amrsOvcService.findByPatientIdAndEncounterIdAndConceptId(patientId, encounterId, conceptId);
      if (amrsOvcList.isEmpty()) {
        AMRSOvc amrsOvc = new AMRSOvc();
        amrsOvc.setPatientId(patientId);
        amrsOvc.setFormId(formId);
        amrsOvc.setConceptId(conceptId);
        amrsOvc.setEncounterId(encounterId);
        amrsOvc.setQuestion(question);
        amrsOvc.setValue(value);
        amrsOvc.setVisitId(visitId);
        amrsOvc.setObsDateTime(obsDatetime);
        amrsOvc.setKenyaEmrEncounterDateTime(encounterDatetime);
        String kenyaEmrPatientUuid = "";
        kenyaEmrPatientUuid = amrsTranslater.KenyaemrPatientUuid(patientId);
        amrsOvc.setKenyaemrPatientUuid(kenyaEmrPatientUuid);
        String kenyaEmrEncounterUuid = "5cf00d9e-09da-11ea-8d71-362b9e155667";
        amrsOvc.setKenyaemrEncounterTypeUuid(kenyaEmrEncounterUuid);
        String kenyaEmrFormUuid = "5cf013e8-09da-11ea-8d71-362b9e155667";
        amrsOvc.setKenyaemrFormUuid(kenyaEmrFormUuid);
        String kenyaemrVisitUuid = "";
        kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
        amrsOvc.setKenyaemrVisitUuid(kenyaemrVisitUuid);
        String kenyaEmrConceptUuid = "";
        kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
        amrsOvc.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
        String kenyaEmrValueUuid = "";
        kenyaEmrValueUuid = amrsTranslater.translater(value);
        amrsOvc.setKenyaEmrValue(kenyaEmrValueUuid);


        amrsOvcService.save(amrsOvc);
      }

      // Call method to create and insert the payload
    }

    CareOpenMRSPayload.ovc(amrsOvcService, amrsTranslater, KenyaEMRlocationUuid, url, auth);
  }

  public static void prepInitial(String server, String username, String password, String KenyaEMRlocationUuid, AMRSPrepInitialService amrsPrepInitialService, AMRSTranslater amrsTranslater, AMRSPatientServices amrsPatientServices, String url, String auth) throws IOException, JSONException, SQLException {

    List<String> stringPIDsList = amrsPatientServices.getAllPatientID();
    String samplePatientList = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);


    String sql = "SELECT o.person_id as patient_id, \n" +
      "                                    e.form_id, \n" +
      "                                    o.concept_id, \n" +
      "                                    o.encounter_id, \n" +
      "                                    cn.name question, \n" +
      "                                    e.visit_id, \n" +
      "                                    e.encounter_datetime, \n" +
      "                                    o.obs_datetime, \n" +
      "                                    case when o.value_datetime is not null then o.value_datetime \n" +
      "                                                    when o.value_coded is not null then o.value_coded \n" +
      "                                                    when o.value_numeric is not null then o.value_numeric \n" +
      "                                                    when o.value_text is not null then o.value_text end  \n" +
      "                                                    as value \n" +
      "                                                    FROM amrs.obs o \n" +
      "                                                    INNER JOIN amrs.concept c ON o.concept_id=c.concept_id \n" +
      "                                                   INNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id \n" +
      "                                                   and cn.locale_preferred=1 \n" +
      "                                                     AND o.person_id IN("+ samplePatientList+") \n" +
      "                                                     -- AND c.concept_id in (1532,782,1086,1432,1435) \n" +
      "                                                   INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0  \n" +
      "                                                  and e.encounter_type in(133) \n" +
      "                                                  ORDER BY patient_id ASC,encounter_id DESC;";

    // System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String question = rs.getString("question");
      String value = rs.getString(("value"));
      String visitId = rs.getString("visit_id");
      String obsDatetime = rs.getString("obs_datetime");
      String encounterDatetime = rs.getString("encounter_datetime");

      List<AMRSPrepInitial> prepInitiationList = amrsPrepInitialService.findByPatientIdAndEncounterIdAndConceptId(patientId, encounterId, conceptId);
      if (prepInitiationList.isEmpty()) {
        AMRSPrepInitial amrsPrepInitial = new AMRSPrepInitial();
        amrsPrepInitial.setPatientId(patientId);
        amrsPrepInitial.setFormId(formId);
        amrsPrepInitial.setConceptId(conceptId);
        amrsPrepInitial.setEncounterId(encounterId);
        amrsPrepInitial.setQuestion(question);
        amrsPrepInitial.setValue(value);
        amrsPrepInitial.setVisitId(visitId);
        amrsPrepInitial.setObsDateTime(obsDatetime);
        amrsPrepInitial.setKenyaEmrEncounterDatetime(encounterDatetime);
        String kenyaEmrPatientUuid = amrsTranslater.KenyaemrPatientUuid(patientId);
        amrsPrepInitial.setKenyaEmrPatientUuid(kenyaEmrPatientUuid);
        String kenyaEmrEncounterUuid = "706a8b12-c4ce-40e4-aec3-258b989bf6d3";
        amrsPrepInitial.setKenyaEmrEncounterUuid(kenyaEmrEncounterUuid);
        String kenyaEmrFormUuid = "1bfb09fc-56d7-4108-bd59-b2765fd312b8";
        amrsPrepInitial.setKenyaEmrFormUuid(kenyaEmrFormUuid);
        String kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
        amrsPrepInitial.setKenyaEmrVisitUuid(kenyaemrVisitUuid);
        String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
        amrsPrepInitial.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
        String kenyaEmrValueUuid = "";
        kenyaEmrValueUuid = amrsTranslater.translater(value);
        amrsPrepInitial.setKenyaEmrValue(kenyaEmrValueUuid);

        if (!Objects.equals(kenyaEmrValueUuid, "") && !Objects.equals(kenyaEmrConceptUuid, "")) {

          amrsPrepInitialService.save(amrsPrepInitial);
        }
      }

      // Call method to create and insert the payload
    }

    CareOpenMRSPayload.prepInitial(amrsPrepInitialService, amrsPatientServices, amrsTranslater, KenyaEMRlocationUuid, url, auth);


  }

  public static void prepFollowUp(String server, String username, String password, String KenyaEMRlocationUuid, AMRSPrepFollowUpService amrsPrepFollowUpService, AMRSTranslater amrsTranslater, AMRSPatientServices amrsPatientServices, String url, String auth) throws IOException, JSONException, SQLException {


    String sql = "SELECT o.person_id as patient_id, \n" +
      "                                    e.form_id, \n" +
      "                                    o.concept_id, \n" +
      "                                    o.encounter_id, \n" +
      "                                    cn.name question, \n" +
      "                                    e.visit_id, \n" +
      "                                    e.encounter_datetime, \n" +
      "                                    o.obs_datetime, \n" +
      "                                    case when o.value_datetime is not null then o.value_datetime \n" +
      "                                                    when o.value_coded is not null then o.value_coded \n" +
      "                                                    when o.value_numeric is not null then o.value_numeric \n" +
      "                                                    when o.value_text is not null then o.value_text end  \n" +
      "                                                    as value \n" +
      "                                                    FROM amrs.obs o \n" +
      "                                                    INNER JOIN amrs.concept c ON o.concept_id=c.concept_id \n" +
      "                                                   INNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id \n" +
      "                                                   and cn.locale_preferred=1 \n" +
      "                                                     AND o.person_id IN(1151769) \n" +
      "                                                     -- AND c.concept_id in (1532,782,1086,1432,1435) \n" +
      "                                                   INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0  \n" +
      "                                                  and e.encounter_type in(134) \n" +
      "                                                  ORDER BY patient_id ASC,encounter_id DESC;";

    //System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String question = rs.getString("question");
      String value = rs.getString(("value"));
      String visitId = rs.getString("visit_id");
      String obsDatetime = rs.getString("obs_datetime");
      String encounterDatetime = rs.getString("encounter_datetime");

      List<AMRSPrepFollowUp> amrsPrepFollowUpList = amrsPrepFollowUpService.findByPatientIdAndEncounterIdAndConceptId(patientId, encounterId, conceptId);

      if (amrsPrepFollowUpList.isEmpty()) {
        AMRSPrepFollowUp amrsPrepFollowUp = new AMRSPrepFollowUp();
        amrsPrepFollowUp.setPatientId(patientId);
        amrsPrepFollowUp.setFormId(formId);
        amrsPrepFollowUp.setConceptId(conceptId);
        amrsPrepFollowUp.setEncounterId(encounterId);
        amrsPrepFollowUp.setQuestion(question);
        amrsPrepFollowUp.setValue(value);
        amrsPrepFollowUp.setVisitId(visitId);
        amrsPrepFollowUp.setObsDateTime(obsDatetime);
        amrsPrepFollowUp.setKenyaEmrEncounterDatetime(encounterDatetime);
        String kenyaEmrPatientUuid = amrsTranslater.KenyaemrPatientUuid(patientId);
        amrsPrepFollowUp.setKenyaEmrPatientUuid(kenyaEmrPatientUuid);
        String kenyaEmrEncounterUuid = "c4a2be28-6673-4c36-b886-ea89b0a42116";
        amrsPrepFollowUp.setKenyaEmrEncounterUuid(kenyaEmrEncounterUuid);
        String kenyaEmrFormUuid = "ee3e2017-52c0-4a54-99ab-ebb542fb8984";
        amrsPrepFollowUp.setKenyaEmrFormUuid(kenyaEmrFormUuid);
        String kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
        amrsPrepFollowUp.setKenyaEmrVisitUuid(kenyaemrVisitUuid);
        String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
        amrsPrepFollowUp.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
        String kenyaEmrValueUuid = "";
        kenyaEmrValueUuid = amrsTranslater.translater(value);
        amrsPrepFollowUp.setKenyaEmrValue(kenyaEmrValueUuid);

        if (!Objects.equals(kenyaEmrValueUuid, "") && !Objects.equals(kenyaEmrConceptUuid, "")) {

          amrsPrepFollowUpService.save(amrsPrepFollowUp);

        }


      }

      // Call method to create and insert the payload
    }

    CareOpenMRSPayload.prepFollowUp(amrsPrepFollowUpService, amrsPatientServices, amrsTranslater, url, auth);


  }

  public static void prepMonthlyRefill(String server, String username, String password, String KenyaEMRlocationUuid, AMRSPrepMonthlyRefillService amrsPrepMonthlyRefillService, AMRSTranslater amrsTranslater, AMRSPatientServices amrsPatientServices, String url, String auth) throws IOException, JSONException, SQLException {


    String sql = "SELECT o.person_id as patient_id, \n" +
      "                                    e.form_id, \n" +
      "                                    o.concept_id, \n" +
      "                                    o.encounter_id, \n" +
      "                                    cn.name question, \n" +
      "                                    e.visit_id, \n" +
      "                                    e.encounter_datetime, \n" +
      "                                    o.obs_datetime, \n" +
      "                                    case when o.value_datetime is not null then o.value_datetime \n" +
      "                                                    when o.value_coded is not null then o.value_coded \n" +
      "                                                    when o.value_numeric is not null then o.value_numeric \n" +
      "                                                    when o.value_text is not null then o.value_text end  \n" +
      "                                                    as value \n" +
      "                                                    FROM amrs.obs o \n" +
      "                                                    INNER JOIN amrs.concept c ON o.concept_id=c.concept_id \n" +
      "                                                   INNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id \n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\tand cn.locale_preferred=1 \n" +
      "                                                     AND o.person_id IN(1151769) \n" +
      "                                                     -- AND c.concept_id in (1532,782,1086,1432,1435) \n" +
      "                                                   INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0  \n" +
      "                                                  and e.encounter_type in(262) \n" +
      "                                                  ORDER BY patient_id ASC,encounter_id DESC;";

    // System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String question = rs.getString("question");
      String value = rs.getString(("value"));
      String visitId = rs.getString("visit_id");
      String obsDatetime = rs.getString("obs_datetime");
      String encounterDatetime = rs.getString("encounter_datetime");

      List<AMRSPrepMonthlyRefill> amrsPrepMonthlyRefillList = amrsPrepMonthlyRefillService.findByPatientIdAndEncounterIdAndConceptId(patientId, encounterId, conceptId);
      if (amrsPrepMonthlyRefillList.isEmpty()) {
        AMRSPrepMonthlyRefill amrsPrepMonthlyRefill = new AMRSPrepMonthlyRefill();
        amrsPrepMonthlyRefill.setPatientId(patientId);
        amrsPrepMonthlyRefill.setFormId(formId);
        amrsPrepMonthlyRefill.setConceptId(conceptId);
        amrsPrepMonthlyRefill.setEncounterId(encounterId);
        amrsPrepMonthlyRefill.setQuestion(question);
        amrsPrepMonthlyRefill.setValue(value);
        amrsPrepMonthlyRefill.setVisitId(visitId);
        amrsPrepMonthlyRefill.setObsDateTime(obsDatetime);
        amrsPrepMonthlyRefill.setKenyaEmrEncounterDatetime(encounterDatetime);
        String kenyaEmrPatientUuid = amrsTranslater.KenyaemrPatientUuid(patientId);
        amrsPrepMonthlyRefill.setKenyaEmrPatientUuid(kenyaEmrPatientUuid);
        String kenyaEmrEncounterUuid = "c4a2be28-6673-4c36-b886-ea89b0a42116";
        amrsPrepMonthlyRefill.setKenyaEmrEncounterUuid(kenyaEmrEncounterUuid);
        String kenyaEmrFormUuid = "ee3e2017-52c0-4a54-99ab-ebb542fb8984";
        amrsPrepMonthlyRefill.setKenyaEmrFormUuid(kenyaEmrFormUuid);
        String kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
        amrsPrepMonthlyRefill.setKenyaEmrVisitUuid(kenyaemrVisitUuid);
        String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
        amrsPrepMonthlyRefill.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
        String kenyaEmrValueUuid = "";
        kenyaEmrValueUuid = amrsTranslater.translater(value);
        amrsPrepMonthlyRefill.setKenyaEmrValue(kenyaEmrValueUuid);

        if (!Objects.equals(kenyaEmrValueUuid, "") && !Objects.equals(kenyaEmrConceptUuid, "")) {

          amrsPrepMonthlyRefillService.save(amrsPrepMonthlyRefill);
        }


      }

      // Call method to create and insert the payload
    }

    CareOpenMRSPayload.prepMonthlyRefill(amrsPrepMonthlyRefillService, amrsPatientServices, amrsTranslater, KenyaEMRlocationUuid, url, auth);


  }

  public static void processCovid(String server, String username, String password, String KenyaEMRlocationUuid, AMRSCovidService amrsCovidService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    String samplePatientList = AMRSSamples.getPersonIdList();

    String sql = "select\n" +
      "\to.person_id,\n" +
      "\te.form_id,\n" +
      "\te.visit_id,\n" +
      "\tcn.name as question,\n" +
      "\te.encounter_id,\n" +
      "\te.encounter_datetime,\n" +
      "\te.encounter_type,\n" +
      "\to.concept_id,\n" +
      "\to.obs_datetime,\n" +
      "\tCOALESCE(o.value_coded, o.value_datetime, o.value_numeric, o.value_text) as value,\n" +
      "\tcd.name as value_type,\n" +
      "\tc.datatype_id,\n" +
      "\tet.name encounterName,\n" +
      "\t\"COVID-19 Assessment form\" as Category\n" +
      "from\n" +
      "\tamrs.obs o\n" +
      "inner join amrs.encounter e on\n" +
      "\t(o.encounter_id = e.encounter_id)\n" +
      "inner join amrs.encounter_type et on\n" +
      "\tet.encounter_type_id = e.encounter_type\n" +
      "inner join amrs.concept c on\n" +
      "\tc.concept_id = o.concept_id\n" +
      "inner join amrs.concept_datatype cd on\n" +
      "\tcd.concept_datatype_id = c.datatype_id\n" +
      "INNER JOIN amrs.concept_name cn ON\n" +
      "\to.concept_id = cn.concept_id\n" +
      "where\n" +
      "\te.encounter_type in (208)\n" +
      "\tand o.concept_id in (984, 1390, 1915, 1944, 2300, 6419, 9728, 10485, 10958, 11124, 11899, 11906, 11908, 11909, 11911, 11912, 11916)\n" +
      //  "\tand e.location_id in (2, 98, 339)\n" +
      "\tand e.voided = 0\n" +
      "\tand cd.name <> 'N/A'\n" +
      "\tand o.person_id in (" + samplePatientList + ")\n" +
      "order by\n" +
      "\to.person_id,\n" +
      "\te.encounter_id desc;";

    // System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("person_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String encounterDatetime = rs.getString("encounter_datetime");
      String value = rs.getString("value");
      String question = rs.getString("question");
      String dataType = rs.getString("datatype_id");
      String visitId = rs.getString("visit_id");
      String obsDateTime = rs.getString("obs_datetime");

      // Check if record already exists
      List<AMRSCovid> existingRecords = amrsCovidService.findByEncounterConceptAndPatient(encounterId, conceptId, patientId);
      if (!existingRecords.isEmpty()) {
        System.out.println("Duplicate record found for encounterId: " + encounterId + ", conceptId: " + conceptId + ", patientId: " + patientId);
        continue; // Skip saving to avoid duplication
      }
      String kenyaemr_value = dataType.equals("2") ? amrsTranslater.translater(value) : value;

      AMRSCovid amrsCovid = new AMRSCovid();
      amrsCovid.setPatientId(patientId);
      amrsCovid.setFormId(formId);
      amrsCovid.setConceptId(conceptId);
      amrsCovid.setEncounterId(encounterId);
      amrsCovid.setValue(value);
      amrsCovid.setConceptDataTypeId(dataType);
      amrsCovid.setVisitId(visitId);
      amrsCovid.setQuestion(question);
      amrsCovid.setObsDateTime(obsDateTime);
      amrsCovid.setKenyaemrEncounterTypeUuid("86709cfc-1490-11ec-82a8-0242ac130003");
      amrsCovid.setKenyaemrFormUuid("86709f36-1490-11ec-82a8-0242ac130003");
      amrsCovid.setKenyaEmrValue(kenyaemr_value);
      amrsCovid.setKenyaEmrEncounterDateTime(encounterDatetime);
      String kenyaemr_patient_uuid = amrsTranslater.KenyaemrPatientUuid(patientId);
      String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
      amrsCovid.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
      amrsCovid.setKenyaemrPatientUuid(kenyaemr_patient_uuid);
      amrsCovidService.save(amrsCovid);
    }
    CareOpenMRSPayload.processCovid(amrsCovidService, amrsPatientServices, amrsTranslater, KenyaEMRlocationUuid, url, auth);
  }

  public static void processHeiOutcome(String server, String username, String password, String KenyaEMRlocationUuid, AMRSHeiOutcomeService amrsHeiOutcomeService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    String samplePatientList = AMRSSamples.getPersonIdList();

    String sql = "SELECT \n" +
      " o.person_id AS patient_id,\n" +
      " e.form_id,\n" +
      " e.visit_id,\n" +
      " o.concept_id,\n" +
      " o.encounter_id,\n" +
      " o.obs_datetime,\n" +
      " e.encounter_datetime,\n" +
      " cn.name question,\n" +
      " e.location_id,\n" +
      " c.datatype_id,\n" +
      " CASE\n" +
      " WHEN o.value_datetime IS NOT NULL THEN o.value_datetime\n" +
      " WHEN o.value_coded IS NOT NULL THEN o.value_coded\n" +
      " WHEN o.value_numeric IS NOT NULL THEN o.value_numeric\n" +
      " WHEN o.value_text IS NOT NULL THEN o.value_text\n" +
      " END AS value\n" +
      "FROM\n" +
      " amrs.obs o\n" +
      " INNER JOIN\n" +
      " amrs.concept c ON o.concept_id = c.concept_id\n" +
      " INNER JOIN\n" +
      " amrs.concept_name cn ON o.concept_id = cn.concept_id\n" +
      " AND cn.locale_preferred = 1\n" +
      " AND o.concept_id IN (8586)\n" +
      " INNER JOIN\n" +
      " amrs.encounter e ON o.encounter_id = e.encounter_id\n" +
      "AND o.person_id IN(1171851)\n" +
      " AND e.voided = 0\n" +
      " AND o.voided = 0\n" +
      " AND e.encounter_type IN (115)\n" +
      "ORDER BY patient_id ASC";

//    System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String encounterDatetime = rs.getString("encounter_datetime");
      String value = rs.getString("value");
      String question = rs.getString("question");
      String dataType = rs.getString("datatype_id");
      String visitId = rs.getString("visit_id");
      String obsDateTime = rs.getString("obs_datetime");

// Check if record already exists
      List<AMRSHeiOutcome> existingRecords = amrsHeiOutcomeService.findByEncounterConceptAndPatient(encounterId, conceptId, patientId);
      if (!existingRecords.isEmpty()) {
        System.out.println("Duplicate record found for encounterId: " + encounterId + ", conceptId: " + conceptId + ", patientId: " + patientId);
        continue; // Skip saving to avoid duplication
      }

      String kenyaemr_value = dataType.equals("2") ? amrsTranslater.translater(value) : value;

      AMRSHeiOutcome amrsHeiOutcome = new AMRSHeiOutcome();
      amrsHeiOutcome.setPatientId(patientId);
      amrsHeiOutcome.setFormId(formId);
      amrsHeiOutcome.setConceptId(conceptId);
      amrsHeiOutcome.setEncounterId(encounterId);
      amrsHeiOutcome.setValue(value);
      amrsHeiOutcome.setConceptDataTypeId(dataType);
      amrsHeiOutcome.setVisitId(visitId);
      amrsHeiOutcome.setQuestion(question);
      amrsHeiOutcome.setObsDateTime(obsDateTime);
      amrsHeiOutcome.setKenyaemrEncounterTypeUuid("01894f88-dc73-42d4-97a3-0929118403fb");
      amrsHeiOutcome.setKenyaemrFormUuid("d823f1ef-0973-44ee-b113-7090dc23257b");
      amrsHeiOutcome.setKenyaEmrValue(kenyaemr_value);
      amrsHeiOutcome.setKenyaEmrEncounterDateTime(encounterDatetime);
      String kenyaemr_patient_uuid = amrsTranslater.KenyaemrPatientUuid(patientId);
      String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
      amrsHeiOutcome.setKenyaEmrConceptUuid("159427AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
      amrsHeiOutcome.setKenyaemrPatientUuid(kenyaemr_patient_uuid);
      amrsHeiOutcomeService.save(amrsHeiOutcome);
    }
    HeiOutcomePayload.processHeiOutcome(amrsHeiOutcomeService, amrsPatientServices, amrsTranslater, url, auth);
  }

  public static void processAlcohol(String server, String username, String password, String KenyaEMRlocationUuid, AMRSAlcoholService amrsAlcoholService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    String samplePatientList = AMRSSamples.getPersonIdList();

    String sql = "select\n" +
      "        o.person_id,\n" +
      "        e.form_id,\n" +
      "        e.visit_id,\n" +
      "        cn.name as question,\n" +
      "        e.encounter_id,\n" +
      "        e.encounter_datetime,\n" +
      "        e.encounter_type,\n" +
      "        o.concept_id,\n" +
      "        o.obs_datetime,\n" +
      "        COALESCE(o.value_coded, o.value_datetime, o.value_numeric, o.value_text) as value,\n" +
      "        cd.name as value_type,\n" +
      "        c.datatype_id,\n" +
      "        et.name encounterName,\n" +
      "        \"Alcohol and Drug Abuse Screening(CAGE-AID/CRAFFT)\" as Category\n" +
      "from\n" +
      "        amrs.obs o\n" +
      "inner join amrs.encounter e on\n" +
      "        (o.encounter_id = e.encounter_id)\n" +
      "inner join amrs.encounter_type et on\n" +
      "        et.encounter_type_id = e.encounter_type\n" +
      "inner join amrs.concept c on\n" +
      "        c.concept_id = o.concept_id\n" +
      "inner join amrs.concept_datatype cd on\n" +
      "        cd.concept_datatype_id = c.datatype_id\n" +
      "INNER JOIN amrs.concept_name cn ON\n" +
      "        o.concept_id = cn.concept_id\n" +
      "where\n" +
      "        o.concept_id in (11634, 9100, 11831)\n" +
      "        and e.voided = 0\n" +
      "        and cd.name <> 'N/A'\n" +
      "        and o.person_id in (" + samplePatientList + ")\n" +
      "order by\n" +
      "        o.person_id,\n" +
      "        e.encounter_id desc;";

    // System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("person_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String encounterDatetime = rs.getString("encounter_datetime");
      String value = rs.getString("value");
      String question = rs.getString("question");
      String dataType = rs.getString("datatype_id");
      String visitId = rs.getString("visit_id");
      String obsDateTime = rs.getString("obs_datetime");

      // Check if record already exists
      List<AMRSAlcohol> existingRecords = amrsAlcoholService.findByEncounterConceptAndPatient(encounterId, conceptId, patientId);
      if (!existingRecords.isEmpty()) {
        System.out.println("Duplicate record found for encounterId: " + encounterId + ", conceptId: " + conceptId + ", patientId: " + patientId);
        continue; // Skip saving to avoid duplication
      }
      String kenyaemr_value = dataType.equals("2") ? amrsTranslater.translater(value) : value;

      AMRSAlcohol amrsModel = new AMRSAlcohol();
      amrsModel.setPatientId(patientId);
      amrsModel.setFormId(formId);
      amrsModel.setConceptId(conceptId);
      amrsModel.setEncounterId(encounterId);
      amrsModel.setValue(value);
      amrsModel.setConceptDataTypeId(dataType);
      amrsModel.setVisitId(visitId);
      amrsModel.setQuestion(question);
      amrsModel.setObsDateTime(obsDateTime);
      amrsModel.setKenyaemrEncounterTypeUuid("4224f8bf-11b2-4e47-a958-1dbdfd7fa41d");
      amrsModel.setKenyaemrFormUuid("7b1ec2d5-a4ad-4ffc-a0d3-ff1ea68e293c");
      amrsModel.setKenyaEmrValue(kenyaemr_value);
      amrsModel.setKenyaEmrEncounterDateTime(encounterDatetime);
      String kenyaemr_patient_uuid = amrsTranslater.KenyaemrPatientUuid(patientId);
      String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
      amrsModel.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
      amrsModel.setKenyaemrPatientUuid(kenyaemr_patient_uuid);
      amrsAlcoholService.save(amrsModel);
    }
    CareOpenMRSPayload.processAlcohol(amrsAlcoholService, amrsPatientServices, amrsTranslater, KenyaEMRlocationUuid, url, auth);
  }
  public static void mchEnrollment(String server, String username, String password, String locations, String parentUUID, AMRSMchEnrollmentService amrsMchEnrollmentService, AMRSTranslater amrsTranslater, AMRSPatientServices amrsPatientServices, String url, String auth) throws IOException, JSONException, SQLException {
    String sql = "SELECT o.person_id as patient_id, \n" +
            "                                    e.form_id, \n" +
            "                                    o.concept_id, \n" +
            "                                    o.encounter_id, \n" +
            "                                    cn.name question, \n" +
            "                                    e.visit_id, \n" +
            "                                    e.encounter_datetime, \n" +
            "                                    o.obs_datetime, \n" +
            "                                    case when o.value_datetime is not null then o.value_datetime \n" +
            "                                                    when o.value_coded is not null then o.value_coded \n" +
            "                                                    when o.value_numeric is not null then o.value_numeric \n" +
            "                                                    when o.value_text is not null then o.value_text end  \n" +
            "                                                    as value \n" +
            "                                                    FROM amrs.obs o \n" +
            "                                                    INNER JOIN amrs.concept c ON o.concept_id=c.concept_id \n" +
            "                                                   INNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id \n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\tand cn.locale_preferred=1 \n" +
            "                                                     AND o.person_id IN(1151769) \n" +
            "                                                     -- AND c.concept_id in (1532,782,1086,1432,1435) \n" +
            "                                                   INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0  \n" +
            "                                                  and e.encounter_type in(238) \n" +
            "                                                  ORDER BY patient_id ASC,encounter_id DESC;";

    System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String question = rs.getString("question");
      String value = rs.getString(("value"));
      String visitId = rs.getString("visit_id");
      String obsDatetime = rs.getString("obs_datetime");
      String encounterDatetime = rs.getString("encounter_datetime");

      List<AMRSMchEnrollment> amrsMchEnrollmentList = amrsMchEnrollmentService.findByPatientIdAndEncounterIdAndConceptId(patientId,encounterId,conceptId);

      if(amrsMchEnrollmentList.isEmpty()) {
        AMRSMchEnrollment amrsMchEnrollment = new AMRSMchEnrollment();
        amrsMchEnrollment.setPatientId(patientId);
        amrsMchEnrollment.setFormId(formId);
        amrsMchEnrollment.setConceptId(conceptId);
        amrsMchEnrollment.setEncounterId(encounterId);
        amrsMchEnrollment.setQuestion(question);
        amrsMchEnrollment.setValue(value);
        amrsMchEnrollment.setVisitId(visitId);
        amrsMchEnrollment.setObsDateTime(obsDatetime);
        amrsMchEnrollment.setKenyaEmrEncounterDatetime(encounterDatetime);
        String kenyaEmrPatientUuid = amrsTranslater.KenyaemrPatientUuid(patientId);
        amrsMchEnrollment.setKenyaEmrPatientUuid(kenyaEmrPatientUuid);
        String kenyaEmrEncounterUuid = "3ee036d8-7c13-4393-b5d6-036f2fe45126";
        amrsMchEnrollment.setKenyaEmrEncounterUuid(kenyaEmrEncounterUuid);
        String kenyaEmrFormUuid = "90a18f0c-17cd-4eec-8204-5af52e8d77cf";
        amrsMchEnrollment.setKenyaEmrFormUuid(kenyaEmrFormUuid);
        String kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
        amrsMchEnrollment.setKenyaEmrVisitUuid(kenyaemrVisitUuid);
        String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
        amrsMchEnrollment.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
        String kenyaEmrValueUuid = "";
        kenyaEmrValueUuid = amrsTranslater.translater(value);
        amrsMchEnrollment.setKenyaEmrValue(kenyaEmrValueUuid);

        if(!Objects.equals(kenyaEmrValueUuid, "") && !Objects.equals(kenyaEmrConceptUuid, "")) {

          amrsMchEnrollmentService.save(amrsMchEnrollment);

        }


      }

      // Call method to create and insert the payload
    }

    CareOpenMRSPayload.mchEnrollment(amrsMchEnrollmentService, amrsPatientServices, amrsTranslater, url, auth);

  }

  public static void mchAntenatal(String server, String username, String password, String locations, String parentUUID, AMRSMchAntenatalService amrsMchAntenatalService, AMRSTranslater amrsTranslater, AMRSPatientServices amrsPatientServices, String url, String auth) throws IOException, JSONException, SQLException {
    String sql = "SELECT o.person_id as patient_id, \n" +
            "                                    e.form_id, \n" +
            "                                    o.concept_id, \n" +
            "                                    o.encounter_id, \n" +
            "                                    cn.name question, \n" +
            "                                    e.visit_id, \n" +
            "                                    e.encounter_datetime, \n" +
            "                                    o.obs_datetime, \n" +
            "                                    case when o.value_datetime is not null then o.value_datetime \n" +
            "                                                    when o.value_coded is not null then o.value_coded \n" +
            "                                                    when o.value_numeric is not null then o.value_numeric \n" +
            "                                                    when o.value_text is not null then o.value_text end  \n" +
            "                                                    as value \n" +
            "                                                    FROM amrs.obs o \n" +
            "                                                    INNER JOIN amrs.concept c ON o.concept_id=c.concept_id \n" +
            "                                                   INNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id \n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\tand cn.locale_preferred=1 \n" +
            "                                                     AND o.person_id IN(1151769) \n" +
            "                                                     -- AND c.concept_id in (1532,782,1086,1432,1435) \n" +
            "                                                   INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0  \n" +
            "                                                  and e.encounter_type in(264) \n" +
            "                                                  ORDER BY patient_id ASC,encounter_id DESC;";

    System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String question = rs.getString("question");
      String value = rs.getString(("value"));
      String visitId = rs.getString("visit_id");
      String obsDatetime = rs.getString("obs_datetime");
      String encounterDatetime = rs.getString("encounter_datetime");

      List<AMRSMchAntenatal> amrsMchAntenatalList = amrsMchAntenatalService.findByPatientIdAndEncounterIdAndConceptId(patientId,encounterId,conceptId);

      if(amrsMchAntenatalList.isEmpty()) {
        AMRSMchAntenatal amrsMchAntenatal = new AMRSMchAntenatal();
        amrsMchAntenatal.setPatientId(patientId);
        amrsMchAntenatal.setFormId(formId);
        amrsMchAntenatal.setConceptId(conceptId);
        amrsMchAntenatal.setEncounterId(encounterId);
        amrsMchAntenatal.setQuestion(question);
        amrsMchAntenatal.setValue(value);
        amrsMchAntenatal.setVisitId(visitId);
        amrsMchAntenatal.setObsDateTime(obsDatetime);
        amrsMchAntenatal.setKenyaEmrEncounterDatetime(encounterDatetime);
        String kenyaEmrPatientUuid = amrsTranslater.KenyaemrPatientUuid(patientId);
        amrsMchAntenatal.setKenyaEmrPatientUuid(kenyaEmrPatientUuid);
        String kenyaEmrEncounterUuid = "415f5136-ca4a-49a8-8db3-f994187c3af6";
        amrsMchAntenatal.setKenyaEmrEncounterUuid(kenyaEmrEncounterUuid);
        String kenyaEmrFormUuid = "8553d869-bdc8-4287-8505-910c7c998aff";
        amrsMchAntenatal.setKenyaEmrFormUuid(kenyaEmrFormUuid);
        String kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
        amrsMchAntenatal.setKenyaEmrVisitUuid(kenyaemrVisitUuid);
        String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
        amrsMchAntenatal.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
        String kenyaEmrValueUuid = "";
        kenyaEmrValueUuid = amrsTranslater.translater(value);
        amrsMchAntenatal.setKenyaEmrValue(kenyaEmrValueUuid);

        if(!Objects.equals(kenyaEmrValueUuid, "") && !Objects.equals(kenyaEmrConceptUuid, "")) {

          amrsMchAntenatalService.save(amrsMchAntenatal);

        }


      }

      // Call method to create and insert the payload
    }

    CareOpenMRSPayload.mchAntenatal(amrsMchAntenatalService, amrsPatientServices, amrsTranslater, url, auth);

  }

  public static void mchDischargeAndReferral(String server, String username, String password, String locations, String parentUUID, AMRSMchDischargeAndReferralService amrsMchDischargeAndReferralService, AMRSTranslater amrsTranslater, AMRSPatientServices amrsPatientServices, String url, String auth) throws IOException, JSONException, SQLException {
    String sql = "SELECT o.person_id as patient_id, \n" +
            "                                    e.form_id, \n" +
            "                                    o.concept_id, \n" +
            "                                    o.encounter_id, \n" +
            "                                    cn.name question, \n" +
            "                                    e.visit_id, \n" +
            "                                    e.encounter_datetime, \n" +
            "                                    o.obs_datetime, \n" +
            "                                    case when o.value_datetime is not null then o.value_datetime \n" +
            "                                                    when o.value_coded is not null then o.value_coded \n" +
            "                                                    when o.value_numeric is not null then o.value_numeric \n" +
            "                                                    when o.value_text is not null then o.value_text end  \n" +
            "                                                    as value \n" +
            "                                                    FROM amrs.obs o \n" +
            "                                                    INNER JOIN amrs.concept c ON o.concept_id=c.concept_id \n" +
            "                                                   INNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id \n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\tand cn.locale_preferred=1 \n" +
            "                                                     AND o.person_id IN(1151769) \n" +
            "                                                     -- AND c.concept_id in (1532,782,1086,1432,1435) \n" +
            "                                                   INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0  \n" +
            "                                                  and e.encounter_type in(238) \n" +
            "                                                  ORDER BY patient_id ASC,encounter_id DESC;";

    System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String question = rs.getString("question");
      String value = rs.getString(("value"));
      String visitId = rs.getString("visit_id");
      String obsDatetime = rs.getString("obs_datetime");
      String encounterDatetime = rs.getString("encounter_datetime");

      List<AMRSMchDischargeAndReferral> amrsMchDischargeAndReferralList = amrsMchDischargeAndReferralService.findByPatientIdAndEncounterIdAndConceptId(patientId,encounterId,conceptId);

      if(amrsMchDischargeAndReferralList.isEmpty()) {
        AMRSMchDischargeAndReferral amrsMchDischargeAndReferral = new AMRSMchDischargeAndReferral();
        amrsMchDischargeAndReferral.setPatientId(patientId);
        amrsMchDischargeAndReferral.setFormId(formId);
        amrsMchDischargeAndReferral.setConceptId(conceptId);
        amrsMchDischargeAndReferral.setEncounterId(encounterId);
        amrsMchDischargeAndReferral.setQuestion(question);
        amrsMchDischargeAndReferral.setValue(value);
        amrsMchDischargeAndReferral.setVisitId(visitId);
        amrsMchDischargeAndReferral.setObsDateTime(obsDatetime);
        amrsMchDischargeAndReferral.setKenyaEmrEncounterDatetime(encounterDatetime);
        String kenyaEmrPatientUuid = amrsTranslater.KenyaemrPatientUuid(patientId);
        amrsMchDischargeAndReferral.setKenyaEmrPatientUuid(kenyaEmrPatientUuid);
        String kenyaEmrEncounterUuid = "c6d09e05-1f25-4164-8860-9f32c5a02df0";
        amrsMchDischargeAndReferral.setKenyaEmrEncounterUuid(kenyaEmrEncounterUuid);
        String kenyaEmrFormUuid = "af273344-a5f9-11e8-98d0-529269fb1459";
        amrsMchDischargeAndReferral.setKenyaEmrFormUuid(kenyaEmrFormUuid);
        String kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
        amrsMchDischargeAndReferral.setKenyaEmrVisitUuid(kenyaemrVisitUuid);
        String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
        amrsMchDischargeAndReferral.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
        String kenyaEmrValueUuid = "";
        kenyaEmrValueUuid = amrsTranslater.translater(value);
        amrsMchDischargeAndReferral.setKenyaEmrValue(kenyaEmrValueUuid);

        if(!Objects.equals(kenyaEmrValueUuid, "") && !Objects.equals(kenyaEmrConceptUuid, "")) {

          amrsMchDischargeAndReferralService.save(amrsMchDischargeAndReferral);

        }


      }

      // Call method to create and insert the payload
    }

//        CareOpenMRSPayload.mchDischargeAndReferral(amrsMchDischargeAndReferralService, amrsPatientServices, amrsTranslater, url, auth);

  }

  public static void mchPostnatal(String server, String username, String password, String locations, String parentUUID, AMRSMchPostnatalService amrsMchPostnatalService, AMRSTranslater amrsTranslater, AMRSPatientServices amrsPatientServices, String url, String auth) throws IOException, JSONException, SQLException {
    String sql = "SELECT o.person_id as patient_id, \n" +
            "                                    e.form_id, \n" +
            "                                    o.concept_id, \n" +
            "                                    o.encounter_id, \n" +
            "                                    cn.name question, \n" +
            "                                    e.visit_id, \n" +
            "                                    e.encounter_datetime, \n" +
            "                                    o.obs_datetime, \n" +
            "                                    case when o.value_datetime is not null then o.value_datetime \n" +
            "                                                    when o.value_coded is not null then o.value_coded \n" +
            "                                                    when o.value_numeric is not null then o.value_numeric \n" +
            "                                                    when o.value_text is not null then o.value_text end  \n" +
            "                                                    as value \n" +
            "                                                    FROM amrs.obs o \n" +
            "                                                    INNER JOIN amrs.concept c ON o.concept_id=c.concept_id \n" +
            "                                                   INNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id \n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\tand cn.locale_preferred=1 \n" +
            "                                                     AND o.person_id IN(1151769) \n" +
            "                                                     -- AND c.concept_id in (1532,782,1086,1432,1435) \n" +
            "                                                   INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0  \n" +
            "                                                  and e.encounter_type in(238) \n" +
            "                                                  ORDER BY patient_id ASC,encounter_id DESC;";

    System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String question = rs.getString("question");
      String value = rs.getString(("value"));
      String visitId = rs.getString("visit_id");
      String obsDatetime = rs.getString("obs_datetime");
      String encounterDatetime = rs.getString("encounter_datetime");

      List<AMRSMchPostnatal> amrsMchPostnatalList = amrsMchPostnatalService.findByPatientIdAndEncounterIdAndConceptId(patientId,encounterId,conceptId);

      if(amrsMchPostnatalList.isEmpty()) {
        AMRSMchPostnatal amrsMchPostnatal = new AMRSMchPostnatal();
        amrsMchPostnatal.setPatientId(patientId);
        amrsMchPostnatal.setFormId(formId);
        amrsMchPostnatal.setConceptId(conceptId);
        amrsMchPostnatal.setEncounterId(encounterId);
        amrsMchPostnatal.setQuestion(question);
        amrsMchPostnatal.setValue(value);
        amrsMchPostnatal.setVisitId(visitId);
        amrsMchPostnatal.setObsDateTime(obsDatetime);
        amrsMchPostnatal.setKenyaEmrEncounterDatetime(encounterDatetime);
        String kenyaEmrPatientUuid = amrsTranslater.KenyaemrPatientUuid(patientId);
        amrsMchPostnatal.setKenyaEmrPatientUuid(kenyaEmrPatientUuid);
        String kenyaEmrEncounterUuid = "c6d09e05-1f25-4164-8860-9f32c5a02df0";
        amrsMchPostnatal.setKenyaEmrEncounterUuid(kenyaEmrEncounterUuid);
        String kenyaEmrFormUuid = "72aa78e0-ee4b-47c3-9073-26f3b9ecc4a7";
        amrsMchPostnatal.setKenyaEmrFormUuid(kenyaEmrFormUuid);
        String kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
        amrsMchPostnatal.setKenyaEmrVisitUuid(kenyaemrVisitUuid);
        String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
        amrsMchPostnatal.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
        String kenyaEmrValueUuid = "";
        kenyaEmrValueUuid = amrsTranslater.translater(value);
        amrsMchPostnatal.setKenyaEmrValue(kenyaEmrValueUuid);

        if(!Objects.equals(kenyaEmrValueUuid, "") && !Objects.equals(kenyaEmrConceptUuid, "")) {

          amrsMchPostnatalService.save(amrsMchPostnatal);

        }
      }

      // Call method to create and insert the payload
    }

//        CareOpenMRSPayload.mchPostnatal(amrsMchPostnatalService, amrsPatientServices, amrsTranslater, url, auth);
  }

  public static void mchDelivery(String server, String username, String password, String locations, String parentUUID, AMRSMchDeliveryService amrsMchDeliveryService, AMRSTranslater amrsTranslater, AMRSPatientServices amrsPatientServices, String url, String auth) throws IOException, JSONException, SQLException {
    String sql = "SELECT o.person_id as patient_id, \n" +
            "                                    e.form_id, \n" +
            "                                    o.concept_id, \n" +
            "                                    o.encounter_id, \n" +
            "                                    cn.name question, \n" +
            "                                    e.visit_id, \n" +
            "                                    e.encounter_datetime, \n" +
            "                                    o.obs_datetime, \n" +
            "                                    case when o.value_datetime is not null then o.value_datetime \n" +
            "                                                    when o.value_coded is not null then o.value_coded \n" +
            "                                                    when o.value_numeric is not null then o.value_numeric \n" +
            "                                                    when o.value_text is not null then o.value_text end  \n" +
            "                                                    as value \n" +
            "                                                    FROM amrs.obs o \n" +
            "                                                    INNER JOIN amrs.concept c ON o.concept_id=c.concept_id \n" +
            "                                                   INNER JOIN amrs.concept_name cn ON o.concept_id = cn.concept_id \n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\tand cn.locale_preferred=1 \n" +
            "                                                     AND o.person_id IN(1151769) \n" +
            "                                                     -- AND c.concept_id in (1532,782,1086,1432,1435) \n" +
            "                                                   INNER JOIN amrs.encounter e ON o.encounter_id=e.encounter_id and e.voided=0 and o.voided=0  \n" +
            "                                                  and e.encounter_type in(238) \n" +
            "                                                  ORDER BY patient_id ASC,encounter_id DESC;";

    System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String question = rs.getString("question");
      String value = rs.getString(("value"));
      String visitId = rs.getString("visit_id");
      String obsDatetime = rs.getString("obs_datetime");
      String encounterDatetime = rs.getString("encounter_datetime");

      List<AMRSMchDelivery> amrsMchDeliveryList = amrsMchDeliveryService.findByPatientIdAndEncounterIdAndConceptId(patientId,encounterId,conceptId);

      if(amrsMchDeliveryList.isEmpty()) {
        AMRSMchDelivery amrsMchDelivery = new AMRSMchDelivery();
        amrsMchDelivery.setPatientId(patientId);
        amrsMchDelivery.setFormId(formId);
        amrsMchDelivery.setConceptId(conceptId);
        amrsMchDelivery.setEncounterId(encounterId);
        amrsMchDelivery.setQuestion(question);
        amrsMchDelivery.setValue(value);
        amrsMchDelivery.setVisitId(visitId);
        amrsMchDelivery.setObsDateTime(obsDatetime);
        amrsMchDelivery.setKenyaEmrEncounterDatetime(encounterDatetime);
        String kenyaEmrPatientUuid = amrsTranslater.KenyaemrPatientUuid(patientId);
        amrsMchDelivery.setKenyaEmrPatientUuid(kenyaEmrPatientUuid);
        String kenyaEmrEncounterUuid = "c6d09e05-1f25-4164-8860-9f32c5a02df0";
        amrsMchDelivery.setKenyaEmrEncounterUuid(kenyaEmrEncounterUuid);
        String kenyaEmrFormUuid = "496c7cc3-0eea-4e84-a04c-2292949e2f7f";
        amrsMchDelivery.setKenyaEmrFormUuid(kenyaEmrFormUuid);
        String kenyaemrVisitUuid = amrsTranslater.kenyaemrVisitUuid(visitId);
        amrsMchDelivery.setKenyaEmrVisitUuid(kenyaemrVisitUuid);
        String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
        amrsMchDelivery.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
        String kenyaEmrValueUuid = "";
        kenyaEmrValueUuid = amrsTranslater.translater(value);
        amrsMchDelivery.setKenyaEmrValue(kenyaEmrValueUuid);

        if(!Objects.equals(kenyaEmrValueUuid, "") && !Objects.equals(kenyaEmrConceptUuid, "")) {

          amrsMchDeliveryService.save(amrsMchDelivery);
        }
      }

      // Call method to create and insert the payload
    }

//        CareOpenMRSPayload.mchDelivery(amrsMchDeliveryService, amrsPatientServices, amrsTranslater, url, auth);

  }

  public static void processGBVScreening(String server, String username, String password,  String KenyaEMRlocationUuid, AMRSGbvScreeningService amrsGbvScreeningService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    String samplePatientList = AMRSSamples.getPersonIdList();


    String sql = "SELECT \n" +
            "    o.person_id AS patient_id,\n" +
            "    e.form_id,\n" +
            "    e.visit_id,\n" +
            "    o.concept_id,\n" +
            "    o.encounter_id,\n" +
            "    o.obs_datetime,\n" +
            "    e.encounter_datetime,\n" +
            "    cn.name question,\n" +
            "    c.datatype_id,\n" +
            "    CASE\n" +
            "        WHEN o.value_datetime IS NOT NULL THEN o.value_datetime\n" +
            "        WHEN o.value_coded IS NOT NULL THEN o.value_coded\n" +
            "        WHEN o.value_numeric IS NOT NULL THEN o.value_numeric\n" +
            "        WHEN o.value_text IS NOT NULL THEN o.value_text\n" +
            "    END AS value\n" +
            "FROM\n" +
            "    amrs.obs o\n" +
            "        INNER JOIN\n" +
            "    amrs.concept c ON o.concept_id = c.concept_id\n" +
            "        INNER JOIN\n" +
            "    amrs.concept_name cn ON o.concept_id = cn.concept_id\n" +
            "        AND cn.locale_preferred = 1\n" +
            "        AND o.person_id IN (7315, 59807, 183479, 1072350, 827082 )\n" +
            "        AND c.concept_id IN (11866 , 11865, 9303)\n" +
            "        INNER JOIN\n" +
            "    amrs.encounter e ON o.encounter_id = e.encounter_id\n" +
            "        AND e.voided = 0\n" +
            "        AND o.voided = 0\n" +
            "        AND e.encounter_type IN (2)\n" +
            "ORDER BY patient_id ASC , encounter_id DESC";


//    System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String encounterDatetime = rs.getString("encounter_datetime");
      String value = rs.getString("value");
      String question = rs.getString("question");
      String dataType = rs.getString("datatype_id");
      String visitId = rs.getString("visit_id");
      String obsDateTime = rs.getString("obs_datetime");

      // Check if record already exists
      List<AMRSGBVScreening> existingRecords = amrsGbvScreeningService.findByEncounterConceptAndPatient(encounterId, conceptId, patientId);
      if (!existingRecords.isEmpty()) {
        System.out.println("Duplicate record found for encounterId: " + encounterId + ", conceptId: " + conceptId + ", patientId: " + patientId);
        continue; // Skip saving to avoid duplication
      }


      String kenyaemr_value = dataType.equals("2") ? amrsTranslater.translater(value) : value;

      AMRSGBVScreening amrsgbvScreening = new AMRSGBVScreening();
      amrsgbvScreening.setPatientId(patientId);
      amrsgbvScreening.setFormId(formId);
      amrsgbvScreening.setConceptId(conceptId);
      amrsgbvScreening.setEncounterId(encounterId);
      amrsgbvScreening.setValue(value);
      amrsgbvScreening.setConceptDataTypeId(dataType);
      amrsgbvScreening.setVisitId(visitId);
      amrsgbvScreening.setQuestion(question);
      amrsgbvScreening.setObsDateTime(obsDateTime);
      amrsgbvScreening.setKenyaemrEncounterTypeUuid("f091b067-bea5-4657-8445-cfec05dc46a2");
      amrsgbvScreening.setKenyaemrFormUuid("03767614-1384-4ce3-aea9-27e2f4e67d01");
      amrsgbvScreening.setKenyaEmrValue(kenyaemr_value);
      amrsgbvScreening.setKenyaEmrEncounterDateTime(encounterDatetime);
      String kenyaemr_patient_uuid = amrsTranslater.KenyaemrPatientUuid(patientId);
      String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
      amrsgbvScreening.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
      amrsgbvScreening.setKenyaemrPatientUuid(kenyaemr_patient_uuid);
      amrsGbvScreeningService.save(amrsgbvScreening);
    }
    GBVScreeningPayload.processGBVScreening(amrsGbvScreeningService, amrsPatientServices, amrsTranslater, url, auth);
  }

  public static void processEac(String server, String username, String password, String KenyaEMRlocationUuid, AMRSEacService amrsEacService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    String samplePatientList = AMRSSamples.getPersonIdList();

    String sql = "select\n" +
      "        o.person_id,\n" +
      "        e.form_id,\n" +
      "        e.visit_id,\n" +
      "        cn.name as question,\n" +
      "        e.encounter_id,\n" +
      "        e.encounter_datetime,\n" +
      "        e.encounter_type,\n" +
      "        o.concept_id,\n" +
      "        o.obs_datetime,\n" +
      "        COALESCE(o.value_coded, o.value_datetime, o.value_numeric, o.value_text) as value,\n" +
      "        cd.name as value_type,\n" +
      "        c.datatype_id,\n" +
      "        et.name encounterName,\n" +
      "        \"Enhanced Adherence Screening\" as Category\n" +
      "from\n" +
      "        amrs.obs o\n" +
      "inner join amrs.encounter e on\n" +
      "        (o.encounter_id = e.encounter_id)\n" +
      "inner join amrs.encounter_type et on\n" +
      "        et.encounter_type_id = e.encounter_type\n" +
      "inner join amrs.concept c on\n" +
      "        c.concept_id = o.concept_id\n" +
      "inner join amrs.concept_datatype cd on\n" +
      "        cd.concept_datatype_id = c.datatype_id\n" +
      "INNER JOIN amrs.concept_name cn ON\n" +
      "        o.concept_id = cn.concept_id\n" +
      "where\n" +
      "        o.concept_id in (1639, 10999, 11000, 6098, 1587, 1743, 1779, 12382, 1658, 2389, 11001, 11002, 11724, 1915, 11018, 11019, 10518, 11003, 11004, 11005, 11006, 11007, 11008, 11009, 11010, 11011, 11012, 11013, 11014, 11015, 11016, 1272, 9320, 11020, 10085, 10530)\n" +
      "        and e.voided = 0\n" +
      "        and cd.name <> 'N/A'\n" +
      "        and o.person_id in (" + samplePatientList + ")\n" +
      "order by\n" +
      "        o.person_id,\n" +
      "        e.encounter_id desc;";

    // System.out.println("locations " + locations + " parentUUID " + parentUUID);
    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
      ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("person_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String encounterDatetime = rs.getString("encounter_datetime");
      String value = rs.getString("value");
      String question = rs.getString("question");
      String dataType = rs.getString("datatype_id");
      String visitId = rs.getString("visit_id");
      String obsDateTime = rs.getString("obs_datetime");

      // Check if record already exists
      List<AMRSEac> existingRecords = amrsEacService.findByEncounterConceptAndPatient(encounterId, conceptId, patientId);
      if (!existingRecords.isEmpty()) {
        System.out.println("Duplicate record found for encounterId: " + encounterId + ", conceptId: " + conceptId + ", patientId: " + patientId);
        continue; // Skip saving to avoid duplication
      }
      String kenyaemr_value = dataType.equals("2") ? amrsTranslater.translater(value) : value;

      AMRSEac amrsModel = new AMRSEac();
      amrsModel.setPatientId(patientId);
      amrsModel.setFormId(formId);
      amrsModel.setConceptId(conceptId);
      amrsModel.setEncounterId(encounterId);
      amrsModel.setValue(value);
      amrsModel.setConceptDataTypeId(dataType);
      amrsModel.setVisitId(visitId);
      amrsModel.setQuestion(question);
      amrsModel.setObsDateTime(obsDateTime);
      amrsModel.setKenyaemrEncounterTypeUuid("54df6991-13de-4efc-a1a9-2d5ac1b72ff8");
      amrsModel.setKenyaemrFormUuid("c483f10f-d9ee-4b0d-9b8c-c24c1ec24701");
      amrsModel.setKenyaEmrValue(kenyaemr_value);
      amrsModel.setKenyaEmrEncounterDateTime(encounterDatetime);
      String kenyaemr_patient_uuid = amrsTranslater.KenyaemrPatientUuid(patientId);
      String kenyaEmrConceptUuid = amrsTranslater.translater(conceptId);
      amrsModel.setKenyaEmrConceptUuid(kenyaEmrConceptUuid);
      amrsModel.setKenyaemrPatientUuid(kenyaemr_patient_uuid);
      amrsEacService.save(amrsModel);
    }
    CareOpenMRSPayload.processEac(amrsEacService, amrsPatientServices, amrsTranslater, KenyaEMRlocationUuid, url, auth);
  }

  public static void processDepressionScreening(String server, String username, String password,  String KenyaEMRlocationUuid, AMRSDepressionScreeningService amrsDepressionScreeningService, AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

    String samplePatientList = AMRSSamples.getPersonIdList();


    String sql = "SELECT \n" +
            "    o.person_id AS patient_id,\n" +
            "    e.form_id,\n" +
            "    e.visit_id,\n" +
            "    o.concept_id,\n" +
            "    o.encounter_id,\n" +
            "    o.obs_datetime,\n" +
            "    e.encounter_datetime,\n" +
            "    cn.name question,\n" +
            "    c.datatype_id,\n" +
            "    CASE\n" +
            "        WHEN o.value_datetime IS NOT NULL THEN o.value_datetime\n" +
            "        WHEN o.value_coded IS NOT NULL THEN o.value_coded\n" +
            "        WHEN o.value_numeric IS NOT NULL THEN o.value_numeric\n" +
            "        WHEN o.value_text IS NOT NULL THEN o.value_text\n" +
            "    END AS value,\n" +
            "    CASE\n" +
            "        WHEN o.concept_id = 7806 AND o.value_numeric = 0 THEN \"163733AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"\n" +
            "        WHEN o.concept_id = 7806 AND o.value_numeric = 1 THEN \"163734AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"\n" +
            "        WHEN o.concept_id = 7806 AND o.value_numeric = 2 THEN \"163735AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"\n" +
            "        WHEN o.concept_id = 7806 AND o.value_numeric = 3 THEN \"163736AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"\n" +
            "        -- feeling down, depressed \n" +
            "\t\tWHEN o.concept_id = 7807 AND o.value_numeric = 0 THEN \"163733AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"\n" +
            "        WHEN o.concept_id = 7807 AND o.value_numeric = 1 THEN \"163734AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"\n" +
            "        WHEN o.concept_id = 7807 AND o.value_numeric = 2 THEN \"163735AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"\n" +
            "        WHEN o.concept_id = 7807 AND o.value_numeric = 3 THEN \"163736AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"\n" +
            "        \n" +
            "        -- depression assessment score translation based on phq-9 score rating\n" +
            "\t\tWHEN o.concept_id = 7815 AND (o.value_numeric >= 0 AND o.value_numeric <= 4) THEN \"1115AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"\n" +
            "\t\tWHEN o.concept_id = 7815 AND (o.value_numeric >= 5 AND o.value_numeric <= 9) THEN \"157790AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"\n" +
            "\t\tWHEN o.concept_id = 7815 AND (o.value_numeric >= 10 AND o.value_numeric <= 14) THEN \"134011AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"\n" +
            "\t\tWHEN o.concept_id = 7815 AND (o.value_numeric >= 15 AND o.value_numeric <= 19) THEN \"134017AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"\n" +
            "\t\tWHEN o.concept_id = 7815 AND (o.value_numeric >= 20 AND o.value_numeric <= 27) THEN \"126627AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"\n" +
            "\t\tELSE NULL\n" +
            "    END AS \"kenyaemr_value\",\n" +
            "    CASE\n" +
            "        WHEN o.concept_id = 7815 THEN \"33bb96f8-e009-482a-ba68-212281780fb8\"\n" +
            "    END AS \"kenyaemr_concept_uuid\"\n" +
            "FROM\n" +
            "    amrs.obs o\n" +
            "        INNER JOIN\n" +
            "    amrs.concept c ON o.concept_id = c.concept_id\n" +
            "        INNER JOIN\n" +
            "    amrs.concept_name cn ON o.concept_id = cn.concept_id\n" +
            "        AND cn.locale_preferred = 1\n" +
            "       --  AND o.person_id IN (7315, 59807, 183479, 1072350, 827082 )\n" +
            "      AND o.person_id in (" + samplePatientList + ")\n" +
            "        AND c.concept_id IN (7815, 7806, 7807)\n" +
            "        INNER JOIN\n" +
            "    amrs.encounter e ON o.encounter_id = e.encounter_id\n" +
            "        AND e.voided = 0\n" +
            "        AND o.voided = 0\n" +
            "        AND e.encounter_type IN (275)\n" +
            "ORDER BY patient_id ASC , encounter_id DESC";


    Connection con = DriverManager.getConnection(server, username, password);
    int x = 0;
    Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
    ResultSet rs = stmt.executeQuery(sql);
    rs.last();
    x = rs.getRow();
    rs.beforeFirst();
    while (rs.next()) {

      String patientId = rs.getString("patient_id");
      String formId = rs.getString("form_id");
      String conceptId = rs.getString("concept_id");
      String encounterId = rs.getString("encounter_id");
      String encounterDatetime = rs.getString("encounter_datetime");
      String value = rs.getString("value");
      String question = rs.getString("question");
      String dataType = rs.getString("datatype_id");
      String visitId = rs.getString("visit_id");
      String obsDateTime = rs.getString("obs_datetime");
      String kenyaemrValue = rs.getString("kenyaemr_value");
      String kenyaemrConceptUuid = rs.getString("kenyaemr_concept_uuid");

      // Check if record already exists
      List<AMRSDepressionScreening> existingRecords = amrsDepressionScreeningService.findByEncounterConceptAndPatient(encounterId, conceptId, patientId);
      if (!existingRecords.isEmpty()) {
        System.out.println("Duplicate record found for encounterId: " + encounterId + ", conceptId: " + conceptId + ", patientId: " + patientId);
        continue; // Skip saving to avoid duplication
      }

      AMRSDepressionScreening amrsDepressionScreening = new AMRSDepressionScreening();
      String kenyaemr_patient_uuid = amrsTranslater.KenyaemrPatientUuid(patientId);

      amrsDepressionScreening.setPatientId(patientId);
      amrsDepressionScreening.setFormId(formId);
      amrsDepressionScreening.setConceptId(conceptId);
      amrsDepressionScreening.setEncounterId(encounterId);
      amrsDepressionScreening.setValue(value);
      amrsDepressionScreening.setConceptDataTypeId(dataType);
      amrsDepressionScreening.setVisitId(visitId);
      amrsDepressionScreening.setQuestion(question);
      amrsDepressionScreening.setObsDateTime(obsDateTime);
      amrsDepressionScreening.setKenyaemrEncounterTypeUuid("f091b067-bea5-4657-8445-cfec05dc46a2");
      amrsDepressionScreening.setKenyaemrFormUuid("03767614-1384-4ce3-aea9-27e2f4e67d01");
      amrsDepressionScreening.setKenyaEmrValue(kenyaemrValue);
      amrsDepressionScreening.setKenyaEmrEncounterDateTime(encounterDatetime);
      amrsDepressionScreening.setKenyaEmrConceptUuid(kenyaemrConceptUuid);
      amrsDepressionScreening.setKenyaemrPatientUuid(kenyaemr_patient_uuid);
      amrsDepressionScreeningService.save(amrsDepressionScreening);
    }
    DepressionScreening.processDepressionScreening(amrsDepressionScreeningService, amrsPatientServices, amrsTranslater, url, auth);
  }

}


