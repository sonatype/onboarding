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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;

import javax.swing.JFileChooser;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.io.S2IOFacade;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.swtvalidation.SwtValidationUI;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ui.GroupValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.spice.interactive.interpolation.Interpolator;
import org.sonatype.spice.interactive.interpolation.Variable;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.ui.internal.Activator;
import com.sonatype.s2.project.ui.internal.Messages;

public class UserSettingsPage
    extends WizardPage
{
    private Logger log = LoggerFactory.getLogger( UserSettingsPage.class );

    private IS2Project project;

    private Collection<Variable> variables;
    
    private Interpolator interpolator;

    SwtValidationGroup group;
    
    private boolean dirty;

    private ScrolledComposite compScrolled;

    private Composite compRoot;

    private SwtValidationGroup completeGroup;

    public UserSettingsPage( IS2Project project )
    {
        super( "userSettingsPage" );
        this.project = project;

        setTitle( Messages.userSettingsPage_title );
        setDescription( Messages.userSettingsPage_message );
        setPageComplete( false );
    }

    public void createControl( Composite parent )
    {
        compScrolled = new ScrolledComposite( parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        compScrolled.setExpandHorizontal( true );
        compScrolled.setExpandVertical( true );
        
        compRoot = new Composite( compScrolled, SWT.NONE);
        compScrolled.setContent( compRoot );
        compRoot.setLayout(new GridLayout(3, false));
        

        setControl( compScrolled );
        if (variables != null) {
        	createVariableComponents(variables);
        }
    }

    private void setVariables( Collection<Variable> variables )
    {
    	assert variables != null;
        this.variables = variables;
        if ( compRoot != null )
        {
        	createVariableComponents(variables);
        }
    }
    
    private void createVariableComponents(final Collection<Variable> vars) {
    	assert vars != null;
    	assert Display.getCurrent() != null;
    	assert compRoot != null;
    	for (Control c : compRoot.getChildren()) {
    		c.dispose();
    	}
    	//recreate a group to throw away the previous validations..
        group = SwtValidationGroup.create(SwtValidationUI.createUI(this, SwtValidationUI.MESSAGE));
        
        completeGroup = SwtValidationGroup.create(new GroupValidator(false) {
			@Override
			protected void performGroupValidation(Problems problems) {
				for (Variable v : vars) {
					if (v.getValue() == null || v.getValue().length() == 0) {
						problems.add("All variables have to be set.", Severity.FATAL);
						break;
					}
				}
			}
		}, SwtValidationUI.createUI(this, SwtValidationUI.BUTTON));
        completeGroup.addItem(group, false);
        
        Text focused = null;
    	for (final Variable var : vars) {
            Label lbl = new Label(compRoot, SWT.NONE);
            GridData gd_lbl = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
            gd_lbl.horizontalIndent = 12;
            gd_lbl.verticalIndent = 12;
            lbl.setLayoutData(gd_lbl);
            lbl.setText(var.getName());
            boolean isPassword = Variable.PASSWORD.equals(var.getType());
            boolean isFile = Variable.FILE.equals(var.getType());
            int style = isPassword ? SWT.SINGLE | SWT.BORDER | SWT.PASSWORD : SWT.SINGLE | SWT.BORDER;
            final Text txt = new Text(compRoot, style );
            if (focused == null) {
            	focused = txt;
            }
            GridData gd_txt = new GridData(SWT.FILL, SWT.TOP, true, false, isFile ? 1 : 2, 1);
            gd_txt.horizontalIndent = 12;
            gd_txt.verticalIndent = 12;
            txt.setLayoutData(gd_txt);
            if (var.getValue() != null) {
            	txt.setText(var.getValue());
            } 
            else if (var.getDefaultValue() != null) {
            	txt.setText(var.getDefaultValue());
            }
            
            txt.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					var.setValue(txt.getText());
					dirty = true;
				}
			});
            
            if (isFile) {
            	Button btn = new Button(compRoot, SWT.PUSH);
                GridData gd_btn = new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1);
                gd_btn.horizontalIndent = 12;
                gd_btn.verticalIndent = 12;
                btn.setLayoutData(gd_btn);
                btn.setText("Browse...");
                btn.addSelectionListener( new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected( SelectionEvent event )
                    {
                    	//copy pasted from EclipseInstallationPage so should be ok..
                        String path = null;
                        if ( System.getProperty( "osgi.os" ).contains( "linux" ) )
                        {
                            JFileChooser chooser = new JFileChooser();
                            chooser.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );

                            if ( JFileChooser.APPROVE_OPTION == chooser.showOpenDialog( SWT_AWT.getFrame( getShell() ) ) )
                                path = chooser.getSelectedFile().toString();
                        }
                        else
                        {
                            DirectoryDialog dialog = new DirectoryDialog( getShell() );
                            path = dialog.open();
                        }
                        if ( path != null )
                        {
                            txt.setText( path );
                        }
                    }
                } );
            }
            if (var.getDescription() != null) {
            	group.add(txt, new Validator<String>() {
					public Class<String> modelType() {
						return String.class;
					}
					public void validate(Problems problems, String arg1, String arg2) {
						problems.add(var.getDescription(), Severity.INFO);
					}
            	});
            }
            
    	}
    	completeGroup.performValidation();
    	if (focused != null) {
    		focused.forceFocus();
    	}
        compScrolled.setMinSize( compRoot.computeSize( SWT.DEFAULT, SWT.DEFAULT ));
        compRoot.layout();
    }
    
    //MECLIPSE-1760 warning to future generations, see what can happen when you skip this methdo override
    @Override
    public boolean canFlipToNextPage()
    {
        // changed the default to not invoke getNextPage() here
        return isPageComplete();
    }    

    public boolean loadVariables()
    {
        if ( project != null && interpolator == null )
        {
            final Exception[] exception = new Exception[] { null };
            try
            {
                getContainer().run( true, true, new IRunnableWithProgress()
                {
                    public void run( IProgressMonitor monitor )
                        throws InvocationTargetException, InterruptedException
                    {
                        monitor.setTaskName( Messages.userSettingsPage_jobs_loadingSettings );
                        try
                        {
                            File settingsFile = downloadMavenSettings( monitor );
                            File userFile = getMavenSettingsDefaultValuesFile();
                            interpolator = new Interpolator( settingsFile, userFile );
                            settingsFile.delete();
                            Display.getDefault().asyncExec( new Runnable()
                            {
                                public void run()
                                {
                                    setVariables( interpolator.getVariables() );
                                }
                            } );
                        }
                        catch ( CoreException e )
                        {
                            exception[0] = e;
                        }
                    }
                } );
            }
            catch ( InvocationTargetException e )
            {
                exception[0] = e;
            }
            catch ( InterruptedException e )
            {
                exception[0] = e;
            }
            if ( exception[0] != null )
            {
                String message = exception[0].getMessage();
                setErrorMessage( message );
                setPageComplete( false );
                log.error( message, exception[0] );
            }
        }

        return variables != null && !variables.isEmpty();
    }

    @Override
    public void setVisible( boolean visible )
    {
        super.setVisible( visible );
        if ( visible )
        {
            loadVariables();
        }
        else if ( interpolator != null && dirty )
        {
            interpolator.saveUserValues();
            dirty = false;
        }
    }

    private File getMavenSettingsDefaultValuesFile()
        throws CoreException
    {
        String userHome = System.getProperty( "user.home" );
        if ( userHome == null || userHome.trim().length() == 0 )
        {
            throw new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                                                 Messages.userSettingsPage_errors_noUserHome ) );
        }

        return new File( userHome, ".m2/settings-defaults.properties" );
    }

    public File downloadMavenSettings( IProgressMonitor monitor )
        throws CoreException
    {
        String url = project.getMavenSettingsLocation().getUrl();

        try
        {
            File settingsFile =
                File.createTempFile( "settings", ".xml", Activator.getDefault().getStateLocation().toFile() );

            log.debug( "Downloading Maven settings from {} to {}", url, settingsFile );

            OutputStream os = new BufferedOutputStream( new FileOutputStream( settingsFile ) );
            try
            {
                InputStream is = S2IOFacade.openStream( url, monitor );
                try
                {
                    byte[] buffer = new byte[0x1000];
                    for ( int n = 0; ( n = is.read( buffer ) ) > -1; os.write( buffer, 0, n ) )
                        ;
                }
                finally
                {
                    is.close();
                }
            }
            finally
            {
                os.close();
            }

            return settingsFile;
        }
        catch ( URISyntaxException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                                                 Messages.userSettingsPage_errors_errorLoadingMavenSettings, e ) );
        }
        catch ( IOException e )
        {
            throw new CoreException( new Status( IStatus.ERROR, Activator.PLUGIN_ID,
                                                 Messages.userSettingsPage_errors_errorLoadingMavenSettings, e ) );
        }
    }
}
