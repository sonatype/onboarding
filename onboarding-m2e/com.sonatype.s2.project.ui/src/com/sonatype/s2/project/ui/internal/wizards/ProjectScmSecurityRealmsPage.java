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
package com.sonatype.s2.project.ui.internal.wizards;

import java.beans.Beans;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.AuthenticationType;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.authentication.internal.AuthData;
import org.maven.ide.eclipse.ui.common.ErrorHandlingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.internal.Activator;
import com.sonatype.s2.project.ui.internal.Images;
import com.sonatype.s2.project.ui.internal.Messages;
import com.sonatype.s2.project.validator.AccessValidationStatus;
import com.sonatype.s2.project.validator.ValidationFacade;

public class ProjectScmSecurityRealmsPage
    extends WizardPage
{
    private static Logger log = LoggerFactory.getLogger( ProjectScmSecurityRealmsPage.class );

    TableViewer tableViewer;

    private IS2Project project;

    private List<IStatus> statuses;

    private String realmId;

    protected boolean credentialsValidated;

    private IStatus validationStatus;

    private IAction copyAction;

    private Label label;

    private Table table;

    private Label lblSecurityRealm;

    private Label lblUsername;

    private Text txtUsername;

    private Label lblPassword;

    private Text txtPassword;

    private Label lblClientCertificate;

    private Text txtClientCertificate;

    private Button btnClientCertificate;

    private Label lblPassphrase;

    private Text txtPassphrase;

    private Text txtSecurityRealm;

    private Label lblHint;

    private boolean showCertificate;

    private Composite container;

    private Button btnUseSslClient;

    public ProjectScmSecurityRealmsPage( IS2Project project, String realmId, List<IStatus> statuses, boolean certificate )
    {
        super( Messages.materializationWizard_realmsPage_title );

        this.project = project;
        this.realmId = realmId;
        this.statuses = new ArrayList<IStatus>();
        this.validationStatus =
            new MultiStatus( Activator.PLUGIN_ID, 0, statuses.toArray( new IStatus[statuses.size()] ), null, null );
        credentialsValidated = false;
        this.showCertificate = certificate;

        setTitle( Messages.materializationWizard_realmsPage_title );
        setDescription( showCertificate ? Messages.ProjectScmSecurityRealmsPage_description
                        : Messages.ProjectScmSecurityRealmsPage_description_short );
    }

    public void createControl( Composite parent )
    {
        container = new Composite( parent, SWT.NULL );

        setControl( container );

        lblSecurityRealm = new Label( container, SWT.NONE );
        lblSecurityRealm.setText( Messages.ProjectScmSecurityRealmsPage_lblSecurityRealm );

        txtSecurityRealm = new Text( container, SWT.READ_ONLY );
        txtSecurityRealm.setBackground( txtSecurityRealm.getDisplay().getSystemColor( SWT.COLOR_WIDGET_BACKGROUND ) );

        lblUsername = new Label( container, SWT.NONE );
        lblUsername.setText( Messages.ProjectScmSecurityRealmsPage_lblUsername );

        txtUsername = new Text( container, SWT.BORDER );

        lblPassword = new Label( container, SWT.NONE );
        lblPassword.setText( Messages.ProjectScmSecurityRealmsPage_lblPassword );

        txtPassword = new Text( container, SWT.BORDER | SWT.PASSWORD );

        btnUseSslClient = new Button( container, SWT.CHECK );
        btnUseSslClient.setText( Messages.ProjectScmSecurityRealmsPage_use_ssl );

        lblClientCertificate = new Label( container, SWT.NONE );
        lblClientCertificate.setText( Messages.ProjectScmSecurityRealmsPage_file );

        txtClientCertificate = new Text( container, SWT.BORDER );

        btnClientCertificate = new Button( container, SWT.NONE );
        btnClientCertificate.setText( Messages.ProjectScmSecurityRealmsPage_btn_ClientCertificate );

        lblPassphrase = new Label( container, SWT.NONE );
        lblPassphrase.setText( Messages.ProjectScmSecurityRealmsPage_lblPassphrase );

        txtPassphrase = new Text( container, SWT.BORDER | SWT.PASSWORD );

        createTreeViewer( container );

        lblHint = new Label( container, SWT.NONE );
        lblHint.setText( Messages.ProjectScmSecurityRealmsPage_lblHint );
        if ( !showCertificate )
        {
            txtClientCertificate.setVisible( false );
            lblClientCertificate.setVisible( false );
            btnClientCertificate.setVisible( false );
            lblPassphrase.setVisible( false );
            txtPassphrase.setVisible( false );
            btnUseSslClient.setVisible( false );
        }

        GroupLayout gl_container = new GroupLayout( container );
        gl_container.setHorizontalGroup( gl_container.createParallelGroup( GroupLayout.LEADING ).add( gl_container.createSequentialGroup().add( gl_container.createParallelGroup( GroupLayout.LEADING ).add( gl_container.createSequentialGroup().addContainerGap().add( gl_container.createParallelGroup( GroupLayout.LEADING ).add( lblSecurityRealm ).add( gl_container.createSequentialGroup().add( 12 ).add( gl_container.createParallelGroup( GroupLayout.LEADING ).add( lblPassphrase ).add( lblClientCertificate ) ) ) ).addPreferredGap( LayoutStyle.UNRELATED ).add( gl_container.createParallelGroup( GroupLayout.LEADING ).add( gl_container.createSequentialGroup().add( gl_container.createParallelGroup( GroupLayout.LEADING ).add( txtUsername,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   GroupLayout.DEFAULT_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   143,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   Short.MAX_VALUE ).add( txtPassword,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          GroupLayout.DEFAULT_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          143,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          Short.MAX_VALUE ).add( txtPassphrase,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 GroupLayout.PREFERRED_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 143,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 GroupLayout.PREFERRED_SIZE ) ).addPreferredGap( LayoutStyle.RELATED ).add( lblHint ).add( 152 ) ).add( gl_container.createSequentialGroup().add( txtClientCertificate,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  GroupLayout.DEFAULT_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  381,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  Short.MAX_VALUE ).addPreferredGap( LayoutStyle.RELATED ).add( btnClientCertificate ) ).add( txtSecurityRealm,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              GroupLayout.DEFAULT_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              468,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              Short.MAX_VALUE ) ) ).add( gl_container.createSequentialGroup().add( 18 ).add( btnUseSslClient ) ).add( gl_container.createSequentialGroup().add( 18 ).add( lblUsername ) ).add( gl_container.createSequentialGroup().add( 18 ).add( lblPassword ) ).add( gl_container.createSequentialGroup().add( 12 ).add( label ) ).add( gl_container.createSequentialGroup().add( 12 ).add( table,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               GroupLayout.DEFAULT_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               566,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               Short.MAX_VALUE ) ) ).addContainerGap() ) );
        gl_container.setVerticalGroup( gl_container.createParallelGroup( GroupLayout.LEADING ).add( gl_container.createSequentialGroup().addContainerGap().add( gl_container.createParallelGroup( GroupLayout.BASELINE ).add( lblSecurityRealm ).add( txtSecurityRealm,
                                                                                                                                                                                                                                                      GroupLayout.PREFERRED_SIZE,
                                                                                                                                                                                                                                                      GroupLayout.DEFAULT_SIZE,
                                                                                                                                                                                                                                                      GroupLayout.PREFERRED_SIZE ) ).addPreferredGap( LayoutStyle.RELATED ).add( gl_container.createParallelGroup( GroupLayout.BASELINE ).add( lblUsername ).add( txtUsername,
                                                                                                                                                                                                                                                                                                                                                                                                                  GroupLayout.PREFERRED_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                  GroupLayout.DEFAULT_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                  GroupLayout.PREFERRED_SIZE ).add( lblHint ) ).addPreferredGap( LayoutStyle.RELATED ).add( gl_container.createParallelGroup( GroupLayout.BASELINE ).add( lblPassword ).add( txtPassword,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             GroupLayout.PREFERRED_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             GroupLayout.DEFAULT_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             GroupLayout.PREFERRED_SIZE ) ).add( 18 ).add( btnUseSslClient ).addPreferredGap( LayoutStyle.RELATED ).add( gl_container.createParallelGroup( GroupLayout.BASELINE ).add( lblClientCertificate ).add( txtClientCertificate,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   GroupLayout.PREFERRED_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   GroupLayout.DEFAULT_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   GroupLayout.PREFERRED_SIZE ).add( btnClientCertificate,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     GroupLayout.PREFERRED_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     30,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     GroupLayout.PREFERRED_SIZE ) ).addPreferredGap( LayoutStyle.RELATED ).add( gl_container.createParallelGroup( GroupLayout.BASELINE ).add( lblPassphrase ).add( txtPassphrase,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   GroupLayout.PREFERRED_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   GroupLayout.DEFAULT_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   GroupLayout.PREFERRED_SIZE ) ).add( 18 ).add( label ).addPreferredGap( LayoutStyle.RELATED ).add( table,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     GroupLayout.DEFAULT_SIZE,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     106,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     Short.MAX_VALUE ).addContainerGap() ) );
        gl_container.linkSize( new Control[] { txtPassphrase, txtUsername, txtPassword }, GroupLayout.HORIZONTAL );
        gl_container.setHonorsVisibility( true );
        container.setLayout( gl_container );

        initialize();
    }

    private void createTreeViewer( Composite container )
    {
        label = new Label( container, SWT.NONE );
        label.setText( Messages.materializationWizard_realmsPage_resources );

        tableViewer = new TableViewer( container, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL );

        table = tableViewer.getTable();
        table.setBackground( table.getDisplay().getSystemColor( SWT.COLOR_WIDGET_BACKGROUND ) );

        tableViewer.setContentProvider( new IStructuredContentProvider()
        {
            public Object[] getElements( Object input )
            {
                return statuses.toArray( new Object[statuses.size()] );
            }

            public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
            {
            }

            public void dispose()
            {
            }
        } );

        tableViewer.setLabelProvider( new LabelProvider()
        {
            @Override
            public Image getImage( Object element )
            {
                if ( element instanceof IStatus )
                {
                    switch ( ( (IStatus) element ).getSeverity() )
                    {
                        case IStatus.OK:
                            return Images.STATUS_OK;
                        case IStatus.ERROR:
                            return Images.STATUS_ERROR;
                        case IStatus.WARNING:
                            return Images.STATUS_WARNING;
                        case IStatus.INFO:
                            return Images.STATUS_INFO;
                    }
                }
                return null;
            }

            private String exceptionMessage( IStatus[] children )
            {
                if ( children == null )
                    return null;
                String message = null;
                for ( IStatus child : children )
                {
                    message = exceptionMessage( child.getChildren() );
                    if ( message != null )
                    {
                        return message;
                    }
                    if ( child.getException() != null )
                    {
                        message =
                            ErrorHandlingUtils.convertNexusIOExceptionToUIText( child.getException(),
                                                                                Messages.ProjectScmSecurityRealmsPage_error_auth,
                                                                                Messages.ProjectScmSecurityRealmsPage_error_forbidden,
                                                                                Messages.ProjectScmSecurityRealmsPage_error_notfound );
                    }
                }
                return message;
            }

            @Override
            public String getText( Object element )
            {
                String url = ""; //$NON-NLS-1$
                String message = ""; //$NON-NLS-1$
                if ( element instanceof AccessValidationStatus )
                {
                    AccessValidationStatus st = (AccessValidationStatus) element;
                    url = st.getLocation().getUrl();
                    message = st.getMessage();
                    String childMessage = exceptionMessage( st.getChildren() );
                    if ( childMessage != null )
                    {
                        message = childMessage;
                    }

                    return message + " - " + url; //$NON-NLS-1$
                }
                else if ( element instanceof IStatus )
                {
                    return ( (IStatus) element ).getMessage();
                }
                return super.getText( element );
            }
        } );

        copyAction = new Action( Messages.actions_copy_title )
        {
            @Override
            public void run()
            {
                StringBuilder sb = new StringBuilder();
                for ( TableItem item : table.getSelection() )
                {
                    if ( sb.length() > 0 )
                    {
                        sb.append( '\n' );
                    }
                    sb.append( item.getText() );
                }

                Clipboard clipboard = new Clipboard( getShell().getDisplay() );
                clipboard.setContents( new Object[] { sb.toString() },
                                       new TextTransfer[] { TextTransfer.getInstance() } );
            }
        };

        MenuManager menuMgr = new MenuManager(); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown( true );
        menuMgr.addMenuListener( new IMenuListener()
        {
            public void menuAboutToShow( IMenuManager manager )
            {
                if ( !tableViewer.getSelection().isEmpty() )
                {
                    manager.add( copyAction );
                }
            }
        } );
        Menu menu = menuMgr.createContextMenu( table );
        table.setMenu( menu );

        tableViewer.setInput( project );
        refreshViewer();
    }

    private void updateSSL()
    {
        boolean use = btnUseSslClient.getSelection();
        txtClientCertificate.setEnabled( use );
        btnClientCertificate.setEnabled( use );
        txtPassphrase.setEnabled( use );
        lblClientCertificate.setEnabled( use );
        lblPassphrase.setEnabled( use );

        txtClientCertificate.setVisible( use );
        lblClientCertificate.setVisible( use );
        btnClientCertificate.setVisible( use );
        lblPassphrase.setVisible( use );
        txtPassphrase.setVisible( use );
    }

    private void initialize()
    {
        if ( Beans.isDesignTime() )
            return;
        updateSSL();
        btnUseSslClient.addSelectionListener( new SelectionAdapter()
        {
            public void widgetSelected( SelectionEvent e )
            {
                updateSSL();
            }
        } );

        btnClientCertificate.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent event )
            {
                String filename = null;
                String current = txtClientCertificate.getText().trim();
                if ( System.getProperty( "jnlp" ) != null && System.getProperty( "osgi.os" ).contains( "linux" ) )
                {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle( Messages.ProjectScmSecurityRealmsPage_fileSelect_title );
                    if ( current.length() > 0 )
                        try
                        {
                            chooser.setCurrentDirectory( new File( current ) );
                        }
                        catch ( Exception e )
                        {
                            // do nothing
                        }
                    chooser.setAcceptAllFileFilterUsed( true );
                    chooser.setFileFilter( new Filter() );

                    if ( JFileChooser.APPROVE_OPTION == chooser.showOpenDialog( SWT_AWT.getFrame( getShell() ) ) )
                        filename = chooser.getSelectedFile().toString();
                }
                else
                {
                    FileDialog fd = new FileDialog( getShell(), SWT.OPEN );
                    fd.setText( Messages.ProjectScmSecurityRealmsPage_fileSelect_title );
                    fd.setFilterExtensions( new String[] { "*.p12;*.crt", "*.*" //$NON-NLS-1$ $NON-NLS-2$
                    } );
                    fd.setFilterNames( new String[] { Messages.ProjectScmSecurityRealmsPage_fileSelect_filter1,
                        Messages.ProjectScmSecurityRealmsPage_fileSelect_filter2 } );
                    if ( current.length() > 0 )
                    {
                        fd.setFileName( current );
                    }
                    filename = fd.open();
                }
                if ( filename != null )
                {
                    txtClientCertificate.setText( filename );
                }
            }
        } );

        IAuthData authData;
        IAuthRealm realm = AuthFacade.getAuthRegistry().getRealm( realmId );
        if ( realm != null )
        {
            txtSecurityRealm.setText( realm.getName() );
            authData = realm.getAuthData();
        }
        else
        {
            // realmId is actually a url (that's not mapped to a realm)
            txtSecurityRealm.setText( realmId );
            authData = AuthFacade.getAuthService().select( realmId );
        }

        if ( authData != null )
        {
            btnUseSslClient.setEnabled( authData.allowsCertificate() );
            txtUsername.setEnabled( authData.allowsUsernameAndPassword() );
            txtPassword.setEnabled( authData.allowsUsernameAndPassword() );
        }
        checkValidationStatus();
    }

    public boolean validateCredentials()
    {
        if ( !credentialsValidated && project != null )
        {
            try
            {
                statuses.clear();

                String userName = txtUsername.getText().trim();
                String password = txtPassword.getText();
                File certificatePath = null;
                String passphrase = null;
                boolean allowCertificate = showCertificate && btnUseSslClient.getSelection();
                if ( allowCertificate )
                {
                    if ( txtClientCertificate.getText().trim().length() > 0 )
                    {
                        certificatePath = new File( txtClientCertificate.getText().trim() );
                    }
                    passphrase = txtPassphrase.getText();
                }

                IAuthRealm realm = AuthFacade.getAuthRegistry().getRealm( realmId );
                if ( realm != null )
                {
                    IAuthData authData = realm.getAuthData();
                    if ( authData.allowsUsernameAndPassword() )
                    {
                        authData.setUsernameAndPassword( userName, password );
                    }
                    if ( authData.allowsCertificate() )
                    {
                        authData.setSSLCertificate( certificatePath, passphrase );
                    }
                    realm.setAuthData( authData );
                }
                else
                {
                    // realmId is a URL not associated with a realm
                    IAuthData authData = AuthFacade.getAuthService().select( realmId );
                    if ( authData == null )
                    {
                        // we have no way of know authentication type of the implicit security realm
                        // even if the user did not provide username/password or certificate/passphrase
                        // we cannot assume these are not needed until successful validation,
                        // and even then there is no guarantee it won't change in the future.
                        AuthenticationType authType = AuthenticationType.CERTIFICATE_AND_USERNAME_PASSWORD;
                        authData = new AuthData( authType );
                    }
                    if ( authData.allowsUsernameAndPassword() )
                    {
                        authData.setUsernameAndPassword( userName, password );
                    }
                    if ( authData.allowsCertificate() )
                    {
                        authData.setSSLCertificate( certificatePath, passphrase );
                    }
                    AuthFacade.getAuthService().save( realmId, authData );
                }

                getContainer().run( true, true, new IRunnableWithProgress()
                {
                    public void run( IProgressMonitor monitor )
                        throws InvocationTargetException, InterruptedException
                    {
                        try
                        {
                            validationStatus =
                                ValidationFacade.getInstance().validateAccess( project, realmId, monitor );
                        }
                        catch ( CoreException e )
                        {
                            validationStatus = e.getStatus();
                        }
                    }
                } );

                checkValidationStatus();
            }
            catch ( InvocationTargetException ex )
            {
                log.error( Messages.materializationWizard_realmsPage_errors_couldNotValidate, ex );
                // setPageComplete( false );
                setErrorMessage( ex.getMessage() );
            }
            catch ( InterruptedException ex )
            {
                log.error( Messages.materializationWizard_realmsPage_errors_canceled, ex );
                setPageComplete( false );
                // setErrorMessage( ex.getMessage() );
            }
        }

        refreshViewer();

        return credentialsValidated;
    }

    private void refreshViewer()
    {
        tableViewer.refresh();
    }

    private void checkValidationStatus()
    {
        statuses.clear();
        extractStatuses( validationStatus );

        if ( validationStatus.getSeverity() != IStatus.ERROR )
        {
            credentialsValidated = validationStatus.isOK();
            setErrorMessage( null );
        }
        else
        {
            // setPageComplete( false );
            setErrorMessage( Messages.materializationWizard_realmsPage_errors_problemsDetected );
        }

        refreshViewer();
    }

    private void extractStatuses( IStatus status )
    {
        if ( status.isMultiStatus() )
        {
            for ( IStatus child : status.getChildren() )
            {
                if ( child.getException() != null )
                {
                    log.info( "Access Validation failed:", child.getException() ); //$NON-NLS-1$
                    StatusManager.getManager().handle( status, StatusManager.LOG );
                }
                if ( child instanceof AccessValidationStatus )
                {
                    statuses.add( child );
                }
                else
                {
                    extractStatuses( child );
                }
            }
        }
        else
        {
            statuses.add( status );
        }
    }

    @Override
    public IWizardPage getNextPage()
    {
        if ( validateCredentials() )
        {
            return super.getNextPage();
        }
        else
        {
            return null;
        }
    }

    @Override
    public boolean canFlipToNextPage()
    {
        // changed the default to not invoke this.getNextPage() here
        return isPageComplete() && super.getNextPage() != null;
    }

    @Override
    public void setVisible( boolean visible )
    {
        super.setVisible( visible );
        if ( visible )
        {
            // according to instantiations, Cocoa API provides bad baseline information when the components are not
            // shown,
            // force a relayout here
            container.layout();
            txtUsername.forceFocus();
        }
    }

    private static class Filter
        extends javax.swing.filechooser.FileFilter
    {
        public boolean accept( File f )
        {
            if ( f.isDirectory() )
                return true;
            String name = f.getName().toLowerCase();

            return name.endsWith( ".p12" ) || name.endsWith( ".crt" );
        }

        public String getDescription()
        {
            return Messages.ProjectScmSecurityRealmsPage_fileSelect_filter1;
        }
    }
}