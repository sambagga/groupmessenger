package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class GroupMessengerActivity extends Activity {
	private static final String KEY_FIELD = "key";
	private static final String VALUE_FIELD = "value";
	EditText editText;
	TextView tv;
	Integer[] destPort = new Integer[3]; // port of avd
	Handler handle = new Handler(); // to append text to textview in threads
	static int explocalSeq = 0, localSeq = 0, expSeqno = 0, seqPort = 5554;
	String selfPort, avd;
	Uri uri;
	ContentResolver conRes;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_group_messenger);
		editText = (EditText) findViewById(R.id.editText1);
		tv = (TextView) findViewById(R.id.textView1);
		tv.setMovementMethod(new ScrollingMovementMethod());
		uri = buildUri("content",
				"edu.buffalo.cse.cse486586.groupmessenger.provider");
		conRes = getContentResolver();
		// detect port of other avd to connect to, reference specs document
		selfPort = getAVD();
		if (selfPort.equals("5554"))
			avd = "avd0";
		else if (selfPort.equals("5556"))
			avd = "avd1";
		else
			avd = "avd2";
		findViewById(R.id.button1).setOnClickListener(
				new OnPTestClickListener(tv, getContentResolver()));

		// initiate the server thread
		Thread serv = new forServer();
		serv.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
		return true;
	}

	// reference PTest case
	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}

	// to identify AVD executing the app
	public String getAVD() {
		TelephonyManager tel = (TelephonyManager) getBaseContext()
				.getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(
				tel.getLine1Number().length() - 4);
		return portStr;
	}

	// get the message from EditText and send
	public void sendMessage(View view) {
		String message = editText.getText().toString();
		if (message != null && !message.isEmpty() && !message.trim().isEmpty()) {
			// initiate client thread
			while(explocalSeq!=localSeq){}
			String msgID = selfPort.concat("" + localSeq);
			Log.i("Initiate client ", "thread");
			Thread cl = new forClient(message, msgID, 1);
			cl.start();
			explocalSeq++;
			editText.setText("");
		}
	}

	// test case 1
	public void test1(View view) {
		Log.i("Test Case 1", "Initiated");
		Thread test1 = new testCase1();
		test1.start();
	}

	// test case 2
	public void test2(View view) {
		Log.i("Test Case 2", "Initiated");
		Log.i("AVD:", avd);
		while(explocalSeq!=localSeq){}
		String msg = avd + ":" + localSeq;
		String msgID = selfPort.concat("" + localSeq);
		
		Log.i("Test Case 2 Message:", msg);
		// multicast the message
		Thread cl = new forClient(msg, msgID, 3);
		cl.start();
		explocalSeq++;
		Log.i("Test Case 2 Message", "Sent");
	}

	// store in content provider
	public void storeCP(String recvMsg) {
		ContentValues cv = new ContentValues();
		cv.put(KEY_FIELD, Integer.toString(expSeqno));
		cv.put(VALUE_FIELD, recvMsg);
		conRes.insert(uri, cv);
	}

	// client thread
	class forClient extends Thread {
		String message, id;
		int type;

		forClient(String msg, String msgID, int msgType) {
			message = msg;
			id = msgID;
			type = msgType;
		}

		public void run() {
			Socket clSock;
			try {
				Log.i("Client Thread", message);
				for (int port = 11108; port <= 11116; port += 4) {
					// connect to server
					clSock = new Socket("10.0.2.2", port);
					Log.i("Sending Message type" + type + "=", message);
					// send the message to server
					PrintWriter sendData = new PrintWriter(
							clSock.getOutputStream());
					if (type == 1)
						sendData.println("%" + id + ";" + message);
					if (type == 2)
						sendData.println("$" + id + ";" + message);
					if (type == 3)
						sendData.println("@" + id + ";" + message);
					sendData.flush();
					sendData.close();
					Log.i("Message sent=", message);
				}
				if (type == 1 || type == 3) {
					clSock = new Socket("10.0.2.2", 11108);
					Log.i("Sending Message to sequencer=", id);
					// send the message to sequencer
					PrintWriter sendData = new PrintWriter(
							clSock.getOutputStream());
					sendData.println("#" + id);
					sendData.flush();
					sendData.close();
					localSeq++;
					Log.i("Message sent to sequencer=", id);
				}
				
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				handle.post(new Runnable() {
					public void run() {
						tv.append("Number format Exception!\n");
					}
				});
				e.printStackTrace();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				handle.post(new Runnable() {
					public void run() {
						tv.append("Unknown Host Exception!\n");
					}
				});
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				handle.post(new Runnable() {
					public void run() {
						tv.append("I/O error occured when creating the socket!\n");
					}
				});
				e.printStackTrace();
			} catch (SecurityException e) {
				handle.post(new Runnable() {
					public void run() {
						tv.append("Security Exception!\n");
					}
				});
				e.printStackTrace();
			}

		}
	}

	// test case 1
	class testCase1 extends Thread {
		public void run() {
			Log.i("AVD:", avd);
			for (int i = 0; i < 5; i++) {
				while(explocalSeq!=localSeq){}
				String msg = avd + ":" + localSeq;
				String msgID = selfPort.concat("" + localSeq);
				Log.i("Test Case 1 Message:", msg);
				// multicast the message
				Thread cl = new forClient(msg, msgID, 1);
				cl.start();
				explocalSeq++;
				Log.i("Test Case 1 Message", "Sent");
				// sleep for 3 secs
				try {
					Thread.sleep(3000);
					Log.i("Sleep", "Over");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	// server thread
	class forServer extends Thread {
		HashMap<String, String> seqBuffer = new HashMap<String, String>();
		HashMap<String, String> holdBack = new HashMap<String, String>();
		int seqNo = 0;
		int test = 0;

		public void run() {
			try {
				// start tracker
				Log.i("Initiate tracker ", "thread");
				Thread track = new tracker();
				track.start();

				// open connection on port 10000
				ServerSocket serSock = new ServerSocket(10000);
				Log.d("Starting Server", "Forserver");
				while (true) {
					// listen for client
					Socket recvSock = serSock.accept();
					Log.i("Connection", "Accepted");
					// get the message
					InputStreamReader readStream = new InputStreamReader(
							recvSock.getInputStream());
					BufferedReader recvInp = new BufferedReader(readStream);
					Log.i("Reader", "Initialized");
					String recvMsg = recvInp.readLine();
					Log.i("Received Message:", recvMsg);
					// recognise message type
					switch (recvMsg.charAt(0)) {
					case '%':
						holdbackQue(recvMsg.substring(1));
						break;
					case '$':
						storeSeq(recvMsg.substring(1));
						break;
					case '@':
						holdbackQue(recvMsg.substring(1));
						test = 1;
						break;
					case '#':
						sequencer(recvMsg.substring(1));
						break;
					}
					recvSock.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				handle.post(new Runnable() {
					public void run() {
						tv.append("I/O error occured when creating the socket!\n");
					}
				});
				e.printStackTrace();
			}

		}

		// hold back queue
		public void holdbackQue(String msg) {
			StringTokenizer sTok = new StringTokenizer(msg, ";");
			String msgID = sTok.nextToken();
			String value = sTok.nextToken();
			Log.i("Hold Back", "Queue " + msgID + " " + value);
			holdBack.put(msgID, value);
		}

		// Make buffer for sequence no and message_id
		public void storeSeq(String msg) {
			StringTokenizer sTok = new StringTokenizer(msg, ";");
			String msgID = sTok.nextToken();
			String seqNo = sTok.nextToken();
			Log.i("Store", "Sequence " + msgID + " " + seqNo);
			seqBuffer.put(seqNo, msgID);
		}

		// sequencer algorithm
		public void sequencer(String msgID) {
			Log.i("Initiate", "sequencer " + msgID);
			Thread cl = new forClient("" + seqNo, msgID, 2);
			cl.start();
			seqNo++;
		}

		// test case 2, multicast 3 messages
		public void send3msg() {
			Log.i("AVD:", avd);
			for (int i = 0; i < 2; i++) {
				while(explocalSeq!=localSeq){}
				String msg = avd + ":" + localSeq;
				String msgID = selfPort.concat("" + localSeq);
				Log.i("Test Case 2 Message:", msg);
				// multicast the message
				Thread cl = new forClient(msg, msgID, 1);
				cl.start();
				explocalSeq++;
				Log.i("Test Case 2 Message", "Sent");
			}
		}

		// tracker thread
		class tracker extends Thread {
			public void run() {
				Log.i("Running", "Tracker");
				while (true) {
					// Log.i("Buffer:"+seqBuffer,"Expected sequence no:"+expSeqno);
					if (seqBuffer.get("" + expSeqno) != null) {
						Log.i("Tracker Thread", "Match " + expSeqno);
						final String seq = "" + expSeqno;
						final String msgID = seqBuffer.get("" + expSeqno);
						final String value = holdBack.get(msgID);
						if (value != null) {
							handle.post(new Runnable() {
								public void run() {
									tv.append(seq + "." + msgID + ":" + value
											+ "\n");
								}
							});
							storeCP(value);
							expSeqno++;
							if (test == 1) {
								send3msg();
								test = 0;
							}
						}
					}
				}
			}
		}// end of tracker thread
	}// end of forServer thread
}// end of GroupMessengerActivity class
