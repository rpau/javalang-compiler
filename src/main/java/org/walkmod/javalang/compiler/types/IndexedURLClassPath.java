package org.walkmod.javalang.compiler.types;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A modified URLClassPath that indexes the contents of classpath elements, for faster resource locating.
 *
 * The standard URLClassPath does a linear scan of the classpath for each resource, which becomes
 * prohibitively expensive for classpaths with many elements.
 */
public class IndexedURLClassPath  {

    private final URL[] urls;
    private int lastIndexed = 0;
    private static URL RT_JAR;
    // Map from resource name to URLClassPath to delegate loading that resource to.
    private final PathTree<URL> index = new PathTree<URL>();

    static {
        // static block to resolve java.lang package classes
        String[] bootPath = System.getProperties().get("sun.boot.class.path").toString()
                .split(Character.toString(File.pathSeparatorChar));
        for (String lib : bootPath) {
            if (lib.endsWith("rt.jar")) {
                File f = new File(lib);
                try {
                    RT_JAR = f.toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException("The java.lang classes cannot be loaded", e.getCause());
                }
            }
        }
    }

    public IndexedURLClassPath(final URL[] urls) {
        this.urls =  urls;
    }


    public URL findResource(final String name) {
        URL delegate = index.get(name);
        if (delegate == null) {
            if (lastIndexed < urls.length) {
                indexURLs(urls[lastIndexed]);
                lastIndexed ++;
                return findResource(name);
            }
            return null;
        }
        try {
            return new URL(delegate, name);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


    public List<String> listPackageContents(final String packageName) {

        String packageFile = packageName.replaceAll("\\.", File.separator);
        while (lastIndexed < urls.length) {
            indexURLs(urls[lastIndexed]);
            lastIndexed ++;
        }
        return index.list(packageFile);
    }

    public List<String> listSDKContents(final String packageName) {
        String packageFile = packageName.replaceAll("\\.", File.separator);
        indexURLs(RT_JAR);
        return index.list(packageFile);
    }


    private void indexURLs(URL url) {
       try {
           if (!"file".equals(url.getProtocol())) {
               throw new RuntimeException("Classpath element is not a file: " + url);
           }
           File root = new File(url.getPath());

           if (root.isDirectory()) {
               String rootPath = root.getPath();
               if (!rootPath.endsWith(File.separator)) {
                   rootPath = rootPath + File.separator;
               }
               addFilesToIndex(rootPath.length(), root, url);
           } else if (root.isFile() && root.getName().endsWith(".jar")) {
               JarFile jarFile = new JarFile(root);
               try {
                   Enumeration<JarEntry> entries = jarFile.entries();
                   while (entries.hasMoreElements()) {
                       JarEntry entry = entries.nextElement();
                       String name = entry.getName();
                       maybeIndexResource(name, url);
                   }
               } finally {
                   jarFile.close();
               }
           }
       } catch (IOException e) {
           throw new RuntimeException(e);
       }
    }

    private void addFilesToIndex(int basePrefixLen, File f, URL delegate) throws IOException {
        if (f.isDirectory()) {
            if (f.getPath().length() > basePrefixLen) {  // Don't index the root itself.
                String relPath = f.getPath().substring(basePrefixLen);
                maybeIndexResource(relPath, delegate);
            }
            File[] directoryEntries = f.listFiles();

            if (directoryEntries == null) {
              throw new RuntimeException("The list of directories of " + f.getAbsolutePath() + " is null");
            }
            for (int i = 0; i < directoryEntries.length; ++i) {
                addFilesToIndex(basePrefixLen, directoryEntries[i], delegate);
            }
        } else {
            String relPath = f.getPath().substring(basePrefixLen);
            maybeIndexResource(relPath, delegate);
        }
    }

    public URL[] getURLs() {
        return urls;
    }

  /**
   * Callers may request the directory itself as a resource, and may
   * do so with or without trailing slashes.  We do this in a while-loop
   * in case the classpath element has multiple superfluous trailing slashes.
   * @param relPath relative path
   * @param delegate value to insert
   */
    private void maybeIndexResource(String relPath, URL delegate) {

        if (!index.containsKey(relPath)) {
            index.put(relPath, delegate);
            if (relPath.endsWith(File.separator)) {
                maybeIndexResource(relPath.substring(0, relPath.length() - File.separator.length()), delegate);
            }
        }
    }
}