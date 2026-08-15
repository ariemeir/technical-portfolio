package edu.ucsf.roboimplant.generic;

public class Utility {

	
	public static void delay(int nmsec)
	{
		try {
			Thread.sleep(nmsec);
		}
		catch(InterruptedException e)
		{
			System.out.println("Time to go back to work");
		}
	}
}
