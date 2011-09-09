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
package com.sonatype.nexus.p2.impl;

import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.MatchQuery;

@SuppressWarnings( "restriction" )
public class PartialInstallableUnitsQuery
    extends MatchQuery
{

    public static final PartialInstallableUnitsQuery QUERY = new PartialInstallableUnitsQuery();

    @Override
    public boolean isMatch( Object candidate )
    {
        if ( !( candidate instanceof IInstallableUnit ) )
        {
            return false;
        }

        IInstallableUnit iu = (IInstallableUnit) candidate;

        return isPartialIU( iu );
    }

    public static boolean isPartialIU( IInstallableUnit iu )
    {
        return Boolean.valueOf( iu.getProperty( IInstallableUnit.PROP_PARTIAL_IU ) ).booleanValue();
    }

}
