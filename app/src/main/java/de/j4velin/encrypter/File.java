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

class File {

    final String name;
    final String mime;
    final Uri uri;
    final int size;

    protected File(final String name, final String mime, final Uri uri, final int size) {
        this.name = name;
        this.mime = mime;
        this.uri = uri;
        this.size = size;
    }

    @Override
    public String toString() {
        return name + "," + mime + "," + size + "," + uri;
    }
}
