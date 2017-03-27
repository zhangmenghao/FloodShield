package net.floodlightcontroller.config;

import net.floodlightcontroller.restserver.RestletRoutable;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Route;
import org.restlet.routing.Router;

/**
 * Created by dalaoshe on 17-3-11.
 */
public class DDosProtectionWebRoutable implements RestletRoutable {
    public static String MAC = "MAC";
    public static String IP = "IP";
    public static String SWID = "SWID";
    public static String OPEN_SUPERVISED = "OPEN_SUPERVISED";
    public static String OPEN_DYNAMIC_CONFIG = "OPEN_DYNAMIC_CONFIG";
    public static String OPEN_STATIC_CONFIG = "OPEN_STATIC_CONFIG";

    public static String MAX_TOTAL_PI_RATE_THRESHOLD = "MAX_TOTAL_PI_RATE_THRESHOLD";
    public static String MAX_SINGLE_PI_RATE_THRESHOLD = "MAX_SINGLE_PI_RATE_THRESHOLD";
    public static String MAX_FLOW_ENTRY_THRESHOLD = "MAX_FLOW_ENTRY_THRESHOLD";


    public static String DHCP_SERVER_IP = "DHCP_SERVER_IP";
    public static String DHCP_SERVER_MAC = "DHCP_SERVER_MAC";
    public static String DHCP_SERVER_AUTHENTICATION_TOKEN = "DHCP_SERVER_AUTHENTICATION_TOKEN";

    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        router.attach("/{"+OPEN_STATIC_CONFIG+"}/{"+MAC+"}/{"+IP+"}/{"+SWID+"}/json", DDosProtectionResource.class);
        router.attach("/{"+OPEN_DYNAMIC_CONFIG+"}/{"+DHCP_SERVER_AUTHENTICATION_TOKEN+"}/{"+DHCP_SERVER_IP+"}/" +
                              "{"+DHCP_SERVER_MAC+"}/dynamic", DDosDynamicConfigResource.class);
        router.attach("/{"+OPEN_SUPERVISED+"}/{"+MAX_SINGLE_PI_RATE_THRESHOLD+"}/{"+MAX_TOTAL_PI_RATE_THRESHOLD+"}" +
                              "/{"+MAX_FLOW_ENTRY_THRESHOLD+"}/supervision", DDosSupervisedConfig.class);

        return router;
    }

    @Override
    public String basePath() {
        return "/wm/config";
    }
}
