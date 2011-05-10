package com.shane87.controlfreak;

import com.shane87.controlfreak.R;
import com.shane87.controlfreak.ShellInterface;

import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListAdapter;
import android.widget.Spinner;
import android.widget.Toast;

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
	protected static final String C_CUR_GOVERNOR = "toolbox cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
	
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
	private int 						cpuThres;
	private String						curGovernor;
	private String						timeInState;
	private boolean						statesAvailable = false;
	
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
				showSchedHelp();
				
			}
		});
        
        //next, the freqButton
        freqButton.setOnClickListener(new View.OnClickListener()
        {
			
			@Override
			public void onClick(View v)
			{
				//Call our showFreqLimitHelp() function to display the frequency limit help
				showFreqLimitHelp();
				
			}
		});
        
        //next, the cpuTButton
        cpuTButton.setOnClickListener(new View.OnClickListener()
        {
			
			@Override
			public void onClick(View v)
			{
				//Call our showThresLimitHelp() function to display the cpu threshold help
				showThresLimitHelp();
				
			}
		});
        
        //finally, the govButton
        govButton.setOnClickListener(new View.OnClickListener()
        {
			
			@Override
			public void onClick(View v)
			{
				//Call our showGovLimitHelp() function to display the governor help
				showGovLimitHelp();
				
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
							cpuThres = (int)arg0.getSelectedItemId();
							
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
        			//as long as cpuThres is non-negative, the kernel supports cpuThresh settings
        			//so we will set the spinner to the correct setting
        			if(cpuThres >= 0)
        				cpuTSpinner.setSelection(cpuThres);
        			
        			//if cpuThres is negative, either the kernel does not support cpuThresh settings
        			//or we weren't able to get them, so we will hide the spinner, button, and text
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
        			String maxCpu = ShellInterface.getProcessOutput(C_SCALING_MAX_FREQ);
        			//dont forget to take 4 digits from the end, since we are
        			//simply getting the contents of scaling_max_frequency file, which has the
        			//currently set max frequency in khz
        			maxCpu = maxCpu.substring(0, maxCpu.length() - 4);
        			
        			for(int i = 0; i < freqSpinner.getCount(); i++)
        			{
        				if(freqSpinner.getItemAtPosition(i).toString().matches(maxCpu + " Mhz"))
        				{
        					freqSpinner.setSelection(i);
        					break;
        				}
        			}
        			for(int i = 0; i < govSpinner.getCount(); i++)
        				if(govSpinner.getItemAtPosition(i).toString().matches(curGovernor))
        				{
        					govSpinner.setSelection(i);
        					break;
        				}
        			
        			break;
        		}
        		case NOROOT:
        		{
        			showNoRootAlert();
        			break;
        		}
        		case WRONGKERNEL:
        		{
        			showWrongKernelAlert();
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
						//we start by calling getFreqVoltTable, which will pull all of our frequencies
						//and their voltage settings and store them in our stockVoltages global
						getFreqVoltTable();
						
						//now lets get our frequency list and tis info from getTimeInState()
						String[] freqTable = getTimeInState();
						
						//then we can pass our tester string and the info returned above to store our
						//frequencies and voltages in or fqStatsList
						getFreqTable(tester, freqTable);
						
						//we need to check our states enabled table, to find out which states are
						//enabled. this will let us set the initial state of the states checkboxes
						//appropriately
						getStates();
						
						try
						{
							//lets make our scheduler script to make setting scheduler changes easier
							OutputStreamWriter out = new OutputStreamWriter(openFileOutput("sched.sh", 0));
							out.write("#!/system/bin/sh\n# Set \"$1\" scheduler for stl, bml and mmc\nfor i in `ls /sys/block/stl*` /sys/block/bml* /sys/block/mmcblk*\ndo\necho \"$1\" > $i/queue/scheduler\ndone");
							out.close();
						}catch(java.io.IOException e){}
						
						//and don't forget to chmod it, or it won't execute!!
						ShellInterface.runCommand("chmod 777 /data/data/com.shane87.controlfreak/" +
												  "files/sched.sh");
						
						//now lets start setting up our spinner adapters, starting with freq
						//first, we let android know what kind of spinner we will be attaching this to
						adapterForFreqSpinner.setDropDownViewResource(
								android.R.layout.simple_spinner_dropdown_item);
						
						//now we loop through all of our frequencies, and add the enabled states to the
						//spinner
						for(int i = 0; i < fqStatsList.size(); i++)
							if(fqStatsList.get(i).getEnabled())
								adapterForFreqSpinner.add(fqStatsList.get(i).getValue() + " mHz");
						
						//while we are on the subject of frequencies, lets go ahead and get our
						//max frequency setting
						maxFreq = ShellInterface.getProcessOutput(C_SCALING_MAX_FREQ);
						//if we get a null string, we have root problems and wouldn't
						//ever make it this far, but it is always best to check for the
						//worst case scenario
						if(maxFreq == null)
							maxFreq = new String("");
						if(maxFreq.equals(""))
							maxFreq.concat("0 0");
						
						//trim the last four digits, since the max freq file stores
						//the frequency in kHz instead of mHz
						maxFreq = maxFreq.substring(0, maxFreq.length() - 4);
						
						//now, the same thing for schedulers, first set the proper spinner type
						adapterForSchedSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						//now lets get what spinners we have available, and the currently
						//enabled one as well
						String schedTableTmp = ShellInterface.getProcessOutput("cat /sys/block" +
								                                            "/mmcblk0/queue/scheduler");
						
						//split individual entries into an array
						schedTable = schedTableTmp.split(" ");
						//then loop through the array
						for(int i = 0; i < schedTable.length; i++)
						{
							//if the entry contains "[", it is our active scheduler, so lets note that
							if(schedTable[i].contains("["))
								activeSched = i;
							//then add all of the schedulers to the adapter
							adapterForSchedSpinner.add(schedTable[i]);
						}
						
						//and now for the cpuThresh settings
						//again, let android know what kind of spinner item it is
						adapterForCpuTSpinner.setDropDownViewResource(
								android.R.layout.simple_spinner_dropdown_item);
						//then add the three supported settings
						adapterForCpuTSpinner.add("Stock");
						adapterForCpuTSpinner.add("Performance");
						adapterForCpuTSpinner.add("Battery Saver");
						
						//now lets find out if we support cpuThres, and if so, what one we have set
						String cpuThresTmp = ShellInterface.getProcessOutput(
								"cat /sys/devices/system/cpu/cpu0/cpufreq/cpu_thres_table");
						//if we get an empty string, we either dont have cpuThres available, or we
						//cant get the settings, so lets set cpuThres to a negative number
						if(cpuThresTmp.equals(""))
							cpuThres = -1;
						else
						{
							//otherwise, lets go on and split the cpuThres values out into an array
							String[] cpuThresVals = cpuThresTmp.split(" ");
							//lets assume a default value of "Stock"
							cpuThres = 0;
							//if the first value is 30, we have performance settings, so lets note that
							if(cpuThresVals[0].equals("30"))
								cpuThres = 1;
							//else if, the first and third settings are 55, we have battery
							//saver settings, so lets note that
							else if(cpuThresVals[0].equals("55") && cpuThresVals[2].equals("55"))
								cpuThres = 2;
							
							try
							{
								//since we know we have cpuThres available, lets make our cpuThres apply
								//scripts, starting with performance settings
								OutputStreamWriter out = new OutputStreamWriter(openFileOutput(
										"cpuT_performance.sh", 0));
								out.write("#! /system/bin/sh\n" +
										  "#Set Performace values to cpu_thres_table\n" +
										  "echo 30 70 30 70 30 70 30 70 30 70 30 70 " +
										  "30 70 30 70 30 70 30 70 30 70 30 70 30 70" +
										  " > /sys/devices/system/cpu/cpu0/cpufreq/cpu_thres_table");
								out.close();
							}
							catch(java.io.IOException e)
							{
							}
							//and lets make it executable
							ShellInterface.runCommand("chmod 777 /data/data/com.shane87.controlfreak/files/cpuT_performance.sh");
							try
							{
								//now for the battery settings
								OutputStreamWriter out = new OutputStreamWriter(openFileOutput(
										"cpuT_battery.sh", 0));
								out.write("#! /system/bin/sh\n" +
										  "#Set Battery Saver values to cpu_thres_table\n" +
										  "echo 55 80 55 90 55 90 55 90 55 90 55 90 " +
										  "60 80 60 80 60 80 60 80 60 80 60 80 60 80" +
										  " > /sys/devices/system/cpu/cpu0/cpufreq/cpu_thres_table");
								out.close();
							}
							catch(java.io.IOException e)
							{
							}
							ShellInterface.runCommand("chmod 777 /data/data/com.shane87.controlfreak/files/cpuT_battery.sh");
							try
							{
								//and finally the stock settings
								OutputStreamWriter out = new OutputStreamWriter(openFileOutput(
										"cpuT_stock.sh", 0));
								out.write("#! /system/bin/sh\n" +
										  "#Set Stock values to cpu_thres_table\n" +
										  "echo 55 80 50 90 50 90 50 90 40 90 40 90 " +
										  "30 80 20 70 20 70 20 70 20 70 20 70 20 70" +
										  " > /sys/devices/system/cpu/cpu0/cpufreq/cpu_thres_table");
								out.close();
							}
							catch(java.io.IOException e)
							{
							}
							ShellInterface.runCommand("chmod 777 /data/data/com.shane87.controlfreak/files/cpuT_stock.sh");
						}
					}
					
					//finally, lets get our list of available governors
					String availGov = ShellInterface.getProcessOutput(C_GOVERNORS_AVAILABLE);
					curGovernor = ShellInterface.getProcessOutput(C_CUR_GOVERNOR);
					String[] availGovAr = availGov.split(" ");
					
					for(int i = 0; i < availGovAr.length; i++)
						adapterForGovSpinner.add(availGovAr[i]);
					
					//now that we have our info gathered, lets let the ui refresh
					uiRefreshHandler.sendEmptyMessage(REFRESH);
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
				int freq, uv, tis;
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
						freq = Integer.parseInt(freqTable[i]) / 1000;
						uv = Integer.parseInt(uvTable[i / 2]);
						tis = Integer.parseInt(freqTable[i + 1]);
						fqStatsList.add(new FrequencyStats(freq, uv, tis, new CheckBox(getBaseContext())));
					}
				}
			}
			
			private String[] getTimeInState()
			{
				timeInState = ShellInterface.getProcessOutput(C_TIME_IN_STATE);
				
				if(timeInState == null)
					timeInState = "";
				if(timeInState == "")
					timeInState.concat("0 0");
				
				String[] freqTable = timeInState.split(" ");
				return freqTable;
			}
			
			private void getFreqVoltTable()
			{
				String freqVoltTable = ShellInterface.getProcessOutput(C_FREQUENCY_VOLTAGE_TABLE);
				
				if(freqVoltTable == "")
				{
					stockVoltages.put("100", "950");
					stockVoltages.put("200", "950");
					stockVoltages.put("400", "1050");
					stockVoltages.put("800", "1200");
					stockVoltages.put("1000", "1275");
					stockVoltages.put("1120", "1300");
					stockVoltages.put("1200", "1300");
				}
				else
				{
					String[] tmpFreqVoltTable = freqVoltTable.split(" ");
					String[] freqTable = new String[20];
					String[] voltTable = new String[20];
					
					for(int i = 0, j = 0; i < tmpFreqVoltTable.length; i += 3, j++)
					{
						freqTable[j] = String.valueOf(tmpFreqVoltTable[i]);
						voltTable[j] = String.valueOf(tmpFreqVoltTable[i + 1]);
						stockVoltages.put(freqTable[j].substring(0, freqTable[j].length() - 3),
								voltTable[j]);
					}
				}
			}
			
			private boolean getStates()
			{
				String statesEnabledTemp = ShellInterface.getProcessOutput(C_STATES_ENABLED);
				
				try
				{
					String[] statesEnable = statesEnabledTemp.split(" ");
					for(int i = 0; i < fqStatsList.size(); i++)
					{
						if(statesEnable[i].equals("1"))
							fqStatsList.get(i).setEnabled(true);
						else
							fqStatsList.get(i).setEnabled(false);
					}
				}catch(Exception ignored){return false;}
				
				statesAvailable = true;
				return true;
			}
        	
        }).start();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	mMenu = menu;
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.layout.menu, mMenu);
    	
    	return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case (R.id.exit): {
			this.finish();
			return true;
		}
		case (R.id.apply): {
			applySettings();
			return true;
		}
		case (R.id.boot): {
			saveBootSettings();
			return true;
		}
		case (R.id.noboot): {
			deleteBootSettings();
			return true;
		}
		case (R.id.about): {
			showAboutScreen();
			return true;
		}
		}
		return true;

	}
    public void getTimes()
    {
    	String timesTmp = ShellInterface.getProcessOutput(C_TIME_IN_STATE);
    	if(timesTmp == null)
    		return;
    	else
    	{
    		String[] times = timesTmp.split(" ");
    		for(int i = 0, j = 1; i < fqStatsList.size(); i++, j += 2)
    		{
    			fqStatsList.get(i).setTIS(Integer.valueOf(times[j]));
    		}
    	}
    }
    
    private void applySettings()
    {
    	ShellInterface.runCommand(buildUVCommand());
    	ShellInterface.runCommand("/data/data/com.shane87.controlfreak/files/sched.sh "
    			+ schedTable[activeSched]);
    	ShellInterface.runCommand("echo \"" + maxFreq.split(" ")[0] 
    	                        + "000\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq");
    	setStates();
    	if(cpuThres == 0)
    		ShellInterface.runCommand("/data/data/com.shane87.controlfreak/files/cpuT_stock.sh");
    	else if(cpuThres == 1)
    		ShellInterface.runCommand("/data/data/com.shane87.controlfreak/files/cpuT_performance.sh");
    	else if(cpuThres == 2)
    		ShellInterface.runCommand("/data/data/com.shane87.controlfreak/files/cpuT_battery.sh");
    	ShellInterface.runCommand("echo \"" + curGovernor + "\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
    	ShellInterface.runCommand("echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/update_states");
    }
    
    private StringBuilder buildStatesEnabledCommand()
    {
    	StringBuilder command = new StringBuilder();
    	command.append("echo \"");
    	
    	for(int i = 0; i < fqStatsList.size(); i++)
    	{
    		if(fqStatsList.get(i).getEnabled())
    			command.append("1 ");
    		else
    			command.append("0 ");
    	}
    	
    	command.append("\" > /sys/devices/system/cpu/cpu0/cpufreq/states_enabled_table");
    	return command;
    }
    
    private String buildUVCommand()
    {
    	StringBuilder command = new StringBuilder();
    	command.append("echo \"");
    	
    	for(int i = 0; i < fqStatsList.size(); i++)
    		command.append(fqStatsList.get(i).getUV() + " ");
    	
    	command.append("\" > /sys/devices/system/cpu/cpu0/cpufreq/UV_mV_table");
    	return command.toString();
    }
    
    private void setStates()
    {
    	StringBuilder command = buildStatesEnabledCommand();
    	
    	ShellInterface.runCommand(command.toString());
    }
    
    private void deleteBootSettings() 
    {
		if (!ShellInterface.getProcessOutput("ls /etc/init.d/").contains("S_volt_scheduler")) 
			Toast.makeText(this, "No settings file present!", Toast.LENGTH_SHORT).show();
		else
		{
			ShellInterface.runCommand("busybox mount -o remount,rw  /system");
			ShellInterface.runCommand("rm /etc/init.d/S_volt_scheduler");
			ShellInterface.runCommand("busybox mount -o remount,ro  /system");
			Toast.makeText(this, "Settings deleted!", Toast.LENGTH_SHORT).show();
		}
	}
    
    private void saveBootSettings() {
		try 
		{
			OutputStreamWriter out = new OutputStreamWriter(openFileOutput("S_volt_scheduler", 0));
			String tmp = "#!/system/bin/sh\n\nLOG_FILE=/data/volt_scheduler.log\nrm -Rf $LOG_FILE\n\necho \"Starting Insanity Volt Scheduler $( date +\"%m-%d-%Y %H:%M:%S\" )\" | tee -a $LOG_FILE;\n\necho \"Set UV\" | tee -a $LOG_FILE; \n"
					+ buildUVCommand()
					+ "\necho \"\"\necho \"---------------\"\n\necho \"Set MAX Scaling Frequency\" | tee -a $LOG_FILE; \necho \""
					+ maxFreq.split(" ")[0]
					+ "000\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq\necho \"\"\necho \"---------------\"\n\necho \"Select Enabled States\" | tee -a $LOG_FILE; \n"
					+ buildStatesEnabledCommand()
					+ "\necho \"\"\necho \"---------------\"\n\necho \"Set Scheduler for stl, bml and mmc\" | tee -a $LOG_FILE; \n    \nfor i in `ls /sys/block/stl*` /sys/block/bml* /sys/block/mmcblk* ; do\n\techo \""
					+ schedTable[activeSched]
					+ "\" > $i/queue/scheduler;\n\techo \"$i/queue/scheduler\";\n\techo \"---------------\";\ndone;\n\necho \"Insanity Volt Scheduler finished at $( date +\"%m-%d-%Y %H:%M:%S\" )\" | tee -a $LOG_FILE;\n";
			
			if(cpuThres == 0)
				tmp = tmp.concat("\necho \"Setting stock cpu_thres values!\" | tee -a $LOG_FILE;" +
						"\necho \"55 80 50 90 50 90 50 90 40 90 40 90 " +
						"30 80 20 70 20 70 20 70 20 70 20 70 20 70\"" +
						" > /sys/devices/system/cpu/cpu0/cpufreq/cpu_thres_table" +
						"\necho \"Settings saved!\" | tee -a $LOG_FILE;\n");
			else if(cpuThres == 1)
				tmp = tmp.concat("\necho \"Setting performance cpu_thres values!\" | tee -a $LOG_FILE;" +
						"\necho \"30 70 30 70 30 70 30 70 30 70 30 70 " +
						"30 70 30 70 30 70 30 70 30 70 30 70 30 70\"" +
						" > /sys/devices/system/cpu/cpu0/cpufreq/cpu_thres_table" +
						"\necho \"Settings saved!\" | tee -a $LOG_FILE;\n");
			else if(cpuThres == 2)
				tmp = tmp.concat("\necho \"Setting battery saver cpu_thres values!\" | tee -a $LOG_FILE;" +
						"\necho \"55 80 55 90 55 90 55 90 55 90 55 90 " +
						"60 80 60 80 60 80 60 80 60 80 60 80 60 80\"" +
						" > /sys/devices/system/cpu/cpu0/cpufreq/cpu_thres_table" +
						"\necho \"Settings saved!\" | tee -a $LOG_FILE;\n");
			
			tmp = tmp.concat("\necho \"Setting current governor\" | tee -a $LOG_FILE;\n" +
							 "echo \"" + curGovernor + "\" >" +
							 " /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor\n" +
							 "echo \"Current Governor Set\" | tee -a $LOG_FILE;\n");
			out.write(tmp);
			out.close();
		} catch (java.io.IOException e) {
			Toast.makeText(this, "ERROR: file not saved!", Toast.LENGTH_LONG)
					.show();
		}

		ShellInterface.runCommand("chmod 777 /data/data/com.shane87.controlfreak/files/S_volt_scheduler");
		ShellInterface.runCommand("busybox mount -o remount,rw  /system");
		ShellInterface.runCommand("mkdir /etc/init.d");
		ShellInterface.runCommand("busybox cp /data/data/com.shane87.controlfreak/files/S_volt_scheduler /etc/init.d/S_volt_scheduler");
		ShellInterface.runCommand("busybox mount -o remount,ro  /system");
		Toast.makeText(this, "Settings saved in file \"/etc/init.d/S_volt_scheduler\"", Toast.LENGTH_LONG).show();
	}
    
    private void showAboutScreen() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.app_name);
		builder.setMessage(R.string.aboutText);
		builder.setTitle("About Control Freak "
				+ getResources().getText(R.string.version));
		builder.setNegativeButton("Back",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {
					}
				});
		builder.setNeutralButton("Visit us on XDA",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {
						String url = "http://forum.xda-developers.com/showthread.php?t=1030994";
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse(url));
						startActivity(i);
					}
				});
		builder.setPositiveButton("Donate to author",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {
						String url = "http://forum.xda-developers.com/donatetome.php?u=3482571";
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse(url));
						startActivity(i);
					}
				});

		AlertDialog alert = builder.create();

		alert.show();
	}
    
    private void showNoRootAlert() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.nosuText);
		builder.setNegativeButton("Exit",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {
						finish();
					}
				});
		builder.setNeutralButton("More info",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {
						String url = "http://forum.xda-developers.com/showthread.php?t=1030994";
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse(url));
						startActivity(i);
						finish();
					}
				});
		builder.setTitle("No root available");
		builder.setCancelable(false);
		AlertDialog alert = builder.create();

		alert.show();
	}
    
    private void showSchedHelp() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.schedHelpText);
				builder.setTitle(R.string.schedHelpHeader);
				builder.setNeutralButton("Ok", null);
		builder.setCancelable(true);
		AlertDialog alert = builder.create();
		alert.show();
	}
    
    private void showFreqLimitHelp() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.freqHelpText);
				builder.setTitle(R.string.freqHelpHeader);
				builder.setNeutralButton("Ok", null);
		builder.setCancelable(true);
		AlertDialog alert = builder.create();
		alert.show();
	}
    
    private void showThresLimitHelp()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.cpuThresHelp);
				builder.setTitle(R.string.cpuThresHelpHeader);
				builder.setNeutralButton("Ok", null);
		builder.setCancelable(true);
		AlertDialog alert = builder.create();
		alert.show();
	}
    
    private void showGovLimitHelp()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.govHelpText);
				builder.setTitle(R.string.govHelpText);
				builder.setNeutralButton("Ok", null);
		builder.setCancelable(true);
		AlertDialog alert = builder.create();
		alert.show();
	}
    
    private void showWrongKernelAlert() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.bkernelText);
		builder.setNegativeButton("Exit",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {
						finish();
					}
				});
		builder.setNeutralButton("Get kernel from XDA",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {
						String url = "http://forum.xda-developers.com/showthread.php?t=930679";
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse(url));
						startActivity(i);
						finish();
					}
				});
		builder.setTitle("Unsupported kernel detected");
		builder.setCancelable(false);
		AlertDialog alert = builder.create();

		alert.show();
	}
}