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
package com.sonatype.s2.project.validation.cvs.internal;

import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.Policy;
import org.eclipse.team.internal.ccvs.core.client.Request;
import org.eclipse.team.internal.ccvs.core.connection.Connection;

/*
 * Copied from org.eclipse.team.internal.ccvs.core.client.Session
 * 
 * Changed to accept local CVSRepositoryLocation
 */
@SuppressWarnings( "restriction" )
public class Session
    extends org.eclipse.team.internal.ccvs.core.client.Session
{
    private CVSRepositoryLocation location;

    private Connection connection;

    private int compressionLevel = 0;

    private String validRequests = null;

    /**
     * Creates a new CVS session, initially in the CLOSED state.
     * 
     * @param location the CVS repository location used for this session
     * @param localRoot represents the current working directory of the client
     * @param outputToConsole if true, command output is directed to the console
     */
    public Session( ICVSRepositoryLocation location, ICVSFolder localRoot, boolean outputToConsole )
    {
        super( null, localRoot, outputToConsole );
        this.location = (CVSRepositoryLocation) location;
    }

    public void open( IProgressMonitor monitor, boolean writeAccess )
        throws CVSException
    {
        if ( connection != null )
            throw new IllegalStateException();
        monitor = Policy.monitorFor( monitor );
        monitor.beginTask( null, 100 );
        boolean opened = false;

        try
        {
            connection = location.openConnection( Policy.subMonitorFor( monitor, 50 ) );

            // If we're connected to a CVSNT server or we don't know the platform,
            // accept MT. Otherwise don't.
            boolean useMT = !( location.getServerPlatform() == CVSRepositoryLocation.CVS_SERVER );
            if ( !useMT )
            {
                removeResponseHandler( "MT" ); //$NON-NLS-1$
            }

            // tell the server the names of the responses we can handle
            connection.writeLine( "Valid-responses " + makeResponseList() ); //$NON-NLS-1$
            // Flush in order to recieve the valid requests
            connection.flush();

            // ask for the set of valid requests
            IStatus status = VALID_REQUEST.execute( this, Policy.subMonitorFor( monitor, 40 ) );
            if ( !status.isOK() )
            {
                throw new CVSException( status );
            }

            // set the root directory on the server for this connection
            connection.writeLine( "Root " + getRepositoryRoot() ); //$NON-NLS-1$

            // enable compression
            compressionLevel = CVSProviderPlugin.getPlugin().getCompressionLevel();
            if ( compressionLevel != 0 && isValidRequest( "gzip-file-contents" ) ) { //$NON-NLS-1$
                // Enable the use of CVS 1.8 per-file compression mechanism.
                // The newer Gzip-stream request seems to be problematic due to Java's
                // GZIPInputStream tendency to block on read() rather than to return a
                // partially filled buffer. The latter option would be better since it
                // can make more effective use of the code dictionary, if it can be made
                // to work...
                connection.writeLine( "gzip-file-contents " + Integer.toString( compressionLevel ) ); //$NON-NLS-1$
            }
            else
            {
                compressionLevel = 0;
            }

            opened = true;
        }
        finally
        {
            if ( connection != null && !opened )
            {
                close();
            }
            monitor.done();
        }
    }

    /**
     * Closes a connection to the server.
     * 
     * @throws IllegalStateException if the Session is not in the OPEN state
     */
    public void close()
    {
        if ( connection != null )
        {
            connection.close();
            connection = null;
            validRequests = null;
        }
    }

    /**
     * Determines if the server supports the specified request.
     * 
     * @param request the request string to verify
     * @return true iff the request is supported
     */
    public boolean isValidRequest( String request )
    {
        return ( validRequests == null ) || ( validRequests.indexOf( " " + request + " " ) != -1 ); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Returns the repository root folder for this session.
     * <p>
     * Specifies the unqualified path to the CVS repository root folder on the server.
     * </p>
     * 
     * @return the repository root folder
     */
    public String getRepositoryRoot()
    {
        return location.getRootDirectory();
    }

    /**
     * Returns an object representing the CVS repository location for this session.
     * 
     * @return the CVS repository location
     */
    public ICVSRepositoryLocation getCVSRepositoryLocation()
    {
        return location;
    }

    public boolean isCVSNT()
    {
        if ( location.getServerPlatform() == CVSRepositoryLocation.UNDETERMINED_PLATFORM )
        {
            return location.getRootDirectory().indexOf( ':' ) == 1;
        }
        else
        {
            return location.getServerPlatform() == CVSRepositoryLocation.CVSNT_SERVER;
        }
    }

    /*
     * Makes a list of all valid responses; for initializing a session.
     * @return a space-delimited list of all valid response strings
     */
    private String makeResponseList()
    {
        StringBuffer result = new StringBuffer( "ok error M E" ); //$NON-NLS-1$
        Iterator elements = getReponseHandlers().keySet().iterator();
        while ( elements.hasNext() )
        {
            result.append( ' ' );
            result.append( (String) elements.next() );
        }

        return result.toString();
    }

    /**
     * Receives a line of text minus the newline from the server.
     * 
     * @return the line of text
     */
    public String readLine()
        throws CVSException
    {
        return connection.readLine();
    }

    /**
     * Sends a request to the server and flushes any output buffers.
     * 
     * @param requestId the string associated with the request to be executed
     */
    public void sendRequest( String requestId )
        throws CVSException
    {
        connection.writeLine( requestId );
        connection.flush();
    }

    /**
     * Sends a global options to the server.
     * <p>
     * e.g. sendGlobalOption("-n") sends:
     * 
     * <pre>
     *   Global_option -n \n
     * </pre>
     * 
     * </p>
     * 
     * @param option the global option to send
     */
    public void sendGlobalOption( String option )
        throws CVSException
    {
        connection.writeLine( "Global_option " + option ); //$NON-NLS-1$
    }

    /**
     * Sends an argument to the server.
     * <p>
     * e.g. sendArgument("Hello\nWorld\n  Hello World") sends:
     * 
     * <pre>
     *   Argument Hello \n
     *   Argumentx World \n
     *   Argumentx Hello World \n
     * </pre>
     * 
     * </p>
     * 
     * @param arg the argument to send
     */
    public void sendArgument( String arg )
        throws CVSException
    {
        connection.write( "Argument " ); //$NON-NLS-1$
        int oldPos = 0;
        for ( ;; )
        {
            int pos = arg.indexOf( '\n', oldPos );
            if ( pos == -1 )
                break;
            connection.writeLine( stripTrainingCR( arg.substring( oldPos, pos ) ) );
            connection.write( "Argumentx " ); //$NON-NLS-1$
            oldPos = pos + 1;
        }
        connection.writeLine( stripTrainingCR( arg.substring( oldPos ) ) );
    }

    /**
     * Sends a Directory request to the server.
     * <p>
     * e.g. sendDirectory("local_dir", "remote_dir") sends:
     * 
     * <pre>
     *   Directory local_dir
     *   repository_root/remote_dir
     * </pre>
     * 
     * </p>
     * 
     * @param localDir the path of the local directory relative to localRoot
     * @param remoteDir the path of the remote directory relative to repositoryRoot
     */
    public void sendDirectory( String localDir, String remoteDir )
        throws CVSException
    {
        if ( localDir.length() == 0 )
            localDir = CURRENT_LOCAL_FOLDER;
        connection.writeLine( "Directory " + localDir ); //$NON-NLS-1$
        connection.writeLine( remoteDir );
    }

    /*
     * Remove any trailing CR from the string
     */
    private String stripTrainingCR( String string )
    {
        if ( string.endsWith( "\r" ) ) { //$NON-NLS-1$
            return string.substring( 0, string.length() - 1 );
        }
        return string;
    }

    private static final ValidRequests VALID_REQUEST = new ValidRequests();

    private static class ValidRequests
        extends Request
    {

        protected String getRequestId()
        {
            return "valid-requests"; //$NON-NLS-1$
        }

        public IStatus execute( Session session, IProgressMonitor monitor )
            throws CVSException
        {
            return executeRequest( session, CommandOutputListener.INSTANCE, monitor );
        }
    }
}
