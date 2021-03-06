/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.test;

import java.io.File;

import junit.framework.TestCase;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test what happens when logs fall behind the snapshots or a follower has a
 * higher epoch than a leader.
 */
public class QuorumZxidSyncTest extends TestCase {
    QuorumBase qb = new QuorumBase();
    
    @Before
    @Override
    protected void setUp() throws Exception {
    	qb.setUp();
    }
    
    @Test
    /**
     * find out what happens when a follower connects to leader that is behind
     */
    public void testBehindLeader() throws Exception {
        // crank up the epoch numbers
        ClientBase.waitForServerUp(qb.hostPort, 10000);
        ClientBase.waitForServerUp(qb.hostPort, 10000);
        ZooKeeper zk = new ZooKeeper(qb.hostPort, 10000, new Watcher() {
            public void process(WatchedEvent event) {
            }});
        zk.create("/0", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        zk.close();
        qb.shutdownServers();
        qb.startServers();
        ClientBase.waitForServerUp(qb.hostPort, 10000);
        qb.shutdownServers();
        qb.startServers();
        ClientBase.waitForServerUp(qb.hostPort, 10000);
        zk = new ZooKeeper(qb.hostPort, 10000, new Watcher() {
            public void process(WatchedEvent event) {
            }});
        zk.create("/1", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        zk.close();
        qb.shutdownServers();
        qb.startServers();
        ClientBase.waitForServerUp(qb.hostPort, 10000);
        qb.shutdownServers();
        qb.startServers();
        ClientBase.waitForServerUp(qb.hostPort, 10000);
        zk = new ZooKeeper(qb.hostPort, 10000, new Watcher() {
            public void process(WatchedEvent event) {
            }});
        zk.create("/2", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        zk.close();
        qb.shutdownServers();
        deleteFiles(qb.s1dir);
        deleteFiles(qb.s2dir);
        deleteFiles(qb.s3dir);
        deleteFiles(qb.s4dir);
        qb.setupServers();
        qb.s1.start();
        qb.s2.start();
        qb.s3.start();
        qb.s4.start();
        assertTrue("Servers didn't come up", ClientBase.waitForServerUp(qb.hostPort, 10000));
        qb.s5.start();
        String hostPort = "127.0.0.1:" + qb.s5.getClientPort();
        assertFalse("Servers came up, but shouldn't have since it's ahead of leader",
                ClientBase.waitForServerUp(hostPort, 10000));
    }
    
    private void deleteFiles(File f) {
        File v = new File(f, "version-2");
        for(File c: v.listFiles()) {
            c.delete();
        }
    }
    
    @Test
    /**
     * find out what happens when the latest state is in the snapshots not
     * the logs.
     */
    public void testLateLogs() throws Exception {
        // crank up the epoch numbers
        ClientBase.waitForServerUp(qb.hostPort, 10000);
        ClientBase.waitForServerUp(qb.hostPort, 10000);
        ZooKeeper zk = new ZooKeeper(qb.hostPort, 10000, new Watcher() {
            public void process(WatchedEvent event) {
            }});
        zk.create("/0", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        zk.close();
        qb.shutdownServers();
        qb.startServers();
        ClientBase.waitForServerUp(qb.hostPort, 10000);
        qb.shutdownServers();
        qb.startServers();
        ClientBase.waitForServerUp(qb.hostPort, 10000);
        zk = new ZooKeeper(qb.hostPort, 10000, new Watcher() {
            public void process(WatchedEvent event) {
            }});
        zk.create("/1", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        zk.close();
        qb.shutdownServers();
        qb.startServers();
        ClientBase.waitForServerUp(qb.hostPort, 10000);
        qb.shutdownServers();
        deleteLogs(qb.s1dir);
        deleteLogs(qb.s2dir);
        deleteLogs(qb.s3dir);
        deleteLogs(qb.s4dir);
        deleteLogs(qb.s5dir);
        qb.startServers();
        ClientBase.waitForServerUp(qb.hostPort, 10000);
        zk = new ZooKeeper(qb.hostPort, 10000, new Watcher() {
            public void process(WatchedEvent event) {
            }});
        zk.create("/2", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        zk.close();
        qb.shutdownServers();
        qb.startServers();
        ClientBase.waitForServerUp(qb.hostPort, 10000);
        zk = new ZooKeeper(qb.hostPort, 10000, new Watcher() {
            public void process(WatchedEvent event) {
            }});
        boolean saw2 = false;
        for(String child: zk.getChildren("/", false)) {
            if (child.equals("2")) {
                saw2 = true;
            }
        }
        zk.close();
        assertTrue("Didn't see /2 (went back in time)", saw2);
    }
    
    private void deleteLogs(File f) {
        File v = new File(f, "version-2");
        for(File c: v.listFiles()) {
            if (c.getName().startsWith("log")) {
                c.delete();
            }
        }
    }
    
    @After
    @Override
    public void tearDown() throws Exception {
        qb.tearDown();
    }
}
