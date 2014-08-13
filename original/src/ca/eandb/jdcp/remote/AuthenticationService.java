/*
 * Copyright (c) 2008 Bradley W. Kimmel
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package ca.eandb.jdcp.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

import javax.security.auth.login.LoginException;

/**
 * A remote service for authenticating <code>JobService</code> users.
 * @author Brad Kimmel
 */
public interface AuthenticationService extends Remote {

	/**
	 * Authenticates a user.
	 * @param username The username identifying the user to authenticate.
	 * @param password The password of the user to authenticate.
	 * @param protocolVersionId The <code>UUID</code> indicating the protocol
	 * 		expected by the client.
	 * @return The <code>JobService</code> to use for this session.
	 * @throws LoginException if the user name or password are invalid.
	 * @throws ProtocolVersionException if the protocol expected by the client
	 * 		(as indicated by <code>protocolVersionId</code>) is incompatible
	 * 		with the protocol expected by the server.
	 */
	JobService authenticate(String username, String password, UUID protocolVersionId)
			throws RemoteException, LoginException, ProtocolVersionException;

}
