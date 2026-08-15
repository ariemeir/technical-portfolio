package edu.ucsf.roboimplantconsole.bluetooth;


import edu.ucsf.roboimplantconsole.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;


//This class is a singleton pattern implementation that allows getting Bluetooth Terminal Services

public class BlueTerm {
    //Singleton instance
  private static BlueTerm _instance=null;
  // Intent request codes
    public static final int REQUEST_CONNECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;

    private static TextView mTitle;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    /**
     * Set to true to add debugging code and logging.
     */
    public static final boolean DEBUG = true;

    /**
     * Set to true to log each character received from the remote process to the
     * android log, which makes it easier to debug some kinds of problems with
     * emulating escape sequences and control codes.
     */
    public static final boolean LOG_CHARACTERS_FLAG = DEBUG && false;

    /**
     * Set to true to log unknown escape sequences.
     */
    public static final boolean LOG_UNKNOWN_ESCAPE_SEQUENCES = DEBUG && false;

    /**
     * The tag we use when logging, so that our messages can be distinguished
     * from other messages in the log. Public because it's used by several
     * classes.
     */
  public static final String LOG_TAG = "BlueTerm";

    // Message types sent from the BluetoothReadService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;  

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
  
  private BluetoothAdapter mBluetoothAdapter = null;
  

  private static BluetoothSerialService mSerialService = null;
    
  private static InputMethodManager mInputManager;
  
    private SharedPreferences mPrefs;
  
    private MenuItem mMenuItemConnect;

    private Context myContext=null;

    //Anyone who wants to know about the status changes of BlueTerm
    private Handler clientHandler=null;
    
    public static BlueTerm getInstance()
    {
      if (_instance==null)
        _instance = new BlueTerm();
      
      return _instance;
    }
    
    public BlueTerm()
    {
      System.out.println("Creating BlueTerm object");
      //mPrefs = PreferenceManager.getDefaultSharedPreferences(_myContext);
       
      if (DEBUG)
    	  Log.e(LOG_TAG, "+++ ON CREATE +++");

        
      //readPrefs();
        
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    if (mBluetoothAdapter == null) {
            finishDialogNoBluetooth();
      return;
    }
    
        mSerialService = new BluetoothSerialService(myContext, mHandlerBT);        

    if (DEBUG)
      Log.e(LOG_TAG, "+++ DONE IN ON CREATE +++");
    
    
    if (mSerialService != null) {
        // Only if the state is STATE_NONE, do we know that we haven't started already
        if (mSerialService.getState() == BluetoothSerialService.STATE_NONE) {
          // Start the Bluetooth chat services
          mSerialService.start();
        }
      }

  }

    
    public void destroyInstance()
    {
        Log.e(LOG_TAG, "--- ON DESTROY ---");
        
        if (mSerialService != null)
          mSerialService.stop();
    }

    public void init(Context _myContext)
    {
    	myContext = _myContext;
    }
    
    public void registerClientHandler(Handler _clientHandler)
    {
    	clientHandler = _clientHandler;
    }

    
    public void restart()
    {	
        if (mSerialService != null)
        {
        	mSerialService.stop();
        	mSerialService.start();
        	
        }
        
    }
    

  public synchronized int getConnectionState() {
    return mSerialService.getState();
  }


    public synchronized void send(byte[] out) {
      mSerialService.write( out );
    }
    
    public void toggleKeyboard() {
      mInputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
    
    public int getTitleHeight() {
      return mTitle.getHeight();
    }
    
    // The Handler that gets information back from the BluetoothSerialService
    private final Handler mHandlerBT = new Handler() {
      
        @Override
        public void handleMessage(Message msg) {          
        	//System.out.println("BlueTerm:Message ("+msg.what+") -----------------------------------------------------------------");
        	
        	//this handler is being bombarded from all over - prevent any race conditions	
        	synchronized(this)
        	{
        	//If the client wants to know whats going on, we'll keep him posted.
        	if (clientHandler!=null)
        	{
        		Message copymsg = new Message();
        		copymsg.copyFrom(msg);
        		clientHandler.sendMessage(copymsg);
        	}
        	
        	switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(DEBUG) Log.i(LOG_TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothSerialService.STATE_CONNECTED:
                  System.out.println("Yeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeah !!!!");
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
                  if (mMenuItemConnect != null) {
                    mMenuItemConnect.setIcon(android.R.drawable.ic_menu_search);
                    mMenuItemConnect.setTitle(R.string.connect);
                  }

                //mInputManager.hideSoftInputFromWindow(mEmulatorView.getWindowToken(), 0);
                  
                    //mTitle.setText(R.string.title_not_connected);

                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                
                break;
                
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;              
                String response = new String(readBuf).substring(0,msg.arg1);
                //Toast.makeText(myContext.getApplicationContext(), "Got message:"+response, Toast.LENGTH_SHORT).show();
                MessageDispatcher.getInstance().process(response);
                
                break;
                
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(myContext.getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(myContext.getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
        }//end of synchronized
    };
	private String currentDeviceAddress;    

    
    public void finishDialogNoBluetooth() {
        AlertDialog.Builder builder = new AlertDialog.Builder(myContext);
        builder.setMessage(R.string.alert_dialog_no_bt)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setTitle(R.string.app_name)
        .setCancelable( false )
        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       //finish();              
                     }
               });
        AlertDialog alert = builder.create();
        alert.show(); 
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(DEBUG) Log.d(LOG_TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        
        case REQUEST_CONNECT_DEVICE:

            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                
                //save this address if somebody wants to cache it and use it for later.
                this.currentDeviceAddress = address;

                System.out.println("Connecting to: "+address);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mSerialService.connect(device);                
            }
            break;

        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                Log.d(LOG_TAG, "BT not enabled");
                
                finishDialogNoBluetooth();                
            }
        }
    }
    
    public String getCurrentDeviceAddress()
    {
    	return currentDeviceAddress;
    }
    
    public void connect(Intent data)
    {
    	String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mSerialService.connect(device);     
        System.out.println("After serial.connect was called for "+address);
    }
    
}
