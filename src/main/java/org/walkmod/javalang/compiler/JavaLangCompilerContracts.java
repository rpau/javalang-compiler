package org.walkmod.javalang.compiler;

/**
 * Global contract related definitions.
 */
public final class JavaLangCompilerContracts {
    /** invariant checking maybe expensive so it's not enabled per default */
    public static final boolean CHECK_INVARIANT_ENABLED = false;

    /** invariants that are not ready for general use  */
    public static final boolean CHECK_EXPERIMENTAL_INVARIANT_ENABLED = false;
}
                                       