package com.peter.budget.controller;

import com.peter.budget.config.JwtAuthFilter;
import com.peter.budget.model.dto.ConnectionDto;
import com.peter.budget.model.dto.SimpleFinSetupRequest;
import com.peter.budget.model.dto.SyncResultDto;
import com.peter.budget.service.simplefin.SimpleFinSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final SimpleFinSyncService syncService;

    @GetMapping
    public ResponseEntity<List<ConnectionDto>> getConnections(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal) {
        List<ConnectionDto> connections = syncService.getConnections(principal.userId());
        return ResponseEntity.ok(connections);
    }

    @PostMapping("/simplefin/setup")
    public ResponseEntity<ConnectionDto> setupSimpleFin(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @Valid @RequestBody SimpleFinSetupRequest request) {
        ConnectionDto connection = syncService.setupConnection(principal.userId(), request.getSetupToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(connection);
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<SyncResultDto> syncConnection(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id) {
        SyncResultDto result = syncService.syncConnection(principal.userId(), id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/sync/full")
    public ResponseEntity<SyncResultDto> fullSyncConnection(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id) {
        SyncResultDto result = syncService.syncConnection(principal.userId(), id, true);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConnection(
            @AuthenticationPrincipal JwtAuthFilter.UserPrincipal principal,
            @PathVariable Long id) {
        syncService.deleteConnection(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
