package ampath.co.ke.amrs_kenyaemr.models;

import jakarta.persistence.*;

@Entity
@Table(name = "amrs_depression_screening")
public class AMRSDepressionScreening {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;
    private String patientId;

    private String formId;
    private String kenyaemrFormUuid;
    private String kenyaemrEncounterTypeUuid;

    private String conceptId;
    private String conceptDataTypeId;

    private String encounterId;
    private String visitId;
    private String kenyaemrVisitUuid;
    private String value;
    private String obsDateTime;
    private String responseCode;

    private String kenyaemrPatientUuid;

    private String kenyaEmrConceptUuid;
    private String kenyaEmrValue;
    private String Question;
    private String kenyaEmrEncounterDateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getKenyaemrFormUuid() {
        return kenyaemrFormUuid;
    }

    public void setKenyaemrFormUuid(String kenyaemrFormUuid) {
        this.kenyaemrFormUuid = kenyaemrFormUuid;
    }

    public String getKenyaemrEncounterTypeUuid() {
        return kenyaemrEncounterTypeUuid;
    }

    public void setKenyaemrEncounterTypeUuid(String kenyaemrEncounterTypeUuid) {
        this.kenyaemrEncounterTypeUuid = kenyaemrEncounterTypeUuid;
    }

    public String getConceptId() {
        return conceptId;
    }

    public void setConceptId(String conceptId) {
        this.conceptId = conceptId;
    }

    public String getConceptDataTypeId() {
        return conceptDataTypeId;
    }

    public void setConceptDataTypeId(String conceptDataTypeId) {
        this.conceptDataTypeId = conceptDataTypeId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public String getVisitId() {
        return visitId;
    }

    public void setVisitId(String visitId) {
        this.visitId = visitId;
    }

    public String getKenyaemrVisitUuid() {
        return kenyaemrVisitUuid;
    }

    public void setKenyaemrVisitUuid(String kenyaemrVisitUuid) {
        this.kenyaemrVisitUuid = kenyaemrVisitUuid;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getObsDateTime() {
        return obsDateTime;
    }

    public void setObsDateTime(String obsDateTime) {
        this.obsDateTime = obsDateTime;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getKenyaemrPatientUuid() {
        return kenyaemrPatientUuid;
    }

    public void setKenyaemrPatientUuid(String kenyaemrPatientUuid) {
        this.kenyaemrPatientUuid = kenyaemrPatientUuid;
    }

    public String getKenyaEmrConceptUuid() {
        return kenyaEmrConceptUuid;
    }

    public void setKenyaEmrConceptUuid(String kenyaEmrConceptUuid) {
        this.kenyaEmrConceptUuid = kenyaEmrConceptUuid;
    }

    public String getKenyaEmrValue() {
        return kenyaEmrValue;
    }

    public void setKenyaEmrValue(String kenyaEmrValue) {
        this.kenyaEmrValue = kenyaEmrValue;
    }

    public String getQuestion() {
        return Question;
    }

    public void setQuestion(String question) {
        Question = question;
    }

    public String getKenyaEmrEncounterDateTime() {
        return kenyaEmrEncounterDateTime;
    }

    public void setKenyaEmrEncounterDateTime(String kenyaEmrEncounterDateTime) {
        this.kenyaEmrEncounterDateTime = kenyaEmrEncounterDateTime;
    }
}
