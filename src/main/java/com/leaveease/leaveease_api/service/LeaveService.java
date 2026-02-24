package com.leaveease.leaveease_api.service;

import com.leaveease.leaveease_api.dto.LeaveRequestCreateDto;
import com.leaveease.leaveease_api.dto.LeaveRequestResponseDto;
import com.leaveease.leaveease_api.entity.LeaveRequest;
import com.leaveease.leaveease_api.entity.LeaveStatus;
import com.leaveease.leaveease_api.entity.User;
import com.leaveease.leaveease_api.exception.InvalidLeaveRequestException;
import com.leaveease.leaveease_api.exception.LeaveOverlapException;
import com.leaveease.leaveease_api.exception.ResourceNotFoundException;
import com.leaveease.leaveease_api.repository.LeaveRequestRepository;
import com.leaveease.leaveease_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;

    @Transactional
    public LeaveRequestResponseDto createLeave(LeaveRequestCreateDto dto, String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        if (!dto.getStartDate().isBefore(dto.getEndDate())) {
            throw new InvalidLeaveRequestException("Start date must be before end date");
        }

        boolean overlap = leaveRequestRepository.existsOverlap(
                user.getId(),
                dto.getStartDate(),
                dto.getEndDate(),
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED));

        if (overlap) {
            throw new LeaveOverlapException(
                    "You already have an approved or pending leave that overlaps with this date range");
        }

        LeaveRequest leave = LeaveRequest.builder()
                .user(user)
                .leaveType(dto.getLeaveType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .build();

        LeaveRequest saved = leaveRequestRepository.save(leave);
        return toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getMyLeaves(String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        return leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getAllLeaves() {
        return leaveRequestRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public LeaveRequestResponseDto updateStatus(Long id, LeaveStatus newStatus, String adminUsername) {
        LeaveRequest leave = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidLeaveRequestException(
                    "Only PENDING leave requests can be updated. Current status: " + leave.getStatus());
        }

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found: " + adminUsername));

        leave.setStatus(newStatus);
        leave.setApprovedBy(admin);

        LeaveRequest saved = leaveRequestRepository.save(leave);
        return toResponseDto(saved);
    }

    private LeaveRequestResponseDto toResponseDto(LeaveRequest lr) {
        return LeaveRequestResponseDto.builder()
                .id(lr.getId())
                .username(lr.getUser().getUsername())
                .leaveType(lr.getLeaveType())
                .startDate(lr.getStartDate())
                .endDate(lr.getEndDate())
                .reason(lr.getReason())
                .status(lr.getStatus())
                .approvedByUsername(lr.getApprovedBy() != null ? lr.getApprovedBy().getUsername() : null)
                .createdAt(lr.getCreatedAt())
                .build();
    }
}
