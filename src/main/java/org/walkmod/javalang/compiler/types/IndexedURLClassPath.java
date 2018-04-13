package org.walkmod.javalang.compiler.types;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import sun.misc.Resource;
import sun.misc.URLClassPath;

/**
 * A modified URLClassPath that indexes the contents of classpath elements, for faster resource locating.
 *
 * The standard URLClassPath does a linear scan of the classpath for each resource, which becomes
 * prohibitively expensive for classpaths with many elements.
 */
public class IndexedURLClassPath extends URLClassPath {

    private final URL[] urls;
    private int lastIndexed = 0;
    private static URL RT_JAR;

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
        super(urls);
        this.urls =  urls;
    }

    @Override
    public Resource getResource(final String name, boolean check) {
        URLClassPath delegate = index.get(name);
        if (delegate == null) {
            if (lastIndexed < urls.length) {
                indexURLs(urls[lastIndexed]);
                lastIndexed ++;
                return getResource(name, check);
            }
            return null;
        }
        return delegate.getResource(name, check);
    }

    @Override
    public URL findResource(final String name, boolean check) {
        URLClassPath delegate = index.get(name);
        if (delegate == null) {
            if (lastIndexed < urls.length) {
                indexURLs(urls[lastIndexed]);
                lastIndexed ++;
                return findResource(name, check);
            }
            return null;
        }
        return delegate.findResource(name, check);
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
           URL[] args = {url};
           URLClassPath delegate = new URLClassPath(args);

           if (root.isDirectory()) {
               String rootPath = root.getPath();
               if (!rootPath.endsWith(File.separator)) {
                   rootPath = rootPath + File.separator;
               }
               addFilesToIndex(rootPath.length(), root, delegate);
           } else if (root.isFile() && root.getName().endsWith(".jar")) {
               JarFile jarFile = new JarFile(root);
               try {
                   Enumeration<JarEntry> entries = jarFile.entries();
                   while (entries.hasMoreElements()) {
                       JarEntry entry = entries.nextElement();
                       String name = entry.getName();
                       maybeIndexResource(name, delegate);
                   }
               } finally {
                   jarFile.close();
               }
           }
       } catch (IOException e) {
           throw new RuntimeException(e);
       }
    }

    private void addFilesToIndex(int basePrefixLen, File f, URLClassPath delegate) throws IOException {
        if (f.isDirectory()) {
            if (f.getPath().length() > basePrefixLen) {  // Don't index the root itself.
                String relPath = f.getPath().substring(basePrefixLen);
                maybeIndexResource(relPath, delegate);
            }
            File[] directoryEntries = f.listFiles();
            assert(directoryEntries != null);
            for (int i = 0; i < directoryEntries.length; ++i) {
                addFilesToIndex(basePrefixLen, directoryEntries[i], delegate);
            }
        } else {
            String relPath = f.getPath().substring(basePrefixLen);
            maybeIndexResource(relPath, delegate);
        }
    }

    private void maybeIndexResource(String relPath, URLClassPath delegate) {

        if (!index.containsKey(relPath)) {
            index.put(relPath, delegate);
            // Callers may request the directory itself as a resource, and may
            // do so with or without trailing slashes.  We do this in a while-loop
            // in case the classpath element has multiple superfluous trailing slashes.
            if (relPath.endsWith(File.separator)) {
                maybeIndexResource(relPath.substring(0, relPath.length() - File.separator.length()), delegate);
            }
        }
    }


    // Map from resource name to URLClassPath to delegate loading that resource to.
    private final PathTree<URLClassPath> index = new PathTree<URLClassPath>();
}