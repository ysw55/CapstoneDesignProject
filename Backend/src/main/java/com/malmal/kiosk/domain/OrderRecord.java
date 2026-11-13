package com.malmal.kiosk.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class OrderRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int totalAmount;

    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String cartItemsJson;

    protected OrderRecord() {}

    public OrderRecord(int totalAmount, LocalDateTime createdAt, String cartItemsJson) {
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.cartItemsJson = cartItemsJson;
    }

    public Long getId() { return id; }
    public int getTotalAmount() { return totalAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCartItemsJson() { return cartItemsJson; }
}
