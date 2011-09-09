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

import java.util.Set;

import junit.framework.TestCase;

import com.sonatype.s2.project.validation.api.IS2AccessValidator;
import com.sonatype.s2.project.validation.api.IS2ProjectValidator;

public class ValidationFacadeTest
    extends TestCase
{

    public void testGetAccessValidators()
        throws Exception
    {
        Set<IS2AccessValidator> validators = ValidationFacade.getInstance().getAccessValidators();
        assertTrue( validators.size() > 0 );
    }

    public void testGetAllValidators()
        throws Exception
    {
        Set<IS2ProjectValidator> validators = ValidationFacade.getInstance().getAllValidators();
        assertTrue( validators.size() > 0 );

        int accessValidators = 0;
        int notAccessValidators = 0;
        for ( IS2ProjectValidator validator : validators )
        {
            if ( IS2ProjectValidator.CATEGORY_ACCESS_VALIDATION.equals( validator.getCategory() ) )
            {
                accessValidators++;
            }
            else
            {
                notAccessValidators++;
            }
        }

        assertTrue( accessValidators > 0 );
        assertTrue( notAccessValidators > 0 );
    }

}
