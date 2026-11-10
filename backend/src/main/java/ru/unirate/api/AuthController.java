package ru.unirate.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.unirate.api.dto.AuthDtos;
import ru.unirate.api.dto.CommonDtos;
import ru.unirate.api.dto.ProfileDtos;
import ru.unirate.api.security.AuthUser;
import ru.unirate.application.AuthService;
import ru.unirate.application.ProfileService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final ProfileService profileService;

    public AuthController(AuthService authService, ProfileService profileService) {
        this.authService = authService;
        this.profileService = profileService;
    }

    @PostMapping("/register/student")
    public ResponseEntity<AuthDtos.AuthResponse> registerStudent(
            @Valid @RequestBody AuthDtos.RegisterStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerStudent(request));
    }

    @PostMapping("/register/teacher")
    public ResponseEntity<AuthDtos.AuthResponse> registerTeacher(
            @Valid @RequestBody AuthDtos.RegisterTeacherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerTeacher(request));
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/verify-email")
    public CommonDtos.MessageResponse verifyEmail(@Valid @RequestBody AuthDtos.VerifyEmailRequest request) {
        return authService.verifyEmail(request.token());
    }

    @PostMapping("/resend-verification")
    public CommonDtos.MessageResponse resendVerification(@AuthenticationPrincipal AuthUser user) {
        return authService.resendVerification(user);
    }

    @GetMapping("/me")
    public ProfileDtos.UserMe me(@AuthenticationPrincipal AuthUser user) {
        return profileService.userMe(user.id());
    }
}
