package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.util.Tuple;
import no.nav.ung.kodeverk.behandling.*;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.person.NavBrukerKjønn;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.metrikker.MetrikkUtils;
import no.nav.ung.sak.metrikker.bigquery.tabeller.YtelsesTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.aksjonspunkt.AksjonspunktRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsresultat.BehandslingsresultatStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingstatus.BehandlingStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsårsak.BehandlingÅrsakRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.etterlysning.EtterlysningRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.fagsakstatus.FagsakStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.personopplysninger.AlderOgKjønnRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.sats.SatsStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.uttalelse.UttalelseRecord;
import no.nav.ung.sak.typer.Saksnummer;
import org.hibernate.query.NativeQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.ung.sak.metrikker.MetrikkUtils.UDEFINERT;
import static no.nav.ung.sak.metrikker.MetrikkUtils.coalesce;


/**
 * Felles spørringer for statistikk som publiseres til BigQuery.
 * <p>
 * Alle spørringer er parametrisert på ytelse og filtrerer eksplisitt på én ytelse. Hvilke metrikker som publiseres
 * for hver ytelse, og til hvilke tabeller, bestemmes av implementasjonene av {@link YtelseStatistikk}.
 * Kun {@link YtelsesTabell#STØTTEDE_YTELSER} er tillatt, slik at utgåtte fagsaker ({@link FagsakYtelseType#OBSOLETE})
 * aldri kommer med i statistikken.
 */
@Dependent
public class BigQueryStatistikkRepository {

    private final EntityManager entityManager;

    @Inject
    public BigQueryStatistikkRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private static String ytelseKode(FagsakYtelseType ytelse) {
        return YtelsesTabell.validerStøttet(ytelse).getKode();
    }

    /**
     * Tabeller for satsgrunnlag per ytelse. Verdiene er kodekonstanter og settes inn i SQL med {@link String#formatted}.
     */
    private enum SatsKilde {
        UNGDOMSYTELSE("ung_gr", "ung_sats_perioder", "ung_sats_periode", "ung_sats_perioder_id"),
        AKTIVITETSPENGER("gr_avp", "avp_sats_perioder", "avp_sats_periode", "avp_sats_perioder_id");

        private final String grunnlag;
        private final String perioder;
        private final String periode;
        private final String perioderFk;

        SatsKilde(String grunnlag, String perioder, String periode, String perioderFk) {
            this.grunnlag = grunnlag;
            this.perioder = perioder;
            this.periode = periode;
            this.perioderFk = perioderFk;
        }

        static SatsKilde forYtelse(FagsakYtelseType ytelse) {
            return switch (ytelse) {
                case UNGDOMSYTELSE -> UNGDOMSYTELSE;
                case AKTIVITETSPENGER -> AKTIVITETSPENGER;
                default -> throw new IllegalArgumentException("Satsstatistikk er ikke støttet for ytelse " + ytelse);
            };
        }
    }

    /**
     * Henter statistikk for fagsaker basert på fagsak-status.
     * <p>
     * Funksjonelt:
     * Denne metoden henter statistikk over fagsaker for én ytelse fra databasen,
     * og grupperer resultatene etter fagsak-status. Den returnerer en samling av FagsakStatusRecord-objekter
     * som inneholder antall fagsaker for hver status.
     * <p>
     * Teknisk:
     * - Spørringen bruker en enkel SELECT-setning for å hente fagsak-status og antall fagsaker.
     * - WHERE-betingelsen filtrerer på oppgitt ytelse-type.
     * - Resultatene grupperes etter fagsak-status, og antall fagsaker telles opp.
     *
     * @param ytelse ytelsen det skal hentes statistikk for
     * @return En samling av FagsakStatusRecord-objekter som inneholder antall fagsaker,
     * fagsak-status og tidspunkt for innhentingen.
     */
    Collection<FagsakStatusRecord> fagsakStatusStatistikk(FagsakYtelseType ytelse) {
        String sql = """
            select f.fagsak_status, count(*) as antall
                     from fagsak f
                     where f.ytelse_type = :ytelseType
                     group by 1
                     order by 1
            """;

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        Stream<jakarta.persistence.Tuple> stream = query
            .setParameter("ytelseType", ytelseKode(ytelse))
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
     * Denne metoden henter statistikk over behandlinger for én ytelse fra databasen,
     * og grupperer resultatene etter ytelse-type, behandling-type og behandling-status.
     * Den returnerer en samling av BehandlingStatusRecord-objekter som inneholder antall behandlinger
     * for hver kombinasjon av disse feltene.
     * <p>
     * Teknisk:
     * - Spørringen bruker INNER JOINs for å koble sammen tabellene <code>fagsak</code> og <code>behandling</code>.
     * - WHERE-betingelsen filtrerer på oppgitt ytelse-type.
     * - Resultatene grupperes etter ytelse-type, behandling-type og behandling-status,
     * og antall behandlinger telles opp.
     *
     * @param ytelse ytelsen det skal hentes statistikk for
     * @return En samling av BehandlingStatusRecord-objekter som inneholder antall behandlinger,
     * ytelse-type, behandling-type, behandling-status og tidspunkt for innhentingen.
     */
    Collection<BehandlingStatusRecord> behandlingStatusStatistikk(FagsakYtelseType ytelse) {
        String sql = """
            select f.ytelse_type, b.behandling_type, b.behandling_status, count(*) as antall\
                  from fagsak f\
                  inner join behandling b on b.fagsak_id=f.id\
                  where f.ytelse_type = :ytelseType \
                  group by 1, 2, 3\
                  order by 1, 2
            """;

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        Stream<jakarta.persistence.Tuple> stream = query
            .setParameter("ytelseType", ytelseKode(ytelse))
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
     * Denne metoden henter statistikk over aksjonspunkter for én ytelse fra databasen,
     * og grupperer resultatene etter ytelse-type, aksjonspunkt-definisjon, status og venteårsak.
     * Den returnerer en samling av AksjonspunktRecord-objekter som inneholder antall aksjonspunkter
     * for hver kombinasjon av disse feltene.
     * <p>
     * Teknisk:
     * - Spørringen bruker INNER JOINs for å koble sammen tabellene <code>aksjonspunkt</code>, <code>behandling</code>
     * og <code>fagsak</code>.
     * - WHERE-betingelsene filtrerer på spesifikke aksjonspunktstatuser og oppgitt ytelse-type.
     * - Resultatene grupperes etter ytelse-type, aksjonspunkt-definisjon, status og venteårsak,
     * og antall aksjonspunkter telles opp.
     *
     * @param ytelse ytelsen det skal hentes statistikk for
     * @return En samling av AksjonspunktRecord-objekter som inneholder antall aksjonspunkter,
     * ytelse-type, aksjonspunkt-definisjon, status, venteårsak og tidspunkt for innhentingen.
     */
    Collection<AksjonspunktRecord> aksjonspunktStatistikk(FagsakYtelseType ytelse) {
        String sql = """
            select f.ytelse_type, a.aksjonspunkt_def as aksjonspunkt, a.aksjonspunkt_status as status, a.vent_aarsak, count(*) as antall
             from aksjonspunkt a
             inner join behandling b on b.id = a.behandling_id
             inner join fagsak f on f.id = b.fagsak_id
             where a.aksjonspunkt_status IN (:statuser)
             and f.ytelse_type = :ytelseType
             group by 1, 2, 3, 4
            """;

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        Stream<jakarta.persistence.Tuple> stream = query
            .setParameter("statuser", MetrikkUtils.AKSJONSPUNKT_STATUSER)
            .setParameter("ytelseType", ytelseKode(ytelse))
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
     * Spørringen filtrerer på oppgitt ytelse og tar kun med aktive poster fra satsgrunnlaget
     * (<code>ung_gr</code> for ungdomsytelsen, <code>gr_avp</code> for aktivitetspenger). Den sikrer også at data
     * for en fagsak kun vurderes dersom de hører til den siste avsluttede behandlingen. Resultatene grupperes
     * etter antall barn og sats-type for å gi en oversikt over hvor mange poster som finnes per kategori.
     * <p>
     * Teknisk:
     * - Tabellnavnene for satsgrunnlaget velges per ytelse via {@link SatsKilde}. Strukturen er lik for begge ytelsene.
     * - Spørringen bruker flere INNER JOINs for å koble sammen satsgrunnlaget, <code>behandling</code>,
     * <code>fagsak</code>, satsperiodene og hver enkelt satsperiode.
     * - WHERE-betingelsene filtrerer på <code>f.ytelse_type = :ytelseType</code> og at grunnlaget er aktivt.
     * - En subquery benyttes for å hente den siste avsluttede behandlingen (basert på maks <code>opprettet_tid</code>)
     * for hver fagsak, ved å sammenligne <code>behandling_status</code> med parameteren <code>:behandlingStatusAvsluttetLKode</code>.
     * - Satsperioden sjekkes for om den enten overlapper med dagens dato eller om
     * startdatoen er i fremtiden, samtidig som den sikrer at det ikke finnes andre perioder som overlapper.
     * - Resultatene grupperes etter <code>antall_barn</code> og <code>sats_type</code> med en aggregeringsfunksjon for
     * å telle antall forekomster per gruppe.
     *
     * @param ytelse ytelsen det skal hentes statistikk for
     * @return En samling av SatsStatistikkRecord-objekter som inneholder antall poster, antall barn,
     * sats-type og tidspunkt for innhentingen.
     */
    Collection<SatsStatistikkRecord> satsSatistikk(FagsakYtelseType ytelse) {
        SatsKilde kilde = SatsKilde.forYtelse(ytelse);
        // language=sql
        String sql = """
            select usp.antall_barn, usp.sats_type, count(*) as antall
            from %1$s ur
                 inner join behandling b on b.id = ur.behandling_id
                 inner join fagsak f on f.id = b.fagsak_id
                 inner join %2$s uspr on uspr.id = ur.%4$s
                 inner join %3$s usp on usp.%4$s = uspr.id

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
                        (select min(lower(usp2.periode)) from %3$s usp2 where usp2.%4$s = uspr.id)

                    -- og det finnes ingen andre perioder som overlapper med dagens dato
                    and not exists(select 1
                                   from %3$s usp2
                                   where usp2.%4$s = uspr.id
                                     and usp2.periode @> current_date)
                       )
                )
            group by usp.antall_barn, usp.sats_type
            """.formatted(kilde.grunnlag, kilde.perioder, kilde.periode, kilde.perioderFk);

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        Stream<jakarta.persistence.Tuple> stream = query
            .setParameter("ytelseType", ytelseKode(ytelse))
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
     * Denne metoden henter statistikk over behandlingsresultater for én ytelse fra databasen,
     * og grupperer resultatene etter behandling-type og behandling-resultat-type. Den returnerer en samling
     * av BehandslingsresultatStatistikkRecord-objekter som inneholder antall behandlinger, manuelle behandlinger
     * og totrinnsbehandlinger for hver kombinasjon av disse feltene.
     * <p>
     * Teknisk:
     * - Spørringen bruker INNER JOINs for å koble sammen tabellene <code>fagsak</code> og <code>behandling</code>.
     * - WHERE-betingelsene filtrerer ut behandlinger med spesifikke statuser (IVERKSETTER_VEDTAK og AVSLUTTET),
     * og filtrerer på oppgitt ytelse-type.
     * - Resultatene grupperes etter behandling-type og behandling-resultat-type,
     * og antall behandlinger telles opp.
     * - I tillegg telles antall manuelle behandlinger (hvor ansvarlig saksbehandler ikke er null)
     * og antall totrinnsbehandlinger (hvor totrinnsbehandling er true).
     *
     * @param ytelse ytelsen det skal hentes statistikk for
     * @return En samling av BehandslingsresultatStatistikkRecord-objekter som inneholder antall behandlinger,
     * behandling-type, behandling-resultat-type, manuelle behandlinger,
     * totrinnsbehandlinger og tidspunkt for innhentingen.
     */
    public Collection<BehandslingsresultatStatistikkRecord> behandlingResultatStatistikk(FagsakYtelseType ytelse) {
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
              and f.ytelse_type = :ytelseType
            group by 1, 2
            order by 1, 2
            """;

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        Stream<jakarta.persistence.Tuple> stream = query
            .setParameter("statuser", Set.of(BehandlingStatus.IVERKSETTER_VEDTAK.getKode(), BehandlingStatus.AVSLUTTET.getKode()))
            .setParameter("ytelseType", ytelseKode(ytelse))
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


    /* Henter etterlysning-data for fagsaker på oppgitt ytelse.
     */
    Collection<EtterlysningRecord> etterlysningData(FagsakYtelseType ytelse, LocalDateTime sistKjørtTidspunkt) {
        String sql = """
            select f.saksnummer, e.type, e.status, e.fom, e.tom, frist, coalesce(e.endret_tid, e.opprettet_tid) opprettet_tid
             from etterlysning e
             inner join behandling b on b.id = e.behandling_id
             inner join fagsak f on f.id = b.fagsak_id
             where f.ytelse_type = :ytelseType and coalesce(e.endret_tid, e.opprettet_tid) > :sistKjørtTidspunkt
            """;

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        Stream<jakarta.persistence.Tuple> stream = query
            .setParameter("ytelseType", ytelseKode(ytelse))
            .setParameter("sistKjørtTidspunkt", sistKjørtTidspunkt)
            .getResultStream();

        return stream.map(t -> {
            String saksnummer = t.get(0, String.class);
            String type = t.get(1, String.class);
            String status = t.get(2, String.class);
            LocalDate fom = t.get(3, LocalDate.class);
            LocalDate tom = t.get(4, LocalDate.class);
            LocalDateTime frist = t.get(5, LocalDateTime.class);
            LocalDateTime tidsstempel = t.get(6, LocalDateTime.class);

            return new EtterlysningRecord(
                new Saksnummer(saksnummer),
                EtterlysningType.fraKode(type),
                EtterlysningStatus.fraKode(status),
                DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
                frist == null ? null : frist.atZone(ZoneId.systemDefault()),
                tidsstempel.atZone(ZoneId.systemDefault())
            );
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }


    /* Henter uttalelse-data for fagsaker på oppgitt ytelse.
     */
    Collection<UttalelseRecord> uttalelseData(FagsakYtelseType ytelse, LocalDateTime sistKjørtTidspunkt, LocalDateTime førsteKjørtTidspunkt) {
        String sql = """
            select f.saksnummer, u.har_uttalelse, u.endring_type, u.fom, u.tom, m.mottatt_tidspunkt
             from gr_uttalelse gr
             inner join uttalelse_v2 u on u.uttalelser_id = gr.uttalelser_id
             inner join behandling b on b.id = gr.behandling_id
             inner join fagsak f on f.id = b.fagsak_id
             inner join mottatt_dokument m on m.journalpost_id = u.svar_journalpost_id and m.behandling_id = b.id
             where f.ytelse_type = :ytelseType and
            """;
        sql += førsteKjørtTidspunkt == null
            ? "(m.mottatt_tidspunkt > :sistKjørtTidspunkt)"
            : "(m.mottatt_tidspunkt > :sistKjørtTidspunkt or m.mottatt_tidspunkt < :førsteKjørtTidspunkt)";

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        query.setParameter("ytelseType", ytelseKode(ytelse));
        query.setParameter("sistKjørtTidspunkt", sistKjørtTidspunkt);
        if (førsteKjørtTidspunkt != null) {
            query.setParameter("førsteKjørtTidspunkt", førsteKjørtTidspunkt);
        }

        Stream<jakarta.persistence.Tuple> stream = query.getResultStream();

        return stream.map(t -> {
            String saksnummer = t.get(0, String.class);
            boolean har_uttalelse = t.get(1, Boolean.class);
            String endringType = t.get(2, String.class);
            LocalDate fom = t.get(3, LocalDate.class);
            LocalDate tom = t.get(4, LocalDate.class);
            LocalDateTime mottattTidspunkt = t.get(5, LocalDateTime.class);

            return new UttalelseRecord(
                new Saksnummer(saksnummer),
                har_uttalelse,
                EndringType.fraKode(endringType),
                DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
                mottattTidspunkt.atZone(ZoneId.systemDefault())
            );
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }


    /**
     * Henter statistikk for behandlinger gruppert på årsakstype og ferdigbehandlet-status.
     * <p>
     * Denne metoden henter antall behandlinger for hver kombinasjon av relevante årsakstyper og om behandlingen er ferdigbehandlet eller ikke.
     * Resultatet inkluderer alle relevante årsaker, også de med 0 forekomster, for både ferdigbehandlet og ikke-ferdigbehandlet status.
     *
     * @param ytelse ytelsen det skal hentes statistikk for
     * @return En samling av BehandlingÅrsakRecord for alle relevante årsaker og ferdigbehandlet-status.
     */
    Collection<BehandlingÅrsakRecord> behandlingÅrsakStatistikk(FagsakYtelseType ytelse) {
        String sql = """
            select ba.behandling_arsak_type, b.behandling_status, count(distinct b.id) as antall\
                  from fagsak f\
                  inner join behandling b on b.fagsak_id=f.id\
                  inner join behandling_arsak ba on ba.behandling_id = b.id\
                  where f.ytelse_type = :ytelseType \
                  group by 1, 2 \
                  order by 1, 2
            """;

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        Stream<jakarta.persistence.Tuple> stream = query
            .setParameter("ytelseType", ytelseKode(ytelse))
            .getResultStream();

        // Gruppér på behandling_årsak_type og erFerdigbehandletStatus
        Map<Tuple<String, Boolean>, Long> grouped = stream.collect(Collectors.groupingBy(
            t -> new Tuple<>(
                t.get(0, String.class),
                BehandlingStatus.fraKode(t.get(1, String.class)).erFerdigbehandletStatus()
            ),
            Collectors.summingLong(t -> t.get(2, Long.class))
        ));

        // Finn alle mulige kombinasjoner av relevanteÅrsaker og erFerdigbehandletStatus (true/false)
        List<BehandlingÅrsakRecord> result = new ArrayList<>();
        for (BehandlingÅrsakType årsak : BehandlingÅrsakType.values()) {
            for (boolean ferdig : List.of(true, false)) {
                Tuple<String, Boolean> key = new Tuple<>(årsak.getKode(), ferdig);
                Long totaltAntall = grouped.getOrDefault(key, 0L);
                result.add(new BehandlingÅrsakRecord(
                    BigDecimal.valueOf(totaltAntall),
                    årsak,
                    ferdig,
                    ZonedDateTime.now()
                ));
            }
        }
        return result;
    }


    Collection<AlderOgKjønnRecord> alderOgKjønnStatistikk(FagsakYtelseType ytelse) {
        String sql = """
            select po.bruker_kjoenn,  extract(YEAR from AGE(current_date, po.foedselsdato)),  count(*) as antall
                     from fagsak f
                     inner join behandling b on b.fagsak_id = f.id
                     inner join gr_personopplysning gr on gr.behandling_id = b.id
                    inner join po_informasjon pi on pi.id = gr.registrert_informasjon_id
                     inner join PO_PERSONOPPLYSNING po on po.po_informasjon_id = pi.id and po.aktoer_id = f.bruker_aktoer_id
                     where f.ytelse_type = :ytelseType and gr.aktiv is true and b.behandling_type = :førstegangsbehandling
                     group by 1,2
                     order by 1,2
            """;

        NativeQuery<jakarta.persistence.Tuple> query = (NativeQuery<jakarta.persistence.Tuple>) entityManager.createNativeQuery(sql, jakarta.persistence.Tuple.class);
        Stream<jakarta.persistence.Tuple> stream = query
            .setParameter("ytelseType", ytelseKode(ytelse))
            .setParameter("førstegangsbehandling", BehandlingType.FØRSTEGANGSSØKNAD.getKode())
            .getResultStream();

        return stream.map(t -> {
            String kjønnKode = t.get(0, String.class);
            var kjønn = NavBrukerKjønn.fraKode(kjønnKode);
            BigDecimal alder = t.get(1, BigDecimal.class);
            Long antall = t.get(2, Long.class);

            return new AlderOgKjønnRecord(BigDecimal.valueOf(antall), alder.intValue(), kjønn, ZonedDateTime.now());
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
