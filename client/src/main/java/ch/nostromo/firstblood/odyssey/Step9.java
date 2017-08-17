package ch.nostromo.firstblood.odyssey;

import java.io.IOException;

import javax.management.MBeanException;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import ch.nostromo.firstblood.agent.FirstBloodAgentControllerMBean;

public class Step9 {

	public static void main(String... args) throws AttachNotSupportedException, IOException, AgentLoadException,
			AgentInitializationException, MBeanException {

		try {

			// Attach to lucky numbers, print out properties
			for (VirtualMachineDescriptor vmDescriptior : VirtualMachine.list()) {
				if (vmDescriptior.displayName().contains("LuckyNumbers")) {
					VirtualMachine victim = VirtualMachine.attach(vmDescriptior);

					victim.loadAgent("C:/dev/Private/github/firstblood/agent/target/agent-1.0-SNAPSHOT.jar");

					FirstBloodAgentControllerMBean mbean = MBeanTools.getMBean(victim);

					mbean.changeMethodBody("ch.nostromo.luckynumbers.LuckyNumbers", "drawNextNumber", "my code	");

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
