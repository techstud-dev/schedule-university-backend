package com.techstud.schedule_university.auth;

import com.techstud.schedule_university.auth.config.TokenProperties;
import com.techstud.schedule_university.auth.dto.request.LoginRecord;
import com.techstud.schedule_university.auth.dto.response.SuccessAuthenticationRecord;
import com.techstud.schedule_university.auth.entity.RefreshToken;
import com.techstud.schedule_university.auth.entity.User;
import com.techstud.schedule_university.auth.exception.BadCredentialsException;
import com.techstud.schedule_university.auth.exception.UserNotFoundException;
import com.techstud.schedule_university.auth.repository.UserRepository;
import com.techstud.schedule_university.auth.security.TokenService;
import com.techstud.schedule_university.auth.service.impl.LoginServiceImpl;
import jakarta.validation.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private TokenProperties jwtProperties;

    @InjectMocks
    private LoginServiceImpl loginService;

    @Test
    void processLogin_ShouldReturnSuccessResponse_WhenCredentialsAreValid() throws Exception {
        LoginRecord loginDto = new LoginRecord("username", "password");
        User user = new User();
        user.setPassword("hashedPassword");

        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(7200L);

        when(userRepository.findByUsernameIgnoreCase("username"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashedPassword"))
                .thenReturn(true);
        when(tokenService.generateTokens(eq(user)))
                .thenReturn(new SuccessAuthenticationRecord("access-token", "new-refresh-token"));

        SuccessAuthenticationRecord response = loginService.processLogin(loginDto);

        assertNotNull(response);
        assertEquals("access-token", response.token());
        assertEquals("new-refresh-token", response.refreshToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        RefreshToken savedRefreshToken = userCaptor.getValue().getRefreshToken();
        assertNotNull(savedRefreshToken);
        assertEquals("new-refresh-token", savedRefreshToken.getRefreshToken());

        assertTrue(savedRefreshToken.getExpiryDate().isAfter(Instant.now().plus(7190, ChronoUnit.SECONDS)));
        assertTrue(savedRefreshToken.getExpiryDate().isBefore(Instant.now().plus(7210, ChronoUnit.SECONDS)));

        verify(jwtProperties).getRefreshTokenExpiration();
    }

    @Test
    void loginWithNonExistentUser_ShouldThrowUserNotFoundException() {
        LoginRecord loginDto = new LoginRecord("unknown_user", "password123");

        when(userRepository.findByUsernameIgnoreCase(anyString()))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.empty());
        when(userRepository.findByPhoneNumber(anyString()))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> loginService.processLogin(loginDto));

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithInvalidPassword_ShouldThrowBadCredentialsException() {
        User mockUser = User.builder()
                .username("test_user")
                .password("correct_hash")
                .build();

        LoginRecord loginDto = new LoginRecord("test_user", "wrong_password");

        when(userRepository.findByUsernameIgnoreCase(anyString()))
                .thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> loginService.processLogin(loginDto));

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithEmptyIdentificationField_ShouldThrowException() {
        LoginRecord loginDto = new LoginRecord("", "password123");

        assertThrows(ConstraintViolationException.class,
                () -> validateLoginDTO(loginDto));
    }

    @Test
    void loginWithNullPassword_ShouldThrowValidationException() {
        LoginRecord loginDto = new LoginRecord("test_user", null);

        assertThrows(ConstraintViolationException.class,
                () -> validateLoginDTO(loginDto));
    }

    private void validateLoginDTO(LoginRecord dto) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<LoginRecord>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
