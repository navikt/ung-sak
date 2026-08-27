package no.nav.ung.ytelse.aktivitetspenger.del1;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

/**
 * Leser saksbehandlers lagrede vurderinger fra {@link AktivitetspengerInngangsvilkårResultatGrunnlag}
 * og setter tilsvarende vilkårsresultat.
 */
@ApplicationScoped
public class InngangsvilkårVurderingTjeneste {

    private InngangsvilkårVurderingRepository vilkårVurderingRepository;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    InngangsvilkårVurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårVurderingTjeneste(InngangsvilkårVurderingRepository vilkårVurderingRepository,
                                           BehandlingRepository behandlingRepository,
                                           VilkårResultatRepository vilkårResultatRepository) {
        this.vilkårVurderingRepository = vilkårVurderingRepository;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public void settBistandsvilkårResultat(Long behandlingId, VilkårResultatBuilder resultatBuilder) {
        var grunnlag = vilkårVurderingRepository.hentGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Fant ikke inngangsvilkår-vurderingsgrunnlag for behandling " + behandlingId));
        var holder = grunnlag.getBistandsvilkårResultatHolder()
            .orElseThrow(() -> new IllegalStateException("Bistandsvilkår-holder mangler i grunnlag for behandling " + behandlingId));

        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.BISTANDSVILKÅR);
        for (var vurdering : holder.getVurderinger()) {
            var periode = vurdering.getPeriode();
            var utfall = vurdering.isGodkjent() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            var avslagsårsak = utfall == Utfall.IKKE_OPPFYLT ? mapBistandsvilkårAvslagsårsk(vurdering.getIkkeOppfyltÅrsak()) : null;
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
                .medBegrunnelse(vurdering.getBegrunnelse())
                .medFritekstVurderingBrev(vurdering.getFritekstVurderingBrev())
                .medUtfallManuell(utfall)
                .medAvslagsårsak(avslagsårsak));
        }
        resultatBuilder.leggTil(vilkårBuilder);
    }

    private Avslagsårsak mapBistandsvilkårAvslagsårsk(BistandsvilkårIkkeOppfyltÅrsak årsak) {
        Objects.requireNonNull(årsak, "avslagsårsak må være satt ved avslag");
        return switch (årsak) {
            case IKKE_14A_VEDTAK -> Avslagsårsak.IKKE_14A_VEDTAK;
            case AVKORTET -> Avslagsårsak.AVKORTET;
            case UDEFINERT -> throw new IllegalStateException("UDEFINERT avslagsårsak ikke tillatt ved avslag");
        };
    }

    public void settAktivitetsvilkårResultat(Long behandlingId, VilkårResultatBuilder resultatBuilder) {
        var grunnlag = vilkårVurderingRepository.hentGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Fant ikke inngangsvilkår-vurderingsgrunnlag for behandling " + behandlingId));
        var vurderinger = grunnlag.hentAktivitetsvilkårResultatPerioder();
        byggAktivitetVilkårIBuilder(resultatBuilder, vurderinger, VilkårType.AKTIVITETSVILKÅR);
    }

    private void byggAktivitetVilkårIBuilder(VilkårResultatBuilder resultatBuilder, List<AktivitetsvilkårResultatPeriode> vurderinger, VilkårType vilkårType) {
        var vilkårBuilder = resultatBuilder.hentBuilderFor(vilkårType);
        for (var vurdering : vurderinger) {
            var periode = vurdering.getPeriode();
            var utfall = vurdering.isGodkjent() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            var avslagsårsak = utfall == Utfall.IKKE_OPPFYLT
                ? mapAktivitetsvilkårAvslagsårsak(vurdering.getIkkeOppfyltÅrsak())
                : null;

            var vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
                .medBegrunnelse(vurdering.getBegrunnelse())
                .medFritekstVurderingBrev(vurdering.getFritekstVurderingBrev())
                .medAvslagsårsak(avslagsårsak);

            if (vurdering.erManuellVurdering()) {
                vilkårPeriodeBuilder.medUtfallManuell(utfall);
            } else {
                vilkårPeriodeBuilder.tilbakestillManuellVurdering().medUtfall(utfall);
            }
            vilkårBuilder.leggTil(vilkårPeriodeBuilder);
        }
        resultatBuilder.leggTil(vilkårBuilder);
    }

    private Avslagsårsak mapAktivitetsvilkårAvslagsårsak(AktivitetsvilkåretIkkeOppfyltÅrsak årsak) {
        Objects.requireNonNull(årsak, "avslagsårsak må være satt ved avslag");
        return switch (årsak) {
            case ANNET -> Avslagsårsak.AKTIVITETSVILKÅR_GENERELL_AVSLAGSÅRSAK;
            case AVKORTET -> Avslagsårsak.AVKORTET;
            case UDEFINERT -> throw new IllegalStateException("UDEFINERT avslagsårsak ikke tillatt ved avslag");
        };
    }

    public void settAndreLivsoppholdsytelserResultat(Long behandlingId, VilkårResultatBuilder resultatBuilder) {
        var grunnlag = vilkårVurderingRepository.hentGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Fant ikke inngangsvilkår-vurderingsgrunnlag for behandling " + behandlingId));
        var holder = grunnlag.getAndreLivsoppholdsytelserResultatHolder()
            .orElseThrow(() -> new IllegalStateException("Andre livsoppholdsytelser-holder mangler i grunnlag for behandling " + behandlingId));

        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR);
        for (var vurdering : holder.getVurderinger()) {
            var periode = vurdering.getPeriode();
            var utfall = vurdering.isGodkjent() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            var avslagsårsak = utfall == Utfall.IKKE_OPPFYLT ? mapAndreLivsoppholdsytelseAvslagsårsak(vurdering.getIkkeOppfyltÅrsak()) : null;
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
                .medBegrunnelse(vurdering.getBegrunnelse())
                .medFritekstVurderingBrev(vurdering.getFritekstVurderingBrev())
                .medUtfallManuell(utfall)
                .medAvslagsårsak(avslagsårsak));
        }
        resultatBuilder.leggTil(vilkårBuilder);
    }

    private Avslagsårsak mapAndreLivsoppholdsytelseAvslagsårsak(AndreLivsoppholdsytelserIkkeOppfyltÅrsak årsak) {
        Objects.requireNonNull(årsak, "avslagsårsak må være satt ved avslag");
        return switch (årsak) {
            case HAR_ANNEN_LIVSOPPHOLDSYTELSE -> Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE;
            case AVKORTET -> Avslagsårsak.AVKORTET;
            case UDEFINERT -> throw new IllegalStateException("UDEFINERT avslagsårsak ikke tillatt ved avslag");
        };
    }

    public void oppdaterBostedsvilkårResultatFraVurdering(Long behandlingId) {
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);
        settBostedsvilkårResultat(behandlingId, resultatBuilder);
        vilkårResultatRepository.lagre(behandlingId, resultatBuilder.build());
    }

    public void oppdaterBostedsvilkårResultatFraVurdering(Long behandlingId, VilkårResultatBuilder resultatBuilder) {
        settBostedsvilkårResultat(behandlingId, resultatBuilder);
    }

    public void settVilkårResultatIkkeVurdertForPeriode(Long behandlingId, VilkårType vilkårType, SequencedCollection<DatoIntervallEntitet> perioder) {
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);
        settVilkårResultatIkkeVurdertForPeriode(vilkårResultatBuilder, vilkårType, perioder);
        vilkårResultatRepository.lagre(behandlingId, vilkårResultatBuilder.build());
    }

    public void settVilkårResultatIkkeVurdertForPeriode(VilkårResultatBuilder vilkårResultatBuilder, VilkårType vilkårType, SequencedCollection<DatoIntervallEntitet> perioder) {
        var resultatBuilderForVilkår = vilkårResultatBuilder.hentBuilderFor(vilkårType);
        perioder.forEach(periode -> {
            var periodeBuilder = resultatBuilderForVilkår.hentBuilderFor(periode).medUtfall(Utfall.IKKE_VURDERT);
            resultatBuilderForVilkår.leggTil(periodeBuilder);
        });
        vilkårResultatBuilder.leggTil(resultatBuilderForVilkår);
    }

    public void gjenopprettForrigeVurderingForPerioderIkkeVurdert(Long behandlingId, VilkårResultatBuilder vilkårResultatBuilder, VilkårType vilkårType, LocalDateTimeline<Boolean> avgrensningstidslinje) {
        var originalBehandlingId = behandlingRepository.hentBehandling(behandlingId).getOriginalBehandlingId().orElse(null);
        if (originalBehandlingId == null) {
            return;
        }
        var perioderSomSkalGjenopprettes = hentPerioderSomSkalGjenopprettes(vilkårResultatBuilder, vilkårType, avgrensningstidslinje);
        if (perioderSomSkalGjenopprettes.isEmpty()) {
            return;
        }

        var tidligereVurderTidslinje = vilkårVurderingRepository.hentGrunnlag(originalBehandlingId)
            .map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBostedTidslinje)
            .orElseThrow(() -> new IllegalStateException("Fant ikke vilkårvurdering på originalbehandling ved gjenoppretting" + originalBehandlingId));

        var tidligereVurderingerSomSkalGjenopprettes = tidligereVurderTidslinje.intersection(perioderSomSkalGjenopprettes)
            .segmenter().stream()
            .map(it ->
                new BostedsvilkårResultatPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()), it.getValue())
            ).toList();

        vilkårVurderingRepository.lagreBostedVurderinger(behandlingId, tidligereVurderingerSomSkalGjenopprettes);
    }

    private static LocalDateTimeline<Boolean> hentPerioderSomSkalGjenopprettes(VilkårResultatBuilder vilkårResultatBuilder, VilkårType vilkårType, LocalDateTimeline<Boolean> avgrensningstidslinje) {
        var vilkårsperioderIkkeVurdert = new LocalDateTimeline<>(vilkårResultatBuilder.hentBuilderFor(vilkårType).build().getPerioder().stream()
            .filter(periode -> periode.getUtfall() == Utfall.IKKE_VURDERT)
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), true))
            .toList());

        return vilkårsperioderIkkeVurdert.intersection(avgrensningstidslinje);
    }

    public void settBostedsvilkårResultat(Long behandlingId, VilkårResultatBuilder resultatBuilder) {
        var grunnlag = vilkårVurderingRepository.hentGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Fant ikke inngangsvilkår-vurderingsgrunnlag for behandling " + behandlingId));
        var vurderinger = grunnlag.hentBostedsvilkårResultatPerioder();
        byggVilkårIBuilder(resultatBuilder, vurderinger, VilkårType.BOSTEDSVILKÅR);
    }

    private void byggVilkårIBuilder(VilkårResultatBuilder resultatBuilder, List<BostedsvilkårResultatPeriode> vurderinger, VilkårType vilkårType) {
        var vilkårBuilder = resultatBuilder.hentBuilderFor(vilkårType);
        for (var vurdering : vurderinger) {
            var periode = vurdering.getPeriode();
            var utfall = vurdering.isGodkjent() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            var avslagsårsak = utfall == Utfall.IKKE_OPPFYLT
                ? mapBostedsvilkårÅrsak(vurdering.getIkkeOppfyltÅrsak())
                : null;

            var vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
                .medBegrunnelse(vurdering.getBegrunnelse())
                .medFritekstVurderingBrev(vurdering.getFritekstVurderingBrev())
                .medAvslagsårsak(avslagsårsak);

            if (vurdering.erManuellVurdering()) {
                vilkårPeriodeBuilder.medUtfallManuell(utfall);
            } else {
                vilkårPeriodeBuilder.tilbakestillManuellVurdering().medUtfall(utfall);
            }
            vilkårBuilder.leggTil(vilkårPeriodeBuilder);
        }
        resultatBuilder.leggTil(vilkårBuilder);
    }

    public static Avslagsårsak mapBostedsvilkårÅrsak(BostedsvilkårIkkeOppfyltÅrsak årsak) {
        Objects.requireNonNull(årsak, "avslagsårsak må være satt ved avslag");
        return switch (årsak) {
            case IKKE_BOSATTADRESSE_I_TRONDHEIM,
                 IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM,
                 STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM,
                 ANNET -> Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE;
            case AVKORTET -> Avslagsårsak.AVKORTET;
            case UDEFINERT -> throw new IllegalStateException("UDEFINERT avslagsårsak ikke tillatt");
        };
    }
}
