CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS aksjonspunkt (
    id bigint NOT NULL,
    periode_fom date,
    periode_tom date,
    begrunnelse character varying(4000),
    totrinn_behandling boolean DEFAULT false NOT NULL,
    behandling_steg_funnet character varying(100),
    aksjonspunkt_status character varying(100) NOT NULL,
    aksjonspunkt_def character varying(100) NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    frist_tid timestamp(3) without time zone,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    vent_aarsak character varying(100) DEFAULT '-'::character varying,
    behandling_id bigint NOT NULL,
    vent_aarsak_variant character varying(4000),
    ansvarlig_saksbehandler character varying(40)
);
COMMENT ON TABLE aksjonspunkt IS 'Aksjoner som en saksbehandler må utføre manuelt.';
COMMENT ON COLUMN aksjonspunkt.id IS 'Primary Key';
COMMENT ON COLUMN aksjonspunkt.periode_fom IS 'Angir starttidspunkt dersom aksjonspunktet gjelder en spesifikk periode. Brukes for aksjonspunkt som kan repteres flere ganger for en behandling.';
COMMENT ON COLUMN aksjonspunkt.periode_tom IS 'Angir sluttidspunkt dersom aksjonspunktet gjelder en spesifikk periode.';
COMMENT ON COLUMN aksjonspunkt.begrunnelse IS 'Begrunnelse for endringer gjort i forbindelse med aksjonspunktet.';
COMMENT ON COLUMN aksjonspunkt.totrinn_behandling IS 'Indikerer at aksjonspunkter krever en totrinnsbehandling';
COMMENT ON COLUMN aksjonspunkt.behandling_steg_funnet IS 'Hvilket steg ble dette aksjonspunktet funnet i?';
COMMENT ON COLUMN aksjonspunkt.aksjonspunkt_status IS 'FK:AKSJONSPUNKT_STATUS Fremmednøkkel til tabellen som inneholder status på aksjonspunktene';
COMMENT ON COLUMN aksjonspunkt.aksjonspunkt_def IS 'Aksjonspunkt kode';
COMMENT ON COLUMN aksjonspunkt.frist_tid IS 'Behandling blir automatisk gjenopptatt etter dette tidspunktet';
COMMENT ON COLUMN aksjonspunkt.vent_aarsak IS 'Årsak for at behandling er satt på vent';
COMMENT ON COLUMN aksjonspunkt.behandling_id IS 'Fremmednøkkel for kobling til behandling';
CREATE TABLE IF NOT EXISTS aksjonspunkt_sporing (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    aksjonspunkt_def character varying(100) NOT NULL,
    payload json NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL
);
COMMENT ON TABLE aksjonspunkt_sporing IS 'Lagrer data som ble brukt til bekreftelse av aksjonspunkt. Kun til bruk i diagnostikkformål. Data i tabellen utgjør ikke en del av behandlingsgrunnlaget.';
COMMENT ON COLUMN aksjonspunkt_sporing.behandling_id IS 'FK: Referanse til behandling';
COMMENT ON COLUMN aksjonspunkt_sporing.aksjonspunkt_def IS 'Aksjonspunktdefinisjon sporingen gjelder for.';
COMMENT ON COLUMN aksjonspunkt_sporing.payload IS 'Bekreftet opplysning i aksjonspunkt i json-format.';
COMMENT ON COLUMN aksjonspunkt_sporing.opprettet_tid IS 'Tidspunkt for opprettelse.';
CREATE TABLE IF NOT EXISTS behandling (
    id bigint NOT NULL,
    fagsak_id bigint NOT NULL,
    behandling_status character varying(100) NOT NULL,
    behandling_type character varying(100) NOT NULL,
    opprettet_dato timestamp(0) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    avsluttet_dato timestamp(0) without time zone,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    ansvarlig_saksbehandler character varying(100),
    ansvarlig_beslutter character varying(100),
    behandlende_enhet character varying(10),
    behandlende_enhet_navn character varying(320),
    behandlende_enhet_arsak character varying(800),
    behandlingstid_frist date NOT NULL,
    startpunkt_type character varying(50) DEFAULT '-'::character varying NOT NULL,
    sist_oppdatert_tidspunkt timestamp(3) without time zone,
    aapnet_for_endring boolean DEFAULT false NOT NULL,
    totrinnsbehandling boolean DEFAULT false NOT NULL,
    uuid uuid,
    migrert_kilde character varying(100) DEFAULT '-'::character varying NOT NULL,
    behandling_resultat_type character varying(100) DEFAULT 'IKKE_FASTSATT'::character varying NOT NULL,
    original_behandling_id bigint
);
COMMENT ON TABLE behandling IS 'Behandling av fagsak';
COMMENT ON COLUMN behandling.id IS 'Primary Key';
COMMENT ON COLUMN behandling.fagsak_id IS 'FK: FAGSAK Fremmednøkkel for kobling til fagsak';
COMMENT ON COLUMN behandling.behandling_status IS 'FK: BEHANDLING_STATUS Fremmednøkkel til tabellen som viser status på behandlinger';
COMMENT ON COLUMN behandling.behandling_type IS 'FK: BEHANDLING_TYPE Fremmedøkkel til oversikten over hvilken behandlingstyper som finnes';
COMMENT ON COLUMN behandling.opprettet_dato IS 'Dato når behandlingen ble opprettet.';
COMMENT ON COLUMN behandling.avsluttet_dato IS 'Dato når behandlingen ble avsluttet.';
COMMENT ON COLUMN behandling.ansvarlig_saksbehandler IS 'Id til saksbehandler som oppretter forslag til vedtak ved totrinnsbehandling.';
COMMENT ON COLUMN behandling.ansvarlig_beslutter IS 'Beslutter som har fattet vedtaket';
COMMENT ON COLUMN behandling.behandlende_enhet IS 'NAV-enhet som behandler behandlingen';
COMMENT ON COLUMN behandling.behandlende_enhet_navn IS 'Navn på behandlende enhet';
COMMENT ON COLUMN behandling.behandlende_enhet_arsak IS 'Fritekst for hvorfor behandlende enhet har blitt endret';
COMMENT ON COLUMN behandling.behandlingstid_frist IS 'Frist for når behandlingen skal være ferdig';
COMMENT ON COLUMN behandling.startpunkt_type IS 'Fremmednøkkel til tabellen som forteller startpunktet slik det er gitt av forretningshendelsen';
COMMENT ON COLUMN behandling.sist_oppdatert_tidspunkt IS 'Beskriver når grunnlagene til behandling ble sist innhentet';
COMMENT ON COLUMN behandling.aapnet_for_endring IS 'Flagget settes når menyvalget "Åpne behandling for endringer" kjøres.';
COMMENT ON COLUMN behandling.totrinnsbehandling IS 'Indikerer at behandlingen skal totrinnsbehandles';
COMMENT ON COLUMN behandling.uuid IS 'Unik UUID for behandling til utvortes bruk';
COMMENT ON COLUMN behandling.migrert_kilde IS 'Hvilket fagsystem behandlingen er migrert fra';
CREATE TABLE IF NOT EXISTS behandling_arsak (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    behandling_arsak_type character varying(50) NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    original_behandling_id bigint,
    manuelt_opprettet boolean DEFAULT false NOT NULL
);
COMMENT ON TABLE behandling_arsak IS 'Årsak for rebehandling';
COMMENT ON COLUMN behandling_arsak.id IS 'Primary Key';
COMMENT ON COLUMN behandling_arsak.behandling_id IS 'FK: BEHANDLING Fremmednøkkel for kobling til behandling';
COMMENT ON COLUMN behandling_arsak.behandling_arsak_type IS 'FK: BEHANDLING_ARSAK_TYPE Fremmednøkkel til oversikten over hvilke årsaker en behandling kan begrunnes med';
COMMENT ON COLUMN behandling_arsak.original_behandling_id IS 'FK: BEHANDLING Fremmednøkkel for kobling til behandlingen denne raden i tabellen hører til';
COMMENT ON COLUMN behandling_arsak.manuelt_opprettet IS 'Angir om behandlingsårsaken oppstod når en behandling ble manuelt opprettet. Brukes til å utlede om behandlingen ble manuelt opprettet.';
CREATE TABLE IF NOT EXISTS behandling_merknad (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    hastesak boolean NOT NULL,
    fritekst character varying(2000),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS behandling_steg_tilstand (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    behandling_steg character varying(100) NOT NULL,
    behandling_steg_status character varying(100) NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    aktiv boolean DEFAULT false NOT NULL
);
COMMENT ON TABLE behandling_steg_tilstand IS 'Angir tilstand for behandlingsteg som kjøres';
COMMENT ON COLUMN behandling_steg_tilstand.id IS 'Primary Key';
COMMENT ON COLUMN behandling_steg_tilstand.behandling_id IS 'FK: BEHANDLING Fremmednøkkel for kobling til behandlingen dette steget er tilknyttet';
COMMENT ON COLUMN behandling_steg_tilstand.behandling_steg IS 'Hvilket BehandlingSteg som kjøres';
COMMENT ON COLUMN behandling_steg_tilstand.behandling_steg_status IS 'Status på steg: (ved) INNGANG, STARTET, VENTER, (ved) UTGANG, UTFØRT';
CREATE TABLE IF NOT EXISTS behandling_vedtak (
    id bigint NOT NULL,
    vedtak_dato timestamp(3) without time zone NOT NULL,
    ansvarlig_saksbehandler character varying(40) NOT NULL,
    vedtak_resultat_type character varying(100) NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    iverksetting_status character varying(100) DEFAULT 'IKKE_IVERKSATT'::character varying NOT NULL,
    beslutning boolean DEFAULT false NOT NULL,
    behandling_id bigint NOT NULL,
    er_publisert boolean DEFAULT false NOT NULL
);
COMMENT ON TABLE behandling_vedtak IS 'Vedtak koblet til en behandling via et behandlingsresultat.';
COMMENT ON COLUMN behandling_vedtak.id IS 'Primary Key';
COMMENT ON COLUMN behandling_vedtak.vedtak_dato IS 'Vedtaksdato.';
COMMENT ON COLUMN behandling_vedtak.ansvarlig_saksbehandler IS 'Ansvarlig saksbehandler som godkjente vedtaket.';
COMMENT ON COLUMN behandling_vedtak.vedtak_resultat_type IS 'FK:VEDTAK_RESULTAT_TYPE Fremmednøkkel til tabellen som viser innholdet i vedtaket';
COMMENT ON COLUMN behandling_vedtak.iverksetting_status IS 'Status for iverksettingssteget';
COMMENT ON COLUMN behandling_vedtak.beslutning IS 'Hvorvidt vedtaket er et beslutningsvedtak. Et beslutningsvedtak er et vedtak med samme utfall som forrige vedtak';
CREATE TABLE IF NOT EXISTS br_andel (
    id bigint NOT NULL,
    br_periode_id bigint NOT NULL,
    dagsats bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    stillingsprosent numeric(5,2) NOT NULL,
    utbetalingsgrad numeric(5,2) NOT NULL,
    dagsats_fra_bg bigint NOT NULL,
    aktivitet_status character varying(100),
    inntektskategori character varying(100) NOT NULL,
    arbeidsforhold_type character varying(100) DEFAULT '-'::character varying NOT NULL,
    bruker_er_mottaker boolean DEFAULT false NOT NULL,
    arbeidsgiver_aktor_id character varying(100),
    arbeidsgiver_orgnr character varying(100),
    arbeidsforhold_intern_id uuid,
    periode daterange,
    feriepenger_beloep bigint,
    beregningsresultat_id bigint,
    utbetalingsgrad_oppdrag numeric(5,2),
    CONSTRAINT chk_br_andel_samme_aar CHECK (((periode IS NULL) OR (date_part('year'::text, lower(periode)) = date_part('year'::text, (upper(periode) - '1 day'::interval)))))
);
COMMENT ON TABLE br_andel IS 'Andel i tilkjent ytelse';
COMMENT ON COLUMN br_andel.id IS 'Primærnøkkel';
COMMENT ON COLUMN br_andel.br_periode_id IS 'Fremmednøkkel til tabell som knytter beregningsresultatandelen til en beregningsgrunnlagsperiode';
COMMENT ON COLUMN br_andel.dagsats IS 'Dagsats for tilkjent ytelse';
COMMENT ON COLUMN br_andel.stillingsprosent IS 'Stillingsprosent';
COMMENT ON COLUMN br_andel.utbetalingsgrad IS 'Uttaksgrad';
COMMENT ON COLUMN br_andel.dagsats_fra_bg IS 'Dagsats fra beregningsgrunnlag';
COMMENT ON COLUMN br_andel.aktivitet_status IS 'Aktivitetstatus for andelen';
COMMENT ON COLUMN br_andel.inntektskategori IS 'Inntektskategori for andelen';
COMMENT ON COLUMN br_andel.arbeidsforhold_type IS 'Typekode for arbeidstakeraktivitet som ikke er tilknyttet noen virksomhet';
COMMENT ON COLUMN br_andel.bruker_er_mottaker IS 'Angir om bruker eller arbeidsgiver er mottaker';
COMMENT ON COLUMN br_andel.arbeidsgiver_aktor_id IS 'Arbeidsgivers aktør id.';
COMMENT ON COLUMN br_andel.arbeidsgiver_orgnr IS 'Organisasjonsnummer for arbeidsgivere som er virksomheter';
COMMENT ON COLUMN br_andel.arbeidsforhold_intern_id IS 'Globalt unikt arbeidsforhold id generert for arbeidsgiver/arbeidsforhold. I motsetning til arbeidsforhold_ekstern_id som holder arbeidsgivers referanse';
CREATE TABLE IF NOT EXISTS br_beregningsresultat (
    id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    regel_input oid NOT NULL,
    regel_sporing oid NOT NULL,
    endringsdato date,
    feriepenger_regel_input oid,
    feriepenger_regel_sporing oid
);
COMMENT ON TABLE br_beregningsresultat IS 'Aggregat for tilkjent ytelse';
COMMENT ON COLUMN br_beregningsresultat.id IS 'Primærnøkkel';
COMMENT ON COLUMN br_beregningsresultat.regel_input IS 'Input til beregningsregel for tilkjent ytelse, JSON';
COMMENT ON COLUMN br_beregningsresultat.regel_sporing IS 'Logg fra beregningsregel for tilkjent ytelse, JSON';
COMMENT ON COLUMN br_beregningsresultat.endringsdato IS 'Endringsdato for beregningsresultat.';
CREATE TABLE IF NOT EXISTS br_feriepenger (
    id bigint NOT NULL,
    beregningsresultat_fp_id bigint NOT NULL,
    feriepenger_regel_input oid NOT NULL,
    feriepenger_regel_sporing oid NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    versjon bigint DEFAULT 0 NOT NULL,
    feriepenger_periode_tom date,
    feriepenger_periode_fom date
);
COMMENT ON TABLE br_feriepenger IS 'Oversikt over ev feriepenger';
COMMENT ON COLUMN br_feriepenger.id IS 'Primær nøkkel';
COMMENT ON COLUMN br_feriepenger.beregningsresultat_fp_id IS 'FK: BEREGNINGSRESULTAT_FP';
COMMENT ON COLUMN br_feriepenger.feriepenger_regel_input IS 'Input til regel (clob)';
COMMENT ON COLUMN br_feriepenger.feriepenger_regel_sporing IS 'Logg fra regel (clob)';
COMMENT ON COLUMN br_feriepenger.feriepenger_periode_tom IS 'Siste dag i brukers feriepengeperiode';
COMMENT ON COLUMN br_feriepenger.feriepenger_periode_fom IS 'Første dag i brukers feriepengeperiode';
CREATE TABLE IF NOT EXISTS br_feriepenger_pr_aar (
    id bigint NOT NULL,
    br_feriepenger_id bigint,
    beregningsresultat_andel_id bigint NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    versjon bigint DEFAULT 0 NOT NULL,
    opptjeningsaar date NOT NULL,
    aarsbeloep bigint NOT NULL
);
COMMENT ON TABLE br_feriepenger_pr_aar IS 'Årsverdier av feriepenger knyttet til andel';
COMMENT ON COLUMN br_feriepenger_pr_aar.id IS 'Primær nøkkel';
COMMENT ON COLUMN br_feriepenger_pr_aar.br_feriepenger_id IS 'FK:BR_FERIEPENGER';
COMMENT ON COLUMN br_feriepenger_pr_aar.beregningsresultat_andel_id IS 'FK:BEREGNINGSRESULTAT_ANDEL';
COMMENT ON COLUMN br_feriepenger_pr_aar.opptjeningsaar IS '31/12 i opptjeningsåret, dvs året før feriepengene utbetales';
COMMENT ON COLUMN br_feriepenger_pr_aar.aarsbeloep IS 'Årsbeløp som skal utbetales, avrundet';
CREATE TABLE IF NOT EXISTS br_periode (
    id bigint NOT NULL,
    beregningsresultat_fp_id bigint NOT NULL,
    br_periode_fom date NOT NULL,
    br_periode_tom date NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    gradering_inntekt_prosent numeric(5,2),
    graderingsfaktor_inntekt numeric(5,2),
    graderingsfaktor_tid numeric(5,2),
    total_utbetalingsgrad_fra_uttak numeric(19,4),
    total_utbetalingsgrad_etter_reduksjon_ved_tilkommet_inntekt numeric(19,4),
    reduksjonsfaktor_inaktiv_type_a numeric(19,4)
);
COMMENT ON TABLE br_periode IS 'Periode i tilkjent ytelse';
COMMENT ON COLUMN br_periode.id IS 'Primærnøkkel';
COMMENT ON COLUMN br_periode.beregningsresultat_fp_id IS 'Fremmednøkkel til tabell som knytter beregningsresultperioden til beregningsresultatet';
COMMENT ON COLUMN br_periode.br_periode_fom IS 'Første dag i periode for tilkjent ytelse';
COMMENT ON COLUMN br_periode.br_periode_tom IS 'Siste dag i periode for tilkjent ytelse';
COMMENT ON COLUMN br_periode.gradering_inntekt_prosent IS 'Graderingsprosent ved gradering mot inntekt.';
COMMENT ON COLUMN br_periode.graderingsfaktor_inntekt IS 'Faktor som inngår i total gradering. Tilsvarer reduksjonen fra tilkommet inntekt';
COMMENT ON COLUMN br_periode.graderingsfaktor_tid IS 'Faktor som inngår i total gradering. Tilsvarer uttaksgrad vektet mot inntekt';
COMMENT ON COLUMN br_periode.total_utbetalingsgrad_fra_uttak IS 'Total utbetalingsgrad fra uttak. Utregnet separat fra reduksjon ved tilkommet inntekt.';
COMMENT ON COLUMN br_periode.total_utbetalingsgrad_etter_reduksjon_ved_tilkommet_inntekt IS 'Total utbetalingsgrad etter reduksjon ved tilkommet inntekt. Utregnet separat fra utbetalingsgrad fra uttak.';
COMMENT ON COLUMN br_periode.reduksjonsfaktor_inaktiv_type_a IS 'Reduksjonsfaktor benyttet ved midlertidig inaktiv type A (§8-47a)';
CREATE TABLE IF NOT EXISTS br_resultat_behandling (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    aktiv boolean DEFAULT false NOT NULL,
    bg_beregningsresultat_fp_id bigint NOT NULL,
    utbet_beregningsresultat_fp_id bigint,
    hindre_tilbaketrekk boolean DEFAULT false NOT NULL
);
COMMENT ON TABLE br_resultat_behandling IS 'Tabell som kobler et beregningsresultat_fp til behandling';
COMMENT ON COLUMN br_resultat_behandling.id IS 'Primary Key';
COMMENT ON COLUMN br_resultat_behandling.behandling_id IS 'FK:BEHANDLING';
COMMENT ON COLUMN br_resultat_behandling.bg_beregningsresultat_fp_id IS 'FK: BEREGNINGSRESULTAT_FP. Tilkjent ytelse basert på beregningsgrunnlag';
COMMENT ON COLUMN br_resultat_behandling.utbet_beregningsresultat_fp_id IS 'FK: BEREGNINGSRESULTAT_FP. Tilkjent ytelse til utbetaling';
COMMENT ON COLUMN br_resultat_behandling.hindre_tilbaketrekk IS 'Definerer om tilbaketrekk av tidligere stønad skal hindres. null dersom det ikke er tatt stilling til av saksbehandler.';
CREATE TABLE IF NOT EXISTS br_sats (
    id bigint NOT NULL,
    sats_type character varying(100) NOT NULL,
    fom date NOT NULL,
    tom date NOT NULL,
    verdi numeric(10,0) NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON TABLE br_sats IS 'Satser brukt ifm beregning av ytelser';
COMMENT ON COLUMN br_sats.id IS 'Primary Key';
COMMENT ON COLUMN br_sats.sats_type IS 'FK:SATS_TYPE Fremmednøkkel til tabell for beskrivelse av satstyper i beregning';
COMMENT ON COLUMN br_sats.fom IS 'Gyldig Fra-Og-Med';
COMMENT ON COLUMN br_sats.tom IS 'Gyldig Til-Og-Med';
COMMENT ON COLUMN br_sats.verdi IS 'Sats verdi.';
CREATE TABLE IF NOT EXISTS diagnostikk_fagsak_logg (
    id bigint NOT NULL,
    fagsak_id bigint NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    begrunnelse text,
    tjeneste character varying(200)
);
CREATE TABLE IF NOT EXISTS etterkontroll (
    id bigint NOT NULL,
    fagsak_id bigint NOT NULL,
    kontroll_type character varying(100) NOT NULL,
    behandlet boolean DEFAULT false NOT NULL,
    kontroll_tid timestamp(3) without time zone NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    behandling_id bigint
);
COMMENT ON TABLE etterkontroll IS 'Tabell for å holde track på behandlinger som trenger etterkontroll';
COMMENT ON COLUMN etterkontroll.id IS 'Primary key';
COMMENT ON COLUMN etterkontroll.fagsak_id IS 'FK: kobling til fagsak.';
COMMENT ON COLUMN etterkontroll.kontroll_type IS 'Kontrolltype kode kode som angir årsak til registrering for etterkontroll.';
COMMENT ON COLUMN etterkontroll.behandlet IS 'Statusfelt. N indikerer at raden ikke er etterkontrollert.';
COMMENT ON COLUMN etterkontroll.kontroll_tid IS 'Skjæringspunkt for når kontroll skal inntreffe.';
COMMENT ON COLUMN etterkontroll.opprettet_tid IS 'Tidspunkt rad opprettet.';
COMMENT ON COLUMN etterkontroll.opprettet_av IS 'Hvem opprettet rad.';
COMMENT ON COLUMN etterkontroll.endret_av IS 'Hvem har endret rad.';
COMMENT ON COLUMN etterkontroll.endret_tid IS 'Tidspunkt når rad ble endret.';
CREATE TABLE IF NOT EXISTS fagsak (
    id bigint NOT NULL,
    fagsak_status character varying(100) NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    ytelse_type character varying(100),
    saksnummer character varying(19),
    til_infotrygd boolean DEFAULT false NOT NULL,
    pleietrengende_aktoer_id character varying(50),
    bruker_aktoer_id character varying(50) NOT NULL,
    periode daterange,
    relatert_person_aktoer_id character varying(50)
);
COMMENT ON TABLE fagsak IS 'Fagsak for engangsstønad og foreldrepenger. Alle behandling er koblet mot en fagsak.';
COMMENT ON COLUMN fagsak.id IS 'Primary Key';
COMMENT ON COLUMN fagsak.fagsak_status IS 'FK:FAGSAK_STATUS Fremmednøkkel til kodeverkstabellen som inneholder oversikten over fagstatuser';
COMMENT ON COLUMN fagsak.ytelse_type IS 'Fremmednøkkel til kodeverkstabellen som inneholder oversikt over ytelser';
COMMENT ON COLUMN fagsak.saksnummer IS 'Saksnummer (som GSAK har mottatt)';
COMMENT ON COLUMN fagsak.til_infotrygd IS 'J hvis saken må behandles av Infotrygd';
CREATE TABLE IF NOT EXISTS fagsak_prosess_task (
    id bigint NOT NULL,
    fagsak_id bigint NOT NULL,
    prosess_task_id bigint NOT NULL,
    behandling_id character varying,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    gruppe_sekvensnr bigint,
    task_type character varying(50)
);
COMMENT ON TABLE fagsak_prosess_task IS '1-M relasjonstabell for � mappe fagsak til prosess_tasks (som ikke er FERDIG)';
COMMENT ON COLUMN fagsak_prosess_task.id IS 'Primærnøkkel';
COMMENT ON COLUMN fagsak_prosess_task.fagsak_id IS 'FK: Fremmednøkkel for kobling til fagsak';
COMMENT ON COLUMN fagsak_prosess_task.prosess_task_id IS 'FK: Fremmednøkkel for knyttning til logging av prosesstask som ???';
COMMENT ON COLUMN fagsak_prosess_task.behandling_id IS 'FK: Fremmednøkkel for kobling til behandling';
COMMENT ON COLUMN fagsak_prosess_task.gruppe_sekvensnr IS 'For en gitt fagsak angir hvilken rekkefølge task skal kjøres.  Kun tasks med laveste gruppe_sekvensnr vil kjøres. Når disse er FERDIG vil de ryddes bort og neste med lavest sekvensnr kan kjøres (gitt at den er KLAR)';
CREATE TABLE IF NOT EXISTS gr_personopplysning (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    registrert_informasjon_id bigint,
    overstyrt_informasjon_id bigint,
    aktiv boolean DEFAULT false NOT NULL
);
COMMENT ON TABLE gr_personopplysning IS 'Behandlingsgrunnlag for Personopplysning (aggregat) for søker med familie';
COMMENT ON COLUMN gr_personopplysning.id IS 'Primærnøkkel';
COMMENT ON COLUMN gr_personopplysning.behandling_id IS 'FK: BEHANDLING Fremmednøkkel for kobling til behandling';
COMMENT ON COLUMN gr_personopplysning.registrert_informasjon_id IS 'FK:';
COMMENT ON COLUMN gr_personopplysning.overstyrt_informasjon_id IS 'FK:';
CREATE TABLE IF NOT EXISTS gr_soeknad (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    soeknad_id bigint NOT NULL,
    aktiv boolean DEFAULT false NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON TABLE gr_soeknad IS 'Grunnlag for søknaden';
COMMENT ON COLUMN gr_soeknad.id IS 'Primærnøkkel';
COMMENT ON COLUMN gr_soeknad.behandling_id IS 'FK: BEHANDLING Fremmednøkkel for kobling til behandling';
COMMENT ON COLUMN gr_soeknad.soeknad_id IS 'FK:';
CREATE TABLE IF NOT EXISTS historikkinnslag (
    id bigint NOT NULL,
    behandling_id bigint,
    historikk_aktoer_id character varying(100) NOT NULL,
    historikkinnslag_type character varying(100) NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    fagsak_id bigint NOT NULL,
    uuid uuid,
    historikk_tid timestamp(3) without time zone,
    opprettet_i_system character varying(20)
);
COMMENT ON TABLE historikkinnslag IS 'Historikk over hendelser i saken';
COMMENT ON COLUMN historikkinnslag.id IS 'Primary Key';
COMMENT ON COLUMN historikkinnslag.behandling_id IS 'FK: BEHANDLING Fremmednøkkel for kobling til behandling';
COMMENT ON COLUMN historikkinnslag.historikk_aktoer_id IS 'FK:HISTORIKK_AKTOER Fremmednøkkel til';
COMMENT ON COLUMN historikkinnslag.historikkinnslag_type IS 'Fremmednøkkel til beskrivelse av historikkinnslaget';
COMMENT ON COLUMN historikkinnslag.fagsak_id IS 'FK:FAGSAK Fremmednøkkel for kobling til fagsak';
COMMENT ON COLUMN historikkinnslag.uuid IS 'Unik UUID for historikkinnslag til utvortes bruk, kan være generert eksternt hvis innslaget kommer fra et annet system';
COMMENT ON COLUMN historikkinnslag.historikk_tid IS 'Når innslaget ble opprettet i opprinnelig system';
COMMENT ON COLUMN historikkinnslag.opprettet_i_system IS 'Hvilket system historikkinnslaget er generert i';
CREATE TABLE IF NOT EXISTS historikkinnslag_del (
    id bigint NOT NULL,
    historikkinnslag_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON TABLE historikkinnslag_del IS 'Et historikkinnslag kan ha en eller flere deler';
COMMENT ON COLUMN historikkinnslag_del.id IS 'Primærnøkkel';
COMMENT ON COLUMN historikkinnslag_del.historikkinnslag_id IS 'FK:HISTORIKKINNSLAG Fremmednøkkel til riktig innslag i historikktabellen';
CREATE TABLE IF NOT EXISTS historikkinnslag_dok_link (
    id bigint NOT NULL,
    link_tekst character varying(100) NOT NULL,
    historikkinnslag_id bigint NOT NULL,
    journalpost_id character varying(100),
    dokument_id character varying(100),
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON TABLE historikkinnslag_dok_link IS 'Kobling fra historikkinnslag til aktuell dokumentasjon';
COMMENT ON COLUMN historikkinnslag_dok_link.id IS 'Primary Key';
COMMENT ON COLUMN historikkinnslag_dok_link.link_tekst IS 'Tekst som vises for link til dokumentet';
COMMENT ON COLUMN historikkinnslag_dok_link.historikkinnslag_id IS 'FK:HISTORIKKINNSLAG Fremmednøkkel til riktig innslag i historikktabellen';
COMMENT ON COLUMN historikkinnslag_dok_link.journalpost_id IS 'FK';
COMMENT ON COLUMN historikkinnslag_dok_link.dokument_id IS 'FK:';
CREATE TABLE IF NOT EXISTS historikkinnslag_felt (
    id bigint NOT NULL,
    historikkinnslag_del_id bigint NOT NULL,
    historikkinnslag_felt_type character varying(100) NOT NULL,
    navn character varying(100),
    kl_navn character varying(100),
    navn_verdi character varying(4000),
    fra_verdi character varying(4000),
    til_verdi character varying(4000),
    sekvens_nr integer,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    fra_verdi_kode character varying(100),
    til_verdi_kode character varying(100),
    kl_fra_verdi character varying(100),
    kl_til_verdi character varying(100)
);
COMMENT ON TABLE historikkinnslag_felt IS 'En historikkinnslagdel har typisk mange felt';
COMMENT ON COLUMN historikkinnslag_felt.id IS 'Primærnøkkel';
COMMENT ON COLUMN historikkinnslag_felt.historikkinnslag_del_id IS 'FK:';
COMMENT ON COLUMN historikkinnslag_felt.historikkinnslag_felt_type IS 'Hva slags type informasjon som er representert';
COMMENT ON COLUMN historikkinnslag_felt.navn IS 'Navn på felt. Gjelder for endrede verdier og opplysninger';
COMMENT ON COLUMN historikkinnslag_felt.kl_navn IS 'Kodeverk anvn';
COMMENT ON COLUMN historikkinnslag_felt.navn_verdi IS 'Verdi som skal brukes som del av feltets navn';
COMMENT ON COLUMN historikkinnslag_felt.fra_verdi IS 'Feltets gamle verdi. Kun string';
COMMENT ON COLUMN historikkinnslag_felt.til_verdi IS 'Feltets nye verdi. Kun string';
COMMENT ON COLUMN historikkinnslag_felt.sekvens_nr IS 'Settes dersom historikkinnslagdelen har flere innslag med samme navn';
COMMENT ON COLUMN historikkinnslag_felt.fra_verdi_kode IS 'Feltets gamle verdi. Kun kodeverk';
COMMENT ON COLUMN historikkinnslag_felt.til_verdi_kode IS 'Feltets nye verdi. Kun kodeverk';
COMMENT ON COLUMN historikkinnslag_felt.kl_fra_verdi IS 'Kodeverk for fra_verdi';
COMMENT ON COLUMN historikkinnslag_felt.kl_til_verdi IS 'Kodeverk for til_verdi';
CREATE TABLE IF NOT EXISTS journalpost (
    id bigint NOT NULL,
    fagsak_id bigint NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    journalpost_id character varying(20) NOT NULL
);
COMMENT ON TABLE journalpost IS 'Journalposter som er blitt knyttet til saken';
COMMENT ON COLUMN journalpost.id IS 'Primary Key';
COMMENT ON COLUMN journalpost.fagsak_id IS 'FK:FAGSAK Fremmednøkkel for kobling til fagsak';
COMMENT ON COLUMN journalpost.journalpost_id IS 'Journalpost-id';
CREATE TABLE IF NOT EXISTS mottatt_dokument (
    id bigint NOT NULL,
    journalpost_id character varying(20),
    type character varying(100),
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    behandling_id bigint,
    mottatt_dato date,
    fagsak_id bigint NOT NULL,
    forsendelse_id uuid,
    payload oid,
    dokument_kategori character varying(100),
    journal_enhet character varying(10),
    mottatt_tidspunkt timestamp(3) without time zone,
    kanalreferanse character varying(100),
    payload_type character varying(10),
    arbeidsgiver character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    status character varying(10),
    feilmelding text,
    kildesystem character varying(100),
    innsendingstidspunkt timestamp(3) without time zone
);
COMMENT ON TABLE mottatt_dokument IS 'Mottatt dokument som er lagret i Joark';
COMMENT ON COLUMN mottatt_dokument.id IS 'Primary Key';
COMMENT ON COLUMN mottatt_dokument.journalpost_id IS 'FK: Journalpostens ID i JOARK';
COMMENT ON COLUMN mottatt_dokument.type IS 'FK:DOKUMENT_TYPE Fremmednøkkel for kobling til dokumenttype';
COMMENT ON COLUMN mottatt_dokument.behandling_id IS 'FK:BEHANDLING Fremmednøkkel for kobling til behandling';
COMMENT ON COLUMN mottatt_dokument.mottatt_dato IS 'Mottatt dato';
COMMENT ON COLUMN mottatt_dokument.fagsak_id IS 'FK: Fremmednøkkel til Fagsak';
COMMENT ON COLUMN mottatt_dokument.forsendelse_id IS 'Unik ID for forsendelsen';
COMMENT ON COLUMN mottatt_dokument.payload IS 'Strukturert informasjon fra det mottatte dokumentet';
COMMENT ON COLUMN mottatt_dokument.dokument_kategori IS 'Dokumentkategori';
COMMENT ON COLUMN mottatt_dokument.journal_enhet IS 'Journalførende enhet fra forside dersom satt';
COMMENT ON COLUMN mottatt_dokument.mottatt_tidspunkt IS 'Arkiveringstidspunkt for journalposten';
COMMENT ON COLUMN mottatt_dokument.kanalreferanse IS 'Kildereferanse for journalposten';
CREATE TABLE IF NOT EXISTS mottatt_hendelse (
    hendelse_uid character varying(100) NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON TABLE mottatt_hendelse IS 'Holder unik identifikator for alle mottatte hendelser. Brukes for å unngå at en hendelse medfører flere revurderinger';
COMMENT ON COLUMN mottatt_hendelse.hendelse_uid IS 'Unik identifikator for hendelse mottatt';
CREATE TABLE IF NOT EXISTS notat_aktoer (
    id bigint NOT NULL,
    uuid uuid NOT NULL,
    aktoer_id character varying(20) NOT NULL,
    ytelse_type character varying(100) NOT NULL,
    skjult boolean NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON TABLE notat_aktoer IS 'Notat som gjelder en aktør, foreløpig bare pleietrengende';
CREATE TABLE IF NOT EXISTS notat_aktoer_tekst (
    id bigint NOT NULL,
    notat_id bigint NOT NULL,
    tekst text NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON COLUMN notat_aktoer_tekst.tekst IS 'Tekst i notatet';
COMMENT ON COLUMN notat_aktoer_tekst.versjon IS 'Versjon av notat teksten med notat_id';
CREATE TABLE IF NOT EXISTS notat_sak (
    id bigint NOT NULL,
    uuid uuid NOT NULL,
    fagsak_id bigint NOT NULL,
    skjult boolean NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON TABLE notat_sak IS 'Notat som gjelder en fagsak';
CREATE TABLE IF NOT EXISTS notat_sak_tekst (
    id bigint NOT NULL,
    notat_id bigint NOT NULL,
    tekst text NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON COLUMN notat_sak_tekst.tekst IS 'Tekst i notatet';
COMMENT ON COLUMN notat_sak_tekst.versjon IS 'Versjon av notat teksten med notat_id';
CREATE TABLE IF NOT EXISTS oppgave_behandling_kobling (
    id bigint NOT NULL,
    oppgave_aarsak character varying(100) NOT NULL,
    oppgave_id character varying(50),
    behandling_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    ferdigstilt boolean DEFAULT false NOT NULL,
    ferdigstilt_av character varying(20),
    ferdigstilt_tid timestamp(3) without time zone,
    saksnummer character varying(19) NOT NULL
);
COMMENT ON TABLE oppgave_behandling_kobling IS 'Kobling mellom opprettede oppgaver i GSAK og behandlinger.';
COMMENT ON COLUMN oppgave_behandling_kobling.id IS 'Primary Key';
COMMENT ON COLUMN oppgave_behandling_kobling.oppgave_aarsak IS 'FK:OPPGAVE_AARSAK Fremmednøkkel for kobling til kodeverkstabell for årsaken til at oppgaven er opprettet';
COMMENT ON COLUMN oppgave_behandling_kobling.oppgave_id IS 'FK:';
COMMENT ON COLUMN oppgave_behandling_kobling.behandling_id IS 'FK:BEHANDLING Fremmednøkkel for kobling til behandling';
COMMENT ON COLUMN oppgave_behandling_kobling.ferdigstilt IS 'Er oppgaven ferdigstilt.';
COMMENT ON COLUMN oppgave_behandling_kobling.ferdigstilt_av IS 'Ident til den som har ferdigstilt oppgaven.';
COMMENT ON COLUMN oppgave_behandling_kobling.ferdigstilt_tid IS 'Tidspunkt for når oppgave ble ferdigstilt.';
COMMENT ON COLUMN oppgave_behandling_kobling.saksnummer IS 'Saksnummer (som GSAK har mottatt)';
CREATE TABLE IF NOT EXISTS po_adresse (
    id bigint NOT NULL,
    fom date NOT NULL,
    tom date NOT NULL,
    adresselinje1 character varying(1000),
    adresselinje2 character varying(1000),
    adresselinje3 character varying(1000),
    adresselinje4 character varying(1000),
    postnummer character varying(20),
    poststed character varying(40),
    land character varying(40),
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    po_informasjon_id bigint NOT NULL,
    adresse_type character varying(100) NOT NULL,
    aktoer_id character varying(50)
);
COMMENT ON TABLE po_adresse IS 'Personopplysning - adresse';
COMMENT ON COLUMN po_adresse.id IS 'Primærnøkkel';
COMMENT ON COLUMN po_adresse.fom IS 'Gyldig fom';
COMMENT ON COLUMN po_adresse.tom IS 'Gyldig tom';
COMMENT ON COLUMN po_adresse.adresselinje1 IS 'Adresse linje (not null)';
COMMENT ON COLUMN po_adresse.adresselinje2 IS 'Adresse linje';
COMMENT ON COLUMN po_adresse.adresselinje3 IS 'Adresse linje';
COMMENT ON COLUMN po_adresse.adresselinje4 IS 'Adresselinje 4';
COMMENT ON COLUMN po_adresse.postnummer IS 'Postnummer';
COMMENT ON COLUMN po_adresse.poststed IS 'Poststed';
COMMENT ON COLUMN po_adresse.land IS 'Land';
COMMENT ON COLUMN po_adresse.po_informasjon_id IS 'FK:';
COMMENT ON COLUMN po_adresse.adresse_type IS 'Fremmednøkkel til kodeverkstabell for oversikt over adressetyper';
COMMENT ON COLUMN po_adresse.aktoer_id IS 'Akt�rid (fra NAV Akt�rregister)';
CREATE TABLE IF NOT EXISTS po_informasjon (
    id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON TABLE po_informasjon IS 'Aggregering av informasjon om personopplysning';
COMMENT ON COLUMN po_informasjon.id IS 'Primærnøkkel';
COMMENT ON COLUMN po_informasjon.versjon IS 'Angir versjon for optimistisk låsing';
CREATE TABLE IF NOT EXISTS po_personopplysning (
    id bigint NOT NULL,
    navn character varying(100),
    foedselsdato date NOT NULL,
    doedsdato date,
    bruker_kjoenn character varying(100),
    sivilstand_type character varying(100) NOT NULL,
    region character varying(100) NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    po_informasjon_id bigint NOT NULL,
    aktoer_id character varying(50)
);
COMMENT ON TABLE po_personopplysning IS 'Personopplysning - personopplysning';
COMMENT ON COLUMN po_personopplysning.id IS 'Primærnøkkel';
COMMENT ON COLUMN po_personopplysning.navn IS 'Navn';
COMMENT ON COLUMN po_personopplysning.foedselsdato IS 'Fødselsdato';
COMMENT ON COLUMN po_personopplysning.doedsdato IS 'Dødsdato (nullable)';
COMMENT ON COLUMN po_personopplysning.bruker_kjoenn IS 'Person kjønn';
COMMENT ON COLUMN po_personopplysning.sivilstand_type IS 'Sivilstand (eks. EKTE/SAMB/ etc)';
COMMENT ON COLUMN po_personopplysning.region IS 'Geopolitisk region - eks NORGE / NORDEN / EØS person anses tilknyttet';
COMMENT ON COLUMN po_personopplysning.po_informasjon_id IS 'FK:';
COMMENT ON COLUMN po_personopplysning.aktoer_id IS 'Akt�rid (fra NAV Akt�rregister)';
CREATE TABLE IF NOT EXISTS po_personstatus (
    id bigint NOT NULL,
    fom date NOT NULL,
    tom date NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    po_informasjon_id bigint NOT NULL,
    personstatus character varying(100) NOT NULL,
    aktoer_id character varying(50)
);
COMMENT ON TABLE po_personstatus IS 'Personopplysning - personstatus';
COMMENT ON COLUMN po_personstatus.id IS 'Primærnøkkel';
COMMENT ON COLUMN po_personstatus.fom IS 'Gyldig fom';
COMMENT ON COLUMN po_personstatus.tom IS 'Gyldig tom';
COMMENT ON COLUMN po_personstatus.po_informasjon_id IS 'FK:';
COMMENT ON COLUMN po_personstatus.personstatus IS 'Personstatus (BOSA - Bosatt, UTVA - Utvandret, etc)';
COMMENT ON COLUMN po_personstatus.aktoer_id IS 'Akt�rid (fra NAV Akt�rregister)';
CREATE TABLE IF NOT EXISTS po_relasjon (
    id bigint NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    po_informasjon_id bigint NOT NULL,
    relasjonsrolle character varying(100) NOT NULL,
    har_samme_bosted boolean DEFAULT false,
    fra_aktoer_id character varying(50),
    til_aktoer_id character varying(50)
);
COMMENT ON TABLE po_relasjon IS 'Angir relasjon mellom to personer (som må ligge i PO_PERSONOPPLYSNING, selv om dette er ingen db-constraint)';
COMMENT ON COLUMN po_relasjon.id IS 'Primærnøkkel';
COMMENT ON COLUMN po_relasjon.po_informasjon_id IS 'FK:';
COMMENT ON COLUMN po_relasjon.relasjonsrolle IS 'Type relasjon mellom to personer (eks. EKTE/BARN/MORA/FARA/MMOR, etc.)';
COMMENT ON COLUMN po_relasjon.har_samme_bosted IS 'Indikerer om personene i relasjonen bor på samme adresse';
COMMENT ON COLUMN po_relasjon.fra_aktoer_id IS 'Hva betyr dette?';
COMMENT ON COLUMN po_relasjon.til_aktoer_id IS 'Hva betyr dette?';
CREATE TABLE IF NOT EXISTS po_statsborgerskap (
    id bigint NOT NULL,
    fom date NOT NULL,
    tom date NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    po_informasjon_id bigint NOT NULL,
    statsborgerskap character varying(100) NOT NULL,
    aktoer_id character varying(50)
);
COMMENT ON TABLE po_statsborgerskap IS 'Personopplysning - statsborgerskap';
COMMENT ON COLUMN po_statsborgerskap.id IS 'Primærnøkkel';
COMMENT ON COLUMN po_statsborgerskap.fom IS 'Gyldig fom';
COMMENT ON COLUMN po_statsborgerskap.tom IS 'Gyldig tom';
COMMENT ON COLUMN po_statsborgerskap.po_informasjon_id IS 'FK:';
COMMENT ON COLUMN po_statsborgerskap.statsborgerskap IS 'Statsborgerskap (landkode ISO-3 country code)';
COMMENT ON COLUMN po_statsborgerskap.aktoer_id IS 'Akt�rid (fra NAV Akt�rregister)';
CREATE TABLE IF NOT EXISTS prosess_task (
    id bigint NOT NULL,
    task_type character varying(50) NOT NULL,
    prioritet smallint DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'KLAR'::character varying NOT NULL,
    task_parametere character varying(4000),
    task_payload text,
    task_gruppe character varying(250) NOT NULL,
    task_sekvens character varying(100) DEFAULT '1'::character varying NOT NULL,
    partition_key character varying(4) DEFAULT to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text),
    neste_kjoering_etter timestamp(0) without time zone DEFAULT timezone('utc'::text, now()),
    feilede_forsoek integer DEFAULT 0,
    siste_kjoering_ts timestamp(6) without time zone,
    siste_kjoering_feil_kode character varying(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(6) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    blokkert_av bigint,
    siste_kjoering_slutt_ts timestamp(6) without time zone,
    siste_kjoering_plukk_ts timestamp(6) without time zone
)
PARTITION BY LIST (status);
COMMENT ON TABLE prosess_task IS 'Inneholder tasks som skal kjøres i bakgrunnen';
COMMENT ON COLUMN prosess_task.id IS 'Primary Key';
COMMENT ON COLUMN prosess_task.task_type IS 'navn på task. Brukes til å matche riktig implementasjon';
COMMENT ON COLUMN prosess_task.prioritet IS 'prioritet på task.  Høyere tall har høyere prioritet';
COMMENT ON COLUMN prosess_task.status IS 'status på task: KLAR, NYTT_FORSOEK, FEILET, VENTER_SVAR, FERDIG';
COMMENT ON COLUMN prosess_task.task_parametere IS 'parametere angitt for en task';
COMMENT ON COLUMN prosess_task.task_payload IS 'inputdata for en task';
COMMENT ON COLUMN prosess_task.task_gruppe IS 'angir en unik id som grupperer flere ';
COMMENT ON COLUMN prosess_task.task_sekvens IS 'angir rekkefølge på task innenfor en gruppe ';
COMMENT ON COLUMN prosess_task.neste_kjoering_etter IS 'tasken skal ikke kjøeres før tidspunkt er passert';
COMMENT ON COLUMN prosess_task.feilede_forsoek IS 'antall feilede forsøk';
COMMENT ON COLUMN prosess_task.siste_kjoering_ts IS 'siste gang tasken ble forsøkt kjørt (før kjøring)';
COMMENT ON COLUMN prosess_task.siste_kjoering_feil_kode IS 'siste feilkode tasken fikk';
COMMENT ON COLUMN prosess_task.siste_kjoering_feil_tekst IS 'siste feil tasken fikk';
COMMENT ON COLUMN prosess_task.siste_kjoering_server IS 'navn på node som sist kjørte en task (server@pid)';
COMMENT ON COLUMN prosess_task.versjon IS 'angir versjon for optimistisk låsing';
COMMENT ON COLUMN prosess_task.blokkert_av IS 'Id til ProsessTask som blokkerer kjøring av denne (når status=VETO)';
COMMENT ON COLUMN prosess_task.siste_kjoering_slutt_ts IS 'tidsstempel siste gang tasken ble kjørt (etter kjøring)';
COMMENT ON COLUMN prosess_task.siste_kjoering_plukk_ts IS 'siste gang tasken ble forsøkt plukket (fra db til in-memory, før kjøring)';
CREATE TABLE IF NOT EXISTS prosess_task_feilhand (
    kode character varying(20) NOT NULL,
    navn character varying(50) NOT NULL,
    beskrivelse character varying(2000),
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    input_variabel1 numeric,
    input_variabel2 numeric
);
COMMENT ON TABLE prosess_task_feilhand IS 'Kodetabell for feilhåndterings-metoder. For eksempel antall ganger å prøve på nytt og til hvilke tidspunkt';
COMMENT ON COLUMN prosess_task_feilhand.kode IS 'Kodeverk Primary Key';
COMMENT ON COLUMN prosess_task_feilhand.navn IS 'Lesbart navn på type feilhåndtering brukt i prosesstask';
COMMENT ON COLUMN prosess_task_feilhand.beskrivelse IS 'Utdypende beskrivelse av koden';
COMMENT ON COLUMN prosess_task_feilhand.input_variabel1 IS 'Variabel 1: Dynamisk konfigurasjon for en feilhåndteringsstrategi.  Verdi og betydning er bestemt av feilhåndteringsalgoritmen';
COMMENT ON COLUMN prosess_task_feilhand.input_variabel2 IS 'Variabel 2: Dynamisk konfigurasjon for en feilhåndteringsstrategi.  Verdi og betydning er bestemt av feilhåndteringsalgoritmen';
CREATE TABLE IF NOT EXISTS prosess_task_partition_default (
    id bigint NOT NULL,
    task_type character varying(50) NOT NULL,
    prioritet smallint DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'KLAR'::character varying NOT NULL,
    task_parametere character varying(4000),
    task_payload text,
    task_gruppe character varying(250) NOT NULL,
    task_sekvens character varying(100) DEFAULT '1'::character varying NOT NULL,
    partition_key character varying(4) DEFAULT to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text),
    neste_kjoering_etter timestamp(0) without time zone DEFAULT timezone('utc'::text, now()),
    feilede_forsoek integer DEFAULT 0,
    siste_kjoering_ts timestamp(6) without time zone,
    siste_kjoering_feil_kode character varying(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(6) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    blokkert_av bigint,
    siste_kjoering_slutt_ts timestamp(6) without time zone,
    siste_kjoering_plukk_ts timestamp(6) without time zone,
    CONSTRAINT ikke_negativ_sekvens CHECK ((NOT starts_with((task_sekvens)::text, '-'::text)))
)
WITH (fillfactor='40');
CREATE TABLE IF NOT EXISTS prosess_task_partition_ferdig (
    id bigint NOT NULL,
    task_type character varying(50) NOT NULL,
    prioritet smallint DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'KLAR'::character varying NOT NULL,
    task_parametere character varying(4000),
    task_payload text,
    task_gruppe character varying(250) NOT NULL,
    task_sekvens character varying(100) DEFAULT '1'::character varying NOT NULL,
    partition_key character varying(4) DEFAULT to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text),
    neste_kjoering_etter timestamp(0) without time zone DEFAULT timezone('utc'::text, now()),
    feilede_forsoek integer DEFAULT 0,
    siste_kjoering_ts timestamp(6) without time zone,
    siste_kjoering_feil_kode character varying(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(6) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    blokkert_av bigint,
    siste_kjoering_slutt_ts timestamp(6) without time zone,
    siste_kjoering_plukk_ts timestamp(6) without time zone
)
PARTITION BY LIST (partition_key);
CREATE TABLE IF NOT EXISTS prosess_task_partition_ferdig_01 (
    id bigint NOT NULL,
    task_type character varying(50) NOT NULL,
    prioritet smallint DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'KLAR'::character varying NOT NULL,
    task_parametere character varying(4000),
    task_payload text,
    task_gruppe character varying(250) NOT NULL,
    task_sekvens character varying(100) DEFAULT '1'::character varying NOT NULL,
    partition_key character varying(4) DEFAULT to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text),
    neste_kjoering_etter timestamp(0) without time zone DEFAULT timezone('utc'::text, now()),
    feilede_forsoek integer DEFAULT 0,
    siste_kjoering_ts timestamp(6) without time zone,
    siste_kjoering_feil_kode character varying(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(6) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    blokkert_av bigint,
    siste_kjoering_slutt_ts timestamp(6) without time zone,
    siste_kjoering_plukk_ts timestamp(6) without time zone
);
CREATE TABLE IF NOT EXISTS prosess_task_partition_ferdig_02 (
    id bigint NOT NULL,
    task_type character varying(50) NOT NULL,
    prioritet smallint DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'KLAR'::character varying NOT NULL,
    task_parametere character varying(4000),
    task_payload text,
    task_gruppe character varying(250) NOT NULL,
    task_sekvens character varying(100) DEFAULT '1'::character varying NOT NULL,
    partition_key character varying(4) DEFAULT to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text),
    neste_kjoering_etter timestamp(0) without time zone DEFAULT timezone('utc'::text, now()),
    feilede_forsoek integer DEFAULT 0,
    siste_kjoering_ts timestamp(6) without time zone,
    siste_kjoering_feil_kode character varying(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(6) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    blokkert_av bigint,
    siste_kjoering_slutt_ts timestamp(6) without time zone,
    siste_kjoering_plukk_ts timestamp(6) without time zone
);
CREATE TABLE IF NOT EXISTS prosess_task_partition_ferdig_03 (
    id bigint NOT NULL,
    task_type character varying(50) NOT NULL,
    prioritet smallint DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'KLAR'::character varying NOT NULL,
    task_parametere character varying(4000),
    task_payload text,
    task_gruppe character varying(250) NOT NULL,
    task_sekvens character varying(100) DEFAULT '1'::character varying NOT NULL,
    partition_key character varying(4) DEFAULT to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text),
    neste_kjoering_etter timestamp(0) without time zone DEFAULT timezone('utc'::text, now()),
    feilede_forsoek integer DEFAULT 0,
    siste_kjoering_ts timestamp(6) without time zone,
    siste_kjoering_feil_kode character varying(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(6) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    blokkert_av bigint,
    siste_kjoering_slutt_ts timestamp(6) without time zone,
    siste_kjoering_plukk_ts timestamp(6) without time zone
);
CREATE TABLE IF NOT EXISTS prosess_task_partition_ferdig_04 (
    id bigint NOT NULL,
    task_type character varying(50) NOT NULL,
    prioritet smallint DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'KLAR'::character varying NOT NULL,
    task_parametere character varying(4000),
    task_payload text,
    task_gruppe character varying(250) NOT NULL,
    task_sekvens character varying(100) DEFAULT '1'::character varying NOT NULL,
    partition_key character varying(4) DEFAULT to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text),
    neste_kjoering_etter timestamp(0) without time zone DEFAULT timezone('utc'::text, now()),
    feilede_forsoek integer DEFAULT 0,
    siste_kjoering_ts timestamp(6) without time zone,
    siste_kjoering_feil_kode character varying(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(6) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    blokkert_av bigint,
    siste_kjoering_slutt_ts timestamp(6) without time zone,
    siste_kjoering_plukk_ts timestamp(6) without time zone
);
CREATE TABLE IF NOT EXISTS prosess_task_partition_ferdig_05 (
    id bigint NOT NULL,
    task_type character varying(50) NOT NULL,
    prioritet smallint DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'KLAR'::character varying NOT NULL,
    task_parametere character varying(4000),
    task_payload text,
    task_gruppe character varying(250) NOT NULL,
    task_sekvens character varying(100) DEFAULT '1'::character varying NOT NULL,
    partition_key character varying(4) DEFAULT to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text),
    neste_kjoering_etter timestamp(0) without time zone DEFAULT timezone('utc'::text, now()),
    feilede_forsoek integer DEFAULT 0,
    siste_kjoering_ts timestamp(6) without time zone,
    siste_kjoering_feil_kode character varying(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(6) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    blokkert_av bigint,
    siste_kjoering_slutt_ts timestamp(6) without time zone,
    siste_kjoering_plukk_ts timestamp(6) without time zone
);
CREATE TABLE IF NOT EXISTS prosess_task_partition_ferdig_06 (
    id bigint NOT NULL,
    task_type character varying(50) NOT NULL,
    prioritet smallint DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'KLAR'::character varying NOT NULL,
    task_parametere character varying(4000),
    task_payload text,
    task_gruppe character varying(250) NOT NULL,
    task_sekvens character varying(100) DEFAULT '1'::character varying NOT NULL,
    partition_key character varying(4) DEFAULT to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text),
    neste_kjoering_etter timestamp(0) without time zone DEFAULT timezone('utc'::text, now()),
    feilede_forsoek integer DEFAULT 0,
    siste_kjoering_ts timestamp(6) without time zone,
    siste_kjoering_feil_kode character varying(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(6) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    blokkert_av bigint,
    siste_kjoering_slutt_ts timestamp(6) without time zone,
    siste_kjoering_plukk_ts timestamp(6) without time zone
);
CREATE TABLE IF NOT EXISTS prosess_task_partition_ferdig_07 (
    id bigint NOT NULL,
    task_type character varying(50) NOT NULL,
    prioritet smallint DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'KLAR'::character varying NOT NULL,
    task_parametere character varying(4000),
    task_payload text,
    task_gruppe character varying(250) NOT NULL,
    task_sekvens character varying(100) DEFAULT '1'::character varying NOT NULL,
    partition_key character varying(4) DEFAULT to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text),
    neste_kjoering_etter timestamp(0) without time zone DEFAULT timezone('utc'::text, now()),
    feilede_forsoek integer DEFAULT 0,
    siste_kjoering_ts timestamp(6) without time zone,
    siste_kjoering_feil_kode character varying(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(6) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    blokkert_av bigint,
    siste_kjoering_slutt_ts timestamp(6) without time zone,
    siste_kjoering_plukk_ts timestamp(6) without time zone
);
CREATE TABLE IF NOT EXISTS prosess_task_partition_ferdig_08 (
    id bigint NOT NULL,
    task_type character varying(50) NOT NULL,
    prioritet smallint DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'KLAR'::character varying NOT NULL,
    task_parametere character varying(4000),
    task_payload text,
    task_gruppe character varying(250) NOT NULL,
    task_sekvens character varying(100) DEFAULT '1'::character varying NOT NULL,
    partition_key character varying(4) DEFAULT to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text),
    neste_kjoering_etter timestamp(0) without time zone DEFAULT timezone('utc'::text, now()),
    feilede_forsoek integer DEFAULT 0,
    siste_kjoering_ts timestamp(6) without time zone,
    siste_kjoering_feil_kode character varying(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(6) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    blokkert_av bigint,
    siste_kjoering_slutt_ts timestamp(6) without time zone,
    siste_kjoering_plukk_ts timestamp(6) without time zone
);
CREATE TABLE IF NOT EXISTS prosess_task_partition_ferdig_09 (
    id bigint NOT NULL,
    task_type character varying(50) NOT NULL,
    prioritet smallint DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'KLAR'::character varying NOT NULL,
    task_parametere character varying(4000),
    task_payload text,
    task_gruppe character varying(250) NOT NULL,
    task_sekvens character varying(100) DEFAULT '1'::character varying NOT NULL,
    partition_key character varying(4) DEFAULT to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text),
    neste_kjoering_etter timestamp(0) without time zone DEFAULT timezone('utc'::text, now()),
    feilede_forsoek integer DEFAULT 0,
    siste_kjoering_ts timestamp(6) without time zone,
    siste_kjoering_feil_kode character varying(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(6) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    blokkert_av bigint,
    siste_kjoering_slutt_ts timestamp(6) without time zone,
    siste_kjoering_plukk_ts timestamp(6) without time zone
);
CREATE TABLE IF NOT EXISTS prosess_task_partition_ferdig_10 (
    id bigint NOT NULL,
    task_type character varying(50) NOT NULL,
    prioritet smallint DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'KLAR'::character varying NOT NULL,
    task_parametere character varying(4000),
    task_payload text,
    task_gruppe character varying(250) NOT NULL,
    task_sekvens character varying(100) DEFAULT '1'::character varying NOT NULL,
    partition_key character varying(4) DEFAULT to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text),
    neste_kjoering_etter timestamp(0) without time zone DEFAULT timezone('utc'::text, now()),
    feilede_forsoek integer DEFAULT 0,
    siste_kjoering_ts timestamp(6) without time zone,
    siste_kjoering_feil_kode character varying(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(6) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    blokkert_av bigint,
    siste_kjoering_slutt_ts timestamp(6) without time zone,
    siste_kjoering_plukk_ts timestamp(6) without time zone
);
CREATE TABLE IF NOT EXISTS prosess_task_partition_ferdig_11 (
    id bigint NOT NULL,
    task_type character varying(50) NOT NULL,
    prioritet smallint DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'KLAR'::character varying NOT NULL,
    task_parametere character varying(4000),
    task_payload text,
    task_gruppe character varying(250) NOT NULL,
    task_sekvens character varying(100) DEFAULT '1'::character varying NOT NULL,
    partition_key character varying(4) DEFAULT to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text),
    neste_kjoering_etter timestamp(0) without time zone DEFAULT timezone('utc'::text, now()),
    feilede_forsoek integer DEFAULT 0,
    siste_kjoering_ts timestamp(6) without time zone,
    siste_kjoering_feil_kode character varying(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(6) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    blokkert_av bigint,
    siste_kjoering_slutt_ts timestamp(6) without time zone,
    siste_kjoering_plukk_ts timestamp(6) without time zone
);
CREATE TABLE IF NOT EXISTS prosess_task_partition_ferdig_12 (
    id bigint NOT NULL,
    task_type character varying(50) NOT NULL,
    prioritet smallint DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'KLAR'::character varying NOT NULL,
    task_parametere character varying(4000),
    task_payload text,
    task_gruppe character varying(250) NOT NULL,
    task_sekvens character varying(100) DEFAULT '1'::character varying NOT NULL,
    partition_key character varying(4) DEFAULT to_char((CURRENT_DATE)::timestamp with time zone, 'MM'::text),
    neste_kjoering_etter timestamp(0) without time zone DEFAULT timezone('utc'::text, now()),
    feilede_forsoek integer DEFAULT 0,
    siste_kjoering_ts timestamp(6) without time zone,
    siste_kjoering_feil_kode character varying(50),
    siste_kjoering_feil_tekst text,
    siste_kjoering_server character varying(50),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(6) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    blokkert_av bigint,
    siste_kjoering_slutt_ts timestamp(6) without time zone,
    siste_kjoering_plukk_ts timestamp(6) without time zone
);
CREATE TABLE IF NOT EXISTS prosess_task_type (
    kode character varying(50) NOT NULL,
    navn character varying(50),
    feil_maks_forsoek numeric(10,0) DEFAULT 1 NOT NULL,
    feil_sek_mellom_forsoek numeric(10,0) DEFAULT 30 NOT NULL,
    feilhandtering_algoritme character varying(20) DEFAULT 'DEFAULT'::character varying,
    beskrivelse character varying(2000),
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    cron_expression character varying(200)
);
COMMENT ON TABLE prosess_task_type IS 'Kodetabell for typer prosesser med beskrivelse og informasjon om hvilken feilhåndteringen som skal benyttes';
COMMENT ON COLUMN prosess_task_type.kode IS 'Kodeverk Primary Key';
COMMENT ON COLUMN prosess_task_type.navn IS 'Lesbart navn på prosesstasktype';
COMMENT ON COLUMN prosess_task_type.feil_maks_forsoek IS 'Maksimalt anntall forsøk på rekjøring om noe går galt';
COMMENT ON COLUMN prosess_task_type.feil_sek_mellom_forsoek IS 'Ventetid i sekunder mellom hvert forsøk på rekjøring om noe har gått galt';
COMMENT ON COLUMN prosess_task_type.feilhandtering_algoritme IS 'FK:PROSESS_TASK_FEILHAND Fremmednøkkel til tabell som viser detaljer om hvordan en feilsituasjon skal håndteres';
COMMENT ON COLUMN prosess_task_type.beskrivelse IS 'Utdypende beskrivelse av koden';
COMMENT ON COLUMN prosess_task_type.cron_expression IS 'Cron-expression for når oppgaven skal kjøres på nytt';
CREATE TABLE IF NOT EXISTS prosess_triggere (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    triggere_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS pt_trigger (
    id bigint NOT NULL,
    triggere_id bigint,
    arsak character varying(100) NOT NULL,
    periode daterange NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS pt_triggere (
    id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS publiser_behandling_arbeidstabell (
    id bigint NOT NULL,
    "kjøring_id" uuid NOT NULL,
    behandling_id bigint NOT NULL,
    status character varying(20) NOT NULL,
    endring text,
    "kjøring_type" character varying(20) NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON TABLE publiser_behandling_arbeidstabell IS 'arbeidstabell for publisering av behandlinger';
COMMENT ON COLUMN publiser_behandling_arbeidstabell."kjøring_id" IS 'unik for kjøringen';
COMMENT ON COLUMN publiser_behandling_arbeidstabell.endring IS 'satt hvis status = FEILET';
CREATE TABLE IF NOT EXISTS rs_soknadsfrist (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    overstyrt_id bigint,
    avklart_id bigint,
    versjon bigint DEFAULT 0 NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS rs_vilkars_resultat (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    vilkarene_id bigint,
    versjon bigint DEFAULT 0 NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON TABLE rs_vilkars_resultat IS 'Behandlingsgrunnlag for arbeid, inntekt og ytelser (aggregat)';
COMMENT ON COLUMN rs_vilkars_resultat.id IS 'Primary Key';
COMMENT ON COLUMN rs_vilkars_resultat.behandling_id IS 'FK: BEHANDLING Fremmednøkkel for kobling til behandling';
CREATE SEQUENCE IF NOT EXISTS seq_aksjonspunkt
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_aksjonspunkt_sporing
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_behandling
    START WITH 3000049
    INCREMENT BY 50
    MINVALUE 3000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_behandling_arsak
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_behandling_merknad
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_behandling_steg_tilstand
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_behandling_vedtak
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_behandling_vedtak_varsel
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_br_andel
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_br_beregningsresultat
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_br_feriepenger
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_br_feriepenger_pr_aar
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_br_periode
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_br_resultat_behandling
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_br_sats
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_bruker
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_diagnostikk_fagsak_logg
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_etterkontroll
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_fagsak
    START WITH 2000049
    INCREMENT BY 50
    MINVALUE 2000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_fagsak_prosess_task
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_gr_soeknad
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_gr_personopplysning
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_historikkinnslag
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_historikkinnslag_del
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_historikkinnslag_dok_link
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_historikkinnslag_felt
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_journalpost
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_mottatt_dokument
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_notat_aktoer
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_notat_aktoer_tekst
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_notat_sak
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_notat_sak_tekst
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_oppgave_behandling_kobling
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_po_adresse
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_po_informasjon
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_po_personopplysning
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_po_personstatus
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_po_relasjon
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_po_statsborgerskap
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_prosess_task
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_prosess_task_gruppe
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_prosess_triggere
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_pt_trigger
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_pt_triggere
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_publiser_behandling_arbeidstabell
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_rs_soknadsfrist
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_rs_vilkars_resultat
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_saksnummer
    START WITH 10000049
    INCREMENT BY 50
    MINVALUE 10000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_sf_avklart_dokument
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_sf_avklart_dokumenter
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_soeknad
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_soeknad_angitt_person
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_soeknad_vedlegg
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;


CREATE SEQUENCE IF NOT EXISTS seq_tilbakekreving_inntrekk
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_tilbakekreving_valg
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;


CREATE SEQUENCE IF NOT EXISTS seq_totrinnresultatgrunnlag
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_totrinnsvurdering
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_ung_gr
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_ung_gr_soeknadsperiode
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_ung_gr_ungdomsprogramperiode
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_ung_sats_periode
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_ung_sats_perioder
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_ung_soeknadsperiode
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_ung_soeknadsperioder
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_ung_ungdomsprogramperiode
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_ung_ungdomsprogramperioder
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_ung_uttak_periode
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_ung_uttak_perioder
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_vilkar
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_vilkar_periode
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_vilkar_resultat
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS seq_vurder_aarsak_ttvurdering
    START WITH 1000049
    INCREMENT BY 50
    MINVALUE 1000000
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS sf_avklart_dokument (
    id bigint NOT NULL,
    dokumenter_id bigint,
    journalpost_id character varying(20) NOT NULL,
    godkjent boolean NOT NULL,
    gyldig_fra date,
    begrunnelse character varying(4000) NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS sf_avklart_dokumenter (
    id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS so_soeknad (
    id bigint NOT NULL,
    soeknadsdato date NOT NULL,
    tilleggsopplysninger character varying(4000),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    elektronisk_registrert boolean DEFAULT false NOT NULL,
    mottatt_dato date,
    begrunnelse_for_sen_innsending character varying(2000),
    er_endringssoeknad boolean DEFAULT false NOT NULL,
    bruker_rolle character varying(100) DEFAULT '-'::character varying NOT NULL,
    sprak_kode character varying(100) DEFAULT 'NB'::character varying NOT NULL,
    fom date,
    tom date,
    journalpost_id character varying(20),
    soeknad_id character varying(100),
    CONSTRAINT chk_so_soeknad_fom_tom CHECK ((((fom IS NULL) AND (tom IS NULL)) OR (fom <= tom)))
);
COMMENT ON TABLE so_soeknad IS 'Søknad om foreldrepenger';
COMMENT ON COLUMN so_soeknad.id IS 'Primary Key';
COMMENT ON COLUMN so_soeknad.soeknadsdato IS 'Søknadsdato';
COMMENT ON COLUMN so_soeknad.tilleggsopplysninger IS 'Tilleggsopplysninger';
COMMENT ON COLUMN so_soeknad.elektronisk_registrert IS 'Elektronisk registrert søknad';
COMMENT ON COLUMN so_soeknad.mottatt_dato IS 'Mottatt dato';
COMMENT ON COLUMN so_soeknad.begrunnelse_for_sen_innsending IS 'Begrunnelse for sen innsending';
COMMENT ON COLUMN so_soeknad.er_endringssoeknad IS 'Er endringssøknad';
COMMENT ON COLUMN so_soeknad.bruker_rolle IS 'FK:RELASJONSROLLE_TYPE';
COMMENT ON COLUMN so_soeknad.sprak_kode IS 'Kode for hvilket språk søker sender søknad på';
CREATE TABLE IF NOT EXISTS so_soeknad_angitt_person (
    id bigint NOT NULL,
    soeknad_id bigint NOT NULL,
    rolle character varying(100) NOT NULL,
    aktoer_id character varying(50),
    situasjon_kode character varying(100),
    tilleggsopplysninger text,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS soeknad_vedlegg (
    id bigint NOT NULL,
    skjemanummer character varying(20),
    tilleggsinfo character varying(2000),
    innsendingsvalg character varying(100) NOT NULL,
    soeknad_id bigint NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    vedlegg_pakrevd boolean DEFAULT false NOT NULL
);
COMMENT ON TABLE soeknad_vedlegg IS 'Vedlegg til søknad, eks: terminbekreftelse';
COMMENT ON COLUMN soeknad_vedlegg.id IS 'Primary Key';
COMMENT ON COLUMN soeknad_vedlegg.skjemanummer IS 'Skjemanummer på vedlegget';
COMMENT ON COLUMN soeknad_vedlegg.tilleggsinfo IS 'Fritekst relatert til vedlegg';
COMMENT ON COLUMN soeknad_vedlegg.innsendingsvalg IS 'FK:INNSENDINGSVALG Fremmednøkkel til tabell over mulige innsendingsvalg';
COMMENT ON COLUMN soeknad_vedlegg.soeknad_id IS 'FK:SOEKNAD Fremmednøkkel som viser til søknaden';
COMMENT ON COLUMN soeknad_vedlegg.vedlegg_pakrevd IS 'Om vedlegget er påkrevd';
CREATE TABLE IF NOT EXISTS tilbakekreving_inntrekk (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    aktiv boolean DEFAULT false NOT NULL,
    avslaatt_inntrekk boolean DEFAULT false NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON TABLE tilbakekreving_inntrekk IS 'Inneholder informasjon om inntrekk skal skrus av ved iverksettelse mot økonomi';
COMMENT ON COLUMN tilbakekreving_inntrekk.id IS 'Primærnøkkel';
COMMENT ON COLUMN tilbakekreving_inntrekk.aktiv IS 'Angir om raden er aktiv (J/N)';
COMMENT ON COLUMN tilbakekreving_inntrekk.avslaatt_inntrekk IS 'Angir om inntrekk skal skrus av ved iverksettelse (J/N)';
CREATE TABLE IF NOT EXISTS tilbakekreving_valg (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    aktiv boolean DEFAULT false NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    tbk_vilkaar_oppfylt boolean DEFAULT false,
    grunn_til_reduksjon boolean DEFAULT false,
    videre_behandling character varying(100) NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    varseltekst character varying(12000)
);
COMMENT ON TABLE tilbakekreving_valg IS 'Lagre valg av tilbakekreving';
COMMENT ON COLUMN tilbakekreving_valg.id IS 'Primary Key';
COMMENT ON COLUMN tilbakekreving_valg.aktiv IS 'Angir status av tilbakekreving valg (J/N)';
COMMENT ON COLUMN tilbakekreving_valg.versjon IS 'Versjon av tilbakekreving valg';
COMMENT ON COLUMN tilbakekreving_valg.tbk_vilkaar_oppfylt IS 'Angir om tilbakekrevingsvilkår oppfylt (J/N/null)';
COMMENT ON COLUMN tilbakekreving_valg.grunn_til_reduksjon IS 'Angir om grunner til særlige grunner til reduksjon er tilstede (J/N/null)';
COMMENT ON COLUMN tilbakekreving_valg.videre_behandling IS 'Angir hvordan saken behandles videre';
COMMENT ON COLUMN tilbakekreving_valg.varseltekst IS 'Tilleggsinformasjon for varsel til bruker om tilbakekreving';
CREATE TABLE IF NOT EXISTS totrinnresultatgrunnlag (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    aktiv boolean DEFAULT false NOT NULL,
    iay_grunnlag_uuid uuid,
    beregningsgrunnlag_grunnlag_uuid uuid
);
COMMENT ON TABLE totrinnresultatgrunnlag IS 'Tabell som held grunnlagsId for data vist i panelet fra beslutter.';
COMMENT ON COLUMN totrinnresultatgrunnlag.id IS 'PK';
COMMENT ON COLUMN totrinnresultatgrunnlag.behandling_id IS 'FK til behandling som hører til totrinn resultatet';
COMMENT ON COLUMN totrinnresultatgrunnlag.iay_grunnlag_uuid IS 'Unik UUID for IAY grunnlag til utvortes bruk. Representerer en immutable og unikt identifiserbar instans av dette aggregatet';
COMMENT ON COLUMN totrinnresultatgrunnlag.beregningsgrunnlag_grunnlag_uuid IS 'Unik UUID for beregningsgrunnlag til utvortes bruk. Representerer en immutable og unikt identifiserbar instans av dette aggregatet';
CREATE TABLE IF NOT EXISTS totrinnsvurdering (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    aksjonspunkt_def character varying(100) NOT NULL,
    godkjent boolean DEFAULT false NOT NULL,
    begrunnelse character varying(4000),
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    aktiv boolean DEFAULT false NOT NULL
);
COMMENT ON TABLE totrinnsvurdering IS 'Statisk read only totrinnsvurdering som brukes til å vise vurderinger til aksjonspunkter uavhengig av status';
COMMENT ON COLUMN totrinnsvurdering.aksjonspunkt_def IS 'Aksjonspunkt som vurderes';
COMMENT ON COLUMN totrinnsvurdering.godkjent IS 'Beslutters godkjenning';
COMMENT ON COLUMN totrinnsvurdering.begrunnelse IS 'Beslutters begrunnelse';
CREATE TABLE IF NOT EXISTS ung_gr (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    ung_sats_perioder_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    aktiv boolean NOT NULL,
    ung_uttak_perioder_id bigint
);
CREATE TABLE IF NOT EXISTS ung_gr_soeknadsperiode (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    relevant_soknadsperiode_id bigint,
    oppgitt_soknadsperiode_id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS ung_gr_ungdomsprogramperiode (
    id bigint NOT NULL,
    behandling_id bigint NOT NULL,
    ung_ungdomsprogramperioder_id bigint NOT NULL,
    aktiv boolean DEFAULT true NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS ung_sats_periode (
    id bigint NOT NULL,
    ung_sats_perioder_id bigint NOT NULL,
    periode daterange NOT NULL,
    dagsats numeric(19,4) NOT NULL,
    "grunnbeløp" numeric(12,2) NOT NULL,
    "grunnbeløp_faktor" numeric(19,4) NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    sats_type character varying(100) NOT NULL,
    antall_barn integer NOT NULL,
    dagsats_barnetillegg numeric(19,4) NOT NULL
);
COMMENT ON TABLE ung_sats_periode IS 'Periode for satser og tilhørende informasjon relatert til satsberegning av ungdomsytelsen';
COMMENT ON COLUMN ung_sats_periode.antall_barn IS 'Antall barn benyttet i beregning av dagsats for barnetillegg';
COMMENT ON COLUMN ung_sats_periode.dagsats_barnetillegg IS 'Utbetalt dagsats for barnetillegg';
CREATE TABLE IF NOT EXISTS ung_sats_perioder (
    id bigint NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS ung_soeknadsperiode (
    id bigint NOT NULL,
    journalpost_id character varying(20) NOT NULL,
    ung_soeknadsperioder_id bigint,
    fom date NOT NULL,
    tom date NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS ung_soeknadsperioder (
    id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS ung_ungdomsprogramperiode (
    id bigint NOT NULL,
    ung_ungdomsprogramperioder_id bigint,
    fom date NOT NULL,
    tom date NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS ung_ungdomsprogramperioder (
    id bigint NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS ung_uttak_periode (
    id bigint NOT NULL,
    ung_uttak_perioder_id bigint NOT NULL,
    periode daterange NOT NULL,
    utbetalingsgrad numeric(19,4) NOT NULL,
    avslag_aarsak character varying(100),
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS ung_uttak_perioder (
    id bigint NOT NULL,
    regel_input oid NOT NULL,
    regel_sporing oid NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
CREATE TABLE IF NOT EXISTS virksomhet (
    id bigint NOT NULL,
    orgnr character varying(100) NOT NULL,
    navn character varying(400),
    registrert date,
    oppstart date,
    avsluttet date,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    opplysninger_oppdatert_tid timestamp(3) without time zone DEFAULT NULL::timestamp without time zone NOT NULL,
    organisasjonstype character varying(100) DEFAULT '-'::character varying NOT NULL
);
COMMENT ON TABLE virksomhet IS 'Virksomhet fra enhetsregisteret';
COMMENT ON COLUMN virksomhet.id IS 'Primærnøkkel';
COMMENT ON COLUMN virksomhet.orgnr IS 'Bedriftens unike identifikator i enhetsregisteret';
COMMENT ON COLUMN virksomhet.navn IS 'Bedriftens navn i enhetsregisteret';
COMMENT ON COLUMN virksomhet.registrert IS 'Når virksomheten ble registrert i enhetsregisteret';
COMMENT ON COLUMN virksomhet.oppstart IS 'Når næringen startet opp';
COMMENT ON COLUMN virksomhet.avsluttet IS 'Når næringen opphørte';
COMMENT ON COLUMN virksomhet.opplysninger_oppdatert_tid IS 'Siste tidspunkt for forespørsel til enhetsregisteret';
COMMENT ON COLUMN virksomhet.organisasjonstype IS 'Organisasjonstype';
CREATE TABLE IF NOT EXISTS vr_vilkar (
    id bigint NOT NULL,
    vilkar_resultat_id bigint NOT NULL,
    vilkar_type character varying(100) NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON TABLE vr_vilkar IS 'Vilkår som inneholder utfallet for en gitt vilkårstype.';
COMMENT ON COLUMN vr_vilkar.id IS 'Primary Key';
COMMENT ON COLUMN vr_vilkar.vilkar_resultat_id IS 'FK:INNGANGSVILKAR_RESULTAT Fremmednøkkel til tabellen som viser de avklarte inngangsvilkårene som er grunnlaget for behandlingsresultatet';
COMMENT ON COLUMN vr_vilkar.vilkar_type IS 'Vilkår type kodeverk';
CREATE TABLE IF NOT EXISTS vr_vilkar_periode (
    id bigint NOT NULL,
    vilkar_id bigint,
    fom date NOT NULL,
    tom date NOT NULL,
    manuelt_vurdert boolean NOT NULL,
    utfall character varying(100) NOT NULL,
    merknad character varying(100) NOT NULL,
    overstyrt_utfall character varying(100) DEFAULT '-'::character varying NOT NULL,
    avslag_kode character varying(100),
    merknad_parametere character varying(1000),
    regel_evaluering text,
    regel_input text,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone,
    begrunnelse character varying(4000),
    regel_evaluering_oid oid,
    regel_input_oid oid
);
CREATE TABLE IF NOT EXISTS vr_vilkar_resultat (
    id bigint NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON TABLE vr_vilkar_resultat IS 'En samling av inngangsvilkår resultat.';
COMMENT ON COLUMN vr_vilkar_resultat.id IS 'Primary Key';
CREATE TABLE IF NOT EXISTS vurder_aarsak_ttvurdering (
    id bigint NOT NULL,
    aarsak_type character varying(100) NOT NULL,
    totrinnsvurdering_id bigint NOT NULL,
    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);
COMMENT ON TABLE vurder_aarsak_ttvurdering IS 'Årsaken til at aksjonspunkt må vurderes på nytt';
COMMENT ON COLUMN vurder_aarsak_ttvurdering.aarsak_type IS 'Type årsak til totrinnsvurdering (eks. FEIL_LOV, FEIL_FAKTA, etc.)';
ALTER TABLE ONLY prosess_task ATTACH PARTITION prosess_task_partition_default DEFAULT;
ALTER TABLE ONLY prosess_task ATTACH PARTITION prosess_task_partition_ferdig FOR VALUES IN ('FERDIG');
ALTER TABLE ONLY prosess_task_partition_ferdig ATTACH PARTITION prosess_task_partition_ferdig_01 FOR VALUES IN ('01');
ALTER TABLE ONLY prosess_task_partition_ferdig ATTACH PARTITION prosess_task_partition_ferdig_02 FOR VALUES IN ('02');
ALTER TABLE ONLY prosess_task_partition_ferdig ATTACH PARTITION prosess_task_partition_ferdig_03 FOR VALUES IN ('03');
ALTER TABLE ONLY prosess_task_partition_ferdig ATTACH PARTITION prosess_task_partition_ferdig_04 FOR VALUES IN ('04');
ALTER TABLE ONLY prosess_task_partition_ferdig ATTACH PARTITION prosess_task_partition_ferdig_05 FOR VALUES IN ('05');
ALTER TABLE ONLY prosess_task_partition_ferdig ATTACH PARTITION prosess_task_partition_ferdig_06 FOR VALUES IN ('06');
ALTER TABLE ONLY prosess_task_partition_ferdig ATTACH PARTITION prosess_task_partition_ferdig_07 FOR VALUES IN ('07');
ALTER TABLE ONLY prosess_task_partition_ferdig ATTACH PARTITION prosess_task_partition_ferdig_08 FOR VALUES IN ('08');
ALTER TABLE ONLY prosess_task_partition_ferdig ATTACH PARTITION prosess_task_partition_ferdig_09 FOR VALUES IN ('09');
ALTER TABLE ONLY prosess_task_partition_ferdig ATTACH PARTITION prosess_task_partition_ferdig_10 FOR VALUES IN ('10');
ALTER TABLE ONLY prosess_task_partition_ferdig ATTACH PARTITION prosess_task_partition_ferdig_11 FOR VALUES IN ('11');
ALTER TABLE ONLY prosess_task_partition_ferdig ATTACH PARTITION prosess_task_partition_ferdig_12 FOR VALUES IN ('12');
ALTER TABLE ONLY aksjonspunkt_sporing
    ADD CONSTRAINT aksjonspunkt_sporing_pkey PRIMARY KEY (id);
ALTER TABLE ONLY behandling_merknad
    ADD CONSTRAINT behandling_merknad_pkey PRIMARY KEY (id);
ALTER TABLE ONLY br_andel
    ADD CONSTRAINT br_andel_ikke_overlapp_periode_ag EXCLUDE USING gist (arbeidsgiver_orgnr WITH =, arbeidsforhold_intern_id WITH =, br_periode_id WITH =, beregningsresultat_id WITH =, arbeidsgiver_aktor_id WITH =, inntektskategori WITH =, arbeidsforhold_type WITH =, aktivitet_status WITH =, periode WITH &&) WHERE ((bruker_er_mottaker = true));
ALTER TABLE ONLY br_andel
    ADD CONSTRAINT br_andel_ikke_overlapp_periode_bruker EXCLUDE USING gist (arbeidsgiver_orgnr WITH =, arbeidsforhold_intern_id WITH =, br_periode_id WITH =, beregningsresultat_id WITH =, arbeidsgiver_aktor_id WITH =, inntektskategori WITH =, arbeidsforhold_type WITH =, aktivitet_status WITH =, periode WITH &&) WHERE ((bruker_er_mottaker = false));
ALTER TABLE ONLY diagnostikk_fagsak_logg
    ADD CONSTRAINT diagnostikk_fagsak_logg_pkey PRIMARY KEY (id);
ALTER TABLE ONLY notat_aktoer
    ADD CONSTRAINT notat_aktoer_pkey PRIMARY KEY (id);
ALTER TABLE ONLY notat_aktoer_tekst
    ADD CONSTRAINT notat_aktoer_tekst_notat_id_versjon_key UNIQUE (notat_id, versjon);
ALTER TABLE ONLY notat_aktoer_tekst
    ADD CONSTRAINT notat_aktoer_tekst_pkey PRIMARY KEY (id);
ALTER TABLE ONLY notat_aktoer
    ADD CONSTRAINT notat_aktoer_uuid_versjon_key UNIQUE (uuid, versjon);
ALTER TABLE ONLY notat_sak
    ADD CONSTRAINT notat_sak_pkey PRIMARY KEY (id);
ALTER TABLE ONLY notat_sak_tekst
    ADD CONSTRAINT notat_sak_tekst_notat_id_versjon_key UNIQUE (notat_id, versjon);
ALTER TABLE ONLY notat_sak_tekst
    ADD CONSTRAINT notat_sak_tekst_pkey PRIMARY KEY (id);
ALTER TABLE ONLY notat_sak
    ADD CONSTRAINT notat_sak_uuid_versjon_key UNIQUE (uuid, versjon);
ALTER TABLE ONLY aksjonspunkt
    ADD CONSTRAINT pk_aksjonspunkt PRIMARY KEY (id);
ALTER TABLE ONLY behandling
    ADD CONSTRAINT pk_behandling PRIMARY KEY (id);
ALTER TABLE ONLY behandling_arsak
    ADD CONSTRAINT pk_behandling_arsak PRIMARY KEY (id);
ALTER TABLE ONLY behandling_steg_tilstand
    ADD CONSTRAINT pk_behandling_steg_tilstand PRIMARY KEY (id);
ALTER TABLE ONLY behandling_vedtak
    ADD CONSTRAINT pk_behandling_vedtak PRIMARY KEY (id);
ALTER TABLE ONLY br_andel
    ADD CONSTRAINT pk_br_andel PRIMARY KEY (id);
ALTER TABLE ONLY br_beregningsresultat
    ADD CONSTRAINT pk_br_beregningsresultat PRIMARY KEY (id);
ALTER TABLE ONLY br_feriepenger
    ADD CONSTRAINT pk_br_feriepenger PRIMARY KEY (id);
ALTER TABLE ONLY br_feriepenger_pr_aar
    ADD CONSTRAINT pk_br_feriepenger_pr_aar PRIMARY KEY (id);
ALTER TABLE ONLY br_periode
    ADD CONSTRAINT pk_br_periode PRIMARY KEY (id);
ALTER TABLE ONLY br_resultat_behandling
    ADD CONSTRAINT pk_br_resultat_behandling PRIMARY KEY (id);
ALTER TABLE ONLY br_sats
    ADD CONSTRAINT pk_br_sats PRIMARY KEY (id);
ALTER TABLE ONLY etterkontroll
    ADD CONSTRAINT pk_etterkontroll PRIMARY KEY (id);
ALTER TABLE ONLY fagsak
    ADD CONSTRAINT pk_fagsak PRIMARY KEY (id);
ALTER TABLE ONLY fagsak_prosess_task
    ADD CONSTRAINT pk_fagsak_prosess_task PRIMARY KEY (id);
ALTER TABLE ONLY gr_personopplysning
    ADD CONSTRAINT pk_gr_personopplysning PRIMARY KEY (id);
ALTER TABLE ONLY gr_soeknad
    ADD CONSTRAINT pk_gr_soeknad PRIMARY KEY (id);
ALTER TABLE ONLY historikkinnslag
    ADD CONSTRAINT pk_historikkinnslag PRIMARY KEY (id);
ALTER TABLE ONLY historikkinnslag_del
    ADD CONSTRAINT pk_historikkinnslag_del PRIMARY KEY (id);
ALTER TABLE ONLY historikkinnslag_dok_link
    ADD CONSTRAINT pk_historikkinnslag_dok_link PRIMARY KEY (id);
ALTER TABLE ONLY historikkinnslag_felt
    ADD CONSTRAINT pk_historikkinnslag_felt PRIMARY KEY (id);
ALTER TABLE ONLY journalpost
    ADD CONSTRAINT pk_journalpost PRIMARY KEY (id);
ALTER TABLE ONLY mottatt_dokument
    ADD CONSTRAINT pk_mottatt_dokument PRIMARY KEY (id);
ALTER TABLE ONLY mottatt_hendelse
    ADD CONSTRAINT pk_mottatt_hendelse PRIMARY KEY (hendelse_uid);
ALTER TABLE ONLY oppgave_behandling_kobling
    ADD CONSTRAINT pk_oppgave_behandling_kobling PRIMARY KEY (id);
ALTER TABLE ONLY po_adresse
    ADD CONSTRAINT pk_po_adresse PRIMARY KEY (id);
ALTER TABLE ONLY po_informasjon
    ADD CONSTRAINT pk_po_informasjon PRIMARY KEY (id);
ALTER TABLE ONLY po_personopplysning
    ADD CONSTRAINT pk_po_personopplysning PRIMARY KEY (id);
ALTER TABLE ONLY po_personstatus
    ADD CONSTRAINT pk_po_personstatus PRIMARY KEY (id);
ALTER TABLE ONLY po_relasjon
    ADD CONSTRAINT pk_po_relasjon PRIMARY KEY (id);
ALTER TABLE ONLY po_statsborgerskap
    ADD CONSTRAINT pk_po_statsborgerskap PRIMARY KEY (id);
ALTER TABLE ONLY prosess_task_feilhand
    ADD CONSTRAINT pk_prosess_task_feilhand PRIMARY KEY (kode);
ALTER TABLE ONLY prosess_task_partition_default
    ADD CONSTRAINT pk_prosess_task_partition_default PRIMARY KEY (id);
ALTER TABLE ONLY prosess_task_partition_ferdig_01
    ADD CONSTRAINT pk_prosess_task_partition_ferdig_01 PRIMARY KEY (id);
ALTER TABLE ONLY prosess_task_partition_ferdig_02
    ADD CONSTRAINT pk_prosess_task_partition_ferdig_02 PRIMARY KEY (id);
ALTER TABLE ONLY prosess_task_partition_ferdig_03
    ADD CONSTRAINT pk_prosess_task_partition_ferdig_03 PRIMARY KEY (id);
ALTER TABLE ONLY prosess_task_partition_ferdig_04
    ADD CONSTRAINT pk_prosess_task_partition_ferdig_04 PRIMARY KEY (id);
ALTER TABLE ONLY prosess_task_partition_ferdig_05
    ADD CONSTRAINT pk_prosess_task_partition_ferdig_05 PRIMARY KEY (id);
ALTER TABLE ONLY prosess_task_partition_ferdig_06
    ADD CONSTRAINT pk_prosess_task_partition_ferdig_06 PRIMARY KEY (id);
ALTER TABLE ONLY prosess_task_partition_ferdig_07
    ADD CONSTRAINT pk_prosess_task_partition_ferdig_07 PRIMARY KEY (id);
ALTER TABLE ONLY prosess_task_partition_ferdig_08
    ADD CONSTRAINT pk_prosess_task_partition_ferdig_08 PRIMARY KEY (id);
ALTER TABLE ONLY prosess_task_partition_ferdig_09
    ADD CONSTRAINT pk_prosess_task_partition_ferdig_09 PRIMARY KEY (id);
ALTER TABLE ONLY prosess_task_partition_ferdig_10
    ADD CONSTRAINT pk_prosess_task_partition_ferdig_10 PRIMARY KEY (id);
ALTER TABLE ONLY prosess_task_partition_ferdig_11
    ADD CONSTRAINT pk_prosess_task_partition_ferdig_11 PRIMARY KEY (id);
ALTER TABLE ONLY prosess_task_partition_ferdig_12
    ADD CONSTRAINT pk_prosess_task_partition_ferdig_12 PRIMARY KEY (id);
ALTER TABLE ONLY prosess_task_type
    ADD CONSTRAINT pk_prosess_task_type PRIMARY KEY (kode);
ALTER TABLE ONLY so_soeknad
    ADD CONSTRAINT pk_so_soeknad PRIMARY KEY (id);
ALTER TABLE ONLY soeknad_vedlegg
    ADD CONSTRAINT pk_soeknad_vedlegg PRIMARY KEY (id);
ALTER TABLE ONLY tilbakekreving_inntrekk
    ADD CONSTRAINT pk_tilbakekreving_inntrekk PRIMARY KEY (id);
ALTER TABLE ONLY tilbakekreving_valg
    ADD CONSTRAINT pk_tilbakekreving_valg PRIMARY KEY (id);
ALTER TABLE ONLY totrinnresultatgrunnlag
    ADD CONSTRAINT pk_totrinnresultatgrunnlag PRIMARY KEY (id);
ALTER TABLE ONLY totrinnsvurdering
    ADD CONSTRAINT pk_totrinnsvurdering PRIMARY KEY (id);
ALTER TABLE ONLY vr_vilkar
    ADD CONSTRAINT pk_vilkar PRIMARY KEY (id);
ALTER TABLE ONLY vr_vilkar_resultat
    ADD CONSTRAINT pk_vilkar_resultat PRIMARY KEY (id);
ALTER TABLE ONLY virksomhet
    ADD CONSTRAINT pk_virksomhet PRIMARY KEY (id);
ALTER TABLE ONLY vurder_aarsak_ttvurdering
    ADD CONSTRAINT pk_vurder_aarsak_ttvurdering PRIMARY KEY (id);
ALTER TABLE ONLY prosess_triggere
    ADD CONSTRAINT prosess_triggere_pkey PRIMARY KEY (id);
ALTER TABLE ONLY pt_trigger
    ADD CONSTRAINT pt_trigger_pkey PRIMARY KEY (id);
ALTER TABLE ONLY pt_triggere
    ADD CONSTRAINT pt_triggere_pkey PRIMARY KEY (id);
ALTER TABLE ONLY publiser_behandling_arbeidstabell
    ADD CONSTRAINT publiser_behandling_arbeidstabell_pkey PRIMARY KEY (id);
ALTER TABLE ONLY rs_soknadsfrist
    ADD CONSTRAINT rs_soknadsfrist_pkey PRIMARY KEY (id);
ALTER TABLE ONLY rs_vilkars_resultat
    ADD CONSTRAINT rs_vilkars_resultat_pkey PRIMARY KEY (id);
ALTER TABLE ONLY sf_avklart_dokument
    ADD CONSTRAINT sf_avklart_dokument_pkey PRIMARY KEY (id);
ALTER TABLE ONLY sf_avklart_dokumenter
    ADD CONSTRAINT sf_avklart_dokumenter_pkey PRIMARY KEY (id);
ALTER TABLE ONLY ung_gr_soeknadsperiode
    ADD CONSTRAINT ung_gr_soeknadsperiode_pkey PRIMARY KEY (id);
ALTER TABLE ONLY ung_gr_ungdomsprogramperiode
    ADD CONSTRAINT ung_gr_ungdomsprogramperiode_pkey PRIMARY KEY (id);
ALTER TABLE ONLY ung_sats_periode
    ADD CONSTRAINT ung_sats_periode_ikke_overlapp EXCLUDE USING gist (ung_sats_perioder_id WITH =, periode WITH &&);
ALTER TABLE ONLY ung_sats_periode
    ADD CONSTRAINT ung_sats_periode_pkey PRIMARY KEY (id);
ALTER TABLE ONLY ung_sats_perioder
    ADD CONSTRAINT ung_sats_perioder_pkey PRIMARY KEY (id);
ALTER TABLE ONLY ung_soeknadsperiode
    ADD CONSTRAINT ung_soeknadsperiode_pkey PRIMARY KEY (id);
ALTER TABLE ONLY ung_soeknadsperioder
    ADD CONSTRAINT ung_soeknadsperioder_pkey PRIMARY KEY (id);
ALTER TABLE ONLY ung_ungdomsprogramperiode
    ADD CONSTRAINT ung_ungdomsprogramperiode_pkey PRIMARY KEY (id);
ALTER TABLE ONLY ung_ungdomsprogramperioder
    ADD CONSTRAINT ung_ungdomsprogramperioder_pkey PRIMARY KEY (id);
ALTER TABLE ONLY ung_uttak_periode
    ADD CONSTRAINT ung_uttak_periode_ikke_overlapp EXCLUDE USING gist (ung_uttak_perioder_id WITH =, periode WITH &&);
ALTER TABLE ONLY ung_uttak_periode
    ADD CONSTRAINT ung_uttak_periode_pkey PRIMARY KEY (id);
ALTER TABLE ONLY ung_uttak_perioder
    ADD CONSTRAINT ung_uttak_perioder_pkey PRIMARY KEY (id);
ALTER TABLE ONLY fagsak
    ADD CONSTRAINT unik_fagsak_1 EXCLUDE USING gist (ytelse_type WITH =, bruker_aktoer_id WITH =, pleietrengende_aktoer_id WITH =, periode WITH &&) WHERE (((pleietrengende_aktoer_id IS NOT NULL) AND (periode IS NOT NULL)));
ALTER TABLE ONLY fagsak
    ADD CONSTRAINT unik_fagsak_2 EXCLUDE USING gist (ytelse_type WITH =, bruker_aktoer_id WITH =, periode WITH &&) WHERE (((pleietrengende_aktoer_id IS NULL) AND (periode IS NOT NULL)));
ALTER TABLE ONLY fagsak
    ADD CONSTRAINT unik_fagsak_3 EXCLUDE USING gist (periode WITH &&, bruker_aktoer_id WITH =, relatert_person_aktoer_id WITH =, ytelse_type WITH =) WHERE (((relatert_person_aktoer_id IS NOT NULL) AND (periode IS NOT NULL)));
ALTER TABLE ONLY vr_vilkar_periode
    ADD CONSTRAINT vilkar_periode_pkey PRIMARY KEY (id);
CREATE INDEX IF NOT EXISTS br_andel_02 ON br_andel USING btree (beregningsresultat_id);
CREATE INDEX IF NOT EXISTS diagnostikk_fagsak_logg_fagsak_id_idx ON diagnostikk_fagsak_logg USING btree (fagsak_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_aksjonspunkt_1 ON aksjonspunkt USING btree (behandling_id, aksjonspunkt_def);
CREATE INDEX IF NOT EXISTS idx_aksjonspunkt_6 ON aksjonspunkt USING btree (behandling_steg_funnet);
CREATE INDEX IF NOT EXISTS idx_aksjonspunkt_7 ON aksjonspunkt USING btree (aksjonspunkt_def);
CREATE INDEX IF NOT EXISTS idx_aksjonspunkt_8 ON aksjonspunkt USING btree (vent_aarsak);
CREATE INDEX IF NOT EXISTS idx_aksjonspunkt_9 ON aksjonspunkt USING btree (aksjonspunkt_status);
CREATE INDEX IF NOT EXISTS idx_behandling_1 ON behandling USING btree (fagsak_id);
CREATE INDEX IF NOT EXISTS idx_behandling_2 ON behandling USING btree (behandling_status);
CREATE INDEX IF NOT EXISTS idx_behandling_3 ON behandling USING btree (behandling_type);
CREATE INDEX IF NOT EXISTS idx_behandling_6 ON behandling USING btree (startpunkt_type);
CREATE INDEX IF NOT EXISTS idx_behandling_7 ON behandling USING btree (original_behandling_id);
CREATE INDEX IF NOT EXISTS idx_behandling_arsak_6 ON behandling_arsak USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_behandling_arsak_7 ON behandling_arsak USING btree (original_behandling_id);
CREATE INDEX IF NOT EXISTS idx_behandling_merknad_1 ON behandling_merknad USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_behandling_steg_tilstand_1 ON behandling_steg_tilstand USING btree (behandling_steg);
CREATE INDEX IF NOT EXISTS idx_behandling_steg_tilstand_6 ON behandling_steg_tilstand USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_behandling_steg_tilstand_7 ON behandling_steg_tilstand USING btree (behandling_steg_status);
CREATE INDEX IF NOT EXISTS idx_behandling_vedtak_6 ON behandling_vedtak USING btree (iverksetting_status);
CREATE INDEX IF NOT EXISTS idx_beregningsres_andel_11 ON br_andel USING btree (arbeidsforhold_intern_id);
CREATE INDEX IF NOT EXISTS idx_beregningsresultat_andel_2 ON br_andel USING btree (aktivitet_status);
CREATE INDEX IF NOT EXISTS idx_beregningsresultat_andel_7 ON br_andel USING btree (arbeidsgiver_orgnr);
CREATE INDEX IF NOT EXISTS idx_br_andel_01 ON br_andel USING btree (br_periode_id);
CREATE INDEX IF NOT EXISTS idx_br_feriepenger_1 ON br_feriepenger USING btree (beregningsresultat_fp_id);
CREATE INDEX IF NOT EXISTS idx_br_feriepenger_pr_aar_1 ON br_feriepenger_pr_aar USING btree (br_feriepenger_id);
CREATE INDEX IF NOT EXISTS idx_br_feriepenger_pr_aar_2 ON br_feriepenger_pr_aar USING btree (beregningsresultat_andel_id);
CREATE INDEX IF NOT EXISTS idx_br_periode_01 ON br_periode USING btree (beregningsresultat_fp_id);
CREATE INDEX IF NOT EXISTS idx_br_periode_02 ON br_periode USING btree (br_periode_fom);
CREATE INDEX IF NOT EXISTS idx_br_periode_03 ON br_periode USING btree (br_periode_tom);
CREATE INDEX IF NOT EXISTS idx_etterkontroll_1 ON etterkontroll USING btree (fagsak_id);
CREATE INDEX IF NOT EXISTS idx_etterkontroll_2 ON etterkontroll USING btree (kontroll_type);
CREATE INDEX IF NOT EXISTS idx_etterkontroll_3 ON etterkontroll USING btree (behandlet, kontroll_tid);
CREATE UNIQUE INDEX IF NOT EXISTS idx_etterkontroll_4 ON etterkontroll USING btree (behandling_id, kontroll_type) WHERE (behandlet = false);
CREATE INDEX IF NOT EXISTS idx_fagsak_1 ON fagsak USING btree (fagsak_status);
CREATE INDEX IF NOT EXISTS idx_fagsak_10 ON fagsak USING btree (relatert_person_aktoer_id);
CREATE INDEX IF NOT EXISTS idx_fagsak_4 ON fagsak USING gist (pleietrengende_aktoer_id, ytelse_type, periode) WHERE ((pleietrengende_aktoer_id IS NOT NULL) AND (periode IS NOT NULL));
CREATE INDEX IF NOT EXISTS idx_fagsak_7 ON fagsak USING btree (ytelse_type);
CREATE INDEX IF NOT EXISTS idx_fagsak_8 ON fagsak USING btree (bruker_aktoer_id);
CREATE INDEX IF NOT EXISTS idx_fagsak_9 ON fagsak USING btree (pleietrengende_aktoer_id);
CREATE INDEX IF NOT EXISTS idx_fagsak_prosess_task_1 ON fagsak_prosess_task USING btree (fagsak_id);
CREATE INDEX IF NOT EXISTS idx_fagsak_prosess_task_2 ON fagsak_prosess_task USING btree (prosess_task_id);
CREATE INDEX IF NOT EXISTS idx_fagsak_prosess_task_3 ON fagsak_prosess_task USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_fagsak_prosess_task_4 ON fagsak_prosess_task USING btree (gruppe_sekvensnr);
CREATE INDEX IF NOT EXISTS idx_fagsak_prosess_task_5 ON fagsak_prosess_task USING btree (task_type);
CREATE INDEX IF NOT EXISTS idx_gr_personopplysning_01 ON gr_personopplysning USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_gr_personopplysning_03 ON gr_personopplysning USING btree (registrert_informasjon_id);
CREATE INDEX IF NOT EXISTS idx_gr_personopplysning_04 ON gr_personopplysning USING btree (overstyrt_informasjon_id);
CREATE INDEX IF NOT EXISTS idx_gr_soeknad_1 ON gr_soeknad USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_gr_soeknad_2 ON gr_soeknad USING btree (soeknad_id);
CREATE INDEX IF NOT EXISTS idx_gr_soeknadsperiode_oppgitt_soeknadperiode ON ung_gr_soeknadsperiode USING btree (oppgitt_soknadsperiode_id);
CREATE INDEX IF NOT EXISTS idx_histinnslag_dok_link_01 ON historikkinnslag_dok_link USING btree (historikkinnslag_id);
CREATE INDEX IF NOT EXISTS idx_historikkinnslag_01 ON historikkinnslag USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_historikkinnslag_03 ON historikkinnslag USING btree (historikkinnslag_type);
CREATE INDEX IF NOT EXISTS idx_historikkinnslag_6 ON historikkinnslag USING btree (fagsak_id);
CREATE INDEX IF NOT EXISTS idx_historikkinnslag_7 ON historikkinnslag USING btree (historikk_aktoer_id);
CREATE INDEX IF NOT EXISTS idx_historikkinnslag_del_1 ON historikkinnslag_del USING btree (historikkinnslag_id);
CREATE INDEX IF NOT EXISTS idx_historikkinnslag_felt_1 ON historikkinnslag_felt USING btree (historikkinnslag_del_id);
CREATE INDEX IF NOT EXISTS idx_historikkinnslag_felt_3 ON historikkinnslag_felt USING btree (navn, kl_navn);
CREATE INDEX IF NOT EXISTS idx_historikkinnslag_felt_4 ON historikkinnslag_felt USING btree (til_verdi_kode, kl_til_verdi);
CREATE INDEX IF NOT EXISTS idx_historikkinnslag_felt_5 ON historikkinnslag_felt USING btree (fra_verdi_kode, kl_fra_verdi);
CREATE INDEX IF NOT EXISTS idx_journalpost_1 ON journalpost USING btree (fagsak_id);
CREATE INDEX IF NOT EXISTS idx_mottatt_dokument_11 ON mottatt_dokument USING btree (kildesystem);
CREATE INDEX IF NOT EXISTS idx_mottatt_dokument_12 ON mottatt_dokument USING btree (innsendingstidspunkt);
CREATE INDEX IF NOT EXISTS idx_mottatt_dokument_2 ON mottatt_dokument USING btree (dokument_kategori);
CREATE INDEX IF NOT EXISTS idx_mottatt_dokument_6 ON mottatt_dokument USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_mottatt_dokument_7 ON mottatt_dokument USING btree (fagsak_id);
CREATE INDEX IF NOT EXISTS idx_mottatt_dokument_8 ON mottatt_dokument USING btree (forsendelse_id);
CREATE INDEX IF NOT EXISTS idx_mottatt_dokument_9 ON mottatt_dokument USING btree (journalpost_id);
CREATE INDEX IF NOT EXISTS idx_mottatte_dokument_1 ON mottatt_dokument USING btree (type);
CREATE INDEX IF NOT EXISTS idx_notat_aktor ON notat_aktoer USING btree (aktoer_id, ytelse_type) WHERE (aktiv = true);
CREATE INDEX IF NOT EXISTS idx_notat_sak ON notat_sak USING btree (fagsak_id) WHERE (aktiv = true);
CREATE INDEX IF NOT EXISTS idx_oppgave_beh_kob_beh_id ON oppgave_behandling_kobling USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_oppgave_behandling_kobli_6 ON oppgave_behandling_kobling USING btree (oppgave_aarsak);
CREATE INDEX IF NOT EXISTS idx_po_adresse_2 ON po_adresse USING btree (po_informasjon_id);
CREATE INDEX IF NOT EXISTS idx_po_adresse_3 ON po_adresse USING btree (aktoer_id);
CREATE INDEX IF NOT EXISTS idx_po_adresse_5 ON po_adresse USING btree (adresse_type);
CREATE INDEX IF NOT EXISTS idx_po_personopplysning_1 ON po_personopplysning USING btree (po_informasjon_id);
CREATE INDEX IF NOT EXISTS idx_po_personopplysning_2 ON po_personopplysning USING btree (aktoer_id);
CREATE INDEX IF NOT EXISTS idx_po_personopplysning_3 ON po_personopplysning USING btree (region);
CREATE INDEX IF NOT EXISTS idx_po_personopplysning_4 ON po_personopplysning USING btree (bruker_kjoenn);
CREATE INDEX IF NOT EXISTS idx_po_personopplysning_5 ON po_personopplysning USING btree (sivilstand_type);
CREATE INDEX IF NOT EXISTS idx_po_personstatus_1 ON po_personstatus USING btree (po_informasjon_id);
CREATE INDEX IF NOT EXISTS idx_po_personstatus_2 ON po_personstatus USING btree (aktoer_id);
CREATE INDEX IF NOT EXISTS idx_po_personstatus_4 ON po_personstatus USING btree (personstatus);
CREATE INDEX IF NOT EXISTS idx_po_relasjon_1 ON po_relasjon USING btree (po_informasjon_id);
CREATE INDEX IF NOT EXISTS idx_po_relasjon_2 ON po_relasjon USING btree (fra_aktoer_id);
CREATE INDEX IF NOT EXISTS idx_po_relasjon_3 ON po_relasjon USING btree (til_aktoer_id);
CREATE INDEX IF NOT EXISTS idx_po_relasjon_5 ON po_relasjon USING btree (relasjonsrolle);
CREATE INDEX IF NOT EXISTS idx_po_statsborgerskap_1 ON po_statsborgerskap USING btree (po_informasjon_id);
CREATE INDEX IF NOT EXISTS idx_po_statsborgerskap_2 ON po_statsborgerskap USING btree (aktoer_id);
CREATE INDEX IF NOT EXISTS idx_po_statsborgerskap_4 ON po_statsborgerskap USING btree (statsborgerskap);
CREATE INDEX IF NOT EXISTS idx_prosess_task_blokkert_av ON ONLY prosess_task USING btree (blokkert_av) WHERE (blokkert_av IS NOT NULL);
CREATE INDEX IF NOT EXISTS idx_prosess_task_default_gruppe_sekvens ON prosess_task_partition_default USING btree (task_gruppe, length((task_sekvens)::text), task_sekvens) WHERE ((status)::text = ANY ((ARRAY['FEILET'::character varying, 'KLAR'::character varying, 'VENTER_SVAR'::character varying, 'SUSPENDERT'::character varying, 'VETO'::character varying])::text[]));
CREATE INDEX IF NOT EXISTS idx_prosess_task_type_1 ON prosess_task_type USING btree (feilhandtering_algoritme);
CREATE INDEX IF NOT EXISTS idx_prosess_triggere_1 ON prosess_triggere USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_prosess_triggere_2 ON prosess_triggere USING btree (triggere_id);
CREATE INDEX IF NOT EXISTS idx_pt_trigger_1 ON pt_trigger USING btree (triggere_id);
CREATE INDEX IF NOT EXISTS idx_publiser_innsyn_kjoring_id ON publiser_behandling_arbeidstabell USING btree ("kjøring_id");
CREATE INDEX IF NOT EXISTS idx_publiser_innsyn_status_kjoring_id ON publiser_behandling_arbeidstabell USING btree (status, "kjøring_id");
CREATE INDEX IF NOT EXISTS idx_res_beregningsresultat_f_6 ON br_resultat_behandling USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_rs_soknadsfrist_1 ON rs_soknadsfrist USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_rs_soknadsfrist_2 ON rs_soknadsfrist USING btree (overstyrt_id);
CREATE INDEX IF NOT EXISTS idx_rs_soknadsfrist_3 ON rs_soknadsfrist USING btree (avklart_id);
CREATE INDEX IF NOT EXISTS idx_rs_vilkars_resultat_1 ON rs_vilkars_resultat USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_rs_vilkars_resultat_2 ON rs_vilkars_resultat USING btree (vilkarene_id);
CREATE INDEX IF NOT EXISTS idx_rs_vilkars_resultat_3 ON rs_vilkars_resultat USING btree (behandling_id, aktiv);
CREATE INDEX IF NOT EXISTS idx_sats_1 ON br_sats USING btree (sats_type);
CREATE INDEX IF NOT EXISTS idx_sf_avklart_dokument_1 ON sf_avklart_dokument USING btree (dokumenter_id);
CREATE INDEX IF NOT EXISTS idx_so_soeknad_12 ON so_soeknad USING btree (journalpost_id);
CREATE INDEX IF NOT EXISTS idx_so_soeknad_13 ON so_soeknad USING btree (soeknad_id);
CREATE INDEX IF NOT EXISTS idx_soeknad_11 ON so_soeknad USING btree (bruker_rolle);
CREATE INDEX IF NOT EXISTS idx_soeknad_12 ON so_soeknad USING btree (sprak_kode);
CREATE INDEX IF NOT EXISTS idx_tilbakekreving_inntrekk_1 ON tilbakekreving_inntrekk USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_tilbakekreving_valg_1 ON tilbakekreving_valg USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_tilbakekreving_valg_2 ON tilbakekreving_valg USING btree (videre_behandling);
CREATE INDEX IF NOT EXISTS idx_torinn_res_gr_01 ON totrinnresultatgrunnlag USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_torinn_res_gr_06 ON totrinnresultatgrunnlag USING btree (iay_grunnlag_uuid);
CREATE INDEX IF NOT EXISTS idx_torinn_res_gr_07 ON totrinnresultatgrunnlag USING btree (beregningsgrunnlag_grunnlag_uuid);
CREATE INDEX IF NOT EXISTS idx_totrinnsvurdering ON totrinnsvurdering USING btree (aksjonspunkt_def);
CREATE INDEX IF NOT EXISTS idx_totrinnsvurdering_2 ON totrinnsvurdering USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_ung_gr_soeknadsperiode_behandling ON ung_gr_soeknadsperiode USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_ung_gr_ungdomsprogramperiode_behandling ON ung_gr_ungdomsprogramperiode USING btree (behandling_id);
CREATE INDEX IF NOT EXISTS idx_ung_gr_ungdomsprogramperiode_perioder ON ung_gr_ungdomsprogramperiode USING btree (ung_ungdomsprogramperioder_id);
CREATE INDEX IF NOT EXISTS idx_ung_sats_periode_perioder ON ung_sats_periode USING btree (ung_sats_perioder_id);
CREATE INDEX IF NOT EXISTS idx_ung_soeknadsperiode_journalpost ON ung_soeknadsperiode USING btree (journalpost_id);
CREATE INDEX IF NOT EXISTS idx_ung_soeknadsperiode_perioder ON ung_soeknadsperiode USING btree (ung_soeknadsperioder_id);
CREATE INDEX IF NOT EXISTS idx_ung_ungdomsprogramperiode_perioder ON ung_ungdomsprogramperiode USING btree (ung_ungdomsprogramperioder_id);
CREATE INDEX IF NOT EXISTS idx_ung_uttak_periode_perioder ON ung_uttak_periode USING btree (ung_uttak_perioder_id);
CREATE INDEX IF NOT EXISTS idx_vedlegg_1 ON soeknad_vedlegg USING btree (innsendingsvalg);
CREATE INDEX IF NOT EXISTS idx_vedlegg_2 ON soeknad_vedlegg USING btree (soeknad_id);
CREATE INDEX IF NOT EXISTS idx_vedtak_1 ON behandling_vedtak USING btree (vedtak_resultat_type);
CREATE INDEX IF NOT EXISTS idx_vedtak_2 ON behandling_vedtak USING btree (ansvarlig_saksbehandler);
CREATE INDEX IF NOT EXISTS idx_vedtak_3 ON behandling_vedtak USING btree (vedtak_dato);
CREATE INDEX IF NOT EXISTS idx_vilkar_2 ON vr_vilkar USING btree (vilkar_type);
CREATE INDEX IF NOT EXISTS idx_vilkar_3 ON vr_vilkar USING btree (vilkar_resultat_id);
CREATE INDEX IF NOT EXISTS idx_virksomhet_1 ON virksomhet USING btree (orgnr);
CREATE INDEX IF NOT EXISTS idx_virksomhet_2 ON virksomhet USING btree (organisasjonstype);
CREATE INDEX IF NOT EXISTS idx_vr_vilkar_periode_2 ON vr_vilkar_periode USING btree (vilkar_id);
CREATE INDEX IF NOT EXISTS idx_vr_vilkar_periode_3 ON vr_vilkar_periode USING btree (fom, tom);
CREATE INDEX IF NOT EXISTS idx_vr_vilkar_periode_4 ON vr_vilkar_periode USING btree (avslag_kode);
CREATE INDEX IF NOT EXISTS idx_vr_vilkar_periode_5 ON vr_vilkar_periode USING btree (vilkar_id, avslag_kode) WHERE (((avslag_kode)::text <> '-'::text) AND (avslag_kode IS NOT NULL));
CREATE INDEX IF NOT EXISTS idx_vurder_aarsak ON vurder_aarsak_ttvurdering USING btree (totrinnsvurdering_id);
CREATE INDEX IF NOT EXISTS idx_vurder_aarsak_2 ON vurder_aarsak_ttvurdering USING btree (aarsak_type);
CREATE INDEX IF NOT EXISTS prosess_task_partition_default_blokkert_av_idx ON prosess_task_partition_default USING btree (blokkert_av) WHERE (blokkert_av IS NOT NULL);
CREATE INDEX IF NOT EXISTS prosess_task_partition_ferdig_blokkert_av_idx ON ONLY prosess_task_partition_ferdig USING btree (blokkert_av) WHERE (blokkert_av IS NOT NULL);
CREATE INDEX IF NOT EXISTS prosess_task_partition_ferdig_01_blokkert_av_idx ON prosess_task_partition_ferdig_01 USING btree (blokkert_av) WHERE (blokkert_av IS NOT NULL);
CREATE INDEX IF NOT EXISTS prosess_task_partition_ferdig_02_blokkert_av_idx ON prosess_task_partition_ferdig_02 USING btree (blokkert_av) WHERE (blokkert_av IS NOT NULL);
CREATE INDEX IF NOT EXISTS prosess_task_partition_ferdig_03_blokkert_av_idx ON prosess_task_partition_ferdig_03 USING btree (blokkert_av) WHERE (blokkert_av IS NOT NULL);
CREATE INDEX IF NOT EXISTS prosess_task_partition_ferdig_04_blokkert_av_idx ON prosess_task_partition_ferdig_04 USING btree (blokkert_av) WHERE (blokkert_av IS NOT NULL);
CREATE INDEX IF NOT EXISTS prosess_task_partition_ferdig_05_blokkert_av_idx ON prosess_task_partition_ferdig_05 USING btree (blokkert_av) WHERE (blokkert_av IS NOT NULL);
CREATE INDEX IF NOT EXISTS prosess_task_partition_ferdig_06_blokkert_av_idx ON prosess_task_partition_ferdig_06 USING btree (blokkert_av) WHERE (blokkert_av IS NOT NULL);
CREATE INDEX IF NOT EXISTS prosess_task_partition_ferdig_07_blokkert_av_idx ON prosess_task_partition_ferdig_07 USING btree (blokkert_av) WHERE (blokkert_av IS NOT NULL);
CREATE INDEX IF NOT EXISTS prosess_task_partition_ferdig_08_blokkert_av_idx ON prosess_task_partition_ferdig_08 USING btree (blokkert_av) WHERE (blokkert_av IS NOT NULL);
CREATE INDEX IF NOT EXISTS prosess_task_partition_ferdig_09_blokkert_av_idx ON prosess_task_partition_ferdig_09 USING btree (blokkert_av) WHERE (blokkert_av IS NOT NULL);
CREATE INDEX IF NOT EXISTS prosess_task_partition_ferdig_10_blokkert_av_idx ON prosess_task_partition_ferdig_10 USING btree (blokkert_av) WHERE (blokkert_av IS NOT NULL);
CREATE INDEX IF NOT EXISTS prosess_task_partition_ferdig_11_blokkert_av_idx ON prosess_task_partition_ferdig_11 USING btree (blokkert_av) WHERE (blokkert_av IS NOT NULL);
CREATE INDEX IF NOT EXISTS prosess_task_partition_ferdig_12_blokkert_av_idx ON prosess_task_partition_ferdig_12 USING btree (blokkert_av) WHERE (blokkert_av IS NOT NULL);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_behandling_03 ON behandling USING btree (uuid);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_behandling_steg_tilstand_01 ON behandling_steg_tilstand USING btree (behandling_id, behandling_steg) WHERE (aktiv = true);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_behandling_vedtak_02 ON behandling_vedtak USING btree (behandling_id);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_br_resultat_behandling_99 ON br_resultat_behandling USING btree (behandling_id) WHERE (aktiv = true);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_fagsak_1 ON fagsak USING btree (saksnummer);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_fagsak_3 ON fagsak USING btree (ytelse_type, bruker_aktoer_id) WHERE ((pleietrengende_aktoer_id IS NULL) AND (periode IS NULL));
CREATE UNIQUE INDEX IF NOT EXISTS uidx_fagsak_prosess_task_1 ON fagsak_prosess_task USING btree (fagsak_id, prosess_task_id);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_gr_personopplysning_99 ON gr_personopplysning USING btree (behandling_id) WHERE (aktiv = true);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_gr_soeknad_99 ON gr_soeknad USING btree (behandling_id) WHERE (aktiv = true);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_historikkinnslag_01 ON historikkinnslag USING btree (uuid);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_mottatt_dokument_01 ON mottatt_dokument USING btree (journalpost_id, fagsak_id, type);
CREATE INDEX IF NOT EXISTS uidx_notat_aktoer_tekst ON notat_aktoer_tekst USING btree (notat_id) WHERE (aktiv = true);
CREATE INDEX IF NOT EXISTS uidx_notat_sak_tekst ON notat_sak_tekst USING btree (notat_id) WHERE (aktiv = true);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_prosess_triggere_01 ON prosess_triggere USING btree (behandling_id) WHERE (aktiv = true);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_rs_soknadsfrist_01 ON rs_soknadsfrist USING btree (behandling_id) WHERE (aktiv = true);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_rs_vilkars_resultat_02 ON rs_vilkars_resultat USING btree (behandling_id) WHERE (aktiv = true);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_tilbakekreving_inntrekk_99 ON tilbakekreving_inntrekk USING btree (behandling_id) WHERE (aktiv = true);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_tilbakekreving_valg_99 ON tilbakekreving_valg USING btree (behandling_id) WHERE (aktiv = true);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_totrinnresultatgrunnlag_99 ON totrinnresultatgrunnlag USING btree (behandling_id) WHERE (aktiv = true);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_totrinnsvurdering_99 ON totrinnsvurdering USING btree (behandling_id, aksjonspunkt_def) WHERE (aktiv = true);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_ung_gr_1 ON ung_gr USING btree (behandling_id) WHERE (aktiv = true);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_ung_gr_soeknadsperiode_aktiv_behandling ON ung_gr_soeknadsperiode USING btree (behandling_id) WHERE (aktiv = true);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_ung_gr_ungdomsprogramperiode_aktiv_behandling ON ung_gr_ungdomsprogramperiode USING btree (behandling_id) WHERE (aktiv = true);
ALTER INDEX idx_prosess_task_blokkert_av ATTACH PARTITION prosess_task_partition_default_blokkert_av_idx;
ALTER INDEX prosess_task_partition_ferdig_blokkert_av_idx ATTACH PARTITION prosess_task_partition_ferdig_01_blokkert_av_idx;
ALTER INDEX prosess_task_partition_ferdig_blokkert_av_idx ATTACH PARTITION prosess_task_partition_ferdig_02_blokkert_av_idx;
ALTER INDEX prosess_task_partition_ferdig_blokkert_av_idx ATTACH PARTITION prosess_task_partition_ferdig_03_blokkert_av_idx;
ALTER INDEX prosess_task_partition_ferdig_blokkert_av_idx ATTACH PARTITION prosess_task_partition_ferdig_04_blokkert_av_idx;
ALTER INDEX prosess_task_partition_ferdig_blokkert_av_idx ATTACH PARTITION prosess_task_partition_ferdig_05_blokkert_av_idx;
ALTER INDEX prosess_task_partition_ferdig_blokkert_av_idx ATTACH PARTITION prosess_task_partition_ferdig_06_blokkert_av_idx;
ALTER INDEX prosess_task_partition_ferdig_blokkert_av_idx ATTACH PARTITION prosess_task_partition_ferdig_07_blokkert_av_idx;
ALTER INDEX prosess_task_partition_ferdig_blokkert_av_idx ATTACH PARTITION prosess_task_partition_ferdig_08_blokkert_av_idx;
ALTER INDEX prosess_task_partition_ferdig_blokkert_av_idx ATTACH PARTITION prosess_task_partition_ferdig_09_blokkert_av_idx;
ALTER INDEX prosess_task_partition_ferdig_blokkert_av_idx ATTACH PARTITION prosess_task_partition_ferdig_10_blokkert_av_idx;
ALTER INDEX prosess_task_partition_ferdig_blokkert_av_idx ATTACH PARTITION prosess_task_partition_ferdig_11_blokkert_av_idx;
ALTER INDEX prosess_task_partition_ferdig_blokkert_av_idx ATTACH PARTITION prosess_task_partition_ferdig_12_blokkert_av_idx;
ALTER INDEX idx_prosess_task_blokkert_av ATTACH PARTITION prosess_task_partition_ferdig_blokkert_av_idx;
ALTER TABLE ONLY behandling
    ADD CONSTRAINT behandling_original_behandling_id_fkey FOREIGN KEY (original_behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY diagnostikk_fagsak_logg
    ADD CONSTRAINT diagnostikk_fagsak_logg_fagsak_id_fkey FOREIGN KEY (fagsak_id) REFERENCES fagsak(id);
ALTER TABLE ONLY etterkontroll
    ADD CONSTRAINT etterkontroll_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY aksjonspunkt
    ADD CONSTRAINT fk_aksjonspunkt_2 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY aksjonspunkt_sporing
    ADD CONSTRAINT fk_aksjonspunkt_sporing_01 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY behandling
    ADD CONSTRAINT fk_behandling_1 FOREIGN KEY (fagsak_id) REFERENCES fagsak(id);
ALTER TABLE ONLY behandling_arsak
    ADD CONSTRAINT fk_behandling_arsak_1 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY behandling_arsak
    ADD CONSTRAINT fk_behandling_arsak_2 FOREIGN KEY (original_behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY historikkinnslag
    ADD CONSTRAINT fk_behandling_id FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY behandling_merknad
    ADD CONSTRAINT fk_behandling_merknad_1 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY behandling_steg_tilstand
    ADD CONSTRAINT fk_behandling_steg_tilstand_1 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY behandling_vedtak
    ADD CONSTRAINT fk_behandling_vedtak_01 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY br_andel
    ADD CONSTRAINT fk_beregningsresultat_andel_1 FOREIGN KEY (br_periode_id) REFERENCES br_periode(id);
ALTER TABLE ONLY br_andel
    ADD CONSTRAINT fk_br_andel_2 FOREIGN KEY (beregningsresultat_id) REFERENCES br_beregningsresultat(id);
ALTER TABLE ONLY br_feriepenger
    ADD CONSTRAINT fk_br_feriepenger_1 FOREIGN KEY (beregningsresultat_fp_id) REFERENCES br_beregningsresultat(id);
ALTER TABLE ONLY br_feriepenger_pr_aar
    ADD CONSTRAINT fk_br_feriepenger_pr_aar_1 FOREIGN KEY (br_feriepenger_id) REFERENCES br_feriepenger(id);
ALTER TABLE ONLY br_feriepenger_pr_aar
    ADD CONSTRAINT fk_br_feriepenger_pr_aar_2 FOREIGN KEY (beregningsresultat_andel_id) REFERENCES br_andel(id);
ALTER TABLE ONLY br_periode
    ADD CONSTRAINT fk_br_periode_1 FOREIGN KEY (beregningsresultat_fp_id) REFERENCES br_beregningsresultat(id);
ALTER TABLE ONLY etterkontroll
    ADD CONSTRAINT fk_etterkontroll_1 FOREIGN KEY (fagsak_id) REFERENCES fagsak(id);
ALTER TABLE ONLY gr_personopplysning
    ADD CONSTRAINT fk_gr_personoppl_beh FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY gr_personopplysning
    ADD CONSTRAINT fk_gr_personopplysning_03 FOREIGN KEY (registrert_informasjon_id) REFERENCES po_informasjon(id);
ALTER TABLE ONLY gr_personopplysning
    ADD CONSTRAINT fk_gr_personopplysning_04 FOREIGN KEY (overstyrt_informasjon_id) REFERENCES po_informasjon(id);
ALTER TABLE ONLY gr_soeknad
    ADD CONSTRAINT fk_gr_soeknad_1 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY gr_soeknad
    ADD CONSTRAINT fk_gr_soeknad_2 FOREIGN KEY (soeknad_id) REFERENCES so_soeknad(id);
ALTER TABLE ONLY historikkinnslag
    ADD CONSTRAINT fk_historikkinnslag_3 FOREIGN KEY (fagsak_id) REFERENCES fagsak(id);
ALTER TABLE ONLY historikkinnslag_del
    ADD CONSTRAINT fk_historikkinnslag_del_1 FOREIGN KEY (historikkinnslag_id) REFERENCES historikkinnslag(id);
ALTER TABLE ONLY historikkinnslag_felt
    ADD CONSTRAINT fk_historikkinnslag_felt_1 FOREIGN KEY (historikkinnslag_del_id) REFERENCES historikkinnslag_del(id);
ALTER TABLE ONLY historikkinnslag_dok_link
    ADD CONSTRAINT fk_historikkinnslag_id FOREIGN KEY (historikkinnslag_id) REFERENCES historikkinnslag(id);
ALTER TABLE ONLY journalpost
    ADD CONSTRAINT fk_journalpost FOREIGN KEY (fagsak_id) REFERENCES fagsak(id);
ALTER TABLE ONLY mottatt_dokument
    ADD CONSTRAINT fk_mottatt_dokument_02 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY mottatt_dokument
    ADD CONSTRAINT fk_mottatt_dokument_04 FOREIGN KEY (fagsak_id) REFERENCES fagsak(id);
ALTER TABLE ONLY oppgave_behandling_kobling
    ADD CONSTRAINT fk_oppgave_beh_kobling_2 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY po_adresse
    ADD CONSTRAINT fk_po_adresse_2 FOREIGN KEY (po_informasjon_id) REFERENCES po_informasjon(id);
ALTER TABLE ONLY po_personopplysning
    ADD CONSTRAINT fk_po_personopplysning_2 FOREIGN KEY (po_informasjon_id) REFERENCES po_informasjon(id);
ALTER TABLE ONLY po_personstatus
    ADD CONSTRAINT fk_po_personstatus_2 FOREIGN KEY (po_informasjon_id) REFERENCES po_informasjon(id);
ALTER TABLE ONLY po_relasjon
    ADD CONSTRAINT fk_po_relasjon_2 FOREIGN KEY (po_informasjon_id) REFERENCES po_informasjon(id);
ALTER TABLE ONLY po_statsborgerskap
    ADD CONSTRAINT fk_po_statsborgerskap_2 FOREIGN KEY (po_informasjon_id) REFERENCES po_informasjon(id);
ALTER TABLE ONLY prosess_task_type
    ADD CONSTRAINT fk_prosess_task_type_1 FOREIGN KEY (feilhandtering_algoritme) REFERENCES prosess_task_feilhand(kode);
ALTER TABLE ONLY prosess_triggere
    ADD CONSTRAINT fk_prosess_triggere_1 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY prosess_triggere
    ADD CONSTRAINT fk_prosess_triggere_2 FOREIGN KEY (triggere_id) REFERENCES pt_triggere(id);
ALTER TABLE ONLY br_resultat_behandling
    ADD CONSTRAINT fk_res_beregningsresultat_fp_1 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY rs_soknadsfrist
    ADD CONSTRAINT fk_rs_soknadsfrist_1 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY rs_soknadsfrist
    ADD CONSTRAINT fk_rs_soknadsfrist_2 FOREIGN KEY (overstyrt_id) REFERENCES sf_avklart_dokumenter(id);
ALTER TABLE ONLY rs_soknadsfrist
    ADD CONSTRAINT fk_rs_soknadsfrist_3 FOREIGN KEY (avklart_id) REFERENCES sf_avklart_dokumenter(id);
ALTER TABLE ONLY rs_vilkars_resultat
    ADD CONSTRAINT fk_rs_vilkars_resultat_1 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY rs_vilkars_resultat
    ADD CONSTRAINT fk_rs_vilkars_resultat_2 FOREIGN KEY (vilkarene_id) REFERENCES vr_vilkar_resultat(id);
ALTER TABLE ONLY tilbakekreving_inntrekk
    ADD CONSTRAINT fk_tilbakekreving_inntrekk_1 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY tilbakekreving_valg
    ADD CONSTRAINT fk_tilbakekreving_valg_1 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY totrinnresultatgrunnlag
    ADD CONSTRAINT fk_totrinnresultatgrunnlag_1 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY totrinnsvurdering
    ADD CONSTRAINT fk_totrinnsvurdering_2 FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY ung_gr
    ADD CONSTRAINT fk_ung_gr_behandling FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY ung_sats_periode
    ADD CONSTRAINT fk_ung_gr_ung_sats_perioder FOREIGN KEY (ung_sats_perioder_id) REFERENCES ung_sats_perioder(id);
ALTER TABLE ONLY ung_gr
    ADD CONSTRAINT fk_ung_gr_ung_sats_perioder FOREIGN KEY (ung_sats_perioder_id) REFERENCES ung_sats_perioder(id);
ALTER TABLE ONLY ung_uttak_periode
    ADD CONSTRAINT fk_ung_gr_ung_uttak_perioder FOREIGN KEY (ung_uttak_perioder_id) REFERENCES ung_uttak_perioder(id);
ALTER TABLE ONLY ung_gr
    ADD CONSTRAINT fk_ung_gr_ung_uttak_perioder FOREIGN KEY (ung_uttak_perioder_id) REFERENCES ung_uttak_perioder(id);
ALTER TABLE ONLY soeknad_vedlegg
    ADD CONSTRAINT fk_vedlegg_2 FOREIGN KEY (soeknad_id) REFERENCES so_soeknad(id);
ALTER TABLE ONLY vr_vilkar
    ADD CONSTRAINT fk_vilkar_3 FOREIGN KEY (vilkar_resultat_id) REFERENCES vr_vilkar_resultat(id);
ALTER TABLE ONLY vurder_aarsak_ttvurdering
    ADD CONSTRAINT fk_vurder_aarsak_ttvurdering_1 FOREIGN KEY (totrinnsvurdering_id) REFERENCES totrinnsvurdering(id);
ALTER TABLE ONLY notat_aktoer_tekst
    ADD CONSTRAINT notat_aktoer_tekst_notat_id_fkey FOREIGN KEY (notat_id) REFERENCES notat_aktoer(id);
ALTER TABLE ONLY notat_sak
    ADD CONSTRAINT notat_sak_fagsak_id_fkey FOREIGN KEY (fagsak_id) REFERENCES fagsak(id);
ALTER TABLE ONLY notat_sak_tekst
    ADD CONSTRAINT notat_sak_tekst_notat_id_fkey FOREIGN KEY (notat_id) REFERENCES notat_sak(id);
ALTER TABLE ONLY pt_trigger
    ADD CONSTRAINT pt_trigger_triggere_id_fkey FOREIGN KEY (triggere_id) REFERENCES pt_triggere(id);
ALTER TABLE ONLY sf_avklart_dokument
    ADD CONSTRAINT sf_avklart_dokument_dokumenter_id_fkey FOREIGN KEY (dokumenter_id) REFERENCES sf_avklart_dokumenter(id);
ALTER TABLE ONLY ung_gr_soeknadsperiode
    ADD CONSTRAINT ung_gr_soeknadsperiode_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY ung_gr_soeknadsperiode
    ADD CONSTRAINT ung_gr_soeknadsperiode_oppgitt_soknadsperiode_id_fkey FOREIGN KEY (oppgitt_soknadsperiode_id) REFERENCES ung_soeknadsperioder(id);
ALTER TABLE ONLY ung_gr_soeknadsperiode
    ADD CONSTRAINT ung_gr_soeknadsperiode_relevant_soknadsperiode_id_fkey FOREIGN KEY (relevant_soknadsperiode_id) REFERENCES ung_soeknadsperioder(id);
ALTER TABLE ONLY ung_gr_ungdomsprogramperiode
    ADD CONSTRAINT ung_gr_ungdomsprogramperiode_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling(id);
ALTER TABLE ONLY ung_gr_ungdomsprogramperiode
    ADD CONSTRAINT ung_gr_ungdomsprogramperiode_ung_ungdomsprogramperioder_id_fkey FOREIGN KEY (ung_ungdomsprogramperioder_id) REFERENCES ung_ungdomsprogramperioder(id);
ALTER TABLE ONLY ung_soeknadsperiode
    ADD CONSTRAINT ung_soeknadsperiode_ung_soeknadsperioder_id_fkey FOREIGN KEY (ung_soeknadsperioder_id) REFERENCES ung_soeknadsperioder(id);
ALTER TABLE ONLY ung_ungdomsprogramperiode
    ADD CONSTRAINT ung_ungdomsprogramperiode_ung_ungdomsprogramperioder_id_fkey FOREIGN KEY (ung_ungdomsprogramperioder_id) REFERENCES ung_ungdomsprogramperioder(id);
ALTER TABLE ONLY vr_vilkar_periode
    ADD CONSTRAINT vilkar_periode_vilkar_id_fkey FOREIGN KEY (vilkar_id) REFERENCES vr_vilkar(id);
