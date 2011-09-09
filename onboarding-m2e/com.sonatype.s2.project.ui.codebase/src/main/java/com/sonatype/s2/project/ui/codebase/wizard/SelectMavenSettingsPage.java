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
package com.sonatype.s2.project.ui.codebase.wizard;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputComposite;
import org.maven.ide.eclipse.ui.common.wizards.RemoteResourceLookupPage;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.ui.codebase.LinkUtil;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.internal.Images;
import com.sonatype.s2.project.ui.internal.composites.NexusUrlComposite;

public class SelectMavenSettingsPage
    extends RemoteResourceLookupPage
{
    private Logger log = LoggerFactory.getLogger( SelectMavenSettingsPage.class );

    private TableViewer tableViewer;

    private String selectedUrl;

    private NexusUrlComposite nexusUrlComposite;

	private String initialSelection;

    public SelectMavenSettingsPage( String serverUrl, String initialSelection )
    {
        super( serverUrl );
        setServerName( Messages.selectMavenSettingsPage_nexusServer );
        setLoadButtonText( Messages.selectMavenSettingsPage_btnLogin );
        setReadyToLoadMessage( Messages.selectMavenSettingsPage_clickLoadNow );
        setResourceLabelText( Messages.selectMavenSettingsPage_lblSettings );
        setSelectMessage( Messages.selectMavenSettingsPage_message );
        setTitle( Messages.selectMavenSettingsPage_title );
        this.initialSelection = initialSelection;
    }

    @Override
    protected UrlInputComposite createUrlInputComposite( Composite parent )
    {
        nexusUrlComposite =
            new NexusUrlComposite( parent, null, getLoadButtonValidationGroup(), null, UrlInputComposite.ALLOW_ANONYMOUS );
        return nexusUrlComposite;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    protected Composite createResourcePanel( Composite container )
    {
        tableViewer = new TableViewer( container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION );
        GridData gd = new GridData( SWT.FILL, SWT.FILL, true, true );
        gd.heightHint = 200;
        gd.widthHint = 400;
        tableViewer.getTable().setLayoutData( gd );

        Link lnkOpenSettings = new Link( container, SWT.NONE );
        lnkOpenSettings.setText( Messages.selectMavenSettingsPage_manage );

        tableViewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                IStructuredSelection sel = (IStructuredSelection) event.getSelection();
                if ( !sel.isEmpty() && sel.getFirstElement() instanceof Template )
                {
                    Template t = (Template) sel.getFirstElement();
                    selectedUrl = t.getDownloadURI();
                }
                else
                {
                    selectedUrl = null;
                }
            }
        } );

        tableViewer.addDoubleClickListener( new IDoubleClickListener()
        {
            public void doubleClick( DoubleClickEvent event )
            {
                if ( selectedUrl != null )
                {
                    getContainer().showPage( getNextPage() );
                }
            }
        } );

        tableViewer.setLabelProvider( new SettingsLabelProvider() );
        tableViewer.setContentProvider( new SettingsLabelProvider() );

        getFinishButtonValidationGroup().add( tableViewer, new Validator<ISelection>()
        {
            public Class<ISelection> modelType()
            {
                return ISelection.class;
            }

            public void validate( Problems problems, String name, ISelection selection )
            {
                if ( selection.isEmpty() )
                {
                    problems.add( Messages.selectMavenSettingsPage_message, Severity.FATAL );
                }
            }
        } );

        lnkOpenSettings.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                LinkUtil.openLink( getManageTemplatesURL(), false );
            }
        } );

        return tableViewer.getTable();
    }

    @Override
    protected void setInput( Object input )
    {
        tableViewer.setInput( input );
    }
    
    protected void setInitialInput( Object input )
    {
    	super.setInitialInput( input );
        if (initialSelection != null && input != null)
        {
        	if (input instanceof Template[])
        	{
        		for (Template t : (Template[])input)
        		{
        			if (initialSelection.equals(t.id))
        			{
        				StructuredSelection s = new StructuredSelection(t);
        				tableViewer.setSelection(s, true);
        				break;
        			}
        		}
        	}
        }
    }

    private URL getManageTemplatesURL()
    {
        String url = getServerUrl();
        if ( url == null || url.length() == 0 )
        {
            return null;//$NON-NLS-1$
        }
        StringBuilder sb = new StringBuilder( url );
        if ( !url.endsWith( "/" ) )//$NON-NLS-1$
        {
            sb.append( '/' );//$NON-NLS-1$
        }
        sb.append( "index.html#m2-settings-templates" );//$NON-NLS-1$
        URL toRet = null;
        try
        {
            toRet = new URL( sb.toString() );
        }
        catch ( MalformedURLException e )
        {
            log.debug( "failed to construct url", e );//$NON-NLS-1$
        }
        return toRet;
    }

    /**
     * url of the selected maven setting
     * 
     * @return
     */
    public String getUrl()
    {
        return selectedUrl;
    }

    private class SettingsLabelProvider
        extends LabelProvider
        implements IStructuredContentProvider
    {

        @Override
        public Image getImage( Object element )
        {
            if ( element instanceof Template )
            {
                return Images.MAVEN_OBJECT;
            }
            return super.getImage( element );
        }

        @Override
        public String getText( Object element )
        {
            if ( element instanceof String )
            {
                return element.toString();
            }
            if ( element instanceof Template )
            {
                Template t = (Template) element;
                return t.getId();
            }
            return super.getText( element );
        }

        public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
        {
        }

        public Object[] getElements( Object inputElement )
        {
            return (Object[]) inputElement;
        }
    }

    @Override
    protected Object loadResources( String uri, IProgressMonitor monitor )
        throws Exception
    {
        IStatus status = nexusUrlComposite.checkNexus( monitor );
        if ( !status.isOK() )
        {
            throw new CoreException( status );
        }
        nexusUrlComposite.saveNexusUrl( monitor );

        if ( !uri.endsWith( "/" ) ) { //$NON-NLS-1$
            uri = uri + "/"; //$NON-NLS-1$
        }
        uri = uri + "service/local/templates/settings"; //$NON-NLS-1$
        InputStream stream = S2IOFacade.openStream( uri, monitor );
        return parseResponse( stream );
    }

    private Template[] parseResponse( InputStream stream )
        throws Exception
    {
        try
        {
            Xpp3Dom dom = Xpp3DomBuilder.build( new XmlStreamReader( stream ) );
            java.util.List<Template> toRet = new ArrayList<Template>();
            Xpp3Dom data = dom.getChild( "data" ); //$NON-NLS-1$
            if ( data != null )
            {
                Xpp3Dom[] templates = data.getChildren( "template" ); //$NON-NLS-1$
                for ( Xpp3Dom template : templates )
                {
                    Template t = new Template();
                    t.setId( template.getChild( "id" ).getValue() ); //$NON-NLS-1$
                    t.setDownloadURI( template.getChild( "downloadURI" ).getValue() ); //$NON-NLS-1$
                    toRet.add( t );
                }
            }
            return toRet.toArray( new Template[0] );
        }
        finally
        {
            stream.close();
        }
    }

    private class Template
    {
        private String id;

        private String downloadURI;

        public void setId( String id )
        {
            this.id = id;
        }

        public String getId()
        {
            return id;
        }

        public void setDownloadURI( String downloadURI )
        {
            this.downloadURI = downloadURI;
        }

        public String getDownloadURI()
        {
            return downloadURI;
        }
    }
}
