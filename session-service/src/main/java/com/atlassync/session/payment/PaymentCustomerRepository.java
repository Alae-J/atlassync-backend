package com.atlassync.session.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentCustomerRepository extends JpaRepository<PaymentCustomer, Long> {
    Optional<PaymentCustomer> findByUserId(Long userId);
}
