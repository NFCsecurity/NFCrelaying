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

import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String LOG_TAG = MainActivity.class.getSimpleName();
	private static final String TECH_ISODEP = "android.nfc.tech.IsoDep";

	private TextView statusText;
	private TextView message;

	private NfcAdapter adapter;
	private PendingIntent pendingIntent;
	private IntentFilter[] filters;
	private String[][] techLists;

	private ReaderApplet readerApplet;
	private NetworkTask networkTask;
	private SharedPreferences settings;

	private String ip;
	private int port;
	private String protocol;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		settings = PreferenceManager.getDefaultSharedPreferences(this);

		setProgressBarIndeterminateVisibility(false);

		statusText = (TextView) findViewById(R.id.status_text);
		message = (TextView) findViewById(R.id.message);
		message.setText("");

		adapter = NfcAdapter.getDefaultAdapter(this);
		adapter.setNdefPushMessage(null, this);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
			adapter.setBeamPushUris(null, this);
		}

		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		filters = new IntentFilter[] { new IntentFilter(
				NfcAdapter.ACTION_TECH_DISCOVERED) };
		techLists = new String[][] { { TECH_ISODEP } };

		readerApplet = new ReaderApplet(message);
		statusText.setText("Reader initialized");

		Intent intent = getIntent();
		String action = intent.getAction();
		Log.d(LOG_TAG, "Intent: " + intent);

		if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
			handleTag(intent);
		} else {
			Log.d(LOG_TAG, "no tech discovered");
		}

	}

	@Override
	public void onResume() {

		Log.d(LOG_TAG, "onResume()");
		super.onResume();

		Log.d(LOG_TAG, "Keep screen on");
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if (adapter != null) {
			adapter.enableForegroundDispatch(this, pendingIntent, filters,
					techLists);
		}
	}

	@Override
	public void onPause() {

		Log.d(LOG_TAG, "onPause()");
		super.onPause();

		if (adapter != null) {
			Log.d(LOG_TAG, "disabling foreground dispatch");
			adapter.disableForegroundDispatch(this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void onSettingsClick(MenuItem item) {
		if (readerApplet != null) {
			readerApplet.stop();
		}
		if (networkTask != null) {
			networkTask.stop();
		}
		startActivity(new Intent(this, SettingsActivity.class));
	}

	@Override
	public void onDestroy() {

		Log.d(LOG_TAG, "onDestroy()");
		super.onDestroy();

		if (readerApplet != null) {
			readerApplet.stop();
		}
		if (networkTask != null) {
			networkTask.stop();
		}
	}

	@Override
	public void onNewIntent(Intent intent) {

		Log.d(LOG_TAG, "onNewIntent()");
		handleTag(intent);

	}

	private void handleTag(Intent intent) {

		Log.d(LOG_TAG, "TECH_DISCOVERED: " + intent);
		Utils.log_display(message, LOG_TAG, "==========================");
		Log.d(LOG_TAG, "Discovered tag  with intent: " + intent);

		try {
			Tag tag = null;
			if (intent.getExtras() != null) {
				tag = (Tag) intent.getExtras().get(NfcAdapter.EXTRA_TAG);
			}
			if (tag == null) {
				return;
			}

			Log.d(LOG_TAG, "Tag: " + tag);
			List<String> techList = Arrays.asList(tag.getTechList());
			Log.d(LOG_TAG, "Tech list: " + techList);

			if (!techList.contains(TECH_ISODEP)) {
				Log.e(LOG_TAG, TECH_ISODEP + " not found in tech list");
				return;
			}

			String tagId = "";
			if (tag.getId() != null) {
				tagId = Utils.toHex(tag.getId());
				Utils.log_display(message, LOG_TAG, "Tag ID: " + tagId + "\n");
			}

			IsoDep isoTag = IsoDep.get(tag);
			Log.d(LOG_TAG, "isConnected() " + isoTag.isConnected());
			if (!isoTag.isConnected()) {
				isoTag.connect();
			}
			isoTag.setTimeout(5000);
			Utils.log_display(message, LOG_TAG, "Staring network thread...");

			settings = PreferenceManager.getDefaultSharedPreferences(this);
			ip = settings.getString("ip", "192.168.1.100");
			port = Integer.parseInt(settings.getString("port", "1337"));
			protocol = settings.getString("protocol", "TCP");

			if (protocol.equals("TCP")) {
				networkTask = new NetworkTaskTCP(message, tagId, ip, port);
				networkTask.start();
			} else if (protocol.equals("UDP")) {
				networkTask = new NetworkTaskUDP(message, tagId, ip, port);
				networkTask.start();
			} else {
				Utils.log_display(message, LOG_TAG,
						"Error starting network thread (no protocol)");
			}

			// stop and start a fresh thread for each new connection
			if (readerApplet != null) {
				Log.d(LOG_TAG, "thread running: " + readerApplet.isRunning());
				if (readerApplet.isRunning()) {
					Log.d(LOG_TAG, "thread alredy running, stopping");
					readerApplet.stop();
				}
			}
			Utils.log_display(message, LOG_TAG,
					"Staring reader applet thread...");
			readerApplet.start(isoTag, networkTask);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}