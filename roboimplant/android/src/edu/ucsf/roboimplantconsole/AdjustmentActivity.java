package edu.ucsf.roboimplantconsole;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import min3d.core.Object3dContainer;
import min3d.core.RendererActivity;
import min3d.parser.IParser;
import min3d.parser.Parser;
import min3d.vos.Light;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.quietlycoding.android.picker.NumberPicker;

import edu.ucsf.roboimplant.db.DatabaseHelper;
import edu.ucsf.roboimplant.generic.ConfigDB;
import edu.ucsf.roboimplant.generic.Utility;
import edu.ucsf.roboimplantconsole.bluetooth.BlueTerm;
import edu.ucsf.roboimplantconsole.bluetooth.BluetoothSerialService;
import edu.ucsf.roboimplantconsole.bluetooth.DeviceListActivity;

/**
 * Example of adding an OpenGL scene within a conventional Android application layout.
 * Entails overriding RenderActivity's onCreateSetContentView() function, and
 * adding _glSurfaceView to the appropriate View...  
 * 
 * @author Lee
 */
public class AdjustmentActivity extends RendererActivity implements View.OnClickListener,CompoundButton.OnCheckedChangeListener
{
	BlueTerm btmanager=null;
	int currentLength=0;
	Object3dContainer spine = null;
	Object3dContainer rod = null;
	//BluetoothSerialService mSerialService;
    private MenuItem mMenuItemConnect=null;
    private Menu optionsMenu=null;
    
    private static Handler myHandler=null;
    //how fast is the 3d spine rotating
    private static final float ROTATION_SPEED=0.3f;
    private float rotationSpeed = 0;
    float savedRotation = -1;
    
    //options menu items
    private static final int DEVICE_SELECT = 0;
    private static final int ADJUSTMENT_MONITOR = 1;
    private static final int PREFERENCES = 2;
    
    
    //Long press support - keep the state of the button
    //private boolean plusButtonIsDown=false;
    //private boolean minusButtonIsDown=false;
    
    private static boolean isInProcedure=false;
    private static int remainingSpins=0;
    private static int targetSpins=0;
    private static Context myContext=null;

    private static ProgressBar procedureProgressBar=null;
    private static ImageButton couplingIndicator=null;
    private String patid=null;
    private String patname=null;
 
    private static double currentAdjustmentDistance=0.0;
    private GraphicalView mChartView=null; 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
    	super.onCreate(savedInstanceState);
    	myContext=this;
    	
    	Bundle bundle = this.getIntent().getExtras();
    	if (bundle!=null)
    	{
    			patid = bundle.getString(ConfigDB.FIELD_PATIENT_ID,null);
        		patname = bundle.getString(ConfigDB.FIELD_PATIENT_NAME,null);
            	System.out.println("This adjustment is for patient id#"+patid+", named:"+patname);
    	}
    }
    
	@Override
	protected void onCreateSetContentView()
	{
		//setContentView(R.layout.roboimplant_adjustment_console);
		setContentView(R.layout.adjustment_console);
		((Switch)findViewById(R.id.switch_operation_mode)).setOnCheckedChangeListener(this);
        LinearLayout ll = (LinearLayout) this.findViewById(R.id.scene1Holder);
        ll.addView(_glSurfaceView);
        
        Button b;
        b = (Button) this.findViewById(R.id.layoutOkay);
        b.setOnClickListener(this);
        b = (Button) this.findViewById(R.id.layoutCancel);
        b.setOnClickListener(this);
        
        Button bb = (Button)this.findViewById(R.id.adjust_button);
        bb.setOnClickListener(this);
        
        EditText et = (EditText)this.findViewById(R.id.dosage_value);
        et.setOnClickListener(this);
        currentAdjustmentDistance = Double.parseDouble(et.getText().toString());
       
        procedureProgressBar = (ProgressBar)this.findViewById(R.id.ProgressBar01);
        couplingIndicator = (ImageButton)this.findViewById(R.id.couplingindicator);
        
        //Blueterm related initializations
        btmanager = BlueTerm.getInstance();
        btmanager.init(this);
        
        myHandler = initHandler(); 
        btmanager.registerClientHandler(myHandler);
        
        //DB related initializations
        
	}

	@Override
    public void onClick(View v)
    {
		TextView tv=null;
    	String slength="";
    	int clickOriginID = v.getId();
    	//System.out.println("Got click from "+clickOriginID);
    	switch(clickOriginID) 
    	{
    		case R.id.layoutCancel:
    			finish();
    			break;
    		case R.id.dosage_value:
    			System.out.println("-----------------------------");
    			//tv = (TextView)(this.findViewById(R.id.currlength));
    			//currentLength--;
    			btmanager.send("atblink\n\r".getBytes());
    			//slength = ""+currentLength+" um";
    			//tv.setText(slength);
    			//TextView tv = (TextView)this.findViewById(R.id.dosage_value);
    			getDosageDialogBox().show();
    			break;
    			
    		case R.id.adjust_button:
    			if (btmanager.getConnectionState() != BluetoothSerialService.STATE_CONNECTED) 
	            {
    				Toast.makeText(getApplicationContext(), "Connect your console with a RoboImplant first !", Toast.LENGTH_SHORT).show();
    				return;
	            }
            	tv = (TextView)this.findViewById(R.id.dosage_value);
            	System.out.println("tv : "+tv + " text: "+tv.getText().toString());
            	int dosageum = (int)(1000.0*Double.parseDouble(tv.getText().toString()));
            	startProcedure(dosageum);
    			System.out.println("User requested to go into acuiqision: dosage="+dosageum);
    			Toast.makeText(getApplicationContext(), "Acquisition will start as soon as coupling is established !", Toast.LENGTH_SHORT).show();
    	};
    }
    
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    	if (buttonView.getId()==R.id.switch_operation_mode)
		{
	    	if (isChecked)
			{
	    
	    		MessageDispatcher.getInstance().process("btestablishedcoupling");
	    /*		ImageButton b = (ImageButton)(this.findViewById(R.id.plus_button));
				b.setImageResource(R.drawable.plus);
				b = (ImageButton)(this.findViewById(R.id.minus_button));
				b.setImageResource(R.drawable.minus);

				final Drawable drawableTop = getResources().getDrawable(R.drawable.adjust_gray);
				Button b2 = (Button)(this.findViewById(R.id.adjust_button));
				b2.setCompoundDrawablesWithIntrinsicBounds(null, drawableTop , null, null);*/

			}else
			{
				MessageDispatcher.getInstance().process("btlostcoupling");
				/*ImageButton b = (ImageButton)(this.findViewById(R.id.plus_button));
				b.setImageResource(R.drawable.plus_gray);
				b = (ImageButton)(this.findViewById(R.id.minus_button));
				b.setImageResource(R.drawable.minus_gray);
				
				final Drawable drawableTop = getResources().getDrawable(R.drawable.adjust);
				Button b2 = (Button)(this.findViewById(R.id.adjust_button));
				b2.setCompoundDrawablesWithIntrinsicBounds(null, drawableTop , null, null);*/
			}
			return;
		}
    }
    
    //
	
	public void initScene() 
	{
		
		scene.lights().add(new Light());
		
		//scene.backgroundColor().setAll(0xff222222);
		scene.backgroundColor().setAll(0xff251025);
		initObjects();
	}
	
	public void initObjects()
	{
		scene.lights().add(new Light());
	
		String resourceID = "edu.ucsf.roboimplantconsole:raw/spine_lowres_obj";
		//Object ooo = getResources().openRawResource(getResources().getIdentifier(resourceID,null,null));

		IParser parser = Parser.createParser(Parser.Type.OBJ,getResources(), resourceID, true);
		parser.parse();
	
		spine = parser.getParsedObject();
		spine.scale().x = spine.scale().y = spine.scale(		).z = 4f;

		if (savedRotation!=-1)
			spine.rotation().y = savedRotation;
		scene.addChild(spine);
		
		
		resourceID = "edu.ucsf.roboimplantconsole:raw/rod_obj";
		parser = Parser.createParser(Parser.Type.OBJ,getResources(), resourceID, true);
		parser.parse();

		rod = parser.getParsedObject();
		//new HollowCylinder(0.05f,0, 1f, 100);
		//rod.scale().x = rod.scale().y = rod.scale().z = 1.0f;
		rod.position().x -= 0.3;
		rod.position().y += 1.0;
		rod.rotation().z = 20;
		//scene.addChild(rod);
	}


	static boolean notified=false;
	@Override 
	public void updateScene() 
	{
		spine.rotation().y += rotationSpeed;
		//rod.rotation().z += 0.5;
		if (rod.scale().y<1.6)
			rod.scale().y *= 1.001;
		else
		{
			if (!notified)
			{
				notified = true;
		        //Message msg = myHandler.obtainMessage(BlueTerm.MESSAGE_TOAST);
		        //Bundle bundle = new Bundle();
		        //bundle.putString(BlueTerm.TOAST, "Procedure Terminated");
		        //msg.setData(bundle);
		        //myHandler.sendMessage(msg);

				//SoundNotifier.getInstance().playAudio(this, R.raw.shutter);
			}
		}
		//System.out.println("y scale : "+rod.scale().y);
		
		//System.out.println("Rotation = "+_object.rotation().y);
	}
	
    @Override 
    public boolean onCreateOptionsMenu(Menu menu) {
    	
    	System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx    menu xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.adjustment_console_menu, menu);
        mMenuItemConnect = menu.getItem(0);
        //for some reason i could not do it in the xml file - do it in the code
        menu.getItem(ADJUSTMENT_MONITOR).setIcon(R.drawable.ic_menu_monitor);
        optionsMenu = menu;
        return true;
    }

    @Override
    public void onPause()
    {
    	System.out.println("Paaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaausing");
    	
    	if (spine!=null)
    		savedRotation=spine.rotation().y;
    	super.onPause();
    	if (optionsMenu!=null)
    		optionsMenu.setGroupEnabled(0,false);
    }
    
    @Override
    public void onResume()
    {
    	System.out.println("Resuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuming");
    	super.onResume();
    	if (optionsMenu!=null)
    		optionsMenu.setGroupEnabled(0,true);
    
    
    	if (mChartView == null) 
    	{
    	
    	    LinearLayout layout = (LinearLayout) findViewById(R.id.torquechart);
    	    //mChartView = ChartFactory.getLineChartView(this, mDataset,mRenderer);
    	    mChartView = getGrafico();
    	    layout.addView(mChartView, new
    	    		LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
    	    		LinearLayout.LayoutParams.FILL_PARENT)); 
    	    
    	    
    	  } else {
    	    mChartView.repaint();
    	  }
    	} 
    
    

    public GraphicalView getGrafico()
    {
            XYMultipleSeriesDataset data = new XYMultipleSeriesDataset();
            XYMultipleSeriesRenderer renderer = new  XYMultipleSeriesRenderer();
            double [] range = {0,300};
            XYSeries series = new XYSeries("Teste");
            series.add(1, 1);
            //series.add(2, 2);
            data.addSeries(series);

            XYSeriesRenderer r = new XYSeriesRenderer();
            r.setColor(Color.BLUE);
            r.setPointStyle(PointStyle.SQUARE);
            r.setFillBelowLine(false);
            r.setFillBelowLineColor(Color.WHITE);
            r.setFillPoints(true);
            renderer.addSeriesRenderer(r);
            renderer.setAxesColor(Color.DKGRAY);
            renderer.setLabelsColor(Color.LTGRAY);
            
            
            String[] titles = new String[] { "Friction", "Useful" };
            List<double[]> values = new ArrayList<double[]>();
            //values.add(new double[] { 100, 200, 300, 15244, 15900, 19200, 22030, 21200, 19500, 15500 });
            //values.add(new double[] { 5230, 7300, 9240, 10540, 7900, 9200, 12030, 11200, 9500, 10500 });
            values.add(new double[] { 100});
            values.add(new double[] { 150});
            int[] colors = new int[] { Color.RED, Color.GREEN};
            renderer = buildBarRenderer(colors);
            /*setChartSettings(renderer, "Monthly sales in the last 2 years", "Month", "Units sold", 0.5,
                12.5, 0, 24000, Color.GRAY, Color.LTGRAY);*/
            renderer.getSeriesRendererAt(0).setDisplayChartValues(true);
            renderer.getSeriesRendererAt(1).setDisplayChartValues(true);
            renderer.setXLabels(0);
            renderer.setYLabels(4);
            
            renderer.setXLabelsAlign(Align.RIGHT);
            renderer.setYLabelsAlign(Align.RIGHT);
            renderer.setPanEnabled(true, false);
            renderer.setMargins(new int[] {35,145,0,120});
            renderer.setChartTitle("Motor Torque [N/m]");
            renderer.setGridColor(Color.BLACK);
            renderer.setInitialRange(range);
            // renderer.setZoomEnabled(false);
            renderer.setZoomRate(1.0f);
            renderer.clearXTextLabels();
            renderer.setBarSpacing(-1);
            renderer.setXAxisMin(0);
            renderer.setXAxisMax(100);
            //renderer.setYAxisMin(0);
            //renderer.setYAxisMax(300);
            
            renderer.setApplyBackgroundColor(true);
            return ChartFactory.getBarChartView(this, this.buildBarDataset(titles, values), renderer,Type.STACKED);
            //return ChartFactory.getLineChartView(this, data, renderer);
    }
    
    protected XYMultipleSeriesRenderer buildBarRenderer(int[] colors) 
    {
        Log.v("abstract","bbb");
      XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
      renderer.setAxisTitleTextSize(16);
      renderer.setChartTitleTextSize(20);
      renderer.setLabelsTextSize(15);
      renderer.setLegendTextSize(15);
      int length = colors.length;
      for (int i = 0; i < length; i++) {
        SimpleSeriesRenderer r = new SimpleSeriesRenderer();
        r.setColor(colors[i]);
        renderer.addSeriesRenderer(r);
      }
      return renderer;
    }

    
    protected XYMultipleSeriesDataset buildBarDataset(String[] titles, List<double[]> values) {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        int length = titles.length;
        for (int i = 0; i < length; i++) {
          CategorySeries series = new CategorySeries(titles[i]);
          double[] v = values.get(i);
          int seriesLength = v.length;
          for (int k = 0; k < seriesLength; k++) {
            series.add(v[k]);
          }
          dataset.addSeries(series.toXYSeries());
        }
        return dataset;
      }

    
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case R.id.connect:
	          
	          if (btmanager.getConnectionState() == BluetoothSerialService.STATE_NONE) {
	            // Launch the DeviceListActivity to see devices and do scan
	            Intent serverIntent = new Intent(this, DeviceListActivity.class);
	            startActivityForResult(serverIntent, BlueTerm.REQUEST_CONNECT_DEVICE);
	          }
	          else
	              if (btmanager.getConnectionState() == BluetoothSerialService.STATE_CONNECTED) 
	              {
	                btmanager.restart();
	              }
	            return true;
	        case R.id.service_terminal:
	    	{
	    			
	    		Intent monitorIntent = new Intent(this, ServiceTerminal.class);
	    		System.out.println("Starting adjustment monitor.......");
	    		startActivity(monitorIntent);
                if (btmanager.getConnectionState() == BluetoothSerialService.STATE_CONNECTED)
                {
                	System.out.println("turning on updates");
                	btmanager.send("atupdate 1\n\ratblink\n\r".getBytes());
                }

	    		return true;
	    	}
	    	
	        case R.id.reset_db:
                DatabaseHelper.getInstance(this).getWritableDatabase();
                System.out.println("Got writable database, about to reset data.....");
                DatabaseHelper.getInstance(this).resetToInitialPosition("none");

	        	break;
	        case R.id.patient_db:
	        	Intent dbi = new Intent(this, edu.ucsf.roboimplant.menu.PatientListActivity.class);
	    		System.out.println("Starting patient DB.......");
	    		startActivity(dbi);
	        	break;
	        	
	        case R.id.calibration_utility:
	    		Intent monitorIntent = new Intent(this, CalibrationActivity.class);
	    		System.out.println("Starting adjustment monitor.......");
	    		startActivity(monitorIntent);
                if (btmanager.getConnectionState() == BluetoothSerialService.STATE_CONNECTED)
                {
                	System.out.println("turning on updates");
                	btmanager.send("atupdate 1\n\ratblink\n\r".getBytes());
                }
	        	
	        
	    	
	        /*case R.id.preferences:
	            doPreferences();
	            return true;*/
	        }
    	   
        return false;
    }
    
    private void doPreferences() {
    	startActivity(new Intent(this, RoboImplantPreferences.class));
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("Bluetooth", "onActivityResult " + resultCode);
        switch (requestCode) {
        
        case BlueTerm.REQUEST_CONNECT_DEVICE:

            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
            	//Toast.makeText(getApplicationContext(), "Connected ok", Toast.LENGTH_SHORT).show();
            	enableButtons();
            	btmanager.connect(data);
                
            }
            break;

        case BlueTerm.REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                Log.d("Bluetooth", "BT not enabled");
                
                //finishDialogNoBluetooth();                
            }
        }
    }

    
    private void enableButtons()
    { 
    	//moved those manual control buttons away from this screen
        /*ImageButton b;
    	b = (ImageButton) this.findViewById(R.id.plus_button);
        b.setEnabled(true);
        b = (ImageButton) this.findViewById(R.id.minus_button);
        b.setEnabled(true);
        
        Button b2 = (Button) this.findViewById(R.id.adjust_button);
        b2.setOnClickListener(this);
        b2.setEnabled(true);*/
    }

    public AlertDialog getDosageDialogBox()
    {
	    final CharSequence[] items = {"0.5", "1", "2"};
	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    builder.setTitle("Select adjustment distance [mm]");
	    builder.setItems(items, new DialogInterface.OnClickListener(){
	        public void onClick(DialogInterface dialogInterface, int item) {
	            //Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
	        	AdjustmentActivity.currentAdjustmentDistance = Double.valueOf(items[item].toString());
	        	myHandler.sendEmptyMessage(ConfigDB.UPDATE_DOSAGE);
	            return;
	        }
	    });
	    return builder.create();
    }
    
    //allows the user to input any number - not ideal in our case
    
    public AlertDialog getDosageFreeStyleDialogBox()
    {
        final Activity ctx=this;
    	LayoutInflater inflater = (LayoutInflater)
    			
        	    getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			TextView tv = (TextView)ctx.findViewById(R.id.dosage_value);
    			int currentDosage = Integer.parseInt(tv.getText().toString());
	    
    			View npView = inflater.inflate(R.layout.number_picker_pref, null);
        	    final NumberPicker np = (NumberPicker)(npView.findViewById(R.id.pref_num_picker));
        	    np.setRange(0, 3000);
        	    np.setCurrent(currentDosage);
        	    return new AlertDialog.Builder(this)
        	        .setTitle("Select desired adjustment lenght in um:")
        	        .setView(npView)
        	        .setPositiveButton("OK",
        	            new DialogInterface.OnClickListener() {
        	                public void onClick(DialogInterface dialog, int whichButton) {
        	                	System.out.println("User chose : "+np.getCurrent());
        	                	TextView tv = (TextView)ctx.findViewById(R.id.dosage_value);
        	                	int value = np.getCurrent();
        	                	tv.setText(""+value);

        	                }
        	            })
        	            .setNegativeButton("Cancel",
        	                new DialogInterface.OnClickListener() {
        	                    public void onClick(DialogInterface dialog, int whichButton) {
        	                    }
        	                })
        	            .create();

    }
    
    public Menu getOptionsMenu()
    {
    	return optionsMenu;
    }

    
    private Handler initHandler()
    {
           myHandler = new Handler() {
            
            @Override
            public void handleMessage(Message msg) {          
            	//System.out.println("BlueTerm:Message ("+msg.what+") -----------------------------------------------------------------");
            	
            	switch (msg.what) {

            	case ConfigDB.UPDATE_DOSAGE:
                	System.out.println("Updating dosage from handler, max:"+currentAdjustmentDistance);
        			EditText et = (EditText)AdjustmentActivity.this.findViewById(R.id.dosage_value);
        			et.setText(""+currentAdjustmentDistance);
        			et.invalidate();
        			et.refreshDrawableState();
                	break;

            	
            	case ConfigDB.UPDATE_PROGRESS_BAR:
                	System.out.println("Updating progress bar from handler, max:"+targetSpins);
            		procedureProgressBar.setMax(targetSpins);
                	procedureProgressBar.setProgress(targetSpins-remainingSpins);
                	procedureProgressBar.invalidate();
                	break;

            	case ConfigDB.PROCEDURE_TERMINATED:
            	  	isInProcedure=false;
            	  	
            	  	//make the progress bar show a complete acquisition
            	  	procedureProgressBar.setMax(targetSpins);
            	  	procedureProgressBar.setProgress(targetSpins);
                	AdjustmentActivity.this.updateDB();
                	rotationSpeed=0;
              
            		break;
            		
            	case ConfigDB.ESTABLISHED_COUPLING:
            		couplingIndicator.setImageResource(R.drawable.greeniconbig);
            		couplingIndicator.invalidate();
            		break;
            	case ConfigDB.LOST_COUPLING:
            		couplingIndicator.setImageResource(R.drawable.rediconbig);
            		couplingIndicator.invalidate();
            		break;

                case BlueTerm.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                    case BluetoothSerialService.STATE_CONNECTED:
                      System.out.println("Yeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeah !!!!");
                      //rotationSpeed = ROTATION_SPEED;
                      if (mMenuItemConnect != null) 
                      {
                    	System.out.println("Oki doki !!!!");
                        mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
                        mMenuItemConnect.setTitle(R.string.disconnect);
                      }
                      break;
                        
                    case BluetoothSerialService.STATE_CONNECTING:
                        break;
                        
                    case BluetoothSerialService.STATE_LISTEN:
                    case BluetoothSerialService.STATE_NONE:
                      //change this to 0 if you want to rotate only when connected...
                    	rotationSpeed=0;
                      if (mMenuItemConnect != null) {
                        mMenuItemConnect.setIcon(android.R.drawable.ic_menu_search);
                        mMenuItemConnect.setTitle(R.string.connect);
                      }
                      break;
                    }
                    break;
                case BlueTerm.MESSAGE_WRITE:
                    break;
                    
                case BlueTerm.MESSAGE_READ:
                    break;
                    
                case BlueTerm.MESSAGE_DEVICE_NAME:
                    break;
                case BlueTerm.MESSAGE_TOAST:
                	Toast.makeText(getApplicationContext(), msg.getData().getString(BlueTerm.TOAST), Toast.LENGTH_SHORT).show();
                	break;
                    
                }//switch
            }//handle
        };    //class

        return myHandler;
    }//method

    protected void updateDB() {
		// TODO Auto-generated method stub
		
	}

    public void startProcedure(int dosage)
    {
    	
    	//turn on the motor
    	btmanager.send("atmotoron\n\r".getBytes());
    	Utility.delay(100);
    	String dosageCmd = "atdosage "+dosage+"\n\r";
    	btmanager.send(dosageCmd.getBytes());
    	Utility.delay(200);
    	btmanager.send("atspeed 120\n\r".getBytes());
    	Utility.delay(100);
    	
    	//send current time+date for logging purposes
    	SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
    	Date date = new Date();
    	String datecmd = "atsettime "+formatter.format(date)+"\n\r"; 
    	btmanager.send(datecmd.getBytes());
    	Utility.delay(100);
    	

    	//start the acquisition
    	btmanager.send("atstart\n\r".getBytes());
    	
    	//enable periodic updates
    	btmanager.send("atupdate1\n\r".getBytes());
    	Utility.delay(100);

    	
    	isInProcedure=true;
    	rotationSpeed = ROTATION_SPEED;
    }
    
    public static void updateRemainingSpins(int _remainingSpins, int _targetSpins)
    {
    	remainingSpins = _remainingSpins;
    	targetSpins = _targetSpins;
    	myHandler.sendEmptyMessage(ConfigDB.UPDATE_PROGRESS_BAR);
    	//System.out.println("Updating the progress bar...");
    	//procedureProgressBar.setMax(targetSpins);
    	//procedureProgressBar.setProgress(targetSpins-remainingSpins);
    	//procedureProgressBar.invalidate();
    }
    
    
    public static void notifyLostCoupling()
    {
    	Toast.makeText(myContext, "Lost coupling", Toast.LENGTH_SHORT).show();
    	myHandler.sendEmptyMessage(ConfigDB.LOST_COUPLING);
    }
    
    public static void notifyEstablishedCoupling()
    {
    	Toast.makeText(myContext,"Coupling established, ready to go into acquisition", Toast.LENGTH_SHORT).show();
    	myHandler.sendEmptyMessage(ConfigDB.ESTABLISHED_COUPLING);
    }
    
    public static void notifyTermination()
    {
    	Toast.makeText(myContext,"RoboImplant reported end of procedure. Patient's record has been updated.", Toast.LENGTH_SHORT).show();
    	//isInProcedure=false;
    	//Halt the motor - manually now by setting speed.
    	//BlueTerm.getInstance().send("atspeed 98\n\r".getBytes());
    	myHandler.sendEmptyMessage(ConfigDB.PROCEDURE_TERMINATED);
    }
    
}

