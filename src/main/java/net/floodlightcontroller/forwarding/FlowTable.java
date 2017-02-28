package net.floodlightcontroller.forwarding;

import java.util.HashMap;
import java.util.LinkedList;

public class FlowTable {
	protected static int LABEL_POOL_SIZE = 4096; // this need to be smaller than 2^^20
	protected HashMap<FlowItem, Integer> used1;
	protected HashMap<Integer, FlowItem> used2;
	protected LinkedList<Integer> unused;
	
	public FlowTable() {
		// TODO Auto-generated constructor stub
		used1 = new HashMap<FlowItem, Integer>();
		used2 = new HashMap<Integer, FlowItem>();
		unused = new LinkedList<Integer>();
		for (int i =1;i < LABEL_POOL_SIZE ; i ++){
			unused.addLast(i);
		}
	}
	public synchronized int ishavelabel(FlowItem fi){
		Object label = used1.get(fi);
		if (label==null){
			return -1;
		}
		else{
			return (int)label;
		}
	}
	
	public synchronized int getnewlabel(FlowItem fi){
		int label=unused.removeLast();
		used1.put(fi,label);
		used2.put(label,fi);
		//System.out.println(used1.size()+ " "+ used2.size());
		return label;
	}
	
	public synchronized boolean restorelabel(int  num)
	{
		FlowItem fi = used2.get(num);
		if (fi!=null){
			if (used1.remove(fi)!=null && used2.remove(num)!=null){
				unused.addLast(num);
				//System.out.println(used1.size()+ " "+ used2.size());
				return true;
			}
		}
		return false;
	}
}

