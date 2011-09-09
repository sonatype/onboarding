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
package com.sonatype.s2.p2lineup.model;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.codehaus.plexus.util.StringUtils;

import com.sonatype.nexus.p2.rest.model.P2LineupError;
import com.sonatype.nexus.p2.rest.model.P2LineupErrorResponse;
import com.sonatype.nexus.p2.rest.model.P2LineupListResponse;
import com.sonatype.nexus.p2.rest.model.P2LineupRepositoryError;
import com.sonatype.nexus.p2.rest.model.P2LineupSummaryDto;
import com.sonatype.nexus.p2.rest.model.P2LineupUnresolvedInstallableUnit;
import com.sonatype.s2.p2lineup.model.io.xstream.XStreamUtil;
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

    public void testErrorResponse()
    {
        P2LineupErrorResponse errorResponse = new P2LineupErrorResponse();

        P2LineupError p2LineupError = new P2LineupError();
        p2LineupError.setErrorMessage( "errorMessage" );
        errorResponse.addError( p2LineupError );

        P2LineupRepositoryError p2LineupRepositoryError =
            new P2LineupRepositoryError( "repositoryURL", "errorMessage1" );
        errorResponse.addError( p2LineupRepositoryError );

        P2LineupUnresolvedInstallableUnit p2LineupUnresolvedInstallableUnit = new P2LineupUnresolvedInstallableUnit();
        p2LineupUnresolvedInstallableUnit.setErrorMessage( "errorMessage2" );
        p2LineupUnresolvedInstallableUnit.setInstallableUnitId( "installableUnitId" );
        p2LineupUnresolvedInstallableUnit.setInstallableUnitVersion( "installableUnitVersion" );
        errorResponse.addError( p2LineupUnresolvedInstallableUnit );

        validateXmlHasNoPackageNames( errorResponse );
        validateToAndFromXML( errorResponse );
    }

    public void testLineupListRespose()
    {
        P2LineupListResponse listRespose = new P2LineupListResponse();
        P2LineupSummaryDto dto1 = new P2LineupSummaryDto();
        listRespose.addData( dto1 );
        dto1.setDescription( "dto1Description" );
        dto1.setGroupId( "dto1groupId" );
        dto1.setId( "dto1Id" );
        dto1.setName( "dto1name" );
        dto1.setVersion( "dto1version" );
        dto1.setResourceUri( "dto1ResourceUri" );
        
        P2LineupSummaryDto dto2 = new P2LineupSummaryDto();
        listRespose.addData( dto2 );
        dto2.setDescription( "dto2Description" );
        dto2.setGroupId( "dto2groupId" );
        dto2.setId( "dto2Id" );
        dto2.setName( "dto2name" );
        dto2.setVersion( "dto2version" );
        dto2.setResourceUri( "dto2ResourceUri" );
        
        validateXmlHasNoPackageNames( listRespose );
        validateToAndFromXML( listRespose );
    }

    public void testLineup() throws Exception
    {
        P2Lineup lineup = new P2Lineup();
        lineup.setDescription( "description" );
        lineup.setGroupId( "groupId" );
        lineup.setId( "id" );
        lineup.setModelEncoding( "modelEncoding" );
        lineup.setName( "name" );
        
        P2LineupP2Advice advice1 = new P2LineupP2Advice();
        lineup.setP2Advice( advice1 );
        advice1.addAdvice( "advice1" );
        advice1.addAdvice( "advice2" );
        advice1.setTouchpointId( "touchpointId1" );
        advice1.setTouchpointVersion( "touchpointVersion1" );
        
        P2LineupP2Advice advice2 = new P2LineupP2Advice();
        lineup.setP2Advice( advice2 );
        advice2.addAdvice( "advice3" );
        advice2.addAdvice( "advice3" );
        advice2.setTouchpointId( "touchpointId2" );
        advice2.setTouchpointVersion( "touchpointVersion2" );
        
        P2LineupSourceRepository sourceRepo1 = new P2LineupSourceRepository();
        lineup.addRepository( sourceRepo1 );
        sourceRepo1.setLayout( "layout1" );
        sourceRepo1.setUrl( "url1" );
        
        P2LineupSourceRepository sourceRepo2 = new P2LineupSourceRepository();
        lineup.addRepository( sourceRepo2 );
        sourceRepo2.setLayout( "layout2" );
        sourceRepo2.setUrl( "url2" );
        
        P2LineupInstallableUnit iu1 = new P2LineupInstallableUnit();
        lineup.addRootInstallableUnit( iu1 );
        iu1.setId( "id1" );
        iu1.setVersion( "version1" );
        
        P2LineupInstallableUnit iu2 = new P2LineupInstallableUnit();
        lineup.addRootInstallableUnit( iu2 );
        iu2.setId( "id2" );
        iu2.setVersion( "version2" );
        
        P2LineupTargetEnvironment targetEnvironment1 = new P2LineupTargetEnvironment();
        lineup.addTargetEnvironment( targetEnvironment1 );
        targetEnvironment1.setOsgiArch( "osgiArch1" );
        targetEnvironment1.setOsgiOS( "osgiOS1" );
        targetEnvironment1.setOsgiWS( "osgiWS1" );
        
        P2LineupTargetEnvironment targetEnvironment2 = new P2LineupTargetEnvironment();
        lineup.addTargetEnvironment( targetEnvironment2 );
        targetEnvironment2.setOsgiArch( "osgiArch2" );
        targetEnvironment2.setOsgiOS( "osgiOS2" );
        targetEnvironment2.setOsgiWS( "osgiWS2" );
        
        lineup.setVersion( "version" );
        
        validateXmlHasNoPackageNames( lineup );
        validateToAndFromXML( lineup );
        
        // NOTE: it is very easy to make xstream and modello use the same xml model, the problem is in nexus
        // the LookAheadXppReader doesn't work correctly with attributes
        // validateToModelloFromXStream( lineup );
        // validateToXStreamFromModello( lineup );

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
    
//    private void validateToModelloFromXStream( P2Lineup lineup ) throws IOException
//    {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        OutputStreamWriter ioWriter = new OutputStreamWriter( baos );
//        P2LineupXpp3Writer writer =new P2LineupXpp3Writer();
//        writer.write(ioWriter, lineup );
//        
//        // modello xml
//        String modelloXML = baos.toString();
//        
    // P2Lineup resultLineup = (P2Lineup) xstream.fromXML( modelloXML );
//        Assert.assertEquals( "Objects differs from original", lineup, resultLineup );
//        
//    }
//    
//    private void validateToXStreamFromModello( P2Lineup lineup ) throws IOException, XmlPullParserException
//    {   
    // String xstreamXml = xstream.toXML( lineup );
//        P2LineupXpp3Reader reader = new P2LineupXpp3Reader();
//        P2Lineup resultLineup = reader.read( new StringReader(xstreamXml) );
//        Assert.assertEquals( "Objects differs from original", lineup, resultLineup );
//        
//    }
}
