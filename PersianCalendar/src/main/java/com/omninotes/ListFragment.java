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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.*;
import android.support.annotation.RequiresApi;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.MaterialDialog;
import com.byagowi.persiancalendar.R;
import com.neopixl.pixlui.components.textview.TextView;
import com.nhaarman.listviewanimations.itemmanipulation.DynamicListView;
import com.pnikosis.materialishprogress.ProgressWheel;
import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import com.omninotes.async.bus.CategoriesUpdatedEvent;
import com.omninotes.async.bus.NavigationUpdatedNavDrawerClosedEvent;
import com.omninotes.async.bus.NotesLoadedEvent;
import com.omninotes.async.bus.NotesMergeEvent;
import com.omninotes.async.notes.*;
import com.omninotes.db.DbHelper;
import com.omninotes.helpers.AnalyticsHelper;
import com.omninotes.helpers.NotesHelper;
import com.omninotes.models.*;
import com.omninotes.models.adapters.NavDrawerCategoryAdapter;
import com.omninotes.models.adapters.NoteAdapter;
import com.omninotes.models.holders.NoteViewHolder;
import com.omninotes.models.listeners.OnViewTouchedListener;
import com.omninotes.models.views.Fab;
import com.omninotes.models.views.InterceptorLinearLayout;
import com.omninotes.utils.*;
import com.omninotes.utils.Display;
import it.feio.android.pixlui.links.UrlCompleter;
import org.apache.commons.lang.ObjectUtils;

import java.util.*;

import static android.support.v4.view.ViewCompat.animate;


public class ListFragment extends BaseFragment implements OnViewTouchedListener, UndoBarController.UndoListener {

    private static final int REQUEST_CODE_CATEGORY = 1;
    private static final int REQUEST_CODE_CATEGORY_NOTES = 2;

    @Bind(R.id.list_root) InterceptorLinearLayout listRoot;
    @Bind(R.id.list) DynamicListView list;
    @Bind(R.id.search_layout) View searchLayout;
    @Bind(R.id.search_query) android.widget.TextView searchQueryView;
    @Bind(R.id.search_cancel) ImageView searchCancel;
    @Bind(R.id.empty_list) TextView empyListItem;
    @Bind(R.id.expanded_image) ImageView expandedImageView;
    @Bind(R.id.fab)  View fabView;
    @Bind(R.id.undobar) View undoBarView;
    @Bind(R.id.progress_wheel) ProgressWheel progress_wheel;

    NoteViewHolder noteViewHolder;

    private List<Note> selectedNotes = new ArrayList<>();
    private List<Note> modifiedNotes = new ArrayList<>();
    private SearchView searchView;
    private MenuItem searchMenuItem;
    private Menu menu;
    private AnimationDrawable jinglesAnimation;
    private int listViewPosition;
    private int listViewPositionOffset = 16;
    private boolean sendToArchive;
    private SharedPreferences prefs;
    private static SharedPreferences staticPrefs;
    private ListFragment mFragment;
    private android.support.v7.view.ActionMode actionMode;
    private boolean keepActionMode = false;
    private TextView listFooter;

    // Undo archive/trash
    private boolean undoTrash = false;
    private boolean undoArchive = false;
    private boolean undoCategorize = false;
    private Category undoCategorizeCategory = null;
    private SparseArray<Note> undoNotesList = new SparseArray<>();
    // Used to remember removed categories from notes
    private Map<Note, Category> undoCategoryMap = new HashMap<>();
    // Used to remember archived state from notes
    private Map<Note, Boolean> undoArchivedMap = new HashMap<>();

    // Search variables
    private String searchQuery;
    private String searchQueryInstant;
    private String searchTags;
    private boolean goBackOnToggleSearchLabel = false;
    private boolean searchLabelActive = false;

    private NoteAdapter listAdapter;
    private UndoBarController ubc;
    private Fab fab;
    private MainActivityPep mainActivity;

    boolean newNoteFromMainActivity = false;
    boolean newNoteFromMainActivityList = false;
    boolean newNoteFromMainActivityCamera = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragment = this;
        setHasOptionsMenu(true);
        setRetainInstance(false);
    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this, 1);
    }


    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("listViewPosition")) {
                listViewPosition = savedInstanceState.getInt("listViewPosition");
                listViewPositionOffset = savedInstanceState.getInt("listViewPositionOffset");
                searchQuery = savedInstanceState.getString("searchQuery");
                searchTags = savedInstanceState.getString("searchTags");
            }
            keepActionMode = false;
        }
        View view = inflater.inflate(R.layout.fragment_list_pep, container, false);
        ButterKnife.bind(this, view);
        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity = (MainActivityPep) getActivity();
        prefs = mainActivity.prefs;

        try{
            newNoteFromMainActivity = getActivity().getIntent().getExtras().getBoolean("NewNoteFromMainActivity");
            newNoteFromMainActivityList = getActivity().getIntent().getExtras().getBoolean("NewNoteFromMainActivityList");
            newNoteFromMainActivityCamera = getActivity().getIntent().getExtras().getBoolean("NewNoteFromMainActivityCamera");
        }catch (Exception e){
        }
        if(newNoteFromMainActivity == true){
            Log.e("newNoteFromMainActivity","");
            View v = mainActivity.findViewById(R.id.fab_note);
            editNote(new Note(), v);
            getActivity().getIntent().putExtra("NewNoteFromMainActivity", false);
        }
        if(newNoteFromMainActivityList == true){
            View v2 = mainActivity.findViewById(R.id.fab_checklist);
            Note note = new Note();
            note.setChecklist(true);
            editNote(note, v2);
            getActivity().getIntent().putExtra("NewNoteFromMainActivityList", false);
        }
        if(newNoteFromMainActivityCamera == true){
            View v3 = mainActivity.findViewById(R.id.fab_camera);
            Intent i = mainActivity.getIntent();
            i.setAction(Constants.ACTION_TAKE_PHOTO);
            mainActivity.setIntent(i);
            editNote(new Note(), v3);
            getActivity().getIntent().putExtra("NewNoteFromMainActivityCamera", false);
        }

        if (savedInstanceState != null) {
            mainActivity.navigationTmp = savedInstanceState.getString("navigationTmp");
        }
        initEasterEgg();
        initListView();
        ubc = new UndoBarController(undoBarView, this);
    }
    private void initFab() {
        fab = new Fab(fabView, list, prefs.getBoolean(Constants.PREF_FAB_EXPANSION_BEHAVIOR, false));
        fab.setOnFabItemClickedListener(id -> {
            View v = mainActivity.findViewById(id);
            switch (id) {
                case R.id.fab_expand_menu_button:
                    editNote(new Note(), v);
                    break;
                case R.id.fab_note:
                    editNote(new Note(), v);
                    break;
                case R.id.fab_camera:
                    Intent i = mainActivity.getIntent();
                    i.setAction(Constants.ACTION_TAKE_PHOTO);
                    mainActivity.setIntent(i);
                    editNote(new Note(), v);
                    break;
                case R.id.fab_checklist:
                    Note note = new Note();
                    note.setChecklist(true);
                    editNote(note, v);
                    break;
            }
        });
        fab.setOverlay(R.color.white_overlay);
    }


    public void switchToEditNote(){
        View v = mainActivity.findViewById(R.id.fab_note);
        editNote(new Note(), v);
    }
    boolean closeFab() {
        if (fab.isExpanded()) {
            fab.performToggle();
            return true;
        }
        return false;
    }


    /**
     * Activity title initialization based on navigation
     */
    private void initTitle() {
        String[] navigationList = getResources().getStringArray(R.array.navigation_list);
        String[] navigationListCodes = getResources().getStringArray(R.array.navigation_list_codes);
        String navigation = mainActivity.navigationTmp != null ? mainActivity.navigationTmp : prefs.getString
                (Constants.PREF_NAVIGATION, navigationListCodes[0]);
        int index = Arrays.asList(navigationListCodes).indexOf(navigation);
        String title;
        // If is a traditional navigation item
        if (index >= 0 && index < navigationListCodes.length) {
            title = navigationList[index];
        } else {
            Category category = DbHelper.getInstance().getCategory(Long.parseLong(navigation));
            title = category != null ? category.getName() : "";
        }
        title = title == null ? getString(R.string.title_activity_list) : title;
        mainActivity.setActionBarTitle(title);
    }


    /**
     * Starts a little animation on Mr.Jingles!
     */
    private void initEasterEgg() {
        empyListItem.setOnClickListener(v -> {
            if (jinglesAnimation == null) {
                jinglesAnimation = (AnimationDrawable) empyListItem.getCompoundDrawables()[1];
                empyListItem.post(() -> {
                    if (jinglesAnimation != null) jinglesAnimation.start();
                });
            } else {
                stopJingles();
            }
        });
    }

    @SuppressWarnings("ResourceType")
    private void stopJingles() {
        if (jinglesAnimation != null) {
            jinglesAnimation.stop();
            jinglesAnimation = null;
            empyListItem.setCompoundDrawablesWithIntrinsicBounds(0, R.animator.jingles_animation, 0, 0);

        }
    }


    @Override
    public void onPause() {
        super.onPause();
        searchQueryInstant = searchQuery;
        stopJingles();
        Crouton.cancelAllCroutons();
        if (!keepActionMode) {
            commitPending();
            list.clearChoices();
            if (getActionMode() != null) {
                getActionMode().finish();
            }
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        refreshListScrollPosition();
        outState.putInt("listViewPosition", listViewPosition);
        outState.putInt("listViewPositionOffset", listViewPositionOffset);
        outState.putString("searchQuery", searchQuery);
        outState.putString("searchTags", searchTags);
    }


    private void refreshListScrollPosition() {
        if (list != null) {
            listViewPosition = list.getFirstVisiblePosition();
            View v = list.getChildAt(0);
            listViewPositionOffset = (v == null) ? (int) getResources().getDimension(R.dimen.vertical_margin) : v.getTop();
        }
    }


    @SuppressWarnings("static-access")
    @Override
    public void onResume() {
        super.onResume();

        initNotesList(mainActivity.getIntent());

        initFab();

        initTitle();

        // Restores again DefaultSharedPreferences too reload in case of data erased from Settings
        prefs = mainActivity.getSharedPreferences(Constants.PREFS_NAME, mainActivity.MODE_MULTI_PROCESS);

//        // Menu is invalidated to start again instructions tour if requested
//        if (!prefs.getBoolean(Constants.PREF_TOUR_PREFIX + "list", false)) {
//            mainActivity.supportInvalidateOptionsMenu();
//        }
    }


    private final class ModeCallback implements android.support.v7.view.ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the menu for the CAB
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_list_pep, menu);
            actionMode = mode;
            fab.setAllowed(false);
            fab.hideFab();
            return true;
        }


        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Here you can make any necessary updates to the activity when
            // the CAB is removed. By default, selected items are
            // deselected/unchecked.
            for (int i = 0; i < listAdapter.getSelectedItems().size(); i++) {
                int key = listAdapter.getSelectedItems().keyAt(i);
                View v = list.getChildAt(key - list.getFirstVisiblePosition());
                if (listAdapter.getCount() > key && listAdapter.getItem(key) != null && v != null) {
                    listAdapter.restoreDrawable(listAdapter.getItem(key), v.findViewById(R.id.card_layout));
                }
            }

            // Backups modified notes in another structure to perform post-elaborations
            modifiedNotes = new ArrayList<>(getSelectedNotes());

            // Clears data structures
            selectedNotes.clear();
            listAdapter.clearSelectedItems();
            list.clearChoices();

            fab.setAllowed(true);
            if (undoNotesList.size() == 0) {
                fab.showFab();
            }

            actionMode = null;
        }


        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            prepareActionModeMenu();
            return true;
        }


        @Override
        public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
            Integer[] protectedActions = {R.id.menu_select_all, R.id.menu_merge};
            if (!Arrays.asList(protectedActions).contains(item.getItemId())) {
                mainActivity.requestPassword(mainActivity, getSelectedNotes(),
                        passwordConfirmed -> {
                            if (passwordConfirmed) {
                                performAction(item, mode);
                            }
                        });
            } else {
                performAction(item, mode);
            }
            return true;
        }
    }


    public void finishActionMode() {
        if (getActionMode() != null) {
            getActionMode().finish();
        }
    }


    /**
     * Manage check/uncheck of notes in list during multiple selection phase
     */
    private void toggleListViewItem(View view, int position) {
        Note note = listAdapter.getItem(position);
        LinearLayout cardLayout = (LinearLayout) view.findViewById(R.id.card_layout);
        if (!getSelectedNotes().contains(note)) {
            getSelectedNotes().add(note);
            listAdapter.addSelectedItem(position);
            cardLayout.setBackgroundColor(getResources().getColor(R.color.list_bg_selected));
        } else {
            getSelectedNotes().remove(note);
            listAdapter.removeSelectedItem(position);
            listAdapter.restoreDrawable(note, cardLayout);
        }
        prepareActionModeMenu();

        // Close CAB if no items are selected
        if (getSelectedNotes().size() == 0) {
            finishActionMode();
        }

    }


    /**
     * Notes list initialization. Data, actions and callback are defined here.
     */
    private void initListView() {
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        list.setItemsCanFocus(false);

        // If device runs KitKat a footer is added to list to avoid
        // navigation bar transparency covering items
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int navBarHeight = Display.getNavigationBarHeightKitkat(mainActivity);
            listFooter = new TextView(mainActivity.getApplicationContext());
            listFooter.setHeight(navBarHeight);
            // To avoid useless events on footer
            listFooter.setOnClickListener(null);
            list.addFooterView(listFooter);
        }

        // Note long click to start CAB mode
        list.setOnItemLongClickListener((arg0, view, position, arg3) -> {
            if (view.equals(listFooter)) return true;
            if (getActionMode() != null) {
                return false;
            }
            // Start the CAB using the ActionMode.Callback defined above
            mainActivity.startSupportActionMode(new ModeCallback());
            toggleListViewItem(view, position);
            setCabTitle();
            return true;
        });

        // Note single click listener managed by the activity itself
        list.setOnItemClickListener((arg0, view, position, arg3) -> {
            if (view.equals(listFooter)) return;
            if (getActionMode() == null) {
                editNote(listAdapter.getItem(position), view);
                return;
            }
            // If in CAB mode
            toggleListViewItem(view, position);
            setCabTitle();
        });

        listRoot.setOnViewTouchedListener(this);
    }


    /**
     * Retrieves from the single listview note item the element to be zoomed when opening a note
     */
    private ImageView getZoomListItemView(View view, Note note) {
        if (expandedImageView != null) {
            View targetView = null;
            if (note.getAttachmentsList().size() > 0) {
                targetView = view.findViewById(R.id.attachmentThumbnail);
            }
            if (targetView == null && note.getCategory() != null) {
                targetView = view.findViewById(R.id.category_marker);
            }
            if (targetView == null) {
                targetView = new ImageView(mainActivity);
                targetView.setBackgroundColor(Color.WHITE);
            }
            targetView.setDrawingCacheEnabled(true);
            targetView.buildDrawingCache();
            Bitmap bmp = targetView.getDrawingCache();
            expandedImageView.setBackgroundColor(BitmapHelper.getDominantColor(bmp));
        }
        return expandedImageView;
    }


    /**
     * Listener that fires note opening once the zooming animation is finished
     */
    private AnimatorListenerAdapter buildAnimatorListenerAdapter(final Note note) {
        return new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                editNote2(note);
            }
        };
    }


    @Override
    public void onViewTouchOccurred(MotionEvent ev) {
        commitPending();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_list_pep, menu);
        super.onCreateOptionsMenu(menu, inflater);
        this.menu = menu;
        initSearchView(menu);
    }


    private void initSortingSubmenu() {
        final String[] arrayDb = getResources().getStringArray(R.array.sortable_columns);
        final String[] arrayDialog = getResources().getStringArray(R.array.sortable_columns_human_readable);
        int selected = Arrays.asList(arrayDb).indexOf(prefs.getString(Constants.PREF_SORTING_COLUMN, arrayDb[0]));

        SubMenu sortMenu = this.menu.findItem(R.id.menu_sort).getSubMenu();
        for (int i = 0; i < arrayDialog.length; i++) {
            if (sortMenu.findItem(i) == null) {
                sortMenu.add(Constants.MENU_SORT_GROUP_ID, i, i, arrayDialog[i]);
            }
            if (i == selected) sortMenu.getItem(i).setChecked(true);
        }
        sortMenu.setGroupCheckable(Constants.MENU_SORT_GROUP_ID, true, true);
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        setActionItemsVisibility(menu, false);
    }


    private void prepareActionModeMenu() {
        Menu menu = getActionMode().getMenu();
        int navigation = Navigation.getNavigation();
        boolean showArchive = navigation == Navigation.NOTES || navigation == Navigation.REMINDERS || navigation ==
                Navigation.UNCATEGORIZED || navigation == Navigation.CATEGORY;
        boolean showUnarchive = navigation == Navigation.ARCHIVE || navigation == Navigation.UNCATEGORIZED ||
                navigation == Navigation.CATEGORY;

        if (navigation == Navigation.TRASH) {
            menu.findItem(R.id.menu_untrash).setVisible(true);
            menu.findItem(R.id.menu_delete).setVisible(true);
        } else {
            if (getSelectedCount() == 1) {
                menu.findItem(R.id.menu_share).setVisible(true);
                menu.findItem(R.id.menu_merge).setVisible(false);
                menu.findItem(R.id.menu_archive)
                        .setVisible(showArchive && !getSelectedNotes().get(0).isArchived
                                ());
                menu.findItem(R.id.menu_unarchive)
                        .setVisible(showUnarchive && getSelectedNotes().get(0).isArchived
                                ());
            } else {
                menu.findItem(R.id.menu_share).setVisible(false);
                menu.findItem(R.id.menu_merge).setVisible(true);
                menu.findItem(R.id.menu_archive).setVisible(showArchive);
                menu.findItem(R.id.menu_unarchive).setVisible(showUnarchive);

            }
            menu.findItem(R.id.menu_category).setVisible(true);
            menu.findItem(R.id.menu_tags).setVisible(true);
            menu.findItem(R.id.menu_trash).setVisible(true);
        }
        menu.findItem(R.id.menu_select_all).setVisible(true);

        setCabTitle();
    }


    private int getSelectedCount() {
        return getSelectedNotes().size();
    }


    private void setCabTitle() {
        if (getActionMode() != null) {
            int title = getSelectedCount();
            getActionMode().setTitle(String.valueOf(title));
        }
    }


    /**
     * SearchView initialization. It's a little complex because it's not using SearchManager but is implementing on its
     * own.
     */
    @SuppressLint("NewApi")
    private void initSearchView(final Menu menu) {

        // Prevents some mysterious NullPointer on app fast-switching
        if (mainActivity == null) return;

        // Save item as class attribute to make it collapse on drawer opening
        searchMenuItem = menu.findItem(R.id.menu_search);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) mainActivity.getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_search));
        searchView.setSearchableInfo(searchManager.getSearchableInfo(mainActivity.getComponentName()));
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        // Expands the widget hiding other actionbar icons
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> setActionItemsVisibility(menu, hasFocus));

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {

            boolean searchPerformed = false;


            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Reinitialize notes list to all notes when search is collapsed
                searchQuery = null;
                if (searchLayout.getVisibility() == View.VISIBLE) {
                    toggleSearchLabel(false);
                }
                mainActivity.getIntent().setAction(Intent.ACTION_MAIN);
                initNotesList(mainActivity.getIntent());
                mainActivity.supportInvalidateOptionsMenu();
                return true;
            }


            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                searchView.setOnQueryTextListener(new OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String arg0) {
                        return prefs.getBoolean("settings_instant_search", false);
                    }


                    @Override
                    public boolean onQueryTextChange(String pattern) {
                        if (prefs.getBoolean("settings_instant_search", false) && searchLayout != null &&
                                searchPerformed && mFragment.isAdded()) {
                            searchTags = null;
                            searchQuery = pattern;
                            new NoteLoaderTask().execute("getNotesByPattern", pattern);
                            return true;
                        } else {
                            searchPerformed = true;
                            return false;
                        }
                    }
                });
                return true;
            }
        });
    }


    private void setActionItemsVisibility(Menu menu, boolean searchViewHasFocus) {

        boolean drawerOpen = mainActivity.getDrawerLayout() != null && mainActivity.getDrawerLayout().isDrawerOpen
                (GravityCompat.START);
        boolean expandedView = prefs.getBoolean(Constants.PREF_EXPANDED_VIEW, true);

        int navigation = Navigation.getNavigation();
        boolean navigationReminders = navigation == Navigation.REMINDERS;
        boolean navigationArchive = navigation == Navigation.ARCHIVE;
        boolean navigationTrash = navigation == Navigation.TRASH;
        boolean navigationCategory = navigation == Navigation.CATEGORY;

        boolean filterPastReminders = prefs.getBoolean(Constants.PREF_FILTER_PAST_REMINDERS, true);
        boolean filterArchivedInCategory = navigationCategory && prefs.getBoolean(Constants
                .PREF_FILTER_ARCHIVED_IN_CATEGORIES + Navigation.getCategory(), false);

        if (!navigationReminders && !navigationArchive && !navigationTrash) {
            fab.setAllowed(true);
            if (!drawerOpen) {
                fab.showFab();
            }
        } else {
            fab.setAllowed(false);
            fab.hideFab();
        }
        menu.findItem(R.id.menu_search).setVisible(!drawerOpen);
        menu.findItem(R.id.menu_filter).setVisible(!drawerOpen && !filterPastReminders && navigationReminders &&
                !searchViewHasFocus);
        menu.findItem(R.id.menu_filter_remove).setVisible(!drawerOpen && filterPastReminders && navigationReminders
                && !searchViewHasFocus);
        menu.findItem(R.id.menu_filter_category).setVisible(!drawerOpen && !filterArchivedInCategory &&
                navigationCategory && !searchViewHasFocus);
        menu.findItem(R.id.menu_filter_category_remove).setVisible(!drawerOpen && filterArchivedInCategory &&
                navigationCategory && !searchViewHasFocus);
        menu.findItem(R.id.menu_sort).setVisible(!drawerOpen && !navigationReminders && !searchViewHasFocus);
        menu.findItem(R.id.menu_expanded_view).setVisible(!drawerOpen && !expandedView && !searchViewHasFocus);
        menu.findItem(R.id.menu_contracted_view).setVisible(!drawerOpen && expandedView && !searchViewHasFocus);
        menu.findItem(R.id.menu_empty_trash).setVisible(!drawerOpen && navigationTrash);
        menu.findItem(R.id.menu_tags).setVisible(searchViewHasFocus);
    }


    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Integer[] protectedActions = {R.id.menu_empty_trash};
        if (Arrays.asList(protectedActions).contains(item.getItemId())) {
            mainActivity.requestPassword(mainActivity, getSelectedNotes(), passwordConfirmed -> {
                if (passwordConfirmed) {
                    performAction(item, null);
                }
            });
        } else {
            performAction(item, null);
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Performs one of the ActionBar button's actions after checked notes protection
     */
    public boolean performAction(MenuItem item, ActionMode actionMode) {
        if (actionMode == null) {
            switch (item.getItemId()) {
                case android.R.id.home:
                    if (mainActivity.getDrawerLayout().isDrawerOpen(GravityCompat.START)) {
                        mainActivity.getDrawerLayout().closeDrawer(GravityCompat.START);
                    } else {
                        mainActivity.getDrawerLayout().openDrawer(GravityCompat.START);
                    }
                    break;
                case R.id.menu_filter:
                    filterReminders(true);
                    break;
                case R.id.menu_filter_remove:
                    filterReminders(false);
                    break;
                case R.id.menu_filter_category:
                    filterCategoryArchived(true);
                    break;
                case R.id.menu_filter_category_remove:
                    filterCategoryArchived(false);
                    break;
                case R.id.menu_tags:
                    filterByTags();
                    break;
                case R.id.menu_sort:
                    initSortingSubmenu();
                    break;
                case R.id.menu_expanded_view:
                    switchNotesView();
                    break;
                case R.id.menu_contracted_view:
                    switchNotesView();
                    break;
                case R.id.menu_empty_trash:
                    emptyTrash();
                    break;
            }
        } else {
            switch (item.getItemId()) {
                case R.id.menu_category:
                    categorizeNotes();
                    break;
                case R.id.menu_tags:
                    tagNotes();
                    break;
                case R.id.menu_share:
                    share();
                    break;
                case R.id.menu_merge:
                    merge();
                    break;
                case R.id.menu_archive:
                    archiveNotes(true);
                    break;
                case R.id.menu_unarchive:
                    archiveNotes(false);
                    break;
                case R.id.menu_trash:
                    trashNotes(true);
                    break;
                case R.id.menu_untrash:
                    trashNotes(false);
                    break;
                case R.id.menu_delete:
                    deleteNotes();
                    break;
                case R.id.menu_select_all:
                    selectAllNotes();
                    break;
//                case R.id.menu_synchronize:
//                    synchronizeSelectedNotes();
//                    break;
            }
        }

        AnalyticsHelper.trackActionFromResourceId(getActivity(), item.getItemId());

        checkSortActionPerformed(item);

        return super.onOptionsItemSelected(item);
    }


    private void switchNotesView() {
        boolean expandedView = prefs.getBoolean(Constants.PREF_EXPANDED_VIEW, true);
        prefs.edit().putBoolean(Constants.PREF_EXPANDED_VIEW, !expandedView).commit();
        // Change list view
        initNotesList(mainActivity.getIntent());
        // Called to switch menu voices
        mainActivity.supportInvalidateOptionsMenu();
    }


    void editNote3(final Note note, final View view) {
        if (note.isLocked() && !prefs.getBoolean("settings_password_access", false)) {
            BaseActivity.requestPassword(mainActivity, passwordConfirmed -> {
                if (passwordConfirmed) {
                    note.setPasswordChecked(true);
                    AnimationsHelper.zoomListItem(mainActivity, view, getZoomListItemView(view, note),
                            listRoot, buildAnimatorListenerAdapter(note));
                }
            });
        } else {
            AnimationsHelper.zoomListItem(mainActivity, view, getZoomListItemView(view, note),
                    listRoot, buildAnimatorListenerAdapter(note));
        }
    }

    void editNote(final Note note, final View view) {
        if (note.isLocked() && !prefs.getBoolean("settings_password_access", false)) {
            BaseActivity.requestPassword(mainActivity, passwordConfirmed -> {
                if (passwordConfirmed) {
                    note.setPasswordChecked(true);
                    AnimationsHelper.zoomListItem(mainActivity, view, getZoomListItemView(view, note),
                            listRoot, buildAnimatorListenerAdapter(note));
                }
            });
        } else {
            AnimationsHelper.zoomListItem(mainActivity, view, getZoomListItemView(view, note),
                    listRoot, buildAnimatorListenerAdapter(note));
        }
    }



    void editNote2(Note note) {
        if (note.get_id() == null) {
            Log.d(Constants.TAG, "Adding new note");
            // if navigation is a category it will be set into note
            try {
                if (Navigation.checkNavigation(Navigation.CATEGORY) || !TextUtils.isEmpty(mainActivity.navigationTmp)) {
                    String categoryId = (String) ObjectUtils.defaultIfNull(mainActivity.navigationTmp,
                            Navigation.getCategory().toString());
                    note.setCategory(DbHelper.getInstance().getCategory(Long.parseLong(categoryId)));
                }
            } catch (NumberFormatException e) {
                Log.v(Constants.TAG, "Maybe was not a category!");
            }
        } else {
            Log.d(Constants.TAG, "Editing note with id: " + note.get_id());
        }

        // Current list scrolling position is saved to be restored later
        refreshListScrollPosition();

        closeFab();

        // Fragments replacing
        mainActivity.switchToDetail(note);
    }
    @Override
    public// Used to show a Crouton dialog after saved (or tried to) a note
    void onActivityResult(int requestCode, final int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case REQUEST_CODE_CATEGORY:
                // Dialog retarded to give time to activity's views of being completely initialized
                // The dialog style is choosen depending on result code
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        mainActivity.showMessage(R.string.category_saved, ONStyle.CONFIRM);
                        EventBus.getDefault().post(new CategoriesUpdatedEvent());
                        break;
                    case Activity.RESULT_FIRST_USER:
                        mainActivity.showMessage(R.string.category_deleted, ONStyle.ALERT);
                        break;
                    default:
                        break;
                }

                break;

            case REQUEST_CODE_CATEGORY_NOTES:
                if (intent != null) {
                    Category tag = intent.getParcelableExtra(Constants.INTENT_CATEGORY);
                    categorizeNotesExecute(tag);
                }
                break;

            default:
                break;
        }

    }


    private void checkSortActionPerformed(MenuItem item) {
        if (item.getGroupId() == Constants.MENU_SORT_GROUP_ID) {
            final String[] arrayDb = getResources().getStringArray(R.array.sortable_columns);
            prefs.edit().putString(Constants.PREF_SORTING_COLUMN, arrayDb[item.getOrder()]).commit();
            initNotesList(mainActivity.getIntent());
            // Resets list scrolling position
            listViewPositionOffset = 16;
            listViewPosition = 0;
            restoreListScrollPosition();
            // Updates app widgets
            BaseActivity.notifyAppWidgets(mainActivity);
        }
    }


    /**
     * Empties trash deleting all the notes
     */
    private void emptyTrash() {
        new MaterialDialog.Builder(mainActivity)
                .content(R.string.empty_trash_confirmation)
                .positiveText(R.string.ok)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        for (int i = 0; i < listAdapter.getCount(); i++) {
                            selectedNotes.add(listAdapter.getItem(i));
                        }
                        deleteNotesExecute();
                    }
                }).build().show();
    }


    /**
     * Notes list adapter initialization and association to view
     */
    void initNotesList(Intent intent) {
        Log.d(Constants.TAG, "initNotesList intent: " + intent.getAction());

        progress_wheel.setAlpha(1);
        list.setAlpha(0);

        NoteLoaderTask mNoteLoaderTask = new NoteLoaderTask();

        // Search for a tag
        // A workaround to simplify it's to simulate normal search
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getCategories() != null
                && intent.getCategories().contains(Intent.CATEGORY_BROWSABLE)) {
            searchTags = intent.getDataString().replace(UrlCompleter.HASHTAG_SCHEME, "");
            goBackOnToggleSearchLabel = true;
        }

        // Searching
        searchQuery = searchQueryInstant;
        searchQueryInstant = null;
        if (searchTags != null || searchQuery != null || Intent.ACTION_SEARCH.equals(intent.getAction())) {

            // Using tags
            if (searchTags != null && intent.getStringExtra(SearchManager.QUERY) == null) {
                searchQuery = searchTags;
                mNoteLoaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "getNotesByTag", searchQuery);
            } else {
                // Get the intent, verify the action and get the query
                if (intent.getStringExtra(SearchManager.QUERY) != null) {
                    searchQuery = intent.getStringExtra(SearchManager.QUERY);
                    searchTags = null;
                }
                mNoteLoaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "getNotesByPattern", searchQuery);
            }

            toggleSearchLabel(true);

        } else {
            // Check if is launched from a widget with categories
            if ((Constants.ACTION_WIDGET_SHOW_LIST.equals(intent.getAction()) && intent
                    .hasExtra(Constants.INTENT_WIDGET))
                    || !TextUtils.isEmpty(mainActivity.navigationTmp)) {
                String widgetId = intent.hasExtra(Constants.INTENT_WIDGET) ? intent.getExtras()
                        .get(Constants.INTENT_WIDGET).toString() : null;
                if (widgetId != null) {
                    String sqlCondition = prefs.getString(Constants.PREF_WIDGET_PREFIX + widgetId, "");
                    String categoryId = TextHelper.checkIntentCategory(sqlCondition);
                    mainActivity.navigationTmp = !TextUtils.isEmpty(categoryId) ? categoryId : null;
                }
                intent.removeExtra(Constants.INTENT_WIDGET);
                if (mainActivity.navigationTmp != null) {
                    mNoteLoaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "getNotesByCategory", mainActivity
                            .navigationTmp);
                } else {
                    mNoteLoaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "getAllNotes", true);
                }

            } else {
                mNoteLoaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "getAllNotes", true);
            }
        }
    }


    public void toggleSearchLabel(boolean activate) {
        if (activate) {
            searchQueryView.setText(Html.fromHtml(getString(R.string.search) + ":<b> " + searchQuery + "</b>"));
            searchLayout.setVisibility(View.VISIBLE);
            searchCancel.setOnClickListener(v -> toggleSearchLabel(false));
            searchLabelActive = true;
        } else {
            if (searchLabelActive) {
                searchLabelActive = false;
                AnimationsHelper.expandOrCollapse(searchLayout, false);
                searchTags = null;
                searchQuery = null;
                if (!goBackOnToggleSearchLabel) {
                    mainActivity.getIntent().setAction(Intent.ACTION_MAIN);
                    if (searchView != null) {
                        MenuItemCompat.collapseActionView(searchMenuItem);
                    }
                    initNotesList(mainActivity.getIntent());
                } else {
                    mainActivity.onBackPressed();
                }
                goBackOnToggleSearchLabel = false;
                if (Intent.ACTION_VIEW.equals(mainActivity.getIntent().getAction())) {
                    mainActivity.getIntent().setAction(null);
                }
            }
        }
    }


    public void onEvent(NavigationUpdatedNavDrawerClosedEvent navigationUpdatedNavDrawerClosedEvent) {
        listViewPosition = 0;
        listViewPositionOffset = 16;
        commitPending();
        initNotesList(mainActivity.getIntent());
    }


    public void onEvent(NotesLoadedEvent notesLoadedEvent) {
        int layoutSelected = prefs.getBoolean(Constants.PREF_EXPANDED_VIEW, true) ? R.layout.note_layout_expanded_pep
                : R.layout.note_layout_pep;
        listAdapter = new NoteAdapter(mainActivity, layoutSelected, notesLoadedEvent.notes);

        View noteLayout = LayoutInflater.from(mainActivity).inflate(layoutSelected, null, false);
        noteViewHolder = new NoteViewHolder(noteLayout);

        if (Navigation.getNavigation() != Navigation.UNCATEGORIZED && prefs.getBoolean(Constants.PREF_ENABLE_SWIPE,
                true)) {
            list.enableSwipeToDismiss((viewGroup, reverseSortedPositions) -> {

                // Avoids conflicts with action mode
                finishActionMode();
                modifiedNotes.clear();

                for (int position : reverseSortedPositions) {
                    Note note;
                    try {
                        note = listAdapter.getItem(position);
                    } catch (IndexOutOfBoundsException e) {
                        Log.d(Constants.TAG, "Please stop swiping in the zone beneath the last card");
                        continue;
                    }
                    getSelectedNotes().add(note);

                    // Depending on settings and note status this action will...
                    // ...restore
                    if (Navigation.checkNavigation(Navigation.TRASH)) {
                        trashNotes(false);
                    }
                    // ...removes category
                    else if (Navigation.checkNavigation(Navigation.CATEGORY)) {
                        categorizeNotesExecute(null);
                    } else {
                        // ...trash
                        if (prefs.getBoolean("settings_swipe_to_trash", false)
                                || Navigation.checkNavigation(Navigation.ARCHIVE)) {
                            trashNotes(true);
                            // ...archive
                        } else {
                            archiveNotes(true);
                        }
                    }
                }
            });
        } else {
            list.disableSwipeToDismiss();
        }
        list.setAdapter(listAdapter);

        // Replace listview with Mr. Jingles if it is empty
        if (notesLoadedEvent.notes.size() == 0) list.setEmptyView(empyListItem);

        // Restores listview position when turning back to list or when navigating reminders
        if (list != null && notesLoadedEvent.notes.size() > 0) {
            if (Navigation.checkNavigation(Navigation.REMINDERS)) {
                listViewPosition = listAdapter.getClosestNotePosition();
            }
            restoreListScrollPosition();
        }

        // Fade in the list view
        animate(progress_wheel).setDuration(getResources().getInteger(R.integer.list_view_fade_anim)).alpha(0);
        animate(list).setDuration(getResources().getInteger(R.integer.list_view_fade_anim)).alpha(1);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void restoreListScrollPosition() {
        if (list.getCount() > listViewPosition) {
            list.setSelectionFromTop(listViewPosition, listViewPositionOffset);
            new Handler().postDelayed(fab::showFab, 150);
        } else {
            list.setSelectionFromTop(0, 0);
        }
    }


    /**
     * Batch note trashing
     */
    public void trashNotes(boolean trash) {
        int selectedNotesSize = getSelectedNotes().size();

        // Restore is performed immediately, otherwise undo bar is shown
        if (trash) {
            for (Note note : getSelectedNotes()) {
                // Saves notes to be eventually restored at right position
                undoNotesList.put(listAdapter.getPosition(note) + undoNotesList.size(), note);
                modifiedNotes.add(note);
                listAdapter.remove(note);
            }
        } else {
            trashNote(getSelectedNotes(), false);
        }

        // If list is empty again Mr Jingles will appear again
        if (listAdapter.getCount() == 0)
            list.setEmptyView(empyListItem);

        finishActionMode();

        // Advice to user
        if (trash) {
            mainActivity.showMessage(R.string.note_trashed, ONStyle.WARN);
        } else {
            mainActivity.showMessage(R.string.note_untrashed, ONStyle.INFO);
        }

        // Creation of undo bar
        if (trash) {
            ubc.showUndoBar(false, selectedNotesSize + " " + getString(R.string.trashed), null);
            fab.hideFab();
            undoTrash = true;
        } else {
            getSelectedNotes().clear();
        }
    }


    private android.support.v7.view.ActionMode getActionMode() {
        return actionMode;
    }


    private List<Note> getSelectedNotes() {
        return selectedNotes;
    }


    /**
     * Single note logical deletion
     */
    @SuppressLint("NewApi")
    protected void trashNote(List<Note> notes, boolean trash) {
        listAdapter.remove(notes);
        new NoteProcessorTrash(notes, trash).process();
    }


    /**
     * Selects all notes in list
     */
    private void selectAllNotes() {
        for (int i = 0; i < list.getChildCount(); i++) {
            LinearLayout v = (LinearLayout) list.getChildAt(i).findViewById(R.id.card_layout);
            // Checks null to avoid the footer
            if (v != null) {
                v.setBackgroundColor(getResources().getColor(R.color.list_bg_selected));
            }
        }
        selectedNotes.clear();
        for (int i = 0; i < listAdapter.getCount(); i++) {
            selectedNotes.add(listAdapter.getItem(i));
            listAdapter.addSelectedItem(i);
        }
        prepareActionModeMenu();
        setCabTitle();
    }


    /**
     * Batch note permanent deletion
     */
    private void deleteNotes() {
        new MaterialDialog.Builder(mainActivity)
                .content(R.string.delete_note_confirmation)
                .positiveText(R.string.ok)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        mainActivity.requestPassword(mainActivity, getSelectedNotes(),
                                passwordConfirmed -> {
                                    if (passwordConfirmed) {
                                        deleteNotesExecute();
                                    }
                                });
                    }
                }).build().show();
    }


    /**
     * Performs notes permanent deletion after confirmation by the user
     */
    private void deleteNotesExecute() {
        listAdapter.remove(getSelectedNotes());
        new NoteProcessorDelete(getSelectedNotes()).process();
        list.clearChoices();
        selectedNotes.clear();
        finishActionMode();
        // If list is empty again Mr Jingles will appear again
        if (listAdapter.getCount() == 0)
            list.setEmptyView(empyListItem);
        mainActivity.showMessage(R.string.note_deleted, ONStyle.ALERT);
    }


    /**
     * Batch note archiviation
     */
    public void archiveNotes(boolean archive) {
        int selectedNotesSize = getSelectedNotes().size();
        // Used in undo bar commit
        sendToArchive = archive;

        if (!archive) {
            archiveNote(getSelectedNotes(), false);
        }
        for (Note note : getSelectedNotes()) {
            // If is restore it will be done immediately, otherwise the undo bar will be shown
            if (archive) {
                // Saves archived state to eventually undo
                undoArchivedMap.put(note, note.isArchived());
                // Saves notes to be eventually restored at right position
                undoNotesList.put(listAdapter.getPosition(note) + undoNotesList.size(), note);
                modifiedNotes.add(note);
            }

            // If actual navigation is not "Notes" the item will not be removed but replaced to fit the new state
            if (Navigation.checkNavigation(Navigation.NOTES)
                    || (Navigation.checkNavigation(Navigation.ARCHIVE) && !archive)
                    || (Navigation.checkNavigation(Navigation.CATEGORY) && prefs.getBoolean(Constants
                    .PREF_FILTER_ARCHIVED_IN_CATEGORIES + Navigation.getCategory(), false))) {
                listAdapter.remove(note);
            } else {
                note.setArchived(archive);
                listAdapter.replace(note, listAdapter.getPosition(note));
            }
        }

        listAdapter.notifyDataSetChanged();
        finishActionMode();

        // If list is empty again Mr Jingles will appear again
        if (listAdapter.getCount() == 0) list.setEmptyView(empyListItem);

        // Advice to user
        int msg = archive ? R.string.note_archived : R.string.note_unarchived;
        Style style = archive ? ONStyle.WARN : ONStyle.INFO;
        mainActivity.showMessage(msg, style);

        // Creation of undo bar
        if (archive) {
            ubc.showUndoBar(false, selectedNotesSize + " " + getString(R.string.archived), null);
            fab.hideFab();
            undoArchive = true;
        } else {
            getSelectedNotes().clear();
        }
    }


    private void archiveNote(List<Note> notes, boolean archive) {
        new NoteProcessorArchive(notes, archive).process();
        if (!Navigation.checkNavigation(Navigation.CATEGORY)) {
            listAdapter.remove(notes);
        }
        Log.d(Constants.TAG, "Notes" + (archive ? "archived" : "restored from archive"));
    }


    /**
     * Categories addition and editing
     */
    void editCategory(Category category) {
        Intent categoryIntent = new Intent(mainActivity, CategoryActivity.class);
        categoryIntent.putExtra(Constants.INTENT_CATEGORY, category);
        startActivityForResult(categoryIntent, REQUEST_CODE_CATEGORY);
    }


    /**
     * Associates to or removes categories
     */
    private void categorizeNotes() {
        // Retrieves all available categories
        final ArrayList<Category> categories = DbHelper.getInstance().getCategories();

        final MaterialDialog dialog = new MaterialDialog.Builder(mainActivity)
                .title(R.string.categorize_as)
                .adapter(new NavDrawerCategoryAdapter(mainActivity, categories), null)
                .positiveText(R.string.add_category)
                .negativeText(R.string.remove_category)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        keepActionMode = true;
                        Intent intent = new Intent(mainActivity, CategoryActivity.class);
                        intent.putExtra("noHome", true);
                        startActivityForResult(intent, REQUEST_CODE_CATEGORY_NOTES);
                    }


                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        categorizeNotesExecute(null);
                    }
                }).build();

        ListView dialogList = dialog.getListView();
        assert dialogList != null;
        dialogList.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            categorizeNotesExecute(categories.get(position));
        });

        dialog.show();
    }


    private void categorizeNotesExecute(Category category) {
        if (category != null)
            categorizeNote(getSelectedNotes(), category);
        for (Note note : getSelectedNotes()) {
            // If is restore it will be done immediately, otherwise the undo bar
            // will be shown
            if (category == null) {
                // Saves categories associated to eventually undo
                undoCategoryMap.put(note, note.getCategory());
                // Saves notes to be eventually restored at right position
                undoNotesList.put(listAdapter.getPosition(note) + undoNotesList.size(), note);
                modifiedNotes.add(note);
            }
            // Update adapter content if actual navigation is the category
            // associated with actually cycled note
            if ((Navigation.checkNavigation(Navigation.CATEGORY) && !Navigation.checkNavigationCategory(category)) ||
                    Navigation.checkNavigation(Navigation.UNCATEGORIZED)) {
                listAdapter.remove(note);
            } else {
                note.setCategory(category);
                listAdapter.replace(note, listAdapter.getPosition(note));
            }
        }

        finishActionMode();

        // If list is empty again Mr Jingles will appear again
        if (listAdapter.getCount() == 0)
            list.setEmptyView(empyListItem);

        if (getActionMode() != null) {
            getActionMode().finish();
        }

        // Advice to user
        String msg;
        if (category != null) {
            msg = getResources().getText(R.string.notes_categorized_as) + " '" + category.getName() + "'";
        } else {
            msg = getResources().getText(R.string.notes_category_removed).toString();
        }
        mainActivity.showMessage(msg, ONStyle.INFO);

        // Creation of undo bar
        if (category == null) {
            ubc.showUndoBar(false, getString(R.string.notes_category_removed), null);
            fab.hideFab();
            undoCategorize = true;
            undoCategorizeCategory = null;
        } else {
            getSelectedNotes().clear();
        }
    }


    private void categorizeNote(List<Note> notes, Category category) {
        new NoteProcessorCategorize(notes, category).process();
    }


    /**
     * Bulk tag selected notes
     */
    private void tagNotes() {

        // Retrieves all available tags
        final List<Tag> tags = DbHelper.getInstance().getTags();

        // If there is no tag a message will be shown
        if (tags.size() == 0) {
            finishActionMode();
            mainActivity.showMessage(R.string.no_tags_created, ONStyle.WARN);
            return;
        }

        final Integer[] preSelectedTags = TagsHelper.getPreselectedTagsArray(selectedNotes, tags);

        new MaterialDialog.Builder(mainActivity)
                .title(R.string.select_tags)
                .items(TagsHelper.getTagsArray(tags))
                .positiveText(R.string.ok)
                .itemsCallbackMultiChoice(preSelectedTags, (dialog, which, text) -> {
                    dialog.dismiss();
                    tagNotesExecute(tags, which, preSelectedTags);
                    return false;
                }).build().show();
    }


    private void tagNotesExecute(List<Tag> tags, Integer[] selectedTags, Integer[] preSelectedTags) {

        // Retrieves selected tags
        for (Note note : getSelectedNotes()) {
            tagNote(tags, selectedTags, note);
        }

        // Clears data structures
        list.clearChoices();

        // Refreshes list
        list.invalidateViews();

        // If list is empty again Mr Jingles will appear again
        if (listAdapter.getCount() == 0)
            list.setEmptyView(empyListItem);

        if (getActionMode() != null) {
            getActionMode().finish();
        }

        mainActivity.showMessage(R.string.tags_added, ONStyle.INFO);
    }


    private void tagNote(List<Tag> tags, Integer[] selectedTags, Note note) {

        Pair<String, List<Tag>> taggingResult = TagsHelper.addTagToNote(tags, selectedTags, note);

        if (note.isChecklist()) {
            note.setTitle(note.getTitle() + System.getProperty("line.separator") + taggingResult.first);
        } else {
            StringBuilder sb = new StringBuilder(note.getContent());
            if (sb.length() > 0) {
                sb.append(System.getProperty("line.separator"))
                        .append(System.getProperty("line.separator"));
            }
            sb.append(taggingResult.first);
            note.setContent(sb.toString());
        }

        // Removes unchecked tags
        Pair<String, String> titleAndContent = TagsHelper.removeTag(note.getTitle(), note.getContent(),
                taggingResult.second);
        note.setTitle(titleAndContent.first);
        note.setContent(titleAndContent.second);

        DbHelper.getInstance().updateNote(note, false);
    }


//	private void synchronizeSelectedNotes() {
//		new DriveSyncTask(mainActivity).execute(new ArrayList<Note>(getSelectedNotes()));
//		// Clears data structures
//		listAdapter.clearSelectedItems();
//		list.clearChoices();
//		finishActionMode();
//	}


    @Override
    public void onUndo(Parcelable undoToken) {
        // Cycles removed items to re-insert into adapter
        for (Note note : modifiedNotes) {
            //   Manages uncategorize or archive  undo
            if ((undoCategorize && !Navigation.checkNavigationCategory(undoCategoryMap.get(note)))
                    || undoArchive && !Navigation.checkNavigation(Navigation.NOTES)) {
                if (undoCategorize) {
                    note.setCategory(undoCategoryMap.get(note));
                } else if (undoArchive) {
                    note.setArchived(undoArchivedMap.get(note));
                }
                listAdapter.replace(note, listAdapter.getPosition(note));
                // Manages trash undo
            } else {
                list.insert(undoNotesList.keyAt(undoNotesList.indexOfValue(note)), note);
            }
        }

        listAdapter.notifyDataSetChanged();

        selectedNotes.clear();
        undoNotesList.clear();
        modifiedNotes.clear();

        undoTrash = false;
        undoArchive = false;
        undoCategorize = false;
        undoNotesList.clear();
        undoCategoryMap.clear();
        undoArchivedMap.clear();
        undoCategorizeCategory = null;
        Crouton.cancelAllCroutons();

        if (getActionMode() != null) {
            getActionMode().finish();
        }
        ubc.hideUndoBar(false);
        fab.showFab();
    }


    void commitPending() {
        if (undoTrash || undoArchive || undoCategorize) {

            if (undoTrash)
                trashNote(modifiedNotes, true);
            else if (undoArchive)
                archiveNote(modifiedNotes, sendToArchive);
            else if (undoCategorize)
                categorizeNote(modifiedNotes, undoCategorizeCategory);

            undoTrash = false;
            undoArchive = false;
            undoCategorize = false;
            undoCategorizeCategory = null;

            // Clears data structures
            selectedNotes.clear();
            modifiedNotes.clear();
            undoNotesList.clear();
            undoCategoryMap.clear();
            undoArchivedMap.clear();
            list.clearChoices();

            ubc.hideUndoBar(false);
            fab.showFab();

            BaseActivity.notifyAppWidgets(mainActivity);
            Log.d(Constants.TAG, "Changes committed");
        }
    }


    /**
     * Shares the selected note from the list
     */
    private void share() {
        // Only one note should be selected to perform sharing but they'll be cycled anyhow
        for (final Note note : getSelectedNotes()) {
            mainActivity.shareNote(note);
        }

        getSelectedNotes().clear();
        if (getActionMode() != null) {
            getActionMode().finish();
        }
    }


    public void merge() {
        new MaterialDialog.Builder(mainActivity)
                .title(R.string.delete_merged)
                .positiveText(R.string.ok)
                .negativeText(R.string.no)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        EventBus.getDefault().post(new NotesMergeEvent(false));
                    }


                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        EventBus.getDefault().post(new NotesMergeEvent(true));
                    }
                }).build().show();
    }


    /**
     * Merges all the selected notes
     */
    public void onEventAsync(NotesMergeEvent notesMergeEvent) {

        final Note finalMergedNote = NotesHelper.mergeNotes(getSelectedNotes(), notesMergeEvent.keepMergedNotes);
        new Handler(Looper.getMainLooper()).post(() -> {

            if (!notesMergeEvent.keepMergedNotes) {
                ArrayList<String> notesIds = new ArrayList<>();
                for (Note selectedNote : getSelectedNotes()) {
                    notesIds.add(String.valueOf(selectedNote.get_id()));
                }
                mainActivity.getIntent().putExtra("merged_notes", notesIds);
            }

            getSelectedNotes().clear();
            if (getActionMode() != null) {
                getActionMode().finish();
            }

            mainActivity.getIntent().setAction(Constants.ACTION_MERGE);
            mainActivity.switchToDetail(finalMergedNote);
        });
    }


    /**
     * Excludes past reminders
     */
    private void filterReminders(boolean filter) {
        prefs.edit().putBoolean(Constants.PREF_FILTER_PAST_REMINDERS, filter).apply();
        // Change list view
        initNotesList(mainActivity.getIntent());
        // Called to switch menu voices
        mainActivity.supportInvalidateOptionsMenu();
    }


    /**
     * Excludes archived notes in categories navigation
     */
    private void filterCategoryArchived(boolean filter) {
        if (filter) {
            prefs.edit().putBoolean(Constants.PREF_FILTER_ARCHIVED_IN_CATEGORIES + Navigation.getCategory(), true).apply();
        } else {
            prefs.edit().remove(Constants.PREF_FILTER_ARCHIVED_IN_CATEGORIES + Navigation.getCategory()).apply();
        }
        // Change list view
        initNotesList(mainActivity.getIntent());
        // Called to switch menu voices
        mainActivity.supportInvalidateOptionsMenu();
    }


    /**
     * Search notes by tags
     */
    private void filterByTags() {

        // Retrieves all available categories
        final List<Tag> tags = TagsHelper.getAllTags(mainActivity);

        // If there is no category a message will be shown
        if (tags.size() == 0) {
            mainActivity.showMessage(R.string.no_tags_created, ONStyle.WARN);
            return;
        }

        // Dialog and events creation
        new MaterialDialog.Builder(mainActivity)
                .title(R.string.select_tags)
                .items(TagsHelper.getTagsArray(tags))
                .positiveText(R.string.ok)
                .itemsCallbackMultiChoice(new Integer[]{}, (dialog, which, text) -> {
                    // Retrieves selected tags
                    List<String> selectedTags = new ArrayList<>();
                    for (Integer aWhich : which) {
                        selectedTags.add(tags.get(aWhich).getText());
                    }

                    // Saved here to allow persisting search
                    searchTags = selectedTags.toString().substring(1, selectedTags.toString().length() - 1)
                            .replace(" ", "");
                    Intent intent = mainActivity.getIntent();

                    // Hides keyboard
                    searchView.clearFocus();
                    KeyboardUtils.hideKeyboard(searchView);

                    intent.removeExtra(SearchManager.QUERY);
                    initNotesList(intent);
                    return false;
                }).build().show();
    }


    public MenuItem getSearchMenuItem() {
        return searchMenuItem;
    }


}
