package vn.edu.fpt.petworldplatform.Entity;

import jakarta.validation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.edu.fpt.petworldplatform.entity.Customer;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerValidationTest {

    private Validator validator;

    @BeforeEach
    void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testInvalidEmail() {
        Customer customer = Customer.builder()
                .username("user1")
                .passwordHash("Password1")
                .email("invalid-email")
                .phone("0123456789")
                .build();

        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);

        assertFalse(violations.isEmpty());
    }

    @Test
    void testInvalidPassword() {
        Customer customer = Customer.builder()
                .username("user1")
                .passwordHash("123")
                .email("user@gmail.com")
                .phone("0123456789")
                .build();

        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);

        assertFalse(violations.isEmpty());
        assertEquals(
                "Password must be at least 8 characters long, containing at least one uppercase letter, one lowercase letter, and one number",
                violations.iterator().next().getMessage()
        );
    }

    @Test
    void testValidCustomer() {
        Customer customer = Customer.builder()
                .username("user1")
                .passwordHash("Password1")
                .email("user@gmail.com")
                .phone("0123456789")
                .build();

        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);

        assertTrue(violations.isEmpty());
    }
}