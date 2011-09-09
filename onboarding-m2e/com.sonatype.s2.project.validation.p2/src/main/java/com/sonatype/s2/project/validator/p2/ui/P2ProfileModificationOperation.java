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
package com.sonatype.s2.project.validator.p2.ui;

import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProfileModificationOperation;

@SuppressWarnings( "restriction" )
class P2ProfileModificationOperation
    extends ProfileModificationOperation
{
    public P2ProfileModificationOperation( String label, String profileId, ProvisioningPlan plan,
                                           ProvisioningContext context )
    {
        super( label, profileId, plan, context );
    }

    @Override
    public boolean runInBackground()
    {
        return false;
    }
}
