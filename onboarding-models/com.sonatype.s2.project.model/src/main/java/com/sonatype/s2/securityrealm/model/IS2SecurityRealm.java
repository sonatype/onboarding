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
package com.sonatype.s2.securityrealm.model;

public interface IS2SecurityRealm
{
    public String getId();

    public void setId( String id );

    public String getName();

    public void setName( String name );

    public String getDescription();

    public void setDescription( String description );

    public void setAuthenticationType( S2SecurityRealmAuthenticationType authType );

    public S2SecurityRealmAuthenticationType getAuthenticationType();
}
