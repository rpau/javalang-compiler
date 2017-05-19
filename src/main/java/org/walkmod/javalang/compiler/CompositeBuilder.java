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
package org.walkmod.javalang.compiler;

import java.util.LinkedList;
import java.util.List;

public class CompositeBuilder<T> implements Builder<T> {

    List<Builder<T>> builders = new LinkedList<Builder<T>>();

    @Override
    public T build(T arg) throws Exception {
        for (Builder<T> builder : builders) {
            arg = builder.build(arg);
        }
        return arg;
    }

    public CompositeBuilder<T> appendBuilder(Builder<T> builder) {
        builders.add(builder);
        return this;
    }

}
