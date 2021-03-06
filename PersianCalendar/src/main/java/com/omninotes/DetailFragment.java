/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.omninotes;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.byagowi.persiancalendar.R;
import com.byagowi.persiancalendar.util.Utils;
import com.byagowi.persiancalendar.view.activity.MainActivity;
import com.mohamadamin.persianmaterialdatetimepicker.date.DatePickerDialog;
import com.mohamadamin.persianmaterialdatetimepicker.time.RadialPickerLayout;
import com.mohamadamin.persianmaterialdatetimepicker.time.TimePickerDialog;
import com.mohamadamin.persianmaterialdatetimepicker.utils.PersianCalendar;
import com.neopixl.pixlui.components.edittext.EditText;
import com.neopixl.pixlui.components.textview.TextView;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


import butterknife.Bind;
import butterknife.ButterKnife;

import calendar.CivilDate;
import calendar.DateConverter;
import calendar.PersianDate;
import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Style;
import it.feio.android.checklistview.ChecklistManager;
import it.feio.android.checklistview.exceptions.ViewNotSupportedException;
import it.feio.android.checklistview.interfaces.CheckListChangedListener;
import it.feio.android.checklistview.models.CheckListViewItem;
import it.feio.android.checklistview.utils.DensityUtil;
import com.omninotes.async.AttachmentTask;
import com.omninotes.async.bus.NotesUpdatedEvent;
import com.omninotes.async.bus.PushbulletReplyEvent;
import com.omninotes.async.bus.SwitchFragmentEvent;
import com.omninotes.async.notes.NoteProcessorDelete;
import com.omninotes.async.notes.SaveNoteTask;
import com.omninotes.db.DbHelper;
import com.omninotes.helpers.AnalyticsHelper;
import com.omninotes.models.Attachment;
import com.omninotes.models.Category;
import com.omninotes.models.Note;
import com.omninotes.models.ONStyle;
import com.omninotes.models.Tag;
import com.omninotes.models.adapters.AttachmentAdapter;
import com.omninotes.models.adapters.NavDrawerCategoryAdapter;
import com.omninotes.models.adapters.PlacesAutoCompleteAdapter;
import com.omninotes.models.listeners.OnAttachingFileListener;
import com.omninotes.models.listeners.OnGeoUtilResultListener;
import com.omninotes.models.listeners.OnNoteSaved;
import com.omninotes.models.listeners.OnReminderPickedListener;
import com.omninotes.models.views.ExpandableHeightGridView;
import com.omninotes.utils.AlphaManager;
import com.omninotes.utils.ConnectionManager;
import com.omninotes.utils.Constants;
import com.omninotes.utils.Display;
import com.omninotes.utils.FileHelper;
import com.omninotes.utils.Fonts;
import com.omninotes.utils.GeocodeHelper;
import com.omninotes.utils.IntentChecker;
import com.omninotes.utils.KeyboardUtils;
import com.omninotes.utils.ReminderHelper;
import com.omninotes.utils.ShortcutHelper;
import com.omninotes.utils.StorageHelper;
import com.omninotes.utils.TagsHelper;
import com.omninotes.utils.TextHelper;
import com.omninotes.utils.date.DateHelper;
import com.omninotes.utils.date.ReminderPickers;
import com.recurrence.utils.DateAndTimeUtil;

import it.feio.android.pixlui.links.TextLinkClickListener;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;




public class DetailFragment extends BaseFragment implements OnReminderPickedListener, OnTouchListener,
		OnGlobalLayoutListener, OnAttachingFileListener, TextWatcher, CheckListChangedListener, OnNoteSaved,
		OnGeoUtilResultListener, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

	private static final String TIMEPICKER = "TimePickerDialog",
			DATEPICKER = "DatePickerDialog";
	private Calendar mCalendar;
	private Utils utils;

	CivilDate civilDate = null;
	CivilDate civilDateintro = null;

	PersianDate persianDate, persianDateintro;

	private static final int TAKE_PHOTO = 1;
	private static final int TAKE_VIDEO = 2;
	private static final int SET_PASSWORD = 3;
	private static final int SKETCH = 4;
	private static final int CATEGORY = 5;
	private static final int DETAIL = 6;
	private static final int FILES = 7;

	@Bind(R.id.detail_root) ViewGroup root;
	@Bind(R.id.detail_title) EditText title;
	@Bind(R.id.detail_content) EditText content;
	@Bind(R.id.gridview) ExpandableHeightGridView mGridView;
	@Bind(R.id.location) TextView locationTextView;
	@Bind(R.id.detail_timestamps) View timestampsView;
	@Bind(R.id.reminder_layout) LinearLayout reminder_layout;
	@Bind(R.id.reminder_icon) ImageView reminderIcon;
	@Bind(R.id.datetime) TextView datetime;
	@Bind(R.id.detail_tile_card) View titleCardView;
	@Bind(R.id.content_wrapper) ScrollView scrollView;
	@Bind(R.id.creation) TextView creationTextView;
	@Bind(R.id.last_modification) TextView lastModificationTextView;
	@Bind(R.id.title_wrapper) View titleWrapperView;
	@Bind(R.id.tag_marker) View tagMarkerView;
	@Bind(R.id.detail_wrapper) ViewManager detailWrapperView;

	public OnDateSetListener onDateSetListener;
	public OnTimeSetListener onTimeSetListener;
	public boolean goBack = false;
	View toggleChecklistView;
	private Uri attachmentUri;
	private AttachmentAdapter mAttachmentAdapter;
	private PopupWindow attachmentDialog;
	private Note note;
	private Note noteTmp;
	private Note noteOriginal;
	// Audio recording
	private String recordName;
	private MediaRecorder mRecorder = null;
	private MediaPlayer mPlayer = null;
	private boolean isRecording = false;
	private View isPlayingView = null;
	private Bitmap recordingBitmap;
	private ChecklistManager mChecklistManager;
	// Values to print result
	private String exitMessage;
	private Style exitCroutonStyle = ONStyle.CONFIRM;
	// Flag to check if after editing it will return to ListActivity or not
	// and in the last case a Toast will be shown instead than Crouton
	private boolean afterSavedReturnsToList = true;
	private boolean swiping;
	private int startSwipeX;
	private SharedPreferences prefs;
	private boolean onCreateOptionsMenuAlreadyCalled = false;
	private View keyboardPlaceholder;
	private boolean orientationChanged;
	private long audioRecordingTimeStart;
	private long audioRecordingTime;
	private DetailFragment mFragment;
	private Attachment sketchEdited;
	private int contentLineCounter = 1;
	private int contentCursorPosition;
	private ArrayList<String> mergedNotesIds;
	private MainActivityPep mainActivityPep;
	private boolean activityPausing;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFragment = this;
		mCalendar = Calendar.getInstance();
		utils = Utils.getInstance(getActivity());
	}


	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().post(new SwitchFragmentEvent(SwitchFragmentEvent.Direction.CHILDREN));
		EventBus.getDefault().register(this);
	}


	@Override
	public void onStop() {
		super.onStop();
		EventBus.getDefault().unregister(this);
	}


	@Override
	public void onResume() {
		super.onResume();
		// Adding a layout observer to perform calculus when showing keyboard
		if (root != null) {
			root.getViewTreeObserver().addOnGlobalLayoutListener(this);
		}

		activityPausing = false;
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		cleanMemoryLeaks();
	}


	private void cleanMemoryLeaks() {
		ChecklistManager.unregister();
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_detail_pep, container, false);
		ButterKnife.bind(this, view);
		return view;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mainActivityPep = (MainActivityPep) getActivity();

		prefs = mainActivityPep.prefs;

		mainActivityPep.getSupportActionBar().setDisplayShowTitleEnabled(false);
		mainActivityPep.getToolbar().setNavigationOnClickListener(v -> navigateUp());

		// Force the navigation drawer to stay opened if tablet mode is on, otherwise has to stay closed
		if (NavigationDrawerFragment.isDoublePanelActive()) {
			mainActivityPep.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
		} else {
			mainActivityPep.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		}

		// Restored temp note after orientation change
		if (savedInstanceState != null) {
			noteTmp = savedInstanceState.getParcelable("noteTmp");
			note = savedInstanceState.getParcelable("note");
			noteOriginal = savedInstanceState.getParcelable("noteOriginal");
			attachmentUri = savedInstanceState.getParcelable("attachmentUri");
			orientationChanged = savedInstanceState.getBoolean("orientationChanged");
		}

		// Added the sketched image if present returning from SketchFragment
		if (mainActivityPep.sketchUri != null) {
			Attachment mAttachment = new Attachment(mainActivityPep.sketchUri, Constants.MIME_TYPE_SKETCH);
			addAttachment(mAttachment);
			mainActivityPep.sketchUri = null;
			// Removes previous version of edited image
			if (sketchEdited != null) {
				noteTmp.getAttachmentsList().remove(sketchEdited);
				sketchEdited = null;
			}
		}

		init();

		setHasOptionsMenu(true);
		setRetainInstance(false);
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (noteTmp != null) {
			noteTmp.setTitle(getNoteTitle());
			noteTmp.setContent(getNoteContent());
			outState.putParcelable("noteTmp", noteTmp);
			outState.putParcelable("note", note);
			outState.putParcelable("noteOriginal", noteOriginal);
			outState.putParcelable("attachmentUri", attachmentUri);
			outState.putBoolean("orientationChanged", orientationChanged);
		}
		super.onSaveInstanceState(outState);
	}


	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	public void onPause() {
		super.onPause();

		activityPausing = true;

		// Checks "goBack" value to avoid performing a double saving
		if (!goBack) {
			saveNote(this);
		}

		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}

		// Unregistering layout observer
		if (root != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			} else {
				root.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		}

		// Closes keyboard on exit
		if (toggleChecklistView != null) {
			KeyboardUtils.hideKeyboard(toggleChecklistView);
			content.clearFocus();
		}
	}


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (getResources().getConfiguration().orientation != newConfig.orientation) {
			orientationChanged = true;
		}
	}


	private void init() {

		// Handling of Intent actions
		handleIntents();

		if (noteOriginal == null) {
			noteOriginal = getArguments().getParcelable(Constants.INTENT_NOTE);
		}

		if (note == null) {
			note = new Note(noteOriginal);
		}

		if (noteTmp == null) {
			noteTmp = new Note(note);
		}

		if (noteTmp.isLocked() && !noteTmp.isPasswordChecked()) {
			checkNoteLock(noteTmp);
			return;
		}

		initViews();
	}


	/**
	 * Checks note lock and password before showing note content
	 */
	private void checkNoteLock(Note note) {
		// If note is locked security password will be requested
		if (note.isLocked()
				&& prefs.getString(Constants.PREF_PASSWORD, null) != null
				&& !prefs.getBoolean("settings_password_access", false)) {
			BaseActivity.requestPassword(mainActivityPep, passwordConfirmed -> {
				if (passwordConfirmed) {
					noteTmp.setPasswordChecked(true);
					init();
				} else {
					goBack = true;
					goHome();
				}
			});
		} else {
			noteTmp.setPasswordChecked(true);
			init();
		}
	}


	private void handleIntents() {
		Intent i = mainActivityPep.getIntent();

		if (Constants.ACTION_MERGE.equals(i.getAction())) {
			noteOriginal = new Note();
			note = new Note(noteOriginal);
			noteTmp = getArguments().getParcelable(Constants.INTENT_NOTE);
			if (i.getStringArrayListExtra("merged_notes") != null) {
				mergedNotesIds = i.getStringArrayListExtra("merged_notes");
			}
			i.setAction(null);
		}

		// Action called from home shortcut
		if (Constants.ACTION_SHORTCUT.equals(i.getAction())
				|| Constants.ACTION_NOTIFICATION_CLICK.equals(i.getAction())) {
			afterSavedReturnsToList = false;
			noteOriginal = DbHelper.getInstance().getNote(i.getLongExtra(Constants.INTENT_KEY, 0));
			// Checks if the note pointed from the shortcut has been deleted
			try {
				note = new Note(noteOriginal);
				noteTmp = new Note(noteOriginal);
			} catch (NullPointerException e) {
				mainActivityPep.showToast(getText(R.string.shortcut_note_deleted), Toast.LENGTH_LONG);
				mainActivityPep.finish();
			}
			i.setAction(null);
		}

		// Check if is launched from a widget
		if (Constants.ACTION_WIDGET.equals(i.getAction())
				|| Constants.ACTION_TAKE_PHOTO.equals(i.getAction())) {

			afterSavedReturnsToList = false;

			//  with tags to set tag
			if (i.hasExtra(Constants.INTENT_WIDGET)) {
				String widgetId = i.getExtras().get(Constants.INTENT_WIDGET).toString();
				if (widgetId != null) {
					String sqlCondition = prefs.getString(Constants.PREF_WIDGET_PREFIX + widgetId, "");
					String categoryId = TextHelper.checkIntentCategory(sqlCondition);
					if (categoryId != null) {
						Category category;
						try {
							category = DbHelper.getInstance().getCategory(Long.parseLong(categoryId));
							noteTmp = new Note();
							noteTmp.setCategory(category);
						} catch (NumberFormatException e) {
							Log.e(Constants.TAG, "Category with not-numeric value!", e);
						}
					}
				}
			}

			// Sub-action is to take a photo
			if (Constants.ACTION_TAKE_PHOTO.equals(i.getAction())) {
				takePhoto();
			}

			i.setAction(null);
		}


		/**
		 * Handles third party apps requests of sharing
		 */
		if ((Intent.ACTION_SEND.equals(i.getAction())
				|| Intent.ACTION_SEND_MULTIPLE.equals(i.getAction())
				|| Constants.INTENT_GOOGLE_NOW.equals(i.getAction()))
				&& i.getType() != null) {

			afterSavedReturnsToList = false;

			if (noteTmp == null) noteTmp = new Note();

			// Text title
			String title = i.getStringExtra(Intent.EXTRA_SUBJECT);
			if (title != null) {
				noteTmp.setTitle(title);
			}

			// Text content
			String content = i.getStringExtra(Intent.EXTRA_TEXT);
			if (content != null) {
				noteTmp.setContent(content);
			}

			// Single attachment data
			Uri uri = i.getParcelableExtra(Intent.EXTRA_STREAM);
			// Due to the fact that Google Now passes intent as text but with
			// audio recording attached the case must be handled in specific way
			if (uri != null && !Constants.INTENT_GOOGLE_NOW.equals(i.getAction())) {
				String name = FileHelper.getNameFromUri(mainActivityPep, uri);
				AttachmentTask task = new AttachmentTask(this, uri, name, this);
				task.execute();
			}

			// Multiple attachment data
			ArrayList<Uri> uris = i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			if (uris != null) {
				for (Uri uriSingle : uris) {
					String name = FileHelper.getNameFromUri(mainActivityPep, uriSingle);
					AttachmentTask task = new AttachmentTask(this, uriSingle, name, this);
					task.execute();
				}
			}

			i.setAction(null);
		}

	}


	@SuppressLint("NewApi")
	private void initViews() {

		// Sets onTouchListener to the whole activity to swipe notes
		root.setOnTouchListener(this);

		// Overrides font sizes with the one selected from user
		Fonts.overrideTextSize(mainActivityPep, prefs, root);

		// Color of tag marker if note is tagged a function is active in preferences
		setTagMarkerColor(noteTmp.getCategory());

		// Sets links clickable in title and content Views
		title = initTitle();
		requestFocus(title);

		content = initContent();

		if (isNoteLocationValid()) {
			if (TextUtils.isEmpty(noteTmp.getAddress())) {
				try {
					noteTmp.setAddress(GeocodeHelper.getAddressFromCoordinates(mainActivityPep, noteTmp.getLatitude(),
							noteTmp.getLongitude()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			locationTextView.setVisibility(View.VISIBLE);
			locationTextView.setText(noteTmp.getAddress());
		}

		// Automatic location insertion
		if (prefs.getBoolean(Constants.PREF_AUTO_LOCATION, false) && noteTmp.get_id() == null) {
			GeocodeHelper.getLocation(mainActivityPep, this);
		}


		locationTextView.setOnClickListener(v -> {
			String uriString = "geo:" + noteTmp.getLatitude() + ',' + noteTmp.getLongitude()
					+ "?q=" + noteTmp.getLatitude() + ',' + noteTmp.getLongitude();
			Intent locationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
			if (!IntentChecker.isAvailable(mainActivityPep, locationIntent, null)) {
				uriString = "http://maps.google.com/maps?q=" + noteTmp.getLatitude() + ',' + noteTmp.getLongitude();
				locationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
			}
			startActivity(locationIntent);
		});
		locationTextView.setOnLongClickListener(v -> {
			MaterialDialog.Builder builder = new MaterialDialog.Builder(mainActivityPep);
			builder.content(R.string.remove_location);
			builder.positiveText(R.string.ok);
			builder.callback(new MaterialDialog.ButtonCallback() {
				@Override
				public void onPositive(MaterialDialog materialDialog) {
					noteTmp.setLatitude("");
					noteTmp.setLongitude("");
					fade(locationTextView, false);
				}
			});
			MaterialDialog dialog = builder.build();
			dialog.show();
			return true;
		});


		// Some fields can be filled by third party application and are always shown
		mAttachmentAdapter = new AttachmentAdapter(mainActivityPep, noteTmp.getAttachmentsList(), mGridView);

		// Initialzation of gridview for images
		mGridView.setAdapter(mAttachmentAdapter);
		mGridView.autoresize();

		// Click events for images in gridview (zooms image)
		mGridView.setOnItemClickListener((parent, v, position, id) -> {
			Attachment attachment = (Attachment) parent.getAdapter().getItem(position);
			Uri uri = attachment.getUri();
			Intent attachmentIntent;
			if (Constants.MIME_TYPE_FILES.equals(attachment.getMime_type())) {

				attachmentIntent = new Intent(Intent.ACTION_VIEW);
				attachmentIntent.setDataAndType(uri, StorageHelper.getMimeType(mainActivityPep,
						attachment.getUri()));
				attachmentIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent
						.FLAG_GRANT_WRITE_URI_PERMISSION);
				if (IntentChecker.isAvailable(mainActivityPep.getApplicationContext(), attachmentIntent, null)) {
					startActivity(attachmentIntent);
				} else {
					mainActivityPep.showMessage(R.string.feature_not_available_on_this_device, ONStyle.WARN);
				}

				// Media files will be opened in internal gallery
			} else if (Constants.MIME_TYPE_IMAGE.equals(attachment.getMime_type())
					|| Constants.MIME_TYPE_SKETCH.equals(attachment.getMime_type())
					|| Constants.MIME_TYPE_VIDEO.equals(attachment.getMime_type())) {
				// Title
				noteTmp.setTitle(getNoteTitle());
				noteTmp.setContent(getNoteContent());
				String title1 = TextHelper.parseTitleAndContent(mainActivityPep,
						noteTmp)[0].toString();
				// Images
				int clickedImage = 0;
				ArrayList<Attachment> images = new ArrayList<>();
				for (Attachment mAttachment : noteTmp.getAttachmentsList()) {
					if (Constants.MIME_TYPE_IMAGE.equals(mAttachment.getMime_type())
							|| Constants.MIME_TYPE_SKETCH.equals(mAttachment.getMime_type())
							|| Constants.MIME_TYPE_VIDEO.equals(mAttachment.getMime_type())) {
						images.add(mAttachment);
						if (mAttachment.equals(attachment)) {
							clickedImage = images.size() - 1;
						}
					}
				}
				// Intent
				attachmentIntent = new Intent(mainActivityPep, GalleryActivity.class);
				attachmentIntent.putExtra(Constants.GALLERY_TITLE, title1);
				attachmentIntent.putParcelableArrayListExtra(Constants.GALLERY_IMAGES, images);
				attachmentIntent.putExtra(Constants.GALLERY_CLICKED_IMAGE, clickedImage);
				startActivity(attachmentIntent);

			} else if (Constants.MIME_TYPE_AUDIO.equals(attachment.getMime_type())) {
				playback(v, attachment.getUri());
			}

		});

		// Long click events for images in gridview (removes image)
		mGridView.setOnItemLongClickListener((parent, v, position, id) -> {

			// To avoid deleting audio attachment during playback
			if (mPlayer != null) return false;

			MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(mainActivityPep)
					.positiveText(R.string.delete);

			// If is an image user could want to sketch it!
			if (Constants.MIME_TYPE_SKETCH.equals(mAttachmentAdapter.getItem(position).getMime_type())) {
				dialogBuilder
						.content(R.string.delete_selected_image)
						.negativeText(R.string.edit)
						.callback(new MaterialDialog.ButtonCallback() {
							@Override
							public void onPositive(MaterialDialog materialDialog) {
								removeAttachment(position);
								mAttachmentAdapter.notifyDataSetChanged();
								mGridView.autoresize();
							}


							@Override
							public void onNegative(MaterialDialog materialDialog) {
								sketchEdited = mAttachmentAdapter.getItem(position);
								takeSketch(sketchEdited);
							}
						});
			} else {
				dialogBuilder
						.content(R.string.delete_selected_image)
						.callback(new MaterialDialog.ButtonCallback() {
							@Override
							public void onPositive(MaterialDialog materialDialog) {
								removeAttachment(position);
								mAttachmentAdapter.notifyDataSetChanged();
								mGridView.autoresize();
							}
						});
			}
			dialogBuilder.build().show();
			return true;
		});


		// Preparation for reminder icon
		reminder_layout.setOnClickListener(v -> {
			int pickerType = prefs.getBoolean("settings_simple_calendar", false) ? ReminderPickers.TYPE_AOSP :
					ReminderPickers.TYPE_GOOGLE;
			ReminderPickers reminderPicker = new ReminderPickers(mainActivityPep, mFragment, pickerType);
			Long presetDateTime = noteTmp.getAlarm() != null ? Long.parseLong(noteTmp.getAlarm()) : null;
			reminderPicker.pick(presetDateTime, noteTmp.getRecurrenceRule());
			onDateSetListener = reminderPicker;
			onTimeSetListener = reminderPicker;
		});


		reminder_layout.setOnLongClickListener(v -> {
			MaterialDialog dialog = new MaterialDialog.Builder(mainActivityPep)
					.content(R.string.remove_reminder)
					.positiveText(R.string.ok)
					.callback(new MaterialDialog.ButtonCallback() {
						@Override
						public void onPositive(MaterialDialog materialDialog) {
							noteTmp.setAlarm(null);
							reminderIcon.setImageResource(R.drawable.ic_reminder_add);
							datetime.setText("");
						}
					}).build();
			dialog.show();
			return true;
		});

		// Reminder
		String reminderString = initReminder(noteTmp);
		if (!StringUtils.isEmpty(reminderString)) {
			reminderIcon.setImageResource(R.drawable.ic_alarm_grey600_18dp);
			datetime.setText(reminderString);
		}

		// Timestamps view
		// Bottom padding set for translucent navbar in Kitkat
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			int navBarHeight = Display.getNavigationBarHeightKitkat(mainActivityPep);
			int negativePadding = navBarHeight >= 27*3 ? - 27 : 0;
			int timestampsViewPaddingBottom = navBarHeight > 0 ? navBarHeight + negativePadding : timestampsView.getPaddingBottom();
			timestampsView.setPadding(timestampsView.getPaddingStart(), timestampsView.getPaddingTop(),
					timestampsView.getPaddingEnd(), timestampsViewPaddingBottom);
		}

		// Footer dates of creation...
		String creation = noteTmp.getCreationShort(mainActivityPep);
		creationTextView.append(creation.length() > 0 ? getString(R.string.creation) + " " + creation : "");
		if (creationTextView.getText().length() == 0)
			creationTextView.setVisibility(View.GONE);

		// ... and last modification
		String lastModification = noteTmp.getLastModificationShort(mainActivityPep);
		lastModificationTextView.append(lastModification.length() > 0 ? getString(R.string.last_update) + " " + lastModification : "");
		if (lastModificationTextView.getText().length() == 0)
			lastModificationTextView.setVisibility(View.GONE);
	}


	private EditText initTitle() {
		title.setText(noteTmp.getTitle());
		title.gatherLinksForText();
		title.setOnTextLinkClickListener(textLinkClickListener);
		// To avoid dropping here the  dragged checklist items
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			title.setOnDragListener((v, event) -> {
//					((View)event.getLocalState()).setVisibility(View.VISIBLE);
				return true;
			});
		}
		//When editor action is pressed focus is moved to last character in content field
		title.setOnEditorActionListener((v, actionId, event) -> {
			content.requestFocus();
			content.setSelection(content.getText().length());
			return false;
		});
		return title;
	}


	private EditText initContent() {
		content.setText(noteTmp.getContent());
		content.gatherLinksForText();
		content.setOnTextLinkClickListener(textLinkClickListener);
		// Avoids focused line goes under the keyboard
		content.addTextChangedListener(this);

		// Restore checklist
		toggleChecklistView = content;
		if (noteTmp.isChecklist()) {
			noteTmp.setChecklist(false);
			AlphaManager.setAlpha(toggleChecklistView, 0);
			toggleChecklist2();
		}

		return content;
	}


	/**
	 * Force focus and shows soft keyboard
	 */
	private void requestFocus(final EditText view) {
		if (note.get_id() == null && !noteTmp.isChanged(note)) {
			KeyboardUtils.showKeyboard(view);
		}
	}


	/**
	 * Colors tag marker in note's title and content elements
	 */
	private void setTagMarkerColor(Category tag) {

		String colorsPref = prefs.getString("settings_colors_app", Constants.PREF_COLORS_APP_DEFAULT);

		// Checking preference
		if (!"disabled".equals(colorsPref)) {

			// Choosing target view depending on another preference
			ArrayList<View> target = new ArrayList<>();
			if ("complete".equals(colorsPref)) {
				target.add(titleWrapperView);
				target.add(scrollView);
			} else {
				target.add(tagMarkerView);
			}

			// Coloring the target
			if (tag != null && tag.getColor() != null) {
				for (View view : target) {
					view.setBackgroundColor(Integer.parseInt(tag.getColor()));
				}
			} else {
				for (View view : target) {
					view.setBackgroundColor(Color.parseColor("#00000000"));
				}
			}
		}
	}


	@SuppressLint("NewApi")
	private void displayLocationDialog() {
		GeocodeHelper.getLocation(mainActivityPep.getApplicationContext(), new OnGeoUtilResultListener() {
			@Override
			public void onAddressResolved(String address) {}

			@Override
			public void onCoordinatesResolved(Location location, String address) {}

			@Override
			public void onLocationRetrieved(Location location) {
				if (location == null) {
					return;
				}
				if (!ConnectionManager.internetAvailable(mainActivityPep)) {
					noteTmp.setLatitude(location.getLatitude());
					noteTmp.setLongitude(location.getLongitude());
					onAddressResolved("");
					return;
				}
				LayoutInflater inflater = mainActivityPep.getLayoutInflater();
				View v = inflater.inflate(R.layout.dialog_location_pep, null);
				final AutoCompleteTextView autoCompView = (AutoCompleteTextView) v.findViewById(R.id
						.auto_complete_location);
				autoCompView.setHint(getString(R.string.search_location));
				autoCompView.setAdapter(new PlacesAutoCompleteAdapter(mainActivityPep, R.layout.simple_text_layout_pep));
				final MaterialDialog dialog = new MaterialDialog.Builder(mainActivityPep)
						.customView(autoCompView, false)
						.positiveText(R.string.use_current_location)
						.callback(new MaterialDialog.ButtonCallback() {
							@Override
							public void onPositive(MaterialDialog materialDialog) {
								if (TextUtils.isEmpty(autoCompView.getText().toString())) {
									noteTmp.setLatitude(location.getLatitude());
									noteTmp.setLongitude(location.getLongitude());
									GeocodeHelper.getAddressFromCoordinates(mainActivityPep, location, mFragment);
								} else {
									GeocodeHelper.getCoordinatesFromAddress(mainActivityPep, autoCompView.getText()
													.toString(),
											mFragment);
								}
							}
						})
						.build();
				autoCompView.addTextChangedListener(new TextWatcher() {
					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}


					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
						if (s.length() != 0) {
							dialog.setActionButton(DialogAction.POSITIVE, getString(R.string.confirm));
						} else {
							dialog.setActionButton(DialogAction.POSITIVE, getString(R.string.use_current_location));
						}
					}


					@Override
					public void afterTextChanged(Editable s) {
					}
				});
				dialog.show();
			}
		});

	}


	@Override
	public void onLocationRetrieved(Location location) {
		if (location == null) {
			mainActivityPep.showMessage(R.string.location_not_found, ONStyle.ALERT);
		}
		if (isNoteLocationValid()) {
			if (!TextUtils.isEmpty(noteTmp.getAddress())) {
				locationTextView.setVisibility(View.VISIBLE);
				locationTextView.setText(noteTmp.getAddress());
			} else {
				GeocodeHelper.getAddressFromCoordinates(mainActivityPep, location, mFragment);
			}
		}
	}


	@Override
	public void onAddressResolved(String address) {
		if (TextUtils.isEmpty(address)) {
			if (!isNoteLocationValid()) {
				mainActivityPep.showMessage(R.string.location_not_found, ONStyle.ALERT);
				return;
			}
			address = noteTmp.getLatitude() + ", " + noteTmp.getLongitude();
		}
		if (!GeocodeHelper.areCoordinates(address)) {
			noteTmp.setAddress(address);
		}
		locationTextView.setVisibility(View.VISIBLE);
		locationTextView.setText(address);
		fade(locationTextView, true);
	}


	@Override
	public void onCoordinatesResolved(Location location, String address) {
		if (location != null) {
			noteTmp.setLatitude(location.getLatitude());
			noteTmp.setLongitude(location.getLongitude());
			noteTmp.setAddress(address);
			locationTextView.setVisibility(View.VISIBLE);
			locationTextView.setText(address);
			fade(locationTextView, true);
		} else {
			mainActivityPep.showMessage(R.string.location_not_found, ONStyle.ALERT);
		}
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_detail_pep, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}


	@Override
	public void onPrepareOptionsMenu(Menu menu) {

		// Closes search view if left open in List fragment
		MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
		if (searchMenuItem != null) {
			MenuItemCompat.collapseActionView(searchMenuItem);
		}

		boolean newNote = noteTmp.get_id() == null;

		menu.findItem(R.id.menu_checklist_on).setVisible(!noteTmp.isChecklist());
		menu.findItem(R.id.menu_checklist_off).setVisible(noteTmp.isChecklist());


		// If note is trashed only this options will be available from menu
		if (noteTmp.isTrashed()) {
			menu.findItem(R.id.menu_untrash).setVisible(true);
			menu.findItem(R.id.menu_delete).setVisible(true);
			// Otherwise all other actions will be available
		} else {
			menu.findItem(R.id.menu_add_shortcut).setVisible(!newNote);
			menu.findItem(R.id.menu_archive).setVisible(!newNote && !noteTmp.isArchived());
			menu.findItem(R.id.menu_unarchive).setVisible(!newNote && noteTmp.isArchived());
			menu.findItem(R.id.menu_trash).setVisible(!newNote);
		}
	}


	@SuppressLint("NewApi")
	public boolean goHome() {
		stopPlaying();

		// The activity has managed a shared intent from third party app and
		// performs a normal onBackPressed instead of returning back to ListActivity
		if (!afterSavedReturnsToList) {
			if (!TextUtils.isEmpty(exitMessage)) {
				mainActivityPep.showToast(exitMessage, Toast.LENGTH_SHORT);
			}
			mainActivityPep.finish();
			return true;
		} else {
			if (!TextUtils.isEmpty(exitMessage) && exitCroutonStyle != null) {
				mainActivityPep.showMessage(exitMessage, exitCroutonStyle);
			}
		}

		// Otherwise the result is passed to ListActivity
		if (mainActivityPep != null && mainActivityPep.getSupportFragmentManager() != null) {
			mainActivityPep.getSupportFragmentManager().popBackStack();
			if (mainActivityPep.getSupportFragmentManager().getBackStackEntryCount() == 1) {
				mainActivityPep.getSupportActionBar().setDisplayShowTitleEnabled(true);
				if (mainActivityPep.getDrawerToggle() != null) {
					mainActivityPep.getDrawerToggle().setDrawerIndicatorEnabled(true);
				}
				EventBus.getDefault().post(new SwitchFragmentEvent(SwitchFragmentEvent.Direction.PARENT));
			}
		}

		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				navigateUp();
				break;
			case R.id.menu_attachment:
				showPopup(mainActivityPep.findViewById(R.id.menu_attachment));
				break;
			case R.id.menu_tag:
				addTags();
				break;
			case R.id.menu_category:
				categorizeNote();
				break;
			case R.id.menu_share:
				shareNote();
				break;
			case R.id.menu_checklist_on:
				toggleChecklist();
				break;
			case R.id.menu_checklist_off:
				toggleChecklist();
				break;
			case R.id.menu_add_shortcut:
				addShortcut();
				break;
			case R.id.menu_archive:
				archiveNote(true);
				break;
			case R.id.menu_unarchive:
				archiveNote(false);
				break;
			case R.id.menu_trash:
				trashNote(true);
				break;
			case R.id.menu_untrash:
				trashNote(false);
				break;
			case R.id.menu_discard_changes:
				discard();
				break;
			case R.id.menu_delete:
				deleteNote();
				break;
		}

		AnalyticsHelper.trackActionFromResourceId(getActivity(), item.getItemId());

		return super.onOptionsItemSelected(item);
	}


	private void navigateUp() {
		afterSavedReturnsToList = true;
		saveAndExit(this);
	}


	/**
	 *
	 */
	private void toggleChecklist() {

		// In case checklist is active a prompt will ask about many options
		// to decide hot to convert back to simple text
		if (!noteTmp.isChecklist()) {
			toggleChecklist2();
			return;
		}

		// If checklist is active but no items are checked the conversion in done automatically
		// without prompting user
		if (mChecklistManager.getCheckedCount() == 0) {
			toggleChecklist2(true, false);
			return;
		}

//		// Inflate the popup_layout.xml
//		LayoutInflater inflater = (LayoutInflater) mainActivityPep.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
//		final View layout = inflater.inflate(R.layout.dialog_remove_checklist_layout_pep,
//				(ViewGroup) getView().findViewById(R.id.layout_root));
//
////		 Retrieves options checkboxes and initialize their values
//		final CheckBox keepChecked = (CheckBox) layout.findViewById(R.id.checklist_keep_checked);
//		final CheckBox keepCheckmarks = (CheckBox) layout.findViewById(R.id.checklist_keep_checkmarks);
//		keepChecked.setChecked(prefs.getBoolean(Constants.PREF_KEEP_CHECKED, true));
//		keepCheckmarks.setChecked(prefs.getBoolean(Constants.PREF_KEEP_CHECKMARKS, true));
//
//		new MaterialDialog.Builder(mainActivityPep)
//				.customView(layout, false)
//				.positiveText(R.string.ok)
//				.callback(new MaterialDialog.ButtonCallback() {
//					@Override
//					public void onPositive(MaterialDialog materialDialog) {
//						prefs.edit()
//								.putBoolean(Constants.PREF_KEEP_CHECKED, keepChecked.isChecked())
//								.putBoolean(Constants.PREF_KEEP_CHECKMARKS, keepCheckmarks.isChecked())
//								.apply();
//						toggleChecklist2();
//					}
//				}).build().show();
	}


	/**
	 * Toggles checklist view
	 */
	private void toggleChecklist2() {
		boolean keepChecked = prefs.getBoolean(Constants.PREF_KEEP_CHECKED, true);
		boolean showChecks = prefs.getBoolean(Constants.PREF_KEEP_CHECKMARKS, true);
		toggleChecklist2(keepChecked, showChecks);
	}


	@SuppressLint("NewApi")
	private void toggleChecklist2(final boolean keepChecked, final boolean showChecks) {
		// Get instance and set options to convert EditText to CheckListView
		mChecklistManager = ChecklistManager.getInstance(mainActivityPep);
		mChecklistManager.setMoveCheckedOnBottom(Integer.valueOf(prefs.getString("settings_checked_items_behavior",
				String.valueOf(it.feio.android.checklistview.Settings.CHECKED_HOLD))));
		mChecklistManager.setShowChecks(true);
		mChecklistManager.setNewEntryHint(getString(R.string.checklist_item_hint));

		// Links parsing options
		mChecklistManager.setOnTextLinkClickListener(textLinkClickListener);
		mChecklistManager.addTextChangedListener(mFragment);
		mChecklistManager.setCheckListChangedListener(mFragment);

		// Options for converting back to simple text
		mChecklistManager.setKeepChecked(keepChecked);
		mChecklistManager.setShowChecks(showChecks);

		// Vibration
		mChecklistManager.setDragVibrationEnabled(true);

		// Switches the views
		View newView = null;
		try {
			newView = mChecklistManager.convert(toggleChecklistView);
		} catch (ViewNotSupportedException e) {
			Log.e(Constants.TAG, "Error switching checklist view", e);
		}

		// Switches the views
		if (newView != null) {
			mChecklistManager.replaceViews(toggleChecklistView, newView);
			toggleChecklistView = newView;
			animate(toggleChecklistView).alpha(1).scaleXBy(0).scaleX(1).scaleYBy(0).scaleY(1);
			noteTmp.setChecklist(!noteTmp.isChecklist());
		}
	}


	/**
	 * Categorize note choosing from a list of previously created categories
	 */
	private void categorizeNote() {
		// Retrieves all available categories
		final ArrayList<Category> categories = DbHelper.getInstance().getCategories();

		String currentCategory = noteTmp.getCategory() != null ? String.valueOf(noteTmp.getCategory().getId()) : null;

		final MaterialDialog dialog = new MaterialDialog.Builder(mainActivityPep)
				.title(R.string.categorize_as)
				.adapter(new NavDrawerCategoryAdapter(mainActivityPep, categories, currentCategory), null)
				.positiveText(R.string.add_category)
				.positiveColorRes(R.color.colorPrimary)
				.negativeText(R.string.remove_category)
				.negativeColorRes(R.color.colorAccent)
				.callback(new MaterialDialog.ButtonCallback() {
					@Override
					public void onPositive(MaterialDialog dialog) {
						Intent intent = new Intent(mainActivityPep, CategoryActivity.class);
						intent.putExtra("noHome", true);
						startActivityForResult(intent, CATEGORY);
					}


					@Override
					public void onNegative(MaterialDialog dialog) {
						noteTmp.setCategory(null);
						setTagMarkerColor(null);
					}
				})
				.build();

		dialog.getListView().setOnItemClickListener((parent, view, position, id) -> {
			noteTmp.setCategory(categories.get(position));
			setTagMarkerColor(categories.get(position));
			dialog.dismiss();
		});

		dialog.show();
	}


	// The method that displays the popup.
	@SuppressWarnings("deprecation")
	private void showPopup(View anchor) {
		DisplayMetrics metrics = new DisplayMetrics();
		mainActivityPep.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		// Inflate the popup_layout.xml
		LayoutInflater inflater = (LayoutInflater) mainActivityPep.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.attachment_dialog_pep, null);

		// Creating the PopupWindow
		attachmentDialog = new PopupWindow(mainActivityPep);
		attachmentDialog.setContentView(layout);
		attachmentDialog.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		attachmentDialog.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		attachmentDialog.setFocusable(true);
		attachmentDialog.setOnDismissListener(() -> {
			if (isRecording) {
				isRecording = false;
				stopRecording();
			}
		});

		// Clear the default translucent background
		attachmentDialog.setBackgroundDrawable(new BitmapDrawable());

		// Camera
		android.widget.TextView cameraSelection = (android.widget.TextView) layout.findViewById(R.id.camera);
		cameraSelection.setOnClickListener(new AttachmentOnClickListener());
		// Audio recording
		android.widget.TextView recordingSelection = (android.widget.TextView) layout.findViewById(R.id.recording);
		recordingSelection.setOnClickListener(new AttachmentOnClickListener());
		// Video recording
		android.widget.TextView videoSelection = (android.widget.TextView) layout.findViewById(R.id.video);
		videoSelection.setOnClickListener(new AttachmentOnClickListener());
		// Files
		android.widget.TextView filesSelection = (android.widget.TextView) layout.findViewById(R.id.files);
		filesSelection.setOnClickListener(new AttachmentOnClickListener());
		// Sketch
		android.widget.TextView sketchSelection = (android.widget.TextView) layout.findViewById(R.id.sketch);
		sketchSelection.setOnClickListener(new AttachmentOnClickListener());


		try {
			attachmentDialog.showAsDropDown(anchor);
		} catch (Exception e) {
			mainActivityPep.showMessage(R.string.error, ONStyle.ALERT);

		}
	}


	private void takePhoto() {
		// Checks for camera app available
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (!IntentChecker.isAvailable(mainActivityPep, intent, new String[]{PackageManager.FEATURE_CAMERA})) {
			mainActivityPep.showMessage(R.string.feature_not_available_on_this_device, ONStyle.ALERT);

			return;
		}
		// Checks for created file validity
		File f = StorageHelper.createNewAttachmentFile(mainActivityPep, Constants.MIME_TYPE_IMAGE_EXT);
		if (f == null) {
			mainActivityPep.showMessage(R.string.error, ONStyle.ALERT);
			return;
		}
		// Launches intent
		attachmentUri = Uri.fromFile(f);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, attachmentUri);
		startActivityForResult(intent, TAKE_PHOTO);
	}


	private void takeVideo() {
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		if (!IntentChecker.isAvailable(mainActivityPep, takeVideoIntent, new String[]{PackageManager.FEATURE_CAMERA})) {
			mainActivityPep.showMessage(R.string.feature_not_available_on_this_device, ONStyle.ALERT);

			return;
		}
		// File is stored in custom ON folder to speedup the attachment
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
			File f = StorageHelper.createNewAttachmentFile(mainActivityPep, Constants.MIME_TYPE_VIDEO_EXT);
			if (f == null) {
				mainActivityPep.showMessage(R.string.error, ONStyle.ALERT);

				return;
			}
			attachmentUri = Uri.fromFile(f);
			takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, attachmentUri);
		}
		String maxVideoSizeStr = "".equals(prefs.getString("settings_max_video_size",
				"")) ? "0" : prefs.getString("settings_max_video_size", "");
		int maxVideoSize = Integer.parseInt(maxVideoSizeStr);
		takeVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, Long.valueOf(maxVideoSize * 1024 * 1024));
		startActivityForResult(takeVideoIntent, TAKE_VIDEO);
	}


	private void takeSketch(Attachment attachment) {

		File f = StorageHelper.createNewAttachmentFile(mainActivityPep, Constants.MIME_TYPE_SKETCH_EXT);
		if (f == null) {
			mainActivityPep.showMessage(R.string.error, ONStyle.ALERT);
			return;
		}
		attachmentUri = Uri.fromFile(f);

		// Forces portrait orientation to this fragment only
		mainActivityPep.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Fragments replacing
		FragmentTransaction transaction = mainActivityPep.getSupportFragmentManager().beginTransaction();
		mainActivityPep.animateTransition(transaction, mainActivityPep.TRANSITION_HORIZONTAL);
		SketchFragment mSketchFragment = new SketchFragment();
		Bundle b = new Bundle();
		b.putParcelable(MediaStore.EXTRA_OUTPUT, attachmentUri);
		if (attachment != null) {
			b.putParcelable("base", attachment.getUri());
		}
		mSketchFragment.setArguments(b);
		transaction.replace(R.id.fragment_container, mSketchFragment, mainActivityPep.FRAGMENT_SKETCH_TAG)
				.addToBackStack(mainActivityPep.FRAGMENT_DETAIL_TAG).commit();
	}


	@SuppressLint("NewApi")
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// Fetch uri from activities, store into adapter and refresh adapter
		Attachment attachment;
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
				case TAKE_PHOTO:
					attachment = new Attachment(attachmentUri, Constants.MIME_TYPE_IMAGE);
					addAttachment(attachment);
					mAttachmentAdapter.notifyDataSetChanged();
					mGridView.autoresize();
					break;
				case TAKE_VIDEO:
					// Gingerbread doesn't allow custom folder so data are retrieved from intent
					if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
						attachment = new Attachment(attachmentUri, Constants.MIME_TYPE_VIDEO);
					} else {
						attachment = new Attachment(intent.getData(), Constants.MIME_TYPE_VIDEO);
					}
					addAttachment(attachment);
					mAttachmentAdapter.notifyDataSetChanged();
					mGridView.autoresize();
					break;
				case FILES:
					onActivityResultManageReceivedFiles(intent);
					break;
				case SET_PASSWORD:
					noteTmp.setPasswordChecked(true);
					lockUnlock();
					break;
				case SKETCH:
					attachment = new Attachment(attachmentUri, Constants.MIME_TYPE_SKETCH);
					addAttachment(attachment);
					mAttachmentAdapter.notifyDataSetChanged();
					mGridView.autoresize();
					break;
				case CATEGORY:
					mainActivityPep.showMessage(R.string.category_saved, ONStyle.CONFIRM);
					Category category = intent.getParcelableExtra("category");
					noteTmp.setCategory(category);
					setTagMarkerColor(category);
					break;
				case DETAIL:
					mainActivityPep.showMessage(R.string.note_updated, ONStyle.CONFIRM);
					break;
			}
		}
	}


	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void onActivityResultManageReceivedFiles(Intent intent) {
		List<Uri> uris = new ArrayList<>();
		if (Build.VERSION.SDK_INT > 16 && intent.getClipData() != null) {
			for (int i = 0; i < intent.getClipData().getItemCount(); i++) {
				uris.add(intent.getClipData().getItemAt(i).getUri());
			}
		} else {
			uris.add(intent.getData());
		}
		for (Uri uri : uris) {
			String name = FileHelper.getNameFromUri(mainActivityPep, uri);
			new AttachmentTask(this, uri, name, this).execute();
		}
	}


	/**
	 * Discards changes done to the note and eventually delete new attachments
	 */
	@SuppressLint("NewApi")
	private void discard() {
		// Checks if some new files have been attached and must be removed
		if (!noteTmp.getAttachmentsList().equals(note.getAttachmentsList())) {
			for (Attachment newAttachment : noteTmp.getAttachmentsList()) {
				if (!note.getAttachmentsList().contains(newAttachment)) {
					StorageHelper.delete(mainActivityPep, newAttachment.getUri().getPath());
				}
			}
		}

		goBack = true;

		if (!noteTmp.equals(noteOriginal)) {
			// Restore original status of the note
			if (noteOriginal.get_id() == null) {
				mainActivityPep.deleteNote(noteTmp);
				goHome();
			} else {
				SaveNoteTask saveNoteTask = new SaveNoteTask(this, false);
				if (Build.VERSION.SDK_INT >= 11) {
					saveNoteTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, noteOriginal);
				} else {
					saveNoteTask.execute(noteOriginal);
				}
			}
			MainActivityPep.notifyAppWidgets(mainActivityPep);
		} else {
			goHome();
		}
	}


	@SuppressLint("NewApi")
	private void archiveNote(boolean archive) {
		// Simply go back if is a new note
		if (noteTmp.get_id() == null) {
			goHome();
			return;
		}

		noteTmp.setArchived(archive);
		goBack = true;
		exitMessage = archive ? getString(R.string.note_archived) : getString(R.string.note_unarchived);
		exitCroutonStyle = archive ? ONStyle.WARN : ONStyle.INFO;
		saveNote(this);
	}


	@SuppressLint("NewApi")
	private void trashNote(boolean trash) {
		// Simply go back if is a new note
		if (noteTmp.get_id() == null) {
			goHome();
			return;
		}

		noteTmp.setTrashed(trash);
		goBack = true;
		exitMessage = trash ? getString(R.string.note_trashed) : getString(R.string.note_untrashed);
		exitCroutonStyle = trash ? ONStyle.WARN : ONStyle.INFO;
		if (trash) {
			ShortcutHelper.removeshortCut(OmniNotes.getAppContext(), noteTmp);
			ReminderHelper.removeReminder(OmniNotes.getAppContext(), noteTmp);
		} else {
			ReminderHelper.addReminder(OmniNotes.getAppContext(), note);
		}
		saveNote(this);
	}


	private void deleteNote() {
		new MaterialDialog.Builder(mainActivityPep)
				.content(R.string.delete_note_confirmation)
				.positiveText(R.string.ok)
				.callback(new MaterialDialog.ButtonCallback() {
					@Override
					public void onPositive(MaterialDialog materialDialog) {
						mainActivityPep.deleteNote(noteTmp);
						Log.d(Constants.TAG, "Deleted note with id '" + noteTmp.get_id() + "'");
						mainActivityPep.showMessage(R.string.note_deleted, ONStyle.ALERT);
						goHome();
					}
				}).build().show();
	}


	public void saveAndExit(OnNoteSaved mOnNoteSaved) {
		if (isAdded()) {
			exitMessage = getString(R.string.note_updated);
			exitCroutonStyle = ONStyle.CONFIRM;
			goBack = true;
			saveNote(mOnNoteSaved);
		}
	}


	/**
	 * Save new notes, modify them or archive
	 */
	void saveNote(OnNoteSaved mOnNoteSaved) {

		// Changed fields
		noteTmp.setTitle(getNoteTitle());
		noteTmp.setContent(getNoteContent());

		// Check if some text or attachments of any type have been inserted or
		// is an empty note
		if (goBack && TextUtils.isEmpty(noteTmp.getTitle()) && TextUtils.isEmpty(noteTmp.getContent())
				&& noteTmp.getAttachmentsList().size() == 0) {
			Log.d(Constants.TAG, "Empty note not saved");
			exitMessage = getString(R.string.empty_note_not_saved);
			exitCroutonStyle = ONStyle.INFO;
			goHome();
			return;
		}

		if (saveNotNeeded()) {
			exitMessage = "";
			if (goBack) {
				goHome();
			}
			return;
		}

		noteTmp.setAttachmentsListOld(note.getAttachmentsList());

		// Saving changes to the note
		SaveNoteTask saveNoteTask = new SaveNoteTask(mOnNoteSaved, lastModificationUpdatedNeeded());
		saveNoteTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, noteTmp);
	}


	/**
	 * Checks if nothing is changed to avoid committing if possible (check)
	 */
	private boolean saveNotNeeded() {
		if (noteTmp.get_id() == null && prefs.getBoolean(Constants.PREF_AUTO_LOCATION, false)) {
			note.setLatitude(noteTmp.getLatitude());
			note.setLongitude(noteTmp.getLongitude());
		}
		return !noteTmp.isChanged(note);
	}


	/**
	 * Checks if only tag, archive or trash status have been changed
	 * and then force to not update last modification date*
	 */
	private boolean lastModificationUpdatedNeeded() {
		note.setCategory(noteTmp.getCategory());
		note.setArchived(noteTmp.isArchived());
		note.setTrashed(noteTmp.isTrashed());
		note.setLocked(noteTmp.isLocked());
		return noteTmp.isChanged(note);
	}


	@Override
	public void onNoteSaved(Note noteSaved) {
		if (!activityPausing) {
			MainActivityPep.notifyAppWidgets(OmniNotes.getAppContext());
			EventBus.getDefault().post(new NotesUpdatedEvent());
			deleteMergedNotes(mergedNotesIds);
			if (noteTmp.getAlarm() != null && !noteTmp.getAlarm().equals(note.getAlarm())) {
				ReminderHelper.showReminderMessage(noteTmp.getAlarm());
			}
		}
		note = new Note(noteSaved);
		if (goBack) {
			goHome();
		}
	}


	private void deleteMergedNotes(List<String> mergedNotesIds) {
		ArrayList<Note> notesToDelete = new ArrayList<Note>();
		if (mergedNotesIds != null) {
			for (String mergedNoteId : mergedNotesIds) {
				Note note = new Note();
				note.set_id(Long.valueOf(mergedNoteId));
				notesToDelete.add(note);
			}
			new NoteProcessorDelete(notesToDelete).process();
		}
	}


	private String getNoteTitle() {
		if (title != null && !TextUtils.isEmpty(title.getText())) {
			return title.getText().toString();
		} else {
			return "";
		}
	}


	private String getNoteContent() {
		String contentText = "";
		if (!noteTmp.isChecklist()) {
			// Due to checklist library introduction the returned EditText class is no more a
			// com.neopixl.pixlui.components.edittext.EditText but a standard android.widget.EditText
			View contentView = root.findViewById(R.id.detail_content);
			if (contentView instanceof EditText) {
				contentText = ((EditText) contentView).getText().toString();
			} else if (contentView instanceof android.widget.EditText) {
				contentText = ((android.widget.EditText) contentView).getText().toString();
			}
		} else {
			if (mChecklistManager != null) {
				mChecklistManager.setKeepChecked(true);
				mChecklistManager.setShowChecks(true);
				contentText = mChecklistManager.getText();
			}
		}
		return contentText;
	}


	/**
	 * Updates share intent
	 */
	private void shareNote() {
		Note sharedNote = new Note(noteTmp);
		sharedNote.setTitle(getNoteTitle());
		sharedNote.setContent(getNoteContent());
		mainActivityPep.shareNote(sharedNote);
	}


	/**
	 * Notes locking with security password to avoid viewing, editing or deleting from unauthorized
	 */
	private void lockNote() {
		Log.d(Constants.TAG, "Locking or unlocking note " + note.get_id());

		// If security password is not set yes will be set right now
		if (prefs.getString(Constants.PREF_PASSWORD, null) == null) {
			Intent passwordIntent = new Intent(mainActivityPep, PasswordActivity.class);
			startActivityForResult(passwordIntent, SET_PASSWORD);
			return;
		}

		// If password has already been inserted will not be asked again
		if (noteTmp.isPasswordChecked() || prefs.getBoolean("settings_password_access", false)) {
			lockUnlock();
			return;
		}

		// Password will be requested here
		BaseActivity.requestPassword(mainActivityPep, passwordConfirmed -> {
			if (passwordConfirmed) {
				lockUnlock();
			}
		});
	}


	private void lockUnlock() {
		// Empty password has been set
		if (prefs.getString(Constants.PREF_PASSWORD, null) == null) {
			mainActivityPep.showMessage(R.string.password_not_set, ONStyle.WARN);

			return;
		}
		// Otherwise locking is performed
		if (noteTmp.isLocked()) {
			mainActivityPep.showMessage(R.string.save_note_to_lock_it, ONStyle.INFO);
			mainActivityPep.supportInvalidateOptionsMenu();
		} else {
			mainActivityPep.showMessage(R.string.save_note_to_lock_it, ONStyle.INFO);
			mainActivityPep.supportInvalidateOptionsMenu();
		}
		noteTmp.setLocked(!noteTmp.isLocked());
		noteTmp.setPasswordChecked(true);
	}


	/**
	 * Used to set actual reminder state when initializing a note to be edited
	 */
	private String initReminder(Note note) {
		if (noteTmp.getAlarm() == null) {
			return "";
		}
		long reminder = Long.parseLong(note.getAlarm());
		String rrule = note.getRecurrenceRule();
		if (!TextUtils.isEmpty(rrule)) {
			return DateHelper.getNoteRecurrentReminderText(reminder, rrule);
		} else {
			return DateHelper.getNoteReminderText(reminder);
		}
	}


	/**
	 * Audio recordings playback
	 */
	private void playback(View v, Uri uri) {
		// Some recording is playing right now
		if (mPlayer != null && mPlayer.isPlaying()) {
			if (isPlayingView != v) {
				// If the audio actually played is NOT the one from the click view the last one is played
				stopPlaying();
				isPlayingView = v;
				startPlaying(uri);
				replacePlayingAudioBitmap(v);
			} else {
				// Otherwise just stops playing
				stopPlaying();
			}
		} else {
			// If nothing is playing audio just plays
			isPlayingView = v;
			startPlaying(uri);
			replacePlayingAudioBitmap(v);
		}
	}


	private void replacePlayingAudioBitmap (View v) {
		Drawable d = ((ImageView) v.findViewById(R.id.gridview_item_picture)).getDrawable();
		if (BitmapDrawable.class.isAssignableFrom(d.getClass())) {
			recordingBitmap =  ((BitmapDrawable) d).getBitmap();
		} else {
			recordingBitmap =  ((GlideBitmapDrawable)d.getCurrent()).getBitmap();
		}
		((ImageView) v.findViewById(R.id.gridview_item_picture)).setImageBitmap(ThumbnailUtils
				.extractThumbnail(BitmapFactory.decodeResource(mainActivityPep.getResources(),
						R.drawable.stop), Constants.THUMBNAIL_SIZE, Constants.THUMBNAIL_SIZE));
	}


	private void startPlaying(Uri uri) {
		if (mPlayer == null) {
			mPlayer = new MediaPlayer();
		}
		try {
			mPlayer.setDataSource(mainActivityPep, uri);
			mPlayer.prepare();
			mPlayer.start();
			mPlayer.setOnCompletionListener(mp -> {
				mPlayer = null;
				((ImageView) isPlayingView.findViewById(R.id.gridview_item_picture)).setImageBitmap
						(recordingBitmap);
				recordingBitmap = null;
				isPlayingView = null;
			});
		} catch (IOException e) {
			Log.e(Constants.TAG, "prepare() failed", e);
			mainActivityPep.showMessage(R.string.error, ONStyle.ALERT);
		}
	}


	private void stopPlaying() {
		if (mPlayer != null) {
			((ImageView) isPlayingView.findViewById(R.id.gridview_item_picture)).setImageBitmap(recordingBitmap);
			isPlayingView = null;
			recordingBitmap = null;
			mPlayer.release();
			mPlayer = null;
		}
	}


	private void startRecording() {
		File f = StorageHelper.createNewAttachmentFile(mainActivityPep, Constants.MIME_TYPE_AUDIO_EXT);
		if (f == null) {
			mainActivityPep.showMessage(R.string.error, ONStyle.ALERT);
			return;
		}
		if (mRecorder == null) {
			mRecorder = new MediaRecorder();
			mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
			mRecorder.setAudioEncodingBitRate(16);
			mRecorder.setAudioSamplingRate(44100);
		}
		recordName = f.getAbsolutePath();
		mRecorder.setOutputFile(recordName);

		try {
			audioRecordingTimeStart = Calendar.getInstance().getTimeInMillis();
			mRecorder.prepare();
			mRecorder.start();
		} catch (IOException | IllegalStateException e) {
			Log.e(Constants.TAG, "prepare() failed", e);
			mainActivityPep.showMessage(R.string.error, ONStyle.ALERT);
		}
	}


	private void stopRecording() {
		if (mRecorder != null) {
			mRecorder.stop();
			audioRecordingTime = Calendar.getInstance().getTimeInMillis() - audioRecordingTimeStart;
			mRecorder.release();
			mRecorder = null;
		}
	}

	@SuppressWarnings("ResourceType")
	private void fade(final View v, boolean fadeIn) {

		int anim = R.animator.fade_out_support;
		int visibilityTemp = View.GONE;


		if (fadeIn) {
			anim = R.animator.fade_in_support;
			visibilityTemp = View.VISIBLE;
		}

		final int visibility = visibilityTemp;

		// Checks if user has left the app
		if (mainActivityPep != null) {
			Animation mAnimation = AnimationUtils.loadAnimation(mainActivityPep, anim);
			mAnimation.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
					// Nothind to do
				}


				@Override
				public void onAnimationRepeat(Animation animation) {
					// Nothind to do
				}


				@Override
				public void onAnimationEnd(Animation animation) {
					v.setVisibility(visibility);
				}
			});
			v.startAnimation(mAnimation);
		}
	}


	/**
	 * Adding shortcut on Home screen
	 */
	private void addShortcut() {
		ShortcutHelper.addShortcut(OmniNotes.getAppContext(), noteTmp);
		mainActivityPep.showMessage(R.string.shortcut_added, ONStyle.INFO);
	}


	TextLinkClickListener textLinkClickListener = new TextLinkClickListener() {
		@Override
		public void onTextLinkClick(View view, final String clickedString, final String url) {
			new MaterialDialog.Builder(mainActivityPep)
					.content(clickedString)
					.negativeColorRes(R.color.colorPrimary)
					.positiveText(R.string.open)
					.negativeText(R.string.copy)
					.callback(new MaterialDialog.ButtonCallback() {
						@Override
						public void onPositive(MaterialDialog dialog) {
							boolean error = false;
							Intent intent = null;
							try {
								intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
								intent.addCategory(Intent.CATEGORY_BROWSABLE);
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							} catch (NullPointerException e) {
								error = true;
							}

							if (intent == null
									|| error
									|| !IntentChecker
									.isAvailable(
											mainActivityPep,
											intent,
											new String[]{PackageManager.FEATURE_CAMERA})) {
								mainActivityPep.showMessage(R.string.no_application_can_perform_this_action,
										ONStyle.ALERT);

							} else {
								startActivity(intent);
							}
						}


						@Override
						public void onNegative(MaterialDialog dialog) {
							android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
									mainActivityPep
											.getSystemService(Activity.CLIPBOARD_SERVICE);
							android.content.ClipData clip = android.content.ClipData.newPlainText("text label",
									clickedString);
							clipboard.setPrimaryClip(clip);
						}
					}).build().show();
			View clickedView = noteTmp.isChecklist() ? toggleChecklistView : content;
			clickedView.clearFocus();
			KeyboardUtils.hideKeyboard(clickedView);
			new Handler().post(new Runnable() {
				@Override
				public void run() {
					View clickedView = noteTmp.isChecklist() ? toggleChecklistView : content;
					KeyboardUtils.hideKeyboard(clickedView);
				}
			});
		}
	};


	@SuppressLint("NewApi")
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int x = (int) event.getX();
		int y = (int) event.getY();

		switch (event.getAction()) {

			case MotionEvent.ACTION_DOWN:
				Log.v(Constants.TAG, "MotionEvent.ACTION_DOWN");
				int w;

				Point displaySize = Display.getUsableSize(mainActivityPep);
				w = displaySize.x;

				if (x < Constants.SWIPE_MARGIN || x > w - Constants.SWIPE_MARGIN) {
					swiping = true;
					startSwipeX = x;
				}

				break;

			case MotionEvent.ACTION_UP:
				Log.v(Constants.TAG, "MotionEvent.ACTION_UP");
				if (swiping)
					swiping = false;
				break;

			case MotionEvent.ACTION_MOVE:
				if (swiping) {
					Log.v(Constants.TAG, "MotionEvent.ACTION_MOVE at position " + x + ", " + y);
					if (Math.abs(x - startSwipeX) > Constants.SWIPE_OFFSET) {
						swiping = false;
						FragmentTransaction transaction = mainActivityPep.getSupportFragmentManager().beginTransaction();
						mainActivityPep.animateTransition(transaction, mainActivityPep.TRANSITION_VERTICAL);
						DetailFragment mDetailFragment = new DetailFragment();
						Bundle b = new Bundle();
						b.putParcelable(Constants.INTENT_NOTE, new Note());
						mDetailFragment.setArguments(b);
						transaction.replace(R.id.fragment_container, mDetailFragment,
								mainActivityPep.FRAGMENT_DETAIL_TAG).addToBackStack(mainActivityPep
								.FRAGMENT_DETAIL_TAG).commit();
					}
				}
				break;
		}

		return true;
	}


	@Override
	public void onGlobalLayout() {
		int screenHeight = Display.getUsableSize(mainActivityPep).y;
		int navBarOffset = Display.orientationLandscape(mainActivityPep) ? 0 : DensityUtil.pxToDp(Display
				.getNavigationBarHeight(mainActivityPep.getWindow().getDecorView()), mainActivityPep);
		int heightDiff = screenHeight - Display.getVisibleSize(mainActivityPep).y + navBarOffset;
		boolean keyboardVisible = heightDiff > 150;
		if (keyboardVisible && keyboardPlaceholder == null) {
			shrinkLayouts(heightDiff);
		} else if (!keyboardVisible && keyboardPlaceholder != null) {
			restoreLayouts();
		}
	}


	private void shrinkLayouts(int heightDiff) {
		detailWrapperView.removeView(timestampsView);
		keyboardPlaceholder = new View(mainActivityPep);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Display.orientationLandscape(mainActivityPep))) {
				root.addView(keyboardPlaceholder, LinearLayout.LayoutParams.MATCH_PARENT, heightDiff);
			}
		}
	}


	private void restoreLayouts() {
		if (root != null) {
			ViewGroup wrapper = (ViewGroup) root.findViewById(R.id.detail_wrapper);
			if (root.indexOfChild(keyboardPlaceholder) != -1) {
				root.removeView(keyboardPlaceholder);
			}
			keyboardPlaceholder = null;
			if (wrapper.indexOfChild(timestampsView) == -1) {
				wrapper.addView(timestampsView);
			}
		}
	}


	@Override
	public void onAttachingFileErrorOccurred(Attachment mAttachment) {
		mainActivityPep.showMessage(R.string.error_saving_attachments, ONStyle.ALERT);
		if (noteTmp.getAttachmentsList().contains(mAttachment)) {
			removeAttachment(mAttachment);
			mAttachmentAdapter.notifyDataSetChanged();
			mGridView.autoresize();
		}
	}


	private void addAttachment(Attachment attachment) {
		noteTmp.addAttachment(attachment);
//		mAttachmentAdapter.getAttachmentsList().add(attachment);
	}


	private void removeAttachment(Attachment mAttachment) {
		noteTmp.removeAttachment(mAttachment);
//		mAttachmentAdapter.getAttachmentsList().remove(mAttachment);
	}


	private void removeAttachment(int position) {
		noteTmp.removeAttachment(noteTmp.getAttachmentsList().get(position));
//		mAttachmentAdapter.getAttachmentsList().remove(position);
	}


	@Override
	public void onAttachingFileFinished(Attachment mAttachment) {
		addAttachment(mAttachment);
		mAttachmentAdapter.notifyDataSetChanged();
		mGridView.autoresize();
	}


	@Override
	public void onReminderPicked(long reminder) {
		noteTmp.setAlarm(reminder);
		if (mFragment.isAdded()) {
			reminderIcon.setImageResource(R.drawable.ic_alarm_grey600_18dp);
			datetime.setText(DateHelper.getNoteReminderText(reminder));
		}
	}

	@Override
	public void onRecurrenceReminderPicked(String recurrenceRule) {
		noteTmp.setRecurrenceRule(recurrenceRule);
		if (!TextUtils.isEmpty(recurrenceRule)) {
			Log.d(Constants.TAG, "Recurrent reminder set: " + recurrenceRule);
			datetime.setText(DateHelper.getNoteRecurrentReminderText(Long.parseLong(noteTmp
					.getAlarm()), recurrenceRule));
		}
	}


	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		scrollContent();
	}


	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// Nothing to do
	}


	@Override
	public void afterTextChanged(Editable s) {
		// Nothing to do
	}


	@Override
	public void onCheckListChanged() {
		scrollContent();
	}


	private void scrollContent() {
		if (noteTmp.isChecklist()) {
			if (mChecklistManager.getCount() > contentLineCounter) {
				scrollView.scrollBy(0, 60);
			}
			contentLineCounter = mChecklistManager.getCount();
		} else {
			if (content.getLineCount() > contentLineCounter) {
				scrollView.scrollBy(0, 60);
			}
			contentLineCounter = content.getLineCount();
		}
	}


	/**
	 * Add previously created tags to content
	 */
	private void addTags() {
		contentCursorPosition = getCursorIndex();

		// Retrieves all available categories
		final List<Tag> tags = TagsHelper.getAllTags(mainActivityPep);

		// If there is no tag a message will be shown
		if (tags.size() == 0) {
			mainActivityPep.showMessage(R.string.no_tags_created, ONStyle.WARN);
			return;
		}

		final Note currentNote = new Note();
		currentNote.setTitle(getNoteTitle());
		currentNote.setContent(getNoteContent());
		Integer[] preselectedTags = TagsHelper.getPreselectedTagsArray(currentNote, tags);

		// Dialog and events creation
		MaterialDialog dialog = new MaterialDialog.Builder(mainActivityPep)
				.title(R.string.select_tags)
				.positiveText(R.string.ok)
				.items(TagsHelper.getTagsArray(tags))
				.itemsCallbackMultiChoice(preselectedTags, (dialog1, which, text) -> {
					dialog1.dismiss();
					tagNote(tags, which, currentNote);
					return false;
				}).build();
		dialog.show();
	}


	private void tagNote(List<Tag> tags, Integer[] selectedTags, Note note) {
		Pair<String, List<Tag>> taggingResult = TagsHelper.addTagToNote(tags, selectedTags, note);

		StringBuilder sb;
		if (noteTmp.isChecklist()) {
			CheckListViewItem mCheckListViewItem = mChecklistManager.getFocusedItemView();
			if (mCheckListViewItem != null) {
				sb = new StringBuilder(mCheckListViewItem.getText());
				sb.insert(contentCursorPosition, " " + taggingResult.first + " ");
				mCheckListViewItem.setText(sb.toString());
				mCheckListViewItem.getEditText().setSelection(contentCursorPosition + taggingResult.first.length()
						+ 1);
			} else {
				title.append(" " + taggingResult.first);
			}
		} else {
			sb = new StringBuilder(getNoteContent());
			if (content.hasFocus()) {
				sb.insert(contentCursorPosition, " " + taggingResult.first + " ");
				content.setText(sb.toString());
				content.setSelection(contentCursorPosition + taggingResult.first.length() + 1);
			} else {
				if (getNoteContent().trim().length() > 0) {
					sb.append(System.getProperty("line.separator"))
							.append(System.getProperty("line.separator"));
				}
				sb.append(taggingResult.first);
				content.setText(sb.toString());
			}
		}

		// Removes unchecked tags
		if (taggingResult.second.size() > 0) {
			if (noteTmp.isChecklist()) {
				toggleChecklist2(true, true);
			}
			Pair<String, String> titleAndContent = TagsHelper.removeTag(getNoteTitle(), getNoteContent(),
					taggingResult.second);
			title.setText(titleAndContent.first);
			content.setText(titleAndContent.second);
			if (noteTmp.isChecklist()) {
				toggleChecklist2();
			}
		}
	}


	private int getCursorIndex() {
		if (!noteTmp.isChecklist()) {
			return content.getSelectionStart();
		} else {
			CheckListViewItem mCheckListViewItem = mChecklistManager.getFocusedItemView();
			if (mCheckListViewItem != null) {
				return mCheckListViewItem.getEditText().getSelectionStart();
			} else {
				return 0;
			}
		}
	}


	/**
	 * Used to check currently opened note from activity to avoid openind multiple times the same one
	 */
	public Note getCurrentNote() {
		return note;
	}


	private boolean isNoteLocationValid() {
		return noteTmp.getLatitude() != null
				&& noteTmp.getLatitude() != 0
				&& noteTmp.getLongitude() != null
				&& noteTmp.getLongitude() != 0;
	}

	@Override
	public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
		int i_year = year;
		int i_month = monthOfYear+1;
		int i_day = dayOfMonth ;

			persianDate = new PersianDate(i_year, i_month, i_day);
			civilDate = DateConverter.persianToCivil(persianDate);

			int civil_year = civilDate.getYear();
			int civil_month = civilDate.getMonth()-1;
			int civil_day = civilDate.getDayOfMonth();

			mCalendar.set(Calendar.YEAR, civil_year);
			mCalendar.set(Calendar.MONTH, civil_month);
			mCalendar.set(Calendar.DAY_OF_MONTH, civil_day);

			int a = mCalendar.get(Calendar.YEAR);
			int b = mCalendar.get(Calendar.MONTH)+1;
			int c = mCalendar.get(Calendar.DAY_OF_MONTH);

			civilDateintro = new CivilDate(a,b,c);
			persianDateintro = DateConverter.civilToPersian(civilDateintro);

			Log.e("[YEAR]", "" + civilDate.getYear());
			Log.e("[MONTH]",""+civilDate.getMonth());
			Log.e("[DAY]",""+civilDate.getDayOfMonth());
			Log.e("[COMPLITE]",""+ DateAndTimeUtil.toStringReadableDate(mCalendar));


			PersianCalendar now = new PersianCalendar();
			TimePickerDialog tpd = TimePickerDialog.newInstance(
					DetailFragment.this,
					now.get(PersianCalendar.HOUR_OF_DAY),
					now.get(PersianCalendar.MINUTE),
					true
			);
			tpd.setThemeDark(true);
			tpd.show(getActivity().getFragmentManager(), TIMEPICKER);



	}

	@Override
	public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
		mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
		mCalendar.set(Calendar.MINUTE, minute);

		datetime.setText("در تاریخ : "+utils.shape(utils.dateToString(persianDateintro))+"\n"
				+"در ساعت : "+DateAndTimeUtil.toStringReadableTime(mCalendar, getActivity()));
	}


	/**
	 * Manages clicks on attachment dialog
	 */
	@SuppressLint("InlinedApi")
	private class AttachmentOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				// Photo from camera
				case R.id.camera:
					takePhoto();
					attachmentDialog.dismiss();
					break;
				case R.id.recording:
					if (!isRecording) {
						isRecording = true;
						android.widget.TextView mTextView = (android.widget.TextView) v;
						mTextView.setText(getString(R.string.stop));
						mTextView.setTextColor(Color.parseColor("#ff0000"));
						startRecording();
					} else {
						isRecording = false;
						stopRecording();
						Attachment attachment = new Attachment(Uri.parse(recordName), Constants.MIME_TYPE_AUDIO);
						attachment.setLength(audioRecordingTime);
						addAttachment(attachment);
						mAttachmentAdapter.notifyDataSetChanged();
						mGridView.autoresize();
						attachmentDialog.dismiss();
					}
					break;
				case R.id.video:
					takeVideo();
					attachmentDialog.dismiss();
					break;
				case R.id.files:
					Intent filesIntent;
					filesIntent = new Intent(Intent.ACTION_GET_CONTENT);
					filesIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
					filesIntent.addCategory(Intent.CATEGORY_OPENABLE);
					filesIntent.setType("*/*");
					startActivityForResult(filesIntent, FILES);
					attachmentDialog.dismiss();
					break;
				case R.id.sketch:
					takeSketch(null);
					attachmentDialog.dismiss();
					break;


			}
		}
	}


	public void onEventMainThread(PushbulletReplyEvent pushbulletReplyEvent) {
		content.setText(getNoteContent() + System.getProperty("line.separator") + pushbulletReplyEvent.message);
	}
}



