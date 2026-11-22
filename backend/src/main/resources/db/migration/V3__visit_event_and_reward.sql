-- =============================================================================
-- V3 — Events, journeys (visits), stamps & rewards
-- Owner : Lê Bá Ninh (visit, event)
-- Tables: cultural_events, journeys, journey_landmark_visits,
--         stamps, user_stamps, rewards, user_rewards
-- =============================================================================

CREATE TABLE cultural_events (
    id             UUID         NOT NULL,
    destination_id UUID,
    title          VARCHAR(240) NOT NULL,
    description    TEXT,
    starts_at      TIMESTAMPTZ  NOT NULL,
    ends_at        TIMESTAMPTZ  NOT NULL,
    venue_name     VARCHAR(200) NOT NULL,
    source_url     TEXT         NOT NULL,
    extracted_at   TIMESTAMPTZ  NOT NULL,
    status         VARCHAR(16)  NOT NULL,
    CONSTRAINT pk_cultural_events PRIMARY KEY (id),
    CONSTRAINT fk_cultural_events_destination
        FOREIGN KEY (destination_id) REFERENCES destinations (id) ON DELETE SET NULL,
    CONSTRAINT ck_cultural_events_status
        CHECK (status IN ('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT ck_cultural_events_period CHECK (ends_at >= starts_at)
);

-- Public feed: approved events ordered by start time.
CREATE INDEX idx_cultural_events_status_starts_at ON cultural_events (status, starts_at);
CREATE INDEX idx_cultural_events_destination_id  ON cultural_events (destination_id) WHERE destination_id IS NOT NULL;

COMMENT ON COLUMN cultural_events.destination_id IS 'NULL = city-wide event.';
COMMENT ON COLUMN cultural_events.status IS 'Imported events start as PENDING_REVIEW and are never auto-published.';

-- -----------------------------------------------------------------------------

CREATE TABLE journeys (
    id             UUID        NOT NULL,
    user_id        UUID        NOT NULL,
    destination_id UUID        NOT NULL,
    status         VARCHAR(16) NOT NULL,
    started_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    paused_at      TIMESTAMPTZ,
    ended_at       TIMESTAMPTZ,
    CONSTRAINT pk_journeys PRIMARY KEY (id),
    CONSTRAINT fk_journeys_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_journeys_destination
        FOREIGN KEY (destination_id) REFERENCES destinations (id),
    CONSTRAINT ck_journeys_status CHECK (status IN ('ACTIVE', 'PAUSED', 'ENDED')),
    CONSTRAINT ck_journeys_ended_at CHECK ((status = 'ENDED') = (ended_at IS NOT NULL)),
    CONSTRAINT ck_journeys_timeline CHECK (
        (paused_at IS NULL OR paused_at >= started_at)
        AND (ended_at IS NULL OR ended_at >= started_at)
    )
);

-- At most one open (ACTIVE or PAUSED) journey per user (ARCHITECTURE §10, SECURITY TM-4.2 -> 409 ACTIVE_VISIT_EXISTS).
CREATE UNIQUE INDEX uq_journeys_one_open_per_user ON journeys (user_id) WHERE status IN ('ACTIVE', 'PAUSED');
CREATE INDEX idx_journeys_user_started_at ON journeys (user_id, started_at DESC);
CREATE INDEX idx_journeys_destination_id  ON journeys (destination_id);

-- -----------------------------------------------------------------------------

CREATE TABLE journey_landmark_visits (
    journey_id  UUID        NOT NULL,
    landmark_id UUID        NOT NULL,
    visited_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_journey_landmark_visits PRIMARY KEY (journey_id, landmark_id),
    CONSTRAINT fk_jlv_journey
        FOREIGN KEY (journey_id) REFERENCES journeys (id) ON DELETE CASCADE,
    CONSTRAINT fk_jlv_landmark
        FOREIGN KEY (landmark_id) REFERENCES landmarks (id)
);

CREATE INDEX idx_jlv_landmark_id ON journey_landmark_visits (landmark_id);

-- -----------------------------------------------------------------------------

CREATE TABLE stamps (
    id             UUID         NOT NULL,
    destination_id UUID         NOT NULL,
    title          VARCHAR(160) NOT NULL,
    icon_asset_url TEXT,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_stamps PRIMARY KEY (id),
    CONSTRAINT uq_stamps_destination UNIQUE (destination_id),   -- one stamp per destination
    CONSTRAINT fk_stamps_destination
        FOREIGN KEY (destination_id) REFERENCES destinations (id)
);

CREATE TABLE user_stamps (
    user_id    UUID        NOT NULL,
    stamp_id   UUID        NOT NULL,
    journey_id UUID        NOT NULL,
    awarded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_stamps PRIMARY KEY (user_id, stamp_id),
    CONSTRAINT fk_user_stamps_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_user_stamps_stamp   FOREIGN KEY (stamp_id)   REFERENCES stamps (id),
    CONSTRAINT fk_user_stamps_journey FOREIGN KEY (journey_id) REFERENCES journeys (id)
);

CREATE INDEX idx_user_stamps_stamp_id   ON user_stamps (stamp_id);
CREATE INDEX idx_user_stamps_journey_id ON user_stamps (journey_id);

-- -----------------------------------------------------------------------------

CREATE TABLE rewards (
    id                   UUID         NOT NULL,
    title                VARCHAR(160) NOT NULL,
    required_stamp_count INTEGER      NOT NULL,
    rule_config          JSONB,
    description          TEXT,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_rewards PRIMARY KEY (id),
    CONSTRAINT ck_rewards_required_stamp_count CHECK (required_stamp_count >= 0)
);

CREATE TABLE user_rewards (
    user_id     UUID        NOT NULL,
    reward_id   UUID        NOT NULL,
    status      VARCHAR(16) NOT NULL,
    unlocked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    redeemed_at TIMESTAMPTZ,
    CONSTRAINT pk_user_rewards PRIMARY KEY (user_id, reward_id),
    CONSTRAINT fk_user_rewards_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_user_rewards_reward FOREIGN KEY (reward_id) REFERENCES rewards (id),
    CONSTRAINT ck_user_rewards_status CHECK (status IN ('UNLOCKED', 'REDEEMED', 'EXPIRED')),
    CONSTRAINT ck_user_rewards_redeemed CHECK ((status = 'REDEEMED') = (redeemed_at IS NOT NULL))
);

CREATE INDEX idx_user_rewards_reward_id ON user_rewards (reward_id);
