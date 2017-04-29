package net.floodlightcontroller.forwarding;

import java.util.HashMap;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostWindowMap {
	protected static final Logger log = LoggerFactory.getLogger(HostWindowMap.class);
	private HashMap<IPv4Address, HostWindowEntry> map;
	
	public HostWindowMap() {
		map = new HashMap<IPv4Address, HostWindowEntry>();
	}
	
	public void addItem(IPv4Address ip) {
		if (!map.containsKey(ip)) {
//			log.info("host-window-map add host " + ip.toString());
			HostWindowEntry entry = new HostWindowEntry();
			map.put(ip, entry);
		}
	}
	public boolean containsKey(IPv4Address ip) {
		return map.containsKey(ip);
	}
	public HostWindowEntry get(IPv4Address ip) {
		return map.get(ip);
	}
	public void update(IPv4Address ip, int l, int n) {
		map.get(ip).limit = l;
		map.get(ip).number = n;
	}
}
