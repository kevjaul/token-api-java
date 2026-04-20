-- Quartz tables for PostgreSQL (Quartz 2.3.x)
-- IMPORTANT: use exactly as-is, do NOT modify types

DROP TABLE IF EXISTS qrtz_fired_triggers CASCADE;
DROP TABLE IF EXISTS qrtz_paused_trigger_grps CASCADE;
DROP TABLE IF EXISTS qrtz_scheduler_state CASCADE;
DROP TABLE IF EXISTS qrtz_locks CASCADE;
DROP TABLE IF EXISTS qrtz_simple_triggers CASCADE;
DROP TABLE IF EXISTS qrtz_cron_triggers CASCADE;
DROP TABLE IF EXISTS qrtz_simprop_triggers CASCADE;
DROP TABLE IF EXISTS qrtz_blob_triggers CASCADE;
DROP TABLE IF EXISTS qrtz_triggers CASCADE;
DROP TABLE IF EXISTS qrtz_job_details CASCADE;
DROP TABLE IF EXISTS qrtz_calendars CASCADE;

-- =========================
-- JOB DETAILS
-- =========================
CREATE TABLE qrtz_job_details (
  sched_name        VARCHAR(120) NOT NULL,
  job_name          VARCHAR(200) NOT NULL,
  job_group         VARCHAR(200) NOT NULL,
  description       VARCHAR(250),
  job_class_name    VARCHAR(250) NOT NULL,
  is_durable        BOOLEAN NOT NULL,
  is_nonconcurrent  BOOLEAN NOT NULL,
  is_update_data    BOOLEAN NOT NULL,
  requests_recovery BOOLEAN NOT NULL,
  job_data          BYTEA,
  PRIMARY KEY (sched_name, job_name, job_group)
);

-- =========================
-- TRIGGERS
-- =========================
CREATE TABLE qrtz_triggers (
  sched_name     VARCHAR(120) NOT NULL,
  trigger_name   VARCHAR(200) NOT NULL,
  trigger_group  VARCHAR(200) NOT NULL,
  job_name       VARCHAR(200) NOT NULL,
  job_group      VARCHAR(200) NOT NULL,
  description    VARCHAR(250),
  next_fire_time BIGINT,
  prev_fire_time BIGINT,
  priority       INTEGER,
  trigger_state  VARCHAR(16) NOT NULL,
  trigger_type   VARCHAR(8) NOT NULL,
  start_time     BIGINT NOT NULL,
  end_time       BIGINT,
  calendar_name  VARCHAR(200),
  misfire_instr  SMALLINT,
  job_data       BYTEA,
  PRIMARY KEY (sched_name, trigger_name, trigger_group),
  FOREIGN KEY (sched_name, job_name, job_group)
    REFERENCES qrtz_job_details (sched_name, job_name, job_group)
);

-- =========================
-- SIMPLE TRIGGERS
-- =========================
CREATE TABLE qrtz_simple_triggers (
  sched_name      VARCHAR(120) NOT NULL,
  trigger_name    VARCHAR(200) NOT NULL,
  trigger_group   VARCHAR(200) NOT NULL,
  repeat_count    BIGINT NOT NULL,
  repeat_interval BIGINT NOT NULL,
  times_triggered BIGINT NOT NULL,
  PRIMARY KEY (sched_name, trigger_name, trigger_group),
  FOREIGN KEY (sched_name, trigger_name, trigger_group)
    REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group)
    ON DELETE CASCADE
);

-- =========================
-- CRON TRIGGERS
-- =========================
CREATE TABLE qrtz_cron_triggers (
  sched_name      VARCHAR(120) NOT NULL,
  trigger_name    VARCHAR(200) NOT NULL,
  trigger_group   VARCHAR(200) NOT NULL,
  cron_expression VARCHAR(120) NOT NULL,
  time_zone_id    VARCHAR(80),
  PRIMARY KEY (sched_name, trigger_name, trigger_group),
  FOREIGN KEY (sched_name, trigger_name, trigger_group)
    REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group)
    ON DELETE CASCADE
);

-- =========================
-- SIMPROP TRIGGERS
-- =========================
CREATE TABLE qrtz_simprop_triggers (
  sched_name    VARCHAR(120) NOT NULL,
  trigger_name  VARCHAR(200) NOT NULL,
  trigger_group VARCHAR(200) NOT NULL,
  str_prop_1    VARCHAR(512),
  str_prop_2    VARCHAR(512),
  str_prop_3    VARCHAR(512),
  int_prop_1    INTEGER,
  int_prop_2    INTEGER,
  long_prop_1   BIGINT,
  long_prop_2   BIGINT,
  dec_prop_1    NUMERIC(13,4),
  dec_prop_2    NUMERIC(13,4),
  bool_prop_1   BOOLEAN,
  bool_prop_2   BOOLEAN,
  PRIMARY KEY (sched_name, trigger_name, trigger_group),
  FOREIGN KEY (sched_name, trigger_name, trigger_group)
    REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group)
    ON DELETE CASCADE
);

-- =========================
-- BLOB TRIGGERS
-- =========================
CREATE TABLE qrtz_blob_triggers (
  sched_name    VARCHAR(120) NOT NULL,
  trigger_name  VARCHAR(200) NOT NULL,
  trigger_group VARCHAR(200) NOT NULL,
  blob_data     BYTEA,
  PRIMARY KEY (sched_name, trigger_name, trigger_group),
  FOREIGN KEY (sched_name, trigger_name, trigger_group)
    REFERENCES qrtz_triggers (sched_name, trigger_name, trigger_group)
    ON DELETE CASCADE
);

-- =========================
-- CALENDARS
-- =========================
CREATE TABLE qrtz_calendars (
  sched_name    VARCHAR(120) NOT NULL,
  calendar_name VARCHAR(200) NOT NULL,
  calendar      BYTEA NOT NULL,
  PRIMARY KEY (sched_name, calendar_name)
);

-- =========================
-- PAUSED GROUPS
-- =========================
CREATE TABLE qrtz_paused_trigger_grps (
  sched_name    VARCHAR(120) NOT NULL,
  trigger_group VARCHAR(200) NOT NULL,
  PRIMARY KEY (sched_name, trigger_group)
);

-- =========================
-- FIRED TRIGGERS
-- =========================
CREATE TABLE qrtz_fired_triggers (
  sched_name        VARCHAR(120) NOT NULL,
  entry_id          VARCHAR(95) NOT NULL,
  trigger_name      VARCHAR(200) NOT NULL,
  trigger_group     VARCHAR(200) NOT NULL,
  instance_name     VARCHAR(200) NOT NULL,
  fired_time        BIGINT NOT NULL,
  sched_time        BIGINT NOT NULL,
  priority          INTEGER NOT NULL,
  state             VARCHAR(16) NOT NULL,
  job_name          VARCHAR(200),
  job_group         VARCHAR(200),
  is_nonconcurrent  BOOLEAN,
  requests_recovery BOOLEAN,
  PRIMARY KEY (sched_name, entry_id)
);

-- =========================
-- SCHEDULER STATE
-- =========================
CREATE TABLE qrtz_scheduler_state (
  sched_name        VARCHAR(120) NOT NULL,
  instance_name     VARCHAR(200) NOT NULL,
  last_checkin_time BIGINT NOT NULL,
  checkin_interval  BIGINT NOT NULL,
  PRIMARY KEY (sched_name, instance_name)
);

-- =========================
-- LOCKS
-- =========================
CREATE TABLE qrtz_locks (
  sched_name VARCHAR(120) NOT NULL,
  lock_name  VARCHAR(40) NOT NULL,
  PRIMARY KEY (sched_name, lock_name)
);

-- =========================
-- INDEXES
-- =========================
CREATE INDEX idx_qrtz_j_req_recovery ON qrtz_job_details(requests_recovery);
CREATE INDEX idx_qrtz_t_next_fire_time ON qrtz_triggers(next_fire_time);
CREATE INDEX idx_qrtz_t_state ON qrtz_triggers(trigger_state);
CREATE INDEX idx_qrtz_f_trig_name ON qrtz_fired_triggers(trigger_name);
CREATE INDEX idx_qrtz_f_instance ON qrtz_fired_triggers(instance_name);