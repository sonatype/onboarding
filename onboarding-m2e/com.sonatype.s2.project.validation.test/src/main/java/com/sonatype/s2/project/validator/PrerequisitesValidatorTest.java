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

import junit.framework.TestCase;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.sonatype.s2.project.model.IS2Project;
import com.sonatype.s2.project.model.descriptor.Prerequisites;
import com.sonatype.s2.project.model.descriptor.Project;
import com.sonatype.s2.project.validation.api.IS2ProjectValidator;
import com.sonatype.s2.project.validation.api.S2ProjectValidationContext;

@SuppressWarnings( "restriction" )
public class PrerequisitesValidatorTest
    extends TestCase
{

    private IS2Project newProject( String requiredMemory )
    {
        Prerequisites prerequisites = new Prerequisites();
        prerequisites.setRequiredMemory( requiredMemory );

        Project project = new Project();
        project.setName( getName() );
        if ( requiredMemory != null )
        {
            project.setPrerequisites( prerequisites );
        }

        return project;
    }

    private IStatus validate( IS2Project s2Project )
        throws Exception
    {
        return validate( s2Project, IS2ProjectValidator.NULL_VALIDATION_CONTEXT );
    }

    private IStatus validate( IS2Project s2Project, S2ProjectValidationContext validationContext )
        throws Exception
    {
        IS2ProjectValidator projectValidator = new PrerequisitesValidator();
        IStatus status = projectValidator.validate( s2Project, new NullProgressMonitor() );
        assertFalse( projectValidator.canRemediate( true ).isOK() );
        assertFalse( projectValidator.canRemediate( false ).isOK() );
        return status;
    }

    public void testMissingSpecification()
        throws Exception
    {
        assertEquals( IStatus.OK, validate( newProject( null ) ).getSeverity() );
        assertEquals( IStatus.OK, validate( newProject( "" ) ).getSeverity() );
    }

    public void testInvalidSpecification()
        throws Exception
    {
        assertEquals( IStatus.WARNING, validate( newProject( "invalid" ) ).getSeverity() );
        assertEquals( IStatus.WARNING, validate( newProject( "17.7m" ) ).getSeverity() );
        assertEquals( IStatus.WARNING, validate( newProject( "17kb" ) ).getSeverity() );
    }

    public void testSpecificationInBytes()
        throws Exception
    {
        assertEquals( IStatus.OK, validate( newProject( "16777216" ) ).getSeverity() );
    }

    public void testSpecificationInKiloBytes()
        throws Exception
    {
        assertEquals( IStatus.OK, validate( newProject( "16384K" ) ).getSeverity() );
        assertEquals( IStatus.OK, validate( newProject( "16384k" ) ).getSeverity() );
    }

    public void testSpecificationInMegaBytes()
        throws Exception
    {
        assertEquals( IStatus.OK, validate( newProject( "16M" ) ).getSeverity() );
        assertEquals( IStatus.OK, validate( newProject( "16m" ) ).getSeverity() );
    }

    public void testUnsatisfiedSpecification()
        throws Exception
    {
        assertEquals( IStatus.ERROR, validate( newProject( "10240M" ) ).getSeverity() );
    }

    public void testBarelySatisfiedSpecification()
        throws Exception
    {
        String mem = Long.toString( Runtime.getRuntime().maxMemory() * 101 / 100 );
        assertEquals( IStatus.OK, validate( newProject( mem ) ).getSeverity() );
    }

    public void testIsApplicable()
        throws Exception
    {
        PrerequisitesValidator validator = new PrerequisitesValidator();
        assertTrue( validator.isApplicable( IS2ProjectValidator.NULL_VALIDATION_CONTEXT ) );

        S2ProjectValidationContext validationContext = new S2ProjectValidationContext();
        validationContext.setProperty( S2ProjectValidationContext.FRESH_INSTALL_PROPNAME, "true" );
        assertFalse( validator.isApplicable( validationContext ) );
    }
}
