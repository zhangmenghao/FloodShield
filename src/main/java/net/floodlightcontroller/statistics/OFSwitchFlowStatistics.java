package net.floodlightcontroller.statistics;

import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by dalaoshe on 17-3-18.
 */
public class OFSwitchFlowStatistics {
    private static final Logger log = LoggerFactory.getLogger(OFSwitchFlowStatistics.class);
    private long all_flow_nmb = 0;
    private long hard_flow_nmb = 0;
    private long high_used_flow_nmb = 0;
    private long low_used_flow_nmb = 0;
    private DatapathId swID;
    public static long FLOW_MATCH_LOW_THRESHOLD = 10;
    public static long FLOW_MATCH_HIGH_THRESHOLD = 20;
    public void setAll_flow_nmb(long nmb) {
        synchronized (this) {
            this.all_flow_nmb = nmb;
        }
    }
    public void setHard_flow_nmb(long nmb) {
        synchronized (this) {
            this.hard_flow_nmb = nmb;
        }
    }
    public void setHigh_used_flow_nmb(long nmb) {
        synchronized (this) {
            this.high_used_flow_nmb = nmb;
        }
    }
    public void setLow_used_flow_nmb(long nmb) {
        synchronized (this) {
            this.low_used_flow_nmb = nmb;
        }
    }
    public void setSwID(DatapathId swId) {
        synchronized (this) {
            this.swID = swId;
        }
    }

    public long getAll_flow_nmb() {
        synchronized (this) {
            return this.all_flow_nmb;
        }
    }
    public long getHard_flow_nmb() {
        synchronized (this) {
            return this.hard_flow_nmb;
        }
    }
    public long getHigh_used_flow_nmb(){
        synchronized (this) {
            return this.high_used_flow_nmb;
        }
    }
    public long getLow_used_flow_nmb() {
        synchronized (this) {
            return this.low_used_flow_nmb;
        }
    }
    public DatapathId getSwID() {
        synchronized (this) {
            return this.swID;
        }
    }
    public double getInvalidRate() {
        synchronized (this) {
            return (double) low_used_flow_nmb / (double) all_flow_nmb;
        }
    }

    private void updateStastics(OFFlowStatsReply fsr) {
    	this.all_flow_nmb += fsr.getEntries().size();
        for (OFFlowStatsEntry pse : fsr.getEntries()) {
            long packetCount = pse.getPacketCount().getValue();
            String id = pse.getMatch().toString();
            if (pse.getHardTimeout() == 0 && pse.getIdleTimeout() == 0) {
                this.hard_flow_nmb++;
                continue;
            }
            if (packetCount > FLOW_MATCH_HIGH_THRESHOLD) {
                this.high_used_flow_nmb++;
            }
            if (packetCount < FLOW_MATCH_HIGH_THRESHOLD) {
                this.low_used_flow_nmb++;
            }
        }
    }

    public void updateStasticsByReplies(List<OFStatsReply> replies) {
       // log.info("1 reply size{}", replies.size());
        synchronized (this) {
            this.all_flow_nmb = 0;
            this.low_used_flow_nmb =  0;
            this.hard_flow_nmb = 0;
            this.high_used_flow_nmb = 0;
            for (OFStatsReply r : replies) {
                OFFlowStatsReply fsr = (OFFlowStatsReply) r;
                updateStastics(fsr);
            }
        }
    }
}
