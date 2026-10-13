terraform {
  backend "s3" {
    bucket       = "maz-terraform-state-c5eb1fe06393a9d70efdcc7e36"
    key          = "hotel-api/shared/terraform.tfstate"
    region       = "eu-west-2"
    use_lockfile = true
    encrypt      = true
  }
}