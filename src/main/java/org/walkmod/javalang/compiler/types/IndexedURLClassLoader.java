package org.walkmod.javalang.compiler.types;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.List;


/**
 * A custom ClassLoader that indexes the contents of classpath elements, for faster class locating.
 *
 * The standard URLClassLoader does a linear scan of the classpath for each class or resource, which
 * becomes prohibitively expensive for classpaths with many elements.
 */
public class IndexedURLClassLoader extends ClassLoader {

    /* The search path for classes and resources */
    private final IndexedURLClassPath ucp;

    public IndexedURLClassLoader(ClassLoader parent) {
        // parent is the default system classloader, which we want to bypass entirely in
        // the delegation hierarchy, so we make our parent that thing's parent instead.
        super(parent.getParent());
        this.ucp = new IndexedURLClassPath(getClassPathURLs());
    }

    public URL[] getURLs() {
        return this.ucp.getURLs();
    }

    public IndexedURLClassLoader(URL[] urls, ClassLoader parent) {
        super(parent);
        this.ucp = new IndexedURLClassPath(urls);
    }

    public List<String> getPackageClasses(String packageName) {
        return ucp.listPackageContents(packageName);
    }

    public List<String> getSDKContents(String packageName) {
        return ucp.listSDKContents(packageName);
    }

    private static URL[] getClassPathURLs() {
        try {
            String[] paths = System.getProperties().getProperty("java.class.path").split(File.pathSeparator);
            URL[] urls = new URL[paths.length];
            for (int i = 0; i < paths.length; ++i) {
                urls[i] = new File(paths[i]).toURI().toURL();
            }
            return urls;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public URL findResource(String name) {
        return ucp.findResource(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            String path = name.replace('.', '/').concat(".class");
            URL res = ucp.findResource(path);
            if (res != null) {
                int i = name.lastIndexOf('.');
                if (i != -1) {
                    String pkgname = name.substring(0, i);
                    // Check if package already loaded.
                    Package pkg = getPackage(pkgname);
                    if (pkg == null) {
                        definePackage(pkgname, null, null, null, null, null, null, null);
                    }
                }
                byte[] data = IOUtil.readStream(res.openStream(), true);

                // Add a CodeSource via a ProtectionDomain, as code may use this to find its own jars.
                CodeSource cs = new CodeSource(res, (Certificate[])null);
                PermissionCollection pc = new Permissions();
                pc.add(new AllPermission());
                ProtectionDomain pd = new ProtectionDomain(cs, pc);
                return defineClass(name, data, 0, data.length, pd);
            } else {
                throw new ClassNotFoundException(String.format("IndexedURLClassLoader failed to read class %s", name));
            }
        } catch (IOException e) {
            throw new ClassNotFoundException(String.format("IndexedURLClassLoader failed to read class %s", name), e);
        }
    }
}