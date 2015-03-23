package org.walkmod.javalang.compiler;

import java.util.LinkedList;
import java.util.List;

public class CompositeBuilder<T> implements Builder<T>{

	List<Builder<T>> builders = new LinkedList<Builder<T>>();
	
	@Override
	public T build (T arg) throws Exception{
		for (Builder<T> builder : builders){
			arg = builder.build(arg);
		}
		return arg;
	}
	
	public CompositeBuilder<T> appendBuilder(Builder<T> builder){
		builders.add(builder);
		return this;
	}
	
}
