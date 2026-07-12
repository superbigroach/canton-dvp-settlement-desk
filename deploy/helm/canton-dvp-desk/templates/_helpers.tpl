{{/* Common naming + label helpers. */}}

{{- define "canton-dvp-desk.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "canton-dvp-desk.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "canton-dvp-desk.labels" -}}
app.kubernetes.io/name: {{ include "canton-dvp-desk.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
{{- end -}}

{{- define "canton-dvp-desk.selectorLabels" -}}
app.kubernetes.io/name: {{ include "canton-dvp-desk.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "canton-dvp-desk.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "canton-dvp-desk.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}

{{/* Name of the Secret holding the JWT (existing one if provided). */}}
{{- define "canton-dvp-desk.jwtSecretName" -}}
{{- if .Values.ledger.jwt.existingSecret -}}
{{- .Values.ledger.jwt.existingSecret -}}
{{- else -}}
{{- printf "%s-jwt" (include "canton-dvp-desk.fullname" .) -}}
{{- end -}}
{{- end -}}

{{/* True when a JWT should be wired in (inline value or an existing secret). */}}
{{- define "canton-dvp-desk.jwtEnabled" -}}
{{- if or .Values.ledger.jwt.existingSecret .Values.ledger.jwt.value -}}true{{- else -}}false{{- end -}}
{{- end -}}
