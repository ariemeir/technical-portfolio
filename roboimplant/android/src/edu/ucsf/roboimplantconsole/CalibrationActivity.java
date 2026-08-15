package edu.ucsf.roboimplantconsole;

import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import edu.ucsf.roboimplant.calib.ExtensionDistanceCalibration;
import edu.ucsf.roboimplantconsole.bluetooth.BlueTerm;
import edu.ucsf.roboimplantconsole.bluetooth.BluetoothSerialService;
public class CalibrationActivity extends Activity implements View.OnClickListener{
	
	private static CalibrationActivity _instance=null;
	//How many points back do we keep
	static final int HISTORY_SIZE = 50;
	
	static final int NUM_POINTS = 10;
	static final int UPDATE_PERIOD=1000; //in msec
	static final double f = 0.2; //in hertz, the signal frequency we chart

	// For all measured parameters we maintain a circular buffer which is periodically plotted
	static int voltageBufferPosition=0;
	static float[] voltageTimes = new float[HISTORY_SIZE];
	static float[] voltageBuffer = new float[HISTORY_SIZE];
	static boolean voltageBufferWraparound=false;
	
	static int currentBufferPosition=0;
	static float[] currentTimes = new float[HISTORY_SIZE];
	static float[] currentBuffer = new float[HISTORY_SIZE];
	static boolean currentBufferWraparound=false;
	
	static long startTime = 0;
	GraphView voltageChart=null;
	GraphView currentChart=null;

	//Long press support - keep the state of the button
    private boolean plusButtonIsDown=false;
    private boolean minusButtonIsDown=false;
    
    Handler myButtonHandler=null; 
    static Random rnd = new Random();
	

	
	static double angularSpeed=-1;
			
	
	static{
		startTime = System.currentTimeMillis();
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
		final Activity act = this;
		super.onCreate(savedInstanceState);
        setContentView(R.layout.calibration_layout);
        drawChart();
        setButtonHandlers();
                
        
        //This handler is registed in the GUI thread and is the place where this activity ui can be changed.
        final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                //System.out.println("Got a message with code:"+msg.what);
                updateCurrentChart();
                updateVoltageChart();

                TextView tv = (TextView)(act.findViewById(R.id.current_value));
                if (currentBufferPosition >0 || (currentBufferPosition==0 && currentBufferWraparound))
                	tv.setText(""+currentBuffer[currentBufferPosition>0?currentBufferPosition-1:0]);
                else
                	tv.setText("--");
                 
                tv = (TextView)(act.findViewById(R.id.voltage_value));
                if (voltageBufferPosition >0 || (voltageBufferPosition==0 && voltageBufferWraparound))
                	tv.setText(""+voltageBuffer[voltageBufferPosition>0?voltageBufferPosition-1:0]);
                else
                	tv.setText("--");
                
                tv = (TextView)(act.findViewById(R.id.angular_speed_value));
               	if (angularSpeed==-1)
               		tv.setText("--");
           		else
               		tv.setText(""+angularSpeed);

            }
        };
        
        Runnable r = new Runnable() {
        	public void run()
        	{
        		while (true)
        		{
        			try {Thread.sleep(UPDATE_PERIOD); }
        			catch(InterruptedException e) 
        			{
        				System.out.println("Got an io exception");
        				e.printStackTrace();
        			}
        			//send a message will cause the ServiceTerminal handler to refresh it's ui controls
        			mHandler.sendEmptyMessage(777);
        		}
        	};
        };
        new Thread(r).start();
    }
    
	
	

    public void drawChart()
    {
		// init example series data
		
    	
    	//updateVoltageData();
    	GraphViewData[] data = getCurrentData();
    	if (data==null) //no data yet
    	{
    		//make a dummy time point
    		data = new GraphViewData[1];
    		data[0] = new GraphViewData(0,0); 
    	}
    	
    	GraphViewSeries exampleSeries = new GraphViewSeries(data);

		// graph with dynamically genereated horizontal and vertical labels
		currentChart = new LineGraphView(this, "Current"	);
		System.out.println("Adding series to current chart");
		currentChart.addSeries(exampleSeries); // data
		System.out.println("CurrentChart:"+currentChart);

		((LineGraphView)currentChart).setDrawBackground(true);
		currentChart.setHorizontalLabels(null);
		currentChart.setVerticalLabels(null);
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.currentChart);
		layout.addView(currentChart);

    	data = getVoltageData();
    	if (data==null) //no data yet
    	{
    		//make a dummy time point
    		data = new GraphViewData[1];
    		data[0] = new GraphViewData(0,0);
    	}
		
		voltageChart = new LineGraphView(this, "Voltage");
		exampleSeries = new GraphViewSeries(data);
		//System.out.println("Adding series to voltage chart");
		voltageChart.addSeries(exampleSeries); // data
		
		((LineGraphView)voltageChart).setDrawBackground(true);
		voltageChart.setHorizontalLabels(null);
		voltageChart.setVerticalLabels(null);

		layout = (LinearLayout) findViewById(R.id.voltageChart);
		layout.addView(voltageChart);
		//System.out.println("voltageChart:"+voltageChart);
     }

     public void updateVoltageChart()
     {
    	 
    	 GraphViewData[] data = getVoltageData();
    	 if (data==null) //no data yet
         {
    		 //System.out.println("Returning emptyhanded");
    		 return;
         
         }
    	 
    	 //System.out.println("About to update the view::::");
    	 LinearLayout layout = (LinearLayout) findViewById(R.id.voltageChart);
    	 
    	 //if the chart is empty, this will throw an exception. 
    	 try { 
    		 voltageChart.removeSeries(0);
    		 
    	 }
    	 catch(IndexOutOfBoundsException e)
    	 {
    		 System.out.println("indexoutofbounds exception : No voltage data has arrived yet");
    		 return;
    	 }
    	 //System.out.println("Remove voltage series succeeded");
    	 layout.removeView(voltageChart);
    	 
    	 //System.out.println("Voltage data contains n points:"+data.length);
    	 GraphViewSeries series = new GraphViewSeries(data);
    	 voltageChart.addSeries(series);
 		 voltageChart.setHorizontalLabels(null);
 		 voltageChart.setVerticalLabels(null);

    	 layout.addView(voltageChart);
    	 voltageChart.invalidate();
    	 
     }


     public static void updateVoltageData(float value)
     {
     	
    	synchronized(voltageTimes)
    	{
	     	float tsec = (float)(System.currentTimeMillis()-startTime);
	     	
			voltageTimes[voltageBufferPosition] = tsec;
			//there is an apparent bug in GraphView - if the input value is constant, it shows no series. I add some minor gaussian noise
			//just for vizualization, it affects no data.
			voltageBuffer[voltageBufferPosition]= value + (float)rnd.nextGaussian()/1000000;
	 		//System.out.println("S#"+voltageBufferPosition+":[t,V(t)]="+"["+tsec+","+value+"]");
	 		if (voltageBufferPosition<HISTORY_SIZE-1)
	 			voltageBufferPosition++;
	 		else
	 		{
	 			voltageBufferPosition=0;
	 			voltageBufferWraparound=true;
	 		}
    	}
     }

     public static void updateCurrentData(float value)
     {
     	
     	float tsec = (float)(System.currentTimeMillis()-startTime);
     	
		currentTimes[currentBufferPosition] = tsec;
		currentBuffer[currentBufferPosition]= value;
 		//System.out.println("S#"+currentBufferPosition+":[t,V(t)]="+"["+tsec+","+value+"]");
 		if (currentBufferPosition<HISTORY_SIZE-1)
 			currentBufferPosition++;
 		else
 		{
 			currentBufferPosition=0;
 			currentBufferWraparound=true;
 		}
     }


     public GraphViewData[] getVoltageData()
     {
    	 GraphViewData[] data=null;
    	 synchronized(voltageTimes)
    	 {
	    	 int npoints=0;
	    	 if (!voltageBufferWraparound)
	    	 {
	    		 npoints = voltageBufferPosition; // no overrun, less than a full buffer
	    		 if (npoints==0)
	    			 return null;
	    		 data = new GraphViewData[npoints];
	    		 for (int i=0;i<npoints;i++)
	    			 data[i] = new GraphViewData(voltageTimes[i],voltageBuffer[i]);
	    	 }else
	    	 {
	    		 npoints=HISTORY_SIZE;
	    		 data = new GraphViewData[npoints];
	    		 int j=0;
	    		 // there was a wrap around, so collect the older part first and the newer after that...
	    		 for (int i=voltageBufferPosition;i<HISTORY_SIZE;i++,j++)
	    			 data[j] = new GraphViewData(voltageTimes[i],voltageBuffer[i]);
	    		 
	    		 for (int i=0;i<voltageBufferPosition;i++,j++)
	    			 data[j] = new GraphViewData(voltageTimes[i],voltageBuffer[i]);
	    	 }
    	 }
    	 return data;
     }
     
     public void updateCurrentChart()
     {
    	 GraphViewData[] data = getCurrentData();
     	if (data==null) //no data yet
     	{
     		//System.out.println("Current - returning emptyhanded");
     		return;
     		//make a dummy time point
     		//data = new GraphViewData[1];
     		//data[0] = new GraphViewData(0,0);
     		//System.out.println("No current data has arrived yet to this point");
     	}
    	 
    	 
    	 //System.out.println("About to update the current view chart::::");
    	 LinearLayout layout = (LinearLayout) findViewById(R.id.currentChart);
    	 try { 
    		 currentChart.removeSeries(0);
    		 }
    	 catch(IndexOutOfBoundsException e)
    	 {
    		 System.out.println("current: indexoutofboundsexception : No current data is there weird");
    		 //return;
    	 }
    	 //System.out.println("Removing current series succeeds");
    	 layout.removeView(currentChart);

    	 
    	 GraphViewSeries series = new GraphViewSeries(data);
    	 currentChart.addSeries(series);
    	 currentChart.setHorizontalLabels(null);
    	 currentChart.setVerticalLabels(null);

    	 layout.addView(currentChart);
    	 currentChart.invalidate();
     }

     
     public GraphViewData[] getCurrentData()
     {
    	 GraphViewData[] data=null;
    	 int npoints=0;
    	 if (!currentBufferWraparound)
    	 {
    		 npoints = currentBufferPosition; // no overrun, less than a full buffer
    		 if (npoints==0)
    			 return null;

    		 data = new GraphViewData[npoints];
    		 for (int i=0;i<npoints;i++)
    			 data[i] = new GraphViewData(currentTimes[i],currentBuffer[i]);
    	 }else
    	 {
    		 npoints=HISTORY_SIZE;
    		 data = new GraphViewData[npoints];
    		 int j=0;
    		 // there was a wrap around, so collect the older part first and the newer after that...
    		 for (int i=currentBufferPosition;i<HISTORY_SIZE;i++,j++)
    			 data[j] = new GraphViewData(currentTimes[i],currentBuffer[i]);
    		 
    		 for (int i=0;i<currentBufferPosition;i++,j++)
    			 data[j] = new GraphViewData(currentTimes[i],currentBuffer[i]);
    	 }
    	 return data;
     }

     public static void updateAngularSpeed(double _angularSpeed)
     {
    	 //System.out.println("Updating angular speed");
    	 angularSpeed = _angularSpeed;
     }
     
     
     public void setButtonHandlers()
     {
         myButtonHandler = new Handler() {
             
             @Override
             public void handleMessage(Message msg) {          
             	System.out.println("Button handler : Message "+msg.what);
             	
             }//handl
         };    //class

    	 System.out.println("setting the listener for the plus button");
         ImageButton bb = (ImageButton) this.findViewById(R.id.plus_button);
         bb.setOnClickListener(this);
         bb.setEnabled(true);
         bb.setOnTouchListener(new OnTouchListener() {            
             @Override
             public boolean onTouch(View v, MotionEvent event) {
                 System.out.println("Got event"+event.getAction());
             	if (event.getAction()==MotionEvent.ACTION_DOWN)
             	{
             		plusButtonIsDown = true;
                 	myButtonHandler.post(mPlusButtonRunnable);
             	}
             	else if (event.getAction()==MotionEvent.ACTION_UP)
                 	{
                 		plusButtonIsDown = false;
                 	}
             		
                 return true;
             }
         });

         
         
         bb = (ImageButton) this.findViewById(R.id.minus_button);
         bb.setOnClickListener(this);
         bb.setEnabled(true);
         bb.setOnTouchListener(new OnTouchListener() {            
             @Override
             public boolean onTouch(View v, MotionEvent event) {
                 System.out.println("Got event"+event.getAction());
             	if (event.getAction()==MotionEvent.ACTION_DOWN)
             	{
             		minusButtonIsDown = true;
                 	myButtonHandler.post(mMinusButtonRunnable);
             	}
             	else if (event.getAction()==MotionEvent.ACTION_UP)
                 	{
                 		minusButtonIsDown = false;
                 	}
             		
                 return true;
             }
         });
         
         
         Button calibButton = (Button)this.findViewById(R.id.calibrate_extension_distance_button);
         calibButton.setOnClickListener( new OnClickListener() {
        	 public void onClick(View v)
        	 {
        		 System.out.println("Calibration 1 should be invoked");
 	            Intent calibIntent = new Intent(CalibrationActivity.this, ExtensionDistanceCalibration.class);
 	            startActivity(calibIntent);

        	 }
         });

     }
     
 	@Override
    public void onClick(View v)
    {
    	String slength="";
    	int clickOriginID = v.getId();
    	//System.out.println("Got click from "+clickOriginID);
    	switch(clickOriginID) 
    	{
    		case R.id.layoutCancel:
    			finish();
    			break;
    		case R.id.plus_button:
    			System.out.println("+++++++++++++++++++++++++++++");
    			BlueTerm.getInstance().send("atinc 10\n\r".getBytes());
    			BlueTerm.getInstance().send("atblink\n\r".getBytes());
    			break;
    		case R.id.minus_button:
    			System.out.println("-----------------------------");
    			//combine two commands: atdec and atblinkmeasureVoltage(&phyParams.voltage);
    			BlueTerm.getInstance().send("atdec 10\n\r".getBytes());
    			BlueTerm.getInstance().send("atblink\n\r".getBytes());
    			break;
    			
    	};
    }

 	
    private final Runnable mPlusButtonRunnable = new Runnable() {
        public void run() {
            if (plusButtonIsDown) {
                if (BlueTerm.getInstance().getConnectionState() == BluetoothSerialService.STATE_CONNECTED)
                {
                	System.out.println("Sending another inc");
                	BlueTerm.getInstance().send("atinc 10\n\r".getBytes());
                	BlueTerm.getInstance().send("atblink\n\r".getBytes());
                }
                myButtonHandler.postDelayed(this, 100);
            }
        }
    };

    private final Runnable mMinusButtonRunnable = new Runnable() {
        public void run() {
            if (minusButtonIsDown) {
                if (BlueTerm.getInstance().getConnectionState() == BluetoothSerialService.STATE_CONNECTED)
                {
                	BlueTerm.getInstance().send("atdec 10\n\r".getBytes());
                	BlueTerm.getInstance().send("atblink\n\r".getBytes());
                }
                myButtonHandler.postDelayed(this, 100);
            }
        }
    };

 	
}

