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
package com.sonatype.nexus.onboarding.its.meclipse1375CodebasesRestApi;

import org.restlet.data.MediaType;
import org.restlet.data.Response;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sonatype.nexus.onboarding.its.AbstractOnboardingIT;
import com.sonatype.nexus.onboarding.its.util.XStreamFactory;
import com.sonatype.nexus.onboarding.rest.dto.CatalogDTO;
import com.sonatype.nexus.onboarding.rest.dto.CatalogEntryDTO;

public class meclipse1375CodebasesRestApiIT
    extends AbstractOnboardingIT
{
    public static final String URI = "service/local/mse/codebases/nx-codebase-repo";

    @Test
    public void listCodebases()
        throws Exception
    {
        Response r = RequestFacade.doGetRequest( URI );
        String t = r.getEntity().getText();

        if ( r.getStatus().isError() )
        {
            Assert.fail( t + "\n" + r.getStatus() );
        }

        XStreamRepresentation representation =
            new XStreamRepresentation( XStreamFactory.getXmlXStream(), t, MediaType.APPLICATION_XML );
        CatalogDTO catalog = (CatalogDTO) representation.getPayload( new CatalogDTO() );

        Assert.assertNotNull( catalog );
        Assert.assertFalse( catalog.getEntries().isEmpty() );

        // TODO better assertions
        CatalogEntryDTO entry = catalog.getEntries().get( 0 );
        Assert.assertTrue( entry.getGroupId().trim().length() > 0 );
        Assert.assertTrue( entry.getId().trim().length() > 0  );
        Assert.assertTrue( entry.getVersion().trim().length() > 0 );
        Assert.assertTrue( entry.getName().trim().length() > 0 );
        Assert.assertTrue( entry.getUrl().trim().length() > 0 );
    }
}
