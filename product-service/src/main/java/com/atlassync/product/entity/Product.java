package com.atlassync.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "products")
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "products_id_seq")
    @jakarta.persistence.SequenceGenerator(name = "products_id_seq", sequenceName = "products_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 14)
    private String barcode;

    @Column(nullable = false)
    private String name;

    @Column(length = 150)
    private String brand;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "EUR";

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "aisle_number")
    private Integer aisleNumber;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "nutriscore_grade", length = 1)
    private String nutriscoreGrade;

    @Column(name = "nova_group")
    private Short novaGroup;

    @Column(name = "ingredients_text", columnDefinition = "TEXT")
    private String ingredientsText;

    @Column(name = "allergen_codes", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] allergenCodes;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 100;

    @Column(name = "rfid_security_required", nullable = false)
    private Boolean rfidSecurityRequired = false;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> nutriments;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> attributes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version = 0L;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return barcode != null && barcode.equals(product.barcode);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(barcode);
    }
}
