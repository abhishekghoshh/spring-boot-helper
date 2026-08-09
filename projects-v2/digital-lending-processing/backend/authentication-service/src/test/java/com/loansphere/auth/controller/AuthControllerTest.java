package com.loansphere.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loansphere.auth.dto.LoginRequest;
import com.loansphere.auth.dto.RegisterRequest;
import com.loansphere.auth.dto.RefreshTokenRequest;
import com.loansphere.auth.model.User;
import com.loansphere.auth.service.AuthService;
import com.loansphere.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOps;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        var request = new RegisterRequest("newuser", "new@test.com", "Pass@123", "First", "Last");
        User user = new User();
        user.setId("id1"); user.setUsername("newuser"); user.setEmail("new@test.com");
        user.setFirstName("First"); user.setLastName("Last");
        user.setRoles(Set.of("CUSTOMER"));

        when(authService.register(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }

    @Test
    void shouldRejectInvalidRegistration() throws Exception {
        var request = new RegisterRequest("", "bad-email", "short", "", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        var request = new LoginRequest("testuser", "Pass@123");
        User user = new User();
        user.setId("id1"); user.setUsername("testuser");
        user.setPassword("encoded"); user.setRoles(Set.of("CUSTOMER"));

        when(authService.findByUsername("testuser")).thenReturn(user);
        when(passwordEncoder.matches("Pass@123", "encoded")).thenReturn(true);
        when(authService.generateAccessToken(user)).thenReturn("access-token");
        when(authService.generateRefreshToken(user)).thenReturn("refresh-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        var request = new LoginRequest("testuser", "wrong");
        User user = new User();
        user.setPassword("encoded");

        when(authService.findByUsername("testuser")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldLogoutSuccessfully() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {
        var request = new RefreshTokenRequest("valid-refresh-token");
        User user = new User();
        user.setId("id1"); user.setUsername("testuser");
        user.setRoles(Set.of("CUSTOMER"));

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("refresh:valid-refresh-token")).thenReturn("id1");
        when(authService.findById("id1")).thenReturn(user);
        when(authService.generateAccessToken(user)).thenReturn("new-access");
        when(authService.generateRefreshToken(user)).thenReturn("new-refresh");

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"));
    }
}
