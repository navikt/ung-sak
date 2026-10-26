package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.util.Tuple;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.aksjonspunkt.AksjonspunktRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsresultat.BehandslingsresultatStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingstatus.BehandlingStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsårsak.BehandlingÅrsakRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.etterlysning.EtterlysningRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.fagsakstatus.FagsakStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.inntektskontroll.KontrollertePerioderRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.personopplysninger.AlderOgKjønnRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.sats.SatsStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.soknad.SøknadStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.uttalelse.UttalelseRecord;
import org.hibernate.query.NativeQuery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.ung.sak.metrikker.bigquery.YtelseStatistikk.metrikk;

/**
 * Metrikker for aktivitetspenger. Publiseres til egne tabeller med prefiks <code>aktivitetspenger_</code> i BigQuery,
 * slik at tilgang kan styres per ytelse på Datamarkedsplassen.
 * <p>
 * Tabellene har samme skjema som tabellene for ungdomsprogramytelsen, men eget navn. Navnene oppgis eksplisitt
 * fordi flere av tabellene for ungdomsprogramytelsen har versjonssuffiks (<code>_v2</code>) som ikke skal arves.
 */
@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class AktivitetspengerStatistikk implements YtelseStatistikk {

    private static final FagsakYtelseType YTELSE = FagsakYtelseType.AKTIVITETSPENGER;
    private static final String AKTIVITETSPENGER_PREFIX = "aktivitetspenger_";


    static final BigQueryTabell<FagsakStatusRecord> FAGSAK_STATUS_TABELL =
        FagsakStatusRecord.FAGSAK_STATUS_TABELL_V2.medTabellnavn(AKTIVITETSPENGER_PREFIX + "fagsak_status");
    static final BigQueryTabell<BehandlingStatusRecord> BEHANDLING_STATUS_TABELL =
        BehandlingStatusRecord.BEHANDLING_STATUS_TABELL.medTabellnavn(AKTIVITETSPENGER_PREFIX + "behandling_status");
    static final BigQueryTabell<AksjonspunktRecord> AKSJONSPUNKT_TABELL =
        AksjonspunktRecord.AKSJONSPUNKT_TABELL.medTabellnavn(AKTIVITETSPENGER_PREFIX + "aksjonspunkter_status");
    static final BigQueryTabell<SatsStatistikkRecord> SATS_STATISTIKK_TABELL =
        SatsStatistikkRecord.SATS_STATISTIKK_TABELL.medTabellnavn(AKTIVITETSPENGER_PREFIX + "sats_statistikk");
    static final BigQueryTabell<BehandslingsresultatStatistikkRecord> BEHANDLINGSRESULTAT_STATISTIKK_TABELL =
        BehandslingsresultatStatistikkRecord.BEHANDLINGSRESULTAT_STATISTIKK_TABELL.medTabellnavn(AKTIVITETSPENGER_PREFIX + "behandlingsresultat_statistikk");
    static final BigQueryTabell<EtterlysningRecord> ETTERLYSNING_TABELL =
        EtterlysningRecord.ETTERLYSNING_TABELL.medTabellnavn(AKTIVITETSPENGER_PREFIX + "etterlysning");
    static final BigQueryTabell<UttalelseRecord> UTTALELSE_TABELL =
        UttalelseRecord.UTTALELSE_TABELL.medTabellnavn(AKTIVITETSPENGER_PREFIX + "uttalelse");
    static final BigQueryTabell<BehandlingÅrsakRecord> BEHANDLING_ÅRSAK_TABELL =
        BehandlingÅrsakRecord.BEHANDLING_ÅRSAK_TABELL.medTabellnavn(AKTIVITETSPENGER_PREFIX + "behandling_årsak");
    static final BigQueryTabell<AlderOgKjønnRecord> ALDER_OG_KJØNN_TABELL =
        AlderOgKjønnRecord.ALDER_OG_KJØNN_BIG_QUERY_TABELL.medTabellnavn(AKTIVITETSPENGER_PREFIX + "alder_og_kjønn");
    static final BigQueryTabell<KontrollertePerioderRecord> KONTROLLERTE_PERIODER_TABELL =
        KontrollertePerioderRecord.KONTROLLERTE_PERIODER_TABELL.medTabellnavn(AKTIVITETSPENGER_PREFIX + "kontrollerte_inntekt_perioder");

    private BigQueryStatistikkRepository statistikkRepository;
    private EntityManager entityManager;

    AktivitetspengerStatistikk() {
        // for CDI proxy
    }

    @Inject
    public AktivitetspengerStatistikk(BigQueryStatistikkRepository statistikkRepository, EntityManager entityManager) {
        this.statistikkRepository = statistikkRepository;
        this.entityManager = entityManager;
    }

    @Override
    public List<Tuple<BigQueryTabell<?>, Collection<?>>> hentHyppigRapporterte(LocalDateTime sistKjørtTidspunkt) {
        return List.of(
            metrikk(FAGSAK_STATUS_TABELL, statistikkRepository.fagsakStatusStatistikk(YTELSE)),
            metrikk(BEHANDLING_STATUS_TABELL, statistikkRepository.behandlingStatusStatistikk(YTELSE)),
            metrikk(AKSJONSPUNKT_TABELL, statistikkRepository.aksjonspunktStatistikk(YTELSE)),
            metrikk(SATS_STATISTIKK_TABELL, satsStatistikk()),
            metrikk(BEHANDLINGSRESULTAT_STATISTIKK_TABELL, statistikkRepository.behandlingResultatStatistikk(YTELSE)),
            metrikk(ETTERLYSNING_TABELL, statistikkRepository.etterlysningData(YTELSE, sistKjørtTidspunkt)),
            metrikk(UTTALELSE_TABELL, statistikkRepository.uttalelseData(YTELSE, sistKjørtTidspunkt, null)),
            metrikk(BEHANDLING_ÅRSAK_TABELL, statistikkRepository.behandlingÅrsakStatistikk(YTELSE)),
            metrikk(SøknadStatistikkRecord.AKTIVITETSPENGER_SØKNAD_TABELL, søknadStatistikk())
        );
    }

    @Override
    public List<Tuple<BigQueryTabell<?>, Collection<?>>> hentDagligRapporterte() {
        return List.of(
            metrikk(ALDER_OG_KJØNN_TABELL, statistikkRepository.alderOgKjønnStatistikk(YTELSE))
        );
    }

    @Override
    public BigQueryTabell<UttalelseRecord> uttalelseTabell() {
        return UTTALELSE_TABELL;
    }

    @Override
    public BigQueryTabell<KontrollertePerioderRecord> kontrollertePerioderTabell() {
        return KONTROLLERTE_PERIODER_TABELL;
    }

    /**
     * Henter antall gjeldende satser for aktivitetspenger, gruppert på antall barn og satstype.
     * <p>
     * Leser tabellene til <code>AktivitetspengerGrunnlag</code> (<code>gr_avp</code>), <code>AktivitetspengerSatsPerioder</code>
     * (<code>avp_sats_perioder</code>) og <code>AktivitetspengerSatsPeriode</code> (<code>avp_sats_periode</code>) i modulen
     * <code>ytelse-aktivitetspenger</code>. Datavarehus er ikke avhengig av den modulen og kan derfor ikke lenke til entitetene.
     * Kun det aktive grunnlaget på siste avsluttede behandling per fagsak telles. Fra grunnlaget telles
     * satsperioden som gjelder i dag, eller den første framtidige perioden dersom ingen periode gjelder i dag.
     * <p>
     * Spørringen kjøres mot Flyway-skjemaet i testene, slik at en migrering som endrer tabellene, feiler i bygget.
     */
    Collection<SatsStatistikkRecord> satsStatistikk() {
        // language=sql
        String sql = """
            select asp.antall_barn, asp.sats_type, count(*) as antall
            from gr_avp gr
                 inner join behandling b on b.id = gr.behandling_id
                 inner join fagsak f on f.id = b.fagsak_id
                 inner join avp_sats_perioder aspr on aspr.id = gr.avp_sats_perioder_id
                 inner join avp_sats_periode asp on asp.avp_sats_perioder_id = aspr.id

            where f.ytelse_type = :ytelseType
              and gr.aktiv is true
              and b.opprettet_tid = (SELECT max(b2.opprettet_tid)
                                     FROM Behandling b2
                                     WHERE b2.fagsak_id = f.id
                                       and b2.behandling_status = :behandlingStatusAvsluttetLKode) -- siste avsluttede behandling for fagsaken
              and (asp.periode @> current_date -- og perioden overlapper med dagens dato
                or (lower(asp.periode) > current_date -- eller fom dato på perioden er i fremtiden
                -- og er første satsperiode for denne behandlingen
                    and lower(asp.periode) =
                        (select min(lower(asp2.periode)) from avp_sats_periode asp2 where asp2.avp_sats_perioder_id = aspr.id)

                    -- og det finnes ingen andre perioder som overlapper med dagens dato
                    and not exists(select 1
                                   from avp_sats_periode asp2
                                   where asp2.avp_sats_perioder_id = aspr.id
                                     and asp2.periode @> current_date)
                       )
                )
            group by asp.antall_barn, asp.sats_type
            """;

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        return query
            .setParameter("ytelseType", YTELSE.getKode())
            .setParameter("behandlingStatusAvsluttetLKode", BehandlingStatus.AVSLUTTET.getKode())
            .getResultStream()
            .map(t -> new SatsStatistikkRecord(
                t.get(2, Long.class),
                t.get(0, Integer.class),
                UngdomsytelseSatsType.fraKode(t.get(1, String.class)),
                ZonedDateTime.now()
            ))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Henter statistikk over mottatte søknadsdokumenter for aktivitetspenger, gruppert på dokumentstatus.
     * <p>
     * Alle mottatte søknadsdokumenter telles (også henlagte og ugyldige), slik at statusfordelingen viser
     * hvor mange som er ferdig behandlet i mottaket. I tillegg telles antall søknader mottatt i dag.
     *
     * @return En samling av SøknadStatistikkRecord per dokumentstatus.
     */
    Collection<SøknadStatistikkRecord> søknadStatistikk() {
        String sql = """
            select md.status, count(*) as antall_totalt, count(*) filter (where md.mottatt_dato = :dagensDato) as antall_i_dag
             from mottatt_dokument md
             inner join fagsak f on f.id = md.fagsak_id
             where f.ytelse_type = :ytelseType
               and md.type in (:soknadTyper)
             group by 1
             order by 1
            """;

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        return query
            .setParameter("ytelseType", YTELSE.getKode())
            .setParameter("soknadTyper", Brevkode.SØKNAD_TYPER.stream().map(Brevkode::getKode).collect(Collectors.toSet()))
            .setParameter("dagensDato", LocalDate.now())
            .getResultStream()
            .map(t -> new SøknadStatistikkRecord(
                DokumentStatus.fraKode(t.get(0, String.class)),
                t.get(1, Long.class),
                t.get(2, Long.class),
                ZonedDateTime.now()
            ))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
