package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.util.Tuple;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.aksjonspunkt.AksjonspunktRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsresultat.BehandslingsresultatStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingstatus.BehandlingStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsårsak.BehandlingÅrsakRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.etterlysning.EtterlysningRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.fagsakstatus.FagsakStatusRecord;
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
 * Metrikker for aktivitetspenger. Publiseres til egne tabeller med prefiks <code>aktivitetspenger_</code> i BigQuery.
 */
@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class AktivitetspengerStatistikk implements YtelseStatistikk {

    private static final FagsakYtelseType YTELSE = FagsakYtelseType.AKTIVITETSPENGER;

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
            metrikk(FagsakStatusRecord.FAGSAK_STATUS_TABELLER.aktivitetspenger(), statistikkRepository.fagsakStatusStatistikk(YTELSE)),
            metrikk(BehandlingStatusRecord.BEHANDLING_STATUS_TABELLER.aktivitetspenger(), statistikkRepository.behandlingStatusStatistikk(YTELSE)),
            metrikk(AksjonspunktRecord.AKSJONSPUNKT_TABELLER.aktivitetspenger(), statistikkRepository.aksjonspunktStatistikk(YTELSE)),
            metrikk(SatsStatistikkRecord.SATS_STATISTIKK_TABELLER.aktivitetspenger(), statistikkRepository.satsSatistikk(YTELSE)),
            metrikk(BehandslingsresultatStatistikkRecord.BEHANDLINGSRESULTAT_STATISTIKK_TABELLER.aktivitetspenger(), statistikkRepository.behandlingResultatStatistikk(YTELSE)),
            metrikk(EtterlysningRecord.ETTERLYSNING_TABELLER.aktivitetspenger(), statistikkRepository.etterlysningData(YTELSE, sistKjørtTidspunkt)),
            metrikk(UttalelseRecord.UTTALELSE_TABELLER.aktivitetspenger(), statistikkRepository.uttalelseData(YTELSE, sistKjørtTidspunkt, null)),
            metrikk(BehandlingÅrsakRecord.BEHANDLING_ÅRSAK_TABELLER.aktivitetspenger(), statistikkRepository.behandlingÅrsakStatistikk(YTELSE)),
            metrikk(SøknadStatistikkRecord.AKTIVITETSPENGER_SØKNAD_TABELL, søknadStatistikk())
        );
    }

    @Override
    public List<Tuple<BigQueryTabell<?>, Collection<?>>> hentDagligRapporterte() {
        return List.of(
            metrikk(AlderOgKjønnRecord.ALDER_OG_KJØNN_TABELLER.aktivitetspenger(), statistikkRepository.alderOgKjønnStatistikk(YTELSE))
        );
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
