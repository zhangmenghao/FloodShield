package net.floodlightcontroller.forwarding;

import net.floodlightcontroller.statistics.StatisticsCollector;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;

import java.util.HashMap;

public class DatapathCollector {
	HashMap<IPv4Address, PacketInCollector> map;
	DatapathId id;
	
	public DatapathCollector(DatapathId id) {
		this.id = id;
		this.map = new HashMap<IPv4Address, PacketInCollector>();
	}
	
	public synchronized void udpate(IPv4Address ip) {
		map.get(ip).updateNumber();
	}
	
	public IPv4Address getMaxNumberIP() {
		int number = -1;
		IPv4Address ip = null;
		for (PacketInCollector collector : map.values()) {
			if (collector.getNumber() > number) {
				number = collector.getNumber();
				ip = collector.getIP();
			}
		}
		return ip;
	}
	
	public int getNum() {
		int number = (int) StatisticsCollector.switchFlowStatisticsHashMap.get(id).getAll_flow_nmb();
		return number;
	}
}
