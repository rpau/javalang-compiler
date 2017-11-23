/*
 * Copyright (C) 2015 Raquel Pau and Albert Coroleu.
 * 
 * Walkmod is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Walkmod is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with Walkmod. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package org.walkmod.javalang.compiler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ParseException;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.PackageDeclaration;
import org.walkmod.javalang.ast.body.TypeDeclaration;

public class Compiler {

    public void compile(File compilationDir, File... files) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> optionList = new ArrayList<String>();

        boolean result = (compilationDir.mkdirs() || compilationDir.exists()) && compilationDir.canWrite();
        if (result) {
            optionList.addAll(Arrays.asList("-d", compilationDir.getAbsolutePath()));
            StandardJavaFileManager sjfm = compiler.getStandardFileManager(null, null, null);
            Iterable<? extends JavaFileObject> fileObjects = sjfm.getJavaFileObjects(files);
            JavaCompiler.CompilationTask task = compiler.getTask(null, null, null, optionList, null, fileObjects);
            task.call();
            sjfm.close();
        } else {
            throw new IOException(
                    "The system cannot compile in the " + compilationDir.getAbsolutePath() + " directory");
        }
    }

    public void compile(File compilationDir, File sourcesDir, String... codes) throws IOException, ParseException {

        File[] sources = new File[codes.length];
        int i = 0;
        for (String code : codes) {
            CompilationUnit cu = ASTManager.parse(code);

            String fileName = cu.getFileName();
            if ((sourcesDir.mkdirs() || sourcesDir.exists()) && sourcesDir.canWrite()) {

                File tmpClass = new File(sourcesDir, fileName);

                if (tmpClass.getParentFile().exists() || tmpClass.getParentFile().mkdirs()) {
                    FileWriter fw = new FileWriter(tmpClass);
                    fw.write(code);
                    fw.flush();
                    fw.close();
                    sources[i] = tmpClass;
                }

            }
            i++;
        }
        compile(compilationDir, sources);
    }

}
