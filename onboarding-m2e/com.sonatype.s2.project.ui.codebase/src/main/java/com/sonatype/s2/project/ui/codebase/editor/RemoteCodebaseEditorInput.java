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
package com.sonatype.s2.project.ui.codebase.editor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.jar.JarInputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction.MavenPathStorageEditorInput;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.maven.ide.eclipse.io.S2IOFacade;

import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.model.IResourceLocation;
import com.sonatype.s2.project.prefs.IPreferenceManager;
import com.sonatype.s2.project.prefs.PreferenceGroup;
import com.sonatype.s2.project.ui.codebase.Activator;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.internal.DownloadJob;
import com.sonatype.s2.project.ui.internal.Images;
import com.sonatype.s2.project.ui.internal.ProjectData;

public class RemoteCodebaseEditorInput
    extends AbstractCodebaseEditorInput
    implements ICodebaseEditorInput, IEditorInput
{
    public final static String SETTINGS_XML = "settings.xml"; //$NON-NLS-1$

    private MavenPathStorageEditorInput mavenSettings;

    private MavenPathStorageEditorInput eclipsePreferences;

    private Image codebaseImage;

    private String url;

    public RemoteCodebaseEditorInput( ProjectData projectData )
    {
        super( projectData.getProject() );
        url = projectData.getUrl();
        codebaseImage = projectData.getImage();
        if ( codebaseImage != null )
        {
            setImageDescriptor( Images.getImageDescriptor( url ) );
        }
    }

    public MavenPathStorageEditorInput getMavenSettings()
    {
        return mavenSettings;
    }

    public MavenPathStorageEditorInput getEclipsePreferences()
    {
        return eclipsePreferences;
    }

    public Image getCodebaseImage()
    {
        return codebaseImage;
    }

    public String getUrl()
    {
        return url;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( o instanceof RemoteCodebaseEditorInput )
        {
            return this.url.equals( ( (RemoteCodebaseEditorInput) o ).url );
        }
        return false;
    }

    public void load( IProgressMonitor monitor )
        throws CoreException
    {
        loadMavenSettings( monitor );

        loadEclipsePreferences( monitor );

        monitor.done();
    }

    private void loadMavenSettings( IProgressMonitor monitor )
        throws CoreException
    {
        final IResourceLocation settingsLocation = getProject().getMavenSettingsLocation();
        if ( settingsLocation != null )
        {
            monitor.subTask( Messages.remoteCodebaseEditorInput_mavenSettings_job );
            final String settingsUrl = settingsLocation.getUrl();
            final byte[] bytes =
                new DownloadJob<byte[]>( Messages.remoteCodebaseEditorInput_mavenSettings_authenticationError,
                                         Messages.remoteCodebaseEditorInput_mavenSettings_label, settingsUrl )
                {
                    @Override
                    public byte[] run( IProgressMonitor monitor )
                        throws CoreException
                    {
                        return S2ProjectCore.getInstance().loadMavenSettings( settingsLocation, monitor );
                    }
                }.download( monitor );
            mavenSettings = new MavenPathStorageEditorInput( SETTINGS_XML, SETTINGS_XML, url, bytes );
        }
    }

    private void loadEclipsePreferences( IProgressMonitor monitor )
        throws CoreException
    {
        final IResourceLocation preferencesLocation = getProject().getEclipsePreferencesLocation();
        if ( preferencesLocation != null )
        {
            monitor.subTask( Messages.remoteCodebaseEditorInput_eclipsePreferences_job );
            final String preferencesUrl = preferencesLocation.getUrl();
            setEclipsePreferenceGroups( new DownloadJob<Collection<PreferenceGroup>>(
                                                                                      Messages.remoteCodebaseEditorInput_eclipsePreferences_authenticationError,
                                                                                      Messages.remoteCodebaseEditorInput_eclipsePreferences_label,
                                                                                      preferencesUrl )
            {
                @Override
                public Collection<PreferenceGroup> run( IProgressMonitor monitor )
                    throws CoreException
                {
                    Exception exception = null;
                    try
                    {
                        InputStream is = S2IOFacade.openStream( preferencesUrl, monitor );
                        try
                        {
                            JarInputStream jis = new JarInputStream( is );
                            IPreferenceManager manager = S2ProjectCore.getInstance().getPrefManager();
                            try
                            {
                                eclipsePreferences =
                                    new MavenPathStorageEditorInput( Messages.remoteCodebaseEditorInput_eclipsePreferences,
                                                                 Messages.remoteCodebaseEditorInput_eclipsePreferences,
                                                                 preferencesUrl, manager.getEclipsePreferences( jis ) );
                                return manager.getPreferenceGroups( jis );
                            }
                            finally
                            {
                                jis.close();
                            }
                        }
                        finally
                        {
                            is.close();
                        }
                    }
                    catch ( IOException e )
                    {
                        exception = e;
                    }
                    catch ( URISyntaxException e )
                    {
                        exception = e;
                    }

                    if ( exception != null )
                    {
                        throw new CoreException(
                                                 new Status(
                                                             IStatus.ERROR,
                                                             Activator.PLUGIN_ID,
                                                             Messages.remoteCodebaseEditorInput_eclipsePreferences_error,
                                                             exception ) );
                    }

                    return null;
                }
            }.download( monitor ) );
        }
    }

    public void doSave( IProgressMonitor monitor )
        throws CoreException
    {
        // unsuppored, yet
    }
}
