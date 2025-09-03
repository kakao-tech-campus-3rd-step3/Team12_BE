data "aws_ssm_parameter" "db_username" {
  name = "/uni-schedule/config/db_username"
}

output "db_username_value" {
  value     = data.aws_ssm_parameter.db_username.value
  sensitive = true
}

data "aws_ssm_parameter" "db_password" {
  name            = "/uni-schedule/config/db_password"
  with_decryption = true
}

output "db_password_value" {
  value     = data.aws_ssm_parameter.db_password.value
  sensitive = true
}

data "aws_ssm_parameter" "account_id" {
  name = "/user-config/account-id"
}

output "account_id_value" {
  value     = data.aws_ssm_parameter.account_id.value
  sensitive = true
}
