package com.BeSpoke.service;

import com.BeSpoke.dto.CreateProductRequest;
import com.BeSpoke.dto.ProductDto;
import com.BeSpoke.dto.ShopOrderDto;
import com.BeSpoke.dto.UpdateProductRequest;
import com.BeSpoke.dto.VendorDashboardDto;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.CompanyType;
import com.BeSpoke.entity.OrderStatus;
import com.BeSpoke.entity.Product;
import com.BeSpoke.entity.ProductCategory;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.ShopOrder;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.ForbiddenException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.CompanyRepository;
import com.BeSpoke.repository.ProductRepository;
import com.BeSpoke.repository.ShopOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Vendor workspace: products, orders, dashboard. Everything is scoped to the
 * actor's vendor company (404 for non-vendor companies, the cross-tenant
 * convention); platform roles pass an explicit companyId instead.
 */
@Service
@Transactional(readOnly = true)
public class VendorService {

    /** Mirrors VendorController's @PreAuthorize sets, re-checked against the DB role. */
    private static final Set<Role> PRODUCT_WRITERS =
            EnumSet.of(Role.DIRECTOR, Role.PRODUCT_MANAGER, Role.PRODUCT_SME);
    private static final Set<Role> ORDER_UPDATERS = EnumSet.of(
            Role.DIRECTOR, Role.ACCOUNT_MANAGER, Role.SALES_MANAGER, Role.CUSTOMER_CONSULTANT);

    private final ProductRepository productRepository;
    private final ShopOrderRepository shopOrderRepository;
    private final CompanyRepository companyRepository;
    private final AuditService auditService;

    public VendorService(ProductRepository productRepository,
                         ShopOrderRepository shopOrderRepository,
                         CompanyRepository companyRepository,
                         AuditService auditService) {
        this.productRepository = productRepository;
        this.shopOrderRepository = shopOrderRepository;
        this.companyRepository = companyRepository;
        this.auditService = auditService;
    }

    public List<ProductDto> products(User actor, Long companyId) {
        return productRepository.findByCompanyOrderByCreatedAtDesc(vendorCompany(actor, companyId))
                .stream().map(ProductDto::from).toList();
    }

    @Transactional
    public ProductDto createProduct(User actor, Long companyId, CreateProductRequest request) {
        requireRole(actor, PRODUCT_WRITERS);
        Company vendor = vendorCompany(actor, companyId);
        Product product = new Product(vendor, request.name().trim(),
                ProductCategory.valueOf(request.category()), request.price());
        product.setDescription(request.description());
        product.setRoomType(request.roomType());
        product.setImageUrl(request.imageUrl());
        product = productRepository.save(product);
        auditService.log(actor, vendor, "PRODUCT_ADDED",
                "Product \"" + product.getName() + "\" added to the shop");
        return ProductDto.from(product);
    }

    @Transactional
    public ProductDto updateProduct(User actor, Long companyId, Long productId, UpdateProductRequest request) {
        requireRole(actor, PRODUCT_WRITERS);
        Company vendor = vendorCompany(actor, companyId);
        Product product = productRepository.findById(productId)
                .filter(p -> p.getCompany().getId().equals(vendor.getId()))
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (request.name() != null) {
            product.setName(request.name().trim());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.category() != null) {
            product.setCategory(ProductCategory.valueOf(request.category()));
        }
        if (request.roomType() != null) {
            product.setRoomType(request.roomType());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.imageUrl() != null) {
            product.setImageUrl(request.imageUrl());
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }
        product = productRepository.save(product);
        auditService.log(actor, vendor, "PRODUCT_UPDATED",
                "Product \"" + product.getName() + "\" updated" + (product.isActive() ? "" : " (inactive)"));
        return ProductDto.from(product);
    }

    public List<ShopOrderDto> orders(User actor, Long companyId, String status) {
        Company vendor = vendorCompany(actor, companyId);
        List<ShopOrder> orders = status == null || status.isBlank()
                ? shopOrderRepository.findByVendorOrderByCreatedAtDesc(vendor)
                : shopOrderRepository.findByVendorAndStatusOrderByCreatedAtDesc(vendor, parseStatus(status));
        return orders.stream().map(ShopOrderDto::from).toList();
    }

    /** Forward-only lifecycle (NEW → CONFIRMED → SHIPPED → DELIVERED), or cancel. */
    @Transactional
    public ShopOrderDto updateStatus(User actor, Long companyId, Long orderId, String status) {
        requireRole(actor, ORDER_UPDATERS);
        Company vendor = vendorCompany(actor, companyId);
        ShopOrder order = shopOrderRepository.findById(orderId)
                .filter(o -> o.getVendor().getId().equals(vendor.getId()))
                .orElseThrow(() -> new NotFoundException("Order not found"));
        OrderStatus target = parseStatus(status);
        OrderStatus current = order.getStatus();
        if (current == OrderStatus.DELIVERED || current == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already " + current.name().toLowerCase());
        }
        if (target != OrderStatus.CANCELLED && target.ordinal() <= current.ordinal()) {
            throw new BadRequestException("Order status can only move forward");
        }
        order.setStatus(target);
        order = shopOrderRepository.save(order);
        auditService.log(actor, vendor, "ORDER_STATUS",
                "Order #" + order.getId() + " " + current + " → " + target);
        return ShopOrderDto.from(order);
    }

    public VendorDashboardDto dashboard(User actor, Long companyId) {
        Company vendor = vendorCompany(actor, companyId);
        List<ShopOrder> orders = shopOrderRepository.findByVendorOrderByCreatedAtDesc(vendor);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            byStatus.put(status.name(), orders.stream().filter(o -> o.getStatus() == status).count());
        }
        BigDecimal revenue = null;
        if (actor.getRole().seesFinance()) {
            revenue = orders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.DELIVERED && o.getTotal() != null)
                    .map(ShopOrder::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return new VendorDashboardDto(
                productRepository.countByCompany(vendor),
                byStatus,
                orders.stream().limit(10).map(ShopOrderDto::from).toList(),
                revenue);
    }

    /**
     * Re-check write roles against the database, not the JWT claim — a 24h-old token
     * from before a demotion still satisfies the @PreAuthorize gate.
     */
    private void requireRole(User actor, Set<Role> allowed) {
        if (!actor.getRole().isPlatform() && !allowed.contains(actor.getRole())) {
            throw new ForbiddenException("Your role cannot perform this action");
        }
    }

    /** The vendor company the actor works in — 404 unless it is a vendor company. */
    private Company vendorCompany(User actor, Long companyId) {
        Company company;
        if (actor.getRole().isPlatform()) {
            if (companyId == null) {
                throw new BadRequestException("companyId is required for platform roles");
            }
            company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new NotFoundException("Vendor company not found"));
        } else {
            company = actor.getCompany();
        }
        if (company == null || company.getType() != CompanyType.VENDOR) {
            throw new NotFoundException("Vendor company not found");
        }
        return company;
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown order status: " + status);
        }
    }
}
