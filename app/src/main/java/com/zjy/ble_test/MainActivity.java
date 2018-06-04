package com.zjy.ble_test;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.hugh.blelib.BleService;
import com.zjy.ble_test.bluetooth.BlueToothInstructUtils;
import com.zjy.ble_test.bluetooth.BlueToothManager;
import com.zjy.ble_test.bluetooth.MyGattAttributes;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static Integer RECONNECT = 222;
    private static boolean IS_OVER_24 = false;
    private DrawerLayout mDrawerLayout;
    private ArrayAdapter<String> adapter;
    private ListView leftDrawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private FrameLayout fl_main;
    private FragmentManager mFragmentMan;
    private Fragment mContent;
    private Toolbar toolbar;
    private MainActivity mActivity;
    private ArrayList<Date> Records = new ArrayList<>();

    private static final String TAG = "MainActivity";

    public static BlueToothManager mBlueToothManager;
    private Set<String> mDevices = new HashSet();
    private List<String> mDevicesName = new ArrayList();
    private Map<String, String> mDeviceMAC = new HashMap();
    private int CurrentFragment;


    private List<Map<String, Object>> deviceList;
    private List<String> serviceList;
    private List<String[]> characteristicList;
    private ProgressDialog progressDialog;

    private Boolean connstate;
    private String if_address;
    private String if_name;
    private ListView lv_scanresult;

    private BluetoothGattCharacteristic character;
    private int pro;
    private String charUuid;
    private BluetoothGattCharacteristic TransferCharacter;
    private BleService mBleService;

    //Constant
    public static final int SERVICE_BIND = 1;
    public static final int SERVICE_SHOW = 2;
    public static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    public static final int SCAN_FINISH = 3;
    public static final int RE_CONNECT = 4;
    private AlertDialog.Builder result_dialog;
    private List<BluetoothGattService> gattServiceList;
    private LinkedList<String> commands;
    private String receive;
    private String[] datas;
    public static boolean isConnected;
    private AlertDialog alertDialog;
    private String coonAddr;
    private String last;
    private Runnable reconn_runnable;
    private Integer scanMode = 111;
    private BluetoothGattCharacteristic Character18;
    private BluetoothGattCharacteristic Character34;

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        //super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBlueToothManager = new BlueToothManager(this);
        InnitEevent();

        deviceList = new ArrayList<>();
        serviceList = new ArrayList<>();
        registerReceiver(bleReceiver, makeIntentFilter());
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private boolean mIsBind;
    /**
     * 服务连接的回调
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBleService = ((BleService.LocalBinder) service).getService();
            if (mBleService != null) mHandler.sendEmptyMessage(SERVICE_BIND);
            if (mBleService != null&&mBleService.initialize()) {
                if (mBleService.enableBluetooth(true)) {
                    verifyIfRequestPermission();
                }
            } else {
                Toast.makeText(MainActivity.this, "不支持蓝牙", Toast.LENGTH_SHORT).show();
            }
            mIsBind = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBleService = null;
            mIsBind = false;
        }
    };

    /**
     * 打开搜索的dialog,对6.0进行特殊的处理
     */
    private void verifyIfRequestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            Log.i(TAG, "onCreate: checkSelfPermission");
            if (this.checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onCreate: Android 6.0 动态申请权限");

                if (this.shouldShowRequestPermissionRationale(
                        Manifest.permission.READ_CONTACTS)) {
                    Log.i(TAG, "*********onCreate: shouldShowRequestPermissionRationale**********");
                    Toast.makeText(this, "打开定位才能使用蓝牙", Toast.LENGTH_SHORT).show();
                } else {
                    this.requestPermissions(
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_ACCESS_COARSE_LOCATION);
                }
            } else {
                showDialog("正在搜索");
                mBleService.scanLeDevice(true);
            }
        } else {
            showDialog("正在搜索");
            mBleService.scanLeDevice(true);
        }
    }

    /**
     * 显示Dialog
     */
    private void showDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        //progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    /**
     * 隐藏Dialog
     */
    private void dismissDialog() {
        if (progressDialog == null) return;
        progressDialog.dismiss();
        progressDialog = null;
    }

    @Override
    protected void onDestroy() {
        //取消连接
        mBleService.disconnect();
        doUnBindService();
        unregisterReceiver(bleReceiver);
        unregisterReceiver(mBlueToothManager.mSearchDevicesReceiver);
        //取消网络状态的广播接收
        super.onDestroy();
    }

    /**
     * 绑定服务
     */
    private void doBindService() {
        Intent serviceIntent = new Intent(this, BleService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(serviceIntent);
    }

    /**
     * 解绑服务
     */
    private void doUnBindService() {
        if (mIsBind) {
            unbindService(serviceConnection);
            mBleService = null;
            mIsBind = false;
        }
    }

    private BroadcastReceiver bleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BleService.ACTION_BLUETOOTH_DEVICE)) {
                String tmpDevName = intent.getStringExtra("name");
                String tmpDevAddress = intent.getStringExtra("address");
                Log.i(TAG, "name: " + tmpDevName + ", address: " + tmpDevAddress);
                HashMap<String, Object> deviceMap = new HashMap<>();
                deviceMap.put("name", tmpDevName);
                deviceMap.put("address", tmpDevAddress);
                deviceMap.put("isConnect", false);
                deviceList.add(deviceMap);
            } else if (intent.getAction().equals(BleService.ACTION_GATT_CONNECTED)) {
                deviceList.get(0).put("isConnect", true);
                dismissDialog();
            } else if (intent.getAction().equals(BleService.ACTION_GATT_DISCONNECTED)) {
                deviceList.get(0).put("isConnect", false);
                /**
                 * 加个判断防止空指针崩溃
                 */
                if (serviceList!=null)
                {
                    serviceList.clear();
                }
                /**
                 * 加个判断防止空指针崩溃
                 */
                if (characteristicList!=null)
                {
                    characteristicList.clear();
                }
                dismissDialog();
            } else if (intent.getAction().equals(BleService.ACTION_SCAN_FINISHED)) {
                /**
                 * 扫描结束发消息
                 */
                Message message = mHandler.obtainMessage();
                message.what = SCAN_FINISH;
                mHandler.sendMessage(message);
                dismissDialog();//收到写数据结束的广播
            } else if(intent.getAction().equals(BleService.WRITE_COMPLETE)) {

            }
        }
    };

    private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_BLUETOOTH_DEVICE);
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_SCAN_FINISHED);
        intentFilter.addAction(BleService.WRITE_COMPLETE);
        return intentFilter;
    }

    private double trycount;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SERVICE_BIND:
                    setBleServiceListener();
                    break;
                case SERVICE_SHOW://获取到服务
                    sendData("0101");
                    if (alertDialog != null){
                        alertDialog.dismiss();
                    }
                    break;
                case SCAN_FINISH://扫描结束
                       showResult();
                    break;
                case RE_CONNECT:
                    break;
            }
        }
    };

    /**
     * Dialog显示扫描的结果,Listview adapter没有进行优化,数据量不大
     */
    private void showResult()
    {
        result_dialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog = result_dialog.create();
        //View inflate = View.inflate(BleScanActivity.this,R.layout.item_device,null);
        View inflate = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_result, null);
        alertDialog.setView(inflate);
        alertDialog.setTitle("选择设备");
        alertDialog.setIcon(R.mipmap.ic_launcher);
        lv_scanresult = (ListView) inflate.findViewById(R.id.lv_scanresult);
        Button bt_ok = (Button) inflate.findViewById(R.id.bt_ok);
        final List<Map<String, Object>> ihelmetList = new ArrayList<>();

        for (int dex = 0;dex < deviceList.size();dex++){
            Map<String, Object> infMap = deviceList.get(dex);
            /*if_name = (String) infMap.get("name");
            if (if_name != null && if_name.startsWith("iHelmet")){*/
                ihelmetList.add(infMap);
            //}
        }

        lv_scanresult.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                if (ihelmetList.size() != 0){
                    return ihelmetList.size();
                }else {
                    return 1;
                }
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                //没有搜索到蓝牙设备的情况
                if (ihelmetList.size() == 0){
                    View notfound = View.inflate(MainActivity.this, R.layout.notfound, null);
                    return notfound;
                }else{
                    final Map<String, Object> infMap = ihelmetList.get(position);
                    if_name = (String) infMap.get("name");
                    if_address = (String) infMap.get("address");
                    connstate = (Boolean) infMap.get("isConnect");

                    View device_item = View.inflate(MainActivity.this, R.layout.item_device, null);
                    TextView txtv_name = (TextView) device_item.findViewById(R.id.txtv_name);
                    Button btn_connect = (Button) device_item.findViewById(R.id.btn_connect);
                    btn_connect.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            coonAddr = (String) ihelmetList.get(position).get("address");
                            mBleService.connect(coonAddr);
                            alertDialog.dismiss();
                        }
                    });
                    txtv_name.setText(if_name);
                    return device_item;

                }
            }
        });
        bt_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                if (!mBleService.isScanning()) {
                    verifyIfRequestPermission();
                    deviceList.clear();
                    mBleService.scanLeDevice(true);
                }
            }
        });
        alertDialog.show();
    }

    /**
     * 发送数据的方法
     */
    public void sendData(String data) {
        if (data != null){
        mBleService.setCharacteristicNotification(TransferCharacter, true);
        mBleService.setCharacteristicNotification(Character18, true);
        mBleService.setCharacteristicNotification(Character34, true);
        mBleService.writeCharacteristic(TransferCharacter,hexStringToByte(data));
        Log.e("单独发送",data+"--"+new Date().getTime());
        }

    }

    /**
     *十六进制数据转化为字节数组
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

    private static byte toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }


    public static String bytesToHexString(byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            String hexString = Integer.toHexString(bytes[i] & 0xFF);
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            result += hexString.toUpperCase();
        }
        return result;
    }

    /**
     * BLE的一系列监听
     */
    private void setBleServiceListener() {
        mBleService.setOnServicesDiscoveredListener(new BleService.OnServicesDiscoveredListener() {

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {

                    gattServiceList = gatt.getServices();
                    characteristicList = new ArrayList<>();
                    /// serviceList.clear();
                    for (BluetoothGattService service : gattServiceList)
                    {
                        String serviceUuid = service.getUuid().toString();
                        serviceList.add(MyGattAttributes.lookup(serviceUuid, "Unknown") + "\n" + serviceUuid);
                        Log.i(TAG, MyGattAttributes.lookup(serviceUuid, "Unknown") + "\n" + serviceUuid);

                        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                        String[] charArra = new String[characteristics.size()];
                        for (int i = 0; i < characteristics.size(); i++) {
                            charUuid = characteristics.get(i).getUuid().toString();
                            character = characteristics.get(i);
                            pro = character.getProperties();
                            Log.e("properties",charUuid+"---"+pro+"---"+characteristics.size());
                            //00002a18-0000-1000-8000-00805f9b34fb   00002a34-0000-1000-8000-00805f9b34fb
                            if (charUuid.equals("00002a18-0000-1000-8000-00805f9b34fb")){
                                Character18 = character;
                            }else if (charUuid.equals("00002a34-0000-1000-8000-00805f9b34fb")){
                                Character34 = character;
                            }

                            //获取读写数据的Characteristic
                            if (((pro | BluetoothGattCharacteristic.PROPERTY_WRITE)>0)&&((pro | BluetoothGattCharacteristic.PROPERTY_NOTIFY)>0))
                            {
                                if("00002a52-0000-1000-8000-00805f9b34fb".equals(charUuid)) //0000ffe1-0000-1000-8000-00805f9b34fb
                                {
                                    TransferCharacter = character;
                                }
                            }
                            charArra[i] = MyGattAttributes.lookup(charUuid, "Unknown") + "\n" + charUuid;
                        }
                        characteristicList.add(charArra);
                    }
                    mHandler.sendEmptyMessage(SERVICE_SHOW);//获取服务以后给handler发消息
                    //此处调用了发送数据的方法
                    sendData("0101");
                }
            }
        });



        /**
         * 设置数据监听
         */
        commands = new LinkedList<>();
        mBleService.setOnDataAvailableListener(new BleService.OnDataAvailableListener() {
            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                String comm;
                if (commands != null&& commands.size()>0) {
                    Log.e("Size", commands.size()+"_"+ commands.toString());
                    while ((comm = commands.poll())!=null)
                    {
                        //处理数据
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) throws UnsupportedEncodingException {
                byte[] value = characteristic.getValue();
                String receive = bytesToHexString(value);
                if (!BlueToothInstructUtils.checkString(receive)) {
                    //不完整
                    datas = BlueToothInstructUtils.formatString(receive);
                    if (datas != null && datas.length > 0) {//格式化后得到的数据
                        for (int i = 0; i < datas.length; i++) {//分别执行里面的执行
                            if (datas[i] != null && BlueToothInstructUtils.checkString(datas[i])){
                                commands.offer(datas[i]);
                            }
                            gatt.readCharacteristic(characteristic);
                        }
                        datas = null;
                    }
                } else {
                    commands.offer(receive);
                    gatt.readCharacteristic(characteristic);
                }
                Log.e("收到的", commands.toString());
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                byte[] value = descriptor.getValue();
                String s = value.toString();
                Log.e("onDescriptorRead",s);
            }

        });


        /**
         * 监听连接状态的改变
         */
        mBleService.setOnConnectListener(new BleService.OnConnectionStateChangeListener() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.e("onConnectionStateChange","Ble连接已断开");
                } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                    //Ble正在连接
                    Log.e("onConnectionStateChange","Ble正在连接");
                } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                    //Ble已连接
                    if (alertDialog != null){
                        alertDialog.dismiss();
                    }
                    isConnected = true;
                    Log.e("onConnectionStateChange","Ble已连接");
                } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                    //Ble正在断开连接
                    Log.e("onConnectionStateChange","Ble正在断开连接");
                }
            }
        });

        mBleService.setOnReadRemoteRssiListener(new BleService.OnReadRemoteRssiListener() {
            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                Log.i(TAG, "onReadRemoteRssi: rssi = " + rssi);
            }
        });
    }


    //将字符串编码成16进制数字,适用于所有字符（包括中文）
    private static String hexString="0123456789ABCDEF";
    public static String bin2hex(String str)
    {
        byte[] bytes=str.getBytes();
        StringBuilder sb=new StringBuilder(bytes.length*2);
        for(int i=0;i<bytes.length;i++)
        {
            sb.append(hexString.charAt((bytes[i]&0xf0)>>4));
            sb.append(hexString.charAt((bytes[i]&0x0f)>>0));
        }
        return sb.toString();
    }

    private void InnitEevent() {

        Button bt_scan = findViewById(R.id.bt_scan);
        bt_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isConnected) {
                    doUnBindService();
                    deviceList.clear();
                    doBindService();
                }else {
                    doBindService();
                }
            }
        });
    }

    /**
     * 处理数据的方法
     * 1F0200E20706010C2A0000009CC0110000  -->  0.0156
     * 1F0100E207051612130000000FC0110000  -->  0.0015
     */
    public static Double ProcessData(String data){
        Double result;
        String substring = data.substring(24, 26);
        int i = Integer.parseInt(substring, 16);
        Log.e("ProcessData",i+"");
        result = Double.valueOf(i/10000.0);
        return result;
    }
}
