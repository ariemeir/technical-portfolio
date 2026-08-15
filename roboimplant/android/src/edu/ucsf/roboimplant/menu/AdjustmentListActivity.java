package edu.ucsf.roboimplant.menu;

import java.util.ArrayList;

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
import android.widget.Toast;
import edu.ucsf.roboimplant.data.Adjustment;
import edu.ucsf.roboimplant.db.AdjustmentDbAdapter;
import edu.ucsf.roboimplant.generic.ConfigDB;
import edu.ucsf.roboimplant.ui.AdjustmentScreen;
import edu.ucsf.roboimplantconsole.R;

public class AdjustmentListActivity extends ListActivity implements OnItemClickListener, OnCreateContextMenuListener 
{
	private final static int REPORT_START_WORK =0;
	private final static int REPORT_STOP_WORK  =1;
	private final static int DELETE_LABOR_REPORT =2;
	private final static int ADD_LABOR_REPORT=5;
	private static AdjustmentListActivity _instance=null;
	AdjustmentListAdapter mlistAdaptor=null;
	AdjustmentDbAdapter adjustmentDbHelper=null;
	String _patient_id=null;
    
	@Override
    public void onCreate(Bundle icicle) {
       super.onCreate(icicle);
       _patient_id = (String) this.getIntent().getExtras().get(ConfigDB.FIELD_PATIENT_ID);
       setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
       setContentView(R.layout.adjustment_list);
       System.out.println("here 1");
       setTitle("Patient "+_patient_id+" : Adjustments");
       System.out.println("here 2");
       ArrayList<Adjustment> data= getData(_patient_id);
       System.out.println("here 3");
       mlistAdaptor = new AdjustmentListAdapter(this,R.layout.adjustment_list_entry,data);
       System.out.println("here 4");
       getListView().setOnCreateContextMenuListener(this);
       System.out.println("here 5");
       getListView().setOnItemClickListener(this);
       System.out.println("here 6");
       configureListHeader();
       System.out.println("here 7");
       setListAdapter(mlistAdaptor);
       System.out.println("here 8");
       getListView().requestFocus();
       System.out.println("here 9");
       
    }
    
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) 
	{
		System.out.println("onCreateContextMenu---------------->");
		super.onCreateContextMenu(menu, v, menuInfo);
		//if (lb.t_start==null)
		menu.add(0, REPORT_START_WORK, 0,"Report Start work");//.setIcon(R.drawable.start);
		//else if (lb.t_start!=null && lb.t_end==null)
		menu.add(0,REPORT_STOP_WORK, 0,	"Report Stop work");//.setIcon(R.drawable.stop);
		
		menu.add(0, DELETE_LABOR_REPORT , 0, "Delete Adjustment Entry").setIcon(R.drawable.delete);
	    
	}

	public static AdjustmentListActivity getInstance()
	{
		if (_instance==null)
			_instance = new AdjustmentListActivity();
		
		return _instance;
	}
	
	/** when press-hold option selected */
	public boolean onContextItemSelected(MenuItem item) {
		System.out.println("Context menu activated !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		AdapterView.AdapterContextMenuInfo info =
			(AdapterView.AdapterContextMenuInfo) item.getMenuInfo(); 
		Adjustment lb = (Adjustment)mlistAdaptor.getItem(info.position-1);
		System.out.println("Lb: "+lb);
		switch(item.getItemId()) {
	    case REPORT_START_WORK:
	        System.out.println("------------->Start work selected : 0");
	        startWork(lb,_patient_id);
	        return true;
	    case REPORT_STOP_WORK:
	        System.out.println("------------->Stop work was selected : 1");
	        reportStopTime(lb);
	        refreshData();
	        return true;
	    case DELETE_LABOR_REPORT:
	        System.out.println("------------->Delete was selected : 2");
	        deleteLabor(lb);
	        refreshData();
	        return true;
	    }

       return super.onContextItemSelected(item);
	}
	
	
    public void onItemClick(AdapterView<?> av, View v,int i,long l)
    {
    	System.out.println("Got click from av="+av+" and v="+v);
    }


    public ArrayList<Adjustment> getData(String soid)
    {
    	AdjustmentDbAdapter.createInstance(this);
        adjustmentDbHelper = AdjustmentDbAdapter.getInstance();
        adjustmentDbHelper.open();

        // Get all of the notes from the database and create the item list
        Cursor c = adjustmentDbHelper.fetchEntryBySOId(soid);
        System.out.println("Number of lines in the cursor : "+c.getCount());
        System.out.println("got all entries");
        startManagingCursor(c);
        System.out.println("started cursor");
        int cols = c.getColumnCount();
        System.out.println("Cols : "+cols);
        for (int i=0;i<cols;i++)
        {
        	System.out.println("Col["+i+"]="+c.getColumnName(i));
        }
        System.out.println("Current position in cursor : "+c.getPosition());
        ArrayList<Adjustment> ret = new ArrayList<Adjustment>();
        while (c.moveToNext())
        {
        	System.out.println("Current position in cursor : "+c.getPosition());
        	Adjustment lbr=null;
        	try {
        		System.out.println("Creating new adjustment");
	        	lbr = new Adjustment(  c.getString(0),
		        						c.getString(1),
		        						c.getString(2),
		        						c.getString(3));
	        }
	        catch(Exception e)
	        {
	        	e.printStackTrace();
	        }
	        ret.add(lbr);
        }
        adjustmentDbHelper.close();
        return ret;
    }

    @Override
    public void onResume()
    {
        refreshData();
        super.onResume();
    }
    
    void configureListHeader()
    {
        View header = View.inflate(this, R.layout.adjustment_list_header, null);
        getListView().addHeaderView(header,null,false);
    }
    
    
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)   
    {
    	System.out.println("Populating menu++++++++++++++++++++++++++++++++++++++++++++"); 
		menu.add(0, ADD_LABOR_REPORT, 0, "Add Adjustment Report").setIcon(R.drawable.add);
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) 
    {
    	System.out.println("1234 Selected : "+item.getItemId());
         switch(item.getItemId()){
              case ADD_LABOR_REPORT:
    	        System.out.println("------------->Add was selected : 5");
    	        Intent it = new Intent(this,AdjustmentScreen.class);
    	        it.putExtra(ConfigDB.FIELD_PATIENT_ID, _patient_id);
    	        startActivity(it);
    	        return true;
         }
         return super.onMenuItemSelected(featureId, item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        System.out.println("Request code : "+requestCode + " result code "+resultCode);
        if (resultCode == RESULT_CANCELED) {
        }
        
        refreshData();
        System.out.println("Done refreshing data");
    }
    
    private void refreshData()
    {
    	ArrayList<Adjustment> data= getData(_patient_id);
    	mlistAdaptor = new AdjustmentListAdapter(this,R.layout.adjustment_list_entry,data);
        setListAdapter(mlistAdaptor);
    }

    private void notifyUserCannotDelete()
    {
        {
      	   AlertDialog.Builder alert = new AlertDialog.Builder(this);

     	   alert.setTitle("Cannot Delete Adjustment Report");
     	   alert.setMessage("Start time has been reported");
     	   alert.setIcon(R.drawable.bad_icon);
     	   
     	   alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
     		     public void onClick(DialogInterface dialog, int whichButton) {
     		    	 //finish();
     		     }
     		   });
    	   
     	   alert.show();
         }
    }

    private void startWork(Adjustment lb, String soid)
    {
    }

    private void deleteLabor(Adjustment lb)
    {
        /*if (lb.t_start!=null)
        {
        	notifyUserCannotDelete();
        }
        else
        {*/
        	adjustmentDbHelper.open();
        	adjustmentDbHelper.deleteOutEntry(lb.adjustment_id);
        	adjustmentDbHelper.close();
        	Toast.makeText(this, "Deleted Adjustment Report Successfully !", Toast.LENGTH_SHORT).show();
       // }
    }
    
    @Override
	public void onConfigurationChanged(Configuration newConfig) {
	  System.out.println("Configuration changed.... so what ? ");
	  super.onConfigurationChanged(newConfig);
	}
    
    private void reportStopTime(Adjustment lb)
    {
    }
    
    private void cannotUpdate(String msg)
    {
   	   AlertDialog.Builder alert = new AlertDialog.Builder(this);

 	   alert.setTitle("Adjustment Report.").setMessage(msg).setIcon(R.drawable.bad_icon).
 	   		setPositiveButton("OK",null).show();
    }
}
