package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.util.Tuple;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlag;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsPerioder;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.aksjonspunkt.AksjonspunktRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsresultat.BehandslingsresultatStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingstatus.BehandlingStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsårsak.BehandlingÅrsakRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.etterlysning.EtterlysningRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.fagsakstatus.FagsakStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.personopplysninger.AlderOgKjønnRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.sats.SatsStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.ungdomsprogram.DagerIProgrammetRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.uttalelse.UttalelseRecord;
import org.hibernate.query.NativeQuery;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.ung.sak.metrikker.bigquery.YtelseStatistikk.metrikk;

/**
 * Metrikker for ungdomsprogramytelsen. Publiseres til de opprinnelige tabellene i BigQuery.
 */
@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public class UngdomsprogramytelseStatistikk implements YtelseStatistikk {

    private static final FagsakYtelseType YTELSE = FagsakYtelseType.UNGDOMSYTELSE;

    private BigQueryStatistikkRepository statistikkRepository;
    private AntallDagerStatistikk antallDagerStatistikk;
    private EntityManager entityManager;

    UngdomsprogramytelseStatistikk() {
        // for CDI proxy
    }

    @Inject
    public UngdomsprogramytelseStatistikk(BigQueryStatistikkRepository statistikkRepository, AntallDagerStatistikk antallDagerStatistikk, EntityManager entityManager) {
        this.statistikkRepository = statistikkRepository;
        this.antallDagerStatistikk = antallDagerStatistikk;
        this.entityManager = entityManager;
    }

    @Override
    public List<Tuple<BigQueryTabell<?>, Collection<?>>> hentHyppigRapporterte(LocalDateTime sistKjørtTidspunkt) {
        return List.of(
            metrikk(FagsakStatusRecord.FAGSAK_STATUS_TABELLER.ungdomsytelse(), statistikkRepository.fagsakStatusStatistikk(YTELSE)),
            metrikk(BehandlingStatusRecord.BEHANDLING_STATUS_TABELLER.ungdomsytelse(), statistikkRepository.behandlingStatusStatistikk(YTELSE)),
            metrikk(AksjonspunktRecord.AKSJONSPUNKT_TABELLER.ungdomsytelse(), statistikkRepository.aksjonspunktStatistikk(YTELSE)),
            metrikk(SatsStatistikkRecord.SATS_STATISTIKK_TABELLER.ungdomsytelse(), satsStatistikk()),
            metrikk(BehandslingsresultatStatistikkRecord.BEHANDLINGSRESULTAT_STATISTIKK_TABELLER.ungdomsytelse(), statistikkRepository.behandlingResultatStatistikk(YTELSE)),
            metrikk(EtterlysningRecord.ETTERLYSNING_TABELLER.ungdomsytelse(), statistikkRepository.etterlysningData(YTELSE, sistKjørtTidspunkt)),
            metrikk(UttalelseRecord.UTTALELSE_TABELLER.ungdomsytelse(), statistikkRepository.uttalelseData(YTELSE, sistKjørtTidspunkt, null)),
            metrikk(BehandlingÅrsakRecord.BEHANDLING_ÅRSAK_TABELLER.ungdomsytelse(), statistikkRepository.behandlingÅrsakStatistikk(YTELSE))
        );
    }

    @Override
    public List<Tuple<BigQueryTabell<?>, Collection<?>>> hentDagligRapporterte() {
        return List.of(
            metrikk(AlderOgKjønnRecord.ALDER_OG_KJØNN_TABELLER.ungdomsytelse(), statistikkRepository.alderOgKjønnStatistikk(YTELSE)),
            metrikk(DagerIProgrammetRecord.DAGER_I_PROGRAMMET_LØPENDE_BIG_QUERY_TABELL, antallDagerStatistikk.dagerIProgrammet())
        );
    }

    /**
     * Henter antall gjeldende satser for ungdomsprogramytelsen, gruppert på antall barn og satstype.
     * <p>
     * Leser tabellene til {@link UngdomsytelseGrunnlag} (<code>ung_gr</code>), {@link UngdomsytelseSatsPerioder}
     * (<code>ung_sats_perioder</code>) og {@link UngdomsytelseSatsPeriode} (<code>ung_sats_periode</code>).
     * Kun det aktive grunnlaget på siste avsluttede behandling per fagsak telles. Fra grunnlaget telles
     * satsperioden som gjelder i dag, eller den første framtidige perioden dersom ingen periode gjelder i dag.
     * <p>
     * Spørringen kjøres mot Flyway-skjemaet i testene, slik at en migrering som endrer tabellene, feiler i bygget.
     */
    Collection<SatsStatistikkRecord> satsStatistikk() {
        // language=sql
        String sql = """
            select usp.antall_barn, usp.sats_type, count(*) as antall
            from ung_gr ur
                 inner join behandling b on b.id = ur.behandling_id
                 inner join fagsak f on f.id = b.fagsak_id
                 inner join ung_sats_perioder uspr on uspr.id = ur.ung_sats_perioder_id
                 inner join ung_sats_periode usp on usp.ung_sats_perioder_id = uspr.id

            where f.ytelse_type = :ytelseType
              and ur.aktiv is true
              and b.opprettet_tid = (SELECT max(b2.opprettet_tid)
                                     FROM Behandling b2
                                     WHERE b2.fagsak_id = f.id
                                       and b2.behandling_status = :behandlingStatusAvsluttetLKode) -- siste avsluttede behandling for fagsaken
              and (usp.periode @> current_date -- og perioden overlapper med dagens dato
                or (lower(usp.periode) > current_date -- eller fom dato på perioden er i fremtiden
                -- og er første satsperiode for denne behandlingen
                    and lower(usp.periode) =
                        (select min(lower(usp2.periode)) from ung_sats_periode usp2 where usp2.ung_sats_perioder_id = uspr.id)

                    -- og det finnes ingen andre perioder som overlapper med dagens dato
                    and not exists(select 1
                                   from ung_sats_periode usp2
                                   where usp2.ung_sats_perioder_id = uspr.id
                                     and usp2.periode @> current_date)
                       )
                )
            group by usp.antall_barn, usp.sats_type
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
}
