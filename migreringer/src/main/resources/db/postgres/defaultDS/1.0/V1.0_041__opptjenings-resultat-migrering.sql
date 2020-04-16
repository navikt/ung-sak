INSERT INTO rs_opptjening (id, behandling_id, aktiv)
SELECT nextval('SEQ_RS_OPPTJENING') as id, behandling_id, aktiv
FROM opptjening;

UPDATE opptjening
set opptjening_resultat_id = o.id
FROM rs_opptjening o;

ALTER TABLE opptjening alter column opptjening_resultat_id set not null;

DROP INDEX uidx_opptjening_99;
ALTER TABLE opptjening drop column behandling_id;
ALTER TABLE opptjening drop column aktiv;
