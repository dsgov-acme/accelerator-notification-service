resource "google_project_service" "run" {
  project = var.gcp_project_id
  service = "run.googleapis.com"
}

resource "google_cloud_run_service" "default" {
  name = var.service_name
  location = var.gcp_region

  template {
    spec {
      containers {
        image = var.image
      }
    }
  }

  depends_on = [google_project_service.run]
}
