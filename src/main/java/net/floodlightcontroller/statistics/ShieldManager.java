package net.floodlightcontroller.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

public class ShieldManager implements IFloodlightModule {
	private static final Logger log = LoggerFactory.getLogger(ShieldManager.class);
	protected IFloodlightProviderService floodlightProvider;
	
	private static final String ALPHA_STR = "alpha";
	private static final String HIGH_COUNT_STR = "highcount";
	public static double alpha = 0;
	public static int highCount = 1;
	private static final String PI_LOW_STR = "pilow";
	private static final String PI_HIGH_STR = "pihigh";
	public static int piLow = 0;
	public static int piHigh = 0;
	private static final String COUNT_LOW_STR = "countlow";
	private static final String COUNT_HIGH_STR = "counthigh";
	public static int countLow = 0;
	public static int countHigh = 0;
	private static final String SCORE_LOW_STR = "scorelow";
	private static final String SCORE_HIGH_STR = "scorehigh";
	public static double scoreLow = 0.0;
	public static double scoreHigh = 0.0;
	
	
	public ShieldManager() {}

	public static void addHost(IPv4Address ip, IOFSwitch sw) {
		if (!StatisticsCollector.hostFlowMap.containsKey(ip)) {
			StatisticsCollector.hostFlowMap.put(ip, new HostEntry(ip, sw));
			StatisticsCollector.hostDpMap.put(ip, sw.getId());
			log.debug("######ADD-IP-{}, {}", ip.toString(), sw.getId().toString());
		}
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
	    Collection<Class<? extends IFloodlightService>> l =
	            new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(IFloodlightProviderService.class);
	    return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		Map<String, String> config = context.getConfigParams(this);
		if (config.containsKey(ALPHA_STR)) {
			try {
				alpha = Double.parseDouble(config.get(ALPHA_STR).trim());
				log.debug("######INIT-{} = {}", ALPHA_STR, alpha);
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", ALPHA_STR, alpha);
			}
		}
		if (config.containsKey(HIGH_COUNT_STR)) {
			try {
				highCount = Integer.parseInt(config.get(HIGH_COUNT_STR).trim());
				log.debug("######INIT-{} = {}", HIGH_COUNT_STR, highCount);
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", HIGH_COUNT_STR, highCount);
			}
		}
		if (config.containsKey(PI_LOW_STR)) {
			try {
				piLow = Integer.parseInt(config.get(PI_LOW_STR).trim());
				log.debug("######INIT-{} = {}", PI_LOW_STR, piLow);
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", PI_LOW_STR, piLow);
			}
		}
		if (config.containsKey(PI_HIGH_STR)) {
			try {
				piHigh = Integer.parseInt(config.get(PI_HIGH_STR).trim());
				log.debug("######INIT-{} = {}", PI_HIGH_STR, piHigh);
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", PI_HIGH_STR, piHigh);
			}
		}
		if (config.containsKey(COUNT_LOW_STR)) {
			try {
				countLow = Integer.parseInt(config.get(COUNT_LOW_STR).trim());
				log.debug("######INIT-{} = {}", COUNT_LOW_STR, countLow);
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", COUNT_LOW_STR, countLow);
			}
		}
		if (config.containsKey(COUNT_HIGH_STR)) {
			try {
				countHigh = Integer.parseInt(config.get(COUNT_HIGH_STR).trim());
				log.debug("######INIT-{} = {}", COUNT_HIGH_STR, countHigh);
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", COUNT_HIGH_STR, countHigh);
			}
		}
		if (config.containsKey(SCORE_LOW_STR)) {
			try {
				scoreLow = Double.parseDouble(config.get(SCORE_LOW_STR).trim());
				log.debug("######INIT-{} = {}", SCORE_LOW_STR, scoreLow);
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", SCORE_LOW_STR, scoreLow);
			}
		}
		if (config.containsKey(SCORE_HIGH_STR)) {
			try {
				scoreHigh = Double.parseDouble(config.get(SCORE_HIGH_STR).trim());
				log.debug("######INIT-{} = {}", SCORE_HIGH_STR, scoreHigh);
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", SCORE_HIGH_STR, scoreHigh);
			}
		}
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		
	}
}
