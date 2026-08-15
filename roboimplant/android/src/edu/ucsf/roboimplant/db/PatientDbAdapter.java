/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package edu.ucsf.roboimplant.db;

import java.util.Date;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Simple notes database access helper class.  */
public class PatientDbAdapter {

    public static final String KEY_PAT_ID = "patient_id";
    public static final String KEY_PAT_NAME = "name";
    public static final String KEY_PAT_BIRTHDATE = "birth_date";
    public static final String KEY_PAT_IMPLANT_DATE = "implant_date";
    public static final String KEY_PAT_ADDRESS = "address";
    public static final String KEY_PAT_PHONE = "so_phone";
    

    //IncomingDbAdapter is a singleton - this is the single instance
    private static PatientDbAdapter _instance =null; 
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private static final String DATABASE_NAME = "data";
    public static final String DATABASE_TABLE = "patients";
    private static final int DATABASE_VERSION = 2;
    
    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
            "create table "+DATABASE_TABLE+" (_id integer primary key autoincrement, "
                    + 	KEY_PAT_ID+" text not null," +
                    	KEY_PAT_NAME+" text not null," +
                    	KEY_PAT_BIRTHDATE+" text not null," +
                    	KEY_PAT_IMPLANT_DATE+" text not null," +
                    	KEY_PAT_ADDRESS+" text not null," +
                    	KEY_PAT_PHONE+" text not null" +
                    			");";
                    	                 	
    private final Context mCtx;

    public static String getCreationString() {return DATABASE_CREATE;}

    public static void createInstance(Context ctx)
    {
    	if (_instance!=null)
    	{
    		System.out.println("Warning:: IncomingDbAdapter instance already exists. nop");
    		return;
    	}
    	_instance = new PatientDbAdapter(ctx);
    }
    
    public static PatientDbAdapter getInstance()
    {
    	return _instance;
    }
    
    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    private PatientDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @throws SQLException if the database could be neither opened or created
     */
    public PatientDbAdapter open() throws SQLException {
    	System.out.println("IncomingDbAdapter::open");
    	mDbHelper = DatabaseHelper.getInstance(mCtx);
    	System.out.println("IncomingDbAdapter::getWritableDatabase");
        mDb = mDbHelper.getWritableDatabase();
    	System.out.println("mdb = "+mDb);
        return this;
    }
    
    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new entry using the parameters provided. If the entry is
     * successfully created return the new rowId for that entry, otherwise return
     * a -1 to indicate failure.
     * 
     * @return rowId or -1 if failed
     */
    public long createEntry(String pat_id,String name,String birthdate,
    				String implant_date,String address,String phone)
    {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_PAT_ID, pat_id);
        initialValues.put(KEY_PAT_NAME, name);
        initialValues.put(KEY_PAT_BIRTHDATE, birthdate);
        initialValues.put(KEY_PAT_IMPLANT_DATE, implant_date);
        initialValues.put(KEY_PAT_ADDRESS, address);
        initialValues.put(KEY_PAT_PHONE, phone);

        System.out.println("mDb = "+mDb+" initial : "+initialValues);
        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Delete the note with the given rowId
     * 
     * @param rowId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteEntry(String pat_id) {

        return mDb.delete(DATABASE_TABLE, KEY_PAT_ID + "=" + pat_id, null) > 0;
    }

    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAllInEntries() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_PAT_ID,
                KEY_PAT_NAME,KEY_PAT_BIRTHDATE,KEY_PAT_IMPLANT_DATE,KEY_PAT_ADDRESS,KEY_PAT_PHONE}, 
                null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the note that matches the given rowId
     * 
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchEntry(String patient_id) throws SQLException {

        Cursor mCursor =

                mDb.query(true, DATABASE_TABLE, new String[] {
                		KEY_PAT_ID,
                        KEY_PAT_NAME,
                        KEY_PAT_BIRTHDATE,
                        KEY_PAT_IMPLANT_DATE,
                        KEY_PAT_ADDRESS,
                        KEY_PAT_PHONE}, 
                        KEY_PAT_ID + "=" + patient_id, null,null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the note using the details provided. The note to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     * 
     * @param rowId id of note to update
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateOutEntry(String pat_id,String name,String birthdate,
 				String implant_date,String address,String phone)
    {
        ContentValues args = new ContentValues();
        args.put(KEY_PAT_ID, pat_id);
        args.put(KEY_PAT_NAME,name);
        args.put(KEY_PAT_BIRTHDATE,birthdate);
        args.put(KEY_PAT_IMPLANT_DATE,implant_date);
        args.put(KEY_PAT_ADDRESS,address);
        args.put(KEY_PAT_PHONE,phone);

        return mDb.update(DATABASE_TABLE, args, KEY_PAT_ID + "=" + pat_id, null) > 0;
    }
}
