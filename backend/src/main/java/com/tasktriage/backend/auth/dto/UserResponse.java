package com.tasktriage.backend.auth.dto;

import com.tasktriage.backend.user.UserRole;

public record UserResponse(Long id, String name, String email, UserRole role) {
}
