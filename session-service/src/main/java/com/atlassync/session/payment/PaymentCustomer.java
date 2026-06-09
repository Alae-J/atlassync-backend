package com.atlassync.session.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "payment_customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCustomer {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "stripe_customer_id", nullable = false, unique = true)
    private String stripeCustomerId;

    @Column(name = "email_at_creation")
    private String emailAtCreation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
