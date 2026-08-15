package edu.ucsf.roboimplant.calib;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;
import android.widget.TextView;
import edu.ucsf.roboimplant.generic.ConfigDB;
import edu.ucsf.roboimplantconsole.MessageDispatcher;
import edu.ucsf.roboimplantconsole.R;
import edu.ucsf.roboimplantconsole.RoboMessageListener;
import edu.ucsf.roboimplantconsole.bluetooth.BlueTerm;
import edu.ucsf.roboimplantconsole.bluetooth.BluetoothSerialService;
import edu.ucsf.roboimplantconsole.bluetooth.DeviceListActivity;

public class ExtensionDistanceCalibration  extends Activity implements RoboMessageListener
{
    private static ProgressBar stepProgressBar=null;
    private Handler myHandler = null;
    int targetSpins=0;
    int remainingSpins=0;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.extension_distance_calib);
    		stepProgressBar = (ProgressBar)this.findViewById(R.id.stepProgressBar);
          
    		myHandler = new Handler() {
                
                @Override
                public void handleMessage(Message msg) {          
                	//System.out.println("BlueTerm:Message ("+msg.what+") -----------------------------------------------------------------");
                	
                	switch (msg.what) {
                	
                	case ConfigDB.UPDATE_PROGRESS_BAR:
                    	System.out.println("Updating progress bar from handler, max:"+targetSpins);
                		stepProgressBar.setMax(targetSpins);
                    	stepProgressBar.setProgress(targetSpins-remainingSpins);
                    	stepProgressBar.invalidate();
                    	break;

                	case ConfigDB.PROCEDURE_TERMINATED:
                		break;

                	} //switch 
                } //handleMessage
    };//Handler

    this.getExtensionDistanceCalibrationDialog().show();
    
    }//onCreate;
    
    
    private final AlertDialog getExtensionDistanceCalibrationDialog()
    {
    	
    return new AlertDialog.Builder(this)
    .setTitle("Set the system to initial state, and press ok. A series of adjustments will be performed " +
    		"and distance of extension estimated. Calibration data will be saved on SD-card and sent via email.")
    .setPositiveButton("OK",
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	ExtensionDistanceCalibration.this.runExtensionDistanceCalibration();
            }
        })
        .setNegativeButton("Cancel",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
        .create();
    }

 	private final void runExtensionDistanceCalibration()
 	{
 		TextView tv = (TextView)this.findViewById(R.id.steps_number);
 		int nsteps = Integer.parseInt(tv.getText().toString());
 		tv = (TextView)(this.findViewById(R.id.current_step));
 		BlueTerm btmanager = BlueTerm.getInstance();
 		String motorControllerAddress = btmanager.getCurrentDeviceAddress();
 		System.out.println("The motorController Bluetooth mac address is :"+motorControllerAddress);
 		Intent cameraClickerData = new Intent();
 		cameraClickerData.putExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS, ConfigDB.CAMERA_CLICKER_ADDRESS);
		Intent motorControllerData = new Intent();
		motorControllerData.putExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS, ConfigDB.MOTOR_CONTROLLER_ADDRESS);

		int stepsize = 100; // make this dynamic
 		MessageDispatcher.getInstance().registerListener(this);
 		
 		for (int i=0;i<nsteps;i++)
 		{
 			
 			btmanager.send("atblink\n\r".getBytes());
 			//btmanager.send("atmotoron\n\r".getBytes());
 			//btmanager.send(("atdosage "+stepsize +"\n\r").getBytes());
 			//btmanager.send("atstart\n\r".getBytes());
 			//connect to the camera
 			getSomeRest(5000);
 			
 			btmanager.connect(cameraClickerData);
 			getSomeRest(5000);
 			if (btmanager.getConnectionState()==BluetoothSerialService.STATE_CONNECTED)
 			{
 				System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++ Connected ok, sending snapshot request");
 				//take a snapshot
 				btmanager.send("p".getBytes());
 			}else
 			{
 				System.out.println("--------------------------------------------------- Could not connect to the clicker");
 			}
 			getSomeRest(3000);
 			btmanager.connect(motorControllerData);
 			getSomeRest(3000);
 			
 			
 		}
 		MessageDispatcher.getInstance().removeListener(this);
 		
 	}

 		
 	private void getSomeRest(int delay)
 	{
		try {
			Thread.sleep(delay);
		}
		catch(Exception e)
		{
			System.out.println("Time to do some work....");
		}
 	} 		
 		
 	@Override
 	public void notifyListener(String opcode,String[] params)
 	{
 		System.out.println("Extension Calibration engine has got a message: "+opcode);
 		if (opcode.equalsIgnoreCase(MessageDispatcher.PROCEDURE_TERMINATED))
 		{
 			
 		}
 		else if (opcode.equalsIgnoreCase(MessageDispatcher.DOSAGE_REMAINING_UPDATE))
 		{
			remainingSpins = Integer.parseInt(params[1]);
			targetSpins = Integer.parseInt(params[2]);
			System.out.println("remaining spins:"+remainingSpins+" out of "+targetSpins);
				
 		}
 	}
    

}
