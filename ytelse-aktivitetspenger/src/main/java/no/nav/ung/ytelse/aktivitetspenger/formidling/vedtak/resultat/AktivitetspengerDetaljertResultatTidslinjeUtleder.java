package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak.resultat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.formidling.vedtak.resultat.*;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
@ApplicationScoped
public class AktivitetspengerDetaljertResultatTidslinjeUtleder implements DetaljertResultatTidslinjeUtleder {


    private static final Map<BehandlingÅrsakType, DetaljertResultatInfo> ÅRSAK_RESULTAT_INNVILGELSE_MAP = Map.of(
        BehandlingÅrsakType.RE_HENDELSE_DØD_BARN, DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_BARN_DØDSFALL),
        BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_ØKT_SATS),
        BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_BARN_FØDSEL),
        BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER, DetaljertResultatInfo.of(DetaljertResultatType.ENDRING_DELTAKER_DØDSFALL),
        BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO, DetaljertResultatInfo.of(DetaljertResultatType.OPPHØR_VED_MAKSDATO)
    );

    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    AktivitetspengerDetaljertResultatTidslinjeUtleder() {
    }

    @Inject
    public AktivitetspengerDetaljertResultatTidslinjeUtleder(
        @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER) ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
        TilkjentYtelseRepository tilkjentYtelseRepository,
        VilkårResultatRepository vilkårResultatRepository) {
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public LocalDateTimeline<DetaljertResultat> utledDetaljertResultat(Behandling behandling) {
        var tilkjentYtelseTidslinje = tilkjentYtelseRepository.hentTidslinje(behandling.getId()).compress();
        var kontrollertePerioderTidslinje = tilkjentYtelseRepository.hentKontrollerInntektTidslinje(behandling.getId()).compress();

        var perioderTilVurdering = DetaljertResultatFelles.utledPerioderTilVurdering(
            prosessTriggerPeriodeUtleder.utledTidslinje(behandling.getId()),
            tilkjentYtelseTidslinje,
            kontrollertePerioderTidslinje);

        var samletVilkårTidslinje = DetaljertResultatFelles.samleVilkårIEnTidslinje(vilkårResultatRepository.hentVilkårResultater(behandling.getId()));

        var vilkårOgBehandlingsårsakerTidslinje = perioderTilVurdering
            .intersection(samletVilkårTidslinje,
                (p, behandlingÅrsaker, vilkårResultater)
                    -> new LocalDateSegment<>(p, new AktivitetspengerDetaljertResultatGrunnlag(vilkårResultater.getValue(), behandlingÅrsaker.getValue(), behandling.erManueltOpprettet())));

        var detaljertResultatTidslinje = vilkårOgBehandlingsårsakerTidslinje
            .combine(tilkjentYtelseTidslinje, bestemResultatForPeriodeCombinator(), JoinStyle.LEFT_JOIN);

        return detaljertResultatTidslinje.compress();
    }

    private Set<DetaljertResultatInfo> bestemDetaljertResultat(LocalDateInterval periode, AktivitetspengerDetaljertResultatGrunnlag vilkårResultat, TilkjentYtelseVerdi tilkjentYtelse) {

        if (!vilkårResultat.ikkeVurderteVilkår().isEmpty()) {
            return Set.of(DetaljertResultatInfo.of(DetaljertResultatType.IKKE_VURDERT));
        }

        var avslåtteVilkår = vilkårResultat.avslåtteVilkår();

        var relevanteÅrsaker = vilkårResultat.behandlingÅrsaker();

        var resultater = new HashSet<DetaljertResultatInfo>();

        if (relevanteÅrsaker.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)) {
            resultater.add(DetaljertResultatFelles.kontrollerInntektDetaljertResultat(tilkjentYtelse));
        }

        if (relevanteÅrsaker.contains(BehandlingÅrsakType.NY_SØKT_PERIODE)
            || vilkårResultat.manuellOpprettetBehandling() && relevanteÅrsaker.contains(BehandlingÅrsakType.RE_SATS_ENDRING)) {
            resultater.add(DetaljertResultatFelles.nyPeriodeDetaljertResultat(avslåtteVilkår, tilkjentYtelse));
        }


        relevanteÅrsaker.stream()
            .filter(ÅRSAK_RESULTAT_INNVILGELSE_MAP::containsKey)
            .map(årsak -> DetaljertResultatFelles.behandlingsårsakDetaljertResultat(ÅRSAK_RESULTAT_INNVILGELSE_MAP, årsak, avslåtteVilkår))
            .forEach(resultater::add);


        if (resultater.isEmpty()) {
            return DetaljertResultatFelles.håndterUkjenteTilfeller(tilkjentYtelse, avslåtteVilkår, relevanteÅrsaker)
                .map(Set::of)
                .orElseThrow(() -> new IllegalStateException(
                    "Klarte ikke å utlede resultattype for periode %s og vilkårsresultat og behandlingsårsaker %s og tilkjent ytelse %s"
                        .formatted(periode, vilkårResultat, tilkjentYtelse)));
        }


        return resultater;
    }

    private LocalDateSegmentCombinator<AktivitetspengerDetaljertResultatGrunnlag, TilkjentYtelseVerdi, DetaljertResultat> bestemResultatForPeriodeCombinator() {
        return (p, lhs, rhs) -> {
            AktivitetspengerDetaljertResultatGrunnlag vilkårResultat = lhs.getValue();
            var tilkjentYtelse = rhs != null ? rhs.getValue() : null;

            if (vilkårResultat == null) {
                throw new IllegalStateException("Ingen vilkårsresultat for periode %s og tilkjentytelse %s".formatted(p, tilkjentYtelse));
            }

            var vilkårSomIkkeErVurdert = vilkårResultat.ikkeVurderteVilkår();
            var avslåtteVilkår = vilkårResultat.avslåtteVilkår();
            var behandlingsårsaker = vilkårResultat.behandlingÅrsaker();

            var resultatType = bestemDetaljertResultat(p, vilkårResultat, tilkjentYtelse);

            var resultat = new DetaljertResultat(resultatType, behandlingsårsaker, avslåtteVilkår, vilkårSomIkkeErVurdert);

            return new LocalDateSegment<>(p, resultat);
        };
    }

}
