package ch.nostromo.firstblood.agent;

import java.lang.instrument.Instrumentation;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

/**
 * Attach API agent. Hosts a MBean for remote control.
 * 
 * @author Bernhard von Gunten <bvg@nostromo.ch>
 *
 */
public class FirstBloodAgent {

    private static FirstBloodAgentController mbean;

    public static void initialize() {
        // Nope
    }

	public static void premain(String args, Instrumentation inst) throws Exception {
	    registerMBean(inst);
	}

	public static void agentmain(String args, Instrumentation inst) throws Exception {
        registerMBean(inst);
    }

	/** 
	 * Create the MBean instance and register it.
	 * 
	 * @param inst
	 * @throws InstanceAlreadyExistsException
	 * @throws NotCompliantMBeanException
	 * @throws MalformedObjectNameException
	 * @throws MBeanException
	 */
	private static void registerMBean( Instrumentation inst) throws InstanceAlreadyExistsException, NotCompliantMBeanException, MalformedObjectNameException, MBeanException {
	    if (mbean != null) {
	        mbean.unregister();
	    }

	    mbean = new FirstBloodAgentController(inst);
	    mbean.register();
	}


}