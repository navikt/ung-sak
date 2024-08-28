update pleietrengende_sykdom_dokument set pleietrengende_sykdom_id = 2820020 where pleietrengende_sykdom_id = 3817900; /* oppdaterer ny til gammel */

delete from pleietrengende_sykdom where id = 3817900; /* ny pleietrengende_sykdom */
delete from person where id = 10398490; /* ny person (gyldig aktørid) */
