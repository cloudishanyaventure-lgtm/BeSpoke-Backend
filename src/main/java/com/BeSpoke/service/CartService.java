package com.BeSpoke.service;

import com.BeSpoke.dto.CartItemDto;
import com.BeSpoke.dto.CartItemRequest;
import com.BeSpoke.entity.CartItem;
import com.BeSpoke.entity.DesignService;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.CartItemRepository;
import com.BeSpoke.repository.DesignServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final DesignServiceRepository designServiceRepository;

    public CartService(CartItemRepository cartItemRepository,
                       DesignServiceRepository designServiceRepository) {
        this.cartItemRepository = cartItemRepository;
        this.designServiceRepository = designServiceRepository;
    }

    @Transactional(readOnly = true)
    public List<CartItemDto> getCart(User user) {
        return cartItemRepository.findByUserId(user.getId()).stream().map(CartItemDto::from).toList();
    }

    @Transactional
    public CartItemDto addToCart(User user, CartItemRequest request) {
        DesignService service = designServiceRepository.findById(request.serviceId())
                .orElseThrow(() -> new NotFoundException("Service not found: " + request.serviceId()));
        CartItem item = cartItemRepository.findByUserIdAndServiceId(user.getId(), service.getId())
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + request.quantity());
                    return existing;
                })
                .orElseGet(() -> new CartItem(user, service, request.quantity()));
        return CartItemDto.from(cartItemRepository.save(item));
    }

    @Transactional
    public void removeFromCart(User user, Long cartItemId) {
        CartItem item = cartItemRepository.findByIdAndUserId(cartItemId, user.getId())
                .orElseThrow(() -> new NotFoundException("Cart item not found: " + cartItemId));
        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(User user) {
        cartItemRepository.deleteByUserId(user.getId());
    }
}
