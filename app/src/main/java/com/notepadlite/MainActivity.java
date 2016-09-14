/* Copyright 2014 Braden Farmer
 * Copyright 2015 Sean93Park
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.notepadlite;

import android.annotation.TargetApi;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements
BackButtonDialogFragment.Listener, 
DeleteDialogFragment.Listener, 
SaveButtonDialogFragment.Listener,
FirstRunDialogFragment.Listener,
WearPluginDialogFragment.Listener,
NoteListFragment.Listener,
NoteEditFragment.Listener, 
NoteViewFragment.Listener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Remove margins from layout on Lollipop devices
            LinearLayout layout = (LinearLayout) findViewById(R.id.noteViewEdit);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layout.getLayoutParams();
            params.setMargins(0, 0, 0, 0);
            layout.setLayoutParams(params);

            // Set action bar elevation
            getSupportActionBar().setElevation(getResources().getDimensionPixelSize(R.dimen.action_bar_elevation));
        }

        // Show dialog if this is the user's first time running Notepad
        SharedPreferences prefMain = getPreferences(Context.MODE_PRIVATE);
        if(prefMain.getInt("first-run", 0) == 0) {
            // Show welcome dialog
            if(getSupportFragmentManager().findFragmentByTag("firstrunfragment") == null) {
                DialogFragment firstRun = new FirstRunDialogFragment();
                firstRun.show(getSupportFragmentManager(), "firstrunfragment");
            }
        } else {
            // Check to see if Android Wear app is installed, and offer to install the Notepad Plugin
            checkForAndroidWear();

            // The following code is only present to support existing users of Notepad on Google Play
            // and can be removed if using this source code for a different app

            // Convert old preferences to new ones
            SharedPreferences pref = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
            if(prefMain.getInt("sort-by", -1) == 0) {
                SharedPreferences.Editor editor = pref.edit();
                SharedPreferences.Editor editorMain = prefMain.edit();

                editor.putString("sort_by", "date");
                editorMain.putInt("sort-by", -1);

                editor.apply();
                editorMain.apply();
            } else if(prefMain.getInt("sort-by", -1) == 1) {
                SharedPreferences.Editor editor = pref.edit();
                SharedPreferences.Editor editorMain = prefMain.edit();

                editor.putString("sort_by", "name");
                editorMain.putInt("sort-by", -1);

                editor.apply();
                editorMain.apply();
            }

            if(pref.getString("font_size", "null").equals("null")) {
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("font_size", "large");
                editor.apply();
            }


            // Rename any saved drafts from 1.3.x
            File oldDraft = new File(getFilesDir() + File.separator + "draft");
            File newDraft = new File(getFilesDir() + File.separator + String.valueOf(System.currentTimeMillis()));

            if(oldDraft.exists())
                oldDraft.renameTo(newDraft);
        }

        // Begin a new FragmentTransaction
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // This fragment shows NoteListFragment as a sidebar (only seen in tablet mode landscape)
        if(!(getSupportFragmentManager().findFragmentById(R.id.noteList) instanceof NoteListFragment))
            transaction.replace(R.id.noteList, new NoteListFragment(), "NoteListFragment");

        // This fragment shows NoteListFragment in the main screen area (only seen on phones and tablet mode portrait),
        // but only if it doesn't already contain NoteViewFragment or NoteEditFragment.
        // If NoteListFragment is already showing in the sidebar, use WelcomeFragment instead
        if(!((getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteEditFragment)
           || (getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteViewFragment))) {
            if((getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) == null
               && findViewById(R.id.layoutMain).getTag().equals("main-layout-large"))
               || ((getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteListFragment)
               && findViewById(R.id.layoutMain).getTag().equals("main-layout-large")))
                    transaction.replace(R.id.noteViewEdit, new WelcomeFragment(), "NoteListFragment");
            else if(findViewById(R.id.layoutMain).getTag().equals("main-layout-normal"))
                transaction.replace(R.id.noteViewEdit, new NoteListFragment(), "NoteListFragment");
        }

        // Commit fragment transaction
        transaction.commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkForAndroidWear();
    }

    private void checkForAndroidWear() {
        // Notepad Plugin for Android Wear sends intent with "plugin_install_complete" extra,
        // in order to verify that the main Notepad app is installed correctly
        if(getIntent().hasExtra("plugin_install_complete")) {
            if(getSupportFragmentManager().findFragmentByTag("WearPluginDialogFragmentAlt") == null) {
                DialogFragment wearDialog = new WearPluginDialogFragmentAlt();
                wearDialog.show(getSupportFragmentManager(), "WearPluginDialogFragmentAlt");
            }

            SharedPreferences pref = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("show_wear_dialog", false);
            editor.apply();
        } else {
            boolean hasAndroidWear = false;

            @SuppressWarnings("unused")
            PackageInfo pInfo;
            try {
                pInfo = getPackageManager().getPackageInfo("com.google.android.wearable.app", 0);
                hasAndroidWear = true;
            } catch (PackageManager.NameNotFoundException e) {}

            if(hasAndroidWear) {
                try {
                    pInfo = getPackageManager().getPackageInfo("com.notepadlite.wear", 0);
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setComponent(ComponentName.unflattenFromString("com.notepadlite.wear/com.notepadlite.wear.MobileMainActivity"));
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (PackageManager.NameNotFoundException e) {
                    SharedPreferences pref = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
                    if(pref.getBoolean("show_wear_dialog", true)
                            && getSupportFragmentManager().findFragmentByTag("WearPluginDialogFragment") == null) {
                        DialogFragment wearDialog = new WearPluginDialogFragment();
                        wearDialog.show(getSupportFragmentManager(), "WearPluginDialogFragment");
                    }
                } catch (ActivityNotFoundException e) {}
            }
        }
    }

    // Keyboard shortcuts
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        super.dispatchKeyShortcutEvent(event);
        if(event.getAction() == KeyEvent.ACTION_DOWN && event.isCtrlPressed()) {
            if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteListFragment) {
                NoteListFragment fragment = (NoteListFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
                fragment.dispatchKeyShortcutEvent(event.getKeyCode());
            } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteViewFragment) {
                NoteViewFragment fragment = (NoteViewFragment) getSupportFragmentManager().findFragmentByTag("NoteViewFragment");
                fragment.dispatchKeyShortcutEvent(event.getKeyCode());
            } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteEditFragment) {
                NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
                fragment.dispatchKeyShortcutEvent(event.getKeyCode());
            } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof WelcomeFragment) {
                WelcomeFragment fragment = (WelcomeFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
                fragment.dispatchKeyShortcutEvent(event.getKeyCode());
            }

            return true;
        }
        return super.dispatchKeyShortcutEvent(event);
    }

    @Override
    public void onDeleteDialogPositiveClick() {
        if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteViewFragment) {
            NoteViewFragment fragment = (NoteViewFragment) getSupportFragmentManager().findFragmentByTag("NoteViewFragment");
            fragment.onDeleteDialogPositiveClick();
        } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteEditFragment) {
            NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
            fragment.onDeleteDialogPositiveClick();
        }
    }

    @Override
    public void onBackPressed() {
        if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteListFragment) {
            NoteListFragment fragment = (NoteListFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
            fragment.onBackPressed();
        } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteViewFragment) {
            NoteViewFragment fragment = (NoteViewFragment) getSupportFragmentManager().findFragmentByTag("NoteViewFragment");
            fragment.onBackPressed();
        } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteEditFragment) {
            NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
            fragment.onBackPressed(null);
        } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof WelcomeFragment) {
            WelcomeFragment fragment = (WelcomeFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
            fragment.onBackPressed();
        }
    }

    @Override
    public void viewNote(String filename) {
        viewEditNote(filename, false);
    }

    @Override
    public void editNote(String filename) {
        viewEditNote(filename, true);
    }

    // Method used by selecting a existing note from the ListView in NoteViewFragment or NoteEditFragment
    // We need this method in MainActivity because sometimes getSupportFragmentManager() is null
    public void viewEditNote(String filename, boolean isEdit) {
        String currentFilename;

        if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteEditFragment) {
            NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
            currentFilename = fragment.getFilename();
        } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteViewFragment) {
            NoteViewFragment fragment = (NoteViewFragment) getSupportFragmentManager().findFragmentByTag("NoteViewFragment");
            currentFilename = fragment.getFilename();
        } else
            currentFilename = " ";

        if(!currentFilename.equals(filename)) {
            if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteEditFragment) {
                NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
                fragment.switchNotes(filename);
            } else {
                Bundle bundle = new Bundle();
                bundle.putString("filename", filename);

                Fragment fragment;
                String tag;

                if(isEdit) {
                    fragment = new NoteEditFragment();
                    tag = "NoteEditFragment";
                } else {
                    fragment = new NoteViewFragment();
                    tag = "NoteViewFragment";
                }

                fragment.setArguments(bundle);

                // Add NoteViewFragment or NoteEditFragment
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.noteViewEdit, fragment, tag)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                        .commit();
            }
        }
    }

    @Override
    public void onBackDialogNegativeClick(String filename) {
        NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
        fragment.onBackDialogNegativeClick(filename);
    }

    @Override
    public void onBackDialogPositiveClick(String filename) {
        NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
        fragment.onBackDialogPositiveClick(filename);
    }

    @Override
    public void onSaveDialogNegativeClick() {
        NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
        fragment.onSaveDialogNegativeClick();
    }

    @Override
    public void onSaveDialogPositiveClick() {
        NoteEditFragment fragment = (NoteEditFragment) getSupportFragmentManager().findFragmentByTag("NoteEditFragment");
        fragment.onSaveDialogPositiveClick();
    }

    @Override
    public void showBackButtonDialog(String filename) {
        Bundle bundle = new Bundle();
        bundle.putString("filename", filename);

        DialogFragment backFragment = new BackButtonDialogFragment();
        backFragment.setArguments(bundle);
        backFragment.show(getSupportFragmentManager(), "back");
    }

    @Override
    public void showDeleteDialog() {
        DialogFragment deleteFragment = new DeleteDialogFragment();
        deleteFragment.show(getSupportFragmentManager(), "delete");
    }

    @Override
    public void showSaveButtonDialog() {
        DialogFragment saveFragment = new SaveButtonDialogFragment();
        saveFragment.show(getSupportFragmentManager(), "save");
    }

    @Override
    public boolean isShareIntent() {
        return false;
    }

    @Override
    public String getCabString(int size) {
        if(size == 1)
            return getResources().getString(R.string.cab_note_selected);
        else
            return getResources().getString(R.string.cab_notes_selected);
    }

    @Override
    public void deleteNote(Object[] filesToDelete) {
        // Build the pathname to delete each file, them perform delete operation
        for(Object file : filesToDelete) {
            File fileToDelete = new File(getFilesDir() + File.separator + file);
            fileToDelete.delete();
        }

        String[] filesToDelete2 = new String[filesToDelete.length];
        Arrays.asList(filesToDelete).toArray(filesToDelete2);

        // Send broadcasts to update UI
        Intent deleteIntent = new Intent();
        deleteIntent.setAction("com.notepadlite.DELETE_NOTES");
        deleteIntent.putExtra("files", filesToDelete2);
        LocalBroadcastManager.getInstance(this).sendBroadcast(deleteIntent);

        Intent listIntent = new Intent();
        listIntent.setAction("com.notepadlite.LIST_NOTES");
        LocalBroadcastManager.getInstance(this).sendBroadcast(listIntent);

        // Show toast notification
        if(filesToDelete.length == 1)
            showToast(R.string.note_deleted);
        else
            showToast(R.string.notes_deleted);
    }

    @Override
    public void exportNote(Object[] filesToExport) {
        try {
            for(Object file : filesToExport) {
                // Load note title to use as filename, and remove any invalid characters
                final String[] characters = new String[]{"<", ">", ":", "\"", "/", "\\\\", "\\|", "\\?", "\\*"};
                String filename = loadNoteTitle(file.toString());

                if(filename.isEmpty())
                    filename = " ";

                for(String character : characters) {
                    filename = filename.replaceAll(character, "");
                }

                // To ensure that the generated filename fits within filesystem limitations,
                // truncate the filename to ~245 characters.
                if(filename.length() > 245)
                    filename = filename.substring(0, 245);

                // Generate exported filename
                File exportedFile = new File(getExternalFilesDir(null), filename + ".txt");
                int suffix = 1;

                // Handle cases where a note may have a duplicate title
                while(exportedFile.exists()) {
                    suffix++;
                    exportedFile = new File(getExternalFilesDir(null), filename + " (" + Integer.toString(suffix) + ").txt");
                }

                // Load note contents and convert line separators to Windows format
                String note = loadNote(file.toString());
                note = note.replaceAll("\r\n", "\n");
                note = note.replaceAll("\n", "\r\n");

                // Write file to external storage
                OutputStream os = new FileOutputStream(exportedFile);
                os.write(note.getBytes());
                os.close();
            }

            // Show toast notification
            Toast toast;
            if(filesToExport.length == 1)
                toast = Toast.makeText(this, getResources().getString(R.string.note_exported_to) + " " + getExternalFilesDir(null), Toast.LENGTH_LONG);
            else
                toast = Toast.makeText(this, getResources().getString(R.string.notes_exported_to) + " " + getExternalFilesDir(null), Toast.LENGTH_LONG);

            toast.show();
        } catch (IOException e) {
            showToast(R.string.error_exporting_notes);
        }
    }

    // Method used to generate toast notifications
    private void showToast(int message) {
        Toast toast = Toast.makeText(this, getResources().getString(message), Toast.LENGTH_SHORT);
        toast.show();
    }

    // Loads note from /data/data/com.notepadlite/files
    public String loadNote(String filename) throws IOException {

        // Initialize StringBuilder which will contain note
        StringBuilder note = new StringBuilder("");

        // Open the file on disk
        FileInputStream input = openFileInput(filename);
        InputStreamReader reader = new InputStreamReader(input);
        BufferedReader buffer = new BufferedReader(reader);

        // Load the file
        String line = buffer.readLine();
        while (line != null ) {
            note.append(line);
            line = buffer.readLine();
            if(line != null)
                note.append("\n");
        }

        // Close file on disk
        reader.close();

        return(note.toString());
    }

    // Loads first line of a note for display in the ListView
    @Override
    public String loadNoteTitle(String filename) throws IOException {
        // Open the file on disk
        FileInputStream input = openFileInput(filename);
        InputStreamReader reader = new InputStreamReader(input);
        BufferedReader buffer = new BufferedReader(reader);

        // Load the file
        String line = buffer.readLine();

        // Close file on disk
        reader.close();

        return(line);
    }

    // Calculates last modified date/time of a note for display in the ListView
    @Override
    public String loadNoteDate(String filename) throws IOException {
        Date lastModified = new Date(Long.parseLong(filename));
        return(DateFormat
                .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(lastModified));
    }

    @Override
    public void showFab() {
        if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteListFragment) {
            NoteListFragment fragment = (NoteListFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
            fragment.showFab();
        } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof WelcomeFragment) {
            WelcomeFragment fragment = (WelcomeFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
            fragment.showFab();
        }
    }

    @Override
    public void hideFab() {
        if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof NoteListFragment) {
            NoteListFragment fragment = (NoteListFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
            fragment.hideFab();
        } else if(getSupportFragmentManager().findFragmentById(R.id.noteViewEdit) instanceof WelcomeFragment) {
            WelcomeFragment fragment = (WelcomeFragment) getSupportFragmentManager().findFragmentByTag("NoteListFragment");
            fragment.hideFab();
        }
    }

    @Override
    public void onFirstRunDialogPositiveClick() {
        // Set some initial preferences
        SharedPreferences prefMain = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefMain.edit();
        editor.putInt("first-run", 1);
        editor.apply();

        checkForAndroidWear();
    }

    @Override
    public void onWearDialogPositiveClick() {
        // Intent to Google Play
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.notepadlite.wear"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onWearDialogNegativeClick() {
        SharedPreferences pref = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("show_wear_dialog", false);
        editor.apply();
    }
}
