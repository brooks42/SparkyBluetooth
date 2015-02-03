package com.archrival.sparkybluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class BluetoothSetupActivity extends Activity implements OnClickListener {

	public static final String COMMAND_RUN = "RUN~";
	public static final String COMMAND_FAST = "FAST~";
	public static final String COMMAND_LEFT = "LEFT~";
	public static final String COMMAND_RIGHT = "RIGHT~";
	public static final String COMMAND_STOP = "STOP~";

	public static final String TAG = "BluetoothTest";

	public static final int REQUEST_ENABLE_BT = 1;
	public static final String DEVICE_ADDRESS = "20:13:11:01:20:10";

	BroadcastReceiver deviceFindReceiver;
	private UUID app_UUID;

	public BluetoothAdapter bluetoothAdapter;
	private BluetoothSocket livingSocket;

	private ConnectionThread thread;

	private TestClient client;

	public TextView outputText;

	/**
	 * 
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth_setup);

		/*
		 * findViewById(R.id.go_btn).setOnClickListener(this);
		 * findViewById(R.id.stop_btn).setOnClickListener(this);
		 * findViewById(R.id.connect_btn).setOnClickListener(this);
		 * findViewById(R.id.run_btn).setOnClickListener(this);
		 * findViewById(R.id.left_btn).setOnClickListener(this);
		 * findViewById(R.id.right_btn).setOnClickListener(this);
		 * 
		 * outputText = (TextView)findViewById(R.id.outputTracker);
		 */
		findViewById(R.id.connectButton).setOnClickListener(this);

		app_UUID = UUID.randomUUID();
	}

	/**
	 * 
	 */
	public void onResume() {
		super.onResume();
		System.out.println("onResume()");
		System.out.println("Restarting client...");
		client = new TestClient(this);
		new Thread(client).start();
		System.out.println("Restarting client complete");
	}

	/**
	 * 
	 */
	@Override
	protected void onDestroy() {
		// disconnect();
		super.onDestroy();
		// kill the test client here
		unregisterReceiver(deviceFindReceiver);
	}

	/**
	 * 
	 */
	@Override
	public void onClick(View view) {
		/*
		 * if (view == findViewById(R.id.go_btn)) {
		 * performCommand(COMMAND_FAST); } if (view ==
		 * findViewById(R.id.stop_btn)) { performCommand(COMMAND_STOP); }
		 */
		if (view == findViewById(R.id.connectButton)) {
			Log.d(TAG, "Connecting...");

			bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

			if (!bluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			} else {
				connect();
			}
		}
		/*
		 * if (view == findViewById(R.id.run_btn)) {
		 * performCommand(COMMAND_RUN); } if (view ==
		 * findViewById(R.id.left_btn)) { performCommand(COMMAND_LEFT); } if
		 * (view == findViewById(R.id.right_btn)) {
		 * performCommand(COMMAND_RIGHT); }
		 */
	}

	/**
	 * 
	 */
	public void connect() {
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter
				.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
			// Loop through paired devices
			for (BluetoothDevice device : pairedDevices) {
				// this implies there will only ever be 1 bluetooth device paired with this phone
				Log.d("Main", "Paired with device " + device.getName() + ": "
						+ device.getAddress());
				try {
					Log.d("Main",
							"Attempting connection with device "
									+ device.getAddress());
					livingSocket = bluetoothAdapter
							.getRemoteDevice(device.getAddress())
							.createRfcommSocketToServiceRecord(
									UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
					manageConnectedSocket(livingSocket);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 
	 * @param action
	 */
	public void performCommand(String action) {
		if (thread != null) {
			thread.write(action.getBytes());
		}
	}

	public void obtainMessage(int bytes, int start, final byte[] array) {
		Log.d("obtainMessage()", bytes + ", " + start + ", "
				+ new String(array));

		BluetoothSetupActivity.this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				outputText.append(new String(array));
			}
		});
	}

	/**
	 * 
	 * @param socket
	 */
	public void manageConnectedSocket(BluetoothSocket socket) {
		Log.d("Main", "Managing connected socket");
		try {
			socket.connect();

			Log.d("manageConnectedSocket()", "Connection acquired");

			thread = new ConnectionThread(socket);
			thread.start();
		} catch (IOException e) {
			Log.d("Main", "Oh no!\n" + e.getMessage());
		}
	}

	/**
	 * 
	 * @author Chris
	 * 
	 */
	class BluetoothAccepter extends Thread {

		private BluetoothServerSocket serverSocket;

		public BluetoothAccepter() {
			// create a server socket
			try {
				serverSocket = bluetoothAdapter
						.listenUsingRfcommWithServiceRecord("Swarmtech Node",
								app_UUID);
			} catch (IOException e) {
				Log.d("Main",
						"BluetoothAccepter threw IOException in constructor: "
								+ e.getMessage());
			}
		}

		public void run() {
			Log.d("Bluetooth Acceptor", "Server started");
			BluetoothSocket socket = null;
			// Keep listening until exception occurs or a socket is returned
			while (true) {
				try {
					Log.d("Bluetooth Acceptor", "Waiting to accept...");
					socket = serverSocket.accept();
					// If a connection was accepted
					if (socket != null) {
						Log.d("Bluetooth Acceptor", "Accepted");
						// Do work to manage the connection (in a separate
						// thread)
						manageConnectedSocket(socket);
						serverSocket.close();
					}
				} catch (IOException e) {
					break;
				}
			}
			Log.d("Bluetooth Acceptor", "Server done");
		}

		public void kill() {
			try {
				serverSocket.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * 
	 * @author Chris
	 * 
	 */
	class ConnectionThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectionThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();

				tmpOut.write(new String("Hello").getBytes());
			} catch (IOException e) {
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			byte[] buffer = new byte[1024]; // buffer store for the stream
			int bytes; // bytes returned from read()

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer);
					// Send the obtained bytes to the UI activity
					obtainMessage(bytes, -1, buffer);
				} catch (IOException e) {
					break;
				}
			}
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(byte[] bytes) {
			try {
				Log.d("ConnectionThread", "Writing bytes: " + new String(bytes));
				mmOutStream.write(bytes);
			} catch (IOException e) {
				Log.e("EXCEPTION", e.getMessage());
			}
		}

		/* Call this from the main activity to shutdown the connection */
		public void cancel() {
			Log.d("ConnectionThread", "Cancel");
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}
}
