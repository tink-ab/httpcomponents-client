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

package org.apache.http.conn.ssl;

/**
 * Provides an {@link AbstractCNExtractor} implementation that does use
 * {@code javax.naming} on platforms that do not support it.
 *
 * @since 4.5.6
 */
class CNExtractorFactory {

    final static AbstractCNExtractor PLATFORM_DEFAULT;

    static {
        PLATFORM_DEFAULT = isJavaxNamingPresent() ? getDefaultCNExtractor() : NullCNExtractor.INSTANCE;
    }

    private static boolean isJavaxNamingPresent() {
        try {
            Class.forName("javax.naming.InitialContext");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static AbstractCNExtractor getDefaultCNExtractor() {
        try {
            final Class<?> clazz = Class.forName("org.apache.http.conn.ssl.DefaultCNExtractor");
            return (AbstractCNExtractor) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
