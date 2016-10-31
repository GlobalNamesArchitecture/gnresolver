#!/bin/bash

while [[ "$(pg_isready -h ${DB_HOST} -U ${DB_USER})" =~ "no response" ]]; do
  echo "Waiting for postgres to start..."
  sleep 0.1
done

declare -a databases=("development" "test_api" "test_resolver")
db_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )/../db-migration" && pwd )"

cd ${db_dir}

for db in "${databases[@]}"
do
  psql -h ${DB_HOST} -U ${DB_USER} -tc \
    "SELECT 1 FROM pg_database WHERE datname = '$db'" \
    | grep -q 1 || psql -h $DB_HOST -U $DB_USER -c "CREATE DATABASE $db"

  rake db:migrate RACK_ENV=$db --trace
done

rake db:seed RACK_ENV=development --trace

cd ..

echo "Starting GNResolver API server"

if [ "$RUN_MODE" = "tests" ]; then
  sbt "~;test:compile;scalastyle;test;api/reStart"
else
  sbt "~api/reStart"
fi
