package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.Strings;

import java.util.List;

class AssertUtil {
    private static final String ASSERT = "Assert";

    /**
     * Build a string representing a navigation from given assert to given property name.
     */
    static String navigationDescription(AbstractAssert<?, ?> as, String propertyName) {
        String text = as.descriptionText();
        if (Strings.isNullOrEmpty(text)) {
            text = removeAssert(as.getClass().getSimpleName());
        }
        return text + " " + propertyName;
    }

    private static String removeAssert(String text) {
        return text.endsWith(ASSERT) ? text.substring(0, text.length() - ASSERT.length()) : text;
    }

    /**
     * Helper for list assertions.
     */
    static <ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> ExtListAssert<ELEMENT_ASSERT, ELEMENT> assertThat(
            List<? extends ELEMENT> actual, Class<ELEMENT_ASSERT> assertClass, String description) {
        return (ExtListAssert<ELEMENT_ASSERT, ELEMENT>) new ExtListAssert<ELEMENT_ASSERT, ELEMENT>(actual, assertClass)
                .as(description);
    }
}
