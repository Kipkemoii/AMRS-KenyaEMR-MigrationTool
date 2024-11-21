package ampath.co.ke.amrs_kenyaemr.service;


import ampath.co.ke.amrs_kenyaemr.models.AMRSEncountersMapping;
import ampath.co.ke.amrs_kenyaemr.repositories.AMRSEncounterMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("encounterService")
public class AMRSEncounterMappingService {

    private AMRSEncounterMappingRepository amrsEncounterMappingRepository;
    @Autowired
    public AMRSEncounterMappingService(AMRSEncounterMappingRepository amrsEncounterMappingRepository){
        this.amrsEncounterMappingRepository=amrsEncounterMappingRepository;
    }

    public AMRSEncountersMapping save(AMRSEncountersMapping dataset){
        return amrsEncounterMappingRepository.save(dataset);
    }

    public List<AMRSEncountersMapping> getByAmrsID(int id){
        return amrsEncounterMappingRepository.findByAMRSEncounterTypeID(id);
    }

    public AMRSEncountersMapping getById(int id){
        return amrsEncounterMappingRepository.findById(id);
    }

    public List<AMRSEncountersMapping> getByKenyaemrID(int id){
        return amrsEncounterMappingRepository.findByKenyaEMREncounterTypeID(id);
    }

    public List<AMRSEncountersMapping> getByKenyaemrUUID(String uuid){
        return amrsEncounterMappingRepository.findByKenyaEMREncounterTypeUUID(uuid);
    }
}
