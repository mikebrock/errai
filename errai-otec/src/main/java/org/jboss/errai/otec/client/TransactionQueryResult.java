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
 * @author Mike Brock
 */
public class TransactionQueryResult {
  private final String commonStateHash;

  private final State effectiveCommonState;

  private final OTOperation commonLocal;
  private final OTOperation commonRemote;

  private final List<OTOperation> needsTransformLocal;
  private final List<OTOperation> needsTransformRemote;

  public TransactionQueryResult(final String commonStateHash,
                                final State effectiveCommonState,
                                final OTOperation commonLocal,
                                final OTOperation commonRemote,
                                final List<OTOperation> needsTransformLocal,
                                final List<OTOperation> needsTransformRemote) {
    this.commonStateHash = commonStateHash;
    this.effectiveCommonState = effectiveCommonState;
    this.commonLocal = commonLocal;
    this.commonRemote = commonRemote;
    this.needsTransformLocal = needsTransformLocal;
    this.needsTransformRemote = needsTransformRemote;
  }

  public String getCommonStateHash() {
    return commonStateHash;
  }

  public State getEffectiveCommonState() {
    return effectiveCommonState;
  }

  public OTOperation getCommonLocal() {
    return commonLocal;
  }

  public OTOperation getCommonRemote() {
    return commonRemote;
  }

  public List<OTOperation> getNeedsTransformLocal() {
    return needsTransformLocal;
  }

  public List<OTOperation> getNeedsTransformRemote() {
    return needsTransformRemote;
  }
}
