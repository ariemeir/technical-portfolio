package edu.ucsf.roboimplant.db;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import edu.ucsf.roboimplant.generic.CSVReader;
import edu.ucsf.roboimplant.generic.ConfigDB;

import android.content.Context;

public class DBSimulator 
{
	private Context ctx=null;
	private static DBSimulator _instance = null;
	
	private DBSimulator(Context c)
	{
		ctx=c;
	}
	
	public static DBSimulator getInstance(Context c)
	{
		if (_instance == null)
			_instance = new DBSimulator(c);
		
		return _instance;
	}
	
	
	public boolean process(String prefix)
	{
		try {
			
			DatabaseHelper.getInstance(ctx).resetToEmptyDB();
			String sofilename = ConfigDB.SIMULATED_DATA_ROOT+prefix+"_so.csv";
			processServiceOrders(sofilename);
			String laborfilename = ConfigDB.SIMULATED_DATA_ROOT+prefix+"_labor.csv";
			processLabor(laborfilename);
			String partsfilename = ConfigDB.SIMULATED_DATA_ROOT+prefix+"_parts.csv";
		}
		catch(Exception e)
		{
			return false;
		}
		return true;
	}
	
	public void processServiceOrders(String file) throws IOException
	{
		CSVReader reader = new CSVReader(
				new FileReader(file),
				CSVReader.DEFAULT_SEPARATOR,
				CSVReader.DEFAULT_QUOTE_CHARACTER,1);  //skip the header line
		String [] nextLine;
		PatientDbAdapter.createInstance(ctx);
		PatientDbAdapter.getInstance().open();
		while ((nextLine = reader.readNext()) != null) 
		{
			PatientDbAdapter.getInstance().createEntry(
					nextLine[0],nextLine[1],nextLine[2],nextLine[3],nextLine[4],
					nextLine[5]);					
		}
		PatientDbAdapter.getInstance().close();
		
	}
	
	public void processLabor(String file) throws IOException
	{
		CSVReader reader = new CSVReader(
				new FileReader(file),
				CSVReader.DEFAULT_SEPARATOR,
				CSVReader.DEFAULT_QUOTE_CHARACTER,1);  //skip the header line
		String [] nextLine;

		AdjustmentDbAdapter.createInstance(ctx);
		AdjustmentDbAdapter.getInstance().open();
		while ((nextLine = reader.readNext()) != null) 
		{
			AdjustmentDbAdapter.getInstance().createEntry(
					nextLine[0],nextLine[1],nextLine[2],nextLine[3]);
		}
		AdjustmentDbAdapter.getInstance().close();
	}

	//go over the simulated data folder and look for *_so.csv - the parts + labor are expected to be there
	public static String[] getAvailableData()
	{
		ArrayList<String> data = new ArrayList<String>();
		File f = new File(ConfigDB.SIMULATED_DATA_ROOT);
		String children[] = f.list();
		for (int i=0;i<children.length;i++)
		{
			if (children[i].endsWith("_so.csv"))
			{
				String prefix = children[i].substring(0, children[i].indexOf("_so.csv"));	
				data.add(prefix);
			}
		}
		if (data.size()==0)
			return null;
		String[] ret = new String[data.size()];
		for (int i=0;i<data.size();i++)
			ret[i]=data.get(i);
		return ret;
	}
}
