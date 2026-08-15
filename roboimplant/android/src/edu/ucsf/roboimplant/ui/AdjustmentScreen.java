package edu.ucsf.roboimplant.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import edu.ucsf.roboimplant.data.Adjustment;
import edu.ucsf.roboimplant.db.AdjustmentDbAdapter;
import edu.ucsf.roboimplant.generic.ConfigDB;
import edu.ucsf.roboimplantconsole.R;

public class AdjustmentScreen extends Activity 
{
	private static final int SAVE_LABOR_REPORT = 0;
	private static final String[] techies = {"John Smith","Jack WU","Ivan Kim"}; 
	View v=null;
	String _patient_id;
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    System.out.println("Creating activity AdjustmentScreen");
	    v = View.inflate(this.getBaseContext(),R.layout.adjustment_data,null);
	    setupSpinner(v);
	    setTitle("Adjustment Report");
	    setContentView(v);
        _patient_id = (String) this.getIntent().getExtras().get(ConfigDB.FIELD_PATIENT_ID);
        System.out.println("soid = "+_patient_id);
	}

	
	private void setupSpinner(View v)
	{
		Spinner sp = (Spinner)v.findViewById( R.id.labor_technician);
		ArrayList<String> allTechies = new ArrayList<String>();
	    for (int i = 0; i < techies.length; i++) {
	      allTechies.add(techies[i]);
	    }

	    ArrayAdapter<String> aspnTechies = new ArrayAdapter<String>
	    		(this,android.R.layout.simple_spinner_item, allTechies);
	    aspnTechies.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

	    sp.setAdapter(aspnTechies); 
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu)   
    {
    	System.out.println("Populating menu++++++++++++++++++++++++++++++++++++++++++++"); 
    
    	menu.add(0,SAVE_LABOR_REPORT,0,"Save").setIcon(R.drawable.save);
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) 
    {
    	CheckBox cb=null; 
    	System.out.println("Selected : "+item.getItemId());
         switch(item.getItemId()){
              case SAVE_LABOR_REPORT:
                  System.out.println("Save");
                  Adjustment lb = getLaborFromUI();
                  saveLabor(lb);
                  finish();
            	  return true;
         }
         return super.onMenuItemSelected(featureId, item);
    }

    Adjustment getLaborFromUI()
    {
		Adjustment lb=null;
    	String tech = ((Spinner)v.findViewById( R.id.labor_technician)).getSelectedItem().toString();
    	String rate = ((TextView)v.findViewById( R.id.labor_rate)).getText().toString();
		String adj_id = "adj_"+System.currentTimeMillis();
    	lb = new Adjustment(adj_id,_patient_id,"10/10/2012","10");
		
		return lb;
    }

    void saveLabor(Adjustment adj)
    {
    	AdjustmentDbAdapter adjDbHelper = AdjustmentDbAdapter.getInstance();
        adjDbHelper.open();
        adjDbHelper.createEntry(adj.adjustment_id, adj.patient_id, adj.adjustment_date,	adj.adjustment_amount); 
        adjDbHelper.close();
    }
    
    @Override
	public void onConfigurationChanged(Configuration newConfig) {
	  System.out.println("Configuration changed.... so what ? ");
	  super.onConfigurationChanged(newConfig);
	}


}


