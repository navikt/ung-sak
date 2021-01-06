-- Manuell søknad Frisinn ble lagt inn i start av 2021, da periode for fagsak ble satt feil. Korrigeres her.
update fagsak
set periode = '[2020-01-01,2021-01-01)'
where periode = '[2020-09-01,2022-01-01)'
  and saksnummer = 'A1T5K';
