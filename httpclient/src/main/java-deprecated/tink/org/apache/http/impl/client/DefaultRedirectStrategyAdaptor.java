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

package tink.org.apache.http.impl.client;

import java.net.URI;

import tink.org.apache.http.HttpRequest;
import tink.org.apache.http.HttpResponse;
import tink.org.apache.http.ProtocolException;
import tink.org.apache.http.annotation.Contract;
import tink.org.apache.http.annotation.ThreadingBehavior;
import tink.org.apache.http.client.RedirectHandler;
import tink.org.apache.http.client.RedirectStrategy;
import tink.org.apache.http.client.methods.HttpGet;
import tink.org.apache.http.client.methods.HttpHead;
import tink.org.apache.http.client.methods.HttpUriRequest;
import tink.org.apache.http.protocol.HttpContext;
import tink.org.apache.http.client.RedirectHandler;

/**
 * @deprecated (4.1) do not use
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
@Deprecated
class DefaultRedirectStrategyAdaptor implements RedirectStrategy {

    private final RedirectHandler handler;

    public DefaultRedirectStrategyAdaptor(final RedirectHandler handler) {
        super();
        this.handler = handler;
    }

    @Override
    public boolean isRedirected(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context) throws ProtocolException {
        return this.handler.isRedirectRequested(response, context);
    }

    @Override
    public HttpUriRequest getRedirect(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context) throws ProtocolException {
        final URI uri = this.handler.getLocationURI(response, context);
        final String method = request.getRequestLine().getMethod();
        if (method.equalsIgnoreCase(HttpHead.METHOD_NAME)) {
            return new HttpHead(uri);
        } else {
            return new HttpGet(uri);
        }
    }

    public RedirectHandler getHandler() {
        return this.handler;
    }

}
