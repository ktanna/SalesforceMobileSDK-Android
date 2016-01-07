/*
 * Copyright (c) 2015, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.androidsdk.reactnative.bridge;

import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.salesforce.androidsdk.accounts.UserAccount;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

public class SalesforceNetReactBridge extends ReactContextBaseJavaModule {

    public static final String METHOD_KEY = "method";
    public static final String PATH_KEY = "path";
    private static final String QUERY_PARAMS_KEY = "queryParams";
    private static final String HEADER_PARAMS_KEY = "headerParams";

    private RestClient restClient;


    public SalesforceNetReactBridge(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "SalesforceNetReactBridge";
    }

    @ReactMethod
    public void sendRequest(ReadableMap args,
                            final Callback successCallback, final Callback errorCallback) {

        // args parsing
        RestRequest.RestMethod method = RestRequest.RestMethod.valueOf(args.getString(METHOD_KEY));
        String path = "/services/data" + args.getString(PATH_KEY);
        ReadableMap queryParams = args.getMap(QUERY_PARAMS_KEY);
        ReadableMap headerParams = args.getMap(HEADER_PARAMS_KEY);

        // Preparing request
        HttpEntity requestEntity = buildEntity(queryParams);
        Map<String, String> additionalHeaders = ReactBridgeHelper.toJavaStringMap(headerParams);
        RestRequest request = new RestRequest(method, path, requestEntity, additionalHeaders);

        // Sending request
        RestClient restClient = getRestClient();
        restClient.sendAsync(request, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse response) {
                try {
                    String responseAsString = response.asString();
                    // XXX Sending the string over and letting javascript do a JSON.parse(result)
                    //     It would be better to using NativeMap/NativeArray
                    //     Although the absence of a common super class would force us to
                    //     introduce two sendRequest methods:
                    //     - one that expects map back from the server
                    //     - one that expects array back from the server
                    successCallback.invoke(responseAsString);
                } catch (IOException e) {
                    Log.e("NetReactBridge", "sendRequest", e);
                    onError(e);
                }
            }

            @Override
            public void onError(Exception exception) {
                errorCallback.invoke(exception.getMessage());
            }
        });

    }

    private RestClient getRestClient() {
        if (restClient == null) {
            UserAccount account = SalesforceSDKManager.getInstance().getUserAccountManager().getCurrentUser();
            if (account == null) {
                restClient = SalesforceSDKManager.getInstance().getClientManager().peekUnauthenticatedRestClient();
            } else {
                restClient = SalesforceSDKManager.getInstance().getClientManager().peekRestClient(account);
            }
        }
        return restClient;
    }

    private static HttpEntity buildEntity(ReadableMap params) {
        HttpEntity entity = null;
        if (params != null) {
            try {
                JSONObject json = new JSONObject(ReactBridgeHelper.toJavaMap(params));
                entity = new StringEntity(json.toString(), HTTP.UTF_8);
            } catch (UnsupportedEncodingException e) {
                Log.e("NetReactBridge", "buildEntity failed", e);
            }
        }
        return entity;
    }
}
