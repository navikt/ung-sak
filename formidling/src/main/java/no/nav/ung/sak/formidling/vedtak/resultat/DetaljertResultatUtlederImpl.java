package no.nav.ung.sak.formidling.vedtak.resultat;

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

        var triggerPerioder = prosessTriggerPeriodeUtleder.utledTidslinje(behandling.getId());

        var vilkårPeriodeResultatMap = hentVilkårTidslinjer(behandling.getId());
        var samletVilkårTidslinje = samleVilkårIEnTidslinje(vilkårPeriodeResultatMap);

        var vilkårOgBehandlingsårsakerTidslinje = triggerPerioder
            .intersection(samletVilkårTidslinje,
                (p, behandlingÅrsaker, vilkårResultater)
                    -> new LocalDateSegment<>(p, new SamletVilkårResultatOgBehandlingÅrsaker(vilkårResultater.getValue(), behandlingÅrsaker.getValue(), behandling.erManueltOpprettet())));

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

        return new DetaljertResultat(resultatType, behandlingsårsaker, avslåtteVilkår, vilkårSomIkkeErVurdert);

    }

    private static final Map<BehandlingÅrsakType, DetaljertResultatInfo> ÅRSAK_RESULTAT_INNVILGELSE_MAP = Map.of(
        BehandlingÅrsakType.RE_HENDELSE_DØD_BARN, DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_BARN_DØDSFALL),
        BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_ØKT_SATS),
        BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_BARN_FØDSEL),
        BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER, DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_DELTAKER_DØDSFALL)
    );

    private static Set<DetaljertResultatInfo> bestemResultat(
        LocalDateInterval p,
        SamletVilkårResultatOgBehandlingÅrsaker vilkårsresultatOgBehandlingsårsaker,
        TilkjentYtelseVerdi tilkjentYtelse) {

        if (!vilkårsresultatOgBehandlingsårsaker.ikkeVurderteVilkår().isEmpty()) {
            return Set.of(DetaljertResultatInfo.of(DetaljertResultatType.IKKE_VURDERT));
        }

        var avslåtteVilkår = vilkårsresultatOgBehandlingsårsaker.avslåtteVilkår();

        var relevanteÅrsaker = new HashSet<>(vilkårsresultatOgBehandlingsårsaker.behandlingÅrsaker());
        relevanteÅrsaker.remove(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER);

        var resultater = new HashSet<DetaljertResultatInfo>();

        if (relevanteÅrsaker.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)) {
            resultater.add(kontrollerInntektDetaljertResultat(tilkjentYtelse));
        }

        if (relevanteÅrsaker.contains(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE)
            || vilkårsresultatOgBehandlingsårsaker.manuellOpprettetBehandling() && relevanteÅrsaker.contains(BehandlingÅrsakType.RE_SATS_ENDRING)) {
            resultater.add(nyPeriodeDetaljertResultat(avslåtteVilkår, tilkjentYtelse));
        }

        if (relevanteÅrsaker.contains(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM)) {
            resultater.add(endretStartdatoDetaljertResultat(avslåtteVilkår));
        }

        if (relevanteÅrsaker.contains(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM)) {
            resultater.add(endretSluttdatoDetaljertResultat(avslåtteVilkår));
        }

        relevanteÅrsaker.stream()
            .filter(ÅRSAK_RESULTAT_INNVILGELSE_MAP::containsKey)
            .map(årsak -> behandlingsårsakDetaljertResultat(årsak, avslåtteVilkår))
            .forEach(resultater::add);

        if (resultater.isEmpty()) {
            return håndterUkjenteTilfeller(tilkjentYtelse, avslåtteVilkår, relevanteÅrsaker)
                .map(Set::of)
                .orElseThrow(() -> new IllegalStateException(
                    "Klarte ikke å utlede resultattype for periode %s og vilkårsresultat og behandlingsårsaker %s og tilkjent ytelse %s"
                        .formatted(p, vilkårsresultatOgBehandlingsårsaker, tilkjentYtelse)));
        }

        return resultater;

    }

    private static DetaljertResultatInfo behandlingsårsakDetaljertResultat(BehandlingÅrsakType key, Set<DetaljertVilkårResultat> avslåtteVilkår) {
        if (avslåtteVilkår.isEmpty()) {
            return ÅRSAK_RESULTAT_INNVILGELSE_MAP.get(key);
        }

        return DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_INNGANGSVILKÅR, "Avslåtte inngangsvilkår, med behandlingsårsak %s".formatted(key));
    }

    private static Optional<DetaljertResultatInfo> håndterUkjenteTilfeller(TilkjentYtelseVerdi tilkjentYtelse, Set<DetaljertVilkårResultat> avslåtteVilkår, Set<BehandlingÅrsakType> relevanteÅrsaker) {
        if (!avslåtteVilkår.isEmpty()) {
            return Optional.of(DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_INNGANGSVILKÅR, "Avslåtte inngangsvilkår ukjent årsak"));
        }

        if (tilkjentYtelse == null) {
            return Optional.of(DetaljertResultatInfo.of(DetaljertResultatType.INNVILGELSE_ANNET, "Innvilgede vilkår uten tilkjent ytelse"));
        }

        if (tilkjentYtelse.utbetalingsgrad() <= 0) {
            return Optional.of(DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_ANNET, "Innvilgede vilkår 0 kr tilkjent ytelse"));
        }

        if (relevanteÅrsaker.isEmpty()) {
            return Optional.of(DetaljertResultatInfo.of(DetaljertResultatType.INNVILGET_UTEN_ÅRSAK, "Innvilget uten årsak"));
        }

        return Optional.empty();
    }

    private static DetaljertResultatInfo endretSluttdatoDetaljertResultat(Set<DetaljertVilkårResultat> avslåtteVilkår) {
        if (harAvslåttVilkår(avslåtteVilkår, VilkårType.UNGDOMSPROGRAMVILKÅRET)) {
            return DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_SLUTTDATO, "Opphør av ungdomsprogramperiode");
        } else {
            return DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_SLUTTDATO, "Opphørsdato flyttet fremover");
        }
    }

    private static DetaljertResultatInfo endretStartdatoDetaljertResultat(Set<DetaljertVilkårResultat> avslåtteVilkår) {
        if (harAvslåttVilkår(avslåtteVilkår, VilkårType.UNGDOMSPROGRAMVILKÅRET)) {
            return DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_STARTDATO, "Endring av startdato fremover");
        } else {
            return DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_STARTDATO, "Endring av startdato bakover");
        }
    }

    private static DetaljertResultatInfo nyPeriodeDetaljertResultat(Set<DetaljertVilkårResultat> avslåtteVilkår, TilkjentYtelseVerdi tilkjentYtelse) {
        if (tilkjentYtelse == null) {
            if (!avslåtteVilkår.isEmpty()) {
                return DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_INNGANGSVILKÅR, "Avslått inngangsvilkår for ny periode");
            }

            return DetaljertResultatInfo.of(DetaljertResultatType.INNVILGELSE_KUN_VILKÅR);
        }

        return DetaljertResultatInfo.of(DetaljertResultatType.INNVILGELSE_UTBETALING);
    }

    private static DetaljertResultatInfo kontrollerInntektDetaljertResultat(TilkjentYtelseVerdi tilkjentYtelse) {
        if (tilkjentYtelse == null) {
            // Usikker om dette er mulig
            return DetaljertResultatInfo.of(DetaljertResultatType.KONTROLLER_INNTEKT_UTEN_TILKJENT_YTELSE);
        }
        if (tilkjentYtelse.utbetalingsgrad() >= 100) {
            return DetaljertResultatInfo.of(DetaljertResultatType.KONTROLLER_INNTEKT_FULL_UTBETALING);
        }

        if (tilkjentYtelse.utbetalingsgrad() <= 0) {
            return DetaljertResultatInfo.of(DetaljertResultatType.KONTROLLER_INNTEKT_INGEN_UTBETALING);
        }

        return DetaljertResultatInfo.of(DetaljertResultatType.KONTROLLER_INNTEKT_REDUKSJON);
    }

    private static boolean harAvslåttVilkår(Set<DetaljertVilkårResultat> avslåtteVilkår, VilkårType vilkårType) {
        return avslåtteVilkår.stream().anyMatch
            (it -> it.vilkårType() == vilkårType);
    }
}
