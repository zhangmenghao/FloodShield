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
    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);
        router.attach("/{"+MAC+"}/{"+IP+"}/{"+SWID+"}/json", DDosProtectionResource.class);
        return router;
    }

    @Override
    public String basePath() {
        return "/wm/config";
    }
}
