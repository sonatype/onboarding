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
package com.sonatype.nexus.proxy.p2.its.nxcm1871;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import org.restlet.data.Method;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

public class CacheMessageUtil
{
    public static void expireRepositoryCache( String... repositories )
        throws Exception
    {
        reindex( false, repositories );
    }

    private static void reindex( boolean group, String... repositories )
        throws IOException,
            Exception
    {
        for ( String repo : repositories )
        {
            // http://localhost:51292/nexus/service/local/data_cache/repo_groups/p2g/content
            String serviceURI;
            if ( group )
            {
                serviceURI = "service/local/data_cache/repo_groups/" + repo + "/content";
            }
            else
            {
                serviceURI = "service/local/data_cache/repositories/" + repo + "/content";
            }

            Response response = null;
            try
            {
                response = RequestFacade.sendMessage( serviceURI, Method.DELETE );
                Status status = response.getStatus();
                Assert.assertTrue( "Fail to update " + repo + " repository index " + status, status.isSuccess() );
            }
            finally
            {
                RequestFacade.releaseResponse( response );
            }
        }

        // let s w8 a few time for indexes
        TaskScheduleUtil.waitForAllTasksToStop();
    }

    public static void expireGroupCache( String... groups )
        throws Exception
    {
        reindex( true, groups );
    }
}
