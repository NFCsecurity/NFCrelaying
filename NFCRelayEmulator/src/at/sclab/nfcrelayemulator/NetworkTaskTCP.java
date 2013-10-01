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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;
import android.widget.TextView;

public class NetworkTaskTCP extends NetworkTask {

	private static final String LOG_TAG = NetworkTaskTCP.class.getSimpleName();
	private static final int TIMEOUT = 20000;
	BufferedReader reader;
	BufferedWriter writer;
	ServerSocket serverSocket = null;

	public NetworkTaskTCP(TextView message, int port) {
		super(message);
		super.setPort(port);
	}

	@Override
	public void start() {
		networkThread = new Thread(this);
		networkThread.start();
	}

	@Override
	public void run() {

		Utils.log_display(message, LOG_TAG, "starting listener socket");
		try {
			try {
				serverSocket = new ServerSocket(getPort());
				Socket clientSocket = null;
				while (true) {
					try {
						Utils.log_display(message, LOG_TAG,
								"waiting for reader...");
						if(serverSocket == null){
							Utils.log_display(message, LOG_TAG,
									"no server Socket");
							break;
						}
						clientSocket = serverSocket.accept();
						clientSocket.setSoTimeout(TIMEOUT);
						processClient(clientSocket);
					} catch (IOException e) {
						Utils.log_display(message, LOG_TAG,
								"Error reading/writing from/to socket");
						Log.d(LOG_TAG, "Error", e);
					} finally {
						if (clientSocket != null && clientSocket.isConnected())
							clientSocket.close();
					}
				}
			} catch (InterruptedException e) {
				Utils.log_display(message, LOG_TAG, "Thread interrupted");
				Log.d(LOG_TAG, "Error", e);
			} catch (IOException e) {
				Utils.log_display(message, LOG_TAG, "Error opening socket");
				Log.d(LOG_TAG, "Error", e);
			} finally {
				socketReady = false;
				Utils.log_display(message, LOG_TAG, "closing socket...");
				if (serverSocket != null)
					serverSocket.close();
			}
		} catch (IOException e1) {
			Utils.log_display(message, LOG_TAG, "Connection failure");
			Log.d(LOG_TAG, "Error", e1);
		}
	}

	private void processClient(Socket clientSocket) throws IOException,
			InterruptedException {
		Utils.log_display(message, LOG_TAG, clientSocket.getInetAddress()
				.getHostAddress() + " connected!");
		reader = new BufferedReader(new InputStreamReader(
				clientSocket.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(
				clientSocket.getOutputStream()));
		Utils.log_display(message, LOG_TAG, "card ready: " + reader.readLine());
		// TODO activate dispatcher
		synchronized (syn) {
			socketReady = true;
			syn.notify();
			while (clientSocket.isConnected()) {
				syn.wait(); // wait for cmd bytes from PCD
				successful = false;
				String stringToSend = Utils.toHex(bytesToSend);
				Utils.log_display(message, LOG_TAG, "<--" + stringToSend);
				writer.write(stringToSend);
				writer.newLine();
				writer.flush();
				String receivedString = reader.readLine();
				if (receivedString == null) {
					socketReady = false;
					Utils.log_display(message, LOG_TAG, "received EOF");
					syn.notify();
					break;
				}
				receivedBytes = Utils.toBytes(receivedString);
				successful = true;
				Utils.log_display(message, LOG_TAG, "-->" + receivedString);
				syn.notify(); // notify Emulator about response from
								// PICC
			}
			Utils.log_display(message, LOG_TAG, "client disconnected");
		}
	}

	@Override
	public void stop() {
		try {
			if (serverSocket != null)
				serverSocket.close();
			if (reader != null)
				reader.close();
			if (writer != null)
				writer.close();
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
