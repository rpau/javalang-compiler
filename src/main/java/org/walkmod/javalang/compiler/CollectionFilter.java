package org.walkmod.javalang.compiler;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class CollectionFilter<T> {

	private Collection<T> elements;
	
	private List<Predicate<T>> predicates;
	
	public CollectionFilter(Collection<T> elements){
		this.elements = elements;
		this.predicates = new LinkedList<Predicate<T>>();
		
	}
	
	public CollectionFilter<T> appendPreficate(Predicate<T> predicate) {
		predicates.add(predicate);
		return this;
	}
	
	public void setElements(Collection<T> elements){
		this.elements = elements;
	}
	
	public List<T> filter() throws Exception {
		List<T> result = new LinkedList<T>();
		if (elements != null) {
			for (T elem : elements) {
				boolean filtered = true;
				Iterator<Predicate<T>> it = predicates.iterator();
				while (it.hasNext() && filtered) {
					Predicate<T> pred = it.next();
					if (pred.filter(elem)) {
						result.add(elem);

					} else {
						filtered = false;
					}
				}
			}
		}
		return result;
	}
	
	public T filterOne() throws Exception{
		T result = null;
		if (elements != null) {
			Iterator<T> itE = elements.iterator();
			while(itE.hasNext() && result == null) {
				T current = itE.next();
				boolean filtered = true;
				Iterator<Predicate<T>> it = predicates.iterator();
				while (it.hasNext() && filtered) {
					Predicate<T> pred = it.next();
					filtered = pred.filter(current);
				}
				if (filtered) {
					result = current;
				}
			}
		}
		return result;
	}
}
