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
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.visitor.InstructionVisitor;
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
import java.util.stream.Collectors;

import static com.guardsquare.bytecodebuilder.BytecodeBuilderToolWindowFactory.CLASS_NAME;
import static com.guardsquare.bytecodebuilder.BytecodeBuilderToolWindowFactory.METHOD_NAME;

public class CodeUtil {
    public static String getProGuardInstructions(String javaCode) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        SimpleJavaFileManager fileManager;
        try {
            fileManager = compile(javaCode, printWriter);
        } catch (IOException e) {
            return e.getMessage() + "\n\n" + stringWriter;
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
                new MemberNameFilter(METHOD_NAME,
                new AllAttributeVisitor(
                new AttributeNameFilter(Attribute.CODE,
                new MultiAttributeVisitor(targetFinder,
                new AttributeVisitor() {
                      @Override
                      public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute) {
                          Map<Integer, ProcessingItem> offsetsToProcessingItems = new HashMap<>();
                          ExceptionLabelManager exceptionLabelManager = new ExceptionLabelManager();
                          LabelPrinter labelPrinter = new LabelPrinter(printWriter, targetFinder);
                          MethodRefPrinter methodRefPrinter = new MethodRefPrinter(printWriter);

                          codeAttribute.instructionsAccept(clazz, method, labelPrinter);
                          codeAttribute.instructionsAccept(clazz, method, methodRefPrinter);

                          // Collect instructions as ProcessingItems to prepare for exception handling.
                          codeAttribute.instructionsAccept(clazz, method, new InstructionVisitor() {
                              @Override
                              public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction) {
                                  offsetsToProcessingItems.put(offset, new ProcessingItem(instruction, offset));
                              }
                          });

                          List<ProcessingItem> thingsToProcess = offsetsToProcessingItems.entrySet()
                                  .stream()
                                  .sorted(Comparator.comparingInt(Map.Entry::getKey))
                                  .map(Map.Entry::getValue).collect(Collectors.toList());

                          // Add exception info where it needs to go.
                          codeAttribute.exceptionsAccept(clazz, method, (clazz1, method1, codeAttribute1, exceptionInfo) -> {

                              String exceptionClassName = getReferencedClassName(clazz1, exceptionInfo.u2catchType);
                              int tryStartOffset = exceptionInfo.u2startPC;
                              int tryEndOffset = exceptionInfo.u2endPC;
                              int exceptionHandlerOffset = exceptionInfo.u2handlerPC;

                              // Add labels.
                              String tryStartLabel = exceptionLabelManager.getFreshTryStartLabel();
                              String tryEndLabel = exceptionLabelManager.getFreshTryEndLabel();
                              String handlerLabel = exceptionLabelManager.getFreshHandlerLabel();

                              insertLabelAt(tryStartLabel, tryStartOffset, thingsToProcess, offsetsToProcessingItems);
                              insertLabelAt(tryEndLabel, tryEndOffset, thingsToProcess, offsetsToProcessingItems);
                              insertLabelAt(handlerLabel, exceptionHandlerOffset, thingsToProcess, offsetsToProcessingItems);

                              // Add the catch pseudo-instruction to the end of our processing list.
                              thingsToProcess.add(new ProcessingItem(new CatchSpec(
                                  tryStartLabel,
                                  tryEndLabel,
                                  handlerLabel,
                                  exceptionClassName
                              )));
                          });

                          // Print more labels.
                          exceptionLabelManager.getLabelCreationStatements().forEach(printWriter::println);
                          printWriter.println("composer");

                          // Iterate over the entire ProcessingItem list, delegating where necessary.
                          InstructionPrinter instructionPrinter = new InstructionPrinter(printWriter, targetFinder, labelPrinter);
                          thingsToProcess.forEach(processingItem -> {
                              switch(processingItem.type) {
                                  case INSTRUCTION:
                                      processingItem.instruction.accept(clazz, method, codeAttribute, processingItem.instructionOffset, instructionPrinter);
                                      break;
                                  case LABEL:
                                      printWriter.printf("\t.label(%s)%n", processingItem.labelName);
                                      break;
                                  case CATCH:
                                      CatchSpec spec = processingItem.catchSpec;
                                      printWriter.printf("\t.catch_(%s, %s, %s, \"%s\", null)%n",
                                                         spec.tryStartLabel,
                                                         spec.tryEndLabel,
                                                         spec.handlerLabel,
                                                         spec.exceptionClassName);
                              }
                          });
                      }
                }))))));

        return stringWriter.toString();
    }

    private static void insertLabelAt(String labelName, int offset, List<ProcessingItem> processingItems, Map<Integer, ProcessingItem> offsetsToProcessingItems)
    {
        ProcessingItem itemToInsertBefore = offsetsToProcessingItems.get(offset);
        int itemIdx = processingItems.indexOf(itemToInsertBefore);
        processingItems.add(itemIdx, new ProcessingItem(labelName));
    }

    private static String getReferencedClassName(Clazz clazz, int constantPoolIdx)
    {
        final List<String> nameContainer = new ArrayList<>();
        clazz.constantPoolEntryAccept(constantPoolIdx, new ConstantVisitor() {
            @Override
            public void visitClassConstant(Clazz clazz, ClassConstant classConstant) {
                nameContainer.add(classConstant.getName(clazz));
            }
        });
        return nameContainer.get(0);
    }

    @NotNull
    public static SimpleJavaFileManager compile(String javaCode, PrintWriter printWriter) throws IOException {
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

        JavaFileObject compilationUnit = new StringJavaFileObject(CLASS_NAME, javaCode);

        DiagnosticListener<JavaFileObject> listener = diagnostic -> {
            printWriter.print(diagnostic.getKind().toString().toLowerCase() + ": ");
            printWriter.print(diagnostic.getMessage(Locale.ENGLISH));
            printWriter.println(" at line " + (diagnostic.getLineNumber()) + ".");
        };

        SimpleJavaFileManager fileManager = new SimpleJavaFileManager(compiler.getStandardFileManager(listener, Locale.ENGLISH, StandardCharsets.UTF_8));

        JavaCompiler.CompilationTask compilationTask = compiler.getTask(
                null,
                fileManager,
                listener,
                Arrays.asList("--release", "8", "-cp", System.getProperty("java.class.path")),
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
