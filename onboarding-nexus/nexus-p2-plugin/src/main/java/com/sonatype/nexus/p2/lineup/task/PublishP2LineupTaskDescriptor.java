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
package com.sonatype.nexus.p2.lineup.task;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.tasks.descriptors.AbstractScheduledTaskDescriptor;
import org.sonatype.nexus.tasks.descriptors.ScheduledTaskDescriptor;

@Component( role = ScheduledTaskDescriptor.class, hint = "PublishP2Lineup", description = "Publish P2 Lineup Repository" )
public class PublishP2LineupTaskDescriptor
    extends AbstractScheduledTaskDescriptor
{
    public static final String ID = "PublishP2LineupTask";

    public String getId()
    {
        return ID;
    }

    public String getName()
    {
        return "Publish P2 Lineup Repository";
    }

    public List<FormField> formFields()
    {
        return new ArrayList<FormField>();
    }
}
