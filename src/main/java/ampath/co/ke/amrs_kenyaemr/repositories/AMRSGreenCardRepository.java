package ampath.co.ke.amrs_kenyaemr.repositories;

import ampath.co.ke.amrs_kenyaemr.models.AMRSGreenCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("AMRSGreenCardRepository")
public interface AMRSGreenCardRepository extends JpaRepository<AMRSGreenCard, Long> {
    List<AMRSGreenCard> findByResponseCodeIsNull();
    List<AMRSGreenCard> findByEncounterId(String encounterId);
    List<AMRSGreenCard> findByVisitId(String visitId);
    List<AMRSGreenCard> findByPatientIdAndVisitIdAndConceptId(String patientId,String visitId,String conceptId);


   //@Query("SELECT amrs from AMRSGreenCard amrs  GROUP BY amrs.encounterId")
   // List<AMRSGreenCard> findDistinctEncounters();
}
