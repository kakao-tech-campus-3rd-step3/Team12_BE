resource "aws_iam_user" "user" {
  name = "uni-schedule-user"
}

resource "aws_iam_user_policy_attachment" "admin_policy_attach" {
  user       = aws_iam_user.user.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}
