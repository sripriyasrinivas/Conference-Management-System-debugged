-- Link sessions to "International Conference on Computer Science 2026"
-- This file runs after Hibernate creates the schema
UPDATE sessions SET conference_id = (SELECT id FROM conferences WHERE name LIKE '%International Conference on Computer Science%' LIMIT 1) WHERE title = 'Keynote: The Future of Cloud Computing';
UPDATE sessions SET conference_id = (SELECT id FROM conferences WHERE name LIKE '%International Conference on Computer Science%' LIMIT 1) WHERE title = 'Deep Learning for NLP - Paper Presentation';
