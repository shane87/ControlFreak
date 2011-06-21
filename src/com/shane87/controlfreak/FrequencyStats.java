package com.shane87.controlfreak;

import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class FrequencyStats 
{
	private int 		value;
	//Holder for the value, in Mhz, of this state
	private int 		uv;
	//Holder for the amount of uv applied to this state, in mV less than stock voltage
	private int 		stockVoltage;
	//Holder for the stock voltage of this state, in mV
	private CheckBox 	checkbox;
	//The check box for this state, as will be displayed in the ui
	private boolean 	stateEnabled;
	//Holder for the state of this state, true for enabled, false for disabled
	private int 		tis;
	//Holder for the time spent in this state, in user-time units, which is hundredth's of seconds
	private int 		hours;
	//Holder for the number of hours spent in this state, found by the private formatTIS() function
	private int 		minutes;
	//Holder for minutes spent in state, as found by formatTIS()
	private int 		seconds;
	//Holder for seconds spent in state, as found by formatTIS()
	//hours, minutes, and seconds are used to generate a string formatted as hh:mm:ss
	private int			tisPercent;
	//Holder for percent of time spent in this state.
	//will be figured by the main part of the program, and passed in either by
	//setTisPercent() or in the constructor. May be obtained with getTisPercent().
	private int 		curGpuVal;
	//Holder for current gpu clock value. Will be obtained by the main program, and
	//passed in by the constructor or setCurGpu(). May be obtained by calling
	//getCurGpu()
	private int			stockGpuVal;
	//Holder for stock gpu clock value. Same as above, passed in with constructor or
	//setStockGpu{}. Obtained with getStockGpu()
	
	public void setCurGpu(int _curGpuVal)
	{
		this.curGpuVal = _curGpuVal;
	}
	public void setStockGpu(int _stockGpuVal)
	{
		this.stockGpuVal = _stockGpuVal;
	}
	public int getCurGpu()
	{
		return this.curGpuVal;
	}
	public int getStockGpu()
	{
		return this.stockGpuVal;
	}
	/*************************************************************
	 * setTIS(int) -                                             *
	 * Arguments - int _tis - the time in state for this state,  *
	 *                        must be in usertime units          *
	 * Returns   - nothing                                       *
	 * Outcome   - sets tis for this state to be _tis, and calls *
	 *             formatTIS() to update hours, minutes and      *
	 *             seconds                                       *
	 *************************************************************/
	public void setTIS(int _tis)
	{
		this.tis = _tis;
		this.formatTIS();
	}
	/*************************************************************
	 * setTisPercent(int) -                                      *
	 * Arguments - int _tisPercent - the percent of total time   *
	 * 								 spent in this state         *
	 * Returns   - nothing                                       *
	 * Outcome   - sets tisPercent for this state to be          *
	 *             _tisPercent                                   *
	 *************************************************************/
	public void setTisPercent(int _tisPercent)
	{
		this.tisPercent = _tisPercent;
	}
	/*************************************************************
	 * getTIS() -                                                *
	 * Arguments - none                                          *
	 * Returns   - current tis for this state, in usertime units *
	 * Outcome   - tis is returned                               *
	 *************************************************************/
	public int getTIS()
	{
		return this.tis;
	}
	/*************************************************************
	 * getTisPercent() -                                         *
	 * Arguments - none                                          *
	 * Returns   - tisPercent for this state                     *
	 * Outcome   - tisPercent is returned                        *
	 *************************************************************/
	public int getTisPercent()
	{
		return this.tisPercent;
	}
	/*************************************************************
	 * getTISFormat() -                                          *
	 * Arguments - none                                          *
	 * Returns   - a string representation of current tis, in the*
	 *             format of hh:mm:ss                            *
	 * Outcome   - the string representation is returned         *
	 *************************************************************/
	public String getTISFormat()
	{
		//String var to build our formatted tis string in
		String tisf = new String("");
		
		//if hours is less than 10, add a zero and hours to the string
		if(this.hours < 10)
			tisf = tisf.concat("0" + Integer.toString(this.hours) + ":");
		//otherwise, just add hours to the string
		else
			tisf = tisf.concat(Integer.toString(this.hours) + ":");
		
		//if minutes is less than 10, add a zero and minutes to the string
		if(this.minutes < 10)
			tisf = tisf.concat("0" + Integer.toString(this.minutes) + ":");
		//otherwise, just add minutes to the string
		else
			tisf = tisf.concat(Integer.toString(this.minutes) + ":");
		
		//if seconds is less than 10, add a zero and seconds to the string
		if(this.seconds < 10)
			tisf = tisf.concat("0" + Integer.toString(this.seconds));
		//otherwise, just add seconds to the string
		else
			tisf = tisf.concat(Integer.toString(this.seconds));
		
		//return our formatted string
		return tisf;
	}
	/*************************************************************
	 * setValue(int) -                                           *
	 * Arguments - int _value - the value for this state,        *
	 *                          must be in mhz                   *
	 * Returns   - nothing                                       *
	 * Outcome   - sets value for this state to be _value        *
	 *************************************************************/
	public void setValue(int _value)
	{
		this.value = _value;
	}
	/*************************************************************
	 * getValue() -                                              *
	 * Arguments - none                                          *
	 * Returns   - value for this state, in mhz                  *
	 * Outcome   - value is returned                             *
	 *************************************************************/
	public int getValue()
	{
		return this.value;
	}
	/*************************************************************
	 * getCheckBox() -                                           *
	 * Arguments - none								             *
	 * Returns   - CheckBox for this state, to be displayed in   *
	 *             the ui                                        *
	 * Outcome   - checkbox is returned                          *
	 *************************************************************/
	public CheckBox getCheckBox()
	{
		return this.checkbox;
	}
	/*************************************************************
	 * setCheckBox(CheckBox) -                                   *
	 * Arguments - CheckBox _checkbox - a checkbox designed to be*
	 *                                  displayed for this state *
	 * Returns   - nothing                                       *
	 * Outcome   - sets checkbox for this state to be _checkbox  *
	 *************************************************************/
	public void setCheckBox(CheckBox _checkbox)
	{
		this.checkbox = _checkbox;
	}
	/*************************************************************
	 * setStockVoltage(int) -                                    *
	 * Arguments - int _stockVoltage - the stock voltage for this*
	 *                                 state, in mv              *
	 * Returns   - nothing                                       *
	 * Outcome   - sets stockVolatage for this state to be       *
	 *             _stockVoltage                                 *
	 *************************************************************/
	public void setStockVoltage(int _stockVoltage)
	{
		this.stockVoltage = _stockVoltage;
	}
	/*************************************************************
	 * getStockVoltage() -                                       *
	 * Arguments - none                                          *
	 * Returns   - stockVoltage for this state, in mv            *
	 * Outcome   - stockVoltage is returned                      *
	 *************************************************************/
	public int getStockVoltage()
	{
		return this.stockVoltage;
	}
	/*************************************************************
	 * setUV(int) -                                              *
	 * Arguments - int _uv - the amount of uv for this state,    *
	 *                       expressed in mv below stockVoltage  *
	 * Returns   - nothing                                       *
	 * Outcome   - sets uv for this state to be _uv              *
	 *************************************************************/
	public void setUV(int _uv)
	{
		this.uv = _uv;
	}
	/*************************************************************
	 * getUV() -                                                 *
	 * Arguments - none                                          *
	 * Returns   - uv for this state, expressed in mv below stock*
	 * Outcome   - uv is returned                                *
	 *************************************************************/
	public int getUV()
	{
		return this.uv;
	}
	/*************************************************************
	 * setEnabled(boolean) -                                     *
	 * Arguments - boolean is - the state of this state, enabled *
	 *                          or disabled, expressed as true or*
	 *                          false                            *
	 * Returns   - nothing                                       *
	 * Outcome   - sets stateEnabled for this state to be is     *
	 *************************************************************/
	public void setEnabled(boolean is)
	{
		this.stateEnabled = is;
	}
	/*************************************************************
	 * getEnabled() -                                            *
	 * Arguments - none                                          *
	 * Returns   - stateEnabled, true if state enabled, false    *
	 *             otherwise                                     *
	 * Outcome   - returns stateEnabled                          *
	 *************************************************************/
	public boolean getEnabled()
	{
		return this.stateEnabled;
	}
	/*************************************************************
	 * formatTIS() -                                             *
	 * Arguments - none                                          *
	 * Returns   - nothing                                       *
	 * Outcome   - converts tis from usertime units to hours,    *
	 *             minutes, seconds. NOT callable except inside  *
	 *             a function in this class                      *
	 *************************************************************/
	private void formatTIS()
	{
		//holders for the floating point versions of each unit of time
		float fhours, fminutes, fseconds, tmp;
		//zero out our times, just to have a clean slate
		this.seconds = 0;
		this.minutes = 0;
		this.hours = 0;
		
		//get the number of seconds, and store it as a floating point value
		//since the tis is measured in usertime units, which is 10ms per tick
		//we multiply by 10, then divide by 1000, or just divide by 100
		fseconds = this.tis / 100;
		//store the integer portion as our seconds value
		this.seconds = (int)fseconds;
		
		//if we have been in this state for at least a minute, figure up how many minutes we have
		if(this.seconds > 59)
		{
			//get the floating point minutes value by dividing the seconds by 60
			fminutes = fseconds / 60;
			//convert the fp minutes to int minutes and save it
			this.minutes = (int)fminutes;
			//now, since our seconds value is more than a minute, we need to see how many seconds
			//we have left after we account for the minutes. so take the decimal part of the minutes
			//(fminutes - minutes) and multiply it by 60 to convert fractions of a minute to seconds
			tmp = fminutes - this.minutes;
			tmp *= 60;
			//store the new seconds value as our seconds value
			this.seconds = (int)tmp;
			
			//now, if we have at least an hour of time in this state, lets find the hours
			//it is handled the same as above, except we are converting from minutes above 60
			//to hours plus remaining minutes instead of seconds above 60 to minutes with remaining
			//seconds
			if(this.minutes > 59)
			{
				fhours = fminutes / 60;
				this.hours = (int)fhours;
				tmp = fhours - this.hours;
				tmp *= 60;
				this.minutes = (int)tmp;
			}
		}
	}
	/*************************************************************
	 * FrequencyStats(int, int, int, CheckBox) -                 *
	 * Arguments - int _value - the value for this state, in mhz *
	 *             int _uv    - the uv for this state, in mv     *
	 *                          below stockVoltage               *
	 *             int _tis   - the time in state for this state *
	 *                          in usertime units                *
	 * Returns   - an instantiated class                         *
	 * Outcome   - sets value to _value, uv to _uv, checkbox to  *
	 *             _checkbox. Also sets up the listener for the  *
	 *             checkbox, sets the text for the checkbox,     *
	 *             checks the checkbox if state is enabled, and  *
	 *             calls formatTIS() to fill in hours, minutes   *
	 *             and seconds                                   *
	 *************************************************************/
	public FrequencyStats(int _value, int _uv, int _tis, int _tisPercent, CheckBox _checkbox, int _curGpuVal, int _stockGpuVal)
	{
		//store our arguments where they belong
		this.value = _value;
		this.uv = _uv;
		this.tis = _tis;
		this.checkbox = _checkbox;
		this.tisPercent = _tisPercent;
		this.curGpuVal = _curGpuVal;
		this.stockGpuVal = _stockGpuVal;
		
		//create our listener for the checkbox
		//this function is called anytime the checkbox is checked or unchecked
		OnCheckedChangeListener listener = new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				//when the box is checked(or unchecked), update our stateEnabled value
				setEnabled(isChecked);
			}
		};
		
		//build our checkbox, since chances are _checkbox is an empty CheckBox hull
		//first, set our listener to the listener we just created
		this.checkbox.setOnCheckedChangeListener(listener);
		//set the checkbox to reflect the current state of the state, i.e checked for enabled,
		//unchecked for disabled
		this.checkbox.setChecked(this.stateEnabled);
		//finally, set the text to equal the value of this state, plus Mhz
		//for example, if this is state 1400, we will set the text to "1400 Mhz"
		this.checkbox.setText(String.valueOf(this.value) + " Mhz");
		
		//just before we finish, lets format our tis values to fill out hours, minutes, seconds
		this.formatTIS();
	}
}
