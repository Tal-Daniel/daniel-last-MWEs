/*--
DELETE FROM googlesn_expression_histograms;
DELETE FROM googlesn_suspect_expression_histograms;
--*/

-- 
SELECT count(expression_id)
  FROM googlesn_expression_histograms;

SELECT count(expression_id)
  FROM googlesn_suspect_expression_histograms;

SELECT DISTINCT expression_id
  FROM googlesn_suspect_expression_histograms;

-- TOP 100
SELECT *
  FROM googlesn_expression_histograms
  WHERE sparsity < .2
  ORDER BY kendalltauz DESC
  LIMIT 100;

-- BOTTOM 100
SELECT *
  FROM googlesn_expression_histograms
  WHERE sparsity < .2
  ORDER BY kendalltauz ASC
  LIMIT 100;


-- Finding synonyms to trendy MWEs:
SELECT * FROM googlesn_expression_histograms
WHERE expression_id LIKE 'well_%';
SELECT * FROM googlesn_expression_histograms
WHERE expression_id LIKE 'exemp%';

SELECT * FROM googlesn_expression_histograms
WHERE expression_id LIKE 'looking_%';

SELECT * FROM googlesn_expression_histograms
WHERE expression_id LIKE 'hand%';

SELECT * FROM googlesn_expression_histograms
WHERE expression_id LIKE '%deed%';

SELECT * FROM googlesn_expression_histograms
WHERE expression_id LIKE '%actu%';

-- in fact synonyms?
SELECT kendalltauz, sparsity, counts, expression_id, histogram_raw, histogram_relative
FROM googlesn_expression_histograms
WHERE 
	--increasing TOP 30 synonyms 
	expression_id LIKE 'in_truth%' 
	OR expression_id LIKE 'in_effect%'
	OR expression_id LIKE '%_a_truth%'
	OR expression_id LIKE 'of_truth%'
	OR expression_id LIKE 'in_esse%'
	-- decreasing trends:
	OR expression_id LIKE 'thereafter%' 
	OR expression_id LIKE 'under_the%'
	OR expression_id LIKE 'dead_and%'
	OR expression_id LIKE 'six_feet%'
	OR expression_id LIKE 'in_esse%'
	OR expression_id LIKE 'truce%' 
	OR expression_id LIKE 'well-meaningness%'
	OR expression_id LIKE 'well_dispose%'
	OR expression_id LIKE 'well_nature'
	OR expression_id LIKE 'well_intention%'
	OR expression_id LIKE '%same_time%'
	OR expression_id LIKE '%the_same%'
	OR expression_id LIKE '%all_the_same%'
	OR expression_id LIKE '%still_and_all%'
	OR expression_id LIKE '%still_an%'
	OR expression_id LIKE '%the_same%'
	OR expression_id LIKE '%law_of_reason%'
	OR expression_id LIKE 'natural_law%'

  -- WHERE expression_id LIKE '%namely%';
  -- WHERE expression_id LIKE '%truce%';
  -- WHERE expression_id LIKE '%all_if%';

 thereafter as