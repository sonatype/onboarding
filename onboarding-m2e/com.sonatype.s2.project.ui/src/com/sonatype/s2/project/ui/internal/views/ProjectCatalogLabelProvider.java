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
package com.sonatype.s2.project.ui.internal.views;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.sonatype.s2.project.model.IS2Module;
import com.sonatype.s2.project.model.IS2ProjectCatalog;
import com.sonatype.s2.project.model.IS2ProjectCatalogEntry;
import com.sonatype.s2.project.ui.internal.Images;

public class ProjectCatalogLabelProvider
    extends LabelProvider
{
    public String getText( Object obj )
    {
        if ( obj instanceof IS2ProjectCatalog )
        {
            String s = ( (IS2ProjectCatalog) obj ).getName();
            return s == null || s.length() == 0 ? ( (IS2ProjectCatalog) obj ).getUrl() : s;
        }
        else if ( obj instanceof IS2ProjectCatalogEntry )
        {
            return ( (IS2ProjectCatalogEntry) obj ).getName();
        }
        else if ( obj instanceof IS2Module )
        {
            return ( (IS2Module) obj ).getName();
        }
        return String.valueOf( obj );
    }

    public Image getImage( Object obj )
    {
        if ( obj instanceof IS2ProjectCatalogEntry )
        {
            return Images.CATALOG_ENTRY;
        }
        return Images.CATALOG_PROJECT;
    }

}
