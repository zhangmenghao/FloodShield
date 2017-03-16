/**
 *    Copyright 2011, Big Switch Networks, Inc.
 *    Originally created by David Erickson, Stanford University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.floodlightcontroller.forwarding;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import net.floodlightcontroller.config.DDosProtectionConfig;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.debugcounter.IDebugCounterService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.packet.*;
import net.floodlightcontroller.routing.ForwardingBase;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.IRoutingDecisionChangedListener;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Path;
import net.floodlightcontroller.routing.RoutingDecision;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.util.FlowModUtils;
import net.floodlightcontroller.util.MatchUtils;
import net.floodlightcontroller.util.OFDPAUtils;
import net.floodlightcontroller.util.OFMessageUtils;
import net.floodlightcontroller.util.OFPortMode;
import net.floodlightcontroller.util.OFPortModeTuple;
import net.floodlightcontroller.util.ParseUtils;

import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionPopMpls;
import org.projectfloodlight.openflow.protocol.action.OFActionPushMpls;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActionSetMplsTtl;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.Masked;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanVid;
import org.python.google.common.collect.ImmutableList;
import org.python.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.floodlightcontroller.dhcpserver.DHCPServer.DHCP_MSG_TYPE_ACK;
import static net.floodlightcontroller.dhcpserver.DHCPServer.DHCP_MSG_TYPE_OFFER;
import static net.floodlightcontroller.forwarding.Forwarding.flowSetIdRegistry;

public class MplsForwarding extends ForwardingBase implements IFloodlightModule, IOFSwitchListener, ILinkDiscoveryListener, IRoutingDecisionChangedListener {
    protected static final Logger log = LoggerFactory.getLogger(MplsForwarding.class);

    /*
     * Cookies are 64 bits:
     * Example: 0x0123456789ABCDEF
     * App ID:  0xFFF0000000000000
     * User:    0x000FFFFFFFFFFFFF
     * 
     * Of the user portion, we further subdivide into routing decision 
     * bits and flowset bits. The former relates the flow to routing
     * decisions, such as firewall allow or deny/drop. It allows for 
     * modification of the flows upon a future change in the routing 
     * decision. The latter indicates a "family" of flows or "flowset" 
     * used to complete an end-to-end connection between two devices
     * or hosts in the network. It is used to assist in the entire
     * flowset removal upon a link or port down event anywhere along
     * the path. This is required in order to allow a new path to be
     * used and a new flowset installed.
     * 
     * TODO: shrink these masks if you need to add more subfields
     * or need to allow for a larger number of routing decisions
     * or flowsets
     */
    private static final boolean SPEED_MONITOR = true;
    private static final short DECISION_BITS = 24;
    private static final short DECISION_SHIFT = 0;
    private static final long DECISION_MASK = ((1L << DECISION_BITS) - 1) << DECISION_SHIFT;

    private static final short FLOWSET_BITS = 28;
    protected static final short FLOWSET_SHIFT = DECISION_BITS;
    private static final long FLOWSET_MASK = ((1L << FLOWSET_BITS) - 1) << FLOWSET_SHIFT;
    private static final long FLOWSET_MAX = (long) (Math.pow(2, FLOWSET_BITS) - 1);
    protected static FlowSetIdRegistry flowSetIdRegistry;
    private DHCPPacketProcessor dhcpPacketProcessor;
    private PacketInMonitor packetInMonitor;
	protected FlowTable flowtable;
	protected PacketinCount pic;

    protected static class FlowSetIdRegistry {
        private volatile Map<NodePortTuple, Set<U64>> nptToFlowSetIds;
        private volatile Map<U64, Set<NodePortTuple>> flowSetIdToNpts;
        
        private volatile long flowSetGenerator = -1;

        private static volatile FlowSetIdRegistry instance;

        private FlowSetIdRegistry() {
            nptToFlowSetIds = new ConcurrentHashMap<NodePortTuple, Set<U64>>();
            flowSetIdToNpts = new ConcurrentHashMap<U64, Set<NodePortTuple>>();
        }

        protected static FlowSetIdRegistry getInstance() {
            if (instance == null) {
                instance = new FlowSetIdRegistry();
            }
            return instance;
        }
        
        /**
         * Only for use by unit test to help w/ordering
         * @param seed
         */
        protected void seedFlowSetIdForUnitTest(int seed) {
            flowSetGenerator = seed;
        }
        
        protected synchronized U64 generateFlowSetId() {
            flowSetGenerator += 1;
            if (flowSetGenerator == FLOWSET_MAX) {
                flowSetGenerator = 0;
                log.warn("Flowset IDs have exceeded capacity of {}. Flowset ID generator resetting back to 0", FLOWSET_MAX);
            }
            U64 id = U64.of(flowSetGenerator << FLOWSET_SHIFT);
            log.debug("Generating flowset ID {}, shifted {}", flowSetGenerator, id);
            return id;
        }

        private void registerFlowSetId(NodePortTuple npt, U64 flowSetId) {
            if (nptToFlowSetIds.containsKey(npt)) {
                Set<U64> ids = nptToFlowSetIds.get(npt);
                ids.add(flowSetId);
            } else {
                Set<U64> ids = new HashSet<U64>();
                ids.add(flowSetId);
                nptToFlowSetIds.put(npt, ids);
            }  

            if (flowSetIdToNpts.containsKey(flowSetId)) {
                Set<NodePortTuple> npts = flowSetIdToNpts.get(flowSetId);
                npts.add(npt);
            } else {
                Set<NodePortTuple> npts = new HashSet<NodePortTuple>();
                npts.add(npt);
                flowSetIdToNpts.put(flowSetId, npts);
            }
        }

        private Set<U64> getFlowSetIds(NodePortTuple npt) {
            return nptToFlowSetIds.get(npt);
        }

        private Set<NodePortTuple> getNodePortTuples(U64 flowSetId) {
            return flowSetIdToNpts.get(flowSetId);
        }

        private void removeNodePortTuple(NodePortTuple npt) {
            nptToFlowSetIds.remove(npt);

            Iterator<Set<NodePortTuple>> itr = flowSetIdToNpts.values().iterator();
            while (itr.hasNext()) {
                Set<NodePortTuple> npts = itr.next();
                npts.remove(npt);
            }
        }

        private void removeExpiredFlowSetId(U64 flowSetId, NodePortTuple avoid, Iterator<U64> avoidItr) {
            flowSetIdToNpts.remove(flowSetId);

            Iterator<Entry<NodePortTuple, Set<U64>>> itr = nptToFlowSetIds.entrySet().iterator();
            boolean removed = false;
            while (itr.hasNext()) {
                Entry<NodePortTuple, Set<U64>> e = itr.next();
                if (e.getKey().equals(avoid) && ! removed) {
                    avoidItr.remove();
                    removed = true;
                } else {
                    Set<U64> ids = e.getValue();
                    ids.remove(flowSetId);
                }
            }
        }
    }//end this class FlowSetIdRegistry
 
    
    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {

        switch (msg.getType()) {
        case PACKET_IN:
            IRoutingDecision decision = null;
            if (cntx != null) {
                decision = RoutingDecision.rtStore.get(cntx, IRoutingDecision.CONTEXT_DECISION);
            }
            if(this.dhcpPacketProcessor.doDHCPPacketProcess(sw,msg,cntx)) break;
            if(this.dhcpPacketProcessor.isDHCPPacket(IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD))) {
                log.info("request");
                doFlood(sw,(OFPacketIn)msg,decision,cntx);
            }
            return this.processPacketInMessage(sw, (OFPacketIn) msg, decision, cntx);
            
		case FLOW_REMOVED:
			return this.processFlowRemovedMessage(sw, (OFFlowRemoved) msg, cntx);
            
        default:
            break;
        }
        return Command.CONTINUE;
    }

    private void pushDHCPReplyToClient(IOFSwitch sw, OFPacketIn pi, OFPort outport, FloodlightContext cntx) {
        OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(sw.getOFFactory().actions().output(outport, Integer.MAX_VALUE));
        pob.setActions(actions);
        pob.setBufferId(OFBufferId.NO_BUFFER);
        if (pob.getBufferId().equals(OFBufferId.NO_BUFFER)) {
            byte[] packetData = pi.getData();
            pob.setData(packetData);
        }
        messageDamper.write(sw, pob.build());
    }

    private void writeIPMACBindFlowToSw(IOFSwitch sw, IPv4Address ip, MacAddress mac) {
        Match.Builder mb = sw.getOFFactory().buildMatch();
        mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
        mb.setExact(MatchField.IPV4_SRC, ip);
        mb.setExact(MatchField.ETH_SRC, mac);

        Match.Builder mb2 = sw.getOFFactory().buildMatch();
        mb2.setExact(MatchField.ETH_TYPE, EthType.ARP);
       // mb2.setExact(MatchField.ETH_SRC, mac);

        List<OFAction> actions = new ArrayList<OFAction>();
        OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
        aob.setPort(OFPort.CONTROLLER);
        aob.setMaxLen(Integer.MAX_VALUE);
        actions.add(aob.build());

        OFFlowAdd defaultFlow3 = sw.getOFFactory().buildFlowAdd()
                .setMatch(mb.build())
                .setTableId(TableId.of(0))
                .setPriority(1)
                .setActions(actions)
                .build();

        OFFlowAdd defaultFlow2 = sw.getOFFactory().buildFlowAdd()
                .setMatch(mb2.build())
                .setTableId(TableId.of(0))
                .setPriority(1)
                .setActions(actions)
                .build();

        sw.write(defaultFlow3);
        sw.write(defaultFlow2);
    }
    private net.floodlightcontroller.core.IListener.Command processFlowRemovedMessage(IOFSwitch sw, OFFlowRemoved msg,
			FloodlightContext cntx) {
    	//long t1 = System.nanoTime();
    	if (msg.getMatch().isExact(MatchField.MPLS_LABEL))
		{
			int label=msg.getMatch().get(MatchField.MPLS_LABEL).getRaw();
			if (flowtable.restorelabel(label))
			{
				//System.out.println(label);
			}
		}
		//long t2 = System.nanoTime();
		//time_flow_removed = time_flow_removed + t2 - t1; 
		//System.out.println("time_flow_removed  " + time_flow_removed/1000+"  " + ( t2 - t1 )/1000);
		return  Command.CONTINUE;
	}

    class monitor implements Runnable{
    	IOFSwitch sw;
    	OFPort srcPort;
        DatapathId srcSw ;
    	IPv4 ip ;
    	IPv4Address srcIp ;
    	IRoutingDecision decision;
    	monitor(IOFSwitch sw, OFPort srcPort, DatapathId srcSw ,IPv4 ip ,IPv4Address srcIp ,IRoutingDecision decision){
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
        	//counts = counts +1;
        	//log.info(srcSw + " " + srcPort + " "+srcIp+"  aaa");
		}
    	
    }
    
    public DHCP getDHCPPayload(Ethernet eth){
    	if (eth.getEtherType() == EthType.IPv4) { /* shallow compare is okay for EthType */
			IPv4 IPv4Payload = (IPv4) eth.getPayload();
			if (IPv4Payload.getProtocol() == IpProtocol.UDP) { /* shallow compare also okay for IpProtocol */
				UDP UDPPayload = (UDP) IPv4Payload.getPayload();
				if ((UDPPayload.getDestinationPort().equals(UDP.DHCP_SERVER_PORT) /* TransportPort must be deep though */
						|| UDPPayload.getDestinationPort().equals(UDP.DHCP_CLIENT_PORT))
						&& (UDPPayload.getSourcePort().equals(UDP.DHCP_SERVER_PORT)
								|| UDPPayload.getSourcePort().equals(UDP.DHCP_CLIENT_PORT))){
                    DHCP DHCPPayload = (DHCP) UDPPayload.getPayload();
				    return DHCPPayload;
				}
			}
			return null;
    	}
    	return null;
    }

    private boolean isDHCPServerPacket(DHCP pi) {
        if(Arrays.equals(pi.getOption(DHCP.DHCPOptionCode.OptionCode_MessageType).getData(), DHCP_MSG_TYPE_ACK) ||
                Arrays.equals(pi.getOption(DHCP.DHCPOptionCode.OptionCode_MessageType).getData(), DHCP_MSG_TYPE_OFFER) ) {
            return true;
        }
        return false;
    }
    
	@Override
    public Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi, IRoutingDecision decision, FloodlightContext cntx) {
        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        // We found a routing decision (i.e. Firewall is enabled... it's the only thing that makes RoutingDecisions)
        if (eth.getEtherType() == EthType.IPv4 && SPEED_MONITOR) { 
        	OFPort srcPort = OFMessageUtils.getInPort(pi);
            DatapathId srcSw = sw.getId();
        	IPv4 ip = (IPv4) eth.getPayload();
        	IPv4Address srcIp = ip.getSourceAddress();
        	this.packetInMonitor.doMonitor(sw,srcPort,srcSw,srcIp,ip,decision);
        	//new Thread(new monitor(sw, srcPort, srcSw ,ip ,srcIp ,decision)).start();
        	/*if (pic.update(new PacketinCountItem(srcSw, srcPort, srcIp))){
        		doDropIp(sw, srcPort, srcIp, decision);
        	}
        	//counts = counts +1;
        	log.info(srcSw + " " + srcPort + " "+srcIp+"  aaa");*/
        }
        
        if (decision != null) {
            if (log.isTraceEnabled()) {
                log.trace("Forwarding decision={} was made for PacketIn={}", decision.getRoutingAction().toString(), pi);
            }

            switch(decision.getRoutingAction()) {
            case NONE:
                // don't do anything
                return Command.CONTINUE;
            case FORWARD_OR_FLOOD:
            case FORWARD:
                doForwardFlow(sw, pi, decision, cntx, false);
                return Command.CONTINUE;
            case MULTICAST:
                // treat as broadcast
                doFlood(sw, pi, decision, cntx);
                return Command.CONTINUE;
            case DROP:
                doDropFlow(sw, pi, decision, cntx);
                return Command.CONTINUE;
            default:
                log.error("Unexpected decision made for this packet-in={}", pi, decision.getRoutingAction());
                return Command.CONTINUE;
            }
        } else { // No routing decision was found. Forward to destination or flood if bcast or mcast.
            if (log.isTraceEnabled()) {
                log.trace("No decision was made for PacketIn={}, forwarding", pi);
            }

            if (eth.isBroadcast() || eth.isMulticast()) {
            	if (getDHCPPayload(eth) != null){
            		//do nothing
            	}
            	else{
            		doFlood(sw, pi, decision, cntx);
            	}
            } else {
                doForwardFlow(sw, pi, decision, cntx, false);
            }
        }

        return Command.CONTINUE;
    }

    private void doDropIp(IOFSwitch sw, OFPort srcPort, IPv4Address ipv4, IRoutingDecision decision) {
		// TODO Auto-generated method stub
    	Match m = createMatchFromPacketIp(sw, srcPort, ipv4);
    	OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
        List<OFAction> actions = new ArrayList<OFAction>(); // set no action to drop
        U64 flowSetId = flowSetIdRegistry.generateFlowSetId();
        U64 cookie = makeForwardingCookie(decision, flowSetId); 

        /* If link goes down, we'll remember to remove this flow */
        if (! m.isFullyWildcarded(MatchField.IN_PORT)) {
            flowSetIdRegistry.registerFlowSetId(new NodePortTuple(sw.getId(), m.get(MatchField.IN_PORT)), flowSetId);
        }

        log.info("Dropping");
        fmb.setCookie(cookie)
        .setHardTimeout(5)
        .setIdleTimeout(5)
        .setBufferId(OFBufferId.NO_BUFFER) 
        .setMatch(m)
        .setPriority(FLOWMOD_DEFAULT_PRIORITY);

        FlowModUtils.setActions(fmb, actions, sw);

        /* Configure for particular switch pipeline */
        if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) != 0) {
            fmb.setTableId(FLOWMOD_DEFAULT_TABLE_ID);
        }

        if (log.isDebugEnabled()) {
            log.debug("write drop flow-mod sw={} match={} flow-mod={}",
                    new Object[] { sw, m, fmb.build() });
        }
        boolean dampened = messageDamper.write(sw, fmb.build());
        log.debug("OFMessage dampened: {}", dampened);
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

	/**
     * Builds a cookie that includes routing decision information.
     *
     * @param decision The routing decision providing a descriptor, or null
     * @return A cookie with our app id and the required fields masked-in
     */
    protected U64 makeForwardingCookie(IRoutingDecision decision, U64 flowSetId) {
        long user_fields = 0;

        U64 decision_cookie = (decision == null) ? null : decision.getDescriptor();
        if (decision_cookie != null) {
            user_fields |= AppCookie.extractUser(decision_cookie) & DECISION_MASK;
        }

        if (flowSetId != null) {
            user_fields |= AppCookie.extractUser(flowSetId) & FLOWSET_MASK;
        }

        // TODO: Mask in any other required fields here

        if (user_fields == 0) {
            return DEFAULT_FORWARDING_COOKIE;
        }
        return AppCookie.makeCookie(FORWARDING_APP_ID, user_fields);
    }

    public U64 makeCookie(IRoutingDecision decision) {
        long user_fields = 0;
        U64 flowSetId = flowSetIdRegistry.generateFlowSetId();
        U64 decision_cookie = (decision == null) ? null : decision.getDescriptor();
        if (decision_cookie != null) {
            user_fields |= AppCookie.extractUser(decision_cookie) & DECISION_MASK;
        }

        if (flowSetId != null) {
            user_fields |= AppCookie.extractUser(flowSetId) & FLOWSET_MASK;
        }

        // TODO: Mask in any other required fields here

        if (user_fields == 0) {
            return DEFAULT_FORWARDING_COOKIE;
        }
        return AppCookie.makeCookie(FORWARDING_APP_ID, user_fields);
    }

    /** Called when the handleDecisionChange is triggered by an event (routing decision was changed in firewall).
     *  
     *  @param changedDecisions Masked routing descriptors for flows that should be deleted from the switch.
     */
    @Override
    public void routingDecisionChanged(Iterable<Masked<U64>> changedDecisions) {
        deleteFlowsByDescriptor(changedDecisions);
    }

    /**
     * Converts a sequence of masked IRoutingDecision descriptors into masked Forwarding cookies.
     *
     * This generates a list of masked cookies that can then be matched in flow-mod messages.
     *
     * @param maskedDescriptors A sequence of masked cookies describing IRoutingDecision descriptors
     * @return A collection of masked cookies suitable for flow-mod operations
     */
    protected Collection<Masked<U64>> convertRoutingDecisionDescriptors(Iterable<Masked<U64>> maskedDescriptors) {
        if (maskedDescriptors == null) {
            return null;
        }

        ImmutableList.Builder<Masked<U64>> resultBuilder = ImmutableList.builder();
        for (Masked<U64> maskedDescriptor : maskedDescriptors) {
            long user_mask = AppCookie.extractUser(maskedDescriptor.getMask()) & DECISION_MASK;
            long user_value = AppCookie.extractUser(maskedDescriptor.getValue()) & user_mask;

            // TODO combine in any other cookie fields you need here.

            resultBuilder.add(
                    Masked.of(
                            AppCookie.makeCookie(FORWARDING_APP_ID, user_value),
                            AppCookie.getAppFieldMask().or(U64.of(user_mask))
                            )
                    );
        }

        return resultBuilder.build();
    }

    /**
     * On all active switches, deletes all flows matching the IRoutingDecision descriptors provided
     * as arguments.
     *
     * @param descriptors The descriptors and masks describing which flows to delete.
     */
    protected void deleteFlowsByDescriptor(Iterable<Masked<U64>> descriptors) {
        Collection<Masked<U64>> masked_cookies = convertRoutingDecisionDescriptors(descriptors);

        if (masked_cookies != null && !masked_cookies.isEmpty()) {
            Map<OFVersion, List<OFMessage>> cache = Maps.newHashMap();

            for (DatapathId dpid : switchService.getAllSwitchDpids()) {
                IOFSwitch sw = switchService.getActiveSwitch(dpid);
                if (sw == null) {
                    continue;
                }

                OFVersion ver = sw.getOFFactory().getVersion();
                if (cache.containsKey(ver)) {
                    sw.write(cache.get(ver));
                } else {
                    ImmutableList.Builder<OFMessage> msgsBuilder = ImmutableList.builder();
                    for (Masked<U64> masked_cookie : masked_cookies) {
                        msgsBuilder.add(
                                sw.getOFFactory().buildFlowDelete()
                                .setCookie(masked_cookie.getValue())
                                .setCookieMask(masked_cookie.getMask())
                                .build()
                                );
                    }

                    List<OFMessage> msgs = msgsBuilder.build();
                    sw.write(msgs);
                    cache.put(ver, msgs);
                }
            }
        }
    }


    protected void doDropFlow(IOFSwitch sw, OFPacketIn pi, IRoutingDecision decision, FloodlightContext cntx) {
        OFPort inPort = OFMessageUtils.getInPort(pi);
        Match m = createMatchFromPacket(sw, inPort, cntx);
        OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
        List<OFAction> actions = new ArrayList<OFAction>(); // set no action to drop
        U64 flowSetId = flowSetIdRegistry.generateFlowSetId();
        U64 cookie = makeForwardingCookie(decision, flowSetId); 

        /* If link goes down, we'll remember to remove this flow */
        if (! m.isFullyWildcarded(MatchField.IN_PORT)) {
            flowSetIdRegistry.registerFlowSetId(new NodePortTuple(sw.getId(), m.get(MatchField.IN_PORT)), flowSetId);
        }

        log.info("Dropping");
        fmb.setCookie(cookie)
        .setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
        .setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
        .setBufferId(OFBufferId.NO_BUFFER) 
        .setMatch(m)
        .setPriority(FLOWMOD_DEFAULT_PRIORITY);

        FlowModUtils.setActions(fmb, actions, sw);

        /* Configure for particular switch pipeline */
        if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) != 0) {
            fmb.setTableId(FLOWMOD_DEFAULT_TABLE_ID);
        }

        if (log.isDebugEnabled()) {
            log.debug("write drop flow-mod sw={} match={} flow-mod={}",
                    new Object[] { sw, m, fmb.build() });
        }
        boolean dampened = messageDamper.write(sw, fmb.build());
        log.debug("OFMessage dampened: {}", dampened);
    }
    
    public boolean pushRoute(Path route, OFPacketIn pi,
            DatapathId pinSwitch, U64 cookie, FloodlightContext cntx,
            boolean requestFlowRemovedNotification, OFFlowModCommand flowModCommand) {

		//long t1 = System.nanoTime();
		boolean packetOutSent = false;
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		List<NodePortTuple> switchPortList = route.getPath();
			
		int switchnum=switchPortList.size()/2;
		int []label =new int[switchnum-1]; 
		int []flag=new int[switchnum-1];
		int []ttl=new int[switchnum-1];
		
		for (int indx = switchPortList.size() - 1; indx > 2; indx -= 2) {	//prepare labels for switches 
			DatapathId switchDPID = switchPortList.get(indx).getNodeId();
			IOFSwitch sw = switchService.getSwitch(switchDPID);
			if (sw == null) {
				if (log.isWarnEnabled()) {
					log.warn("Unable to push route, switch at DPID {} " + "not available", switchDPID);
				}
				return packetOutSent;
			}
			OFPort outPort = switchPortList.get(indx).getPortId();
			
			List<OFAction> actions = new ArrayList<OFAction>();
			FlowItem fi = new FlowItem();
			if (indx==switchPortList.size() - 1)//last switch, modify ethtype
			{			
					OFActionPopMpls.Builder apmb = sw.getOFFactory().actions().buildPopMpls();
					apmb.setEthertype(eth.getEtherType());
					actions.add(apmb.build());
					OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
					aob.setPort(outPort);
					aob.setMaxLen(Integer.MAX_VALUE);
					actions.add(aob.build());
					
					fi.setActions(actions);
					fi.setSw(sw);
					
					flag[switchnum-1-(indx-1)/2] = flowtable.ishavelabel(fi);
					label[switchnum-1-(indx-1)/2]=flag[switchnum-1-(indx-1)/2];
					if (flag[switchnum-1-(indx-1)/2] == -1){
						label[switchnum-1-(indx-1)/2] = flowtable.getnewlabel(fi);
					}
					ttl[switchnum-1-(indx-1)/2]=switchnum-1-(indx-1)/2+2;
			}
			else//core switch, pop mpls and do action according to the mpls label
			{
					OFActionPopMpls.Builder apmb = sw.getOFFactory().actions().buildPopMpls();
					apmb.setEthertype(EthType.MPLS_UNICAST);
					actions.add(apmb.build());
					OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
					aob.setPort(outPort);
					aob.setMaxLen(Integer.MAX_VALUE);
					actions.add(aob.build());
					
					fi.setActions(actions);
					fi.setSw(sw);
					
					flag[switchnum-1-(indx-1)/2] = flowtable.ishavelabel(fi);
					label[switchnum-1-(indx-1)/2]=flag[switchnum-1-(indx-1)/2];
					if (flag[switchnum-1-(indx-1)/2] == -1){
						label[switchnum-1-(indx-1)/2] = flowtable.getnewlabel(fi);
					}
					ttl[switchnum-1-(indx-1)/2]=switchnum-1-(indx-1)/2+2;
			}
			//System.out.println(fi.hashCode());
			//System.out.println(flag[switchnum-1-(indx-1)/2] +" "+label[switchnum-1-(indx-1)/2] );
		}
		
		
		for (int indx = switchPortList.size() - 1; indx > 0; indx -= 2) {//reverse 
			// indx and indx-1 will always have the same switch DPID.
			DatapathId switchDPID = switchPortList.get(indx).getNodeId();
			IOFSwitch sw = switchService.getSwitch(switchDPID);
			
			if (sw == null) {
				if (log.isWarnEnabled()) {
					log.warn("Unable to push route, switch at DPID {} " + "not available", switchDPID);
				}
				return packetOutSent;
			}
			
			// need to build flow mod based on what type it is. Cannot set command later
			OFFlowMod.Builder fmb;
			switch (flowModCommand) {
			case ADD:
				fmb = sw.getOFFactory().buildFlowAdd();
				break;
			case DELETE:
				fmb = sw.getOFFactory().buildFlowDelete();
				break;
			case DELETE_STRICT:
				fmb = sw.getOFFactory().buildFlowDeleteStrict();
				break;
			case MODIFY:
				fmb = sw.getOFFactory().buildFlowModify();
				break;
			default:
				log.error("Could not decode OFFlowModCommand. Using MODIFY_STRICT. (Should another be used as the default?)");        
			case MODIFY_STRICT:
				fmb = sw.getOFFactory().buildFlowModifyStrict();
				break;			
			}
			
			if (indx==1)//the first switch, push a lot of mpls
			{
				List<OFAction> actions = new ArrayList<OFAction>();
	 			//Match.Builder mb = MatchUtils.convertToVersion(match, sw.getOFFactory().getVersion());
				// set input and output ports on the switch
				OFPort outPort = switchPortList.get(indx).getPortId();
				OFPort inPort = switchPortList.get(indx - 1).getPortId();
				//mb.setExact(MatchField.IN_PORT, inPort);
				Match match = createMatchFromPacket(sw, inPort, cntx);
				Match.Builder mb = MatchUtils.convertToVersion(match, sw.getOFFactory().getVersion());
				mb.setExact(MatchField.IN_PORT, inPort);

				for (int k = 0;k < switchnum-1;k ++)
				{
					/*
					OFActionPushMpls.Builder apmb = sw.getOFFactory().actions().buildPushMpls();
					apmb.setEthertype(EthType.MPLS_UNICAST);
					OFOxms oxms = sw.getOFFactory().oxms();
					OFActionSetMplsTtl.Builder setmplsttl = sw.getOFFactory().actions().buildSetMplsTtl();
					OFActionSetField setmplslabel = sw.getOFFactory().actions().buildSetField().setField(oxms.buildMplsLabel().setValue((U32.of(label[k]))).build()).build();
					setmplsttl.setMplsTtl((short)ttl[k]);
					//setmplslabel.setMplsLabel((long)label[k]);
					actions.add(apmb.build());
					actions.add(setmplsttl.build());
					actions.add(setmplslabel);*/
					OFActionPushMpls.Builder apmb = sw.getOFFactory().actions().buildPushMpls();
					apmb.setEthertype(EthType.MPLS_UNICAST);
					OFOxms oxms = sw.getOFFactory().oxms();
					OFActionSetMplsTtl.Builder setmplsttl = sw.getOFFactory().actions().buildSetMplsTtl();
					OFActionSetField.Builder setfield = sw.getOFFactory().actions().buildSetField();
					setmplsttl.setMplsTtl((short)ttl[k]);
					setfield.setField(oxms.buildMplsLabel().setValue(U32.of((long)label[k])).build());
					actions.add(apmb.build());
					actions.add(setmplsttl.build());
					actions.add(setfield.build());
				}
				OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
				aob.setPort(outPort);
				aob.setMaxLen(Integer.MAX_VALUE);
				actions.add(aob.build());
				
				fmb.setMatch(mb.build())
				.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
				.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
				.setBufferId(OFBufferId.NO_BUFFER)
				.setCookie(cookie)
				.setOutPort(outPort)
				.setPriority(FLOWMOD_DEFAULT_PRIORITY);
				
				FlowModUtils.setActions(fmb, actions, sw);
				if (log.isTraceEnabled()) {
					log.trace("Pushing Route flowmod routeIndx={} " +
							"sw={} inPort={} outPort={}",
							new Object[] {indx,
							sw,
							fmb.getMatch().get(MatchField.IN_PORT),
							outPort });
				}
				
				if (OFDPAUtils.isOFDPASwitch(sw)) {
					OFDPAUtils.addLearningSwitchFlow(sw, cookie, 
							FLOWMOD_DEFAULT_PRIORITY, 
							FLOWMOD_DEFAULT_HARD_TIMEOUT,
							FLOWMOD_DEFAULT_IDLE_TIMEOUT,
							fmb.getMatch(), 
							null, // TODO how to determine output VLAN for lookup of L2 interface group
							outPort);
				} else {
					//long tx1 = System.nanoTime();
					messageDamper.write(sw, fmb.build());
					//long tx2 = System.nanoTime();
					//System.out.println("messageDamper  "+ ( tx2 - tx1 )/1000);
				}
				
			
				if (sw.getId().equals(pinSwitch) &&
						!fmb.getCommand().equals(OFFlowModCommand.DELETE) &&
						!fmb.getCommand().equals(OFFlowModCommand.DELETE_STRICT)) {
					//long ty1 = System.nanoTime();
					pushPacket(sw, pi, outPort, true, cntx, ttl, label, switchnum-1);
					packetOutSent = true;
					//long ty2 = System.nanoTime();
					//System.out.println("pushfirstPacket  "+ ( ty2 - ty1 )/1000);
				}
			}
			else if (indx==switchPortList.size() - 1)//last switch, modify ethtype
			{
				if (flag[0] == -1){
					List<OFAction> actions = new ArrayList<OFAction>();
		 			//Match.Builder mb = MatchUtils.convertToVersion(match, sw.getOFFactory().getVersion());
					// set input and output ports on the switch
					OFPort outPort = switchPortList.get(indx).getPortId();
					//OFPort inPort = switchPortList.get(indx - 1).getPortId();
					//mb.setExact(MatchField.IN_PORT, inPort);
					if (FLOWMOD_DEFAULT_SET_SEND_FLOW_REM_FLAG || requestFlowRemovedNotification) {
						Set<OFFlowModFlags> flags = new HashSet<>();
						flags.add(OFFlowModFlags.SEND_FLOW_REM);
						fmb.setFlags(flags);
					}
					Match m = createMatchFromMpls(sw, label[0]);
					Match.Builder mb = MatchUtils.convertToVersion(m, sw.getOFFactory().getVersion());
					
					OFActionPopMpls.Builder apmb = sw.getOFFactory().actions().buildPopMpls();
					apmb.setEthertype(eth.getEtherType());
					actions.add(apmb.build());
					
					OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
					aob.setPort(outPort);
					aob.setMaxLen(Integer.MAX_VALUE);
					actions.add(aob.build());
					
					fmb.setMatch(mb.build())
					.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
					.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
					.setBufferId(OFBufferId.NO_BUFFER)
					.setCookie(cookie)
					.setOutPort(outPort)
					.setPriority(FLOWMOD_DEFAULT_PRIORITY);
					
					FlowModUtils.setActions(fmb, actions, sw);

					if (log.isTraceEnabled()) {
						log.trace("Pushing Route flowmod routeIndx={} " +
								"sw={} inPort={} outPort={}",
								new Object[] {indx,
								sw,
								fmb.getMatch().get(MatchField.IN_PORT),
								outPort });
					}
					
					if (OFDPAUtils.isOFDPASwitch(sw)) {
						OFDPAUtils.addLearningSwitchFlow(sw, cookie, 
								FLOWMOD_DEFAULT_PRIORITY, 
								FLOWMOD_DEFAULT_HARD_TIMEOUT,
								FLOWMOD_DEFAULT_IDLE_TIMEOUT,
								fmb.getMatch(), 
								null, // TODO how to determine output VLAN for lookup of L2 interface group
								outPort);
					} else {
						//long tx1 = System.nanoTime();
						messageDamper.write(sw, fmb.build());
						//long tx2 = System.nanoTime();
						//System.out.println("messageDamper  "+ ( tx2 - tx1 )/1000);
					}
				}
			}
			else//core switch, pop mpls and do action according to the mpls label
			{
				if ( flag[switchnum-1-(indx-1)/2] == -1){
					List<OFAction> actions = new ArrayList<OFAction>();
		 			//Match.Builder mb = MatchUtils.convertToVersion(match, sw.getOFFactory().getVersion());
					// set input and output ports on the switch
					OFPort outPort = switchPortList.get(indx).getPortId();
					//OFPort inPort = switchPortList.get(indx - 1).getPortId();
					//mb.setExact(MatchField.IN_PORT, inPort);
					if (FLOWMOD_DEFAULT_SET_SEND_FLOW_REM_FLAG || requestFlowRemovedNotification) {
						Set<OFFlowModFlags> flags = new HashSet<>();
						flags.add(OFFlowModFlags.SEND_FLOW_REM);
						fmb.setFlags(flags);
					}
					Match m = createMatchFromMpls(sw, label[switchnum-1-(indx-1)/2]);
					Match.Builder mb = MatchUtils.convertToVersion(m, sw.getOFFactory().getVersion());
					
					OFActionPopMpls.Builder apmb = sw.getOFFactory().actions().buildPopMpls();
					apmb.setEthertype(EthType.MPLS_UNICAST);
					actions.add(apmb.build());
					
					OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
					aob.setPort(outPort);
					aob.setMaxLen(Integer.MAX_VALUE);
					actions.add(aob.build());
					
					fmb.setMatch(mb.build())
					.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
					.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
					.setBufferId(OFBufferId.NO_BUFFER)
					.setCookie(cookie)
					.setOutPort(outPort)
					.setPriority(FLOWMOD_DEFAULT_PRIORITY);
					
					FlowModUtils.setActions(fmb, actions, sw);	
					
					if (log.isTraceEnabled()) {
						log.trace("Pushing Route flowmod routeIndx={} " +
								"sw={} inPort={} outPort={}",
								new Object[] {indx,
								sw,
								fmb.getMatch().get(MatchField.IN_PORT),
								outPort });
					}
					
					if (OFDPAUtils.isOFDPASwitch(sw)) {
						OFDPAUtils.addLearningSwitchFlow(sw, cookie, 
								FLOWMOD_DEFAULT_PRIORITY, 
								FLOWMOD_DEFAULT_HARD_TIMEOUT,
								FLOWMOD_DEFAULT_IDLE_TIMEOUT,
								fmb.getMatch(), 
								null, // TODO how to determine output VLAN for lookup of L2 interface group
								outPort);
					} else {
						//long tx1 = System.nanoTime();
						messageDamper.write(sw, fmb.build());
						//long tx2 = System.nanoTime();
						//System.out.println("messageDamper  "+ ( tx2 - tx1 )/1000);
					}
				}
			}
			//System.out.println(Integer.MAX_VALUE+" "+Integer.MIN_VALUE);
			//System.out.println(actions.hashCode());
		}
		//long t2 = System.nanoTime();
		//time_pushRoute = time_pushRoute + t2 - t1;
		//System.out.println("time_pushRoute  "+time_pushRoute/1000 + "  "+ ( t2 - t1 )/1000);
		return packetOutSent;
	}

   
	private void pushPacket(IOFSwitch sw, OFPacketIn pi, OFPort outPort, boolean useBufferedPacket, FloodlightContext cntx, int[] ttl,
			int[] label, int num) {
		if (pi == null) {
			return;
		}

		// The assumption here is (sw) is the switch that generated the
		// packet-in. If the input port is the same as output port, then
		// the packet-out should be ignored.
		if ((pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT)).equals(outPort)) {
			if (log.isDebugEnabled()) {
				log.debug("Attempting to do packet-out to the same " +
						"interface as packet-in. Dropping packet. " +
						" SrcSwitch={}, pi={}",
						new Object[]{sw, pi});
				return;
			}
		}

		if (log.isTraceEnabled()) {
			log.trace("PacketOut srcSwitch={} pi={}",
					new Object[] {sw, pi});
		}
		
		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
		List<OFAction> actions = new ArrayList<OFAction>();
		
		for (int k = 0;k < num;k ++)
		{
			OFActionPushMpls.Builder apmb = sw.getOFFactory().actions().buildPushMpls();
			apmb.setEthertype(EthType.MPLS_UNICAST);
			OFOxms oxms = sw.getOFFactory().oxms();
			OFActionSetMplsTtl.Builder setmplsttl = sw.getOFFactory().actions().buildSetMplsTtl();
			OFActionSetField.Builder setfield = sw.getOFFactory().actions().buildSetField();
			setmplsttl.setMplsTtl((short)ttl[k]);
			setfield.setField(oxms.buildMplsLabel().setValue(U32.of((long)label[k])).build());
			actions.add(apmb.build());
			actions.add(setmplsttl.build());
			actions.add(setfield.build());
		}
		
		actions.add(sw.getOFFactory().actions().output(outPort, Integer.MAX_VALUE));
		pob.setActions(actions);

		if (useBufferedPacket) {
			pob.setBufferId(pi.getBufferId()); 
		} else {
			pob.setBufferId(OFBufferId.NO_BUFFER);
		}
		
		if (pob.getBufferId().equals(OFBufferId.NO_BUFFER)) {
			byte[] packetData = pi.getData();
			pob.setData(packetData);
		}
		
		pob.setInPort((pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT)));
		messageDamper.write(sw, pob.build());
	}

	private Match createMatchFromMpls(IOFSwitch sw, int value) {
		// TODO Auto-generated method stub
		Match.Builder mb=sw.getOFFactory().buildMatch();
		//mb.setExact(MatchField.IN_PORT, inPort);
		mb.setExact(MatchField.ETH_TYPE, EthType.MPLS_UNICAST);
		mb.setExact(MatchField.MPLS_LABEL, U32.of(value));
		return mb.build();
	}

	protected void doForwardFlow(IOFSwitch sw, OFPacketIn pi, IRoutingDecision decision, FloodlightContext cntx, boolean requestFlowRemovedNotifn) {
        OFPort srcPort = OFMessageUtils.getInPort(pi);
        DatapathId srcSw = sw.getId();
        IDevice dstDevice = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_DST_DEVICE);
        IDevice srcDevice = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_SRC_DEVICE);

        if (dstDevice == null) {
            log.debug("Destination device unknown. Flooding packet");
            doFlood(sw, pi, decision, cntx);
            return;
        }

        if (srcDevice == null) {
            log.error("No device entry found for source device. Is the device manager running? If so, report bug.");
            return;
        }

        /* Some physical switches partially support or do not support ARP flows */
        if (FLOOD_ALL_ARP_PACKETS && 
                IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD).getEtherType() 
                == EthType.ARP) {
            log.debug("ARP flows disabled in Forwarding. Flooding ARP packet");
            doFlood(sw, pi, decision, cntx);
            return;
        }

        /* This packet-in is from a switch in the path before its flow was installed along the path */
        if (!topologyService.isEdge(srcSw, srcPort)) {  
            log.debug("Packet destination is known, but packet was not received on an edge port (rx on {}/{}). Flooding packet", srcSw, srcPort);
            doFlood(sw, pi, decision, cntx);
            return; 
        }   

        /* 
         * Search for the true attachment point. The true AP is
         * not an endpoint of a link. It is a switch port w/o an
         * associated link. Note this does not necessarily hold
         * true for devices that 'live' between OpenFlow islands.
         * 
         * TODO Account for the case where a device is actually
         * attached between islands (possibly on a non-OF switch
         * in between two OpenFlow switches).
         */
        SwitchPort dstAp = null;
        for (SwitchPort ap : dstDevice.getAttachmentPoints()) {
            if (topologyService.isEdge(ap.getNodeId(), ap.getPortId())) {
                dstAp = ap;
                break;
            }
        }	

        /* 
         * This should only happen (perhaps) when the controller is
         * actively learning a new topology and hasn't discovered
         * all links yet, or a switch was in standalone mode and the
         * packet in question was captured in flight on the dst point
         * of a link.
         */
        if (dstAp == null) {
            log.debug("Could not locate edge attachment point for destination device {}. Flooding packet");
            doFlood(sw, pi, decision, cntx);
            return; 
        }

        /* Validate that the source and destination are not on the same switch port */
        if (sw.getId().equals(dstAp.getNodeId()) && srcPort.equals(dstAp.getPortId())) {
            log.info("Both source and destination are on the same switch/port {}/{}. Dropping packet", sw.toString(), srcPort);
            return;
        }			

        U64 flowSetId = flowSetIdRegistry.generateFlowSetId();
        U64 cookie = makeForwardingCookie(decision, flowSetId);
        Path path = routingEngineService.getPath(srcSw, 
                srcPort,
                dstAp.getNodeId(),
                dstAp.getPortId());

        //Match m = createMatchFromPacket(sw, srcPort, cntx);

        if (! path.getPath().isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("pushRoute inPort={} route={} " +
                        "destination={}:{}",
                        new Object[] { srcPort, path,
                                dstAp.getNodeId(),
                                dstAp.getPortId()});
                //log.debug("Creating flow rules on the route, match rule: {}", m);
            }

            pushRoute(path, pi, sw.getId(), cookie, 
                    cntx, requestFlowRemovedNotifn,
                    OFFlowModCommand.ADD);	
            
            /* 
             * Register this flowset with ingress and egress ports for link down
             * flow removal. This is done after we push the path as it is blocking.
             */
            for (NodePortTuple npt : path.getPath()) {
                flowSetIdRegistry.registerFlowSetId(npt, flowSetId);
            }
        } /* else no path was found */
    }

    /**
     * Instead of using the Firewall's routing decision Match, which might be as general
     * as "in_port" and inadvertently Match packets erroneously, construct a more
     * specific Match based on the deserialized OFPacketIn's payload, which has been 
     * placed in the FloodlightContext already by the Controller.
     * 
     * @param sw, the switch on which the packet was received
     * @param inPort, the ingress switch port on which the packet was received
     * @param cntx, the current context which contains the deserialized packet
     * @return a composed Match object based on the provided information
     */
    protected Match createMatchFromPacket(IOFSwitch sw, OFPort inPort, FloodlightContext cntx) {
        // The packet in match will only contain the port number.
        // We need to add in specifics for the hosts we're routing between.
        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

        VlanVid vlan = null;      
        if (vlan == null) {
            vlan = VlanVid.ofVlan(eth.getVlanID()); /* VLAN might still be in packet */
        }
        
        MacAddress srcMac = eth.getSourceMACAddress();
        MacAddress dstMac = eth.getDestinationMACAddress();

        Match.Builder mb = sw.getOFFactory().buildMatch();
        if (FLOWMOD_DEFAULT_MATCH_IN_PORT) {
            mb.setExact(MatchField.IN_PORT, inPort);
        }

        if (FLOWMOD_DEFAULT_MATCH_MAC) {
            if (FLOWMOD_DEFAULT_MATCH_MAC_SRC) {
                mb.setExact(MatchField.ETH_SRC, srcMac);
            }
            if (FLOWMOD_DEFAULT_MATCH_MAC_DST) {
                mb.setExact(MatchField.ETH_DST, dstMac);
            }
        }

        if (FLOWMOD_DEFAULT_MATCH_VLAN) {
            if (!vlan.equals(VlanVid.ZERO)) {
                mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlanVid(vlan));
            }
        }

        // TODO Detect switch type and match to create hardware-implemented flow
        if (eth.getEtherType() == EthType.IPv4) { /* shallow check for equality is okay for EthType */
            IPv4 ip = (IPv4) eth.getPayload();
            IPv4Address srcIp = ip.getSourceAddress();
            IPv4Address dstIp = ip.getDestinationAddress();

            if (FLOWMOD_DEFAULT_MATCH_IP) {
                mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
                if (FLOWMOD_DEFAULT_MATCH_IP_SRC) {
                    mb.setExact(MatchField.IPV4_SRC, srcIp);
                }
                if (FLOWMOD_DEFAULT_MATCH_IP_DST) {
                    mb.setExact(MatchField.IPV4_DST, dstIp);
                }
            }

            if (FLOWMOD_DEFAULT_MATCH_TRANSPORT) {
                /*
                 * Take care of the ethertype if not included earlier,
                 * since it's a prerequisite for transport ports.
                 */
                if (!FLOWMOD_DEFAULT_MATCH_IP) {
                    mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
                }

                if (ip.getProtocol().equals(IpProtocol.TCP)) {
                    TCP tcp = (TCP) ip.getPayload();
                    mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
                    if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_SRC) {
                        mb.setExact(MatchField.TCP_SRC, tcp.getSourcePort());
                    }
                    if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_DST) {
                        mb.setExact(MatchField.TCP_DST, tcp.getDestinationPort());
                    }
                } else if (ip.getProtocol().equals(IpProtocol.UDP)) {
                    UDP udp = (UDP) ip.getPayload();
                    mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP);
                    if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_SRC) {
                        mb.setExact(MatchField.UDP_SRC, udp.getSourcePort());
                    }
                    if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_DST) {
                        mb.setExact(MatchField.UDP_DST, udp.getDestinationPort());
                    }
                }
            }
        } else if (eth.getEtherType() == EthType.ARP) { /* shallow check for equality is okay for EthType */
            mb.setExact(MatchField.ETH_TYPE, EthType.ARP);
        } else if (eth.getEtherType() == EthType.IPv6) {
            IPv6 ip = (IPv6) eth.getPayload();
            IPv6Address srcIp = ip.getSourceAddress();
            IPv6Address dstIp = ip.getDestinationAddress();

            if (FLOWMOD_DEFAULT_MATCH_IP) {
                mb.setExact(MatchField.ETH_TYPE, EthType.IPv6);
                if (FLOWMOD_DEFAULT_MATCH_IP_SRC) {
                    mb.setExact(MatchField.IPV6_SRC, srcIp);
                }
                if (FLOWMOD_DEFAULT_MATCH_IP_DST) {
                    mb.setExact(MatchField.IPV6_DST, dstIp);
                }
            }

            if (FLOWMOD_DEFAULT_MATCH_TRANSPORT) {
                /*
                 * Take care of the ethertype if not included earlier,
                 * since it's a prerequisite for transport ports.
                 */
                if (!FLOWMOD_DEFAULT_MATCH_IP) {
                    mb.setExact(MatchField.ETH_TYPE, EthType.IPv6);
                }

                if (ip.getNextHeader().equals(IpProtocol.TCP)) {
                    TCP tcp = (TCP) ip.getPayload();
                    mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
                    if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_SRC) {
                        mb.setExact(MatchField.TCP_SRC, tcp.getSourcePort());
                    }
                    if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_DST) {
                        mb.setExact(MatchField.TCP_DST, tcp.getDestinationPort());
                    }
                } else if (ip.getNextHeader().equals(IpProtocol.UDP)) {
                    UDP udp = (UDP) ip.getPayload();
                    mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP);
                    if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_SRC) {
                        mb.setExact(MatchField.UDP_SRC, udp.getSourcePort());
                    }
                    if (FLOWMOD_DEFAULT_MATCH_TRANSPORT_DST) {
                        mb.setExact(MatchField.UDP_DST, udp.getDestinationPort());
                    }
                }
            }
        }
        return mb.build();
    }

    /**
     * Creates a OFPacketOut with the OFPacketIn data that is flooded on all ports unless
     * the port is blocked, in which case the packet will be dropped.
     * @param sw The switch that receives the OFPacketIn
     * @param pi The OFPacketIn that came to the switch
     * @param decision The decision that caused flooding, or null
     * @param cntx The FloodlightContext associated with this OFPacketIn
     */
    protected void doFlood(IOFSwitch sw, OFPacketIn pi, IRoutingDecision decision, FloodlightContext cntx) {
        OFPort inPort = OFMessageUtils.getInPort(pi);
        OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
        List<OFAction> actions = new ArrayList<OFAction>();
        Set<OFPort> broadcastPorts = this.topologyService.getSwitchBroadcastPorts(sw.getId());

        if (broadcastPorts.isEmpty()) {
            log.debug("No broadcast ports found. Using FLOOD output action");
            broadcastPorts = Collections.singleton(OFPort.FLOOD);
        }

        for (OFPort p : broadcastPorts) {
            if (p.equals(inPort)) continue;
            actions.add(sw.getOFFactory().actions().output(p, Integer.MAX_VALUE));
        }
        pob.setActions(actions);
        // log.info("actions {}",actions);
        // set buffer-id, in-port and packet-data based on packet-in
        pob.setBufferId(OFBufferId.NO_BUFFER);
        OFMessageUtils.setInPort(pob, inPort);
        pob.setData(pi.getData());

        if (log.isTraceEnabled()) {
            log.trace("Writing flood PacketOut switch={} packet-in={} packet-out={}",
                    new Object[] {sw, pi, pob.build()});
        }
        messageDamper.write(sw, pob.build());

        return;
    }

    // IFloodlightModule methods

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        // We don't export any services
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService>
    getServiceImpls() {
        // We don't have any services
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        l.add(IDeviceService.class);
        l.add(IRoutingService.class);
        l.add(ITopologyService.class);
        l.add(IDebugCounterService.class);
        l.add(ILinkDiscoveryService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        super.init();
        flowtable = new FlowTable();
        pic = new PacketinCount();
        this.floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
        this.deviceManagerService = context.getServiceImpl(IDeviceService.class);
        this.routingEngineService = context.getServiceImpl(IRoutingService.class);
        this.topologyService = context.getServiceImpl(ITopologyService.class);
        this.debugCounterService = context.getServiceImpl(IDebugCounterService.class);
        this.switchService = context.getServiceImpl(IOFSwitchService.class);
        this.linkService = context.getServiceImpl(ILinkDiscoveryService.class);

        flowSetIdRegistry = FlowSetIdRegistry.getInstance();

        this.dhcpPacketProcessor = new DHCPPacketProcessor();
        this.dhcpPacketProcessor.registerForwardingModule(this.switchService,this.routingEngineService,this.topologyService,this.messageDamper);
        this.packetInMonitor = new PacketInMonitor();
        this.packetInMonitor.registerPacketInMonitor(this.switchService);

        Map<String, String> configParameters = context.getConfigParams(this);
        String tmp = configParameters.get("hard-timeout");
        if (tmp != null) {
            FLOWMOD_DEFAULT_HARD_TIMEOUT = ParseUtils.parseHexOrDecInt(tmp);
            log.info("Default hard timeout set to {}.", FLOWMOD_DEFAULT_HARD_TIMEOUT);
        } else {
            log.info("Default hard timeout not configured. Using {}.", FLOWMOD_DEFAULT_HARD_TIMEOUT);
        }
        tmp = configParameters.get("idle-timeout");
        if (tmp != null) {
            FLOWMOD_DEFAULT_IDLE_TIMEOUT = ParseUtils.parseHexOrDecInt(tmp);
            log.info("Default idle timeout set to {}.", FLOWMOD_DEFAULT_IDLE_TIMEOUT);
        } else {
            log.info("Default idle timeout not configured. Using {}.", FLOWMOD_DEFAULT_IDLE_TIMEOUT);
        }
        tmp = configParameters.get("table-id");
        if (tmp != null) {
            FLOWMOD_DEFAULT_TABLE_ID = TableId.of(ParseUtils.parseHexOrDecInt(tmp));
            log.info("Default table ID set to {}.", FLOWMOD_DEFAULT_TABLE_ID);
        } else {
            log.info("Default table ID not configured. Using {}.", FLOWMOD_DEFAULT_TABLE_ID);
        }
        tmp = configParameters.get("priority");
        if (tmp != null) {
            FLOWMOD_DEFAULT_PRIORITY = ParseUtils.parseHexOrDecInt(tmp);
            log.info("Default priority set to {}.", FLOWMOD_DEFAULT_PRIORITY);
        } else {
            log.info("Default priority not configured. Using {}.", FLOWMOD_DEFAULT_PRIORITY);
        }
        tmp = configParameters.get("set-send-flow-rem-flag");
        if (tmp != null) {
            FLOWMOD_DEFAULT_SET_SEND_FLOW_REM_FLAG = Boolean.parseBoolean(tmp);
            log.info("Default flags will be set to SEND_FLOW_REM {}.", FLOWMOD_DEFAULT_SET_SEND_FLOW_REM_FLAG);
        } else {
            log.info("Default flags will be empty.");
        }
        tmp = configParameters.get("match");
        if (tmp != null) {
            tmp = tmp.toLowerCase();
            if (!tmp.contains("in-port") && !tmp.contains("vlan") 
                    && !tmp.contains("mac") && !tmp.contains("ip") 
                    && !tmp.contains("transport")) {
                /* leave the default configuration -- blank or invalid 'match' value */
            } else {
                FLOWMOD_DEFAULT_MATCH_IN_PORT = tmp.contains("in-port") ? true : false;
                FLOWMOD_DEFAULT_MATCH_VLAN = tmp.contains("vlan") ? true : false;
                FLOWMOD_DEFAULT_MATCH_MAC = tmp.contains("mac") ? true : false;
                FLOWMOD_DEFAULT_MATCH_IP = tmp.contains("ip") ? true : false;
                FLOWMOD_DEFAULT_MATCH_TRANSPORT = tmp.contains("transport") ? true : false;
            }
        }
        log.info("Default flow matches set to: IN_PORT=" + FLOWMOD_DEFAULT_MATCH_IN_PORT
                + ", VLAN=" + FLOWMOD_DEFAULT_MATCH_VLAN
                + ", MAC=" + FLOWMOD_DEFAULT_MATCH_MAC
                + ", IP=" + FLOWMOD_DEFAULT_MATCH_IP
                + ", TPPT=" + FLOWMOD_DEFAULT_MATCH_TRANSPORT);

        tmp = configParameters.get("detailed-match");
        if (tmp != null) {
            tmp = tmp.toLowerCase();
            if (!tmp.contains("src-mac") && !tmp.contains("dst-mac") 
                    && !tmp.contains("src-ip") && !tmp.contains("dst-ip")
                    && !tmp.contains("src-transport") && !tmp.contains("dst-transport")) {
                /* leave the default configuration -- both src and dst for layers defined above */
            } else {
                FLOWMOD_DEFAULT_MATCH_MAC_SRC = tmp.contains("src-mac") ? true : false;
                FLOWMOD_DEFAULT_MATCH_MAC_DST = tmp.contains("dst-mac") ? true : false;
                FLOWMOD_DEFAULT_MATCH_IP_SRC = tmp.contains("src-ip") ? true : false;
                FLOWMOD_DEFAULT_MATCH_IP_DST = tmp.contains("dst-ip") ? true : false;
                FLOWMOD_DEFAULT_MATCH_TRANSPORT_SRC = tmp.contains("src-transport") ? true : false;
                FLOWMOD_DEFAULT_MATCH_TRANSPORT_DST = tmp.contains("dst-transport") ? true : false;
            }
        }
        log.info("Default detailed flow matches set to: SRC_MAC=" + FLOWMOD_DEFAULT_MATCH_MAC_SRC
                + ", DST_MAC=" + FLOWMOD_DEFAULT_MATCH_MAC_DST
                + ", SRC_IP=" + FLOWMOD_DEFAULT_MATCH_IP_SRC
                + ", DST_IP=" + FLOWMOD_DEFAULT_MATCH_IP_DST
                + ", SRC_TPPT=" + FLOWMOD_DEFAULT_MATCH_TRANSPORT_SRC
                + ", DST_TPPT=" + FLOWMOD_DEFAULT_MATCH_TRANSPORT_DST);

        tmp = configParameters.get("flood-arp");
        if (tmp != null) {
            tmp = tmp.toLowerCase();
            if (!tmp.contains("yes") && !tmp.contains("yep") && !tmp.contains("true") && !tmp.contains("ja") && !tmp.contains("stimmt")) {
                FLOOD_ALL_ARP_PACKETS = false;
                log.info("Not flooding ARP packets. ARP flows will be inserted for known destinations");
            } else {
                FLOOD_ALL_ARP_PACKETS = true;
                log.info("Flooding all ARP packets. No ARP flows will be inserted");
            }
        }

        tmp = configParameters.get("remove-flows-on-link-or-port-down");
        if (tmp != null) {
            REMOVE_FLOWS_ON_LINK_OR_PORT_DOWN = Boolean.parseBoolean(tmp);
        }
        if (REMOVE_FLOWS_ON_LINK_OR_PORT_DOWN) {
            log.info("Flows will be removed on link/port down events");
        } else {
            log.info("Flows will not be removed on link/port down events");
        }
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        super.startUp();
        switchService.addOFSwitchListener(this);
        routingEngineService.addRoutingDecisionChangedListener(this);
        floodlightProviderService.addOFMessageListener(OFType.FLOW_REMOVED, this);
        /* Register only if we want to remove stale flows */
        if (REMOVE_FLOWS_ON_LINK_OR_PORT_DOWN) {
            linkService.addListener(this);
        }



    }

    @Override
    public void switchAdded(DatapathId switchId) {
    }

    @Override
    public void switchRemoved(DatapathId switchId) {		
    }

    @Override
    public void switchActivated(DatapathId switchId) {
        IOFSwitch sw = switchService.getSwitch(switchId);
        if (sw == null) {
            log.warn("Switch {} was activated but had no switch object in the switch service. Perhaps it quickly disconnected", switchId);
            return;
        }
        if (OFDPAUtils.isOFDPASwitch(sw)) {
            messageDamper.write(sw, sw.getOFFactory().buildFlowDelete()
                    .setTableId(TableId.ALL)
                    .build()
                    );
            messageDamper.write(sw, sw.getOFFactory().buildGroupDelete()
                    .setGroup(OFGroup.ANY)
                    .setGroupType(OFGroupType.ALL)
                    .build()
                    );
            messageDamper.write(sw, sw.getOFFactory().buildGroupDelete()
                    .setGroup(OFGroup.ANY)
                    .setGroupType(OFGroupType.INDIRECT)
                    .build()
                    );
            messageDamper.write(sw, sw.getOFFactory().buildBarrierRequest().build());

            List<OFPortModeTuple> portModes = new ArrayList<OFPortModeTuple>();
            for (OFPortDesc p : sw.getPorts()) {
                portModes.add(OFPortModeTuple.of(p.getPortNo(), OFPortMode.ACCESS));
            }
            if (log.isWarnEnabled()) {
                log.warn("For OF-DPA switch {}, initializing VLAN {} on ports {}", new Object[] { switchId, VlanVid.ZERO, portModes});
            }
            OFDPAUtils.addLearningSwitchPrereqs(sw, VlanVid.ZERO, portModes);
        }
    }

    @Override
    public void switchPortChanged(DatapathId switchId, OFPortDesc port, PortChangeType type) {	
        /* Port down events handled via linkDiscoveryUpdate(), which passes thru all events */
    }

    @Override
    public void switchChanged(DatapathId switchId) {
    }

    @Override
    public void switchDeactivated(DatapathId switchId) {
    }

    @Override
    public void linkDiscoveryUpdate(List<LDUpdate> updateList) {
        for (LDUpdate u : updateList) {
            /* Remove flows on either side if link/port went down */
            if (u.getOperation() == UpdateOperation.LINK_REMOVED ||
                    u.getOperation() == UpdateOperation.PORT_DOWN ||
                    u.getOperation() == UpdateOperation.TUNNEL_PORT_REMOVED) {
                Set<OFMessage> msgs = new HashSet<OFMessage>();

                if (u.getSrc() != null && !u.getSrc().equals(DatapathId.NONE)) {
                    IOFSwitch srcSw = switchService.getSwitch(u.getSrc());
                    /* src side of link */
                    if (srcSw != null) {
                        Set<U64> ids = flowSetIdRegistry.getFlowSetIds(
                                new NodePortTuple(u.getSrc(), u.getSrcPort()));
                        if (ids != null) {
                            Iterator<U64> i = ids.iterator();
                            while (i.hasNext()) {
                                U64 id = i.next();
                                U64 cookie = id.or(DEFAULT_FORWARDING_COOKIE);
                                U64 cookieMask = U64.of(FLOWSET_MASK).or(AppCookie.getAppFieldMask());
                                /* flows matching on src port */
                                msgs.add(srcSw.getOFFactory().buildFlowDelete()
                                        .setCookie(cookie)
                                        .setCookieMask(cookieMask)
                                        .setMatch(srcSw.getOFFactory().buildMatch()
                                                .setExact(MatchField.IN_PORT, u.getSrcPort())
                                                .build())
                                        .build());
                                /* flows outputting to src port */
                                msgs.add(srcSw.getOFFactory().buildFlowDelete()
                                        .setCookie(cookie)
                                        .setCookieMask(cookieMask)
                                        .setOutPort(u.getSrcPort())
                                        .build());
                                messageDamper.write(srcSw, msgs);
                                log.debug("src: Removing flows to/from DPID={}, port={}", u.getSrc(), u.getSrcPort());
                                log.debug("src: Cookie/mask {}/{}", cookie, cookieMask);
                                
                                /* 
                                 * Now, for each ID on this particular failed link, remove
                                 * all other flows in the network using this ID.
                                 */
                                Set<NodePortTuple> npts = flowSetIdRegistry.getNodePortTuples(id);
                                if (npts != null) {
                                    for (NodePortTuple npt : npts) {
                                        msgs.clear();
                                        IOFSwitch sw = switchService.getSwitch(npt.getNodeId());
                                        if (sw != null) {
                                            msgs.add(sw.getOFFactory().buildFlowDelete()
                                                    .setCookie(cookie)
                                                    .setCookieMask(cookieMask)
                                                    .setMatch(sw.getOFFactory().buildMatch()
                                                            .setExact(MatchField.IN_PORT, npt.getPortId())
                                                            .build())
                                                    .build());
                                            /* flows outputting to port */
                                            msgs.add(sw.getOFFactory().buildFlowDelete()
                                                    .setCookie(cookie)
                                                    .setCookieMask(cookieMask)
                                                    .setOutPort(npt.getPortId())
                                                    .build());
                                            messageDamper.write(sw, msgs);
                                            log.debug("src: Removing same-cookie flows to/from DPID={}, port={}", npt.getNodeId(), npt.getPortId());
                                            log.debug("src: Cookie/mask {}/{}", cookie, cookieMask);
                                        }
                                    }
                                }
                                flowSetIdRegistry.removeExpiredFlowSetId(id, new NodePortTuple(u.getSrc(), u.getSrcPort()), i);
                            }
                        }
                    }
                    flowSetIdRegistry.removeNodePortTuple(new NodePortTuple(u.getSrc(), u.getSrcPort()));
                }

                /* must be a link, not just a port down, if we have a dst switch */
                if (u.getDst() != null && !u.getDst().equals(DatapathId.NONE)) {
                    /* dst side of link */
                    IOFSwitch dstSw = switchService.getSwitch(u.getDst());
                    if (dstSw != null) {
                        Set<U64> ids = flowSetIdRegistry.getFlowSetIds(
                                new NodePortTuple(u.getDst(), u.getDstPort()));
                        if (ids != null) {
                            Iterator<U64> i = ids.iterator();
                            while (i.hasNext()) {
                                U64 id = i.next();
                                U64 cookie = id.or(DEFAULT_FORWARDING_COOKIE);
                                U64 cookieMask = U64.of(FLOWSET_MASK).or(AppCookie.getAppFieldMask());
                                /* flows matching on dst port */
                                msgs.clear();
                                msgs.add(dstSw.getOFFactory().buildFlowDelete()
                                        .setCookie(cookie)
                                        .setCookieMask(cookieMask)
                                        .setMatch(dstSw.getOFFactory().buildMatch()
                                                .setExact(MatchField.IN_PORT, u.getDstPort())
                                                .build())
                                        .build());
                                /* flows outputting to dst port */
                                msgs.add(dstSw.getOFFactory().buildFlowDelete()
                                        .setCookie(cookie)
                                        .setCookieMask(cookieMask)
                                        .setOutPort(u.getDstPort())
                                        .build());
                                messageDamper.write(dstSw, msgs);
                                log.debug("dst: Removing flows to/from DPID={}, port={}", u.getDst(), u.getDstPort());
                                log.debug("dst: Cookie/mask {}/{}", cookie, cookieMask);

                                /* 
                                 * Now, for each ID on this particular failed link, remove
                                 * all other flows in the network using this ID.
                                 */
                                Set<NodePortTuple> npts = flowSetIdRegistry.getNodePortTuples(id);
                                if (npts != null) {
                                    for (NodePortTuple npt : npts) {
                                        msgs.clear();
                                        IOFSwitch sw = switchService.getSwitch(npt.getNodeId());
                                        if (sw != null) {
                                            msgs.add(sw.getOFFactory().buildFlowDelete()
                                                    .setCookie(cookie)
                                                    .setCookieMask(cookieMask)
                                                    .setMatch(sw.getOFFactory().buildMatch()
                                                            .setExact(MatchField.IN_PORT, npt.getPortId())
                                                            .build())
                                                    .build());
                                            /* flows outputting to port */
                                            msgs.add(sw.getOFFactory().buildFlowDelete()
                                                    .setCookie(cookie)
                                                    .setCookieMask(cookieMask)
                                                    .setOutPort(npt.getPortId())
                                                    .build());
                                            messageDamper.write(sw, msgs);
                                            log.debug("dst: Removing same-cookie flows to/from DPID={}, port={}", npt.getNodeId(), npt.getPortId());
                                            log.debug("dst: Cookie/mask {}/{}", cookie, cookieMask);
                                        }
                                    }
                                }
                                flowSetIdRegistry.removeExpiredFlowSetId(id, new NodePortTuple(u.getDst(), u.getDstPort()), i);
                            }
                        }
                    }
                    flowSetIdRegistry.removeNodePortTuple(new NodePortTuple(u.getDst(), u.getDstPort()));
                }
            }
        }
    }
}