package edu.ucsf.roboimplant.data;

import java.io.Serializable;
import java.util.ArrayList;


import edu.ucsf.roboimplant.db.PatientDbAdapter;
import edu.ucsf.roboimplant.menu.AdjustmentListActivity;

public class Patient implements Serializable {

	public static String STATE_INPROGRESS = "INP";
	public static String STATE_COMPLETED  = "CMP";
	public static String STATE_SUSPENDED  = "SUS";
	
	//unique id of the patient
	public String id;
	//patient name
	public String name;
	public String birthdate;
	//the date of the implantation surgery
	public String implant_date;
	public String address;
	public String phone;
	
	
	public Patient()
	{
		
	}
	
	public Patient(String _id,String _name,String _birthdate,String _implant_date,
					String _address,String _phone)
	{
		id=_id;
		name=_name;
		birthdate=_birthdate;
		implant_date=_implant_date;
		address=_address;
		phone=_phone;
		
	}
	
	public String toString()
	{
		return ""+"patient_id:"+id+",name="+name;
	}
	
	public double getTotalCost()
	{
	    //ArrayList<Part> parts= PartListActivity.getInstance().getData(patient_id);
	    ArrayList<Adjustment> adjustments= AdjustmentListActivity.getInstance().getData(id);
		PatientDbAdapter.getInstance().open();


		//precalculate the costs for the total field
		double total = getTotalAdjustment(adjustments); 
		return total;
	}
	
	
	public static double getTotalAdjustment(ArrayList<Adjustment> adjustments)
	{
		double total = 0,amount=0;
		for (int i=0;i<adjustments.size();i++)
		{
			Adjustment adj = adjustments.get(i);
			System.out.println("Adjustment is "+adj);
			amount = Double.parseDouble(adj.adjustment_amount);
			//System.out.println("Hours : "+hours);
			//System.out.println("Rate: "+Double.parseDouble(adj.rate));
			total += amount;
		}
		long tmp = (int)Math.round(total * 10);   
		total = tmp / 10.0;
		return total;
	}

	public boolean appearsOnActiveList()
	{
		return true;
	}
	
	
}
