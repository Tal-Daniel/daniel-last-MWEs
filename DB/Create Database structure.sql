DROP TABLE IF EXISTS DOCUMENTS;
DROP TABLE IF EXISTS SENTENCES;
DROP TABLE IF EXISTS EXPRESSIONS;
DROP TABLE IF EXISTS EXPRESSION_OCCURANCES;
DROP TABLE IF EXISTS STATISTICS_EXPRESSION_HISTOGRAM;
DROP MATERIALIZED VIEW IF EXISTS DOCUMENTS_SUMMARY;

CREATE TABLE IF NOT EXISTS Documents (
	document_id serial PRIMARY KEY,
	title varchar(500) NOT NULL,
	author varchar(500) NOT NULL,
	year int, 
	words int,
	UNIQUE (title, author)
);
CREATE INDEX Documents_year_idx ON Documents (year);
-- ALTER TABLE Documents ADD COLUMN words int;

CREATE TABLE IF NOT EXISTS Sentences (
	sentence_id serial PRIMARY KEY, --bigserial
	sentence text NOT NULL,
-- 	sentence_number int NOT NULL,
	document_id int REFERENCES documents -- ON DELETE RESTRICT/CASACADE
-- 	, UNIQUE (document_id, sentence_number)
);
CREATE INDEX Sentences_doc_idx ON Sentences (document_id);


CREATE TABLE EXPRESSIONS (
	expression_id varchar(1000) PRIMARY KEY,
	tokens varchar(1000) NOT NULL,
	tags varchar(1000) NOT NULL
);


CREATE TABLE EXPRESSION_OCCURANCES (
	expression_id varchar(1000) REFERENCES Expressions,
	sentence_id int REFERENCES Sentences
	-- , PRIMARY KEY (expression_id, sentence_id) -- NOTE: duplicates are possible (2 x expression in same sentence)
	-- so PRIMARY KEY isn't valid here.
);
CREATE INDEX EXPRESSION_OCCURANCES_expression_idx ON EXPRESSION_OCCURANCES (expression_id);

-- TODO ...
CREATE TABLE STATISTICS_EXPRESSION_HISTOGRAM (
	expression_id varchar(1000) PRIMARY KEY REFERENCES Expressions,
	histogram_raw float ARRAY,
	histogram_relative float ARRAY
);

--ALTER TABLE EXPRESSION_OCCURANCES 
--ADD CONSTRAINT PK_expression_occurances PRIMARY KEY (expression_id, sentence_id);


-- Create Persistent VIEWS for better read performance:
-- CREATE MATERIALIZED VIEW sentences_summary AS
-- 	(SELECT expression_id, sentence_id, s.document_id, year 
-- 	FROM sentences AS s 
-- 	LEFT JOIN documents AS doc on
-- 	s.document_id = doc.document_id)
-- 	WITH DATA;

CREATE MATERIALIZED VIEW EXPRESSION_OCCURANCES_summary AS
	(SELECT expression_id, s.sentence_id, doc.year, doc.document_id
	FROM EXPRESSION_OCCURANCES AS eo 
	LEFT JOIN sentences AS s ON eo.sentence_id = s.sentence_id  
	LEFT JOIN documents AS doc ON s.document_id = doc.document_id
	ORDER BY expression_id, doc.year)
	WITH DATA;

DROP MATERIALIZED VIEW IF EXISTS DOCUMENTS_SUMMARY;
CREATE MATERIALIZED VIEW DOCUMENTS_SUMMARY AS
	(SELECT year, sum(words) AS words, count(document_id) AS documents
	FROM documents
	GROUP BY year
	ORDER BY year)
	WITH DATA;
-- TODO? create index on expression_occurances_summary?


/* Tables for Google Syntactic N-grams dataset 
 * notes: total counts are read from text file.
 */
CREATE TABLE GoogleSN_EXPRESSION_HISTOGRAMS (
	expression_id varchar(1000) PRIMARY KEY,
	counts int,
	sparsity float,
	histogram_raw float ARRAY,
	histogram_relative float ARRAY,
	kendallTauZ float
);

-- Table: googlesn_expression_histograms
-- DROP TABLE googlesn_expression_histograms;
CREATE TABLE googlesn_suspect_expression_histograms
(
  expression_id character varying(1000) NOT NULL,
  counts integer,
  sparsity double precision,
  histogram_raw double precision[],
  histogram_relative double precision[],
  kendalltauz double precision,
  CONSTRAINT googlesn_suspect_expression_histograms_pkey PRIMARY KEY (expression_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE googlesn_suspect_expression_histograms
  OWNER TO postgres;
