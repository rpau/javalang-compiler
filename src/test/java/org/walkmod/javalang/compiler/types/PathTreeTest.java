package org.walkmod.javalang.compiler.types;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class PathTreeTest {

  private static final String ROOT_KEY = "foo2";
  private static final String PACKAGE = ROOT_KEY + File.separator + "bar";
  private static final String HELLO_CLASS_KEY = PACKAGE + File.separator + "Hello.class";
  private static final String BYE_CLASS_KEY = PACKAGE  + File.separator + "Bye.class";
  private String AUX_CLASS_KEY = ROOT_KEY + File.separator +  "Aux.class";

  @Test
  public void testInsertion() {

    PathTree<String> tree =  createTreeWithData();

    Assert.assertEquals(HELLO_CLASS_KEY, tree.get(HELLO_CLASS_KEY));
    Assert.assertEquals(BYE_CLASS_KEY, tree.get(BYE_CLASS_KEY));

  }

  private PathTree<String> createTreeWithData() {
    PathTree<String> tree = new PathTree<String>();

    tree.put(AUX_CLASS_KEY, AUX_CLASS_KEY);
    tree.put(HELLO_CLASS_KEY, HELLO_CLASS_KEY);
    tree.put(BYE_CLASS_KEY, BYE_CLASS_KEY);
    return tree;
  }

  @Test
  public void testList() {
    PathTree<String> tree = createTreeWithData();
    List<String> result = tree.list("");
    Assert.assertTrue(result.isEmpty());

    result = tree.list(ROOT_KEY);
    Assert.assertTrue(result.contains(AUX_CLASS_KEY));

    result = tree.list(PACKAGE);
    Assert.assertTrue(result.contains(HELLO_CLASS_KEY));
    Assert.assertTrue(result.contains(BYE_CLASS_KEY));

  }

}
