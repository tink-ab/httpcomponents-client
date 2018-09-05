/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.client.fluent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import se.tink.org.apache.http.entity.AbstractHttpEntity;
import se.tink.org.apache.http.entity.ContentType;
import se.tink.org.apache.http.util.Args;

class InternalFileEntity extends AbstractHttpEntity implements Cloneable {

    private final File file;

    public InternalFileEntity(final File file, final ContentType contentType) {
        super();
        this.file = Args.notNull(file, "File");
        if (contentType != null) {
            setContentType(contentType.toString());
        }
    }

    public boolean isRepeatable() {
        return true;
    }

    public long getContentLength() {
        return this.file.length();
    }

    public InputStream getContent() throws IOException {
        return new FileInputStream(this.file);
    }

    public void writeTo(final OutputStream outstream) throws IOException {
        Args.notNull(outstream, "Output stream");
        final InputStream instream = new FileInputStream(this.file);
        try {
            final byte[] tmp = new byte[4096];
            int l;
            while ((l = instream.read(tmp)) != -1) {
                outstream.write(tmp, 0, l);
            }
            outstream.flush();
        } finally {
            instream.close();
        }
    }

    public boolean isStreaming() {
        return false;
    }

}
