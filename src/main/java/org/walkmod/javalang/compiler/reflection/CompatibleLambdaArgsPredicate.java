/*
 * Copyright (C) 2015 Raquel Pau and Albert Coroleu.
 * 
 * Walkmod is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Walkmod is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with Walkmod. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package org.walkmod.javalang.compiler.reflection;

import java.util.List;

import org.walkmod.javalang.ast.body.Parameter;
import org.walkmod.javalang.ast.expr.LambdaExpr;
import org.walkmod.javalang.compiler.symbols.SymbolType;

public class CompatibleLambdaArgsPredicate<A> extends CompatibleArgsPredicate<A> {

    private LambdaExpr lambda;

    public CompatibleLambdaArgsPredicate(LambdaExpr lambda) {
        this.lambda = lambda;

    }

    @Override
    public boolean filter(A method) throws Exception {
        List<Parameter> lambdaParams = lambda.getParameters();

        if (lambdaParams != null) {

            SymbolType[] typeArgs = new SymbolType[lambdaParams.size()];
            int i = 0;
            for (Parameter param : lambdaParams) {
                typeArgs[i] = (SymbolType) param.getSymbolData();
                i++;
            }

            setTypeArgs(typeArgs);
            return super.filter(method);
        }
        return true;
    }

}
