package com.guardsquare.bytecodebuilder.backend;

import org.jetbrains.annotations.NotNull;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.attribute.visitor.AttributeNameFilter;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.attribute.visitor.MultiAttributeVisitor;
import proguard.classfile.util.BranchTargetFinder;
import proguard.classfile.visitor.AllMethodVisitor;
import proguard.classfile.visitor.ClassPoolFiller;
import proguard.classfile.visitor.MemberNameFilter;
import proguard.io.ClassReader;
import proguard.io.DataEntry;
import proguard.io.DataEntrySource;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CodeUtil {
    private static final String CLASS_NAME  = "CodeGen";
    private static final String METHOD_NAME = "method";

    public static String getProGuardInstructions(String javaCode) {
        // Writer for service.
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        SimpleJavaFileManager fileManager;
        try {
            fileManager = compile(javaCode, printWriter);
        } catch (IOException e) {
            return stringWriter.toString();
        }

        ClassPool classPool = new ClassPool();

        // Parse all classes from the input jar and collect them in the class pool.
        DataEntrySource source = dataEntryReader -> fileManager.getGeneratedOutputFiles().forEach(f -> {
            try {
                dataEntryReader.read(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        try {
            source.pumpDataEntries(
                    new ClassReader(false, false, false, false, null,
                    new ClassPoolFiller(classPool)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        printWriter.println("CompactCodeAttributeComposer composer = new CompactCodeAttributeComposer(targetClass);");

        BranchTargetFinder targetFinder = new BranchTargetFinder();
        classPool.classAccept(CLASS_NAME, new AllMethodVisitor(
                new MemberNameFilter("method",
                new AllAttributeVisitor(
                new AttributeNameFilter(Attribute.CODE,
                new MultiAttributeVisitor(targetFinder,
                new AttributeVisitor() {
                      @Override
                      public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute) {
                          LabelPrinter labelPrinter = new LabelPrinter(printWriter, targetFinder);
                          MethodRefPrinter methodRefPrinter = new MethodRefPrinter(printWriter);
                          codeAttribute.instructionsAccept(clazz, method, labelPrinter);
                          codeAttribute.instructionsAccept(clazz, method, methodRefPrinter);
                          printWriter.println("composer");
                          codeAttribute.instructionsAccept(clazz, method, new InstructionPrinter(printWriter, targetFinder, labelPrinter));
                      }
                }))))));

        return stringWriter.toString();
    }

    @NotNull
    public static SimpleJavaFileManager compile(String javaCode, PrintWriter printWriter) throws IOException {
        String program =
            "public class " + CLASS_NAME + " {\n" +
                "public static void main(String[] args) { method(); }\n" +
                "public static " + (javaCode.contains("return;") || !javaCode.contains("return") ? "void" : "Object") + " " + METHOD_NAME + "() {\n" +
                    javaCode + "\n" +
                "}\n" +
            "}\n";

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null)
        {
            try
            {
                Class<?> javacTool = Class.forName("com.sun.tools.javac.api.JavacTool");
                java.lang.reflect.Method create = javacTool.getMethod("create");
                compiler = (JavaCompiler) create.invoke(null);
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        JavaFileObject compilationUnit = new StringJavaFileObject(CLASS_NAME, program);

        DiagnosticListener<JavaFileObject> listener = diagnostic -> {
            printWriter.println("Compilation failed!\n");
            printWriter.print(diagnostic.getMessage(Locale.ENGLISH));
            printWriter.println(" at line " + (diagnostic.getLineNumber() - 3) + "."); // -3 for class and method definitions.
        };

        SimpleJavaFileManager fileManager = new SimpleJavaFileManager(compiler.getStandardFileManager(listener, Locale.ENGLISH, StandardCharsets.UTF_8));

        JavaCompiler.CompilationTask compilationTask = compiler.getTask(
                null,
                fileManager,
                listener,
                Arrays.asList("--release", "8"),
                null,
                Collections.singletonList(compilationUnit));

        if (!compilationTask.call()) {
            throw new IOException("Compilation failed.");
        }

        return fileManager;
    }

    private static class StringJavaFileObject extends SimpleJavaFileObject {
        private final String code;

        public StringJavaFileObject(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    private static class ClassJavaFileObject extends SimpleJavaFileObject implements DataEntry {
        private final ByteArrayOutputStream outputStream;
        private final String className;

        protected ClassJavaFileObject(String className, Kind kind) {
            super(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind);
            this.className = className;
            outputStream = new ByteArrayOutputStream();
        }

        @Override
        public OutputStream openOutputStream() {
            return outputStream;
        }

        public byte[] getBytes() {
            return outputStream.toByteArray();
        }

        public String getClassName() {
            return className;
        }

        @Override
        public String getOriginalName() {
            return this.getClassName();
        }

        @Override
        public long getSize() {
            return this.getBytes().length;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(this.getBytes());
        }

        @Override
        public void closeInputStream() {}

        @Override
        public DataEntry getParent() {
            return null;
        }
    }

    public static class SimpleJavaFileManager extends ForwardingJavaFileManager {
        private final List<ClassJavaFileObject> outputFiles;

        protected SimpleJavaFileManager(JavaFileManager fileManager) {
            super(fileManager);
            outputFiles = new ArrayList<>();
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            ClassJavaFileObject file = new ClassJavaFileObject(className, kind);
            outputFiles.add(file);
            return file;
        }

        public List<ClassJavaFileObject> getGeneratedOutputFiles() {
            return outputFiles;
        }
    }
}
