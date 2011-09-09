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
package com.sonatype.s2.project.ui.lineup.wizard;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.maven.ide.eclipse.ui.common.ErrorHandlingUtils;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputComposite;
import org.maven.ide.eclipse.ui.common.wizards.RemoteArtifactLookupPage;

import com.sonatype.s2.project.ui.internal.composites.NexusUrlComposite;
import com.sonatype.s2.project.ui.lineup.Messages;

public class SelectLineupPage
    extends RemoteArtifactLookupPage<Xpp3Dom>
{
    private static final String DOM_DATA = "data"; //$NON-NLS-1$

    private static final String DOM_P2_LINEUP = "p2-lineup"; //$NON-NLS-1$

    private static final String DOM_GROUP_ID = "groupId"; //$NON-NLS-1$

    private static final String DOM_ARTIFACT_ID = "id"; //$NON-NLS-1$

    private static final String DOM_VERSION = "version"; //$NON-NLS-1$

    private static final String DOM_REPOSITORY_URL = "repositoryUrl"; //$NON-NLS-1$

    public static final String LINEUPS_RESOURCE_URI = "service/local/p2/lineups"; //$NON-NLS-1$
    
    private NexusUrlComposite nexusUrlComposite;

	private String initialSelection;


	public SelectLineupPage( String serverUrl, String initialPath )
    {
        super( serverUrl );
        setServerName( Messages.selectLineupPage_nexusServer_expanded );
        setLoadButtonText( Messages.selectLineupPage_loadLineups );
        setReadyToLoadMessage( Messages.selectLineupPage_clickLoadNow );
        setResourceLabelText( Messages.selectLineupPage_availableLineups );
        setSelectMessage( Messages.selectLineupPage_selectLineup );
        setTitle( Messages.selectLineupPage_title );
        initialSelection = initialPath;
    }

    public String getLineupUrl()
    {
        return getSelection().getEntry().getChild( DOM_REPOSITORY_URL ).getValue();
    }
    
    @Override
    protected UrlInputComposite createUrlInputComposite( Composite parent )
    {
        nexusUrlComposite = new NexusUrlComposite( parent, null, getLoadButtonValidationGroup(), null, UrlInputComposite.ALLOW_ANONYMOUS );
        return nexusUrlComposite;
    }

    @Override
    protected Object loadResources( String url, IProgressMonitor monitor )
        throws Exception
    {
        IStatus status = nexusUrlComposite.checkNexus( monitor );
        if ( !status.isOK() )
        {
            throw new CoreException( status );
        }
        nexusUrlComposite.saveNexusUrl( monitor );
        
        if ( !url.endsWith( "/" ) ) //$NON-NLS-1$
        {
            url += '/';
        }
        url += LINEUPS_RESOURCE_URI;

        InputStream in = S2IOFacade.openStream( url, monitor );
        try
        {
            Xpp3Dom dom = Xpp3DomBuilder.build( new XmlStreamReader( in ) );

            Xpp3Dom data = dom.getChild( DOM_DATA );
            if ( data != null )
            {
                return createGroups( Arrays.asList( data.getChildren( DOM_P2_LINEUP ) ) );
            }
            return new ArrayList<Object>();
        }
        finally
        {
            in.close();
        }
    }

    @Override
    protected String exceptionToUIText( Exception e )
    {
        return ErrorHandlingUtils.convertNexusIOExceptionToUIText( e, Messages.selectLineupPage_error_authFailed,
                                                                   Messages.selectLineupPage_error_forbidden,
                                                                   Messages.selectLineupPage_error_notfound );
    }

    @Override
    protected String getArtifactId( Xpp3Dom entry )
    {
        return entry.getChild( DOM_ARTIFACT_ID ).getValue();
    }

    @Override
    protected String getGroupId( Xpp3Dom entry )
    {
        return entry.getChild( DOM_GROUP_ID ).getValue();
    }

    @Override
    protected String getVersion( Xpp3Dom entry )
    {
        return entry.getChild( DOM_VERSION ).getValue();
    }

    @Override
    public String getServerUrl()
    {
        return super.getServerUrl();
    }
    
    @Override
	protected void setInitialInput(Object input) {
		super.setInitialInput( input );
		if (initialSelection != null) {
			if (input instanceof Collection) {
				Collection<Object> col = (Collection<Object>)input;
				for (Object obj : col) {
					if (obj instanceof RemoteArtifactLookupPage<?>.Group) {
						RemoteArtifactLookupPage<?>.Group grp = (RemoteArtifactLookupPage<?>.Group) obj;
						//rely on toString() in Group
						String group = grp.toString().replace('.', '/');
						if (initialSelection.startsWith(group)) {
							for (RemoteArtifactLookupPage<?>.Artifact art : grp.getArtifacts()) {
								if (initialSelection.startsWith(group + "/" + art.toString())) {
									for (RemoteArtifactLookupPage<?>.Version vers : art.getVersions()) {
										if (initialSelection.equals(vers.getGroupId().replace('.', '/') + "/" + vers.getArtifactId() + "/" + vers.getVersion())) {
											//select this one..
											getTreeViewer().expandToLevel(grp, 1);
											getTreeViewer().expandToLevel(art, 1);
											getTreeViewer().reveal(vers);
											getTreeViewer().setSelection(new StructuredSelection(vers), true);
											break;
										}
									}
								}
							}
						}
					}
				}
				initialSelection = null;
			}
		} 
	}
    
}
