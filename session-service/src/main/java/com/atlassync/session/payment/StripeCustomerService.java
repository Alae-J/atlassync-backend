package com.atlassync.session.payment;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Lazily creates a Stripe Customer for an AtlasSync user the first time they
 * check out, then caches the resulting {@code cus_…} id in
 * {@link PaymentCustomer}. Every subsequent PaymentIntent for that user is
 * attached to the same Customer — saved cards, dashboard history, and
 * refund/dispute correlation all flow from this one id.
 *
 * <p>We do NOT create Customers at sign-up because:
 * <ul>
 *   <li>Registration shouldn't depend on Stripe being reachable.</li>
 *   <li>Users who never check out shouldn't pollute the Stripe dashboard.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripeCustomerService {

    private final PaymentCustomerRepository repository;

    @Transactional
    public String getOrCreate(Long userId, String email) throws StripeException {
        try {
            return repository.findByUserId(userId)
                    .map(PaymentCustomer::getStripeCustomerId)
                    .orElseGet(() -> createAndPersistUnchecked(userId, email));
        } catch (StripeRuntimeException e) {
            // Unwrap so callers see the original checked exception.
            throw e.getStripeCause();
        }
    }

    // Wrapper that turns StripeException into a runtime exception so
    // {@code orElseGet} accepts it. {@link #getOrCreate} unwraps before
    // returning to the caller.
    private String createAndPersistUnchecked(Long userId, String email) {
        try {
            return createAndPersist(userId, email);
        } catch (StripeException e) {
            throw new StripeRuntimeException(e);
        }
    }

    private String createAndPersist(Long userId, String email) throws StripeException {
        CustomerCreateParams.Builder paramsBuilder = CustomerCreateParams.builder()
                .putMetadata("atlassync_user_id", userId.toString());
        if (email != null && !email.isBlank()) {
            paramsBuilder.setEmail(email);
        }
        Customer customer = Customer.create(paramsBuilder.build());
        PaymentCustomer row = new PaymentCustomer(
                userId, customer.getId(), email, Instant.now());
        repository.save(row);
        log.info("[stripe-customer] created customer={} for user={} email={}",
                customer.getId(), userId, email != null && !email.isBlank() ? "yes" : "none");
        return customer.getId();
    }

    /** Internal: rewraps {@link StripeException} as unchecked so the lambda compiles. */
    static class StripeRuntimeException extends RuntimeException {
        StripeRuntimeException(StripeException cause) {
            super(cause);
        }
        public StripeException getStripeCause() { return (StripeException) getCause(); }
    }
}
