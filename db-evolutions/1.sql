# --- !Ups

CREATE TABLE gni.name_strings
(
   id uuid primary key UNIQUE,
   name character varying(255) NOT NULL,
   normalized character varying(255) NOT NULL
);

# --- !Downs

DROP TABLE gni.name_strings;