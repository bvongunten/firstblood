package ch.nostromo.firstblood.odyssey;

import java.io.IOException;

import javax.management.MBeanException;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

public class Step5 {

	public static void main(String...args) throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException, MBeanException {
		// Attach to lucky numbers, print out properties
		for (VirtualMachineDescriptor vmDescriptior : VirtualMachine.list()) {
		   if (vmDescriptior.displayName().contains("LuckyNumbers")) {
			   VirtualMachine victim = VirtualMachine.attach(vmDescriptior);
			 			   
			   victim.loadAgent("C:/dev/Private/github/firstblood/agent/target/agent-1.0-SNAPSHOT.jar");
			   
		   }
		}
	}
	
}
