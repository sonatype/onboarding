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
package com.sonatype.s2.project.ui.internal.wizards;

import java.beans.Beans;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Combo;
import org.maven.ide.eclipse.ui.common.InputHistory;

abstract public class WizardPageWithHistory
    extends WizardPage
{
    private InputHistory inputHistory;

    /** a helper flag to ensure the history is only loaded once */
    private boolean historyLoaded = false;

    protected WizardPageWithHistory( String title )
    {
        super( title );
        setTitle( title );

    }

    /** Loads the advanced settings data when the page is displayed. */
    public void setVisible( boolean visible )
    {
        if ( visible  && !Beans.isDesignTime())
        {
            if ( !historyLoaded )
            {
                getInputHistory().load();
                historyLoaded = true;
            }
            else
            {
                getInputHistory().save();
            }
        
        }
        super.setVisible( visible );
    }

    /** Saves the history when the page is disposed. */
    public void dispose()
    {
        if (!java.beans.Beans.isDesignTime()) {
            getInputHistory().save();
        }
        super.dispose();
    }

    protected void addToInputHistory( String id, Combo combo )
    {
        getInputHistory().add( id, combo );
    }

    protected InputHistory getInputHistory()
    {
        if (inputHistory == null) {
            inputHistory = new InputHistory( getTitle() );
        }
        return inputHistory;
    }
}
