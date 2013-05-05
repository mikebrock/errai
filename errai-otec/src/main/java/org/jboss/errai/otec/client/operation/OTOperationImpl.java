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
import org.jboss.errai.otec.client.State;
import org.jboss.errai.otec.client.mutation.Mutation;
import org.jboss.errai.otec.client.util.OTLogUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Christian Sadilek
 * @author Mike Brock
 */
public class OTOperationImpl implements OTOperation {
  private final OTEngine engine;
  private final List<Mutation> mutations;
  private final int entityId;
  private final String agentId;
  private final boolean propagate;
  private boolean resolvedConflict;
  private String revisionHash;
  private boolean nonCanon;
  private int revision;
  private int updatesRevision;

  private OTOperation transformedPath = this;

  private final OpPair transformedFrom;

  private OTOperationImpl(final OTEngine engine,
                          final List<Mutation> mutationList,
                          final int entityId,
                          final String agentId,
                          final int revision,
                          final int updatesRevision,
                          final String revisionHash,
                          final OpPair transformedFrom,
                          final boolean propagate,
                          final boolean resolvedConflict) {

    this.engine = engine;
    this.mutations = mutationList;
    this.entityId = entityId;
    this.agentId = agentId;
    this.revision = revision;
    this.updatesRevision = updatesRevision;
    this.revisionHash = revisionHash;
    this.transformedFrom = transformedFrom;
    this.propagate = propagate;
    this.resolvedConflict = resolvedConflict;
    if (transformedFrom != null) {
      transformedFrom.getRemoteOp().setOuterTransformedPath(this);
    }
  }

  public static OTOperation createNoop(final OTEngine engine, OTEntity entity) {
    return new OTOperationImpl(engine, Collections.<Mutation>emptyList(), entity.getId(), null, -1, -1, null, null, false, false);
  }

  public static OTOperation createLocalOnlyOperation(final OTEngine engine,
                                                     final List<Mutation> mutationList,
                                                     final OTEntity entity,
                                                     final String agentId,
                                                     final String hash,
                                                     final OpPair pair) {
    return new OTOperationImpl(engine, mutationList, entity.getId(), agentId, entity.getRevision(), -1, hash, pair, false, false);

  }

  public static OTOperation createLocalOnlyOperation(final OTEngine engine,
                                                     final List<Mutation> mutationList,
                                                     final OTEntity entity,
                                                     final int revision,
                                                     final OpPair pair) {

    return new OTOperationImpl(engine, mutationList, entity.getId(), engine.getId(), revision, -1, entity.getState().getHash(), pair, false, false);

  }

  public static OTOperation createOperation(final OTEngine engine,
                                            final List<Mutation> mutationList,
                                            final int entityId,
                                            final int revision,
                                            final String revisionHash) {
    return new OTOperationImpl(engine, mutationList, entityId, engine.getId(), revision, -1, revisionHash, null, true, false);

  }

  public static OTOperation createOperation(final OTEngine engine,
                                            final List<Mutation> mutationList,
                                            final int entityId,
                                            final String agentId,
                                            final int revision,
                                            final String revisionHash) {
    return new OTOperationImpl(engine, mutationList, entityId, agentId, revision, -1, revisionHash, null, true, false);

  }

  public static OTOperation createOperation(final OTEngine engine,
                                            final List<Mutation> mutationList,
                                            final int entityId,
                                            final String agentId,
                                            final int revision,
                                            final String revisionHash,
                                            final OpPair transformedFrom) {

    return new OTOperationImpl(engine, mutationList, entityId, agentId, revision, -1, revisionHash, transformedFrom, true, false);
  }

//  public static OTOperation createLocalOnlyOperation(final OTEngine engine,
//                                                     final List<Mutation> mutationList,
//                                                     final int entityId,
//                                                     final int revision,
//                                                     final String revisionHash,
//                                                     final OpPair transformedFrom) {
//
//    return new OTOperationImpl(engine, mutationList, entityId, revision, revisionHash, transformedFrom, false, false);
//  }

  public static OTOperation createLocalOnlyOperation(final OTEngine engine, final OTOperation operation) {
    return new OTOperationImpl(engine, operation.getMutations(), operation.getEntityId(),
        operation.getAgentId(), operation.getRevision(), operation.getUpdatesRevision(), operation.getRevisionHash(), operation.getTransformedFrom(),
        false, operation.isResolvedConflict());
  }

  public static OTOperation createOperation(final OTOperation op) {
    return new OTOperationImpl(op.getEngine(), op.getMutations(), op.getEntityId(), op.getAgentId(), -1, op.getUpdatesRevision(),
        op.getRevisionHash(), op.getTransformedFrom(), op.shouldPropagate(), op.isResolvedConflict());
  }

  public static OTOperation createOperation(final OTOperation op, final OpPair transformedFrom) {
    return new OTOperationImpl(op.getEngine(), op.getMutations(), op.getEntityId(), op.getAgentId(), -1, op.getUpdatesRevision(),
        op.getRevisionHash(), transformedFrom, op.shouldPropagate(), op.isResolvedConflict());
  }

  @Override
  public List<Mutation> getMutations() {
    return mutations;
  }

  @Override
  public int getEntityId() {
    return entityId;
  }

  @Override
  public int getRevision() {
    return revision;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean apply(final OTEntity entity) {
    try {

      revisionHash = entity.getState().getHash();
      if (revision == -1) {
        revision = entity.getRevision();
      }

      if (nonCanon)
        return shouldPropagate();

      final State state = entity.getState();

      for (final Mutation mutation : mutations) {
        mutation.apply(state);
      }

      assert OTLogUtil.log("APPLY", toString(), "-", engine.toString(), revision, "\"" + entity.getState().get() + "\"");

      entity.getTransactionLog().appendLog(this);
      entity.incrementRevision();
      return shouldPropagate();
    }
    catch (Throwable t) {
      t.printStackTrace();
      throw new RuntimeException("failed to apply op", t);
    }
  }

  @Override
  public void removeFromCanonHistory() {
    nonCanon = true;
  }

  @Override
  public void markAsResolvedConflict() {
    resolvedConflict = true;
  }

  @Override
  public boolean isCanon() {
    return !nonCanon;
  }

  @Override
  public boolean shouldPropagate() {
    return propagate;
  }

  @Override
  public OTEngine getEngine() {
    return engine;
  }

  @Override
  public boolean isNoop() {
    return mutations.isEmpty();
  }

  @Override
  public String getAgentId() {
    return agentId;
  }

  @Override
  public boolean isResolvedConflict() {
    return resolvedConflict;
  }

  @Override
  public OTOperation getBasedOn(final int revision) {
    return new OTOperationImpl(engine, mutations, entityId, agentId, revision, updatesRevision, revisionHash, transformedFrom, propagate, resolvedConflict);
  }

  @Override
  public OpPair getTransformedFrom() {
    return transformedFrom;
  }

  @Override
  public void setOuterTransformedPath(OTOperation operation) {
    transformedPath = operation;
  }

  @Override
  public int getUpdatesRevision() {
    return updatesRevision;
  }

  @Override
  public void setUpdatesRevision(int updatesRevision) {
    this.updatesRevision = updatesRevision;
  }

  @Override
  public OTOperation getOuterTransformedPath() {
    return transformedPath;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o)
      return true;
    if (!(o instanceof OTOperationImpl))
      return false;

    final OTOperationImpl that = (OTOperationImpl) o;

    return entityId == that.entityId && mutations.equals(that.mutations);
  }

  @Override
  public int hashCode() {
    int result = mutations.hashCode();
    result = 31 * result + entityId;
    return result;
  }

  @Override
  public String toString() {
    return Arrays.toString(mutations.toArray());
  }

  @Override
  public String getRevisionHash() {
    return revisionHash;
  }
}