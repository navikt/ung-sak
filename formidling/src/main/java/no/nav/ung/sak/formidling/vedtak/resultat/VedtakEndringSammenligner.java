package no.nav.ung.sak.formidling.vedtak.resultat;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårPeriodeResultatDto;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sammenligner vilkårsutfall og dagsats i en behandling med originalbehandlingen.
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
            vilkårEndringer(behandling.getId(), originalBehandlingId, avgrensning),
            dagsatsEndringer(behandling.getId(), originalBehandlingId, avgrensning)));
    }

    private LocalDateTimeline<Map<VilkårType, VilkårEndringType>> vilkårEndringer(long behandlingId, long originalBehandlingId, LocalDateTimeline<?> avgrensning) {
        var nye = detaljertVilkårResultatPerType(behandlingId, avgrensning);
        var originale = detaljertVilkårResultatPerType(originalBehandlingId, avgrensning);

        var vilkårTyper = EnumSet.noneOf(VilkårType.class);
        vilkårTyper.addAll(nye.keySet());
        vilkårTyper.addAll(originale.keySet());

        var endringer = vilkårTyper.stream()
            .flatMap(vilkårType -> nye.getOrDefault(vilkårType, LocalDateTimeline.empty())
                .crossJoin(originale.getOrDefault(vilkårType, LocalDateTimeline.empty()), (interval, nySegment, originalSegment) ->
                    new LocalDateSegment<>(interval, Map.of(vilkårType, endringType(verdi(nySegment), verdi(originalSegment)))))
                .stream())
            .toList();
        return new LocalDateTimeline<>(endringer, VedtakEndringSammenligner::slåSammen).compress();
    }

    private static LocalDateSegment<Map<VilkårType, VilkårEndringType>> slåSammen(LocalDateInterval interval,
                                                                                  LocalDateSegment<Map<VilkårType, VilkårEndringType>> lhs,
                                                                                  LocalDateSegment<Map<VilkårType, VilkårEndringType>> rhs) {
        var sammenslått = new EnumMap<>(lhs.getValue());
        sammenslått.putAll(rhs.getValue());
        return new LocalDateSegment<>(interval, sammenslått);
    }

    private LocalDateTimeline<DagsatsEndringType> dagsatsEndringer(long behandlingId, long originalBehandlingId, LocalDateTimeline<?> avgrensning) {
        var ny = dagsats(behandlingId, avgrensning);
        var original = dagsats(originalBehandlingId, avgrensning);

        return ny.crossJoin(original, (interval, nySegment, originalSegment) ->
                new LocalDateSegment<>(interval, endringType(dagsatsEller0(nySegment), dagsatsEller0(originalSegment))))
            .compress();
    }

    private LocalDateTimeline<BigDecimal> dagsats(long behandlingId, LocalDateTimeline<?> avgrensning) {
        return tilkjentYtelseRepository.hentTidslinje(behandlingId).intersection(avgrensning).mapValue(TilkjentYtelseVerdi::dagsats);
    }

    private Map<VilkårType, LocalDateTimeline<DetaljertVilkårResultat>> detaljertVilkårResultatPerType(long behandlingId, LocalDateTimeline<?> avgrensning) {
        return vilkårResultatRepository.hentVilkårResultater(behandlingId).stream()
            .collect(Collectors.groupingBy(
                VilkårPeriodeResultatDto::getVilkårType,
                () -> new EnumMap<>(VilkårType.class),
                Collectors.collectingAndThen(
                    Collectors.mapping(resultat -> new LocalDateSegment<>(resultat.getPeriode().getFom(), resultat.getPeriode().getTom(), new DetaljertVilkårResultat(resultat.getAvslagsårsak(), resultat.getVilkårType(), resultat.getUtfall())), Collectors.toList()),
                    segmenter -> new LocalDateTimeline<>(segmenter).intersection(avgrensning)
                ))
            );
    }

    private static VilkårEndringType endringType(DetaljertVilkårResultat ny, DetaljertVilkårResultat original) {
        if (original == null) {
            return VilkårEndringType.NY;
        }
        if (ny == null) {
            return VilkårEndringType.TRUKKET;
        }
        var erLike = ny.utfall() == original.utfall() && ny.avslagsårsak() == original.avslagsårsak();
        return erLike ? VilkårEndringType.UENDRET : VilkårEndringType.ENDRET;
    }

    private static DagsatsEndringType endringType(BigDecimal ny, BigDecimal original) {
        return switch (Integer.signum(ny.compareTo(original))) {
            case 1 -> DagsatsEndringType.ØKNING;
            case -1 -> DagsatsEndringType.REDUKSJON;
            default -> DagsatsEndringType.UENDRET;
        };
    }

    private static BigDecimal dagsatsEller0(LocalDateSegment<BigDecimal> segment) {
        return segment == null || segment.getValue() == null ? BigDecimal.ZERO : segment.getValue();
    }

    private static <V> V verdi(LocalDateSegment<V> segment) {
        return segment == null ? null : segment.getValue();
    }
}
