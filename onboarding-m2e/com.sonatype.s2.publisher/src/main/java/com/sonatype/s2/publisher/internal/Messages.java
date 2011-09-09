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
package com.sonatype.s2.publisher.internal;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends NLS
{
    private static final String BUNDLE = "com.sonatype.s2.publisher.internal.messages";

    static
    {
        NLS.initializeMessages( BUNDLE, Messages.class );
    }

    public static String abstractPublishAction_publish;

    public static String action_close;

    public static String environmentDialog_all;

    public static String environmentDialog_format;

    public static String environmentDialog_message;

    public static String environmentDialog_some;

    public static String environmentDialog_title;

    public static String errorContactingNexus;

    public static String errorInvalidNexusUrl;

    public static String errorParsing;

    public static String errorRepositoriesDialog_message;

    public static String errorRepositoriesDialog_title;

    public static String lineupWizard_errors_couldNotPublishLineup;

    public static String lineupWizard_errors_emptyIUs;

    public static String lineupWizard_errors_emptyRepos;

    public static String lineupWizard_errors_selectOne;

    public static String lineupWizard_errors_iuImportFailed;

    public static String lineupWizard_errors_nexusCheckFailed;

    public static String lineupWizard_errors_nexusForbidden;

    public static String lineupWizard_errors_nexusNotFound;

    public static String lineupWizard_errors_unknownServerError;

    public static String lineupWizard_errors_unresolvedRepositories;
    
    public static String lineupWizard_warnings_unresolvedRepositories;

    public static String lineupWizard_errors_warningOnly;

    public static String lineupWizard_uiPage_materialize_question;

    public static String lineupWizard_uiPage_materialize_title;

    public static String lineupWizard_iuPage_add;

    public static String lineupWizard_iuPage_add2;

    public static String lineupWizard_iuPage_addIU_title;

    public static String lineupWizard_iuPage_addIU_message;

    public static String lineupWizard_iuPage_addRepository_component;

    public static String lineupWizard_iuPage_addRepository_location;

    public static String lineupWizard_iuPage_addRepository_name;

    public static String lineupWizard_iuPage_addRepository_title;

    public static String lineupWizard_iuPage_edit;

    public static String lineupWizard_iuPage_idColumn;

    public static String lineupWizard_iuPage_installableUnits;

    public static String lineupWizard_iuPage_loading_repositories;

    public static String lineupWizard_iuPage_manageNexusRepositories;

    public static String lineupWizard_iuPage_message;

    public static String lineupWizard_iuPage_moveDown;

    public static String lineupWizard_iuPage_moveUp;

    public static String lineupWizard_iuPage_remove;

    public static String lineupWizard_iuPage_remove2;

    public static String lineupWizard_iuPage_title;

    public static String lineupWizard_iuPage_validate;

    public static String lineupWizard_iuPage_versionColumn;

    public static String lineupWizard_lineupPage_defaultCatalogUrl;

    public static String lineupWizard_lineupPage_description;

    public static String lineupWizard_lineupPage_importJob;

    public static String lineupWizard_lineupPage_loadIUs;

    public static String lineupWizard_lineupPage_maximumMemory;

    public static String lineupWizard_lineupPage_runtimeEnvironments;

    public static String lineupWizard_lineupPage_title;

    public static String lineupWizard_nexusPage_artifactId;

    public static String lineupWizard_nexusPage_coordinatesSubtitle;

    public static String lineupWizard_nexusPage_description;

    public static String lineupWizard_nexusPage_groupId;

    public static String lineupWizard_nexusPage_message;

    public static String lineupWizard_nexusPage_nexusCheckJob;

    public static String lineupWizard_nexusPage_nexusSubtitle;

    public static String lineupWizard_nexusPage_password;

    public static String lineupWizard_nexusPage_title;

    public static String lineupWizard_nexusPage_url;

    public static String lineupWizard_nexusPage_username;

    public static String lineupWizard_nexusPage_version;

    public static String lineupPublishWizardPage_cbPmd_text;

    public static String lineupPublishWizardPage_lblLineup_text;

    public static String lineupPublishWizardPage_lblNexusServer_text;

    public static String lineupPublishWizardPage_lblRepositories_text;

    public static String lineupPublishWizardPage_this_title;

    public static String LineupUploadInfoWizardPage_artifactid_validation;

    public static String LineupUploadInfoWizardPage_error_auth;

    public static String LineupUploadInfoWizardPage_error_no_osgi_version;

    public static String LineupUploadInfoWizardPage_groupid_validation;

    public static String LineupUploadInfoWizardPage_password_validation;

    public static String LineupUploadInfoWizardPage_server_validation;

    public static String LineupUploadInfoWizardPage_username_validation;

    public static String LineupUploadInfoWizardPage_version_validation;

    public static String lineupWizard_title;

    public static String validation_error;

    public static String NewS2ProjectWizard_artifactId;

    public static String NewS2ProjectWizard_groupId;

    public static String NewS2ProjectWizard_page_desc;

    public static String NewS2ProjectWizard_page_title;

    public static String NewS2ProjectWizard_title;

    public static String NewS2ProjectWizard_version;

    public static String NexusCodebasePublisher_error_codebaseExists;

    public static String publish_action_unresolvedErrors_message;

    public static String publishWizard_error;

    public static String PublishWizard_error_auth;

    public static String PublishWizard_error_forbidden;

    public static String PublishWizard_error_notfound;

    public static String PublishWizardPage_description;

    public static String PublishWizardPage_password_validation;

    public static String PublishWizardPage_server_label;

    public static String PublishWizardPage_server_validation;

    public static String PublishWizardPage_username_validation;

    public static String S2ProjectInfoPage_artifactid_validation;

    public static String S2ProjectInfoPage_groupid_validation;

    public static String S2ProjectInfoPage_version_validation;

    public static String nexusLineupPublisher_p2_validation_errors;
    
    public static String lineup;
    
    public static String codebase;
    
}
