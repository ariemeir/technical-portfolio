package edu.ucsf.roboimplantconsole;

import java.util.ArrayList;

public class MessageDispatcher 
{
	private ArrayList<RoboMessageListener> mylisteners = new ArrayList();
	
	private static MessageDispatcher _instance=null;
	public static String CURRENT_UPDATE = "btcurrent";
	public static String CURRENT_AMPLITUDE_UPDATE = "btcurrentamplitude";
	public static String VOLTAGE_UPDATE = "btvoltage";
	public static String VOLTAGE_AMPLITUDE_UPDATE = "btvoltageamplitude";
	public static String ANGULAR_SPEED_UPDATE = "btangularspeed";
	public static String TIME_COUNTER_UPDATE = "btmseccounter";
	public static String DOSAGE_REMAINING_UPDATE = "btremaining";
	public static String PROCEDURE_TERMINATED = "btterminated";
	public static String LOST_COUPLING = "btlostcoupling";
	public static String ESTABLISHED_COUPLING = "btestablishedcoupling";
	
	
	
	private MessageDispatcher()
	{
	}
	
	public static MessageDispatcher getInstance()
	{
		if (_instance==null)
			_instance=new MessageDispatcher();
		return _instance;
	}
	
	public void process(String msg)
	{
		//System.out.println("dispatcher got:"+msg);
		
		 String[] params = msg.split("\\s");
	     //for (int x=0; x<result.length; x++)
	       //  System.out.println(result[x]);
	     
		String opcode = params[0]; 
		if (opcode.equalsIgnoreCase(CURRENT_UPDATE))
		{
			//For this opcode, the first parameter is the scaled (to integer) magnitude, and the second parameter is the scaling factor
			//System.out.println("result[1]:"+result[1]+"len="+result[1].length()+"obj:"+Integer.parseInt(result[1]));
			
			int value = Integer.parseInt(params[1]);
			int scalingFactor = Integer.parseInt(params[2]);
			//System.out.println("Sensor value:"+value);
			float rescaledValue = (float)value/scalingFactor;
			ServiceTerminal.updateCurrentData(rescaledValue);
		}
		else if (opcode.equalsIgnoreCase(CURRENT_AMPLITUDE_UPDATE))
		{
			//For this opcode, the first parameter is the scaled (to integer) magnitude, and the second parameter is the scaling factor
			int value = Integer.parseInt(params[1]);
			int scalingFactor = Integer.parseInt(params[2]);
			float rescaledValue = (float)value/scalingFactor;
			//System.out.println("Sensor value:"+value);
			ServiceTerminal.updateCurrentAmplitude(rescaledValue);	
		}

		else if (opcode.equalsIgnoreCase(VOLTAGE_UPDATE))
		{
			//For this opcode, the first parameter is the scaled (to integer) magnitude, and the second parameter is the scaling factor
			int value = Integer.parseInt(params[1]);
			int scalingFactor = Integer.parseInt(params[2]);
			float rescaledValue = (float)value/scalingFactor;
			//System.out.println("Sensor value:"+value);
			ServiceTerminal.updateVoltageData(rescaledValue);	
		}
		else if (opcode.equalsIgnoreCase(VOLTAGE_AMPLITUDE_UPDATE))
		{
			//For this opcode, the first parameter is the scaled (to integer) magnitude, and the second parameter is the scaling factor
			int value = Integer.parseInt(params[1]);
			int scalingFactor = Integer.parseInt(params[2]);
			float rescaledValue = (float)value/scalingFactor;
			//System.out.println("Sensor value:"+value);
			ServiceTerminal.updateVoltageAmplitude(rescaledValue);	
		}

		else if (opcode.equalsIgnoreCase(ANGULAR_SPEED_UPDATE ))
		{
			int value = Integer.parseInt(params[1]);
			int scalingFactor = Integer.parseInt(params[2]);
			double rescaledValue = (double)value/scalingFactor;
			//System.out.println("Sensor value:"+value);
			ServiceTerminal.updateAngularSpeed(rescaledValue);	
			
		}
		else if (opcode.equalsIgnoreCase(DOSAGE_REMAINING_UPDATE))
		{

			int remainingSpins = Integer.parseInt(params[1]);
			int targetSpins = Integer.parseInt(params[2]);
			System.out.println("remaining spins:"+remainingSpins+" out of "+targetSpins);
			AdjustmentActivity.updateRemainingSpins(remainingSpins,targetSpins);	
		}
		else if (opcode.equalsIgnoreCase(PROCEDURE_TERMINATED))
		{
			AdjustmentActivity.notifyTermination();
		}
		else if (opcode.equalsIgnoreCase(LOST_COUPLING))
		{
			AdjustmentActivity.notifyLostCoupling();
		}
		else if (opcode.equalsIgnoreCase(ESTABLISHED_COUPLING))
		{
			AdjustmentActivity.notifyEstablishedCoupling();
		}
    	synchronized(mylisteners)
    	{
			for (int i=0;i<mylisteners.size();i++)
				mylisteners.get(i).notifyListener(opcode, params);
    	}
	}
	
	
	public void registerListener(RoboMessageListener listener)
	{
    	synchronized(mylisteners)
    	{
    		mylisteners.add(listener);
    	}
	}

	public void removeListener(
			RoboMessageListener listener) {
		
    	synchronized(mylisteners)
    	{
    		mylisteners.remove(listener);
		}
		
	}
	
}
