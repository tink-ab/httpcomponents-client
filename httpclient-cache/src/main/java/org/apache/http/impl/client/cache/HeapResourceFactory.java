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
package org.apache.http.impl.client.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import tink.org.apache.http.annotation.Contract;
import tink.org.apache.http.annotation.ThreadingBehavior;
import tink.org.apache.http.client.cache.InputLimit;
import tink.org.apache.http.client.cache.Resource;
import tink.org.apache.http.client.cache.ResourceFactory;

/**
 * Generates {@link Resource} instances stored entirely in heap.
 *
 * @since 4.1
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class HeapResourceFactory implements ResourceFactory {

    @Override
    public Resource generate(
            final String requestId,
            final InputStream instream,
            final InputLimit limit) throws IOException {
        final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        final byte[] buf = new byte[2048];
        long total = 0;
        int l;
        while ((l = instream.read(buf)) != -1) {
            outstream.write(buf, 0, l);
            total += l;
            if (limit != null && total > limit.getValue()) {
                limit.reached();
                break;
            }
        }
        return createResource(outstream.toByteArray());
    }

    @Override
    public Resource copy(
            final String requestId,
            final Resource resource) throws IOException {
        final byte[] body;
        if (resource instanceof HeapResource) {
            body = ((HeapResource) resource).getByteArray();
        } else {
            final ByteArrayOutputStream outstream = new ByteArrayOutputStream();
            IOUtils.copyAndClose(resource.getInputStream(), outstream);
            body = outstream.toByteArray();
        }
        return createResource(body);
    }

    Resource createResource(final byte[] buf) {
        return new HeapResource(buf);
    }

}
