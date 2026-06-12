package no.nav.ung.ytelse.aktivitetspenger.del1;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.inngangsvilkår.InngangsvilkårTjeneste;

/**
 * Leser saksbehandlers lagrede vurderinger fra {@link AktivitetspengerInngangsvilkårResultatGrunnlag}
 * og setter tilsvarende vilkårsresultat.
 */
@ApplicationScoped
public class InngangsvilkårVurderingTjeneste {

    private InngangsvilkårVurderingRepository repository;

    InngangsvilkårVurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårVurderingTjeneste(InngangsvilkårVurderingRepository repository) {
        this.repository = repository;
    }

    public void settBistandsvilkårResultat(Long behandlingId, VilkårResultatBuilder resultatBuilder) {
        var grunnlag = repository.hentGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Fant ikke inngangsvilkår-vurderingsgrunnlag for behandling " + behandlingId));
        var holder = grunnlag.getBistandsvilkårResultatHolder()
            .orElseThrow(() -> new IllegalStateException("Bistandsvilkår-holder mangler i grunnlag for behandling " + behandlingId));

        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.BISTANDSVILKÅR);
        for (var vurdering : holder.getVurderinger()) {
            var periode = vurdering.getPeriode();
            var utfall = vurdering.isGodkjent() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            var avslagsårsak = utfall == Utfall.IKKE_OPPFYLT ? Avslagsårsak.IKKE_14A_VEDTAK : null;
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
                .medBegrunnelse(vurdering.getBegrunnelse())
                .medFritekstVurderingBrev(vurdering.getFritekstVurderingBrev())
                .medUtfallManuell(utfall)
                .medAvslagsårsak(avslagsårsak));
        }
        resultatBuilder.leggTil(vilkårBuilder);
    }

    public void settAndreLivsoppholdsytelserResultat(Long behandlingId, VilkårResultatBuilder resultatBuilder) {
        var grunnlag = repository.hentGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Fant ikke inngangsvilkår-vurderingsgrunnlag for behandling " + behandlingId));
        var holder = grunnlag.getAndreLivsoppholdsytelserResultatHolder()
            .orElseThrow(() -> new IllegalStateException("Andre livsoppholdsytelser-holder mangler i grunnlag for behandling " + behandlingId));

        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR);
        for (var vurdering : holder.getVurderinger()) {
            var periode = vurdering.getPeriode();
            var utfall = vurdering.isGodkjent() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            var avslagsårsak = utfall == Utfall.IKKE_OPPFYLT ? Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE : null;
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
                .medBegrunnelse(vurdering.getBegrunnelse())
                .medFritekstVurderingBrev(vurdering.getFritekstVurderingBrev())
                .medUtfallManuell(utfall)
                .medAvslagsårsak(avslagsårsak));
        }
        resultatBuilder.leggTil(vilkårBuilder);
    }

    public void settBostedsvilkårResultat(Long behandlingId, VilkårResultatBuilder resultatBuilder) {
        var grunnlag = repository.hentGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Fant ikke inngangsvilkår-vurderingsgrunnlag for behandling " + behandlingId));
        var holder = grunnlag.getBostedsvilkårResultatHolder()
            .orElseThrow(() -> new IllegalStateException("Bostedsvilkår-holder mangler i grunnlag for behandling " + behandlingId));

        var vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.BOSTEDSVILKÅR);
        for (var vurdering : holder.getVurderinger()) {
            var periode = vurdering.getPeriode();
            var utfall = vurdering.isGodkjent() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            var avslagsårsak = utfall == Utfall.IKKE_OPPFYLT
                ? mapBostedsvilkårÅrsak(vurdering.getIkkeOppfyltÅrsak())
                : null;
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
                .medBegrunnelse(vurdering.getBegrunnelse())
                .medFritekstVurderingBrev(vurdering.getFritekstVurderingBrev())
                .medUtfallManuell(utfall)
                .medAvslagsårsak(avslagsårsak));
        }
        resultatBuilder.leggTil(vilkårBuilder);
    }

    public static Avslagsårsak mapBostedsvilkårÅrsak(BostedsvilkårIkkeOppfyltÅrsak årsak) {
        if (årsak == null || årsak == BostedsvilkårIkkeOppfyltÅrsak.UDEFINERT) {
            return Avslagsårsak.UDEFINERT;
        }
        return switch (årsak) {
            case YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE ->
                Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE;
            default -> Avslagsårsak.UDEFINERT;
        };
    }

}
