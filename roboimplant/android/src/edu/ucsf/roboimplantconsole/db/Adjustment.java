package edu.ucsf.roboimplantconsole.db;

import java.util.Date;


public class Adjustment {
 
    //private variables
    int _id;
    int _patient_id ;
    Date _adjustment_date;
    Date _adjustment_size;
    String _comments;
    String _placeholder1;
    String _placeholder2;
 
    // Empty constructor
    public Adjustment(){
 
    }
    // constructor
    public Adjustment(int id, int patient_id, Date adj_date){
        this._id = id;
        this._patient_id = patient_id;
        this._adjustment_date = adj_date;
    }
 
    // getting ID
    public int getID(){
        return this._id;
    }
 
    // setting id
    public void setID(int id){
        this._id = id;
    }
 
    // getting patient id
    public int getPatientId(){
        return this._patient_id;
    }

    // setting patient id
    public void setPatientId(int patient_id){
        this._patient_id = patient_id;
    }
 
}