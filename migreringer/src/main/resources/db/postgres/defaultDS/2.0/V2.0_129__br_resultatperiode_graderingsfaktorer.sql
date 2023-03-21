ALTER TABLE BR_PERIODE ADD Column GRADERINGSFAKTOR_INNTEKT NUMERIC(5,2);
comment on column BR_PERIODE.GRADERINGSFAKTOR_INNTEKT is 'Faktor som inngår i total gradering. Tilsvarer reduksjonen fra tilkommet inntekt';

ALTER TABLE BR_PERIODE ADD Column GRADERINGSFAKTOR_TID NUMERIC(5,2);
comment on column BR_PERIODE.GRADERINGSFAKTOR_TID is 'Faktor som inngår i total gradering. Tilsvarer uttaksgrad vektet mot inntekt';
