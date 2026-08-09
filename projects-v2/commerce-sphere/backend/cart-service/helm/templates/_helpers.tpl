{{- define "cart-service.fullname" -}}{{ .Release.Name }}-{{ .Chart.Name | trunc 63 }}{{- end -}}
{{- define "cart-service.labels" -}}app: {{ .Chart.Name }}{{- end -}}
{{- define "cart-service.selectorLabels" -}}app: {{ .Chart.Name }}{{- end -}}
