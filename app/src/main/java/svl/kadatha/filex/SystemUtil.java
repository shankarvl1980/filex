package svl.kadatha.filex;

	import android.os.Build;


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

	}
	
