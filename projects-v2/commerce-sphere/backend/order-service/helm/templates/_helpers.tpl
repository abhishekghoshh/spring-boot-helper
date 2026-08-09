{{- define "order-service.fullname" -}}{{ .Release.Name }}-{{ .Chart.Name | trunc 63 }}{{- end -}}
{{- define "order-service.labels" -}}app: {{ .Chart.Name }}{{- end -}}
{{- define "order-service.selectorLabels" -}}app: {{ .Chart.Name }}{{- end -}}
