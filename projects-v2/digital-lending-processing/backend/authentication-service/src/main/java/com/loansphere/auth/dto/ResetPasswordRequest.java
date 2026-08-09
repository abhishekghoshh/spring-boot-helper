package com.loansphere.auth.dto;

public record ResetPasswordRequest(String token, String newPassword) {}
