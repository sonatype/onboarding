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

import static com.sonatype.s2.project.ui.codebase.LinkUtil.createHyperlink;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.maven.ide.eclipse.ui.common.authentication.IRealmChangeListener;
import org.maven.ide.eclipse.ui.common.editor.ValidatingFormPage;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.codebase.Activator;
import com.sonatype.s2.project.ui.codebase.LinkUtil;
import com.sonatype.s2.project.ui.codebase.composites.ICodebaseChangeListener;

public abstract class AbstractCodebaseEditorPage
    extends ValidatingFormPage
    implements ICodebaseChangeListener, IRealmChangeListener
{
    private CodebaseDescriptorEditor editor;

    public AbstractCodebaseEditorPage( CodebaseDescriptorEditor editor, String title )
    {
        super( editor, getPageId( title ), title );
        this.setCodebaseEditor( editor );
    }

    public static String getPageId( String suffix )
    {
        return Activator.PLUGIN_ID + ".codebaseeditor." + suffix;
    }

    protected String validateUrl( String url )
    {
        try
        {
            if ( url.length() > 0 )
            {
                new URL( url );
            }
            return null;
        }
        catch ( MalformedURLException e )
        {
            return e.getMessage();
        }
    }

    protected void setCodebaseEditor( CodebaseDescriptorEditor editor )
    {
        this.editor = editor;
    }

    protected CodebaseDescriptorEditor getCodebaseEditor()
    {
        return editor;
    }

    protected Text addUrlInput( Composite parent, FormToolkit toolkit, String labelText, String controlName, int span )
    {
        final Text[] text = new Text[1];
        final Hyperlink hyperlink = createHyperlink( parent, toolkit, new LinkUtil.IUrlProvider()
        {
            public String getUrl()
            {
                return text[0].getText();
            }
        }, labelText, null );
        hyperlink.setLayoutData( new GridData( SWT.LEFT, SWT.TOP, false, false ) );
        hyperlink.setEnabled( false );

        text[0] = toolkit.createText( parent, "" );
        text[0].setData( "name", controlName );
        GridData gd = new GridData( SWT.FILL, SWT.CENTER, true, false, span, 1 );
        gd.widthHint = 100;
        text[0].setLayoutData( gd );
        text[0].addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                String url = text[0].getText().trim();
                hyperlink.setEnabled( url.length() > 0 && validateUrl( url ) == null );
                setDirty( true );
            }
        } );

        return text[0];
    }

    public void codebaseChanged( IS2Project project )
    {
        setDirty( true );
    }
    
    public void realmsChanged()
    {
        CodebaseDescriptorEditor.updateAllCodebaseEditors();
    }
}
