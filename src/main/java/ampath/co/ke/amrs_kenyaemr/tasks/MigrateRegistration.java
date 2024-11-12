package ampath.co.ke.amrs_kenyaemr.tasks;

import ampath.co.ke.amrs_kenyaemr.models.AMRSIdentifiers;
import ampath.co.ke.amrs_kenyaemr.models.AMRSPatients;
import ampath.co.ke.amrs_kenyaemr.models.AMRSUsers;
import ampath.co.ke.amrs_kenyaemr.service.AMRSIdentifiersService;
import ampath.co.ke.amrs_kenyaemr.service.AMRSPatientServices;
import ampath.co.ke.amrs_kenyaemr.service.AMRSUserServices;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

public class MigrateRegistration {
    public static void users(String server, String username, String password, String locations, String parentUUID,AMRSUserServices amrsUserServices,String url,String auth) throws SQLException, JSONException, ParseException, IOException {

        System.out.println("locations " + locations + " parentUUID " + parentUUID);
        Connection con = DriverManager.getConnection(server, username, password);
        int x = 0;
        Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
        ResultSet rs = stmt.executeQuery("select \n" +
                "u.uuid,\n" +
                "u.user_id,\n" +
                "u.system_id,\n" +
                "u.username,\n" +
                "pn.given_name,\n" +
                "pn.family_name,\n" +
                "pn.middle_name,\n" +
                "p.gender,\n" +
                "p.birthdate,\n" +
                "pa.address1,\n" +
                "pa.county_district,\n" +
                "pa.address4,\n" +
                "pa.address5,\n" +
                "pa.address6,\n" +
                "p.dead,\n" +
                "p.birthdate_estimated\n"+
                "from amrs.encounter e \n" +
                "inner join amrs.users u on e.creator =u.user_id\n" +
                "inner join amrs.person p on p.person_id=u.person_id\n" +
                "inner join amrs.person_name pn on pn.person_id=p.person_id\n" +
                "inner join amrs.person_address pa on pa.person_id=p.person_id\n" +
                "where location_id in ('"+ locations+"') \n" +
                "group by u.user_id\n" +
                "order by u.username asc limit 25");
        rs.last();
        x = rs.getRow();
        rs.beforeFirst();
        while (rs.next()) {
            System.out.println("User id "+ rs.getString(1));
            List<AMRSUsers> afyastatErrors = amrsUserServices.getUserByLocation(rs.getString(1),parentUUID);
            if (afyastatErrors.size()==0) {
                AMRSUsers ae = new AMRSUsers();
                ae.setUuid(rs.getString(1));
                ae.setUser_id(rs.getString(2));
                ae.setSystem_id(rs.getString(3));
                ae.setUsername(rs.getString(4));
                ae.setGiven_name(rs.getString(5));
                ae.setFamily_name(rs.getString(6));
                ae.setMiddle_name(rs.getString(7));
                ae.setGender(rs.getString(8));
                ae.setBirthdate(rs.getString(9));
                ae.setAddress1(rs.getString(10));
                ae.setCounty_district(rs.getString(11));
                ae.setAddress4(rs.getString(12));
                ae.setAddress5(rs.getString(13));
                ae.setAddress6(rs.getString(14));
                ae.setDead(rs.getString(15));
                ae.setBirthdate_estimate(rs.getString(16));
                ae.setAmrsLocation(parentUUID);
                amrsUserServices.save(ae);

                //Migate user
                RegisterOpenMRSPayload.users(ae,amrsUserServices,url,auth);


            }
//con.close();
        }
    }
    //Patients
    public static void patients (String server, String username, String password, String locations, String parentUUID, AMRSPatientServices amrsPatientServices, AMRSIdentifiersService amrsIdentifiersService,String url,String auth) throws SQLException, JSONException, ParseException, IOException {
        String sql ="select \n" +
                "p.uuid,\n" +
                "p.person_id,\n" +
                "pn.given_name,\n" +
                "pn.family_name,\n" +
                "pn.middle_name,\n" +
                "p.gender,\n" +
                "p.birthdate,\n" +
                "pa.address1,\n" +
                "pa.county_district,\n" +
                "pa.address4,\n" +
                "pa.address5,\n" +
                "pa.address6,\n" +
                "p.dead,\n" +
                "p.birthdate_estimated,\n" +
                "p.voided,\n" +
                "l.location_id,\n" +
                "l.name\n" +
                "from amrs.encounter e \n" +
                "inner join amrs.patient pt on e.patient_id =pt.patient_id\n" +
                "inner join amrs.person p on p.person_id=pt.patient_id\n" +
                "inner join amrs.person_name pn on pn.person_id=p.person_id\n" +
                "inner join amrs.person_address pa on pa.person_id=p.person_id\n" +
                "inner join amrs.location l on e.location_id=l.location_id\n" +
                "where e.location_id in ('"+ locations +"') and e.patient_id<1098247 \n" +
                "group by pt.patient_id\n" +
                "order by e.patient_id desc";
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

            System.out.println("User id "+ rs.getString(1));
            List<AMRSPatients> patientsList = amrsPatientServices.getPatientByLocation(rs.getString(2),parentUUID);
            if (patientsList.size()==0) {

                String person_id=rs.getString(2);

                AMRSPatients ae = new AMRSPatients();

                ae.setUuid(rs.getString(1));
                ae.setPersonId(rs.getString(2));
                ae.setGiven_name(rs.getString(3));
                ae.setFamily_name(rs.getString(4));
                ae.setMiddle_name(rs.getString(5));
                ae.setGender(rs.getString(6));
                ae.setBirthdate(rs.getString(7));
                ae.setAddress1(rs.getString(8));
                ae.setCounty_district(rs.getString(9));
                ae.setAddress4(rs.getString(10));
                ae.setAddress5(rs.getString(11));
                ae.setAddress6(rs.getString(12));
                ae.setDead(rs.getString(13));
                ae.setBirthdate_estimated(rs.getString(14));
                ae.setVoided(rs.getString(15));
                ae.setLocation_id(rs.getString(16));
                ae.setParentlocationuuid(parentUUID);
                amrsPatientServices.save(ae);


                String identifiersSQl = "select pi.patient_id,\n" +
                        "pit.uuid identifer_uuid," +
                        "pi.identifier,pi.preferred,pi.voided,pi.location_id\n" +
                        "from amrs.patient_identifier pi\n" +
                        "join amrs.patient_identifier_type pit on pi.identifier_type=pit.patient_identifier_type_id\n" +
                        "inner join amrs.person p on p.person_id =pi.patient_id\n" +
                        "where pi.voided=0 and pi.patient_id="+ rs.getString(2) +"\n" +
                        "order by pi.patient_id desc";
                //identifers
                Statement stmtID = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
                ResultSet rsID = stmtID.executeQuery(identifiersSQl);
                rsID.last();
                x = rsID.getRow();
                rsID.beforeFirst();
                while (rsID.next()) {
                    List<AMRSIdentifiers> identifiers = amrsIdentifiersService.getPatientByLocation(rsID.getString(1), parentUUID);
                    if (identifiers.size() == 0) {
                        AMRSIdentifiers iden = new AMRSIdentifiers();
                        String pref ="";
                        if(rsID.getString(4).equals("0")){
                            pref="false";
                        }else{
                            pref="true";
                        }
                        String identifer="";
                        if(rsID.getString(2).equals("f2d6ff1a-8440-4d35-a150-1d4b5a930c5e")){
                            identifer = rsID.getString(3).replace("-", "");

                        }else{
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

                        amrsIdentifiersService.save(iden);

                    }
                    //end of identifers
                }
                //Migate Patient
                RegisterOpenMRSPayload.patient(ae,amrsPatientServices,amrsIdentifiersService,url,auth);

            }

        }
        con.close();
    }
}
