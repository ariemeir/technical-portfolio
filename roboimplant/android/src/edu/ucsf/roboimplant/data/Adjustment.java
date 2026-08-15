package edu.ucsf.roboimplant.data;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.ucsf.roboimplant.generic.ConfigDB;

public class Adjustment implements Serializable {

	public String adjustment_id;
	public String patient_id;
	public String adjustment_date;
	public String adjustment_amount;
	
	public Adjustment()
	{
		
	}
	
	public Adjustment(String _id,String _patient_id,String _adjustment_date,String _adjustment_amount)
			
	{
		adjustment_id=_id;
		patient_id  = _patient_id;
		adjustment_date=_adjustment_date;
		adjustment_amount = _adjustment_amount;
	}

	public String toString()
	{
		String s = "";
		s = "adjustment_id:"+adjustment_id+", performed on :"+adjustment_date+", of distance: "+adjustment_amount;
		return s;
	}

	
}
