package ray.library.android.common;

/**
 * Created by Ray Lee on 2016/8/19.
 */
public class RayUtility {

    /**
     * get current class, ex : MainActivity
     * igo
     *
     * @return
     */
    public static String getCurrentClassSimpleName() {
        String tag = "null";
        try {
            String stackClassName = Thread.currentThread().getStackTrace()[3].getClassName();

            int moneyIndex = stackClassName.indexOf('$');

            if (moneyIndex != -1) {
                stackClassName = stackClassName.substring(0, moneyIndex);
            }

            tag = Class.forName(stackClassName).getSimpleName();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return tag;
    }
}
