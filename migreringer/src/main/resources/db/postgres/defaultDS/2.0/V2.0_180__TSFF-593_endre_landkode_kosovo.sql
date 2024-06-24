-- Gjelder TSFF-593 Saksnummer EJDA4
-- Landkode for kosovo er satt feil av søknadsdialogen. Må rettes til riktig kode

UPDATE MOTTATT_DOKUMENT
SET PAYLOAD = lo_from_bytea(0, convert_to(
        REPLACE(convert_from(lo_get(PAYLOAD), 'UTF8'),
                'XKK',
                'XXK'), 'UTF8'))
WHERE ID = 2612435 and STATUS = 'MOTTATT' and fagsak_id = 1348263;
