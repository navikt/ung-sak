package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Utfall.IKKE_VURDERT;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagRegelResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Utfall;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
class BeregningsgrunnlagVilkårTjeneste {


    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    protected BeregningsgrunnlagVilkårTjeneste() {
        // CDI Proxy
    }

    @Inject
    public BeregningsgrunnlagVilkårTjeneste(BehandlingRepository behandlingRepository,
                                            BehandlingsresultatRepository behandlingsresultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    void lagreVilkårresultat(BehandlingskontrollKontekst kontekst, BeregningsgrunnlagRegelResultat beregningsgrunnlagResultat) {
        boolean vilkårOppfylt = beregningsgrunnlagResultat.getVilkårOppfylt();
        final var beregningsgrunnlagPeriode = beregningsgrunnlagResultat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0);
        String regelEvaluering = beregningsgrunnlagPeriode.getRegelEvalueringVilkårvurdering();
        String regelInput = beregningsgrunnlagPeriode.getRegelInputVilkårvurdering();
        final var behandlingsresultat = getBehandlingsresultat(kontekst.getBehandlingId());
        VilkårResultatBuilder vilkårResultatBuilder = opprettVilkårsResultat(regelEvaluering, regelInput, vilkårOppfylt, behandlingsresultat);
        if (!vilkårOppfylt) {
            Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            behandlingsresultat.setAvslagsårsak(Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG);
            behandlingsresultatRepository.lagre(kontekst.getBehandlingId(), behandlingsresultat);
        }
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        behandlingsresultat.medOppdatertVilkårResultat(vilkårResultatBuilder.build());
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), kontekst.getSkriveLås());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hent(behandlingId);
    }

    private VilkårResultatBuilder opprettVilkårsResultat(String regelEvaluering, String regelInput, boolean oppfylt, Behandlingsresultat behandlingsresultat) {
        VilkårResultat vilkårResultat = behandlingsresultat.getVilkårResultat();
        VilkårResultatBuilder builder = VilkårResultat.builderFraEksisterende(vilkårResultat);
        final var vilkårBuilder = builder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        vilkårBuilder.leggTil(vilkårBuilder
            .hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)
            .medUtfall(oppfylt ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT)
            .medMerknad(oppfylt ? VilkårUtfallMerknad.UDEFINERT : VilkårUtfallMerknad.VM_1041)
            .medAvslagsårsak(oppfylt ? null : Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG)
            .medRegelInput(regelInput)
            .medRegelEvaluering(regelEvaluering));
        builder.leggTil(vilkårBuilder);
        return builder;
    }

    void ryddVedtaksresultatOgVilkår(BehandlingskontrollKontekst kontekst) {
        Optional<Behandlingsresultat> behandlingresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(kontekst.getBehandlingId());
        ryddOppVilkårsvurdering(kontekst, behandlingresultatOpt);
        nullstillVedtaksresultat(kontekst, behandlingresultatOpt);
    }

    private void ryddOppVilkårsvurdering(BehandlingskontrollKontekst kontekst, Optional<Behandlingsresultat> behandlingresultatOpt) {
        Optional<VilkårResultat> vilkårResultatOpt = behandlingresultatOpt
            .map(Behandlingsresultat::getVilkårResultat);
        if (vilkårResultatOpt.isEmpty()) {
            return;
        }
        VilkårResultat vilkårResultat = vilkårResultatOpt.get();
        Optional<Vilkår> beregningsvilkåret = vilkårResultat.getVilkårene().stream()
            .filter(vilkår -> vilkår.getVilkårType().equals(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .findFirst();
        if (beregningsvilkåret.isEmpty()) {
            return;
        }
        final var behandlingsresultat = behandlingresultatOpt.get();
        VilkårResultatBuilder builder = VilkårResultat.builderFraEksisterende(vilkårResultat);
        final var vilkårBuilder = builder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        final var vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE);// FIXME (k9) hent ut for perioden(e) som skal evalueres
        vilkårBuilder.leggTil(vilkårPeriodeBuilder.medUtfall(IKKE_VURDERT));
        final var nyttResultat = builder.build();
        behandlingsresultat.medOppdatertVilkårResultat(nyttResultat);
        behandlingsresultatRepository.lagre(kontekst.getBehandlingId(), behandlingsresultat);
        behandlingRepository.lagre(nyttResultat, kontekst.getSkriveLås());
    }

    private void nullstillVedtaksresultat(BehandlingskontrollKontekst kontekst, Optional<Behandlingsresultat> behandlingresultatOpt) {
        if (behandlingresultatOpt.isEmpty() || Objects.equals(behandlingresultatOpt.get().getBehandlingResultatType(), BehandlingResultatType.IKKE_FASTSATT)) {
            return;
        }
        Behandlingsresultat.Builder builder = Behandlingsresultat.builderEndreEksisterende(behandlingresultatOpt.get())
            .medBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT);
        behandlingsresultatRepository.lagre(kontekst.getBehandlingId(), builder.build());
    }

}
