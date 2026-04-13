package com.atlassync.product.dto;

import com.atlassync.product.entity.Product;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductDto toDto(Product p) {
        return new ProductDto(
                p.getId(),
                p.getBarcode(),
                p.getName(),
                p.getBrand(),
                p.getPrice(),
                p.getCurrencyCode(),
                p.getCategoryId(),
                p.getAisleNumber(),
                p.getImageUrl(),
                p.getNutriscoreGrade(),
                p.getNovaGroup(),
                p.getIngredientsText(),
                p.getAllergenCodes(),
                p.getStockQuantity(),
                p.getRfidSecurityRequired(),
                p.getNutriments(),
                p.getAttributes()
        );
    }
}
