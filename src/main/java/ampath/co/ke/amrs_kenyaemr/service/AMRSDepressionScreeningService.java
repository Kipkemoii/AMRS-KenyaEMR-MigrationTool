package ampath.co.ke.amrs_kenyaemr.service;


import ampath.co.ke.amrs_kenyaemr.models.AMRSDepressionScreening;
import ampath.co.ke.amrs_kenyaemr.repositories.AMRSDepressionScreeningRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("amrsdepressionscreeningservice")
public class AMRSDepressionScreeningService {
    private final AMRSDepressionScreeningRepository amrsDepressionScreeningRepository;

    @Autowired
    public AMRSDepressionScreeningService(AMRSDepressionScreeningRepository amrsDepressionScreeningRepository) {
        this.amrsDepressionScreeningRepository = amrsDepressionScreeningRepository;
    }

    public List<AMRSDepressionScreening> findall() {
        return amrsDepressionScreeningRepository.findAll();
    }


    public List<AMRSDepressionScreening> findByResponseCodeIsNull() {
        return amrsDepressionScreeningRepository.findByResponseCodeIsNull();
    }

    public List<AMRSDepressionScreening> findByEncounterId(String encounterId) {
        return amrsDepressionScreeningRepository.findByEncounterId(encounterId);
    }

    public List<AMRSDepressionScreening> findByVisitId(String visitId) {
        return amrsDepressionScreeningRepository.findByVisitId(visitId);
    }

    public AMRSDepressionScreening save(AMRSDepressionScreening AMRSDepressionScreening) {
        return amrsDepressionScreeningRepository.save(AMRSDepressionScreening);
    }

    public List<AMRSDepressionScreening> findByEncounterConceptAndPatient(String encounterId, String conceptId, String patientId) {
        return amrsDepressionScreeningRepository.findByEncounterIdAndConceptIdAndPatientId(encounterId, conceptId, patientId);
    }
}
