package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.vilkår.Utfall.IKKE_VURDERT;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
class BeregningsgrunnlagVilkårTjeneste {

    protected BehandlingRepository behandlingRepository;
    private VedtakVarselRepository behandlingsresultatRepository;
    protected VilkårResultatRepository vilkårResultatRepository;

    protected BeregningsgrunnlagVilkårTjeneste() {
        // CDI Proxy
    }

    @Inject
    public BeregningsgrunnlagVilkårTjeneste(BehandlingRepository behandlingRepository,
                                            VedtakVarselRepository behandlingsresultatRepository,
                                            VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    void lagreAvslåttVilkårresultat(BehandlingskontrollKontekst kontekst,
                             DatoIntervallEntitet vilkårsPeriode,
                             DatoIntervallEntitet orginalVilkårsPeriode,
                                    Avslagsårsak avslagsårsak) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        VilkårResultatBuilder vilkårResultatBuilder = opprettAvslåttVilkårsResultat(
            vilkårene,
            vilkårsPeriode,
            orginalVilkårsPeriode,
            avslagsårsak);
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårResultatBuilder.build());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    void lagreVilkårresultat(BehandlingskontrollKontekst kontekst,
                             boolean vilkårOppfylt,
                             DatoIntervallEntitet vilkårsPeriode,
                             DatoIntervallEntitet orginalVilkårsPeriode) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        VilkårResultatBuilder vilkårResultatBuilder = opprettVilkårsResultat(vilkårOppfylt, vilkårene, vilkårsPeriode, orginalVilkårsPeriode);
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (!vilkårOppfylt) {
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        }
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårResultatBuilder.build());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }


    void lagreVilkårresultatSkalBehandlesIInfotrygd(BehandlingskontrollKontekst kontekst) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        vilkårBuilder.leggTil(vilkårBuilder
            .hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)
            .medUtfall(Utfall.IKKE_OPPFYLT)
            //FIXME (k9) bestem riktig avslagsårsak og utfall
            .medMerknad(VilkårUtfallMerknad.VM_1041)
            .medAvslagsårsak(Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG));
        builder.leggTil(vilkårBuilder);

        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), builder.build());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }


    private VilkårResultatBuilder opprettAvslåttVilkårsResultat(Vilkårene vilkårene,
                                                                DatoIntervallEntitet vilkårsPeriode,
                                                                DatoIntervallEntitet orginalVilkårsPeriode,
                                                                Avslagsårsak avslagsårsak) {
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        if (!vilkårsPeriode.equals(orginalVilkårsPeriode)) {
            vilkårBuilder.tilbakestill(orginalVilkårsPeriode);
        }
        finnVilkårUtfallMerknad(avslagsårsak);
        vilkårBuilder
            .leggTil(vilkårBuilder
                .hentBuilderFor(vilkårsPeriode)
                .medUtfall(Utfall.IKKE_OPPFYLT)
                .medMerknad(finnVilkårUtfallMerknad(avslagsårsak))
                .medAvslagsårsak(avslagsårsak));
        builder.leggTil(vilkårBuilder);
        return builder;
    }

    private VilkårUtfallMerknad finnVilkårUtfallMerknad(Avslagsårsak avslagsårsak) {
        return VilkårUtfallMerknad.fraKode(avslagsårsak.getKode());
    }


    private VilkårResultatBuilder opprettVilkårsResultat(boolean oppfylt, Vilkårene vilkårene, DatoIntervallEntitet vilkårsPeriode, DatoIntervallEntitet orginalVilkårsPeriode) {
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        if (!vilkårsPeriode.equals(orginalVilkårsPeriode)) {
            vilkårBuilder.tilbakestill(orginalVilkårsPeriode);
        }
        vilkårBuilder
            .leggTil(vilkårBuilder
                .hentBuilderFor(vilkårsPeriode)
                .medUtfall(oppfylt ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT)
                .medMerknad(oppfylt ? VilkårUtfallMerknad.UDEFINERT : VilkårUtfallMerknad.VM_1041)
                .medAvslagsårsak(oppfylt ? null : Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG));
        builder.leggTil(vilkårBuilder);
        return builder;
    }

    void ryddVedtaksresultatOgVilkår(BehandlingskontrollKontekst kontekst, DatoIntervallEntitet vilkårsPeriode) {
        Optional<VedtakVarsel> behandlingresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(kontekst.getBehandlingId());
        ryddOppVilkårsvurdering(kontekst, behandlingresultatOpt, vilkårsPeriode);
        nullstillVedtaksresultat(kontekst, behandlingresultatOpt);
    }

    private void ryddOppVilkårsvurdering(BehandlingskontrollKontekst kontekst, Optional<VedtakVarsel> behandlingresultatOpt, DatoIntervallEntitet vilkårsPeriode) {
        Optional<Vilkårene> vilkårResultatOpt = vilkårResultatRepository.hentHvisEksisterer(kontekst.getBehandlingId());
        if (vilkårResultatOpt.isEmpty()) {
            return;
        }
        Vilkårene vilkårene = vilkårResultatOpt.get();
        Optional<Vilkår> beregningsvilkåret = vilkårene.getVilkårene().stream()
            .filter(vilkår -> vilkår.getVilkårType().equals(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .findFirst();
        if (beregningsvilkåret.isEmpty()) {
            return;
        }
        var behandlingsresultat = behandlingresultatOpt.get();
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(vilkårsPeriode);
        vilkårBuilder.leggTil(vilkårPeriodeBuilder.medUtfall(IKKE_VURDERT));
        var nyttResultat = builder.build();
        behandlingsresultatRepository.lagre(kontekst.getBehandlingId(), behandlingsresultat);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), nyttResultat);
    }

    private void nullstillVedtaksresultat(BehandlingskontrollKontekst kontekst, Optional<VedtakVarsel> behandlingresultatOpt) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (behandlingresultatOpt.isEmpty() || Objects.equals(behandling.getBehandlingResultatType(), BehandlingResultatType.IKKE_FASTSATT)) {
            return;
        }
        behandling.setBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

}
