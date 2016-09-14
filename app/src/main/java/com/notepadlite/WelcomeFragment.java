/* Copyright 2014 Braden Farmer
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
import android.app.ActivityManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

public class WelcomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_welcome_alt, container, false);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set values
        setRetainInstance(true);
        setHasOptionsMenu(true);

        // Animate elevation change
        if(getActivity().findViewById(R.id.layoutMain).getTag().equals("main-layout-large")
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            LinearLayout noteViewEdit = (LinearLayout) getActivity().findViewById(R.id.noteViewEdit);
            LinearLayout noteList = (LinearLayout) getActivity().findViewById(R.id.noteList);

            noteViewEdit.animate().z(0f);
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                noteList.animate().z(getResources().getDimensionPixelSize(R.dimen.note_list_elevation_land));
            else
                noteList.animate().z(getResources().getDimensionPixelSize(R.dimen.note_list_elevation));
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Read preferences
        SharedPreferences prefMain = getActivity().getPreferences(Context.MODE_PRIVATE);

        // Before we do anything else, check for a saved draft; if one exists, load it
        if(prefMain.getLong("draft-name", 0) != 0) {
            Bundle bundle = new Bundle();
            bundle.putString("filename", "draft");

            Fragment fragment = new NoteEditFragment();
            fragment.setArguments(bundle);

            // Add NoteEditFragment
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.noteViewEdit, fragment, "NoteEditFragment")
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .commit();
        } else {
            // Change window title
            String title = getResources().getString(R.string.app_name);

            getActivity().setTitle(title);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(title, null, getResources().getColor(R.color.primary));
                getActivity().setTaskDescription(taskDescription);
            }

            // Don't show the Up button in the action bar, and disable the button
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(false);

            // Floating action button
            FloatingActionButton floatingActionButton = (FloatingActionButton) getActivity().findViewById(R.id.button_floating_action_welcome);
            floatingActionButton.setImageResource(R.drawable.ic_action_new);
            floatingActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle bundle = new Bundle();
                    bundle.putString("filename", "new");

                    Fragment fragment = new NoteEditFragment();
                    fragment.setArguments(bundle);

                    // Add NoteEditFragment
                    getFragmentManager()
                            .beginTransaction()
                            .replace(R.id.noteViewEdit, fragment, "NoteEditFragment")
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                            .commit();
                    }
                });

        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch(item.getItemId()) {
            case R.id.action_settings:
                Intent intentSettings = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intentSettings);
                return true;
            case R.id.action_import:
                try {
                    getActivity().getExternalFilesDir(null);
                    Intent intent = new Intent(getActivity(), ImportActivity.class);
                    startActivity(intent);
                } catch (NullPointerException e) {
                    // Throws a NullPointerException if no external storage is present
                    showToastLong(R.string.no_external_storage);
                }
                return true;
            case R.id.action_about:
                DialogFragment aboutFragment = new AboutDialogFragment();
                aboutFragment.show(getFragmentManager(), "about");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void dispatchKeyShortcutEvent(int keyCode) {
        switch(keyCode) {
                // CTRL+N: New Note
            case KeyEvent.KEYCODE_N:
                Bundle bundle = new Bundle();
                bundle.putString("filename", "new");

                Fragment fragment = new NoteEditFragment();
                fragment.setArguments(bundle);

                // Add NoteEditFragment
                getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.noteViewEdit, fragment, "NoteEditFragment")
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .commit();
                break;
        }
    }

    public void onBackPressed() {
        getActivity().finish();
    }

    public void showFab() {
        FloatingActionButton floatingActionButton = (FloatingActionButton) getActivity().findViewById(R.id.button_floating_action_welcome);
        floatingActionButton.show();
    }

    public void hideFab() {
        FloatingActionButton floatingActionButton = (FloatingActionButton) getActivity().findViewById(R.id.button_floating_action_welcome);
        floatingActionButton.hide();
    }

    private void showToastLong(int message) {
        Toast toast = Toast.makeText(getActivity(), getResources().getString(message), Toast.LENGTH_LONG);
        toast.show();
    }
}
