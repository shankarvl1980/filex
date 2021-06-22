package svl.kadatha.filex;

	import android.os.Build;
	//import android.support.annotation.RequiresApi;


//import de.jeisfeld.augendiagnoselib.Application;

	/**
	 * Utility class for getting system information.
	 */
	public final class SystemUtil {
		/**
		 * Hide default constructor.
		 */
		private SystemUtil() {
			throw new UnsupportedOperationException();
		}

		/**
		 * Get information if Android version is Lollipop (5.0) or higher.
		 *
		 * @return true if Lollipop or higher.
		 */
		public static boolean isAndroid5() {
			return isAtLeastVersion(Build.VERSION_CODES.LOLLIPOP);
		}

		/**
		 * Check if Android version is at least the given version.
		 *
		 * @param version The version
		 * @return true if Android version is at least the given version
		 */
		public static boolean isAtLeastVersion(final int version) {
			return Build.VERSION.SDK_INT >= version;
		}

		/*public static boolean hasCameraActivity() {
			Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			return takePictureIntent.resolveActivity(Application.getAppContext().getPackageManager()) != null;
		}
*/
		/*public static boolean hasCamera() {
			PackageManager pm = Application.getAppContext().getPackageManager();

			return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
		}
*/
		/*public static boolean hasFlashlight() {
			PackageManager pm = Application.getAppContext().getPackageManager();

			return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
		}
*/
		/*@SuppressLint("InlinedApi")
		public static boolean hasManualSensor() {
			PackageManager pm = Application.getAppContext().getPackageManager();

			return isAndroid5() && pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_CAPABILITY_MANUAL_SENSOR);
		}
*/
		/*public static boolean isAppInstalled(final String appPackage) {
			Intent appIntent = Application.getAppContext().getPackageManager().getLaunchIntentForPackage(appPackage);
			return appIntent != null;
		}
*/
		/*public static boolean isLandscape() {
			// use screen width as criterion rather than getRotation
			WindowManager wm = (WindowManager) Application.getAppContext().getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int width = size.x;
			int height = size.y;
			return width > height;
		}
*/
		/*public static boolean isTablet() {
			return (Application.getAppContext().getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
		}
*/
		/*private static Display getDefaultDisplay() {
			WindowManager wm = (WindowManager) Application.getAppContext().getSystemService(Context.WINDOW_SERVICE);
			return wm.getDefaultDisplay();
		}
*/
		/*public static int getDisplaySize() {
			Point p = new Point();
			getDefaultDisplay().getSize(p);
			return Math.max(p.x, p.y);
		}
*/
		/*public static double getPhysicalDisplaySize() {
			DisplayMetrics dm = new DisplayMetrics();
			getDefaultDisplay().getMetrics(dm);
			Point p = new Point();
			getDefaultDisplay().getSize(p);
			double x = p.x / dm.xdpi;
			double y = p.y / dm.ydpi;
			return Math.max(x, y);
		}
*/
		/*@Nullable
		public static String getUserCountry() {
			Context context = Application.getAppContext();
			String locale = null;

			final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			final String simCountry = tm.getSimCountryIso();
			if (simCountry != null && simCountry.length() == 2) { // SIM country code is available
				locale = simCountry.toUpperCase(Locale.getDefault());
			}
			else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
				String networkCountry = tm.getNetworkCountryIso();
				if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
					locale = networkCountry.toLowerCase(Locale.getDefault());
				}
			}

			if (locale == null || locale.length() != 2) {
				if (VERSION.SDK_INT >= VERSION_CODES.N) {
					locale = getDefaultLocale24().getCountry();
				}
				else {
					locale = getDefaultLocale23().getCountry();
				}
			}

			return locale;
		}
*/
		/*@SuppressWarnings("deprecation")
		private static Locale getDefaultLocale23() {
			return Application.getAppContext().getResources().getConfiguration().locale;
		}
*/
		/*@RequiresApi(api = VERSION_CODES.N)
		private static Locale getDefaultLocale24() {
			return Application.getAppContext().getResources().getConfiguration().getLocales().get(0);
		}
*/
		/*public static int getLargeMemoryClass() {
			ActivityManager manager =
				(ActivityManager) Application.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);

			return manager.getLargeMemoryClass();
		}
		*/
	}
	
