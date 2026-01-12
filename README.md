# SRE Playground

Hands-on SRE / Platform project built on Kubernetes, focusing on observability,
reliability, deployments, and failure scenarios.

## Local Run

Prereq: Java 17+ (tested with Java 20)

Run service:
```powershell
cd service
.\mvnw.cmd -DskipTests spring-boot:run

