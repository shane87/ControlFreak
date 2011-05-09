package com.shane87.controlfreak;

import com.shane87.controlfreak.R;

import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListAdapter;
import android.widget.Spinner;

public class ControlFreak extends ExpandableListActivity {
	//Command strings to pass to ShellInterface to get various values
	//Copied as is from xan's VoltageControl app
	protected static final String C_FREQUENCY_VOLTAGE_TABLE = "toolbox cat /sys/devices/system/cpu/cpu0/cpufreq/frequency_voltage_table";
	protected static final String C_STATES_ENABLED = "toolbox cat /sys/devices/system/cpu/cpu0/cpufreq/states_enabled_table";
	protected static final String C_TIME_IN_STATE = "toolbox cat /sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state";
	protected static final String C_UV_MV_TABLE = "toolbox cat /sys/devices/system/cpu/cpu0/cpufreq/UV_mV_table";
	protected static final String C_SCALING_MAX_FREQ = "toolbox cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
	//Command to enumerate list of available governors
	protected static final String C_GOVERNORS_AVAILABLE = "toolbox cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors";
	
	//Kernel Support flags, copied as is from xan's VoltageControl app
	protected static final int 	  K_FREQUENCY_VOLTAGE_TABLE_CAP = 5;
	protected static final int 	  K_NO_CAP = -1;
	protected static final int 	  K_ROOT_CAP = 3;
	protected static final int 	  K_STATES_ENABLED_CAP = 6;
	protected static final int 	  K_UV_CAP = 4;
	protected static final int 	  NOROOT = 1;
	
	//Handler messages, same as voltage control
	protected static final int	  REFRESH = 0;
	protected static final int	  WRONGKERNEL = 2;
    
	//Global variables for our class
	private ExpandableListAdapter 		frequencyAdapter;
	String 								maxFreq;
	protected int 						maxFreqId;
	private String 						maxFrequency = " ";
	private ArrayList<FrequencyStats> 	fqStatsList = new ArrayList<FrequencyStats>();
	private Menu 						mMenu;
	private int 						activeSched = 0;
	private String[] 					schedTable;
	Map<String, String> 				stockVoltages = new HashMap<String, String>();
	private String 						uvValues;
	private String[] 					cpuThresValues;
	private String 						cpuThres;
	private String						curGovernor;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	//Request the orientation to stay vertical
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
    	//Call the superclass onCreate method
        super.onCreate(savedInstanceState);
        
        //set the contentview to main
        setContentView(R.layout.main);
        
        //lets setup the arrays for our spinners
        final ArrayAdapter<String> adapterForSchedSpinner = new ArrayAdapter<String>(this,
        														android.R.layout.simple_spinner_item);
        final ArrayAdapter<String> adapterForFreqSpinner  = new ArrayAdapter<String>(this,
																android.R.layout.simple_spinner_item);
        final ArrayAdapter<String> adapterForCpuTSpinner  = new	ArrayAdapter<String>(this,
																android.R.layout.simple_spinner_item);
        final ArrayAdapter<String> adapterForGovSpinner   = new ArrayAdapter<String>(this,
																android.R.layout.simple_spinner_item);
        
        //now lets get pointers to our spinners
        final Spinner schedSpinner 	= (Spinner)findViewById(R.id.schedSpinner);
        final Spinner freqSpinner 	= (Spinner)findViewById(R.id.freqSpinner);
        final Spinner cpuTSpinner 	= (Spinner)findViewById(R.id.cpuThreshSpinner);
        final Spinner govSpinner 		= (Spinner)findViewById(R.id.governorSpinner);
        
        //and pointers to our help buttons
        Button schedButton		= (Button)findViewById(R.id.schedHelpButton);
        Button freqButton		= (Button)findViewById(R.id.freqHelpButton);
        Button cpuTButton		= (Button)findViewById(R.id.cpuThreshHelpButton);
        Button govButton		= (Button)findViewById(R.id.governorHelpButton);
        
        //lets go ahead and setup our onClickListeners, so our buttons will do something
        //first, the schedButton
        schedButton.setOnClickListener(new View.OnClickListener()
        {
			
			@Override
			public void onClick(View v)
			{
				//Call our showSchedHelp() function to display the help associated with the scheduler
				//showSchedHelp();
				
			}
		});
        
        //next, the freqButton
        freqButton.setOnClickListener(new View.OnClickListener()
        {
			
			@Override
			public void onClick(View v)
			{
				//Call our showFreqHelp() function to display the frequency limit help
				//showFreqHelp();
				
			}
		});
        
        //next, the cpuTButton
        cpuTButton.setOnClickListener(new View.OnClickListener()
        {
			
			@Override
			public void onClick(View v)
			{
				//Call our showCpuTHelp() function to display the cpu threshold help
				//showCpuTHelp();
				
			}
		});
        
        //finally, the govButton
        govButton.setOnClickListener(new View.OnClickListener()
        {
			
			@Override
			public void onClick(View v)
			{
				//Call our showGovHelp() function to display the governor help
				//showGovHelp();
				
			}
		});
        
        //as a last step before we go to our initialization code, lets get our frequency list adapter
        frequencyAdapter = new FrequencyListAdapter(this, this.getApplicationContext());
        
        //now lets setup our refresh handler, so that when we finish initialization we can finish our
        //ui setup
        final Handler uiRefreshHandler = new Handler()
        {
        	@Override
        	public void handleMessage(Message msg)
        	{
        		switch(msg.what)
        		{
        		case REFRESH:
        		{
        			OnItemSelectedListener schedSpinnerListener = new Spinner.OnItemSelectedListener()
        			{

						@Override
						public void onItemSelected(AdapterView<?> arg0,
								View arg1, int arg2, long arg3)
						{
							activeSched = (int)arg0.getSelectedItemId();
							
						}

						//not used, but we have to implement it
						@Override
						public void onNothingSelected(AdapterView<?> arg0){}
        				
        			};
        			
        			OnItemSelectedListener freqSpinnerListener = new Spinner.OnItemSelectedListener()
        			{

						@Override
						public void onItemSelected(AdapterView<?> arg0,
								View arg1, int arg2, long arg3)
						{
							maxFrequency = arg0.getSelectedItem().toString();
							
						}

						//not used, but we have to implement it
						@Override
						public void onNothingSelected(AdapterView<?> arg0){}
        				
        			};
        			
        			OnItemSelectedListener cpuTSpinnerListener = new Spinner.OnItemSelectedListener()
        			{

						@Override
						public void onItemSelected(AdapterView<?> arg0,
								View arg1, int arg2, long arg3)
						{
							switch((int)arg0.getSelectedItemId())
							{
							case 0:
								cpuThres = "stock";
								break;
							case 1:
								cpuThres = "performance";
								break;
							case 2:
								cpuThres = "battery";
							}
							
						}

						//not used, but we have to implement it
						@Override
						public void onNothingSelected(AdapterView<?> arg0) {}
        				
        			};
        			
        			OnItemSelectedListener govSpinnerListener = new Spinner.OnItemSelectedListener()
        			{

						@Override
						public void onItemSelected(AdapterView<?> arg0,
								View arg1, int arg2, long arg3)
						{
							curGovernor = arg0.getSelectedItem().toString();
							
						}

						@Override
						public void onNothingSelected(AdapterView<?> arg0) {}
        				
        			};
        			
        			//lets link our listeners to their Spinners, and the Array adapters to the Spinners
        			//as well
        			//first, lets link the listeners
        			freqSpinner.setOnItemSelectedListener(freqSpinnerListener);
        			schedSpinner.setOnItemSelectedListener(schedSpinnerListener);
        			cpuTSpinner.setOnItemSelectedListener(cpuTSpinnerListener);
        			govSpinner.setOnItemSelectedListener(govSpinnerListener);
        			
        			//now lets link the array adapters to the correct spinners
        			freqSpinner.setAdapter(adapterForFreqSpinner);
        			schedSpinner.setAdapter(adapterForSchedSpinner);
        			cpuTSpinner.setAdapter(adapterForCpuTSpinner);
        			govSpinner.setAdapter(adapterForGovSpinner);
        			
        			//lets set the initial selections of our spinners appropriately
        			//first the scheduler spinner
        			schedSpinner.setSelection(activeSched);
        			
        			//next the cpuThresh spinner
        			//stock is in the first position, 0
        			if(cpuThres.contains("stock"))
        				cpuTSpinner.setSelection(0);
        			//performance is in the second position, 1
        			else if(cpuThres.contains("performance"))
        				cpuTSpinner.setSelection(1);
        			//battery is in the third position, 2
        			else if(cpuThres.contains("battery"))
        				cpuTSpinner.setSelection(2);
        			//if cpuThresh is anything else, then cpuThresh is not supported
        			//so lets hide the cpuThresh views
        			//NOTE: typically, cpuThresh should be either stock, battery, performance, or none
        			//		this is just to catch anything else if it is not set correctly
        			else
        			{
        				findViewById(R.id.cpuThreshHelpButton).setVisibility(View.GONE);
        				findViewById(R.id.cpuThreshSpinner).setVisibility(View.GONE);
        				findViewById(R.id.cpuThreshTextView).setVisibility(View.GONE);
        			}
        			
        			//next, the governor spinner
        			govSpinner.setSelection(adapterForGovSpinner.getPosition(curGovernor));
        			
        			//before we can set the freqSpinner, lets setup our expandable list adapter
        			((FrequencyListAdapter) frequencyAdapter).setFrequencies(fqStatsList);
        			setListAdapter(frequencyAdapter);
        			
        			//now lets set the freqSpinner, first we call for the current max frequency,
        			//then we will loop through the available frequencies to pick the right one
        			String maxCpu = ""; //= getMaxFreq();
        			//dont forget to take 4 digits from the end, since the getMaxFreq() function
        			//simply returns the contents of scaling_max_frequency file, which has the
        			//currently set max frequency in hz
        			//maxCpu = maxCpu.substring(0, maxCpu.length() - 4);
        			
        			for(int i = 0; i < freqSpinner.getCount(); i++)
        			{
        				if(freqSpinner.getItemAtPosition(i).toString().matches(maxCpu + " Mhz"))
        				{
        					freqSpinner.setSelection(i);
        					break;
        				}
        			}
        			
        			break;
        		}
        		case NOROOT:
        		{
        			//showNoRootAlert();
        			break;
        		}
        		case WRONGKERNEL:
        		{
        			//showWrongKernelAlert();
        			break;
        		}
        		}
        	}
        };
        
        //Ok, now that the refresh handler is setup, lets get down to the business of initialization
        //Lets show a spinner dialog to let the user know what's going on
        final ProgressDialog spinnerDiag = ProgressDialog.show(this,
        		"Initialization", 
        		"Querrying System Settings", true);
        
        //now lets start a background thread to do the actual initialization
        new Thread(new Runnable()
        {

			@Override
			public void run() {
				//first, lets set up a string to test the availability of su
				String tester = null;
				
				//if su is available, we will put the contents of uv_mv_table in the tester string
				//otherwise, tester will stay as null
				if(ShellInterface.isSuAvailable())
					tester = ShellInterface.getProcessOutput(C_UV_MV_TABLE);
				
				//if tester is not null, we have su, so lets continue
				if(tester != null)
				{
					//if tester is not null, but is empty, this kernel does not support
					//uv/oc control in the right way, so lets send the WRONGKERNEL message
					//to let the user know
					if(tester == "")
						uiRefreshHandler.sendEmptyMessage(WRONGKERNEL);
					//otherwise, we are ready to begin!
					else
					{
						
					}
				}
				//if tester IS null, we do not have su, so lets send the NOROOT message to the handler
				//so it can notify the user
				else
					uiRefreshHandler.sendEmptyMessage(NOROOT);
				
				//now we can dismiss our spinner dialog, then return
				spinnerDiag.dismiss();
				return;
			}
			
			private void getFreqTable(String tester, String[] freqTable)
			{
				String[] uvTable;
				uvValues = tester;
				if(uvValues == null)
					uvValues = new String("");
				
				if(uvValues.equals(""))
				{
					for(int i = 0; i < freqTable.length; i += 2)
					{
						fqStatsList.add(new FrequencyStats(Integer.parseInt(freqTable[i]) - 1000,
								                           0, 
								                           Integer.parseInt(freqTable[i + 1]),
								                           new CheckBox(getBaseContext())));
					}
				}
				else
				{
					uvTable = uvValues.split(" ");
					for(int i = 0; i < freqTable.length; i += 2)
					{
						fqStatsList.add(new FrequencyStats(Integer.parseInt(freqTable[i]) - 1000,
								                           Integer.parseInt(uvTable[i / 2]), 
								                           Integer.parseInt(freqTable[i + 1]),
								                           new CheckBox(getBaseContext())));
					}
				}
			}
        	
        }).start();
    }
}