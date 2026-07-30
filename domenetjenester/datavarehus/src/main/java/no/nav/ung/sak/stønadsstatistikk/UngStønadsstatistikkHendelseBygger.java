package no.nav.ung.sak.stønadsstatistikk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.oppdrag.kontrakt.sporing.HenvisningUtleder;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelse;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelsePeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlag;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.hendelse.stønadstatistikk.StønadstatistikkHendelseBygger;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadsstatistikkSatsPeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadsstatistikkTilkjentYtelsePeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkHendelse;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkInngangsvilkår;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkInntektPeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkPeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkSatsType;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkUtfall;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.UngdomsprogramDeltakelsePeriode;
import no.nav.ung.sak.kontroll.KontrollerteInntektperioderTjeneste;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_BEGYNNELSE;
import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public class UngStønadsstatistikkHendelseBygger implements StønadstatistikkHendelseBygger {

    private static final FagsakYtelseType YTELSE_TYPE = FagsakYtelseType.UNGDOMSYTELSE;
    private static final String KLASSEKODE = "UNG";

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private SøknadRepository søknadRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;

    UngStønadsstatistikkHendelseBygger() {
        //for CDI proxy
    }

    @Inject
    public UngStønadsstatistikkHendelseBygger(BehandlingRepository behandlingRepository, BehandlingVedtakRepository behandlingVedtakRepository, SøknadRepository søknadRepository, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository, UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository, VilkårResultatRepository vilkårResultatRepository, TilkjentYtelseRepository tilkjentYtelseRepository, KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.søknadRepository = søknadRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
    }

    @Override
    public StønadstatistikkHendelse lagHendelse(UUID behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid);
        return new StønadstatistikkHendelse(
            YTELSE_TYPE,
            behandling.getAktørId(),
            behandling.getFagsak().getSaksnummer(),
            behandling.getUuid(),
            hentOrginalBehandlingUuidHvisEksisterer(behandling),
            hentFørsteSøknadsdato(behandling),
            hentVedtakTidspunkt(behandling),
            HenvisningUtleder.utledHenvisning(behandling.getUuid(), null),
            hentDeltakelsePeriode(behandling),
            hentBehandlingsperioder(behandling),
            hentSatsPerioder(behandling),
            hentTilkjentYtelsePerioder(behandling),
            hentInntektPerioder(behandling)
        );
    }

    private LocalDateTime hentVedtakTidspunkt(Behandling behandling) {
        return behandlingVedtakRepository.hentBehandlingVedtakFor(behandling.getUuid()).orElseThrow().getVedtakstidspunkt();
    }

    private UUID hentOrginalBehandlingUuidHvisEksisterer(Behandling behandling) {
        return behandling.getOriginalBehandlingId()
            .map(it -> behandlingRepository.hentBehandling(it))
            .map(Behandling::getUuid)
            .orElse(null);
    }

    private LocalDate hentFørsteSøknadsdato(Behandling behandling) {
        List<SøknadEntitet> søknader = søknadRepository.hentSøknaderForFagsak(behandling.getFagsakId());
        return søknader.stream().map(SøknadEntitet::getMottattDato).min(Comparator.naturalOrder()).orElseThrow();
    }

    private UngdomsprogramDeltakelsePeriode hentDeltakelsePeriode(Behandling behandling) {
        UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElse(null);
        if (ungdomsprogramPeriodeGrunnlag == null || ungdomsprogramPeriodeGrunnlag.getUngdomsprogramPerioder().getPerioder().isEmpty()){
            return null;
        }
        DatoIntervallEntitet ungdomsprogramPeriode = ungdomsprogramPeriodeGrunnlag.hentForEksaktEnPeriode();
        return new UngdomsprogramDeltakelsePeriode(ungdomsprogramPeriode.getFomDato(), ungdomsprogramPeriode.getTomDato());
    }

    private List<StønadstatistikkPeriode> hentBehandlingsperioder(Behandling behandling) {
        LocalDateTimeline<Map<VilkårType, Utfall>> vilkårResultatTidslinje = lagVilkårTidslineFraVilkårResultat(behandling);
        return vilkårResultatTidslinje.toSegments().stream()
            .map(this::map)
            .toList();
    }

    private StønadstatistikkPeriode map(LocalDateSegment<Map<VilkårType, Utfall>> vilkårsegment) {
        StønadstatistikkUtfall samletUtfall = map(Utfall.ranger(vilkårsegment.getValue().values()));
        List<StønadstatistikkInngangsvilkår> inngangsvilkår = vilkårsegment.getValue().entrySet().stream()
            .map(e -> new StønadstatistikkInngangsvilkår(e.getKey(), map(e.getValue())))
            .toList();
        return new StønadstatistikkPeriode(vilkårsegment.getFom(), vilkårsegment.getTom(), samletUtfall, inngangsvilkår);
    }

    private LocalDateTimeline<Map<VilkårType, Utfall>> lagVilkårTidslineFraVilkårResultat(Behandling behandling) {
        Vilkårene vilkårene = vilkårResultatRepository.hent(behandling.getId());
        List<LocalDateSegment<Map<VilkårType, Utfall>>> segmenter = new ArrayList<>();
        Map<VilkårType, LocalDateTimeline<VilkårPeriode>> vilkårTidslinjer = vilkårene.getVilkårTidslinjer(DatoIntervallEntitet.fra(TIDENES_BEGYNNELSE, TIDENES_ENDE));
        for (Map.Entry<VilkårType, LocalDateTimeline<VilkårPeriode>> entry : vilkårTidslinjer.entrySet()) {
            for (LocalDateSegment<VilkårPeriode> vilkårSegment : entry.getValue().toSegments()) {
                segmenter.add(new LocalDateSegment<>(vilkårSegment.getLocalDateInterval(), Map.of(entry.getKey(), vilkårSegment.getValue().getUtfall())));
            }
        }
        return new LocalDateTimeline<>(segmenter, SEGMENT_KOMBINATOR_VILKÅR_UTFALL);
    }

    private List<StønadsstatistikkSatsPeriode> hentSatsPerioder(Behandling behandling) {
        UngdomsytelseGrunnlag ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandling.getId()).orElse(null);
        if (ungdomsytelseGrunnlag == null) {
            return List.of();
        }
        return ungdomsytelseGrunnlag.getSatsTidslinje().toSegments()
            .stream().map(it -> new StønadsstatistikkSatsPeriode(
                it.getFom(),
                it.getTom(),
                map(it.getValue().satsType()),
                it.getValue().dagsats(),
                it.getValue().antallBarn(),
                it.getValue().dagsatsBarnetillegg(),
                it.getValue().grunnbeløpFaktor()))
            .sorted(Comparator.comparing(StønadsstatistikkSatsPeriode::fom))
            .toList();
    }

    private List<StønadstatistikkInntektPeriode> hentInntektPerioder(Behandling behandling) {
        return kontrollerteInntektperioderTjeneste.hentTidslinje(behandling.getId()).toSegments().stream()
            .map(it -> new StønadstatistikkInntektPeriode(
                it.getFom(),
                it.getTom(),
                normaliser(it.getValue())
            ))
            .toList();
    }

    private List<StønadsstatistikkTilkjentYtelsePeriode> hentTilkjentYtelsePerioder(Behandling behandling) {
        List<TilkjentYtelsePeriode> tilkjentYtelsePerioder = tilkjentYtelseRepository.hentTilkjentYtelse(behandling.getId())
            .map(TilkjentYtelse::getPerioder)
            .orElse(List.of());
        return tilkjentYtelsePerioder.stream()
            .map(it -> new StønadsstatistikkTilkjentYtelsePeriode(
                it.getPeriode().getFomDato(),
                it.getPeriode().getTomDato(),
                normaliser(it.getDagsats()),
                normaliser(it.getReduksjon()),
                KLASSEKODE))
            .sorted(Comparator.comparing(StønadsstatistikkTilkjentYtelsePeriode::fom))
            .toList();

    }

    private static BigDecimal normaliser(BigDecimal beløp) {
        //for å få tall til å serialiseres 'normalt', uten ekstra 0-desimaler eller vitenskaplig format (for eksempel '0e9')
        return beløp.setScale(Math.max(0, beløp.scale()), RoundingMode.UNNECESSARY);
    }

    private static LocalDateSegmentCombinator<Map<VilkårType, Utfall>, Map<VilkårType, Utfall>, Map<VilkårType, Utfall>> SEGMENT_KOMBINATOR_VILKÅR_UTFALL = (LocalDateInterval intervall, LocalDateSegment<Map<VilkårType, Utfall>> lhs, LocalDateSegment<Map<VilkårType, Utfall>> rhs) -> new LocalDateSegment<>(intervall, nullSafeUnion(lhs, rhs));

    private static Map<VilkårType, Utfall> nullSafeUnion(LocalDateSegment<Map<VilkårType, Utfall>> lhs, LocalDateSegment<Map<VilkårType, Utfall>> rhs) {
        if (lhs != null && rhs != null) {
            return union(lhs.getValue(), rhs.getValue());
        }
        return lhs != null ? lhs.getValue() : rhs.getValue();
    }

    private static Map<VilkårType, Utfall> union(Map<VilkårType, Utfall> a, Map<VilkårType, Utfall> b) {
        Map<VilkårType, Utfall> resultat = new EnumMap<>(VilkårType.class);
        resultat.putAll(a);
        resultat.putAll(b);
        return resultat;
    }

    private static StønadstatistikkUtfall map(Utfall value) {
        return switch (value) {
            case IKKE_OPPFYLT -> StønadstatistikkUtfall.IKKE_OPPFYLT;
            case IKKE_VURDERT -> StønadstatistikkUtfall.IKKE_VURDERT;
            case OPPFYLT -> StønadstatistikkUtfall.OPPFYLT;
            case IKKE_RELEVANT, UDEFINERT -> throw new IllegalArgumentException("Ikke-støttet utfall-type: " + value);
        };
    }

    private static StønadstatistikkSatsType map(UngdomsytelseSatsType satsType) {
        return switch (satsType) {
            case LAV -> StønadstatistikkSatsType.LAV;
            case HØY -> StønadstatistikkSatsType.HØY;
        };
    }
}
