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

import android.widget.TextView;

public abstract class NetworkTask implements Runnable {

	private int port = 1337;
	TextView message;
	Object syn = new Object();
	byte[] bytesToSend;
	byte[] receivedBytes;
	boolean socketReady = false;
	boolean successful = false;
	Thread networkThread;

	public NetworkTask(TextView message) {
		this.message = message;
	}

	public byte[] transceive(byte[] cmd) throws IOException,
			InterruptedException {
		synchronized (syn) {
			while (!socketReady) {
				syn.wait();
			}
			bytesToSend = cmd;
			syn.notify(); // notify network thread
			syn.wait(); // wait for network thread
			if (!successful) {
				throw new IOException("error reading/writing");
			}
			return receivedBytes;
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

}
