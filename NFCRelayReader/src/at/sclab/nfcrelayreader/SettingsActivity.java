/* Copyright 2013 Michael Heinzl, Stefan Peherstorfer and Georg Chalupar

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package at.sclab.nfcrelayreader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Display the fragment as the main content.
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment()).commit();
	}

	public static class SettingsFragment extends PreferenceFragment {

		SharedPreferences settings;

		@Override
		public void onCreate(Bundle savedInstanceState) {

			super.onCreate(savedInstanceState);
			settings = PreferenceManager.getDefaultSharedPreferences(this
					.getActivity());

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.preferences);

			EditTextPreference ip = (EditTextPreference) findPreference("ip");
			if (ip != null) {
				ip.setSummary(ip.getText());
				ip.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						SharedPreferences.Editor editor = settings.edit();
						EditTextPreference ip = (EditTextPreference) preference;
						// update summary
						ip.setSummary((String) newValue);
						ip.setText((String) newValue);
						// set preference
						editor.putString("ip", (String) newValue);
						editor.commit();
						return false;
					}

				});
			}

			EditTextPreference port = (EditTextPreference) findPreference("port");
			if (port != null) {
				port.setSummary(port.getText());
				port.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						SharedPreferences.Editor editor = settings.edit();
						EditTextPreference port = (EditTextPreference) preference;
						// update summary
						port.setSummary((String) newValue);
						port.setText((String) newValue);
						// set preference
						editor.putString("port", (String) newValue);
						editor.commit();
						return false;
					}

				});
			}

			ListPreference protocol = (ListPreference) findPreference("protocol");
			if (protocol != null) {

				protocol.setSummary(protocol.getEntry());
				protocol.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						SharedPreferences.Editor editor = settings.edit();
						ListPreference protocol = (ListPreference) preference;
						// update summary
						protocol.setSummary((String) newValue);
						// set preference
						editor.putString("protocol", (String) newValue);
						editor.commit();
						return true;
					}

				});
			}
		}
	}
}
