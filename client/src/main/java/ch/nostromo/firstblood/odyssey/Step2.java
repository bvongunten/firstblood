package ch.nostromo.firstblood.odyssey;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

public class Step2 {

	public static void main(String...args) throws AttachNotSupportedException, IOException {
		// Attach to lucky numbers, print out properties
		for (VirtualMachineDescriptor vmDescriptior : VirtualMachine.list()) {
		   if (vmDescriptior.displayName().contains("LuckyNumbers")) {
			   VirtualMachine victim = VirtualMachine.attach(vmDescriptior);
			   Properties systemProperties = victim.getSystemProperties();
			   for(Entry<Object, Object> e : systemProperties.entrySet()) {
		            System.out.println(e.getKey() + " = " + e.getValue());
		        }
			   
		   }
		}
	}
	
}
