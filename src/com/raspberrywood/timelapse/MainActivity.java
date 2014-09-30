package com.raspberrywood.timelapse;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	//startService(new Intent(this, HelloIOIOService.class));

		if (savedInstanceState == null) {
			// During initial setup, plug in the details fragment.
			PreferenceFragment mPrefsFragment = new PrefsFragment();
			mPrefsFragment.setArguments(getIntent().getExtras());
			getFragmentManager().beginTransaction()
					.add(android.R.id.content, mPrefsFragment).commit();
		}
		
		
		//stopService(new Intent(this, HelloIOIOService.class));
		
		
		


		
	}

}