package edu.ucsf.roboimplant.menu;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import edu.ucsf.roboimplant.data.Patient;
import edu.ucsf.roboimplantconsole.R;


public class PatientListAdapter extends BaseAdapter{

	    private Context context;
	    private List<Patient> studyList;
	    private boolean[] selected=null;
		private int rowResID;

	    public PatientListAdapter(Context context, int rowResID,List<Patient> studyList ) { 
	        this.context = context;
			this.rowResID = rowResID;
	        this.studyList = studyList;
	        this.selected = new boolean[studyList.size()];
	        for (int i=0;i<this.selected.length;i++)
	        	selected[i]=false;
	    }

	    public int getCount() {                        
	        return studyList.size();
	    }

	    public Object getItem(int position) {     
	        return studyList.get(position);
	    }

	    public long getItemId(int position) {  
	        return position;
	    }

	    public View getView(final int position, View convertView, ViewGroup parent) { 
	        System.out.println("getView("+position+")");
	    	Patient patient = studyList.get(position);
	        LayoutInflater inflate = LayoutInflater.from(context);
	        View v = inflate.inflate( rowResID, parent, false);
	        TextView patient_id = (TextView)v.findViewById( R.id.patient_id);
			patient_id.setText( patient.id);
			
			TextView desc = (TextView)v.findViewById( R.id.patient_name);
			desc.setText( patient.name);

			//TextView birthdate = (TextView)v.findViewById( R.id.birth_date);
			//birthdate.setText( patient.dispatchdate);

			TextView product = (TextView)v.findViewById( R.id.implant_date);
			product.setText( patient.implant_date);

			//TextView status = (TextView)v.findViewById( R.id.current_extension);
			//status.setText( patient.status);

			//TextView prio = (TextView)v.findViewById( R.id.patient_priority);
			//prio.setText( patient.priority);

			return v;
	    }
	    
	    public void invertSelection(int position)
	    {
	    	selected[position]= !selected[position];
	    	System.out.println("Selection inverted to "+selected[position]+" for pos #"+position);
	    }
	    
	    public void selectAll()
	    {	
	    	for (int i=0;i<selected.length;i++)
	    		selected[i]=true;
	    	notifyDataSetInvalidated();
	    }

	    public void deselectAll()
	    {
	    	for (int i=0;i<selected.length;i++)
	    		selected[i]=false;
	    	notifyDataSetInvalidated();
	    }

	    public void deselect(int pos)
	    {
    		selected[pos]=false;
	    }

	    public void select(int pos)
	    {
    		selected[pos]=true;
	    }

	    public ArrayList<Patient> getSelectedItems()
	    {
	    	ArrayList<Patient> data = new ArrayList<Patient>();
	    	for (int i=0;i<selected.length;i++)
	    	{
	    		if (selected[i])
	    			data.add(studyList.get(i));
	    	}
	    	return data;
	    }

}
