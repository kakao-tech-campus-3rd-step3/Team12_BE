data "aws_ssm_parameter" "db_username" {
  name = "/uni-schedule/config/db_username"
}

data "aws_ssm_parameter" "db_password" {
  name            = "/uni-schedule/config/db_password"
  with_decryption = true
}

data "aws_ssm_parameter" "db_url" {
  name = "/uni-schedule/config/db_url"
}

data "aws_ssm_parameter" "jwt_secret" {
  name = "/uni-schedule/config/jwt_secret"
}

data "aws_ssm_parameter" "jwt_access_token_timeout_sec" {
  name = "/uni-schedule/config/jwt_access_token_timeout_sec"
}

data "aws_ssm_parameter" "jwt_refresh_token_timeout_sec" {
  name = "/uni-schedule/config/jwt_refresh_token_timeout_sec"
}
