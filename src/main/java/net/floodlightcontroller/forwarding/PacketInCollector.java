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
	private static int TOTAL_TYPE = 0;
	private static int SINGLE_TYPE = 1;
	protected static final Logger log = LoggerFactory.getLogger(PacketInCollector.class);
	public int rateNumber;
	public long rate;
	private long rateLastTimeStamp;
	private LinkedList<Integer> numList;
	private int tempNum;
	public int number;
	private IPv4Address ip;
	private int collectorType;
	
	ScheduledExecutorService service;
	
	public int getNumber() {
		return number;
	}
	public IPv4Address getIP() {
		return ip;
	}
	
	public PacketInCollector() {
		this.rateNumber = 0;
		this.rateLastTimeStamp = 0;
		this.rate = -1;
		numList = new LinkedList<Integer>();
		this.tempNum = 0;
		this.number = 0;
		this.collectorType = TOTAL_TYPE;
		
		service = Executors.newSingleThreadScheduledExecutor();
		service.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.SECONDS);
	}
	public PacketInCollector(IPv4Address ip) {
		this.ip = ip;
		this.rateNumber = 0;
		this.rateLastTimeStamp = 0;
		this.rate = -1;
		numList = new LinkedList<Integer>();
		this.tempNum = 0;
		this.number = 0;
		this.collectorType = SINGLE_TYPE;
		
		service = Executors.newSingleThreadScheduledExecutor();
		service.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.SECONDS);
	}
	
	public boolean allowForward() {
		if (collectorType == SINGLE_TYPE && rate < 300 && rate > 0)  {
			return false;
		}
		if (collectorType == TOTAL_TYPE && rate < 1000 && rate > 0)  {
			return false;
		}
		if (collectorType == SINGLE_TYPE && number > 400) {
			return false;
		}
		return true;
	}
	
	public synchronized void updateNumber() {
		this.tempNum += 1;
	}
	public synchronized void updateRate() {
		long ts = System.currentTimeMillis();
		if (rateNumber == 0) {
			rateLastTimeStamp = ts;
		}
		this.rateNumber += 1;
		this.tempNum += 1;
		if (collectorType == SINGLE_TYPE) {
			if (rateNumber == 50) {
				rate = ts - rateLastTimeStamp;
				rateLastTimeStamp = ts;
				rateNumber = 0;
			}
		} else if (collectorType == TOTAL_TYPE) {
			if (rateNumber == 400000) {
				rate = ts - rateLastTimeStamp;
				rateLastTimeStamp = ts;
				rateNumber = 0;
			}
		}
	}
	public synchronized void resetRate() {
		this.rate = -1;
		this.rateNumber = 0;
	}
	
	Runnable runnable = new Runnable() {
		@Override
        public void run() {
        	if (collectorType == SINGLE_TYPE) {
        		if (System.currentTimeMillis() - rateLastTimeStamp > 1000) {
        			resetRate();
        		}
        	}
    		numList.add(tempNum);
    		if (numList.size() > 5) {
    			number = numList.get(numList.size()-1) - numList.get(numList.size()-6);
        		while (numList.size() > 6) numList.remove(0);
    		}
        }

    };
}
