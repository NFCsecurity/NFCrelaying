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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Locale;

import android.nfc.tech.IsoDep;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import at.sclab.nfcrelayreader.Utils;

public class ReaderApplet implements Runnable {

	private static final String LOG_TAG = ReaderApplet.class.getSimpleName();

	private IsoDep tag;
	private Thread readerThread;
	private volatile boolean isRunning = false;
	private TextView log_message;
	private NetworkTask networkTask;
	private long last_log_time;
	File path = new File(
			Environment
					.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
			"apdu_logs");
	File file;
	BufferedWriter writer;

	public ReaderApplet(TextView log_message) {
		this.log_message = log_message;
	}

	public void start(IsoDep tag, NetworkTask networkTask) {
		this.tag = tag;
		this.networkTask = networkTask;
		Log.d(LOG_TAG, "start reader app");
		readerThread = new Thread(this);
		readerThread.start();
		isRunning = true;
	}

	private byte[] transceive(byte[] cmd) throws IOException {

		Log.d(LOG_TAG,
				String.format("[%s] --> %s", readerThread.getName(),
						Utils.toHex(cmd)));
		long time = Calendar.getInstance().getTime().getTime();
		writer.write(String.format("[%1$d][%2$3d] --> %3$s", time, time
				- last_log_time, Utils.toHex(cmd)));
		writer.newLine();
		last_log_time = time;

		byte[] response = tag.transceive(cmd);

		time = Calendar.getInstance().getTime().getTime();
		writer.write(String.format("[%1$d][%2$3d] <-- %3$s", time, time
				- last_log_time, Utils.toHex(response)));
		writer.newLine();
		writer.flush();
		last_log_time = time;
		Log.d(LOG_TAG,
				String.format("[%s] <-- %s", readerThread.getName(),
						Utils.toHex(response)));
		return response;
	}

	@Override
	public void run() {
		try {
			path.mkdirs();
			String protocol = networkTask.getClass().getSimpleName()
					.substring(NetworkTask.class.getSimpleName().length())
					.toLowerCase(new Locale("en"));
			file = new File(path, String.format(
					"%1$tY%1$tm%1$td_%1$tH%1$tM%1$tS_%2$s_reader.log",
					Calendar.getInstance(), protocol));
			writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file)));
			Utils.log_display(log_message, LOG_TAG, "Reader is waiting for cmd");
			last_log_time = Calendar.getInstance().getTime().getTime();

			while (true) {

				byte[] cmd = networkTask.receiveCmd();
				byte[] response = transceive(cmd);
				if (response == null) {
					Log.d(LOG_TAG, "response is null");
					break;
				}
				networkTask.sendResponse(response);
			}
		} catch (IOException e) {
			Utils.log_display(log_message, LOG_TAG,
					"IO Exception: " + e.getMessage());
			Log.e(LOG_TAG, "Error", e);

		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				Utils.log_display(log_message, LOG_TAG, "Error closing logfile");
			}
		}
		isRunning = false;
	}

	public synchronized void stop() {
		Log.d(LOG_TAG, "stopping applet thread");
		if (networkTask != null) {
			networkTask.stop();
		}
		if (readerThread != null) {
			readerThread.interrupt();
			Log.d(LOG_TAG, "applet thread running: " + isRunning);
		}

		Log.d(LOG_TAG, "Resetting applet state");
		resetState();
	}

	public boolean isRunning() {
		return isRunning;
	}

	private void resetState() {
		if (tag != null) {
			try {
				if (tag.isConnected()) {
					tag.close();
				}
				tag = null;
			} catch (IOException e) {
				Log.d(LOG_TAG, "Error closing tag: " + e.getMessage(), e);
			}
		}
	}

}
