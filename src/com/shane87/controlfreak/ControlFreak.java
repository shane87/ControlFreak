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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.Spinner;
import android.widget.TextView;
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
	//Command to check availability of Governor tweaks, based on governor selected
	protected static final String C_GOV_TWEAKS_AVAIL = "ls /sys/devices/system/cpu/cpufreq";
	
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
	private ArrayAdapter<String> 		adapterForFreqSpinner;
	private OutputStreamWriter			out;
	//a bool variable to let functions know that we are currently loading settings
	//this is used to let the updateFreqSpinner() know that we do NOT want to change
	//the max cpu limit during start up
	private boolean 					loading;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	//Write to our log file that we are starting up
    	log("ControlFreak starting");
    	//Request the orientation to stay vertical
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
    	//Call the superclass onCreate method
        super.onCreate(savedInstanceState);
        
        //set the contentview to main
        setContentView(R.layout.main);
        
        //lets setup the arrays for our spinners
        final ArrayAdapter<String> adapterForSchedSpinner = new ArrayAdapter<String>(this,
        														android.R.layout.simple_spinner_item);
        final ArrayAdapter<String> adapterForCpuTSpinner  = new	ArrayAdapter<String>(this,
																android.R.layout.simple_spinner_item);
        final ArrayAdapter<String> adapterForGovSpinner   = new ArrayAdapter<String>(this,
																android.R.layout.simple_spinner_item);
        adapterForFreqSpinner   = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        
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
        
        //lets setup our Sliding Drawer
        SlidingDrawer slider;
        slider = (SlidingDrawer)findViewById(R.id.SlidingDrawer);
        Button sliderButton;
        sliderButton = (Button)findViewById(R.id.SliderButton);
        
        slider.setOnDrawerOpenListener(new OnDrawerOpenListener()
        {

			@Override
			public void onDrawerOpened() 
			{	
			}
        	
        });
        
        slider.setOnDrawerCloseListener(new OnDrawerCloseListener()
        {

			@Override
			public void onDrawerClosed() 
			{
			}
        	        	
        });
        //as a last step before we go to our initialization code, lets get our frequency list adapter
        frequencyAdapter = new FrequencyListAdapter(this, this.getApplicationContext());
        
        //Lets get a holder for our async task class, and tell it to start working
        log("Starting AsyncTask");
        @SuppressWarnings("unchecked")
		AsyncTask<ArrayAdapter<String>, Void, Integer> init = new initializer().execute(adapterForSchedSpinner, adapterForFreqSpinner, adapterForCpuTSpinner, adapterForGovSpinner);
        
        //lets create a variable for our asyncTask return value
        int ret = -1;
        //now we will get the return code once our background task completes
        //we put it in a try/catch block since the get() function can throw exceptions
        try
        {
        	ret = init.get();
        }catch(Exception Igonored){}
        
        //now that we have our return value, we will switch on it, to either update the ui, or to
        //let the user know we had errors with no su or wrong kernel
        switch(ret)
        {
        case REFRESH:
        {
       
        	updateDrawer();
       
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
        			//updateDrawer();

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
        		if(freqSpinner.getItemAtPosition(i).toString().matches(maxCpu + " MHz"))
        		{
        			freqSpinner.setSelection(i);
        			break;
        		}
        	}
        	for(int i = 0; i < govSpinner.getCount(); i++)
        		if(curGovernor.contains(govSpinner.getItemAtPosition(i).toString()))
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
    
    //lets setup our menu when the user presses the menu button
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	//copy the menu value into our mMenu global, so we can reference it later
    	mMenu = menu;
    	//now lets get an inflater for our menu
    	MenuInflater inflater = getMenuInflater();
    	//and inflate the main menu
    	inflater.inflate(R.layout.menu, mMenu);
    	
    	return true;
    }
    
    //this controls our menu interface, to complete the actions the user selects
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	//ok, we will switch on the id of the menu item selected, so we know what to o
		switch (item.getItemId()) {
		case (R.id.exit): {
			//if they press exit, we will simply call finish() on our app, which will let the
			//os know it can destroy our app and free up its used memory
			this.finish();
			return true;
		}
		case (R.id.apply): {
			//if they press apply, we will call applySettings() to make the current settings
			//active
			applySettings();
			return true;
		}
		case (R.id.boot): {
			//if they press Save as Boot Settings, we will call saveBootSettings() to write the current
			//settings to S_volt_scheduler and copy it to /etc/init.d
			saveBootSettings();
			return true;
		}
		case (R.id.noboot): {
			//if they press Delete Boot Settings, we will call deleteBootSettings() to remove
			//S_volt_scheduler from /etc/init.d
			deleteBootSettings();
			return true;
		}
		case (R.id.about): {
			//if they press About, we will call showAboutScreen() to build the About dialog
			//and display it to the user
			showAboutScreen();
			return true;
		}
		case(R.id.exportlog):
		{
			exportLog();
			Toast.makeText(getApplicationContext(), "cf.log exported!", Toast.LENGTH_LONG);
			break;
		}
		}
		return true;

	}
    
    //this will get the newest tis info. not sure if we still need this, since the
    //app doesn't really have a way to call for an update. this function was
    //used in VCEX to update the times every time the States drawer was opened
    //since we have no States drawer, there is currently no way to update tis info,
    //but i left this in, because I might add a main menu item to allow users
    //to update the tis info. works basically the same as getTimeInStates() below, but
    //uses an if/else block instead of a try/catch block, and it also calls setTIS() for
    //each entry in fqStatsList, instead of putting in the value in a different function
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
    
    //this is called when the user presses Apply Settings for Now
    private void applySettings()
    {
    	//first, set the uv settings, by calling buildUVCommand() and passing its string
    	//to ShellInterface
    	ShellInterface.runCommand(buildUVCommand());
    	//Then we will set our scheduler, by calling our sched script, and passing it the
    	//scheduler we want to activate
    	ShellInterface.runCommand("/data/data/com.shane87.controlfreak/files/sched.sh "
    			+ schedTable[activeSched]);
    	//Now we put our max cpu limit in the correct file
    	ShellInterface.runCommand("echo \"" + maxFrequency.split(" ")[0] 
    	                        + "000\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq");
    	//Call setStates() to echo the states info to the proper file
    	setStates();
    	//Call the appropriate cpuT script, if we have cpu threshold settings active
    	if(cpuThres == 0)
    		ShellInterface.runCommand("/data/data/com.shane87.controlfreak/files/cpuT_stock.sh");
    	else if(cpuThres == 1)
    		ShellInterface.runCommand("/data/data/com.shane87.controlfreak/files/cpuT_performance.sh");
    	else if(cpuThres == 2)
    		ShellInterface.runCommand("/data/data/com.shane87.controlfreak/files/cpuT_battery.sh");
    	//Echo our currently selected governor to the correct file
    	ShellInterface.runCommand("echo \"" + curGovernor + "\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
    	//finally, echo 1 to the update_states file, so the kernel knows to use our new
    	//settings
    	ShellInterface.runCommand("echo 1 > /sys/devices/system/cpu/cpu0/cpufreq/update_states");
    }
    
    //this function builds our states enabled command, which is used by setStates() to set the
    //states enabled correctly
    private StringBuilder buildStatesEnabledCommand()
    {
    	//make our string builder, and put in the echo command
    	StringBuilder command = new StringBuilder();
    	command.append("echo \"");
    	
    	//loop through the fqStatsList and put a 1 for enabled states and a 0 for disabled
    	//states
    	for(int i = 0; i < fqStatsList.size(); i++)
    	{
    		if(fqStatsList.get(i).getEnabled())
    			command.append("1 ");
    		else
    			command.append("0 ");
    	}
    	
    	//finally, add the path to the states_enabled_table file and return our string builder
    	command.append("\" > /sys/devices/system/cpu/cpu0/cpufreq/states_enabled_table");
    	return command;
    }
    
    //this function builds our uv command, to echo the uv setings to the correct file
    private String buildUVCommand()
    {
    	//get our string builder, and add echo
    	StringBuilder command = new StringBuilder();
    	command.append("echo \"");
    	
    	//add our uv settings, by looping through fqStatsList and getting the uv values
    	for(int i = 0; i < fqStatsList.size(); i++)
    		command.append(fqStatsList.get(i).getUV() + " ");
    	
    	//finally, add the path to the UV_mV_table, and return the string from the builder
    	command.append("\" > /sys/devices/system/cpu/cpu0/cpufreq/UV_mV_table");
    	return command.toString();
    }
    
    //this function simply calls the buildStatesEnabled() function, then passes that string
    //to the shellInterface to set the states
    private void setStates()
    {
    	StringBuilder command = buildStatesEnabledCommand();
    	
    	ShellInterface.runCommand(command.toString());
    }
    
    //this is called when the user presses delete boot settings
    private void deleteBootSettings() 
    {
    	//if there is no S_volt_scheduler file, there is nothing to delete
    	//so we let the user know
		if (!ShellInterface.getProcessOutput("ls /etc/init.d/").contains("S_volt_scheduler")) 
			Toast.makeText(this, "No settings file present!", Toast.LENGTH_SHORT).show();
		else
		{
			//if there is a S_volt_scheduler file, we need to remount /system as rw, since
			// /etc is simply a symlink to /system/etc
			ShellInterface.runCommand("busybox mount -o remount,rw  /system");
			//then we can delete S_volt_scheduler
			ShellInterface.runCommand("rm /etc/init.d/S_volt_scheduler");
			//finally, remount /system as ro, and let the user know it is deleted
			ShellInterface.runCommand("busybox mount -o remount,ro  /system");
			Toast.makeText(this, "Settings deleted!", Toast.LENGTH_SHORT).show();
		}
	}
    
    //this is called when the user presses save as boot settings
    private void saveBootSettings() {
		try 
		{
			//lets get an output stream writer, and create S_volt_scheduler
			OutputStreamWriter out = new OutputStreamWriter(openFileOutput("S_volt_scheduler", 0));
			//now lets create a tmp string with all of our settings in it, except for cpuThresh
			//and the governor settings, they are added later
			String tmp = "#!/system/bin/sh\n\nLOG_FILE=/data/volt_scheduler.log\nrm -Rf $LOG_FILE\n\necho \"Starting Insanity Volt Scheduler $( date +\"%m-%d-%Y %H:%M:%S\" )\" | tee -a $LOG_FILE;\n\necho \"Set UV\" | tee -a $LOG_FILE; \n"
					+ buildUVCommand()
					+ "\necho \"\"\necho \"---------------\"\n\necho \"Set MAX Scaling Frequency\" | tee -a $LOG_FILE; \necho \""
					+ maxFreq.split(" ")[0]
					+ "000\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq\necho \"\"\necho \"---------------\"\n\necho \"Select Enabled States\" | tee -a $LOG_FILE; \n"
					+ buildStatesEnabledCommand()
					+ "\necho \"\"\necho \"---------------\"\n\necho \"Set Scheduler for stl, bml and mmc\" | tee -a $LOG_FILE; \n    \nfor i in `ls /sys/block/stl*` /sys/block/bml* /sys/block/mmcblk* ; do\n\techo \""
					+ schedTable[activeSched]
					+ "\" > $i/queue/scheduler;\n\techo \"$i/queue/scheduler\";\n\techo \"---------------\";\ndone;\n\necho \"Insanity Volt Scheduler finished at $( date +\"%m-%d-%Y %H:%M:%S\" )\" | tee -a $LOG_FILE;\n";
			
			//now add the cpuThresh settings, if they are available
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
			
			//finally, add the governor settings
			tmp = tmp.concat("\necho \"Setting current governor\" | tee -a $LOG_FILE;\n" +
							 "echo \"" + curGovernor + "\" >" +
							 " /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor\n" +
			
			"echo \"Current Governor Set\" | tee -a $LOG_FILE;\n");
			//write the tmp string to the file and close the file
			out.write(tmp);
			out.close();
		} catch (java.io.IOException e) {
			//if there is a problem, let the user know
			Toast.makeText(this, "ERROR: file not saved!", Toast.LENGTH_LONG)
					.show();
		}

		//now we chmod the script to make it executable
		ShellInterface.runCommand("chmod 777 /data/data/com.shane87.controlfreak/files/S_volt_scheduler");
		//mount system as rw
		ShellInterface.runCommand("busybox mount -o remount,rw  /system");
		//make the /etc/init.d folder if it is not already there
		ShellInterface.runCommand("mkdir /etc/init.d");
		//copy the S_volt_scheduler file to init.d
		ShellInterface.runCommand("busybox cp /data/data/com.shane87.controlfreak/files/S_volt_scheduler /etc/init.d/S_volt_scheduler");
		//mount system as ro
		ShellInterface.runCommand("busybox mount -o remount,ro  /system");
		//let the user know we saved the file
		Toast.makeText(this, "Settings saved in file \"/etc/init.d/S_volt_scheduler\"", Toast.LENGTH_LONG).show();
	}
    
    //called when the user presses About
    private void showAboutScreen() {
    	//get a new AlertDialog builder
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		//set the title (for some reason it is set twice, not sure, may remove it)
		builder.setTitle(R.string.app_name);
		//set the message, and the title again
		builder.setMessage(R.string.aboutText);
		builder.setTitle("About Control Freak "
				+ getResources().getText(R.string.version));
		//create the negative button, to close the dialog, along with the onClickListener
		//which does nothing so that the dialog will close
		builder.setNegativeButton("Back",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {
					}
				});
		//set the neutral button, and the onClickListener, which will create an intent
		//with the url to my xda thread so the user can visit me on xda
		builder.setNeutralButton("Visit us on XDA",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {
						String url = "http://forum.xda-developers.com/showthread.php?t=1072403";
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse(url));
						startActivity(i);
					}
				});
		//set the positive button, with the onClickListener which will create an intent
		//with the url to my xda donate button, so the user can make a donation
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

		//create the alert dialog
		AlertDialog alert = builder.create();

		//show the alert dialog
		alert.show();
	}
    
    //called when the user has no root access
    private void showNoRootAlert() {
    	//create an alert builder
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		//set the message
		builder.setMessage(R.string.nosuText);
		//set the negative buton to exit the app, along with the onClickListener
		builder.setNegativeButton("Exit",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {
						finish();
					}
				});
		//set the neutral button, with a link to my xda thread
		//also create the onClickListener
		builder.setNeutralButton("More info",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int id) {
						String url = "http://forum.xda-developers.com/showthread.php?t=1072403";
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse(url));
						startActivity(i);
						finish();
					}
				});
		//set the title
		builder.setTitle("No root available");
		//make it so that closing the dialog will always close the app
		builder.setCancelable(false);
		//create the dialog
		AlertDialog alert = builder.create();
		//show the dialog
		alert.show();
	}
    
    //called when the user presses the help button beside scheduler settings
    //exactly like above, except that there is no need for onClickListeners
    //and the dialog is cancelable
    private void showSchedHelp() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.schedHelpText);
				builder.setTitle(R.string.schedHelpHeader);
				builder.setNeutralButton("Ok", null);
		builder.setCancelable(true);
		AlertDialog alert = builder.create();
		alert.show();
	}
    
    //same as above, only for the frequency limit help button
    private void showFreqLimitHelp() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.freqHelpText);
				builder.setTitle(R.string.freqHelpHeader);
				builder.setNeutralButton("Ok", null);
		builder.setCancelable(true);
		AlertDialog alert = builder.create();
		alert.show();
	}
    
    //and again for the cpu threshold help button
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
    
    //and one last time for the governor help button
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
    
    //called when the user has an incompatible kernel
    //identical to the showNoRootAlert()
    //neutral button links to existz's Talon kernel thread
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
						String url = "http://forum.xda-developers.com/showthread.php?t=1050206";
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
    
    public void updateFreqSpinner(int id)
    {
    	if(fqStatsList.get(id).getEnabled())
    		adapterForFreqSpinner.add(String.valueOf(fqStatsList.get(id).getValue()) + " mHz");
    	else
    		adapterForFreqSpinner.remove(String.valueOf(fqStatsList.get(id).getValue()) + " mHz");
    }
    
    //working on adding some governor specific tweaks to a sliding drawer
    //not finished yet, and I have been more focused on getting the base app 
    //to run properly, so I am going to just have the sliding drawer hide itself
    //for now, until I get the base app straightened out. Then I will bring the sliding
    //drawer back, and start working on this again.
    private void updateDrawer()
    {
    	RelativeLayout rl = (RelativeLayout)findViewById(R.id.contentLayout);
		Context cont = getApplicationContext();
		
		findViewById(R.id.SlidingDrawer).setVisibility(View.INVISIBLE);
		/*
		String test = ShellInterface.getProcessOutput(C_GOV_TWEAKS_AVAIL);
		if(test == "")
		{
			findViewById(R.id.SlidingDrawer).setVisibility(View.INVISIBLE);
		}
		else
		{
			if(curGovernor.contains("interactive"))
			{
				findViewById(R.id.SlidingDrawer).setVisibility(View.VISIBLE);
				CheckBox cb;
				final EditText et;
				RelativeLayout.LayoutParams rllp;
				OnCheckedChangeListener listener;
				final String minValue;
				
				cb = new CheckBox(cont);
				et = new EditText(cont);
				rllp = new RelativeLayout.LayoutParams(475, RelativeLayout.LayoutParams.WRAP_CONTENT);
				minValue = ShellInterface.getProcessOutput("toolbox cat /sys/devices/system/cpu/cpufreq/interactive/min_sample_time");
				listener = new OnCheckedChangeListener()
				{

					@Override
					public void onCheckedChanged(
							CompoundButton buttonView,
							boolean isChecked) {
						et.setEnabled(isChecked);
						
					}
					
				};
				
				cb.setOnCheckedChangeListener(listener);
				cb.setText("Edit Min Sampling Time?");
				cb.setChecked(false);
				cb.setId(1);
				
				et.setText(minValue);
				et.setEnabled(false);
				
				rllp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				rl.addView(cb, rllp);
				
				rllp = new RelativeLayout.LayoutParams(475, RelativeLayout.LayoutParams.WRAP_CONTENT);
				rllp.addRule(RelativeLayout.BELOW, 1);
				rl.addView(et, rllp);
			}
			else
			{
				findViewById(R.id.SlidingDrawer).setVisibility(View.INVISIBLE);
			}
		}*/
    }
    
    private void log(String msg)
    {
    	try
    	{
    		out = new OutputStreamWriter(openFileOutput("cf.log", MODE_APPEND));
    		out.write("ControlFreak @ " + getTS() + ": " + msg + "\n");
    		out.close();
    	}
    	catch(Exception ignored){}
    }
    
    private String getTS()
    {
    	return Long.toString(System.currentTimeMillis());
    }
    
    private void exportLog()
    {
    	ShellInterface.runCommand("cp /data/data/com.shane87.controlfreak/files/cf.log /sdcard/cf.log");
    }
    
    private class initializer extends AsyncTask<ArrayAdapter<String>, Void, Integer>
    {    	
    	public ArrayAdapter<String> adapterForSchedSpinner;
    	public ArrayAdapter<String> adapterForFreqSpinner;
    	public ArrayAdapter<String> adapterForCpuTSpinner;
    	public ArrayAdapter<String> adapterForGovSpinner;
    	
    	protected Integer doInBackground(ArrayAdapter<String>... arrays)
    	{
    		Integer retVal = 0;
    		
    		adapterForSchedSpinner = arrays[0];
    		adapterForFreqSpinner = arrays[1];
    		adapterForCpuTSpinner = arrays[2];
    		adapterForGovSpinner = arrays[3];
    		
    		//first, lets set up a string to test the availability of su
			String tester = null;
			
			//if su is available, we will put the contents of uv_mv_table in the tester string
			//otherwise, tester will stay as null
			if(ShellInterface.isSuAvailable())
				tester = ShellInterface.getProcessOutput(C_UV_MV_TABLE);
			
			log("tester val: " + tester);
			
			//if tester is not null, we have su, so lets continue
			if(tester != null)
			{
				//if tester is not null, but is empty, this kernel does not support
				//uv/oc control in the right way, so lets send the WRONGKERNEL message
				//to let the user know
				if(tester == "")
					retVal = WRONGKERNEL;
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
							adapterForFreqSpinner.add(fqStatsList.get(i).getValue() + " MHz");
					
					//while we are on the subject of frequencies, lets go ahead and get our
					//max frequency setting
					maxFreq = ShellInterface.getProcessOutput(C_SCALING_MAX_FREQ);
					log("maxFreq val: " + maxFreq);
					//if we get a null string, we have root problems and wouldn't
					//ever make it this far, but it is always best to check for the
					//worst case scenario
					if(maxFreq == null)
						maxFreq = new String("");
					if(maxFreq.equals(""))
						maxFreq.concat("0 0");
					
					//trim the last three digits, plus the null terminator, since the max freq file stores
					//the frequency in kHz instead of mHz
					maxFreq = maxFreq.substring(0, maxFreq.length() - 4);
					
					//now, the same thing for schedulers, first set the proper spinner type
					adapterForSchedSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					//now lets get what spinners we have available, and the currently
					//enabled one as well
					String schedTableTmp = ShellInterface.getProcessOutput("cat /sys/block" +
							                                            "/mmcblk0/queue/scheduler");
					log("schedTableTmp val: " + schedTableTmp);
					
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
					log("cpuThresTmp val: " + cpuThresTmp);
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
				
				//lets set the correct type for our govSpinner adapter
				adapterForGovSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				//finally, lets get our list of available governors
				String availGov = ShellInterface.getProcessOutput(C_GOVERNORS_AVAILABLE);
				log("availGov val: " + availGov);
				curGovernor = ShellInterface.getProcessOutput(C_CUR_GOVERNOR);
				log("curGov val: " + curGovernor);
				String[] availGovAr = availGov.split(" ");
				
				for(int i = 0; i < availGovAr.length; i++)
					adapterForGovSpinner.add(availGovAr[i]);
				
				//now that we have our info gathered, lets let the ui refresh
				retVal = REFRESH;
			}
			//if tester IS null, we do not have su, so lets send the NOROOT message to the handler
			//so it can notify the user
			else
				retVal = NOROOT;
			
			//now we can dismiss our spinner dialog, then return
			return retVal;
		}
		
		//ok, this function is designed to take the tester string, which has our uv values in it,
		//and our freqTable array, which has our tis info and frequency info, and use these
		//values to set up our fqStatsList arraylist
		private void getFreqTable(String tester, String[] freqTable)
		{
			//a holder for our uv values, so we can split them down into individual values
			String[] uvTable;
			//lets store our tester string in or uvValues global variable, so we know what values
			//have been modified by the user when it comes time to apply/save the settings
			uvValues = tester;
			//some holders for the integer values of our strings
			//I added these to help track down why it was getting the wrong values for
			//frequency. as it turns out, SUBTRACTING 1000 does NOT give the same results
			//as I intended, since I intended to DIVIDE by 1000, lol
			int freq, uv, tis;
			
			int[] tisPerc = getTisPercent(freqTable, getDeepSleep());
			
			//if we don't have any uvValues. This should never happen, since
			//uvValues is loaded from tester, and if tester is null, we will never reach
			//this code. but we put it here just in case, to avoid null pointer exceptions
			if(uvValues == null)
				//if uvValues is null, lets make uvValues equal "" so we can trigger the next if
				uvValues = new String("");
			
			//again, this shouldn't happen, since a tester value of "" will cause an incompatible
			//kernel alert, and the above code, which would also trigger this, shouldnt happen either
			//but just in case:
			if(uvValues.equals(""))
			{
				//if we have no uv values, we will instantiate our fqStatsList array with 
				//FrequencyStats members with the following settings:
				//value of the frequency for each state
				//0 for uv value
				//value of the tis for each state
				for(int i = 0; i < freqTable.length; i += 2)
				{
					fqStatsList.add(new FrequencyStats(Integer.parseInt(freqTable[i]) / 1000,
							                           0, 
							                           Integer.parseInt(freqTable[i + 1]),
							                           tisPerc[i / 2],
							                           new CheckBox(getBaseContext())));
				}
			}
			//else, we have uvValues, so lets get the fqStatsList setup right
			else
			{
				//first, split the uvValues into individual strings, one for each freq
				uvTable = uvValues.split(" ");
				//now we loop through, pulling freq and tis from freqTable, and uv from uvTable
				//i is incremented by 2 for each step, since freqTable[0] is the first freq and
				//freqTable[1] is the tis for freqTable[0], freqTable[2] is the second freq, etc etc
				//by using i to reference the freq, i + 1 to reference the tis, and i / 2 to access
				//the the uv values, everything is pulled from the right place of each array
				for(int i = 0; i < freqTable.length; i += 2)
				{
					freq = Integer.parseInt(freqTable[i]) / 1000;
					uv = Integer.parseInt(uvTable[i / 2]);
					tis = Integer.parseInt(freqTable[i + 1]);
					//now that we have the int values of our settings, we can setup our
					//fqStatsList entry
					fqStatsList.add(new FrequencyStats(freq, uv, tis, tisPerc[i/2], new CheckBox(getBaseContext())));
				}
			}
			
			//Lets add a fqStatsList for DeepSleep
			fqStatsList.add(new FrequencyStats(0, 0, getDeepSleep(), tisPerc[freqTable.length], new CheckBox(getBaseContext())));
		}
		
		//this is used to get our time in state info, and our list of frequencies
		//the value returned by this is used by getFreqTable to setup our fqStatsList
		private String[] getTimeInState()
		{
			//ok, we will pull our tis string from the correct file, and store it in
			//our global timeInState variable
			timeInState = ShellInterface.getProcessOutput(C_TIME_IN_STATE);
			log("timeInState val: " + timeInState);
			
			//this should never happen, since the return from getProcessOutput() should only
			//be null when you have no root access, and that would be detected long before we get
			//here. but the only thing you can always be sure of on these phones is that they
			//will do as they please, so lets just be careful
			if(timeInState == null)
				timeInState = "";
			if(timeInState == "")
				timeInState.concat("0 0");
			
			//all that is left to do is spilt our timeInState string into an array, and return it
			String[] freqTable = timeInState.split(" ");
			return freqTable;
		}
		
		//this function sets up our stockVoltages map, so we can refer to their stock value
		//note that the "stock" value is actually their value when the app is launched, unless
		//the kernel does not support uv, in which case the app should let them know
		private void getFreqVoltTable()
		{
			//get our freq_volt_table, which has the frequencies and voltages listed
			String freqVoltTable = ShellInterface.getProcessOutput(C_FREQUENCY_VOLTAGE_TABLE);
			log("freqVoltTable val: " + freqVoltTable);
			
			//if we have no freq_volt_table, why are we still running?
			//but it is best to be on the safe side
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
			//otherwise, we have a freq_volt_table, so we will populate our stock voltages
			//map with the values we just got
			else
			{
				//first, lets split one string into an array, with one entry per index
				String[] tmpFreqVoltTable = freqVoltTable.split(" ");
				//temp holders to split the frequencies from the voltages, since they
				//are all mixed together in our first array
				String[] freqTable = new String[20];
				String[] voltTable = new String[20];
				
				//now we loop through our freqVoltTable, and store the frequencies and 
				//voltages as indicated above, then remove the last three zeros from the frequency
				//and store the values in our map
				for(int i = 0, j = 0; i < tmpFreqVoltTable.length; i += 3, j++)
				{
					freqTable[j] = String.valueOf(tmpFreqVoltTable[i]);
					voltTable[j] = String.valueOf(tmpFreqVoltTable[i + 1]);
					stockVoltages.put(freqTable[j].substring(0, freqTable[j].length() - 3),
							voltTable[j]);
				}
			}
		}
		
		//ok, in this function, we will get our states enabled info, so that we can
		//set our states to the correct enabled/disabled setting
		private boolean getStates()
		{
			//get the string from the states_enabled_table file
			String statesEnabledTemp = ShellInterface.getProcessOutput(C_STATES_ENABLED);
			log("statesEnabledTemp val: " + statesEnabledTemp);
			
			try
			{
				//we put all of this in a try catch block, instead of checking for null values
				//split the string into an array, and if the string is null, we will get an exception
				//to be caught, then ignored, by the catch block
				String[] statesEnable = statesEnabledTemp.split(" ");
				//now loop through and enable the enabled states, and disable the disabled states
				for(int i = 0; i < fqStatsList.size(); i++)
				{
					if(statesEnable[i].equals("1"))
						fqStatsList.get(i).setEnabled(true);
					else
						fqStatsList.get(i).setEnabled(false);
				}
			}catch(Exception ignored){return false;}
			
			//just before we return, we will set statesAvailable to true, to keep from
			//having to call this again
			statesAvailable = true;
			return true;
    	}
		
		private int getDeepSleep()
		{
			int sleep = (int)(SystemClock.elapsedRealtime() - SystemClock.uptimeMillis());
			
			log("sleep val: " + Integer.toString(sleep));
			
			return sleep / 10;
		}
		
		private int[] getTisPercent(String[] freqTable, int deepSleep)
		{
			double totalTime = 0;
			int[] tisPercents = new int[20];
			
			for(int i = 1; i < freqTable.length; i +=2)
			{
				totalTime += Double.parseDouble(freqTable[i]);
			}
			totalTime += deepSleep;
			
			for(int i = 1, j = 0; i < freqTable.length; i += 2, j++)
			{
				Double d = new Double((Double.parseDouble(freqTable[i]) / totalTime) * 100);
				int whole = d.intValue();
				if(d - whole >= 0.5)
					d += 1;
				tisPercents[j] = d.intValue();
			}
			
			Double d = new Double((deepSleep / totalTime) * 100);
			int whole = d.intValue();
			if(d - whole >= 0.5)
				d += 1;
			tisPercents[freqTable.length] = d.intValue();
			
			return tisPercents;
		}
    }
}