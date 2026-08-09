{{- define "inventory-service.fullname" -}}{{ .Release.Name }}-{{ .Chart.Name | trunc 63 }}{{- end -}}
{{- define "inventory-service.labels" -}}app: {{ .Chart.Name }}{{- end -}}
{{- define "inventory-service.selectorLabels" -}}app: {{ .Chart.Name }}{{- end -}}
