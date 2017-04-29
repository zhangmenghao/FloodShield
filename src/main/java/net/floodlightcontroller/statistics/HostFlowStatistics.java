package net.floodlightcontroller.statistics;

import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostFlowStatistics {
	private static final Logger log = LoggerFactory.getLogger(HostFlowStatistics.class);
	private int limit;
	private int highFlowNumber;
	private int hardFlowNumber;
	private int lowFlowNumber;
	private int flowNumber;
	public static long FLOW_MATCH_HIGH_THRESHOLD = 10;
	
    public void setAll_flow_nmb(int number) {
        synchronized (this) {
            this.flowNumber = number;
        }
    }
    public void setHighFlowNumber(int number) {
    	synchronized (this) {
    		this.highFlowNumber = number;
    	}
    }
    public void setLowFlowNumber(int number) {
    	synchronized (this) {
    		this.lowFlowNumber = number;
    	}
    }
    public void setHardFlowNumber(int number) {
    	synchronized (this) {
    		this.hardFlowNumber = number;
    	}
    }
    public void setFlowNumber(int number) {
    	synchronized (this) {
    		this.flowNumber = number;
    	}
    }
    public void setLimit(int number) {
    	synchronized (this) {
    		this.limit = number;
    	}
    }
    public boolean forwardFlow() {
    	synchronized (this) {
    		log.info("examine limit = " + limit + " number is " + flowNumber);
    		return (flowNumber < limit);
    	}
    }
    public void print() {
    	log.info("flowNumber = " + flowNumber + " limit = " + limit);
    }
    
    public HostFlowStatistics() {
    	limit = 10;
    	highFlowNumber = 0;
    	lowFlowNumber = 0;
    	hardFlowNumber = 0;
    	flowNumber = 0;
    }
    public void addFlowNumber() {
    	synchronized (this) {
    		this.flowNumber += 1;
    	}
    }
    public int getLimit() {
    	return limit;
    }
    public int getNumber() {
    	return flowNumber;
    }
    
    public void updateByEntry(OFFlowStatsEntry entry) {
    	synchronized (this) {
            String id = entry.getMatch().toString();
            long packetCount = entry.getPacketCount().getValue();
            if (entry.getHardTimeout() == 0 && entry.getIdleTimeout() == 0) {
                this.hardFlowNumber += 1;
            }
            if (packetCount > FLOW_MATCH_HIGH_THRESHOLD) {
                this.highFlowNumber += 1;
            }
            if (packetCount < FLOW_MATCH_HIGH_THRESHOLD) {
                this.lowFlowNumber += 1;
            }
            this.flowNumber += 1;
            this.limit = 16 + hardFlowNumber + highFlowNumber;
    	}
    }
    
    public void update(HostFlowStatistics e) {
    	synchronized (this) {
        	this.limit = e.limit;
        	this.highFlowNumber = e.highFlowNumber;
        	this.lowFlowNumber = e.lowFlowNumber;
        	this.flowNumber = e.flowNumber;
        	this.hardFlowNumber = e.hardFlowNumber;	
    	}
    }
}
