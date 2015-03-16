package org.walkmod.javalang.compiler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.walkmod.javalang.ASTManager;
import org.walkmod.javalang.ParseException;
import org.walkmod.javalang.ast.CompilationUnit;
import org.walkmod.javalang.ast.PackageDeclaration;
import org.walkmod.javalang.ast.body.ModifierSet;
import org.walkmod.javalang.ast.body.TypeDeclaration;

public class Compiler {

	public void compile(File compilationDir, File... files) throws IOException {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		List<String> optionList = new ArrayList<String>();

		boolean result = (compilationDir.mkdirs() || compilationDir.exists())
				&& compilationDir.canWrite();
		if (result) {
			optionList.addAll(Arrays.asList("-d",
					compilationDir.getAbsolutePath()));
			StandardJavaFileManager sjfm = compiler.getStandardFileManager(
					null, null, null);
			Iterable<? extends JavaFileObject> fileObjects = sjfm
					.getJavaFileObjects(files);
			JavaCompiler.CompilationTask task = compiler.getTask(null, null,
					null, optionList, null, fileObjects);
			task.call();
			sjfm.close();
		} else {
			throw new IOException("The system cannot compile in the "
					+ compilationDir.getAbsolutePath() + " directory");
		}
	}

	public void compile(File compilationDir, File sourcesDir, String code)
			throws IOException, ParseException {

		CompilationUnit cu = ASTManager.parse(code);

		List<TypeDeclaration> types = cu.getTypes();
		String name = null;
		if (types != null) {
			boolean finish = false;
			Iterator<TypeDeclaration> it = types.iterator();
			while (it.hasNext() && !finish) {
				TypeDeclaration next = it.next();
				finish = ModifierSet.isPublic(next.getModifiers());
				if (finish) {
					name = next.getName() + ".java";
				}
			}
		}
		PackageDeclaration pd = cu.getPackage();

		if (pd != null) {
			String pckg = pd.getName().toString();
			sourcesDir = new File(sourcesDir, pckg.replaceAll(".", "//"));
		}

		if ((sourcesDir.mkdirs() || sourcesDir.exists())
				&& sourcesDir.canWrite()) {

			File tmpClass = new File(sourcesDir, name);
			FileWriter fw = new FileWriter(tmpClass);
			fw.write(code);
			fw.flush();
			fw.close();

			compile(compilationDir, tmpClass);
		}

	}

}
