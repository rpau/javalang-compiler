package org.walkmod.javalang.compiler.types;



import java.io.File;
import java.util.*;

public class PathTree<T> {

    private final Map<String, PathTree<T>> children;
    private final TreeValue<T> value;

    public PathTree() {
      this(new HashMap<String, PathTree<T>>(), null);
    }

    public PathTree(Map<String, PathTree<T>> children, TreeValue<T> value) {
      this.children = children;
      this.value = value;
    }

    public void put(String key, T value) {
        insertTree(key, Arrays.asList(key.split(File.separator)), new TreeValue(key, value));
    }

    private void insertTree(String path, List<String> keys, TreeValue<T> value) {
        if (keys.isEmpty()) {
            return;
        }

        String currentKey = keys.get(0);
        PathTree tree = children.get(currentKey);

        if (tree != null) {
            tree.insertTree(path, keys.subList(1, keys.size()), value);
        } else {
          TreeValue<T> valueToInsert = null;
          if (keys.size() == 1) {
            valueToInsert = value;
          }

          PathTree<T> aux = new PathTree(new HashMap<String, PathTree>(), valueToInsert);
          children.put(currentKey, aux);
          aux.insertTree(path, keys.subList(1, keys.size()), value);
        }
    }

    public List<String> list(String key) {
        if ("".equals(key)) {
            return findAll(Collections.<String>emptyList());
        }
        return findAll(Arrays.asList(key.split(File.separator)));
    }

    private List<String> findAll(List<String> keys) {
        List<String> list = new LinkedList<String>();
        if (keys.isEmpty()) {
            if (value != null) {
                list.add(value.path);
            }
            Iterator<PathTree<T>> it = children.values().iterator();
            while (it.hasNext()) {
                PathTree tree = it.next();
                if (tree.value != null) {
                    list.add(tree.value.path);
                }
            }
        } else {
            String next = keys.get(0);
            PathTree tree = children.get(next);
            if (tree != null) {
                return tree.findAll(keys.subList(1, keys.size()));
            }
        }
        return list;
    }

    public T get(String key) {
        return findOne(Arrays.asList(key.split(File.separator)));
    }

    public boolean containsKey(String key) {
        return get(key) != null;
    }

    private T findOne(List<String> keys) {
        if (keys.isEmpty()) {
            if (value != null) {
                return value.delegate;
            }
            return null;
        }
        String next = keys.get(0);
        PathTree<T> tree  = children.get(next);
        if (tree != null) {
            return tree.findOne(keys.subList(1, keys.size()));
        }
        return null;
    }

    class TreeValue<T> {
        final String path;
        final T delegate;

        public TreeValue(String path, T delegate) {
            this.path = path;
            this.delegate = delegate;
        }
    }
}
