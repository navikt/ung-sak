-- Gjelder TSF-3272 Saksnummer C62N2
-- Landkode for kosovo er satt feil av søknadsdialogen. Må rettes til riktig kode

--   INV_WRITE  = 0x00020000
--   INV_READ = 0x00040000
UPDATE MOTTATT_DOKUMENT
SET PAYLOAD = lowrite(lo_open(PAYLOAD, x'60000'::int),
                      convert_to(REPLACE(convert_from(loread(lo_open(PAYLOAD::int, x'40000'::int), x'40000'::int),  'UTF8'), 'XKX', 'XXK'),'UTF8'))
WHERE ID = 2389411 and STATUS = 'MOTTATT' and fagsak_id = 1251701;
