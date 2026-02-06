package adrianmikula.jakartamigration.runtimeverification.service.impl;

import adrianmikula.jakartamigration.runtimeverification.domain.*;
import adrianmikula.jakartamigration.runtimeverification.service.BytecodeAnalyzer;
import adrianmikula.jakartamigration.runtimeverification.service.BytecodeAnalysisException;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * ASM-based implementation of BytecodeAnalyzer.
 * Fast, lightweight bytecode analysis for Jakarta migration verification.
 */
public class AsmBytecodeAnalyzer implements BytecodeAnalyzer {
    
    private final Set<String> javaxClasses = new HashSet<>();
    private final Set<String> jakartaClasses = new HashSet<>();
    private final Set<String> mixedNamespaceClasses = new HashSet<>();
    
    @Override
    public BytecodeAnalysisResult analyzeJar(Path jarPath) {
        if (!Files.exists(jarPath)) {
            throw new IllegalArgumentException("JAR file does not exist: " + jarPath);
        }
        
        if (!Files.isRegularFile(jarPath)) {
            throw new IllegalArgumentException("Path is not a file: " + jarPath);
        }
        
        long startTime = System.currentTimeMillis();
        javaxClasses.clear();
        jakartaClasses.clear();
        mixedNamespaceClasses.clear();
        
        List<RuntimeError> potentialErrors = new ArrayList<>();
        List<Warning> warnings = new ArrayList<>();
        int classesAnalyzed = 0;
        
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                if (entryName.endsWith(".class") && !entryName.contains("$")) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        analyzeClass(is, potentialErrors, warnings);
                        classesAnalyzed++;
                    } catch (Exception e) {
                        warnings.add(new Warning(
                            "Failed to analyze class " + entryName + ": " + e.getMessage(),
                            "ANALYSIS_ERROR",
                            LocalDateTime.now(),
                            0.3
                        ));
                    }
                }
            }
        } catch (IOException e) {
            throw new BytecodeAnalysisException("Failed to read JAR file: " + jarPath, e);
        }
        
        // Check for mixed namespace issues
        checkForMixedNamespaces(potentialErrors);
        
        long analysisTime = System.currentTimeMillis() - startTime;
        
        return new BytecodeAnalysisResult(
            new HashSet<>(javaxClasses),
            new HashSet<>(jakartaClasses),
            new HashSet<>(mixedNamespaceClasses),
            potentialErrors,
            warnings,
            analysisTime,
            classesAnalyzed
        );
    }
    
    @Override
    public BytecodeAnalysisResult analyzeClasses(Path classesDirectory) {
        if (!Files.exists(classesDirectory)) {
            throw new IllegalArgumentException("Classes directory does not exist: " + classesDirectory);
        }
        
        if (!Files.isDirectory(classesDirectory)) {
            throw new IllegalArgumentException("Path is not a directory: " + classesDirectory);
        }
        
        long startTime = System.currentTimeMillis();
        javaxClasses.clear();
        jakartaClasses.clear();
        mixedNamespaceClasses.clear();
        
        List<RuntimeError> potentialErrors = new ArrayList<>();
        List<Warning> warnings = new ArrayList<>();
        java.util.concurrent.atomic.AtomicInteger classesAnalyzed = new java.util.concurrent.atomic.AtomicInteger(0);
        
        try (Stream<Path> paths = Files.walk(classesDirectory)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".class"))
                 .filter(p -> !p.toString().contains("$")) // Skip inner classes
                 .forEach(classFile -> {
                     try (InputStream is = Files.newInputStream(classFile)) {
                         analyzeClass(is, potentialErrors, warnings);
                         classesAnalyzed.incrementAndGet();
                     } catch (Exception e) {
                         warnings.add(new Warning(
                             "Failed to analyze class " + classFile + ": " + e.getMessage(),
                             "ANALYSIS_ERROR",
                             LocalDateTime.now(),
                             0.3
                         ));
                     }
                 });
        } catch (IOException e) {
            throw new BytecodeAnalysisException("Failed to read classes directory: " + classesDirectory, e);
        }
        
        // Check for mixed namespace issues
        checkForMixedNamespaces(potentialErrors);
        
        long analysisTime = System.currentTimeMillis() - startTime;
        
        return new BytecodeAnalysisResult(
            new HashSet<>(javaxClasses),
            new HashSet<>(jakartaClasses),
            new HashSet<>(mixedNamespaceClasses),
            potentialErrors,
            warnings,
            analysisTime,
            classesAnalyzed.get()
        );
    }
    
    @Override
    public boolean isJavaxClass(String className) {
        return className != null && className.startsWith("javax.");
    }
    
    @Override
    public boolean isJakartaClass(String className) {
        return className != null && className.startsWith("jakarta.");
    }
    
    @Override
    public Set<String> getJavaxClasses() {
        return new HashSet<>(javaxClasses);
    }
    
    @Override
    public Set<String> getJakartaClasses() {
        return new HashSet<>(jakartaClasses);
    }
    
    private void analyzeClass(InputStream classInputStream, 
                             List<RuntimeError> potentialErrors, 
                             List<Warning> warnings) {
        try {
            ClassReader reader = new ClassReader(classInputStream);
            NamespaceDetector detector = new NamespaceDetector();
            reader.accept(detector, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            
            // Collect detected namespaces
            if (detector.hasJavax()) {
                javaxClasses.add(detector.getClassName());
            }
            if (detector.hasJakarta()) {
                jakartaClasses.add(detector.getClassName());
            }
            if (detector.hasBoth()) {
                mixedNamespaceClasses.add(detector.getClassName());
            }
            
        } catch (IOException e) {
            warnings.add(new Warning(
                "Failed to read class file: " + e.getMessage(),
                "IO_ERROR",
                LocalDateTime.now(),
                0.5
            ));
        }
    }
    
    private void checkForMixedNamespaces(List<RuntimeError> potentialErrors) {
        // If we found both javax and jakarta classes, this indicates a mixed namespace issue
        if (!javaxClasses.isEmpty() && !jakartaClasses.isEmpty()) {
            potentialErrors.add(new RuntimeError(
                ErrorType.LINKAGE_ERROR,
                "Mixed javax and jakarta namespaces detected in bytecode",
                new StackTrace("BytecodeAnalysis", "Mixed namespaces", Collections.emptyList()),
                "Multiple classes",
                "analyzeJar",
                LocalDateTime.now(),
                0.9
            ));
        }
        
        // Check for classes that reference both namespaces
        for (String className : mixedNamespaceClasses) {
            potentialErrors.add(new RuntimeError(
                ErrorType.LINKAGE_ERROR,
                "Class " + className + " uses both javax and jakarta namespaces",
                new StackTrace("BytecodeAnalysis", className, Collections.emptyList()),
                className,
                "analyzeClass",
                LocalDateTime.now(),
                0.95
            ));
        }
    }
    
    /**
     * ASM ClassVisitor that detects javax/jakarta namespace usage.
     */
    private static class NamespaceDetector extends ClassVisitor {
        private String className;
        private boolean hasJavax = false;
        private boolean hasJakarta = false;
        
        public NamespaceDetector() {
            super(Opcodes.ASM9);
        }
        
        @Override
        public void visit(int version, int access, String name, 
                         String signature, String superName, String[] interfaces) {
            this.className = name.replace('/', '.');
            
            // Check class name itself
            if (isJavaxClass(this.className)) {
                hasJavax = true;
            } else if (isJakartaClass(this.className)) {
                hasJakarta = true;
            }
            
            // Check superclass
            if (superName != null) {
                String superClassName = superName.replace('/', '.');
                if (isJavaxClass(superClassName)) {
                    hasJavax = true;
                } else if (isJakartaClass(superClassName)) {
                    hasJakarta = true;
                }
            }
            
            // Check interfaces
            if (interfaces != null) {
                for (String iface : interfaces) {
                    String ifaceName = iface.replace('/', '.');
                    if (isJavaxClass(ifaceName)) {
                        hasJavax = true;
                    } else if (isJakartaClass(ifaceName)) {
                        hasJakarta = true;
                    }
                }
            }
            
            super.visit(version, access, name, signature, superName, interfaces);
        }
        
        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            checkDescriptor(descriptor);
            return super.visitAnnotation(descriptor, visible);
        }
        
        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, 
                                      String signature, Object value) {
            checkDescriptor(descriptor);
            if (signature != null) {
                checkSignature(signature);
            }
            return super.visitField(access, name, descriptor, signature, value);
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, 
                                        String signature, String[] exceptions) {
            checkDescriptor(descriptor);
            if (signature != null) {
                checkSignature(signature);
            }
            if (exceptions != null) {
                for (String exception : exceptions) {
                    String exceptionName = exception.replace('/', '.');
                    if (isJavaxClass(exceptionName)) {
                        hasJavax = true;
                    } else if (isJakartaClass(exceptionName)) {
                        hasJakarta = true;
                    }
                }
            }
            return new MethodNamespaceDetector(super.visitMethod(access, name, descriptor, signature, exceptions));
        }
        
        private void checkDescriptor(String descriptor) {
            if (descriptor == null) return;
            
            // Parse descriptor for class references (Lpackage/Class;)
            int start = descriptor.indexOf('L');
            while (start >= 0) {
                int end = descriptor.indexOf(';', start);
                if (end > start) {
                    String className = descriptor.substring(start + 1, end).replace('/', '.');
                    if (isJavaxClass(className)) {
                        hasJavax = true;
                    } else if (isJakartaClass(className)) {
                        hasJakarta = true;
                    }
                }
                start = descriptor.indexOf('L', end);
            }
        }
        
        private void checkSignature(String signature) {
            if (signature == null) return;
            
            // Check for javax/jakarta in generic signatures
            if (signature.contains("javax/")) {
                hasJavax = true;
            }
            if (signature.contains("jakarta/")) {
                hasJakarta = true;
            }
        }
        
        private static boolean isJavaxClass(String className) {
            return className != null && 
                   (className.startsWith("javax.") || className.startsWith("Ljavax/"));
        }
        
        private static boolean isJakartaClass(String className) {
            return className != null && 
                   (className.startsWith("jakarta.") || className.startsWith("Ljakarta/"));
        }
        
        public String getClassName() {
            return className;
        }
        
        public boolean hasJavax() {
            return hasJavax;
        }
        
        public boolean hasJakarta() {
            return hasJakarta;
        }
        
        public boolean hasBoth() {
            return hasJavax && hasJakarta;
        }
    }
    
    /**
     * MethodVisitor that detects namespace usage in method bodies.
     */
    private static class MethodNamespaceDetector extends MethodVisitor {
        private final MethodVisitor mv;
        private boolean hasJavax = false;
        private boolean hasJakarta = false;
        
        public MethodNamespaceDetector(MethodVisitor mv) {
            super(Opcodes.ASM9);
            this.mv = mv;
        }
        
        @Override
        public void visitTypeInsn(int opcode, String type) {
            String typeName = type.replace('/', '.');
            if (NamespaceDetector.isJavaxClass(typeName)) {
                hasJavax = true;
            } else if (NamespaceDetector.isJakartaClass(typeName)) {
                hasJakarta = true;
            }
            if (mv != null) mv.visitTypeInsn(opcode, type);
        }
        
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, 
                                   String descriptor, boolean isInterface) {
            String ownerName = owner.replace('/', '.');
            if (NamespaceDetector.isJavaxClass(ownerName)) {
                hasJavax = true;
            } else if (NamespaceDetector.isJakartaClass(ownerName)) {
                hasJakarta = true;
            }
            if (mv != null) mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
        
        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            String ownerName = owner.replace('/', '.');
            if (NamespaceDetector.isJavaxClass(ownerName)) {
                hasJavax = true;
            } else if (NamespaceDetector.isJakartaClass(ownerName)) {
                hasJakarta = true;
            }
            if (mv != null) mv.visitFieldInsn(opcode, owner, name, descriptor);
        }
    }
}

