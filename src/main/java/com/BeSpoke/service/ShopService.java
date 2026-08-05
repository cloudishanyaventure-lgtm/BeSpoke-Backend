package com.BeSpoke.service;

import com.BeSpoke.dto.PlaceOrderRequest;
import com.BeSpoke.dto.ProductDto;
import com.BeSpoke.dto.ShopOrderDto;
import com.BeSpoke.dto.ShopVendorDto;
import com.BeSpoke.entity.Company;
import com.BeSpoke.entity.CompanyType;
import com.BeSpoke.entity.KycStatus;
import com.BeSpoke.entity.Product;
import com.BeSpoke.entity.ShopOrder;
import com.BeSpoke.entity.ShopOrderItem;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.CompanyRepository;
import com.BeSpoke.repository.ProductRepository;
import com.BeSpoke.repository.ShopOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Public shop catalog + customer checkout. Only active products of active,
 * KYC-verified vendor companies are ever visible or orderable.
 */
@Service
@Transactional(readOnly = true)
public class ShopService {

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final ShopOrderRepository shopOrderRepository;

    public ShopService(ProductRepository productRepository,
                       CompanyRepository companyRepository,
                       ShopOrderRepository shopOrderRepository) {
        this.productRepository = productRepository;
        this.companyRepository = companyRepository;
        this.shopOrderRepository = shopOrderRepository;
    }

    /** Vendor is eligible for the public shop. */
    private static boolean eligible(Company vendor) {
        return vendor.isActive()
                && vendor.getType() == CompanyType.VENDOR
                && vendor.getKycStatus() == KycStatus.VERIFIED;
    }

    // ponytail: in-memory filter over all active products, matches the codebase's
    // list idiom (QuoteService.list); move to derived queries if the catalog grows.
    public List<ProductDto> products(String category, String room, Long companyId, String q) {
        String needle = q == null ? null : q.trim().toLowerCase(Locale.ROOT);
        return productRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .filter(p -> eligible(p.getCompany()))
                .filter(p -> category == null || p.getCategory().name().equalsIgnoreCase(category))
                .filter(p -> room == null || room.equalsIgnoreCase(p.getRoomType()))
                .filter(p -> companyId == null || companyId.equals(p.getCompany().getId()))
                .filter(p -> needle == null || needle.isEmpty()
                        || p.getName().toLowerCase(Locale.ROOT).contains(needle)
                        || (p.getDescription() != null
                            && p.getDescription().toLowerCase(Locale.ROOT).contains(needle)))
                .map(ProductDto::from)
                .toList();
    }

    public ProductDto product(Long id) {
        return ProductDto.withVendor(shopProduct(id));
    }

    public List<ShopVendorDto> vendors() {
        return companyRepository
                .findByActiveTrueAndTypeAndKycStatusOrderByNameAsc(CompanyType.VENDOR, KycStatus.VERIFIED)
                .stream()
                .map(vendor -> ShopVendorDto.from(vendor, productRepository.countByCompanyAndActiveTrue(vendor)))
                .toList();
    }

    /** Checkout: split the cart per vendor, snapshot name/price, one order each. */
    @Transactional
    public List<ShopOrderDto> place(User customer, PlaceOrderRequest request) {
        Map<Company, List<PlaceOrderRequest.Item>> byVendor = new LinkedHashMap<>();
        Map<Long, Product> products = new LinkedHashMap<>();
        for (PlaceOrderRequest.Item item : request.items()) {
            Product product = products.computeIfAbsent(item.productId(), this::shopProduct);
            byVendor.computeIfAbsent(product.getCompany(), vendor -> new ArrayList<>()).add(item);
        }
        List<ShopOrderDto> placed = new ArrayList<>();
        for (Map.Entry<Company, List<PlaceOrderRequest.Item>> entry : byVendor.entrySet()) {
            ShopOrder order = new ShopOrder(customer, entry.getKey(),
                    request.shippingAddress().trim(), request.phone().trim());
            BigDecimal total = BigDecimal.ZERO;
            for (PlaceOrderRequest.Item item : entry.getValue()) {
                Product product = products.get(item.productId());
                order.getItems().add(new ShopOrderItem(order, product,
                        product.getName(), product.getPrice(), item.qty()));
                total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.qty())));
            }
            order.setTotal(total);
            placed.add(ShopOrderDto.from(shopOrderRepository.save(order)));
        }
        return placed;
    }

    public List<ShopOrderDto> myOrders(User customer) {
        return shopOrderRepository.findByCustomerOrderByCreatedAtDesc(customer)
                .stream().map(ShopOrderDto::from).toList();
    }

    /** A product a shopper may see/order: active, from an eligible vendor — else 404. */
    private Product shopProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        if (!product.isActive() || !eligible(product.getCompany())) {
            throw new NotFoundException("Product not found");
        }
        return product;
    }
}
