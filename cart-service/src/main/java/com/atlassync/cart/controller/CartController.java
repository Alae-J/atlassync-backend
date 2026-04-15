package com.atlassync.cart.controller;

import com.atlassync.cart.dto.AddToCartRequest;
import com.atlassync.cart.dto.CartItemDto;
import com.atlassync.cart.dto.CartSnapshot;
import com.atlassync.cart.dto.HelpRequest;
import com.atlassync.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/{sessionId}/items")
    public ResponseEntity<CartItemDto> addItem(
            @PathVariable String sessionId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody AddToCartRequest request) {
        CartItemDto dto = cartService.addItem(sessionId, request.barcode(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/{sessionId}/items/{barcode}")
    public ResponseEntity<Void> removeItem(
            @PathVariable String sessionId,
            @PathVariable String barcode,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        cartService.removeItem(sessionId, barcode, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<CartSnapshot> getCart(@PathVariable String sessionId) {
        return ResponseEntity.ok(cartService.getCart(sessionId));
    }

    @PostMapping("/{sessionId}/help")
    public ResponseEntity<Void> requestHelp(
            @PathVariable String sessionId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody HelpRequest request) {
        int aisle = request.aisleNumber() == null ? 0 : request.aisleNumber();
        cartService.requestHelp(sessionId, aisle, userId);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> clearCart(@PathVariable String sessionId) {
        cartService.clearCart(sessionId);
        return ResponseEntity.noContent().build();
    }
}
