--- !Ups

CREATE TABLE name_strings
(
  id uuid primary key UNIQUE,
  id_mysql uuid UNIQUE,
  name character varying(255) NOT NULL,
  canonical_uuid uuid DEFAULT NULL,
  canonical character varying(255) NOT NULL
);

--- !Downs

DROP TABLE name_strings;
