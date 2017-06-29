package net.floodlightcontroller.statistics;

import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.python.antlr.PythonParser.else_clause_return;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.forwarding.Forwarding;

public class HostEntry {
	private static final Logger log = LoggerFactory.getLogger(HostEntry.class);
	private IPv4Address ip;
	private IOFSwitch sw;
	
	public int pi;
	public int piCopy;
	public double score;
	public int level;
	public boolean FIRST_SCORE;
	private int highFlowNumber;
	private int number;
	
	public HostEntry(IPv4Address ipAddress, IOFSwitch swit) {
		number = 0;
		highFlowNumber = 0;
		ip = ipAddress;
		sw = swit;
		pi = 0;
		score = 0;
		piCopy = 0;
		FIRST_SCORE = true;
		level = 2;
	}
	
	public void init() {
		number = 0;
		highFlowNumber = 0;
		pi = piCopy;
		piCopy = 0;
	}
	
	public void setScore(double s) {
		score = s;
	}
	public String toString() {
		return "highCount(" + String.valueOf(highFlowNumber) + "),"
				+ " count(" + String.valueOf(number) + "),"
				+ " pi(" + String.valueOf(pi) + "),"
				+ " score(" + String.valueOf((int)score) + "),"
				+ " level(" + String.valueOf(level) + ").";
	}
	public int getHighFlowNumber() {
		return highFlowNumber;
	}
	public int getNumber() {
		return number;
	}
	public double getScore() {
		return score;
	}
	
	public void compute() {
		// count part
		double temp = 0.0;
		if (number < 2) temp = ShieldManager.countLow;
		else
			temp = (double)highFlowNumber / number;
		temp *= 10;
		int countScore = 0;
		if (temp >= ShieldManager.countHigh) 		countScore = 2;
		else if (temp >= ShieldManager.countLow) 	countScore = 1;

		// pi part
		int piScore = 0;
		if (pi < ShieldManager.piLow) 		piScore = 2;
		else if (pi < ShieldManager.piHigh) piScore = 1;
		
		if (FIRST_SCORE) {
			FIRST_SCORE = false;
			score = piScore + countScore;
		} else {
			score = ShieldManager.alpha * (piScore + countScore) + (1 - ShieldManager.alpha) * score;
		}
		
		
		if (piScore == 0 || score < 1.7) level = 1;
		else if (piScore == 2 && countScore >= 1 && level != 1) level = 3;
		else level = 2;
		
				
//		if (level == 1) Forwarding.installDropEntry(ip, sw);
	}
	
	public void addHighCount(OFFlowStatsEntry pse) {
		int packetCount = (int)pse.getPacketCount().getValue();
        number += 1;
        if (packetCount > ShieldManager.highCount) {
            this.highFlowNumber += 1;
        }
	}
}
