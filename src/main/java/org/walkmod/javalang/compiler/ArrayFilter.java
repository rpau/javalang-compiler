package org.walkmod.javalang.compiler;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ArrayFilter<T> {

	private T[] elements;

	private List<Predicate<T>> predicates;

	public ArrayFilter(T[] elements) {
		this.elements = elements;
		predicates = new LinkedList<Predicate<T>>();

	}
	
	public List<Predicate<T>> getPredicates(){
		return predicates;
	}

	public ArrayFilter<T> appendPredicate(Predicate<T> predicate) {
		predicates.add(predicate);
		return this;
	}

	public void setElements(T[] elements) {
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

	public T filterOne() throws Exception {
		T result = null;
		if (elements != null) {
			for (int i = 0; i < elements.length && result == null; i++) {
				boolean filtered = true;
				Iterator<Predicate<T>> it = predicates.iterator();
				while (it.hasNext() && filtered) {
					Predicate<T> pred = it.next();
					filtered = pred.filter(elements[i]);
				}
				if (filtered) {
					result = elements[i];
				}
			}
		}
		return result;
	}
}
