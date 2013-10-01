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

import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String LOG_TAG = MainActivity.class.getSimpleName();
	private static final String TECH_ISO_PCDA = "android.nfc.tech.IsoPcdA";

	private TextView statusText;
	private TextView message;

	private NfcAdapter adapter;
	private PendingIntent pendingIntent;
	private IntentFilter[] filters;
	private String[][] techLists;

	private EmulatorApplet emulatorApplet;
	private NetworkTask networkTask;

	private int port;
	private String protocol;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent firstintent = getIntent();
		port = firstintent.getIntExtra("port", 1337);
		protocol = firstintent.getStringExtra("protocol");
		setContentView(R.layout.activity_main);

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
		techLists = new String[][] { { "android.nfc.tech.IsoPcdA" } };

		if (protocol.equals("TCP")) {
			networkTask = new NetworkTaskTCP(message, port);
			networkTask.start();
		} else if (protocol.equals("UDP")) {
			networkTask = new NetworkTaskUDP(message, port);
			networkTask.start();
		} else {
			Utils.log_display(message, LOG_TAG,
					"Error starting network thread (no protocol)");
		}

		emulatorApplet = new EmulatorApplet(message);
		statusText.setText("Emulator initialized");

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
	public void onDestroy() {

		Log.d(LOG_TAG, "onDestroy()");
		super.onDestroy();

		if (emulatorApplet != null) {
			emulatorApplet.stop();
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

			if (!techList.contains(TECH_ISO_PCDA)) {
				Log.e(LOG_TAG, "IsoPcdA not found in tech list");
				return;
			}

			TagWrapper tw = new TagWrapper(tag, TECH_ISO_PCDA);
			Log.d(LOG_TAG, "isConnected() " + tw.isConnected());

			if (!tw.isConnected()) {
				tw.connect();
			}

			if (tag.getId() != null) {
				Log.d(LOG_TAG, "Tag ID: " + Utils.toHex(tag.getId()));
				Utils.log_display(message, LOG_TAG,
						"Tag ID: " + Utils.toHex(tag.getId()) + "\n");
			}
			Log.d(LOG_TAG, "Max length: " + tw.getMaxTransceiveLength());
			Utils.log_display(message, LOG_TAG,
					"Max length: " + tw.getMaxTransceiveLength() + "\n");
			Utils.log_display(message, LOG_TAG,
					"Staring emulator applet thread...");

			// stop and start a fresh thread for each new connection
			if (emulatorApplet != null) {
				Log.d(LOG_TAG, "thread running: " + emulatorApplet.isRunning());
				if (emulatorApplet.isRunning()) {
					Log.d(LOG_TAG, "thread already running, stopping");
					emulatorApplet.stop();
				}
			}

			Log.d(LOG_TAG, "emulator start");
			emulatorApplet.start(tw, networkTask);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}