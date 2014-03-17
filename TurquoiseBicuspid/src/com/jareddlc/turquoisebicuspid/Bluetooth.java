package com.jareddlc.turquoisebicuspid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class Bluetooth {
	private static final String LOG_TAG = "TurquoiseBicuspid:Bt";
	//private static String deviceName = "TurquoiseBicuspid";
	private static String deviceMAC = "20:13:12:06:90:58";
	private static UUID mDeviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static Set<BluetoothDevice> pairedDevices;
	private static BluetoothAdapter mBluetoothAdapter;
	private static BluetoothDevice mBluetoothDevice;
	private static BluetoothSocket mSocket;
	private static InputStream mInStream;
    private static OutputStream mOutStream;
    private static ConnectThread connect;
    private static ConnectedThread conx;
	public boolean isEnabled = false;
	public static CharSequence[] pairedEntries;
	public static CharSequence[] pairedEntryValues;
	
	public Bluetooth() {
		Log.d(LOG_TAG, "Initializing Bluetooth");
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter == null) {
			Log.d(LOG_TAG, "Bluetooth not supported.");
		}
		if(mBluetoothAdapter.isEnabled()) {
		    isEnabled = true;
		}
		else {
			Log.d(LOG_TAG, "Bluetooth is not enabled.");
			isEnabled = false;
		}
		getPaired();
	}
	
	public void enableBluetooth() {   
		if(!mBluetoothAdapter.isEnabled()) {
			mBluetoothAdapter.enable();
			isEnabled = true;
			getPaired();
		}
	}
	
	public void disableBluetooth() {
		mBluetoothAdapter.disable();
		isEnabled = false;
	}
	
	public void getPaired() {
		if(isEnabled) {
			pairedDevices = mBluetoothAdapter.getBondedDevices();
			if(pairedDevices.size() > 0) {
				List<CharSequence> entries = new ArrayList<CharSequence>();
				List<CharSequence> values = new ArrayList<CharSequence>();
				
			    // loop through paired devices
			    for(BluetoothDevice device : pairedDevices) {
			    	Log.d(LOG_TAG, "Device: "+device.getName()+":"+device.getAddress());
			    	entries.add(device.getName());
			    	values.add(device.getAddress());
			    	
			        if(device.getAddress().equals(deviceMAC)) {
			        	Log.d(LOG_TAG, "Set device: "+device.getName()+":"+device.getAddress());
			        	mBluetoothDevice = device;
			        }
			    }
			    pairedEntries = entries.toArray(new CharSequence[entries.size()]);
			    pairedEntryValues = values.toArray(new CharSequence[values.size()]);
			}
		}
	}
	
	public CharSequence[] getEntries() {
		return pairedEntries;
    }

    public CharSequence[] getEntryValues() {
		return pairedEntryValues;
    }
	
	public void send(String str) {
		Log.d(LOG_TAG, "Sending");
		conx.write(str.getBytes());
	}
	
	public void btDisconnect() {
		conx.close();
		connect.close();
	}
	
	public void btConnect() {
		if(isEnabled) {
			Log.d(LOG_TAG, "Spawning ConnectThread");
			connect = new ConnectThread(mBluetoothDevice);
			connect.start();
		}
		else {
			Log.d(LOG_TAG, "Bt Not enabled");
		}
	}
	
	public void btConnected() {
		Log.d(LOG_TAG, "Spawning ConnectedThread");
		ConnectedThread connected = new ConnectedThread(mSocket);
		connected.start();
	}
	
	private class ConnectThread extends Thread {
	    public ConnectThread(BluetoothDevice device) {
	    	Log.d(LOG_TAG, "Initializing ConnectThread");
	        
	        // get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	        	Log.d(LOG_TAG, "try ConnectThread: "+device.getName()+" with UUID: "+mDeviceUUID.toString());
	        	mSocket = device.createRfcommSocketToServiceRecord(mDeviceUUID);
	        }
	        catch(Exception e) {
	        	Log.e(LOG_TAG, "Error: device.createRfcommSocketToServiceRecord", e);
	        }
	    }
	 
	    public void run() {
	    	Log.d(LOG_TAG, "Running ConnectThread");
	        // Cancel discovery because it will slow down the connection
	        mBluetoothAdapter.cancelDiscovery();
	 
	        try {
	            // connect the device through the socket. This will block until it succeeds or throws an exception
	        	mSocket.connect();
	        }
	        catch(IOException connectException) {
	        	Log.e(LOG_TAG, "Error: mmSocket.connect()", connectException);
	            try {
	            	mSocket.close();
	            }
	            catch (IOException closeException) {
	            	Log.e(LOG_TAG, "Error: mmSocket.close()", closeException);
	            }
	            return;
	        }
	 
	        // Do work to manage the connection
	        conx = new ConnectedThread(mSocket);
	        conx.start();
	        String str = "Android connected.";
	        conx.write(str.getBytes());
	    }
	    	 
	    public void close() {
	        try {
	        	mSocket.close();
	        }
	        catch (IOException e) {
	        	Log.e(LOG_TAG, "Error: mmSocket.close()", e);
	        }
	    }
	}
	
	private class ConnectedThread extends Thread {
	    public ConnectedThread(BluetoothSocket socket) {
	    	Log.d(LOG_TAG, "Initializing ConnectedThread");
	 
	        // get the input and output streams
	        try {
	        	mInStream = socket.getInputStream();
	        	mOutStream = socket.getOutputStream();
	        }
	        catch(IOException e) {
	        	Log.e(LOG_TAG, "Error: socket.getInputStream()/socket.getOutputStream()", e);
	        }
	    }
	 
	    public void run() {
	    	Log.d(LOG_TAG, "Running ConnectedThread");
	        byte[] buffer = new byte[1024];
	        //int bytes;
	 
	        // listen to the InputStream
	        while(true) {
	            try {
	            	mInStream.read(buffer);
	                //bytes = mInStream.read(buffer);
					// Send the obtained bytes to the UI activity
	                //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
	            }
	            catch(IOException e) {
	            	Log.e(LOG_TAG, "Error: mInStream.read()", e);
	                break;
	            }
	        }
	    }
	 
	    public void write(byte[] bytes) {
	        try {
	        	Log.d(LOG_TAG, "writting: "+bytes);
	            mOutStream.write(bytes);
	        }
	        catch(IOException e) {
	        	Log.e(LOG_TAG, "Error: mOutStream.write()", e);
	        }
	    }
	 
	    public void close() {
	        try {
	            mSocket.close();
	        }
	        catch(IOException e) {
	        	Log.e(LOG_TAG, "Error: mSocket.close()", e);
	        }
	    }
	}
}


