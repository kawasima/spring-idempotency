package net.unit8.spring.idempotency.example.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private static final Logger LOG = LoggerFactory.getLogger(OrderController.class);
    @PostMapping
    public void create(OrderForm form) {

    }

    public record OrderForm(
            String customerId,
            String productId,
            Long amount
    ) {
    }
}
