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
package com.sonatype.s2.securityrealm.model.io.xstream;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import org.codehaus.plexus.util.IOUtil;

import com.sonatype.nexus.onboarding.rest.dto.xstream.XStreamUtil;
import com.sonatype.s2.securityrealm.model.IS2SecurityRealm;
import com.sonatype.s2.securityrealm.model.IS2SecurityRealmURLAssoc;
import com.thoughtworks.xstream.XStream;

public class S2SecurityRealmXstreamIO
{
    public static List<IS2SecurityRealm> readRealmList( InputStream is )
    {
        XStream xs = newInitializedXstream();

        return (List<IS2SecurityRealm>) xs.fromXML( is );
    }

    public static byte[] writeRealm( IS2SecurityRealm realm )
    {
        XStream xs = newInitializedXstream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try
        {
            xs.toXML( realm, os );
        }
        finally
        {
            IOUtil.close( os );
        }
        return os.toByteArray();
    }

    private static XStream newInitializedXstream()
    {
        XStream xs = new XStream();
        XStreamUtil.initializeXStream( xs );
        // use the models classloader
        xs.setClassLoader( S2SecurityRealmXstreamIO.class.getClassLoader() );
        return xs;
    }

    public static List<IS2SecurityRealmURLAssoc> readRealmURLAssocList( InputStream is )
    {
        XStream xs = newInitializedXstream();

        return (List<IS2SecurityRealmURLAssoc>) xs.fromXML( is );
    }

    public static byte[] writeRealmURLAssoc( IS2SecurityRealmURLAssoc s2RealmURLAssoc )
    {
        XStream xs = newInitializedXstream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try
        {
            xs.toXML( s2RealmURLAssoc, os );
        }
        finally
        {
            IOUtil.close( os );
        }
        return os.toByteArray();
    }

    public static IS2SecurityRealmURLAssoc readRealmURLAssoc( InputStream is )
    {
        XStream xs = newInitializedXstream();

        return (IS2SecurityRealmURLAssoc) xs.fromXML( is );
    }
}
