package org.walkmod.javalang.compiler.types;


import sun.misc.URLClassPath;

import java.io.File;
import java.util.*;

public class ClassPathTree {
    private String key;
    private Map<String, ClassPathTree> children;
    private TreeValue value;

    public ClassPathTree(String key, String path, Map<String, ClassPathTree> children, URLClassPath value) {
        this.key = key;
        this.children = children;
        this.value = new TreeValue(path, value);
    }

    public void put(String key, URLClassPath value) {
        insertTree(key, Arrays.asList(key.split(File.separator)), value);
    }

    private void insertTree(String path, List<String> keys, URLClassPath value) {
        if (keys.isEmpty()) {
            return;
        }
        String currentKey = keys.get(0);
        ClassPathTree tree = children.get(currentKey);
        if (tree != null) {
            tree.insertTree(path, keys.subList(1, keys.size()), value);
        } else {
            if (children.isEmpty()) {
                String originalKey = this.value.path.substring(this.value.path.lastIndexOf("/") + 1);

                children.put(originalKey, new ClassPathTree(
                        originalKey,
                        this.value.path,
                        new HashMap<String, ClassPathTree>(),
                        this.value.delegate));
                this.value = null;
            }
            children.put(currentKey, new ClassPathTree(currentKey, path, new HashMap<String, ClassPathTree>(), value));
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
            Iterator<ClassPathTree> it = children.values().iterator();
            while (it.hasNext()) {
                ClassPathTree tree = it.next();
                if (tree.value != null) {
                    list.add(tree.value.path);
                }
            }
        } else {
            String next = keys.get(0);
            ClassPathTree tree = children.get(next);
            if (tree != null) {
                return tree.findAll(keys.subList(1, keys.size()));
            }
        }
        return list;
    }

    public URLClassPath get(String key) {
        return findOne(Arrays.asList(key.split(File.separator)));
    }

    public boolean containsKey(String key) {
        return get(key) != null;
    }

    private URLClassPath findOne(List<String> keys) {
        if (keys.isEmpty()) {
            if (value != null) {
                return value.delegate;
            }
            return null;
        }
        String next = keys.get(0);
        ClassPathTree tree  = children.get(next);
        if (tree != null) {
            return tree.findOne(keys.subList(1, keys.size()));
        }
        return null;
    }

    class TreeValue {
        String path;
        URLClassPath delegate;

        public TreeValue(String path, URLClassPath delegate) {
            this.path = path;
            this.delegate = delegate;
        }
    }
}
