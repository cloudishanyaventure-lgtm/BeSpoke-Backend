package com.BeSpoke.dto;

import com.BeSpoke.entity.ShopOrder;
import com.BeSpoke.entity.ShopOrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ShopOrderDto(
        Long id,
        Long vendorId,
        String vendorName,
        String customerName,
        String status,
        BigDecimal total,
        String shippingAddress,
        String phone,
        Instant createdAt,
        List<ItemDto> items
) {

    public record ItemDto(Long id, Long productId, String name, BigDecimal price, int qty) {

        public static ItemDto from(ShopOrderItem item) {
            return new ItemDto(item.getId(),
                    item.getProduct() != null ? item.getProduct().getId() : null,
                    item.getName(), item.getPrice(), item.getQty());
        }
    }

    public static ShopOrderDto from(ShopOrder order) {
        return new ShopOrderDto(
                order.getId(),
                order.getVendor().getId(),
                order.getVendor().getName(),
                order.getCustomer().getName(),
                order.getStatus().name(),
                order.getTotal(),
                order.getShippingAddress(),
                order.getPhone(),
                order.getCreatedAt(),
                order.getItems().stream().map(ItemDto::from).toList()
        );
    }
}
