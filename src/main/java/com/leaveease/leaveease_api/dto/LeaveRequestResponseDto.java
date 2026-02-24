package com.leaveease.leaveease_api.dto;

import com.leaveease.leaveease_api.entity.LeaveStatus;
import com.leaveease.leaveease_api.entity.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class LeaveRequestResponseDto {

    private Long id;
    private String username;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private LeaveStatus status;
    private String approvedByUsername;
    private LocalDateTime createdAt;
}
