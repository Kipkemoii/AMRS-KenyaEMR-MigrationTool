package ampath.co.ke.amrs_kenyaemr.repositories;

import ampath.co.ke.amrs_kenyaemr.models.AMRSDepressionScreening;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AMRSDepressionScreeningRepository extends JpaRepository<AMRSDepressionScreening, Long> {
    List<AMRSDepressionScreening> findByResponseCodeIsNull();

    List<AMRSDepressionScreening> findByEncounterId(String encounterId);

    List<AMRSDepressionScreening> findByVisitId(String visitId);

    List<AMRSDepressionScreening> findByEncounterIdAndConceptIdAndPatientId(String encounterId, String conceptId, String patientId);
}
