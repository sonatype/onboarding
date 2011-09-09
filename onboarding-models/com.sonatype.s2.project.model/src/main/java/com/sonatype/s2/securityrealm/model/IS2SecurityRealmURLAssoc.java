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

public interface IS2SecurityRealmURLAssoc
{
    public String getId();

    public String getRealmId();

    public String getUrl();

    public void setId( String id );

    public void setRealmId( String realmId );

    public void setUrl( String url );

    public void setAnonymousAccess( S2AnonymousAccessType anonymousAccess );

    public S2AnonymousAccessType getAnonymousAccess();
}
