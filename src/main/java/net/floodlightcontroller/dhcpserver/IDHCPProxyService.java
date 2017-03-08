package net.floodlightcontroller.dhcpserver;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.NodePortTuple;
import org.projectfloodlight.openflow.types.VlanVid;

import java.util.Collection;

public interface IDHCPProxyService extends IFloodlightService {

	public void enable();
	public void disable();
	public boolean isEnabled();
	
	public boolean addInstance(DHCPInstance instance);
	
	public DHCPInstance getInstance(String name);
	public DHCPInstance getInstance(NodePortTuple member);
	public DHCPInstance getInstance(VlanVid member);
	public Collection<DHCPInstance> getInstances();
	
	public boolean deleteInstance(String name);
}
