package edu.ucsf.roboimplant.db;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;


public class DatabaseHelper extends SQLiteOpenHelper 
{
    private static final String DATABASE_NAME = "data";
    private static final int DATABASE_VERSION = 2;
    private static DatabaseHelper _instance = null;
    SQLiteDatabase mydb=null; //cache it for later operations
    static Context ctx;
    
    private DatabaseHelper(Context context) 
    {
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
    	ctx = context;
    	System.out.println("DatabaseHelper::ctor");
    }
    
    public static DatabaseHelper getInstance(Context ctx)
    {
    	if (_instance ==null)
    		_instance = new DatabaseHelper(ctx);
    	
    	return _instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        System.out.println("DatabaseHelper::onCreate---------------------------------");
    	db.execSQL(PatientDbAdapter.getCreationString());
    	db.execSQL(AdjustmentDbAdapter.getCreationString());
    	mydb=db;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	System.out.println("DatabaseHelper::onUpgrade "+oldVersion+" --> "+newVersion);
        db.execSQL("DROP TABLE IF EXISTS "+PatientDbAdapter.DATABASE_TABLE);
        db.execSQL("DROP TABLE IF EXISTS "+AdjustmentDbAdapter.DATABASE_TABLE);
        onCreate(db);
    }
    
    public void resetToInitialPosition(String configuration)
    {
   		mydb = this.getWritableDatabase();
    	//just erase the whole thing and fill it in with data
    	System.out.println("mydb = "+mydb);
        mydb.execSQL("DROP TABLE IF EXISTS "+PatientDbAdapter.DATABASE_TABLE);
        mydb.execSQL("DROP TABLE IF EXISTS "+AdjustmentDbAdapter.DATABASE_TABLE);

        mydb.execSQL(PatientDbAdapter.getCreationString());
        mydb.execSQL(AdjustmentDbAdapter.getCreationString());

        //add some data here according to configuration
        PatientDbAdapter.createInstance(ctx);
        PatientDbAdapter.getInstance().open();
        
        PatientDbAdapter.getInstance().createEntry("1533", "Jack London", "04/22/60", "04/22/61",
        		"London Square", "510-600-8000");
        PatientDbAdapter.getInstance().createEntry("1834", "Steve Jordan", "01/01/50", "04/01/60",
        		"Oakland Downtown", "510-300-3888");
        PatientDbAdapter.getInstance().close();
        
        AdjustmentDbAdapter.createInstance(ctx);
        AdjustmentDbAdapter.getInstance().open();
        AdjustmentDbAdapter.getInstance().createEntry("11", "1533", "04/22/09 08:35", "1.0");
        AdjustmentDbAdapter.getInstance().createEntry("12", "1533", "04/23/09 11:00", "2.0");
        AdjustmentDbAdapter.getInstance().createEntry("15", "1834", "05/18/09 11:35", "3.00");
        AdjustmentDbAdapter.getInstance().createEntry("16", "1834", "05/18/09 14:00", "80");
        AdjustmentDbAdapter.getInstance().close();
        

        mydb.close();
        Toast.makeText(ctx, "Reset DB successfully !", Toast.LENGTH_LONG).show();
    }
    
    public static void setContext(Context c)
    {
    	ctx=c;
    }
    
    public void resetToEmptyDB()
    {
   		mydb = this.getWritableDatabase();
    	//just erase the whole thing and fill it in with data
    	System.out.println("mydb = "+mydb);
        mydb.execSQL("DROP TABLE IF EXISTS "+PatientDbAdapter.DATABASE_TABLE);
        mydb.execSQL("DROP TABLE IF EXISTS "+AdjustmentDbAdapter.DATABASE_TABLE);

        mydb.execSQL(PatientDbAdapter.getCreationString());
        mydb.execSQL(AdjustmentDbAdapter.getCreationString());
    }
    
}
