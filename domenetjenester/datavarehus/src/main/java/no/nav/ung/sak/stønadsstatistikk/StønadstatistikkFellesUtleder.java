package no.nav.ung.sak.stønadsstatistikk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelse;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelsePeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadsstatistikkTilkjentYtelsePeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkInngangsvilkår;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkPeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkUtfall;

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

/**
 * Utleder de delene av stønadsstatistikk-hendelsen som er felles for alle ytelser.
 */
@ApplicationScoped
public class StønadstatistikkFellesUtleder {

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private SøknadRepository søknadRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private TilkjentYtelseRepository tilkjentYtelseRepository;

    StønadstatistikkFellesUtleder() {
        //for CDI proxy
    }

    @Inject
    public StønadstatistikkFellesUtleder(BehandlingRepository behandlingRepository,
                                         BehandlingVedtakRepository behandlingVedtakRepository,
                                         SøknadRepository søknadRepository,
                                         VilkårResultatRepository vilkårResultatRepository,
                                         TilkjentYtelseRepository tilkjentYtelseRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.søknadRepository = søknadRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
    }

    public LocalDateTime hentVedtakTidspunkt(Behandling behandling) {
        return behandlingVedtakRepository.hentBehandlingVedtakFor(behandling.getUuid()).orElseThrow().getVedtakstidspunkt();
    }

    public UUID hentOrginalBehandlingUuidHvisEksisterer(Behandling behandling) {
        return behandling.getOriginalBehandlingId()
            .map(it -> behandlingRepository.hentBehandling(it))
            .map(Behandling::getUuid)
            .orElse(null);
    }

    public LocalDate hentFørsteSøknadsdato(Behandling behandling) {
        List<SøknadEntitet> søknader = søknadRepository.hentSøknaderForFagsak(behandling.getFagsakId());
        return søknader.stream().map(SøknadEntitet::getMottattDato).min(Comparator.naturalOrder()).orElseThrow();
    }

    public List<StønadstatistikkPeriode> hentBehandlingsperioder(Behandling behandling) {
        LocalDateTimeline<Map<VilkårType, Utfall>> vilkårResultatTidslinje = lagVilkårTidslineFraVilkårResultat(behandling);
        return vilkårResultatTidslinje.segmenter().stream()
            .map(StønadstatistikkFellesUtleder::map)
            .toList();
    }

    public List<StønadsstatistikkTilkjentYtelsePeriode> hentTilkjentYtelsePerioder(Behandling behandling, String klassekode) {
        List<TilkjentYtelsePeriode> tilkjentYtelsePerioder = tilkjentYtelseRepository.hentTilkjentYtelse(behandling.getId())
            .map(TilkjentYtelse::getPerioder)
            .orElse(List.of());
        return tilkjentYtelsePerioder.stream()
            .map(it -> new StønadsstatistikkTilkjentYtelsePeriode(
                it.getPeriode().getFomDato(),
                it.getPeriode().getTomDato(),
                normaliser(it.getDagsats()),
                normaliser(it.getReduksjon()),
                klassekode))
            .sorted(Comparator.comparing(StønadsstatistikkTilkjentYtelsePeriode::fom))
            .toList();
    }

    public static BigDecimal normaliser(BigDecimal beløp) {
        //for å få tall til å serialiseres 'normalt', uten ekstra 0-desimaler eller vitenskaplig format (for eksempel '0e9')
        return beløp.setScale(Math.max(0, beløp.scale()), RoundingMode.UNNECESSARY);
    }

    private LocalDateTimeline<Map<VilkårType, Utfall>> lagVilkårTidslineFraVilkårResultat(Behandling behandling) {
        Vilkårene vilkårene = vilkårResultatRepository.hent(behandling.getId());
        List<LocalDateSegment<Map<VilkårType, Utfall>>> segmenter = new ArrayList<>();
        Map<VilkårType, LocalDateTimeline<VilkårPeriode>> vilkårTidslinjer = vilkårene.getVilkårTidslinjer(DatoIntervallEntitet.fra(TIDENES_BEGYNNELSE, TIDENES_ENDE));
        for (Map.Entry<VilkårType, LocalDateTimeline<VilkårPeriode>> entry : vilkårTidslinjer.entrySet()) {
            for (LocalDateSegment<VilkårPeriode> vilkårSegment : entry.getValue().segmenter()) {
                segmenter.add(new LocalDateSegment<>(vilkårSegment.getLocalDateInterval(), Map.of(entry.getKey(), vilkårSegment.getValue().getUtfall())));
            }
        }
        return new LocalDateTimeline<>(segmenter, SEGMENT_KOMBINATOR_VILKÅR_UTFALL);
    }

    private static StønadstatistikkPeriode map(LocalDateSegment<Map<VilkårType, Utfall>> vilkårsegment) {
        StønadstatistikkUtfall samletUtfall = map(Utfall.ranger(vilkårsegment.getValue().values()));
        List<StønadstatistikkInngangsvilkår> inngangsvilkår = vilkårsegment.getValue().entrySet().stream()
            .map(e -> new StønadstatistikkInngangsvilkår(e.getKey(), map(e.getValue())))
            .toList();
        return new StønadstatistikkPeriode(vilkårsegment.getFom(), vilkårsegment.getTom(), samletUtfall, inngangsvilkår);
    }

    private static final LocalDateSegmentCombinator<Map<VilkårType, Utfall>, Map<VilkårType, Utfall>, Map<VilkårType, Utfall>> SEGMENT_KOMBINATOR_VILKÅR_UTFALL = (LocalDateInterval intervall, LocalDateSegment<Map<VilkårType, Utfall>> lhs, LocalDateSegment<Map<VilkårType, Utfall>> rhs) -> new LocalDateSegment<>(intervall, nullSafeUnion(lhs, rhs));

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

    static StønadstatistikkUtfall map(Utfall value) {
        return switch (value) {
            case IKKE_OPPFYLT -> StønadstatistikkUtfall.IKKE_OPPFYLT;
            case IKKE_VURDERT -> StønadstatistikkUtfall.IKKE_VURDERT;
            case OPPFYLT -> StønadstatistikkUtfall.OPPFYLT;
            case IKKE_RELEVANT -> StønadstatistikkUtfall.IKKE_RELEVANT;
            case UDEFINERT -> throw new IllegalArgumentException("Ikke-støttet utfall-type: " + value);
        };
    }
}
