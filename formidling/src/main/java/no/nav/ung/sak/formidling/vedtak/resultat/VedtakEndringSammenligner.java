package no.nav.ung.sak.formidling.vedtak.resultat;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårPeriodeResultatDto;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Sammenligner vilkårsutfall og tilkjent ytelse i en behandling med originalbehandlingen.
 */
@Dependent
public class VedtakEndringSammenligner {

    private final VilkårResultatRepository vilkårResultatRepository;
    private final TilkjentYtelseRepository tilkjentYtelseRepository;

    @Inject
    public VedtakEndringSammenligner(VilkårResultatRepository vilkårResultatRepository,
                                     TilkjentYtelseRepository tilkjentYtelseRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
    }

    /**
     * Sammenligner behandlingen med originalbehandlingen innenfor avgrensningen.
     * Tom når behandlingen ikke har en originalbehandling.
     */
    public Optional<VedtakEndring> sammenlignMedOriginal(Behandling behandling, LocalDateTimeline<?> avgrensning) {
        return behandling.getOriginalBehandlingId().map(originalBehandlingId -> new VedtakEndring(
            endredeVilkår(behandling.getId(), originalBehandlingId, avgrensning),
            endretTilkjentYtelse(behandling.getId(), originalBehandlingId, avgrensning)));
    }

    private LocalDateTimeline<Set<VilkårType>> endredeVilkår(long behandlingId, long originalBehandlingId, LocalDateTimeline<?> avgrensning) {
        var nye = vilkårPerType(behandlingId, avgrensning);
        var originale = vilkårPerType(originalBehandlingId, avgrensning);

        var vilkårTyper = EnumSet.noneOf(VilkårType.class);
        vilkårTyper.addAll(nye.keySet());
        vilkårTyper.addAll(originale.keySet());

        var endringer = LocalDateTimeline.<Set<VilkårType>>empty();
        for (var vilkårType : vilkårTyper) {
            var ny = nye.getOrDefault(vilkårType, LocalDateTimeline.empty());
            var original = originale.getOrDefault(vilkårType, LocalDateTimeline.empty());

            var avvik = ny.crossJoin(original, (interval, nySegment, originalSegment) ->
                    new LocalDateSegment<>(interval, erEndret(behandlingId, verdi(nySegment), verdi(originalSegment))))
                .filterValue(Boolean::booleanValue);

            if (!avvik.isEmpty()) {
                endringer = endringer.crossJoin(avvik.mapValue(_ -> Set.of(vilkårType)), StandardCombinators::union);
            }
        }
        return endringer.compress();
    }

    private LocalDateTimeline<Boolean> endretTilkjentYtelse(long behandlingId, long originalBehandlingId, LocalDateTimeline<?> avgrensning) {
        var ny = tilkjentYtelseRepository.hentTidslinje(behandlingId).intersection(avgrensning);
        var original = tilkjentYtelseRepository.hentTidslinje(originalBehandlingId).intersection(avgrensning);

        return ny.crossJoin(original, (interval, nySegment, originalSegment) ->
                new LocalDateSegment<>(interval, erEndret(verdi(nySegment), verdi(originalSegment))))
            .filterValue(Boolean::booleanValue)
            .compress();
    }

    private Map<VilkårType, LocalDateTimeline<DetaljertVilkårResultat>> vilkårPerType(long behandlingId, LocalDateTimeline<?> avgrensning) {
        Map<VilkårType, List<LocalDateSegment<DetaljertVilkårResultat>>> segmenterPerType = new EnumMap<>(VilkårType.class);
        for (VilkårPeriodeResultatDto resultat : vilkårResultatRepository.hentVilkårResultater(behandlingId)) {
            segmenterPerType.computeIfAbsent(resultat.getVilkårType(), _ -> new ArrayList<>())
                .add(new LocalDateSegment<>(resultat.getPeriode().getFom(), resultat.getPeriode().getTom(),
                    new DetaljertVilkårResultat(resultat.getAvslagsårsak(), resultat.getVilkårType(), resultat.getUtfall())));
        }

        Map<VilkårType, LocalDateTimeline<DetaljertVilkårResultat>> perType = new EnumMap<>(VilkårType.class);
        segmenterPerType.forEach((vilkårType, segmenter) -> {
            var tidslinje = new LocalDateTimeline<>(segmenter).intersection(avgrensning);
            if (!tidslinje.isEmpty()) {
                perType.put(vilkårType, tidslinje);
            }
        });
        return perType;
    }

    private static boolean erEndret(long behandlingId, DetaljertVilkårResultat ny, DetaljertVilkårResultat original) {
        // Brevreglene kjører etter at vilkårene er vurdert, så IKKE_VURDERT innenfor avgrensningen er en prosessfeil - ikke en «uendring».
        if (ny != null && ny.utfall() == Utfall.IKKE_VURDERT) {
            throw new IllegalStateException("Vilkår " + ny.vilkårType() + " er ikke vurdert innenfor perioden til vurdering, behandlingId: " + behandlingId);
        }
        if (ny == null || original == null) {
            return true;
        }
        return ny.utfall() != original.utfall() || ny.avslagsårsak() != original.avslagsårsak();
    }

    private static boolean erEndret(TilkjentYtelseVerdi ny, TilkjentYtelseVerdi original) {
        if (ny == null || original == null) {
            return true;
        }
        return erUlike(ny.dagsats(), original.dagsats())
            || erUlike(ny.redusertBeløp(), original.redusertBeløp())
            || erUlike(ny.utbetalingsgrad(), original.utbetalingsgrad())
            || erUlike(ny.tilkjentBeløp(), original.tilkjentBeløp());
    }

    // BigDecimal.equals er skalasensitiv - 10.00 og 10.0 er ulike verdier for equals, men samme beløp.
    private static boolean erUlike(BigDecimal ny, BigDecimal original) {
        if (ny == null || original == null) {
            return ny != original;
        }
        return ny.compareTo(original) != 0;
    }

    private static <V> V verdi(LocalDateSegment<V> segment) {
        return segment == null ? null : segment.getValue();
    }
}
