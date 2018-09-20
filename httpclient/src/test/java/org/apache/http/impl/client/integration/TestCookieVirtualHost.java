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
package org.apache.http.impl.client.integration;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import tink.org.apache.http.HttpException;
import tink.org.apache.http.HttpHost;
import tink.org.apache.http.HttpRequest;
import tink.org.apache.http.HttpResponse;
import tink.org.apache.http.HttpStatus;
import tink.org.apache.http.HttpVersion;
import tink.org.apache.http.client.CookieStore;
import tink.org.apache.http.client.methods.CloseableHttpResponse;
import tink.org.apache.http.client.methods.HttpGet;
import tink.org.apache.http.client.protocol.HttpClientContext;
import tink.org.apache.http.cookie.Cookie;
import tink.org.apache.http.impl.client.BasicCookieStore;
import tink.org.apache.http.localserver.LocalServerTestBase;
import tink.org.apache.http.message.BasicHeader;
import tink.org.apache.http.protocol.HttpContext;
import tink.org.apache.http.protocol.HttpRequestHandler;
import tink.org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * This class tests cookie matching when using Virtual Host.
 */
public class TestCookieVirtualHost extends LocalServerTestBase {

    @Test
    public void testCookieMatchingWithVirtualHosts() throws Exception {
        this.serverBootstrap.registerHandler("*", new HttpRequestHandler() {
            @Override
            public void handle(
                    final HttpRequest request,
                    final HttpResponse response,
                    final HttpContext context) throws HttpException, IOException {

                final int n = Integer.parseInt(request.getFirstHeader("X-Request").getValue());
                switch (n) {
                case 1:
                    // Assert Host is forwarded from URI
                    Assert.assertEquals("app.mydomain.fr", request
                            .getFirstHeader("Host").getValue());

                    response.setStatusLine(HttpVersion.HTTP_1_1,
                            HttpStatus.SC_OK);
                    // Respond with Set-Cookie on virtual host domain. This
                    // should be valid.
                    response.addHeader(new BasicHeader("Set-Cookie",
                            "name1=value1; domain=mydomain.fr; path=/"));
                    break;

                case 2:
                    // Assert Host is still forwarded from URI
                    Assert.assertEquals("app.mydomain.fr", request
                            .getFirstHeader("Host").getValue());

                    // We should get our cookie back.
                    Assert.assertNotNull("We must get a cookie header",
                            request.getFirstHeader("Cookie"));
                    response.setStatusLine(HttpVersion.HTTP_1_1,
                            HttpStatus.SC_OK);
                    break;

                case 3:
                    // Assert Host is forwarded from URI
                    Assert.assertEquals("app.mydomain.fr", request
                            .getFirstHeader("Host").getValue());

                    response.setStatusLine(HttpVersion.HTTP_1_1,
                            HttpStatus.SC_OK);
                    break;
                default:
                    Assert.fail("Unexpected value: " + n);
                    break;
                }
            }

        });

        final HttpHost target = start();

        final CookieStore cookieStore = new BasicCookieStore();
        final HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);

        // First request : retrieve a domain cookie from remote server.
        URI uri = new URI("http://app.mydomain.fr");
        HttpRequest httpRequest = new HttpGet(uri);
        httpRequest.addHeader("X-Request", "1");
        final CloseableHttpResponse response1 = this.httpclient.execute(target, httpRequest, context);
        try {
            EntityUtils.consume(response1.getEntity());
        } finally {
            response1.close();
        }

        // We should have one cookie set on domain.
        final List<Cookie> cookies = cookieStore.getCookies();
        Assert.assertNotNull(cookies);
        Assert.assertEquals(1, cookies.size());
        Assert.assertEquals("name1", cookies.get(0).getName());

        // Second request : send the cookie back.
        uri = new URI("http://app.mydomain.fr");
        httpRequest = new HttpGet(uri);
        httpRequest.addHeader("X-Request", "2");
        final CloseableHttpResponse response2 = this.httpclient.execute(target, httpRequest, context);
        try {
            EntityUtils.consume(response2.getEntity());
        } finally {
            response2.close();
        }

        // Third request : Host header
        uri = new URI("http://app.mydomain.fr");
        httpRequest = new HttpGet(uri);
        httpRequest.addHeader("X-Request", "3");
        final CloseableHttpResponse response3 = this.httpclient.execute(target, httpRequest, context);
        try {
            EntityUtils.consume(response3.getEntity());
        } finally {
            response3.close();
        }
    }

}
