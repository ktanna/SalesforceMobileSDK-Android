/*
 * Copyright (c) 2019-present, salesforce.com, inc.
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

package com.salesforce.androidsdk.smartsync.target;

import com.salesforce.androidsdk.smartsync.manager.SyncManagerTestCase;
import com.salesforce.androidsdk.smartsync.util.Constants;
import com.salesforce.androidsdk.smartsync.util.SyncState;

import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

/**
 * Test class for SyncUpTarget.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SyncUpTargetTest extends SyncManagerTestCase {

    // Misc
    protected static final int COUNT_TEST_ACCOUNTS = 10;
    protected Map<String, Map<String, Object>> idToFields;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        createAccountsSoup();
        idToFields = createRecordsOnServerReturnFields(COUNT_TEST_ACCOUNTS, Constants.ACCOUNT, null);
    }

    @After
    public void tearDown() throws Exception {
        deleteRecordsOnServer(idToFields.keySet(), Constants.ACCOUNT);
        dropAccountsSoup();
        super.tearDown();
    }

    /**
     * Sync down the test accounts, modify a few, sync up specifying update field list, check smartstore and server afterwards
     */
    @Test
    public void testSyncUpWithUpdateFieldList() throws Exception {
        // First sync down
        trySyncDown(SyncState.MergeMode.OVERWRITE);

        // Update a few entries locally
        Map<String, Map<String, Object>> idToFieldsLocallyUpdated = makeLocalChanges(idToFields, ACCOUNTS_SOUP);

        // Sync up with update field list including only name
        trySyncUp(idToFieldsLocallyUpdated.size(), SyncState.MergeMode.OVERWRITE, null, Arrays.asList(new String[] { Constants.NAME }));

        // Check that db doesn't show entries as locally modified anymore
        Set<String> ids = idToFieldsLocallyUpdated.keySet();
        checkDbStateFlags(ids, false, false, false, ACCOUNTS_SOUP);

        // Check server - make sure only name was updated
        Map<String, Map<String, Object>> idToFieldsExpectedOnServer = new HashMap<>();
        for (String id : idToFieldsLocallyUpdated.keySet()) {
            // Should have modified name but original description
            Map<String, Object> expectedFields = new HashMap<>();
            expectedFields.put(Constants.NAME, idToFieldsLocallyUpdated.get(id).get(Constants.NAME));
            expectedFields.put(Constants.DESCRIPTION, idToFields.get(id).get(Constants.DESCRIPTION));
            idToFieldsExpectedOnServer.put(id, expectedFields);
        }
        checkServer(idToFieldsExpectedOnServer, Constants.ACCOUNT);
    }

    /**
     * Create accounts locally, sync up specifying create field list, check smartstore and server afterwards
     */
    @Test
    public void testSyncUpWithCreateFieldList() throws Exception {
        // Create a few entries locally
        String[] names = new String[] { createRecordName(Constants.ACCOUNT),
                createRecordName(Constants.ACCOUNT),
                createRecordName(Constants.ACCOUNT) };
        createAccountsLocally(names);

        // Sync up with create field list including only name
        trySyncUp(3, SyncState.MergeMode.OVERWRITE, Arrays.asList(new String[] { Constants.NAME }), null);

        // Check that db doesn't show entries as locally created anymore and that they use sfdc id
        Map<String, Map<String, Object>> idToFieldsCreated = getIdToFieldsByName(ACCOUNTS_SOUP, new String[]{Constants.NAME, Constants.DESCRIPTION}, Constants.NAME, names);
        checkDbStateFlags(idToFieldsCreated.keySet(), false, false, false, ACCOUNTS_SOUP);

        // Check server - make sure only name was set
        Map<String, Map<String, Object>> idToFieldsExpectedOnServer = new HashMap<>();
        for (String id : idToFieldsCreated.keySet()) {
            // Should have name but no description
            Map<String, Object> expectedFields = new HashMap<>();
            expectedFields.put(Constants.NAME, idToFieldsCreated.get(id).get(Constants.NAME));
            expectedFields.put(Constants.DESCRIPTION, null);
            idToFieldsExpectedOnServer.put(id, expectedFields);
        }
        checkServer(idToFieldsExpectedOnServer, Constants.ACCOUNT);

        // Adding to idToFields so that they get deleted in tearDown.
        idToFields.putAll(idToFieldsCreated);
    }

    /**
     * Create a few records - some with bad names (too long or empty)
     * Sync up
     * Make sure the records with bad names are still marked as locally created and have the last error field populated
     * @throws Exception
     */
    @Test
    public void testSyncUpWithErrors() throws Exception {
        // Build name too long
        StringBuffer buffer = new StringBuffer(256);
        for (int i = 0; i < 256; i++) buffer.append("x");
        String nameTooLong = buffer.toString();

        // Create a few entries locally
        String[] goodNames = new String[]{
                createRecordName(Constants.ACCOUNT),
                createRecordName(Constants.ACCOUNT),
                createRecordName(Constants.ACCOUNT)
        };

        String[] badNames = new String[] {
                nameTooLong,
                "" // empty
        };
        createAccountsLocally(goodNames);
        createAccountsLocally(badNames);

        // Sync up
        trySyncUp(5, SyncState.MergeMode.OVERWRITE);

        // Check db for records with good names
        Map<String, Map<String, Object>> idToFieldsGoodNames = getIdToFieldsByName(ACCOUNTS_SOUP, new String[]{Constants.NAME, Constants.DESCRIPTION}, Constants.NAME, goodNames);
        checkDbStateFlags(idToFieldsGoodNames.keySet(), false, false, false, ACCOUNTS_SOUP);

        // Check db for records with bad names
        Map<String, Map<String, Object>> idToFieldsBadNames = getIdToFieldsByName(ACCOUNTS_SOUP, new String[]{Constants.NAME, Constants.DESCRIPTION, SyncTarget.LAST_ERROR}, Constants.NAME, badNames);
        checkDbStateFlags(idToFieldsBadNames.keySet(), true, false, false, ACCOUNTS_SOUP);

        for (Map<String, Object> fields : idToFieldsBadNames.values()) {
            String name = (String) fields.get(Constants.NAME);
            String lastError = (String) fields.get(SyncTarget.LAST_ERROR);
            if (name.equals(nameTooLong)) {
                Assert.assertTrue("Name too large error expected", lastError.contains("Account Name: data value too large"));
            }
            else if (name.equals("")) {
                Assert.assertTrue("Missing name error expected", lastError.contains("Required fields are missing: [Name]"));
            }
            else {
                Assert.fail("Unexpected record found: " + name);
            }
        }

        // Check server for records with good names
        checkServer(idToFieldsGoodNames, Constants.ACCOUNT);

        // Adding to idToFields so that they get deleted in tearDown
        idToFields.putAll(idToFieldsGoodNames);
    }

    /**
     * Sync down the test accounts, modify a few, sync up, check smartstore and server afterwards
     */
    @Test
    public void testSyncUpWithLocallyUpdatedRecords() throws Exception {
        // First sync down
        trySyncDown(SyncState.MergeMode.OVERWRITE);

        // Update a few entries locally
        Map<String, Map<String, Object>> idToFieldsLocallyUpdated = makeLocalChanges(idToFields, ACCOUNTS_SOUP);

        // Sync up
        trySyncUp(3, SyncState.MergeMode.OVERWRITE);

        // Check that db doesn't show entries as locally modified anymore
        Set<String> ids = idToFieldsLocallyUpdated.keySet();
        checkDbStateFlags(ids, false, false, false, ACCOUNTS_SOUP);

        // Check server
        checkServer(idToFieldsLocallyUpdated, Constants.ACCOUNT);
    }

    /**
     * Sync down the test accounts, update a few locally,
     * update a few on server,
     * Sync up with merge mode LEAVE_IF_CHANGED, check smartstore and server
     * Then sync up again with merge mode OVERWRITE, check smartstore and server
     */
    @Test
    public void testSyncUpWithLocallyUpdatedRecordsWithoutOverwrite() throws Exception {
        // First sync down
        trySyncDown(SyncState.MergeMode.LEAVE_IF_CHANGED);

        // Update a few entries locally
        Map<String, Map<String, Object>> idToFieldsLocallyUpdated = makeLocalChanges(idToFields, ACCOUNTS_SOUP);

        // Update entries on server
        Thread.sleep(1000); // time stamp precision is in seconds
        final Map<String,  Map<String, Object>> idToFieldsRemotelyUpdated = new HashMap<>();
        final Set<String> ids = idToFieldsLocallyUpdated.keySet();
        Assert.assertNotNull("List of IDs should not be null", ids);
        for (final String id : ids) {
            Map<String, Object> fields = idToFieldsLocallyUpdated.get(id);
            Map<String, Object> updatedFields = new HashMap<>();
            for (final String fieldName : fields.keySet()) {
                updatedFields.put(fieldName, fields.get(fieldName) + "_updated_again");
            }
            idToFieldsRemotelyUpdated.put(id, updatedFields);
        }
        updateRecordsOnServer(idToFieldsRemotelyUpdated, Constants.ACCOUNT);

        // Sync up with leave-if-changed
        trySyncUp(3, SyncState.MergeMode.LEAVE_IF_CHANGED);

        // Check that db shows entries as locally modified
        checkDbStateFlags(ids, false, true, false, ACCOUNTS_SOUP);

        // Check server still has remote updates
        checkServer(idToFieldsRemotelyUpdated, Constants.ACCOUNT);

        // Sync up with overwrite
        trySyncUp(3, SyncState.MergeMode.OVERWRITE);

        // Check that db no longer shows entries as locally modified
        checkDbStateFlags(ids, false, false, false, ACCOUNTS_SOUP);

        // Check server has local updates
        checkServer(idToFieldsLocallyUpdated, Constants.ACCOUNT);
    }

    /**
     * Create accounts locally, sync up with merge mode OVERWRITE, check smartstore and server afterwards
     */
    @Test
    public void testSyncUpWithLocallyCreatedRecords() throws Exception {
        trySyncUpWithLocallyCreatedRecords(SyncState.MergeMode.OVERWRITE);
    }

    /**
     * Create accounts locally, sync up with mege mode LEAVE_IF_CHANGED, check smartstore and server afterwards
     */
    @Test
    public void testSyncUpWithLocallyCreatedRecordsWithoutOverwrite() throws Exception {
        trySyncUpWithLocallyCreatedRecords(SyncState.MergeMode.LEAVE_IF_CHANGED);
    }

    private void trySyncUpWithLocallyCreatedRecords(SyncState.MergeMode syncUpMergeMode) throws Exception {
        // Create a few entries locally
        String[] names = new String[] { createRecordName(Constants.ACCOUNT),
                createRecordName(Constants.ACCOUNT),
                createRecordName(Constants.ACCOUNT) };
        createAccountsLocally(names);

        // Sync up
        trySyncUp(3, syncUpMergeMode);

        // Check that db doesn't show entries as locally created anymore and that they use sfdc id
        Map<String, Map<String, Object>> idToFieldsCreated = getIdToFieldsByName(ACCOUNTS_SOUP, new String[]{Constants.NAME, Constants.DESCRIPTION}, Constants.NAME, names);
        checkDbStateFlags(idToFieldsCreated.keySet(), false, false, false, ACCOUNTS_SOUP);

        // Check server
        checkServer(idToFieldsCreated, Constants.ACCOUNT);

        // Adding to idToFields so that they get deleted in tearDown
        idToFields.putAll(idToFieldsCreated);
    }

    /**
     * Sync down the test accounts, delete a few, sync up, check smartstore and server afterwards
     */
    @Test
    public void testSyncUpWithLocallyDeletedRecords() throws Exception {
        // First sync down
        trySyncDown(SyncState.MergeMode.OVERWRITE);

        // Delete a few entries locally
        String[] allIds = idToFields.keySet().toArray(new String[0]);
        String[] idsLocallyDeleted = new String[] { allIds[0], allIds[1], allIds[2] };
        deleteRecordsLocally(ACCOUNTS_SOUP, idsLocallyDeleted);

        // Sync up
        trySyncUp(3, SyncState.MergeMode.OVERWRITE);

        // Check that db doesn't contain those entries anymore
        checkDbDeleted(ACCOUNTS_SOUP, idsLocallyDeleted, Constants.ID);

        // Check server
        checkServerDeleted(idsLocallyDeleted, Constants.ACCOUNT);
    }

    /**
     * Create accounts locally, delete them locally, sync up with merge mode LEAVE_IF_CHANGED, check smartstore
     *
     * Ideally an application that deletes locally created records should simply remove them from the smartstore
     * But if records are kept in the smartstore and are flagged as created and deleted (or just deleted), then
     * sync up should not throw any error and the records should end up being removed from the smartstore
     */
    @Test
    public void testSyncUpWithLocallyCreatedAndDeletedRecords() throws Exception {
        // Create a few entries locally
        String[] names = new String[] { createRecordName(Constants.ACCOUNT), createRecordName(Constants.ACCOUNT), createRecordName(Constants.ACCOUNT)};
        createAccountsLocally(names);
        Map<String, Map<String, Object>> idToFieldsCreated = getIdToFieldsByName(ACCOUNTS_SOUP, new String[]{Constants.NAME, Constants.DESCRIPTION}, Constants.NAME, names);

        String[] allIds = idToFieldsCreated.keySet().toArray(new String[0]);
        String[] idsLocallyDeleted = new String[] { allIds[0], allIds[1], allIds[2] };
        deleteRecordsLocally(ACCOUNTS_SOUP, idsLocallyDeleted);

        // Sync up
        trySyncUp(3, SyncState.MergeMode.LEAVE_IF_CHANGED);

        // Check that db doesn't contain those entries anymore
        checkDbDeleted(ACCOUNTS_SOUP, idsLocallyDeleted, Constants.ID);
    }

    /**
     * Sync down the test accounts, delete a few locally,
     * update a few on server,
     * Sync up with merge mode LEAVE_IF_CHANGED, check smartstore and server
     * Then sync up again with merge mode OVERWRITE, check smartstore and server
     */
    @Test
    public void testSyncUpWithLocallyDeletedRecordsWithoutOverwrite() throws Exception {
        // First sync down
        trySyncDown(SyncState.MergeMode.LEAVE_IF_CHANGED);

        // Delete a few entries locally
        String[] allIds = idToFields.keySet().toArray(new String[0]);
        String[] idsLocallyDeleted = new String[] { allIds[0], allIds[1], allIds[2] };
        deleteRecordsLocally(ACCOUNTS_SOUP, idsLocallyDeleted);

        // Update entries on server
        Thread.sleep(1000); // time stamp precision is in seconds
        final Map<String, Map<String, Object>> idToFieldsRemotelyUpdated = new HashMap<>();
        for (int i = 0; i < idsLocallyDeleted.length; i++) {
            String id = idsLocallyDeleted[i];
            Map<String, Object> updatedFields = updatedFields(idToFields.get(id), REMOTELY_UPDATED);
            idToFieldsRemotelyUpdated.put(id, updatedFields);
        }
        updateRecordsOnServer(idToFieldsRemotelyUpdated, Constants.ACCOUNT);

        // Sync up with leave-if-changed
        trySyncUp(3, SyncState.MergeMode.LEAVE_IF_CHANGED);

        // Check that db still contains those entries
        checkDbStateFlags(Arrays.asList(idsLocallyDeleted), false, false, true, ACCOUNTS_SOUP);

        // Check server
        checkServer(idToFieldsRemotelyUpdated, Constants.ACCOUNT);

        // Sync up with overwrite
        trySyncUp(3, SyncState.MergeMode.OVERWRITE);

        // Check that db no longer contains deleted records
        checkDbDeleted(ACCOUNTS_SOUP, idsLocallyDeleted, Constants.ID);

        // Check server no longer contains deleted record
        checkServerDeleted(idsLocallyDeleted, Constants.ACCOUNT);
    }

    /**
     * Sync down the test accounts, delete record on server and locally, sync up, check smartstore and server afterwards
     */
    @Test
    public void testSyncUpWithLocallyDeletedRemotelyDeletedRecords() throws Exception {
        // First sync down
        trySyncDown(SyncState.MergeMode.OVERWRITE);

        // Delete record locally
        String[] allIds = idToFields.keySet().toArray(new String[0]);
        String[] idsLocallyDeleted = new String[] { allIds[0], allIds[1], allIds[2] };
        deleteRecordsLocally(ACCOUNTS_SOUP, idsLocallyDeleted);

        // Delete same records on server
        deleteRecordsOnServer(idToFields.keySet(), Constants.ACCOUNT);

        // Sync up
        trySyncUp(3, SyncState.MergeMode.OVERWRITE);

        // Check that db doesn't contain those entries anymore
        checkDbDeleted(ACCOUNTS_SOUP, idsLocallyDeleted, Constants.ID);

        // Check server
        checkServerDeleted(idsLocallyDeleted, Constants.ACCOUNT);
    }

    /**
     * Sync down the test accounts, delete record on server and update same record locally, sync up, check smartstore and server afterwards
     */
    @Test
    public void testSyncUpWithLocallyUpdatedRemotelyDeletedRecords() throws Exception {
        // First sync down
        trySyncDown(SyncState.MergeMode.OVERWRITE);

        // Update a few entries locally
        Map<String, Map<String, Object>> idToFieldsLocallyUpdated = makeLocalChanges(idToFields, ACCOUNTS_SOUP);

        // Delete record on server
        String remotelyDeletedId = idToFieldsLocallyUpdated.keySet().toArray(new String[0])[0];
        deleteRecordsOnServer(new HashSet<String>(Arrays.asList(remotelyDeletedId)), Constants.ACCOUNT);

        // Name of locally recorded record that was deleted on server
        String locallyUpdatedRemotelyDeletedName = (String) idToFieldsLocallyUpdated.get(remotelyDeletedId).get(Constants.NAME);

        // Sync up
        trySyncUp(3, SyncState.MergeMode.OVERWRITE);

        // Getting id / fields of updated records looking up by name
        Map<String, Map<String, Object>> idToFieldsUpdated = getIdToFieldsByName(ACCOUNTS_SOUP, new String[]{Constants.NAME, Constants.DESCRIPTION}, Constants.NAME, getNamesFromIdToFields(idToFieldsLocallyUpdated));

        // Check db
        checkDb(idToFieldsUpdated, ACCOUNTS_SOUP);

        // Expect 3 records
        Assert.assertEquals(3, idToFieldsUpdated.size());

        // Expect remotely deleted record to have a new id
        Assert.assertFalse(idToFieldsUpdated.containsKey(remotelyDeletedId));
        for (String accountId : idToFieldsUpdated.keySet()) {
            String accountName = (String) idToFieldsUpdated.get(accountId).get(Constants.NAME);

            // Check that locally updated / remotely deleted record has new id (not in idToFields)
            if (accountName.equals(locallyUpdatedRemotelyDeletedName)) {
                Assert.assertFalse(idToFields.containsKey(accountId));

                //update the record entry using the new id
                idToFields.remove(remotelyDeletedId);
                idToFields.put(accountId, idToFieldsUpdated.get(accountId));
            }
            // Otherwise should be a known id (in idToFields)
            else {
                Assert.assertTrue(idToFields.containsKey(accountId));
            }
        }

        // Check server
        checkServer(idToFieldsUpdated, Constants.ACCOUNT);
    }

    /**
     * Sync down the test accounts, delete record on server and update same record locally, sync up with merge mode LEAVE_IF_CHANGED, check smartstore and server afterwards
     */
    @Test
    public void testSyncUpWithLocallyUpdatedRemotelyDeletedRecordsWithoutOverwrite() throws Exception {
        // First sync down
        trySyncDown(SyncState.MergeMode.OVERWRITE);

        // Update a few entries locally
        Map<String, Map<String, Object>> idToFieldsLocallyUpdated = makeLocalChanges(idToFields, ACCOUNTS_SOUP);

        // Delete record on server
        String remotelyDeletedId = idToFieldsLocallyUpdated.keySet().toArray(new String[0])[0];
        deleteRecordsOnServer(new HashSet<String>(Arrays.asList(remotelyDeletedId)), Constants.ACCOUNT);

        // Sync up
        trySyncUp(3, SyncState.MergeMode.LEAVE_IF_CHANGED);

        // Getting id / fields of updated records looking up by name
        Map<String, Map<String, Object>> idToFieldsUpdated = getIdToFieldsByName(ACCOUNTS_SOUP, new String[]{Constants.NAME, Constants.DESCRIPTION}, Constants.NAME, getNamesFromIdToFields(idToFieldsLocallyUpdated));

        // Expect 3 records
        Assert.assertEquals(3, idToFieldsUpdated.size());

        // Expect remotely deleted record to be there
        Assert.assertTrue(idToFieldsUpdated.containsKey(remotelyDeletedId));

        // Checking the remotely deleted record locally
        checkDbStateFlags(Arrays.asList(new String[]{remotelyDeletedId}), false, true, false, ACCOUNTS_SOUP);

        // Check the other 2 records in db
        HashMap<String, Map<String, Object>> otherIdtoFields = new HashMap<>(idToFieldsLocallyUpdated);
        otherIdtoFields.remove(remotelyDeletedId);
        checkDb(otherIdtoFields, ACCOUNTS_SOUP);

        // Check server
        checkServer(otherIdtoFields, Constants.ACCOUNT);
        checkServerDeleted(new String[]{remotelyDeletedId}, Constants.ACCOUNT);
    }

    /**
     * Sync down the test accounts, modify a few, create accounts locally, sync up specifying different create and update field list,
     * check smartstore and server afterwards
     */
    @Test
    public void testSyncUpWithCreateAndUpdateFieldList() throws Exception {
        // First sync down
        trySyncDown(SyncState.MergeMode.OVERWRITE);

        // Update a few entries locally
        Map<String, Map<String, Object>> idToFieldsLocallyUpdated = makeLocalChanges(idToFields, ACCOUNTS_SOUP);
        String[] namesOfUpdated = getNamesFromIdToFields(idToFieldsLocallyUpdated);

        // Create a few entries locally
        String[] namesOfCreated = new String[] { createRecordName(Constants.ACCOUNT),
                createRecordName(Constants.ACCOUNT),
                createRecordName(Constants.ACCOUNT) };
        createAccountsLocally(namesOfCreated);

        // Sync up with different create and update field lists
        trySyncUp(namesOfCreated.length + namesOfUpdated.length, SyncState.MergeMode.OVERWRITE, Arrays.asList(new String[]{Constants.NAME}), Arrays.asList(new String[]{Constants.DESCRIPTION}));

        // Check that db doesn't show created entries as locally created anymore and that they use sfdc id
        Map<String, Map<String, Object>> idToFieldsCreated = getIdToFieldsByName(ACCOUNTS_SOUP, new String[]{Constants.NAME, Constants.DESCRIPTION}, Constants.NAME, namesOfCreated);
        checkDbStateFlags(idToFieldsCreated.keySet(), false, false, false, ACCOUNTS_SOUP);

        // Check that db doesn't show updated entries as locally modified anymore
        checkDbStateFlags(idToFieldsLocallyUpdated.keySet(), false, false, false, ACCOUNTS_SOUP);

        // Check server - make updated records only have updated description - make sure created records only have name
        Map<String, Map<String, Object>> idToFieldsExpectedOnServer = new HashMap<>();
        for (String id : idToFieldsCreated.keySet()) {
            // Should have name but no description
            Map<String, Object> expectedFields = new HashMap<>();
            expectedFields.put(Constants.NAME, idToFieldsCreated.get(id).get(Constants.NAME));
            expectedFields.put(Constants.DESCRIPTION, null);
            idToFieldsExpectedOnServer.put(id, expectedFields);
        }
        for (String id : idToFieldsLocallyUpdated.keySet()) {
            // Should have modified name but original description
            Map<String, Object> expectedFields = new HashMap<>();
            expectedFields.put(Constants.NAME, idToFields.get(id).get(Constants.NAME));
            expectedFields.put(Constants.DESCRIPTION, idToFieldsLocallyUpdated.get(id).get(Constants.DESCRIPTION));
            idToFieldsExpectedOnServer.put(id, expectedFields);
        }
        checkServer(idToFieldsExpectedOnServer, Constants.ACCOUNT);

        // Adding to idToFields so that they get deleted in tearDown.
        idToFields.putAll(idToFieldsCreated);
    }

    /**
     * Sync up helper
     * @param numberChanges
     * @param mergeMode
     * @throws JSONException
     */
    protected void trySyncUp(int numberChanges, SyncState.MergeMode mergeMode) throws JSONException {
        trySyncUp(numberChanges, mergeMode, null, null);
    }

    /**
     * Sync up helper
     * @param numberChanges
     * @param mergeMode
     * @throws JSONException
     */
    protected void trySyncUp(int numberChanges, SyncState.MergeMode mergeMode, List<String> createFieldlist, List<String> updateFieldlist) throws JSONException {
        trySyncUp(new SyncUpTarget(createFieldlist, updateFieldlist), numberChanges, mergeMode);
    }

    /**
     * Sync down helper
     * @throws JSONException
     * @param mergeMode
     */
    protected long trySyncDown(SyncState.MergeMode mergeMode) throws JSONException {
        return trySyncDown(mergeMode, null);
    }

    /**
     * Sync down helper
     * @throws JSONException
     * @param mergeMode
     */
    protected long trySyncDown(SyncState.MergeMode mergeMode, String syncName) throws JSONException {
        final SyncDownTarget target = new SoqlSyncDownTarget("SELECT Id, Name, Description, LastModifiedDate FROM Account WHERE Id IN " + makeInClause(idToFields.keySet()));
        return trySyncDown(mergeMode, target, ACCOUNTS_SOUP, idToFields.size(), 1, syncName);

    }

}
