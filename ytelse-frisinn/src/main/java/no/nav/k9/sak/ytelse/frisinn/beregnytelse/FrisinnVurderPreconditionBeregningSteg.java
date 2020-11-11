package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagSteg;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.vedtak.konfig.KonfigVerdi;

/**
 * Dummysteg for FRISINN.
 *
 * Eksisterer for riktig opprydding av vilkår og beregningsgrunnlag ved kjøring av TilbakeTilStartBeregningTask eller tilbakehopp grunnet re-bekreftelse av 8004.
 *
 * Steget kjører rydding av vilkår til resultat fra originalbehandling ved revurdering.
 *
 */
@FagsakYtelseTypeRef("FRISINN")
@BehandlingStegRef(kode = "PRECONDITION_BERGRUNN")
@BehandlingTypeRef
@ApplicationScoped
public class FrisinnVurderPreconditionBeregningSteg implements BeregningsgrunnlagSteg {

    private BeregningTjeneste kalkulusTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste vilkårTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;
    private boolean toggletVilkårsperioder;

    public FrisinnVurderPreconditionBeregningSteg() {
        // CDI
    }

    @Inject
    public FrisinnVurderPreconditionBeregningSteg(BeregningTjeneste kalkulusTjeneste,
                                                  BehandlingRepository behandlingRepository,
                                                  BeregningsgrunnlagVilkårTjeneste vilkårTjeneste,
                                                  VilkårResultatRepository vilkårResultatRepository,
                                                  @FagsakYtelseTypeRef("FRISINN") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                  @KonfigVerdi(value = "FRISINN_VILKARSPERIODER", defaultVerdi = "false") Boolean toggletVilkårsperioder) {
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.toggletVilkårsperioder = toggletVilkårsperioder;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!toggletVilkårsperioder) {
            return;
        }
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (behandling.erRevurdering()) {
            var originalBehandling = behandlingRepository.hentBehandling(behandling.getOriginalBehandlingId().get());
            ryddVilkårForOverstyrtRevurdering(kontekst, behandling, originalBehandling);
        }
    }

    /**
     * Rydder beregningsgrunnlagvilkår for revurdering.
     *
     * Setter vilkårsresultatet tilbake til resultatet fra originalbehandling for perioder som er endret i overstyring.
     *  @param kontekst Behandlingskontrollkontekst
     * @param behandling Aktiv behandling
     * @param originalBehandling Original behandling
     */
    private void ryddVilkårForOverstyrtRevurdering(BehandlingskontrollKontekst kontekst, Behandling behandling, Behandling originalBehandling) {
        Optional<Vilkår> origBeregningsvilkår = finnOriginaltBeregningsgrunnlagVilkår(originalBehandling);

        if (origBeregningsvilkår.isPresent()) {
            var vilkårResultat = vilkårResultatRepository.hent(behandling.getId());
            if (!harBeregninsgrunnlagVilkår(vilkårResultat)) {
                return;
            }
            VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårResultat);
            var origVilkårsperioder = origBeregningsvilkår.get().getPerioder();
            BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
            vilkårTjeneste.utledPerioderTilVurdering(ref, false)
                .forEach(periode -> tilbakestillUtfallTilOriginal(periode, builder, origVilkårsperioder));
            kalkulusTjeneste.gjenopprettInitiell(ref);
            vilkårResultatRepository.lagre(kontekst.getBehandlingId(), builder.build());
        }
    }

    private VilkårBuilder finnBuilderForBeregningsgrunnlagVilkår(VilkårResultatBuilder builder) {
        return builder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .medKantIKantVurderer(vilkårsPerioderTilVurderingTjeneste.getKantIKantVurderer());
    }

    private void tilbakestillUtfallTilOriginal(DatoIntervallEntitet periode, VilkårResultatBuilder builder, List<VilkårPeriode> origVilkårsperioder) {
        var vilkårBuilder = finnBuilderForBeregningsgrunnlagVilkår(builder);
        var origUtfall = finnOriginaltUtfallForPeriode(origVilkårsperioder, periode);
        settUtfallForPeriode(periode, builder, vilkårBuilder, origUtfall);
    }

    private void settUtfallForPeriode(DatoIntervallEntitet periode, VilkårResultatBuilder builder, VilkårBuilder vilkårBuilder, Utfall origUtfall) {
        var vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(periode);
        vilkårBuilder.leggTil(vilkårPeriodeBuilder.medUtfall(origUtfall));
        builder.leggTil(vilkårBuilder);
    }

    private Utfall finnOriginaltUtfallForPeriode(List<VilkårPeriode> origVilkårsperioder, DatoIntervallEntitet periode) {
        var overlappendePerioder = origVilkårsperioder.stream()
            .filter(p -> p.getPeriode().overlapper(periode)).collect(Collectors.toList());
        if (overlappendePerioder.size() > 1) {
            throw new IllegalStateException("Skal ikke kunne ha flere overlappende originalperioder ved tilbakestilling av revurdering");
        }
        if (overlappendePerioder.isEmpty()) {
            throw new IllegalStateException("Finner ikke originalperiode ved tilbakestilling av revurdering");
        }
        return overlappendePerioder.get(0).getGjeldendeUtfall();
    }

    private boolean harBeregninsgrunnlagVilkår(Vilkårene vilkårResultat) {
        var beregningsvilkåret = vilkårResultat.getVilkårene().stream()
            .filter(vilkår -> vilkår.getVilkårType().equals(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .findFirst();
        return beregningsvilkåret.isPresent();
    }

    private Optional<Vilkår> finnOriginaltBeregningsgrunnlagVilkår(Behandling originalBehandling) {
        var origVilkår = vilkårResultatRepository.hentHvisEksisterer(originalBehandling.getId());
        return origVilkår.stream()
            .flatMap(it -> it.getVilkårene().stream()
                .filter(v -> v.getVilkårType().equals(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)))
            .findFirst();
    }

}
