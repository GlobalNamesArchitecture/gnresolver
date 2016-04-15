--- !Ups

CREATE TABLE name_strings
(
  id uuid primary key UNIQUE,
  id_mysql uuid,
  name character varying(255) NOT NULL,
  canonical_uuid uuid DEFAULT NULL,
  canonical character varying(255) NOT NULL
);

CREATE INDEX name_strings_index_canonical_uuid ON
  name_strings USING BTREE (canonical_uuid);

--- !Downs

DROP TABLE name_strings;
