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
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BistandsvilkårResultatPeriode;
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

    public boolean harLagretVurdering(Long behandlingId, VilkårType vilkårType) {
        var grunnlag = vilkårVurderingRepository.hentEksisterendeGrunnlag(behandlingId);
        if (grunnlag.isEmpty()) {
            return false;
        }
        return switch (vilkårType) {
            case BISTANDSVILKÅR -> grunnlag.get().getBistandsvilkårResultatHolder().isPresent();
            case BOSTEDSVILKÅR -> grunnlag.get().getBostedsvilkårResultatHolder().isPresent();
            case AKTIVITETSVILKÅR -> grunnlag.get().getAktivitetsvilkårResultatHolder().isPresent();
            case ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR -> grunnlag.get().getAndreLivsoppholdsytelserResultatHolder().isPresent();
            default -> throw new IllegalArgumentException("Ikke støttet vilkårtype " + vilkårType);
        };
    }

    public void settBistandsvilkårResultat(Long behandlingId, VilkårResultatBuilder resultatBuilder) {
        var grunnlag = vilkårVurderingRepository.hentEksisterendeGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Fant ikke inngangsvilkår-vurderingsgrunnlag for behandling " + behandlingId));
        var holder = grunnlag.getBistandsvilkårResultatHolder()
            .orElseThrow(() -> new IllegalStateException("Bistandsvilkår-holder mangler i grunnlag for behandling " + behandlingId));

        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.BISTANDSVILKÅR);
        for (var vurdering : holder.getVurderinger()) {
            var periode = vurdering.getPeriode();
            var utfall = vurdering.isGodkjent() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            var avslagsårsak = utfall == Utfall.IKKE_OPPFYLT ? avslagsårsak(vurdering.getIkkeOppfyltÅrsak()) : null;
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
                .medBegrunnelse(vurdering.getBegrunnelse())
                .medFritekstVurderingBrev(vurdering.getFritekstVurderingBrev())
                .medUtfallManuell(utfall)
                .medAvslagsårsak(avslagsårsak));
        }
        resultatBuilder.leggTil(vilkårBuilder);
    }

    public void settAktivitetsvilkårResultat(Long behandlingId, VilkårResultatBuilder resultatBuilder) {
        var grunnlag = vilkårVurderingRepository.hentEksisterendeGrunnlag(behandlingId)
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
                ? avslagsårsak(vurdering.getIkkeOppfyltÅrsak())
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

    public void settAndreLivsoppholdsytelserResultat(Long behandlingId, VilkårResultatBuilder resultatBuilder) {
        var grunnlag = vilkårVurderingRepository.hentEksisterendeGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Fant ikke inngangsvilkår-vurderingsgrunnlag for behandling " + behandlingId));
        var holder = grunnlag.getAndreLivsoppholdsytelserResultatHolder()
            .orElseThrow(() -> new IllegalStateException("Andre livsoppholdsytelser-holder mangler i grunnlag for behandling " + behandlingId));

        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR);
        for (var vurdering : holder.getVurderinger()) {
            var periode = vurdering.getPeriode();
            var utfall = vurdering.isGodkjent() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            var avslagsårsak = utfall == Utfall.IKKE_OPPFYLT ? avslagsårsak(vurdering.getIkkeOppfyltÅrsak()) : null;
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
                .medBegrunnelse(vurdering.getBegrunnelse())
                .medFritekstVurderingBrev(vurdering.getFritekstVurderingBrev())
                .medUtfallManuell(utfall)
                .medAvslagsårsak(avslagsårsak));
        }
        resultatBuilder.leggTil(vilkårBuilder);
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

        var originalGrunnlag = vilkårVurderingRepository.hentEksisterendeGrunnlag(originalBehandlingId)
            .orElseThrow(() -> new IllegalStateException("Fant ikke vilkårvurdering på originalbehandling ved gjenoppretting" + originalBehandlingId));

        switch (vilkårType) {
            case BOSTEDSVILKÅR -> {
                var tidligereVurderingerSomSkalGjenopprettes = originalGrunnlag.hentBostedTidslinje().intersection(perioderSomSkalGjenopprettes)
                    .segmenter().stream()
                    .map(it -> new BostedsvilkårResultatPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()), it.getValue()))
                    .toList();
                vilkårVurderingRepository.lagreBostedVurderinger(behandlingId, tidligereVurderingerSomSkalGjenopprettes);
            }
            case BISTANDSVILKÅR -> {
                var tidligereVurderingerSomSkalGjenopprettes = originalGrunnlag.hentBistandsvilkårResultatTidslinje().intersection(perioderSomSkalGjenopprettes)
                    .segmenter().stream()
                    .map(it -> new BistandsvilkårResultatPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()), it.getValue()))
                    .toList();
                vilkårVurderingRepository.lagreBistandsVurderinger(behandlingId, tidligereVurderingerSomSkalGjenopprettes);
            }
            default -> throw new IllegalArgumentException("Gjenoppretting av forrige vurdering er ikke støttet for vilkårtype " + vilkårType);
        }
    }

    private static LocalDateTimeline<Boolean> hentPerioderSomSkalGjenopprettes(VilkårResultatBuilder vilkårResultatBuilder, VilkårType vilkårType, LocalDateTimeline<Boolean> avgrensningstidslinje) {
        var vilkårsperioderIkkeVurdert = new LocalDateTimeline<>(vilkårResultatBuilder.hentBuilderFor(vilkårType).build().getPerioder().stream()
            .filter(periode -> periode.getUtfall() == Utfall.IKKE_VURDERT)
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), true))
            .toList());

        return vilkårsperioderIkkeVurdert.intersection(avgrensningstidslinje);
    }

    public void settBostedsvilkårResultat(Long behandlingId, VilkårResultatBuilder resultatBuilder) {
        var grunnlag = vilkårVurderingRepository.hentEksisterendeGrunnlag(behandlingId)
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
                ? avslagsårsak(vurdering.getIkkeOppfyltÅrsak())
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

    private static Avslagsårsak avslagsårsak(IkkeOppfyltDetaljertÅrsak årsak) {
        Objects.requireNonNull(årsak, "avslagsårsak må være satt ved avslag");
        return årsak.avslagsårsak()
            .orElseThrow(() -> new IllegalStateException(årsak + " har ingen definert avslagsårsak, og kan derfor ikke føre til avslag"));
    }
}
