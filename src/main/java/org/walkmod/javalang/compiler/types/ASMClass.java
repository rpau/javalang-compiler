package org.walkmod.javalang.compiler.types;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;


public class ASMClass extends ClassNode {


    private String actualName;
    private Boolean anonymous;
    private boolean isPrivate = false;


    public ASMClass() {
        super(Opcodes.ASM5);
    }

    @Override
    public void visit(int version, int access,
                      String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        actualName = name;
        isPrivate = access == Opcodes.ACC_PRIVATE;
    }

    @Override
    public void visitInnerClass(String name, String outer, String innerName, int access) {
        super.visitInnerClass(name, outer, innerName, access);
        if (name.equals(actualName)) {
            anonymous = innerName == null;
        }
    }

    public boolean isAnonymous() {
        return anonymous != null && anonymous;
    }

    public boolean isMemberClass() {
        return anonymous != null && !anonymous;
    }

    public String getActualName() {
        return actualName;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public String getPackage() {
        if (!actualName.contains(File.separator)){
            return null;
        }
        return getActualName().substring(0, getActualName().lastIndexOf("/") + 1)
                .replace("/", "\\.");
    }

    public String getSimpleName() {
        int index = getActualName().lastIndexOf("/");
        if (index != -1){
            return actualName;
        } else {
            return getActualName().substring(index + 1);
        }
    }

    public String getCanonicalName() {
        return getPackage() + "." + getSimpleName().replace("$", "\\.");
    }
}
