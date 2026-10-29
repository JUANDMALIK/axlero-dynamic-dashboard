package com.axlero.dashboard;

import com.axlero.dashboard.entity.User;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserValidationTest {

    private static Validator validator;
    private static AutoCloseable validatorFactory;

    @BeforeAll
    static void createValidator() {
        var factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        validatorFactory = factory::close;
    }

    @AfterAll
    static void closeValidator() throws Exception {
        validatorFactory.close();
    }

    @Test
    void rejectsMissingRequiredFieldsAndInvalidEmail() {
        User user = new User();
        user.setEmail("not-an-email");

        assertThat(validator.validate(user))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("name", "email", "password");
    }

    @Test
    void acceptsUserWithValidRequiredFields() {
        User user = new User();
        user.setName("Ada Lovelace");
        user.setEmail("ada@example.com");
        user.setPassword("encoded-password");

        assertThat(validator.validate(user)).isEmpty();
    }
}
