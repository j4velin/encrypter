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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

public class Database extends SQLiteOpenHelper {

    private final static String DB_NAME = "db";
    private final static int DB_VERSION = 1;

    static class EncryptedFilesContract implements BaseColumns {
        private final static String TABLE_NAME = "encrypted_files";
        private final static String COLUMN_FILENAME = "name";
        private final static String COLUMN_MIME = "mime";
        private final static String COLUMN_URI = "uri";
        private final static String COLUMN_SIZE = "size";
        private final static String[] ALL_COLUMNS =
                new String[]{_ID, COLUMN_FILENAME, COLUMN_MIME, COLUMN_URI, COLUMN_SIZE};
    }

    public Database(final Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + EncryptedFilesContract.TABLE_NAME + " (" +
                EncryptedFilesContract._ID + " INTEGER PRIMARY KEY," +
                EncryptedFilesContract.COLUMN_FILENAME +
                " TEXT, " + EncryptedFilesContract.COLUMN_MIME + " TEXT, " +
                EncryptedFilesContract.COLUMN_URI +
                " TEXT, " + EncryptedFilesContract.COLUMN_SIZE + " INTEGER)");
    }

    /**
     * Adds the given file to the database of encrypted files
     *
     * @param file the encrypted file
     * @return the entry id
     */
    public long addFile(final File file) {
        ContentValues values = new ContentValues();
        values.put(EncryptedFilesContract.COLUMN_FILENAME, file.name);
        values.put(EncryptedFilesContract.COLUMN_MIME, file.mime);
        values.put(EncryptedFilesContract.COLUMN_URI, file.uri.toString());
        values.put(EncryptedFilesContract.COLUMN_SIZE, file.size);
        return getWritableDatabase().insert(EncryptedFilesContract.TABLE_NAME, null, values);
    }

    /**
     * Deletes a file from the database
     *
     * @param id the id of the entry to delete
     */
    public void deleteFile(final int id) {
        getWritableDatabase()
                .delete(EncryptedFilesContract.TABLE_NAME, EncryptedFilesContract._ID + " = ?",
                        new String[]{String.valueOf(id)});
    }

    /**
     * Gets all encrypted files in the database
     *
     * @return the list of encrypted files
     */
    public List<File> getFiles() {
        try (Cursor c = getReadableDatabase()
                .query(EncryptedFilesContract.TABLE_NAME, EncryptedFilesContract.ALL_COLUMNS, null,
                        null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int indexId = c.getColumnIndex(EncryptedFilesContract._ID);
                int indexName = c.getColumnIndex(EncryptedFilesContract.COLUMN_FILENAME);
                int indexMime = c.getColumnIndex(EncryptedFilesContract.COLUMN_MIME);
                int indexUri = c.getColumnIndex(EncryptedFilesContract.COLUMN_URI);
                int indexSize = c.getColumnIndex(EncryptedFilesContract.COLUMN_SIZE);
                List<File> re = new ArrayList<>(c.getCount());
                while (!c.isAfterLast()) {
                    re.add(new File(c.getInt(indexId), c.getString(indexName),
                            c.getString(indexMime), Uri.parse(c.getString(indexUri)),
                            c.getInt(indexSize)));
                    c.moveToNext();
                }
                return re;
            }
        }
        return new ArrayList<>(0);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, int from, int to) {

    }
}
