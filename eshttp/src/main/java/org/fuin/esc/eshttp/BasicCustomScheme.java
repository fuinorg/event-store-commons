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
package org.fuin.esc.eshttp;

import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.RFC2617Scheme;
import org.apache.http.message.BufferedHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EncodingUtils;

/**
 * This is basically a copy of {@link BasicScheme} from Apache HTTP CLient. The only difference is that it has the name "BasicCustom"
 * instead of "Basic". This is necessary as unfortunately the event store returns unfortunately a non standard authentication method
 * 'WWW-Authenticate: BasicCustom realm="ES"' instead of the standard 'WWW-Authenticate: Basic realm="ES"'.
 */
public final class BasicCustomScheme extends RFC2617Scheme {

    private static final long serialVersionUID = -1931571557597830536L;

    public static final String NAME = "BasicCustom";

    /** Whether the basic authentication process is complete */
    private boolean complete;

    /**
     * @param credentialsCharset Charset.
     * @since 4.3
     */
    public BasicCustomScheme(final Charset credentialsCharset) {
        super(credentialsCharset);
        this.complete = false;
    }

    public BasicCustomScheme() {
        this(Consts.ASCII);
    }

    /**
     * Returns textual designation of the basic authentication scheme.
     *
     * @return {@code basic}
     */
    @Override
    public String getSchemeName() {
        return NAME;
    }

    /**
     * Processes the Basic challenge.
     *
     * @param header
     *            the challenge header
     *
     * @throws MalformedChallengeException
     *             is thrown if the authentication challenge is malformed
     */
    @Override
    public void processChallenge(final Header header) throws MalformedChallengeException {
        super.processChallenge(header);
        this.complete = true;
    }

    /**
     * Tests if the Basic authentication process has been completed.
     *
     * @return {@code true} if Basic authorization has been processed, {@code false} otherwise.
     */
    @Override
    public boolean isComplete() {
        return this.complete;
    }

    /**
     * Returns {@code false}. Basic authentication scheme is request based.
     *
     * @return {@code false}.
     */
    @Override
    public boolean isConnectionBased() {
        return false;
    }

    /**
     * @deprecated (4.2) Use
     *             {@link org.apache.http.auth.ContextAwareAuthScheme#authenticate( Credentials, HttpRequest, org.apache.http.protocol.HttpContext)}
     */
    @Override
    @Deprecated
    public Header authenticate(final Credentials credentials, final HttpRequest request) throws AuthenticationException {
        return authenticate(credentials, request, new BasicHttpContext());
    }

    private String credentialsCharset(final HttpRequest request) {
        String charset = (String) request.getParams().getParameter(AuthPNames.CREDENTIAL_CHARSET);
        if (charset == null) {
            charset = getCredentialsCharset().name();
        }
        return charset;
    }

    /**
     * Produces basic authorization header for the given set of {@link Credentials}.
     *
     * @param credentials
     *            The set of credentials to be used for authentication
     * @param request
     *            The request being authenticated
     * @throws org.apache.http.auth.InvalidCredentialsException
     *             if authentication credentials are not valid or not applicable for this authentication scheme
     * @throws AuthenticationException
     *             if authorization string cannot be generated due to an authentication failure
     *
     * @return a basic authorization string
     */
    @Override
    public Header authenticate(final Credentials credentials, final HttpRequest request, final HttpContext context)
            throws AuthenticationException {

        Args.notNull(credentials, "Credentials");
        Args.notNull(request, "HTTP request");
        final StringBuilder tmp = new StringBuilder();
        tmp.append(credentials.getUserPrincipal().getName());
        tmp.append(":");
        tmp.append((credentials.getPassword() == null) ? "null" : credentials.getPassword());

        final Base64 base64codec = new Base64(0);
        final byte[] base64password = base64codec.encode(EncodingUtils.getBytes(tmp.toString(), credentialsCharset(request)));

        final CharArrayBuffer buffer = new CharArrayBuffer(32);
        if (isProxy()) {
            buffer.append(AUTH.PROXY_AUTH_RESP);
        } else {
            buffer.append(AUTH.WWW_AUTH_RESP);
        }
        buffer.append(": Basic ");
        buffer.append(base64password, 0, base64password.length);

        return new BufferedHeader(buffer);
    }

    /**
     * Returns a basic {@code Authorization} header value for the given {@link Credentials} and charset.
     *
     * @param credentials
     *            The credentials to encode.
     * @param charset
     *            The charset to use for encoding the credentials
     * @param proxy Proxy
     *
     * @return a basic authorization header
     *
     * @deprecated (4.3) use {@link #authenticate(Credentials, HttpRequest, HttpContext)}.
     */
    @Deprecated
    public static Header authenticate(final Credentials credentials, final String charset, final boolean proxy) {
        Args.notNull(credentials, "Credentials");
        Args.notNull(charset, "charset");

        final StringBuilder tmp = new StringBuilder();
        tmp.append(credentials.getUserPrincipal().getName());
        tmp.append(":");
        tmp.append((credentials.getPassword() == null) ? "null" : credentials.getPassword());

        final byte[] base64password = Base64.encodeBase64(EncodingUtils.getBytes(tmp.toString(), charset), false);

        final CharArrayBuffer buffer = new CharArrayBuffer(32);
        if (proxy) {
            buffer.append(AUTH.PROXY_AUTH_RESP);
        } else {
            buffer.append(AUTH.WWW_AUTH_RESP);
        }
        buffer.append(": Basic ");
        buffer.append(base64password, 0, base64password.length);

        return new BufferedHeader(buffer);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BASIC_CUSTOM [complete=").append(complete).append("]");
        return builder.toString();
    }
}
