package net.floodlightcontroller.config;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
/**
 * Created by dalaoshe on 17-3-27.
 */
public class DDosDynamicConfigResource extends ServerResource{
    @Get("json")
    public String installConfig() {
        String dhcpIP = (String) getRequestAttributes().get(DDosProtectionWebRoutable.DHCP_SERVER_IP);
        String dhcpMAC = (String) getRequestAttributes().get(DDosProtectionWebRoutable.DHCP_SERVER_MAC);
        String token = (String) getRequestAttributes().get(DDosProtectionWebRoutable.DHCP_SERVER_AUTHENTICATION_TOKEN);
        String open = (String) getRequestAttributes().get(DDosProtectionWebRoutable.OPEN_DYNAMIC_CONFIG);
        DDosProtectionConfig.getDynamicConfig().setAuthenticationToken(token);
        DDosProtectionConfig.getDynamicConfig().setDhcpServerIP(dhcpIP);
        DDosProtectionConfig.getDynamicConfig().setDhcpServerMAC(dhcpMAC);
        DDosProtectionConfig.getDynamicConfig().setOpenDHCPConfig(open);
        return  DDosProtectionConfig.getDynamicConfig().getState();
    }
}
