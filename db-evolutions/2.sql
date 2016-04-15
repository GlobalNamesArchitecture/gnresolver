--- !Ups

CREATE TABLE data_sources (
  id SERIAL,
  title varchar(255) DEFAULT NULL,
  description text,
  logo_url varchar(255)  DEFAULT NULL,
  web_site_url varchar(255)  DEFAULT NULL,
  data_url varchar(255)  DEFAULT NULL,
  refresh_period_days integer DEFAULT '14',
  name_strings_count integer DEFAULT '0',
  data_hash varchar(40)  DEFAULT NULL,
  unique_names_count integer DEFAULT '0',
  created_at TIMESTAMP DEFAULT NULL,
  updated_at TIMESTAMP DEFAULT NULL
);

-- todo: synonym should be not null
CREATE TABLE name_string_indices (
  data_source_id integer NOT NULL,
  name_string_id integer NOT NULL,
  taxon_id varchar(255) NOT NULL DEFAULT '',
  global_id varchar(255) DEFAULT NULL,
  url varchar(255) DEFAULT NULL,
  rank varchar(255) DEFAULT NULL,
  accepted_taxon_id varchar(255) DEFAULT NULL,
  synonym varchar(20)[],
  check (synonym <@ ARRAY['synonym','lexical','homotypic','heterotypic']::varchar[]),
  classification_path text ,
  classification_path_ids text ,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  nomenclatural_code_id integer DEFAULT NULL,
  local_id varchar(255) DEFAULT NULL,
  classification_path_ranks text
);

CREATE TABLE name_strings_author_words (
  author_word VARCHAR(100),
  name_uuid UUID
);

CREATE TABLE name_strings_year (
  year VARCHAR(8),
  name_uuid UUID
);

CREATE TABLE name_strings_genus (
  genus VARCHAR(50),
  name_uuid UUID
);

CREATE TABLE name_strings_uninomial (
  uninomial VARCHAR(50),
  name_uuid UUID
);

CREATE TABLE name_strings_species (
  species VARCHAR(50),
  name_uuid UUID
);

CREATE TABLE name_strings_subspecies (
  subspecies VARCHAR(50),
  name_uuid UUID
);

CREATE INDEX name_strings_author_words_index ON
  name_strings_author_words USING BTREE (author_word);
CREATE INDEX name_strings_year_index ON
  name_strings_year USING BTREE (year);
CREATE INDEX name_strings_genus_index ON
  name_strings_genus USING BTREE (genus);
CREATE INDEX name_strings_uninomial_index ON
  name_strings_uninomial USING BTREE (uninomial);
CREATE INDEX name_strings_species_index ON
  name_strings_species USING BTREE (species);
CREATE INDEX name_strings_subspecies_index ON
  name_strings_subspecies USING BTREE (subspecies);

--- !Downs

DROP TABLE data_sources;

DROP TABLE name_string_indices;

DROP TABLE name_strings_author_words;

DROP TABLE name_strings_year;

DROP TABLE name_strings_genus;

DROP TABLE name_strings_uninomial;

DROP TABLE name_strings_subspecies;
