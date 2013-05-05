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

package org.jboss.errai.otec.client;

import org.jboss.errai.otec.client.operation.OTOperation;

import java.util.List;

/**
 * @author Christian Sadilek <csadilek@redhat.com>
 * @author Mike Brock
 */
public interface TransactionLog {
  public Object getLock();

  public List<OTOperation> getLog();

  public List<OTOperation> getLogFromId(int revision, boolean includeNonCanon);

  public List<OTOperation> getCanonLog();

  public void insertLog(int revision, OTOperation operation);

  public void appendLog(OTOperation operation);

  State getEffectiveStateForRevision(int revision);

  int purgeTo(int revision);

  List<OTOperation> pruneFromOperation(OTOperation operation);

  public void appendRemoteLog(String remotePeerId, OTOperation operation);

  public List<OTOperation> getRemoteLogFromId(String remotePeerId, int revision, boolean includeNonCanon);

  State getEffectiveStateForRemoteRevision(String remotePeerId, int revision);

  void markDirty();

  void cleanLog();

  void logDebugDump();

  TransactionQueryResult findCommonParentsFor(OTOperation remoteOp);
}
