package net.floodlightcontroller.forwarding;

import net.floodlightcontroller.core.IOFSwitch;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DHCPBindingItem {
	private IOFSwitch sw;
	private OFPort inPort;
	private IPv4Address IP;
	private MacAddress MAC;
	private boolean LEASE_STATUS;
	protected static final Logger log = LoggerFactory.getLogger(DHCPBindingItem.class);
	public DHCPBindingItem(){
		sw=null;
		inPort=null;
		IP= IPv4Address.NONE;
		MAC = MacAddress.NONE;
		LEASE_STATUS = false;
	}
	
	public IOFSwitch getSw() {
		return sw;
	}

	public void setSw(IOFSwitch sw) {
		this.sw = sw;
	}

	public OFPort getInPort() {
		return inPort;
	}

	public void setInPort(OFPort inPort) {
		this.inPort = inPort;
	}

	public IPv4Address getIP() {
		return IP;
	}

	public void setIP(IPv4Address iP) {
		IP = iP;
	}

	public MacAddress getMAC() {
		return MAC;
	}

	public void setMAC(MacAddress mAC) {
		MAC = mAC;
	}

	public boolean isLEASE_STATUS() {
		return LEASE_STATUS;
	}

	public void setLEASE_STATUS(boolean lEASE_STATUS) {
		LEASE_STATUS = lEASE_STATUS;
	}
	public void print() {
		log.info("sw: "+sw.getId() +"\n");
		log.info("c_MAC: "+MAC.toString() +"\n");
		log.info("c_IP: "+IP.toString() +"\n");
		//log.info("sw: "+sw.getId() +"\n");
	}
}
