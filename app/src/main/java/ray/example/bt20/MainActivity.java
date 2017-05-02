package ray.example.bt20;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import ray.library.android.bluetooth.BTManager;
import ray.library.android.common.RayUtility;
import ray.library.android.supereasypermissions.PermissionsHelper;

public class MainActivity extends Activity {
    private static String TAG = RayUtility.getCurrentClassSimpleName();
    private BTManager mBTManager;
    private TextView mText;
    private Object mToastObject = new Object();
    private Toast mToast;
    private BTManager.OnFoundBluetoothDeviceListener mOnFoundBluetoothDeviceListener = new BTManager.OnFoundBluetoothDeviceListener() {

        @Override
        public void onFoundBluetoothDevice(final BluetoothDevice device) {
            doFoundBluetoothDevice(device);

        }
    };
    private BTManager.OnPairBluetoothDeviceListener mOnPairBluetoothDeviceListener = new BTManager.OnPairBluetoothDeviceListener() {
        @Override
        public void onPairedBluetoothDevice(BluetoothDevice device) {
            showTextToast(device.getName() + ",已配對");
        }

        @Override
        public void onUnpairedBluetoothDevice(BluetoothDevice device) {
            showTextToast(device.getName() + ",解除配對");
        }
    };

    private BTManager.OnBluetoothStateChangedListener mOnBluetoothStateChangedListener = new BTManager.OnBluetoothStateChangedListener() {
        @Override
        public void onBluetoothStateChanged(boolean enable) {
            showTextToast("enable=" + enable);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mText = (TextView) findViewById(R.id.text);
        mText.setText("");
        //長按會crash
        mText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        // this step is mandated for the url and clickable styles.
        mText.setMovementMethod(LinkMovementMethod.getInstance());

        init();
    }

    private void init() {
        mBTManager = BTManager.getInstance(MainActivity.this);

        mBTManager.setOnFoundBluetoothDeviceListener(mOnFoundBluetoothDeviceListener);
        mBTManager.setOnPairBluetoothDeviceListener(mOnPairBluetoothDeviceListener);

        mBTManager.setOnBluetoothStateChangedListener(mOnBluetoothStateChangedListener);

        findViewById(R.id.discover).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermissionsHelper.request(MainActivity.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION, new PermissionsHelper.PermissionsHelperListener() {

                            @Override
                            public void onPermissionsResult(String permissions, boolean isGrant,
                                                            boolean hasShowedRequestPermissionDialog) {
                                if (isGrant) {// 請求權限已被允許
                                    mBTManager.cancelDiscovery();
                                    mBTManager.startDiscovery();
                                    appendText("startDiscovery\n");
                                } else {// 請求權限已被拒絕
                                    if (hasShowedRequestPermissionDialog) {// 有透過系統彈出提示請求權限
                                        Toast.makeText(getApplicationContext(), "權限[" + permissions + "]已被拒絕", Toast.LENGTH_SHORT).show();
                                    } else {// 使用者不允許彈出提示，也不開啟權限
                                        // TODO 提示使用者到app設定開啟
                                        Toast.makeText(getApplicationContext(), "請至app設定開啟權限[" + permissions + "]", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });


            }
        });

        findViewById(R.id.cancelDiscover).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBTManager.cancelDiscovery();
                appendText("cancelDiscovery\n");
            }
        });

        findViewById(R.id.sppServer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    mBTManager.initSPPBluetoothServerSocket();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void doFoundBluetoothDevice(BluetoothDevice device) {
        String deviceName = device.getName();
        String deviceAddress = device.getAddress();
        String msg = "name[" + deviceName + "],address[" + deviceAddress + "]";

        appendBluetoothAddress(msg + "\n", deviceAddress);

    }

    private void appendText(final CharSequence text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mText.append(text);
            }
        });
    }

    private void appendBluetoothAddress(String text, final String address) {
        final SpannableString spannableString = new SpannableString(text);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                confirmPair(mBTManager.getRemoteDevice(address));
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(MainActivity.this.getResources().getColor(android.R.color.holo_red_dark));
                ds.setUnderlineText(false);
                ds.clearShadowLayer();
            }
        };
        int beginIndex = text.lastIndexOf(address);
        int endIndex = beginIndex + address.length();

        spannableString.setSpan(clickableSpan, beginIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mText.append(spannableString);
            }
        });
    }

    private void showTextToast(CharSequence txt) {
        synchronized (mToastObject) {
            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(this, txt, Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    private void confirmPair(final BluetoothDevice device) {

        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(device.getName())
                    .setItems(new String[]{"藍牙配對"}, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            device.createBond();
                        }
                    });
            builder.create().show();
        } else {
            showTextToast("已經藍牙配對");
            //TODO 建立SPP連線

        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
