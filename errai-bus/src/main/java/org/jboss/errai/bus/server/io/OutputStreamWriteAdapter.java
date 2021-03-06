/*
 * Copyright 2012 JBoss, by Red Hat, Inc
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

package org.jboss.errai.bus.server.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A simple wrapper implementation of {@link ByteWriteAdapter} which forwards all writes to the
 * {@link OutputStream} specified when creating the adapter.
 *
 * @author Mike Brock
 */
public class OutputStreamWriteAdapter extends AbstractByteWriteAdapter {
  private final OutputStream outputStream;

  public OutputStreamWriteAdapter(final OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  @Override
  public void write(final byte b) throws IOException {
    outputStream.write(b);
  }

  @Override
  public void flush() throws IOException {
    outputStream.flush();
  }
}
