SELECT projects.project_name, docs.language, docs.title, docs.topic, docs.summary, docs.creator, physical_docs.data
FROM docs, current_docs, physical_docs, pools, tasks, projects, ph
WHERE docs.doc_id = current_docs.doc_id
   AND docs.version_id = current_docs.version_id
   AND docs.physical_doc_id = physical_docs.physical_doc_id
   AND docs.physical_doc_version_id = physical_docs.version_id
   AND docs.doc_id = pools.id
   AND pools.deleted = 0
   AND pools.task_id = tasks.task_id
   AND tasks.project_id = projects.project_id
   AND projects.project_name like '%Hub%';