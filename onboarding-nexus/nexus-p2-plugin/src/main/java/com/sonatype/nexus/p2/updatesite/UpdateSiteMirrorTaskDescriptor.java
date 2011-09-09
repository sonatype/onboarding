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
package com.sonatype.nexus.p2.updatesite;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.RepoOrGroupComboFormField;
import org.sonatype.nexus.tasks.descriptors.AbstractScheduledTaskDescriptor;
import org.sonatype.nexus.tasks.descriptors.ScheduledTaskDescriptor;

@Component( role = ScheduledTaskDescriptor.class, hint = UpdateSiteMirrorTask.ROLE_HINT, description = "Mirror Eclipse Update Site" )
public class UpdateSiteMirrorTaskDescriptor
    extends AbstractScheduledTaskDescriptor
{
    public static final String REPO_OR_GROUP_FIELD_ID = "repositoryId";

    public static final String FORCE_MIRROR_FIELD_ID = "ForceMirror";

    private final RepoOrGroupComboFormField repoField =
        new RepoOrGroupComboFormField( REPO_OR_GROUP_FIELD_ID, RepoOrGroupComboFormField.DEFAULT_LABEL,
                                       "Select Eclipse Update Site repository to assign to this task.",
                                       FormField.MANDATORY );

    private final CheckboxFormField forceField =
        new CheckboxFormField( FORCE_MIRROR_FIELD_ID, "Force mirror",
                               "Mirror Eclipse Update Site content even if site.xml did not change.",
                               FormField.OPTIONAL );

    public String getId()
    {
        return UpdateSiteMirrorTask.ROLE_HINT;
    }

    public String getName()
    {
        return "Mirror Eclipse Update Site";
    }

    public List<FormField> formFields()
    {
        List<FormField> fields = new ArrayList<FormField>();

        fields.add( repoField );
        fields.add( forceField );

        return fields;
    }
}
