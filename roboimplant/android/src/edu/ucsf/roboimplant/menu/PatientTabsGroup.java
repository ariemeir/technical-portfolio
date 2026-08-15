package edu.ucsf.roboimplant.menu;

import android.app.TabActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import edu.ucsf.roboimplant.generic.ConfigDB;
import edu.ucsf.roboimplant.ui.PatientScreen;
import edu.ucsf.roboimplant.ui.PatientSummary;
import edu.ucsf.roboimplantconsole.R;


public class PatientTabsGroup extends TabActivity {

	
	public static final String ADJUSTMENTS="";
	public static final String SUMMARY="";
	public static final String EDIT_INFO="";
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String patient_id = (String) this.getIntent().getExtras().get(ConfigDB.FIELD_PATIENT_ID);
        String patient_name = (String) this.getIntent().getExtras().get(ConfigDB.FIELD_PATIENT_NAME);
        System.out.println("onCreate:soid = "+patient_id);
        setTitle("Patiend ID "+patient_id+" :"+patient_name);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        final TabHost tabHost = getTabHost();
       
        Intent tabAdjustments = new Intent(this, AdjustmentListActivity.class);
        tabAdjustments.putExtra(ConfigDB.FIELD_PATIENT_ID,patient_id);
        TabSpec t = tabHost.newTabSpec(ADJUSTMENTS);

        View indicator1 = getLayoutInflater().inflate(R.layout.tabindicator, null);
        TextView title1 = (TextView)indicator1.findViewById(R.id.tab_title);
        title1.setText("Adjustments");

        t.setIndicator(indicator1);
        t.setContent(tabAdjustments);


        
        tabHost.addTab(t);

        /*Intent tabSummary = new Intent(this, PatientSummary.class);
        tabSummary.putExtra(ConfigDB.FIELD_PATIENT_ID,patient_id);
        Drawable summaryIcon = getResources().getDrawable(R.drawable.summary);
        View indicator2 = getLayoutInflater().inflate(R.layout.tabindicator, null);
        TextView title2 = (TextView)indicator2.findViewById(R.id.tab_title);
        title2.setText("Summary");

        tabHost.addTab(tabHost.newTabSpec(SUMMARY)
                .setIndicator(indicator2)
                .setContent(tabSummary));*/

        Intent tabDetails = new Intent(this, PatientScreen.class);
        tabDetails.putExtra(ConfigDB.FIELD_PATIENT_ID,patient_id);
        Drawable detailsIcon = getResources().getDrawable(R.drawable.details);
        View indicator3 = getLayoutInflater().inflate(R.layout.tabindicator, null);
        TextView title3 = (TextView)indicator3.findViewById(R.id.tab_title);
        title3.setText("Patient Details");

        tabHost.addTab(tabHost.newTabSpec(EDIT_INFO)
                .setIndicator(indicator3)
                .setContent(tabDetails));

        for (int i=0;i<2;i++)
        	tabHost.getTabWidget().getChildAt(i).setBackgroundColor(Color.BLACK);
        //selected gets a special color
        tabHost.getTabWidget().getChildAt(0).setBackgroundColor(Color.parseColor(ConfigDB.COLOR_DEEP_PURPLE));
        
        tabHost.setOnTabChangedListener(new OnTabChangeListener() {

        	@Override
        	public void onTabChanged(String tabId) {

        	int selected = getTabHost().getCurrentTab();
        	System.out.print("@@@@@@@@ CLICK TAB NUMBER------" + selected);
            //for some reason tabHost.getChildCount returns 1.... look into it later.
        	for (int i=0;i<2;i++)
            	tabHost.getTabWidget().getChildAt(i).setBackgroundColor(Color.BLACK);
        	tabHost.getTabWidget().getChildAt(selected).setBackgroundColor(Color.parseColor(ConfigDB.COLOR_DEEP_PURPLE));

        	}});
    }
}
