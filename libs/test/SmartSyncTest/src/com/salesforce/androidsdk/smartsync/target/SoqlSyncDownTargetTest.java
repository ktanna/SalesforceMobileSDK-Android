/*
 * Copyright (c) 2017-present, salesforce.com, inc.
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
import com.salesforce.androidsdk.smartsync.util.SOQLBuilder;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

/**
 * Test class for SoqlSyncDownTarget.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SoqlSyncDownTargetTest extends SyncManagerTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test addFilterForReSync with various queries
     */
    @Test
    public void testAddFilterForResync() {
        Date date = new Date();
        long dateLong = date.getTime();
        String dateStr = Constants.TIMESTAMP_FORMAT.format(date);
        Assert.assertEquals("Wrong result for addFilterForReSync", "select Id from Account where LastModifiedDate > " + dateStr, SoqlSyncDownTarget.addFilterForReSync("select Id from Account", "LastModifiedDate", dateLong));
        Assert.assertEquals("Wrong result for addFilterForReSync", "select Id from Account where otherDate > " + dateStr, SoqlSyncDownTarget.addFilterForReSync("select Id from Account", "otherDate", dateLong));
        Assert.assertEquals("Wrong result for addFilterForReSync", "select Id from Account where LastModifiedDate > " + dateStr + " limit 100", SoqlSyncDownTarget.addFilterForReSync("select Id from Account limit 100", "LastModifiedDate", dateLong));
        Assert.assertEquals("Wrong result for addFilterForReSync", "select Id from Account where LastModifiedDate > " + dateStr + " and Name = 'John'", SoqlSyncDownTarget.addFilterForReSync("select Id from Account where Name = 'John'", "LastModifiedDate", dateLong));
        Assert.assertEquals("Wrong result for addFilterForReSync", "select Id from Account where LastModifiedDate > " + dateStr + " and Name = 'John' limit 100", SoqlSyncDownTarget.addFilterForReSync("select Id from Account where Name = 'John' limit 100", "LastModifiedDate", dateLong));
        Assert.assertEquals("Wrong result for addFilterForReSync", "select Id from Account where LastModifiedDate > " + dateStr, SoqlSyncDownTarget.addFilterForReSync("SELECT Id FROM Account", "LastModifiedDate", dateLong));
        Assert.assertEquals("Wrong result for addFilterForReSync", "select Id from Account where LastModifiedDate > " + dateStr + " limit 100", SoqlSyncDownTarget.addFilterForReSync("SELECT Id FROM Account LIMIT 100", "LastModifiedDate", dateLong));
        Assert.assertEquals("Wrong result for addFilterForReSync", "select Id from Account where LastModifiedDate > " + dateStr + " and Name = 'John'", SoqlSyncDownTarget.addFilterForReSync("SELECT Id FROM Account WHERE Name = 'John'", "LastModifiedDate", dateLong));
        Assert.assertEquals("Wrong result for addFilterForReSync", "select Id from Account where LastModifiedDate > " + dateStr + " and Name = 'John' limit 100", SoqlSyncDownTarget.addFilterForReSync("SELECT Id FROM Account WHERE Name = 'John' LIMIT 100", "LastModifiedDate", dateLong));
    }

    /**
     * Test getSoqlForRemoteIds for SoqlSyncDownTarget
     */
    @Test
    public void testGetSoqlForRemoteIds() {
        SoqlSyncDownTarget target = new SoqlSyncDownTarget("SELECT Name FROM Account WHERE Name = 'James Bond'");
        Assert.assertEquals("select Id from Account where Name = 'James Bond'", target.getSoqlForRemoteIds());
    }

    /**
     * Test query with subqueries
     */
    @Test
    public void testQueryWithSubqueries() throws Exception {
        SoqlSyncDownTarget targetWithSubqueryInSelect = new SoqlSyncDownTarget("SELECT Name, (SELECT Contact.LastName FROM Account.Contacts) FROM Account WHERE Name = 'James Bond' LIMIT 10");
        Assert.assertEquals("select Id from Account where Name = 'James Bond' limit 10", targetWithSubqueryInSelect.getSoqlForRemoteIds());
        SoqlSyncDownTarget targetWithSubqueryInWhere = new SoqlSyncDownTarget("SELECT Name FROM Account WHERE Id IN (SELECT Id FROM Account WHERE Name = 'James Bond' LIMIT 10)");
        Assert.assertEquals("select Id from Account where Id IN (SELECT Id FROM Account WHERE Name = 'James Bond' LIMIT 10)", targetWithSubqueryInWhere.getSoqlForRemoteIds());
        SoqlSyncDownTarget targetWithSubqueries = new SoqlSyncDownTarget("SELECT Name, (SELECT Contact.LastName FROM Account.Contacts) from Account where Id IN (SELECT Id FROM Account WHERE Name = 'James Bond' LIMIT 10)");
        Assert.assertEquals("select Id from Account where Id IN (SELECT Id FROM Account WHERE Name = 'James Bond' LIMIT 10)", targetWithSubqueries.getSoqlForRemoteIds());
    }

    /**
     * Test query with "From_customer__c" field
     */
    @Test
    public void testQueryWithFromField() throws Exception {
        SoqlSyncDownTarget target = new SoqlSyncDownTarget("SELECT From_customer__c FROM Account WHERE Name = 'James Bond' LIMIT 10");
        Assert.assertEquals("select Id from Account where Name = 'James Bond' limit 10", target.getSoqlForRemoteIds());
    }


    /**
     * Tests if missing fields / order by are added to a SOQL target.
     */
    @Test
    public void testAddMissingFieldsAndOrderByToSOQLTarget() {
        String soqlExpected = "select Id,LastModifiedDate,FirstName, LastName from Contact order by LastModifiedDate";
        SoqlSyncDownTarget target = new SoqlSyncDownTarget("select FirstName, LastName from Contact");
        Assert.assertEquals("SOQL query should contain Id and LastModifiedDate fields", soqlExpected, target.getQuery());
    }

}
