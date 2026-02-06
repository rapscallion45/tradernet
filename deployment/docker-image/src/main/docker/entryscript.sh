#!/usr/bin/env bash
set -euo pipefail

JBOSS_HOME="${JBOSS_HOME:-/opt/jboss/wildfly}"
DB_TYPE="${DB_TYPE:-POSTGRES}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-tradernet}"
DB_USER="${DB_USER:-tradernet}"
DB_PASSWORD="${DB_PASSWORD:-tradernet}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-ChangeMe}"

log() {
  printf '[entryscript] %s\n' "$*" >&2
}

set -x
trap 'log "Exiting with status $?."' EXIT
trap 'log "Error on line ${LINENO}."' ERR

log "Starting Tradernet entry script."

if ! "$JBOSS_HOME/bin/add-user.sh" --silent -e -u "${ADMIN_USERNAME}" -p "${ADMIN_PASSWORD}" >/dev/null 2>&1; then
  echo "Warning: unable to create admin user '${ADMIN_USERNAME}'." >&2
fi

configure_datasource() {
  local driver_name="$1"
  local connection_url="$2"
  local driver_class="$3"
  local driver_module="$4"

  local cli_output
  if ! cli_output="$("$JBOSS_HOME/bin/jboss-cli.sh" --connect --commands="/subsystem=datasources/jdbc-driver=${driver_name}:read-resource,if (outcome != success) of /subsystem=datasources/jdbc-driver=${driver_name}:add(driver-name=${driver_name},driver-module-name=${driver_module},driver-class-name=${driver_class}),/subsystem=datasources/data-source=TradernetDS:read-resource,if (outcome != success) of /subsystem=datasources/data-source=TradernetDS:add(jndi-name=java:/jdbc/TradernetDS,driver-name=${driver_name},connection-url=${connection_url},user-name=${DB_USER},password=${DB_PASSWORD},enabled=true)" 2>&1)"; then
    echo "Error: failed to configure TradernetDS datasource." >&2
    echo "${cli_output}" >&2
    return 1
  fi

  echo "${cli_output}"
}

ensure_datasource() {
  local cli_output
  if ! cli_output="$("$JBOSS_HOME/bin/jboss-cli.sh" --connect --command="/subsystem=datasources/data-source=TradernetDS:read-resource" 2>&1)"; then
    echo "Error: TradernetDS datasource is not available after configuration." >&2
    echo "${cli_output}" >&2
    echo "Datasource subsystem snapshot:" >&2
    "$JBOSS_HOME/bin/jboss-cli.sh" --connect --command="/subsystem=datasources:read-resource(recursive=true)" >&2 || true
    kill "${SERVER_PID}" >/dev/null 2>&1 || true
    exit 1
  fi
}

deploy_artifacts() {
  local deployments_dir="${JBOSS_HOME}/standalone/deployments"
  local deployment

  for deployment in tradernet.ear; do
    if [[ -f "${deployments_dir}/${deployment}.failed" ]]; then
      rm -f "${deployments_dir}/${deployment}.failed"
    fi
    touch "${deployments_dir}/${deployment}.dodeploy"
  done
}

start_server() {
  log "Starting WildFly server."
  "$JBOSS_HOME/bin/standalone.sh" -b 0.0.0.0 -bmanagement 0.0.0.0 &
  local server_pid=$!

  for _ in {1..30}; do
    if "$JBOSS_HOME/bin/jboss-cli.sh" --connect --command=":read-attribute(name=server-state)" >/dev/null 2>&1; then
      log "WildFly management endpoint is ready."
      echo "${server_pid}"
      return
    fi
    sleep 1
  done

  log "Error: WildFly management endpoint did not become ready in time."
  kill "${server_pid}" >/dev/null 2>&1 || true
  exit 1
}

SERVER_PID="$(start_server)"
log "WildFly server PID is ${SERVER_PID}."

log "Configuring datasources."

case "${DB_TYPE}" in
  POSTGRES)
    log "Configuring TradernetDS datasource for PostgreSQL."
    configure_datasource "postgresql" "jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" "org.postgresql.Driver" "org.postgresql"
    ensure_datasource
    ;;
  *)
    echo "Unsupported DB_TYPE '${DB_TYPE}'. Supported values: POSTGRES." >&2
    kill "${SERVER_PID}" >/dev/null 2>&1 || true
    exit 1
    ;;
esac

log "Deploying Tradernet EAR."
deploy_artifacts

log "Entry script complete; waiting on WildFly process."
wait "${SERVER_PID}"
