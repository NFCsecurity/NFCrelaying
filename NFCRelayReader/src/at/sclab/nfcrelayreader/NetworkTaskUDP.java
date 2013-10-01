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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.util.Log;
import android.widget.TextView;

public class NetworkTaskUDP extends NetworkTask {

	private static final String LOG_TAG = NetworkTaskUDP.class.getSimpleName();
	private static final int TIMEOUT = 30000;

	DatagramSocket clientSocket = null;

	public NetworkTaskUDP(TextView message, String uid, String ip, int port) {
		super(message, uid);
		super.setIp(ip);
		super.setPort(port);
	}

	@Override
	public void start() {

		networkThread = new Thread(this);
		networkThread.start();
	}

	private void processClient(DatagramSocket clientSocket) throws IOException,
			InterruptedException {
		Utils.log_display(message, LOG_TAG, "sending UPD packet to: " + getIp());
		Utils.log_display(message, LOG_TAG, "<--" + "UID: " + uid);
		byte[] sendBytes = uid.getBytes();
		InetAddress server = InetAddress.getByName(getIp());
		DatagramPacket sendPacket = new DatagramPacket(sendBytes,
				sendBytes.length, server, getPort());
		clientSocket.send(sendPacket);

		byte[] receivedData = new byte[1024];
		synchronized (syn) {
			socketReady = true;
			syn.notify();
			while (true) {
				successful = false;

				DatagramPacket receivedPacket = new DatagramPacket(
						receivedData, receivedData.length);
				clientSocket.receive(receivedPacket);
				String receivedString = new String(receivedPacket.getData(), 0,
						receivedPacket.getLength());
				if (receivedString == "") {
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
				sendBytes = stringToSend.getBytes();
				sendPacket = new DatagramPacket(sendBytes, sendBytes.length,
						server, getPort());
				clientSocket.send(sendPacket);
				responseReady = false;
			}
		}
	}

	@Override
	public void run() {
		try {

			try {
				Utils.log_display(message, LOG_TAG, "connecting to emulator...");
				clientSocket = new DatagramSocket();
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
		}

	}

	@Override
	public void stop() {
		if (clientSocket != null)
			clientSocket.close();

		Log.d(LOG_TAG, "stopping applet thread");
		if (networkThread != null) {
			networkThread.interrupt();
			Log.d(LOG_TAG, "applet thread stopped");
		}
	}
}
