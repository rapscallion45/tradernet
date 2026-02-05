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

if ! "$JBOSS_HOME/bin/add-user.sh" --silent -e -u "${ADMIN_USERNAME}" -p "${ADMIN_PASSWORD}" >/dev/null 2>&1; then
  echo "Warning: unable to create admin user '${ADMIN_USERNAME}'." >&2
fi

configure_datasource() {
  local driver_name="$1"
  local connection_url="$2"
  local driver_class="$3"
  local driver_module="$4"

  "$JBOSS_HOME/bin/jboss-cli.sh" --connect --std-out=echo --commands=
"/subsystem=datasources/jdbc-driver=${driver_name}:read-resource,if (outcome != success) of /subsystem=datasources/jdbc-driver=${driver_name}:add(driver-name=${driver_name},driver-module-name=${driver_module},driver-class-name=${driver_class}),
/subsystem=datasources/data-source=TradernetDS:read-resource,if (outcome != success) of /subsystem=datasources/data-source=TradernetDS:add(jndi-name=java:/jdbc/TradernetDS,driver-name=${driver_name},connection-url=${connection_url},user-name=${DB_USER},password=${DB_PASSWORD},enabled=true)"
}

start_server() {
  "$JBOSS_HOME/bin/standalone.sh" -b 0.0.0.0 -bmanagement 0.0.0.0 &
  local server_pid=$!

  for _ in {1..30}; do
    if "$JBOSS_HOME/bin/jboss-cli.sh" --connect --command=":read-attribute(name=server-state)" >/dev/null 2>&1; then
      echo "WildFly management endpoint is ready."
      echo "${server_pid}"
      return
    fi
    sleep 1
  done

  echo "Error: WildFly management endpoint did not become ready in time." >&2
  kill "${server_pid}" >/dev/null 2>&1 || true
  exit 1
}

case "${DB_TYPE}" in
  POSTGRES)
    configure_datasource "postgresql" "jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" "org.postgresql.Driver" "org.postgresql"
    ;;
  *)
    echo "Unsupported DB_TYPE '${DB_TYPE}'. Supported values: POSTGRES." >&2
    exit 1
    ;;
esac

SERVER_PID="$(start_server)"
wait "${SERVER_PID}"
