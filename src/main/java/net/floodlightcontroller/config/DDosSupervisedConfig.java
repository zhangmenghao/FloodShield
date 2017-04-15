package net.floodlightcontroller.config;
import net.floodlightcontroller.core.web.CoreWebRoutable;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * Created by dalaoshe on 17-3-27.
 */
public class DDosSupervisedConfig  extends ServerResource {
    @Get("json")
    public String installConfig() {

        String open = (String) getRequestAttributes().get(DDosProtectionWebRoutable.OPEN_SUPERVISED);
        String max_total_pi_rate = (String) getRequestAttributes().get(DDosProtectionWebRoutable.MAX_TOTAL_PI_RATE_THRESHOLD);
        String max_single_pi_rate = (String) getRequestAttributes().get(DDosProtectionWebRoutable.MAX_SINGLE_PI_RATE_THRESHOLD);
        String max_flow_entry = (String) getRequestAttributes().get(DDosProtectionWebRoutable.MAX_FLOW_ENTRY_THRESHOLD);
        DDosProtectionRESTService service = (DDosProtectionRESTService) getContext().getAttributes().
                get(DDosProtectionRESTService.class.getCanonicalName());
        DDosProtectionConfig.getSupervisedConfig().setMaxFlowEntryThreshold(max_flow_entry);
        DDosProtectionConfig.getSupervisedConfig().setMaxTotalPiRateThreshold(max_total_pi_rate);
        DDosProtectionConfig.getSupervisedConfig().setMaxSinglePiRateThreshold(max_single_pi_rate);
        DDosProtectionConfig.getSupervisedConfig().setOpenSupervised(open);
        return DDosProtectionConfig.getSupervisedConfig().getState();
    }
}
