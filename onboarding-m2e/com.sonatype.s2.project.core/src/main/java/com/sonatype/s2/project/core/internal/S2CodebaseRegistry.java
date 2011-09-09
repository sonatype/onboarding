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
package com.sonatype.s2.project.core.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.core.IWorkspaceCodebase;
import com.sonatype.s2.project.core.IWorkspaceSourceTree;
import com.sonatype.s2.project.core.internal.io.xpp3.S2WorkspaceCodebasesXpp3Reader;
import com.sonatype.s2.project.core.internal.io.xpp3.S2WorkspaceCodebasesXpp3Writer;
import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.S2ProjectFacade;

public class S2CodebaseRegistry
{
    public static final String CODEBASE_REGISTRY_FILENAME = "codebases.xml";

    private static Logger log = LoggerFactory.getLogger( S2CodebaseRegistry.class );

    private Map<String, IWorkspaceCodebase> codebases;

    private final File registryFile;

    private final File basedir;

    public S2CodebaseRegistry( File basedir )
    {
        this.basedir = basedir;
        this.registryFile = new File( basedir, CODEBASE_REGISTRY_FILENAME );
        this.codebases = new LinkedHashMap<String, IWorkspaceCodebase>();
        try
        {
            Workspace persistentModel;
            InputStream is = new BufferedInputStream( new FileInputStream( registryFile ) );
            try
            {
                S2WorkspaceCodebasesXpp3Reader reader = new S2WorkspaceCodebasesXpp3Reader();
                persistentModel = reader.read( is );
            }
            finally
            {
                IOUtil.close( is );
            }

            for ( IWorkspaceCodebase codebase : persistentModel.getCodebases() )
            {
                String key = getKey( codebase );
                if ( !codebases.containsKey( key ) )
                {
                    try
                    {
                        ( (WorkspaceCodebase) codebase ).setS2Project( getS2Project( codebase.getGroupId(),
                                                                                     codebase.getArtifactId(),
                                                                                     codebase.getVersion() ) );

                        codebases.put( key, codebase );
                    }
                    catch ( CoreException e )
                    {
                        log.error( "Could not read workspace codebase", e );
                    }
                }
                else
                {
                    log.warn( "Duplicate codebase groupId:artifactId " + key );
                }
            }
        }
        catch ( XmlPullParserException e )
        {
            log.debug( "Could not read codebase registry file from " + registryFile.getAbsolutePath(), e );
        }
        catch ( FileNotFoundException e )
        {
            // expected and silently ignored
        }
        catch ( IOException e )
        {
            log.debug( "Could not read codebase registry file from " + registryFile.getAbsolutePath(), e );
        }
    }

    public void addCodebase( WorkspaceCodebase codebase )
    {
        String key = getKey( codebase );

        // TODO there is currently no way to delete codebases, so the check below results in false-positives
        // if ( codebases.containsKey( key ) )
        // {
        // throw new IllegalStateException( "Duplicate codebase groupId:artifactId " + key );
        // }

        codebases.put( key, codebase );
    }

    public static String getKey( IWorkspaceCodebase codebase )
    {
        return codebase.getGroupId() + ":" + codebase.getArtifactId();
    }

    public void save()
        throws CoreException
    {
        Workspace workspace = new Workspace();

        for ( IWorkspaceCodebase codebase : codebases.values() )
        {
            workspace.addCodebase( codebase );
        }

        try
        {
            OutputStream os = new BufferedOutputStream( new FileOutputStream( registryFile ) );
            try
            {
                S2WorkspaceCodebasesXpp3Writer writer = new S2WorkspaceCodebasesXpp3Writer();
                Writer w = new OutputStreamWriter( os, "UTF-8" );
                try
                {
                    writer.write( w, workspace );
                }
                finally
                {
                    w.flush();
                }
            }
            finally
            {
                IOUtil.close( os );
            }
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                 "Could not persist workspace codebases registry", e ) );
        }
    }

    public List<IWorkspaceCodebase> getCodebases()
    {
        return new ArrayList<IWorkspaceCodebase>( codebases.values() );
    }

    public void clear()
        throws CoreException
    {
        codebases.clear();
        save();
    }

    public static WorkspaceCodebase createCodebase( IS2Project descriptor )
    {
        WorkspaceCodebase codebase = new WorkspaceCodebase();
        codebase.setS2Project( descriptor );
        codebase.setDescriptorUrl( descriptor.getDescriptorUrl() );
        codebase.setGroupId( descriptor.getGroupId() );
        codebase.setArtifactId( descriptor.getArtifactId() );
        codebase.setVersion( descriptor.getVersion() );
        if ( descriptor.getP2LineupLocation() != null )
        {
            codebase.setP2LineupLocation( descriptor.getP2LineupLocation().getUrl() );
        }
        return codebase;
    }

    public static WorkspaceSourceTree createSourceTree( IS2Module module, File location )
    {
        if ( module.getScmLocation() == null )
        {
            throw new IllegalArgumentException();
        }

        WorkspaceSourceTree sourceTree = new WorkspaceSourceTree();
        sourceTree.setName( module.getName() );
        sourceTree.setProfiles( module.getProfiles() );
        sourceTree.setRoots( module.getRoots() );
        try
        {
            sourceTree.setLocation( location.getCanonicalPath() );
        }
        catch ( IOException e )
        {
            sourceTree.setLocation( location.getAbsolutePath() );
        }

        sourceTree.setScmUrl( module.getScmLocation().getUrl() );
        sourceTree.setScmBranch( module.getScmLocation().getBranch() );

        return sourceTree;
    }

    public void replaceCodebase( WorkspaceCodebase originalCodebase, WorkspaceCodebase newCodebase,
                                 IS2Project newS2Project )
        throws CoreException
    {
        if ( originalCodebase != null )
        {
            codebases.remove( getKey( originalCodebase ) );
            removeS2Project( originalCodebase.getGroupId(), originalCodebase.getArtifactId(),
                             originalCodebase.getVersion() );
        }
        addCodebase( newCodebase );
        setS2Project( newS2Project );
    }

    public IWorkspaceSourceTree getSourceTree( IPath memberPath )
    {
        for ( IWorkspaceCodebase codebase : codebases.values() )
        {
            for ( IWorkspaceSourceTree tree : codebase.getSourceTrees() )
            {
                IPath location = Path.fromOSString( tree.getLocation() );
                if ( location.isPrefixOf( memberPath ) )
                {
                    return tree;
                }
            }
        }

        return null;
    }

    public IS2Project getS2Project( String groupId, String artifactId, String version )
        throws CoreException
    {
        File file = getStateLocation( groupId, artifactId, version );
        if ( !file.canRead() )
        {
            return null;
        }

        try
        {
            InputStream is = new BufferedInputStream( new FileInputStream( file ) );
            try
            {
                return S2ProjectFacade.loadProject( is, true );
            }
            finally
            {
                IOUtil.close( is );
            }
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                 "Could not persist workspace codebases registry", e ) );
        }
    }

    public void setS2Project( IS2Project s2Project )
        throws CoreException
    {
        File file = getStateLocation( s2Project.getGroupId(), s2Project.getArtifactId(), s2Project.getVersion() );
        try
        {
            OutputStream os = new BufferedOutputStream( new FileOutputStream( file ) );
            try
            {
                S2ProjectFacade.writeProject( s2Project, os );
            }
            finally
            {
                IOUtil.close( os );
            }
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, S2ProjectPlugin.PLUGIN_ID,
                                                 "Could not persist workspace codebases registry", e ) );
        }
    }

    private File getStateLocation( String groupId, String artifactId, String version )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( groupId ).append( '_' ).append( artifactId ).append( '_' ).append( version );
        return new File( basedir, sb.toString() );
    }

    private void removeS2Project( String groupId, String artifactId, String version )
    {
        getStateLocation( groupId, artifactId, version ).delete();
    }
}
