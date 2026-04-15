package com.atlassync.cart.dto;

import jakarta.validation.constraints.NotBlank;

public record AddToCartRequest(@NotBlank String barcode) {
}
