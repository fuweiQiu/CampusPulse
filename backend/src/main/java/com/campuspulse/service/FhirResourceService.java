package com.campuspulse.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
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
        Patient patient = fetchPreferredPatient(user);
        return readJson(encode(patient));
    }

    @Transactional(readOnly = true)
    public List<Object> observationResources(User user) {
        return fetchPreferredObservations(user)
                .stream()
                .map(this::encode)
                .map(this::readJson)
                .toList();
    }

    @Transactional(readOnly = true)
    public Object exportBundle(User user) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.addEntry().setResource(fetchPreferredPatient(user));
        for (Observation observation : fetchPreferredObservations(user)) {
            bundle.addEntry().setResource(observation);
        }
        return readJson(encode(bundle));
    }

    @Transactional(readOnly = true)
    public List<ObservationRecord> preferredObservationRecords(User user, LocalDateTime start, LocalDateTime end) {
        Optional<List<ObservationRecord>> remoteRecords = tryFetchRemoteObservationRecords(user);
        if (remoteRecords.isPresent()) {
            return remoteRecords.get().stream()
                    .filter(record -> !record.getEffectiveDateTime().isBefore(start) && !record.getEffectiveDateTime().isAfter(end))
                    .sorted(Comparator.comparing(ObservationRecord::getEffectiveDateTime))
                    .toList();
        }
        return observationRecordRepository.findByUserAndEffectiveDateTimeBetweenOrderByEffectiveDateTimeAsc(user, start, end);
    }

    @Transactional(readOnly = true)
    public ObservationRecord preferredLatestObservationRecord(User user) {
        Optional<List<ObservationRecord>> remoteRecords = tryFetchRemoteObservationRecords(user);
        if (remoteRecords.isPresent()) {
            return remoteRecords.get().stream()
                    .max(Comparator.comparing(ObservationRecord::getEffectiveDateTime))
                    .orElse(null);
        }
        return observationRecordRepository.findTopByUserOrderByEffectiveDateTimeDesc(user).orElse(null);
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

    private Patient fetchPreferredPatient(User user) {
        Optional<Patient> remotePatient = tryFetchRemotePatient(user);
        return remotePatient.orElseGet(() -> parsePatient(user.getPatientResourceJson()));
    }

    private List<Observation> fetchPreferredObservations(User user) {
        Optional<List<Observation>> remoteObservations = tryFetchRemoteObservations(user);
        if (remoteObservations.isPresent()) {
            return remoteObservations.get();
        }
        return observationRecordRepository.findByUserOrderByEffectiveDateTimeAsc(user)
                .stream()
                .map(record -> parseObservation(record.getResourceJson()))
                .toList();
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

    private Optional<Patient> tryFetchRemotePatient(User user) {
        if (!remoteSyncEnabled || user.getPatientFhirId() == null || user.getPatientFhirId().isBlank()) {
            return Optional.empty();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resourceUrl("Patient", user.getPatientFhirId())))
                    .header("Accept", "application/fhir+json")
                    .timeout(readTimeout)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null || response.body().isBlank()) {
                throw new IllegalStateException("FHIR server returned HTTP " + response.statusCode());
            }
            return Optional.of((Patient) fhirContext.newJsonParser().parseResource(response.body()));
        } catch (Exception exception) {
            LOGGER.warn("Unable to fetch patient from remote FHIR server {}: {}. Falling back to local cache.", remoteBaseUrl, exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<List<Observation>> tryFetchRemoteObservations(User user) {
        if (!remoteSyncEnabled || user.getPatientFhirId() == null || user.getPatientFhirId().isBlank()) {
            return Optional.empty();
        }

        try {
            List<Observation> observations = new ArrayList<>();
            String nextUrl = remoteBaseUrl
                    + "/Observation?subject="
                    + URLEncoder.encode("Patient/" + user.getPatientFhirId(), StandardCharsets.UTF_8)
                    + "&_sort=date&_count=200";

            for (int page = 0; page < 10 && nextUrl != null; page++) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(nextUrl))
                        .header("Accept", "application/fhir+json")
                        .timeout(readTimeout)
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null || response.body().isBlank()) {
                    throw new IllegalStateException("FHIR server returned HTTP " + response.statusCode());
                }

                Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(response.body());
                bundle.getEntry().stream()
                        .map(Bundle.BundleEntryComponent::getResource)
                        .filter(Observation.class::isInstance)
                        .map(Observation.class::cast)
                        .forEach(observations::add);

                nextUrl = bundle.getLink().stream()
                        .filter(link -> "next".equals(link.getRelation()))
                        .map(link -> link.getUrl())
                        .findFirst()
                        .orElse(null);
            }

            observations.sort(Comparator.comparing(this::observationDateTime));
            return Optional.of(observations);
        } catch (Exception exception) {
            LOGGER.warn("Unable to fetch observations from remote FHIR server {}: {}. Falling back to local cache.", remoteBaseUrl, exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<List<ObservationRecord>> tryFetchRemoteObservationRecords(User user) {
        Optional<List<Observation>> remoteObservations = tryFetchRemoteObservations(user);
        return remoteObservations.map(observations -> observations.stream()
                .map(this::toObservationRecordSnapshot)
                .toList());
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

    private ObservationRecord toObservationRecordSnapshot(Observation observation) {
        ObservationRecord record = new ObservationRecord();
        record.setFhirId(observation.getIdElement().getIdPart());
        record.setResourceUrl(resourceUrl("Observation", observation.getIdElement().getIdPart()));
        record.setStatus(observation.getStatus() == null ? null : observation.getStatus().toCode());

        Coding categoryCoding = observation.getCategoryFirstRep().getCodingFirstRep();
        record.setCategoryCode(categoryCoding == null ? null : categoryCoding.getCode());

        Coding mainCoding = observation.getCode().getCodingFirstRep();
        if (mainCoding != null) {
            record.setCodeSystem(mainCoding.getSystem());
            record.setCodeValue(mainCoding.getCode());
            record.setCodeDisplay(mainCoding.getDisplay());
        }

        LocalDateTime effectiveDateTime = observationDateTime(observation);
        record.setEffectiveDateTime(effectiveDateTime);
        record.setIssuedAt(observation.getIssued() == null ? effectiveDateTime : toLocalDateTime(observation.getIssued().toInstant()));

        for (Observation.ObservationComponentComponent component : observation.getComponent()) {
            String componentCode = component.getCode().getCodingFirstRep().getCode();
            if ("stress-score".equals(componentCode)) {
                if (component.hasValueIntegerType()) {
                    record.setStressScore(component.getValueIntegerType().getValue());
                } else if (component.hasDataAbsentReason()) {
                    record.setStressAbsentReasonCode(component.getDataAbsentReason().getCodingFirstRep().getCode());
                }
            } else if ("sleep-duration-hours".equals(componentCode)) {
                if (component.hasValueQuantity()) {
                    record.setSleepHours(component.getValueQuantity().getValue().doubleValue());
                } else if (component.hasDataAbsentReason()) {
                    record.setSleepAbsentReasonCode(component.getDataAbsentReason().getCodingFirstRep().getCode());
                }
            } else if ("self-reported-emotion".equals(componentCode)) {
                if (component.hasValueCodeableConcept()) {
                    CodeableConcept concept = component.getValueCodeableConcept();
                    record.setEmotionCode(concept.getCodingFirstRep().getCode());
                    record.setEmotionDisplay(concept.getCodingFirstRep().getDisplay());
                } else if (component.hasDataAbsentReason()) {
                    record.setEmotionAbsentReasonCode(component.getDataAbsentReason().getCodingFirstRep().getCode());
                }
            }
        }

        for (Annotation note : observation.getNote()) {
            if (note.getText() == null) {
                continue;
            }
            if (note.getText().startsWith("Source narrative: ")) {
                record.setSourceText(note.getText().substring("Source narrative: ".length()));
            } else if (note.getText().startsWith("CampusPulse assistant summary: ")) {
                record.setSuggestion(note.getText().substring("CampusPulse assistant summary: ".length()));
            }
        }

        record.setResourceJson(encode(observation));
        return record;
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

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private LocalDateTime observationDateTime(Observation observation) {
        if (observation.getEffectiveDateTimeType() != null && observation.getEffectiveDateTimeType().getValue() != null) {
            return toLocalDateTime(observation.getEffectiveDateTimeType().getValue().toInstant());
        }
        if (observation.getIssued() != null) {
            return toLocalDateTime(observation.getIssued().toInstant());
        }
        return LocalDateTime.now();
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
