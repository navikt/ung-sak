package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.vilkår.Utfall.IKKE_VURDERT;

import java.time.LocalDate;
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

    public void lagreAvslåttVilkårresultat(BehandlingskontrollKontekst kontekst,
                                           DatoIntervallEntitet vilkårsPeriode,
                                           Avslagsårsak avslagsårsak) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        VilkårResultatBuilder vilkårResultatBuilder = opprettAvslåttVilkårsResultat(
            behandling,
            vilkårene,
            vilkårsPeriode,
            avslagsårsak);
        behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårResultatBuilder.build());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    public void lagreVilkårresultat(BehandlingskontrollKontekst kontekst,
                                    DatoIntervallEntitet vilkårsPeriode, boolean vilkårOppfylt) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        VilkårResultatBuilder vilkårResultatBuilder = opprettVilkårsResultat(vilkårOppfylt, vilkårene, vilkårsPeriode, behandling);
        if (!vilkårOppfylt) {
            behandling.setBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        }
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), vilkårResultatBuilder.build());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private VilkårResultatBuilder opprettAvslåttVilkårsResultat(Behandling behandling, Vilkårene vilkårene,
                                                                DatoIntervallEntitet vilkårsPeriode,
                                                                Avslagsårsak avslagsårsak) {
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .medKantIKantVurderer(getVilkårsPerioderTilVurderingTjeneste(behandling).getKantIKantVurderer());
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

    private VilkårResultatBuilder opprettVilkårsResultat(boolean oppfylt, Vilkårene vilkårene, DatoIntervallEntitet vilkårsPeriode, Behandling behandling) {
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .medKantIKantVurderer(getVilkårsPerioderTilVurderingTjeneste(behandling).getKantIKantVurderer());
        vilkårBuilder
            .leggTil(vilkårBuilder
                .hentBuilderFor(vilkårsPeriode)
                .medUtfall(oppfylt ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT)
                .medMerknad(oppfylt ? VilkårUtfallMerknad.UDEFINERT : VilkårUtfallMerknad.VM_1041)
                .medAvslagsårsak(oppfylt ? null : Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG));
        builder.leggTil(vilkårBuilder);
        return builder;
    }

    private VilkårsPerioderTilVurderingTjeneste getVilkårsPerioderTilVurderingTjeneste(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType()).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + VilkårsPerioderTilVurderingTjeneste.class.getName() + " for ytelsetype=" + behandling.getFagsakYtelseType()));
    }


    public void ryddVedtaksresultatOgVilkår(BehandlingskontrollKontekst kontekst, DatoIntervallEntitet vilkårsPeriode) {
        Optional<VedtakVarsel> behandlingresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(kontekst.getBehandlingId());
        settVilkårutfallTilIkkeVurdert(kontekst.getBehandlingId(), vilkårsPeriode);
        nullstillVedtaksresultat(kontekst, behandlingresultatOpt);
    }

    public void settVilkårutfallTilIkkeVurdert(Long behandlingId, DatoIntervallEntitet vilkårsPeriode) {
        Optional<Vilkårene> vilkårResultatOpt = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
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
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .medKantIKantVurderer(getVilkårsPerioderTilVurderingTjeneste(behandling).getKantIKantVurderer());
        var vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(vilkårsPeriode);
        vilkårBuilder.leggTil(vilkårPeriodeBuilder.medUtfall(IKKE_VURDERT));
        builder.leggTil(vilkårBuilder);
        var nyttResultat = builder.build();
        vilkårResultatRepository.lagre(behandlingId, nyttResultat);
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
        var maxTom = perioder.stream().map(DatoIntervallEntitet::getTomDato).max(LocalDate::compareTo);

        if (vilkår.isPresent() && skalIgnorereAvslåttePerioder) {
            if (maxTom.isPresent()) {
                var utvidetTilVUrdering = vilkår.get()
                    .getPerioder()
                    .stream()
                    .filter(it -> it.getFom().isAfter(maxTom.get()))
                    .filter(it -> IKKE_VURDERT.equals(it.getUtfall()))
                    .map(VilkårPeriode::getPeriode)
                    .collect(Collectors.toList());
                perioder.addAll(utvidetTilVUrdering);
            }
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
