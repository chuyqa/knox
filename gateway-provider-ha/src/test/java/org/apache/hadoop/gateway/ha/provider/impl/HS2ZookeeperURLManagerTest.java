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
package org.apache.hadoop.gateway.ha.provider.impl;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingCluster;
import org.apache.hadoop.gateway.ha.provider.HaServiceConfig;
import org.apache.hadoop.gateway.ha.provider.URLManager;
import org.apache.hadoop.gateway.ha.provider.URLManagerLoader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HS2ZookeeperURLManagerTest {

  private TestingCluster cluster;
  private HS2ZookeeperURLManager manager;

  @Before
  public void setup() throws Exception {
    cluster = new TestingCluster(3);
    cluster.start();

    CuratorFramework zooKeeperClient =
        CuratorFrameworkFactory.builder().connectString(cluster.getConnectString())
            .retryPolicy(new ExponentialBackoffRetry(1000, 3)).build();

    String host1 = "hive.server2.authentication=NONE;hive.server2.transport.mode=http;hive.server2.thrift.http.path=cliservice;" +
        "hive.server2.thrift.http.port=10001;hive.server2.thrift.bind.host=host1;hive.server2.use.SSL=true";
    String host2 = "hive.server2.authentication=NONE;hive.server2.transport.mode=http;hive.server2.thrift.http.path=foobar;" +
        "hive.server2.thrift.http.port=10002;hive.server2.thrift.bind.host=host2;hive.server2.use.SSL=false";
    String host3 = "hive.server2.authentication=NONE;hive.server2.transport.mode=http;hive.server2.thrift.http.path=cliservice;" +
        "hive.server2.thrift.http.port=10003;hive.server2.thrift.bind.host=host3;hive.server2.use.SSL=false";
    String host4 = "hive.server2.authentication=NONE;hive.server2.transport.mode=http;hive.server2.thrift.http.path=cliservice;" +
        "hive.server2.thrift.http.port=10004;hive.server2.thrift.bind.host=host4;hive.server2.use.SSL=true";
    zooKeeperClient.start();
    zooKeeperClient.create().forPath("/hiveServer2");
    zooKeeperClient.create().forPath("/hiveServer2/host1", host1.getBytes());
    zooKeeperClient.create().forPath("/hiveServer2/host2", host2.getBytes());
    zooKeeperClient.create().forPath("/hiveServer2/host3", host3.getBytes());
    zooKeeperClient.create().forPath("/hiveServer2/host4", host4.getBytes());
    zooKeeperClient.close();
    manager = new HS2ZookeeperURLManager();
    HaServiceConfig config = new DefaultHaServiceConfig("HIVE");
    config.setEnabled(true);
    config.setZookeeperEnsemble(cluster.getConnectString());
    config.setZookeeperNamespace("hiveServer2");
    manager.setConfig(config);

  }

  @After
  public void teardown() throws IOException {
    cluster.stop();
  }

  @Test
  public void testActiveURLManagement() throws Exception {
    List<String> urls = manager.getURLs();
    Assert.assertNotNull(urls);
    String url1 = "https://host4:10004/cliservice";
    String url2 = "http://host3:10003/cliservice";
    String url3 = "http://host2:10002/foobar";
    assertEquals(url1, urls.get(0));
    assertEquals(url1, manager.getActiveURL());
    manager.markFailed(url1);
    assertEquals(url2, manager.getActiveURL());
    manager.markFailed(url2);
    assertEquals(url3, manager.getActiveURL());
  }

  @Test
  public void testMarkingFailedURL() {
    ArrayList<String> urls = new ArrayList<>();
    String url1 = "https://host4:10004/cliservice";
    urls.add(url1);
    String url2 = "http://host3:10003/cliservice";
    urls.add(url2);
    String url3 = "http://host2:10002/foobar";
    urls.add(url3);
    String url4 = "https://host1:10001/cliservice";
    urls.add(url4);
    assertTrue(manager.getURLs().containsAll(urls));
    assertEquals(url1, manager.getActiveURL());
    manager.markFailed(url1);
    assertEquals(url2, manager.getActiveURL());
    manager.markFailed(url1);
    assertEquals(url2, manager.getActiveURL());
    manager.markFailed(url3);
    assertEquals(url2, manager.getActiveURL());
    manager.markFailed(url4);
    assertEquals(url2, manager.getActiveURL());
    manager.markFailed(url2);
    //now the urls should get re-looked up
    assertEquals(url1, manager.getActiveURL());
  }

  @Test
  public void testHS2URLManagerLoading() {
    HaServiceConfig config = new DefaultHaServiceConfig("HIVE");
    config.setEnabled(true);
    config.setZookeeperEnsemble(cluster.getConnectString());
    config.setZookeeperNamespace("hiveServer2");
    URLManager manager = URLManagerLoader.loadURLManager(config);
    Assert.assertNotNull(manager);
    Assert.assertTrue(manager instanceof HS2ZookeeperURLManager);
  }


}
