package no.nav.k9.sak.metrikker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hibernate.QueryTimeoutException;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import no.nav.k9.felles.integrasjon.sensu.SensuEvent;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.krav.StatusForPerioderPåBehandling;
import no.nav.k9.sak.kontrakt.krav.ÅrsakTilVurdering;
import no.nav.k9.sak.perioder.SøknadsfristTjenesteProvider;
import no.nav.k9.sak.perioder.UtledPerioderMedRegisterendring;
import no.nav.k9.sak.perioder.UtledStatusPåPerioderTjeneste;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

/**
 * For innhenting av metrikker relatert til kvartalsmål for OKR
 */
@Dependent
public class RevurderingMetrikkRepository {


    public static final Set<String> PLEIEPENGE_YTELSER = Set.of(FagsakYtelseType.PSB.getKode(), FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE.getKode(), FagsakYtelseType.OPPLÆRINGSPENGER.getKode());
    static final List<String> YTELSER = Stream.of(
            FagsakYtelseType.FRISINN,
            FagsakYtelseType.OMSORGSPENGER,
            FagsakYtelseType.OMSORGSPENGER_KS,
            FagsakYtelseType.OMSORGSPENGER_MA,
            FagsakYtelseType.OMSORGSPENGER_AO,
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE)
        .map(FagsakYtelseType::getKode).collect(Collectors.toList());

    private static final Logger log = LoggerFactory.getLogger(RevurderingMetrikkRepository.class);

    private final EntityManager entityManager;
    private final BehandlingRepository behandlingRepository;
    private final SøknadsfristTjenesteProvider søknadsfristTjenesteProvider;
    private final UtledStatusPåPerioderTjeneste statusPåPerioderTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    @Inject
    public RevurderingMetrikkRepository(EntityManager entityManager, BehandlingRepository behandlingRepository,
                                        SøknadsfristTjenesteProvider søknadsfristTjenesteProvider,
                                        UtledPerioderMedRegisterendring utledPerioderMedRegisterendring,
                                        @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjenesteProvider = søknadsfristTjenesteProvider;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
        this.statusPåPerioderTjeneste = new UtledStatusPåPerioderTjeneste(false, utledPerioderMedRegisterendring);
    }

    public List<SensuEvent> hentAlle(LocalDate revurderingUtenSøknadTomDato) {
        LocalDate dag = LocalDate.now();

        List<SensuEvent> metrikker = new ArrayList<>();

        if (!revurderingUtenSøknadTomDato.equals(dag)) {
            // Kjører kun innhenting av saksnummer dersom tomdato er ulik dagens dato, dette betyr at vi er utenfor schedulert kjøring
            try {
                metrikker.addAll(timeCall(() -> revurderingerUtenNySøknadMedAksjonspunkt(revurderingUtenSøknadTomDato), "revurderingerUtenNySøknadMedAksjonspunkt"));
            } catch (QueryTimeoutException e) {
                log.warn("Uthenting av revurderingerUtenNySøknadMedAksjonspunkt feiler", e);
            }
        } else {

            try {
                metrikker.addAll(timeCall(() -> antallAksjonspunktFordelingForRevurderingSisteSyvDager(dag), "antallAksjonspunktFordelingForRevurderingSisteSyvDager"));
            } catch (QueryTimeoutException e) {
                log.warn("Uthenting av antallAksjonspunktFordelingForRevurderingSisteSyvDager feiler", e);
            }
            try {
                metrikker.addAll(timeCall(() -> antallAksjonspunktFordelingForRevurderingUtenNyttStpSisteSyvDager(dag), "antallAksjonspunktFordelingForRevurderingUtenNyttStpSisteSyvDager"));
            } catch (QueryTimeoutException e) {
                log.warn("Uthenting av antallAksjonspunktFordelingForRevurderingUtenNyttStpSisteSyvDager feiler", e);
            }
            try {
                metrikker.addAll(timeCall(() -> antallRevurderingMedAksjonspunktPrKodeSisteSyvDager(dag), "antallRevurderingMedAksjonspunktPrKodeSisteSyvDager"));
            } catch (QueryTimeoutException e) {
                log.warn("Uthenting av antallRevurderingMedAksjonspunktPrKodeSisteSyvDager feiler", e);
            }
            try {
                metrikker.addAll(timeCall(() -> antallRevurderingUtenNyttStpMedAksjonspunktPrKodeSisteSyvDager(dag), "antallRevurderingUtenNyttStpMedAksjonspunktPrKodeSisteSyvDager"));
            } catch (QueryTimeoutException e) {
                log.warn("Uthenting av antallRevurderingUtenNyttStpMedAksjonspunktPrKodeSisteSyvDager feiler", e);
            }
            try {
                metrikker.addAll(timeCall(() -> antallRevurderingUtenNyttStpÅrsakStatistikk(dag), "antallRevurderingUtenNyttStpÅrsakStatistikk"));
            } catch (QueryTimeoutException e) {
                log.warn("Uthenting av antallRevurderingUtenNyttStpÅrsakStatistikk feiler", e);
            }
            try {
                metrikker.addAll(timeCall(() -> antallAksjonspunktFordelingForRevurderingUtenNySøknadSisteSyvDagerPSB(dag), "antallAksjonspunktFordelingForRevurderingUtenNySøknadSisteSyvDagerPSB"));
            } catch (QueryTimeoutException e) {
                log.warn("Uthenting av antallAksjonspunktFordelingForRevurderingUtenNySøknadSisteSyvDagerPSB feiler", e);
            }
            try {
                metrikker.addAll(timeCall(() -> antallRevurderingUtenNySøknadMedAksjonspunktPrKodeSisteSyvDagerPSB(dag), "antallRevurderingUtenNySøknadMedAksjonspunktPrKodeSisteSyvDagerPSB"));
            } catch (QueryTimeoutException e) {
                log.warn("Uthenting av antallRevurderingUtenNySøknadMedAksjonspunktPrKodeSisteSyvDagerPSB feiler", e);
            }
            try {
                metrikker.addAll(timeCall(() -> revurderingerUtenNySøknadMedAksjonspunkt(revurderingUtenSøknadTomDato), "revurderingerUtenNySøknadMedAksjonspunkt"));
            } catch (QueryTimeoutException e) {
                log.warn("Uthenting av revurderingerUtenNySøknadMedAksjonspunkt feiler", e);
            }
        }
        return metrikker;
    }

    private Collection<SensuEvent> timeCall(Supplier<Collection<SensuEvent>> collectionSupplier, String function) {
        var start = System.currentTimeMillis();
        var sensuEvents = collectionSupplier.get();
        var slutt = System.currentTimeMillis();

        log.info("{} benyttet {} ms", function, (slutt - start));

        return sensuEvents;
    }

    @SuppressWarnings("unchecked")
    Collection<SensuEvent> antallAksjonspunktFordelingForRevurderingSisteSyvDager(LocalDate dato) {
        String sql = "select " +
            "ytelse_type, " +
            "antall_aksjonspunkter, " +
            "antall_behandlinger," +
            "antall_behandlinger/sum(antall_behandlinger) over (partition by ytelse_type)*100 as behandlinger_prosentandel " +
            "from (select ytelse_type, " +
            "antall_aksjonspunkter, " +
            "count(*) as antall_behandlinger from (" +
            "   select f.ytelse_type, b.id, " +
            "   count(a.aksjonspunkt_def) as antall_aksjonspunkter " +
            "   from behandling b" +
            "            inner join fagsak f on f.id=b.fagsak_id" +
            "            full outer join aksjonspunkt a on b.id = a.behandling_id " +
            "   where (a.aksjonspunkt_status is null or a.aksjonspunkt_status != 'AVBR') " +
            "   and (vent_aarsak is null or vent_aarsak = '-') " +
            "   and b.avsluttet_dato is not null " +
            "   and b.avsluttet_dato>=:startTid and b.avsluttet_dato < :sluttTid " +
            "   and b.behandling_type=:revurdering " +
            "   group by 1, 2) as statistikk_pr_behandling " +
            "group by 1, 2) as statistikk_pr_behandling_og_total order by antall_aksjonspunkter;";

        String metricName = "revurdering_antall_aksjonspunkt_fordeling_v2";
        String metricField = "antall_behandlinger";
        var metricField2 = "behandlinger_prosentandel";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("revurdering", BehandlingType.REVURDERING.getKode())
            .setParameter("startTid", dato.minusDays(7).atStartOfDay())
            .setParameter("sluttTid", dato.atStartOfDay());

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
                toMap(
                    "ytelse_type", t.get(0, String.class),
                    "antall_aksjonspunkter", t.get(1, Long.class).toString()),
                Map.of(metricField, t.get(2, Number.class),
                    metricField2, t.get(3, Number.class))))
            .collect(Collectors.toCollection(LinkedHashSet::new));


        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", YTELSER,
                "antall_aksjonspunkter", IntStream.range(0, 11).boxed().map(Object::toString).toList()), // Lager antall fra 0 til 10
            Map.of(
                metricField, 0L,
                metricField2, BigDecimal.ZERO));

        values.addAll(zeroValues); // NB: utnytter at Set#addAll ikke legger til verdier som ikke finnes fra før

        return values;

    }

    @SuppressWarnings("unchecked")
    Collection<SensuEvent> antallAksjonspunktFordelingForRevurderingUtenNyttStpSisteSyvDager(LocalDate dato) {
        String sql = "select " +
            "ytelse_type, " +
            "antall_aksjonspunkter, " +
            "antall_behandlinger," +
            "antall_behandlinger/sum(antall_behandlinger) over (partition by ytelse_type)*100 as behandlinger_prosentandel " +
            "from (select ytelse_type, " +
            "antall_aksjonspunkter, " +
            "count(*) as antall_behandlinger from (" +
            "   select f.ytelse_type, b.id, " +
            "   count(a.aksjonspunkt_def) as antall_aksjonspunkter " +
            "   from behandling b" +
            "            inner join fagsak f on f.id=b.fagsak_id" +
            "            full outer join aksjonspunkt a on b.id = a.behandling_id " +
            "   where (a.aksjonspunkt_status is null or a.aksjonspunkt_status != :avbrutt) " +
            "   and (vent_aarsak is null or vent_aarsak = :udefinert) " +
            "   and b.avsluttet_dato is not null " +
            "   and b.avsluttet_dato>=:startTid and b.avsluttet_dato < :sluttTid " +
            "   and b.behandling_type=:revurdering " +
            " and not exists ( " +
            " select 1 from rs_vilkars_resultat rv" +
            " inner join vr_vilkar vv on vv.vilkar_resultat_id=rv.vilkarene_id" +
            " inner join vr_vilkar_periode vp on vp.vilkar_id=vv.id" +
            " inner join rs_vilkars_resultat rv_original on rv_original.behandling_id = b.original_behandling_id" +
            " inner join vr_vilkar vv_original on vv_original.vilkar_resultat_id=rv_original.vilkarene_id" +
            " inner join vr_vilkar_periode vp_original on vp_original.vilkar_id=vv_original.id" +
            " where rv.aktiv=true and rv.behandling_id = b.id and vv.vilkar_type = :bg_vilkaret " +
            " and rv_original.aktiv=true and vp_original.fom != vp.fom and vv_original.vilkar_type = :bg_vilkaret)" +
            "   group by 1, 2) as statistikk_pr_behandling " +
            " group by 1, 2) as statistikk_pr_behandling_og_total order by antall_aksjonspunkter;";

        String metricName = "revurdering_uten_nye_stp_antall_aksjonspunkt_fordeling";
        String metricField = "antall_behandlinger";
        var metricField2 = "behandlinger_prosentandel";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("revurdering", BehandlingType.REVURDERING.getKode())
            .setParameter("bg_vilkaret", VilkårType.BEREGNINGSGRUNNLAGVILKÅR.getKode())
            .setParameter("avbrutt", AksjonspunktStatus.AVBRUTT.getKode())
            .setParameter("udefinert", Venteårsak.UDEFINERT.getKode())
            .setParameter("startTid", dato.minusDays(7).atStartOfDay())
            .setParameter("sluttTid", dato.atStartOfDay());

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
                toMap(
                    "ytelse_type", t.get(0, String.class),
                    "antall_aksjonspunkter", t.get(1, Long.class).toString()),
                Map.of(metricField, t.get(2, Number.class),
                    metricField2, t.get(3, Number.class))
            ))
            .collect(Collectors.toCollection(LinkedHashSet::new));


        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", YTELSER,
                "antall_aksjonspunkter", IntStream.range(0, 11).boxed().map(Object::toString).toList()), // Lager antall fra 0 til 10
            Map.of(
                metricField, 0L,
                metricField2, BigDecimal.ZERO));

        values.addAll(zeroValues); // NB: utnytter at Set#addAll ikke legger til verdier som ikke finnes fra før

        return values;

    }


    @SuppressWarnings("unchecked")
    Collection<SensuEvent> antallAksjonspunktFordelingForRevurderingUtenNySøknadSisteSyvDagerPSB(LocalDate dato) {
        String sql = "select " +
            "ytelse_type, " +
            "antall_aksjonspunkter, " +
            "antall_behandlinger," +
            "antall_behandlinger/sum(antall_behandlinger) over (partition by ytelse_type)*100 as behandlinger_prosentandel " +
            "from (select ytelse_type, " +
            "antall_aksjonspunkter, " +
            "count(*) as antall_behandlinger from (" +
            "   select f.ytelse_type, b.id, " +
            "   count(a.aksjonspunkt_def) as antall_aksjonspunkter " +
            "   from behandling b" +
            "            inner join fagsak f on f.id=b.fagsak_id" +
            "            left join aksjonspunkt a on b.id = a.behandling_id " +
            "   where (a.aksjonspunkt_status is null or a.aksjonspunkt_status != :avbrutt) " +
            "   and (vent_aarsak is null or vent_aarsak = :udefinert) " +
            "   and b.avsluttet_dato is not null " +
            "   and b.avsluttet_dato>=:startTid and b.avsluttet_dato < :sluttTid " +
            "   and b.behandling_type=:revurdering " +
            "   and f.ytelse_type in (:pleiepengeYtelser) " +
            " and not exists ( " +
            " select 1 from GR_SOEKNADSPERIODE gr " +
            "inner join sp_soeknadsperioder_holder sp_holder on gr.oppgitt_soknadsperiode_id = sp_holder.id " +
            "inner join SP_SOEKNADSPERIODER sp_perioder on sp_holder.id = sp_perioder.holder_id " +
            "inner join GR_SOEKNADSPERIODE gr_original on gr_original.behandling_id = b.original_behandling_id " +
            "inner join sp_soeknadsperioder_holder sp_holder_original on sp_holder_original.id = gr_original.oppgitt_soknadsperiode_id " +
            "inner join sp_soeknadsperioder sp_perioder_original on sp_holder_original.id = sp_perioder_original.holder_id " +
            "where gr.behandling_id = b.id and gr.aktiv = true and gr_original.aktiv = true and sp_perioder.journalpost_id != sp_perioder_original.journalpost_id)" +
            "   group by 1, 2) as statistikk_pr_behandling " +
            " group by 1, 2) as statistikk_pr_behandling_og_total order by antall_aksjonspunkter;";

        String metricName = "revurdering_uten_ny_soknad_antall_aksjonspunkt_fordeling_v2";
        String metricField = "antall_behandlinger";
        var metricField2 = "behandlinger_prosentandel";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("revurdering", BehandlingType.REVURDERING.getKode())
            .setParameter("avbrutt", AksjonspunktStatus.AVBRUTT.getKode())
            .setParameter("udefinert", Venteårsak.UDEFINERT.getKode())
            .setParameter("startTid", dato.minusDays(7).atStartOfDay())
            .setParameter("sluttTid", dato.atStartOfDay())
            .setParameter("pleiepengeYtelser", PLEIEPENGE_YTELSER);

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
                toMap(
                    "ytelse_type", t.get(0, String.class),
                    "antall_aksjonspunkter", t.get(1, Long.class).toString()),
                Map.of(metricField, t.get(2, Number.class),
                    metricField2, t.get(3, Number.class))
            ))
            .collect(Collectors.toCollection(LinkedHashSet::new));


        var zeroValues = emptyEvents(metricName,
            Map.of(
                "ytelse_type", YTELSER,
                "antall_aksjonspunkter", IntStream.range(0, 11).boxed().map(Object::toString).toList()), // Lager antall fra 0 til 10
            Map.of(
                metricField, 0L,
                metricField2, BigDecimal.ZERO));

        values.addAll(zeroValues); // NB: utnytter at Set#addAll ikke legger til verdier som ikke finnes fra før

        return values;

    }


    @SuppressWarnings("unchecked")
    Collection<SensuEvent> antallRevurderingMedAksjonspunktPrKodeSisteSyvDager(LocalDate dato) {
        String sql = "select f.ytelse_type, a.aksjonspunkt_def, count(*) as antall_behandlinger " +
            "from behandling b" +
            "         inner join fagsak f on f.id=b.fagsak_id" +
            "         inner join aksjonspunkt a on b.id = a.behandling_id " +
            "where a.aksjonspunkt_status != 'AVBR' " +
            "and (vent_aarsak is null or vent_aarsak = '-') " +
            "and b.avsluttet_dato is not null " +
            "and b.avsluttet_dato>=:startTid and b.avsluttet_dato < :sluttTid " +
            "and b.behandling_type=:revurdering " +
            "group by 1, 2";

        String metricName = "revurdering_antall_behandlinger_pr_aksjonspunkt_v2";
        String metricField = "antall_behandlinger";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("revurdering", BehandlingType.REVURDERING.getKode())
            .setParameter("startTid", dato.minusDays(7).atStartOfDay())
            .setParameter("sluttTid", dato.atStartOfDay());

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
                toMap(
                    "ytelse_type", t.get(0, String.class),
                    "aksjonspunkt", t.get(1, String.class),
                    "aksjonspunkt_navn", coalesce(AksjonspunktDefinisjon.kodeMap().getOrDefault(t.get(1, String.class), AksjonspunktDefinisjon.UNDEFINED).getNavn(), "-")),
                Map.of(metricField, t.get(2, Number.class))))
            .collect(Collectors.toList());

        return values;

    }


    @SuppressWarnings("unchecked")
    Collection<SensuEvent> antallRevurderingUtenNyttStpMedAksjonspunktPrKodeSisteSyvDager(LocalDate dato) {
        String sql = "select f.ytelse_type, a.aksjonspunkt_def, count(*) as antall_behandlinger " +
            "from behandling b" +
            "         inner join fagsak f on f.id=b.fagsak_id" +
            "         inner join aksjonspunkt a on b.id = a.behandling_id " +
            "where a.aksjonspunkt_status != 'AVBR' " +
            "and (vent_aarsak is null or vent_aarsak = '-') " +
            "and b.avsluttet_dato is not null " +
            "and b.avsluttet_dato>=:startTid and b.avsluttet_dato < :sluttTid " +
            "and b.behandling_type=:revurdering " +
            " and not exists ( " +
            " select 1 from rs_vilkars_resultat rv" +
            " inner join vr_vilkar vv on vv.vilkar_resultat_id=rv.vilkarene_id" +
            " inner join vr_vilkar_periode vp on vp.vilkar_id=vv.id" +
            " inner join rs_vilkars_resultat rv_original on rv_original.behandling_id = b.original_behandling_id" +
            " inner join vr_vilkar vv_original on vv_original.vilkar_resultat_id=rv_original.vilkarene_id" +
            " inner join vr_vilkar_periode vp_original on vp_original.vilkar_id=vv_original.id" +
            " where rv.aktiv=true and rv.behandling_id = b.id and vv.vilkar_type = :bg_vilkaret " +
            " and rv_original.aktiv=true and vp_original.fom != vp.fom and vv_original.vilkar_type = :bg_vilkaret)" +
            "group by 1, 2";

        String metricName = "revurdering_uten_nytt_stp_antall_behandlinger_pr_aksjonspunkt";
        String metricField = "antall_behandlinger";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("revurdering", BehandlingType.REVURDERING.getKode())
            .setParameter("bg_vilkaret", VilkårType.BEREGNINGSGRUNNLAGVILKÅR.getKode())
            .setParameter("startTid", dato.minusDays(7).atStartOfDay())
            .setParameter("sluttTid", dato.atStartOfDay());

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
                toMap(
                    "ytelse_type", t.get(0, String.class),
                    "aksjonspunkt", t.get(1, String.class),
                    "aksjonspunkt_navn", coalesce(AksjonspunktDefinisjon.kodeMap().getOrDefault(t.get(1, String.class), AksjonspunktDefinisjon.UNDEFINED).getNavn(), "-")),
                Map.of(metricField, t.get(2, Number.class))))
            .collect(Collectors.toList());

        return values;

    }

    @SuppressWarnings("unchecked")
    Collection<SensuEvent> antallRevurderingUtenNySøknadMedAksjonspunktPrKodeSisteSyvDagerPSB(LocalDate dato) {
        String sql = "select f.ytelse_type, a.aksjonspunkt_def, count(*) as antall_behandlinger " +
            "from behandling b" +
            "         inner join fagsak f on f.id=b.fagsak_id" +
            "         inner join aksjonspunkt a on b.id = a.behandling_id " +
            "where a.aksjonspunkt_status != 'AVBR' " +
            "and (vent_aarsak is null or vent_aarsak = '-') " +
            "and b.avsluttet_dato is not null " +
            "and b.avsluttet_dato>=:startTid and b.avsluttet_dato < :sluttTid " +
            "and b.behandling_type=:revurdering " +
            "and f.ytelse_type in (:pleiepengeYtelser) " +
            " and not exists ( " +
            " select 1 from GR_SOEKNADSPERIODE gr " +
            "inner join sp_soeknadsperioder_holder sp_holder on gr.oppgitt_soknadsperiode_id = sp_holder.id " +
            "inner join SP_SOEKNADSPERIODER sp_perioder on sp_holder.id = sp_perioder.holder_id " +
            "inner join GR_SOEKNADSPERIODE gr_original on gr_original.behandling_id = b.original_behandling_id " +
            "inner join sp_soeknadsperioder_holder sp_holder_original on sp_holder_original.id = gr_original.oppgitt_soknadsperiode_id " +
            "inner join sp_soeknadsperioder sp_perioder_original on sp_holder_original.id = sp_perioder_original.holder_id " +
            "where gr.behandling_id = b.id and gr.aktiv = true and gr_original.aktiv = true and sp_perioder.journalpost_id != sp_perioder_original.journalpost_id)" +
            "group by 1, 2";

        String metricName = "revurdering_uten_ny_soknad_antall_behandlinger_pr_aksjonspunkt";
        String metricField = "antall_behandlinger";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("revurdering", BehandlingType.REVURDERING.getKode())
            .setParameter("startTid", dato.minusDays(7).atStartOfDay())
            .setParameter("sluttTid", dato.atStartOfDay())
            .setParameter("pleiepengeYtelser", PLEIEPENGE_YTELSER);

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));

        var values = stream.map(t -> SensuEvent.createSensuEvent(metricName,
                toMap(
                    "ytelse_type", t.get(0, String.class),
                    "aksjonspunkt", t.get(1, String.class),
                    "aksjonspunkt_navn", coalesce(AksjonspunktDefinisjon.kodeMap().getOrDefault(t.get(1, String.class), AksjonspunktDefinisjon.UNDEFINED).getNavn(), "-")),
                Map.of(metricField, t.get(2, Number.class))))
            .collect(Collectors.toList());

        return values;

    }


    @SuppressWarnings("unchecked")
    Collection<SensuEvent> antallRevurderingUtenNyttStpÅrsakStatistikk(LocalDate dato) {
        String sql = "select f.ytelse_type, b.id " +
            "from behandling b inner join fagsak f on f.id=b.fagsak_id " +
            "where f.ytelse_type = 'PSB' " +
            "and b.avsluttet_dato is not null " +
            "and b.avsluttet_dato>=:startTid and b.avsluttet_dato < :sluttTid " +
            "and b.behandling_type=:revurdering " +
            " and not exists ( " +
            " select 1 from rs_vilkars_resultat rv" +
            " inner join vr_vilkar vv on vv.vilkar_resultat_id=rv.vilkarene_id" +
            " inner join vr_vilkar_periode vp on vp.vilkar_id=vv.id" +
            " inner join rs_vilkars_resultat rv_original on rv_original.behandling_id = b.original_behandling_id" +
            " inner join vr_vilkar vv_original on vv_original.vilkar_resultat_id=rv_original.vilkarene_id" +
            " inner join vr_vilkar_periode vp_original on vp_original.vilkar_id=vv_original.id" +
            " where rv.aktiv=true and rv.behandling_id = b.id and vv.vilkar_type = :bg_vilkaret " +
            " and rv_original.aktiv=true and vp_original.fom != vp.fom and vv_original.vilkar_type = :bg_vilkaret)";


        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("revurdering", BehandlingType.REVURDERING.getKode())
            .setParameter("bg_vilkaret", VilkårType.BEREGNINGSGRUNNLAGVILKÅR.getKode())
            .setParameter("startTid", dato.minusDays(7).atStartOfDay())
            .setParameter("sluttTid", dato.atStartOfDay());

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));
        var perioderTilVurderingPrBehandling = stream.collect(Collectors.toMap(
            Function.identity(), t -> {
                var behandling = behandlingRepository.hentBehandling(t.get(1, Long.class));
                var ref = BehandlingReferanse.fra(behandling);
                return getStatusForPerioderPåBehandling(ref, behandling,
                    VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, ref.getFagsakYtelseType(), ref.getBehandlingType()));
            }));

        var gruppertPrAntallPerioder = perioderTilVurderingPrBehandling.entrySet().stream().collect(Collectors.groupingBy(it -> it.getValue().getPerioderTilVurdering().size()));
        var totalAntallBehandlinger = perioderTilVurderingPrBehandling.size();

        var values = gruppertPrAntallPerioder.entrySet().stream().map(behandlingerPrAntallPeriode -> SensuEvent.createSensuEvent(
                "antall_revurderinger_uten_nytt_stp_pr_antall_perioder",
                toMap(
                    "ytelse_type", FagsakYtelseType.PSB.getKode(),
                    "antall_perioder", behandlingerPrAntallPeriode.getKey().toString()),
                Map.of("antall_behandlinger", behandlingerPrAntallPeriode.getValue().size(),
                    "behandlinger_prosentandel", BigDecimal.valueOf(behandlingerPrAntallPeriode.getValue().size()).divide(BigDecimal.valueOf(totalAntallBehandlinger), RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)))))
            .collect(Collectors.toCollection(HashSet::new));


        var antallBehandlingerPrÅrsak = Arrays.stream(ÅrsakTilVurdering.values()).collect(Collectors.toMap(
            Function.identity(),
            årsak -> perioderTilVurderingPrBehandling.entrySet().stream().filter(it -> it.getValue().getÅrsakMedPerioder().stream().anyMatch(a -> a.getÅrsak().equals(årsak) && !a.getPerioder().isEmpty())).count()
        ));
        antallBehandlingerPrÅrsak.entrySet().stream().map(antallPrÅrsak -> SensuEvent.createSensuEvent(
                "antall_revurderinger_uten_nytt_stp_pr_aarsak",
                toMap(
                    "ytelse_type", FagsakYtelseType.PSB.getKode(),
                    "aarsak", antallPrÅrsak.getKey().toString()),
                Map.of("antall_behandlinger", antallPrÅrsak.getValue(),
                    "behandlinger_prosentandel", BigDecimal.valueOf(antallPrÅrsak.getValue()).divide(BigDecimal.valueOf(totalAntallBehandlinger), RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)))))
            .forEach(values::add);

        return values;

    }

    @SuppressWarnings("unchecked")
    Collection<SensuEvent> revurderingerUtenNySøknadMedAksjonspunkt(LocalDate dato) {
        String sql = "select f.ytelse_type, f.saksnummer, b.id, a.aksjonspunkt_def, b.avsluttet_dato " +
            "from behandling b" +
            "         inner join fagsak f on f.id=b.fagsak_id" +
            "         inner join aksjonspunkt a on b.id = a.behandling_id " +
            "where a.aksjonspunkt_status != 'AVBR' " +
            "and (vent_aarsak is null or vent_aarsak = '-') " +
            "and b.avsluttet_dato is not null " +
            "and b.avsluttet_dato>=:startTid and b.avsluttet_dato < :sluttTid " +
            "and b.behandling_type=:revurdering " +
            "and f.ytelse_type in (:pleiepengeYtelser) " +
            " and not exists ( " +
            " select 1 from GR_SOEKNADSPERIODE gr " +
            "inner join sp_soeknadsperioder_holder sp_holder on gr.oppgitt_soknadsperiode_id = sp_holder.id " +
            "inner join SP_SOEKNADSPERIODER sp_perioder on sp_holder.id = sp_perioder.holder_id " +
            "inner join GR_SOEKNADSPERIODE gr_original on gr_original.behandling_id = b.original_behandling_id " +
            "inner join sp_soeknadsperioder_holder sp_holder_original on sp_holder_original.id = gr_original.oppgitt_soknadsperiode_id " +
            "inner join sp_soeknadsperioder sp_perioder_original on sp_holder_original.id = sp_perioder_original.holder_id " +
            "where gr.behandling_id = b.id and gr.aktiv = true and gr_original.aktiv = true and sp_perioder.journalpost_id != sp_perioder_original.journalpost_id)";

        String metricName = "revurdering_uten_ny_soknad";

        NativeQuery<Tuple> query = (NativeQuery<Tuple>) entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("revurdering", BehandlingType.REVURDERING.getKode())
            .setParameter("startTid", dato.minusDays(1).atStartOfDay())
            .setParameter("sluttTid", dato.atStartOfDay())
            .setParameter("pleiepengeYtelser", PLEIEPENGE_YTELSER);

        Stream<Tuple> stream = query.getResultStream()
            .filter(t -> !Objects.equals(FagsakYtelseType.OBSOLETE.getKode(), t.get(0, String.class)));


        var values = stream.map(t -> {
                String ytelseType = t.get(0, String.class);
                String saksnummer = t.get(1, String.class);
                String behandlingId = t.get(2, Long.class).toString();
                String aksjonspunktKode = t.get(3, String.class);
                String aksjonspunktNavn = coalesce(AksjonspunktDefinisjon.kodeMap().getOrDefault(aksjonspunktKode, AksjonspunktDefinisjon.UNDEFINED).getNavn(), "-");
                long tidsstempel = t.get(4, Timestamp.class).getTime();
                return SensuEvent.createSensuEvent(metricName,
                    toMap(
                        "ytelse_type", ytelseType,
                        "aksjonspunkt", aksjonspunktKode),
                    Map.of(
                        "aksjonspunkt_navn", aksjonspunktNavn,
                        "saksnummer", saksnummer,
                        "behandlingId", behandlingId),
                    tidsstempel);
            })
            .collect(Collectors.toList());


        return values;

    }


    private static String coalesce(String str, String defValue) {
        return str != null ? str : defValue;
    }

    /**
     * Map.of() takler ikke null verdier, så vi lager vår egen variant.
     */
    private static Map<String, String> toMap(String... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Må ha partall antall argumenter, fikk: " + Arrays.asList(args));
        }
        var map = new HashMap<String, String>();
        for (int i = 0; i < args.length; i += 2) {
            // influxdb Point takler ikke null key eller value. skipper null verdier
            String v = args[i + 1];
            if (v != null) {
                map.put(args[i], v);
            }
        }
        return map;
    }

    /**
     * Lager events med 0 målinger for alle kombinasjoner av oppgitte vektorer.
     */
    private Collection<SensuEvent> emptyEvents(String metricName, Map<String, Collection<String>> vectors, Map<String, Object> defaultVals) {
        List<Map<String, String>> matrix = new CombineLists<String>(vectors).toMap();
        return matrix.stream()
            .map(v -> SensuEvent.createSensuEvent(metricName, v, defaultVals))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private StatusForPerioderPåBehandling getStatusForPerioderPåBehandling(BehandlingReferanse ref, Behandling behandling, VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        var søknadsfristTjeneste = søknadsfristTjenesteProvider.finnVurderSøknadsfristTjeneste(ref);

        var kravdokumenter = søknadsfristTjeneste.relevanteKravdokumentForBehandling(ref);
        var perioderSomSkalTilbakestilles = perioderTilVurderingTjeneste.perioderSomSkalTilbakestilles(ref.getBehandlingId());

        var kravdokumenterMedPeriode = søknadsfristTjeneste.hentPerioderTilVurdering(ref);
        var definerendeVilkår = perioderTilVurderingTjeneste.definerendeVilkår();

        var perioderTilVurdering = definerendeVilkår.stream()
            .map(it -> perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), it))
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(TreeSet::new));

        perioderTilVurdering.addAll(perioderTilVurderingTjeneste.utledUtvidetRevurderingPerioder(ref));

        var revurderingPerioderFraAndreParter = perioderTilVurderingTjeneste.utledRevurderingPerioder(ref);
        var kantIKantVurderer = perioderTilVurderingTjeneste.getKantIKantVurderer();

        var statusForPerioderPåBehandling = statusPåPerioderTjeneste.utled(
            behandling,
            kantIKantVurderer,
            kravdokumenter,
            kravdokumenterMedPeriode,
            perioderTilVurdering,
            perioderSomSkalTilbakestilles,
            revurderingPerioderFraAndreParter);
        return statusForPerioderPåBehandling;
    }

}
