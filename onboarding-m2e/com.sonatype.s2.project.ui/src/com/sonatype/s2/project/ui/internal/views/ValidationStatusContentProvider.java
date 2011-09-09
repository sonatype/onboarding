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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ValidationStatusContentProvider
    implements ITreeContentProvider
{
    private boolean errorsOnly = true;

    public Object[] getElements( Object input )
    {
        List<IStatus> list = new ArrayList<IStatus>();

        if ( input instanceof IStatus )
        {
            addStatus( list, (IStatus) input );
        }
        return filterErrors( list );
    }

    private void addStatus( List<IStatus> list, IStatus status )
    {
        if ( status.isMultiStatus() )// && ( status.getMessage() == null || status.getMessage().length() == 0 ) )
        {
            Collections.addAll( list, status.getChildren() );
        }
        else
        {
            list.add( status );
        }
    }

    private Object[] filterErrors( List<IStatus> statuses )
    {
        if ( errorsOnly )
        {
            List<IStatus> list = new ArrayList<IStatus>( statuses.size() );
            for ( IStatus status : statuses )
            {
                if ( !status.isOK() )
                {
                    list.add( status );
                }
            }
            statuses = list;
        }
        return statuses.toArray();
    }

    public Object[] getChildren( Object parentElement )
    {
        if ( parentElement instanceof IStatus && ( (IStatus) parentElement ).isMultiStatus() )
        {
            return filterErrors( Arrays.asList( ( (IStatus) parentElement ).getChildren() ) );
        }
        return new Object[0];
    }

    public Object getParent( Object element )
    {
        return null;
    }

    public boolean hasChildren( Object element )
    {
        return element instanceof IStatus && ( (IStatus) element ).isMultiStatus();
    }

    public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
    {
    }

    public void dispose()
    {
    }

    public void setErrorsOnly( boolean errorsOnly )
    {
        this.errorsOnly = errorsOnly;
    }

    public boolean isErrorsOnly()
    {
        return errorsOnly;
    }
}
