package net.floodlightcontroller.dhcpserver;


import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.forwarding.MplsForwarding;
import net.floodlightcontroller.packet.DHCP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Path;
import net.floodlightcontroller.routing.RoutingDecision;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.util.OFMessageDamper;
import net.floodlightcontroller.util.OFMessageUtils;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;

import static net.floodlightcontroller.dhcpserver.DHCPServer.*;
import static net.floodlightcontroller.packetstreamer.thrift.OFMessageType.PACKET_IN;

/**
 * Created by dalaoshe on 17-3-2.
 */
public class DHCPProxy implements IOFMessageListener, IFloodlightModule{
    protected static Logger log = LoggerFactory.getLogger(DHCPProxy.class);
    protected IFloodlightProviderService FloodlightProvider ;
    protected ITopologyService topologyService;
    protected IRoutingService routingEngineService;
    protected IOFSwitchService switchService;
    protected OFMessageDamper messageDamper;
    private AtomicLong PACKET_IN_COUNT = new AtomicLong() ;
    private static volatile boolean enabled = false;
    @Override
    public String getName() {
        return "DHCPProxy" ;
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {

        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {

        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>() ;
        l.add(IFloodlightProviderService.class);
        return l ;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
        FloodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        this.topologyService = context.getServiceImpl(ITopologyService.class);
        this.routingEngineService = context.getServiceImpl(IRoutingService.class);
        this.switchService = context.getServiceImpl(IOFSwitchService.class);
        messageDamper = new OFMessageDamper(OFMESSAGE_DAMPER_CAPACITY,
                EnumSet.of(OFType.FLOW_MOD),
                OFMESSAGE_DAMPER_TIMEOUT);
    }

    @Override
    public void startUp(FloodlightModuleContext context)
            throws FloodlightModuleException {
        FloodlightProvider.addOFMessageListener(OFType.PACKET_IN,this) ;
    }

    @Override
    public net.floodlightcontroller.core.IListener.Command receive(
            IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        long count = PACKET_IN_COUNT.incrementAndGet() ;

        if(msg.getType() == OFType.PACKET_IN) {
            IRoutingDecision decision = null;
            if (cntx != null) {
                decision = RoutingDecision.rtStore.get(cntx, IRoutingDecision.CONTEXT_DECISION);
            }
            Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
            // We found a routing decision (i.e. Firewall is enabled... it's the only thing that makes RoutingDecisions)
            OFPacketIn pi = (OFPacketIn) msg;

            if (eth.getEtherType() == EthType.IPv4) {
                IPv4 IPv4Payload = (IPv4) eth.getPayload();
                if (IPv4Payload.getProtocol() == IpProtocol.UDP) {
                    UDP UDPPayload = (UDP) IPv4Payload.getPayload();
                    if ((UDPPayload.getDestinationPort().equals(UDP.DHCP_SERVER_PORT) /* TransportPort must be deep though */
                            || UDPPayload.getDestinationPort().equals(UDP.DHCP_CLIENT_PORT))
                            && (UDPPayload.getSourcePort().equals(UDP.DHCP_SERVER_PORT)
                            || UDPPayload.getSourcePort().equals(UDP.DHCP_CLIENT_PORT))) {
                        log.info("Got DHCP Packet");
                        log.info("dhcp ack/offer swid " + sw.getId());
                        DHCP DHCPPayload = (DHCP) UDPPayload.getPayload();
                        if (Arrays.equals(DHCPPayload.getOption(DHCP.DHCPOptionCode.OptionCode_MessageType).getData(), DHCP_MSG_TYPE_ACK) ||
                                Arrays.equals(DHCPPayload.getOption(DHCP.DHCPOptionCode.OptionCode_MessageType).getData(), DHCP_MSG_TYPE_OFFER)  ) {

                            if(Arrays.equals(DHCPPayload.getOption(DHCP.DHCPOptionCode.OptionCode_MessageType).getData(), DHCP_MSG_TYPE_OFFER)) {
                                log.info("dhcp OFFER");
                            }

                            doPacketout(sw, pi, cntx);

                            return Command.STOP;
                        }
                    }
                }

            }

        }

        return Command.CONTINUE ;
    }

        protected void doPacketout(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx){

            OFPort srcPort = OFMessageUtils.getInPort(pi);
            DatapathId srcSw = sw.getId();
            IDevice dstDevice = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_DST_DEVICE);
            SwitchPort dstAp = null;

            if (dstDevice == null) {
                log.info("Destination device unknown. Flooding packet");

                doFlood(sw, pi, RoutingDecision.rtStore.get(cntx, IRoutingDecision.CONTEXT_DECISION), cntx);
                return;
            }

            for (SwitchPort ap : dstDevice.getAttachmentPoints()) {
                if (topologyService.isEdge(ap.getNodeId(), ap.getPortId())) {
                    dstAp = ap;
                    break;
                }
            }

            Path path = routingEngineService.getPath(srcSw,
                    srcPort,
                    dstAp.getNodeId(),
                    dstAp.getPortId());

            List<NodePortTuple> switchPortList = path.getPath();
            int indx = switchPortList.size() - 1;
            DatapathId switchDPID = switchPortList.get(indx).getNodeId();
            IOFSwitch sw_o = switchService.getSwitch(switchDPID);
            OFPort outPort = switchPortList.get(indx).getPortId();

            pushPacket(sw_o, pi, outPort, true, cntx);
            log.info("push packet to " + sw_o.getId());

            return ;
        }

    protected void pushPacket(IOFSwitch sw, OFPacketIn pi, OFPort outport, boolean useBufferedPacket, FloodlightContext cntx) {
        if (pi == null) {
            return;
        }

        // The assumption here is (sw) is the switch that generated the
        // packet-in. If the input port is the same as output port, then
        // the packet-out should be ignored.
        if ((pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT)).equals(outport)) {
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
        actions.add(sw.getOFFactory().actions().output(outport, Integer.MAX_VALUE));
        pob.setActions(actions);

        /* Use packet in buffer if there is a buffer ID set */
        if (useBufferedPacket) {
            pob.setBufferId(pi.getBufferId()); /* will be NO_BUFFER if there isn't one */
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
}
