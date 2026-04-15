package com.atlassync.cart.service;

import com.atlassync.cart.dto.CartItemDto;
import com.atlassync.cart.dto.CartSnapshot;
import com.atlassync.cart.entity.CartItem;
import com.atlassync.cart.exception.CartItemNotFoundException;
import com.atlassync.cart.grpc.ProductServiceGrpcClient;
import com.atlassync.cart.grpc.SessionServiceGrpcClient;
import com.atlassync.cart.kafka.CartEventProducer;
import com.atlassync.cart.repository.CartItemRepository;
import com.atlassync.proto.product.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private static final String EVENT_ITEM_ADDED = "ITEM_ADDED";
    private static final String EVENT_ITEM_REMOVED = "ITEM_REMOVED";
    private static final String EVENT_CLEARED = "CLEARED";

    private final CartItemRepository cartItemRepository;
    private final CartRedisService cartRedisService;
    private final CartBroadcastService cartBroadcastService;
    private final ProductServiceGrpcClient productClient;
    private final SessionServiceGrpcClient sessionClient;
    private final CartEventProducer eventProducer;

    public CartService(CartItemRepository cartItemRepository,
                       CartRedisService cartRedisService,
                       CartBroadcastService cartBroadcastService,
                       ProductServiceGrpcClient productClient,
                       SessionServiceGrpcClient sessionClient,
                       CartEventProducer eventProducer) {
        this.cartItemRepository = cartItemRepository;
        this.cartRedisService = cartRedisService;
        this.cartBroadcastService = cartBroadcastService;
        this.productClient = productClient;
        this.sessionClient = sessionClient;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public CartItemDto addItem(String sessionId, String barcode, Long userId) {
        sessionClient.validateSession(sessionId, userId);
        ProductResponse product = productClient.getProduct(barcode);

        BigDecimal price = parsePrice(product.getPrice());

        CartItem item = cartItemRepository.findBySessionIdAndProductId(sessionId, barcode)
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + 1);
                    return existing;
                })
                .orElseGet(() -> {
                    CartItem created = new CartItem();
                    created.setSessionId(sessionId);
                    created.setProductId(barcode);
                    created.setProductName(product.getName());
                    created.setQuantity(1);
                    created.setPriceAtAddition(price);
                    created.setImageUrl(product.getImageUrl());
                    return created;
                });

        CartItem saved = cartItemRepository.save(item);
        CartItemDto dto = toDto(saved);

        try {
            cartRedisService.putItem(sessionId, barcode, dto);
        } catch (Exception ex) {
            log.warn("Failed to write cart item to Redis for session {}, invalidating cache", sessionId, ex);
            safeDeleteRedis(sessionId);
        }

        CartSnapshot snapshot = buildSnapshot(sessionId);
        cartBroadcastService.broadcast(sessionId, EVENT_ITEM_ADDED, snapshot);
        eventProducer.publishItemAdded(sessionId, barcode, product.getName(), price, saved.getQuantity());

        return dto;
    }

    @Transactional
    public void removeItem(String sessionId, String barcode, Long userId) {
        sessionClient.validateSession(sessionId, userId);

        CartItem item = cartItemRepository.findBySessionIdAndProductId(sessionId, barcode)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found: " + barcode));

        if (item.getQuantity() > 1) {
            item.setQuantity(item.getQuantity() - 1);
            CartItem saved = cartItemRepository.save(item);
            try {
                cartRedisService.putItem(sessionId, barcode, toDto(saved));
            } catch (Exception ex) {
                log.warn("Failed to update Redis for session {}, invalidating", sessionId, ex);
                safeDeleteRedis(sessionId);
            }
        } else {
            cartItemRepository.deleteBySessionIdAndProductId(sessionId, barcode);
            try {
                cartRedisService.removeItem(sessionId, barcode);
            } catch (Exception ex) {
                log.warn("Failed to remove from Redis for session {}, invalidating", sessionId, ex);
                safeDeleteRedis(sessionId);
            }
        }

        CartSnapshot snapshot = buildSnapshot(sessionId);
        cartBroadcastService.broadcast(sessionId, EVENT_ITEM_REMOVED, snapshot);
        eventProducer.publishItemRemoved(sessionId, barcode);
    }

    @Transactional(readOnly = true)
    public CartSnapshot getCart(String sessionId) {
        return buildSnapshot(sessionId);
    }

    public void requestHelp(String sessionId, int aisleNumber, Long userId) {
        sessionClient.validateSession(sessionId, userId);
        eventProducer.publishHelpRequested(sessionId, aisleNumber, userId);
    }

    @Transactional
    public void clearCart(String sessionId) {
        cartItemRepository.deleteBySessionId(sessionId);
        safeDeleteRedis(sessionId);
        CartSnapshot snapshot = new CartSnapshot(sessionId, List.of(), BigDecimal.ZERO, 0);
        cartBroadcastService.broadcast(sessionId, EVENT_CLEARED, snapshot);
    }

    private CartSnapshot buildSnapshot(String sessionId) {
        List<CartItemDto> items;
        try {
            Map<String, CartItemDto> redisCart = cartRedisService.getCart(sessionId);
            if (redisCart == null || redisCart.isEmpty()) {
                items = loadFromDbAndWarmCache(sessionId);
            } else {
                items = redisCart.values().stream()
                        .sorted(Comparator.comparing(CartItemDto::productId))
                        .toList();
            }
        } catch (Exception ex) {
            log.warn("Redis read failed for session {}, falling back to DB", sessionId, ex);
            items = loadFromDb(sessionId);
        }

        BigDecimal total = items.stream()
                .map(i -> i.priceAtAddition().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int itemCount = items.stream().mapToInt(CartItemDto::quantity).sum();
        return new CartSnapshot(sessionId, items, total, itemCount);
    }

    private List<CartItemDto> loadFromDbAndWarmCache(String sessionId) {
        List<CartItem> dbItems = cartItemRepository.findBySessionId(sessionId);
        List<CartItemDto> dtos = dbItems.stream().map(this::toDto).toList();
        try {
            for (CartItemDto dto : dtos) {
                cartRedisService.putItem(sessionId, dto.productId(), dto);
            }
        } catch (Exception ex) {
            log.warn("Failed to warm Redis cache for session {}", sessionId, ex);
        }
        return dtos;
    }

    private List<CartItemDto> loadFromDb(String sessionId) {
        return cartItemRepository.findBySessionId(sessionId).stream()
                .map(this::toDto)
                .toList();
    }

    private void safeDeleteRedis(String sessionId) {
        try {
            cartRedisService.deleteCart(sessionId);
        } catch (Exception ex) {
            log.warn("Failed to delete Redis cart for session {}", sessionId, ex);
        }
    }

    private CartItemDto toDto(CartItem item) {
        return new CartItemDto(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPriceAtAddition(),
                item.getImageUrl(),
                item.getCreatedAt()
        );
    }

    private BigDecimal parsePrice(String price) {
        if (price == null || price.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(price);
        } catch (NumberFormatException ex) {
            log.warn("Invalid price from product service: {}", price);
            return BigDecimal.ZERO;
        }
    }
}
