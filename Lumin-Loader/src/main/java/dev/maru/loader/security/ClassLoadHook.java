package dev.maru.loader.security;

import niurendeobf.ZKMIndy;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ZKMIndy
public final class ClassLoadHook {
    private static final ClassLoadHook INSTANCE = new ClassLoadHook();
    
    private static final String CLOUD_CLASSES_KEY = "lumin.cloud.classes";
    
    private final Map<String, byte[]> cloudClasses = new ConcurrentHashMap<>();
    private volatile Instrumentation instrumentation;
    private volatile boolean hooked = false;
    private volatile boolean injected = false;

    private ClassLoadHook() {
        System.getProperties().put(CLOUD_CLASSES_KEY, cloudClasses);
    }

    public static ClassLoadHook getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, byte[]> getCloudClasses() {
        Object obj = System.getProperties().get(CLOUD_CLASSES_KEY);
        if (obj == null) {
            return INSTANCE.cloudClasses;
        }
        return (Map<String, byte[]>) obj;
    }

    public void registerCloudClass(String className, byte[] bytes) {
        cloudClasses.put(className, bytes);
        cloudClasses.put(className.replace('.', '/'), bytes);
    }

    public void registerCloudClasses(Map<String, byte[]> classes) {
        if (classes == null) return;
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            registerCloudClass(entry.getKey(), entry.getValue());
        }
    }

    public byte[] getCloudClassBytes(String className) {
        byte[] bytes = cloudClasses.get(className);
        if (bytes == null) {
            bytes = cloudClasses.get(className.replace('.', '/'));
        }
        return bytes;
    }

    public boolean hasCloudClass(String className) {
        return cloudClasses.containsKey(className) || 
               cloudClasses.containsKey(className.replace('.', '/'));
    }

    public void installAgent() {
        if (hooked) return;
        
        try {
            instrumentation = installByteBuddyAgent();
            if (instrumentation != null) {
                hooked = true;
                installTransformer();
                IntegrityChecker.getInstance().setInstrumentation(instrumentation);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to install agent", e);
        }
    }

    private Instrumentation installByteBuddyAgent() {
        try {
            Class<?> byteBuddyAgentClass = Class.forName("net.bytebuddy.agent.ByteBuddyAgent");
            MethodHandle installMH = MethodHandles.lookup().findStatic(
                byteBuddyAgentClass, "install", 
                MethodType.methodType(Instrumentation.class));
            return (Instrumentation) installMH.invoke();
        } catch (Throwable e) {
            return tryAlternativeAgentInstall();
        }
    }

    private Instrumentation tryAlternativeAgentInstall() {
        try {
            String pid = getProcessId();
            String agentPath = getAgentJarPath();
            
            Class<?> virtualMachineClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            MethodHandle attachMH = MethodHandles.lookup().findStatic(
                virtualMachineClass, "attach", 
                MethodType.methodType(virtualMachineClass, String.class));
            Object vm = attachMH.invoke(pid);
            
            MethodHandle loadAgentMH = MethodHandles.lookup().findVirtual(
                virtualMachineClass, "loadAgent", 
                MethodType.methodType(void.class, String.class));
            loadAgentMH.invoke(vm, agentPath);
            
            MethodHandle detachMH = MethodHandles.lookup().findVirtual(
                virtualMachineClass, "detach", 
                MethodType.methodType(void.class));
            detachMH.invoke(vm);
            
            return null;
        } catch (Throwable e) {
            return null;
        }
    }

    private String getProcessId() {
        String runtimeName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        return runtimeName.split("@")[0];
    }

    private String getAgentJarPath() {
        return null;
    }

    private void installTransformer() {
        if (instrumentation == null) return;
        
        ClassFileTransformer transformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, 
                                   Class<?> classBeingRedefined,
                                   ProtectionDomain protectionDomain, 
                                   byte[] classfileBuffer) {
                String dotName = className.replace('/', '.');
                
                if (hasCloudClass(dotName)) {
                    byte[] cloudBytes = getCloudClassBytes(dotName);
                    if (cloudBytes != null) {
                        IntegrityChecker.getInstance().registerClass(dotName, cloudBytes);
                        return cloudBytes;
                    }
                }
                
                return null;
            }
        };
        
        instrumentation.addTransformer(transformer, true);
    }

    public void injectIntoClassLoader() {
        if (injected) return;
        
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            
            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            long offset = unsafe.staticFieldOffset(implLookupField);
            MethodHandles.Lookup trustedLookup = (MethodHandles.Lookup) 
                unsafe.getObject(MethodHandles.Lookup.class, offset);
            
            MethodHandle defineClassMH = trustedLookup.findVirtual(
                ClassLoader.class, "defineClass",
                MethodType.methodType(Class.class, String.class, byte[].class, 
                                      int.class, int.class, ProtectionDomain.class));
            
            MethodHandle findLoadedClassMH = trustedLookup.findVirtual(
                ClassLoader.class, "findLoadedClass",
                MethodType.methodType(Class.class, String.class));
            
            for (Map.Entry<String, byte[]> entry : cloudClasses.entrySet()) {
                String className = entry.getKey();
                
                if (className.contains(".mixin.") || className.contains("/mixin/")) {
                    continue;
                }
                
                if (className.contains("$")) {
                    continue;
                }
                
                try {
                    Object loaded = findLoadedClassMH.invoke(classLoader, className);
                    if (loaded == null) {
                        byte[] bytes = entry.getValue();
                        defineClassMH.invoke(classLoader, className, bytes, 0, bytes.length, null);
                    }
                } catch (Throwable ignored) {
                }
            }
            
            injected = true;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject into classloader", e);
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    public boolean isInjected() {
        return injected;
    }

    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public void clear() {
        cloudClasses.clear();
    }
}
