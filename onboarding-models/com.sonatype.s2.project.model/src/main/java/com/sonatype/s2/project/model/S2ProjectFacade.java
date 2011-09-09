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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.model.catalog.ProjectCatalog;
import com.sonatype.s2.project.model.catalog.ProjectCatalogEntry;
import com.sonatype.s2.project.model.catalog.io.xpp3.S2ProjectCatalogXpp3Reader;
import com.sonatype.s2.project.model.descriptor.EclipseInstallationLocation;
import com.sonatype.s2.project.model.descriptor.EclipseWorkspaceLocation;
import com.sonatype.s2.project.model.descriptor.Project;
import com.sonatype.s2.project.model.descriptor.io.xpp3.S2ProjectDescriptorXpp3Reader;
import com.sonatype.s2.project.model.descriptor.io.xpp3.S2ProjectDescriptorXpp3Writer;

public class S2ProjectFacade
{
    private static Logger log = LoggerFactory.getLogger( S2ProjectFacade.class );

    public static String PROPERTY_PREFIX = "mse-codebase.";

    public static String ARTIFACT_ID_PROPERTY = PROPERTY_PREFIX + "artifactId";

    public static String GROUP_ID_PROPERTY = PROPERTY_PREFIX + "groupId";

    public static String NAME_PROPERTY = PROPERTY_PREFIX + "name";

    public static String VERSION_PROPERTY = PROPERTY_PREFIX + "version";

    public static IS2Project loadProject( InputStream is, boolean strict )
        throws IOException
    {
        IS2Project s2Project;
        try
        {
            s2Project = new S2ProjectDescriptorXpp3Reader().read( is, strict );
        }
        catch ( XmlPullParserException e )
        {
            IOException ioEx = new IOException( e.getMessage() );
            ioEx.initCause( e );
            throw ioEx;
        }

        log.debug( "Loaded s2 project: {}", s2Project.getName() );
        return s2Project;
    }

    public static IS2ProjectCatalog loadProjectCatalog( InputStream is )
        throws IOException
    {
        ProjectCatalog catalog;
        try
        {
            catalog = new S2ProjectCatalogXpp3Reader().read( is );
        }
        catch ( XmlPullParserException e )
        {
            IOException ioEx = new IOException( e.getMessage() );
            ioEx.initCause( e );
            throw ioEx;
        }

        log.debug( "Loaded s2 project catalog: {}", catalog.getName() );
        return catalog;
    }

    public static void applyCatalogUrl( IS2ProjectCatalog catalog, String url )
    {
        if ( catalog instanceof ProjectCatalog )
        {
            ( (ProjectCatalog) catalog ).setUrl( url );
        }
        for ( IS2ProjectCatalogEntry entry : catalog.getEntries() )
        {
            if ( entry instanceof ProjectCatalogEntry )
            {
                ( (ProjectCatalogEntry) entry ).setCatalogUrl( url );
            }
        }
    }

    public static String getCatalogFileUrl( String url )
    {
        String urlStr = url;
        if ( !urlStr.endsWith( "/" ) )
        {
            urlStr += '/';
        }
        urlStr += IS2ProjectCatalog.CATALOG_FILENAME;
        return urlStr;
    }

    /**
     * Creates new IS2Project instance with the given name
     */
    public static IS2Project createProject( String groupId, String id, String version )
    {
        Project project = new Project();
        project.setGroupId( groupId );
        project.setArtifactId( id );
        project.setVersion( version );
        project.setInstallerVersion( IS2Project.CURRENT_INSTALLER_VERSION );

        EclipseInstallationLocation installLocation = new EclipseInstallationLocation();
        installLocation.setDirectory( IS2Project.DEFAULT_INSTALL_PATH );
        project.setEclipseInstallationLocation( installLocation );

        EclipseWorkspaceLocation workspaceLocation = new EclipseWorkspaceLocation();
        workspaceLocation.setDirectory( IS2Project.DEFAULT_WORKSPACE_PATH );
        project.setEclipseWorkspaceLocation( workspaceLocation );

        return project;
    }

    public static void writeProject( IS2Project project, OutputStream out )
        throws IOException
    {
        if ( !( project instanceof Project ) )
        {
            throw new IllegalArgumentException();
        }
        project.setInstallerVersion( IS2Project.CURRENT_INSTALLER_VERSION );

        S2ProjectDescriptorXpp3Writer writer = new S2ProjectDescriptorXpp3Writer();

        Writer w = new OutputStreamWriter( out, "UTF-8" );
        try
        {
            writer.write( w, (Project) project );
        }
        finally
        {
            w.flush();
        }
    }

    public static String replaceVariables( String value, IS2Project project )
    {
        Pattern p = Pattern.compile( "@([^@]*)@" );
        Matcher m = p.matcher( value );
        StringBuffer sb = new StringBuffer();
        while ( m.find() )
        {
            String match = m.group( 1 );
            String newValue = null;
            if ( match.startsWith( PROPERTY_PREFIX ) )
            {
                if ( ARTIFACT_ID_PROPERTY.equals( match ) )
                {
                    newValue = project.getArtifactId();
                }
                else if ( GROUP_ID_PROPERTY.equals( match ) )
                {
                    newValue = project.getGroupId();
                }
                else if ( NAME_PROPERTY.equals( match ) )
                {
                    newValue = project.getName();
                }
                else if ( VERSION_PROPERTY.equals( match ) )
                {
                    newValue = project.getVersion();
                }
                if ( newValue != null )
                {
                    newValue = newValue.replaceAll( "[^\\.\\w\\-]+", "" );
                }
            }
            else
            {
                newValue = System.getProperty( match );
            }

            if ( newValue != null )
            {
                m.appendReplacement( sb, Matcher.quoteReplacement( newValue ) );
            }
        }
        m.appendTail( sb );
        return sb.toString();
    }
}
