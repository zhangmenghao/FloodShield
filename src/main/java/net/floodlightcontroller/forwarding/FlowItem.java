package net.floodlightcontroller.forwarding;

import java.util.List;

import org.projectfloodlight.openflow.protocol.action.OFAction;
import net.floodlightcontroller.core.IOFSwitch;

public class FlowItem {

	private IOFSwitch sw;
	private List<OFAction> actions;
	
	public FlowItem() {
		// TODO Auto-generated constructor stub
		sw=null;
		actions = null;
	}
	
	public IOFSwitch getSw() {
		return sw;
	}

	public void setSw(IOFSwitch sw) {
		this.sw = sw;
	}

	public List<OFAction> getActions() {
		return actions;
	}

	public void setActions(List<OFAction> actions) {
		this.actions = actions;
	}
	@Override
	public int hashCode(){
		int hashCode = 1;
		hashCode=hashCode*31+sw.hashCode();
		hashCode=hashCode*31+actions.hashCode();
		return hashCode;
	}
	@Override
	public boolean  equals(Object obj){
		if (obj instanceof FlowItem)
		if (((FlowItem)obj).sw.equals(this.sw) && ((FlowItem)obj).actions.equals(this.actions)){
			return true;
		}
		return false;
	}
}

