package net.floodlightcontroller.forwarding;

public class HostWindowEntry {
	public int limit;
	public int number;
	
	public HostWindowEntry() {
		this.limit = 10;
		this.number = 0;
	}
	
	public HostWindowEntry(int limit, int nubmer) {
		this.limit = limit;
		this.number = number;
	}
	
	public void update() {}
}
