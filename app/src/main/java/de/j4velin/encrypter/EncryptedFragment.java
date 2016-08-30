/*
 * Copyright 2016 Thomas Hoffmann
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
package de.j4velin.encrypter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Fragment showing the list of encrypted files
 */
public class EncryptedFragment extends Fragment {

    private FileAdapter adapter;
    private final static int REQUEST_OUTPUT = 1;
    private final BroadcastReceiver cryptoCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(MainActivity.CRYPTO_COMPLETE_ACTION)) {
                File resultFile = intent.getParcelableExtra(MainActivity.EXTRA_RESULT_FILE);
                if (resultFile.isEncrypted) {
                    adapter.add(resultFile);
                }
            }
        }
    };

    private File selectedFile;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        RecyclerView recyclerView =
                (RecyclerView) inflater.inflate(R.layout.fragment_filelist, container, false);
        Database db = new Database(getContext());
        adapter = new FileAdapter(getContext(), new FileAdapter.ClickListener() {
            @Override
            public void click(final File file) {
                selectedFile = file;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(selectedFile.mime);
                intent.putExtra(Intent.EXTRA_TITLE, selectedFile.name);
                EncryptedFragment.this.startActivityForResult(intent, REQUEST_OUTPUT);
            }
        }, new FileAdapter.DeleteListener() {
            @Override
            public boolean delete(final File file) {
                java.io.File f = new java.io.File(file.uri.getPath());
                if (!f.exists() || f.delete()) {
                    Database db = new Database(getContext());
                    db.deleteFile(file.id);
                    db.close();
                    return true;
                } else {
                    return false;
                }
            }
        }, db.getFiles());
        db.close();
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        return recyclerView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity()
                .registerReceiver(cryptoCompleteReceiver, new IntentFilter(MainActivity.CRYPTO_COMPLETE_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(cryptoCompleteReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OUTPUT) {
                String expectedFilename;
                if (data.hasExtra(Intent.EXTRA_TITLE)) {
                    expectedFilename = data.getStringExtra(Intent.EXTRA_TITLE);
                } else {
                    expectedFilename = selectedFile.name;
                }
                try {
                    CryptoUtil.decrypt(getContext(), selectedFile, data.getData(), expectedFilename);
                } catch (GeneralSecurityException e) {
                    Snackbar.make(((MainActivity) getActivity()).getCoordinatorLayout(),
                            getString(R.string.error_security, e.getMessage()),
                            Snackbar.LENGTH_LONG).show();
                } catch (FileNotFoundException e) {
                    Snackbar.make(((MainActivity) getActivity()).getCoordinatorLayout(),
                            R.string.error_file_not_found, Snackbar.LENGTH_LONG).show();
                } catch (IOException e) {
                    Snackbar.make(((MainActivity) getActivity()).getCoordinatorLayout(),
                            getString(R.string.error_io, e.getMessage()), Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        }
    }
}
