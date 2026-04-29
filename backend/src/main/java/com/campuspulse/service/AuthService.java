package com.campuspulse.service;

import com.campuspulse.dto.AuthResponse;
import com.campuspulse.dto.LoginRequest;
import com.campuspulse.dto.RegisterRequest;
import com.campuspulse.model.User;
import com.campuspulse.repository.UserRepository;
import com.campuspulse.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final FhirResourceService fhirResourceService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            FhirResourceService fhirResourceService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.fhirResourceService = fhirResourceService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAuthToken(jwtService.generateToken(username));
        user.setDisplayName(blankToNull(request.displayName()));
        user.setBirthDate(request.birthDate());
        user.setGender(blankToNull(request.gender()));
        userRepository.save(user);
        fhirResourceService.syncPatientResource(user);
        userRepository.save(user);

        return new AuthResponse(
                user.getUsername(),
                user.getDisplayName(),
                user.getAuthToken(),
                user.getPatientFhirId(),
                user.getPatientResourceUrl(),
                "Registration successful"
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        user.setAuthToken(jwtService.generateToken(username));
        fhirResourceService.syncPatientResource(user);
        userRepository.save(user);

        return new AuthResponse(
                user.getUsername(),
                user.getDisplayName(),
                user.getAuthToken(),
                user.getPatientFhirId(),
                user.getPatientResourceUrl(),
                "Login successful"
        );
    }

    @Transactional(readOnly = true)
    public User authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token is required");
        }

        try {
            String username = jwtService.extractUsername(token);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token"));

            if (user.getAuthToken() == null || !user.getAuthToken().equals(token) || !jwtService.isTokenValid(token, username)) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token");
            }

            return user;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
