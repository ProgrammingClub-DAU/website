package com.cpclub.backend.cpgroup.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.cpgroup.dto.CpGroupMemberDetailDto;
import com.cpclub.backend.cpgroup.dto.CpGroupMemberRowDto;
import com.cpclub.backend.cpgroup.dto.CpGroupRequests;
import com.cpclub.backend.cpgroup.service.CpGroupService;
import com.cpclub.backend.security.service.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The CP Practice Group, for admins only. {@code /api/admin/**} is admin-only in
 * SecurityConfig as well; the annotation keeps this so if that rule ever changes.
 */
@RestController
@RequestMapping("/api/admin/cp-group")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "CP Practice Group", description = "Admin only: the CP Practice Group and how its students are doing")
public class CpGroupAdminController {

    private final CpGroupService cpGroupService;

    @GetMapping("/members")
    @Operation(summary = "Every student in the group, with solves over 7 days, 30 days and overall")
    public ResponseEntity<ApiResponse<List<CpGroupMemberRowDto>>> listMembers() {
        return ResponseEntity.ok(ApiResponse.success(cpGroupService.listMembers(), "Fetched CP Practice Group"));
    }

    @GetMapping("/members/{userId}")
    @Operation(summary = "One student in full: daily solves, contests, recent solves and notes")
    public ResponseEntity<ApiResponse<CpGroupMemberDetailDto>> memberDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(cpGroupService.memberDetail(userId), "Fetched student"));
    }

    @PostMapping("/members")
    @Operation(summary = "Add students to a batch of the group (a member already in it moves to that batch)")
    public ResponseEntity<ApiResponse<CpGroupRequests.AddResult>> addMembers(
            @Valid @RequestBody CpGroupRequests.AddMembers request,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        CpGroupRequests.AddResult result = cpGroupService.addMembers(request, admin.getId());
        return ResponseEntity.ok(ApiResponse.success(result, "Updated CP Practice Group"));
    }

    @PatchMapping("/members/{userId}")
    @Operation(summary = "Change a student's group batch or notes")
    public ResponseEntity<ApiResponse<Void>> updateMember(
            @PathVariable Long userId,
            @Valid @RequestBody CpGroupRequests.UpdateMember request,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        cpGroupService.updateMember(userId, request, admin.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Updated student"));
    }

    @DeleteMapping("/members/{userId}")
    @Operation(summary = "Remove a student from the group")
    public ResponseEntity<Void> removeMember(@PathVariable Long userId) {
        cpGroupService.removeMember(userId);
        return ResponseEntity.noContent().build();
    }
}
