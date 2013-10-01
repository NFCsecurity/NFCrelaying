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

import android.util.Log;
import android.widget.TextView;

public class Utils {

	public static String toHex(byte[] bytes) {
		StringBuffer buff = new StringBuffer();
		for (byte b : bytes) {
			buff.append(String.format("%02X", b));
		}

		return buff.toString();
	}
	
	//http://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
	public static byte[] toBytes(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}

	public static void log_display(final TextView message, String tag,
			final String log_message) {
		message.post(new Runnable() {

			@Override
			public void run() {
				message.append(log_message + "\n");

			}
		});
		Log.d(tag, log_message);
	}

}
