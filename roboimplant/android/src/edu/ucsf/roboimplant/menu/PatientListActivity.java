package edu.ucsf.roboimplant.menu;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import edu.ucsf.roboimplant.data.Patient;
import edu.ucsf.roboimplant.db.PatientDbAdapter;
import edu.ucsf.roboimplant.generic.ConfigDB;
import edu.ucsf.roboimplant.ui.PatientScreen;
import edu.ucsf.roboimplantconsole.R;

public class PatientListActivity extends ListActivity implements OnItemClickListener, OnCreateContextMenuListener 
{
	private final static int ADJUST_IMPLANT = 10;
	private final static int EDIT_PATIENT_INFO = 11;
	private final static int DELETE_PATIENT = 12;
	PatientListAdapter mlistAdaptor=null;
	PatientDbAdapter patDBHelper=null;
    
	@Override
    public void onCreate(Bundle icicle) {
       super.onCreate(icicle);
       setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
       setContentView(R.layout.patient_list);
       ArrayList<Patient> data= getData();
       mlistAdaptor = new PatientListAdapter(this,R.layout.patient_list_entry,data);
       getListView().setOnCreateContextMenuListener(this);
       getListView().setOnItemClickListener(this);
       configureListHeader();
       setListAdapter(mlistAdaptor);
       getListView().requestFocus();
       
    }
    
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) 
	{
		System.out.println("onCreateContextMenu---------------->");
		super.onCreateContextMenu(menu, v, menuInfo);
	    menu.add(0, ADJUST_IMPLANT, 0, "Adjust Implant");
	    menu.add(0, EDIT_PATIENT_INFO, 0, "Edit Patient Info");
	    menu.add(0, DELETE_PATIENT, 0, "Delete Patient");
	    
	}

	/** when press-hold option selected */
	@TargetApi(16)
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info =
			(AdapterView.AdapterContextMenuInfo) item.getMenuInfo(); 
		Patient patient = (Patient)mlistAdaptor.getItem(info.position-1);
        patDBHelper.open();
        patDBHelper.close();
        
		switch(item.getItemId()) {
	    case DELETE_PATIENT:
	        System.out.println("Delete was selected : 0");
	        if (PatientScreen.isSafeToDeleteSO(patient.id,this))
	        	confirmDeletion(patient.id);
	        else
	        	this.notifyUserCannotDelete();
			return true;
	    case ADJUST_IMPLANT:
	    	System.out.println("Report complete was selected : 1");
	    	Intent it = new Intent(this,edu.ucsf.roboimplantconsole.AdjustmentActivity.class);
	    	Bundle options = new Bundle();
	    	options.putString(ConfigDB.FIELD_PATIENT_ID, patient.id);
	    	options.putString(ConfigDB.FIELD_PATIENT_NAME, patient.name);
	    	startActivityForResult(it, ADJUST_IMPLANT, options);

	    	refreshData();
	        return true;
	    case EDIT_PATIENT_INFO:
	    	System.out.println("Report suspended was selected : 1");
	    	/*patDBHelper.updateOutEntry(so.patient_id, Patient.STATE_SUSPENDED, so.priority, so.current_extension,
	        		so.contract, so.customer, so.site, so.address, so.contact, so.details, 
	        		so.product, so.patient_name, so.implant_date,so.notes);
	        patDBHelper.close();*/
	        refreshData();
	        return true;
	    }
	    return super.onContextItemSelected(item);
	}
	
	@Override
	public void onActivityResult(int requestCode,int resultCode,Intent data)
	{
		switch(requestCode)
		{
		case ADJUST_IMPLANT:
			updateDB();
			System.out.println("Got back from the adjustment sub-activity");
			break;
		}
	}
	
	   //this method updates the patient database after the procedure is done,
    //i.e. creates a new adjustment and links it to the patient
    public void updateDB()
    {
    	String adj_id = null;
    	String pat_id = null;
    	String adj_date = null;
    	String adj_amount = null;
        //need to extract those from the data we got from the sub-activity.
    	//need to make sure those unique keys are generated properly - i don't want to worry about them all the time
    	edu.ucsf.roboimplant.db.AdjustmentDbAdapter.getInstance().createEntry(adj_id, pat_id, adj_date, adj_amount);
    }
 
	
	@Override
	public void onResume()
	{
		System.out.println("Resuming !!!!");
		super.onResume();
		System.out.println("About to refresh data...");
		refreshData();
	}

	@Override
	public void onPause()
	{
		super.onPause();
	}
	
    public void onItemClick(AdapterView<?> av, View v,int i,long l)
    {
    	System.out.println("Got click from av="+av+" and v="+v);
    	Patient patient = (Patient)av.getItemAtPosition(i);
    	System.out.println("Patient :"+patient);
    	Intent it = new Intent(this,edu.ucsf.roboimplant.menu.PatientTabsGroup.class);
    	it.putExtra(ConfigDB.FIELD_PATIENT_ID, patient.id);
    	it.putExtra(ConfigDB.FIELD_PATIENT_NAME, patient.name);
    	startActivity(it);
    }


    private ArrayList<Patient> getData()
    {
    	PatientDbAdapter.createInstance(this);
        patDBHelper = PatientDbAdapter.getInstance();
        patDBHelper.open();

        System.out.println("fetChallInEntries...");
        // Get all of the notes from the database and create the item list
        Cursor c = patDBHelper.fetchAllInEntries();
        System.out.println("Number of lines in the cursor : "+c.getCount());
        System.out.println("got all entries");
        //this line is deprecated and leads to crashes .....
        //startManagingCursor(c);
        System.out.println("started cursor");
        int cols = c.getColumnCount();
        System.out.println("Cols : "+cols);
        for (int i=0;i<cols;i++)
        {
        	System.out.println("Col["+i+"]="+c.getColumnName(i));
        }
        System.out.println("Current position in cursor : "+c.getPosition());
        ArrayList<Patient> ret = new ArrayList<Patient>();
        while (c.moveToNext())
        {
        	System.out.println("Current position in cursor : "+c.getPosition());
        	Patient sd=null;
        	try {
	        	sd = new Patient(  c.getString(0),
		        						c.getString(1),
		        						c.getString(2),
		        						c.getString(3),
		        						c.getString(4),
		        						c.getString(5));
	        }
	        catch(Exception e)
	        {
	        	e.printStackTrace();
	        }
	        
	        if (sd.appearsOnActiveList())
	        	ret.add(sd);
        }
        patDBHelper.close();
        return ret;
    }

    void configureListHeader()
    {
        View header = View.inflate(this, R.layout.patient_list_header, null); 
        getListView().addHeaderView(header,null,false);
    }
    
    
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// Toggle the checkbox state!
		System.out.println("onListItemClick in solistactiv.");
    	if ( v != null )
		{
		}
		
		super.onListItemClick(l, v, position, id);
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)   
    {
    	System.out.println("Populating menu++++++++++++++++++++++++++++++++++++++++++++"); 
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) 
    {
    	System.out.println("Selected : "+item.getItemId());
         switch(item.getItemId()){
              case 0:
            	  return true;
         }
         return super.onMenuItemSelected(featureId, item);
    }

    private void refreshData()
    {
    	System.out.println("About to refresh data");
    	
    	ArrayList<Patient> data= getData();
    	mlistAdaptor = new PatientListAdapter(this,R.layout.patient_list_entry,data);
        setListAdapter(mlistAdaptor);
    }

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	  System.out.println("Configuration changed.... so what ? ");
	  super.onConfigurationChanged(newConfig);
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

	 
	 
	    private void confirmDeletion(final String soid)
	    {
	        {
	      	   AlertDialog.Builder alert = new AlertDialog.Builder(this);
	      	   alert.setTitle("Delete SO #"+soid);
	     	   alert.setMessage("Are you sure");
	     	   alert.setIcon(R.drawable.question);
	     	   
	     	   alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	     		     public void onClick(DialogInterface dialog, int whichButton) {
	     		    	PatientDbAdapter.createInstance(PatientListActivity.this);
	     		    	PatientDbAdapter soDbHelper=PatientDbAdapter.getInstance();
	     		    	soDbHelper.open();
	     		    	soDbHelper.deleteEntry(soid);
	     		        soDbHelper.close();
	     		        refreshData();
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

}
