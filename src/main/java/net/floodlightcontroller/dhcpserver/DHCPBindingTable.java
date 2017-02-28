package net.floodlightcontroller.dhcpserver;

import java.util.ArrayList;
import net.floodlightcontroller.core.IOFSwitch;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;

public class DHCPBindingTable{

	private ArrayList<DHCPBindingItem> dhcpbt;
	
	public DHCPBindingTable() {
		// TODO Auto-generated constructor stub
		dhcpbt = new ArrayList<DHCPBindingItem>();
	}

	public synchronized  boolean ishavaMac(MacAddress chaddr) {
		// TODO Auto-generated method stub
		for (DHCPBindingItem item : dhcpbt){
			if (item.getMAC().equals(chaddr)){
				return true;
			}
		}
		return false;
	}

	public synchronized void addnewItem(MacAddress chaddr, IOFSwitch sw, OFPort inPort) {
		// TODO Auto-generated method stub
		DHCPBindingItem item = new DHCPBindingItem();
		item.setMAC(chaddr);
		item.setSw(sw);
		item.setInPort(inPort);
		dhcpbt.add(item);
	}
	
	public void addnewItem(MacAddress chaddr, IOFSwitch sw, OFPort inPort,
			IPv4Address desiredIPAddr) {
		// TODO Auto-generated method stub
		DHCPBindingItem item = new DHCPBindingItem();
		item.setMAC(chaddr);
		item.setSw(sw);
		item.setInPort(inPort);
		item.setIP(desiredIPAddr);
		dhcpbt.add(item);
	}

	public void setIPFromMac(MacAddress chaddr, IPv4Address yiaddr) {
		// TODO Auto-generated method stub
		for (DHCPBindingItem item : dhcpbt){
			if (item.getMAC().equals(chaddr)){
				item.setIP(yiaddr);
			}
		}
	}

	public boolean getItemStatus(MacAddress chaddr) {
		// TODO Auto-generated method stub
		for (DHCPBindingItem item : dhcpbt){
			if (item.getMAC().equals(chaddr)){
				return item.isLEASE_STATUS();
			}
		}
		return false;
	}

	public DHCPBindingItem setItemStatus(MacAddress chaddr, boolean lEASE_STATUS) {
		// TODO Auto-generated method stub
		for (DHCPBindingItem item : dhcpbt){
			if (item.getMAC().equals(chaddr)){
				System.out.println(item.getSw()+" "+item.getInPort()+" "+item.getIP()+" "+item.getMAC()+" add");
				item.setLEASE_STATUS(lEASE_STATUS);
				return item;
			}
		}
		return null;
	}

	public DHCPBindingItem deleteItemFromMac(MacAddress chaddr) {
		// TODO Auto-generated method stub
		for (DHCPBindingItem item : dhcpbt){
			if (item.getMAC().equals(chaddr)){
				System.out.println(item.getSw()+" "+item.getInPort()+" "+item.getIP()+" "+item.getMAC()+" delete");
				dhcpbt.remove(item);
				return item;
			}
		}
		return null;
	}
	
}

