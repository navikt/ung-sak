package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.behandling.*;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.metrikker.MetrikkUtils;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.aksjonspunkt.AksjonspunktRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsresultat.BehandslingsresultatStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingstatus.BehandlingStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.fagsakstatus.FagsakStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.sats.SatsStatistikkRecord;
import org.hibernate.query.NativeQuery;
import org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.ung.sak.metrikker.MetrikkUtils.UDEFINERT;
import static no.nav.ung.sak.metrikker.MetrikkUtils.coalesce;


@Dependent
public class BigQueryStatistikkRepository {

    private static final Logger log = LoggerFactory.getLogger(BigQueryStatistikkRepository.class);

    private static final String OBSOLETE_KODE = FagsakYtelseType.OBSOLETE.getKode();

    private final EntityManager entityManager;
    private final Set<String> taskTyper;

    @Inject
    public BigQueryStatistikkRepository(
        EntityManager entityManager,
        @Any Instance<ProsessTaskHandler> handlers
    ) {
        this.entityManager = entityManager;
        this.taskTyper = handlers.stream()
            .map(this::extractClass)
            .map(it -> it.getAnnotation(ProsessTask.class).value())
            .collect(Collectors.toSet());
    }

    private Class<?> extractClass(ProsessTaskHandler bean) {
        if (!bean.getClass().isAnnotationPresent(ProsessTask.class) && bean instanceof TargetInstanceProxy<?> tip) {
            return tip.weld_getTargetInstance().getClass();
        } else {
            return bean.getClass();
        }
    }

    public List<Tuple<BigQueryTabell<?>, Collection<?>>> hentHyppigRapporterte() {
        List<Tuple<BigQueryTabell<?>, Collection<?>>> hyppigRapporterte = new ArrayList<>();

        Collection<FagsakStatusRecord> fagsakStatusStatistikk = fagsakStatusStatistikk();
        hyppigRapporterte.add(new Tuple<>(FagsakStatusRecord.FAGSAK_STATUS_TABELL_V2, fagsakStatusStatistikk));

        Collection<BehandlingStatusRecord> behandlingStatusStatistikk = behandlingStatusStatistikk();
        hyppigRapporterte.add(new Tuple<>(BehandlingStatusRecord.BEHANDLING_STATUS_TABELL, behandlingStatusStatistikk));

        Collection<AksjonspunktRecord> aksjonspunktStatistikk = aksjonspunktStatistikk();
        hyppigRapporterte.add(new Tuple<>(AksjonspunktRecord.AKSJONSPUNKT_TABELL, aksjonspunktStatistikk));

        Collection<SatsStatistikkRecord> satsStatistikk = satsSatistikk();
        hyppigRapporterte.add(new Tuple<>(SatsStatistikkRecord.SATS_STATISTIKK_TABELL, satsStatistikk));

        Collection<BehandslingsresultatStatistikkRecord> behandlingResultatStatistikk = behandlingResultatStatistikk();
        hyppigRapporterte.add(new Tuple<>(BehandslingsresultatStatistikkRecord.BEHANDLINGSRESULTAT_STATISTIKK_TABELL, behandlingResultatStatistikk));

        return hyppigRapporterte;
    }

    /**
     * Henter statistikk for fagsaker basert på fagsak-status.
     * <p>
     * Funksjonelt:
     * Denne metoden henter statistikk over fagsaker fra databasen, filtrerer ut utdatert ytelse,
     * og grupperer resultatene etter fagsak-status. Den returnerer en samling av FagsakStatusRecord-objekter
     * som inneholder antall fagsaker for hver status.
     * <p>
     * Teknisk:
     * - Spørringen bruker en enkel SELECT-setning for å hente fagsak-status og antall fagsaker.
     * - WHERE-betingelsen filtrerer ut fagsaker med en spesifikk ytelse-type som er utdatert.
     * - Resultatene grupperes etter fagsak-status, og antall fagsaker telles opp.
     *
     * @return En samling av FagsakStatusRecord-objekter som inneholder antall fagsaker,
     * fagsak-status og tidspunkt for innhentingen.
     */
    Collection<FagsakStatusRecord> fagsakStatusStatistikk() {
        String sql = """
            select f.fagsak_status, count(*) as antall
                     from fagsak f
                     where f.ytelse_type <> :obsoleteKode
                     group by 1
                     order by 1
            """;

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        Stream<jakarta.persistence.Tuple> stream = query
            .setParameter("obsoleteKode", OBSOLETE_KODE)
            .getResultStream();

        return stream.map(t -> {
            String fagsakStatus = t.get(0, String.class);
            Long totaltAntall = t.get(1, Long.class);

            return new FagsakStatusRecord(BigDecimal.valueOf(totaltAntall), FagsakStatus.fraKode(fagsakStatus), ZonedDateTime.now());
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Henter statistikk for behandlinger basert på ytelse-type, behandling-type og behandling-status.
     * <p>
     * Funksjonelt:
     * Denne metoden henter statistikk over behandlinger fra databasen, filtrerer ut utdatert ytelse,
     * og grupperer resultatene etter ytelse-type, behandling-type og behandling-status.
     * Den returnerer en samling av BehandlingStatusRecord-objekter som inneholder antall behandlinger
     * for hver kombinasjon av disse feltene.
     * <p>
     * Teknisk:
     * - Spørringen bruker INNER JOINs for å koble sammen tabellene <code>fagsak</code> og <code>behandling</code>.
     * - WHERE-betingelsene filtrerer ut behandlinger med en spesifikk ytelse-type som er utdatert.
     * - Resultatene grupperes etter ytelse-type, behandling-type og behandling-status,
     * og antall behandlinger telles opp.
     *
     * @return En samling av BehandlingStatusRecord-objekter som inneholder antall behandlinger,
     * ytelse-type, behandling-type, behandling-status og tidspunkt for innhentingen.
     */
    Collection<BehandlingStatusRecord> behandlingStatusStatistikk() {
        String sql = """
            select f.ytelse_type, b.behandling_type, b.behandling_status, count(*) as antall\
                  from fagsak f\
                  inner join behandling b on b.fagsak_id=f.id\
                  where f.ytelse_type <> :obsoleteKode \
                  group by 1, 2, 3\
                  order by 1, 2
            """;

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        Stream<jakarta.persistence.Tuple> stream = query
            .setParameter("obsoleteKode", OBSOLETE_KODE)
            .getResultStream();

        return stream.map(t -> {
            String ytelseType = t.get(0, String.class);
            String behandlingType = t.get(1, String.class);
            String behandlingStatus = t.get(2, String.class);
            Long totaltAntall = t.get(3, Long.class);
            return new BehandlingStatusRecord(
                BigDecimal.valueOf(totaltAntall),
                FagsakYtelseType.fraKode(ytelseType),
                BehandlingType.fraKode(behandlingType),
                BehandlingStatus.fraKode(behandlingStatus),
                ZonedDateTime.now()
            );
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Henter statistikk for aksjonspunkter basert på ytelse-type, aksjonspunkt-definisjon, status og venteårsak.
     * <p>
     * Funksjonelt:
     * Denne metoden henter statistikk over aksjonspunkter fra databasen, filtrerer ut utdatert ytelse,
     * og grupperer resultatene etter ytelse-type, aksjonspunkt-definisjon, status og venteårsak.
     * Den returnerer en samling av AksjonspunktRecord-objekter som inneholder antall aksjonspunkter
     * for hver kombinasjon av disse feltene.
     * <p>
     * Teknisk:
     * - Spørringen bruker INNER JOINs for å koble sammen tabellene <code>aksjonspunkt</code>, <code>behandling</code>
     * og <code>fagsak</code>.
     * - WHERE-betingelsene filtrerer ut aksjonspunkter med spesifikke statuser og ytelse-typer.
     * - Resultatene grupperes etter ytelse-type, aksjonspunkt-definisjon, status og venteårsak,
     * og antall aksjonspunkter telles opp.
     *
     * @return En samling av AksjonspunktRecord-objekter som inneholder antall aksjonspunkter,
     * ytelse-type, aksjonspunkt-definisjon, status, venteårsak og tidspunkt for innhentingen.
     */
    Collection<AksjonspunktRecord> aksjonspunktStatistikk() {
        String sql = """
            select f.ytelse_type, a.aksjonspunkt_def as aksjonspunkt, a.aksjonspunkt_status as status, a.vent_aarsak, count(*) as antall
             from aksjonspunkt a
             inner join behandling b on b.id = a.behandling_id
             inner join fagsak f on f.id = b.fagsak_id
             where a.aksjonspunkt_status IN (:statuser)
             and f.ytelse_type <> :obsoleteKode
             group by 1, 2, 3, 4
            """;

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        Stream<jakarta.persistence.Tuple> stream = query
            .setParameter("statuser", MetrikkUtils.AKSJONSPUNKT_STATUSER)
            .setParameter("obsoleteKode", OBSOLETE_KODE)
            .getResultStream();

        return stream.map(t -> {
            String ytelseType = t.get(0, String.class);
            String aksjonspunktKode = t.get(1, String.class);
            AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.kodeMap().getOrDefault(aksjonspunktKode, AksjonspunktDefinisjon.UNDEFINED);

            String aksjonspunktStatusKode = t.get(2, String.class);
            String venteÅrsakKode = coalesce(t.get(3, String.class), UDEFINERT);
            Long antall = t.get(4, Long.class);

            return new AksjonspunktRecord(
                FagsakYtelseType.fraKode(ytelseType),
                antall,
                aksjonspunktDefinisjon,
                AksjonspunktStatus.fraKode(aksjonspunktStatusKode),
                Venteårsak.fraKode(venteÅrsakKode, false),
                ZonedDateTime.now()
            );
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }


    /**
     * Henter statistikk for sats basert på antall barn og sats-type.
     * <p>
     * Funksjonelt:
     * Denne metoden henter statistikk fra flere relaterte tabeller der satsdata registreres.
     * Spørringen filtrerer ut fagsaker med en utdatert ytelse og tar kun med aktive poster fra
     * tabellen <code>ung_gr</code>. Den sikrer også at data for en fagsak kun vurderes dersom de hører til
     * den siste avsluttede behandlingen. Resultatene grupperes etter antall barn og sats-type for å
     * gi en oversikt over hvor mange poster som finnes per kategori.
     * <p>
     * Teknisk:
     * - Spørringen bruker flere INNER JOINs for å koble sammen tabellene <code>ung_gr</code>, <code>behandling</code>,
     * <code>fagsak</code>, <code>ung_sats_perioder</code> og <code>ung_sats_periode</code>.
     * - WHERE-betingelsene filtrerer ut rader basert på ytelse-type, hvor <code>f.ytelse_type</code> ikke
     * matcher den utdatert koden (<code>:obsoleteKode</code>) og hvor <code>ur.aktiv</code> er true.
     * - En subquery benyttes for å hente den siste avsluttede behandlingen (basert på maks <code>opprettet_tid</code>)
     * for hver fagsak, ved å sammenligne <code>behandling_status</code> med parameteren <code>:behandlingStatusAvsluttetLKode</code>.
     * - Perioden fra <code>ung_sats_periode</code> sjekkes for om den enten overlapper med dagens dato eller om
     * startdatoen er i fremtiden, samtidig som den sikrer at det ikke finnes andre perioder som overlapper.
     * - Resultatene grupperes etter <code>antall_barn</code> og <code>sats_type</code> med en aggregeringsfunksjon for
     * å telle antall forekomster per gruppe.
     *
     * @return En samling av SatsStatistikkRecord-objekter som inneholder antall poster, antall barn,
     * sats-type og tidspunkt for innhentingen.
     */
    Collection<SatsStatistikkRecord> satsSatistikk() {
        // language=sql
        String sql = """
            select usp.antall_barn, usp.sats_type, count(*) as antall
            from ung_gr ur
                 inner join behandling b on b.id = ur.behandling_id
                 inner join fagsak f on f.id = b.fagsak_id
                 inner join ung_sats_perioder uspr on uspr.id = ur.ung_sats_perioder_id
                 inner join ung_sats_periode usp on usp.ung_sats_perioder_id = uspr.id

            where f.ytelse_type <> :obsoleteKode
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
        Stream<jakarta.persistence.Tuple> stream = query
            .setParameter("obsoleteKode", OBSOLETE_KODE)
            .setParameter("behandlingStatusAvsluttetLKode", BehandlingStatus.AVSLUTTET.getKode())
            .getResultStream();

        return stream.map(t -> {
            Integer antallBarn = t.get(0, Integer.class);
            String satsTypeKode = t.get(1, String.class);
            Long antall = t.get(2, Long.class);

            return new SatsStatistikkRecord(
                antall,
                antallBarn,
                UngdomsytelseSatsType.fraKode(satsTypeKode),
                ZonedDateTime.now()
            );
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Henter statistikk for behandlingsresultater basert på behandling-type, behandling-resultat-type,
     * antall behandlinger, manuelle behandlinger og totrinnsbehandlinger.
     * <p>
     * Funksjonelt:
     * Denne metoden henter statistikk over behandlingsresultater fra databasen, filtrerer ut utdatert ytelse,
     * og grupperer resultatene etter behandling-type og behandling-resultat-type. Den returnerer en samling
     * av BehandslingsresultatStatistikkRecord-objekter som inneholder antall behandlinger, manuelle behandlinger
     * og totrinnsbehandlinger for hver kombinasjon av disse feltene.
     * <p>
     * Teknisk:
     * - Spørringen bruker INNER JOINs for å koble sammen tabellene <code>fagsak</code> og <code>behandling</code>.
     * - WHERE-betingelsene filtrerer ut behandlinger med spesifikke statuser (IVERKSETTER_VEDTAK og AVSLUTTET),
     * og deretter filtrerer ut behandlinger med en spesifikk ytelse-type som er utdatert.
     * - Resultatene grupperes etter behandling-type og behandling-resultat-type,
     * og antall behandlinger telles opp.
     * - I tillegg telles antall manuelle behandlinger (hvor ansvarlig saksbehandler ikke er null)
     * og antall totrinnsbehandlinger (hvor totrinnsbehandling er true).
     *
     * @return En samling av BehandslingsresultatStatistikkRecord-objekter som inneholder antall behandlinger,
     * behandling-type, behandling-resultat-type, manuelle behandlinger,
     * totrinnsbehandlinger og tidspunkt for innhentingen.
     */
    public Collection<BehandslingsresultatStatistikkRecord> behandlingResultatStatistikk() {
        //language=sql
        String sql = """
            select b.behandling_type,
                    b.behandling_resultat_type,
                    count(*) antall,
                    count(ansvarlig_saksbehandler) filter (where ansvarlig_saksbehandler is not null) manuell_antall,
                    count(totrinnsbehandling) filter (where totrinnsbehandling=true) as totrinn_antall
            from fagsak f
                inner join behandling b on b.fagsak_id=f.id
            where b.behandling_status IN (:statuser)
              and behandling_resultat_type is not null
              and f.ytelse_type <> :obsoleteKode
            group by 1, 2
            order by 1, 2
            """;

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        Stream<jakarta.persistence.Tuple> stream = query
            .setParameter("statuser", Set.of(BehandlingStatus.IVERKSETTER_VEDTAK.getKode(), BehandlingStatus.AVSLUTTET.getKode()))
            .setParameter("obsoleteKode", OBSOLETE_KODE)
            .getResultStream();

        return stream.map(t -> {
            String behandlingTypeKode = t.get(0, String.class);
            String behandlingResultatTypeKode = t.get(1, String.class);
            Long totalAntall = t.get(2, Long.class);
            Long manuellAntall = t.get(3, Long.class);
            Long totrinnsbehandlingAntall = t.get(4, Long.class);

            return new BehandslingsresultatStatistikkRecord(
                BehandlingType.fraKode(behandlingTypeKode),
                BehandlingResultatType.fraKode(behandlingResultatTypeKode),
                totalAntall,
                manuellAntall,
                totrinnsbehandlingAntall,
                ZonedDateTime.now()
            );
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

}
