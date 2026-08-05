package com.BeSpoke.controller;

import com.BeSpoke.dto.ProductDto;
import com.BeSpoke.dto.ShopVendorDto;
import com.BeSpoke.service.ShopService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public shop catalog — no authentication (GET /api/shop/** is permitAll). */
@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/products")
    public List<ProductDto> products(@RequestParam(required = false) String category,
                                     @RequestParam(required = false) String room,
                                     @RequestParam(required = false) Long companyId,
                                     @RequestParam(required = false) String q) {
        return shopService.products(category, room, companyId, q);
    }

    @GetMapping("/products/{id}")
    public ProductDto product(@PathVariable Long id) {
        return shopService.product(id);
    }

    @GetMapping("/vendors")
    public List<ShopVendorDto> vendors() {
        return shopService.vendors();
    }
}
