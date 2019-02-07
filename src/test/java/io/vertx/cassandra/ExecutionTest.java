/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.vertx.cassandra;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test Casssandra client on how it executes queries.
 */
@RunWith(VertxUnitRunner.class)
public class ExecutionTest extends CassandraClientTestBase {

  @Test
  public void tableHaveSomeRows(TestContext testContext) {
    initializeRandomStringKeyspace(1);
    String query = "select count(*) as cnt from random_strings.random_string_by_first_letter";
    client.execute(query, testContext.asyncAssertSuccess(resultSet -> {
      resultSet.one(testContext.asyncAssertSuccess(row -> {
        testContext.assertTrue(row.getLong("cnt") > 0);
      }));
    }));
  }

  @Test
  public void simpleExecuteWithBigAmountOfFetches(TestContext testContext) {
    initializeRandomStringKeyspace(50);
    String query = "select random_string from random_strings.random_string_by_first_letter where first_letter = 'B'";
    SimpleStatement statement = new SimpleStatement(query);
    // we would like to test that we are able to handle several fetches.
    // that is why we are setting a small fetch size
    statement.setFetchSize(3);
    client.executeWithFullFetch(statement, testContext.asyncAssertSuccess(rows -> {
      for (Row row : rows) {
        String selectedString = row.getString(0);
        testContext.assertNotNull(selectedString);
      }
    }));
  }

  @Test
  public void preparedStatementsShouldWork(TestContext testContext) {
    initializeNamesKeyspace();
    String insert = "INSERT INTO names.names_by_first_letter (first_letter, name) VALUES (?, ?)";
    client.prepare(insert, testContext.asyncAssertSuccess(prepared -> {
      Statement statement = prepared.bind("P", "Pavel");
      client.execute(statement, testContext.asyncAssertSuccess(exec -> {
        String select = "select NAME as n from names.names_by_first_letter where first_letter = 'P'";
        client.executeWithFullFetch(select, testContext.asyncAssertSuccess(rows -> {
          testContext.assertTrue(rows.get(0).getString("n").equals("Pavel"));
        }));
      }));
    }));
  }
}
