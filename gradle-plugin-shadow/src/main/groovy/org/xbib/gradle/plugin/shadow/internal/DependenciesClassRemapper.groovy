package org.xbib.gradle.plugin.shadow.internal

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.TypePath
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper

class DependenciesClassRemapper extends ClassRemapper {

    private static final AnnotationVisitor annotationVisitor = new MyAnnotationVisitor()

    private static final MethodVisitor methodVisitor = new MyMethodVisitor()

    private static final FieldVisitor fieldVisitor = new MyFieldVisitor()

    private static final ClassVisitor classVisitor = new MyClassVisitor()

    DependenciesClassRemapper() {
        super(classVisitor, new CollectingRemapper())
    }

    Set<String> getDependencies() {
        return ((CollectingRemapper) super.remapper).classes
    }

    private static class CollectingRemapper extends Remapper {
        Set<String> classes = new HashSet<String>()

        @Override
        String map(String className) {
            classes.add(className.replace('/', '.'))
            className
        }
    }

    private static class MyAnnotationVisitor extends AnnotationVisitor {

        MyAnnotationVisitor() {
            super(Opcodes.ASM7)
        }

        @Override
        AnnotationVisitor visitAnnotation(String name, String desc) {
            this
        }

        @Override
        AnnotationVisitor visitArray(String name) {
            this
        }
    }

    private static class MyMethodVisitor extends MethodVisitor {

        MyMethodVisitor() {
            super(Opcodes.ASM7)
        }

        @Override
        AnnotationVisitor visitAnnotationDefault() {
            annotationVisitor
        }

        @Override
        AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            annotationVisitor
        }

        @Override
        AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
            annotationVisitor
        }

        @Override
        AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            annotationVisitor
        }

        @Override
        AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start,
                                                       Label[] end, int[] index, String descriptor,
                                                       boolean visible) {
            annotationVisitor
        }

        @Override
        AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible ) {
            annotationVisitor
        }

        @Override
        AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible ) {
            annotationVisitor
        }
    }

    private static class MyFieldVisitor extends FieldVisitor {

        MyFieldVisitor() {
            super(Opcodes.ASM7)
        }

        @Override
        AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            annotationVisitor
        }
        @Override
        AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            annotationVisitor
        }
    }

    private static class MyClassVisitor extends ClassVisitor {

        MyClassVisitor() {
            super(Opcodes.ASM7);
        }

        @Override
        AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            annotationVisitor
        }

        @Override
        FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            fieldVisitor
        }

        @Override
        MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            methodVisitor
        }

        @Override
        AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible ) {
            annotationVisitor
        }
    }
}
