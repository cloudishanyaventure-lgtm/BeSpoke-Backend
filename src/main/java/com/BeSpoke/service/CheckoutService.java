package com.BeSpoke.service;

import com.BeSpoke.dto.CartItemRequest;
import com.BeSpoke.dto.CheckoutRequest;
import com.BeSpoke.dto.CheckoutResponse;
import com.BeSpoke.entity.DesignService;
import com.BeSpoke.entity.Lead;
import com.BeSpoke.entity.LeadStatus;
import com.BeSpoke.entity.Order;
import com.BeSpoke.entity.OrderItem;
import com.BeSpoke.entity.OrderStatus;
import com.BeSpoke.entity.PayeeType;
import com.BeSpoke.entity.Payment;
import com.BeSpoke.entity.PaymentStatus;
import com.BeSpoke.entity.PayoutStatus;
import com.BeSpoke.entity.Role;
import com.BeSpoke.entity.User;
import com.BeSpoke.exception.BadRequestException;
import com.BeSpoke.exception.NotFoundException;
import com.BeSpoke.repository.CartItemRepository;
import com.BeSpoke.repository.DesignServiceRepository;
import com.BeSpoke.repository.LeadRepository;
import com.BeSpoke.repository.OrderRepository;
import com.BeSpoke.repository.PaymentRepository;
import com.BeSpoke.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CheckoutService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final LeadRepository leadRepository;
    private final DesignServiceRepository designServiceRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentService paymentService;

    public CheckoutService(OrderRepository orderRepository,
                           PaymentRepository paymentRepository,
                           LeadRepository leadRepository,
                           DesignServiceRepository designServiceRepository,
                           UserRepository userRepository,
                           CartItemRepository cartItemRepository,
                           PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.leadRepository = leadRepository;
        this.designServiceRepository = designServiceRepository;
        this.userRepository = userRepository;
        this.cartItemRepository = cartItemRepository;
        this.paymentService = paymentService;
    }

    /**
     * Creates Order + Payment (mock success) + Lead, applying the routing rules:
     * 1. Paid + designer selected  -> Lead ASSIGNED to that designer (pending approval).
     * 2. Paid + no designer        -> Lead UNASSIGNED_PAID (admin queue).
     */
    @Transactional
    public CheckoutResponse checkout(User customer, CheckoutRequest request) {
        User designer = null;
        if (request.designerId() != null) {
            designer = userRepository.findById(request.designerId())
                    .orElseThrow(() -> new NotFoundException("Designer not found: " + request.designerId()));
            if (designer.getRole() != Role.DESIGNER) {
                throw new BadRequestException("Selected user is not a designer");
            }
        }

        // Build order
        Order order = new Order();
        order.setCustomer(customer);
        order.setDesigner(designer);
        order.setAddress(request.address());

        BigDecimal total = BigDecimal.ZERO;
        for (CartItemRequest itemReq : request.items()) {
            DesignService service = designServiceRepository.findById(itemReq.serviceId())
                    .orElseThrow(() -> new NotFoundException("Service not found: " + itemReq.serviceId()));
            OrderItem item = new OrderItem(order, service, service.getTitle(), service.getPrice(), itemReq.quantity());
            order.getItems().add(item);
            total = total.add(service.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));
        }
        order.setTotalAmount(total);
        order = orderRepository.save(order);

        // Charge via payment provider (mock in dev, Razorpay in production)
        PaymentService.PaymentResult result = paymentService.charge(total, "INR",
                "BeSpoke order #" + order.getId());
        if (!result.success()) {
            throw new BadRequestException("Payment failed: " + result.failureReason());
        }
        order.setStatus(OrderStatus.PAID);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(total);
        payment.setCurrency("INR");
        payment.setProvider(paymentService.providerName());
        payment.setProviderRef(result.providerRef());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPayeeType(PayeeType.DESIGNER); // payments go directly to the designer
        payment.setPayeeDesigner(designer);       // null until admin assigns one
        payment.setPayoutStatus(PayoutStatus.PENDING);
        payment = paymentRepository.save(payment);

        // Lead routing
        Lead lead = new Lead();
        lead.setCustomer(customer);
        lead.setOrder(order);
        lead.setContactName(customer.getName());
        lead.setContactEmail(customer.getEmail());
        if (!order.getItems().isEmpty()) {
            lead.setCategory(order.getItems().get(0).getService().getCategory());
        }
        if (designer != null) {
            lead.setDesigner(designer);
            lead.setStatus(LeadStatus.ASSIGNED); // rule 1: pending designer approval
        } else {
            lead.setStatus(LeadStatus.UNASSIGNED_PAID); // rule 2: admin queue
        }
        lead = leadRepository.save(lead);

        // Clear the customer's cart after successful checkout
        cartItemRepository.deleteByUserId(customer.getId());

        return new CheckoutResponse(order.getId(), payment.getId(), lead.getId(),
                lead.getStatus().name(), total, payment.getProviderRef());
    }
}
