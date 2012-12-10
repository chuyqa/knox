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
package org.apache.hadoop.gateway.descriptor.xml;

import org.apache.hadoop.gateway.descriptor.GatewayDescriptor;
import org.apache.hadoop.gateway.descriptor.GatewayDescriptorFactory;
import org.apache.hadoop.gateway.descriptor.FilterDescriptor;
import org.apache.hadoop.gateway.descriptor.FilterParamDescriptor;
import org.apache.hadoop.gateway.descriptor.ResourceDescriptor;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.internal.matchers.StringContains.containsString;

public class XmlGatewayDescriptorImporterTest {

  @Test
  public void testFormat() {
    XmlGatewayDescriptorImporter importer = new XmlGatewayDescriptorImporter();
    assertThat( importer.getFormat(), is( "xml" ) );
  }

  @Test
  public void testXmlClusterDescriptorLoad() throws IOException {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
        "<gateway>\n" +
        "  <resource>\n" +
        "    <source>resource1-source</source>\n" +
        "    <target>resource1-target</target>\n" +
        "    <filter>\n" +
        "      <role>resource1-filter1-role</role>\n" +
        "      <class>resource1-filter1-impl</class>\n" +
        "      <param>\n" +
        "        <name>resource1-filter1-param1-name</name>\n" +
        "        <value>resource1-filter1-param1-value</value>\n" +
        "      </param>\n" +
        "      <param>\n" +
        "        <name>resource1-filter1-param2-name</name>\n" +
        "        <value>resource1-filter1-param2-value</value>\n" +
        "      </param>\n" +
        "    </filter>\n" +
        "    <filter>\n" +
        "      <role>resource1-filter2-role</role>\n" +
        "      <class>resource1-filter2-impl</class>\n" +
        "    </filter>\n" +
        "  </resource>\n" +
        "  <resource>\n" +
        "    <source>resource2-source</source>\n" +
        "    <target>resource2-target</target>\n" +
        "  </resource>\n" +
        "</gateway>";

    Reader reader = new StringReader( xml );

    GatewayDescriptor descriptor = GatewayDescriptorFactory.load( "xml", reader );

    assertThat( descriptor, notNullValue() );
    assertThat( descriptor.resources().size(), is( 2 ) );

    ResourceDescriptor resource1 = descriptor.resources().get( 0 );
    assertThat( resource1, notNullValue() );
    assertThat( resource1.source(), is( "resource1-source" ) );
    assertThat( resource1.target(), is( "resource1-target" ) );

    assertThat( resource1.filters().size(), is( 2 ) );

    FilterDescriptor filter1 = resource1.filters().get( 0 );
    assertThat( filter1, notNullValue() );
    assertThat( filter1.role(), is( "resource1-filter1-role" ) );
    assertThat( filter1.impl(), is( "resource1-filter1-impl" ) );

    assertThat( filter1.params().size(), is( 2 ) );

    FilterParamDescriptor param1 = filter1.params().get( 0 );
    assertThat( param1, notNullValue() );
    assertThat( param1.name(), is( "resource1-filter1-param1-name" ) );
    assertThat( param1.value(), is( "resource1-filter1-param1-value" ) );

    FilterParamDescriptor param2 = filter1.params().get( 1 );
    assertThat( param2, notNullValue() );
    assertThat( param2.name(), is( "resource1-filter1-param2-name" ) );
    assertThat( param2.value(), is( "resource1-filter1-param2-value" ) );

    FilterDescriptor filter2 = resource1.filters().get( 1 );
    assertThat( filter2, notNullValue() );
    assertThat( filter2.role(), is( "resource1-filter2-role" ) );
    assertThat( filter2.impl(), is( "resource1-filter2-impl" ) );

    ResourceDescriptor resource2 = descriptor.resources().get( 1 );
    assertThat( resource2, notNullValue() );
    assertThat( resource2.source(), is( "resource2-source" ) );
    assertThat( resource2.target(), is( "resource2-target" ) );
  }

  @Test
  public void testXmlClusterDescriptorLoadEmpty() throws IOException {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
        "<gateway>\n" +
        "  <resource>\n" +
        "    <filter>\n" +
        "      <param>\n" +
        "      </param>\n" +
        "    </filter>\n" +
        "  </resource>\n" +
        "</gateway>";

    Reader reader = new StringReader( xml );

    GatewayDescriptor descriptor = GatewayDescriptorFactory.load( "xml", reader );

    assertThat( descriptor, notNullValue() );
    assertThat( descriptor.resources().size(), is( 1 ) );
    ResourceDescriptor resource1 = descriptor.resources().get( 0 );
    assertThat( resource1, notNullValue() );
    assertThat( resource1.filters().size(), is( 1 ) );
    FilterDescriptor filter1 = resource1.filters().get( 0 );
    assertThat( filter1, notNullValue() );
    assertThat( filter1.params().size(), is( 1 ) );
    FilterParamDescriptor param1 = filter1.params().get( 0 );
    assertThat( param1, notNullValue() );
  }

  @Test
  public void testXmlClusterDescriptorLoadInvalid() throws IOException {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
        "<gateway>\n" +
        "  <resource>\n" +
        "    <filter>\n" +
        "      <param>";

    Reader reader = new StringReader( xml );
    try {
      GatewayDescriptorFactory.load( "xml", reader );
      fail( "Should have thrown IOException" );
    } catch( IOException e ) {
      assertThat( e.getMessage(), containsString( "org.xml.sax.SAXParseException" ) );
    }
  }

}