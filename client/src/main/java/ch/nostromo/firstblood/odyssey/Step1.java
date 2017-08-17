package ch.nostromo.firstblood.odyssey;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

public class Step1 {

	public static void main(String...args) {
		// List all VMs
		for (VirtualMachineDescriptor vmDescriptor : VirtualMachine.list()) {
			System.out.println(vmDescriptor.displayName());
		}
	}
	
}
