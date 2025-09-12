terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.11"
    }
  }

  backend "s3" {
    bucket       = "uni-schedule-remote-state-bucket"
    key          = "terraform/state"
    region       = "ap-northeast-2"
    use_lockfile = true
  }
}
