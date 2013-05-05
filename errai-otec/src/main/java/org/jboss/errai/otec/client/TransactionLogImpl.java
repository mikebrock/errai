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

import org.jboss.errai.common.client.util.LogUtil;
import org.jboss.errai.otec.client.mutation.Mutation;
import org.jboss.errai.otec.client.operation.OTOperation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * @author Christian Sadilek <csadilek@redhat.com>
 * @author Mike Brock
 */
public class TransactionLogImpl implements TransactionLog {
  private final Object lock = new Object();

  private volatile boolean logDirty = false;
  private final List<StateSnapshot> stateSnapshots = new LinkedList<StateSnapshot>();
  private final List<OTOperation> transactionLog = new LinkedList<OTOperation>();
  private final Map<String, List<OTOperation>> remoteTransactionLogs = new HashMap<String, List<OTOperation>>();

  private final String engineId;
  private final OTEntity entity;

  private TransactionLogImpl(final String engineId, final OTEntity entity) {
    this.engineId = engineId;
    this.entity = entity;
    stateSnapshots.add(new StateSnapshot(entity.getRevision(), entity.getState().snapshot()));
  }

  public static TransactionLog createTransactionLog(final OTEngine engine, final OTEntity entity) {
    return new TransactionLogImpl(engine.getId(), entity);
  }

  @Override
  public Object getLock() {
    return lock;
  }

  @Override
  public List<OTOperation> getLog() {
    synchronized (lock) {
      return transactionLog;
    }
  }

  @Override
  public int purgeTo(final int revision) {
    if (revision < 0) {
      return 0;
    }
    synchronized (lock) {
      makeSnapshot(entity.getRevision(), entity.getState());

      int purged = 0;
      final Iterator<OTOperation> iterator = transactionLog.iterator();
      while (iterator.hasNext()) {
        if (iterator.next().getRevision() < revision) {
          purged++;
          iterator.remove();
        }
        else {
          break;
        }
      }

      if (stateSnapshots.size() > 1) {
        final Iterator<StateSnapshot> iterator1 = stateSnapshots.iterator();
        while (iterator1.hasNext()) {
          if (iterator1.next().getRevision() < revision) {
            iterator1.remove();
          }
          else {
            break;
          }
        }
      }

      return purged;
    }
  }

  @Override
  public TransactionQueryResult findCommonParentsFor(final OTOperation remoteOp) {
    synchronized (lock) {
      final List<OTOperation> remoteOps = remoteTransactionLogs.get(remoteOp.getAgentId());

      final ListIterator<OTOperation> remoteOpsIter = remoteOps.listIterator(remoteOps.size());

      while (remoteOpsIter.hasPrevious()) {
        final OTOperation rOp = remoteOpsIter.previous();
        final ListIterator<OTOperation> localOpsIter = transactionLog.listIterator(remoteOps.size());
        do {
          final OTOperation baseLocalOp = localOpsIter.previous();
          if (rOp.getRevisionHash().equals(baseLocalOp.getRevisionHash())) {
            // found!

            final State commonState = _getEffectiveStateFor(baseLocalOp.getRevision(), stateSnapshots, transactionLog);

            final TransactionQueryResult transactionQueryResult
                = new TransactionQueryResult(baseLocalOp.getRevisionHash(), commonState,
                baseLocalOp, rOp,
                filterAndRemoveCanonExceptFirst(transactionLog.subList(localOpsIter.nextIndex(), transactionLog.size())),
                remoteOps.subList(remoteOpsIter.nextIndex(), remoteOps.size()));

            return transactionQueryResult;

          }
        }
        while (localOpsIter.hasPrevious());
      }
    }

    return null;
   // throw new OTException("no common history!");
  }

  @Override
  public List<OTOperation> pruneFromOperation(final OTOperation operation) {
    synchronized (lock) {
      final int index = transactionLog.indexOf(operation);
      if (index == -1) {
        return Collections.emptyList();
      }

      final ListIterator<OTOperation> delIter = transactionLog.listIterator(index);
      final List<OTOperation> operationList = new ArrayList<OTOperation>();
      while (delIter.hasNext()) {
        entity.decrementRevisionCounter();
        final OTOperation next = delIter.next();
        operationList.add(next);
        next.removeFromCanonHistory();
      }
      logDirty = true;
      return operationList;
    }
  }

  @Override
  public List<OTOperation> getLogFromId(final int revision, final boolean includeNonCanon) {
    synchronized (lock) {
      return _getLogFromId(transactionLog, revision, includeNonCanon);
    }
  }

  @Override
  public List<OTOperation> getCanonLog() {
    synchronized (lock) {
      if (logDirty) {
        final List<OTOperation> canonLog = new ArrayList<OTOperation>(transactionLog.size());
        for (final OTOperation operation : transactionLog) {
          if (operation.isCanon()) {
            canonLog.add(operation);
          }
        }
        return canonLog;
      }
      else {
        return transactionLog;
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public State getEffectiveStateForRevision(final int revision) {
    synchronized (lock) {
      return _getEffectiveStateFor(revision, stateSnapshots, transactionLog);
    }
  }

  private static StateSnapshot getLatestParentSnapshot(final List<StateSnapshot> stateSnapshots, final int revision) {
    final ListIterator<StateSnapshot> snapshotListIterator = stateSnapshots.listIterator(stateSnapshots.size());

    while (snapshotListIterator.hasPrevious()) {
      final StateSnapshot stateSnapshot = snapshotListIterator.previous();
      if (stateSnapshot.getRevision() <= revision) {
        return stateSnapshot;
      }
    }

    LogUtil.log("NO_PARENT_STATE: " + revision);

    _logDebugDump(stateSnapshots, Collections.<OTOperation>emptyList());

    throw new RuntimeException("no parent state for: " + revision);
  }

  private void makeSnapshot(final int revision, final State state) {
    stateSnapshots.add(new StateSnapshot(revision, state));
    LogUtil.log("SNAPSHOT rev=" + revision + "; of=\"" + state.get() + "\"");
  }

  @Override
  public void appendLog(final OTOperation operation) {
    synchronized (lock) {
      if (operation.isNoop()) {
        return;
      }

      transactionLog.add(operation);
    }
  }

  @Override
  public void insertLog(int revision, OTOperation operation) {
    synchronized (lock) {
      final ListIterator<OTOperation> operationListIterator
          = transactionLog.listIterator(transactionLog.size());

      while (operationListIterator.hasPrevious()) {
        if (operationListIterator.previous().getRevision() == revision) {
          operationListIterator.set(operation);
          break;
        }
      }
    }
  }

  @Override
  public void appendRemoteLog(String remotePeerId, OTOperation operation) {
    synchronized (lock) {
      List<OTOperation> operationList = remoteTransactionLogs.get(remotePeerId);
      if (operationList == null) {
        remoteTransactionLogs.put(remotePeerId, operationList = new LinkedList<OTOperation>());
      }

      if (operation.getUpdatesRevision() != -1) {
        final Iterator<OTOperation> iter = operationList.iterator();
        while (iter.hasNext()) {
          if (iter.next().getRevision() == operation.getUpdatesRevision()) {
            iter.remove();
            break;
          }
        }
      }

      operationList.add(operation);
    }
  }

  @Override
  public List<OTOperation> getRemoteLogFromId(String remotePeerId, int revision, boolean includeNonCanon) {
    synchronized (lock) {
      final List<OTOperation> operationList = remoteTransactionLogs.get(remotePeerId);
      if (operationList == null) {
        return Collections.emptyList();
      }

      return _getLogFromId(operationList, revision, includeNonCanon);
    }
  }

  @Override
  public State getEffectiveStateForRemoteRevision(String remotePeerId, int revision) {
    synchronized (lock) {
      final List<OTOperation> operationList = remoteTransactionLogs.get(remotePeerId);
      if (operationList == null) {
        throw new OTException("no known remote state for peer: " + remotePeerId);
      }
      return _getEffectiveStateFor(revision, stateSnapshots, operationList);
    }
  }

  @Override
  public void markDirty() {
    synchronized (lock) {
      this.logDirty = true;
    }
  }

  @Override
  public void cleanLog() {
    synchronized (lock) {
      final Iterator<OTOperation> iterator = transactionLog.iterator();
      while (iterator.hasNext()) {
        if (!iterator.next().isCanon()) {
          iterator.remove();
        }
      }
      logDirty = false;
    }
  }

  @Override
  public String toString() {
    return Arrays.toString(getCanonLog().toArray());
  }

  @Override
  public void logDebugDump() {
    synchronized (lock) {
      _logDebugDump(stateSnapshots, transactionLog);
    }
  }

  private static List<OTOperation> filterAndRemoveCanonExceptFirst(List<OTOperation> log) {
    final List<OTOperation> ops = new ArrayList<OTOperation>();
    boolean first = true;
    for (final OTOperation otOperation : log) {
      if (otOperation.isCanon()) {
        ops.add(otOperation);

        if (!first)
          otOperation.removeFromCanonHistory();
      }
      first = false;
    }
    return ops;
  }

  private static void _logDebugDump(final List<StateSnapshot> stateSnapshots, final List<OTOperation> transactionLog) {
    LogUtil.log("******* LOG DUMP ********");
    LogUtil.log("STATE SNAPSHOTS:");

    for (StateSnapshot stateSnapshot : stateSnapshots) {
      LogUtil.log("#" + stateSnapshot.getRevision() + "  \"" + stateSnapshot.getState().get() + "\"");
    }

    LogUtil.log("\n\nLOG:");

    for (OTOperation op : transactionLog) {
      if (op.isCanon()) {
        LogUtil.log("#" + op.getRevision() + " hash:" + op.getRevisionHash() + ": " + op);
      }
    }
  }

  private static State _getEffectiveStateFor(int revision, List<StateSnapshot> stateSnapshots, List<OTOperation> log) {
    final StateSnapshot latestSnapshotState = getLatestParentSnapshot(stateSnapshots, revision);
    final State stateToTranslate = latestSnapshotState.getState().snapshot();
    final ListIterator<OTOperation> operationListIterator
        = log.listIterator(log.size());

    while (operationListIterator.hasPrevious()) {
      if (operationListIterator.previous().getRevision() == latestSnapshotState.getRevision()) {
        break;
      }
    }

    try {
      while (operationListIterator.hasNext()) {
        final OTOperation op = operationListIterator.next();
        if (!op.isCanon()) continue;

        if (op.getRevision() < revision) {
          for (final Mutation mutation : op.getMutations()) {
            mutation.apply(stateToTranslate);
          }
        }
      }


      return stateToTranslate;
    }
    catch (OTException e) {
      _logDebugDump(stateSnapshots, log);

      throw new OTException("failed to rewind to a consistent state for revision: " + revision, e);
    }
  }

  private static List<OTOperation> _getLogFromId(final List<OTOperation> transactionLog,
                                                 final int revision,
                                                 final boolean includeNonCanon) {
    if (transactionLog.isEmpty()) {
      return Collections.emptyList();
    }

    final ListIterator<OTOperation> operationListIterator = transactionLog.listIterator(transactionLog.size());
    final List<OTOperation> operationList = new ArrayList<OTOperation>();

    while (operationListIterator.hasPrevious()) {
      final OTOperation previous = operationListIterator.previous();
      if (!includeNonCanon && !previous.isCanon()) {
        continue;
      }
      operationList.add(previous);
      if (previous.getRevision() == revision) {
        if (!previous.isCanon()) {
          final Iterator<OTOperation> iterator = operationList.iterator();
          while (iterator.hasNext()) {
            final OTOperation next = iterator.next();
            if (next.isCanon()) {
              iterator.remove();
            }
          }
        }

        Collections.reverse(operationList);
        return operationList;
      }
    }

    if ((revision - 1) == transactionLog.get(transactionLog.size() - 1).getRevision()) {
      return Collections.emptyList();
    }
    else {
//        LogUtil.log("Could not find revision: " + revision);
//        LogUtil.log("Current Log:\n");
//        for (OTOperation operation : transactionLog) {
//          LogUtil.log("Rev:" + operation.getRevision() + ":" + operation);
//        }


      throw new OTException("unable to find revision in log: " + revision);
    }
  }

  private static class StateSnapshot {
    private final int revision;
    private final State state;

    private StateSnapshot(final int revision, final State state) {
      this.revision = revision;
      this.state = state;
    }

    private int getRevision() {
      return revision;
    }

    private State getState() {
      return state;
    }
  }
}
