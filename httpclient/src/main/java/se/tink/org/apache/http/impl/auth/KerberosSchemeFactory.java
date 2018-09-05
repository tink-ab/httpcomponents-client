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
package se.tink.org.apache.http.impl.auth;

import se.tink.org.apache.http.annotation.Immutable;
import se.tink.org.apache.http.auth.AuthScheme;
import se.tink.org.apache.http.auth.AuthSchemeFactory;
import se.tink.org.apache.http.auth.AuthSchemeProvider;
import se.tink.org.apache.http.params.HttpParams;
import se.tink.org.apache.http.protocol.HttpContext;
import se.tink.org.apache.http.auth.AuthScheme;
import se.tink.org.apache.http.auth.AuthSchemeFactory;
import se.tink.org.apache.http.auth.AuthSchemeProvider;

/**
 * {@link AuthSchemeProvider} implementation that creates and initializes
 * {@link KerberosScheme} instances.
 *
 * @since 4.2
 */
@Immutable
@SuppressWarnings("deprecation")
public class KerberosSchemeFactory implements AuthSchemeFactory, AuthSchemeProvider {

    private final boolean stripPort;

    public KerberosSchemeFactory(final boolean stripPort) {
        super();
        this.stripPort = stripPort;
    }

    public KerberosSchemeFactory() {
        this(false);
    }

    public boolean isStripPort() {
        return stripPort;
    }

    public AuthScheme newInstance(final HttpParams params) {
        return new KerberosScheme(this.stripPort);
    }

    public AuthScheme create(final HttpContext context) {
        return new KerberosScheme(this.stripPort);
    }

}
