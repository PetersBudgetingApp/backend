package com.peter.budget.controller;

import com.peter.budget.config.JwtAuthFilter;
import com.peter.budget.model.dto.MigrationImportRequest;
import com.peter.budget.model.dto.MigrationImportResponse;
import com.peter.budget.service.MigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/migration")
@RequiredArgsConstructor
public class MigrationController {

    private final MigrationService migrationService;

    @PostMapping("/import")
    public ResponseEntity<MigrationImportResponse> importSnapshot(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @RequestBody MigrationImportRequest request) {
        MigrationImportResponse response = migrationService.importSnapshot(principal.userId(), request);
        return ResponseEntity.ok(response);
    }
}
