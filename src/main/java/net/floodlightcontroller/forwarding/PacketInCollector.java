package net.floodlightcontroller.forwarding;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.IRoutingDecision;

public class PacketInCollector {
	protected static final Logger log = LoggerFactory.getLogger(PacketInCollector.class);
	private int rateNumber;
	private long rate;
	private long rateLastTimeStamp;
	private LinkedList<Integer> numList;
	private int tempNum;
	private int number;
	private IPv4Address ip;
	
	ScheduledExecutorService service;
	
	public PacketInCollector() {
		this.rateNumber = 0;
		this.rateLastTimeStamp = 0;
		this.rate = -1;
		numList = new LinkedList<Integer>();
		this.tempNum = 0;
		this.number = 0;
		
		service = Executors.newSingleThreadScheduledExecutor();
		service.scheduleAtFixedRate(runnable, 0, 5, TimeUnit.SECONDS);
	}
	public PacketInCollector(IPv4Address ip) {
		this.ip = ip;
		this.rateNumber = 0;
		this.rateLastTimeStamp = 0;
		this.rate = -1;
		numList = new LinkedList<Integer>();
		this.tempNum = 0;
		this.number = 0;
		
		service = Executors.newSingleThreadScheduledExecutor();
		service.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.SECONDS);
	}
	
	public boolean allowForward() {
		if (rate < 1000 && rate > 0)  {
			return false;
		}
		if (numList.size() > 6) {
			if (number > 40) {
				return false;
			}
		}
		return true;
	}
	
	public synchronized void updateRate(long ts) {
		if (rate == -1) {
			rateLastTimeStamp = ts;
		}
		this.rateNumber += 1;
		this.tempNum += 1;
		if (rateNumber == 10) {
			rate = ts - rateLastTimeStamp;
			rateLastTimeStamp = ts;
			rateNumber = 0;
		}
	}
	public synchronized void resetRate() {
		this.rate = -1;
		this.rateNumber = 0;
	}
	
	Runnable runnable = new Runnable() {
        @Override
        public void run() {
    		numList.add(tempNum);
    		if (System.currentTimeMillis() - rateLastTimeStamp > 1000) {
    			resetRate();
    		}
    		if (numList.size() > 5) {
            	log.info("udpate ip = " + ip.toString() + " number = " + number + " rate = " + rate);
    			number = numList.get(numList.size()-1) - numList.get(numList.size()-6);
        		while (numList.size() > 6) numList.remove(0);
    		}
        }

    };
}
