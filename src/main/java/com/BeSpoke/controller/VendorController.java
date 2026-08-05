package com.BeSpoke.controller;

import com.BeSpoke.dto.CreateProductRequest;
import com.BeSpoke.dto.OrderStatusRequest;
import com.BeSpoke.dto.ProductDto;
import com.BeSpoke.dto.ShopOrderDto;
import com.BeSpoke.dto.UpdateProductRequest;
import com.BeSpoke.dto.VendorDashboardDto;
import com.BeSpoke.entity.User;
import com.BeSpoke.service.CurrentUserService;
import com.BeSpoke.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Vendor workspace. URL security admits vendor-company roles + platform;
 * the service scopes everything to the actor's vendor company (404 for
 * non-vendor companies). Platform roles pass ?companyId= instead.
 */
@RestController
@RequestMapping("/api/vendor")
public class VendorController {

    private static final String PRODUCT_WRITERS =
            "hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR','PRODUCT_MANAGER','PRODUCT_SME')";
    private static final String ORDER_UPDATERS =
            "hasAnyRole('SUPER_ADMIN','ADMIN','DIRECTOR','ACCOUNT_MANAGER','SALES_MANAGER','CUSTOMER_CONSULTANT')";

    private final VendorService vendorService;
    private final CurrentUserService currentUserService;

    public VendorController(VendorService vendorService, CurrentUserService currentUserService) {
        this.vendorService = vendorService;
        this.currentUserService = currentUserService;
    }

    private User me(Authentication authentication) {
        return currentUserService.requireByEmail(authentication.getName());
    }

    @GetMapping("/products")
    public List<ProductDto> products(Authentication authentication,
                                     @RequestParam(required = false) Long companyId) {
        return vendorService.products(me(authentication), companyId);
    }

    @PostMapping("/products")
    @PreAuthorize(PRODUCT_WRITERS)
    public ResponseEntity<ProductDto> createProduct(Authentication authentication,
                                                    @RequestParam(required = false) Long companyId,
                                                    @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vendorService.createProduct(me(authentication), companyId, request));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize(PRODUCT_WRITERS)
    public ProductDto updateProduct(Authentication authentication,
                                    @PathVariable Long id,
                                    @RequestParam(required = false) Long companyId,
                                    @Valid @RequestBody UpdateProductRequest request) {
        return vendorService.updateProduct(me(authentication), companyId, id, request);
    }

    @GetMapping("/orders")
    public List<ShopOrderDto> orders(Authentication authentication,
                                     @RequestParam(required = false) Long companyId,
                                     @RequestParam(required = false) String status) {
        return vendorService.orders(me(authentication), companyId, status);
    }

    @PutMapping("/orders/{id}/status")
    @PreAuthorize(ORDER_UPDATERS)
    public ShopOrderDto updateOrderStatus(Authentication authentication,
                                          @PathVariable Long id,
                                          @RequestParam(required = false) Long companyId,
                                          @Valid @RequestBody OrderStatusRequest request) {
        return vendorService.updateStatus(me(authentication), companyId, id, request.status());
    }

    @GetMapping("/dashboard")
    public VendorDashboardDto dashboard(Authentication authentication,
                                        @RequestParam(required = false) Long companyId) {
        return vendorService.dashboard(me(authentication), companyId);
    }
}
