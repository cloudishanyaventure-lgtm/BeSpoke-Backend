package com.BeSpoke.dto;

import com.BeSpoke.entity.Order;
import com.BeSpoke.entity.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDto(
        Long id,
        List<OrderItemDto> items,
        BigDecimal totalAmount,
        String address,
        String status,
        UserDto designer,
        Instant createdAt
) {

    public record OrderItemDto(Long id, Long serviceId, String title, BigDecimal unitPrice, int quantity) {

        public static OrderItemDto from(OrderItem item) {
            return new OrderItemDto(item.getId(), item.getService().getId(), item.getTitle(),
                    item.getUnitPrice(), item.getQuantity());
        }
    }

    public static OrderDto from(Order order) {
        return new OrderDto(
                order.getId(),
                order.getItems().stream().map(OrderItemDto::from).toList(),
                order.getTotalAmount(),
                order.getAddress(),
                order.getStatus().name(),
                UserDto.from(order.getDesigner()),
                order.getCreatedAt()
        );
    }
}
