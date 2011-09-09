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
package com.sonatype.s2.project.core.test;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.authentication.IAuthRealm;
import org.maven.ide.eclipse.authentication.ISecurityRealmPersistence;
import org.maven.ide.eclipse.authentication.ISecurityRealmURLAssoc;
import org.maven.ide.eclipse.authentication.SecurityRealmPersistenceException;
import org.maven.ide.eclipse.authentication.SecurityRealmURLAssoc;

public class DummySecurityRealmPersistence
    implements ISecurityRealmPersistence
{
    private Random rand = new Random( System.currentTimeMillis() );
    private String generateId()
    {
        return Long.toHexString( System.nanoTime() + rand.nextInt( 2008 ) );
    }

    public void addRealm( IAuthRealm realm, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
    }

    public ISecurityRealmURLAssoc addURLToRealmAssoc( ISecurityRealmURLAssoc securityRealmURLAssoc,
                                                      IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        return new SecurityRealmURLAssoc( generateId(), securityRealmURLAssoc.getUrl(),
                                          securityRealmURLAssoc.getRealmId(),
                                   securityRealmURLAssoc.getAnonymousAccess() );
    }

    public void deleteRealm( String realmId, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
    }

    public void deleteURLToRealmAssoc( String urlAssocId, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
    }

    public int getPriority()
    {
        return 0;
    }

    public Set<IAuthRealm> getRealms( IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        if ( ex != null )
            throw new SecurityRealmPersistenceException( ex );
        return new LinkedHashSet<IAuthRealm>();
    }

    public Set<ISecurityRealmURLAssoc> getURLToRealmAssocs( IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
        return new LinkedHashSet<ISecurityRealmURLAssoc>();
    }

    public boolean isActive()
    {
        return false;
    }

    public void setActive( boolean active )
    {
    }

    public void updateRealm( IAuthRealm realm, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
    }

    public void updateURLToRealmAssoc( ISecurityRealmURLAssoc securityRealmURLAssoc, IProgressMonitor monitor )
        throws SecurityRealmPersistenceException
    {
    }

    public static void setException( Exception e )
    {
        ex = e;
    }

    private static Exception ex;
}
