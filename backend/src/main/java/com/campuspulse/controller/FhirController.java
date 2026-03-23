package com.campuspulse.controller;

import java.util.List;

import com.campuspulse.model.User;
import com.campuspulse.service.AuthService;
import com.campuspulse.service.FhirResourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fhir")
public class FhirController {

    private final AuthService authService;
    private final FhirResourceService fhirResourceService;

    public FhirController(AuthService authService, FhirResourceService fhirResourceService) {
        this.authService = authService;
        this.fhirResourceService = fhirResourceService;
    }

    @GetMapping("/patient")
    public Object patient(@RequestParam String token) {
        User user = authService.authenticate(token);
        return fhirResourceService.patientResource(user);
    }

    @GetMapping("/observations")
    public List<Object> observations(@RequestParam String token) {
        User user = authService.authenticate(token);
        return fhirResourceService.observationResources(user);
    }

    @GetMapping("/bundle")
    public Object bundle(@RequestParam String token) {
        User user = authService.authenticate(token);
        return fhirResourceService.exportBundle(user);
    }
}
