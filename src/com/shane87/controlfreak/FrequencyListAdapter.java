package com.shane87.controlfreak;

import java.util.ArrayList;

import com.shane87.controlfreak.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class FrequencyListAdapter extends BaseExpandableListAdapter
{
	//Holder for a reference to our main class, ControlFreak
	private final ControlFreak controlFreak;
	//Holder for the Context of our app
	private Context context;
	//Holder for our Array of FrequencyStats classes
	ArrayList<FrequencyStats> fqStatsList = new ArrayList<FrequencyStats>();
	
	/***********************************************************************
	 * FrequencyListAdapter(ControlFreak, Context) -                       *
	 * Arguments - ControlFreak _controlFreak - a reference to our main    *
	 *                                          ControlFreak class         *
	 *             Context _context - a reference to the Context of the app*
	 * Returns - nothing                                                   *
	 * Outcome - _controlFreak is stored in this.controlFreak, _context    *
	 * 			is stored in this.context                                  *
	 ***********************************************************************/
	public FrequencyListAdapter(ControlFreak _controlFreak, Context _context)
	{
		this.controlFreak = _controlFreak;
		this.context = _context;
	}
	
	/***********************************************************************
	 * getChild(int, int) -                                                *
	 * Arguments - int groupPosition - the group we are referencing, in    *
	 *                                 this class, a "group" is an entry   *
	 *                                 in fqStatsList that holds all of the*
	 *                                 info about one frequency, i.e. group*
	 *                                 0 would reference fqStatsList.get(0)*
	 *                                 which would be the group for the    *
	 *                                 highest available frequency         *
	 *             int childPosition - a reference to a child of this group*
	 *                                 each group has one child, for now,  *
	 *                                 and the child is the uV stats for   *
	 *                                 group, so regardless of the child   *
	 *                                 referenced, we just return this     *
	 *                                 group's uV value                    *
	 * Returns - an Object containing the uV setting for this group        *                                                   *
	 * Outcome - the uV setting is returned in an Object                   *
	 ***********************************************************************/
	@Override
	public Object getChild(int groupPosition, int childPosition)
	{
		return fqStatsList.get(groupPosition).getUV();
	}
	
	/***********************************************************************
	 * getChildId(int, int) -                                              *
	 * Arguments - int groupPosition - a reference to the group we are     *
	 *                                 getting info about                  *
	 *             int childPosition - a reference to the child of this    *
	 *                                 group we are getting info about     *
	 * Returns - the id of the referenced child in the referenced group    *
	 *           since the id of the child is the same as the child's      *
	 *           position, we just return chilPosition                     *
	 * Outcome - the id of the child is returned                           *
	 ***********************************************************************/
	@Override
	public long getChildId(int groupPosition, int childPosition)
	{
		return childPosition;
	}
	
	/***********************************************************************
	 * getChildrenCount(int) -                                             *
	 * Arguments - int groupPosition - reference to which group we are     *
	 *                                 getting info about                  *
	 * Returns - the number of children this group has, since each group   *
	 *           has only one child, we simply return 1                    *
	 * Outcome - the childrenCount for this group is returned              *
	 ***********************************************************************/
	@Override
	public int getChildrenCount(int groupPosition)
	{
		return 1;
	}
	
	/***********************************************************************
	 * getChildView(int, int, boolean, View, ViewGroup) -                  *
	 * Arguments - int groupPosition - reference to what group we are      *
	 *                                 getting info about                  *
	 *             int childPosition - reference to what child of this     *
	 *                                 group we are getting info about     *
	 *             boolean isLastChild - true or false value letting us    *
	 *                                   know if the child we are getting  *
	 *                                   info about is the last child of   *
	 *                                   this group                        *
	 *             View convertView - a View variable that we will build   *
	 *                                the View for this child in           *
	 *             ViewGroup parent - a reference to the parent that our   *
	 *                                child View will be built under       *
	 * Returns - a fully instantiated view to display the stats for this   *
	 *           child to the user                                         *
	 * Outcome - the view for this child is built and returned             *
	 ***********************************************************************/
	@Override
	public View getChildView(final int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent)
	{
		//Holder for the LinearLayout we will be building this child's View in
		//The base layout for this LinearLayout is contained in module.xml
		//The call to LayoutInflater gets the information from module.xml and inflates the view
		LinearLayout module = (LinearLayout) LayoutInflater.from(this.controlFreak.getBaseContext()).
							  inflate(R.layout.module, parent, false);
		//Holder for the SeekBar contained in the LinearLayout loaded from module.xml,
		//allows us to change the SeekBar position, and register for notifications about
		//the state of the SeekBar, such as its current position, and how far it has been moved
		//by the user
		final SeekBar seekBar;
		//Holders for the TextViews contained in the LinearLayout loaded from module.xml,
		//progressText will hold the current uv settings readout, and will be updated as
		//as the user adjusts the SeekBar
		//tisText will hold the TimeInState info for this group, and will display it to the user
		final TextView progressText, tisText;
		//Holder for the checkbox contained in the LinearLayout loaded from module.xml,
		//this checkbox will be filled out with the stateEnabled info from this group,
		//and will be passed back to the FrequencyStats class as the checkBox for this
		//group, to allow the user to enable/disable this state
		final CheckBox checkBox;
		
		//Grab the SeekBar found in module.xml
		seekBar = (SeekBar) module.findViewById(R.id.freqSB);
		//Grab the TextView for progressText found in module.xml
		progressText = (TextView) module.findViewById(R.id.freq_voltageTxt);
		//Grab the CheckBox found in module.xml
		checkBox = (CheckBox) module.findViewById(R.id.enabledCB);
		//Grab the TextView for tisText found in module.xml
		tisText = (TextView) module.findViewById(R.id.tis);
		
		//Set the text of this TextView to show the current uv value of this state, this will
		//be updated as the user adjusts the seekbar
		progressText.setText(Integer.toString(fqStatsList.get(groupPosition).getUV()) + " mV");
		
		//Pass FrequencyStats the reference to the checkbox from module.xml
		fqStatsList.get(groupPosition).setCheckBox(checkBox);
		//Set the text of the checkBox in module.xml to show the state (i.e 1400mhz) we are talking about
		fqStatsList.get(groupPosition).getCheckBox().setText(
				Integer.toString(fqStatsList.get(groupPosition).getValue()) + " Mhz");
		//Update the checkbox to be checked if this state is enabled, otherwise, leave it unchecked
		fqStatsList.get(groupPosition).getCheckBox().setChecked(
				fqStatsList.get(groupPosition).getEnabled());
		//Change the style of the checkbox
		fqStatsList.get(groupPosition).getCheckBox().setSingleLine(true);
		//Build the listener for the checkbox so we can make changes when the user checks/unchecks
		//the checkbox
		OnCheckedChangeListener listener = new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				fqStatsList.get(groupPosition).setEnabled(isChecked);
				controlFreak.updateFreqSpinner(groupPosition);
			}
		};
		
		//Link the listener we just created to our checkbox
		fqStatsList.get(groupPosition).getCheckBox().setOnCheckedChangeListener(listener);
		
		//Set the text of the tisTextView to be the formatted TIS info
		tisText.setText(fqStatsList.get(childPosition).getTISFormat());
		
		//Set the starting position of the SeekBar, based on currently applied uv
		seekBar.setProgress((200 - fqStatsList.get(groupPosition).getUV()) / 25);
		//Build our listener for this seekbar, and link it to the seekbar at the same time
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() 
		{
			//we are required to implement this method, but we dont need to know about
			//when the user stops touching the seekbar
			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{
			}
			
			//same thing for this, we have to implement it, but we dont need to know when
			//the user starts touching the seekbar
			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{
			}
			
			//We do, however, need to know about the user changing the position of the seekbar
			//So in this method, which gets called anytime the seekbar position is changed,
			//we update our states uv value, and we update the progressText to show the user
			//what change they have made
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) 
			{
				progressText.setText("-" + Integer.toString(200 - 25 * progress) + "mV");
				fqStatsList.get(groupPosition).setUV(200 - 25 * progress);
				
			}
		});
		
		//Return the view we just created
		return module;
	}
	/***********************************************************************
	 * getGroup(int) -                                                     *
	 * Arguments - int groupPosition - reference to which group we are     *
	 *                                 getting info for                    *
	 * Returns - an Object containing a String representation of the basic *
	 *           info about this group, formatted as                       *
	 *           "freq Mhz: stockVolt - uv = actualVolt mV"                *
	 * Outcome - the group info is formatted into a string and returned    *
	 ***********************************************************************/
	@Override
	public Object getGroup(int groupPosition)
	{
		//Holder for the int value of this groups frequency
		int frequency = fqStatsList.get(groupPosition).getValue();
		//Holder for the String representation of this group's frequency
		String frequencyS = String.valueOf(frequency);
		//Holder for the String representation of this group's stockVoltage
		String stockVoltage;
		//Holder for the String representation of this group's actualVoltage
		String actualVoltage;
		//Holder for the String representation of this group's uv setting
		String uV = String.valueOf(fqStatsList.get(groupPosition).getUV());
		
		//Get this group's stock voltage from ControlFreak, and convert it to a String
		stockVoltage = this.controlFreak.stockVoltages.get(String.valueOf(frequency));
		
		//If this group has a stock voltage, stockVoltage will NOT be null, so we can
		//figure out the actual voltage by subtracting uv from stock voltage
		if(stockVoltage != null)
			actualVoltage = String.valueOf(Integer.parseInt(stockVoltage) - Integer.parseInt(uV));
		//If this group does not have a stock voltage, we don't know the actual voltage eaither
		//so we will just use "?" to show that we don't know
		else
		{
			stockVoltage = "?";
			actualVoltage = "?";
		}
		
		//Put all of the info together in a String and return it as an Object
		return frequencyS + " Mhz: " + stockVoltage + " - " + uV + " = " + actualVoltage + "mV";
	}
	
	/***********************************************************************
	 * getGroupCount() -                                                   *
	 * Arguments - none                                                    *
	 * Returns - the number of groups in our ArrayList, which is found by  *
	 *           calling the size() function of our array                  *
	 * Outcome - the number of groups is returned                          *
	 ***********************************************************************/
	@Override
	public int getGroupCount()
	{
		return fqStatsList.size();
	}
	
	/***********************************************************************
	 * getGroupId(int) -                                                   *
	 * Arguments - int groupPosition - reference to what group we are      *
	 *                                 getting info about                  *
	 * Returns - the id for this group. since the id is the same as the    *
	 *           position, we just return groupPosition                    *
	 * Outcome - the id of this group is returned                          *
	 ***********************************************************************/
	@Override
	public long getGroupId(int groupPosition)
	{
		return groupPosition;
	}
	
	/***********************************************************************
	 * getGroupView(int, boolean, View, ViewGroup) -                       *
	 * Arguments - int groupPosition - reference to the group we are       *
	 *                                 getting the View for                *
	 *             boolean isExpanded - true/false value telling us whether*
	 *                                  or not the view is expanded        *
	 *             View convertView - the view we will be building our     *
	 *                                group view in                        *
	 *             ViewGroup parent - the parent view we will be building  *
	 *                                the group view under                 *
	 * Returns - a fully fleshed out view for this group. this view is what*
	 *           the user will see when the state control is closed in the *
	 *           main view                                                 *
	 * Outcome - the group view is fleshed out and returned                *
	 ***********************************************************************/
	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent)
	{
		//String variable, filled with the String representation of the basic stats for this
		//group. see getGroup(int) for details on the string format
		String group = (String)getGroup(groupPosition);
		
		//if they passed us an empty view, lets fill it out, otherwise we will skip to the
		//next step
		if(convertView == null)
		{
			//create a layout inflater to inflate our view
			LayoutInflater infalInflater = (LayoutInflater)context.getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			//have our inflater inflate the view found in group.xml
			convertView = infalInflater.inflate(R.layout.group, null);
		}
		
		//get a reference to the textview found in group.xml, and have it display the basic info
		//stored in the string var group
		TextView tv = (TextView)convertView.findViewById(R.id.tvGroup);
		tv.setText(group);
		
		//return the inflated and updated view
		return convertView;
	}
	
	/***********************************************************************
	 * hasStableIds() -                                                    *
	 * Arguments - none                                                    *
	 * Returns - a boolean value to show if the ids of this array will     *
	 *           change                                                    *
	 * Outcome - true is returned, since our id's are never changed        *
	 ***********************************************************************/
	@Override
	public boolean hasStableIds()
	{
		return true;
	}
	
	/***********************************************************************
	 * isChildSelectable(int, int) -                                       *
	 * Arguments - int groupPosition - reference to what group we are      *
	 *                                 getting info about                  *
	 *             int childPosition - reference to which child of this    *
	 *                                 group we are getting info about     *
	 * Returns - boolean value indicating the selectability of the child   *
	 * Outcome - always returns true, since all children are selectable    *
	 ***********************************************************************/
	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition)
	{
		return true;
	}
	
	/***********************************************************************
	 * setFrequency(ArrayList<FrequencyStats>) -                           *
	 * Arguments - ArrayList<FrequencyStats> _fqStatsList - a copy of the  *
	 *                                                      array list we  *
	 *                                                      are being asked*
	 *                                                      to use         *
	 * Returns - nothing                                                   *
	 * Outcome - this.fqStatsList is updated to _fqStatsList               *
	 ***********************************************************************/
	public void setFrequencies(ArrayList<FrequencyStats> _fqStatsList)
	{
		this.fqStatsList = _fqStatsList;
	}
}
