/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  The ASF licenses this file to You
* under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.jackrabbit.webdav.client.methods;

import java.io.IOException;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpState;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.header.PollTimeoutHeader;
import org.apache.jackrabbit.webdav.observation.EventDiscovery;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * <code>PollMethod</code> implementation.
 * @deprecated as of 2.13.6, use {@link HttpPoll} instead
 */
@Deprecated
public class PollMethod extends DavMethodBase implements ObservationConstants {

    private static Logger log = LoggerFactory.getLogger(PollMethod.class);

    private EventDiscovery eventDiscovery;

    public PollMethod(String uri, String subscriptionId) {
        this(uri, subscriptionId, 0);
    }

    public PollMethod(String uri, String subscriptionId, long timeout) {
        super(uri);
        setRequestHeader(ObservationConstants.HEADER_SUBSCRIPTIONID, subscriptionId);
        if (timeout > 0) {
            setRequestHeader(new PollTimeoutHeader(timeout));
        }
    }

    public EventDiscovery getResponseAsEventDiscovery() throws IOException, DavException {
        checkUsed();
        if (eventDiscovery != null) {
            return eventDiscovery;
        } else {
            DavException dx = getResponseException();
            if (dx != null) {
                throw dx;
            } else {
                throw new DavException(getStatusCode(), getName() + " resulted with unexpected status: " + getStatusLine());
            }
        }
    }
    //---------------------------------------------------------< HttpMethod >---
    @Override
    public String getName() {
        return DavMethods.METHOD_POLL;
    }

    //------------------------------------------------------< DavMethodBase >---
    @Override
    protected boolean isSuccess(int statusCode) {
        return DavServletResponse.SC_OK == statusCode;
    }

    //------------------------------------------------------< HttpMethodBase >---
     /**
      * Retrieves the DAV:subscriptiondiscovery property present in the response body
      * and builds 'Subscription' objects from the corresponding DAV:subscription
      * child elements inside the discovery. If parsing the response body
      * fails for whatever reason or if the DAV:subscriptiondiscovery did not contain
      * at least a single DAV:subscription entry (the one created by the SUBSCRIBE
      * call) this methods in addition resets the 'success' flag to false.
      *
      * @param httpState
      * @param httpConnection
      * @see HttpMethodBase#processResponseBody(HttpState, HttpConnection)
      */
     @Override
     protected void processResponseBody(HttpState httpState, HttpConnection httpConnection) {
         // in case of successful response code -> parse xml body discovery object
         if (getSuccess()) {
             try {
                 setSuccess(buildDiscoveryFromRoot(getRootElement()));
             } catch (IOException e) {
                 log.error("Error while parsing multistatus response: " + e);
                 setSuccess(false);
             }
         }
     }

     //------------------------------------------------------------< private >---
     /**
      *
      * @param root
      * @return
      */
    private boolean buildDiscoveryFromRoot(Element root) {
        try {
            eventDiscovery = EventDiscovery.createFromXml(root);
            return true;
        } catch (IllegalArgumentException ex) {
            log.debug("Missing 'eventdiscovery' response body in POLL method.", ex);
            return false;
        }
    }
}
