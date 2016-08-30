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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Fragment showing the list of decrypted files
 */
public class PlaintextFragment extends Fragment {

    private FileAdapter adapter;
    private final BroadcastReceiver cryptoCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(MainActivity.CRYPTO_COMPLETE_ACTION)) {
                if (adapter.getSize() == 0) {
                    // we're about to add a file so show the plaintext layout now
                    ((MainActivity) getActivity()).showPlaintextLayout(true);
                }
                File file = intent.getParcelableExtra(MainActivity.EXTRA_RESULT_FILE);
                if (file.isEncrypted) {
                    // encrypt operation -> add the original file instead
                    file = intent.getParcelableExtra(MainActivity.EXTRA_ORIGINAL_FILE);
                }
                adapter.add(file);
            }
        }
    };

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        RecyclerView recyclerView =
                (RecyclerView) inflater.inflate(R.layout.fragment_filelist, container, false);
        adapter = new FileAdapter(getContext(), new FileAdapter.ClickListener() {
            @Override
            public void click(final File file) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(file.uri, file.mime);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }
        }, new FileAdapter.DeleteListener() {
            @Override
            public boolean delete(final File file) {
                boolean result = DocumentsContract.deleteDocument(getActivity().getContentResolver(), file.uri);
                if (!result) {
                    java.io.File f = new java.io.File(file.uri.getPath());
                    result = !f.exists() || f.delete();
                }
                if (!result) {
                    Snackbar.make(((MainActivity) getActivity()).getCoordinatorLayout(),
                            getString(R.string.can_not_delete, file.name), Snackbar.LENGTH_LONG)
                            .show();
                }
                if (result && adapter.getSize() == 1) {
                    // we're removing the last element -> hide the view
                    ((MainActivity) getActivity()).showPlaintextLayout(false);
                }
                return result;
            }
        });
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

}
