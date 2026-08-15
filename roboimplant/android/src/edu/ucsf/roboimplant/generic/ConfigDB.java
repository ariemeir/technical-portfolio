package edu.ucsf.roboimplant.generic;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import android.content.Context;
import android.hardware.SensorManager;
import android.telephony.TelephonyManager;

/*
 * This is a configuration DataBase : all the config params are stored only in this class
 */

public class ConfigDB 
{
	private static Context ctx=null;
	//all common server host
	//public static final String SERVER_HOST="10.72.39.222";
	public static String SIMULATED_DATA_ROOT = "/sdcard/roboimplant/";

	public  static final String FIELD_PATIENT_ID="patient_id"; 
	public  static final String FIELD_PATIENT_NAME="name";

	public static final String COLOR_DEEP_PURPLE="#251025";
	
    //messages
    public static final int UPDATE_PROGRESS_BAR = 1021;
    public static final int PROCEDURE_TERMINATED = 1022;
    public static final int ESTABLISHED_COUPLING = 1023;
    public static final int LOST_COUPLING = 1024;
    public static final int UPDATE_DOSAGE = 1025;
    
    public static final String CAMERA_CLICKER_ADDRESS = "00:12:08:08:01:32";
    public static final String MOTOR_CONTROLLER_ADDRESS = "00:06:66:02:D3:83";
    
    public static final int ROTATION_CW = 0;
    public static final int ROTATION_CCW = 1;
    
	
	
	public static final DateFormat StdDateFormat = new SimpleDateFormat("MM/dd/yy HH:mm");
	
	public static void init(Context c)
	{
		ctx=c;
	}

	public static boolean isEmulator()
	{
		return true;
	}
	
}
