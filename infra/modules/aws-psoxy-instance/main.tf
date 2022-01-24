# provision a Psoxy instance into AWS account

terraform {
  required_providers {
    aws = {
      version = "~> 3.0"
    }
  }
}


# map API gateway request --> lambda function backend
resource "aws_apigatewayv2_integration" "map" {
  api_id                    = var.api_gateway.id
  integration_type          = "AWS_PROXY"
  connection_type           = "INTERNET"

  integration_method        = "POST"
  integration_uri           = aws_lambda_function.psoxy-instance.invoke_arn
  request_parameters        = {}
  request_templates         = {}
  payload_format_version    = "2.0"
}


resource "aws_apigatewayv2_route" "example" {
  api_id             = var.api_gateway.id
  route_key          = "GET /${var.function_name}/{proxy+}"
  authorization_type = "AWS_IAM"
  target             = "integrations/${aws_apigatewayv2_integration.map.id}"
}

# allow API gateway to invoke the lambda function
resource "aws_lambda_permission" "lambda_permission" {
  statement_id  = "Allow${var.function_name}Invoke"
  action        = "lambda:InvokeFunction"
  function_name = var.function_name
  principal     = "apigateway.amazonaws.com"

  # The /*/*/* part allows invocation from any stage, method and resource path
  # within API Gateway REST API.
  source_arn = "${var.api_gateway.arn}/*/*/*"

  depends_on = [
    aws_lambda_function.psoxy-instance
  ]
}

locals {
  proxy_endpoint_url = "${var.api_gateway.api_endpoint}/live/${var.function_name}"
}

resource "aws_iam_role" "iam_for_lambda" {
  name = "iam_for_lambda_${var.function_name}"

  assume_role_policy = jsonencode({
    "Version": "2012-10-17",
    "Statement": [
      {
        "Action": "sts:AssumeRole",
        "Principal": {
          "Service": "lambda.amazonaws.com"
        },
        "Effect": "Allow",
        "Sid": ""
      }
    ]
  })
}

resource "aws_lambda_function" "psoxy-instance" {
  function_name    = var.function_name
  role             = aws_iam_role.iam_for_lambda.arn
  handler          = "co.worklytics.psoxy.Handler"
  runtime          = "java11"
  filename         = var.path_to_function_zip
  source_code_hash = var.function_zip_hash
  timeout          = 55 # seconds
  memory_size      = 512 # megabytes

  environment {
    # NOTE: can use merge() to combine var map from config with additional values
    variables = yamldecode(file(var.path_to_config))
  }
}

resource "aws_iam_policy" "policy" {
  name        = "${var.function_name}_ssmGetParameters"
  description = "Allow lambda function role to read SSM parameters"

  policy = jsonencode(
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "ssm:GetParameter*"
      ],
      "Effect": "Allow",
      "Resource": "*"
      # TODO: limit to SSM parameters in question
      # "Resource": "arn:aws:ssm:us-east-2:123456789012:parameter/prod-*"
    }
  ]
})

}


resource "aws_iam_role_policy_attachment" "basic" {
  role       = aws_iam_role.iam_for_lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "policy"{
  role       = aws_iam_role.iam_for_lambda.name
  policy_arn = aws_iam_policy.policy.arn
}


resource "local_file" "todo" {
  filename = "test ${var.function_name}.md"
  content  = <<EOT

## Testing

Review the deployed function in AWS console:

- https://console.aws.amazon.com/lambda/home?region=${var.region}#/functions/${var.function_name}?tab=monitoring

### Prereqs
Requests to AWS API need to be [signed](https://docs.aws.amazon.com/general/latest/gr/signing_aws_api_requests.html).
One tool to do it easily is [awscurl](https://github.com/okigan/awscurl). Install it:

```shell
pip install awscurl
```

### From Terminal

From root of your checkout of the Psoxy repo:

```shell
./tools/test-psoxy-lambda.sh "${var.aws_assume_role_arn}" "${local.proxy_endpoint_url}/admin/directory/v1/customer/my_customer/domains"
```

See `docs/example-api-calls/` for more example API calls specific to the data source to which your
Proxy is configured to connect.

EOT
}

output "endpoint_url" {
  value = local.proxy_endpoint_url
}