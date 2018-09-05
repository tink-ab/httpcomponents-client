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

package se.tink.org.apache.http.impl.conn;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import se.tink.org.apache.http.HttpRequest;
import se.tink.org.apache.http.HttpResponse;
import se.tink.org.apache.http.annotation.Immutable;
import se.tink.org.apache.http.config.ConnectionConfig;
import se.tink.org.apache.http.conn.HttpConnectionFactory;
import se.tink.org.apache.http.conn.ManagedHttpClientConnection;
import se.tink.org.apache.http.conn.routing.HttpRoute;
import se.tink.org.apache.http.impl.io.DefaultHttpRequestWriterFactory;
import se.tink.org.apache.http.io.HttpMessageParserFactory;
import se.tink.org.apache.http.io.HttpMessageWriterFactory;
import se.tink.org.apache.http.conn.HttpConnectionFactory;
import se.tink.org.apache.http.conn.ManagedHttpClientConnection;
import se.tink.org.apache.http.conn.routing.HttpRoute;

/**
 * Factory for {@link ManagedHttpClientConnection} instances.
 * @since 4.3
 */
@Immutable
public class ManagedHttpClientConnectionFactory
        implements HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> {

    private static final AtomicLong COUNTER = new AtomicLong();

    public static final ManagedHttpClientConnectionFactory INSTANCE = new ManagedHttpClientConnectionFactory();

    private final Log log = LogFactory.getLog(DefaultManagedHttpClientConnection.class);
    private final Log headerlog = LogFactory.getLog("org.apache.http.headers");
    private final Log wirelog = LogFactory.getLog("org.apache.http.wire");

    private final HttpMessageWriterFactory<HttpRequest> requestWriterFactory;
    private final HttpMessageParserFactory<HttpResponse> responseParserFactory;

    public ManagedHttpClientConnectionFactory(
            final HttpMessageWriterFactory<HttpRequest> requestWriterFactory,
            final HttpMessageParserFactory<HttpResponse> responseParserFactory) {
        super();
        this.requestWriterFactory = requestWriterFactory != null ? requestWriterFactory :
            DefaultHttpRequestWriterFactory.INSTANCE;
        this.responseParserFactory = responseParserFactory != null ? responseParserFactory :
            DefaultHttpResponseParserFactory.INSTANCE;
    }

    public ManagedHttpClientConnectionFactory(
            final HttpMessageParserFactory<HttpResponse> responseParserFactory) {
        this(null, responseParserFactory);
    }

    public ManagedHttpClientConnectionFactory() {
        this(null, null);
    }

    public ManagedHttpClientConnection create(final HttpRoute route, final ConnectionConfig config) {
        final ConnectionConfig cconfig = config != null ? config : ConnectionConfig.DEFAULT;
        CharsetDecoder chardecoder = null;
        CharsetEncoder charencoder = null;
        final Charset charset = cconfig.getCharset();
        final CodingErrorAction malformedInputAction = cconfig.getMalformedInputAction() != null ?
                cconfig.getMalformedInputAction() : CodingErrorAction.REPORT;
        final CodingErrorAction unmappableInputAction = cconfig.getUnmappableInputAction() != null ?
                cconfig.getUnmappableInputAction() : CodingErrorAction.REPORT;
        if (charset != null) {
            chardecoder = charset.newDecoder();
            chardecoder.onMalformedInput(malformedInputAction);
            chardecoder.onUnmappableCharacter(unmappableInputAction);
            charencoder = charset.newEncoder();
            charencoder.onMalformedInput(malformedInputAction);
            charencoder.onUnmappableCharacter(unmappableInputAction);
        }
        final String id = "http-outgoing-" + Long.toString(COUNTER.getAndIncrement());
        return new LoggingManagedHttpClientConnection(
                id,
                log,
                headerlog,
                wirelog,
                cconfig.getBufferSize(),
                cconfig.getFragmentSizeHint(),
                chardecoder,
                charencoder,
                cconfig.getMessageConstraints(),
                null,
                null,
                requestWriterFactory,
                responseParserFactory);
    }

}
