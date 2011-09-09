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
package com.sonatype.s2.project.ui.catalog;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends com.sonatype.s2.project.ui.internal.Messages
{
    private static final String BUNDLE_NAME = Messages.class.getName().toLowerCase();

    static
    {
        NLS.initializeMessages( BUNDLE_NAME, Messages.class );
    }

    public static String catalogContentProvider_catalogUrlLabel;

    public static String catalogContentProvider_errors_authenticationError;

    public static String catalogContentProvider_errors_errorLoadingCatalog;

    public static String catalogContentProvider_errors_errorLoadingProject;

    public static String catalogContentProvider_errors_errorLoadingRegistry;

    public static String catalogContentProvider_jobs_loadingCatalogs;

    public static String catalogContentProvider_jobs_loadingProjectFrom;

    public static String catalogView_actions_add_error;

    public static String catalogView_actions_add_dialogTitle;

    public static String catalogView_actions_add_title;

    public static String catalogView_actions_add_tooltip;

    public static String catalogView_actions_copyUrl_error;

    public static String catalogView_actions_copyUrl_title;

    public static String catalogView_actions_copyUrl_tooltip;

    public static String catalogView_actions_editCredentials_dialogTitle;

    public static String catalogView_actions_editCredentials_title;

    public static String catalogView_actions_editCredentials_tooltip;

    public static String catalogView_actions_editProject_title;

    public static String catalogView_actions_editProject_tooltip;

    public static String catalogView_actions_reload_title;

    public static String catalogView_actions_reload_tooltip;

    public static String catalogView_actions_remove_dialogTitle;

    public static String catalogView_actions_remove_message;

    public static String catalogView_actions_remove_title;

    public static String catalogView_actions_remove_tooltip;

    public static String catalogView_actions_viewProject_title;

    public static String catalogView_actions_viewProject_tooltip;

    public static String catalogView_descriptorUrlLabel;

    public static String catalogView_empty_addNew;

    public static String catalogView_empty_loadDefault;

    public static String catalogView_empty_message;

    public static String catalogView_numberOfEntries;

    public static String catalogView_errors_projectDescriptorAuthenticationError;

    public static String catalogView_jobs_loadingProjectDetails;

    public static String catalogView_title;

    public static String eclipsePreferencesDialog_exportWorkspacePreferences;

    public static String eclipsePreferencesDialog_externalUrl;

    public static String eclipsePreferencesDialog_message;

    public static String eclipsePreferencesDialog_title;

    public static String linkUtil_actions_copy;

    public static String linkUtil_actions_open;

    public static String linkUtil_actions_openExternal;

    public static String linkUtil_errors_errorOpeningUrl;

    public static String linkUtil_errors_invalidUrl;

    public static String NexusResourceLookupDialog_nexusServer;

    public static String ProjectEditorOverviewPage_artifactid;

    public static String ProjectEditorOverviewPage_artifactid_cannot_be_empty;

    public static String ProjectEditorOverviewPage_groupId;

    public static String ProjectEditorOverviewPage_groupid_cannot_be_empty;

    public static String ProjectEditorOverviewPage_version;

    public static String ProjectEditorOverviewPage_version_cannot_be_empty;

    public static String projectEditor_details_builds;

    public static String projectEditor_details_description;

    public static String projectEditor_details_documentation;

    public static String projectEditor_details_eclipsePreferences;

    public static String projectEditor_details_image;

    public static String projectEditor_details_imageTemplate;

    public static String projectEditor_details_installPath;

    public static String projectEditor_details_installPathCustomize;

    public static String projectEditor_details_issueTracking;

    public static String projectEditor_details_moduleName;

    public static String projectEditor_details_p2Lineup;

    public static String projectEditor_details_projectHome;

    public static String projectEditor_details_projectName;

    public static String projectEditor_details_requiredMemory;

    public static String projectEditor_details_scmLocation;

    public static String projectEditor_details_securityRealm;

    public static String projectEditor_details_title;

    public static String projectEditor_details_workingSet;

    public static String projectEditor_details_workspacePath;

    public static String projectEditor_details_workspacePathCustomize;

    public static String projectEditor_eclipsePreferences;

    public static String projectEditor_eclipsePreferences_view;

    public static String projectEditor_errors_badInput;

    public static String projectEditor_errors_eclipsePreferencesAuthenticationError;

    public static String projectEditor_errors_errorLoadingEclipsePreferences;

    public static String projectEditor_errors_errorLoadingOverview;

    public static String projectEditor_errors_errorLoadingSettings;

    public static String projectEditor_errors_errorOpeningProject;

    public static String projectEditor_errors_errorSavingProject;

    public static String projectEditor_errors_feedEmpty;

    public static String projectEditor_errors_feedInvalidUrl;

    public static String projectEditor_errors_fileChangedTitle;

    public static String projectEditor_errors_fileChangedText;

    public static String projectEditor_errors_fileDeletedTitle;

    public static String projectEditor_errors_fileDeletedText;

    public static String projectEditor_errors_invalidDocsUrl;

    public static String projectEditor_errors_invalidEclipsePreferencesUrl;

    public static String projectEditor_errors_invalidHomeUrl;

    public static String projectEditor_errors_invalidMavenSettingsUrl;

    public static String projectEditor_errors_invalidP2LineupUrl;

    public static String projectEditor_errors_moduleNameEmpty;

    public static String projectEditor_errors_moduleNameExists;

    public static String projectEditor_errors_modulesNeeded;

    public static String projectEditor_errors_noDocumentProvider;

    public static String projectEditor_errors_p2LineupNeeded;

    public static String projectEditor_errors_profileEmpty;

    public static String projectEditor_errors_profileExists;

    public static String projectEditor_errors_projectNameEmpty;

    public static String projectEditor_errors_profileWrongChars;

    public static String projectEditor_errors_realmIdEmpty;

    public static String projectEditor_errors_realmIdExists;

    public static String projectEditor_errors_rootEmpty;

    public static String projectEditor_errors_rootExists;

    public static String projectEditor_errors_scmAccessError;

    public static String projectEditor_errors_scmLocationRequired;

    public static String projectEditor_errors_scmTypeRequired;

    public static String projectEditor_errors_settingsAuthenticationError;

    public static String projectEditor_feeds_title;

    public static String projectEditor_jobs_loadingEclipsePreferences;

    public static String projectEditor_jobs_loadingMavenSettings;

    public static String projectEditor_mavenSettings_label;

    public static String projectEditor_mavenSettings_hyperlink;

    public static String projectEditor_mavenSettings_view;

    public static String projectEditor_modules_delete_message;

    public static String projectEditor_modules_delete_message2;

    public static String projectEditor_modules_delete_title;

    public static String projectEditor_modules_feeds_description;

    public static String projectEditor_modules_feeds_newMessage;

    public static String projectEditor_modules_feeds_newTitle;

    public static String projectEditor_modules_feeds_title;

    public static String projectEditor_modules_label;

    public static String projectEditor_modules_moduleInformation_description;

    public static String projectEditor_modules_moduleInformation_title;

    public static String projectEditor_modules_newModule_message;

    public static String projectEditor_modules_newModule_title;

    public static String projectEditor_modules_profiles_description;

    public static String projectEditor_modules_profiles_newMessage;

    public static String projectEditor_modules_profiles_newTitle;

    public static String projectEditor_modules_profiles_title;

    public static String projectEditor_modules_roots_description;

    public static String projectEditor_modules_roots_newMessage;

    public static String projectEditor_modules_roots_newTitle;

    public static String projectEditor_modules_roots_title;

    public static String projectEditor_modules_scm_description;

    public static String projectEditor_modules_scm_title;

    public static String projectEditor_modules_scm_validate;

    public static String projectEditor_modules_scm_validating;

    public static String projectEditor_modules_scm_validationSuccessful;

    public static String projectEditor_modules_title;

    public static String projectEditor_overview_eclipse_description;

    public static String projectEditor_overview_eclipse_title;

    public static String projectEditor_overview_image_title;

    public static String projectEditor_overview_locations_description;

    public static String projectEditor_overview_locations_title;

    public static String projectEditor_overview_modules_description;

    public static String projectEditor_overview_modules_title;

    public static String projectEditor_overview_projectInformation_description;

    public static String projectEditor_overview_projectInformation_title;

    public static String projectEditor_overview_realms_description;

    public static String projectEditor_overview_realms_title;

    public static String projectEditor_overview_tab;

    public static String projectEditor_overview_title;

    public static String projectEditor_realms_delete_message;

    public static String projectEditor_realms_delete_message2;

    public static String projectEditor_realms_delete_title;

    public static String projectEditor_realms_new_message;

    public static String projectEditor_realms_new_title;

    public static String projectEditor_realms_realm;

    public static String projectEditor_settingsUrlLabel;

    public static String projectEditor_title;

    public static String projectViewer_title;

    public static String SelectMavenSettingsDialog_btnLogin;

    public static String SelectMavenSettingsDialog_clickLoadNow;

    public static String SelectMavenSettingsDialog_comServer_validation;

    public static String SelectMavenSettingsDialog_contactingNexus;

    public static String SelectMavenSettingsDialog_error;

    public static String SelectMavenSettingsDialog_error_auth;

    public static String SelectMavenSettingsDialog_error_forbidden;

    public static String SelectMavenSettingsDialog_error_notfound;

    public static String SelectMavenSettingsDialog_lblHint;

    public static String SelectMavenSettingsDialog_lblPassword;

    public static String SelectMavenSettingsDialog_lblPassword_validation;

    public static String SelectMavenSettingsDialog_lblServer_text;

    public static String SelectMavenSettingsDialog_lblSettings;

    public static String SelectMavenSettingsDialog_lblUsername;

    public static String SelectMavenSettingsDialog_manage;

    public static String SelectMavenSettingsDialog_message;

    public static String SelectMavenSettingsDialog_title;

    public static String SelectMavenSettingsDialog_txtUsername_validation;

    public static String selectLineupDialog_availableLineups;

    public static String selectLineupDialog_clickLoadNow;

    public static String selectLineupDialog_errors_errorLoadingLineups;

    public static String selectLineupDialog_loadLineups;

    public static String selectLineupDialog_loadingLineups;

    public static String selectLineupDialog_nexusServer_collapsed;

    public static String selectLineupDialog_nexusServer_expanded;

    public static String selectLineupDialog_nexusServer_label;

    public static String selectLineupDialog_selectLineup;

    public static String selectLineupDialog_title;

    public static String selectLineupDialog_error_authFailed;

    public static String selectLineupDialog_error_forbidden;

    public static String selectLineupDialog_error_notfound;

}
