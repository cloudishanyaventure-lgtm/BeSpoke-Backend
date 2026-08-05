package com.BeSpoke.dto;

import com.BeSpoke.entity.Product;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

/** Vendor is included only on the shop product detail view. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductDto(
        Long id,
        String name,
        String description,
        String category,
        String roomType,
        BigDecimal price,
        String imageUrl,
        boolean active,
        Instant createdAt,
        VendorRef vendor
) {

    public record VendorRef(Long id, String name, String city) {
    }

    public static ProductDto from(Product product) {
        return build(product, null);
    }

    public static ProductDto withVendor(Product product) {
        return build(product, new VendorRef(product.getCompany().getId(),
                product.getCompany().getName(), product.getCompany().getCity()));
    }

    private static ProductDto build(Product product, VendorRef vendor) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory().name(),
                product.getRoomType(),
                product.getPrice(),
                product.getImageUrl(),
                product.isActive(),
                product.getCreatedAt(),
                vendor
        );
    }
}
