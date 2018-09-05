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

package se.tink.org.apache.http.impl.client;

import java.io.IOException;
import java.net.Socket;

import se.tink.org.apache.http.ConnectionReuseStrategy;
import se.tink.org.apache.http.HttpEntity;
import se.tink.org.apache.http.HttpException;
import se.tink.org.apache.http.HttpHost;
import se.tink.org.apache.http.HttpRequest;
import se.tink.org.apache.http.HttpResponse;
import se.tink.org.apache.http.HttpVersion;
import se.tink.org.apache.http.auth.AUTH;
import se.tink.org.apache.http.auth.AuthSchemeRegistry;
import se.tink.org.apache.http.auth.AuthScope;
import se.tink.org.apache.http.auth.AuthState;
import se.tink.org.apache.http.auth.Credentials;
import se.tink.org.apache.http.client.config.AuthSchemes;
import se.tink.org.apache.http.client.config.RequestConfig;
import se.tink.org.apache.http.client.params.HttpClientParamConfig;
import se.tink.org.apache.http.client.protocol.HttpClientContext;
import se.tink.org.apache.http.client.protocol.RequestClientConnControl;
import se.tink.org.apache.http.config.ConnectionConfig;
import se.tink.org.apache.http.conn.HttpConnectionFactory;
import se.tink.org.apache.http.conn.ManagedHttpClientConnection;
import se.tink.org.apache.http.conn.routing.HttpRoute;
import se.tink.org.apache.http.conn.routing.RouteInfo.LayerType;
import se.tink.org.apache.http.conn.routing.RouteInfo.TunnelType;
import se.tink.org.apache.http.entity.BufferedHttpEntity;
import se.tink.org.apache.http.impl.DefaultConnectionReuseStrategy;
import se.tink.org.apache.http.impl.auth.BasicSchemeFactory;
import se.tink.org.apache.http.impl.auth.DigestSchemeFactory;
import se.tink.org.apache.http.impl.auth.HttpAuthenticator;
import se.tink.org.apache.http.impl.auth.KerberosSchemeFactory;
import se.tink.org.apache.http.impl.auth.NTLMSchemeFactory;
import se.tink.org.apache.http.impl.auth.SPNegoSchemeFactory;
import se.tink.org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import se.tink.org.apache.http.impl.execchain.TunnelRefusedException;
import se.tink.org.apache.http.message.BasicHttpRequest;
import se.tink.org.apache.http.params.BasicHttpParams;
import se.tink.org.apache.http.params.HttpParamConfig;
import se.tink.org.apache.http.params.HttpParams;
import se.tink.org.apache.http.protocol.BasicHttpContext;
import se.tink.org.apache.http.protocol.HttpContext;
import se.tink.org.apache.http.protocol.HttpCoreContext;
import se.tink.org.apache.http.protocol.HttpProcessor;
import se.tink.org.apache.http.protocol.HttpRequestExecutor;
import se.tink.org.apache.http.protocol.ImmutableHttpProcessor;
import se.tink.org.apache.http.protocol.RequestTargetHost;
import se.tink.org.apache.http.protocol.RequestUserAgent;
import se.tink.org.apache.http.util.Args;
import se.tink.org.apache.http.util.EntityUtils;
import se.tink.org.apache.http.auth.AUTH;
import se.tink.org.apache.http.auth.AuthSchemeRegistry;
import se.tink.org.apache.http.auth.AuthScope;
import se.tink.org.apache.http.auth.AuthState;
import se.tink.org.apache.http.auth.Credentials;
import se.tink.org.apache.http.client.config.AuthSchemes;
import se.tink.org.apache.http.client.config.RequestConfig;
import se.tink.org.apache.http.client.params.HttpClientParamConfig;
import se.tink.org.apache.http.client.protocol.HttpClientContext;
import se.tink.org.apache.http.client.protocol.RequestClientConnControl;
import se.tink.org.apache.http.conn.HttpConnectionFactory;
import se.tink.org.apache.http.conn.ManagedHttpClientConnection;
import se.tink.org.apache.http.conn.routing.HttpRoute;
import se.tink.org.apache.http.conn.routing.RouteInfo;
import se.tink.org.apache.http.impl.auth.BasicSchemeFactory;
import se.tink.org.apache.http.impl.auth.DigestSchemeFactory;
import se.tink.org.apache.http.impl.auth.HttpAuthenticator;
import se.tink.org.apache.http.impl.auth.KerberosSchemeFactory;
import se.tink.org.apache.http.impl.auth.NTLMSchemeFactory;
import se.tink.org.apache.http.impl.auth.SPNegoSchemeFactory;
import se.tink.org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import se.tink.org.apache.http.impl.execchain.TunnelRefusedException;

/**
 * ProxyClient can be used to establish a tunnel via an HTTP proxy.
 */
@SuppressWarnings("deprecation")
public class ProxyClient {

    private final HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory;
    private final ConnectionConfig connectionConfig;
    private final RequestConfig requestConfig;
    private final HttpProcessor httpProcessor;
    private final HttpRequestExecutor requestExec;
    private final ProxyAuthenticationStrategy proxyAuthStrategy;
    private final se.tink.org.apache.http.impl.auth.HttpAuthenticator authenticator;
    private final AuthState proxyAuthState;
    private final AuthSchemeRegistry authSchemeRegistry;
    private final ConnectionReuseStrategy reuseStrategy;

    /**
     * @since 4.3
     */
    public ProxyClient(
            final HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory,
            final ConnectionConfig connectionConfig,
            final RequestConfig requestConfig) {
        super();
        this.connFactory = connFactory != null ? connFactory : ManagedHttpClientConnectionFactory.INSTANCE;
        this.connectionConfig = connectionConfig != null ? connectionConfig : ConnectionConfig.DEFAULT;
        this.requestConfig = requestConfig != null ? requestConfig : RequestConfig.DEFAULT;
        this.httpProcessor = new ImmutableHttpProcessor(
                new RequestTargetHost(), new RequestClientConnControl(), new RequestUserAgent());
        this.requestExec = new HttpRequestExecutor();
        this.proxyAuthStrategy = new ProxyAuthenticationStrategy();
        this.authenticator = new HttpAuthenticator();
        this.proxyAuthState = new AuthState();
        this.authSchemeRegistry = new AuthSchemeRegistry();
        this.authSchemeRegistry.register(AuthSchemes.BASIC, new BasicSchemeFactory());
        this.authSchemeRegistry.register(AuthSchemes.DIGEST, new DigestSchemeFactory());
        this.authSchemeRegistry.register(AuthSchemes.NTLM, new NTLMSchemeFactory());
        this.authSchemeRegistry.register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory());
        this.authSchemeRegistry.register(AuthSchemes.KERBEROS, new KerberosSchemeFactory());
        this.reuseStrategy = new DefaultConnectionReuseStrategy();
    }

    /**
     * @deprecated (4.3) use {@link ProxyClient#ProxyClient(HttpConnectionFactory, ConnectionConfig, RequestConfig)}
     */
    @Deprecated
    public ProxyClient(final HttpParams params) {
        this(null,
                HttpParamConfig.getConnectionConfig(params),
                HttpClientParamConfig.getRequestConfig(params));
    }

    /**
     * @since 4.3
     */
    public ProxyClient(final RequestConfig requestConfig) {
        this(null, null, requestConfig);
    }

    public ProxyClient() {
        this(null, null, null);
    }

    /**
     * @deprecated (4.3) do not use.
     */
    @Deprecated
    public HttpParams getParams() {
        return new BasicHttpParams();
    }

    /**
     * @deprecated (4.3) do not use.
     */
    @Deprecated
    public AuthSchemeRegistry getAuthSchemeRegistry() {
        return this.authSchemeRegistry;
    }

    public Socket tunnel(
            final HttpHost proxy,
            final HttpHost target,
            final Credentials credentials) throws IOException, HttpException {
        Args.notNull(proxy, "Proxy host");
        Args.notNull(target, "Target host");
        Args.notNull(credentials, "Credentials");
        HttpHost host = target;
        if (host.getPort() <= 0) {
            host = new HttpHost(host.getHostName(), 80, host.getSchemeName());
        }
        final HttpRoute route = new HttpRoute(
                host,
                this.requestConfig.getLocalAddress(),
                proxy, false, RouteInfo.TunnelType.TUNNELLED, RouteInfo.LayerType.PLAIN);

        final ManagedHttpClientConnection conn = this.connFactory.create(
                route, this.connectionConfig);
        final HttpContext context = new BasicHttpContext();
        HttpResponse response;

        final HttpRequest connect = new BasicHttpRequest(
                "CONNECT", host.toHostString(), HttpVersion.HTTP_1_1);

        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(proxy), credentials);

        // Populate the execution context
        context.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, target);
        context.setAttribute(HttpCoreContext.HTTP_CONNECTION, conn);
        context.setAttribute(HttpCoreContext.HTTP_REQUEST, connect);
        context.setAttribute(HttpClientContext.HTTP_ROUTE, route);
        context.setAttribute(HttpClientContext.PROXY_AUTH_STATE, this.proxyAuthState);
        context.setAttribute(HttpClientContext.CREDS_PROVIDER, credsProvider);
        context.setAttribute(HttpClientContext.AUTHSCHEME_REGISTRY, this.authSchemeRegistry);
        context.setAttribute(HttpClientContext.REQUEST_CONFIG, this.requestConfig);

        this.requestExec.preProcess(connect, this.httpProcessor, context);

        for (;;) {
            if (!conn.isOpen()) {
                final Socket socket = new Socket(proxy.getHostName(), proxy.getPort());
                conn.bind(socket);
            }

            this.authenticator.generateAuthResponse(connect, this.proxyAuthState, context);

            response = this.requestExec.execute(connect, conn, context);

            final int status = response.getStatusLine().getStatusCode();
            if (status < 200) {
                throw new HttpException("Unexpected response to CONNECT request: " +
                        response.getStatusLine());
            }
            if (this.authenticator.isAuthenticationRequested(proxy, response,
                    this.proxyAuthStrategy, this.proxyAuthState, context)) {
                if (this.authenticator.handleAuthChallenge(proxy, response,
                        this.proxyAuthStrategy, this.proxyAuthState, context)) {
                    // Retry request
                    if (this.reuseStrategy.keepAlive(response, context)) {
                        // Consume response content
                        final HttpEntity entity = response.getEntity();
                        EntityUtils.consume(entity);
                    } else {
                        conn.close();
                    }
                    // discard previous auth header
                    connect.removeHeaders(AUTH.PROXY_AUTH_RESP);
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        final int status = response.getStatusLine().getStatusCode();

        if (status > 299) {

            // Buffer response content
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                response.setEntity(new BufferedHttpEntity(entity));
            }

            conn.close();
            throw new TunnelRefusedException("CONNECT refused by proxy: " +
                    response.getStatusLine(), response);
        }
        return conn.getSocket();
    }

}
