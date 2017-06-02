package org.walkmod.javalang.compiler.types;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.ObjectCreationExpr;
import org.walkmod.javalang.ast.stmt.IfStmt;

/**
 * Methods dealing with conditional compilation.
 *
 * See JLS 14.21. Unreachable Statements,
 * http://docs.oracle.com/javase/specs/jls/se8/html/jls-14.html#jls-14.21
 */
class ConditionalCompilationUtil {
    static boolean isDisabledCode(ObjectCreationExpr n) {
        Node child = n;
        Node node = child.getParentNode();
        while (node != null) {
            if (node instanceof IfStmt) {
                IfStmt ifs = (IfStmt) node;
                if (child == ifs.getThenStmt() && isCompilationDisabledCondition(ifs.getCondition())) {
                    return true;
                }
            }
            child = node;
            node = child.getParentNode();
        }
        return false;
    }

    /**
     * Is value of expression definitely "false" following conditional compilation rules?
     */
    private static boolean isCompilationDisabledCondition(Expression condition) {
        Object conditionValue = condition.accept(new ConditionalCompilationConditionEvaluator(), null);
        return Boolean.FALSE.equals(conditionValue);
    }
}
