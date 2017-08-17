package ch.nostromo.firstblood.odyssey;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.VirtualMachine;

import ch.nostromo.firstblood.agent.FirstBloodAgentControllerMBean;

public class MBeanTools {

	public static FirstBloodAgentControllerMBean getMBean(VirtualMachine currentVm) throws IOException, AgentLoadException, AgentInitializationException {

		String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
		Properties props = currentVm.getAgentProperties();
		String connectorAddress = props.getProperty(CONNECTOR_ADDRESS);
		if (connectorAddress == null) {
			String agent = currentVm.getSystemProperties().getProperty("java.home") + File.separator + "lib"
					+ File.separator + "management-agent.jar";
			currentVm.loadAgent(agent);

			connectorAddress = currentVm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
			if (connectorAddress == null) {
				throw new RuntimeException("Unable to get connector address for the MBean");
			}
		}

		JMXServiceURL url = new JMXServiceURL(connectorAddress);
		JMXConnector jmxConnector = JMXConnectorFactory.connect(url);
		MBeanServerConnection mbeanConn = jmxConnector.getMBeanServerConnection();
		Set<ObjectName> beanSet = mbeanConn.queryNames(null, null);
		for (ObjectName on : beanSet) {
			if (FirstBloodAgentControllerMBean.MBEAN_OBJECT_NAMAE.equals(on.getCanonicalName())) {
				return JMX.newMBeanProxy(mbeanConn, on, FirstBloodAgentControllerMBean.class, true);
			}
		}

		throw new RuntimeException("Unable to get the MBean from VM");

	}
}
