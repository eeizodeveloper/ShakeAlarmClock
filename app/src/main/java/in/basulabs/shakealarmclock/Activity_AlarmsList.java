/*
Copyright (C) 2022  Wrichik Basu (basulabs.developer@gmail.com)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package in.basulabs.shakealarmclock;

import static android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Activity_AlarmsList extends AppCompatActivity implements AlarmAdapter.AdapterInterface {

	private AlarmAdapter alarmAdapter;
	private RecyclerView alarmsRecyclerView;
	private AlarmDatabase alarmDatabase;
	private ViewModel_AlarmsList viewModel;

	/**
	 * The view stub will show up only when there are no alarms to show.
	 */
	private ViewStub viewStub;

	private static final int MODE_ADD_NEW_ALARM = 103;
	private static final int MODE_ACTIVATE_EXISTING_ALARM = 604;

	private static final int MODE_DELETE_ALARM = 504;
	private static final int MODE_DEACTIVATE_ONLY = 509;

	private ActivityResultLauncher<Intent> settingsActLauncher, newAlarmActLauncher, oldAlarmActLauncher;

	private String toastText = null;

	//--------------------------------------------------------------------------------------------------

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alarmslist);
		setSupportActionBar(findViewById(R.id.toolbar));

		alarmDatabase = AlarmDatabase.getInstance(this);
		viewModel = new ViewModelProvider(this).get(ViewModel_AlarmsList.class);
		SharedPreferences sharedPreferences = getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME, MODE_PRIVATE);

		// Initialise the view model:
		viewModel.init(alarmDatabase);

		// Find and set the app theme:
		int defaultTheme = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ConstantsAndStatics.THEME_SYSTEM : ConstantsAndStatics.THEME_AUTO_TIME;
		if (savedInstanceState == null) {
			AppCompatDelegate
					.setDefaultNightMode(ConstantsAndStatics.getTheme(sharedPreferences.getInt(ConstantsAndStatics.SHARED_PREF_KEY_THEME,
							defaultTheme)));
		}

		Button addAlarmButton = findViewById(R.id.addAlarmButton);
		addAlarmButton.setOnClickListener(view -> {
			Intent intent = new Intent(this, Activity_AlarmDetails.class);
			intent.setAction(ConstantsAndStatics.ACTION_NEW_ALARM);
			newAlarmActLauncher.launch(intent);
		});

		alarmsRecyclerView = findViewById(R.id.alarmsRecyclerView);
		alarmsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

		viewStub = findViewById(R.id.viewStub);

		initialiseRecyclerView();

		manageViewStub(viewModel.getAlarmsCount(alarmDatabase));

		getLifecycle().addObserver(viewModel);

		// Observe the number of alarms in the database, and display the view stub based on that count.
		viewModel.getLiveAlarmsCount().observe(this, this::manageViewStub);

		initActLaunchers();

		///////////////////////////////////////////////////////////////////////////////////////////////////
		// This variable determines whether the update dialog will be displayed or not.
		// Do not display the update dialog if the either of the snooze or update services is running.
		// Also, do not display the dialog if the activity is created by an Activity_IntentManager.
		//////////////////////////////////////////////////////////////////////////////////////////////////
		boolean canShowDialogs = Service_RingAlarm.isThisServiceRunning || Service_SnoozeAlarm.isThisServiceRunning;

		// Check if this activity has been started by Activity_IntentManager.
		// If yes, start Activity_AlarmDetails with necessary data.
		if (getIntent().getAction() != null) {

			if (getIntent().getAction().equals(ConstantsAndStatics.ACTION_NEW_ALARM_FROM_INTENT)) {

				canShowDialogs = false;

				Intent intent = new Intent(this, Activity_AlarmDetails.class);
				intent.setAction(ConstantsAndStatics.ACTION_NEW_ALARM_FROM_INTENT);

				if (getIntent().getExtras() != null) {
					intent.putExtras(getIntent().getExtras());
				}

				newAlarmActLauncher.launch(intent);
			}
		}

		if (savedInstanceState == null && canShowDialogs) {
			showDialogs();
		}

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			deleteNotifChannels();
		}

	}

	//--------------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

			if (viewModel.getPendingStatus() && viewModel.getPendingALarmData() != null
					&& viewModel.getIsSettingsActOver()) {

				AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
				if (alarmManager.canScheduleExactAlarms()) {
					viewModel.setPendingStatus(false);
					viewModel.setIsSettingsActOver(false);
					setAlarm(viewModel.getPendingALarmData());
					viewModel.savePendingAlarm(null);
				} else {
					requestExactAlarmPerm();
				}
			}
		}
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_alarmlist_menu, menu);
		return true;
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.settingsMenuItem) {
			Intent intent = new Intent(this, Activity_Settings.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ConstantsAndStatics.schedulePeriodicWork(this);
	}


	//--------------------------------------------------------------------------------------------------

	/**
	 * Displays/hides the view stub.
	 *
	 * @param count The total number of alarms currently in the database.
	 */
	private void manageViewStub(int count) {
		if (count == 0) {
			viewStub.setVisibility(View.VISIBLE);
		} else {
			viewStub.setVisibility(View.GONE);
		}
	}

	//--------------------------------------------------------------------------------------------------

	/*
	 * Kept for future reference.
	 *
	private void onDateChanged() {
		viewModel.forceInitAndWait(alarmDatabase);
		alarmAdapter = new AlarmAdapter(viewModel.getAlarmDataArrayList(), this, this);
		alarmsRecyclerView.swapAdapter(alarmAdapter, false);
	}*/

	//--------------------------------------------------------------------------------------------------

	/**
	 * Initialises the {@code alarmsRecyclerView}.
	 */
	private void initialiseRecyclerView() {
		alarmAdapter = new AlarmAdapter(viewModel.getAlarmDataArrayList(), this, this);
		alarmsRecyclerView.setAdapter(alarmAdapter);
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Activates an alarm.
	 * <p>
	 * Adds the alarm to the database, depending on the {@code mode}, updates the {@code alarmAdapter}, and finally activates the alarm via {@link
	 * AlarmManager}.
	 *
	 * @param mode The mode. Can be either {@link #MODE_ADD_NEW_ALARM} or {@link #MODE_ACTIVATE_EXISTING_ALARM}.
	 * @param alarmEntity The {@link AlarmEntity} object representing the alarm.
	 * @param repeatDays The days on which the alarm is to repeat. Can be {@code null} is repeat is OFF.
	 */
	private void addOrActivateAlarm(int mode, AlarmEntity alarmEntity, @Nullable ArrayList<Integer> repeatDays) {

		ConstantsAndStatics.cancelScheduledPeriodicWork(this);

		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		if (repeatDays != null) {
			Collections.sort(repeatDays);
		}

		LocalDateTime alarmDateTime = ConstantsAndStatics.getAlarmDateTime(LocalDate.of(alarmEntity.alarmYear,
				alarmEntity.alarmMonth, alarmEntity.alarmDay), LocalTime.of(alarmEntity.alarmHour,
				alarmEntity.alarmMinutes), alarmEntity.isRepeatOn, repeatDays);

		///////////////////////////////////////////////////////////////////////////////////////////
		// IMPORTANT:
		// The alarmEntity object does NOT have an ID for new alarms. So the ID has to be extracted after
		// adding the alarm to the database because the ID will be auto-generated.
		///////////////////////////////////////////////////////////////////////////////////////////

		if (mode == MODE_ADD_NEW_ALARM) {

			int[] result = viewModel.addAlarm(alarmDatabase, alarmEntity, repeatDays);

			alarmEntity.alarmID = result[0];

			if (viewModel.getAlarmsCount(alarmDatabase) == 1) {
				alarmAdapter = new AlarmAdapter(viewModel.getAlarmDataArrayList(), this, this);
				alarmsRecyclerView.swapAdapter(alarmAdapter, false);
			} else {
				alarmAdapter.notifyItemInserted(result[1]);
			}

			alarmsRecyclerView.scrollToPosition(result[1]);

		}

		Bundle data = alarmEntity.getAlarmDetailsInABundle();
		data.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS, repeatDays);
		data.putSerializable(ConstantsAndStatics.BUNDLE_KEY_DATE_TIME, alarmDateTime);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			if (!alarmManager.canScheduleExactAlarms()) {
				viewModel.setPendingStatus(true);
				viewModel.savePendingAlarm(data);
				requestExactAlarmPerm();
				return;
			}
		}

		setAlarm(data);

		ConstantsAndStatics.schedulePeriodicWork(this);
	}

	//------------------------------------------------------------------------------------------------------

	/**
	 * Deletes or deactivates an alarm.
	 *
	 * @param mode Can have only two values: {@link #MODE_DEACTIVATE_ONLY} or {@link #MODE_DELETE_ALARM}.
	 * @param hour The alarm hour.
	 * @param mins The alarm minutes.
	 */
	private void deleteOrDeactivateAlarm(int mode, int hour, int mins) {

		ConstantsAndStatics.cancelScheduledPeriodicWork(this);

		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		Intent intent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class)
				.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM)
				.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);

		int alarmID = viewModel.getAlarmId(alarmDatabase, hour, mins);

		int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M ?
				PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE :
				PendingIntent.FLAG_NO_CREATE;

		PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), alarmID, intent, flags);

		if (pendingIntent != null) {
			alarmManager.cancel(pendingIntent);
		}

		ConstantsAndStatics.killServices(this, alarmID);

		DateTimeFormatter formatter;
		if (DateFormat.is24HourFormat(this)) {
			formatter = DateTimeFormatter.ofPattern("HH:mm");
		} else {
			formatter = DateTimeFormatter.ofPattern("hh:mm a");
		}
		LocalTime alarmTime = LocalTime.of(hour, mins);

		if (mode == MODE_DELETE_ALARM) {

			int pos = viewModel.removeAlarm(alarmDatabase, hour, mins);
			alarmAdapter.notifyItemRemoved(pos);

			toastText = getString(R.string.toast_alarmDeleted, alarmTime.format(formatter));
		} else {

			int index = viewModel.toggleAlarmState(alarmDatabase, hour, mins, 0);
			alarmAdapter.notifyItemChanged(index);

			toastText = getString(R.string.toast_alarmSwitchedOff, alarmTime.format(formatter));
		}

		ConstantsAndStatics.schedulePeriodicWork(this);
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Toggle the alarm state. Updates the database, and cancels/places an alarm via {@link AlarmManager}.
	 *
	 * @param hour The hour of the alarm
	 * @param mins The minute of the alarm.
	 * @param newAlarmState The new state of the alarm. {@code 0} means OFF and {@code 1} means ON.
	 */
	private void toggleAlarmState(int hour, int mins, final int newAlarmState) {

		ConstantsAndStatics.killServices(this, viewModel.getAlarmId(alarmDatabase, hour, mins));

		if (newAlarmState == 0) {
			deleteOrDeactivateAlarm(MODE_DEACTIVATE_ONLY, hour, mins);
			showToast();
		} else {
			addOrActivateAlarm(MODE_ACTIVATE_EXISTING_ALARM, viewModel.getAlarmEntity(alarmDatabase, hour, mins),
					viewModel.getRepeatDays(alarmDatabase, hour, mins));
		}

	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onOnOffButtonClick(int rowNumber, int hour, int mins, int newAlarmState) {
		toggleAlarmState(hour, mins, newAlarmState);
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onDeleteButtonClicked(int rowNumber, int hour, int mins) {
		deleteOrDeactivateAlarm(MODE_DELETE_ALARM, hour, mins);
		showToast();
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onItemClicked(int rowNumber, int hour, int mins) {

		Context context = this;

		final String KEY_START_ACTIVITY = "startTheActivity";

		Handler handler = new Handler(Looper.getMainLooper()) {
			@Override
			public void handleMessage(@NonNull Message msg) {
				Bundle data = msg.getData();
				if (data.getBoolean(KEY_START_ACTIVITY)) {

					Bundle bundle = data.getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS);
					assert bundle != null;
					bundle.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS,
							data.getIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS));

					Intent intent = new Intent(context, Activity_AlarmDetails.class)
							.setAction(ConstantsAndStatics.ACTION_EXISTING_ALARM)
							.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, bundle);
					oldAlarmActLauncher.launch(intent);
				}
			}
		};

		Thread thread = new Thread(() -> {
			Looper.prepare();

			Bundle bundle = new Bundle();

			List<AlarmEntity> list = alarmDatabase.alarmDAO().getAlarmDetails(hour, mins);
			for (AlarmEntity entity : list) {
				bundle.putBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, entity.getAlarmDetailsInABundle());
				bundle.putIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS,
						new ArrayList<>(alarmDatabase.alarmDAO().getAlarmRepeatDays(entity.alarmID)));
			}
			bundle.putBoolean(KEY_START_ACTIVITY, true);

			Message message = Message.obtain();
			message.setData(bundle);
			handler.sendMessageAtFrontOfQueue(message);
		});
		thread.start();

	}

	//-------------------------------------------------------------------------------------------------------------

	/**
	 * Converts a {@link Duration} object to human-readable form.
	 *
	 * @return The duration in a human-readable form.
	 */
	@NonNull
	private String getDuration(@NonNull Duration duration) {

		NumberFormat numFormat = NumberFormat.getInstance();
		numFormat.setGroupingUsed(false);

		long days = duration.toDays();
		duration = duration.minusDays(days);

		long hours = duration.toHours();
		duration = duration.minusHours(hours);

		long minutes = duration.toMinutes();

		String msg;

		if (days == 0) {
			if (hours == 0) {
				msg = numFormat.format(minutes) + getResources().getQuantityString(R.plurals.mins, (int) minutes);
			} else {
				msg = numFormat.format(hours) + getResources().getQuantityString(R.plurals.hour, (int) hours)
						+ getString(R.string.and)
						+ numFormat.format(minutes) + getResources().getQuantityString(R.plurals.mins, (int) minutes);
			}
		} else {
			msg = numFormat.format(days) + getResources().getQuantityString(R.plurals.day, (int) days) + ", "
					+ numFormat.format(hours) + getResources().getQuantityString(R.plurals.hour, (int) hours)
					+ getString(R.string.and)
					+ numFormat.format(minutes) + " " + getResources().getQuantityString(R.plurals.mins, (int) minutes);
		}
		return msg;
	}

	//-------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the alarm in the Android system.
	 *
	 * @param data The details of the alarm to be set.
	 */
	private void setAlarm(@NonNull Bundle data) {

		LocalDateTime alarmDateTime = (LocalDateTime) data.getSerializable(ConstantsAndStatics.BUNDLE_KEY_DATE_TIME);
		data.remove(ConstantsAndStatics.BUNDLE_KEY_DATE_TIME);

		int index = viewModel.toggleAlarmState(alarmDatabase, Objects.requireNonNull(alarmDateTime).getHour(),
				alarmDateTime.getMinute(), 1);
		alarmAdapter.notifyItemChanged(index);

		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

		Intent intent = new Intent(getApplicationContext(), AlarmBroadcastReceiver.class);
		intent.setAction(ConstantsAndStatics.ACTION_DELIVER_ALARM);
		intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		intent.putExtra(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS, data);

		int alarmID = data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_ID);

		int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
		PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), alarmID, intent, flags);

		ZonedDateTime zonedDateTime = ZonedDateTime.of(alarmDateTime.withSecond(0), ZoneId.systemDefault());

		alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(zonedDateTime.toEpochSecond() * 1000, pendingIntent), pendingIntent);

		toastText = getString(R.string.toast_alarmSwitchedOn,
				getDuration(Duration.between(ZonedDateTime.now(ZoneId.systemDefault()).withSecond(0), zonedDateTime)));
		showToast();
	}

	//-------------------------------------------------------------------------------------------------------------

	/**
	 * Show the battery optimization dialog/update available dialog.
	 * <p>
	 * If the battery optimizations dialog is shown, then the update dialog is not shown.
	 * </p>
	 */
	private void showDialogs() {

		boolean showBatteryOptimDialog = getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME, MODE_PRIVATE)
				.getBoolean(ConstantsAndStatics.SHARED_PREF_KEY_SHOW_BATTERY_OPTIM_DIALOG, true);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && showBatteryOptimDialog) {
			PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
			if (!powerManager.isIgnoringBatteryOptimizations("in.basulabs.shakealarmclock")) {
				DialogFragment dialogFragment = new AlertDialog_BatteryOptimizations();
				dialogFragment.setCancelable(false);
				dialogFragment.show(getSupportFragmentManager(), "");
			}
		}
	}

	//-------------------------------------------------------------------------------------------------------------

	/**
	 * Requests the {@link android.Manifest.permission#SCHEDULE_EXACT_ALARM} permission by directing the user to go to Settings.
	 */
	@RequiresApi(api = Build.VERSION_CODES.S)
	private void requestExactAlarmPerm() {

		Intent intent = new Intent();
		intent.setAction(ACTION_REQUEST_SCHEDULE_EXACT_ALARM);

		new AlertDialog.Builder(this)
				.setMessage(R.string.request_exact_alarm_perm)
				.setCancelable(false)
				.setPositiveButton("Go to Settings", (dialog, which) -> {
					viewModel.setIsSettingsActOver(false);
					settingsActLauncher.launch(intent);
				})
				.show();
	}

	//------------------------------------------------------------------------------

	/**
	 * Shows a toast using the text in {@code toastText} variable.
	 */
	private void showToast() {
		if (toastText != null) {
			Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
			toastText = null;
		}
	}

	//------------------------------------------------------------------------------

	/**
	 * Initializes all activity launchers.
	 */
	private void initActLaunchers() {

		settingsActLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				result -> viewModel.setIsSettingsActOver(true));

		newAlarmActLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(), (result) -> {
					if (result.getResultCode() == RESULT_OK) {

						Intent intent = result.getData();

						if (intent != null) {

							Bundle data = Objects.requireNonNull(intent.getExtras()).getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS);
							assert data != null;

							if (viewModel.getAlarmId(alarmDatabase, data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE)) != 0) {

								deleteOrDeactivateAlarm(MODE_DELETE_ALARM,
										data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
										data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE));
							}

							AlarmEntity alarmEntity = new AlarmEntity(data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE),
									false,
									data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_SNOOZE_ON),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_TIME_IN_MINS),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_FREQUENCY),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_VOLUME),
									data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_DAY),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MONTH),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_YEAR),
									data.getParcelable(ConstantsAndStatics.BUNDLE_KEY_ALARM_TONE_URI),
									data.getString(ConstantsAndStatics.BUNDLE_KEY_ALARM_MESSAGE),
									data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_HAS_USER_CHOSEN_DATE));

							addOrActivateAlarm(MODE_ADD_NEW_ALARM, alarmEntity,
									data.getIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS));
						}
					}
				});

		oldAlarmActLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(), (result) -> {

					if (result.getResultCode() == RESULT_OK) {

						Intent intent = result.getData();

						if (intent != null) {

							Bundle data = Objects.requireNonNull(intent.getExtras())
							                     .getBundle(ConstantsAndStatics.BUNDLE_KEY_ALARM_DETAILS);
							assert data != null;

							deleteOrDeactivateAlarm(MODE_DELETE_ALARM,
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_OLD_ALARM_HOUR),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_OLD_ALARM_MINUTE));

							AlarmEntity alarmEntity = new AlarmEntity(
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_HOUR),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MINUTE),
									false,
									data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_SNOOZE_ON),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_TIME_IN_MINS),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_SNOOZE_FREQUENCY),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_VOLUME),
									data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_IS_REPEAT_ON),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_TYPE),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_DAY),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_MONTH),
									data.getInt(ConstantsAndStatics.BUNDLE_KEY_ALARM_YEAR),
									data.getParcelable(ConstantsAndStatics.BUNDLE_KEY_ALARM_TONE_URI),
									data.getString(ConstantsAndStatics.BUNDLE_KEY_ALARM_MESSAGE),
									data.getBoolean(ConstantsAndStatics.BUNDLE_KEY_HAS_USER_CHOSEN_DATE));

							addOrActivateAlarm(MODE_ADD_NEW_ALARM, alarmEntity,
									data.getIntegerArrayList(ConstantsAndStatics.BUNDLE_KEY_REPEAT_DAYS));

						}
					}

				});

	}

	/**
	 * Deletes all notification channels created by the app.
	 * <p>
	 * In the last release of the app, many notification channels were created without proper description and names. This method deletes all
	 * notification channels created by the app so that the mess is removed, and we can start afresh.
	 * <p>
	 * The work is done in a background thread so that the UI thread is not burdened.
	 */
	@RequiresApi(api = Build.VERSION_CODES.O)
	private void deleteNotifChannels() {

		new Thread(() -> {

			SharedPreferences sharedPref = getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME, MODE_PRIVATE);

			if (!sharedPref.getBoolean(ConstantsAndStatics.SHARED_PREF_KEY_NOTIF_CHANNELS_DELETED, false)) {

				sharedPref.edit().putBoolean(ConstantsAndStatics.SHARED_PREF_KEY_NOTIF_CHANNELS_DELETED, true).apply();

				NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				List<NotificationChannel> channelList = notificationManager.getNotificationChannels();
				if (channelList != null && !channelList.isEmpty()) {
					for (NotificationChannel channel : channelList) {
						String id = channel.getId();
						notificationManager.deleteNotificationChannel(id);
					}
				}
			}

		}).start();

	}

}