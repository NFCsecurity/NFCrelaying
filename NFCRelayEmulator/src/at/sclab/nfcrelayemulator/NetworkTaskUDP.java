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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.util.Log;
import android.widget.TextView;

public class NetworkTaskUDP extends NetworkTask {

	private static final String LOG_TAG = NetworkTaskUDP.class.getSimpleName();
	private static final int TIMEOUT = 20000;

	DatagramSocket serverSocket = null;

	public NetworkTaskUDP(TextView message, int port) {
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

		Log.d(LOG_TAG, "<============= doInBackground()");
		Utils.log_display(message, LOG_TAG, "starting listener socket");

		try {
			serverSocket = new DatagramSocket(getPort());
			while (true) {
				try {
					Utils.log_display(message, LOG_TAG, "waiting for reader...");
					serverSocket.setSoTimeout(TIMEOUT);
					processClient(serverSocket);
				} catch (IOException e) {
					Utils.log_display(message, LOG_TAG,
							"Error reading/writing from/to socket");
					Log.d(LOG_TAG, "Error", e);
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
	}

	private void processClient(DatagramSocket serverSocket) throws IOException,
			InterruptedException {
		byte[] receivedData = new byte[1024];
		DatagramPacket receivedPacket = new DatagramPacket(receivedData,
				receivedData.length);
		serverSocket.receive(receivedPacket);
		InetAddress client = receivedPacket.getAddress();
		int clientPort = receivedPacket.getPort();
		Utils.log_display(message, LOG_TAG, client.getHostAddress()
				+ " connected!");
		String receivedString = new String(receivedPacket.getData(), 0,
				receivedPacket.getLength());
		Utils.log_display(message, LOG_TAG, "card ready: " + receivedString);
		// TODO activate dispatcher
		synchronized (syn) {
			socketReady = true;
			syn.notify();
			// while (serverSocket.isConnected()) {
			while (true) {
				syn.wait(); // wait for cmd bytes from PCD
				successful = false;
				String stringToSend = Utils.toHex(bytesToSend);
				Utils.log_display(message, LOG_TAG, "<--" + stringToSend);
				byte[] sendBytes = stringToSend.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendBytes,
						sendBytes.length, client, clientPort);
				serverSocket.send(sendPacket);

				receivedPacket = new DatagramPacket(receivedData,
						receivedData.length);
				serverSocket.receive(receivedPacket);
				receivedString = new String(receivedPacket.getData(), 0,
						receivedPacket.getLength());

				if (receivedString == "") {
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
		}
	}

	@Override
	public void stop() {
		if (serverSocket != null)
			serverSocket.close();

		Log.d(LOG_TAG, "stopping applet thread");
		if (networkThread != null) {
			networkThread.interrupt();
			Log.d(LOG_TAG, "applet thread stopped");
		}

	}
}
