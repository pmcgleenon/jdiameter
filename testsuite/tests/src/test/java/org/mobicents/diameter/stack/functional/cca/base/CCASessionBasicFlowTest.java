/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and/or its affiliates, and individual
 * contributors as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * 
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free 
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.diameter.stack.functional.cca.base;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jdiameter.api.Mode;
import org.jdiameter.api.Peer;
import org.jdiameter.api.PeerState;
import org.jdiameter.api.PeerTable;
import org.jdiameter.api.Stack;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * 
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
@RunWith(Parameterized.class)
public class CCASessionBasicFlowTest {

  private Client clientNode;
  private Server serverNode1;
  private URI clientConfigURI;
  private URI serverNode1ConfigURI;

  /**
   * @param clientNode
   * @param node1
   * @param node2
   * @param serverCount
   */
  public CCASessionBasicFlowTest(String clientConfigUrl, String serverNode1ConfigURL) throws Exception {
    super();
    this.clientConfigURI = new URI(clientConfigUrl);
    this.serverNode1ConfigURI = new URI(serverNode1ConfigURL);
  }

  @Before
  public void setUp() throws Exception {
    try {
      this.clientNode = new Client();
      this.serverNode1 = new Server();

      this.serverNode1.init(new FileInputStream(new File(this.serverNode1ConfigURI)), "SERVER1");
      this.serverNode1.start();

      this.clientNode.init(new FileInputStream(new File(this.clientConfigURI)), "CLIENT");
      this.clientNode.start(Mode.ANY_PEER, 10, TimeUnit.SECONDS);
      Stack stack = this.clientNode.getStack();

      List<Peer> peers = stack.unwrap(PeerTable.class).getPeerTable();
      if (peers.size() == 1) {
        // ok
      }
      else if (peers.size() > 1) {
        // works better with replicated, since disconnected peers are also listed
        boolean foundConnected = false;
        for(Peer p : peers) {
          if(p.getState(PeerState.class).equals(PeerState.OKAY)) {
            if(foundConnected) {
              throw new Exception("Wrong number of connected peers: " + peers);
            }
            foundConnected = true;
          }
        }
      }
      else {
        throw new Exception("Wrong number of connected peers: " + peers);
      }
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }

  @After
  public void tearDown() {
    if (this.serverNode1 != null) {
      try {
        this.serverNode1.stop();
      }
      catch (Exception e) {

      }
      this.serverNode1 = null;
    }

    if (this.clientNode != null) {
      try {
        this.clientNode.stop();
      }
      catch (Exception e) {

      }
      this.clientNode = null;
    }
  }

  @Test
  public void testBasicFlow() throws Exception {
    try {
      // pain of parameter tests :) ?
      clientNode.sendInitial();
      waitForMessage();

      serverNode1.sendInitial();
      waitForMessage();

      clientNode.sendInterim();
      waitForMessage();

      serverNode1.sendInterim();
      waitForMessage();

      clientNode.sendTermination();
      waitForMessage();

      serverNode1.sendTermination();
      waitForMessage();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail(e.toString());
    }

    if (!clientNode.isReceiveINITIAL()) {
      StringBuilder sb = new StringBuilder("Did not receive INITIAL! ");
      sb.append("Client ER:\n").append(clientNode.createErrorReport(this.clientNode.getErrors()));

      fail(sb.toString());
    }
    if (!clientNode.isReceiveINTERIM()) {
      StringBuilder sb = new StringBuilder("Did not receive INTERIM! ");
      sb.append("Client ER:\n").append(clientNode.createErrorReport(this.clientNode.getErrors()));

      fail(sb.toString());
    }
    if (!clientNode.isReceiveTERMINATE()) {
      StringBuilder sb = new StringBuilder("Did not receive TERMINATE! ");
      sb.append("Client ER:\n").append(clientNode.createErrorReport(this.clientNode.getErrors()));

      fail(sb.toString());
    }
    if (!clientNode.isPassed()) {
      StringBuilder sb = new StringBuilder();
      sb.append("Client ER:\n").append(clientNode.createErrorReport(this.clientNode.getErrors()));

      fail(sb.toString());
    }

    if (!serverNode1.isReceiveINITIAL()) {
      StringBuilder sb = new StringBuilder("Did not receive INITIAL! ");
      sb.append("Server ER:\n").append(serverNode1.createErrorReport(this.serverNode1.getErrors()));

      fail(sb.toString());
    }
    else if (!serverNode1.isReceiveINTERIM()) {
      StringBuilder sb = new StringBuilder("Did not receive INTERIM! ");
      sb.append("Server ER:\n").append(serverNode1.createErrorReport(this.serverNode1.getErrors()));

      fail(sb.toString());
    }
    else if (!serverNode1.isReceiveTERMINATE()) {
      StringBuilder sb = new StringBuilder("Did not receive TERMINATE! ");
      sb.append("Server ER:\n").append(serverNode1.createErrorReport(this.serverNode1.getErrors()));

      fail(sb.toString());
    }
    if (!serverNode1.isPassed()) {
      StringBuilder sb = new StringBuilder();
      sb.append("Server ER:\n").append(serverNode1.createErrorReport(this.serverNode1.getErrors()));

      fail(sb.toString());
    }
  }

  @Parameters
  public static Collection<Object[]> data() {
    String client = "config-client.xml";
    String server1 = "config-server-node1.xml";

    String replicatedClient = "replicated-config-client.xml";
    String replicatedServer1 = "replicated-config-server-node1.xml";

    Class<CCASessionBasicFlowTest> t = CCASessionBasicFlowTest.class;
    client = t.getResource(client).toString();
    server1 = t.getResource(server1).toString();
    replicatedClient = t.getResource(replicatedClient).toString();
    replicatedServer1 = t.getResource(replicatedServer1).toString();

    return Arrays.asList(new Object[][] { { client, server1 }, { replicatedClient, replicatedServer1 } });
  }

  private void waitForMessage() {
    try {
      Thread.sleep(2000);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}
