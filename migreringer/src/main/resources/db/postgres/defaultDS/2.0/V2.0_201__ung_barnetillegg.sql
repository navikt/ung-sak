ALTER TABLE UNG_SATS_PERIODE
ADD COLUMN ANTALL_BARN int,
ADD COLUMN DAGSATS_BARNETILLEGG numeric(19, 4);

UPDATE UNG_SATS_PERIODE SET ANTALL_BARN = 0, DAGSATS_BARNETILLEGG = 0 where ANTALL_BARN is null;


ALTER TABLE UNG_SATS_PERIODE
    ALTER COLUMN ANTALL_BARN SET NOT NULL,
    ALTER COLUMN DAGSATS_BARNETILLEGG SET NOT NULL;

comment on table UNG_SATS_PERIODE is 'Periode for satser og tilhørende informasjon relatert til satsberegning av ungdomsytelsen';
comment on column UNG_SATS_PERIODE.ANTALL_BARN is 'Antall barn benyttet i beregning av dagsats for barnetillegg';
comment on column UNG_SATS_PERIODE.DAGSATS_BARNETILLEGG is 'Utbetalt dagsats for barnetillegg';
