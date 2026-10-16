package com.cpclub.backend.contest.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.contest.client.CodeforcesAuthorizedClient.CodeforcesApiException;
import com.cpclub.backend.contest.dto.ClubContestCreateRequest;
import com.cpclub.backend.contest.dto.ClubContestDetailDto;
import com.cpclub.backend.contest.dto.ClubContestSummaryDto;
import com.cpclub.backend.contest.dto.MyUpsolvesDto;
import com.cpclub.backend.contest.dto.UpsolveMatrixDto;
import com.cpclub.backend.contest.service.ClubContestService;
import com.cpclub.backend.contest.service.ClubContestSyncService;
import com.cpclub.backend.contest.service.UpsolveService;
import com.cpclub.backend.security.service.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Club contests (O13) and the upsolve tracker (P1).
 *
 * <p>The URL rules in SecurityConfig already make every GET under
 * {@code /api/club-contests/**} public and every other method admin-only. Two things
 * here depend on that and are easy to break:</p>
 * <ul>
 *   <li>Every numeric route is {@code /{id:[0-9]+}}, so {@code /upsolve/me} can never be
 *       captured as a contest id.</li>
 *   <li>{@code /upsolve/me} is a GET, so the URL layer lets anonymous callers through.
 *       Its {@code @PreAuthorize} is the only thing that makes it require a login.</li>
 * </ul>
 * <p>Admin writes also carry {@code @PreAuthorize}, so dropping a URL rule in a later
 * refactor does not quietly open them.</p>
 */
@RestController
@RequestMapping("/api/club-contests")
@RequiredArgsConstructor
@Tag(name = "Club contests", description = "Club-only ranklists and upsolve tracking")
public class ClubContestController {

    private final ClubContestService contestService;
    private final ClubContestSyncService syncService;
    private final UpsolveService upsolveService;

    @GetMapping
    @Operation(summary = "All club contests, newest first")
    public ResponseEntity<ApiResponse<List<ClubContestSummaryDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(contestService.list(), "Fetched club contests successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a Codeforces contest as a club contest (admin)")
    public ResponseEntity<ApiResponse<ClubContestSummaryDto>> create(
            @Valid @RequestBody ClubContestCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        ClubContestSummaryDto created = contestService.create(request, admin.getId());
        return ResponseEntity.ok(ApiResponse.success(created, "Club contest added"));
    }

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "A club contest's club-only ranklist")
    public ResponseEntity<ApiResponse<ClubContestDetailDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(contestService.getRanklist(id), "Fetched club contest successfully"));
    }

    @PostMapping("/{id:[0-9]+}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fetch standings from Codeforces now (admin)")
    public ResponseEntity<ApiResponse<ClubContestDetailDto>> sync(@PathVariable Long id) {
        try {
            syncService.sync(id);
        } catch (CodeforcesApiException e) {
            // Codeforces being down or refusing is an upstream failure, not ours.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Codeforces: " + e.getMessage());
        }
        return ResponseEntity.ok(ApiResponse.success(contestService.getRanklist(id), "Club contest synced"));
    }

    @DeleteMapping("/{id:[0-9]+}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove a club contest (admin)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        contestService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Club contest removed"));
    }

    @GetMapping("/{id:[0-9]+}/upsolve")
    @Operation(summary = "Upsolve matrix: participants against problems")
    public ResponseEntity<ApiResponse<UpsolveMatrixDto>> upsolve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(upsolveService.getContestUpsolve(id), "Fetched upsolve matrix successfully"));
    }

    @GetMapping("/upsolve/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "The caller's pending upsolves")
    public ResponseEntity<ApiResponse<MyUpsolvesDto>> myUpsolves(@AuthenticationPrincipal UserDetailsImpl me) {
        return ResponseEntity.ok(ApiResponse.success(upsolveService.getMyUpsolves(me.getId()), "Fetched pending upsolves successfully"));
    }
}
