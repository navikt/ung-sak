package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.vilkår.Utfall.IKKE_VURDERT;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@Dependent
public class BeregningsgrunnlagVilkårTjeneste {

    private BehandlingRepository behandlingRepository;
    private VedtakVarselRepository behandlingsresultatRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    protected BeregningsgrunnlagVilkårTjeneste() {
        // CDI Proxy
    }

    @Inject
    public BeregningsgrunnlagVilkårTjeneste(BehandlingRepository behandlingRepository,
                                            VedtakVarselRepository behandlingsresultatRepository,
                                            @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                            VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vilkårsPerioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    void lagreAvslåttVilkårresultat(BehandlingskontrollKontekst kontekst,
                                    DatoIntervallEntitet vilkårsPeriode,
                                    Avslagsårsak avslagsårsak) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        VilkårResultatBuilder vilkårResultatBuilder = opprettAvslåttVilkårsResultat(
            vilkårene,
            vilkårsPeriode,
            avslagsårsak);
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårResultatBuilder.build());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    void lagreVilkårresultat(BehandlingskontrollKontekst kontekst,
                             DatoIntervallEntitet vilkårsPeriode, boolean vilkårOppfylt) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        VilkårResultatBuilder vilkårResultatBuilder = opprettVilkårsResultat(vilkårOppfylt, vilkårene, vilkårsPeriode);
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (!vilkårOppfylt) {
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        }
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårResultatBuilder.build());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private VilkårResultatBuilder opprettAvslåttVilkårsResultat(Vilkårene vilkårene,
                                                                DatoIntervallEntitet vilkårsPeriode,
                                                                Avslagsårsak avslagsårsak) {
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
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

    private VilkårResultatBuilder opprettVilkårsResultat(boolean oppfylt, Vilkårene vilkårene, DatoIntervallEntitet vilkårsPeriode) {
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
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

    public NavigableSet<DatoIntervallEntitet> utledPerioderTilVurdering(BehandlingReferanse ref, boolean skalIgnorereAvslåttePerioder) {
        String ytelseTypeKode = ref.getFagsakYtelseType().getKode();
        var perioderTilVurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(vilkårsPerioderTilVurderingTjenester, ytelseTypeKode).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + VilkårsPerioderTilVurderingTjeneste.class.getName() + " for ytelsetype=" + ytelseTypeKode));

        var vilkår = vilkårResultatRepository.hentHvisEksisterer(ref.getBehandlingId()).flatMap(it -> it.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR));
        var perioder = new TreeSet<>(perioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR));

        if (vilkår.isPresent() && skalIgnorereAvslåttePerioder) {
            var avslåttePerioder = vilkår.get()
                .getPerioder()
                .stream()
                .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getUtfall()))
                .map(VilkårPeriode::getPeriode)
                .collect(Collectors.toList());

            perioder.removeAll(avslåttePerioder);
        }
        return Collections.unmodifiableNavigableSet(perioder);
    }

}
