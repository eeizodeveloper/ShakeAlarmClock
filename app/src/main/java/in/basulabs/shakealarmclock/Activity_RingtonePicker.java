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

import static android.media.RingtoneManager.ACTION_RINGTONE_PICKER;
import static android.media.RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI;
import static android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI;
import static android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI;
import static android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT;
import static android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT;
import static android.media.RingtoneManager.EXTRA_RINGTONE_TITLE;
import static android.media.RingtoneManager.EXTRA_RINGTONE_TYPE;
import static android.media.RingtoneManager.ID_COLUMN_INDEX;
import static android.media.RingtoneManager.TITLE_COLUMN_INDEX;
import static android.media.RingtoneManager.TYPE_ALL;
import static android.media.RingtoneManager.URI_COLUMN_INDEX;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Objects;

public class Activity_RingtonePicker extends AppCompatActivity implements View.OnClickListener, AlertDialog_PermissionReason.DialogListener {

	private AudioAttributes audioAttributes;
	private Bundle savedInstanceState;
	private MediaPlayer mediaPlayer;
	private RadioGroup radioGroup;
	private static final int DEFAULT_RADIO_BTN_ID = View.generateViewId(), SILENT_RADIO_BTN_ID = View.generateViewId();
	private static final int PERMISSIONS_REQUEST_CODE = 3720;
	private SharedPreferences sharedPreferences;
	private ViewModel_RingtonePicker viewModel;
	private ConstraintLayout chooseToneLayout;
	private ActivityResultLauncher<Intent> fileActLauncher;

	//----------------------------------------------------------------------------------------------------

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ringtonepicker);

		sharedPreferences = getSharedPreferences(ConstantsAndStatics.SHARED_PREF_FILE_NAME, MODE_PRIVATE);

		viewModel = new ViewModelProvider(this).get(ViewModel_RingtonePicker.class);

		this.savedInstanceState = savedInstanceState;

		radioGroup = findViewById(R.id.ringtonePickerRadioGroup);
		chooseToneLayout = findViewById(R.id.chooseCustomToneConstarintLayout);

		mediaPlayer = new MediaPlayer();
		audioAttributes = new AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
				.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
				.build();

		chooseToneLayout.setVisibility(View.GONE);

		initActLaunchers();

		if (Objects.equals(getIntent().getAction(), ACTION_RINGTONE_PICKER) && !viewModel.getPermissionRationaleBeingShown()) {

			if (isPermissionAvailable()) {
				initialise();
			} else {
				viewModel.setPermissionRationaleBeingShown(true);
				checkAndRequestPermission();
			}
		}
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_ringtonepicker_menu, menu);

		// Get the action view used in your playTone item
		MenuItem toggleservice = menu.findItem(R.id.playTone);
		SwitchCompat actionView = (SwitchCompat) toggleservice.getActionView();
		actionView.setChecked(viewModel.getPlayTone());
		actionView.setOnCheckedChangeListener((buttonView, isChecked) -> {
			viewModel.setPlayTone(isChecked);
			if (!isChecked) {
				try {
					mediaPlayer.stop();
				} catch (IllegalStateException ignored) {
				}
			}
		});
		return super.onCreateOptionsMenu(menu);
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	protected void onResume() {
		super.onResume();
		if (!viewModel.getPermissionRationaleBeingShown() && !viewModel.getIsInitialised()) {
			viewModel.setPermissionRationaleBeingShown(false);
			if (isPermissionAvailable()) {
				initialise();
			} else {
				onPermissionDenied();
			}
		}
	}


	//----------------------------------------------------------------------------------------------------

	@Override
	protected void onPause() {
		super.onPause();
		try {
			if (mediaPlayer.isPlaying()) {
				mediaPlayer.stop();
			}
		} catch (Exception ignored) {
		}
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Populate {@link #radioGroup} by creating and adding appropriate {@link RadioButton}.
	 */
	private void populateRadioGroup() {

		if (viewModel.getShowDefault()) {
			createOneRadioButton(DEFAULT_RADIO_BTN_ID, getResources().getString(R.string.defaultTone));
		}

		if (viewModel.getShowSilent()) {
			createOneRadioButton(SILENT_RADIO_BTN_ID, getResources().getString(R.string.silentTone));
		}

		for (int i = 0; i < viewModel.getToneIdList().size(); i++) {
			createOneRadioButton(viewModel.getToneIdList().get(i), viewModel.getToneNameList().get(i));
		}

		if (viewModel.getExistingUri() != null) {

			////////////////////////////////////////////////////////////////////
			// As existingUri is not null, we are required to pre-select
			// a specific RadioButton.
			///////////////////////////////////////////////////////////////////

			if (viewModel.getShowDefault() && viewModel.getExistingUri().equals(viewModel.getDefaultUri())) {

				///////////////////////////////////////////////////////////////////////////
				// The existingUri is same as defaultUri, and showDefault is true.
				// So, we check the "Default" RadioButton.
				//////////////////////////////////////////////////////////////////////////
				((RadioButton) findViewById(DEFAULT_RADIO_BTN_ID)).setChecked(true);
				setPickedUri(viewModel.getDefaultUri());

			} else {

				// Find index of existingUri in toneUriList
				int index = viewModel.getToneUriList().indexOf(viewModel.getExistingUri());

				if (index != -1) {

					// toneUriList has existingUri. Check the corresponding RadioButton.
					((RadioButton) findViewById(viewModel.getToneIdList().get(index))).setChecked(true);
					setPickedUri(viewModel.getExistingUri());

				} else {
					///////////////////////////////////////////////////////////////////////
					// toneUriList does NOT have existingUri. It is a custom Uri
					// provided to us. We have to first check whether the file exists
					// or not. If it exists, we shall add that file to our RadioGroup.
					// If the file does not exist, we do not select any Radiogroup.
					///////////////////////////////////////////////////////////////////////
					try (Cursor cursor = getContentResolver().query(viewModel.getExistingUri(), null, null, null, null)) {

						if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
							// existingUri is a valid Uri.

							String fileNameWithExt;
							int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
							if (columnIndex != -1) {
								fileNameWithExt = cursor.getString(columnIndex);
							} else {
								fileNameWithExt = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
							}

							int toneId = View.generateViewId();

							viewModel.getToneNameList().add(fileNameWithExt);
							viewModel.getToneUriList().add(viewModel.getExistingUri());
							viewModel.getToneIdList().add(toneId);

							createOneRadioButton(toneId, fileNameWithExt);

							((RadioButton) findViewById(toneId)).setChecked(true);

							setPickedUri(viewModel.getExistingUri());

						}
					}
				}
			}
		} else {

			if (viewModel.getWasExistingUriGiven()) {
				//////////////////////////////////////////////////////////////////////////
				// existingUri was specifically passed as a null value. If showSilent
				// is true, we pre-select the "Silent" RadioButton. Otherwise
				// we do not select any specific RadioButton.
				/////////////////////////////////////////////////////////////////////////
				if (viewModel.getShowSilent()) {
					((RadioButton) findViewById(SILENT_RADIO_BTN_ID)).setChecked(true);
				}
			}
			setPickedUri(null);
		}
	}

	//----------------------------------------------------------------------------------------------------

	private void setPickedUri(@Nullable Uri newUri) {
		if (savedInstanceState == null) {
			viewModel.setPickedUri(newUri);
		}
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Creates one {@link RadioButton} and adds it to {@link #radioGroup}.
	 *
	 * @param id The id to be assigned to the {@link RadioButton}.
	 * @param text The text to be set in the {@link RadioButton}. Cannot be {@code null}.
	 */
	private void createOneRadioButton(int id, @NonNull String text) {
		RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.setMargins(5, 24, 5, 24);

		RadioButton radioButton = new RadioButton(this);
		radioButton.setId(id);
		radioButton.setTextColor(getResources().getColor(R.color.defaultLabelColor));
		radioButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
		radioButton.setLayoutParams(params);
		radioButton.setText(text);
		radioButton.setOnClickListener(this);
		radioGroup.addView(radioButton);
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			this.onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onClick(View view) {
		if (view.getId() == DEFAULT_RADIO_BTN_ID) {
			viewModel.setPickedUri(viewModel.getDefaultUri());
			playChosenTone();
		} else if (view.getId() == SILENT_RADIO_BTN_ID) {
			viewModel.setPickedUri(null);
		} else if (view.getId() == R.id.chooseCustomToneConstarintLayout) {
			openFileBrowser();
		} else {
			viewModel.setPickedUri(viewModel.getToneUriList().get(viewModel.getToneIdList().indexOf(view.getId())));
			playChosenTone();
		}
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onBackPressed() {

		if (viewModel.getPickedUri() == null) {
			if (viewModel.getShowSilent()) {
				Intent intent = new Intent().putExtra(EXTRA_RINGTONE_PICKED_URI, viewModel.getPickedUri());
				setResult(RESULT_OK, intent);
			} else {
				setResult(RESULT_CANCELED);
			}
		} else {
			Intent intent = new Intent().putExtra(EXTRA_RINGTONE_PICKED_URI, viewModel.getPickedUri());
			setResult(RESULT_OK, intent);
		}
		finish();
	}

	//----------------------------------------------------------------------------------------------------

	/**
	 * Fires an implicit Intent to open a file browser and let the user choose an alarm tone.
	 */
	private void openFileBrowser() {
		String[] mimeTypes = new String[]{"audio/mpeg", "audio/ogg", "audio/aac", "audio/x-matroska"};

		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
				.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
				.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
				.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
				.setType("*/*")
				.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

		fileActLauncher.launch(intent);

		//startActivityForResult(intent, FILE_REQUEST_CODE);
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Checks whether {@link Manifest.permission#READ_EXTERNAL_STORAGE} permission is available or not.
	 *
	 * @return {@code true} if the permission is available, otherwise {@code false}.
	 */
	private boolean isPermissionAvailable() {
		return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}

	//--------------------------------------------------------------------------------------------------

	private void checkAndRequestPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
				/////////////////////////////////////////////////////////////////
				// User has denied the permission once or more than once, but
				// never clicked on "Don't ask again" before denying.
				////////////////////////////////////////////////////////////////
				showPermissionExplanationDialog();
			} else {
				////////////////////////////////////////////////////////////////
				// Two possibilities:
				// 1. We are asking for the permission the first time.
				// 2. User has clicked on "Don't ask again".
				///////////////////////////////////////////////////////////////
				if (!sharedPreferences.getBoolean(ConstantsAndStatics.SHARED_PREF_KEY_PERMISSION_WAS_ASKED_BEFORE, false)) {
					// Permission was never asked before.
					sharedPreferences.edit()
					                 .remove(ConstantsAndStatics.SHARED_PREF_KEY_PERMISSION_WAS_ASKED_BEFORE)
					                 .putBoolean(ConstantsAndStatics.SHARED_PREF_KEY_PERMISSION_WAS_ASKED_BEFORE, true)
					                 .commit();

					ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);

				} else {
					////////////////////////////////////////////
					// User had chosen "Don't ask again".
					////////////////////////////////////////////
					showPermissionExplanationDialog();
				}
			}
			viewModel.setPermissionRationaleBeingShown(true);
		}
	}

	//--------------------------------------------------------------------------------------------------

	/**
	 * Shows an {@code AlertDialog} explaining why the permission is necessary.
	 */
	private void showPermissionExplanationDialog() {
		DialogFragment dialogPermissionReason =
				AlertDialog_PermissionReason.getInstance(getResources().getString(R.string.permissionReasonExp_ringtonePicker));
		dialogPermissionReason.setCancelable(false);
		dialogPermissionReason.show(getSupportFragmentManager(), "");
	}

	//--------------------------------------------------------------------------------------------------

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == PERMISSIONS_REQUEST_CODE) {
			viewModel.setPermissionRationaleBeingShown(false);
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show();
				initialise();
			} else {
				onPermissionDenied();
			}
		}
	}

	//----------------------------------------------------------------------------------------------------

	private void initActLaunchers() {

		fileActLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(), (result) -> {

					int resultCode = result.getResultCode();
					Intent data = result.getData();

					if (resultCode == RESULT_OK && data != null) {

						Uri toneUri = data.getData();
						assert toneUri != null;

						try (Cursor cursor = getContentResolver().query(toneUri, null, null, null, null)) {

							if (cursor != null) {

								if (viewModel.getToneUriList().contains(toneUri)) {

									int index = viewModel.getToneUriList().indexOf(toneUri);
									((RadioButton) findViewById(viewModel.getToneIdList().get(index))).setChecked(true);

								} else {

									int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
									cursor.moveToFirst();

									String fileName = cursor.getString(nameIndex);

									int indexOfDot = fileName.lastIndexOf(".");
									if (indexOfDot != -1) {
										fileName = fileName.substring(0, indexOfDot);
									}
									int toneId = View.generateViewId();

									viewModel.getToneNameList().add(fileName);
									viewModel.getToneUriList().add(toneUri);
									viewModel.getToneIdList().add(toneId);

									createOneRadioButton(toneId, fileName);

									((RadioButton) findViewById(toneId)).setChecked(true);
								}
								viewModel.setPickedUri(toneUri);
								playChosenTone();
							}
						}
					}

				});

	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onDialogPositiveClick(DialogFragment dialogFragment) {

		if (dialogFragment.getClass().equals(AlertDialog_PermissionReason.class)) {

			if ((!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) &&
					(sharedPreferences.getBoolean(ConstantsAndStatics.SHARED_PREF_KEY_PERMISSION_WAS_ASKED_BEFORE, false))) {

				////////////////////////////////////////////////////////////////////////////////
				// User had chosen "Don't ask again". We need to redirect the user to the
				// Settings app to obtain the permission.
				////////////////////////////////////////////////////////////////////////////////
				Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
				Uri uri = Uri.fromParts("package", getPackageName(), null);
				intent.setData(uri);
				startActivity(intent);
				viewModel.setPermissionRationaleBeingShown(false);

			} else {

				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
			}
		}
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	public void onDialogNegativeClick(DialogFragment dialogFragment) {
		if (dialogFragment.getClass().equals(AlertDialog_PermissionReason.class)) {
			onPermissionDenied();
		}
	}

	//----------------------------------------------------------------------------------------------------

	private void onPermissionDenied() {
		viewModel.setPermissionRationaleBeingShown(false);
		Toast.makeText(this, "Operation not possible without the permission.", Toast.LENGTH_LONG).show();
		setResult(RESULT_CANCELED);
		finish();
	}

	//----------------------------------------------------------------------------------------------------

	private void initialise() {

		if (!viewModel.getIsInitialised()) {

			RingtoneManager ringtoneManager = new RingtoneManager(this);

			Intent intent = getIntent();

			Cursor allTonesCursor;

			int type;
			if (intent.hasExtra(EXTRA_RINGTONE_TYPE)) {
				type = Objects.requireNonNull(intent.getExtras()).getInt(EXTRA_RINGTONE_TYPE);
			} else {
				type = TYPE_ALL;
			}
			ringtoneManager.setType(type);
			allTonesCursor = ringtoneManager.getCursor();

			Thread thread = new Thread(() -> {
				if (allTonesCursor.moveToFirst()) {
					do {
						int id = allTonesCursor.getInt(ID_COLUMN_INDEX);
						String uri = allTonesCursor.getString(URI_COLUMN_INDEX);

						viewModel.getToneUriList().add(Uri.parse(uri + "/" + id));
						viewModel.getToneNameList().add(allTonesCursor.getString(TITLE_COLUMN_INDEX));
						viewModel.getToneIdList().add(View.generateViewId());
					} while (allTonesCursor.moveToNext());
				}
			});
			thread.start();

			if (intent.hasExtra(EXTRA_RINGTONE_SHOW_DEFAULT)) {
				viewModel.setShowDefault(Objects.requireNonNull(intent.getExtras()).getBoolean(EXTRA_RINGTONE_SHOW_DEFAULT));
			} else {
				viewModel.setShowDefault(true);
			}

			if (intent.hasExtra(EXTRA_RINGTONE_SHOW_SILENT)) {
				viewModel.setShowSilent(Objects.requireNonNull(intent.getExtras()).getBoolean(EXTRA_RINGTONE_SHOW_SILENT));
			} else {
				viewModel.setShowSilent(false);
			}

			if (viewModel.getShowDefault()) {
				if (intent.hasExtra(EXTRA_RINGTONE_DEFAULT_URI)) {
					viewModel.setDefaultUri(Objects.requireNonNull(intent.getExtras()).getParcelable(EXTRA_RINGTONE_DEFAULT_URI));
				} else {
					if (type == RingtoneManager.TYPE_ALARM) {
						viewModel.setDefaultUri(Settings.System.DEFAULT_ALARM_ALERT_URI);
					} else if (type == RingtoneManager.TYPE_NOTIFICATION) {
						viewModel.setDefaultUri(Settings.System.DEFAULT_NOTIFICATION_URI);
					} else if (type == RingtoneManager.TYPE_RINGTONE) {
						viewModel.setDefaultUri(Settings.System.DEFAULT_RINGTONE_URI);
					} else {
						viewModel.setDefaultUri(RingtoneManager.getActualDefaultRingtoneUri(this, type));
					}
				}
			} else {
				viewModel.setDefaultUri(null);
			}

			if (intent.hasExtra(EXTRA_RINGTONE_EXISTING_URI)) {
				viewModel.setExistingUri(Objects.requireNonNull(intent.getExtras()).getParcelable(EXTRA_RINGTONE_EXISTING_URI));
				viewModel.setWasExistingUriGiven(true);
			} else {
				viewModel.setExistingUri(null);
				viewModel.setWasExistingUriGiven(false);
			}

			if (intent.hasExtra(EXTRA_RINGTONE_TITLE)) {

				String title = Objects.requireNonNull(intent.getExtras()).getString(EXTRA_RINGTONE_TITLE);
				viewModel.setTitle((CharSequence) title != null ? title : getResources().getString(R.string.ringtonePicker_defaultTitle));

			} else {
				viewModel.setTitle((CharSequence) getResources().getString(R.string.ringtonePicker_defaultTitle));
			}

			if (intent.hasExtra(ConstantsAndStatics.EXTRA_PLAY_RINGTONE)) {
				viewModel.setPlayTone(Objects.requireNonNull(intent.getExtras()).getBoolean(ConstantsAndStatics.EXTRA_PLAY_RINGTONE));
			} else {
				viewModel.setPlayTone(true);
			}

			try {
				thread.join();
			} catch (InterruptedException ignored) {
			}

			viewModel.setIsInitialised(true);

		}

		setSupportActionBar(findViewById(R.id.toolbar4));
		Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
		Objects.requireNonNull(getSupportActionBar()).setTitle(viewModel.getTitle());

		populateRadioGroup();

		chooseToneLayout.setVisibility(View.VISIBLE);
		chooseToneLayout.setOnClickListener(this);

	}

	//----------------------------------------------------------------------------------------------------

	private void playChosenTone() {
		if (viewModel.getPickedUri() != null && viewModel.getPlayTone()) {
			try {
				mediaPlayer.reset();
				mediaPlayer.setDataSource(this, viewModel.getPickedUri());
				mediaPlayer.setLooping(false);
				mediaPlayer.setAudioAttributes(audioAttributes);
				mediaPlayer.prepareAsync();
				mediaPlayer.setOnPreparedListener(MediaPlayer::start);
			} catch (Exception ignored) {
			}
		}
	}

	//----------------------------------------------------------------------------------------------------

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			mediaPlayer.release();
		} catch (Exception ignored) {
		}
	}

}
