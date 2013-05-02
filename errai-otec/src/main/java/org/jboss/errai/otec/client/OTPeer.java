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

/**
 * @author Mike Brock
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public interface OTPeer {
  public String getId();
  public void send(OTOperation operation);
  public void beginSyncRemoteEntity(String peerId, int entityId, EntitySyncCompletionCallback<State> callback);
  public int getLastKnownRemoteSequence(Integer entity);
  public String getLastTransmittedHash(Integer entity);
}
