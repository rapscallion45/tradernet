#!/usr/bin/env bash
set -euo pipefail

JBOSS_HOME="${JBOSS_HOME:-/opt/jboss/wildfly}"
DB_TYPE="${DB_TYPE:-H2}"
DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-tradernet}"
DB_USER="${DB_USER:-sa}"
DB_PASSWORD="${DB_PASSWORD:-}"
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

wait_for_db() {
  log "Waiting for database at ${DB_HOST}:${DB_PORT}."

  # DNS sanity check (helps diagnose compose/network issues)
  if command -v getent >/dev/null 2>&1; then
    if ! getent hosts "${DB_HOST}" >/dev/null 2>&1; then
      log "DNS resolution failed for DB_HOST='${DB_HOST}'. Are both containers started via the same docker compose project/network?"
      return 1
    fi
    log "DNS resolution OK for DB_HOST='${DB_HOST}'."
  else
    log "getent not available; skipping DNS check."
  fi

  for i in {1..120}; do
    # Use exec + fd so we can reliably see failures and not hide them in a subshell.
    if exec 3<>"/dev/tcp/${DB_HOST}/${DB_PORT}" 2>/dev/null; then
      exec 3<&- 3>&-
      log "Database port is reachable (attempt ${i})."
      return 0
    fi
    log "Database not reachable yet (attempt ${i})."
    sleep 1
  done

  log "Error: database did not become reachable in time."
  return 1
}

start_server() {
  log "Starting WildFly server."
  "$JBOSS_HOME/bin/standalone.sh" -b 0.0.0.0 -bmanagement 0.0.0.0 &
  SERVER_PID=$!

  for _ in {1..60}; do
    if "$JBOSS_HOME/bin/jboss-cli.sh" --connect --command=":read-attribute(name=server-state)" 2>/dev/null | grep -q running; then
      log "WildFly is running."
      return 0
    fi
    sleep 1
  done

  log "Error: WildFly did not reach RUNNING in time."
  kill "${SERVER_PID}" >/dev/null 2>&1 || true
  exit 1
}

configure_datasource() {
  local driver_name="$1"
  local connection_url="$2"
  local driver_class="$3"
  local driver_module="$4"
  local db_user="${5:-$DB_USER}"
  local db_password="${6:-$DB_PASSWORD}"
  local password_clause

  local cli_output

  # Add driver if missing
  if ! "$JBOSS_HOME/bin/jboss-cli.sh" --connect --command="/subsystem=datasources/jdbc-driver=${driver_name}:read-resource" >/dev/null 2>&1; then
    if ! cli_output="$("$JBOSS_HOME/bin/jboss-cli.sh" --connect --command="/subsystem=datasources/jdbc-driver=${driver_name}:add(driver-name=${driver_name},driver-module-name=${driver_module},driver-class-name=${driver_class})" 2>&1)"; then
      echo "Error: failed to add JDBC driver '${driver_name}'." >&2
      echo "${cli_output}" >&2
      return 1
    fi
    echo "${cli_output}"
  fi

  # Add datasource if missing
  if ! "$JBOSS_HOME/bin/jboss-cli.sh" --connect --command="/subsystem=datasources/data-source=TradernetDS:read-resource" >/dev/null 2>&1; then
    if [[ -n "${db_password}" ]]; then
      password_clause=",password=${db_password}"
    else
      password_clause=""
    fi
    if ! cli_output="$("$JBOSS_HOME/bin/jboss-cli.sh" --connect --command="/subsystem=datasources/data-source=TradernetDS:add(jndi-name=java:/jdbc/TradernetDS,driver-name=${driver_name},connection-url=${connection_url},user-name=${db_user}${password_clause},enabled=true)" 2>&1)"; then
      echo "Error: failed to add TradernetDS datasource." >&2
      echo "${cli_output}" >&2
      return 1
    fi
    echo "${cli_output}"
  fi

  # Test connection explicitly (fail fast)
  if ! cli_output="$("$JBOSS_HOME/bin/jboss-cli.sh" --connect --command="/subsystem=datasources/data-source=TradernetDS:test-connection-in-pool" 2>&1)"; then
    echo "Error: TradernetDS test-connection-in-pool failed." >&2
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
  local pending_dir="${deployments_dir}.pending"
  local deployment_path="${pending_dir}/tradernet.ear"

  if [[ ! -f "${deployment_path}" ]]; then
    log "Error: expected deployment artifact at ${deployment_path}."
    return 1
  fi

  # Deploy via CLI for determinism (no scanner timing games)
  if ! "$JBOSS_HOME/bin/jboss-cli.sh" --connect --command="deploy ${deployment_path} --force"; then
    log "Error: failed to deploy ${deployment_path}."
    return 1
  fi
}

# --- Main flow ---
start_server
log "WildFly server PID is ${SERVER_PID}."

log "Configuring datasources."

case "${DB_TYPE}" in
  H2)
    log "Configuring TradernetDS datasource for H2."
    configure_datasource "h2" "jdbc:h2:mem:${DB_NAME};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE" "org.h2.Driver" "com.h2database.h2" "sa" ""
    ensure_datasource
    ;;
  POSTGRES)
    log "Configuring TradernetDS datasource for PostgreSQL."
    if ! wait_for_db; then
      kill "${SERVER_PID}" >/dev/null 2>&1 || true
      exit 1
    fi
    configure_datasource "postgresql" "jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" "org.postgresql.Driver" "org.postgresql"
    ensure_datasource
    ;;
  *)
    echo "Unsupported DB_TYPE '${DB_TYPE}'. Supported values: H2, POSTGRES." >&2
    kill "${SERVER_PID}" >/dev/null 2>&1 || true
    exit 1
    ;;
esac

log "Deploying Tradernet EAR."
if ! deploy_artifacts; then
  kill "${SERVER_PID}" >/dev/null 2>&1 || true
  exit 1
fi

log "Entry script complete; waiting on WildFly process."
wait "${SERVER_PID}"
