package net.floodlightcontroller.forwarding;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.IRoutingDecision;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dalaoshe on 17-3-13.
 */
public class PacketInMonitor {
    protected static final Logger log = LoggerFactory.getLogger(PacketInMonitor.class);
    protected IOFSwitchService switchService;
    protected PacketinCount pic;


    public PacketInMonitor() {

    }
    public void registerPacketInMonitor(IOFSwitchService switchService) {
        this.pic = new PacketinCount();
        this.switchService = switchService;
    }

    public class Monitor implements Runnable{
        IOFSwitch sw;
        OFPort srcPort;
        DatapathId srcSw ;
        IPv4 ip ;
        IPv4Address srcIp ;
        IRoutingDecision decision;
        public Monitor(IOFSwitch sw, OFPort srcPort, DatapathId srcSw , IPv4 ip , IPv4Address srcIp , IRoutingDecision decision){
            this.sw = sw;
            this.srcPort= srcPort;
            this.srcSw = srcSw;
            this.ip = ip;
            this.srcIp = srcIp;
            this.decision = decision;
        }
        @Override
        public void run() {
            // TODO Auto-generated method stub
            if (pic.update(new PacketinCountItem(srcSw, srcPort, srcIp))){
                doDropIp(sw, srcPort, srcIp, decision);
            }
        }

    }

    private void doDropIp(IOFSwitch sw, OFPort srcPort, IPv4Address ipv4, IRoutingDecision decision) {
        // TODO Auto-generated method stub
        Match m = createMatchFromPacketIp(sw, srcPort, ipv4);
        OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
        List<OFAction> actions = new ArrayList<OFAction>(); // set no action to drop


        OFFlowAdd filterFlow = sw.getOFFactory().buildFlowAdd()
                .setMatch(m)
                .setHardTimeout(10)
                .setIdleTimeout(10)
                .setTableId(TableId.of(0))
                .setPriority(4)
                .setActions(actions)
                .build();
        log.info("drop id{}",sw.getId());

        if (log.isDebugEnabled()) {
            log.debug("write drop flow-mod sw={} match={} flow-mod={}",
                      new Object[] { sw, m, fmb.build() });
        }
        if(sw.write(filterFlow)) {
            log.info("drop ok");
        }
        else {
            log.info("drop fail");
        }
    }

    private Match createMatchFromPacketIp(IOFSwitch sw, OFPort srcPort, IPv4Address ip) {
        // TODO Auto-generated method stub
        Match.Builder mb=sw.getOFFactory().buildMatch();
        //mb.setExact(MatchField.IN_PORT, inPort);
        mb.setExact(MatchField.IN_PORT, srcPort);
        mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
        mb.setExact(MatchField.IPV4_SRC, ip);
        return mb.build();
    }


    public void doMonitor(IOFSwitch sw, OFPort srcPort, DatapathId srcSw, IPv4Address srcIp, IPv4 ip , IRoutingDecision decision) {
        new Thread(new Monitor(sw, srcPort, srcSw ,ip ,srcIp ,decision)).start();
    }
}
