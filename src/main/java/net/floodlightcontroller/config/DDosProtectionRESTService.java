package net.floodlightcontroller.config;

import net.floodlightcontroller.core.module.IFloodlightService;

/**
 * Created by dalaoshe on 17-3-11.
 */
public interface DDosProtectionRESTService extends IFloodlightService {
    public String getSName();
    public void addStaticConfig(DDosProtectionConfig.StaticConfigItem item);
}
