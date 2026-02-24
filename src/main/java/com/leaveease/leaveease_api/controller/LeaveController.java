package com.leaveease.leaveease_api.controller;

import com.leaveease.leaveease_api.dto.LeaveRequestCreateDto;
import com.leaveease.leaveease_api.dto.LeaveRequestResponseDto;
import com.leaveease.leaveease_api.entity.LeaveStatus;
import com.leaveease.leaveease_api.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
@Tag(name = "Leave Management", description = "Endpoints for creating, viewing, and managing leave requests")
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Submit a leave request",
               description = "Employees can submit a new leave request. Validates date range and checks for overlapping leaves.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Leave request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input, date range error, or overlapping leave exists"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized — requires EMPLOYEE role")
    })
    public ResponseEntity<LeaveRequestResponseDto> createLeave(
            @Valid @RequestBody LeaveRequestCreateDto dto,
            Authentication authentication) {

        LeaveRequestResponseDto response = leaveService.createLeave(dto, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "View my leave requests",
               description = "Returns all leave requests submitted by the currently authenticated employee, sorted newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of leave requests returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized — requires EMPLOYEE role")
    })
    public ResponseEntity<List<LeaveRequestResponseDto>> getMyLeaves(Authentication authentication) {
        return ResponseEntity.ok(leaveService.getMyLeaves(authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "View all leave requests",
               description = "Admins can view all leave requests across all employees.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of all leave requests returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized — requires ADMIN role")
    })
    public ResponseEntity<List<LeaveRequestResponseDto>> getAllLeaves() {
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a leave request",
               description = "Admins can approve a pending leave request. Only PENDING requests can be approved.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leave request approved"),
            @ApiResponse(responseCode = "400", description = "Leave request is not in PENDING status"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized — requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Leave request not found")
    })
    public ResponseEntity<LeaveRequestResponseDto> approveLeave(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(leaveService.updateStatus(id, LeaveStatus.APPROVED, authentication.getName()));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a leave request",
               description = "Admins can reject a pending leave request. Only PENDING requests can be rejected.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leave request rejected"),
            @ApiResponse(responseCode = "400", description = "Leave request is not in PENDING status"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized — requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Leave request not found")
    })
    public ResponseEntity<LeaveRequestResponseDto> rejectLeave(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(leaveService.updateStatus(id, LeaveStatus.REJECTED, authentication.getName()));
    }
}
