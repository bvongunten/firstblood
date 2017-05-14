package ch.nostromo.firstblood.agent;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.PlainTextOutput;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

/**
 * MBean for remote code decompilation & injection.
 * 
 * @author Bernhard von Gunten <bvg@nostromo.ch>
 *
 */
public class FirstBloodAgentController implements FirstBloodAgentControllerMBean {

    private Instrumentation instrumentation;

    public FirstBloodAgentController(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    /**
     * Register the MBean
     * 
     * @throws InstanceAlreadyExistsException
     * @throws MBeanRegistrationException
     * @throws NotCompliantMBeanException
     * @throws MalformedObjectNameException
     */
    public void register() throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException {
        ManagementFactory.getPlatformMBeanServer().registerMBean(this, new ObjectName(FirstBloodAgentControllerMBean.MBEAN_OBJECT_NAMAE));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ch.nostromo.firstblood.agent.FirstBloodAgentControllerMBean#unregister()
     */
    @Override
    public void unregister() throws MBeanException {
        try {
            if (ManagementFactory.getPlatformMBeanServer().isRegistered(new ObjectName(FirstBloodAgentControllerMBean.MBEAN_OBJECT_NAMAE))) {
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName(FirstBloodAgentControllerMBean.MBEAN_OBJECT_NAMAE));
            }
        } catch (InstanceNotFoundException | MalformedObjectNameException e) {
            throw new MBeanException(e, "Unable to unregister the MBean instance. Message=" + e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.nostromo.firstblood.agent.FirstBloodAgentControllerMBean#
     * getLoadedClasses(java.lang.String)
     */
    @Override
    public List<String> getLoadedClasses(String filter) {
        List<String> result = new ArrayList<String>();
        Class<?>[] allClasses = instrumentation.getAllLoadedClasses();
        for (Class<?> clazz : allClasses) {

            if (filter == null || filter.isEmpty()) {
                result.add(clazz.getName());
            } else if (clazz.getName().toUpperCase().contains(filter.toUpperCase())) {
                result.add(clazz.getName());
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.nostromo.firstblood.agent.FirstBloodAgentControllerMBean#
     * getClassSource(java.lang.String)
     */
    @Override
    public String getClassSource(String name) throws MBeanException {

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream(); OutputStreamWriter writer = new OutputStreamWriter(stream)) {
            String clazz = name.replaceAll("\\.", "/");
            Decompiler.decompile(clazz, new PlainTextOutput(writer));
            writer.flush();
            return new String(stream.toByteArray());
        } catch (Exception e) {
            throw new MBeanException(e, "Unable to get class source. Message=" + e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.nostromo.firstblood.agent.FirstBloodAgentControllerMBean#
     * changeMethodBody(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void changeMethodBody(final String className, final String methodName, final String code) throws MBeanException {

        try {

            // Create class (re-)transformer
            ClassFileTransformer transformer = new ClassFileTransformer() {

                public byte[] transform(ClassLoader classLoader, String s, Class<?> aClass, ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {

                    try {

                        ClassPool cp = ClassPool.getDefault();
                        CtClass cc = cp.get(className);
                        CtMethod m = cc.getDeclaredMethod(methodName);

                        m.setBody(code);

                        byte[] byteCode = cc.toBytecode();
                        cc.detach();
                        return byteCode;
                    } catch (Exception ex) {
                        throw new IllegalClassFormatException("Unable to transform class with error : " + ex.getMessage());
                    }

                }
            };

            // Add transformer
            instrumentation.addTransformer(transformer, true);

            // Trigger class retransformation
            Class<?>[] allClasses = instrumentation.getAllLoadedClasses();
            for (Class<?> clazz : allClasses) {
                if (clazz.getName().contains(className)) {
                    instrumentation.retransformClasses(clazz);
                }
            }

            // Remove transformer
            instrumentation.removeTransformer(transformer);

        } catch (Exception e) {
            throw new MBeanException(e, "Unable totransform the class. Message=" + e.getMessage());
        }

    }

}
