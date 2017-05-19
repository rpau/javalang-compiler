package org.walkmod.javalang.compiler.test.assertj;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ClassBasedNavigableListAssert;
import org.assertj.core.api.ListAssert;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper class for navigable lists.
 */
public class ExtListAssert<ELEMENT_ASSERT extends AbstractAssert, ELEMENT> extends ListAssert<ELEMENT> {
    private final ClassBasedNavigableListAssert assertion;

    ExtListAssert(List<?> elements, Class<ELEMENT_ASSERT> assertClass) {
        super((List<? extends ELEMENT>) elements);
        assertion = assertThat((List<?>) elements, assertClass);
    }

    public ELEMENT_ASSERT item(int index) {
        isNotNull();
        final String description = navigationDescription("[" + index + "]");

        final List<?> l = actual;
        assertThat(l.size()).as(navigationDescription("size")).isGreaterThan(index);
        return (ELEMENT_ASSERT) assertion.toAssert(l.get(index), description);
    }
}
