package net.floodlightcontroller.config;

import net.floodlightcontroller.core.web.CoreWebRoutable;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * Created by dalaoshe on 17-3-11.
 */
public class DDosProtectionResource extends ServerResource {

    @Get("json")
    public String installConfig() {

        String MAC = (String) getRequestAttributes().get(DDosProtectionWebRoutable.MAC);
        String IP = (String) getRequestAttributes().get(DDosProtectionWebRoutable.IP);
        String SWID = (String) getRequestAttributes().get(DDosProtectionWebRoutable.SWID);
        DDosProtectionConfig.StaticConfigItem item = new DDosProtectionConfig.StaticConfigItem();
        item.setIP(IP);
        item.setMAC(MAC);
        item.setSWID(SWID);
        DDosProtectionRESTService service = (DDosProtectionRESTService) getContext().getAttributes().
                get(DDosProtectionRESTService.class.getCanonicalName());
        service.addStaticConfig(item);
        return SWID;
    }
}
