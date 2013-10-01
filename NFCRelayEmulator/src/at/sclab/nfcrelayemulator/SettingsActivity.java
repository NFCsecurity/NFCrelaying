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

package at.sclab.nfcrelayemulator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class SettingsActivity extends Activity {

	private TextView statusText;
	private Spinner protocol;
	private EditText port;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.preferences);
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		statusText = (TextView) findViewById(R.id.status_text);
		statusText.setText("Server Settings");

		protocol = (Spinner) findViewById(R.id.protocol);

		port = (EditText) findViewById(R.id.port);
		port.setText(settings.getString("port", "1337"));

		Button ok = (Button) findViewById(R.id.ok);
		ok.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent myIntent = new Intent(SettingsActivity.this,
						MainActivity.class);
				String s = protocol.getSelectedItem().toString();
				myIntent.putExtra("protocol", s);
				int p = Integer.parseInt(port.getText().toString());
				myIntent.putExtra("port", p);
				SettingsActivity.this.startActivity(myIntent);
			}

		});

	}
}
