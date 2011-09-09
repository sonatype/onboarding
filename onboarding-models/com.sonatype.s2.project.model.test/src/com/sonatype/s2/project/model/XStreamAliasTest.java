/**
 * Copyright (c) 2008-2010 Sonatype, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sonatype, Inc. - initial API and implementation
 */
package com.sonatype.s2.project.model;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.codehaus.plexus.util.StringUtils;

import com.sonatype.nexus.onboarding.rest.dto.xstream.XStreamUtil;
import com.sonatype.s2.securityrealm.model.S2AnonymousAccessType;
import com.sonatype.s2.securityrealm.model.S2SecurityRealm;
import com.sonatype.s2.securityrealm.model.S2SecurityRealmAuthenticationType;
import com.sonatype.s2.securityrealm.model.S2SecurityRealmURLAssoc;
import com.thoughtworks.xstream.XStream;

public class XStreamAliasTest
    extends TestCase

{
    private XStream xstream;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        xstream = XStreamUtil.initializeXStream( new XStream() );
        xstream.setClassLoader( XStreamUtil.class.getClassLoader() );
    }

    public void testSecurityRealm()
        throws Exception
    {
        S2SecurityRealm realm = new S2SecurityRealm();
        realm.setId( "realm-id" );
        realm.setName( "realm-name" );
        realm.setDescription( "realm-desc" );
        realm.setAuthenticationType( S2SecurityRealmAuthenticationType.USERNAME_PASSWORD );
        
        validateXmlHasNoPackageNames( realm );
        validateToAndFromXML( realm );
    }

    public void testSecurityRealmURLAssoc()
        throws Exception
    {
        S2SecurityRealmURLAssoc urlAssoc = new S2SecurityRealmURLAssoc();
        urlAssoc.setId( "url-id" );
        urlAssoc.setRealmId( "realm-id" );
        urlAssoc.setUrl( "http://foo" );
        urlAssoc.setAnonymousAccess( S2AnonymousAccessType.ALLOWED );

        validateXmlHasNoPackageNames( urlAssoc );
        validateToAndFromXML( urlAssoc );
    }

    private void validateXmlHasNoPackageNames( Object obj )
    {
        String xml = xstream.toXML( obj );

        // quick way of looking for the class="org attribute
        // i don't want to parse a dom to figure this out

        int totalCount = StringUtils.countMatches( xml, "org.sonatype" );
        totalCount += StringUtils.countMatches( xml, "com.sonatype" );

        // check the counts
        Assert.assertFalse( "Found package name in XML:\n" + xml, totalCount > 0 );

         // print out each type of method, so i can rafb it
//         System.out.println( "\n\nClass: "+ obj.getClass() +"\n" );
//         System.out.println( xml+"\n" );
        //        
        // Assert.assertFalse( "Found <string> XML: " + obj.getClass() + "\n" + xml, xml.contains( "<string>" ) );
    }
    
    private void validateToAndFromXML( Object obj )
    {
        String xml = xstream.toXML( obj );
        Object result = xstream.fromXML( xml );
        String resultXml = xstream.toXML( result );
        
        Assert.assertEquals( "Objects differ original Xml:\n"+ xml + "\n\nresult Xml:\n"+ resultXml +"\n\n", obj, result );
    }
}
