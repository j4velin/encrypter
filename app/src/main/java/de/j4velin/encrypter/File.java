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

import android.net.Uri;

/**
 * A class representing an encrypted or a plaintext file.
 * If the object represents an encrypted file, the name, mime type and size information are those of
 * the originating plaintext file
 */
class File {

    /**
     * The original display name
     */
    final String name;
    /**
     * The original mime type
     */
    final String mime;
    /**
     * The uri of the file
     */
    final Uri uri;
    /**
     * The original file size
     */
    final int size;
    /**
     * The id of the entry in the database or -1, if no such exists (yet)
     */
    final int id;

    protected File(final int id, final String name, final String mime, final Uri uri,
                   final int size) {
        this.id = id;
        this.name = name;
        this.mime = mime;
        this.uri = uri;
        this.size = size;
    }

    @Override
    public String toString() {
        return id + "," + name + "," + mime + "," + size + "," + uri;
    }
}
