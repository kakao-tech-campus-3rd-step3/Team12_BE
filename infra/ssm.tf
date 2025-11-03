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
  name            = "/uni-schedule/config/jwt_secret"
  with_decryption = true
}

data "aws_ssm_parameter" "jwt_access_token_timeout_sec" {
  name = "/uni-schedule/config/jwt_access_token_timeout_sec"
}

data "aws_ssm_parameter" "jwt_refresh_token_timeout_sec" {
  name = "/uni-schedule/config/jwt_refresh_token_timeout_sec"
}

data "aws_ssm_parameter" "openai_api_key" {
  name            = "/uni-schedule/config/openai_api_key"
  with_decryption = true
}

data "aws_ssm_parameter" "redis_host" {
  name = "/uni-schedule/config/redis_host"
}

data "aws_ssm_parameter" "redis_password" {
  name            = "/uni-schedule/config/redis_password"
  with_decryption = true
}

data "aws_ssm_parameter" "redis_username" {
  name = "/uni-schedule/config/redis_username"
}

data "aws_ssm_parameter" "mail_username" {
  name            = "/uni-schedule/mail/username"
  with_decryption = true
}

data "aws_ssm_parameter" "mail_password" {
  name            = "/uni-schedule/mail/password"
  with_decryption = true
}

data "aws_ssm_parameter" "google_client_id" {
  name            = "/uni-schedule/calendar/google_client_id"
  with_decryption = true
}

data "aws_ssm_parameter" "google_client_secret" {
  name            = "/uni-schedule/calendar/google_client_secret"
  with_decryption = true
}

data "aws_ssm_parameter" "google_redirect_url" {
  name = "/uni-schedule/calendar/google_redirect_url"
}
