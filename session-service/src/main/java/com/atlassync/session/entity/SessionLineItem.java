package com.atlassync.session.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Frozen snapshot of a single cart line, taken at payment time. Lives in the
 * session-service rather than cart-service so receipts survive the cart's Redis
 * TTL — once a session closes, the line items here are the authoritative record.
 */
@Entity
@Table(name = "session_line_items")
@Getter
@Setter
@NoArgsConstructor
public class SessionLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ShoppingSession session;

    @Column(nullable = false, length = 64)
    private String barcode;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "added_at")
    private Instant addedAt;
}
