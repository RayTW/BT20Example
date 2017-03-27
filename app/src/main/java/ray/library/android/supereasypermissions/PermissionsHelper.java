package ray.library.android.supereasypermissions;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

public class PermissionsHelper {
    private static PermissionsHelper sInstance = new PermissionsHelper();
    private PermissionsRequestProxy mPermissionsRequestProxy;

    private PermissionsHelper() {
        mPermissionsRequestProxy = new PermissionsRequestProxy();
        PermissionsRequestActivity.setPermissionsHelperListener(mPermissionsRequestProxy);
    }

    /**
     * 請求權限
     *
     * @param activity
     * @param permission
     * @param listener
     */
    public static void request(@NonNull Activity activity, @NonNull String permission,
                               @NonNull PermissionsHelperListener listener) {
        sInstance.doRequest(activity, permission, listener);
    }

    /**
     * 檢查是否有取得允許權限
     *
     * @param context
     * @param permission
     * @return
     */
    public static boolean hasGrantedPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * to app settings
     *
     * @param context
     */
    @TargetApi(9)
    public static void startActivityAppDetails(Context context) {
        String apppackage = context.getApplicationContext().getPackageName();

        Intent intent = new Intent();
        int i = Build.VERSION.SDK_INT;
        if (i >= 9) {
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.fromParts("package", apppackage, null));
        } else {
            String str2;
            if (i == 8) {
                str2 = "pkg";
            } else {
                str2 = "com.android.settings.ApplicationPkgName";
            }
            intent.setAction("android.intent.action.VIEW");
            intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            intent.putExtra(str2, apppackage);
        }
        if (!(context instanceof Activity)) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void doRequest(Activity activity, String permission, PermissionsHelperListener listener) {
        if (permission == null) {
            throw new IllegalArgumentException("permission == null");
        }

        mPermissionsRequestProxy.setPermissionsHelperListener(listener);

        Intent intent = new Intent(activity, PermissionsRequestActivity.class);

        intent.putExtra(PermissionsRequestActivity.PERMISSIONS_NAME, permission);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    public static interface PermissionsHelperListener {
        public void onPermissionsResult(String permissions, boolean isGrant, boolean hasShowedRequestPermissionDialog);
    }

    private class PermissionsRequestProxy implements PermissionsHelperListener {
        private PermissionsHelperListener mPermissionsHelperListener;

        PermissionsRequestProxy() {
        }

        private void setPermissionsHelperListener(PermissionsHelperListener listener) {
            mPermissionsHelperListener = listener;
        }

        @Override
        public void onPermissionsResult(String permissions, boolean isGrant, boolean hasShowedRequestPermissionDialog) {

            if (mPermissionsHelperListener != null) {
                mPermissionsHelperListener.onPermissionsResult(permissions, isGrant, hasShowedRequestPermissionDialog);
            }

            mPermissionsHelperListener = null;
        }
    }
}
