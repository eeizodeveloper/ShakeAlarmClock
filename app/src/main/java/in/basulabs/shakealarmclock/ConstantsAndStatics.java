package in.basulabs.shakealarmclock;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AppCompatDelegate;

import java.time.LocalTime;

/**
 * A class containing all the constants required by this app.
 */
final class ConstantsAndStatics {

	/**
	 * Bundle key for the Bundle that is passed with intent from {@link Activity_AlarmDetails} to {@link
	 * Activity_AlarmsList} containing the data set by the user.
	 */
	static final String BUNDLE_KEY_ALARM_DETAILS = "in.basulabs.shakealarmclock.BundleWithAlarmDetails";

	/**
	 * Bundle key for the alarm hour. The value is an integer.
	 */
	static final String BUNDLE_KEY_ALARM_HOUR = "in.basulabs.shakealarmclock.HourPickedByUser";

	/**
	 * Bundle key for the alarm minute. The value is an integer.
	 */
	static final String BUNDLE_KEY_ALARM_MINUTE = "in.basulabs.shakealarmclock.MinsPickedByUser";

	/**
	 * Bundle key for the alarm type. The value is one of {@link #ALARM_TYPE_SOUND_ONLY}, {@link
	 * #ALARM_TYPE_VIBRATE_ONLY} or {@link #ALARM_TYPE_SOUND_AND_VIBRATE}.
	 */
	static final String BUNDLE_KEY_ALARM_TYPE = "in.basulabs.shakealarmclock.AlarmType";

	/**
	 * Bundle key for the alarm volume. The value is an integer.
	 */
	static final String BUNDLE_KEY_ALARM_VOLUME = "in.basulabs.shakealarmclock.AlarmVolume";

	/**
	 * Bundle key for the alarm snooze interval. The value is an integer.
	 */
	static final String BUNDLE_KEY_SNOOZE_TIME_IN_MINS = "in.basulabs.shakealarmclock.SnoozeTimeInMins";

	/**
	 * Bundle key for the number of times the alarm should snooze itself. The value is an integer.
	 */
	static final String BUNDLE_KEY_SNOOZE_FREQUENCY = "in.basulabs.shakealarmclock.SnoozeFrequency";

	/**
	 * Bundle key denoting whether repeat is on or off. Value is boolean.
	 */
	static final String BUNDLE_KEY_IS_REPEAT_ON = "in.basulabs.shakealarmclock.IsRepeatOn";

	/**
	 * Bundle key denoting whether snooze is on or off. Value is boolean.
	 */
	static final String BUNDLE_KEY_IS_SNOOZE_ON = "in.basulabs.shakealarmclock.IsSnoozeOn";

	/**
	 * Bundle key denoting whether alarm is on or off. Value is boolean.
	 */
	static final String BUNDLE_KEY_IS_ALARM_ON = "in.basulabs.shakealarmclock.IsAlarmOn";

	/**
	 * Bundle key for the alarm repeat days. The value is an ArrayList of Integer type. Monday is 1 and Sunday
	 * is 7.
	 */
	static final String BUNDLE_KEY_REPEAT_DAYS = "in.basulabs.shakealarmclock.ArrayListOfRepeatDays";

	/**
	 * Denotes that the alarm type will be "Sound".
	 */
	static final int ALARM_TYPE_SOUND_ONLY = 0;

	/**
	 * Denotes that the alarm type will be "Vibrate".
	 */
	static final int ALARM_TYPE_VIBRATE_ONLY = 1;

	/**
	 * Denotes that the alarm type will be "Sound and vibrate".
	 */
	static final int ALARM_TYPE_SOUND_AND_VIBRATE = 2;

	/**
	 * Intent action to be sent to broadcast receiver for sounding alarm.
	 */
	static final String ACTION_DELIVER_ALARM = "in.basulabs.shakealarmclock.DELIVER_ALARM";

	/**
	 * The day on which the alarm is supposed to ring.
	 */
	static final String BUNDLE_KEY_ALARM_DAY = "in.basulabs.shakealarmclock.ALARM_DAY";

	/**
	 * The month in which the alarm will ring.
	 */
	static final String BUNDLE_KEY_ALARM_MONTH = "in.basulabs.shakealarmclock.ALARM_MONTH";

	/**
	 * The year in which the alarm will ring.
	 */
	static final String BUNDLE_KEY_ALARM_YEAR = "in.basulabs.shakealarmclock.ALARM_YEAR";

	/**
	 * Bundle key for Uri of the alarm tone.
	 */
	static final String BUNDLE_KEY_ALARM_TONE_URI = "in.basulabs.shakealarmclock.ALARM_TONE_URI";

	static final String BUNDLE_KEY_HAS_USER_CHOSEN_DATE = "in.basulabs.shakealarmclock.HAS_USER_CHOSEN_DATE";

	/**
	 * Intent action delivered to {@link android.content.BroadcastReceiver} in {@link Service_RingAlarm}
	 * instructing it to snooze the alarm.
	 */
	static final String ACTION_SNOOZE_ALARM = "in.basulabs.shakealarmclock.Service_RingAlarm -- SNOOZE_ALARM";

	/**
	 * Intent action delivered to {@link android.content.BroadcastReceiver} in {@link Service_RingAlarm}
	 * instructing it to cancel the alarm.
	 */
	static final String ACTION_CANCEL_ALARM = "in.basulabs.shakealarmclock.Service_RingAlarm -- CANCEL_ALARM";

	/**
	 * The name of the {@link android.content.SharedPreferences} file for this app.
	 */
	static final String SHARED_PREF_FILE_NAME = "in.basulabs.shakealarmclock.SHARED_PREF_FILE";

	/**
	 * Intent action indicating that {@link Activity_AlarmDetails} should prepare for a new alarm.
	 */
	static final String ACTION_NEW_ALARM = "in.basulabs.shakealarmclock.ACTION_NEW_ALARM";

	/**
	 * Intent action indicating that {@link Activity_AlarmDetails} should show the details of an old alarm.
	 */
	static final String ACTION_EXISTING_ALARM = "in.basulabs.shakealarmclock.ACTION_EXISTING_ALARM";

	static final String EXTRA_PLAY_RINGTONE = "in.basulabs.shakealarmclock.EXTRA_PLAY_RINGTONE";

	/**
	 * Bundle key for the old alarm hour.
	 * <p>This is passed from {@link Activity_AlarmDetails} to
	 * {@link Activity_AlarmsList} if the user saves the edits made to an existing alarm. Using this and
	 * {@link #BUNDLE_KEY_OLD_ALARM_MINUTE}, {@link Activity_AlarmsList} deletes the old alarm and
	 * adds/activates the new alarm.
	 * </p>
	 *
	 * @see #BUNDLE_KEY_OLD_ALARM_MINUTE
	 */
	static final String BUNDLE_KEY_OLD_ALARM_HOUR = "in.basulabs.shakealarmclock.OLD_ALARM_HOUR";

	/**
	 * Bundle key for the old alarm minute.
	 * <p>This is passed from {@link Activity_AlarmDetails} to
	 * {@link Activity_AlarmsList} if the user saves the edits made to an existing alarm. Using this and
	 * {@link #BUNDLE_KEY_OLD_ALARM_HOUR}, {@link Activity_AlarmsList} deletes the old alarm and
	 * adds/activates the new alarm.
	 * </p>
	 *
	 * @see #BUNDLE_KEY_OLD_ALARM_HOUR
	 */
	static final String BUNDLE_KEY_OLD_ALARM_MINUTE = "in.basulabs.shakealarmclock.OLD_ALARM_MINUTE";

	/**
	 * Bundle key for the alarm ID.
	 */
	static final String BUNDLE_KEY_ALARM_ID = "in.basulabs.shakealarmclock.OLD_ALARM_ID";

	/**
	 * Intent action passed as broadcast to {@link AlarmBroadcastReceiver} from {@link Service_AlarmActivater}
	 * so that the former can recreate the latter when the latter is killed.
	 */
	static final String ACTION_CREATE_BACKGROUND_SERVICE = "in.basulabs.shakealarmclock" +
			".CreateBackgroundService";

	/**
	 * Broadcast action: {@link Activity_RingAlarm} should now be destroyed.
	 */
	static final String ACTION_DESTROY_RING_ALARM_ACTIVITY = "in.basulabs.shakealarmclock" +
			".DESTROY_RING_ALARM_ACTIVITY";

	/**
	 * {@link android.content.SharedPreferences} Key indicating whether the app was recently active. The value
	 * is {@code boolean}.
	 */
	static final String SHARED_PREF_KEY_WAS_APP_RECENTLY_ACTIVE = "in.basulabs.shakealarmclock" +
			".SHARED_PREF_KEY_WAS_APP_RECENTLY_ACTIVE";

	/**
	 * {@link android.content.SharedPreferences} key indicating whether the read storage permission was asked
	 * before. This is used to determine if the user had chosen "Don't ask again" before denying the
	 * permission. The value is {@code boolean}.
	 */
	static final String SHARED_PREF_KEY_PERMISSION_WAS_ASKED_BEFORE = "in.basulabs.shakealarmclock.PERMISSION_WAS_ASKED_BEFORE";

	/**
	 * {@link android.content.SharedPreferences} key to store the default shake operation. Can be either
	 * {@link #DISMISS} or {@link #SNOOZE}.
	 */
	static final String SHARED_PREF_KEY_DEFAULT_SHAKE_OPERATION = "in.basulabs.shakealarmclock" +
			".DEFAULT_SHAKE_OPERATION";

	/**
	 * {@link android.content.SharedPreferences} key to store the default power button operation. Can be
	 * either {@link #DISMISS} or {@link #SNOOZE}.
	 */
	static final String SHARED_PREF_KEY_DEFAULT_POWER_BTN_OPERATION = "in.basulabs.shakealarmclock" +
			".DEFAULT_POWER_BTN_OPERATION";

	static final int SNOOZE = 0;

	static final int DISMISS = 1;

	static final int DO_NOTHING = 2;

	/**
	 * {@link android.content.SharedPreferences} key to store the default snooze state. The value is {@code
	 * boolean}.
	 */
	static final String SHARED_PREF_KEY_DEFAULT_SNOOZE_IS_ON = "in.basulabs.shakealarmclock" +
			".DEFAULT_SNOOZE_STATE";

	/**
	 * {@link android.content.SharedPreferences} key to store the default snooze interval in minutes.
	 */
	static final String SHARED_PREF_KEY_DEFAULT_SNOOZE_INTERVAL = "in.basulabs.shakealarmclock" +
			".DEFAULT_SNOOZE_INTERVAL";

	/**
	 * {@link android.content.SharedPreferences} key to store the default snooze frequency, i.e. the number of
	 * times the alarm will ring before being cancelled automatically.
	 */
	static final String SHARED_PREF_KEY_DEFAULT_SNOOZE_FREQ = "in.basulabs.shakealarmclock" +
			".DEFAULT_SNOOZE_FREQUENCY";

	/**
	 * {@link android.content.SharedPreferences} key to store the default alarm tone Uri. If the file is
	 * unavailable, it will be replaced by the default alarm tone during runtime. The value is {@code String};
	 * should be converted to Uri using {@link android.net.Uri#parse(String)}.
	 */
	static final String SHARED_PREF_KEY_DEFAULT_ALARM_TONE_URI = "in.basulabs.shakealarmclock" +
			".DEFAULT_ALARM_TONE_URI";

	/**
	 * {@link android.content.SharedPreferences} key to store the default alarm volume.
	 */
	static final String SHARED_PREF_KEY_DEFAULT_ALARM_VOLUME = "in.basulabs.shakealarmclock" +
			".DEFAULT_ALARM_VOLUME";

	static final int THEME_TIME = 0, THEME_LIGHT = 1, THEME_DARK = 2, THEME_SYSTEM = 3;

	/**
	 * {@link android.content.SharedPreferences} key to store the current theme. Can only have the values
	 * {@link #THEME_TIME}, {@link #THEME_LIGHT}, {@link #THEME_DARK} or {@link #THEME_SYSTEM}.
	 */
	static final String SHARED_PREF_KEY_THEME = "in.basulabs.shakealarmclock.THEME";

	static final String SHARED_PREF_KEY_AUTO_SET_TONE = "in.basulabs.shakealarmclock.AUTO_SET_TONE";

	/**
	 * Get the theme that can be applied using {@link AppCompatDelegate#setDefaultNightMode(int)}.
	 *
	 * @param theme The theme value as stored in {@link android.content.SharedPreferences}. Can only have
	 * 		the values {@link #THEME_TIME}, {@link #THEME_LIGHT}, {@link #THEME_DARK} or {@link #THEME_SYSTEM}.
	 *
	 * @return Can have the values {@link AppCompatDelegate#MODE_NIGHT_YES}, {@link
	 *        AppCompatDelegate#MODE_NIGHT_NO} or {@link AppCompatDelegate#MODE_NIGHT_FOLLOW_SYSTEM}.
	 */
	static int getTheme(int theme) {
		switch (theme) {
			case THEME_SYSTEM:
				return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
			case THEME_LIGHT:
				return AppCompatDelegate.MODE_NIGHT_NO;
			case THEME_DARK:
				return AppCompatDelegate.MODE_NIGHT_YES;
			default:
				if (LocalTime.now().isAfter(LocalTime.of(22, 0))
						|| LocalTime.now().isBefore(LocalTime.of(6, 0))) {
					return AppCompatDelegate.MODE_NIGHT_YES;
				} else {
					return AppCompatDelegate.MODE_NIGHT_NO;
				}
		}
	}

	static void killServices(Context context, int alarmID){
		if (Service_RingAlarm.isThisServiceRunning && Service_RingAlarm.alarmID == alarmID){
			Intent intent = new Intent(context, Service_RingAlarm.class);
			context.stopService(intent);
		}
		if (Service_SnoozeAlarm.isThisServiceRunning && Service_SnoozeAlarm.alarmID == alarmID){
			Intent intent = new Intent(context, Service_SnoozeAlarm.class);
			context.stopService(intent);
		}
	}

}
