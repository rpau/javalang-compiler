/* 
  Copyright (C) 2013 Raquel Pau and Albert Coroleu.
 
 Walkmod is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 Walkmod is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public License
 along with Walkmod.  If not, see <http://www.gnu.org/licenses/>.*/
package org.walkmod.javalang.compiler;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.expr.NameExpr;


public class Symbol {

	private NameExpr name;
	
	private Type type;
	
	private Node initNode;
	
	public Symbol(NameExpr name, Type type, Node initExpression){
		setName(name);
		setType(type);
		setInitNode(initExpression);
	}

	public NameExpr getName() {
		return name;
	}

	public void setName(NameExpr name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Node getInitNode() {
		return initNode;
	}

	public void setInitNode(Node initNode) {
		this.initNode = initNode;
	}

	
	
}
