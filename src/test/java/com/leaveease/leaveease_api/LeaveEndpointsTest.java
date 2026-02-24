package com.leaveease.leaveease_api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.leaveease.leaveease_api.entity.Role;
import com.leaveease.leaveease_api.entity.User;
import com.leaveease.leaveease_api.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LeaveEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    private final String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    private final String employeeUsername = "emp_" + uniqueSuffix;
    private final String adminUsername = "adm_" + uniqueSuffix;
    private final String password = "Test@12345";

    private String employeeToken;
    private String adminToken;
    private Long createdLeaveId;

    // ───────────────────── Auth helpers ─────────────────────

    private String registerUser(String username, String email) throws Exception {
        String body = """
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(username, email, password);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private String loginUser(String username) throws Exception {
        String body = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    // ───────────────────── Setup ─────────────────────

    @BeforeAll
    void setUp() throws Exception {
        employeeToken = registerUser(employeeUsername, employeeUsername + "@test.com");

        registerUser(adminUsername, adminUsername + "@test.com");
        User adminUser = userRepository.findByUsername(adminUsername).orElseThrow();
        adminUser.setRole(Role.ADMIN);
        userRepository.save(adminUser);
        adminToken = loginUser(adminUsername);
    }

    // ───────────────────── EMPLOYEE: create leave ─────────────────────

    @Test
    @Order(1)
    @DisplayName("EMPLOYEE: POST /api/leaves → 201 Created with PENDING status")
    void employee_createLeave_returns201() throws Exception {
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(3);

        String body = """
                {
                    "leaveType": "ANNUAL",
                    "startDate": "%s",
                    "endDate": "%s",
                    "reason": "Family vacation"
                }
                """.formatted(start, end);

        MvcResult result = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.leaveType").value("ANNUAL"))
                .andExpect(jsonPath("$.username").value(employeeUsername))
                .andExpect(jsonPath("$.reason").value("Family vacation"))
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        createdLeaveId = json.get("id").asLong();
    }

    // ───────────────────── EMPLOYEE: get my leaves ─────────────────────

    @Test
    @Order(2)
    @DisplayName("EMPLOYEE: GET /api/leaves/my → 200 OK with at least one leave")
    void employee_getMyLeaves_returns200() throws Exception {
        mockMvc.perform(get("/api/leaves/my")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].username").value(employeeUsername))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    // ───────────────────── EMPLOYEE: denied access to admin endpoint ─────────────────────

    @Test
    @Order(3)
    @DisplayName("EMPLOYEE: GET /api/leaves → 403 Forbidden (admin only)")
    void employee_getAllLeaves_returns403() throws Exception {
        mockMvc.perform(get("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    // ───────────────────── ADMIN: see all leaves ─────────────────────

    @Test
    @Order(4)
    @DisplayName("ADMIN: GET /api/leaves → 200 OK, sees employee's leave")
    void admin_getAllLeaves_returns200() throws Exception {
        mockMvc.perform(get("/api/leaves")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.username == '%s')]", employeeUsername).exists());
    }

    // ───────────────────── ADMIN: approve leave ─────────────────────

    @Test
    @Order(5)
    @DisplayName("ADMIN: PUT /api/leaves/{id}/approve → 200 OK")
    void admin_approveLeave_returns200() throws Exception {
        mockMvc.perform(put("/api/leaves/" + createdLeaveId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.id").value(createdLeaveId));
    }

    // ───────────────────── ADMIN: verify approved status ─────────────────────

    @Test
    @Order(6)
    @DisplayName("ADMIN: GET /api/leaves → 200 OK, leave now APPROVED")
    void admin_getAllLeaves_showsApproved() throws Exception {
        mockMvc.perform(get("/api/leaves")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].status", createdLeaveId).value("APPROVED"));
    }

    // ───────────────────── EMPLOYEE: sees approved status ─────────────────────

    @Test
    @Order(7)
    @DisplayName("EMPLOYEE: GET /api/leaves/my → 200 OK, sees APPROVED status")
    void employee_getMyLeaves_showsApproved() throws Exception {
        mockMvc.perform(get("/api/leaves/my")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    // ───────────────────── Edge cases ─────────────────────

    @Test
    @Order(8)
    @DisplayName("UNAUTHENTICATED: GET /api/leaves → 403 Forbidden (no token)")
    void unauthenticated_getAllLeaves_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/leaves"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    @DisplayName("ADMIN: POST /api/leaves → 403 Forbidden (employee only)")
    void admin_createLeave_returns403() throws Exception {
        LocalDate start = LocalDate.now().plusDays(20);
        LocalDate end = start.plusDays(2);

        String body = """
                {
                    "leaveType": "SICK",
                    "startDate": "%s",
                    "endDate": "%s",
                    "reason": "N/A"
                }
                """.formatted(start, end);

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    @DisplayName("EMPLOYEE: POST /api/leaves with past dates → 400 Bad Request")
    void employee_createLeave_pastDates_returns400() throws Exception {
        String body = """
                {
                    "leaveType": "CASUAL",
                    "startDate": "2020-01-01",
                    "endDate": "2020-01-05",
                    "reason": "Past leave"
                }
                """;

        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(11)
    @DisplayName("ADMIN: PUT /api/leaves/{id}/approve on already-approved leave → 400")
    void admin_approveAlreadyApproved_returns400() throws Exception {
        mockMvc.perform(put("/api/leaves/" + createdLeaveId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(12)
    @DisplayName("ADMIN: PUT /api/leaves/999999/approve → 404 Not Found")
    void admin_approveNonExistent_returns404() throws Exception {
        mockMvc.perform(put("/api/leaves/999999/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
