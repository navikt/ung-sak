ALTER TABLE if exists UNG_GR_SOEKNADGRUNNLAG RENAME TO UNG_GR_STARTDATO;
ALTER SEQUENCE if exists SEQ_UNG_GR_SOEKNADGRUNNLAG RENAME TO SEQ_UNG_GR_STARTDATO;

ALTER TABLE if exists UNG_SOEKNADER RENAME TO UNG_STARTDATOER;
ALTER SEQUENCE if exists SEQ_UNG_SOEKNADER RENAME TO SEQ_UNG_STARTDATOER;

