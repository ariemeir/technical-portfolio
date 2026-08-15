package edu.ucsf.roboimplant.menu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;
import edu.ucsf.roboimplant.db.DBSimulator;
import edu.ucsf.roboimplant.db.DatabaseHelper;

public class MsaMainMenu extends Activity {
    private static final int RESET_DB = 0;
    private static final int LOAD_DB = 1;
    private static final int ABOUT_DIALOG = 2;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        /*super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); 
        ConfigDB.init(getApplicationContext());
        setContentView(R.layout.main_menu);
        addSynchorinizationButtonListener();
        addHandleSOListener();
        addSettingsButtonListener();
    
        startServices();
        //Intent i = new Intent(this,edu.ucsf.roboimplant.ui.ListSample.class);
        //startActivity(i);*/
    }

    private void addSynchorinizationButtonListener()
    {
        /*Button b = (Button)findViewById(R.id.syncbtn);
        b.setOnClickListener( new Button.OnClickListener() {
            public void onClick(View v) {
            	startSyncInfoMenu();
            }
        });*/

    }
    
    private void startSyncInfoMenu()
    {
   		//Intent i = new Intent(this,PatientListActivity.class); 
		//startActivity(i);
    }


    private void addHandleSOListener()
    {
        /*Button b = (Button)findViewById(R.id.sobtn);
        b.setOnClickListener( new Button.OnClickListener() {
            public void onClick(View v) {
            	startSoList();
            }
        });
        b.requestFocus();
        */

    }
    
    private void startSoList()
    {
   		Intent i = new Intent(this,PatientListActivity.class); 
		startActivity(i);
    }

    private void addSettingsButtonListener()
    {
        /*Button b = (Button)findViewById(R.id.settingsbtn);
        b.setOnClickListener( new Button.OnClickListener() {
            public void onClick(View v) {
            	startSettingsMenu();
            }
        });*/

    }
    
    private void startSettingsMenu()
    {
   		//Intent i = new Intent(this,ResultsExpandableListMenu.class); 
		//startActivity(i);
    }

    private void startServices()
    {
    	
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)   
    {
    	/*System.out.println("Populating menu++++++++++++++++++++++++++++++++++++++++++++"); 
    	menu.add(0,RESET_DB,0,"Reset DB").setIcon(R.drawable.reset);
    	menu.add(0,LOAD_DB,0,"Load DB").setIcon(R.drawable.load);
    	menu.add(0,ABOUT_DIALOG,0,"About").setIcon(R.drawable.icon3small);*/
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) 
    {
    	CheckBox cb=null; 
    	System.out.println("Selected : "+item.getItemId());
         switch(item.getItemId()){
              case RESET_DB:
                  DatabaseHelper.getInstance(this).getWritableDatabase();
                  System.out.println("---->33333333333333333333");
                  DatabaseHelper.getInstance(this).resetToInitialPosition("none");
            	  return true;
              case LOAD_DB:
				  showLoadDialog();
                  return true;
              case ABOUT_DIALOG:
            	  showAboutDialog();
           		  return true;
         }
         return super.onMenuItemSelected(featureId, item);
    }



	private void showLoadDialog()
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Load data");
		alert.setMessage("Choose data set");

		// Set an EditText view to get user input 
		final Spinner spinner = getDataLoadingSpinner();
		if (spinner==null)
		{
			alert.setMessage("No data files were found !!");
			alert.setPositiveButton("Go back", new DialogInterface.OnClickListener() {
				  public void onClick(DialogInterface dialog, int whichButton) {
					return;
				  }
			});
		}
		else //spinner ok == data files exist
		{
			alert.setView(spinner);
		
			alert.setPositiveButton("Load", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			  String value = spinner.getSelectedItem().toString();
			  System.out.println("Processing "+value+"_so.csv");
			  boolean ret = DBSimulator.getInstance(MsaMainMenu.this).process(value);
			  if (ret)
				  Toast.makeText(MsaMainMenu.this, "Loaded <"+value+"> data successfully !", Toast.LENGTH_SHORT).show();
			  else
				  Toast.makeText(MsaMainMenu.this, "Could not load data !", Toast.LENGTH_LONG).show();
			  }
			});
	
			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton) {
				return;
			  }
			});
		}
		
		alert.show();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	  System.out.println("Configuration changed.... so what ? ");
	  super.onConfigurationChanged(newConfig);
	}

	private Spinner getDataLoadingSpinner()
	{
		Spinner spinner = new Spinner(this);
		String[] data = DBSimulator.getAvailableData();
		if (data==null)
			return null;
		
		ArrayAdapter spinnerArrayAdapter = new ArrayAdapter(this,
		        android.R.layout.simple_spinner_dropdown_item,data);
		    spinner.setAdapter(spinnerArrayAdapter);
		    
		return spinner;
	
	}
	
	void showAboutDialog()
	{
		/*AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("About MoServ");
		alert.setView(View.inflate(this, R.layout.about, null));
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton) {
				return;
			  }
			});
		alert.show();*/
	}
}