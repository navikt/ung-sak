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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        var triggerPerioder = prosessTriggerPeriodeUtleder.utledTidslinje(behandling.getId());

        var vilkårPeriodeResultatMap = hentVilkårTidslinjer(behandling.getId());
        var samletVilkårTidslinje = samleVilkårIEnTidslinje(vilkårPeriodeResultatMap);

        var vilkårOgBehandlingsårsakerTidslinje = triggerPerioder
            .intersection(samletVilkårTidslinje,
                (p, behandlingÅrsaker, vilkårResultater)
                    -> new LocalDateSegment<>(p, new SamletVilkårResultatOgBehandlingÅrsaker(vilkårResultater.getValue(), behandlingÅrsaker.getValue())));

        var tilkjentYtelseTidslinje = tilkjentYtelseRepository.hentTidslinje(behandling.getId()).compress();

        var detaljertResultatTidslinje = vilkårOgBehandlingsårsakerTidslinje
            .combine(tilkjentYtelseTidslinje, DetaljertResultatUtlederImpl::bestemResultatForPeriode, JoinStyle.LEFT_JOIN);

        return detaljertResultatTidslinje.compress();

    }

    private static LocalDateSegment<DetaljertResultat> bestemResultatForPeriode(LocalDateInterval p, LocalDateSegment<SamletVilkårResultatOgBehandlingÅrsaker> lhs, LocalDateSegment<TilkjentYtelseVerdi> rhs) {
        SamletVilkårResultatOgBehandlingÅrsaker vilkårResultat = lhs.getValue();
        var tilkjentYtelse = rhs != null ? rhs.getValue() : null;

        if (vilkårResultat == null) {
            throw new IllegalStateException("Ingen vilkårsresultat for periode %s og tilkjentytelse %s".formatted(p, tilkjentYtelse));
        }

        var resultat = bestemDetaljertResultat(p, vilkårResultat, tilkjentYtelse);
        return new LocalDateSegment<>(p, resultat);

    }

    @SuppressWarnings("Convert2MethodRef")
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
                    list -> new LocalDateTimeline<>(list) // Ikke bruk Method reference da det gir kompilerings feil runtime!
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

        var resultatType = bestemResultat(p, vilkårResultat, tilkjentYtelse);

        return DetaljertResultat.of(resultatType, behandlingsårsaker, avslåtteVilkår, vilkårSomIkkeErVurdert);

    }

    private static DetaljertResultatInfo bestemResultat(
        LocalDateInterval p,
        SamletVilkårResultatOgBehandlingÅrsaker vilkårsresultatOgBehandlingsårsaker,
        TilkjentYtelseVerdi tilkjentYtelse) {

        if (!vilkårsresultatOgBehandlingsårsaker.ikkeVurderteVilkår().isEmpty()) {
            return DetaljertResultatInfo.of(DetaljertResultatType.IKKE_VURDERT);
        }

        var avslåtteVilkår = vilkårsresultatOgBehandlingsårsaker.avslåtteVilkår();
        var relevanteÅrsaker = vilkårsresultatOgBehandlingsårsaker.behandlingÅrsaker().stream()
            .filter(it -> it != BehandlingÅrsakType.UTTALELSE_FRA_BRUKER)
            .collect(Collectors.toSet());


        if (!avslåtteVilkår.isEmpty()) {
            if (innholderBare(relevanteÅrsaker, BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM)
                && harAvslåttVilkår(avslåtteVilkår, VilkårType.UNGDOMSPROGRAMVILKÅRET)) {
                return DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_ENDRING_STARTDATO);
            }

            return DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_INNGANGSVILKÅR);
        }

        if (tilkjentYtelse != null) {
            var tilkjentYtelseResultat = bestemDetaljertResultatMedTilkjentYtelse(tilkjentYtelse, relevanteÅrsaker);
            if (tilkjentYtelseResultat != null) return tilkjentYtelseResultat;
        }

        if (innholderBare(relevanteÅrsaker, BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM)) {
            return DetaljertResultatInfo.of(DetaljertResultatType.INNVILGELSE_ENDRING_STARTDATO);
        }

        if (innholderBare(relevanteÅrsaker, BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE)) {
            return DetaljertResultatInfo.of(DetaljertResultatType.INNVILGELSE_VILKÅR_NY_PERIODE);
        }
        if (innholderBare(relevanteÅrsaker, BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS)) {
            return DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_ØKT_SATS);
        }

        if (relevanteÅrsaker.contains(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM)) {
            return DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_ANNET, "Opphør av ungdomsprogram");
        }

        if (innholderBare(relevanteÅrsaker, BehandlingÅrsakType.RE_HENDELSE_FØDSEL)) {
            return DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_BARN_FØDSEL);
        }

        if (innholderBare(relevanteÅrsaker, BehandlingÅrsakType.RE_HENDELSE_DØD_BARN)) {
            return DetaljertResultatInfo.of(DetaljertResultatType.INNVILGELSE_ANNET, "Endring pga dødsfall av barn");
        }

        if (innholderBare(relevanteÅrsaker, BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER)) {
            return DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_ANNET, "Opphør pga dødsfall av søker");
        }

        if (relevanteÅrsaker.isEmpty()) {
            return DetaljertResultatInfo.of(DetaljertResultatType.INNVILGELSE_ANNET, "Uendret innvilget periode i kant med en endret periode");
        }

        throw new IllegalStateException("Klarte ikke å utlede resultattype for periode %s og vilkårsresultat og behandlingsårsaker %s og tilkjent ytelse %s"
            .formatted(p, vilkårsresultatOgBehandlingsårsaker, tilkjentYtelse));
    }

    private static DetaljertResultatInfo bestemDetaljertResultatMedTilkjentYtelse(TilkjentYtelseVerdi tilkjentYtelse, Set<BehandlingÅrsakType> behandlingsårsaker) {
        if (behandlingsårsaker.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)) {
            if (tilkjentYtelse.utbetalingsgrad() <= 0) {
                return DetaljertResultatInfo.of(DetaljertResultatType.KONTROLLER_INNTEKT_INGEN_UTBETALING);
            }

            if (tilkjentYtelse.utbetalingsgrad() >= 100) {
                return DetaljertResultatInfo.of(DetaljertResultatType.KONTROLLER_INNTEKT_FULL_UTBETALING);
            }

            return DetaljertResultatInfo.of(DetaljertResultatType.KONTROLLER_INNTEKT_REDUKSJON);
        }

        // Behandling ved endring av programperiode
        if (innholderBare(behandlingsårsaker, BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE)) {
            if (tilkjentYtelse.utbetalingsgrad() > 0) {
                return DetaljertResultatInfo.of(DetaljertResultatType.INNVILGELSE_UTBETALING_NY_PERIODE);
            }
            return DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_ANNET, "Ny programperiode uten tilkjent ytelse og uten rapportert inntekt");
        }

        if (tilkjentYtelse.utbetalingsgrad() <= 0) {
            return DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_ANNET, "Ingen utbetalingsgrad. TilkjentYtelse: %s, Behandlingsårsaker: %s".formatted(tilkjentYtelse, behandlingsårsaker));
        }

        return null;
    }

    @SafeVarargs
    private static <V> boolean innholderBare(Set<V> set, V... value) {
        return set.equals(Arrays.stream(value).collect(Collectors.toSet()));
    }

    private static boolean harAvslåttVilkår(Set<DetaljertVilkårResultat> avslåtteVilkår, VilkårType vilkårType) {
        return avslåtteVilkår.stream().anyMatch
            (it -> it.vilkårType() == vilkårType);
    }
}
