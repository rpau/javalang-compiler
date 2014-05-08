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
import org.walkmod.javalang.ast.expr.Expression;
import org.walkmod.javalang.ast.expr.NameExpr;

import java.util.HashMap;
import java.util.Map;



public class Scope {

	private Map<NameExpr,Symbol> symbols = new HashMap<NameExpr, Symbol>();
	


	public Map<NameExpr, Symbol> getSymbols() {
		return symbols;
	}

	public void setSymbols(Map<NameExpr, Symbol> symbols) {
		this.symbols = symbols;
	}
	
	
	public Symbol getSymbol(NameExpr name){
		return symbols.get(name);
	}
	
	public Symbol getSymbol(String name){
		return getSymbol(new NameExpr(name)); 
	}
	
	public void chageSymbol(Symbol oldSymbol, Symbol newSymbol){
		 for(NameExpr expr : symbols.keySet()){
			 if(expr.getName().equals(oldSymbol.getName())){
				 expr.setName(newSymbol.getName().getName());
			 }
		 }
	}
	
	public void addSymbol(String symbolName, Type type, Node initNode){
		NameExpr sname = new NameExpr(symbolName);
		Symbol s = new Symbol(sname, type, initNode);
		symbols.put(sname, s);
	}
}
