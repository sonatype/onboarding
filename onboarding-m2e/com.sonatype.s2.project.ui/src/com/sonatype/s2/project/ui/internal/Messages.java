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
package com.sonatype.s2.project.ui.internal;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends NLS
{
    private static final String BUNDLE = "com.sonatype.s2.project.ui.internal.messages";

    static
    {
        NLS.initializeMessages( BUNDLE, Messages.class );
    }

    public static String actions_browse_title;

    public static String actions_collapseAll_title;

    public static String actions_collapseAll_tooltip;

    public static String actions_copy_title;

    public static String actions_create_title;

    public static String actions_delete_title;

    public static String actions_deselectAll_title;

    public static String actions_materializeProject_errors_illegalState;

    public static String actions_materializeProject_errors_materializationFailed;

    public static String actions_materializeProject_tasks_loadingProjectDescriptor;

    public static String actions_materializeProject_tasks_validatingProjectRequirements;

    public static String actions_materializeProject_title;

    public static String actions_materializeProject_tooltip;

    public static String actions_select_title;

    public static String actions_selectAll_title;

    public static String errors_unresolvedAddress;

    public static String exportWizard_errors_temporaryFile;

    public static String exportWizard_groupsPage_deployToLabel;

    public static String exportWizard_groupsPage_description;

    public static String exportWizard_groupsPage_destination;

    public static String exportWizard_groupsPage_errors_enterFileName;

    public static String exportWizard_groupsPage_errors_directory;

    public static String exportWizard_groupsPage_errors_absolute;

    public static String exportWizard_groupsPage_errors_exists;

    public static String exportWizard_groupsPage_errors_enterUrl;

    public static String exportWizard_groupsPage_errors_nothingSelected;

    public static String exportWizard_groupsPage_preferenceGroups;

    public static String exportWizard_groupsPage_saveToFileLabel;

    public static String exportWizard_groupsPage_title;

    public static String exportWizard_groupsPage_uploadToLabel;

    public static String exportWizard_jobs_deployingPreferences;

    public static String exportWizard_jobs_exportingPreferences;

    public static String exportWizard_jobs_exportingWorkspacePreferences;

    public static String exportWizard_title;

    public static String installationWizard_couldNotCreateEclipseInstallation;

    public static String installationWizard_errorAccessingNexusUrl;

    public static String installationWizard_errors_pathIsNotAbsolute;

    public static String installationWizard_errors_pathIsNotADirectory;

    public static String installationWizard_errors_pathIsNotAnEmptyDirectory;

    public static String installationWizard_errors_workspacePathNestedInEclipsePath;

    public static String installationWizard_errors_workspacePathSameAsEclipsePath;

    public static String installationWizard_installationPage_description;

    public static String installationWizard_installationPage_installType;

    public static String installationWizard_installationPage_installationDirectory;

    public static String installationWizard_installationPage_installationDirectoryName;

    public static String installationWizard_installationPage_shared;

    public static String installationWizard_installationPage_standalone;

    public static String installationWizard_installationPage_title;

    public static String installationWizard_installationPage_workspaceLocation;

    public static String installationWizard_installationPage_workspaceLocationName;

    public static String installationWizard_nexusPage_description;

    public static String installationWizard_nexusPage_title;

    public static String installationWizard_projectPage_description;

    public static String installationWizard_projectPage_title;

    public static String installationWizard_title;

    public static String loading;

    public static String mavenStudio;

    public static String materializationWizard_errors_couldNotValidate;

    public static String materializationWizard_errors_errorLoadingCatalog;

    public static String materializationWizard_errors_errorLoadingProject;

    public static String materializationWizard_jobs_projectMaterialization;

    public static String materializationWizard_realmsPage_description;

    public static String materializationWizard_realmsPage_errors_couldNotValidate;

    public static String materializationWizard_realmsPage_errors_canceled;

    public static String materializationWizard_realmsPage_errors_problemsDetected;

    public static String materializationWizard_errors_notfound;

    public static String materializationWizard_errors_forbidden;

    public static String materializationWizard_realmsPage_ignoreValidationResults;

    public static String materializationWizard_realmsPage_realm;

    public static String materializationWizard_realmsPage_resources;

    public static String materializationWizard_realmsPage_title;

    public static String materializationWizard_title;

    public static String materializationWizard_urlPage_description;

    public static String materializationWizard_urlPage_errors_authenticationError;

    public static String materializationWizard_urlPage_errors_errorLoadingProject;

    public static String materializationWizard_urlPage_enterUrl;

    public static String materializationWizard_urlPage_invalidUrl;

    public static String materializationWizard_urlPage_urlLabel;

    public static String materializationWizard_validationPage_description;

    public static String materializationWizard_validationPage_errors_couldNotRemediate;

    public static String materializationWizard_validationPage_errors_couldNotValidateProject;

    public static String materializationWizard_validationPage_errors_projectValidationCanceled;

    public static String materializationWizard_validationPage_errors_remediationInterrupted;

    public static String materializationWizard_validationPage_errors_validationIncomplete;

    public static String materializationWizard_validationPage_errors_validationKaputt;

    public static String materializationWizard_validationPage_ignoreValidationResults;

    public static String materializationWizard_validationPage_remediateButton;

    public static String materializationWizard_validationPage_title;

    public static String materializationWizard_validationPage_validatingProjectRequirements;

    public static String materializationWizard_validationPage_validationSuccessful;

    public static String nexusUrlComposite_url_label;

    public static String nexusUrlComposite_validating;

    public static String nexusUrlComposite_validationFailed;

    public static String projectData_descriptorUrlLabel;

    public static String projectData_errors_errorLoadingProjectDescriptor;

    public static String projectData_errors_errorLoadingProjects;

    public static String projectData_errors_invalidUrl;

    public static String projectData_errors_projectDescriptorAuthenticationError;

    public static String projectData_jobs_loadingProjectDetails;

    public static String projectData_jobs_loadingProjects;

    public static String projectNameTemplateComposite_projectName_label;

    public static String projectNameTemplateComposite_projectName_name;

    public static String projectNameTemplateComposite_template_label;

    public static String projectNameTemplateComposite_template_name;

    public static String ProjectScmSecurityRealmsPage_btn_ClientCertificate;

    public static String ProjectScmSecurityRealmsPage_description;

    public static String ProjectScmSecurityRealmsPage_description_short;

    public static String ProjectScmSecurityRealmsPage_fileSelect_title;

    public static String ProjectScmSecurityRealmsPage_fileSelect_filter1;

    public static String ProjectScmSecurityRealmsPage_fileSelect_filter2;

    public static String ProjectScmSecurityRealmsPage_lblClientCertificate;

    public static String ProjectScmSecurityRealmsPage_lblHint;

    public static String ProjectScmSecurityRealmsPage_lblPassphrase;

    public static String ProjectScmSecurityRealmsPage_lblPassword;

    public static String ProjectScmSecurityRealmsPage_lblSecurityRealm;

    public static String ProjectScmSecurityRealmsPage_lblUsername;

    public static String ProjectScmSecurityRealmsPage_validator_certificate;

    public static String ProjectScmSecurityRealmsPage_error_auth;

    public static String ProjectScmSecurityRealmsPage_error_forbidden;

    public static String ProjectScmSecurityRealmsPage_error_notfound;

    public static String ProjectScmSecurityRealmsPage_file;

    public static String ProjectScmSecurityRealmsPage_use_ssl;

    public static String sshHandler_title;

    public static String status_cancel;

    public static String status_error;

    public static String status_information;

    public static String status_ok;

    public static String status_warning;

    public static String userSettingsPage_columns_description;

    public static String userSettingsPage_columns_name;

    public static String userSettingsPage_columns_value;

    public static String userSettingsPage_errors_empty;

    public static String userSettingsPage_errors_errorLoadingMavenSettings;

    public static String userSettingsPage_errors_noUserHome;

    public static String userSettingsPage_jobs_loadingSettings;

    public static String userSettingsPage_message;

    public static String userSettingsPage_restoreDefault;

    public static String userSettingsPage_title;

    public static String userSettingsPage_tooltip;

    public static String validationStatusViewer_columns_messages;

    public static String validationStatusViewer_columns_validator;

    public static String validationStatusViewer_failed;

    public static String validationStatusViewer_showErrorsOnly;

    public static String validationStatusViewer_successful;

    public static String validationStatusViewer_title;
}
