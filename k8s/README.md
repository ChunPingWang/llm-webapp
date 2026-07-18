# K8s 部署(WP7-T2)

```
k8s/
├── namespace.yaml
├── secret.example.yaml     # 範本 —— 勿提交真實金鑰;正式環境用 SealedSecrets / ExternalSecrets / Vault
├── postgres.yaml           # StatefulSet + PVC + Service
├── minio.yaml              # Deployment + PVC + Service
├── langfuse.yaml           # Deployment + Service(可觀測性,ADR-007)
├── backend.yaml            # Deployment + Service(Spring Boot,profiles: postgres,otel[,oidc])
├── frontend.yaml           # Deployment + Service(nginx 靜態)
├── ingress.yaml
└── kustomization.yaml
```

## 部署步驟

```bash
# 1) 建立 secret(金鑰只進 K8s Secret,不落 git)
kubectl -n llm-webapp create secret generic llm-webapp-secrets \
  --from-literal=ICA_API_URL="$ICA_API_URL" \
  --from-literal=ICA_CLAUDE_KEY="$ICA_CLAUDE_KEY" \
  --from-literal=POSTGRES_PASSWORD="<strong-password>"

# 2) 套用
kubectl apply -k k8s/

# 3) 建置映像(倉庫根目錄)
docker build -t <registry>/llm-webapp-backend:latest backend/
docker build -t <registry>/llm-webapp-frontend:latest frontend/
```

- OIDC:backend Deployment 設 `SPRING_PROFILES_ACTIVE=postgres,otel,oidc` 並提供
  `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`(Keycloak/Auth0/Entra)。
- Langfuse:`OTEL_EXPORTER_OTLP_ENDPOINT=http://langfuse:3000/api/public/otel` + `LANGFUSE_OTLP_AUTH` secret。
