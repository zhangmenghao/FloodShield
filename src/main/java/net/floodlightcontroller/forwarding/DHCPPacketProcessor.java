package net.floodlightcontroller.forwarding;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.debugcounter.IDebugCounterService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
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
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionGotoTable;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.floodlightcontroller.dhcpserver.DHCPServer.DHCP_MSG_TYPE_ACK;
import static net.floodlightcontroller.dhcpserver.DHCPServer.DHCP_MSG_TYPE_OFFER;

/**
 * Created by dalaoshe on 17-3-13.
 */
public class DHCPPacketProcessor {
    protected static final Logger log = LoggerFactory.getLogger(DHCPPacketProcessor.class);
    protected IOFSwitchService switchService;
    protected IRoutingService routingEngineService;
    protected ITopologyService topologyService;
    private DHCPBindingTable dhcpBindingTable;
    protected OFMessageDamper messageDamper;

    public DHCPPacketProcessor() {

    }
    public void registerForwardingModule(IOFSwitchService switchService, IRoutingService routingEngineService,
                                         ITopologyService topologyService, OFMessageDamper messageDamper) {
        this.dhcpBindingTable = new DHCPBindingTable();
        this.switchService = switchService;
        this.routingEngineService = routingEngineService;
        this.topologyService = topologyService;
        this.messageDamper = messageDamper;
    }

    public DHCP getDHCPPayload(Ethernet eth) {
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

    public boolean isDHCPPacket(Ethernet eth) {
        if (eth.getEtherType() == EthType.IPv4) { /* shallow compare is okay for EthType */
            IPv4 IPv4Payload = (IPv4) eth.getPayload();
            if (IPv4Payload.getProtocol() == IpProtocol.UDP) { /* shallow compare also okay for IpProtocol */
                UDP UDPPayload = (UDP) IPv4Payload.getPayload();
                if ((UDPPayload.getDestinationPort().equals(UDP.DHCP_SERVER_PORT) /* TransportPort must be deep though */
                        || UDPPayload.getDestinationPort().equals(UDP.DHCP_CLIENT_PORT))
                        && (UDPPayload.getSourcePort().equals(UDP.DHCP_SERVER_PORT)
                        || UDPPayload.getSourcePort().equals(UDP.DHCP_CLIENT_PORT))){
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isDHCPClientPacket(DHCP pi) {
        return !this.isDHCPServerPacket(pi);
    }

    public boolean isDHCPServerPacket(DHCP pi) {
        if(Arrays.equals(pi.getOption(DHCP.DHCPOptionCode.OptionCode_MessageType).getData(), DHCP_MSG_TYPE_ACK) ||
                Arrays.equals(pi.getOption(DHCP.DHCPOptionCode.OptionCode_MessageType).getData(), DHCP_MSG_TYPE_OFFER) ) {
            return true;
        }
        return false;
    }

    public boolean doDHCPPacketProcess(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        OFPacketIn pi = (OFPacketIn) msg;
        DHCP dhcpPayload = this.getDHCPPayload(eth);
        if(dhcpPayload != null) {
            if(!this.isDHCPServerPacket(dhcpPayload)) {
                log.info("DHCP REQUEST" + " sw:" + sw.getId() + " inport:" + OFMessageUtils.getInPort(pi));
                return false;
            }
            log.info("DHCP SERVER");
            OFPort srcPort = OFMessageUtils.getInPort(pi);
            DatapathId srcSw = sw.getId();
            IDevice dstDevice = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_DST_DEVICE);
            SwitchPort dstAp = null;
            if (dstDevice == null) {
                log.info("Destination device unknown. Flooding packet");
                return false;
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

            // add new ip into Forwarding.host-pi-map
            if (!Forwarding.hostPacketInMap.containsKey(dhcpPayload.getYourIPAddress()))
            	Forwarding.hostPacketInMap.put(dhcpPayload.getYourIPAddress(), new PacketInCollector(dhcpPayload.getYourIPAddress()));
            
            this.pushDHCPReplyToClient(sw_o,pi,outPort,cntx);
            log.info("push packet to " + sw_o.getId() + " oport: " + outPort.toString()
                             + " inport:" +srcPort.toString());

            this.writeIPMACBindFlowToSw(sw_o, dhcpPayload.getYourIPAddress(), dhcpPayload.getClientHardwareAddress(),outPort);

            this.dhcpBindingTable.addnewItem(dhcpPayload.getClientHardwareAddress(), sw_o, outPort,
                                        dhcpPayload.getYourIPAddress());

            return true;
        }
        return false;
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

    public void writeIPMACBindFlowToSw(IOFSwitch sw, IPv4Address ip, MacAddress mac, OFPort port) {
    	// new version
    	List<OFInstruction> instructions = new ArrayList<OFInstruction>();
    	OFInstructionGotoTable.Builder ib = sw.getOFFactory().instructions().buildGotoTable();
    	ib.setTableId(TableId.of(1));
    	instructions.add(ib.build());

    	Match.Builder mb = sw.getOFFactory().buildMatch();
    	mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
    	mb.setExact(MatchField.IPV4_SRC, ip);
    	mb.setExact(MatchField.ETH_SRC, mac);
    	mb.setExact(MatchField.IN_PORT, port);

    	Match.Builder mb2 = sw.getOFFactory().buildMatch();
    	mb2.setExact(MatchField.ETH_TYPE, EthType.ARP);
    	mb2.setExact(MatchField.ETH_SRC, mac);
    	mb2.setExact(MatchField.IN_PORT, port);
	  
		OFFlowAdd defaultFlow1 = sw.getOFFactory().buildFlowAdd()
	    .setMatch(mb.build())
	    .setTableId(TableId.of(0))
	    .setPriority(2)
	    .setInstructions(instructions)
	    .setHardTimeout(0)
	    .setIdleTimeout(0)
	    .build();
	  
	    OFFlowAdd defaultFlow2 = sw.getOFFactory().buildFlowAdd()
	    .setMatch(mb.build())
	    .setTableId(TableId.of(0))
	    .setPriority(2)
	    .setInstructions(instructions)
	    .setHardTimeout(0)
	    .setIdleTimeout(0)
	    .build();
	    
	    sw.write(defaultFlow1);
	    sw.write(defaultFlow2);
	    
	    List<OFAction> actions = new ArrayList<OFAction>();
    	Match.Builder mb3 = sw.getOFFactory().buildMatch();
    	mb3.setExact(MatchField.ETH_TYPE, EthType.IPv4);
    	mb3.setExact(MatchField.IN_PORT, port);

    	Match.Builder mb4 = sw.getOFFactory().buildMatch();
    	mb4.setExact(MatchField.ETH_TYPE, EthType.ARP);
    	mb4.setExact(MatchField.IN_PORT, port);
    	
		OFFlowAdd defaultFlow3 = sw.getOFFactory().buildFlowAdd()
		.setMatch(mb.build())
		.setTableId(TableId.of(0))
		.setPriority(1)
		.setActions(actions)
		.setHardTimeout(0)
		.setIdleTimeout(0)
		.build();
			  
		OFFlowAdd defaultFlow4 = sw.getOFFactory().buildFlowAdd()
		.setMatch(mb.build())
		.setTableId(TableId.of(0))
		.setPriority(1)
		.setActions(actions)
		.setHardTimeout(0)
		.setIdleTimeout(0)
		.build();
		
		sw.write(defaultFlow3);
		sw.write(defaultFlow4);
    }
}
