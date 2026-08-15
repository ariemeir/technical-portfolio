package edu.ucsf.roboimplant.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import edu.ucsf.roboimplant.data.Patient;
import edu.ucsf.roboimplant.db.AdjustmentDbAdapter;
import edu.ucsf.roboimplant.db.PatientDbAdapter;
import edu.ucsf.roboimplant.generic.ConfigDB;
import edu.ucsf.roboimplantconsole.R;

public class PatientScreen extends Activity 
{
	private final static int SAVE_PATIENT_INFO	=0;
	private final static int DELETE_PATIENT_INFO	=1;
	
	View v=null;
	String _soid;
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    System.out.println("Creating activity SOScreen");
	    //setContentView(R.layout.so_data);
        _soid = (String) this.getIntent().getExtras().get(ConfigDB.FIELD_PATIENT_ID);
        System.out.println("soid = "+_soid);
        v = fillInData();
        setContentView(v);
        setTitle("SO "+_soid+" : Details");
        Toast.makeText(PatientScreen.this, "Click on field to edit !", Toast.LENGTH_SHORT).show();
	}

	 @Override
	 public boolean onPrepareOptionsMenu(Menu menu)
	 {
		 System.out.println("Menu preparation .................");
		 return true;
	 }

	 @Override
    public boolean onCreateOptionsMenu(Menu menu)   
    {
    	System.out.println("Populating menu+++inside +++++++++++++++++++++++++++++++++++++++++"); 
    
    	//menu.add(0,0,SAVE_PATIENT_INFO,"Save").setIcon(R.drawable.save);
    	//menu.add(1,1,DELETE_PATIENT_INFO,"Delete").setIcon(R.drawable.delete);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.patient_details_screen_menu, menu);
        menu.getItem(0).setIcon(R.drawable.ic_menu_monitor);
    	return true;//super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) 
    {
    	System.out.println("Selected : "+item.getItemId());
         switch(item.getItemId()){
              case R.id.save_patient:
                  System.out.println("Save");
                  Patient sd = getServiceOrderFromUI();
                  savePatient(sd);
                  finish();
            	  return true;
              case R.id.delete_patient:
            	  System.out.println("Delete");
            	  if (!isSafeToDeleteSO(_soid,this))
            		  notifyUserCannotDelete();
            	  else
            	  {
            		  confirmDeletion(_soid);
            	  }
            	  return true;
         }
         return super.onMenuItemSelected(featureId, item);
    }
    
    public View fillInData()
    {
    	System.out.println("About to fill in data.........");
    	PatientDbAdapter.createInstance(this);
    	PatientDbAdapter inDbHelper = PatientDbAdapter.getInstance();
        inDbHelper.open();
        System.out.println("About to open db.........");

        // Get all of the notes from the database and create the item list
        Cursor c = inDbHelper.fetchEntry(_soid);
        System.out.println("Number of lines in the cursor : "+c.getCount());
        System.out.println("got entry id'ed "+_soid);
        startManagingCursor(c);
        System.out.println("started cursor");
        int cols = c.getColumnCount();
        System.out.println("Cols : "+cols);
        System.out.println("Current position in cursor : "+c.getPosition());
    	Patient patient = new Patient(  c.getString(0),
        						c.getString(1),
        						c.getString(2),
        						c.getString(3),
        						c.getString(4),
        						c.getString(5));
        inDbHelper.close();
        
        View v = View.inflate(this, R.layout.patient_data, null);
     
		TextView pat_id = (TextView)v.findViewById( R.id.patient_id);
		pat_id.setText( patient.id);
		//cannot change patient id;
		pat_id.setEnabled(false);
		TextView patname = (TextView)v.findViewById( R.id.patient_name);
		patname.setText( patient.name);
		TextView implant_date= (TextView)v.findViewById( R.id.implant_date);
		implant_date.setText( patient.implant_date);
		TextView birthdate = (TextView)v.findViewById( R.id.birthdate);
		birthdate.setText(patient.birthdate);
		
		TextView address = (TextView)v.findViewById( R.id.address);
		address.setText( patient.address);
		TextView phone = (TextView)v.findViewById( R.id.phone);
		phone.setText( patient.phone);
		
		return v;
    }
    
    Patient getServiceOrderFromUI()
    {
		Patient sd=null;
    	String id = ((TextView)v.findViewById( R.id.patient_id)).getText().toString();
    	String name = ((TextView)v.findViewById( R.id.patient_name)).getText().toString();
    	String birthdate = ((TextView)v.findViewById( R.id.birthdate)).getText().toString();
    	String implant_date = ((TextView)v.findViewById( R.id.implant_date)).getText().toString();
    	String address = ((TextView)v.findViewById( R.id.address)).getText().toString();
    	String phone = ((TextView)v.findViewById( R.id.phone)).getText().toString();
		sd = new Patient(id,name,birthdate,implant_date,address,phone);
		
		return sd;
    }

    void savePatient(Patient p)
    {
    	PatientDbAdapter inDbHelper = PatientDbAdapter.getInstance();
        inDbHelper.open();
        inDbHelper.updateOutEntry(p.id, p.name,p.birthdate,
        	p.implant_date,p.address, p.phone);
        inDbHelper.close();
    }
    
    public static boolean isSafeToDeleteSO(String soid,Context ctx)
    {
    	System.out.println("---->About to deleteSO:"+soid);
    	AdjustmentDbAdapter.createInstance(ctx);
    	AdjustmentDbAdapter laborDbHelper=AdjustmentDbAdapter.getInstance(); 
    	laborDbHelper.open();
    	Cursor c = laborDbHelper.fetchEntryBySOId(soid);
    	laborDbHelper.close();
    	System.out.println("C labors: "+c+" count "+c.getCount());
    	if (c!=null && c.getCount()>0)
    	{
    		System.out.println("Cannot delete this so : it has labors : "+c.getCount());
    		return false;
    	}
        System.out.println("------> Returning true : safe to delete so");
        return true;
    }
    
    
    private void confirmDeletion(final String soid)
    {
        {
      	   AlertDialog.Builder alert = new AlertDialog.Builder(this);
      	   alert.setTitle("Delete SO #"+soid);
     	   alert.setMessage("Are you sure");
     	   alert.setIcon(R.drawable.question);
     	   
     	   alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
     		     public void onClick(DialogInterface dialog, int whichButton) {
     		    	PatientDbAdapter.createInstance(PatientScreen.this);
     		    	PatientDbAdapter soDbHelper=PatientDbAdapter.getInstance();
     		    	soDbHelper.open();
     		    	soDbHelper.deleteEntry(soid);
     		        soDbHelper.close();
     		        Toast.makeText(PatientScreen.this, "Deleted SO Successfully !", Toast.LENGTH_SHORT).show();
     		        finish();

     		     }
     		   });
     	   alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
   		     public void onClick(DialogInterface dialog, int whichButton) {
   		    	 return;
   		     }
   		   });
    	   
     	   alert.show();
         }
    }

    
    private void notifyUserCannotDelete()
    {
        {
      	   AlertDialog.Builder alert = new AlertDialog.Builder(this);

     	   alert.setTitle("Cannot Delete SO.");
     	   alert.setMessage("SO has associated labor/components");
     	   alert.setIcon(R.drawable.bad_icon);
     	   
     	   alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
     		     public void onClick(DialogInterface dialog, int whichButton) {
     		    	 //finish();
     		     }
     		   });
    	   
     	   alert.show();
         }
    }    
    @Override
	public void onConfigurationChanged(Configuration newConfig) {
	  System.out.println("Configuration changed.... so what ? ");
	  super.onConfigurationChanged(newConfig);
	}


}


