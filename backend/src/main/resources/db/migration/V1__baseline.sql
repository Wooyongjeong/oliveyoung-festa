CREATE TABLE schema_marker (
    id INTEGER PRIMARY KEY,
    description VARCHAR(100) NOT NULL
);

INSERT INTO schema_marker (id, description)
VALUES (1, 'stage-0 baseline');
