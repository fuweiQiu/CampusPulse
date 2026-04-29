package com.campuspulse.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
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
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FhirResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FhirResourceService.class);

    private static final String OBSERVATION_CATEGORY_SYSTEM = "http://terminology.hl7.org/CodeSystem/observation-category";
    private static final String DATA_ABSENT_REASON_SYSTEM = "http://terminology.hl7.org/CodeSystem/data-absent-reason";
    private static final String CAMPUSPULSE_SYSTEM = "https://campuspulse.example/fhir/CodeSystem/student-wellbeing";
    private static final String CAMPUSPULSE_IDENTIFIER_SYSTEM = "https://campuspulse.example/fhir/NamingSystem/student-identifier";
    private static final String UCUM_SYSTEM = "http://unitsofmeasure.org";

    private final FhirContext fhirContext;
    private final ObjectMapper objectMapper;
    private final ObservationRecordRepository observationRecordRepository;
    private final boolean remoteSyncEnabled;
    private final boolean failOnCloudSyncError;
    private final String remoteBaseUrl;
    private final Duration readTimeout;
    private final HttpClient httpClient;

    public FhirResourceService(
            FhirContext fhirContext,
            ObjectMapper objectMapper,
            ObservationRecordRepository observationRecordRepository,
            @Value("${app.fhir.remote.enabled:true}") boolean remoteSyncEnabled,
            @Value("${app.fhir.remote.fail-on-sync-error:false}") boolean failOnCloudSyncError,
            @Value("${app.fhir.remote.base-url:https://hapi.fhir.org/baseR4}") String remoteBaseUrl,
            @Value("${app.fhir.remote.connect-timeout-seconds:10}") long connectTimeoutSeconds,
            @Value("${app.fhir.remote.read-timeout-seconds:20}") long readTimeoutSeconds
    ) {
        this.fhirContext = fhirContext;
        this.objectMapper = objectMapper;
        this.observationRecordRepository = observationRecordRepository;
        this.remoteSyncEnabled = remoteSyncEnabled;
        this.failOnCloudSyncError = failOnCloudSyncError;
        this.remoteBaseUrl = normalizeBaseUrl(remoteBaseUrl);
        this.readTimeout = Duration.ofSeconds(readTimeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();
    }

    public void syncPatientResource(User user) {
        Patient patient = buildPatient(user);

        if (!remoteSyncEnabled) {
            persistLocalPatient(user, patient);
            return;
        }

        try {
            Patient syncedPatient;
            if (user.getPatientFhirId() == null || user.getPatientFhirId().isBlank()) {
                syncedPatient = createRemote("Patient", patient, Patient.class);
            } else {
                patient.setId(user.getPatientFhirId());
                syncedPatient = updateRemote("Patient", user.getPatientFhirId(), patient, Patient.class);
            }
            applySyncedPatient(user, syncedPatient);
        } catch (Exception exception) {
            handleCloudSyncFailure("patient", exception);
            persistLocalPatient(user, patient);
        }
    }

    public ObservationRecord createObservationRecord(User user, ChatSession session, String suggestion) {
        LocalDateTime now = LocalDateTime.now();
        Observation observation = buildObservation(user, session, suggestion, now);

        Observation persistedObservation = observation;
        String resourceUrl = null;

        if (remoteSyncEnabled && user.getPatientResourceUrl() != null && !user.getPatientResourceUrl().isBlank()) {
            try {
                persistedObservation = createRemote("Observation", observation, Observation.class);
                resourceUrl = resourceUrl("Observation", persistedObservation.getIdElement().getIdPart());
            } catch (Exception exception) {
                handleCloudSyncFailure("observation", exception);
                persistedObservation = persistLocalObservation(observation);
            }
        } else {
            persistedObservation = persistLocalObservation(observation);
        }

        ObservationRecord record = new ObservationRecord();
        record.setUser(user);
        record.setFhirId(persistedObservation.getIdElement().getIdPart());
        record.setResourceUrl(resourceUrl);
        record.setStatus(persistedObservation.getStatus().toCode());
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
        record.setResourceJson(encode(persistedObservation));
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

    private Patient buildPatient(User user) {
        Patient patient = new Patient();
        if (user.getPatientFhirId() != null && !user.getPatientFhirId().isBlank()) {
            patient.setId(user.getPatientFhirId());
        }
        patient.setActive(true);
        patient.addIdentifier()
                .setSystem(CAMPUSPULSE_IDENTIFIER_SYSTEM)
                .setValue(user.getUsername());
        patient.addName()
                .setText(patientDisplay(user))
                .setFamily(user.getDisplayName() == null || user.getDisplayName().isBlank() ? user.getUsername() : user.getDisplayName());

        if (user.getBirthDate() != null) {
            patient.setBirthDate(Date.valueOf(user.getBirthDate()));
        }
        if (user.getGender() != null && !user.getGender().isBlank()) {
            patient.setGender(parseGender(user.getGender()));
        } else {
            patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
        }
        return patient;
    }

    private void persistLocalPatient(User user, Patient patient) {
        if (user.getPatientFhirId() == null || user.getPatientFhirId().isBlank()) {
            user.setPatientFhirId(UUID.randomUUID().toString());
        }
        patient.setId(user.getPatientFhirId());
        user.setPatientResourceJson(encode(patient));
    }

    private void applySyncedPatient(User user, Patient patient) {
        user.setPatientFhirId(patient.getIdElement().getIdPart());
        user.setPatientResourceUrl(resourceUrl("Patient", patient.getIdElement().getIdPart()));
        user.setPatientResourceJson(encode(patient));
    }

    private Observation buildObservation(User user, ChatSession session, String suggestion, LocalDateTime now) {
        Observation observation = new Observation();
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
        return observation;
    }

    private Observation persistLocalObservation(Observation observation) {
        if (!observation.hasId()) {
            observation.setId(UUID.randomUUID().toString());
        }
        return observation;
    }

    private <T extends Resource> T createRemote(String resourceType, T resource, Class<T> responseType) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(remoteBaseUrl + "/" + resourceType))
                .header("Accept", "application/fhir+json")
                .header("Content-Type", "application/fhir+json; charset=UTF-8")
                .header("Prefer", "return=representation")
                .timeout(readTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(encode(resource), StandardCharsets.UTF_8))
                .build();
        return sendRemote(request, responseType, resource);
    }

    private <T extends Resource> T updateRemote(String resourceType, String resourceId, T resource, Class<T> responseType) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(remoteBaseUrl + "/" + resourceType + "/" + resourceId))
                .header("Accept", "application/fhir+json")
                .header("Content-Type", "application/fhir+json; charset=UTF-8")
                .header("Prefer", "return=representation")
                .timeout(readTimeout)
                .PUT(HttpRequest.BodyPublishers.ofString(encode(resource), StandardCharsets.UTF_8))
                .build();
        return sendRemote(request, responseType, resource);
    }

    private <T extends Resource> T sendRemote(HttpRequest request, Class<T> responseType, T fallbackResource) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("FHIR server returned HTTP " + response.statusCode() + ": " + response.body());
        }

        if (response.body() != null && !response.body().isBlank()) {
            return responseType.cast(fhirContext.newJsonParser().parseResource(responseType, response.body()));
        }

        String location = response.headers().firstValue("Location").orElse(null);
        if (location != null && !location.isBlank()) {
            fallbackResource.setId(new IdType(location).getIdPart());
        }
        return fallbackResource;
    }

    private void handleCloudSyncFailure(String resourceType, Exception exception) {
        if (failOnCloudSyncError) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Unable to sync " + resourceType + " to cloud FHIR server");
        }
        LOGGER.warn("Unable to sync {} to remote FHIR server {}: {}", resourceType, remoteBaseUrl, exception.getMessage());
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
        return switch (gender.toLowerCase(Locale.ROOT)) {
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

    private String resourceUrl(String resourceType, String resourceId) {
        return remoteBaseUrl + "/" + resourceType + "/" + resourceId;
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "https://hapi.fhir.org/baseR4";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
