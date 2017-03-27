package ray.library.android.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by ray.lee on 2017/3/24.
 */
public class BTManager {
    public final static String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private static String TAG = BTManager.class.getSimpleName();
    private static BTManager sInstance;
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothReceiver mBluetoothReceiver;
    private OnFoundBluetoothDeviceListener mOnFoundBluetoothDeviceListener;
    private ServerThread mServerThread;
    private BluetoothServerSocket mBluetoothServerSocket;
    private OnAcceptBluetoothSocketListener mOnAcceptBluetoothSocketListener;
    private OnPairBluetoothDeviceListener mOnPairBluetoothDeviceListener;

    private BTManager(Context context) {
        init(context);
    }

    public static BTManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (BTManager.class) {
                if (sInstance == null) {
                    sInstance = new BTManager(context);
                }
            }
        }
        return sInstance;
    }

    private void init(Context context) {
        Context appContext = context.getApplicationContext();

        if (appContext == null) {
            appContext = context;
        }
        mContext = appContext;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        registerBluetoothReceiver(mContext);
    }

    public void initSPPBluetoothServerSocket() throws IOException {
        initBluetoothServerSocket(TAG, SPP_UUID);
    }

    public void initBluetoothServerSocket(String name, String uuid) throws IOException {
        mBluetoothServerSocket = mBluetoothAdapter
                .listenUsingInsecureRfcommWithServiceRecord(name,
                        UUID.fromString(uuid));
        mServerThread = new ServerThread();
        mServerThread.start();
    }

    private void registerBluetoothReceiver(Context context) {
        if (mBluetoothReceiver == null) {
            mBluetoothReceiver = new BluetoothReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
            intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            context.registerReceiver(mBluetoothReceiver, intentFilter);
        }
    }

    public BluetoothDevice getRemoteDevice(String address) {
        return mBluetoothAdapter.getRemoteDevice(address);
    }

    public boolean setBluetooth(boolean enable) {
        boolean isEnabled = mBluetoothAdapter.isEnabled();
        if (enable && !isEnabled) {
            return mBluetoothAdapter.enable();
        } else if (!enable && isEnabled) {
            return mBluetoothAdapter.disable();
        }
        // No need to change bluetooth state
        return true;
    }

    public void setOnFoundBluetoothDeviceListener(OnFoundBluetoothDeviceListener listener) {
        mOnFoundBluetoothDeviceListener = listener;
    }

    public void setOnAcceptBluetoothSocketListener(OnAcceptBluetoothSocketListener listener) {
        mOnAcceptBluetoothSocketListener = listener;
    }

    public void setOnPairBluetoothDeviceListener(OnPairBluetoothDeviceListener listener) {
        mOnPairBluetoothDeviceListener = listener;
    }

    public boolean startDiscovery() {
        return mBluetoothAdapter.startDiscovery();
    }

    public boolean cancelDiscovery() {
        return mBluetoothAdapter.cancelDiscovery();
    }

    public static interface OnFoundBluetoothDeviceListener {
        void onFoundBluetoothDevice(BluetoothDevice device);
    }

    public static interface OnPairBluetoothDeviceListener {
        void onPairedBluetoothDevice(BluetoothDevice device);

        void onUnpairedBluetoothDevice(BluetoothDevice device);

    }

    public static interface OnAcceptBluetoothSocketListener {
        void onAcceptBluetoothSocket(BluetoothSocket socket);
    }

    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "BluetoothReceiver.action[" + action + "]");

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (mOnFoundBluetoothDeviceListener != null) {
                    mOnFoundBluetoothDeviceListener.onFoundBluetoothDevice(device);
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    if (mOnPairBluetoothDeviceListener != null) {
                        mOnPairBluetoothDeviceListener.onPairedBluetoothDevice(device);
                    }
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    if (mOnPairBluetoothDeviceListener != null) {
                        mOnPairBluetoothDeviceListener.onUnpairedBluetoothDevice(device);
                    }
                }

            }
        }
    }

    public class ServerThread extends Thread {

        boolean mIsClose = false;

        public ServerThread() {
        }

        public void close() {
            mIsClose = true;
            try {
                mBluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (!mIsClose) {
                try {
                    BluetoothSocket socket = mBluetoothServerSocket.accept();

                    if (mOnAcceptBluetoothSocketListener != null) {
                        mOnAcceptBluetoothSocketListener.onAcceptBluetoothSocket(socket);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
