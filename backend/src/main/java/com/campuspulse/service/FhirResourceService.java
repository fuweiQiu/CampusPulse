package com.campuspulse.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.campuspulse.model.ChatSession;
import com.campuspulse.model.ObservationRecord;
import com.campuspulse.model.User;
import com.campuspulse.repository.ObservationRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FhirResourceService {

    private static final String OBSERVATION_CATEGORY_SYSTEM = "http://terminology.hl7.org/CodeSystem/observation-category";
    private static final String DATA_ABSENT_REASON_SYSTEM = "http://terminology.hl7.org/CodeSystem/data-absent-reason";
    private static final String CAMPUSPULSE_SYSTEM = "https://campuspulse.example/fhir/CodeSystem/student-wellbeing";
    private static final String CAMPUSPULSE_IDENTIFIER_SYSTEM = "https://campuspulse.example/fhir/NamingSystem/student-identifier";
    private static final String UCUM_SYSTEM = "http://unitsofmeasure.org";

    private final FhirContext fhirContext;
    private final ObjectMapper objectMapper;
    private final ObservationRecordRepository observationRecordRepository;

    public FhirResourceService(
            FhirContext fhirContext,
            ObjectMapper objectMapper,
            ObservationRecordRepository observationRecordRepository
    ) {
        this.fhirContext = fhirContext;
        this.objectMapper = objectMapper;
        this.observationRecordRepository = observationRecordRepository;
    }

    public void syncPatientResource(User user) {
        if (user.getPatientFhirId() == null || user.getPatientFhirId().isBlank()) {
            user.setPatientFhirId(UUID.randomUUID().toString());
        }

        Patient patient = new Patient();
        patient.setId(user.getPatientFhirId());
        patient.setActive(true);
        patient.addIdentifier()
                .setSystem(CAMPUSPULSE_IDENTIFIER_SYSTEM)
                .setValue(user.getUsername());
        patient.addName().setText(patientDisplay(user));

        if (user.getBirthDate() != null) {
            patient.setBirthDate(Date.valueOf(user.getBirthDate()));
        }
        if (user.getGender() != null && !user.getGender().isBlank()) {
            patient.setGender(parseGender(user.getGender()));
        } else {
            patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
        }

        user.setPatientResourceJson(encode(patient));
    }

    public ObservationRecord createObservationRecord(User user, ChatSession session, String suggestion) {
        LocalDateTime now = LocalDateTime.now();
        Observation observation = new Observation();
        observation.setId(UUID.randomUUID().toString());
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.addCategory(codeable(OBSERVATION_CATEGORY_SYSTEM, "survey", "Survey"));
        observation.setCode(codeable(CAMPUSPULSE_SYSTEM, "student-wellbeing-panel", "Student Wellbeing Panel"));
        observation.setSubject(new Reference("Patient/" + user.getPatientFhirId()).setDisplay(patientDisplay(user)));
        observation.setEffective(new DateTimeType(toDate(now)));
        observation.setIssued(toDate(now));
        observation.addPerformer(new Reference("Patient/" + user.getPatientFhirId()).setDisplay(patientDisplay(user)));
        observation.setMethod(codeable(CAMPUSPULSE_SYSTEM, "self-reported-chat", "Self-reported conversational intake"));
        observation.addNote(new Annotation().setText("Source narrative: " + Optional.ofNullable(session.getSourceText()).orElse("")));
        observation.addNote(new Annotation().setText("CampusPulse assistant summary: " + suggestion));

        Observation.ObservationComponentComponent stressComponent = new Observation.ObservationComponentComponent()
                .setCode(codeable(CAMPUSPULSE_SYSTEM, "stress-score", "Perceived Stress Score"));
        if (session.getStressScore() != null) {
            stressComponent.setValue(new IntegerType(session.getStressScore()));
        } else if (session.getStressAbsentReasonCode() != null) {
            stressComponent.setDataAbsentReason(codeable(DATA_ABSENT_REASON_SYSTEM, session.getStressAbsentReasonCode(), "Unknown"));
        }
        observation.addComponent(stressComponent);

        Observation.ObservationComponentComponent sleepComponent = new Observation.ObservationComponentComponent()
                .setCode(codeable(CAMPUSPULSE_SYSTEM, "sleep-duration-hours", "Sleep Duration"));
        if (session.getSleepHours() != null) {
            sleepComponent.setValue(new Quantity()
                    .setValue(BigDecimal.valueOf(session.getSleepHours()))
                    .setUnit("h")
                    .setSystem(UCUM_SYSTEM)
                    .setCode("h"));
        } else if (session.getSleepAbsentReasonCode() != null) {
            sleepComponent.setDataAbsentReason(codeable(DATA_ABSENT_REASON_SYSTEM, session.getSleepAbsentReasonCode(), "Unknown"));
        }
        observation.addComponent(sleepComponent);

        Observation.ObservationComponentComponent emotionComponent = new Observation.ObservationComponentComponent()
                .setCode(codeable(CAMPUSPULSE_SYSTEM, "self-reported-emotion", "Self-reported Emotion"));
        if (session.getEmotionCode() != null) {
            emotionComponent.setValue(codeable(CAMPUSPULSE_SYSTEM, session.getEmotionCode(), session.getEmotionDisplay()));
        } else if (session.getEmotionAbsentReasonCode() != null) {
            emotionComponent.setDataAbsentReason(codeable(DATA_ABSENT_REASON_SYSTEM, session.getEmotionAbsentReasonCode(), "Unknown"));
        }
        observation.addComponent(emotionComponent);

        ObservationRecord record = new ObservationRecord();
        record.setUser(user);
        record.setFhirId(observation.getIdElement().getIdPart());
        record.setStatus(observation.getStatus().toCode());
        record.setCategoryCode("survey");
        record.setCodeSystem(CAMPUSPULSE_SYSTEM);
        record.setCodeValue("student-wellbeing-panel");
        record.setCodeDisplay("Student Wellbeing Panel");
        record.setEffectiveDateTime(now);
        record.setIssuedAt(now);
        record.setStressScore(session.getStressScore());
        record.setSleepHours(session.getSleepHours());
        record.setStressAbsentReasonCode(session.getStressAbsentReasonCode());
        record.setSleepAbsentReasonCode(session.getSleepAbsentReasonCode());
        record.setEmotionAbsentReasonCode(session.getEmotionAbsentReasonCode());
        record.setEmotionCode(session.getEmotionCode());
        record.setEmotionDisplay(session.getEmotionDisplay());
        record.setSourceText(session.getSourceText());
        record.setSuggestion(suggestion);
        record.setResourceJson(encode(observation));
        return observationRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public Object patientResource(User user) {
        return readJson(user.getPatientResourceJson());
    }

    @Transactional(readOnly = true)
    public List<Object> observationResources(User user) {
        return observationRecordRepository.findByUserOrderByEffectiveDateTimeAsc(user)
                .stream()
                .map(record -> readJson(record.getResourceJson()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Object exportBundle(User user) {
        List<ObservationRecord> records = observationRecordRepository.findByUserOrderByEffectiveDateTimeAsc(user);

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.addEntry().setResource(parsePatient(user.getPatientResourceJson()));
        for (ObservationRecord record : records) {
            bundle.addEntry().setResource(parseObservation(record.getResourceJson()));
        }
        return readJson(encode(bundle));
    }

    public Object readJson(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read FHIR resource JSON", exception);
        }
    }

    private String encode(org.hl7.fhir.instance.model.api.IBaseResource resource) {
        IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
        return parser.encodeResourceToString(resource);
    }

    private Patient parsePatient(String json) {
        return (Patient) fhirContext.newJsonParser().parseResource(json);
    }

    private Observation parseObservation(String json) {
        return (Observation) fhirContext.newJsonParser().parseResource(json);
    }

    private CodeableConcept codeable(String system, String code, String display) {
        return new CodeableConcept().addCoding(new Coding(system, code, display)).setText(display);
    }

    private Enumerations.AdministrativeGender parseGender(String gender) {
        return switch (gender.toLowerCase(java.util.Locale.ROOT)) {
            case "male" -> Enumerations.AdministrativeGender.MALE;
            case "female" -> Enumerations.AdministrativeGender.FEMALE;
            case "other" -> Enumerations.AdministrativeGender.OTHER;
            default -> Enumerations.AdministrativeGender.UNKNOWN;
        };
    }

    private java.util.Date toDate(LocalDateTime dateTime) {
        return java.util.Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private String patientDisplay(User user) {
        return user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? user.getUsername()
                : user.getDisplayName();
    }
}
