Global Names Resolver
=====================

Data Migration from MySQL
-------------------------

1. Configure ``${RESOLVER_ROOT}/data-migrate/src/main/resources/application.conf``
2. Create table at Cassandra:

.. code:: SQL

  CREATE KEYSPACE gni_keyspace
  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

  USE gni_keyspace;

  CREATE TABLE normalized_strings (
    id int PRIMARY KEY,
    name text,
    normalized text,
    uuid uuid,
  );

  CREATE INDEX ON normalized_strings (uuid);

3. Run ``sbt dataMigrate/run``. That would create ``name_strings.cql`` file
4. Run ``cqlsh -f ./name_strings.cql``
5. Check data at Cassandra:

.. code:: SQL

  $ cqlsh
  cqlsh> USE gni_keyspace;
  cqlsh:gni_keyspace> SELECT * FROM normalized_strings WHERE uuid = 16f235a0-e4a3-529c-9b83-bd15fe722110;

   id      | name         | normalized   | uuid
  ---------+--------------+--------------+--------------------------------------
  8219955 | Homo sapiens | HOMO SAPIENS | 16f235a0-e4a3-529c-9b83-bd15fe722110

  (1 rows)
