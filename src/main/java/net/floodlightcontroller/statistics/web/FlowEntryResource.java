package net.floodlightcontroller.statistics.web;

import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.statistics.OFSwitchFlowStatistics;
import net.floodlightcontroller.statistics.StatisticsCollector;
import org.projectfloodlight.openflow.types.DatapathId;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dalaoshe on 17-3-18.
 */
public class FlowEntryResource extends ServerResource {
    private static String FLOW_NUMBER = "FLOW_NUMBER";
    private static String HARD_NUMBER = "HARD_NUMBER";
    private static String INVALID_RATE = "INVALID_RATE";
    private static String SWITCH_ID = "SWITCH_ID";
    private static String SWITCH_STATISTIC = "SWITCH_STATISTIC";
    private static String ALL_FLOW_NUMBER = "ALL_FLOW_NUMBER";
    @Get("json")
    @Put
    @Post
    public Object retrieve() {
        StatisticsCollector statisticsService = (StatisticsCollector) getContext().getAttributes().get(IStatisticsService.class.getCanonicalName());
        String result = "";
        ArrayList<HashMap<String, String>> switch_stats = new ArrayList<HashMap<String, String>>();
        double all_nmb = 0;
        double all_invalid = 0;
        for(Map.Entry<DatapathId, OFSwitchFlowStatistics> e: statisticsService.switchFlowStatisticsHashMap.entrySet()) {
            HashMap<String, String> item = new HashMap<String, String>();

            item.put(FLOW_NUMBER,String.valueOf(e.getValue().getAll_flow_nmb()));
          //  item.put(SWITCH_ID,String.valueOf(e.getValue().getSwID().toString()));
            item.put(HARD_NUMBER,String.valueOf(e.getValue().getHard_flow_nmb()));
            switch_stats.add(item);
            all_nmb += e.getValue().getAll_flow_nmb();
            all_invalid += e.getValue().getLow_used_flow_nmb();
//            item.put(FLOW_NUMBER,String.valueOf(e.getValue().getAll_flow_nmb()));
        }
        if(all_nmb == 0)all_nmb = 1;
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put(FLOW_NUMBER, all_nmb);
        map.put(INVALID_RATE, new Double(all_invalid/all_nmb));
        map.put(SWITCH_STATISTIC,switch_stats);
        return map;
    }
}
