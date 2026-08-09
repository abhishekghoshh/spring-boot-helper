{{- define "catalog-service.fullname" -}}{{ .Release.Name }}-{{ .Chart.Name | trunc 63 }}{{- end -}}
{{- define "catalog-service.labels" -}}app: {{ .Chart.Name }}{{- end -}}
{{- define "catalog-service.selectorLabels" -}}app: {{ .Chart.Name }}{{- end -}}
