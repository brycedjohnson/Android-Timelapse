package com.raspberrywood.timelapse;


import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;

public class PrefsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	private Intent myService;
    String prefixStr;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.layout.preferences);
        initSummary(getPreferenceScreen());
        
        CheckBoxPreference checkboxPref = (CheckBoxPreference)getPreferenceManager().findPreference("service_toggle");
        checkboxPref.setChecked(false);
	    checkboxPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {            
	
	        public boolean onPreferenceChange(Preference preference, Object newValue) {
	
	               boolean myValue = (Boolean) newValue;
	               myService = new Intent(getActivity(), HelloIOIOService.class);
	               
	               if(myValue) {
	            	   getActivity().startService(myService);
	                     
	               } else {
	            	   getActivity().stopService(myService);
	               }
	              return true;
	         }
	    });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
    	SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();

        updatePrefSummary(findPreference(key));
        
		float photo_time = Float.parseFloat(prefs.getString("photo_time", "@string/photo_time_default"));
		float pause_before_photo_time = Float.parseFloat(prefs.getString("pause_before_photo_time", "@string/pause_before_photo_time_default"));
		float dolly_length = Float.parseFloat(prefs.getString("dolly_length", "@string/dolly_length_default"));
		float inches_per_pause = Float.parseFloat(prefs.getString("inches_per_pause", "@string/inches_per_pause_default")); 	
        
		float total_time = (dolly_length/inches_per_pause) * (photo_time + pause_before_photo_time) / (3600);
		findPreference("total_time").setSummary(Float.toString(total_time) + " Hours");
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    private void updatePrefSummary(Preference p) {
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());

        }
        if (p instanceof MultiSelectListPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());
        }
    }
	 
}
