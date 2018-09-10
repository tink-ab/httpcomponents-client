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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import tink.org.apache.http.Consts;
import tink.org.apache.http.HttpEntity;
import tink.org.apache.http.HttpException;
import tink.org.apache.http.HttpRequest;
import tink.org.apache.http.HttpResponse;
import tink.org.apache.http.HttpResponseInterceptor;
import tink.org.apache.http.HttpStatus;
import tink.org.apache.http.auth.AUTH;
import tink.org.apache.http.auth.AuthScheme;
import tink.org.apache.http.auth.AuthSchemeProvider;
import tink.org.apache.http.auth.AuthScope;
import tink.org.apache.http.auth.Credentials;
import tink.org.apache.http.auth.UsernamePasswordCredentials;
import tink.org.apache.http.client.CredentialsProvider;
import tink.org.apache.http.client.config.RequestConfig;
import tink.org.apache.http.client.methods.HttpGet;
import tink.org.apache.http.client.protocol.HttpClientContext;
import tink.org.apache.http.config.Registry;
import tink.org.apache.http.config.RegistryBuilder;
import tink.org.apache.http.entity.StringEntity;
import tink.org.apache.http.impl.auth.BasicScheme;
import tink.org.apache.http.impl.auth.BasicSchemeFactory;
import tink.org.apache.http.impl.client.HttpClients;
import tink.org.apache.http.impl.client.TargetAuthenticationStrategy;
import tink.org.apache.http.localserver.LocalTestServer;
import tink.org.apache.http.localserver.RequestBasicAuth;
import tink.org.apache.http.protocol.HttpContext;
import tink.org.apache.http.protocol.HttpProcessor;
import tink.org.apache.http.protocol.HttpProcessorBuilder;
import tink.org.apache.http.protocol.HttpRequestHandler;
import tink.org.apache.http.protocol.ResponseConnControl;
import tink.org.apache.http.protocol.ResponseContent;
import tink.org.apache.http.protocol.ResponseDate;
import tink.org.apache.http.protocol.ResponseServer;
import tink.org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestClientReauthentication extends IntegrationTestBase {

    public class ResponseBasicUnauthorized implements HttpResponseInterceptor {

        public void process(
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                response.addHeader(AUTH.WWW_AUTH, "MyBasic realm=\"test realm\"");
            }
        }

    }

    @Before
    public void setUp() throws Exception {
        final HttpProcessor httpproc = HttpProcessorBuilder.create()
            .add(new ResponseDate())
            .add(new ResponseServer(LocalTestServer.ORIGIN))
            .add(new ResponseContent())
            .add(new ResponseConnControl())
            .add(new RequestBasicAuth())
            .add(new ResponseBasicUnauthorized()).build();

        this.localServer = new LocalTestServer(httpproc, null);
        startServer();
    }

    static class AuthHandler implements HttpRequestHandler {

        private final AtomicLong count = new AtomicLong(0);

        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {
            final String creds = (String) context.getAttribute("creds");
            if (creds == null || !creds.equals("test:test")) {
                response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
            } else {
                // Make client re-authenticate on each fourth request
                if (this.count.incrementAndGet() % 4 == 0) {
                    response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
                } else {
                    response.setStatusCode(HttpStatus.SC_OK);
                    final StringEntity entity = new StringEntity("success", Consts.ASCII);
                    response.setEntity(entity);
                }
            }
        }

    }

    static class TestCredentialsProvider implements CredentialsProvider {

        private final Credentials creds;
        private AuthScope authscope;

        TestCredentialsProvider(final Credentials creds) {
            super();
            this.creds = creds;
        }

        public void clear() {
        }

        public Credentials getCredentials(final AuthScope authscope) {
            this.authscope = authscope;
            return this.creds;
        }

        public void setCredentials(final AuthScope authscope, final Credentials credentials) {
        }

        public AuthScope getAuthScope() {
            return this.authscope;
        }

    }

    @Test
    public void testBasicAuthenticationSuccess() throws Exception {
        this.localServer.register("*", new AuthHandler());

        final BasicSchemeFactory myBasicAuthSchemeFactory = new BasicSchemeFactory() {

            @Override
            public AuthScheme create(final HttpContext context) {
                return new BasicScheme() {

                    @Override
                    public String getSchemeName() {
                        return "MyBasic";
                    }

                };
            }

        };

        final TargetAuthenticationStrategy myAuthStrategy = new TargetAuthenticationStrategy() {

            @Override
            protected boolean isCachable(final AuthScheme authScheme) {
                return "MyBasic".equalsIgnoreCase(authScheme.getSchemeName());
            }

        };

        final TestCredentialsProvider credsProvider = new TestCredentialsProvider(
                new UsernamePasswordCredentials("test", "test"));

        final RequestConfig config = RequestConfig.custom()
            .setTargetPreferredAuthSchemes(Arrays.asList("MyBasic"))
            .build();
        final Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create()
            .register("MyBasic", myBasicAuthSchemeFactory)
            .build();
        this.httpclient = HttpClients.custom()
            .setDefaultAuthSchemeRegistry(authSchemeRegistry)
            .setTargetAuthenticationStrategy(myAuthStrategy)
            .setDefaultCredentialsProvider(credsProvider)
            .build();

        final HttpClientContext context = HttpClientContext.create();
        for (int i = 0; i < 10; i++) {
            final HttpGet httpget = new HttpGet("/");
            httpget.setConfig(config);
            final HttpResponse response = this.httpclient.execute(getServerHttp(), httpget, context);
            final HttpEntity entity = response.getEntity();
            Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertNotNull(entity);
            EntityUtils.consume(entity);
        }
    }

}
