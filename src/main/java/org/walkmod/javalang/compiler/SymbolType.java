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

import java.util.List;


public class SymbolType {
	

	private String name;
	
	private List<SymbolType> parameterizedTypes;
	
	private int arrayCount = 0;

	public SymbolType(){
		
	}
	
	public SymbolType(String name){
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<SymbolType> getParameterizedTypes() {
		return parameterizedTypes;
	}

	public void setParameterizedTypes(List<SymbolType> parameterizedTypes) {
		this.parameterizedTypes = parameterizedTypes;
	}

	public int getArrayCount() {
		return arrayCount;
	}

	public void setArrayCount(int arrayCount) {
		this.arrayCount = arrayCount;
	}

		
	

}
