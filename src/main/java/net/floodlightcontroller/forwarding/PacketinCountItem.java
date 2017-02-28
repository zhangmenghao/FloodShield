package net.floodlightcontroller.forwarding;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;

public class PacketinCountItem {
	private DatapathId dpId;
    private OFPort portId;
    private IPv4Address ipv4;
    
    PacketinCountItem(){
    	dpId = null;
    	portId = null;
    	ipv4 = null;
    }
    
    PacketinCountItem(DatapathId dpid, OFPort port, IPv4Address ip){
    	dpId = dpid;
    	portId = port;
    	ipv4 = ip;
    }

	public DatapathId getDpId() {
		return dpId;
	}

	public void setDpId(DatapathId dpId) {
		this.dpId = dpId;
	}

	public OFPort getPortId() {
		return portId;
	}

	public void setPortId(OFPort portId) {
		this.portId = portId;
	}

	public IPv4Address getIpv4() {
		return ipv4;
	}

	public void setIpv4(IPv4Address ipv4) {
		this.ipv4 = ipv4;
	}
    
	@Override
	public int hashCode(){
		int hashCode = 1;
		hashCode=hashCode*31 + dpId.hashCode();
		hashCode=hashCode*31 + portId.hashCode();
		hashCode=hashCode*31 + ipv4.hashCode();
		return hashCode;
	}
	@Override
	public boolean equals(Object obj){
		if (obj instanceof PacketinCountItem)
		if (((PacketinCountItem)obj).dpId.equals(this.dpId) && ((PacketinCountItem)obj).portId.equals(this.portId) && ((PacketinCountItem)obj).ipv4.equals(this.ipv4)){
			return true;
		}
		return false;
	}


}
