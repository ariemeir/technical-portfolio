package edu.ucsf.roboimplant.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import edu.ucsf.roboimplant.data.Adjustment;
import edu.ucsf.roboimplant.data.Patient;
import edu.ucsf.roboimplant.db.PatientDbAdapter;
import edu.ucsf.roboimplant.generic.ConfigDB;
import edu.ucsf.roboimplant.menu.AdjustmentListActivity;
import edu.ucsf.roboimplant.menu.AdjustmentListAdapter;
import edu.ucsf.roboimplantconsole.R;

public class PatientSummary extends Activity {

	public final static String ITEM_TITLE = "title";
	public final static String ITEM_CAPTION = "caption";
	private String patient_id=null;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		double totalAdjustment=0;
		double totalCost=0;
		patient_id = (String) this.getIntent().getExtras().get(ConfigDB.FIELD_PATIENT_ID);
	    setTitle("Patient "+patient_id+" : Treatment Summary");
	
	    //ArrayList<Part> parts= PartListActivity.getInstance().getData(patient_id);
	    //PartListAdapter partAdapter = new PartListAdapter(this,R.layout.part_list_entry,parts);

	    ArrayList<Adjustment> adjustment= AdjustmentListActivity.getInstance().getData(patient_id);
	    AdjustmentListAdapter laborAdapter = new AdjustmentListAdapter(this,R.layout.adjustment_list_entry,adjustment);

		
		// create our list and custom adapter
		SeparatedListAdapter adapter = new SeparatedListAdapter(this);
		//add the fixed fields on the top.
		
		PatientDbAdapter.getInstance().open();
		Cursor c = PatientDbAdapter.getInstance().fetchEntry(patient_id);
		Patient patient = 	        	new Patient(  c.getString(0),c.getString(1),
				c.getString(2),	c.getString(3),	c.getString(4),	c.getString(5));

		//precalculate the costs for the total field
		totalAdjustment = patient.getTotalAdjustment(adjustment); 
		totalCost = totalAdjustment;
		
		adapter.addSection("Patient ID:"+patient.id+"                  Name: "+patient.name
				+"      BirthDate: "+patient.birthdate,
				new ArrayAdapter<String>(this,R.layout.caption_list_entry, 
						new String[] {"Total Cost: "+"$"+totalCost}));
	
		adapter.addSection("Address:"+patient.address+"    Phone:"+patient.phone, 
				new ArrayAdapter<String>(this,R.layout.caption_list_entry, new String[] {}));
		
		
		//adapter.addSection("Sku               Description                   WH      Planned     Issued     Cost",partAdapter);
		//adjustments
		String totalAdjustmentString = totalAdjustment+ " [mm]";
		adapter.addSection("Total Adjustment: 				      	 "+totalAdjustmentString,
				new ArrayAdapter<String>(this,R.layout.list_item, new String[] {}));
		
		System.out.println("LaborGetcount : "+laborAdapter.getCount());
		adapter.addSection("Technician                     Start            Stop            Hours         Rate ",laborAdapter);
		ListView list = new ListView(this);
		list.setAdapter(adapter);
		this.setContentView(list);

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	  System.out.println("Configuration changed.... so what ? ");
	  super.onConfigurationChanged(newConfig);
	}

	public Map<String,?> createItem(String title, String caption) {
		Map<String,String> item = new HashMap<String,String>();
		item.put(ITEM_TITLE, title);
		item.put(ITEM_CAPTION, caption);
		return item;
	}


}
