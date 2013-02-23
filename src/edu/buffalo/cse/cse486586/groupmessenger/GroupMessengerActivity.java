package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;


import android.app.Activity;
import android.content.Context;
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
	EditText editText;
	TextView tv;
	Integer[] destPort = new Integer[2]; // port of avd
	String recvMsg;
	Handler handle = new Handler(); // to append text to textview in threads

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_group_messenger);
		editText = (EditText) findViewById(R.id.editText1);
		tv = (TextView) findViewById(R.id.textView1);
		tv.setMovementMethod(new ScrollingMovementMethod());

		// detect port of other avd to connect to, reference specs document
		TelephonyManager tel = (TelephonyManager) getBaseContext()
				.getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(
				tel.getLine1Number().length() - 4);
		if (portStr.equals("5554")) {
			destPort[0] = 11112;
			destPort[1] = 11116;
		} else if (portStr.equals("5556")) {
			destPort[0] = 11108;
			destPort[1] = 11116;
		} else if (portStr.equals("5558")) {
			destPort[0] = 11108;
			destPort[1] = 11112;
		}

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

	// get the message from EditText and send
	public void sendMessage(View view) {
		String message = editText.getText().toString();
		if (message != null && !message.isEmpty() && !message.trim().isEmpty()) {
			tv.append("Me:" + message + "\n");
			// initiate client thread
			for (int i = 0; i < 2; i++) {
				Log.i("Initiate client ","thread");
				Thread cl = new forClient(message, destPort[i]);
				cl.start();
				editText.setText("");
			}
		}
	}

	// client thread
	class forClient extends Thread {
		String message;
		Integer destPort;

		forClient(String msg, Integer port) {
			message = msg;
			destPort = port;
		}

		public void run() {
			Socket clSock;
			try {
				Log.i("Client Thread", message);
				// connect to server
				clSock = new Socket("10.0.2.2", destPort);
				Log.i("Sending Message=", message);
				// send the message to server
				PrintWriter sendData = new PrintWriter(clSock.getOutputStream());
				sendData.println(message);
				sendData.flush();
				sendData.close();
				Log.i("Message sent=", message);

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

	// server thread
	class forServer extends Thread {
		public void run() {
			try {
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
					recvMsg = recvInp.readLine();
					Log.i("Received Message=", recvMsg);
					// display text in TextView widget
					handle.post(new Runnable() {
						public void run() {
							tv.append("From:" + recvMsg + "\n");
						}
					});
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
	}
}
