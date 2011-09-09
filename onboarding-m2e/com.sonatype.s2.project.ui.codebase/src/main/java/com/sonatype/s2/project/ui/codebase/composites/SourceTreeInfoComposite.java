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
package com.sonatype.s2.project.ui.codebase.composites;

import static org.maven.ide.eclipse.ui.common.FormUtils.nvl;
import static org.maven.ide.eclipse.ui.common.FormUtils.toNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.maven.ide.eclipse.ui.common.validation.SonatypeValidators;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.descriptor.CIServerLocation;
import com.sonatype.s2.project.ui.codebase.Messages;

@SuppressWarnings( "restriction" )
public class SourceTreeInfoComposite
    extends SourceTreeComposite
{
    private Text moduleNameText;

    private boolean moduleNameChanged;

    private Text homepageText;

    private Text documentationText;

    private Text issueTrackingText;

    private Text buildsText;
    
    private static final Logger log = LoggerFactory.getLogger( SourceTreeInfoComposite.class );

    public SourceTreeInfoComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                                    FormToolkit toolkit )
    {
        super( parent, widthGroup, validationGroup, toolkit );

        setLayout( new GridLayout( 2, false ) );

        createProjectNameControls();
        createHomepageControls();
        createDocumentationControls();
        createIssueTrackingControls();
        createBuildsControls();
    }

    private void createProjectNameControls()
    {
        createLabel( Messages.sourceTreeInfoComposite_name_label );

        moduleNameText = createText( "moduleNameText", Messages.sourceTreeInfoComposite_name_name ); //$NON-NLS-1$
        moduleNameText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                moduleNameChanged = true;
                getModule().setName( toNull( moduleNameText ) );
            }
        } );
        moduleNameText.addFocusListener( new FocusAdapter()
        {
            public void focusLost( org.eclipse.swt.events.FocusEvent e )
            {
                if ( moduleNameChanged )
                {
                    moduleNameChanged = false;
                    saveModuleName( toNull( moduleNameText ) );
                }
            }
        } );
        addToValidationGroup( moduleNameText, StringValidators.REQUIRE_NON_EMPTY_STRING );
    }

    private void createHomepageControls()
    {
        createLabel( Messages.sourceTreeInfoComposite_homepage_label );

        homepageText = createText( "homepageText", Messages.sourceTreeInfoComposite_homepage_name ); //$NON-NLS-1$
        homepageText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                getModule().setHomeUrl( toNull( homepageText ) );
                notifyCodebaseChangeListeners();
            }
        } );
        addToValidationGroup( homepageText, SonatypeValidators.EMPTY_OR_URL );
    }

    private void createDocumentationControls()
    {
        createLabel( Messages.sourceTreeInfoComposite_documentation_label );

        documentationText = createText( "documentationText", Messages.sourceTreeInfoComposite_documentation_name ); //$NON-NLS-1$
        documentationText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                getModule().setDocsUrl( toNull( documentationText ) );
                notifyCodebaseChangeListeners();
            }
        } );
        addToValidationGroup( documentationText, SonatypeValidators.EMPTY_OR_URL );
    }

    private void createIssueTrackingControls()
    {
        createLabel( Messages.sourceTreeInfoComposite_issues_label );

        issueTrackingText = createText( "issueTrackingText", Messages.sourceTreeInfoComposite_issues_name ); //$NON-NLS-1$
        issueTrackingText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                getModule().setIssuesUrl( toNull( issueTrackingText ) );
                notifyCodebaseChangeListeners();
            }
        } );
        addToValidationGroup( issueTrackingText, SonatypeValidators.EMPTY_OR_URL );
    }

    private void createBuildsControls()
    {
        createLabel( Messages.sourceTreeInfoComposite_builds_label );

        buildsText = createText( "buildsText", Messages.sourceTreeInfoComposite_builds_name ); //$NON-NLS-1$
        buildsText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                String text = toNull( buildsText );
                getModule().setBuildUrl( text );
                //MECLIPSE-1661 for now just populate the ciServers section based on the url.
                getModule().getCiServers().clear();
                if (text != null) {
                    String[] hudson = parseHudsonWebURL( text );
                    if (hudson != null) {
                        CIServerLocation loc = new CIServerLocation();
                        loc.setUrl( hudson[0] );
                        loc.addJob( hudson[1] );
                        getModule().getCiServers().add( loc );
                    } else {
                        log.debug( "Url " + text + " not recognized as hudson job url.");
                    }
                }
                notifyCodebaseChangeListeners();
            }
        } );
        addToValidationGroup( buildsText, SonatypeValidators.EMPTY_OR_URL );
    }
    
    /**
     * Parse the job URLs as returned used in the hudson job UI and return the server URL and the job name
     * @param jobURL
     * @return array of 2 strings - index 0 is server url and index 1 is the job name 
     */
    private static String[] parseHudsonWebURL( String jobURL )
    {
        //!!!! PLEASE note that this code is copied from HudsonUtils to avoid interdependencies
        // any change done here has to be duplicated there..
        String serverName = null;
        String jobName = null;

        Pattern p = Pattern.compile( "(.*)/job/([^/]*)/*.*" );
        Matcher m = p.matcher( jobURL );
        if ( !m.find() )
            return null;

        serverName = m.group( 1 );
        jobName = m.group( 2 );

        Pattern viewPattern = Pattern.compile( "(.*)/view.*" );
        Matcher m2 = viewPattern.matcher( m.group( 1 ) );
        if ( m2.find() )
            serverName = m2.group( 1 );
        return new String[] { serverName, jobName };
    }
    

    @Override
    protected void update( IS2Project project, IS2Module module )
    {
        moduleNameText.setText( nvl( module.getName() ) );
        homepageText.setText( nvl( module.getHomeUrl() ) );
        documentationText.setText( nvl( module.getDocsUrl() ) );
        issueTrackingText.setText( nvl( module.getIssuesUrl() ) );
        buildsText.setText( nvl( module.getBuildUrl() ) );
    }

    protected void saveModuleName( String name )
    {
        getModule().setName( name );
        notifyCodebaseChangeListeners();
    }
}
