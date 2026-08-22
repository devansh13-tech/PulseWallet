package com.pulsewallet.pulsewallet.support;

import java.lang.reflect.Field;

/**
 * Entities intentionally expose no {@code setId(...)} - the id is
 * database-generated. Unit tests that need an entity with a specific id
 * (without a real database) go through this instead of adding a setter that
 * would only exist for tests.
 */
public final class TestEntities {

    private TestEntities() {
    }

    public static <T> T withId(T entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not set id on " + entity.getClass(), ex);
        }
    }
}
