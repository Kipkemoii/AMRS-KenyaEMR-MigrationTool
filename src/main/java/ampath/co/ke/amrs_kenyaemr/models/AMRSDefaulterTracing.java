package ampath.co.ke.amrs_kenyaemr.models;

import jakarta.persistence.*;


@Table(name = "defaulter_tracing")
@Entity
public class AMRSDefaulterTracing {

    @jakarta.persistence.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer Id;
    private String patientId;
    private String encounterId;
    private String formId;
    private String question;
    private String conceptId;
    private String value;
    private String kenyaEmrValueUuid;
    private String kenyaEmrConceptUuid;
    private String kenyaEmrEncounterUuid;
    private String kenyaEmrPatientUuid;
    private String kenyaEmrFormUuid;
    private String responseCode;

    public Integer getId() {
        return Id;
    }

    public void setId(Integer id) {
        Id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getConceptId() {
        return conceptId;
    }

    public void setConceptId(String conceptId) {
        this.conceptId = conceptId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getKenyaEmrValueUuid() {
        return kenyaEmrValueUuid;
    }

    public void setKenyaEmrValueUuid(String kenyaEmrValueUuid) {
        this.kenyaEmrValueUuid = kenyaEmrValueUuid;
    }

    public String getKenyaEmrEncounterUuid() {
        return kenyaEmrEncounterUuid;
    }

    public void setKenyaEmrEncounterUuid(String kenyaEmrEncounterUuid) {
        this.kenyaEmrEncounterUuid = kenyaEmrEncounterUuid;
    }

    public String getKenyaEmrConceptUuid() {
        return kenyaEmrConceptUuid;
    }

    public void setKenyaEmrConceptUuid(String kenyaEmrConceptUuid) {
        this.kenyaEmrConceptUuid = kenyaEmrConceptUuid;
    }

    public String getKenyaEmrPatientUuid() {
        return kenyaEmrPatientUuid;
    }

    public void setKenyaEmrPatientUuid(String kenyaEmrPatientUuid) {
        this.kenyaEmrPatientUuid = kenyaEmrPatientUuid;
    }

    public String getKenyaEmrFormUuid() {
        return kenyaEmrFormUuid;
    }

    public void setKenyaEmrFormUuid(String kenyaEmrFormUuid) {
        this.kenyaEmrFormUuid = kenyaEmrFormUuid;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }
}