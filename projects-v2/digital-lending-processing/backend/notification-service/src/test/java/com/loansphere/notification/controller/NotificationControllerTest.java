package com.loansphere.notification.controller;

import com.loansphere.notification.model.Notification;
import com.loansphere.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.loansphere.notification.controller.NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    @WithMockUser
    void shouldGetUserNotifications() throws Exception {
        Notification n = new Notification();
        n.setId("n1"); n.setTitle("Test"); n.setMessage("Hello");
        n.setUserId("user1");

        when(notificationService.getUserNotifications("user1")).thenReturn(List.of(n));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    void shouldMarkNotificationAsRead() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/notif1/read"))
                .andExpect(status().isOk());

        verify(notificationService).markAsRead("notif1");
    }

    @Test
    void shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }
}
