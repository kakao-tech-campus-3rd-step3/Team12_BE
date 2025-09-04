data "aws_ssm_parameter" "db_username" {
  name = "/uni-schedule/config/db_username"
}

data "aws_ssm_parameter" "db_password" {
  name            = "/uni-schedule/config/db_password"
  with_decryption = true
}
