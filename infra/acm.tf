resource "aws_acm_certificate" "this" {
  domain_name       = "*.uni-schedule.com"
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_acm_certificate_validation" "this" {
  certificate_arn = aws_acm_certificate.this.arn
  validation_record_fqdns = [
    for record in aws_route53_record.cert_validation : record.fqdn
  ]
}
