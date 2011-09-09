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
package com.sonatype.s2.project.validator;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.scm.internal.ScmHandlerFactory;
import org.maven.ide.eclipse.authentication.AnonymousAccessType;
import org.maven.ide.eclipse.authentication.AuthFacade;

import com.sonatype.s2.project.core.S2ProjectCore;
import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.S2ProjectFacade;
import com.sonatype.s2.project.model.descriptor.S2Module;
import com.sonatype.s2.project.validation.api.IScmAccessData;
import com.sonatype.s2.project.validation.api.ScmAccessData;
import com.sonatype.s2.project.validation.git.GitAccessValidator;
import com.sonatype.s2.project.validation.git.GitUtil;

public class ScmAccessValidatorTest
    extends AbstractValidatorTest
{
    private IProgressMonitor monitor = new NullProgressMonitor();

    private void assertErrorStatus( IStatus status, String messagePrefix )
    {
        assertEquals( getMessage( status ), status.getSeverity(), IStatus.ERROR );
        assertTrue( getMessage( status ), status instanceof MultiStatus );
        assertEquals( getMessage( status ), 1, status.getChildren().length );
        assertTrue( getMessage( status ), status.getChildren()[0].getMessage().startsWith( messagePrefix ) );
    }

    private String getMessage( IStatus status )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( status.getMessage() );
        if ( status.isMultiStatus() )
        {
            IStatus[] children = status.getChildren();
            for ( int i = 0; i < children.length; i++ )
            {
                sb.append( '\n' ).append( i ).append( ": " ).append( children[i].getMessage() );
            }
        }
        return sb.toString();
    }

    public void testKnownProviderWithoutRealms()
        throws Exception
    {
        IStatus status =
            validateAccess( "resources/scmaccessvalidator/known-scm-provider-without-realms.xml", null /* securityRealmIdFilter */);
        assertTrue( status.toString(), status.isOK() );

        status =
            validateAccess( "resources/scmaccessvalidator/known-scm-provider-without-realms.xml",
                            "scm:testfile:resources/projects/dummy" );
        assertTrue( status.toString(), status.isOK() );

        status =
            validateAccess( "resources/scmaccessvalidator/known-scm-provider-without-realms.xml",
                            "security.realm.doesNotExist" );
        assertNull( status );
    }

    public void testKnownProviderWithoutRealmsWithSlash()
        throws Exception
    {
        IStatus status =
            validateAccess( "resources/scmaccessvalidator/known-scm-provider-without-realms-with-slash.xml", null /* securityRealmIdFilter */);
        assertTrue( status.toString(), status.isOK() );

        status =
            validateAccess( "resources/scmaccessvalidator/known-scm-provider-without-realms-with-slash.xml",
                            "scm:testfile:resources/projects/dummy" );
        assertTrue( status.toString(), status.isOK() );

        status =
            validateAccess( "resources/scmaccessvalidator/known-scm-provider-without-realms-with-slash.xml",
                            "security.realm.doesNotExist" );
        assertNull( status );
    }

    public void testUnknownProvider()
        throws Exception
    {
        IStatus status =
            validateAccess( "resources/scmaccessvalidator/unknownscmprovider.xml", null /* securityRealmIdFilter */);
        assertErrorStatus( status, "SCM provider is not available for " );
    }

    public void testFailOnAnyInvalidModule()
        throws Exception
    {
        IStatus status =
            validateAccess( "resources/scmaccessvalidator/fail-on-any-invalid-scm-module.xml", null /* securityRealmIdFilter */);
        assertFalse( status.isOK() );
    }

    private IStatus validateAccess( String descriptor, String securityRealmIdFilter )
        throws Exception
    {
        S2ProjectCore core = S2ProjectCore.getInstance();
        File file = new File( descriptor );
        IS2Project s2Project = core.loadProject( file.toURI().toURL().toExternalForm(), new NullProgressMonitor() );

        ScmAccessValidator projectValidator = new ScmAccessValidator();
        IStatus status = projectValidator.validate( s2Project, securityRealmIdFilter, new NullProgressMonitor() );
        assertFalse( projectValidator.canRemediate( true ).isOK() );
        assertFalse( projectValidator.canRemediate( false ).isOK() );
        return status;
    }

    public void testKnownTypes()
        throws Exception
    {
        ScmAccessValidator validator = new ScmAccessValidator();

        // git and svn are supported as of mse 1.0.0

        String GIT = "git";
        assertNotNull( ScmHandlerFactory.getHandlerByType( GIT ) );
        assertTrue( validator.isSupportedType( GIT ) );

        String SVN = "svn";
        assertNotNull( ScmHandlerFactory.getHandlerByType( SVN ) );
        assertTrue( validator.isSupportedType( SVN ) );
    }

    public void testUnknownType()
        throws Exception
    {
        ScmAccessValidator validator = new ScmAccessValidator();

        String VSS = "vss";
        // assertNotNull( ScmHandlerFactory.getHandlerByType( VSS ) );
        assertFalse( validator.isSupportedType( VSS ) );

        assertFalse( validator.isSupportedType( "random string" ) );

    }

    public void testNoScmRoot()
    {
        ScmAccessValidator validator = new ScmAccessValidator();

        IS2Project project = S2ProjectFacade.createProject( "a", "b", "c" );
        S2Module module = new S2Module();
        module.setName( "d" );
        project.addModule( module );

        assertTrue( validator.validate( project, monitor ).getSeverity() == IStatus.ERROR );
    }

    public void testGitFileRepository()
        throws Exception
    {
        GitAccessValidator validator = new GitAccessValidator();

        File repoDir = new File( "target/fake-git-repo" ).getCanonicalFile();
        if ( !repoDir.exists() )
        {
            assertTrue( repoDir.mkdirs() );
        }

        String repoPath = repoDir.getPath();

        // @TODO decide if file:///C:/bar is acceptable on windows
        if ( !repoPath.startsWith( "/" ) )
        {
            repoPath = "/" + repoPath;
        }

        String repoUrl = "file://" + repoPath;

        IScmAccessData data = new ScmAccessData( GitUtil.SCM_GIT_PREFIX + repoUrl, null, null, null );
        IStatus status = validator.validate( data, monitor );
        assertTrue( status.isOK() );
    }

    public void testAnonymousNotAllowed()
        throws Exception
    {
        String scmUrl = "file://anonymous-not-allowed-module";
        
        // Anonymous allowed
        addRealmAndURL( "testAnonymousNotAllowed", scmUrl, AnonymousAccessType.ALLOWED, "" /* username */, "" /* password */);
        IStatus status =
            validateAccess( "resources/scmaccessvalidator/anonymous-not-allowed.xml", null /* securityRealmIdFilter */);
        assertTrue( status.isOK() );

        // Anonymous not allowed
        AuthFacade.getAuthRegistry().clear();
        addRealmAndURL( "testAnonymousNotAllowed", scmUrl, AnonymousAccessType.NOT_ALLOWED, "" /* username */, "" /* password */);
        status =
            validateAccess( "resources/scmaccessvalidator/anonymous-not-allowed.xml", null /* securityRealmIdFilter */);
        assertErrorStatus( status, "Anonymous access is not allowed" );
    }
}
