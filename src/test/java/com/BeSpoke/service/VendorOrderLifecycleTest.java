package com.BeSpoke.service;

import com.BeSpoke.entity.*;
import com.BeSpoke.exception.*;
import com.BeSpoke.repository.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class VendorOrderLifecycleTest {
    private final ShopOrderRepository orders = mock(ShopOrderRepository.class);
    private final VendorService service = new VendorService(mock(ProductRepository.class), orders,
            mock(CompanyRepository.class), mock(AuditService.class));
    private final Company vendor = new Company("Workshop", "workshop");
    private final User director = new User("Owner", "owner@example.com", "hash", Role.DIRECTOR);

    private ShopOrder order(OrderStatus status) {
        vendor.setId(1L);
        vendor.setType(CompanyType.VENDOR);
        director.setCompany(vendor);
        ShopOrder order = mock(ShopOrder.class);
        when(order.getId()).thenReturn(7L);
        when(order.getVendor()).thenReturn(vendor);
        when(order.getCustomer()).thenReturn(new User("Customer", "customer@example.com", "hash", Role.CUSTOMER));
        when(order.getStatus()).thenReturn(status);
        when(order.getItems()).thenReturn(java.util.List.of());
        when(orders.findById(7L)).thenReturn(Optional.of(order));
        when(orders.save(any())).thenAnswer(call -> call.getArgument(0));
        return order;
    }

    @Test void cannotSkipConfirmationOrShipping() {
        ShopOrder order = order(OrderStatus.NEW);
        assertThrows(BadRequestException.class, () -> service.updateStatus(director, null, 7L, "DELIVERED"));
        verify(order, never()).setStatus(any());
        verify(orders, never()).save(any());
    }
    @Test void followsEveryStep() {
        OrderStatus[] states = {OrderStatus.NEW, OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED};
        for (int i = 0; i < states.length - 1; i++) {
            ShopOrder order = order(states[i]);
            service.updateStatus(director, null, 7L, states[i + 1].name());
            verify(order).setStatus(states[i + 1]);
        }
    }
    @Test void permitsCancellationButNeverReopensTerminalOrders() {
        ShopOrder order = order(OrderStatus.NEW);
        service.updateStatus(director, null, 7L, "CANCELLED");
        verify(order).setStatus(OrderStatus.CANCELLED);
        for (OrderStatus status : new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.DELIVERED}) {
            order(status);
            assertThrows(BadRequestException.class, () -> service.updateStatus(director, null, 7L, "CONFIRMED"));
        }
    }
    @Test void rejectsOtherVendorsOrders() {
        ShopOrder order = order(OrderStatus.NEW);
        Company other = new Company("Other", "other"); other.setId(2L);
        when(order.getVendor()).thenReturn(other);
        assertThrows(NotFoundException.class, () -> service.updateStatus(director, null, 7L, "CONFIRMED"));
        verify(orders, never()).save(any());
    }
    @Test void rejectsAnActorWhoseRoleWasDemoted() {
        order(OrderStatus.NEW);
        director.setRole(Role.PRODUCT_SME);
        assertThrows(ForbiddenException.class, () -> service.updateStatus(director, null, 7L, "CONFIRMED"));
    }
}
