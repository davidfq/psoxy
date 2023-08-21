# create a connection from Worklytics to a Psoxy instance hosted in AWS
#
# kept as a 'strong' interface for AWS-case; enforces that the caller must provide the AWS-specific
# settings (role ARN, region, etc.)

module "generic" {
  source = "../worklytics-psoxy-connection-generic"

  psoxy_instance_id      = var.psoxy_instance_id
  psoxy_host_platform_id = "AWS"
  todo_step              = var.todo_step
  display_name           = var.display_name
  worklytics_host        = var.worklytics_host
  connector_id           = var.connector_id


  settings_to_provide = merge(
    var.psoxy_endpoint_url == null ? {} : { "Psoxy Base URL" = var.psoxy_endpoint_url },
    # Source Bucket (bulk file) case
    var.bucket_name == null ? {} : { "Bucket Name" = var.bucket_name },
    {
      "AWS Psoxy Role ARN" = var.aws_role_arn,
      "AWS Psoxy Region"   = var.aws_region,
    },
    var.connector_settings_to_provide
  )

}

output "next_todo_step" {
  value = module.generic.next_todo_step
}

output "todo" {
  value = module.generic.todo
}
