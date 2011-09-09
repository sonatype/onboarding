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
package com.sonatype.s2.project.materializer;

import java.io.File;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.ide.ChooseWorkspaceDialog;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.osgi.service.prefs.Preferences;

import com.sonatype.s2.nexus.NexusFacade;

/*
 * Copy&Paste from org.eclipse.ui.internal.ide.application.IDEApplication
 */
@SuppressWarnings( "restriction" )
public class S2MaterializerApplication
    implements IApplication, IExecutableExtension
{
    private static final String ARG_DESCRIPTOR_URL = "-materializer.descriptorURL"; //$NON-NLS-1$

    private static final String ARG_NEXUS_BASE_URL = NexusFacade.ARG_NEXUS_BASE_URL;

    private static final String PROP_EXIT_CODE = "eclipse.exitcode"; //$NON-NLS-1$

    private static final String PROP_VM = "eclipse.vm"; //$NON-NLS-1$

    private static final String PROP_VMARGS = "eclipse.vmargs"; //$NON-NLS-1$

    private static final String PROP_COMMANDS = "eclipse.commands"; //$NON-NLS-1$

    public static final String PROP_EXIT_DATA = "eclipse.exitdata"; //$NON-NLS-1$

    private static final String CMD_VMARGS = "-vmargs"; //$NON-NLS-1$

    private static final String CMD_APPLICATION = "-application"; //$NON-NLS-1$

    private static final String NEW_LINE = "\n"; //$NON-NLS-1$

    private static final String PROP_LAUNCHER = "eclipse.launcher"; //$NON-NLS-1$

    // This must match org.eclipse.ui.internal.ide.ChooseWorkspaceData.PERS_ENCODING_VERSION_CONFIG_PREFS_NO_COMMAS
    private static final int PERS_ENCODING_VERSION_CONFIG_PREFS_NO_COMMAS = 3;

    /*
     * (non-Javadoc)
     * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext context)
     */
    public Object start( IApplicationContext context )
        throws Exception
    {
        System.out.println( "S2MaterializerApplication.start" );
        Display display = PlatformUI.createDisplay();

        try
        {
            // look and see if there's a splash shell we can parent off of
            Shell shell = WorkbenchPlugin.getSplashShell( display );
            if ( shell != null )
            {
                // should should set the icon and message for this shell to be the
                // same as the chooser dialog - this will be the guy that lives in
                // the task bar and without these calls you'd have the default icon
                // with no message.
                shell.setText( ChooseWorkspaceDialog.getWindowTitle() );
                shell.setImages( Dialog.getDefaultImages() );
            }
            //
            // if ( !checkInstanceLocation( shell ) )
            // {
            // WorkbenchPlugin.unsetSplashShell( display );
            // Platform.endSplash();
            // return EXIT_OK;
            // }

            String[] args = (String[]) context.getArguments().get( IApplicationContext.APPLICATION_ARGS );
            String s2ProjectURL = null;
            String nexusBaseURL = null;
            for ( int i = 0; i < args.length - 1; i++ )
            {
                if ( ARG_DESCRIPTOR_URL.equals( args[i] ) )
                {
                    s2ProjectURL = args[i + 1];
                }

                if ( ARG_NEXUS_BASE_URL.equals( args[i] ) )
                {
                    nexusBaseURL = args[i + 1];
                }
            }

            if ( s2ProjectURL == null || nexusBaseURL == null )
            {
                System.err.println( "Syntax: " + ARG_DESCRIPTOR_URL + " <descriptor url> " + ARG_NEXUS_BASE_URL
                    + " <nexus server url>" );
                return EXIT_OK;
            }

            NexusFacade.setMainNexusServerData( nexusBaseURL, null /* username */, null /* password */,
                                                new NullProgressMonitor() );

            // Dump some useful debug info :)
            System.out.println( PROP_LAUNCHER + "=" + System.getProperty( PROP_LAUNCHER ) );
            System.out.println( PROP_VM + "=" + System.getProperty( PROP_VM ) );
            System.out.println( PROP_VMARGS + "=" + System.getProperty( PROP_VMARGS ) );
            System.out.println( PROP_COMMANDS + "=" + System.getProperty( PROP_COMMANDS ) );

            // Save the current workspace location in the eclipse preferences
            Location instanceLoc = Platform.getInstanceLocation();
            File workspaceFile = new File( instanceLoc.getURL().getFile() );
            Preferences node = new ConfigurationScope().getNode( IDEWorkbenchPlugin.IDE_WORKBENCH );
            node.put( IDE.Preferences.RECENT_WORKSPACES, workspaceFile.getCanonicalPath() );
            node.putInt( IDE.Preferences.RECENT_WORKSPACES_PROTOCOL, PERS_ENCODING_VERSION_CONFIG_PREFS_NO_COMMAS );
            node.flush();

            // create the workbench with this advisor and run it until it exits
            // N.B. createWorkbench remembers the advisor, and also registers
            // the workbench globally so that all UI plug-ins can find it using
            // PlatformUI.getWorkbench() or AbstractUIPlugin.getWorkbench()
            int returnCode =
                PlatformUI.createAndRunWorkbench( display, new S2MaterializerWorkbenchAdvisor( s2ProjectURL ) );

            // the workbench doesn't support relaunch yet (bug 61809) so
            // for now restart is used, and exit data properties are checked
            // here to substitute in the relaunch return code if needed
            if ( returnCode != PlatformUI.RETURN_RESTART )
            {
                return EXIT_OK;
            }

            // if the exit code property has been set to the relaunch code, then
            // return that code now, otherwise this is a normal restart
            String command_line = buildCommandLine();
            System.out.println( "New command line=" + command_line );
            System.setProperty( PROP_EXIT_DATA, command_line );
            System.out.println( PROP_EXIT_DATA + "=" + System.getProperty( PROP_EXIT_DATA ) );
            System.setProperty( PROP_EXIT_CODE, "" + Integer.toString( EXIT_RELAUNCH ) );
            System.out.println( PROP_EXIT_CODE + "=" + System.getProperty( PROP_EXIT_CODE ) );

            return EXIT_RELAUNCH;
        }
        finally
        {
            if ( display != null )
            {
                display.dispose();
            }
            Location instanceLoc = Platform.getInstanceLocation();
            if ( instanceLoc != null )
                instanceLoc.release();
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.equinox.app.IApplication#stop()
     */
    public void stop()
    {
        final IWorkbench workbench = PlatformUI.getWorkbench();
        if ( workbench == null )
            return;
        final Display display = workbench.getDisplay();
        display.syncExec( new Runnable()
        {
            public void run()
            {
                if ( !display.isDisposed() )
                    workbench.close();
            }
        } );
    }

    /*
     * (non-Javadoc)
     * @see
     * org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement
     * , java.lang.String, java.lang.Object)
     */
    public void setInitializationData( IConfigurationElement config, String propertyName, Object data )
    {
        // Nothing to do
    }

    /*
     * Based on the code in org.eclipse.ui.internal.ide.actions.OpenWorkspaceAction.buildCommandLine()
     */
    private String buildCommandLine()
    {
        String exitData = System.getProperty( PROP_EXIT_DATA );
        if ( exitData != null && exitData.trim().length() > 0 )
        {
            // PROP_EXIT_DATA was set - porbably we are here after the user chose to switch workspaces
            // PROP_EXIT_DATA already contains the command line for the eclipse restart, we only need to remove the
            // materializer specific args.
            exitData = removeArgFromCommandLine( exitData, CMD_APPLICATION );
            exitData = removeArgFromCommandLine( exitData, ARG_DESCRIPTOR_URL );
            exitData = removeArgFromCommandLine( exitData, ARG_NEXUS_BASE_URL );
            return exitData;
        }

        String property = System.getProperty( PROP_VM );
        // if ( property == null )
        // {
        // // MessageDialog.openError( window.getShell(), IDEWorkbenchMessages.OpenWorkspaceAction_errorTitle,
        // // NLS.bind( IDEWorkbenchMessages.OpenWorkspaceAction_errorMessage, PROP_VM ) );
        // return null;
        // }

        StringBuffer result = new StringBuffer( 512 );
        if ( property != null )
        {
            result.append( property );
            result.append( NEW_LINE );
        }

        // append the vmargs and commands. Assume that these already end in \n
        String vmargs = System.getProperty( PROP_VMARGS );
        if ( vmargs != null )
        {
            result.append( vmargs );
        }

        // append the rest of the args, replacing or adding -data as required
        property = System.getProperty( PROP_COMMANDS );
        if ( property != null )
        {
            property = removeArgFromCommandLine( property, CMD_APPLICATION );
            property = removeArgFromCommandLine( property, ARG_DESCRIPTOR_URL );
            result.append( property );
        }

        // put the vmargs back at the very end (the eclipse.commands property
        // already contains the -vm arg)
        if ( vmargs != null )
        {
            result.append( CMD_VMARGS );
            result.append( NEW_LINE );
            result.append( vmargs );
        }

        return result.toString();
    }

    private String removeArgFromCommandLine( String commandLine, String arg )
    {
        int argAt = commandLine.indexOf( arg );
        if ( argAt < 0 )
        {
            return commandLine;
        }

        int newLineAt = commandLine.indexOf( NEW_LINE, argAt );
        if ( newLineAt < 0 )
        {
            return commandLine.substring( 0, argAt );
        }
        newLineAt = commandLine.indexOf( NEW_LINE, newLineAt + 1 );
        if ( newLineAt < 0 )
        {
            return commandLine.substring( 0, argAt );
        }
        return commandLine.substring( 0, argAt ) + commandLine.substring( newLineAt );
    }
}
