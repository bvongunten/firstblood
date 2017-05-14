package ch.nostromo.firstblood.agent;

import java.util.List;

import javax.management.MBeanException;

public interface FirstBloodAgentControllerMBean {

    /**
     * MBean object name
     */
    public static final String MBEAN_OBJECT_NAMAE = "FirstBlood:name=FirstBlood";

    /**
     * Unregister the MBean 
     * 
     * @throws MBeanException
     */
    public void unregister() throws MBeanException;
    
    /**
     * Returns a list of all loaded classes filtered by the given parameter. If the filter is null or empty, all loaded classes are returned.
     * 
     * @param filter
     * @return
     * @throws MBeanException holding the root cause
     */
	public List<String> getLoadedClasses(String filter) throws MBeanException;
	
	/**
	 * Returns the decompiled source code of a given class.
	 * 
	 * @param name name of the class.
	 * @return
     * @throws MBeanException holding the root cause
	 */
	public String getClassSource(String name) throws MBeanException;
	
	/**
	 * Overwrite the body of any method given by class name, method & code fragment.
	 * 
	 * @param className
	 * @param methodName
	 * @param code
     * @throws MBeanException holding the root cause
	 */
	public void changeMethodBody(String className, String methodName, String code) throws MBeanException;
	
}
