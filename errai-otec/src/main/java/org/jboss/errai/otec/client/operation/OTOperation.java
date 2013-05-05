/*
 * Copyright 2013 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.otec.client.operation;

import org.jboss.errai.otec.client.OTEngine;
import org.jboss.errai.otec.client.OTEntity;
import org.jboss.errai.otec.client.mutation.Mutation;

import java.util.List;

/**
 * @author Christian Sadilek <csadilek@redhat.com>
 * @author Mike Brock
 */
public interface OTOperation {
  List<Mutation> getMutations();

  int getEntityId();

  int getRevision();

  String getAgentId();

  String getRevisionHash();
  
  boolean shouldPropagate();

  boolean apply(OTEntity entity);

  OTEngine getEngine();

  boolean isNoop();

  OTOperation getBasedOn(int revision);

  boolean isCanon();

  boolean isResolvedConflict();

  void removeFromCanonHistory();

  void markAsResolvedConflict();
  
  OpPair getTransformedFrom();

  void setOuterTransformedPath(OTOperation operation);

  OTOperation getOuterTransformedPath();

  int getUpdatesRevision();

  void setUpdatesRevision(int updatesRevision);
}
