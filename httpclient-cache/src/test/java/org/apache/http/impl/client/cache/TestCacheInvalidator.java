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

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import tink.org.apache.http.Header;
import tink.org.apache.http.HttpEntityEnclosingRequest;
import tink.org.apache.http.HttpHost;
import tink.org.apache.http.HttpRequest;
import tink.org.apache.http.HttpResponse;
import tink.org.apache.http.HttpStatus;
import tink.org.apache.http.HttpVersion;
import tink.org.apache.http.ProtocolVersion;
import tink.org.apache.http.client.cache.HttpCacheEntry;
import tink.org.apache.http.client.cache.HttpCacheStorage;
import tink.org.apache.http.client.utils.DateUtils;
import tink.org.apache.http.message.BasicHeader;
import tink.org.apache.http.message.BasicHttpEntityEnclosingRequest;
import tink.org.apache.http.message.BasicHttpRequest;
import tink.org.apache.http.message.BasicHttpResponse;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestCacheInvalidator {

    private static final ProtocolVersion HTTP_1_1 = new ProtocolVersion("HTTP", 1, 1);

    private CacheInvalidator impl;
    private HttpCacheStorage mockStorage;
    private HttpHost host;
    private CacheKeyGenerator cacheKeyGenerator;
    private HttpCacheEntry mockEntry;
    private HttpRequest request;
    private HttpResponse response;

    private Date now;
    private Date tenSecondsAgo;

    @Before
    public void setUp() {
        now = new Date();
        tenSecondsAgo = new Date(now.getTime() - 10 * 1000L);

        host = new HttpHost("foo.example.com");
        mockStorage = createNiceMock(HttpCacheStorage.class);
        cacheKeyGenerator = new CacheKeyGenerator();
        mockEntry = createNiceMock(HttpCacheEntry.class);
        request = HttpTestUtils.makeDefaultRequest();
        response = HttpTestUtils.make200Response();

        impl = new CacheInvalidator(cacheKeyGenerator, mockStorage);
    }

    private void replayMocks() {
        replay(mockStorage);
        replay(mockEntry);
    }

    private void verifyMocks() {
        verify(mockStorage);
        verify(mockEntry);
    }

    // Tests
    @Test
    public void testInvalidatesRequestsThatArentGETorHEAD() throws Exception {
        request = new BasicHttpRequest("POST","/path", HTTP_1_1);
        final String theUri = "http://foo.example.com:80/path";
        final Map<String,String> variantMap = new HashMap<String,String>();
        cacheEntryHasVariantMap(variantMap);

        cacheReturnsEntryForUri(theUri);
        entryIsRemoved(theUri);
        replayMocks();

        impl.flushInvalidatedCacheEntries(host, request);

        verifyMocks();
    }

    @Test
    public void testInvalidatesUrisInContentLocationHeadersOnPUTs() throws Exception {
        final HttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("PUT","/",HTTP_1_1);
        request.setEntity(HttpTestUtils.makeBody(128));
        request.setHeader("Content-Length","128");

        final String contentLocation = "http://foo.example.com/content";
        request.setHeader("Content-Location", contentLocation);

        final String theUri = "http://foo.example.com:80/";
        cacheEntryHasVariantMap(new HashMap<String,String>());

        cacheReturnsEntryForUri(theUri);
        entryIsRemoved(theUri);
        entryIsRemoved("http://foo.example.com:80/content");

        replayMocks();

        impl.flushInvalidatedCacheEntries(host, request);

        verifyMocks();
    }

    @Test
    public void testInvalidatesUrisInLocationHeadersOnPUTs() throws Exception {
        final HttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("PUT","/",HTTP_1_1);
        request.setEntity(HttpTestUtils.makeBody(128));
        request.setHeader("Content-Length","128");

        final String contentLocation = "http://foo.example.com/content";
        request.setHeader("Location",contentLocation);

        final String theUri = "http://foo.example.com:80/";
        cacheEntryHasVariantMap(new HashMap<String,String>());

        cacheReturnsEntryForUri(theUri);
        entryIsRemoved(theUri);
        entryIsRemoved(cacheKeyGenerator.canonicalizeUri(contentLocation));

        replayMocks();

        impl.flushInvalidatedCacheEntries(host, request);

        verifyMocks();
    }

    @Test
    public void testInvalidatesRelativeUrisInContentLocationHeadersOnPUTs() throws Exception {
        final HttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("PUT","/",HTTP_1_1);
        request.setEntity(HttpTestUtils.makeBody(128));
        request.setHeader("Content-Length","128");

        final String relativePath = "/content";
        request.setHeader("Content-Location",relativePath);

        final String theUri = "http://foo.example.com:80/";
        cacheEntryHasVariantMap(new HashMap<String,String>());

        cacheReturnsEntryForUri(theUri);
        entryIsRemoved(theUri);
        entryIsRemoved("http://foo.example.com:80/content");

        replayMocks();

        impl.flushInvalidatedCacheEntries(host, request);

        verifyMocks();
    }

    @Test
    public void testDoesNotInvalidateUrisInContentLocationHeadersOnPUTsToDifferentHosts() throws Exception {
        final HttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("PUT","/",HTTP_1_1);
        request.setEntity(HttpTestUtils.makeBody(128));
        request.setHeader("Content-Length","128");

        final String contentLocation = "http://bar.example.com/content";
        request.setHeader("Content-Location",contentLocation);

        final String theUri = "http://foo.example.com:80/";
        cacheEntryHasVariantMap(new HashMap<String,String>());

        cacheReturnsEntryForUri(theUri);
        entryIsRemoved(theUri);

        replayMocks();

        impl.flushInvalidatedCacheEntries(host, request);

        verifyMocks();
    }

    @Test
    public void testDoesNotInvalidateGETRequest() throws Exception {
        request = new BasicHttpRequest("GET","/",HTTP_1_1);
        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request);
        verifyMocks();
    }

    @Test
    public void testDoesNotInvalidateHEADRequest() throws Exception {
        request = new BasicHttpRequest("HEAD","/",HTTP_1_1);
        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request);
        verifyMocks();
    }

    @Test
    public void testDoesNotInvalidateRequestsWithClientCacheControlHeaders() throws Exception {
        request = new BasicHttpRequest("GET","/",HTTP_1_1);
        request.setHeader("Cache-Control","no-cache");
        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request);
        verifyMocks();
    }

    @Test
    public void testDoesNotInvalidateRequestsWithClientPragmaHeaders() throws Exception {
        request = new BasicHttpRequest("GET","/",HTTP_1_1);
        request.setHeader("Pragma","no-cache");
        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request);
        verifyMocks();
    }

    @Test
    public void testVariantURIsAreFlushedAlso() throws Exception {
        request = new BasicHttpRequest("POST","/",HTTP_1_1);
        final String theUri = "http://foo.example.com:80/";
        final String variantUri = "theVariantURI";

        final Map<String,String> mapOfURIs = new HashMap<String,String>();
        mapOfURIs.put(variantUri,variantUri);

        cacheReturnsEntryForUri(theUri);
        cacheEntryHasVariantMap(mapOfURIs);

        entryIsRemoved(variantUri);
        entryIsRemoved(theUri);

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request);
        verifyMocks();
    }

    @Test
    public void testCacheFlushException() throws Exception {
        request = new BasicHttpRequest("POST","/",HTTP_1_1);
        final String theURI = "http://foo.example.com:80/";

        cacheReturnsExceptionForUri(theURI);

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request);
        verifyMocks();
    }

    @Test
    public void doesNotFlushForResponsesWithoutContentLocation()
            throws Exception {
        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }

    @Test
    public void flushesEntryIfFresherAndSpecifiedByContentLocation()
            throws Exception {
        response.setHeader("ETag","\"new-etag\"");
        response.setHeader("Date", DateUtils.formatDate(now));
        final String theURI = "http://foo.example.com:80/bar";
        response.setHeader("Content-Location", theURI);

        final HttpCacheEntry entry = HttpTestUtils.makeCacheEntry(new Header[] {
           new BasicHeader("Date", DateUtils.formatDate(tenSecondsAgo)),
           new BasicHeader("ETag", "\"old-etag\"")
        });

        expect(mockStorage.getEntry(theURI)).andReturn(entry).anyTimes();
        mockStorage.removeEntry(theURI);

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }

    @Test
    public void flushesEntryIfFresherAndSpecifiedByLocation()
            throws Exception {
        response.setStatusCode(201);
        response.setHeader("ETag","\"new-etag\"");
        response.setHeader("Date", DateUtils.formatDate(now));
        final String theURI = "http://foo.example.com:80/bar";
        response.setHeader("Location", theURI);

        final HttpCacheEntry entry = HttpTestUtils.makeCacheEntry(new Header[] {
           new BasicHeader("Date", DateUtils.formatDate(tenSecondsAgo)),
           new BasicHeader("ETag", "\"old-etag\"")
        });

        expect(mockStorage.getEntry(theURI)).andReturn(entry).anyTimes();
        mockStorage.removeEntry(theURI);

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }

    @Test
    public void doesNotFlushEntryForUnsuccessfulResponse()
            throws Exception {
        response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST, "Bad Request");
        response.setHeader("ETag","\"new-etag\"");
        response.setHeader("Date", DateUtils.formatDate(now));
        final String theURI = "http://foo.example.com:80/bar";
        response.setHeader("Content-Location", theURI);

        final HttpCacheEntry entry = HttpTestUtils.makeCacheEntry(new Header[] {
           new BasicHeader("Date", DateUtils.formatDate(tenSecondsAgo)),
           new BasicHeader("ETag", "\"old-etag\"")
        });

        expect(mockStorage.getEntry(theURI)).andReturn(entry).anyTimes();
        mockStorage.removeEntry(theURI);
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() {
                Assert.fail();
                return null;
              }
          }).anyTimes();

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }

    @Test
    public void flushesEntryIfFresherAndSpecifiedByNonCanonicalContentLocation()
            throws Exception {
        response.setHeader("ETag","\"new-etag\"");
        response.setHeader("Date", DateUtils.formatDate(now));
        final String cacheKey = "http://foo.example.com:80/bar";
        response.setHeader("Content-Location", "http://foo.example.com/bar");

        final HttpCacheEntry entry = HttpTestUtils.makeCacheEntry(new Header[] {
           new BasicHeader("Date", DateUtils.formatDate(tenSecondsAgo)),
           new BasicHeader("ETag", "\"old-etag\"")
        });

        expect(mockStorage.getEntry(cacheKey)).andReturn(entry).anyTimes();
        mockStorage.removeEntry(cacheKey);

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }

    @Test
    public void flushesEntryIfFresherAndSpecifiedByRelativeContentLocation()
            throws Exception {
        response.setHeader("ETag","\"new-etag\"");
        response.setHeader("Date", DateUtils.formatDate(now));
        final String cacheKey = "http://foo.example.com:80/bar";
        response.setHeader("Content-Location", "/bar");

        final HttpCacheEntry entry = HttpTestUtils.makeCacheEntry(new Header[] {
           new BasicHeader("Date", DateUtils.formatDate(tenSecondsAgo)),
           new BasicHeader("ETag", "\"old-etag\"")
        });

        expect(mockStorage.getEntry(cacheKey)).andReturn(entry).anyTimes();
        mockStorage.removeEntry(cacheKey);

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }

    @Test
    public void doesNotFlushEntryIfContentLocationFromDifferentHost()
            throws Exception {
        response.setHeader("ETag","\"new-etag\"");
        response.setHeader("Date", DateUtils.formatDate(now));
        final String cacheKey = "http://baz.example.com:80/bar";
        response.setHeader("Content-Location", cacheKey);

        final HttpCacheEntry entry = HttpTestUtils.makeCacheEntry(new Header[] {
           new BasicHeader("Date", DateUtils.formatDate(tenSecondsAgo)),
           new BasicHeader("ETag", "\"old-etag\"")
        });

        expect(mockStorage.getEntry(cacheKey)).andReturn(entry).anyTimes();
        mockStorage.removeEntry(cacheKey);
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() {
                Assert.fail();
                return null;
              }
          }).anyTimes();

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }



    @Test
    public void doesNotFlushEntrySpecifiedByContentLocationIfEtagsMatch()
            throws Exception {
        response.setHeader("ETag","\"same-etag\"");
        response.setHeader("Date", DateUtils.formatDate(now));
        final String theURI = "http://foo.example.com:80/bar";
        response.setHeader("Content-Location", theURI);

        final HttpCacheEntry entry = HttpTestUtils.makeCacheEntry(new Header[] {
           new BasicHeader("Date", DateUtils.formatDate(tenSecondsAgo)),
           new BasicHeader("ETag", "\"same-etag\"")
        });

        expect(mockStorage.getEntry(theURI)).andReturn(entry).anyTimes();
        mockStorage.removeEntry(theURI);
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() {
                Assert.fail();
                return null;
              }
          }).anyTimes();

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }

    @Test
    public void doesNotFlushEntrySpecifiedByContentLocationIfOlder()
            throws Exception {
        response.setHeader("ETag","\"new-etag\"");
        response.setHeader("Date", DateUtils.formatDate(tenSecondsAgo));
        final String theURI = "http://foo.example.com:80/bar";
        response.setHeader("Content-Location", theURI);

        final HttpCacheEntry entry = HttpTestUtils.makeCacheEntry(new Header[] {
           new BasicHeader("Date", DateUtils.formatDate(now)),
           new BasicHeader("ETag", "\"old-etag\"")
        });

        expect(mockStorage.getEntry(theURI)).andReturn(entry).anyTimes();
        mockStorage.removeEntry(theURI);
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() {
                Assert.fail();
                return null;
              }
          }).anyTimes();

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }

    @Test
    public void doesNotFlushEntryIfNotInCache()
            throws Exception {
        response.setHeader("ETag","\"new-etag\"");
        response.setHeader("Date", DateUtils.formatDate(now));
        final String theURI = "http://foo.example.com:80/bar";
        response.setHeader("Content-Location", theURI);

        expect(mockStorage.getEntry(theURI)).andReturn(null).anyTimes();
        mockStorage.removeEntry(theURI);
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() {
                Assert.fail();
                return null;
              }
          }).anyTimes();

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }

    @Test
    public void doesNotFlushEntrySpecifiedByContentLocationIfResponseHasNoEtag()
            throws Exception {
        response.removeHeaders("ETag");
        response.setHeader("Date", DateUtils.formatDate(now));
        final String theURI = "http://foo.example.com:80/bar";
        response.setHeader("Content-Location", theURI);

        final HttpCacheEntry entry = HttpTestUtils.makeCacheEntry(new Header[] {
           new BasicHeader("Date", DateUtils.formatDate(tenSecondsAgo)),
           new BasicHeader("ETag", "\"old-etag\"")
        });

        expect(mockStorage.getEntry(theURI)).andReturn(entry).anyTimes();
        mockStorage.removeEntry(theURI);
        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() {
                Assert.fail();
                return null;
              }
          }).anyTimes();

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }

    @Test
    public void doesNotFlushEntrySpecifiedByContentLocationIfEntryHasNoEtag()
            throws Exception {
        response.setHeader("ETag", "\"some-etag\"");
        response.setHeader("Date", DateUtils.formatDate(now));
        final String theURI = "http://foo.example.com:80/bar";
        response.setHeader("Content-Location", theURI);

        final HttpCacheEntry entry = HttpTestUtils.makeCacheEntry(new Header[] {
           new BasicHeader("Date", DateUtils.formatDate(tenSecondsAgo)),
        });

        expect(mockStorage.getEntry(theURI)).andReturn(entry).anyTimes();

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }

    @Test
    public void flushesEntrySpecifiedByContentLocationIfResponseHasNoDate()
            throws Exception {
        response.setHeader("ETag", "\"new-etag\"");
        response.removeHeaders("Date");
        final String theURI = "http://foo.example.com:80/bar";
        response.setHeader("Content-Location", theURI);

        final HttpCacheEntry entry = HttpTestUtils.makeCacheEntry(new Header[] {
                new BasicHeader("ETag", "\"old-etag\""),
                new BasicHeader("Date", DateUtils.formatDate(tenSecondsAgo)),
        });

        expect(mockStorage.getEntry(theURI)).andReturn(entry).anyTimes();
        mockStorage.removeEntry(theURI);

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }

    @Test
    public void flushesEntrySpecifiedByContentLocationIfEntryHasNoDate()
            throws Exception {
        response.setHeader("ETag","\"new-etag\"");
        response.setHeader("Date", DateUtils.formatDate(now));
        final String theURI = "http://foo.example.com:80/bar";
        response.setHeader("Content-Location", theURI);

        final HttpCacheEntry entry = HttpTestUtils.makeCacheEntry(new Header[] {
           new BasicHeader("ETag", "\"old-etag\"")
        });

        expect(mockStorage.getEntry(theURI)).andReturn(entry).anyTimes();
        mockStorage.removeEntry(theURI);

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }

    @Test
    public void flushesEntrySpecifiedByContentLocationIfResponseHasMalformedDate()
            throws Exception {
        response.setHeader("ETag","\"new-etag\"");
        response.setHeader("Date", "blarg");
        final String theURI = "http://foo.example.com:80/bar";
        response.setHeader("Content-Location", theURI);

        final HttpCacheEntry entry = HttpTestUtils.makeCacheEntry(new Header[] {
                new BasicHeader("ETag", "\"old-etag\""),
                new BasicHeader("Date", DateUtils.formatDate(tenSecondsAgo))
        });

        expect(mockStorage.getEntry(theURI)).andReturn(entry).anyTimes();
        mockStorage.removeEntry(theURI);

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }

    @Test
    public void flushesEntrySpecifiedByContentLocationIfEntryHasMalformedDate()
            throws Exception {
        response.setHeader("ETag","\"new-etag\"");
        response.setHeader("Date", DateUtils.formatDate(now));
        final String theURI = "http://foo.example.com:80/bar";
        response.setHeader("Content-Location", theURI);

        final HttpCacheEntry entry = HttpTestUtils.makeCacheEntry(new Header[] {
                new BasicHeader("ETag", "\"old-etag\""),
                new BasicHeader("Date", "foo")
        });

        expect(mockStorage.getEntry(theURI)).andReturn(entry).anyTimes();
        mockStorage.removeEntry(theURI);

        replayMocks();
        impl.flushInvalidatedCacheEntries(host, request, response);
        verifyMocks();
    }


    // Expectations
    private void cacheEntryHasVariantMap(final Map<String,String> variantMap) {
        expect(mockEntry.getVariantMap()).andReturn(variantMap);
    }

    private void cacheReturnsEntryForUri(final String theUri) throws IOException {
        expect(mockStorage.getEntry(theUri)).andReturn(mockEntry);
    }

    private void cacheReturnsExceptionForUri(final String theUri) throws IOException {
        expect(mockStorage.getEntry(theUri)).andThrow(
                new IOException("TOTAL FAIL"));
    }

    private void entryIsRemoved(final String theUri) throws IOException {
        mockStorage.removeEntry(theUri);
    }

}
