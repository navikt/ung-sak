ALTER TABLE IF EXISTS uttalelse
    RENAME COLUMN har_godtatt_endringen TO har_uttalelse;

UPDATE uttalelse
SET har_uttalelse = NOT har_uttalelse
WHERE har_uttalelse IS NOT NULL;
