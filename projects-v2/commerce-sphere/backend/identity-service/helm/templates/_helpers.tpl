{{- define "identity-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "identity-service.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "identity-service.name" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "identity-service.labels" -}}
app.kubernetes.io/name: {{ include "identity-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "identity-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "identity-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
