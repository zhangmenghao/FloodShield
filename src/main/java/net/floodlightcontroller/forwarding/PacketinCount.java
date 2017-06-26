package net.floodlightcontroller.forwarding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class PacketinCount {
	protected static final Logger log = LoggerFactory.getLogger(PacketinCount.class);
	protected HashMap<PacketinCountItem, Long> packettime;
	protected HashMap<PacketinCountItem, Integer> packetcount;
	
	public PacketinCount(){
		packettime = new HashMap<PacketinCountItem, Long> ();
		packetcount = new HashMap<PacketinCountItem, Integer> ();
	}
	
	
	public synchronized boolean update(PacketinCountItem pci){
		Object counts =  packetcount.get(pci);
		if (counts == null)
		{
			packettime.put(pci, new Long(System.currentTimeMillis()));
			packetcount.put(pci, 1);
			//System.out.println("new find!");
		}
		else{
			int countss = (int)counts + 1;
			//System.out.println(countss);
			packetcount.put(pci, countss);
			if (countss == 10 ){
				if ((System.currentTimeMillis() - packettime.get(pci)) < 1000 ){
					//pass
					packettime.put(pci, new Long(System.currentTimeMillis()));
					packetcount.put(pci, 0);
					//System.out.println("issue flow rule!");
					return true;
				}
				packettime.put(pci, new Long(System.currentTimeMillis()));
				packetcount.put(pci, 0);
				log.info("src_ip:"+pci.getIpv4()+" counts {} , times {}",countss,(System.currentTimeMillis() - packettime.get(pci)));
			}
		}
		return false;
	}
}
