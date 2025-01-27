package ampath.co.ke.amrs_kenyaemr.cron;

import ampath.co.ke.amrs_kenyaemr.methods.AMRSLocation;
import ampath.co.ke.amrs_kenyaemr.methods.AMRSTranslater;
import ampath.co.ke.amrs_kenyaemr.service.*;
import ampath.co.ke.amrs_kenyaemr.tasks.MigrateCareData;
import ampath.co.ke.amrs_kenyaemr.tasks.MigrateRegistration;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.concurrent.CompletableFuture;

@Component
public class CronTasks {
  @Value("${spring.etl.username}")
  public String username;
  @Value("${spring.etl.password}")
  public String password;
  @Value("${spring.etl.server}")
  public String server;
  @Value("${spring.openmrs.url}")
  public String OpenMRSURL;
  @Value("${spring.openmrs.auth}")
  public String auth;
  @Autowired
  private LocationService locationsService;
  @Autowired
  private AMRSUserServices amrsUserServices;
  @Autowired
  private AMRSIdentifiersService amrsIdentifiersService;
  @Autowired
  private AMRSPatientServices amrsPatientServices;
  @Autowired
  private AMRSFormsMappingService formsMappingService;
  @Autowired
  private AMRSEncounterFormsMappingService amrsEncounterFormsMappingService;
  @Autowired
  private AMRSProgramService amrsProgramService;
  @Autowired
  private AMRSEnrollmentService amrsEnrollmentService;
  @Autowired
  private AMRSObsService amrsObsService;
  @Autowired
  private AMRSConceptMappingService amrsConceptMappingService;
  @Autowired
  private AMRSVisitService amrsVisitService;
  @Autowired
  private AMRSTriageService amrsTriageService;
  @Autowired
  private AMRSEncounterMappingService amrsEncounterMappingService;
  @Autowired
  private AMRSEncounterService amrsEncounterService;
  @Autowired
  private AMRSHIVEnrollmentService amrsHIVEnrollmentService;
  @Autowired
  private AMRSOrderService amrsOrderService;
  @Autowired
  private AMRSPersonAtrributesService amrsPersonAtrributesService;
  @Autowired
  private AMRSRegimenSwitchService amrsRegimenSwitchService;
  @Autowired
  private AMRSPatientStatusService amrsPatientStatusService;
  @Autowired
  private AMRSGreenCardService amrstcaService;
  @Autowired
  private AMRSMappingService amrsMappingService;
  @Autowired
  private AMRSPatientRelationshipService amrsPatientRelationshipService;
  @Autowired
  private AMRSTranslater amrsTranslater;
  @Autowired
  private AMRSOrdersResultsService amrsOrdersResultsService;
  @Autowired
  private AMRSArtRefillService amrsArtRefillService;
  @Autowired
  private AMRSDefaulterTracingService amrsDefaulterTracingService;
  private AMRSOtzActivityService amrsOtzActivityService;
  @Autowired
  private AMRSOtzDiscontinuationService amrsOtzDiscontinuationService;
  @Autowired
  private AMRSOtzEnrollmentService amrsOtzEnrollmentService;
  @Autowired
  private AMRSTbScreeningService amrsTbScreeningService;
  @Autowired
  private AMRSOvcService amrsOvcService;
  @Autowired
  private LocationService locationService;
  @Autowired
  private AMRSPrepFollowUpService amrsPrepFollowUpService;

  @Autowired
  private AMRSPrepInitialService amrsPrepInitialService;

  @Autowired
  private AMRSPrepMonthlyRefillService amrsPrepMonthlyRefillService;

  @Autowired
  private AMRSCovidService amrsCovidService;

  @Autowired
  private AMRSAlcoholService amrsAlcoholService;

    @Autowired
    private AMRSHeiOutcomeService amrsHeiOutcomeService;

  @Autowired
  private AMRSGbvScreeningService amrsGbvScreeningService;

  @Autowired
  private AMRSEacService amrsEacService;

  @Autowired
  private AMRSDepressionScreeningService amrsDepressionScreeningService;

  @Value("${mapping.endpoint:http://localhost:8082/mappings/concepts}")
  private String mappingEndpoint;
  private final RestTemplate restTemplate = new RestTemplate();

  //@Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void callEndpoint() {
    try {
      String response = restTemplate.getForObject(mappingEndpoint, String.class);
      System.out.println("Endpoint response: " + response);
    } catch (Exception e) {
      System.err.println("Error calling the endpoint: " + e.getMessage());
    }
  }

 @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000) // Every 30 minutes
  public void ProcessMappings() throws JSONException, ParseException, SQLException, IOException {
    MigrateRegistration.conceptMapping(amrsMappingService);
  }

  @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000) // Every 30 minutes
  public void ProcessLocations() throws JSONException, ParseException, SQLException, IOException {
    MigrateRegistration.locations(server, username, password, locationService);
  }

  //@Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000) // Every 30 minutes
  public void ProcessUsers() throws JSONException, ParseException, SQLException, IOException {
    AMRSLocation amrsLocation = new AMRSLocation();
    String locationId = amrsLocation.getLocationsUuid(locationService);
    String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
    MigrateRegistration.users(server, username, password, locationId, amrsUserServices, OpenMRSURL, auth);

  }
 @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000) // Every 30 minutes
  public void ProcessPatients() throws JSONException, ParseException, SQLException, IOException {
    AMRSLocation amrsLocation = new AMRSLocation();
    String locationId = amrsLocation.getLocationsUuid(locationService);
    String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
    String parentUuid = KenyaEMRlocationUuid;
    Boolean samplePatients = true;
    MigrateRegistration.patients(server, username, password, locationId, parentUuid, amrsPatientServices, amrsIdentifiersService, amrsPersonAtrributesService, samplePatients, KenyaEMRlocationUuid, OpenMRSURL, auth);
  }

  @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000) // Every 30 minutes
  public void ProcessPatientRelationShips() throws JSONException, ParseException, SQLException, IOException {
    MigrateRegistration.patient_relationship(server, username, password, amrsPatientRelationshipService, amrsPatientServices, amrsTranslater, OpenMRSURL, auth);
  }

  @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void civilStatus() throws JSONException, ParseException, SQLException, IOException {
    MigrateCareData.patientStatus(server, username, password, amrsPatientStatusService, amrsConceptMappingService, amrsPatientServices, OpenMRSURL, auth);
  }

 @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void ProcessPrograms() throws JSONException, ParseException, SQLException, IOException {
    AMRSLocation amrsLocation = new AMRSLocation();
    String locationId = amrsLocation.getLocationsUuid(locationService);
      String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
    MigrateCareData.programs(server, username, password, amrsProgramService, amrsPatientServices, amrsTranslater, KenyaEMRlocationUuid, OpenMRSURL, auth);
  }

  @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void ProcessVisits() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        // String locationId=amrsLocation.getLocationsUuid(locationService);
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
        MigrateCareData.visits(server, username, password, KenyaEMRlocationUuid, amrsVisitService, amrsPatientServices, amrsTranslater, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void ProcessTriage() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String locationId = amrsLocation.getLocationsUuid(locationService);
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
        System.out.println("Locations is here " + locationId);
        MigrateCareData.triage(server, username, password, locationId, KenyaEMRlocationUuid, amrsTranslater, amrsTriageService, amrsPatientServices, amrsEncounterService, amrsConceptMappingService, amrsVisitService, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void ProcessOrders() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
        MigrateCareData.order(server, username, password, KenyaEMRlocationUuid, amrsOrderService, amrsPatientServices, amrsVisitService, amrsTranslater, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

   @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void HIVEnrollments() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
        MigrateCareData.hivenrollment(server, username, password, KenyaEMRlocationUuid, amrsHIVEnrollmentService, amrsTranslater, amrsPatientServices, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void ProcessProgramSwitches() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
        MigrateCareData.DrugSwitches(server, username, password, KenyaEMRlocationUuid, amrsRegimenSwitchService, amrsTranslater, amrsPatientServices, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  @Scheduled(initialDelay = 0, fixedRate = 50 * 60 * 1000)
  public void processGreenCard() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
        MigrateCareData.processGreenCard(server, username, password, KenyaEMRlocationUuid, amrstcaService, amrsPatientServices, amrsTranslater, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

 @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void ArtRefill() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
        MigrateCareData.artRefill(server, username, password, KenyaEMRlocationUuid, amrsArtRefillService, amrsTranslater, amrsPatientServices, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void defaulterTracing() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
        MigrateCareData.defaulterTracing(server, username, password,KenyaEMRlocationUuid, amrsDefaulterTracingService, amrsTranslater, amrsPatientServices, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

   @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void OTZEnrollment() throws JSONException, ParseException, SQLException, IOException {
       CompletableFuture.runAsync(() -> {
         try {
           AMRSLocation amrsLocation = new AMRSLocation();
           String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
          MigrateCareData.processOtzEnrollments(server, username, password, KenyaEMRlocationUuid, amrsOtzEnrollmentService, amrsPatientServices, amrsTranslater, OpenMRSURL, auth);
         } catch (Exception e) {
           e.printStackTrace();
         }
       });
     }


  @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void OTZActivity() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
       MigrateCareData.processOtzActivity(server, username, password, KenyaEMRlocationUuid, amrsOtzActivityService, amrsTranslater,amrsPatientServices, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

   @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void OTZDiscontinuation() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
        MigrateCareData.processOtzDiscontinuation(server, username, password,KenyaEMRlocationUuid, amrsOtzDiscontinuationService, amrsPatientServices, amrsTranslater, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }


  @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void TBScreening() throws JSONException, ParseException, SQLException, IOException {
      CompletableFuture.runAsync(() -> {
          try {
      AMRSLocation amrsLocation = new AMRSLocation();
      String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
      MigrateCareData.processTBScreening(server, username, password, KenyaEMRlocationUuid, amrsTbScreeningService, amrsPatientServices, amrsTranslater, OpenMRSURL, auth);
          } catch (Exception e) {
              e.printStackTrace();
          }
      });
  }

  //@Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void ovc() throws JSONException, ParseException, SQLException, IOException {
      CompletableFuture.runAsync(() -> {
          try {
              AMRSLocation amrsLocation = new AMRSLocation();
              String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
              MigrateCareData.ovc(server, username, password, KenyaEMRlocationUuid, amrsOvcService, amrsTranslater, amrsPatientServices, OpenMRSURL, auth);
          } catch (Exception e) {
              e.printStackTrace();
          }
      });
  }

  //@Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void prepInitial() throws JSONException, ParseException, SQLException, IOException {
      CompletableFuture.runAsync(() -> {
          try {
              AMRSLocation amrsLocation = new AMRSLocation();
              String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
              MigrateCareData.prepInitial(server, username, password, KenyaEMRlocationUuid, amrsPrepInitialService, amrsTranslater, amrsPatientServices, OpenMRSURL, auth);
          } catch (Exception e) {
              e.printStackTrace();
          }
      });
  }

  @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void prepFollowUp() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
        MigrateCareData.prepFollowUp(server, username, password, KenyaEMRlocationUuid, amrsPrepFollowUpService, amrsTranslater, amrsPatientServices, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  //@Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void prepMonthlyRefill() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
        MigrateCareData.prepMonthlyRefill(server, username, password, KenyaEMRlocationUuid, amrsPrepMonthlyRefillService, amrsTranslater, amrsPatientServices, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

 // @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void processCovid() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
        MigrateCareData.processCovid(server, username, password, KenyaEMRlocationUuid, amrsCovidService, amrsPatientServices, amrsTranslater, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

 // @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void processAlcohol() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
        MigrateCareData.processAlcohol(server, username, password, KenyaEMRlocationUuid, amrsAlcoholService, amrsPatientServices, amrsTranslater, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
 // @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void processMCHEnrollements() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
       // MigrateCareData(server, username, password, KenyaEMRlocationUuid, amrsAlcoholService, amrsPatientServices, amrsTranslater, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

    // @Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
    public void processHeiOutcome() throws JSONException, ParseException, SQLException, IOException {
        CompletableFuture.runAsync(() -> {
            try {
                AMRSLocation amrsLocation = new AMRSLocation();
                String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
                MigrateCareData.processHeiOutcome(server, username, password, KenyaEMRlocationUuid, amrsHeiOutcomeService, amrsPatientServices, amrsTranslater, OpenMRSURL, auth);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

     //@Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
    public void GBVScreening() throws JSONException, ParseException, SQLException, IOException {
         CompletableFuture.runAsync(() -> {
             try {
                 AMRSLocation amrsLocation = new AMRSLocation();
                 String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
                 MigrateCareData.processGBVScreening(server, username, password,  KenyaEMRlocationUuid, amrsGbvScreeningService, amrsPatientServices, amrsTranslater, OpenMRSURL, auth);
             } catch (Exception e) {
                 e.printStackTrace();
             }
         });
    }
  //@Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void processEac() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
        MigrateCareData.processEac(server, username, password, KenyaEMRlocationUuid, amrsEacService, amrsPatientServices, amrsTranslater, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  //@Scheduled(initialDelay = 0, fixedRate = 30 * 60 * 1000)
  public void processDepressionScreening() throws JSONException, ParseException, SQLException, IOException {
    CompletableFuture.runAsync(() -> {
      try {
        AMRSLocation amrsLocation = new AMRSLocation();
        String KenyaEMRlocationUuid = amrsLocation.getKenyaEMRLocationUuid();
        MigrateCareData.processDepressionScreening(server, username, password, KenyaEMRlocationUuid, amrsDepressionScreeningService, amrsPatientServices, amrsTranslater, OpenMRSURL, auth);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
}


