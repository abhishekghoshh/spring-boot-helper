package com.commercemesh.auth.controller;

import com.commercemesh.auth.dto.AuthResponse;
import com.commercemesh.auth.dto.LoginRequest;
import com.commercemesh.auth.dto.RefreshTokenRequest;
import com.commercemesh.auth.dto.RegisterRequest;
import com.commercemesh.auth.exception.GlobalExceptionHandler;
import com.commercemesh.auth.security.JwtAuthenticationFilter;
import com.commercemesh.auth.security.SecurityConfig;
import com.commercemesh.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                        classes = {SecurityConfig.class, JwtAuthenticationFilter.class})
        })
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private final AuthResponse authResponse = AuthResponse.of(
            "access-token-abc", "refresh-token-xyz", 3600, "testuser", Set.of("CUSTOMER"));

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("should return 200 and AuthResponse when login is successful")
        void loginWithValidBodyReturns200() throws Exception {
            LoginRequest request = new LoginRequest("testuser", "password123");
            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token-abc"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token-xyz"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
        }

        @Test
        @DisplayName("should return 400 when username is blank")
        void loginWithBlankUsernameReturns400() throws Exception {
            String invalidBody = "{\"username\":\"\",\"password\":\"password123\"}";

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when password is blank")
        void loginWithBlankPasswordReturns400() throws Exception {
            String invalidBody = "{\"username\":\"testuser\",\"password\":\"\"}";

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when request body is empty")
        void loginWithEmptyBodyReturns400() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("should return 201 and AuthResponse when registration is successful")
        void registerValidReturns201() throws Exception {
            RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password123", "New User");
            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").value("access-token-abc"))
                    .andExpect(jsonPath("$.username").value("testuser"));
        }

        @Test
        @DisplayName("should return 400 when username is too short")
        void registerWithShortUsernameReturns400() throws Exception {
            String invalidBody = """
                    {"username":"ab","email":"test@example.com","password":"password123"}""";

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when email is invalid")
        void registerWithInvalidEmailReturns400() throws Exception {
            String invalidBody = """
                    {"username":"newuser","email":"not-an-email","password":"password123"}""";

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when password is too short")
        void registerWithShortPasswordReturns400() throws Exception {
            String invalidBody = """
                    {"username":"newuser","email":"test@example.com","password":"short"}""";

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when required fields are missing")
        void registerWithMissingFieldsReturns400() throws Exception {
            String invalidBody = """
                    {"username":"newuser"}""";

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh-token")
    class RefreshTokenTests {

        @Test
        @DisplayName("should return 200 and new tokens when refresh token is valid")
        void refreshTokenValidReturns200() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
            when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post("/api/v1/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token-abc"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token-xyz"));
        }

        @Test
        @DisplayName("should return 400 when refresh token is blank")
        void refreshTokenBlankReturns400() throws Exception {
            String invalidBody = "{\"refreshToken\":\"\"}";

            mockMvc.perform(post("/api/v1/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/verify-email")
    class VerifyEmailTests {

        @Test
        @DisplayName("should return 200 when verification token is provided")
        void verifyEmailWithTokenReturns200() throws Exception {
            mockMvc.perform(get("/api/v1/auth/verify-email")
                            .param("token", "valid-verification-token"))
                    .andExpect(status().isOk());

            verify(authService).verifyEmail("valid-verification-token");
        }

        @Test
        @DisplayName("should return 400 when token is missing")
        void verifyEmailWithoutTokenReturns400() throws Exception {
            mockMvc.perform(get("/api/v1/auth/verify-email"))
                    .andExpect(status().isBadRequest());
        }
    }
}
