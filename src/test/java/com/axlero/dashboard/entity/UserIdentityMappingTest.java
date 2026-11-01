package com.axlero.dashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserIdentityMappingTest {

    @Test
    void mapsExistingAppUsersIdentityWithoutChangingIdGeneration() throws Exception {
        assertEquals("app_users", User.class.getAnnotation(Table.class).name());

        var id = User.class.getDeclaredField("id");
        assertEquals(Long.class, id.getType());
        assertEquals(GenerationType.IDENTITY,
                id.getAnnotation(GeneratedValue.class).strategy());
        assertEquals(Id.class, id.getAnnotation(Id.class).annotationType());

        var username = User.class.getDeclaredField("username");
        assertEquals(100, username.getAnnotation(Column.class).length());
    }
}
