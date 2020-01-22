package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Utfall.IKKE_VURDERT;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.vedtak.konfig.Tid;

public class RyddOpptjening {

    private final OpptjeningRepository opptjeningRepository;
    private final BehandlingskontrollKontekst kontekst;
    private final BehandlingRepository behandlingRepository;

    public RyddOpptjening(BehandlingRepository behandlingRepository, OpptjeningRepository opptjeningRepository, BehandlingskontrollKontekst kontekst) {
        this.opptjeningRepository = opptjeningRepository;
        this.behandlingRepository = behandlingRepository;
        this.kontekst = kontekst;
    }

    public void ryddOpp() {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        Optional<Vilkår> vilkår = ryddOppVilkårsvurderinger(behandling, Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE);
        if (vilkår.isPresent()) {
            opptjeningRepository.deaktiverOpptjening(behandling);
            tilbakestillOpptjenigsperiodevilkår(behandling);
        }
    }

    public void ryddOppAktiviteter() {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        ryddOppVilkårsvurderinger(behandling, Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE);
    }

    private Optional<Vilkår> ryddOppVilkårsvurderinger(Behandling behandling, LocalDate fom, LocalDate tom) {
        VilkårResultat vilkårResultat = hentVilkårResultat(behandling);
        if (vilkårResultat == null) {
            return Optional.empty();
        }
        Optional<Vilkår> opptjeningVilkår = vilkårResultat.getVilkårene().stream()
            .filter(vilkåret -> vilkåret.getVilkårType().equals(VilkårType.OPPTJENINGSVILKÅRET))
            .findFirst();

        if (opptjeningVilkår.isPresent()) {
            VilkårResultatBuilder builder = VilkårResultat.builderFraEksisterende(vilkårResultat);
            final var vilkårBuilder = builder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET);
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(fom, tom)
                .medUtfall(IKKE_VURDERT));
            builder.leggTil(vilkårBuilder);
            final var nyttResultat = builder.build();
            behandling.getBehandlingsresultat().medOppdatertVilkårResultat(nyttResultat);
            behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        }
        return opptjeningVilkår;
    }

    private VilkårResultat hentVilkårResultat(Behandling behandling) {
        Optional<VilkårResultat> vilkårResultatOpt = Optional.ofNullable(behandling.getBehandlingsresultat())
            .map(Behandlingsresultat::getVilkårResultat);
        return vilkårResultatOpt.orElse(null);
    }

    private void tilbakestillOpptjenigsperiodevilkår(Behandling behandling) {
        VilkårResultat vilkårResultat = hentVilkårResultat(behandling);
        if (vilkårResultat == null) {
            return;
        }
        Optional<Vilkår> opptjeningPeriodeVilkår = vilkårResultat.getVilkårene().stream()
            .filter(vilkåret -> vilkåret.getVilkårType().equals(VilkårType.OPPTJENINGSPERIODEVILKÅR))
            .findFirst();
        if (opptjeningPeriodeVilkår.isPresent()) {
            VilkårResultatBuilder builder = VilkårResultat.builderFraEksisterende(vilkårResultat);
            final var vilkårBuilder = builder.hentBuilderFor(VilkårType.OPPTJENINGSPERIODEVILKÅR);
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)
                .medUtfall(IKKE_VURDERT));
            builder.leggTil(vilkårBuilder);
            final var nyttResultat = builder.build();
            behandling.getBehandlingsresultat().medOppdatertVilkårResultat(nyttResultat);
            behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        }
    }
}
