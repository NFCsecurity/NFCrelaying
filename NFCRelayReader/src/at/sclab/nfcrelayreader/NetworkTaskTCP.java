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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import android.util.Log;
import android.widget.TextView;

public class NetworkTaskTCP extends NetworkTask {

	private static final String LOG_TAG = NetworkTaskTCP.class.getSimpleName();
	private static final int TIMEOUT = 30000;

	BufferedReader reader;
	BufferedWriter writer;
	Socket clientSocket = null;

	public NetworkTaskTCP(TextView message, String uid, String ip, int port) {
		super(message, uid);
		super.setIp(ip);
		super.setPort(port);
	}

	public void start() {

		networkThread = new Thread(this);
		networkThread.start();
	}

	private void processClient(Socket clientSocket) throws IOException,
			InterruptedException {
		Utils.log_display(message, LOG_TAG, "connected to: "
				+ clientSocket.getInetAddress().getHostAddress());
		reader = new BufferedReader(new InputStreamReader(
				clientSocket.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(
				clientSocket.getOutputStream()));
		Utils.log_display(message, LOG_TAG, "<--" + "UID: " + uid);
		writer.write(uid);
		writer.newLine();
		writer.flush();
		synchronized (syn) {
			socketReady = true;
			syn.notify();
			while (true) {
				successful = false;
				String receivedString = reader.readLine();
				if (receivedString == null) {
					socketReady = false;
					Utils.log_display(message, LOG_TAG, "received EOF");
					syn.notify();
					break;
				}
				successful = true;
				receivedBytes = Utils.toBytes(receivedString);
				cmdReady = true;
				syn.notify(); // notify Reader about cmd from PCD
				Utils.log_display(message, LOG_TAG, "-->" + receivedString);
				while (!responseReady) {
					syn.wait(); // wait for response bytes from PICC
				}

				String stringToSend = Utils.toHex(bytesToSend);
				Utils.log_display(message, LOG_TAG, "<--" + stringToSend);
				writer.write(stringToSend);
				writer.newLine();
				writer.flush();
				responseReady = false;
			}
		}
	}

	@Override
	public void run() {

		try {
			try {
				Utils.log_display(message, LOG_TAG, "connecting to emulator...");
				clientSocket = new Socket(getIp(), getPort());
				clientSocket.setSoTimeout(TIMEOUT);
				processClient(clientSocket);
			} catch (IOException e) {
				Utils.log_display(message, LOG_TAG,
						"Error reading/writing from/to socket");
				Log.d(LOG_TAG, "Error", e);
			} finally {
				if (clientSocket != null)
					clientSocket.close();
			}
		} catch (InterruptedException e) {
			Utils.log_display(message, LOG_TAG, "Thread interrupted");
			Log.d(LOG_TAG, "Error", e);
		} catch (IOException e) {
			Utils.log_display(message, LOG_TAG, "Error closing socket");
			Log.d(LOG_TAG, "Error", e);
		}
	}

	public synchronized void stop() {

		try {
			if (reader != null)
				reader.close();
			if (writer != null)
				writer.close();
			if (clientSocket != null)
				clientSocket.close();
		} catch (IOException e) {
			Utils.log_display(message, LOG_TAG,
					"Error closing socket on stop()");
			Log.d(LOG_TAG, "Error", e);
		}
		Log.d(LOG_TAG, "stopping applet thread");
		if (networkThread != null) {
			networkThread.interrupt();
			Log.d(LOG_TAG, "applet thread stopped");
		}
	}
}
