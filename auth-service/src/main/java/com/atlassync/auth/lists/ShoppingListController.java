package com.atlassync.auth.lists;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * CRUD endpoints for per-user shopping lists. The gateway validates the JWT
 * and injects {@code X-User-Id}; this controller trusts that header as the
 * caller identity — same pattern as {@code MeController}.
 */
@RestController
@RequestMapping("/api/lists")
@Slf4j
public class ShoppingListController {

    private final ShoppingListService service;

    public ShoppingListController(ShoppingListService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ListSummary>> getAll(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("[lists] GET / userId={}", userId);
        return ResponseEntity.ok(service.listForUser(userId));
    }

    @PostMapping
    public ResponseEntity<ListDetail> create(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid CreateListRequest request) {
        log.info("[lists] POST / userId={} name=\"{}\"", userId, request.name());
        ListDetail created = service.create(userId, request);
        return ResponseEntity.created(URI.create("/api/lists/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListDetail> getOne(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        log.info("[lists] GET /{} userId={}", id, userId);
        return ResponseEntity.ok(service.getForUser(userId, id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ListDetail> update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @RequestBody @Valid UpdateListRequest request) {
        log.info("[lists] PATCH /{} userId={}", id, userId);
        return ResponseEntity.ok(service.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        log.info("[lists] DELETE /{} userId={}", id, userId);
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
