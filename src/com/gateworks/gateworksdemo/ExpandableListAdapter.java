/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gateworks.gateworksdemo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.gateworks.gateworksutil.*;

public class ExpandableListAdapter extends BaseExpandableListAdapter {
    // 4 Group types
    private static final int GROUP_LED = 0;
    private static final int GROUP_GPIO = 1;
    private static final int GROUP_HWMON = 2;
    private static final int GROUP_PWM = 3;

    private Activity _context;
    private List<String> _listDataHeader; // List of group types
    private HashMap<String, List<String>> _listDataChild; // Map of all children for each group

    public ArrayList<ArrayList<ViewHolder>> holders = new ArrayList<ArrayList<ViewHolder>>(); // List of all viewholders

    public ExpandableListAdapter(final Activity context, List<String> listDataHeader,
                                 HashMap<String, List<String>> listChildData) {
        this._context = context;
        this._listDataHeader = listDataHeader;
        this._listDataChild = listChildData;

        ArrayList<ViewHolder> tmp;
        for (String type: _listDataHeader) {
            tmp = new ArrayList<ViewHolder>();

            // Create a viewholder for each list child
            for (String propText :_listDataChild.get(type))
                tmp.add(new ViewHolder(propText));

            holders.add(tmp);
        }
    }

    public Object getChild(int groupPosition, int childPosititon) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .get(childPosititon);
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        LayoutInflater inflater = _context.getLayoutInflater();
        ViewHolder holder;

        holder = holders.get(groupPosition).get(childPosition);
        holder.groupPosition = groupPosition;
        holder.childPosition = childPosition;

        try {
            // Assign applicable viewholder fields depending on type of child
            switch (getGroupType(groupPosition)) {
                case GROUP_LED:
                    convertView = inflater.inflate(R.layout.list_led, null);
                    holder.view = convertView;
                    holder.name = (TextView) convertView.findViewById(R.id.lblListName);
                    holder.toggle = (Switch) convertView.findViewById(R.id.lblListSwitch);
                    holder.spinner = (Spinner) convertView.findViewById(R.id.lblListSpinner);
                    updateLED(holder);
                    addLEDListeners(holder);
                    break;
                case GROUP_GPIO:
                    convertView = inflater.inflate(R.layout.list_gpio, null);
                    holder.view = convertView;
                    holder.name = (TextView) convertView.findViewById(R.id.lblListName);
                    holder.toggle = (Switch) convertView.findViewById(R.id.lblListSwitch);
                    holder.monitor = (TextView) convertView.findViewById(R.id.lblListMonitor);
                    holder.group = (RadioGroup) convertView.findViewById(R.id.lblListRadioGroup);
                    updateGPIO(holder);
                    addGPIOListeners(holder);
                    break;
                case GROUP_HWMON:
                    convertView = inflater.inflate(R.layout.list_hwmon, null);
                    holder.view = convertView;
                    holder.name = (TextView) convertView.findViewById(R.id.lblListName);
                    holder.monitor = (TextView) convertView.findViewById(R.id.lblListMonitor);
                    updateHWMON(holder);
                    break;
                case GROUP_PWM:
                    convertView = inflater.inflate(R.layout.list_pwm, null);
                    holder.view = convertView;
                    holder.name = (TextView) convertView.findViewById(R.id.lblListName);
                    holder.checkBox = (CheckBox) convertView.findViewById(R.id.lblCheckBox);
                    holder.editText = (EditText) convertView.findViewById(R.id.lblEditText);
                    holder.slider = (SeekBar) convertView.findViewById(R.id.lblListSlider);
                    updatePWM(holder);
                    addPWMListeners(holder);
                    break;
                default:
                    //undefined child view type
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        convertView.setTag(holder);
        //Setting the name of the item is the same for all child types
        holder.name.setText(holder.nameVal);
        return convertView;
    }

    public int getChildrenCount(int groupPosition) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition)).size();
    }

    public Object getGroup(int groupPosition) {
        return this._listDataHeader.get(groupPosition);
    }

    public int getGroupCount() {
        return this._listDataHeader.size();
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        final String headerTitle = (String) getGroup(groupPosition);
        LayoutInflater inflater = _context.getLayoutInflater();
        Integer groupType = getGroupType(groupPosition);

        // We need to create a new "cell container" if none exists
        if (convertView == null || convertView.getTag() != groupType) {
            convertView = inflater.inflate(R.layout.header, null);
        }

        //Setting the name of the header group will be the same for all groups
        TextView item = (TextView) convertView.findViewById(R.id.lblListHeaderName);
        item.setTypeface(null, Typeface.BOLD);
        item.setText(headerTitle);

        return convertView;
    }

    public boolean hasStableIds() {
        return true;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    public int getChildTypeCount() {
        return 4;
    }

    public int getGroupTypeCount() {
        return 4;
    }

    public void onGroupCollapsed(int groupPosition) {
        switch (groupPosition) {
            case 1: MainActivity.pausedThreads.add("GPIO");
                break;
            case 2: MainActivity.pausedThreads.add("HWMON");
                break;
        }
    }

    public void onGroupExpanded(int groupPosition) {
        switch (groupPosition) {
            case 1: MainActivity.pausedThreads.remove("GPIO");
                break;
            case 2: MainActivity.pausedThreads.remove("HWMON");
                break;
        }
    }

    public int getGroupType(int groupPosition) {
        switch (groupPosition) {
            case 0:
                return GROUP_LED;
            case 1:
                return GROUP_GPIO;
            case 2:
                return GROUP_HWMON;
            case 3:
                return GROUP_PWM;
            default:
                return -1;
        }
    }

    public void updateLED(ViewHolder holder) {
        //create and set adapter for spinner using trigger options
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(_context,
                android.R.layout.simple_spinner_dropdown_item,
                (String []) LightEmittingDiode.getAllLedTriggers(holder.nameVal).toArray());
        holder.spinner.setAdapter(adapter);

        //set default selection to the mode indicated in trigger
        holder.spinner.setSelection(adapter.getPosition(
                LightEmittingDiode.getLedTrigger(holder.nameVal)));

        // Set LED switch to on/off depending on current setting
        holder.toggle.setChecked(LightEmittingDiode.getLedValue(holder.nameVal));
    }


    public void updateGPIO(ViewHolder holder) {
        // Record current value of gpio
        int gpioVal = GeneralPurposeIO.getGpioValue(holder.nameVal);

        // GPIO direction needs to be read and assigned to radio group
        holder.radioChecked = GeneralPurposeIO.getGpioDirection(holder.nameVal)
                .toString().equals("IN") ? R.id.rdioIn : R.id.rdioOut;
        holder.group.check(holder.radioChecked);

        // If the gpio being updated is the canbus (output only), set input radio child to invisible
        if(holder.nameVal.contains("can_stby"))
            holder.group.findViewById(R.id.rdioIn).setVisibility(View.INVISIBLE);

        switch (holder.radioChecked) {
            case R.id.rdioIn:
                // Update monitor to reflect value if set as input
                holder.monitor.setText(Integer.toString(gpioVal));
                holder.monitor.setVisibility(View.VISIBLE);
                holder.toggle.setVisibility(View.GONE);
                break;
            case R.id.rdioOut:
                // Update switch attributes if set as output
                holder.toggle.setTextOn("HIGH");
                holder.toggle.setTextOff("LOW");
                holder.toggle.setChecked(gpioVal == 1);
                holder.monitor.setVisibility(View.GONE);
                holder.toggle.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }

        //force redraw after changes
        holder.view.invalidate();
    }

    public void updateHWMON(ViewHolder holder) {
        holder.monitor.setText(Integer.toString(HardwareMonitor.getHwmonValue(holder.nameVal)));
        holder.monitor.invalidate();
    }

    public void updatePWM(ViewHolder holder) {
        // Update the checkbox to reflect the enabled status
        holder.checkBox.setChecked(PulseWidthModulation.getEnabled(holder.nameVal));

        // Record current period value of PWM
        int period = PulseWidthModulation.getPeriod(holder.nameVal);

        // Update edit box's text to current period of PWM (in milliseconds)
        holder.editText.setText(Integer.toString(period / 1000));

        // Update the slider to show the current duty cycle to period ratio
        holder.slider.setMax(period/1000);
        holder.slider.setProgress(PulseWidthModulation.getDutyCycle(holder.nameVal) / 1000);

        // Save a copy of untouched seekbar thumb for redraw purposes
        if (holder.sliderClean == null)
            holder.sliderClean = holder.slider.getThumb().getConstantState().newDrawable();

        // Write current duty cycle percentage on thumb
        holder.slider.setThumb(writeOnDrawable(holder.slider.getThumb(),
                (holder.slider.getProgress() * 100 / holder.slider.getMax()) + "%"));
    }

    public void addLEDListeners(final ViewHolder holder) {
        // Create and set trigger spinner (aka dropdown menu) listener
        holder.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LightEmittingDiode.setLedTrigger(holder.nameVal,
                        parent.getItemAtPosition(position).toString());
                ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK);
            }


            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Create and set brightness toggle listener
        holder.toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // isChecked will be true if the switch is in the On position
                LightEmittingDiode.setLedValue(holder.nameVal, isChecked);
                if (!isChecked && holder.spinner != null)
                    holder.spinner.setSelection(0);
            }
        });
    }

    public void addGPIOListeners(final ViewHolder holder) {
        // Create and set direction radio button group listener
        holder.group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            public void onCheckedChanged(RadioGroup group, int checkedId) {
                GeneralPurposeIO.setGpioDirection(holder.nameVal, checkedId == R.id.rdioIn ?
                        GeneralPurposeIO.Direction.IN : GeneralPurposeIO.Direction.OUT);
                updateGPIO(holder);
            }
        });

        // Create and set output value toggle listener
        holder.toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // isChecked will be true if the switch is in the On position
                GeneralPurposeIO.setGpioValue(holder.nameVal, isChecked ? 1 : 0);
            }
        });
    }

    public void addPWMListeners(final ViewHolder holder) {
        // Create and set enable check box listener
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PulseWidthModulation.setEnabled(holder.nameVal, isChecked);
            }
        });

        holder.editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String newPeriod = v.getText().toString();
                // Ignore empty strings or strings that are too long to avoid runtime exceptions
                if (newPeriod.isEmpty() || newPeriod.length() > 8) {
                    Toast.makeText(_context, "Invalid period.", Toast.LENGTH_SHORT).show();
                    return false;
                }

                // Record the int equivalent of the string entered
                int val = Integer.parseInt(newPeriod);

                if (val < 0) {
                    Toast.makeText(_context, "Must use a positive period.", Toast.LENGTH_SHORT).show();
                    return false;
                }

                // Clear duty cycle, set new period, then reset duty cycle using previous percentage
                int previousRatio = PulseWidthModulation.getDutyCycle(holder.nameVal) * 100 /
                        PulseWidthModulation.getPeriod(holder.nameVal);
                PulseWidthModulation.setDutyCycle(holder.nameVal, 0);
                PulseWidthModulation.setPeriod(holder.nameVal, val * 1000);
                PulseWidthModulation.setDutyCycle(holder.nameVal, val * 10 * previousRatio);

                v.clearFocus();
                hideKeyboard(_context);

                return false;
            }
        });

        // Create and set duty cycle slider listener
        holder.slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Convert progress value from nano to milliseconds
                // -1 if value is max; pwm driver does not allow duty_cycle == period
                progress = (progress == seekBar.getMax() ? progress * 1000 - 1 : progress * 1000);

                PulseWidthModulation.setDutyCycle(holder.nameVal, progress);

                // Redraw thumb to remove old percentage, then place the new one
                seekBar.setThumb(holder.sliderClean.getConstantState().newDrawable());
                seekBar.setThumb(writeOnDrawable(seekBar.getThumb(),
                        (progress / holder.slider.getMax() / 10) + "%"));
            }
        });
    }

    private Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap;

        // If drawable contains native method, use that
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        // Single color bitmap will be created of 1x1 pixel
        bitmap = Bitmap.createBitmap(Math.max(1, drawable.getIntrinsicWidth()),
                                     Math.max(1, drawable.getIntrinsicHeight()),
                                     Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private BitmapDrawable writeOnDrawable(Drawable drawable, String text){
        // Convert drawable to bitmap
        Bitmap bm = drawableToBitmap(drawable);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setTextSize(15);

        // Draw text onto bitmap using configured painter via canvas
        Canvas canvas = new Canvas(bm);
        canvas.drawText(text, 2, bm.getHeight() / 2, paint);

        return new BitmapDrawable(_context.getResources(), bm);
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public class ViewHolder {
        View view;
        TextView name;
        String nameVal;
        TextView monitor;
        Switch toggle;
        RadioGroup group;
        int radioChecked;
        Spinner spinner;
        CheckBox checkBox;
        EditText editText;
        SeekBar slider;
        Drawable sliderClean;
        int groupPosition, childPosition;
        String propText, propVal;

        public ViewHolder(String propText) {
            this.nameVal = propText
                    .substring(propText.lastIndexOf('.') + 1, propText.indexOf(']'));
            this.propText = propText;
            this.propVal = propText
                    .substring(propText.lastIndexOf('[') + 1, propText.lastIndexOf(']'));
        }
    }
}