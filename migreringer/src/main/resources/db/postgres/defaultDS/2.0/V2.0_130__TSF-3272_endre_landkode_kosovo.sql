-- Gjelder TSF-3272 Saksnummer C62N2
-- Landkode for kosovo er satt feil av søknadsdialogen. Må rettes til riktig kode

UPDATE MOTTATT_DOKUMENT
SET PAYLOAD = lo_from_bytea(0, convert_to(
        REPLACE(convert_from(lo_get(PAYLOAD), 'UTF8'),
                'XKX',
                'XXK'), 'UTF8'))
WHERE ID = 2389411 and STATUS = 'MOTTATT' and fagsak_id = 1251701;
