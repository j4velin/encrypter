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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private final Context context;
    private final List<File> files;
    private final DeleteListener deleteListenerImpl;
    private final ClickListener clickListenerImpl;

    interface DeleteListener {
        boolean delete(final File file);
    }

    interface ClickListener {
        void click(final File file);
    }

    private final View.OnClickListener deleteListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            final int position = (int) view.getTag();
            final File file = files.get(position);
            new AlertDialog.Builder(context)
                    .setMessage(context.getString(R.string.ask_delete, file.name))
                    .setNegativeButton(android.R.string.no,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialogInterface,
                                                    int i) {
                                    dialogInterface.dismiss();
                                }
                            }).setPositiveButton(android.R.string.yes,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            if (deleteListenerImpl.delete(file)) {
                                files.remove(position);
                                notifyDataSetChanged(); // invalid all views to have the correct position put in the views
                            }
                        }
                    }).create().show();
        }
    };

    private final View.OnClickListener viewListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            int position = (int) view.getTag();
            File selectedFile = files.get(position);
            clickListenerImpl.click(selectedFile);
        }
    };

    FileAdapter(final Context context, final ClickListener clickListener, final DeleteListener deleteListener) {
        this(context, clickListener, deleteListener, new ArrayList<File>());
    }

    FileAdapter(final Context context, final ClickListener clickListener, final DeleteListener deleteListener, final List<File> files) {
        this.context = context;
        this.clickListenerImpl = clickListener;
        this.deleteListenerImpl = deleteListener;
        this.files = files;
    }

    void add(File file) {
        files.add(file);
        notifyItemInserted(files.size());
    }

    int getSize() {
        return files.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listitem, parent, false);
        v.findViewById(R.id.delete).setOnClickListener(deleteListener);
        v.setOnClickListener(viewListener);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        File f = files.get(position);
        holder.name.setText(f.name);
        holder.size.setText(File.formatSize(f.size));
        if (f.isEncrypted) {
            holder.mime.setImageResource(R.drawable.ic_lock);
        } else {
            if (f.mime.contains("/")) {
                holder.mime.setContentDescription(f.mime.substring(0, f.mime.indexOf("/")));
            } else {
                holder.mime.setContentDescription(context.getString(R.string.unknown_file));
            }
            int icon;
            if (f.mime.startsWith("image")) {
                icon = R.drawable.ic_photo;
            } else if (f.mime.startsWith("video")) {
                icon = R.drawable.ic_movie;
            } else if (f.mime.startsWith("audio")) {
                icon = R.drawable.ic_sound;
            } else {
                icon = R.drawable.ic_file;
            }
            holder.mime.setImageResource(icon);
        }
        holder.delete.setTag(position);
        holder.card.setTag(position);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mime;
        private final TextView name, size;
        private final View delete;
        private final View card;

        public ViewHolder(final View itemView) {
            super(itemView);
            card = itemView;
            mime = (ImageView) itemView.findViewById(R.id.mime);
            name = (TextView) itemView.findViewById(R.id.name);
            size = (TextView) itemView.findViewById(R.id.size);
            delete = itemView.findViewById(R.id.delete);
        }
    }
}
