package org.walkmod.javalang.compiler.types;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.SymbolDefinition;
import org.walkmod.javalang.ast.body.VariableDeclarator;
import org.walkmod.javalang.ast.expr.BinaryExpr;
import org.walkmod.javalang.ast.expr.BooleanLiteralExpr;
import org.walkmod.javalang.ast.expr.CharLiteralExpr;
import org.walkmod.javalang.ast.expr.DoubleLiteralExpr;
import org.walkmod.javalang.ast.expr.IntegerLiteralExpr;
import org.walkmod.javalang.ast.expr.LongLiteralExpr;
import org.walkmod.javalang.ast.expr.NameExpr;
import org.walkmod.javalang.ast.expr.UnaryExpr;
import org.walkmod.javalang.visitors.GenericVisitorAdapter;

/**
 * Evaluates expression if it represents a constant of "false" acceptable
 * for conditional compilation.
 *
 * If a definitive constant condition is met this condition is returned, otherwise null.
 *
 * See JLS 14.21. Unreachable Statements,
 * http://docs.oracle.com/javase/specs/jls/se8/html/jls-14.html#jls-14.21
 */
class ConditionalCompilationConditionEvaluator extends GenericVisitorAdapter<Object, Void> {
    @Override
    public Boolean visit(BooleanLiteralExpr n, Void arg) {
        return n.getValue();
    }

    @Override
    public Object visit(UnaryExpr n, Void arg) {
        final Object inner = n.getExpr().accept(this, arg);
        return inner instanceof Boolean && n.getOperator() == UnaryExpr.Operator.not
                ? !(Boolean)inner
                : null;
    }

    @Override
    public Object visit(CharLiteralExpr n, Void arg) {
        return Long.valueOf(n.getValue().charAt(0));
    }

    @Override
    public Object visit(IntegerLiteralExpr n, Void arg) {
        return Long.valueOf(n.getValue());
    }

    @Override
    public Object visit(LongLiteralExpr n, Void arg) {
        final String s = n.getValue();
        return Long.valueOf(s.substring(0, s.length() - 1));
    }

    @Override
    public Object visit(DoubleLiteralExpr n, Void arg) {
        final String s = n.getValue();
        return Double.valueOf(s.substring(0, s.length() - 1));
    }

    @Override
    public Boolean visit(BinaryExpr n, Void arg) {
        final Object leftLiteral = n.getLeft().accept(this, arg);
        if (!(leftLiteral instanceof Double)
                && !(leftLiteral instanceof Long)
                && !(leftLiteral instanceof Boolean)
                ) {
            return null;
        }
        final Object rightLiteral = n.getLeft().accept(this, arg);
        if (leftLiteral instanceof Boolean) {
            final Boolean left = (Boolean) leftLiteral;
            if (rightLiteral != null && !(rightLiteral instanceof Boolean)) {
                return null;
            }
            final Boolean right = (Boolean) rightLiteral;
            switch (n.getOperator()) {
                case and:
                    return !left ? Boolean.FALSE : right;
                case or:
                    return left ? Boolean.TRUE : right;
                case xor:
                    return left ^ Boolean.FALSE.equals(right);
                default:
                    return false;
            }
        } else {
            if (!(rightLiteral instanceof Double) && !(rightLiteral instanceof Long)) {
                return null;
            }
            // left/right may be each Long or Double
            final Comparable left;
            final Comparable right;
            if (leftLiteral instanceof Long) {
                if (rightLiteral instanceof Long) {
                    left = (Long) leftLiteral;
                    right = (Long) rightLiteral;
                } else {
                    left = Double.valueOf((Long) leftLiteral);
                    right = (Comparable) rightLiteral;
                }
            } else {
                if (rightLiteral instanceof Double) {
                    left = (Comparable) leftLiteral;
                    right = (Comparable) rightLiteral;
                } else {
                    left = (Comparable) leftLiteral;
                    right = Double.valueOf((Long) rightLiteral);
                }
            }
            switch (n.getOperator()) {
                case equals:
                    return left.equals(right);
                case greater:
                    return left.compareTo(right) == 1;
                case greaterEquals:
                    return left.compareTo(right) != -1;
                case less:
                    return left.compareTo(right) == -1;
                case lessEquals:
                    return left.compareTo(right) != 1;
                default:
                    return null;
            }
        }
    }

    @Override
    public Object visit(NameExpr n, Void arg) {
        final SymbolDefinition sd = n.getSymbolDefinition();
        return sd instanceof Node ? ((Node)sd).accept(this, arg) : null;
    }

    @Override
    public Object visit(VariableDeclarator n, Void arg) {
        return n.getInit() != null ? n.getInit().accept(this, arg) : null;
    }
}
