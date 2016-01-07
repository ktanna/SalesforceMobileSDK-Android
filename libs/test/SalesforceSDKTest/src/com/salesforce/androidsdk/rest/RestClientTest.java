/*
 * Copyright (c) 2011-2015, salesforce.com, inc.
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
package com.salesforce.androidsdk.rest;

import android.os.Environment;
import android.test.InstrumentationTestCase;

import com.android.volley.Request;
import com.google.common.io.CharStreams;
import com.salesforce.androidsdk.TestCredentials;
import com.salesforce.androidsdk.auth.HttpAccess;
import com.salesforce.androidsdk.auth.OAuth2;
import com.salesforce.androidsdk.auth.OAuth2.TokenEndpointResponse;
import com.salesforce.androidsdk.rest.RestClient.AuthTokenProvider;
import com.salesforce.androidsdk.rest.RestClient.ClientInfo;
import com.salesforce.androidsdk.rest.RestClient.WrappedRestRequest;
import com.salesforce.androidsdk.rest.RestRequest.RestMethod;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
/**
 * Tests for RestClient
 *
 * Does live calls to a test org
 *
 */
public class RestClientTest extends InstrumentationTestCase {

    private static final String ENTITY_NAME_PREFIX = "RestClientTest";
    private static final String SEARCH_ENTITY_NAME = "RestClientSearchTest";
    private static final String SEARCH_ENTITY_ID = "001S000000gxUx7IAE";
    private static final String BAD_TOKEN = "bad-token";
    private ClientInfo clientInfo;
    private HttpAccess httpAccess;
    private RestClient restClient;
    private String authToken;
    private String instanceUrl;
    public static final String TEST_FIRST_NAME = "firstName";
    public static final String TEST_LAST_NAME = "lastName";
    public static final String TEST_EMAIL = "test@email.com";
    public static final String TEST_PHOTO_URL = "http://some.photo.url";
    public static final String TEST_THUMBNAIL_URL = "http://some.thumbnail.url";


    @Override
    public void setUp() throws Exception {
        super.setUp();
        TestCredentials.init(getInstrumentation().getContext());
        httpAccess = new HttpAccess(null, null);
        TokenEndpointResponse refreshResponse = OAuth2.refreshAuthToken(httpAccess, new URI(TestCredentials.INSTANCE_URL), TestCredentials.CLIENT_ID, TestCredentials.REFRESH_TOKEN);
        authToken = refreshResponse.authToken;
        instanceUrl = refreshResponse.instanceUrl;
        clientInfo = new ClientInfo(TestCredentials.CLIENT_ID,
        		new URI(TestCredentials.INSTANCE_URL),
        		new URI(TestCredentials.LOGIN_URL),
        		new URI(TestCredentials.IDENTITY_URL),
        		TestCredentials.ACCOUNT_NAME, TestCredentials.USERNAME,
        		TestCredentials.USER_ID, TestCredentials.ORG_ID, null, null,
                TEST_FIRST_NAME, TEST_LAST_NAME, TEST_EMAIL, TEST_PHOTO_URL, TEST_THUMBNAIL_URL);
        restClient = new RestClient(clientInfo, authToken, httpAccess, null);
    }

    @Override
    public void tearDown() throws Exception {
        cleanup();
    }

    /**
     * Testing getClientInfo
     * @throws URISyntaxException
     */
    public void testGetClientInfo() throws URISyntaxException {
        assertEquals("Wrong client id", TestCredentials.CLIENT_ID, restClient.getClientInfo().clientId);
        assertEquals("Wrong instance url", new URI(TestCredentials.INSTANCE_URL), restClient.getClientInfo().instanceUrl);
        assertEquals("Wrong login url", new URI(TestCredentials.LOGIN_URL), restClient.getClientInfo().loginUrl);
        assertEquals("Wrong account name", TestCredentials.ACCOUNT_NAME, restClient.getClientInfo().accountName);
        assertEquals("Wrong username", TestCredentials.USERNAME, restClient.getClientInfo().username);
        assertEquals("Wrong userId", TestCredentials.USER_ID, restClient.getClientInfo().userId);
        assertEquals("Wrong orgId", TestCredentials.ORG_ID, restClient.getClientInfo().orgId);
        assertEquals("Wrong firstName", TEST_FIRST_NAME, restClient.getClientInfo().firstName);
        assertEquals("Wrong lastName", TEST_LAST_NAME, restClient.getClientInfo().lastName);
        assertEquals("Wrong email", TEST_EMAIL, restClient.getClientInfo().email);
        assertEquals("Wrong photoUrl", TEST_PHOTO_URL, restClient.getClientInfo().photoUrl);
        assertEquals("Wrong thumbnailUrl", TEST_THUMBNAIL_URL, restClient.getClientInfo().thumbnailUrl);

    }

    public void testClientInfoResolveUrl() {
    	assertEquals("Wrong url", TestCredentials.INSTANCE_URL + "/a/b/", clientInfo.resolveUrl("a/b/").toString());
    	assertEquals("Wrong url", TestCredentials.INSTANCE_URL + "/a/b/", clientInfo.resolveUrl("/a/b/").toString());
    }

    public void testClientInfoResolveUrlForHttpsUrl() {
        assertEquals("Wrong url", "https://testurl", clientInfo.resolveUrl("https://testurl").toString());
        assertEquals("Wrong url", "http://testurl", clientInfo.resolveUrl("http://testurl").toString());
        assertEquals("Wrong url", "HTTPS://testurl", clientInfo.resolveUrl("HTTPS://testurl").toString());
        assertEquals("Wrong url", "HTTP://testurl", clientInfo.resolveUrl("HTTP://testurl").toString());
    }

    public void testClientInfoResolveUrlForCommunityUrl() throws Exception {
        final ClientInfo info = new ClientInfo(TestCredentials.CLIENT_ID,
        		new URI(TestCredentials.INSTANCE_URL),
        		new URI(TestCredentials.LOGIN_URL),
        		new URI(TestCredentials.IDENTITY_URL),
        		TestCredentials.ACCOUNT_NAME, TestCredentials.USERNAME,
        		TestCredentials.USER_ID, TestCredentials.ORG_ID, null,
        		TestCredentials.COMMUNITY_URL, null, null, null, null, null);
    	assertEquals("Wrong url", TestCredentials.COMMUNITY_URL + "/a/b/", info.resolveUrl("a/b/").toString());
    	assertEquals("Wrong url", TestCredentials.COMMUNITY_URL + "/a/b/", info.resolveUrl("/a/b/").toString());
    }

    public void testGetInstanceUrlForCommunity() throws Exception {
        final ClientInfo info = new ClientInfo(TestCredentials.CLIENT_ID,
        		new URI(TestCredentials.INSTANCE_URL),
        		new URI(TestCredentials.LOGIN_URL),
        		new URI(TestCredentials.IDENTITY_URL),
        		TestCredentials.ACCOUNT_NAME, TestCredentials.USERNAME,
        		TestCredentials.USER_ID, TestCredentials.ORG_ID, null,
        		TestCredentials.COMMUNITY_URL, null, null, null, null, null);
        assertEquals("Wrong url", TestCredentials.COMMUNITY_URL, info.getInstanceUrlAsString());
    }

    public void testGetInstanceUrl() {
        assertEquals("Wrong url", TestCredentials.INSTANCE_URL, clientInfo.getInstanceUrlAsString());
    }

    /**
     * Testing getAuthToken
     */
    public void testGetAuthToken() {
        assertEquals("Wrong auth token", authToken, restClient.getAuthToken());
    }

    /**
     * Testing a call with a bad auth token when restClient has no token provider
     * Expect a 401.
     * @throws URISyntaxException
     * @throws IOException
     */
    public void testCallWithBadAuthToken() throws URISyntaxException, IOException {
        RestClient unauthenticatedRestClient = new RestClient(clientInfo, BAD_TOKEN, httpAccess, null);
        RestResponse response = unauthenticatedRestClient.sendSync(RestRequest.getRequestForResources(TestCredentials.API_VERSION));
        assertFalse("Expected error", response.isSuccess());
        checkResponse(response, HttpStatus.SC_UNAUTHORIZED, true);
    }

    /**
     * Testing a call with a bad auth token when restClient has a token provider
     * Expect token provider to be invoked and new token to be used.
     * @throws URISyntaxException
     * @throws IOException
     */
    public void testCallWithBadTokenAndTokenProvider() throws URISyntaxException, IOException {
        AuthTokenProvider authTokenProvider = new AuthTokenProvider() {
            @Override
            public String getNewAuthToken() {
                return authToken;
            }

            @Override
            public String getRefreshToken() {
                return null;
            }

            @Override
            public long getLastRefreshTime() {
                return -1;
            }

            @Override
            public String getInstanceUrl() { return instanceUrl; }
        };
        RestClient unauthenticatedRestClient = new RestClient(clientInfo, BAD_TOKEN, httpAccess, authTokenProvider);
        assertEquals("RestClient should be using the bad token initially", BAD_TOKEN, unauthenticatedRestClient.getAuthToken());
        RestResponse response = unauthenticatedRestClient.sendSync(RestRequest.getRequestForResources(TestCredentials.API_VERSION));
        assertEquals("RestClient should now be using the good token", authToken, unauthenticatedRestClient.getAuthToken());
        assertTrue("Expected success", response.isSuccess());
        checkResponse(response, HttpStatus.SC_OK, false);
    }

    /**
     * Testing a call with a bad auth token when restClient has a token provider
     * Expect token provider to be invoked and new token to be used and a new instance url to be returned.
     * @throws URISyntaxException
     * @throws IOException
     */
    public void testCallWithBadInstanceUrl() throws URISyntaxException, IOException {
        AuthTokenProvider authTokenProvider = new AuthTokenProvider() {
            @Override
            public String getNewAuthToken() {
                return authToken;
            }

            @Override
            public String getRefreshToken() {
                return null;
            }

            @Override
            public long getLastRefreshTime() {
                return -1;
            }

            @Override
            public String getInstanceUrl() { return instanceUrl; }
        };
        RestClient unauthenticatedRestClient = new RestClient(clientInfo, BAD_TOKEN, httpAccess, authTokenProvider);
        assertEquals("RestClient has bad instance url", new URI(TestCredentials.INSTANCE_URL), unauthenticatedRestClient.getClientInfo().instanceUrl);
        RestResponse response = unauthenticatedRestClient.sendSync(RestRequest.getRequestForResources(TestCredentials.API_VERSION));
        assertEquals("RestClient should now have the correct instance url", new URI(instanceUrl), unauthenticatedRestClient.getClientInfo().instanceUrl);
        assertTrue("Expected success", response.isSuccess());
        checkResponse(response, HttpStatus.SC_OK, false);
    }


    /**
     * Testing a get versions call to the server - check response
     * @throws Exception
     */
    public void testGetVersions() throws Exception {
        // We don't need to be authenticated
        RestClient unauthenticatedRestClient = new RestClient(clientInfo, BAD_TOKEN, httpAccess, null);
        RestResponse response = unauthenticatedRestClient.sendSync(RestRequest.getRequestForVersions());
        checkResponse(response, HttpStatus.SC_OK, true);
        checkKeys(response.asJSONArray().getJSONObject(0), "label", "url", "version");
    }

    /**
     * Testing a get resources call to the server - check response
     * @throws Exception
     */
    public void testGetResources() throws Exception {
        RestResponse response = restClient.sendSync(RestRequest.getRequestForResources(TestCredentials.API_VERSION));
        checkResponse(response, HttpStatus.SC_OK, false);
        checkKeys(response.asJSONObject(), "sobjects", "search", "recent");
    }

    /**
     * Testing a get resources async call to the server - check response
     * @throws Exception
     */
    public void testGetResourcesAsync() throws Exception {
        RestResponse response = sendAsync(restClient, RestRequest.getRequestForResources(TestCredentials.API_VERSION));
        checkResponse(response, HttpStatus.SC_OK, false);
        checkKeys(response.asJSONObject(), "sobjects", "search", "recent");
    }

    /**
     * Testing a describe global call to the server - check response
     * @throws Exception
     */
    public void testDescribeGlobal() throws Exception {
        RestResponse response = restClient.sendSync(RestRequest.getRequestForDescribeGlobal(TestCredentials.API_VERSION));
        checkResponse(response, HttpStatus.SC_OK, false);
        JSONObject jsonResponse = response.asJSONObject();
        checkKeys(jsonResponse, "encoding", "maxBatchSize", "sobjects");
        checkKeys(jsonResponse.getJSONArray("sobjects").getJSONObject(0), "name", "label", "custom", "keyPrefix");
    }

    /**
     * Testing a describe global async call to the server - check response
     * @throws Exception
     */
    public void testDescribeGlobalAsync() throws Exception {
        RestResponse response = sendAsync(restClient, RestRequest.getRequestForDescribeGlobal(TestCredentials.API_VERSION));
        checkResponse(response, HttpStatus.SC_OK, false);
        JSONObject jsonResponse = response.asJSONObject();
        checkKeys(jsonResponse, "encoding", "maxBatchSize", "sobjects");
        checkKeys(jsonResponse.getJSONArray("sobjects").getJSONObject(0), "name", "label", "custom", "keyPrefix");
    }

    /**
     * Testing a metadata call to the server - check response
     * @throws Exception
     */
    public void testMetadata() throws Exception {
        RestResponse response = restClient.sendSync(RestRequest.getRequestForMetadata(TestCredentials.API_VERSION, "account"));
        checkResponse(response, HttpStatus.SC_OK, false);
        JSONObject jsonResponse = response.asJSONObject();
        checkKeys(jsonResponse, "objectDescribe", "recentItems");
        checkKeys(jsonResponse.getJSONObject("objectDescribe"), "name", "label", "keyPrefix");
        assertEquals("Wrong object name", "Account", jsonResponse.getJSONObject("objectDescribe").getString("name"));
    }

    /**
     * Testing a describe call to the server - check response
     * @throws Exception
     */
    public void testDescribe() throws Exception {
        RestResponse response = restClient.sendSync(RestRequest.getRequestForDescribe(TestCredentials.API_VERSION, "account"));
        checkResponse(response, HttpStatus.SC_OK, false);
        JSONObject jsonResponse = response.asJSONObject();
        checkKeys(jsonResponse, "name", "fields", "urls", "label");
        assertEquals("Wrong object name", "Account", jsonResponse.getString("name"));
    }

    /**
     * Testing a create call to the server - check response
     * @throws Exception
     */
    public void testCreate() throws Exception {
        Map<String, Object> fields = new HashMap<String, Object>();
        String newAccountName = ENTITY_NAME_PREFIX + System.nanoTime();
        fields.put("name", newAccountName);
        RestResponse response = restClient.sendSync(RestRequest.getRequestForCreate(TestCredentials.API_VERSION, "account", fields));
        JSONObject jsonResponse = response.asJSONObject();
        checkKeys(jsonResponse, "id", "errors", "success");
        assertTrue("Create failed", jsonResponse.getBoolean("success"));
    }

    /**
     * Testing a retrieve call to the server.
     * Create new account then retrieve it.
     * @throws Exception
     */
    public void testRetrieve() throws Exception {
        List<String> fields = Arrays.asList(new String[] {"name", "ownerId"});
        IdName newAccountIdName = createAccount();
        RestResponse response = restClient.sendSync(RestRequest.getRequestForRetrieve(TestCredentials.API_VERSION, "account", newAccountIdName.id, fields));
        checkResponse(response, HttpStatus.SC_OK, false);
        JSONObject jsonResponse = response.asJSONObject();
        checkKeys(jsonResponse, "attributes", "Name", "OwnerId", "Id");
        assertEquals("Wrong row returned", newAccountIdName.name, jsonResponse.getString("Name"));
    }

    /**
     * Testing an update call to the server.
     * Create new account then update it then get it back
     * @throws Exception
     */
    public void testUpdate() throws Exception {

        // Create
        IdName newAccountIdName = createAccount();

        // Update
        Map<String, Object> fields = new HashMap<String, Object>();
        String updatedAccountName = ENTITY_NAME_PREFIX + "-" + System.nanoTime();
        fields.put("name", updatedAccountName);
        RestResponse updateResponse = restClient.sendSync(RestRequest.getRequestForUpdate(TestCredentials.API_VERSION, "account", newAccountIdName.id, fields));
        assertTrue("Update failed", updateResponse.isSuccess());

        // Retrieve - expect updated name
        RestResponse response = restClient.sendSync(RestRequest.getRequestForRetrieve(TestCredentials.API_VERSION, "account", newAccountIdName.id, Arrays.asList(new String[]{"name"})));
        assertEquals("Wrong row returned", updatedAccountName, response.asJSONObject().getString("Name"));
    }


    /**
     * Testing a delete call to the server.
     * Create new account then delete it then try to retrieve it again (expect 404).
     * @throws Exception
     */
    public void testDelete() throws Exception {

        // Create
        IdName newAccountIdName = createAccount();

        // Delete
        RestResponse deleteResponse = restClient.sendSync(RestRequest.getRequestForDelete(TestCredentials.API_VERSION, "account", newAccountIdName.id));
        assertTrue("Delete failed", deleteResponse.isSuccess());

        // Retrieve - expect 404
        List<String> fields = Arrays.asList(new String[] {"name"});
        RestResponse response = restClient.sendSync(RestRequest.getRequestForRetrieve(TestCredentials.API_VERSION, "account", newAccountIdName.id, fields));
        assertEquals("404 was expected", HttpStatus.SC_NOT_FOUND, response.getStatusCode());
    }


    /**
     * Testing a query call to the server.
     * Create new account then look for it using soql.
     * @throws Exception
     */
    public void testQuery() throws Exception {
        IdName newAccountIdName = createAccount();
        RestResponse response = restClient.sendSync(RestRequest.getRequestForQuery(TestCredentials.API_VERSION, "select name from account where id = '" + newAccountIdName.id + "'"));
        checkResponse(response, HttpStatus.SC_OK, false);
        JSONObject jsonResponse = response.asJSONObject();
        checkKeys(jsonResponse, "done", "totalSize", "records");
        assertEquals("Expected one row", 1, jsonResponse.getInt("totalSize"));
        assertEquals("Wrong row returned", newAccountIdName.name, jsonResponse.getJSONArray("records").getJSONObject(0).get("Name"));
    }

    /**
     * Testing a search call to the server.
     * Create new account then look for it using sosl.
     * @throws Exception
     */
    public void testSearch() throws Exception {
        RestResponse response = restClient.sendSync(RestRequest.getRequestForSearch(TestCredentials.API_VERSION, "find {" + SEARCH_ENTITY_NAME + "}"));
        checkResponse(response, HttpStatus.SC_OK, true);
        JSONArray matchingRows = response.asJSONArray();
        assertEquals("Expected one row", 1, matchingRows.length());
        JSONObject matchingRow = matchingRows.getJSONObject(0);
        checkKeys(matchingRow, "attributes", "Id");
        assertEquals("Wrong row returned", SEARCH_ENTITY_ID, matchingRow.get("Id"));
    }

    /**
     * Testing that calling resume more than once on a RestResponse doesn't throw an exception
     * @throws Exception 
     */
    public void testDoubleConsume() throws Exception {
        RestResponse response = restClient.sendSync(RestRequest.getRequestForMetadata(TestCredentials.API_VERSION, "account"));
        checkResponse(response, HttpStatus.SC_OK, false);
        try {
        	response.consume();
        	response.consume();
        }
        catch (IllegalStateException e) {
        	fail("Calling consume should not have thrown an exception");
        }
    }
    
    /**
     * Testing that WrappedRestRequest's url field is correct with various RestRequest objects
     * @throws Exception
     */
    public void testWrappedRestRequestUrl() throws Exception {
    	checkWrappedRestRequestUrl(RestRequest.getRequestForMetadata(TestCredentials.API_VERSION, "account"), clientInfo.instanceUrl + "/services/data/" + TestCredentials.API_VERSION + "/sobjects/account/");
		Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("name", "NewAccount");
		checkWrappedRestRequestUrl(RestRequest.getRequestForCreate(TestCredentials.API_VERSION, "account", fields), clientInfo.instanceUrl + "/services/data/" + TestCredentials.API_VERSION + "/sobjects/account");
		checkWrappedRestRequestUrl(RestRequest.getRequestForUpdate(TestCredentials.API_VERSION, "account", "fakeId", fields), clientInfo.instanceUrl + "/services/data/" + TestCredentials.API_VERSION + "/sobjects/account/fakeId");
    }

    /**
     * Testing that WrappedRestRequest's method field is correct with various RestRequest objects
     * @throws Exception
     */
    public void testWrappedRestRequestMethod() throws Exception {
    	checkWrappedRestRequestMethod(RestRequest.getRequestForMetadata(TestCredentials.API_VERSION, "account"), Request.Method.GET);
    	Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("name", "NewAccount");
		checkWrappedRestRequestMethod(RestRequest.getRequestForCreate(TestCredentials.API_VERSION, "account", fields), Request.Method.POST);
		checkWrappedRestRequestMethod(RestRequest.getRequestForUpdate(TestCredentials.API_VERSION, "account", "fakeId", fields), RestMethod.MethodPATCH);
    }

    /**
     * Testing that WrappedRestRequest's body field is correct with various RestRequest objects
     * @throws Exception
     */
    public void testWrappedRestRequestBody() throws Exception {
		checkWrappedRestRequestBody(RestRequest.getRequestForMetadata(TestCredentials.API_VERSION, "account"), null);
		Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("name", "NewAccount");
		checkWrappedRestRequestBody(RestRequest.getRequestForCreate(TestCredentials.API_VERSION, "account", fields), "{\"name\":\"NewAccount\"}".getBytes());
		checkWrappedRestRequestBody(RestRequest.getRequestForUpdate(TestCredentials.API_VERSION, "account", "fakeId", fields), "{\"name\":\"NewAccount\"}".getBytes());
    }

    /**
     * Testing that WrappedRestRequest's body content type field is correct with various RestRequest objects
     * @throws Exception
     */
    public void testWrappedRestRequestBodyContentType() throws Exception {
        checkWrappedRestRequestBodyContentType(RestRequest.getRequestForMetadata(TestCredentials.API_VERSION, "account"), "application/x-www-form-urlencoded; charset=UTF-8");
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("name", "NewAccount");
        checkWrappedRestRequestBodyContentType(RestRequest.getRequestForCreate(TestCredentials.API_VERSION, "account", fields), "application/json; charset=UTF-8");
        checkWrappedRestRequestBodyContentType(RestRequest.getRequestForUpdate(TestCredentials.API_VERSION, "account", "fakeId", fields), "application/json; charset=UTF-8");
    }

    /**
     * Testing doing a sync request against a non salesforce public api with a RestClient that uses an UnauthenticatedClientInfo
     * @return
     * @throws Exception
     */
    public void testRestClientUnauthenticatedlientInfo() throws Exception {
        RestClient unauthenticatedRestClient = new RestClient(new RestClient.UnauthenticatedClientInfo(), null, HttpAccess.DEFAULT, null);
        RestRequest request = new RestRequest(RestMethod.GET, "https://api.spotify.com/v1/search?q=James%20Brown&type=artist", null);
        RestResponse response = unauthenticatedRestClient.sendSync(request);
        checkResponse(response, HttpStatus.SC_OK, false);
        JSONObject jsonResponse = response.asJSONObject();
        checkKeys(jsonResponse, "artists");
        checkKeys(jsonResponse.getJSONObject("artists"), "href", "items", "limit", "next", "offset", "previous", "total");
    }

    /**
     * Testing doing an async request against a non salesforce public api with a RestClient that uses an UnauthenticatedClientInfo
     * @return
     * @throws Exception
     */
    public void testRestClientUnauthenticatedlientInfoAsync() throws Exception {
        RestClient unauthenticatedRestClient = new RestClient(new RestClient.UnauthenticatedClientInfo(), null, HttpAccess.DEFAULT, null);
        RestRequest request = new RestRequest(RestMethod.GET, "https://api.spotify.com/v1/search?q=James%20Brown&type=artist", null);
        RestResponse response = sendAsync(unauthenticatedRestClient, request);
        checkResponse(response, HttpStatus.SC_OK, false);
        JSONObject jsonResponse = response.asJSONObject();
        checkKeys(jsonResponse, "artists");
        checkKeys(jsonResponse.getJSONObject("artists"), "href", "items", "limit", "next", "offset", "previous", "total");
    }

    /**
     * Tests if the file upload API is working per design.
     *
     * @throws Exception
     */
    public void testFileUpload() throws Exception {
        final String filename  = "MyFile.txt";
        final File file = new File(Environment.getExternalStorageDirectory() + File.separator + filename);
        if (!file.exists()) {
            file.createNewFile();
            final OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file));
            out.write("This is a test!");
            out.close();
        }
        assertTrue("File should exist", file.exists());
        final RestResponse response = restClient.uploadFile(file, filename, "Test Title", "Test Description");
        assertNotNull("Response should not be null", response);
        assertEquals("Status code should be 201 CREATED", HttpStatus.SC_CREATED, response.getStatusCode());
        file.delete();
        assertFalse("File should not exist", file.exists());
    }

    /**
     * Tests if a stream from {@link RestResponse#asInputStream()} is readable.
     *
     * @throws Exception
     */
    public void testResponseStreamIsReadable() throws Exception {
        final RestResponse response = getStreamTestResponse();

        try {
            InputStream in = response.asInputStream();
            assertStreamTestResponseStreamIsValid(in);
        } catch (IOException e) {
            fail("The InputStream should be readable and an IOException should not have been thrown");
        } catch (JSONException e) {
            fail("Valid JSON data should have been returned");
        } finally {
            response.consumeQuietly();
        }
    }

    /**
     * Tests if a stream from {@link RestResponse#asInputStream()} is consumed (according to the REST client) by fully reading the stream.
     *
     * @throws Exception
     */
    public void testResponseStreamConsumedByReadingStream() throws Exception {
        final RestResponse response = getStreamTestResponse();

        try {
            InputStream in = response.asInputStream();
            inputStreamToString(in);
        } catch (IOException e) {
            fail("The InputStream should be readable and an IOException should not have been thrown");
        }

        // We read the entire stream but forgot to call consume() or consumeQuietly() - can another REST call be made?
        final RestResponse anotherResponse = getStreamTestResponse();
        assertNotNull(anotherResponse);
    }

    /**
     * Tests that a stream from {@link RestResponse#asInputStream()} cannot be read from twice.
     *
     * @throws Exception
     */
    public void testResponseStreamCannotBeReadTwice() throws Exception {
        final RestResponse response = getStreamTestResponse();

        try {
            final InputStream in = response.asInputStream();
            inputStreamToString(in);
        } catch (IOException e) {
            fail("The InputStream should be readable and an IOException should not have been thrown");
        }

        try {
            response.asInputStream();
            fail("An IOException should have been thrown while trying to read the InputStream a second time");
        } catch (IOException e) {
            // Expected
        } finally {
            response.consumeQuietly();
        }
    }

    /**
     * Tests that {@link RestResponse}'s accessor methods (like {@link RestResponse#asBytes()} do not return valid data if the response is streamed first.
     *
     * @throws Exception
     */
    public void testOtherAccessorsNotAvailableAfterResponseStreaming() throws Exception {
        final RestResponse response = getStreamTestResponse();

        final Runnable testAccessorsNotAccessible = new Runnable() {
            @Override
            public void run() {
                try {
                    // The other accessors should not return valid data as soon as the stream is opened
                    assertNotNull(response.asBytes());
                    assertEquals("asBytes() array should be empty", 0, response.asBytes().length);
                    assertEquals("asString() should return the empty string", "", response.asString());

                    try {
                        assertNull(response.asJSONObject());
                        fail("asJSONObject() should fail");
                    } catch (JSONException e) {
                        // Expected
                    }

                    try {
                        assertNull(response.asJSONArray());
                        fail("asJSONArray() should fail");
                    } catch (JSONException e) {
                        // Expected
                    }
                } catch (IOException e) {
                    fail("IOException not expected");
                }
            }
        };

        try {
            response.asInputStream();
            testAccessorsNotAccessible.run();
        } catch (IOException e) {
            fail("The InputStream should be readable and an IOException should not have been thrown");
        } finally {
            response.consumeQuietly();
        }

        // Ensure that consuming the stream doesn't make the accessors accessible again
        testAccessorsNotAccessible.run();
    }

    /**
     * Tests that any call to {@link RestResponse}'s accessor methods prevent the response data from being streamed via {@link RestResponse#asInputStream()}.
     *
     * @throws Exception
     */
    public void testAccessorMethodsPreventResponseStreaming() throws Exception {
        final RestResponse response = getStreamTestResponse();
        response.asBytes();

        try {
            response.asInputStream();
            fail("The InputStream should not be readable after an accessor method is called");
        } catch (IOException e) {
            // Expected
        } finally {
            response.consumeQuietly();
        }
    }

    //
    // Helper methods
    //

    /**
     * @return a {@link RestResponse} for testing streaming. It should contain some JSON data.
     * @throws IOException if the response could not be made
     */
    private RestResponse getStreamTestResponse() throws IOException {
        final RestResponse response = restClient.sendSync(RestRequest.getRequestForResources(TestCredentials.API_VERSION));
        assertEquals("Response code should be HTTP OK", response.getStatusCode(), HttpStatus.SC_OK);
        return response;
    }

    /**
     * Assert that the {@link RestResponse} returned from {@link #getStreamTestResponse()} is valid.
     * @param in the {@link InputStream} of response data
     * @throws IOException if the stream could not be read
     * @throws JSONException if the response could not be decoded to a valid JSON object
     */
    private void assertStreamTestResponseStreamIsValid(InputStream in) throws IOException, JSONException {
        final String responseData = inputStreamToString(in);
        assertNotNull("The response should contain data", responseData);

        final JSONObject responseJson = new JSONObject(responseData);
        checkKeys(responseJson, "sobjects", "search", "recent");
    }

    private String inputStreamToString(InputStream inputStream) throws IOException {
        return CharStreams.toString(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    /**
     * Send request using sendAsync method
     * @param client
     * @param request
     * @return
     * @throws InterruptedException
     */
    private RestResponse sendAsync(RestClient client, RestRequest request) throws InterruptedException {
        final BlockingQueue<RestResponse> responseBlockingQueue = new ArrayBlockingQueue<>(1);
        client.sendAsync(request, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse response) {
                responseBlockingQueue.add(response);
            }

            @Override
            public void onError(Exception exception) {
                responseBlockingQueue.add(null);
            }
        });
        return responseBlockingQueue.poll(30, TimeUnit.SECONDS);
    }

    private void checkWrappedRestRequestUrl(RestRequest restRequest, String expectedUrl) throws Exception {
        WrappedRestRequest request = new RestClient.WrappedRestRequest(clientInfo, restRequest, null);
        assertEquals("Wrong url", expectedUrl, request.getUrl());
    }

    private void checkWrappedRestRequestMethod(RestRequest restRequest, int expectedMethod) throws Exception {
        WrappedRestRequest request = new RestClient.WrappedRestRequest(clientInfo, restRequest, null);
        assertEquals("Wrong method", expectedMethod, request.getMethod());
    }

    private void checkWrappedRestRequestBody(RestRequest restRequest, byte[] expectedBody) throws Exception {
    	WrappedRestRequest request = new RestClient.WrappedRestRequest(clientInfo, restRequest, null);
    	if (expectedBody == null) {
    		assertNull("Body should be null", request.getBody());
    	} else {
    		assertEquals("Wrong body", new String(expectedBody), new String(request.getBody()));
    	}
    }

    private void checkWrappedRestRequestBodyContentType(RestRequest restRequest, String expectedBodyContentType) throws Exception {
    	WrappedRestRequest request = new RestClient.WrappedRestRequest(clientInfo, restRequest, null);
		assertTrue("Wrong body content type", request.getBodyContentType().startsWith(expectedBodyContentType));
    }    
    
    /**
     * Helper method to create a account with a unique name and returns its name and id
     */
    private IdName createAccount() throws Exception {
        Map<String, Object> fields = new HashMap<String, Object>();
        String newAccountName = ENTITY_NAME_PREFIX + "-" + System.nanoTime();
        fields.put("name", newAccountName);
        RestResponse response = restClient.sendSync(RestRequest.getRequestForCreate(TestCredentials.API_VERSION, "account", fields));
        String newAccountId = response.asJSONObject().getString("id");
        return new IdName(newAccountId, newAccountName);
    }

    /**
     * Helper method to delete any entities created by one of the test
     */
    private void cleanup() {
        try {
            RestResponse searchResponse = restClient.sendSync(RestRequest.getRequestForSearch(TestCredentials.API_VERSION, "find {" + ENTITY_NAME_PREFIX + "}"));
            JSONArray matchingRows = searchResponse.asJSONArray();
            for (int i = 0; i < matchingRows.length(); i++) {
                JSONObject matchingRow = matchingRows.getJSONObject(i);
                String matchingRowType = matchingRow.getJSONObject("attributes").getString("type");
                String matchingRowId = matchingRow.getString("Id");
                restClient.sendSync(RestRequest.getRequestForDelete(TestCredentials.API_VERSION, matchingRowType, matchingRowId));
            }
        }
        catch(Exception e) {
            // We tried our best :-(
        }
    }

    /**
     * Helper method to validate responses
     * @param response
     * @param expectedStatusCode
     */
    private void checkResponse(RestResponse response, int expectedStatusCode, boolean isJsonArray) {
        // Check status code
        assertEquals(expectedStatusCode  + " response expected", expectedStatusCode, response.getStatusCode());

        // Try to parse as json
        try {
            if (isJsonArray) {
                response.asJSONArray();
            }
            else {
                response.asJSONObject();
            }
        }
        catch (Exception e) {
            fail("Failed to parse response body");
            e.printStackTrace();
        }
    }

    /**
     * Helper method to check if a jsonObject has all the expected keys
     * @param jsonObject
     * @param expectedKeys
     */
    private void checkKeys(JSONObject jsonObject, String... expectedKeys) {
        for (String expectedKey : expectedKeys) {
            assertTrue("Object should have key: " + expectedKey, jsonObject.has(expectedKey));
        }
    }

    /**
     * Helper class to hold name and id
     */
    private static class IdName {
        public final String id;
        public final String name;

        public IdName(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
