locals {
  default_config_parameter_prefix       = length(var.environment_name) == 0 ? "psoxy_" : "${var.environment_name}_"
  config_parameter_prefix               = var.config_parameter_prefix == "" ? local.default_config_parameter_prefix : var.config_parameter_prefix
  environment_id_prefix                 = "${var.environment_name}${length(var.environment_name) > 0 ? "-" : ""}"
  environment_id_display_name_qualifier = length(var.environment_name) > 0 ? " ${var.environment_name} " : ""

  secret_replica_locations = coalesce(var.replica_regions, var.secret_replica_locations)
}

module "psoxy" {
  source = "../../modules/gcp"

  project_id                   = var.gcp_project_id
  environment_id_prefix        = local.environment_id_prefix
  psoxy_base_dir               = var.psoxy_base_dir
  deployment_bundle            = var.deployment_bundle
  force_bundle                 = var.force_bundle
  bucket_location              = var.gcp_region
  config_parameter_prefix      = local.config_parameter_prefix
  install_test_tool            = var.install_test_tool
  custom_artifacts_bucket_name = var.custom_artifacts_bucket_name
  default_labels               = var.default_labels
}

# constants
locals {
  SA_NAME_MIN_LENGTH = 6
  SA_NAME_MAX_LENGTH = 30
}

# BEGIN API CONNECTORS

locals {
  secrets_to_provision = {
    for k, v in var.api_connectors :
    k => {
      for var_def in v.secured_variables :
      # TODO: in v0.5, the prefix with the instance_id can be removed
      "${replace(upper(var_def.name), "-", "_")}" =>
      merge({
        instance_id        = k
        instance_secret_id = "${replace(upper(k), "-", "_")}_${replace(upper(var_def.name), "-", "_")}"
        value              = "TODO: fill me"
        description        = ""
        },
      var_def)
    }
  }
  lockable_secrets = flatten([
    for instance_id, secrets in local.secrets_to_provision :
    [for secret_id, secret in values(secrets) : secret if secret.lockable]
  ])
}

output "secrets_to_provision" {
  value = local.lockable_secrets
}

module "secrets" {
  for_each = var.api_connectors

  source = "../../modules/gcp-secrets"

  secret_project    = var.gcp_project_id
  path_prefix       = "${local.config_parameter_prefix}${replace(upper(each.key), "-", "_")}_"
  secrets           = local.secrets_to_provision[each.key]
  default_labels    = var.default_labels
  replica_locations = local.secret_replica_locations
}

resource "google_secret_manager_secret_iam_member" "grant_sa_updater_on_lockable_secrets" {
  for_each = { for secret in local.lockable_secrets : secret.instance_secret_id => secret }

  member    = "serviceAccount:${google_service_account.api_connectors[each.value.instance_id].email}"
  role      = module.psoxy.psoxy_instance_secret_locker_role_id
  project   = var.gcp_project_id
  secret_id = "${local.config_parameter_prefix}${each.value.instance_secret_id}"
}

locals {
  # sa account_ids must be at least 6 chars long; if api_connector keys are short, and environment_name
  # is also short (or empty), keys alone might not be long enough; so prepend in such cases

  # distinguishes SA for Cloud Functions from SAs for connector OAuth Clients
  function_qualifier = "fn-"

  default_sa_prefix      = "${local.environment_id_prefix}${local.function_qualifier}"
  long_default_sa_prefix = "psoxy-${local.environment_id_prefix}${local.function_qualifier}"

  sa_prefix = length(local.default_sa_prefix) < local.SA_NAME_MIN_LENGTH ? local.long_default_sa_prefix : local.default_sa_prefix
}

resource "google_service_account" "api_connectors" {
  for_each = var.api_connectors

  project      = var.gcp_project_id
  account_id   = substr("${local.sa_prefix}${replace(each.key, "_", "-")}", 0, local.SA_NAME_MAX_LENGTH)
  display_name = "${local.environment_id_display_name_qualifier} ${each.key} API Connector Cloud Function"
  description  = "Service account that cloud function for ${each.key} API Connector will run as"
}

module "api_connector" {
  for_each = var.api_connectors

  source = "../../modules/gcp-psoxy-rest"

  project_id                            = var.gcp_project_id
  region                                = var.gcp_region
  source_kind                           = each.value.source_kind
  environment_id_prefix                 = local.environment_id_prefix
  instance_id                           = each.key
  service_account_email                 = google_service_account.api_connectors[each.key].email
  artifacts_bucket_name                 = module.psoxy.artifacts_bucket_name
  deployment_bundle_object_name         = module.psoxy.deployment_bundle_object_name
  path_to_config                        = null
  path_to_repo_root                     = var.psoxy_base_dir
  example_api_calls                     = each.value.example_api_calls
  example_api_calls_user_to_impersonate = each.value.example_api_calls_user_to_impersonate
  todo_step                             = var.todo_step
  target_host                           = each.value.target_host
  source_auth_strategy                  = each.value.source_auth_strategy
  oauth_scopes                          = try(each.value.oauth_scopes_needed, [])
  config_parameter_prefix               = local.config_parameter_prefix
  invoker_sa_emails                     = var.worklytics_sa_emails
  default_labels                        = var.default_labels
  todos_as_local_files                  = var.todos_as_local_files

  environment_variables = merge(
    var.general_environment_variables,
    try(each.value.environment_variables, {}),
    {
      BUNDLE_FILENAME      = module.psoxy.filename
      IS_DEVELOPMENT_MODE  = contains(var.non_production_connectors, each.key)
      PSEUDONYMIZE_APP_IDS = tostring(var.pseudonymize_app_ids)
      CUSTOM_RULES_SHA     = try(var.custom_api_connector_rules[each.key], null) != null ? filesha1(var.custom_api_connector_rules[each.key]) : null
    }
  )

  secret_bindings = merge(
    # bc some of these are later filled directly, bind to 'latest'
    { for k, v in module.secrets[each.key].secret_bindings : k => merge(v, { version_number : "latest" }) },
    module.psoxy.secrets
  )
}

module "custom_api_connector_rules" {
  for_each = var.custom_api_connector_rules

  source = "../../modules/gcp-sm-rules"

  prefix         = "${local.config_parameter_prefix}${upper(replace(each.key, "-", "_"))}_"
  file_path      = each.value
  default_labels = var.default_labels
}
# END API CONNECTORS

# BEGIN BULK CONNECTORS
module "bulk_connector" {
  for_each = var.bulk_connectors

  source = "../../modules/gcp-psoxy-bulk"

  project_id                    = var.gcp_project_id
  region                        = var.gcp_region
  environment_id_prefix         = local.environment_id_prefix
  instance_id                   = each.key
  worklytics_sa_emails          = var.worklytics_sa_emails
  config_parameter_prefix       = local.config_parameter_prefix
  source_kind                   = each.value.source_kind
  artifacts_bucket_name         = module.psoxy.artifacts_bucket_name
  deployment_bundle_object_name = module.psoxy.deployment_bundle_object_name
  psoxy_base_dir                = var.psoxy_base_dir
  bucket_write_role_id          = module.psoxy.bucket_write_role_id
  secret_bindings               = module.psoxy.secrets
  example_file                  = try(each.value.example_file, null)
  input_expiration_days         = var.bulk_input_expiration_days
  sanitized_expiration_days     = var.bulk_sanitized_expiration_days
  input_bucket_name             = try(each.value.input_bucket_name, null)
  sanitized_bucket_name         = try(each.value.sanitized_bucket_name, null)
  default_labels                = var.default_labels
  todos_as_local_files          = var.todos_as_local_files


  environment_variables = merge(
    var.general_environment_variables,
    try(each.value.environment_variables, {}),
    {
      SOURCE              = each.value.source_kind
      RULES               = yamlencode(try(var.custom_bulk_connector_rules[each.key], each.value.rules))
      BUNDLE_FILENAME     = module.psoxy.filename
      IS_DEVELOPMENT_MODE = contains(var.non_production_connectors, each.key)
    }
  )
}
# END BULK CONNECTORS

# BEGIN LOOKUP TABLES
module "lookup_output" {
  for_each = var.lookup_tables

  source = "../../modules/gcp-output-bucket"

  bucket_write_role_id           = module.psoxy.bucket_write_role_id
  function_service_account_email = module.bulk_connector[each.value.source_connector_id].instance_sa_email
  project_id                     = var.gcp_project_id
  region                         = var.gcp_region
  bucket_name_prefix             = module.bulk_connector[each.value.source_connector_id].bucket_prefix
  bucket_name_suffix             = "-lookup" # TODO: what if multiple lookups from same source??
  expiration_days                = each.value.expiration_days
  sanitizer_accessor_principals  = each.value.sanitized_accessor_principals
  bucket_labels                  = var.default_labels
}

locals {
  inputs_to_build_lookups_for = toset(distinct([for k, v in var.lookup_tables : v.source_connector_id]))
}

# TODO: this would be cleaner as env var, but creates a cycle:
# Error: Cycle: module.psoxy.module.psoxy-bulk.local_file.todo-gcp-psoxy-bulk-test, module.psoxy.module.lookup_output.var.function_service_account_email (expand), module.psoxy.module.lookup_output.google_storage_bucket_iam_member.write_to_output_bucket, module.psoxy.module.lookup_output.output.bucket_name (expand), module.psoxy.module.lookup_output.var.bucket_name_prefix (expand), module.psoxy.module.lookup_output.google_storage_bucket.bucket, module.psoxy.module.lookup_output.google_storage_bucket_iam_member.accessors, module.psoxy.module.lookup_output (close), module.psoxy.module.psoxy-bulk.var.environment_variables (expand), module.psoxy.module.psoxy-bulk.google_cloudfunctions_function.function, module.psoxy.module.psoxy-bulk (close)
resource "google_secret_manager_secret" "additional_transforms" {
  for_each = local.inputs_to_build_lookups_for

  project   = var.gcp_project_id
  secret_id = "${local.config_parameter_prefix}${upper(replace(each.key, "-", "_"))}_ADDITIONAL_TRANSFORMS"
  labels    = var.default_labels

  replication {
    user_managed {
      dynamic "replicas" {
        for_each = local.secret_replica_locations
        content {
          location = replicas.value
        }
      }
    }
  }

  lifecycle {
    ignore_changes = [
      labels
    ]
  }
}

resource "google_secret_manager_secret_version" "additional_transforms" {
  for_each = local.inputs_to_build_lookups_for

  secret = google_secret_manager_secret.additional_transforms[each.key].name
  secret_data = yamlencode([
    for k, v in var.lookup_tables : {
      destinationBucketName : module.lookup_output[k].bucket_name
      rules : {
        columnsToDuplicate : {
          (v.join_key_column) : "${v.join_key_column}_pseudonym"
        },
        columnsToPseudonymize : ["${v.join_key_column}_pseudonym"]
        columnsToInclude : try(concat(v.columns_to_include, [
          v.join_key_column, "${v.join_key_column}_pseudonym"
        ]), null)
      }
    } if v.source_connector_id == each.key
  ])
}

resource "google_secret_manager_secret_iam_member" "additional_transforms" {
  for_each = local.inputs_to_build_lookups_for

  secret_id = google_secret_manager_secret.additional_transforms[each.key].id
  member    = "serviceAccount:${module.bulk_connector[each.key].instance_sa_email}"
  role      = "roles/secretmanager.secretAccessor"
}

# END LOOKUP TABLES

locals {
  api_instances = { for instance in module.api_connector :
    instance.instance_id => merge(
      {
        endpoint_url : instance.cloud_function_url
      },
      instance,
      var.api_connectors[instance.instance_id]
    )
  }

  bulk_instances = { for instance in module.bulk_connector :
    instance.instance_id => merge(
      {
        sanitized_bucket_name : instance.sanitized_bucket
      },
      instance,
      var.bulk_connectors[instance.instance_id]
    )
  }

  all_instances = merge(local.api_instances, local.bulk_instances)
}

# script to test ALL connectors
resource "local_file" "test_all_script" {
  count = var.todos_as_local_files ? 1 : 0

  filename        = "test-all.sh"
  file_permission = "0770"
  content         = <<EOF
#!/bin/bash

echo "Testing API Connectors ..."

%{for test_script in values(module.api_connector)[*].test_script~}
./${test_script}
%{endfor}

echo "Testing Bulk Connectors ..."

%{for test_script in values(module.bulk_connector)[*].test_script~}
./${test_script}
%{endfor}
EOF
}
