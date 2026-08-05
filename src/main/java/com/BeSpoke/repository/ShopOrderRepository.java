package com.BeSpoke.repository;

import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.OrderStatus;
import com.BeSpoke.entity.ShopOrder;
import com.BeSpoke.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    List<ShopOrder> findByCustomerOrderByCreatedAtDesc(User customer);

    List<ShopOrder> findByVendorOrderByCreatedAtDesc(Company vendor);

    List<ShopOrder> findByVendorAndStatusOrderByCreatedAtDesc(Company vendor, OrderStatus status);

    long countByVendor(Company vendor);
}
