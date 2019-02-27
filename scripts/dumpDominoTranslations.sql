select doc_id, data
from physical_translations
where physical_translation_id=m
in (select doc_id, max(physical_translation_id) from translations group by doc_id);



select t.doc_id, pt.data, MAX(pt.physical_translation_id)
from physical_translations pt
join translations t on t.physical_translation_id = pt.physical_translation_id
group by t.doc_id



select ptm.doc_id, REPLACE(pt.data , '\n', '<br/>') as translation
INTO OUTFILE '/tmp/allTransPiped.txt'
FIELDS TERMINATED BY '|'
LINES TERMINATED BY '\n'
from (select doc_id, max(physical_translation_id) as mpt from translations group by doc_id) as ptm
inner join physical_translations as pt
on pt.physical_translation_id = ptm.mpt



SELECT docs.doc_id, projects.project_name, docs.language, docs.title, docs.topic, docs.summary, docs.creator, REPLACE(physical_docs.data , '\n', '<br/>') as content
INTO OUTFILE '/tmp/allDocsPiped2.txt'
FIELDS TERMINATED BY '|'
LINES TERMINATED BY '\n'
FROM docs, current_docs, physical_docs, pools, tasks, projects
WHERE docs.doc_id = current_docs.doc_id
  AND docs.version_id = current_docs.version_id
  AND docs.physical_doc_id = physical_docs.physical_doc_id
  AND docs.physical_doc_version_id = physical_docs.version_id
  AND docs.doc_id = pools.id
  AND pools.deleted = 0
  AND pools.task_id = tasks.task_id
  AND tasks.project_id = projects.project_id
  AND projects.project_name like '%Hub%';
