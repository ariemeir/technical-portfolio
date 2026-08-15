package edu.ucsf.roboimplantconsole.db;
 
import java.util.ArrayList;
import java.util.List;
 
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
 
public class DatabaseHandler extends SQLiteOpenHelper {
 
    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;
 
    // Database Name
    private static final String DATABASE_NAME = "RoboImplantDB";
 
    // Patients table name
    private static final String TABLE_PATIENTS = "patients";
 
    // Contacts Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_PH_NO = "phone_number";
 
    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
 
    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_PATIENTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
                + KEY_PH_NO + " TEXT" + ")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }
 
    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PATIENTS);
 
        // Create tables again
        onCreate(db);
    }
 
    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */
 
    // Adding new patient
    public void addPatient(Patient patient) {
        SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, patient.getName()); // Contact Name
        values.put(KEY_PH_NO, patient.getPhoneNumber()); // Contact Phone
 
        // Inserting Row
        db.insert(TABLE_PATIENTS, null, values);
        db.close(); // Closing database connection
    }
 
    // Getting single patient
    Patient getPatient(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
 
        Cursor cursor = db.query(TABLE_PATIENTS, new String[] { KEY_ID,
                KEY_NAME, KEY_PH_NO }, KEY_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
 
        Patient patient = new Patient(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), cursor.getString(2));
        // return patient
        return patient;
    }
 
    // Getting All Contacts
    public List<Patient> getAllPatient() {
        List<Patient> patientList = new ArrayList<Patient>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_PATIENTS;
 
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
 
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
            	Patient patient = new Patient();
                patient.setID(Integer.parseInt(cursor.getString(0)));
                patient.setName(cursor.getString(1));
                patient.setPhoneNumber(cursor.getString(2));
                // Adding patient to list
                patientList.add(patient);
            } while (cursor.moveToNext());
        }
 
        // return patient list
        return patientList;
    }
 
    // Updating single patient
    public int updatePatient(Patient patient) {
        SQLiteDatabase db = this.getWritableDatabase();
 
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, patient.getName());
        values.put(KEY_PH_NO, patient.getPhoneNumber());
 
        // updating row
        return db.update(TABLE_PATIENTS, values, KEY_ID + " = ?",
                new String[] { String.valueOf(patient.getID()) });
    }
 
    // Deleting single patient
    public void deleteContact(Patient patient) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PATIENTS, KEY_ID + " = ?",
                new String[] { String.valueOf(patient.getID()) });
        db.close();
    }
 
    // Getting patients Count
    public int getContactsCount() {
        String countQuery = "SELECT  * FROM " + TABLE_PATIENTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();
 
        // return count
        return cursor.getCount();
    }
 
}