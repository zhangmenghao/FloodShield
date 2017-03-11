package net.floodlightcontroller.config;

/**
 * Created by dalaoshe on 17-3-10.
 */
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
public class DDosProtectionConfig {
    public static ArrayList<StaticConfigItem> staticConfigs = new ArrayList<StaticConfigItem>();
    public static void parseXmlData(String xmlFilePath){
        Document doc = parseXML2Document(xmlFilePath);
        Element root  = doc.getRootElement();
        Element staticElement = root.element("StaticConfig");

        for(Iterator i_action = staticElement.elementIterator(); i_action.hasNext();){
            Element e_action = (Element)i_action.next();

            try {
                Class<?> c = StaticConfigItem.class;
                StaticConfigItem o = (StaticConfigItem) c.newInstance();
                String methodPrefix = "set";

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
}


