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

import java.io.IOException;

import se.tink.org.apache.http.HttpException;
import se.tink.org.apache.http.HttpRequest;
import se.tink.org.apache.http.HttpResponse;
import se.tink.org.apache.http.HttpStatus;
import se.tink.org.apache.http.ProtocolVersion;
import se.tink.org.apache.http.client.methods.CloseableHttpResponse;
import se.tink.org.apache.http.client.methods.HttpExecutionAware;
import se.tink.org.apache.http.client.methods.HttpRequestWrapper;
import se.tink.org.apache.http.client.protocol.HttpClientContext;
import se.tink.org.apache.http.conn.routing.HttpRoute;
import se.tink.org.apache.http.impl.execchain.ClientExecChain;
import se.tink.org.apache.http.message.BasicHttpResponse;

public class DummyBackend implements ClientExecChain {

    private HttpRequest request;
    private HttpResponse response = new BasicHttpResponse(new ProtocolVersion("HTTP",1,1), HttpStatus.SC_OK, "OK");
    private int executions = 0;

    public void setResponse(final HttpResponse resp) {
        response = resp;
    }

    public HttpRequest getCapturedRequest() {
        return request;
    }

    public CloseableHttpResponse execute(
            final HttpRoute route,
            final HttpRequestWrapper request,
            final HttpClientContext clientContext,
            final HttpExecutionAware execAware) throws IOException, HttpException {
        this.request = request;
        executions++;
        return Proxies.enhanceResponse(response);
    }

    public int getExecutions() {
        return executions;
    }
}
