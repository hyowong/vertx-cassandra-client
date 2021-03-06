/*
 * Copyright 2019 The Vert.x Community.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vertx.cassandra.impl;

import io.vertx.cassandra.CassandraClient;
import io.vertx.cassandra.Mapper;
import io.vertx.cassandra.MappingManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.impl.ContextInternal;

import java.util.Objects;

/**
 * @author Martijn Zwennes
 */
public class MappingManagerImpl implements MappingManager {

  final CassandraClientImpl client;
  private com.datastax.driver.mapping.MappingManager mappingManager;

  public MappingManagerImpl(CassandraClient client) {
    Objects.requireNonNull(client, "client");
    this.client = (CassandraClientImpl) client;
  }

  @Override
  public <T> Mapper<T> mapper(Class<T> mappedClass) {
    return new MapperImpl<>(this, mappedClass);
  }

  synchronized void getMappingManager(ContextInternal context, Handler<AsyncResult<com.datastax.driver.mapping.MappingManager>> handler) {
    if (mappingManager != null) {
      handler.handle(Future.succeededFuture(mappingManager));
    } else {
      client.getSession(context, ar -> {
        if (ar.succeeded()) {
          com.datastax.driver.mapping.MappingManager manager;
          try {
            synchronized (this) {
              if (mappingManager == null) {
                mappingManager = new com.datastax.driver.mapping.MappingManager(ar.result());
              }
              manager = mappingManager;
            }
          } catch (Exception e) {
            handler.handle(Future.failedFuture(e));
            return;
          }
          handler.handle(Future.succeededFuture(manager));
        } else {
          handler.handle(Future.failedFuture(ar.cause()));
        }
      });
    }
  }
}
