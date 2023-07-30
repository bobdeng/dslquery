package cn.beagile.dslquery;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddedFieldsTest {
    public static class Empty {

    }

    @Test
    public void should_return_empty() {
        AttributeOverrides attributeOverrides = getAttributeOverrides(new AttributeOverride[0]);
        EmbeddedFields embeddedFields = new EmbeddedFields(Empty.class, attributeOverrides);
        assertEquals(0, embeddedFields.fields().size());
    }
    @Test
    public void should_return_one() {
        AttributeOverrides attributeOverrides = getAttributeOverrides(new AttributeOverride[0]);
        EmbeddedFields embeddedFields = new EmbeddedFields(Empty.class, attributeOverrides);
        assertEquals(0, embeddedFields.fields().size());
    }

    @NotNull
    private static AttributeOverrides getAttributeOverrides(AttributeOverride[] attributeOverrides1) {
        AttributeOverrides attributeOverrides = new AttributeOverrides() {
            @Override
            public Class<? extends AttributeOverrides> annotationType() {
                return null;
            }

            @Override
            public AttributeOverride[] value() {

                return attributeOverrides1;
            }
        };
        return attributeOverrides;
    }
}
