package org.cf.smalivm;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cf.smalivm.exception.UnknownAncestors;
import org.cf.util.Dexifier;
import org.cf.util.SmaliClassUtils;
import org.cf.util.SmaliFileFactory;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.dexlib2.writer.builder.BuilderClassDef;
import org.jf.dexlib2.writer.builder.BuilderField;
import org.jf.dexlib2.writer.builder.BuilderMethod;
import org.jf.dexlib2.writer.builder.BuilderMethodParameter;
import org.jf.dexlib2.writer.builder.DexBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class manager is responsible for loading Smali files into dexlib2 objects and making them available.
 *
 * @author cfenton
 *
 */
public class SmaliClassManager {

    private static final Logger log = LoggerFactory.getLogger(SmaliClassManager.class.getSimpleName());

    // Use separate dex builder to intern framework classes so they're not included in output dex
    private static final DexBuilder frameworkDexBuilder = DexBuilder.makeDexBuilder();

    private final Map<String, SmaliFile> classNameToSmaliFile;
    private final DexBuilder dexBuilder;
    private final Map<String, BuilderClassDef> classNameToClassDef;
    private final Map<String, BuilderMethod> methodDescriptorToMethod;
    private final Map<String, List<String>> methodDescriptorToParameterTypes;
    private final Map<String, List<? extends TryBlock<? extends ExceptionHandler>>> methodDescriptorToTryBlocks;
    private final Map<String, List<String>> classNameToFieldNameAndType;
    private final SmaliFileFactory smaliFileFactory;

    /**
     *
     * @param smaliPath
     *            Path to Smali file or folder
     * @param dexBuilder
     * @throws IOException
     */
    public SmaliClassManager(File smaliPath, DexBuilder dexBuilder) throws IOException {
        smaliFileFactory = new SmaliFileFactory();
        Set<SmaliFile> smaliFiles = smaliFileFactory.getSmaliFiles(smaliPath);
        classNameToSmaliFile = new HashMap<String, SmaliFile>();
        for (SmaliFile smaliFile : smaliFiles) {
            classNameToSmaliFile.put(smaliFile.getClassName(), smaliFile);
        }
        this.dexBuilder = dexBuilder;
        classNameToClassDef = new HashMap<String, BuilderClassDef>();
        methodDescriptorToMethod = new HashMap<String, BuilderMethod>();
        methodDescriptorToParameterTypes = new HashMap<String, List<String>>();
        methodDescriptorToTryBlocks = new HashMap<String, List<? extends TryBlock<? extends ExceptionHandler>>>();
        classNameToFieldNameAndType = new HashMap<String, List<String>>();
    }

    /**
     *
     * @param smaliPath
     *            Path to Smali file or folder
     * @throws IOException
     */
    public SmaliClassManager(String smaliPath) throws IOException {
        this(smaliPath, DexBuilder.makeDexBuilder());
    }

    /**
     *
     * @param smaliPath
     *            Path to Smali file or folder
     * @param dexBuilder
     * @throws IOException
     */
    public SmaliClassManager(String smaliPath, DexBuilder dexBuilder) throws IOException {
        this(new File(smaliPath), dexBuilder);
    }

    /**
     * Loads the class if it has not been loaded.
     *
     * @param className
     *            Fully qualified Smali class descriptor
     * @return class definition for the given class name
     */
    public BuilderClassDef getClass(String className) {
        loadClassIfNecessary(className);

        return classNameToClassDef.get(className);
    }

    /**
     * Does not load any Smali files.
     *
     * @return all local class names, including framework
     */
    public Set<String> getClassNames() {
        return classNameToSmaliFile.keySet();
    }

    /**
     * Does not load any Smali files.
     *
     * @return all local class names, excluding framework
     */
    public Set<String> getNonFrameworkClassNames() {
        Set<String> classNames = new HashSet<String>();
        for (String className : classNameToSmaliFile.keySet()) {
            if (!smaliFileFactory.isFrameworkClass(className)) {
                classNames.add(className);
            }
        }

        return classNames;
    }

    public boolean isFrameworkClass(String className) {
        return smaliFileFactory.isFrameworkClass(className);
    }

    public Set<String> getLoadedClassNames() {
        return classNameToClassDef.keySet();
    }

    /**
     *
     * @param className
     * @return
     */
    public List<String> getFieldNameAndTypes(String className) {
        loadClassIfNecessary(className);

        return classNameToFieldNameAndType.get(className);
    }

    /**
     *
     * @param methodDescriptor
     * @return
     */
    public BuilderMethod getMethod(String methodDescriptor) {
        loadClassIfNecessary(methodDescriptor);

        return methodDescriptorToMethod.get(methodDescriptor);
    }

    /**
     *
     * @param className
     * @return
     */
    public Set<String> getMethodDescriptors(String className) {
        loadClassIfNecessary(className);

        BuilderClassDef classDef = getClass(className);
        Set<String> methodNames = new HashSet<String>();
        for (BuilderMethod method : classDef.getMethods()) {
            String methodName = ReferenceUtil.getMethodDescriptor(method);
            methodNames.add(methodName);
        }

        return methodNames;
    }

    /**
     *
     * @param methodDescriptor
     * @return
     */
    public List<String> getParameterTypes(String methodDescriptor) {
        loadClassIfNecessary(methodDescriptor);

        return methodDescriptorToParameterTypes.get(methodDescriptor);
    }

    /**
     *
     * @param methodDescriptor
     * @return
     */
    public List<? extends TryBlock<? extends ExceptionHandler>> getTryBlocks(String methodDescriptor) {
        loadClassIfNecessary(methodDescriptor);

        return methodDescriptorToTryBlocks.get(methodDescriptor);
    }

    /**
     *
     * @param className
     * @return true if the Smali file for the className was available at runtime
     */
    public boolean isLocalClass(String className) {
        return classNameToSmaliFile.containsKey(className);
    }

    /**
     *
     * @param methodDescriptor
     * @return true if {@link=isLocalClass} is true, and method is defined for class
     */
    public boolean isLocalMethod(String methodDescriptor) {
        String[] parts = methodDescriptor.split("->");
        String className = parts[0];
        if (!isLocalClass(className)) {
            return false;
        }

        return getMethod(methodDescriptor) != null;
    }

    /**
     *
     * @param methodDescriptor
     * @return
     */
    public boolean methodHasImplementation(String methodDescriptor) {
        BuilderMethod method = getMethod(methodDescriptor);

        return null != method.getImplementation();
    }

    private void addFieldNameAndTypes(BuilderClassDef classDef) {
        String className = ReferenceUtil.getReferenceString(classDef);
        Collection<BuilderField> fields = classDef.getFields();
        List<String> fieldNameAndTypes = new LinkedList<String>();
        for (BuilderField field : fields) {
            String fieldDescriptor = ReferenceUtil.getFieldDescriptor(field);
            String fieldNameAndType = fieldDescriptor.split("->")[1];
            fieldNameAndTypes.add(fieldNameAndType);
        }
        classNameToFieldNameAndType.put(className, fieldNameAndTypes);
    }

    private void addMethods(BuilderClassDef classDef) {
        for (BuilderMethod method : classDef.getMethods()) {
            String methodDescriptor = ReferenceUtil.getMethodDescriptor(method);
            methodDescriptorToMethod.put(methodDescriptor, method);
            addParameterTypes(method);
            addTryBlocks(method);
        }
    }

    private void addParameterTypes(BuilderMethod method) {
        List<? extends BuilderMethodParameter> builderParameters = method.getParameters();
        List<String> parameterTypes = new LinkedList<String>();
        for (BuilderMethodParameter builderParameter : builderParameters) {
            parameterTypes.add(builderParameter.getType());
        }

        int accessFlags = method.getAccessFlags();
        boolean isStatic = ((accessFlags & AccessFlags.STATIC.getValue()) != 0);
        if (!isStatic) {
            // First "parameter" for non-static methods is instance ref
            parameterTypes.add(0, method.getDefiningClass());
        }

        String methodDescriptor = ReferenceUtil.getMethodDescriptor(method);
        methodDescriptorToParameterTypes.put(methodDescriptor, parameterTypes);
    }

    private void addTryBlocks(BuilderMethod method) {
        String methodDescriptor = ReferenceUtil.getMethodDescriptor(method);
        MethodImplementation implementation = method.getImplementation();
        if (implementation == null) {
            return;
        }
        methodDescriptorToTryBlocks.put(methodDescriptor, implementation.getTryBlocks());
    }

    private void loadClassIfNecessary(String typeDescriptor) {
        String[] parts = typeDescriptor.split("->");
        String className = parts[0];
        if (getLoadedClassNames().contains(className)) {
            return;
        }

        SmaliFile smaliFile = classNameToSmaliFile.get(className);
        BuilderClassDef classDef;
        try {
            if (smaliFileFactory.isFrameworkClass(className)) {
                classDef = Dexifier.dexifySmaliFile(smaliFile.getPath(), smaliFile.open(), frameworkDexBuilder);
            } else {
                classDef = Dexifier.dexifySmaliFile(smaliFile.getPath(), smaliFile.open(), dexBuilder);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Error while loading class necessary for " + typeDescriptor, e);
            }
            System.exit(-1);
            return;
        }

        classNameToClassDef.put(className, classDef);
        addMethods(classDef);
        addFieldNameAndTypes(classDef);
    }

    public boolean isInstance(Class childClass, Class targetClass) throws UnknownAncestors {
        if ((childClass == null) || (targetClass == null)) {
            return false;
        }

        String childType = SmaliClassUtils.javaClassToSmali(childClass);
        String targetType = SmaliClassUtils.javaClassToSmali(targetClass);

        return isInstance(childType, targetType);
    }

    public boolean isInstance(String childType, String targetType) throws UnknownAncestors {
        /*
         * Note: not 100% sure how java's instanceof works with arrays, but some poking shows it compares the base
         * classes, and will not compile if types are incompatible, e.g. Integer[][] vs Object[].
         */
        String baseChild = SmaliClassUtils.getBaseClass(childType);
        if (SmaliClassUtils.isPrimitiveType(baseChild)) {
            baseChild = SmaliClassUtils.javaClassToSmali(SmaliClassUtils.smaliPrimitiveToJavaWrapper(baseChild));
        }
        String baseTarget = SmaliClassUtils.getBaseClass(targetType);
        if (SmaliClassUtils.isPrimitiveType(baseTarget)) {
            baseTarget = SmaliClassUtils.javaClassToSmali(SmaliClassUtils.smaliPrimitiveToJavaWrapper(baseTarget));
        }

        return isInstance(baseChild, baseTarget, new HashSet<String>());
    }

    private boolean isInstance(String childType, String targetType, Set<String> visited) throws UnknownAncestors {
        if (childType.equals(targetType)) {
            return true;
        }

        Set<String> parents = getAncestors(childType);
        for (String parent : parents) {
            if (visited.contains(parent)) {
                continue;
            }
            visited.add(parent);

            if (parent.equals(targetType)) {
                return true;
            } else if (isInstance(parent, targetType, visited)) {
                return true;
            }
        }

        return false;
    }

    private Set<String> getAncestors(String className) throws UnknownAncestors {
        Set<String> parents = new HashSet<String>();
        if (isLocalClass(className)) {
            BuilderClassDef classDef = getClass(className);
            parents.addAll(classDef.getInterfaces());
            if (null != classDef.getSuperclass()) {
                parents.add(classDef.getSuperclass());
            }
        } else {
            String javaClass = SmaliClassUtils.smaliClassToJava(className);
            try {
                Class<?> klazz = Class.forName(javaClass);
                Class<?>[] interfaces = klazz.getInterfaces();
                for (Class<?> interFace : interfaces) {
                    parents.add(SmaliClassUtils.javaClassToSmali(interFace));
                }
                Class<?> superklazz = klazz.getSuperclass();
                if (null != superklazz) {
                    parents.add(SmaliClassUtils.javaClassToSmali(superklazz));
                }
            } catch (ClassNotFoundException e) {
                throw new UnknownAncestors(className);
            }
        }

        return parents;
    }
}
