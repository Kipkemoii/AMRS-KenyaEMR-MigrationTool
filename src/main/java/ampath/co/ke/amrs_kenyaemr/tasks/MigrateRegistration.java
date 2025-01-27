package ampath.co.ke.amrs_kenyaemr.tasks;

import ampath.co.ke.amrs_kenyaemr.methods.AMRSLocation;
import ampath.co.ke.amrs_kenyaemr.methods.AMRSSamples;
import ampath.co.ke.amrs_kenyaemr.methods.AMRSTranslater;
import ampath.co.ke.amrs_kenyaemr.models.*;
import ampath.co.ke.amrs_kenyaemr.service.*;
import ampath.co.ke.amrs_kenyaemr.tasks.payloads.RegisterOpenMRSPayload;
import jakarta.persistence.Column;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.*;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.thymeleaf.util.StringUtils.substring;

public class MigrateRegistration {
    @Autowired
    LocationService locationService;

    public static String samplePatientList = AMRSSamples.getPersonIdList();
    //getPersonIdListKapsoya
    public static void conceptMapping(AMRSMappingService amrsMappingService) throws IOException {
        ClassPathResource resource = new ClassPathResource("all_revised_concepts.csv");
        Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord());
        // Initialize row count
        int rowCount = 0;
        // Iterate through the records and count
        for (CSVRecord records : csvParser) {
            rowCount++;
        }

        List<AMRSMappings> amrsMappingsList = amrsMappingService.getAll();
        System.out.println("AMRS Mappings Size "+ amrsMappingsList.size()+" rows "+ rowCount);

        if(amrsMappingsList.size() < rowCount){
           // CSVParser csvParserr = new CSVParser(reader, CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord());
           // System.out.println(" csv "+ csvParser);
            reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord());

            for (CSVRecord record : csvParser) {
                //System.out.println("AMRS Mappings Size "+ record);
                String amrs_concept_id = record.get("amrs_concept_id").trim();
                String kenyaemr_concept_uuid = record.get("kenyaemr_concept_uuid").trim();

                String kenyaemrConceptId = kenyaemr_concept_uuid.replaceAll("[^0-9]", "");
                String uniqueKey = amrs_concept_id + ":" + kenyaemr_concept_uuid;
                List<AMRSMappings> amrsMappings = amrsMappingService.findByAmrsConceptID(amrs_concept_id);
                if(amrsMappings.isEmpty()) {
                    // Process the record (e.g., save to database)
                    AMRSMappings anc = new AMRSMappings();
                    anc.setAmrsConceptId(amrs_concept_id);
                    anc.setKenyaemrConceptUuid(kenyaemr_concept_uuid);
                    anc.setKenyaemrConceptId(kenyaemrConceptId);
                    amrsMappingService.save(anc);
                }
            }

        }else{
            System.out.println("Mapping dictionary already migrated");
        }
        csvParser.close();
        Set<String> processedEntries = new HashSet<>();


        System.out.println("Locations");
    }


    public static void locations(String server, String username, String password,LocationService locationsService) throws SQLException, JSONException, ParseException, IOException {

        List<AMRSLocations> amrsLocationsList = locationsService.getAll();
        if(amrsLocationsList.size()<522) {
            Connection con = DriverManager.getConnection(server, username, password);
            int x = 0;
            Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);

            String sql = "SELECT uuid,location_id,name,COALESCE(parent_location, location_id) AS parent_location FROM amrs.location\n" +
                    "ORDER BY location_id,parent_location;";
            ResultSet rs = stmt.executeQuery(sql);
            rs.last();
            x = rs.getRow();
            rs.beforeFirst();
            while (rs.next()) {
                AMRSLocations locations = locationsService.findByChildUUID(rs.getString("uuid"));
                if (locations == null) {
                    AMRSLocations ae = new AMRSLocations();
                    ae.setId(Integer.valueOf(rs.getString("location_id")));
                    ae.setChildlocationId(rs.getString("location_id"));
                    ae.setChildlocationName(rs.getString("name"));
                    ae.setCuuid(rs.getString("uuid"));
                    ae.setParentlocationId(rs.getString("parent_location"));
                    //ae.setPuuid(rs.getString(5));
                    //ae.setParentlocationName(rs.getString(6));
                    ae.setStatus(1);
                    locationsService.save(ae);
                    System.out.println("test here");
                } else {
                    AMRSLocations ae = locations;
                    ae.setStatus(1);
                    locationsService.save(ae);
                }

            }
        }else{
            System.out.println("Locations already migrated");
        }

    }

    public static void users(String server, String username, String password, String locations, AMRSUserServices amrsUserServices, String url, String auth) throws SQLException, JSONException, ParseException, IOException {
        String sql = "";
        String parentUUID="";
        List<AMRSUsers> amrsVisitsList = amrsUserServices.findFirstByOrderByIdDesc();
        if (!amrsVisitsList.isEmpty()) {
            String visitId = amrsVisitsList.get(0).getUser_id();
            sql = "select \n" +
                    "                u.uuid,\n" +
                    "                u.user_id,\n" +
                    "                u.system_id,\n" +
                    "                u.username,\n" +
                    "                pn.given_name,\n" +
                    "                pn.family_name,\n" +
                    "                pn.middle_name,\n" +
                    "                p.gender,\n" +
                    "                p.birthdate,\n" +
                    "                pa.address1,\n" +
                    "     case when pa.county_district  is not null or pa.county_district  then pa.county_district else \"Missing\" end county_district,\n" +
                    "                                     case when pa.state_province  is not null then pa.state_province else \"Missing\" end state_province,\n" +
                    "                                     case when pa.address4  is not null then pa.address4 else \"Missing\" end address4,\n" +
                    "                pa.address5,\n" +
                    "                pa.address6,\n" +
                    "                p.dead,\n" +
                    "                p.birthdate_estimated \n" +
                    "                from amrs.encounter e \n" +
                    "                inner join amrs.users u on e.creator =u.user_id\n" +
                    "                inner join amrs.person p on p.person_id=u.person_id\n" +
                    "                inner join amrs.person_name pn on pn.person_id=p.person_id\n" +
                    "                inner join amrs.person_address pa on pa.person_id=p.person_id\n" +
                    "                where location_id in (" + locations + ") and u.user_id >" + visitId + "  \n" +
                    "                group by u.user_id\n" +
                    "                order by u.user_id asc";
        } else {
            sql = "select \n" +
                    "                u.uuid,\n" +
                    "                u.user_id,\n" +
                    "                u.system_id,\n" +
                    "                u.username,\n" +
                    "                pn.given_name,\n" +
                    "                pn.family_name,\n" +
                    "                pn.middle_name,\n" +
                    "                p.gender,\n" +
                    "                p.birthdate,\n" +
                    "                pa.address1,\n" +
                    "     case when pa.county_district  is not null or pa.county_district  then pa.county_district else \"Missing\" end county_district,\n" +
                    "                                     case when pa.state_province  is not null then pa.state_province else \"Missing\" end state_province,\n" +
                    "                                     case when pa.address4  is not null then pa.address4 else \"Missing\" end address4,\n" +

                    "                pa.address5,\n" +
                    "                pa.address6,\n" +
                    "                p.dead,\n" +
                    "                p.birthdate_estimated \n" +
                    "                from amrs.encounter e \n" +
                    "                inner join amrs.users u on e.creator =u.user_id\n" +
                    "                inner join amrs.person p on p.person_id=u.person_id\n" +
                    "                inner join amrs.person_name pn on pn.person_id=p.person_id\n" +
                    "                inner join amrs.person_address pa on pa.person_id=p.person_id\n" +
                    "                where location_id in (" + locations + ") \n" +
                    "                group by u.user_id\n" +
                    "                order by u.user_id asc";
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
            System.out.println("User id " + rs.getString(1));
            List<AMRSUsers> amrsUsers = amrsUserServices.getUserByLocation(rs.getString(1), parentUUID);
            if (amrsUsers.isEmpty()) {
                AMRSUsers ae = new AMRSUsers();
                ae.setUuid(rs.getString("uuid"));
                ae.setUser_id(rs.getString("user_id"));
                ae.setSystem_id(rs.getString("system_id"));
                ae.setUsername(rs.getString("username"));
                ae.setGiven_name(rs.getString("given_name"));
                ae.setFamily_name(rs.getString("family_name"));
                ae.setMiddle_name(rs.getString("middle_name"));
                ae.setGender(rs.getString("gender"));
                ae.setBirthdate(rs.getString("birthdate"));
                ae.setAddress1(rs.getString("address1"));
                ae.setCounty_district(rs.getString("county_district"));
                ae.setAddress4(rs.getString("address4"));
                ae.setAddress5(rs.getString("address5"));
                ae.setAddress6(rs.getString("address6"));
                ae.setDead(rs.getString("dead"));
                ae.setBirthdate_estimate(rs.getString("birthdate_estimated"));
                ae.setAmrsLocation(parentUUID);
                amrsUserServices.save(ae);

                //Migate user
                RegisterOpenMRSPayload.users(ae, amrsUserServices, url, auth);


            }
//con.close();
        }
    }

    //Patients
    public static void patients(String server, String username, String password, String locations, String parentUUID, AMRSPatientServices amrsPatientServices, AMRSIdentifiersService amrsIdentifiersService, AMRSPersonAtrributesService amrsPersonAtrributesService,Boolean samplePatients,String kenyaemrLocationUuid, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

        RegisterOpenMRSPayload.patient(amrsPatientServices, amrsIdentifiersService, amrsPersonAtrributesService,kenyaemrLocationUuid, url, auth);

        List<AMRSPatients> patientsListt = amrsPatientServices.findFirstByOrderByIdDesc();
        String sql = "";
        String whereSQL="";
        if (samplePatients) {
            whereSQL ="where l.uuid in ( " + locations + " ) and p.voided=0  and p.person_id in ( " + samplePatientList + ")";
        }else{
            whereSQL="where l.uuid in ( " + locations + " ) and p.voided=0  and p.voided=0";
        }
        if (patientsListt.isEmpty()) {

            sql = "select  \n" +
                    "                   p.uuid, \n" +
                    "                   p.person_id, \n" +
                    "                   case when pn.given_name is null then ' ' else pn.given_name end given_name, \n" +
                    "                   case when pn.family_name is null then ' ' else pn.family_name end  family_name, \n" +
                    "                   case when pn.middle_name is null then ' ' else  pn.middle_name end middle_name , \n" +
                    "                   p.gender, \n" +
                    "                   p.birthdate, \n" +
                    " case when pa.address1 is null or pa.address1=''  then 'Missing' else pa.address1 end address1, \n" +
                    " case when pa.county_district is null or pa.county_district=''  then 'Missing' else pa.county_district end county_district, \n" +
                    " case when pa.address4 is null or pa.address4=''  then 'Missing' else pa.address4 end address4, \n" +
                    " case when pa.address5 is null or pa.address5=''  then 'Missing' else pa.address5 end address5, \n" +
                    " case when pa.address6 is null or pa.address6=''  then 'Missing' else pa.address6 end address6, \n" +
                    "                   p.dead, \n" +
                    "                   p.cause_of_death,\n" +
                    "                   p.death_date,\n" +
                    "case \n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 16 then 142412\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 43 then 114100\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 58 then 112141\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 60 then 115835\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 84 then 84\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 86 then 86\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 102 then 102\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 123 then 116128\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 148 then 112234\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 507 then 507\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 903 then 117399\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1067 then 1067\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1107 then 1107\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1571 then 125561\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1593 then 159\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 2375 then 137296\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5041 then 5041\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5547 then 119975\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5622 then 5622\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 6483 then 139444\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 7257 then 134612\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 7971 then 145717 \n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 10366 then 133814\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 12038 then 155762 \n" +
                    "    else 5622\n" +
                    "\tend as kmr_concept_id," +
                    "case\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 16 then '142412AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 43 then '114100AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 58 then '112141AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 60 then '115835AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 84 then '84AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 86 then '86AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 102 then '102AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 123 then '116128AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 148 then '112234AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 507 then '507AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 903 then '117399AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1067 then '1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1107 then '1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1571 then '125561AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1593 then '159AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 2375 then '137296AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5041 then '5041AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5547 then '119975AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5622 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 6483 then '139444AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 7257 then '134612AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 7971 then '145717AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 10366 then '133814AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 12038 then '155762AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "    else '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\tend as kmr_concept_uuid, \n" +
                    "                   p.birthdate_estimated, \n" +
                    "                   p.voided, \n" +
                    "                   l.location_id, \n" +
                    "                   l.name location_name, \n" +
                    "case when pa.address1 is null or pa.address1=''  then 'Missing' else pa.address1 end county,\n" +
                    "case when pa.address2 is null or pa.address2=''  then 'Missing' else pa.address2 end sub_county,\n" +
                    "case when pa.city_village is null or pa.city_village=''  then 'Missing' else pa.city_village end city_village,\n" +
                    "case when pa.state_province is null or pa.state_province=''  then 'Missing' else pa.state_province end state_province,\n" +
                    "case when pa.address3 is null or pa.address3=''  then 'Missing' else pa.address3 end landmark\n" +
                    "                   from amrs.encounter e  \n" +
                    "                   inner join amrs.patient pt on e.patient_id =pt.patient_id and pt.voided=0 \n" +
                    "                   inner join amrs.person p on p.person_id=pt.patient_id and p.voided=0 \n" +
                    "                   inner join amrs.person_name pn on pn.person_id=p.person_id and pn.voided=0 \n" +
                    "                   inner join amrs.person_address pa on pa.person_id=p.person_id and pa.preferred=1 and pa.voided=0 \n" +
                    "                   inner join amrs.location l on e.location_id=l.location_id \n" +
                    "                   " + whereSQL + " \n" +
                    "                   group by pt.patient_id \n" +
                    "                   order by e.patient_id asc limit 1000 ";
            System.out.println("SQL ID is " + sql);
        } else {
            String pid = patientsListt.get(0).getPersonId();
            System.out.println("Person ID is " + pid);
           // System.out.println("SQL ID is " + sql);
            if (samplePatients) {
                whereSQL ="where l.uuid in ( " + locations + " ) and p.voided=0  and p.person_id in ( " + samplePatientList + ")";
            }else{
                whereSQL="where l.uuid in ( " + locations + " ) and p.voided=0 and p.person_id >"+ pid +"";
            }

            sql = "select  \n" +
                    "                   p.uuid, \n" +
                    "                   p.person_id, \n" +
                    "                   case when pn.given_name is null then '' else pn.given_name end given_name, \n" +
                    "                   case when pn.family_name is null then '' else pn.family_name end  family_name, \n" +
                    "                   case when pn.middle_name is null then '' else  pn.middle_name end middle_name , \n" +
                    "                   p.gender, \n" +
                    "                   p.birthdate, \n" +
                    " case when pa.address1 is null or pa.address1=''  then 'Missing' else pa.address1 end address1, \n" +
                    " case when pa.county_district is null or pa.county_district=''  then 'Missing' else pa.county_district end county_district, \n" +
                    " case when pa.address4 is null or pa.address4=''  then 'Missing' else pa.address4 end address4, \n" +
                    " case when pa.address5 is null or pa.address5=''  then 'Missing' else pa.address5 end address5, \n" +
                    " case when pa.address6 is null or pa.address6=''  then 'Missing' else pa.address6 end address6, \n" +

                    "                   p.dead, \n" +
                    "                   p.cause_of_death,\n" +
                    "                   p.death_date,\n" +
                    "case \n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 16 then 142412\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 43 then 114100\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 58 then 112141\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 60 then 115835\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 84 then 84\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 86 then 86\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 102 then 102\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 123 then 116128\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 148 then 112234\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 507 then 507\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 903 then 117399\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1067 then 1067\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1107 then 1107\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1571 then 125561\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1593 then 159\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 2375 then 137296\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5041 then 5041\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5547 then 119975\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5622 then 5622\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 6483 then 139444\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 7257 then 134612\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 7971 then 145717 \n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 10366 then 133814\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 12038 then 155762 \n" +
                    "    else 5622\n" +
                    "\tend as kmr_concept_id," +
                    "case\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 16 then '142412AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 43 then '114100AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 58 then '112141AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 60 then '115835AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 84 then '84AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 86 then '86AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 102 then '102AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and  p.cause_of_death = 123 then '116128AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 148 then '112234AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 507 then '507AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 903 then '117399AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1067 then '1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1107 then '1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1571 then '125561AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 1593 then '159AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 2375 then '137296AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5041 then '5041AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5547 then '119975AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 5622 then '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 6483 then '139444AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 7257 then '134612AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 7971 then '145717AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 10366 then '133814AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' \n" +
                    "\twhen p.dead = 1 and p.cause_of_death = 12038 then '155762AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "    else '5622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'\n" +
                    "\tend as kmr_concept_uuid, \n" +
                    "                   p.birthdate_estimated, \n" +
                    "                   p.voided, \n" +
                    "                   l.location_id, \n" +
                    "                   l.name location_name, \n" +
                    "case when pa.address1 is null or pa.address1=''  then 'Missing' else pa.address1 end county,\n" +
                    "case when pa.address2 is null or pa.address2=''  then 'Missing' else pa.address2 end sub_county,\n" +
                    "case when pa.city_village is null or pa.city_village=''  then 'Missing' else pa.city_village end city_village,\n" +
                    "case when pa.state_province is null or pa.state_province=''  then 'Missing' else pa.state_province end state_province,\n" +
                    "case when pa.address3 is null or pa.address3=''  then 'Missing' else pa.address3 end landmark\n" +
                    "                   from amrs.encounter e  \n" +
                    "                   inner join amrs.patient pt on e.patient_id =pt.patient_id and pt.voided=0\n" +
                    "                   inner join amrs.person p on p.person_id=pt.patient_id  and p.voided=0\n" +
                    "                   inner join amrs.person_name pn on pn.person_id=p.person_id and pn.voided=0 \n" +
                    "                   inner join amrs.person_address pa on pa.person_id=p.person_id and pa.preferred=1 and pa.voided=0 \n" +
                    "                   inner join amrs.location l on e.location_id=l.location_id \n" +
                    "" + whereSQL + " \n" +
                    "group by pt.patient_id\n" +
                    "order by e.patient_id asc limit 1000";

        }
        //System.out.println("locations " + locations + " parentUUID " + parentUUID);
        //System.out.println("SQL "+ sql);
        Connection con = DriverManager.getConnection(server, username, password);
        int x = 0;
        Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
        ResultSet rs = stmt.executeQuery(sql);
        rs.last();
        x = rs.getRow();
        rs.beforeFirst();
        while (rs.next()) {

            System.out.println("User id " + rs.getString(1));
            String pid = rs.getString("person_id");
            //List<AMRSPatients> patientsList = amrsPatientServices.getPatientByLocation(rs.getString("person_id"), parentUUID);
            //List<AMRSPatients> patientsList = amrsPatientServices.getPatientByStatus(rs.getString("person_id"));
            List<AMRSPatients> patientsList = amrsPatientServices.getByPatientID(pid);

            if (patientsList.isEmpty()) {
                String person_id = rs.getString("person_id");
                AMRSPatients ae = new AMRSPatients();
                ae.setUuid(rs.getString("uuid"));
                ae.setPersonId(rs.getString("person_id"));
                ae.setGiven_name(rs.getString("given_name"));
                ae.setFamily_name(rs.getString("family_name"));
                ae.setMiddle_name(rs.getString("middle_name"));
                ae.setGender(rs.getString("gender"));
                ae.setBirthdate(rs.getString("birthdate"));
                ae.setAddress1(rs.getString("address1"));//county
                ae.setCounty_district(rs.getString("county_district"));
                ae.setAddress4(rs.getString("address4")); //
                ae.setAddress5(rs.getString("address5")); //
                ae.setAddress6(rs.getString("address6")); //
                ae.setCounty(rs.getString("county"));
                ae.setSubcounty(rs.getString("sub_county"));
                ae.setLandmark(rs.getString("landmark"));
                ae.setVillage(rs.getString("city_village"));
                ae.setDead(rs.getString("dead"));
                ae.setCauseOfDead(rs.getString("cause_of_death"));
                ae.setBirthdate_estimated(rs.getString("birthdate_estimated"));
                ae.setKenyaemrCauseOfDead(rs.getString("kmr_concept_id"));
                ae.setKenyaemrCauseOfDeadUuid(rs.getString("kmr_concept_uuid"));
                ae.setVoided(rs.getString("voided"));
                ae.setLocation_id(rs.getString("location_id"));
                ae.setCityVillage(rs.getString("city_village"));
                ae.setAddress2(rs.getString("Sub_county"));//Sub_county;
                ae.setAddress3(rs.getString("landmark"));//landmark;
                ae.setCounty_district(rs.getString("county_district"));
                ae.setStateProvince(rs.getString("state_province"));
                ae.setParentlocationuuid(parentUUID);
                amrsPatientServices.save(ae);

                String identifiersSQl = "select pi.patient_id,\n" +
                        "pit.uuid identifer_uuid," +
                        "pi.identifier,pi.preferred,pi.voided,pi.location_id,pi.identifier_type\n" +
                        "from amrs.patient_identifier pi\n" +
                        "join amrs.patient_identifier_type pit on pi.identifier_type=pit.patient_identifier_type_id\n" +
                        "inner join amrs.person p on p.person_id =pi.patient_id\n" +
                        "where pi.voided=0 and pi.patient_id=" + rs.getString("person_id") + "\n" +
                        "order by pi.patient_id desc";
                //identifers
                Statement stmtID = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
                ResultSet rsID = stmtID.executeQuery(identifiersSQl);
                rsID.last();
                x = rsID.getRow();
                rsID.beforeFirst();
                while (rsID.next()) {
                    List<AMRSIdentifiers> identifiers = amrsIdentifiersService.findByPatientidAndIdentifierType(rsID.getString("patient_id"),rsID.getString("identifier_type"));
                    if (identifiers.isEmpty()) {
                        AMRSIdentifiers iden = new AMRSIdentifiers();
                        String pref = "";
                        if (rsID.getString(4).equals("0")) {
                            pref = "false";
                        } else {
                            pref = "true";
                        }
                        String identifer = "";
                        if (rsID.getString(2).equals("f2d6ff1a-8440-4d35-a150-1d4b5a930c5e")) {
                            identifer = rsID.getString(3).replace("-", "");

                        } else {
                            identifer = rsID.getString(3);
                        }
                        iden.setPatientid(rsID.getString(1));
                        iden.setUuid(rsID.getString(2));
                        iden.setIdentifier(identifer);
                        iden.setPreferred(pref);
                        iden.setVoided(rsID.getString(5));
                        iden.setLocation(rsID.getString(6));
                        iden.setKenyaemr_uuid(Mappers.identifers(rsID.getString(2)));
                        iden.setParentlocationuuid(parentUUID);
                        iden.setIdentifierType(rsID.getString("identifier_type"));

                        amrsIdentifiersService.save(iden);

                    }
                    //end of identifers
                    //Person Attributes
                    String pattreibutesSQl =
                            "select  pa.person_id,\n" +
                                    "pt.person_attribute_type_id,\n" +
                                    "pt.name,\n" +
                                    "pa.value,\n" +
                                    "case when pt.person_attribute_type_id=10 then 'b2c38640-2603-4629-aebd-3b54f33f1e3a' -- Telephone\n" +
                                    "when pt.person_attribute_type_id =12 then '830bef6d-b01f-449d-9f8d-ac0fede8dbd3'\n" +
                                    "when pt.person_attribute_type_id =25 then '342a1d39-c541-4b29-8818-930916f4c2dc' -- contact\n" +
                                    "when pt.person_attribute_type_id =0 then '7cf22bec-d90a-46ad-9f48-035952261294' -- Kin Address\n" +
                                    " when pt.person_attribute_type_id =0 then '94614350-84c8-41e0-ac29-86bc107069be' -- alternative phone\n" +
                                    "WHEN pt.person_attribute_type_id = 10 THEN 'b2c38640-2603-4629-aebd-3b54f33f1e3a'\n" + "       " +
                                    " WHEN pt.person_attribute_type_id = 12 THEN '830bef6d-b01f-449d-9f8d-ac0fede8dbd3'\n" +
                                    "        WHEN pt.person_attribute_type_id = 25 THEN '342a1d39-c541-4b29-8818-930916f4c2dc'\n" +
                                    "        WHEN pt.person_attribute_type_id = 0 THEN '7cf22bec-d90a-46ad-9f48-035952261294' \n" +
                                    "        WHEN pt.person_attribute_type_id = 40 THEN '94614350-84c8-41e0-ac29-86bc107069be'\n" +
                                    "\t\tWHEN pt.person_attribute_type_id = 163 THEN 'accb7273-ef29-11ed-8ec5-70b5e8686cf7'\n" +
                                    "        WHEN pt.person_attribute_type_id = 2 THEN '8d8718c2-c2cc-11de-8d13-0010c6dffd0f'\n" +
                                    "        WHEN pt.person_attribute_type_id = 3 THEN '8d871afc-c2cc-11de-8d13-0010c6dffd0f'\n" +
                                    "        WHEN pt.person_attribute_type_id = 5 THEN '8d871f2a-c2cc-11de-8d13-0010c6dffd0f'\n" +
                                    "        WHEN pt.person_attribute_type_id = 60 THEN 'b8d0b331-1d2d-4a9a-b741-1816f498bdb6'\n" +
                                    "        when pt.person_attribute_type_id = 7  then '8d87236c-c2cc-11de-8d13-0010c6dffd0f'\n" +
                                    "        when pt.person_attribute_type_id = 59  then 'd0aa9fd1-2ac5-45d8-9c5e-4317c622c8f5'\n" +
                                    "        when pt.person_attribute_type_id = 165  then '752a0331-5293-4aa5-bf46-4d51aaf2cdc5'\n" +
                                    "        when pt.person_attribute_type_id = 164  then '869f623a-f78e-4ace-9202-0bed481822f5'\n" +
                                    "        when pt.person_attribute_type_id = 4  then '8d871d18-c2cc-11de-8d13-0010c6dffd0f' \n" +
                                    "      when pt.person_attribute_type_id = 1  then  '8d871386-c2cc-11de-8d13-0010c6dffd0f'\n" +
                                    "else null end as kenyaemruuid\n" +
                                    " from amrs.person_attribute pa\n" +
                                    " inner join amrs.person_attribute_type pt on pa.person_attribute_type_id = pt.person_attribute_type_id\n" +
                                    "where pa.person_id  in (" + rs.getString("person_id") + ") and pa.voided=0";
                    Statement stmtPA = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                            ResultSet.CONCUR_READ_ONLY);
                    ResultSet rsPA = stmtPA.executeQuery(pattreibutesSQl);
                    rsPA.last();
                    x = rsPA.getRow();
                    rsPA.beforeFirst();
                    while (rsPA.next()) {
                        List<AMRSPatientAttributes> patientAttributes = amrsPersonAtrributesService.getByPatientIDAndPType(rsPA.getString("person_id"), rsPA.getString("person_attribute_type_id"));
                        if (patientAttributes.isEmpty()) {
                            AMRSPatientAttributes apt = new AMRSPatientAttributes();
                            apt.setPatientId(rsPA.getString("person_id"));
                            apt.setPersonAttributeTypeId(rsPA.getString("person_attribute_type_id"));
                            apt.setPersonAttributeName(rsPA.getString("name"));
                            apt.setPersonAttributeValue(rsPA.getString("value"));
                            apt.setKenyaemrAttributeUuid(rsPA.getString("kenyaemruuid"));
                            amrsPersonAtrributesService.save(apt);
                        }

                    }
                    //End of Person Attributes

                }
                //Migate Patient
                RegisterOpenMRSPayload.patient(amrsPatientServices, amrsIdentifiersService, amrsPersonAtrributesService,kenyaemrLocationUuid, url, auth);

            }
            else{
                System.out.println("Patient with ID "+ pid  +" Already Exist");
            }

        }
        //Migate Patient
        RegisterOpenMRSPayload.patient(amrsPatientServices, amrsIdentifiersService, amrsPersonAtrributesService,kenyaemrLocationUuid, url, auth);

        con.close();
       }
    public static void patient_relationship(String server, String username, String password,AMRSPatientRelationshipService amrsPatientRelationshipService,AMRSPatientServices amrsPatientServices, AMRSTranslater amrsTranslater, String url, String auth) throws SQLException, JSONException, ParseException, IOException {

        List<String> stringPIDsList = amrsPatientServices.getAllPatientID();

        String PatientList = stringPIDsList.toString().substring(1, stringPIDsList.toString().length() - 1);

        String sql = "SELECT r.person_a,\n" +
                " r.relationship,\n" +
                " rt.a_is_to_b,\n" +
                " rt.b_is_to_a,\n" +
                " rt.uuid,\n" +
                " r.person_b,\n" +
                "case when relationship =1 then '8d919b58-c2cc-11de-8d13-0010c6dffd0f'\n" +
                "                  when relationship =2 then '8d91a01c-c2cc-11de-8d13-0010c6dffd0f'\n" +
                "                  when relationship =3 then '8d91a210-c2cc-11de-8d13-0010c6dffd0f'\n" +
                "                  when relationship =4 then '8d91a3dc-c2cc-11de-8d13-0010c6dffd0f'\n" +
                "                  when relationship =5 then '5f115f62-68b7-11e3-94ee-6bef9086de92'\n" +
                "                  when relationship =6 then 'd6895098-5d8d-11e3-94ee-b35a4132a5e3'\n" +
                "                  when relationship =7 then '007b765f-6725-4ae9-afee-9966302bace4'\n" +
                "                  when relationship =8 then '2ac0d501-eadc-4624-b982-563c70035d46'\n" +
                "                  when relationship =9 then '58da0d1e-9c89-42e9-9412-275cef1e0429'\n" +
                "                  when relationship =10 then 'a8058424-5ddf-4ce2-a5ee-6e08d01b5960'\n" +
                "                  end as kenyaemr_uuid\n"+
                " FROM amrs.relationship r \n" +
                " inner join amrs.relationship_type rt on rt.relationship_type_id=r.relationship\n" +
                " where r.voided =0 and r.person_a in ("+ PatientList +")";

        Connection con = DriverManager.getConnection(server, username, password);
        int x = 0;
        Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
        ResultSet rs = stmt.executeQuery(sql);
        rs.last();
        x = rs.getRow();
        rs.beforeFirst();
        while (rs.next()) {
            String persona =rs.getString("person_a");
            String personb =rs.getString("person_b");
            String relationshipType =rs.getString("relationship");
            String relationshipuuid =rs.getString("uuid");
            String aIsToB =rs.getString("a_is_to_b");
            String bIsToA =rs.getString("b_is_to_a");
            String kenyaemruuid =rs.getString("kenyaemr_uuid");

            System.out.println("aIsToB "+ aIsToB);
            System.out.println("bIsToA "+ bIsToA);

            List<AMRSPatientRelationship> amrsPatientRelationships = amrsPatientRelationshipService.findByPersonAAndPeronBAndRelationship(persona,personb,relationshipType);
            System.out.println("Totals size  "+ amrsPatientRelationships.size());
            if (amrsPatientRelationships.isEmpty()) {
                AMRSPatientRelationship pr =new AMRSPatientRelationship();
                System.out.println("aIsToB "+ aIsToB);
                System.out.println("bIsToA "+ bIsToA);
                pr.setAistob(aIsToB);
                pr.setBistoa(bIsToA);
                pr.setPersonA(persona);
                pr.setPersonB(personb);
                pr.setRelationshipUuid(kenyaemruuid);
                pr.setRelationshipType(relationshipType);
               // pr.setRelationshipUuid(relationshipuuid);
                String puuida =   amrsTranslater.KenyaemrPatientUuid(persona);
                String puuidb =   amrsTranslater.KenyaemrPatientUuid(personb);
                System.out.println("bIsToA "+ puuida +" puuida "+ puuidb);
                pr.setKenyaemrpersonAUuid(amrsTranslater.KenyaemrPatientUuid(persona));
                pr.setKenyaemrpersonBUuid(amrsTranslater.KenyaemrPatientUuid(personb));
                amrsPatientRelationshipService.save(pr);

            }
        }
        RegisterOpenMRSPayload.relationship(amrsPatientRelationshipService, url, auth);
    }
}
