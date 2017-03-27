package net.floodlightcontroller.config;

/**
 * Created by dalaoshe on 17-3-10.
 */
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

import net.floodlightcontroller.core.*;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.python.antlr.ast.Str;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DDosProtectionConfig implements IOFMessageListener, IFloodlightModule, DDosProtectionRESTService{
    public static ArrayList<StaticConfigItem> staticConfigs = new ArrayList<StaticConfigItem>();
    protected IOFSwitchService switchService;
    protected IFloodlightProviderService floodlightProviderService;
    protected IRestApiService restApiService;
    protected static Logger logger;
    private String configPath = "xmltest";
    private static boolean openStaticIPConfig = false;
    public static SupervisedConfig supervisedConfig;
    public static DynamicConfig dynamicConfig;
    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }

    public static void setOpenStaticIPConfig(String open) {
        if(open.equals("ON")) {
            openStaticIPConfig = true;
        }
        else openStaticIPConfig = false;

    }

    public static void parseSupervisedConfig(Element root) {
        Element supervisedElement = root.element("SupervisedConfig");
        String methodPrefix = "set";
        try {
            Class<?> c = SupervisedConfig.class;
            Iterator root_action = supervisedElement.attributeIterator();
            Attribute root_attribute = (Attribute) root_action.next();
            String openMethodName = methodPrefix + root_attribute.getName();
            Method openMethod = c.getMethod(openMethodName, String.class);
            openMethod.invoke(supervisedConfig, root_attribute.getStringValue());
            for (Iterator i_action = supervisedElement.elementIterator(); i_action.hasNext(); ) {
                Element e_action = (Element) i_action.next();
                Iterator a_action = e_action.attributeIterator();
                Attribute attribute = (Attribute) a_action.next();
                String methodName = methodPrefix + attribute.getValue();
                Method method = c.getMethod(methodName, String.class);
                method.invoke(supervisedConfig, e_action.getStringValue());
            }
        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void parseStaticIPConfig(Element root) {
        Element staticElement = root.element("StaticIPConfig");
        String methodPrefix = "set";
        try {
            Class<?> c = DDosProtectionConfig.class;
            Iterator root_action = staticElement.attributeIterator();
            Attribute root_attribute = (Attribute) root_action.next();
            String openMethodName = methodPrefix + root_attribute.getName();
            Method openMethod = c.getMethod(openMethodName, String.class);
            openMethod.invoke(c.newInstance(), root_attribute.getStringValue());
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        for(Iterator i_action = staticElement.elementIterator(); i_action.hasNext();){
            Element e_action = (Element)i_action.next();
            try {
                Class<?> c = StaticConfigItem.class;
                StaticConfigItem o = (StaticConfigItem) c.newInstance();
                for (Iterator a_action = e_action.attributeIterator(); a_action.hasNext(); ) {
                    Attribute attribute = (Attribute) a_action.next();
                    String methodName = methodPrefix + attribute.getName();
                    Method method = c.getMethod(methodName,String.class);
                    method.invoke(o, attribute.getValue());
                }
                staticConfigs.add(o);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void parseDynamicIPConfig(Element root) {
        Element dynamicElement = root.element("DynamicIPConfig");
        String methodPrefix = "set";
        try {
            Class<?> c = DynamicConfig.class;
            Iterator root_action = dynamicElement.attributeIterator();
            Attribute root_attribute = (Attribute) root_action.next();
            String openMethodName = methodPrefix + root_attribute.getName();
            Method openMethod = c.getMethod(openMethodName, String.class);
            openMethod.invoke(dynamicConfig, root_attribute.getStringValue());
            for (Iterator i_action = dynamicElement.elementIterator(); i_action.hasNext(); ) {
                Element e_action = (Element) i_action.next();
                Iterator a_action = e_action.attributeIterator();
                Attribute attribute = (Attribute) a_action.next();
                String methodName = methodPrefix + attribute.getValue();
                Method method = c.getMethod(methodName, String.class);
                method.invoke(dynamicConfig, e_action.getStringValue());
            }
        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public static void parseXmlData(String xmlFilePath){
        Document doc = parseXML2Document(xmlFilePath);
        Element root  = doc.getRootElement();
        parseSupervisedConfig(root);
        parseStaticIPConfig(root);
        parseDynamicIPConfig(root);
    }
    public static Document parseXML2Document(String xmlFilePath) {
        try {

            File f = new File(xmlFilePath);
            if(!f.exists()) {
                System.out.print("file not exist\n");
                System.exit(1);
            }
            SAXReader reader = new SAXReader();
            return reader.read(f);

        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    public static void print() {
        for(StaticConfigItem config : staticConfigs) {
            config.print();
        }
    }

    @Override
    public String getName() {
        return DDosProtectionConfig.class.getName();
    }
    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
       // this.writeStaticConfig();
        return Command.CONTINUE;
    }

    public void writeStaticConfig() {
        ArrayList<Integer> mark = new ArrayList<Integer>();
        for(DDosProtectionConfig.StaticConfigItem item: DDosProtectionConfig.staticConfigs) {
            if(this.writeStaticConfigToOFSwitch(item)) {
                mark.add(DDosProtectionConfig.staticConfigs.indexOf(item));
            }
        }
        int count = 0;
        for(Integer i: mark) {
            DDosProtectionConfig.staticConfigs.remove(i.intValue() - count);
            logger.info("remove{} size{}",i,DDosProtectionConfig.staticConfigs.size());
            count++;
        }
    }

    protected boolean writeStaticConfigToOFSwitch(StaticConfigItem item) {
        IOFSwitch sw = switchService.getSwitch(DatapathId.of(item.getSwID()));
        if(sw == null) {
            logger.info("SWID:{} not exist", item.getSwID());
            return false;
        }

        Match.Builder mb = sw.getOFFactory().buildMatch();
        List<OFAction> actions = new ArrayList<OFAction>();
        OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
        aob.setPort(OFPort.CONTROLLER);
        aob.setMaxLen(Integer.MAX_VALUE);
        actions.add(aob.build());

        mb.setExact(MatchField.ETH_SRC, MacAddress.of(item.getMAC()));
        OFFlowAdd defaultFlow = sw.getOFFactory().buildFlowAdd()
                .setMatch(mb.build())
                .setTableId(TableId.of(0))
                .setPriority(0)
                .setActions(actions)
                .build();
        return sw.write(defaultFlow);
    }

    protected void reloadStaticConfig(String xmlPath) {
        this.configPath = xmlPath;
        DDosProtectionConfig.staticConfigs.clear();
        DDosProtectionConfig.parseXmlData(xmlPath);
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(DDosProtectionRESTService.class);
        return l;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>, IFloodlightService> m =
                new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
        m.put(DDosProtectionRESTService.class, this);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IOFSwitchService.class);
        l.add(IFloodlightProviderService.class);
        l.add(IRestApiService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
        switchService = context.getServiceImpl(IOFSwitchService.class);
        restApiService = context.getServiceImpl(IRestApiService.class);
        logger = LoggerFactory.getLogger(DDosProtectionConfig.class);
        logger.info("init");
        // StaticConfig Test
        logger.info("fetch config data");
        supervisedConfig = new SupervisedConfig();
        dynamicConfig = new DynamicConfig();
        DDosProtectionConfig.parseXmlData(configPath);
        logger.info("fetch ok size: {}",DDosProtectionConfig.staticConfigs.size());
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProviderService.addOFMessageListener(OFType.PACKET_IN, this);
        restApiService.addRestletRoutable(new DDosProtectionWebRoutable());
        logger.info("start up");
    }

    @Override
    public String getSName() {
        return this.getName();
    }

    @Override
    public void addStaticConfig(StaticConfigItem item) {
        if(this.writeStaticConfigToOFSwitch(item)) return;
        DDosProtectionConfig.staticConfigs.add(item);
    }

    public static class StaticConfigItem {
        String swID;
        String MAC;
        String IP;
        public void setSWID(String swID) {
            this.swID = swID;
        }
        public void setMAC(String MAC) {
            this.MAC = MAC;
        }
        public void setIP(String IP) {
            this.IP = IP;
        }
        public String getSwID() {
            return this.swID;
        }
        public String getMAC() {
            return this.MAC;
        }
        public String getIP() {
            return this.IP;
        }
        public void print() {
            System.out.print(this.swID + " " + this.MAC + " " + this.IP);
        }
        public StaticConfigItem() {
            super();
        }
    }

    public class SupervisedConfig{
        boolean open = false;
        float maxTotalPiRateThreshold = 1.0f;
        float maxFlowEntryThreshold = 1.0f;
        float maxSinglePiRateThreshold = 1.0f;
        String defencePolicy;
        boolean checkValid(String info) {
            if(info.equals("") || info.equals(" ") || info.length() < 2) return false;
            return true;
        }
        public void setOpenSupervised(String opSupervised) {
            if(!checkValid(opSupervised))return;
            if(opSupervised.equals("ON")) open = true;
            else open = false;
        }
        public void setMaxTotalPiRateThreshold(String piRateThreshold) {
            if(!checkValid(piRateThreshold))return;
            this.maxTotalPiRateThreshold = Float.valueOf(piRateThreshold).floatValue();
        }
        public void setMaxSinglePiRateThreshold(String piRateThreshold) {
            if(!checkValid(piRateThreshold))return;
            this.maxSinglePiRateThreshold = Float.valueOf(piRateThreshold).floatValue();
        }
        public void setMaxFlowEntryThreshold(String flowEntryThreshold) {
            if(!checkValid(flowEntryThreshold))return;
            this.maxFlowEntryThreshold = Float.valueOf(flowEntryThreshold).floatValue();
        }
        public void setDefencePolicy(String defencePolicy) {
            if(!checkValid(defencePolicy))return;
            this.defencePolicy = defencePolicy;
        }
        public String getDefencePolicy() {
            return this.defencePolicy;
        }
        public float getMaxTotalPiRateThreshold() {
            return this.maxTotalPiRateThreshold;

        }
        public float getMaxSinglePiRateThreshold() {
            return this.maxSinglePiRateThreshold;

        }
        public float getMaxFlowEntryThreshold() {
            return this.getMaxFlowEntryThreshold();
        }
        public String getState() {
            String state = "{" +
                    "Open:" + open + "\n"+
                    "maxTotalPiRateThreshold:"+maxTotalPiRateThreshold + "\n" +
                    "maxSinglePiRateThreshold:" + maxSinglePiRateThreshold + "\n" +
                    "maxFlowEntryThreshold:"+maxFlowEntryThreshold+"\n"+
                    "defencePolicy:"+defencePolicy+"\n"
            +"}\n";
            return state;
        }
    }

    public class DynamicConfig{
        // only support dhcp temporarily
        String dhcpServerIP;
        String dhcpServerMAC;
        String authenticationToken;
        boolean beOpen = false;
        boolean checkValid(String info) {
            if(info.equals("") || info.equals(" ") || info.length() < 2) return false;
            return true;
        }

        public void setOpenDHCPConfig(String open) {
            if(!checkValid(open))return;
            if(open.equals("ON")) beOpen = true;
            else beOpen = false;
        }

        public void setDhcpServerIP(String dhcpServerIP) {
            if(!checkValid(dhcpServerIP))return;
            this.dhcpServerIP = dhcpServerIP;
        }

        public void setDhcpServerMAC(String dhcpServerMAC) {
            if(!checkValid(dhcpServerMAC))return;
            this.dhcpServerMAC = dhcpServerMAC;
        }

        public void setAuthenticationToken(String authenticationToken) {
            if(!checkValid(authenticationToken))return;
            this.authenticationToken = authenticationToken;
        }

        public String getDhcpServerIP() {
            return this.dhcpServerIP;
        }
        public String getDhcpServerMAC() {
            return this.dhcpServerMAC;
        }
        public String getAuthenticationToken() {
            return this.authenticationToken;
        }
        public String getState() {
            String state = "{\n" +
                    "DHCPOpen:" + beOpen + "\n"+
                    "dhcpServerIP:"+dhcpServerIP + "\n" +
                    "dhcpServerMAC:" + dhcpServerMAC + "\n" +
                    "authenticateToken:"+authenticationToken+"\n"+
                    "}\n";
            return state;
        }
    }

    public static SupervisedConfig getSupervisedConfig() {
        return supervisedConfig;
    }
    public static DynamicConfig getDynamicConfig() {
        return dynamicConfig;
    }
}


