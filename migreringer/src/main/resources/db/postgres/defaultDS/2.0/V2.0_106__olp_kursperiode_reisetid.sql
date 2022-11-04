alter table UP_KURS_PERIODE drop column avreise;
alter table UP_KURS_PERIODE drop column hjemkomst;
alter table UP_KURS_PERIODE add column reiseTilFom DATE;
alter table UP_KURS_PERIODE add column reiseTilTom DATE;
alter table UP_KURS_PERIODE add column reiseHjemFom DATE;
alter table UP_KURS_PERIODE add column reiseHjemTom DATE;
