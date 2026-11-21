-- =============================================================================
-- V8 — Rename journeys -> visits and drop the reward tables (ADR 011, ARCHITECTURE §10)
-- Owner : Lê Bá Ninh (visit)
-- ADR 011: a visit covers one heritage and there is no cross-destination "journey" entity and no rewards.
-- Earlier migrations (V3-V7) are already merged and are not edited; this file renames in place, so every row
-- and foreign key is kept. Data-neutral apart from the two dropped reward tables.
-- =============================================================================

-- ---------------------------------------------------------------- rewards ----
-- Nothing else references them (user_rewards -> rewards, user_rewards -> users only).
DROP TABLE user_rewards;
DROP TABLE rewards;

-- ---------------------------------------------------------------- tables -----
ALTER TABLE journeys                 RENAME TO visits;
ALTER TABLE journey_landmark_visits  RENAME TO visit_landmarks;

-- ---------------------------------------------------------------- columns ----
ALTER TABLE visit_landmarks  RENAME COLUMN journey_id TO visit_id;
ALTER TABLE user_stamps      RENAME COLUMN journey_id TO visit_id;
ALTER TABLE photos           RENAME COLUMN journey_id TO visit_id;
ALTER TABLE upload_sessions  RENAME COLUMN journey_id TO visit_id;
ALTER TABLE recaps           RENAME COLUMN journey_id TO visit_id;
ALTER TABLE qa_conversations RENAME COLUMN journey_id TO visit_id;
ALTER TABLE arrival_checks   RENAME COLUMN journey_id TO visit_id;

-- ------------------------------------------------- constraints (and their indexes)
ALTER TABLE visits RENAME CONSTRAINT pk_journeys                TO pk_visits;
ALTER TABLE visits RENAME CONSTRAINT fk_journeys_user           TO fk_visits_user;
ALTER TABLE visits RENAME CONSTRAINT fk_journeys_destination    TO fk_visits_destination;
ALTER TABLE visits RENAME CONSTRAINT ck_journeys_status         TO ck_visits_status;
ALTER TABLE visits RENAME CONSTRAINT ck_journeys_ended_at       TO ck_visits_ended_at;
ALTER TABLE visits RENAME CONSTRAINT ck_journeys_timeline       TO ck_visits_timeline;
ALTER TABLE visits RENAME CONSTRAINT ck_journeys_location_precision TO ck_visits_location_precision;

ALTER TABLE visit_landmarks RENAME CONSTRAINT pk_journey_landmark_visits TO pk_visit_landmarks;
ALTER TABLE visit_landmarks RENAME CONSTRAINT fk_jlv_journey             TO fk_visit_landmarks_visit;
ALTER TABLE visit_landmarks RENAME CONSTRAINT fk_jlv_landmark            TO fk_visit_landmarks_landmark;

ALTER TABLE user_stamps      RENAME CONSTRAINT fk_user_stamps_journey      TO fk_user_stamps_visit;
ALTER TABLE photos           RENAME CONSTRAINT fk_photos_journey           TO fk_photos_visit;
ALTER TABLE upload_sessions  RENAME CONSTRAINT fk_upload_sessions_journey  TO fk_upload_sessions_visit;
ALTER TABLE recaps           RENAME CONSTRAINT fk_recaps_journey           TO fk_recaps_visit;
ALTER TABLE qa_conversations RENAME CONSTRAINT fk_qa_conversations_journey TO fk_qa_conversations_visit;
ALTER TABLE arrival_checks   RENAME CONSTRAINT fk_arrival_checks_journey   TO fk_arrival_checks_visit;
-- ck_recaps_type_shape keeps its name; its expression follows the renamed column automatically.

-- ---------------------------------------------------------------- indexes ----
ALTER INDEX uq_journeys_one_open_per_user      RENAME TO uq_visits_one_open_per_user;
ALTER INDEX idx_journeys_user_started_at       RENAME TO idx_visits_user_started_at;
ALTER INDEX idx_journeys_destination_id        RENAME TO idx_visits_destination_id;
ALTER INDEX idx_jlv_landmark_id                RENAME TO idx_visit_landmarks_landmark_id;
ALTER INDEX idx_user_stamps_journey_id         RENAME TO idx_user_stamps_visit_id;
ALTER INDEX idx_photos_journey_id              RENAME TO idx_photos_visit_id;
ALTER INDEX idx_upload_sessions_journey_id     RENAME TO idx_upload_sessions_visit_id;
ALTER INDEX idx_recaps_journey_id              RENAME TO idx_recaps_visit_id;
ALTER INDEX idx_qa_conversations_journey_id    RENAME TO idx_qa_conversations_visit_id;
ALTER INDEX idx_arrival_checks_journey_id      RENAME TO idx_arrival_checks_visit_id;

-- ---------------------------------------------------------------- comments ---
COMMENT ON TABLE visits IS 'One visit to one heritage (ADR 011). At most one ACTIVE or PAUSED visit per user (uq_visits_one_open_per_user). No coordinates are stored.';
COMMENT ON TABLE visit_landmarks IS 'Landmarks checked in during a visit; one row per (visit, landmark).';
