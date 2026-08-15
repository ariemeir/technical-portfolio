package edu.ucsf.roboimplant.menu;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import edu.ucsf.roboimplant.data.Adjustment;
import edu.ucsf.roboimplantconsole.R;


public class AdjustmentListAdapter extends BaseAdapter{

	    private Context context;
	    private List<Adjustment> studyList;
	    private boolean[] selected=null;
		private int rowResID;

	    public AdjustmentListAdapter(Context context, int rowResID,List<Adjustment> adjustmentList ) { 
	        this.context = context;
			this.rowResID = rowResID;
	        this.studyList = adjustmentList;
	        this.selected = new boolean[adjustmentList.size()];
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
	        Adjustment adj = studyList.get(position);
	        LayoutInflater inflate = LayoutInflater.from(context);
	        View v = inflate.inflate( rowResID, parent, false);
			
			TextView adj_date = (TextView)v.findViewById( R.id.adjustment_date);
			adj_date.setText( adj.adjustment_date);

			TextView start = (TextView)v.findViewById( R.id.adjustment_amount);
			start.setText( adj.adjustment_amount);

			/*TextView end = (TextView)v.findViewById( R.id.labor_end);
			end.setText( lbr.t_end);

			TextView hours = (TextView)v.findViewById( R.id.labor_hours);
			hours.setText( lbr.t_hours);

			TextView rate = (TextView)v.findViewById( R.id.labor_rate);
			rate.setText( lbr.rate);*/

			return v;
	    }
}
