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

package org.jboss.errai.otec;

import org.jboss.errai.otec.client.EntitySyncCompletionCallback;
import org.jboss.errai.otec.client.OTEngine;
import org.jboss.errai.otec.client.OTEntity;
import org.jboss.errai.otec.client.OTPeer;
import org.jboss.errai.otec.client.State;
import org.jboss.errai.otec.client.util.OTLogUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Mike Brock
 */
public abstract class AbstractMockPeer implements OTPeer {
  protected OTEngine localEngine;
  protected OTEngine remoteEngine;
  protected final Map<Integer, String> lastTransmittedSequencees = new HashMap<Integer, String>();


  @Override
  public String getId() {
    return remoteEngine.getId();
  }

  @Override
  public int getLastKnownRemoteSequence(final Integer entity) {
    return 0;
  }

  @Override
  public String getLastTransmittedHash(final Integer entity) {
    return lastTransmittedSequencees.get(entity);
  }


  @Override @SuppressWarnings("unchecked")
  public void beginSyncRemoteEntity(final String peerId,
                                    final int entityId,
                                    final EntitySyncCompletionCallback<State> callback) {

    final OTEntity entity = remoteEngine.getEntityStateSpace().getEntity(entityId);
    localEngine.getEntityStateSpace().addEntity(new OTTestEntity(entity));

    OTLogUtil.log("SYNC", "",
        remoteEngine.getName(),
        localEngine.getName(),
        entity.getRevision(),
        "\"" + entity.getState().get() + "\"");

    localEngine.associateEntity(remoteEngine.getId(), entityId);
    remoteEngine.associateEntity(localEngine.getId(), entityId);

    callback.syncComplete(entity);
  }

  @Override
  public String toString() {
    return remoteEngine.getName();
  }
}
