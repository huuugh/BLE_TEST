package com.zjy.ble_test.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

public class BlueToothManager {
	public BlueToothManager(Context context) {
		mContext = context;
		// 获取蓝牙适配器对象实例
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// 注册Receiver来获取蓝牙设备相关的结果
		IntentFilter intent = new IntentFilter();
		intent.addAction(BluetoothDevice.ACTION_FOUND);// 用BroadcastReceiver来取得搜索结果
		intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		mContext.registerReceiver(mSearchDevicesReceiver, intent);
	}

	private Context mContext;
	/** 蓝牙适配器对象实例 */
	public BluetoothAdapter mBluetoothAdapter;
	private AcceptThread mServerThread;
	private static BluetoothSocket mTempSocket;
	private static BluetoothSocket mBluetoothSocket;
	/** 蓝牙通讯的套接字 */
	private BluetoothServerSocket serverSocket;
	/***************************************************************/
	private boolean isRecording;// 线程控制标记
	private Handler mHandler;
	private OutputStream outStream;//发送数据输出流
	private InputStream inStream;// 蓝牙数据输入流
	private ConnectedThread mConnectedThread;// 连接设备线程
	private static final int READ_DATA_SUCCESS = 101;//数据读取成功
	private static final int CONN_OFF = 100;// 蓝牙连接断开

	/**
	 * 关闭广播，线程,socket，流
	 */
	public void closeData() {
		try {
			mContext.unregisterReceiver(mSearchDevicesReceiver);
		} catch (IllegalArgumentException e) {
			if (e.getMessage().contains("Receiver not registered")) {
			} else {
				throw e;
			}
		}
		bluetoothOffClossData();
	}

	/**
	 * 蓝牙断开需要关闭的数据
	 */
	private void bluetoothOffClossData(){
		if (inStream != null) {
			try {
				inStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		isRecording=false;
		if (mConnectedThread != null) {//接收数据服务线程
			mConnectedThread.interrupt();//线程中断
		}
		if (outStream != null) {//发送数据输出流
			try {
				outStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (serverSocket != null) {//取消套接字连接，然后线程返回
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (mServerThread != null) {//蓝牙连接线程
			mServerThread.interrupt();
		}
		if (mTempSocket != null) {
			try {
				mTempSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (mBluetoothSocket != null) {
			try {
				mBluetoothSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/***************************************************************/
	/******************** 以下是连接设备代码 ********************/
	/***************************************************************/
	/***************************************************************/
	/**
	 * 检测蓝牙设备是否开启
	 */
	public boolean checkBlueToothIsOpen() {
		if (mBluetoothAdapter == null) {
			Toast.makeText(mContext, "Bluetooth device not found", Toast.LENGTH_SHORT).show();
			return false;
		} else {// 判断设备是否开启
			return mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON;
		}
	}

	/**
	 * 检测蓝牙设备连接状态，true：已连接，false：未连接
	 */
	public boolean getBlueToothConnState() {
		boolean isConn = false;
		if (mBluetoothSocket != null) {
			isConn = mBluetoothSocket.isConnected();
		}
		return isConn;
	}

	/**
	 * 初始化设备,设备已开启,开启服务线程,扫描设备
	 */
	public void initBlueTooth() {
		if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
			// 开启bluetoothServerSocket服务端监听，保持连接直到异常发生或套接字返回
			mServerThread = new AcceptThread();
			// 开始执行AcceptThread中的run（）方法
			mServerThread.start();
			scanBlueTooth();
		}
	}

	/**
	 * 开启设备
	 */
	public void openBlueTooth(Context con) {
		if (mBluetoothAdapter != null) {
			if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF && !mBluetoothAdapter.isEnabled()) {
				// 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				con.startActivity(enableBtIntent);
			}
		}
	}

	/**
	 * 扫描设备
	 */
	public void scanBlueTooth() {
		mBluetoothAdapter.startDiscovery(); // 开始查找蓝牙设备
	}

	/** 获取蓝牙设备相关的结果 */
	public BroadcastReceiver mSearchDevicesReceiver = new BroadcastReceiver() {

		@Override  
		public void onReceive(Context context, Intent intent) {  
		    if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				String deviceName = device.getName();
				String deviceAddress = device.getAddress();
				String deviceData = deviceName + "|" + deviceAddress;
				//Lg.e("**mSearchDevices",deviceData);

				/**
				 * 取消判断是3.0,还是4.0,使其全部显示
				 *
				 * --------hugh
				 */
				//if (deviceAddress.substring(3, 5).equals("03")) {
					//Lg.e("**mSearchDevices03",deviceData);
					// 4接口方法回调数据
					// 每当搜索到新设备，更新数据
					if (mOnBlueToothSearchListener != null) {
						mOnBlueToothSearchListener.searchDevice(deviceData);
					}
				//}
		   }
//		    else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {  
//		        // 状态改变的广播  
//			   Lg.e("=========", "状态改变的广播");
//		        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);  
//		        if (device.getName().equalsIgnoreCase(lastAddress)) {   
//		            int bondState = device.getBondState();  
//		            switch (bondState) {  
//		                case BluetoothDevice.BOND_NONE:  
//		                    break;  
//		                case BluetoothDevice.BOND_BONDING:  
//		                    break;  
//		                case BluetoothDevice.BOND_BONDED:  
//		                    // 连接  
//		                    connectionDevice(lastAddress);
//		                    Lg.e("ACTION_BOND_STATE_CHANGED", "ACTION_BOND_STATE_CHANGED");
//		                    break;  
//		            }  
//		        }  
//		    }
		}  
	};
	
	// 3声明成员变量
	private OnBlueToothSearchListener mOnBlueToothSearchListener;
	
	// 2获取接口对象
	public void setOnBlueToothSearchListener(OnBlueToothSearchListener listener) {
		mOnBlueToothSearchListener = listener;
	}

	// 1创建回调接口
	public interface OnBlueToothSearchListener {
		void searchDevice(String device);
	}

	/**
	 * 完成连接bluetoothSocket
	 */
	private void manageConnectedSocket() {
		// 检测到蓝牙接入
		mBluetoothSocket = mTempSocket;
		// 发送接收线程启动
		openConnection();
	}

	/**
	 * 连接蓝牙
	 */
	class AcceptThread extends Thread {
		public AcceptThread() {
			// Use a temporary object that is later assigned to mmServerSocket,
			// because mmServerSocket is final
			BluetoothServerSocket tmp = null;// 使用一个临时对象代替，因为cwjServerSocket定义为final
			try {
				Method listenMethod = mBluetoothAdapter.getClass().getMethod("listenUsingRfcommOn",
						new Class[] { int.class });
				tmp = (BluetoothServerSocket) listenMethod.invoke(mBluetoothAdapter, Integer.valueOf(1));
			} catch (Exception e) {
				e.printStackTrace();
			}
			serverSocket = tmp;
		}

		public void run() {
			// Keep listening until exception occurs or a socket is returned
			// mState!=STATE_CONNECTED
			while (true) {// 保持连接直到异常发生或套接字返回
				try {
					if (serverSocket != null) {
						mTempSocket = serverSocket.accept(); // 如果一个连接同意
					}
					// If a connection was accepted
					if (mTempSocket != null) {
						// Do work to manage the connection (in a separate
						// thread)
						manageConnectedSocket(); // 管理一个已经连接的RFCOMM通道在单独的线程。
						serverSocket.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
		}
	}

	/**
	 * 停止搜索设备
	 */
	public void cancelDiscovery() {
		mBluetoothAdapter.cancelDiscovery();
	}

	/**
	 * 连接选中的设备
	 * 
	 * @param address
	 *            设备MAC地址
	 */
	public void connectSelectDevice(String address) {
		if(address!=null&&address.length()>0){
			BluetoothDevice btDev = mBluetoothAdapter.getRemoteDevice(address);
			Method m;
			try {
				m = btDev.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
				mBluetoothSocket = (BluetoothSocket) m.invoke(btDev, Integer.valueOf(1));
				cancelDiscovery();// 停止搜索设备
				mBluetoothSocket.connect();
				// 打开发送接收连接
				openConnection();
			}catch (Exception e) {
				//TODO 连接失败处理
				//Lg.e("**connectionFailure",address);//连接失败的物理地址
				e.printStackTrace();
			}
		}
	}

	/***************************************************************/
	/******************** 以下是发送接受数据代码 ********************/
	/***************************************************************/

	/**
	 * 发送接收线程启动
	 */
	private void openConnection() {
		mHandler = new Handler(mContext.getMainLooper()) {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case READ_DATA_SUCCESS:
					String readData = (String) msg.obj;
					if (mOnBlueToothDataChangeListener != null) {
						mOnBlueToothDataChangeListener.dataChange(readData);
						//Lg.e("**receiveMessage=", readData);
					}
					break;
				case CONN_OFF:
					Toast.makeText(mContext, "蓝牙连接已断开", Toast.LENGTH_SHORT).show();
					if(mOnBlueToothConnStateChangeListener!=null){
						mOnBlueToothConnStateChangeListener.connStateChange(getBlueToothConnState());//蓝牙链接断开
					}
				}
			};
		};
		mOnBlueToothConnStateChangeListener.connStateChange(getBlueToothConnState());//蓝牙链接成功
		mConnectedThread = new ConnectedThread();
		mConnectedThread.Start();
	}

	class ConnectedThread extends Thread {

		private int wait;
		private Thread thread;

		public ConnectedThread() {
			isRecording = false;
			this.wait = 50;
			thread = new Thread(new ReadRunnable());
		}

		public void Start() {
			isRecording = true;
			State state = thread.getState();
			if (state == State.NEW) {
				thread.start();
			} else
				thread.resume();
		}

		private class ReadRunnable implements Runnable {

			public void run() {
				while (isRecording) {
					try {
						inStream = mBluetoothSocket.getInputStream();
						int length = 20;
						byte[] temp = new byte[length];
						// 读编码
						if (inStream != null) {
							int len = inStream.read(temp, 0, length - 1);
							if (len > 0) {
								byte[] btBuf = new byte[len];
								System.arraycopy(temp, 0, btBuf, 0, btBuf.length);
								String readString = printHexString(btBuf);
								mHandler.obtainMessage(READ_DATA_SUCCESS, len, -1, readString).sendToTarget();
							}
							Thread.sleep(wait);// 延时一定时间缓冲数据
						}
					} catch (Exception e) {
						mHandler.obtainMessage(CONN_OFF).sendToTarget();
						bluetoothOffClossData();//蓝牙断开连接,需要把所有连接都关闭,等待重新连接
					}
				}
			}
		}
	}

	/**
	 * 通过设备发送数据
	 * 
	 * @param message
	 *            需要发送的数据
	 */
	public void sendMessage(String message) {
		try {
			//Lg.e("**sendMessage=",message);
			outStream = mBluetoothSocket.getOutputStream();
			outStream.write(hexStringToByte(message));
		} catch (IOException e) {// 输出流创建失败
			Toast.makeText(mContext, "发送数据失败", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 输入10进制数组，输出16进制格式的字符串
	 * 
	 * @param b
	 *            10进制字符数组 [90,1,1,1,1,0,0,88,77]相当于[W----XM]
	 * @return 16进制格式的字符串 [5A010101010000584D]
	 */
	public String printHexString(byte[] b) {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			String hex = Integer.toHexString(b[i] & 0xFF);// 取低8位数据，同时转换为16进制字符串
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			result += hex.toUpperCase();// 5A
		}
		return result;
	}

	/**
	 * 输入10进制，输出16进制格式的字符
	 * 
	 * @param c
	 *            10进制 [10]
	 * @return 16进制格式的字符 [a]
	 */
	private static byte toByte(char c) {
		byte b = (byte) "0123456789ABCDEF".indexOf(c);
		return b;
	}

	/**
	 * 输入16进制格式的字符串，输出10进制数组
	 * 
	 * @param hex
	 *            16进制格式的字符串 [5A010101010000584D]
	 * @return 10进制数组 [90,1,1,1,1,0,0,88,77]
	 */
	public static byte[] hexStringToByte(String hex) {
		int len = (hex.length() / 2);
		byte[] result = new byte[len];
		char[] chars = hex.toCharArray();
		for (int i = 0; i < len; i++) {
			int pos = i * 2;
			result[i] = (byte) (toByte(chars[pos]) << 4 | toByte(chars[pos + 1]));
		}
		return result;
	}

	/**
	 * 16进制形式的String字符转ASCLL字符串
	 **/
	public static String toStringHex(String s) {
		byte[] baKeyword = new byte[s.length() / 2];
		for (int i = 0; i < baKeyword.length; i++) {
			try {
				baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			s = new String(baKeyword, "utf-8");// 十进制数转为ASCII字符串
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return s;
	}

	// 声明监听对象
	OnBlueToothDataChangeListener mOnBlueToothDataChangeListener;

	// 获取接口对象
	public void setOnBlueToothDataChangeListener(OnBlueToothDataChangeListener listener) {
		mOnBlueToothDataChangeListener = listener;
	}

	// 创建回调接口
	public interface OnBlueToothDataChangeListener {
		void dataChange(String data);
	}

	/**************************************************/
	// 声明监听对象
	OnBlueToothConnStateChangeListener mOnBlueToothConnStateChangeListener;

	// 获取接口对象
	public void setOnBlueToothConnStateChangeListener(OnBlueToothConnStateChangeListener listener) {
		mOnBlueToothConnStateChangeListener = listener;
	}

	// 创建回调接口
	public interface OnBlueToothConnStateChangeListener {
		void connStateChange(Boolean connState);
	}
}
