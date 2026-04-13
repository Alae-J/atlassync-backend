package com.atlassync.product.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ProductDto(
        Long id,
        String barcode,
        String name,
        String brand,
        BigDecimal price,
        String currencyCode,
        Long categoryId,
        Integer aisleNumber,
        String imageUrl,
        String nutriscoreGrade,
        Short novaGroup,
        String ingredientsText,
        String[] allergenCodes,
        Integer stockQuantity,
        Boolean rfidSecurityRequired,
        Map<String, Object> nutriments,
        Map<String, Object> attributes
) {
}
