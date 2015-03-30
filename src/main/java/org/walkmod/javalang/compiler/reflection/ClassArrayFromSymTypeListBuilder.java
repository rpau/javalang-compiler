package org.walkmod.javalang.compiler.reflection;

import java.util.List;

import org.walkmod.javalang.ast.SymbolDataAware;
import org.walkmod.javalang.compiler.Builder;

public class ClassArrayFromSymTypeListBuilder<T extends SymbolDataAware<?>> implements Builder<Class<?>[]>{
	
	private List<T> members;

	public  ClassArrayFromSymTypeListBuilder(List<T> members){
		this.members = members;
	}
	
	
	@Override
	public Class<?>[] build(Class<?>[] obj) throws Exception {
		if(members != null && obj != null){
			if(members.size() == obj.length){
				int i = 0;
				for(T member: members){
					obj[i] = member.getSymbolData().getClazz();
					i++;
				}
			}
		}
		return obj;
	}

}
