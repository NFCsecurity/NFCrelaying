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
import android.widget.TextView;

public abstract class NetworkTask implements Runnable {

	private String ip = "192.168.43.1";
	private int port = 1337;

	TextView message;
	String uid;

	Object syn = new Object();
	byte[] bytesToSend;
	byte[] receivedBytes;
	boolean socketReady = false;
	boolean successful = false;
	boolean responseReady = false;
	boolean cmdReady = false;
	Thread networkThread;

	public NetworkTask(TextView message, String uid) {
		this.message = message;
		this.uid = uid;
	}

	public byte[] receiveCmd() throws IOException {
		synchronized (syn) {
			try {
				while (!socketReady) {
					syn.wait(); // wait for socket ready
				}
				while (!cmdReady) {
					syn.wait(); // wait for network thread
				}
				cmdReady = false;
				if (!successful) {
					throw new IOException("error reading/writing");
				}
			} catch (InterruptedException e) {
				throw new IOException("thread error", e);
			}
			return receivedBytes;
		}
	}

	public void sendResponse(byte[] cmd) throws IOException {
		try {
			synchronized (syn) {
				while (!socketReady) {
					syn.wait();
				}
				bytesToSend = cmd;
				responseReady = true;
				syn.notify(); // notify network thread
				if (!successful) {
					throw new IOException("error reading/writing");
				}
			}
		} catch (InterruptedException e) {
			throw new IOException("thread error", e);
		}
	}

	public abstract void start();

	public abstract void stop();

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
}
