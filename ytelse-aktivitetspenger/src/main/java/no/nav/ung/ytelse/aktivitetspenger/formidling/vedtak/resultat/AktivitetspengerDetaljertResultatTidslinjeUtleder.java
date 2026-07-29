package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak.resultat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatFelles;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinjeUtleder;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;

import java.util.Set;

@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
@ApplicationScoped
public class AktivitetspengerDetaljertResultatTidslinjeUtleder implements DetaljertResultatTidslinjeUtleder {

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

        // Kun perioder til vurdering (intersection) inngår i tidslinjen. Strategiene utleder selv sitt resultat fra
        // behandlingsårsak/vilkår/tilkjent ytelse, slik at utlederen ikke må oppdateres for hver nye behandlingsårsak.
        var vilkårOgBehandlingsårsakerTidslinje = perioderTilVurdering
            .intersection(samletVilkårTidslinje,
                (p, behandlingÅrsaker, vilkårResultater)
                    -> new LocalDateSegment<>(p, new AktivitetspengerDetaljertResultatGrunnlag(vilkårResultater.getValue(), behandlingÅrsaker.getValue())));

        return vilkårOgBehandlingsårsakerTidslinje
            .combine(tilkjentYtelseTidslinje, byggDetaljertResultatCombinator(), JoinStyle.LEFT_JOIN)
            .compress();
    }

    private LocalDateSegmentCombinator<AktivitetspengerDetaljertResultatGrunnlag, TilkjentYtelseVerdi, DetaljertResultat> byggDetaljertResultatCombinator() {
        return (p, lhs, rhs) -> {
            var grunnlag = lhs.getValue();
            if (grunnlag == null) {
                throw new IllegalStateException("Ingen vilkårsresultat for periode %s".formatted(p));
            }
            var tilkjentYtelse = rhs != null ? rhs.getValue() : null;
            var resultat = new DetaljertResultat(
                Set.of(),
                grunnlag.behandlingÅrsaker(),
                grunnlag.avslåtteVilkår(),
                grunnlag.ikkeVurderteVilkår(),
                tilkjentYtelse,
                true);
            return new LocalDateSegment<>(p, resultat);
        };
    }
}
