package no.nav.ung.sak.formidling.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårPeriodeResultatDto;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@Dependent
public class DetaljertResultatUtlederImpl implements DetaljertResultatUtleder {

    private final ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private final TilkjentYtelseRepository tilkjentYtelseRepository;
    private final VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public DetaljertResultatUtlederImpl(
        ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
        TilkjentYtelseRepository tilkjentYtelseRepository,
        VilkårResultatRepository vilkårResultatRepository) {

        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public LocalDateTimeline<DetaljertResultat> utledDetaljertResultat(Behandling behandling) {

        var perioderTilVurdering = prosessTriggerPeriodeUtleder.utledTidslinje(behandling.getId());

        var tilkjentYtelseTidslinje = tilkjentYtelseRepository.hentTidslinje(behandling.getId()).compress();

        var vilkårPeriodeResultatMap = hentVilkårTidslinjer(behandling.getId());
        var samletVilkårTidslinje = samleVilkårIEnTidslinje(vilkårPeriodeResultatMap);

        var vilkårOgBehandlingsårsakerTidslinje = perioderTilVurdering
            .combine(samletVilkårTidslinje, DetaljertResultatUtlederImpl::kombinerVilkårOgBehandlingsårsaker, JoinStyle.LEFT_JOIN);

        var detaljertResultatTidslinje = vilkårOgBehandlingsårsakerTidslinje
            .combine(tilkjentYtelseTidslinje, DetaljertResultatUtlederImpl::kombinerMedTilkjentYtelse, JoinStyle.LEFT_JOIN);

        return detaljertResultatTidslinje.compress();

    }

    private static LocalDateSegment<SamletVilkårResultatOgBehandlingÅrsaker> kombinerVilkårOgBehandlingsårsaker(LocalDateInterval p, LocalDateSegment<Set<BehandlingÅrsakType>> lhs, LocalDateSegment<List<DetaljertVilkårResultat>> rhs) {
        Set<BehandlingÅrsakType> årsaker = lhs != null ? lhs.getValue() : Collections.emptySet();
        List<DetaljertVilkårResultat> samletResultat = rhs != null ? rhs.getValue() : null;

        if (samletResultat == null) {
            throw new IllegalStateException("Ingen vilkårsresultat for periode %s og årsaker %s ".formatted(p, årsaker));
        }

        var resultat = new SamletVilkårResultatOgBehandlingÅrsaker(samletResultat, årsaker);
        return new LocalDateSegment<>(p, resultat);
    }

    private static LocalDateSegment<DetaljertResultat> kombinerMedTilkjentYtelse(LocalDateInterval p, LocalDateSegment<SamletVilkårResultatOgBehandlingÅrsaker> lhs, LocalDateSegment<TilkjentYtelseVerdi> rhs) {
        SamletVilkårResultatOgBehandlingÅrsaker vilkårResultat = lhs != null ? lhs.getValue() : null;
        var tilkjentYtelse = rhs != null ? rhs.getValue() : null;

        if (vilkårResultat == null) {
            throw new IllegalStateException("Ingen vilkårsresultat for periode %s og tilkjentytelse %s".formatted(p, tilkjentYtelse));
        }

        var resultat = bestemDetaljertResultat(p, vilkårResultat, tilkjentYtelse);
        return new LocalDateSegment<>(p, resultat);

    }

    private Map<VilkårType, LocalDateTimeline<DetaljertVilkårResultat>> hentVilkårTidslinjer(Long behandlingId) {
        return vilkårResultatRepository.hentVilkårResultater(behandlingId).stream()
            .collect(Collectors.groupingBy(
                VilkårPeriodeResultatDto::getVilkårType,
                Collectors.collectingAndThen(
                    Collectors.mapping(it -> new LocalDateSegment<>(
                            it.getPeriode().getFom(),
                            it.getPeriode().getTom(),
                            new DetaljertVilkårResultat(it.getAvslagsårsak(), it.getVilkårType(), it.getUtfall())
                        ), Collectors.toList()
                    ),
                    LocalDateTimeline::new
                )
            ));
    }

    private static LocalDateTimeline<List<DetaljertVilkårResultat>> samleVilkårIEnTidslinje(Map<VilkårType, LocalDateTimeline<DetaljertVilkårResultat>> vilkårPeriodeResultatMap) {
        var samletVilkårTidslinje = LocalDateTimeline.<List<DetaljertVilkårResultat>>empty();
        for (var entry : vilkårPeriodeResultatMap.entrySet()) {
            LocalDateTimeline<DetaljertVilkårResultat> v = entry.getValue();
            samletVilkårTidslinje = samletVilkårTidslinje.crossJoin(v, StandardCombinators::allValues);
        }
        return samletVilkårTidslinje;
    }

    private static DetaljertResultat bestemDetaljertResultat(LocalDateInterval p, SamletVilkårResultatOgBehandlingÅrsaker vilkårResultat, TilkjentYtelseVerdi tilkjentYtelse) {
        var vilkårSomIkkeErVurdert = vilkårResultat.ikkeVurderteVilkår();
        var avslåtteVilkår = vilkårResultat.avslåtteVilkår();
        var behandlingsårsaker = vilkårResultat.behandlingÅrsaker();

        DetaljertResultatType resultatType = bestemResultat(p, vilkårResultat, tilkjentYtelse, behandlingsårsaker);

        return DetaljertResultat.of(resultatType, behandlingsårsaker, avslåtteVilkår, vilkårSomIkkeErVurdert);

    }

    @NotNull
    private static DetaljertResultatType bestemResultat(
        LocalDateInterval p,
        SamletVilkårResultatOgBehandlingÅrsaker vilkårResultat,
        TilkjentYtelseVerdi tilkjentYtelse,
        Set<BehandlingÅrsakType> behandlingsårsaker) {

        if (!vilkårResultat.ikkeVurderteVilkår().isEmpty())  {
            return DetaljertResultatType.IKKE_VURDERT;
        }

        if (!vilkårResultat.avslåtteVilkår().isEmpty()) {
            return DetaljertResultatType.AVSLAG_INNGANGSVILKÅR;
        }

        if (tilkjentYtelse != null) {
            DetaljertResultatType tilkjentYtelseResultat = bestemDetaljertResultatMedTilkjentYtelse(tilkjentYtelse, behandlingsårsaker);
            if (tilkjentYtelseResultat != null) return tilkjentYtelseResultat;
        }

        if (innholderBare(behandlingsårsaker, BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE)) {
            return DetaljertResultatType.INNVILGELSE_VILKÅR_NY_PERIODE;
        }
        if (innholderBare(behandlingsårsaker, BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS)) {
            return DetaljertResultatType.ENDRING_ØKT_SATS;
        }

        throw new IllegalStateException("Klarte ikke å utlede resultattype for periode %s og vilkår %s og tilkjent ytelse %s og behandlingårsaker %s"
                .formatted(p, vilkårResultat, tilkjentYtelse, behandlingsårsaker));
    }

    @Nullable
    private static DetaljertResultatType bestemDetaljertResultatMedTilkjentYtelse(TilkjentYtelseVerdi tilkjentYtelse, Set<BehandlingÅrsakType> behandlingsårsaker) {
        if (behandlingsårsaker.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)) {
            if (tilkjentYtelse.utbetalingsgrad() > 0) {
                return DetaljertResultatType.ENDRING_RAPPORTERT_INNTEKT;
            }
            return DetaljertResultatType.AVSLAG_RAPPORTERT_INNTEKT;
        }

        if (innholderBare(behandlingsårsaker, BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE)) {
            if (tilkjentYtelse.utbetalingsgrad() > 0) {
                return DetaljertResultatType.INNVILGELSE_UTBETALING_NY_PERIODE;
            }
            return DetaljertResultatType.AVSLAG_UTBETALING_NY_PERIODE;
        }
        return null;
    }

    @SafeVarargs
    private static <V> boolean innholderBare(Set<V> set, V... value) {
        return set.equals(Arrays.stream(value).collect(Collectors.toSet()));
    }


}
