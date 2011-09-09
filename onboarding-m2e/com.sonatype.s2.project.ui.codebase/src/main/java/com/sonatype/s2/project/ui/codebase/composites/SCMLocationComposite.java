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

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.m2e.scm.internal.ScmHandlerFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.maven.ide.eclipse.swtvalidation.SwtValidationGroup;
import org.maven.ide.eclipse.ui.common.authentication.IRealmChangeListener;
import org.maven.ide.eclipse.ui.common.authentication.RealmComposite;
import org.maven.ide.eclipse.ui.common.authentication.RealmUrlCollector;
import org.maven.ide.eclipse.ui.common.layout.WidthGroup;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;

import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.IScmLocation;
import com.sonatype.s2.project.model.descriptor.ScmLocation;
import com.sonatype.s2.project.ui.codebase.Messages;
import com.sonatype.s2.project.ui.internal.DownloadJob;
import com.sonatype.s2.project.validator.ScmAccessValidator;

@SuppressWarnings( "restriction" )
abstract public class SCMLocationComposite
    extends SourceTreeComposite
{
    private ScmAccessValidator scmValidator;

    private CCombo scmTypeCombo;

    private Text scmUrlText;

    private RealmComposite scmUrlRealmComposite;

    private Button scmValidateButton;

    public SCMLocationComposite( Composite parent, WidthGroup widthGroup, SwtValidationGroup validationGroup,
                                 FormToolkit toolkit )
    {
        super( parent, widthGroup, validationGroup, toolkit );
        scmValidator = new ScmAccessValidator();

        setLayout( new GridLayout( 3, false ) );

        createLabel( Messages.scmLocationComposite_location_label );

        scmTypeCombo = new CCombo( this, getCComboStyle() | SWT.READ_ONLY );
        if ( isFormMode() )
        {
            scmTypeCombo.setData( FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER );
        }
        scmTypeCombo.setData( "name", "scmTypeCombo" ); //$NON-NLS-1$ //$NON-NLS-2$
        GridData scmTypeData = createInputData();
        scmTypeData.horizontalAlignment = SWT.LEFT;
        scmTypeData.grabExcessHorizontalSpace = false;
        scmTypeData.widthHint = SWT.DEFAULT;
        scmTypeCombo.setLayoutData( scmTypeData );
        scmTypeCombo.setItems( getScmHandlerTypes() );
        scmTypeCombo.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                saveScmLocation();
            }
        } );
        SwtValidationGroup.setComponentName( scmTypeCombo, Messages.scmLocationComposite_location_type );
        addToValidationGroup( scmTypeCombo, StringValidators.REQUIRE_NON_EMPTY_STRING );

        scmUrlText = createText( "scmUrlText", Messages.scmLocationComposite_location_url ); //$NON-NLS-1$
        scmUrlText.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                saveScmLocation();
            }
        } );
        scmUrlText.addVerifyListener( new VerifyListener()
        {
            public void verifyText( VerifyEvent e )
            {
                if ( e.character == 0 )
                {
                    if ( e.text != null && e.text.length() > 0 )
                    {
                        e.text = e.text.replaceAll( "[\n\r]", "" );
                    }
                }
                else
                {
                    e.doit = e.character != '\n' && e.character != '\r';
                }
            }
        } );

        addToValidationGroup( scmUrlText, StringValidators.REQUIRE_NON_EMPTY_STRING );

        Label realmLabel = createLabel( Messages.scmLocationComposite_securityRealm );
        GridData realmLabelData = (GridData) realmLabel.getLayoutData();
        realmLabelData.horizontalIndent = INPUT_INDENT;

        scmUrlRealmComposite = new RealmComposite( this, scmUrlText, getValidationGroup(), getToolkit() )
        {
            @Override
            protected String getUrl()
            {
                return getScmUrl();
            }
        };
        scmUrlRealmComposite.setLayoutData( createInputData( 2, 1 ) );
        scmUrlRealmComposite.addModifyListener( new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                notifyCodebaseChangeListeners();
            }
        } );

        scmValidateButton = createButton( Messages.scmLocationComposite_validate );
        scmValidateButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                validateScmAccess();
            }
        } );
        scmValidateButton.setLayoutData( new GridData( SWT.RIGHT, SWT.TOP, false, false, 3, 1 ) );
        scmValidateButton.setEnabled( false );
    }

    abstract protected void validateScmAccess();

    protected IStatus runValidation( final Shell shell, IProgressMonitor monitor )
    {
        try
        {
            final IScmLocation scmLocation = getModule().getScmLocation();
            final String url = scmLocation.getUrl();
            final String realmId = null; // scmLocation.getSecurityRealmId();

            DownloadJob<IStatus> downloadJob =
                new DownloadJob<IStatus>( Messages.scmLocationComposite_validationError,
                                          Messages.scmLocationComposite_location_label, realmId == null ? url : realmId )
                {
                    @Override
                    public IStatus run( IProgressMonitor monitor )
                        throws CoreException
                    {
                        IStatus validationStatus = scmValidator.validate( scmLocation, monitor );
                        if ( unauthorized( validationStatus ) )
                        {
                            throw new CoreException( validationStatus );
                        }
                        return validationStatus;
                    }
                };
            downloadJob.setCertificateSupportEnabled( false );

            IStatus status = downloadJob.download( monitor );

            if ( status.isOK() )
            {
                shell.getDisplay().asyncExec( new Runnable()
                {
                    public void run()
                    {
                        MessageDialog.openInformation( shell, Messages.scmLocationComposite_validate,
                                                       Messages.scmLocationComposite_validationSuccessful );
                    }
                } );
            }

            return status;
        }
        catch ( CoreException e )
        {
            return e.getStatus();
        }
    }

    private String[] getScmHandlerTypes()
    {
        ArrayList<String> types = new ArrayList<String>();

        for ( String type : ScmHandlerFactory.getTypes() )
        {
            if ( scmValidator.isSupportedType( type ) )
            {
                types.add( type );
            }
        }

        return types.toArray( new String[types.size()] );
    }

    private void saveScmLocation()
    {
        String url = getScmUrl();
        if ( url.length() == 0 )
        {
            getModule().setScmLocation( null );
        }
        else
        {
            ScmLocation location = new ScmLocation();
            location.setUrl( url );
            getModule().setScmLocation( location );
        }
        updateValidateButton();
        notifyCodebaseChangeListeners();
    }

    private String getScmUrl()
    {
        String url = scmUrlText.getText();
        return url.length() == 0 ? url : ( "scm:" + scmTypeCombo.getText() + ':' + url ); //$NON-NLS-1$
    }

    private void updateValidateButton()
    {
        String scmType = scmTypeCombo.getText();
        String scmUrl = scmUrlText.getText();
        scmValidateButton.setEnabled( scmType.length() > 0 && scmUrl.length() > 0 );
    }

    @Override
    protected void update( IS2Project project, IS2Module module )
    {
        IScmLocation scmLocation = module.getScmLocation();
        scmTypeCombo.setText( "" ); //$NON-NLS-1$
        scmUrlText.setText( "" ); //$NON-NLS-1$
        if ( scmLocation != null )
        {
            String url = scmLocation.getUrl();
            if ( url.startsWith( "scm:" ) ) //$NON-NLS-1$
            {
                url = url.substring( 4 );
            }
            int n = url.indexOf( ':' );
            if ( n >= 0 )
            {
                scmTypeCombo.setText( url.substring( 0, n ) );
                url = url.substring( n + 1 );
            }
            scmUrlText.setText( url );
        }
        updateValidateButton();
    }

    public void addRealmChangeListener( IRealmChangeListener listener )
    {
        scmUrlRealmComposite.addRealmChangeListener( listener );
    }

    public void saveRealms( RealmUrlCollector realmUrlCollector )
    {
        realmUrlCollector.collect( scmUrlRealmComposite );
    }
}
