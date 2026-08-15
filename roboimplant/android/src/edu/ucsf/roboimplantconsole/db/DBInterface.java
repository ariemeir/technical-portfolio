package edu.ucsf.roboimplantconsole.db;

import java.util.List;

import android.content.Context;
import android.util.Log;

public class DBInterface {

	private Context _ctx=null;
	private static DBInterface _instance=null;
	static {
		_instance = new DBInterface();
	}
	
	private DBInterface()
	{}
	
	public void init(Context ctx)
	{
		_ctx = ctx;
	}
	
	
	public static DBInterface getInstance()
	{
		return _instance;
	}
	
	
	public void createSomeRecords()
	{
		DatabaseHandler db = new DatabaseHandler(_ctx);
		 
        /**
         * CRUD Operations
         * */
        // Inserting Contacts
        Log.d("Insert: ", "Inserting ..");
        db.addPatient(new Patient("Ravi", "9100000000"));
        db.addPatient(new Patient("Srinivas", "9199999999"));
        db.addPatient(new Patient("Tommy", "9522222222"));
        db.addPatient(new Patient("Karthik", "9533333333"));
 
        // Reading all contacts
        Log.d("Reading: ", "Reading all contacts..");
        List<Patient> contacts = db.getAllPatient();      
 
        for (Patient cn : contacts) 
        {
            String log = "Id: "+cn.getID()+" ,Name: " + cn.getName() + " ,Phone: " + cn.getPhoneNumber();
                // Writing Contacts to log
        Log.d("Name: ", log);
        }
	}
}
