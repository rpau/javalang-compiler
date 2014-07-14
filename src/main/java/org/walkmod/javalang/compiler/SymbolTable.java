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

import java.util.Stack;

import org.walkmod.javalang.ast.Node;
import org.walkmod.javalang.ast.expr.NameExpr;



public class SymbolTable {

	private Stack<Scope> indexStructure;
	
	private int symbolCounter = 1;
	
	public SymbolTable(){
		indexStructure = new Stack<Scope>();
	}
	
	public SymbolType getType(String symbolName){		
		Symbol s = getSymbol(symbolName);
		if(s != null){
			return s.getType();
		}
		return null;
	}
	
	
	
	public Symbol getSymbol(String symbolName){
		int i = indexStructure.size()-1;
		Symbol result = null;
		
		while(i >= 0 && result==null){
			Scope scope = indexStructure.get(i);
			result = scope.getSymbol(symbolName);
			i--;
		}
		return result;
	}
	
	public boolean containsSymbol(String symbolName){
		SymbolType type = getType(symbolName);
		return(type != null);
	}
	
	public void insertSymbol(String symbolName, SymbolType symbolType, Node initExpr){
		Scope lastScope = indexStructure.peek();
		SymbolType type = getType(symbolName);
		if(type!=null){
			 Symbol aux = createSymbol(type);
			 lastScope.chageSymbol(getSymbol(symbolName), aux);			 
		}
		else if(symbolType.getName() == null){
			throw new RuntimeException("Null symbol type resoltion for "+symbolName);
		}
		lastScope.addSymbol(symbolName, symbolType, initExpr);
	}
	
	
	public void popScope(){
		indexStructure.pop();
	}
	
	public void pushScope(){
		Scope newScope = new Scope();
		indexStructure.push(newScope);
	}
	
	public Symbol createSymbol(SymbolType typeName){
		String sname = "v"+symbolCounter;
		SymbolType type = getType(sname);
		if(type != null){
			symbolCounter ++;
			return createSymbol(typeName);
		}	
		
		Symbol result = new Symbol(new NameExpr(sname), null, null);
		
		return result;
	}
}
