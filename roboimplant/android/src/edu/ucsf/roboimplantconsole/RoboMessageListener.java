package edu.ucsf.roboimplantconsole;

public interface RoboMessageListener {

	//this is the callback method called on each registered listener
	void notifyListener(String opcode, String[] params);
}
