-- :name insert-statement
-- :command :insert
-- :result :affected
-- :doc Insert a new statement.
INSERT INTO xapi_statement SET
id = :primary-key,
statement_id = :statement-id,
sub_statement_id = :?sub-statement-id,
statement_ref_id = :?statement-ref-id,
created = :timestamp,
stored = :stored,
registration = :?registration,
verb_iri = :verb-iri,
is_voided = :voided?,
payload = :payload

-- :name insert-agent
-- :command :insert
-- :result :affected
-- :doc Insert a new agent.
INSERT INTO agent SET 
id = :primary-key,
agent_name = :?name,
agent_ifi = :agent-ifi,
is_identified_group = :identified-group?

-- :name insert-activity
-- :command :insert
-- :result :affected
-- :doc Insert a new activity.
INSERT INTO activity SET 
id = :primary-key,
activity_iri = :activity-iri,
payload = :payload

-- :name insert-attachment,
-- :command :insert,
-- :result :affected
-- :doc Insert a new attachment
INSERT INTO attachment SET 
id = :primary-key,
attachment_sha = :attachment-sha,
content_type = :content-type,
file_url = :file-url,
payload = :payload

-- :name insert-statement-to-agent
-- :command :insert
-- :result :affected
-- :doc Insert a new statement-to-agent relation.
INSERT INTO statement_to_agent SET 
id = :primary-key,
statement_id = :statement-id,
usage = :usage,
agent_ifi = :agent-ifi

-- :name insert-statement-to-activity
-- :command :insert
-- :result :affected
-- :doc Insert a new statement-to-activity relation.
INSERT INTO statement_to_activity SET
id = :primary-key,
statement_id = :statement-id,
usage = :usage,
activity_iri = :activity-iri

-- :name insert-statement-to-attachment
-- :command :insert
-- :result :affected
-- :doc Insert a new statement-to-attachment relation.
INSERT INTO statement_to_attachment SET
id = :primary-key,
statement_id = :statement-id,
attachment_sha = :attachment-sha
