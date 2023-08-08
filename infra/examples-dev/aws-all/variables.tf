variable "environment_name" {
  type        = string
  description = "friendly qualifier to distinguish resources created by this terraform configuration other Terraform deployments, (eg, 'prod', 'dev', etc)"

  validation {
    condition     = can(regex("^[a-zA-Z][a-zA-Z0-9-_ ]*[a-zA-Z0-9]$", var.environment_name))
    error_message = "The `environment_name` must start with a letter, can contain alphanumeric characters, hyphens, underscores, and spaces, and must end with a letter or number."
  }

  validation {
    condition     = !can(regex("^(?i)(aws|ssm)", var.environment_name))
    error_message = "The `environment_name` cannot start with 'aws' or 'ssm', as this will name your AWS resources with prefixes that displease the AMZN overlords."
  }
}

variable "aws_account_id" {
  type        = string
  description = "id of aws account in which to provision your AWS infra"

  validation {
    condition     = can(regex("^\\d{12}$", var.aws_account_id))
    error_message = "The aws_account_id value should be 12-digit numeric string."
  }
}

variable "aws_assume_role_arn" {
  type        = string
  description = "ARN of role Terraform should assume when provisioning your infra. (can be `null` if your CLI is auth'd as the right user/role)"
  default     = null
}

variable "aws_region" {
  type        = string
  description = "default region in which to provision your AWS infra"
  default     = "us-east-1"
}

variable "aws_ssm_param_root_path" {
  type        = string
  description = "root to path under which SSM parameters created by this module will be created; NOTE: shouldn't be necessary to use this is you're following recommended approach of using dedicated AWS account for deployment"
  default     = ""

  validation {
    condition     = length(var.aws_ssm_param_root_path) == 0 || length(regexall("/", var.aws_ssm_param_root_path)) == 0 || startswith(var.aws_ssm_param_root_path, "/")
    error_message = "The aws_ssm_param_root_path value must be fully qualified (begin with `/`) if it contains any `/` characters."
  }
}

variable "project_aws_kms_key_arn" {
  type        = string
  description = "AWS KMS key ARN to use to encrypt all AWS components created by this Terraform configuration that support CMEKs. NOTE: Terraform must be authenticated as an AWS principal authorized to encrypt/decrypt with this key."
  default     = null

  validation {
    condition     = var.project_aws_kms_key_arn == null || can(regex("^arn:aws:kms:.*:\\d{12}:key\\/.*$", var.project_aws_kms_key_arn))
    error_message = "The project_aws_kms_key_arn value should be null or a valid an AWS KMS key ARN."
  }
}

variable "worklytics_host" {
  type        = string
  description = "host of worklytics instance where tenant resides. (e.g. intl.worklytics.co for prod; but may differ for dev/staging)"
  default     = "intl.worklytics.co"
}

variable "caller_gcp_service_account_ids" {
  type        = list(string)
  description = "ids of GCP service accounts allowed to send requests to the proxy (eg, unique ID of the SA of your Worklytics instance)"
  default     = []

  validation {
    condition = alltrue([
      for i in var.caller_gcp_service_account_ids : (length(regexall("^\\d{21}$", i)) > 0)
    ])
    error_message = "The values of caller_gcp_service_account_ids should be 21-digit numeric strings."
  }
}

variable "caller_aws_arns" {
  type        = list(string)
  description = "ARNs of AWS accounts allowed to send requests to the proxy (eg, arn:aws:iam::914358739851:root)"
  default     = []

  validation {
    condition = alltrue([
      for i in var.caller_aws_arns : (length(regexall("^arn:aws:iam::\\d{12}:((role|user)\\/)?\\w+$", i)) > 0)
    ])
    error_message = "The values of caller_aws_arns should be AWS Resource Names, something like 'arn:aws:iam::123123123123:root'."
  }
}


variable "connector_display_name_suffix" {
  type        = string
  description = "suffix to append to display_names of connector SAs; helpful to distinguish between various ones in testing/dev scenarios"
  default     = ""
}

variable "psoxy_base_dir" {
  type        = string
  description = "the path where your psoxy repo resides. Preferably a full path, /home/user/repos/, avoid tilde (~) shortcut to $HOME"

  validation {
    condition     = can(regex(".*\\/$", var.psoxy_base_dir))
    error_message = "The psoxy_base_dir value should end with a slash."
  }
}

variable "deployment_bundle" {
  type        = string
  description = "path to deployment bundle to use (if not provided, will build one)"
  default     = null

  validation {
    condition     = var.deployment_bundle == null || var.deployment_bundle != ""
    error_message = "`deployment_bundle`, if non-null, must be non-empty string."
  }
}

variable "force_bundle" {
  type        = bool
  description = "whether to force build of deployment bundle, even if it already exists for this proxy version"
  default     = false
}

variable "provision_testing_infra" {
  type        = bool
  description = "whether to provision infra needed to support testing of deployment"
  default     = false
}

variable "install_test_tool" {
  type        = bool
  description = "whether to install the test tool (can be 'false' if Terraform not running from a machine where you intend to run tests of your Psoxy deployment)"
  default     = true
}

variable "general_environment_variables" {
  type        = map(string)
  description = "environment variables to add for all connectors"
  default     = {}
}

variable "pseudonymize_app_ids" {
  type        = string
  description = "if set, will set value of PSEUDONYMIZE_APP_IDS environment variable to this value for all sources"
  default     = true
}

variable "enabled_connectors" {
  type        = list(string)
  description = "list of ids of connectors to enabled; see modules/worklytics-connector-specs"
}

variable "non_production_connectors" {
  type        = list(string)
  description = "connector ids in this list will be in development mode (not for production use)"
  default     = []
}

variable "bulk_input_expiration_days" {
  type        = number
  description = "Number of days after which objects in the bucket will expire. This could be as low as 1 day; longer aids debugging of issues."
  default     = 30
}

variable "bulk_sanitized_expiration_days" {
  type        = number
  description = "Number of days after which objects in the bucket will expire. In practice, Worklytics syncs data ~weekly, so 30 day minimum for this value."
  default     = 1805 # 5 years; intent is 'forever', but some upperbound in case bucket is forgotten
}

variable "custom_api_connector_rules" {
  type        = map(string)
  description = "map of connector id --> YAML file with custom rules"
  default     = {}
}

variable "custom_bulk_connectors" {
  type = map(object({
    source_kind = string
    rules = object({
      pseudonymFormat       = optional(string, "URL_SAFE_TOKEN")
      columnsToRedact       = optional(list(string)) # columns to remove from CSV
      columnsToInclude      = optional(list(string)) # if you prefer to include only an explicit list of columns, rather than redacting those you don't want
      columnsToPseudonymize = optional(list(string)) # columns to pseudonymize
      columnsToDuplicate    = optional(map(string))  # columns to create copy of; name --> new name
      columnsToRename       = optional(map(string))  # columns to rename: original name --> new name; renames applied BEFORE pseudonymization
    })
    memory_size_mb      = optional(number, null)
    settings_to_provide = optional(map(string), {})
  }))
  description = "specs of custom bulk connectors to create"

  default = {
    #    "custom-survey" = {
    #      source_kind = "survey"
    #      rules       = {
    #        columnsToRedact       = []
    #        columnsToPseudonymize = [
    #          "employee_id", # primary key
    #          # "employee_email", # if exists
    #        ]
    #      }
    #    }
  }
}

variable "custom_bulk_connector_rules" {
  type = map(object({
    pseudonymFormat       = optional(string, "URL_SAFE_TOKEN")
    columnsToRedact       = optional(list(string)) # columns to remove from CSV
    columnsToInclude      = optional(list(string)) # if you prefer to include only an explicit list of columns, rather than redacting those you don't want
    columnsToPseudonymize = optional(list(string)) # columns to pseudonymize
    columnsToDuplicate    = optional(map(string))  # columns to create copy of; name --> new name
    columnsToRename       = optional(map(string))  # columns to rename: original name --> new name; renames applied BEFORE pseudonymization
  }))

  description = "map of connector id --> rules object"
  default = {
    # hris = {
    #   columnsToRedact       = []
    #   columnsToPseudonymize = [
    #     "EMPLOYEE_ID",
    #     "EMPLOYEE_EMAIL",
    #     "MANAGER_ID",
    #     "MANAGER_EMAIL"
    #  ]
    # columnsToRename = {
    #   # original --> new
    #   "workday_id" = "employee_id"
    # }
    # columnsToInclude = [
    # ]
  }
}

variable "custom_bulk_connector_arguments" {
  type = map(object({
    memory_size_mb = optional(number)
  }))

  description = "map of connector id --> arguments object, to override defaults for bulk connector instances"
  default     = {}
}

# TODO: rethink this schema before we publish this
variable "lookup_table_builders" {
  type = map(object({
    input_connector_id            = string
    sanitized_accessor_role_names = list(string)
    rules = object({
      pseudonymFormat       = optional(string, "URL_SAFE_TOKEN")
      columnsToRedact       = optional(list(string))
      columnsToInclude      = optional(list(string))
      columnsToPseudonymize = optional(list(string))
      columnsToDuplicate    = optional(map(string))
      columnsToRename       = optional(map(string))
    })
  }))
  default = {
    #    "hris-lookup" = {
    #      input_connector_id = "hris",
    #      sanitized_accessor_role_names = [
    #        # ADD LIST OF NAMES OF YOUR AWS ROLES WHICH CAN READ LOOKUP TABLE
    #      ],
    #      rules       = {
    #        pseudonym_format = "URL_SAFE_TOKEN"
    #        columnsToRedact       = [
    #          "employee_email",
    #          "manager_id",
    #          "manager_email",
    #        ]
    #        columnsToPseudonymize = [
    #          "employee_id", # primary key
    #        ]
    #        columnsToDuplicate   = {
    #          "employee_id" = "employee_id_orig"
    #        }
    #        columnsToRename      = {}
    #        columnsToInclude     = null
    #      }
    #
    #    }
  }
}

variable "todos_as_outputs" {
  type        = bool
  description = "whether to render TODOs as outputs (former useful if you're using Terraform Cloud/Enterprise, or somewhere else where the filesystem is not readily accessible to you)"
  default     = false
}

variable "todos_as_local_files" {
  type        = bool
  description = "whether to render TODOs as flat files"
  default     = true
}
