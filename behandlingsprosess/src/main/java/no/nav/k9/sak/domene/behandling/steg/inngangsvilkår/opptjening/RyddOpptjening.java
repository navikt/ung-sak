package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import static no.nav.k9.kodeverk.vilkår.Utfall.IKKE_VURDERT;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.vedtak.konfig.Tid;

public class RyddOpptjening {

    private final OpptjeningRepository opptjeningRepository;
    private final BehandlingskontrollKontekst kontekst;
    private final BehandlingRepository behandlingRepository;
    private final VilkårResultatRepository vilkårResultatRepository;

    public RyddOpptjening(BehandlingRepository behandlingRepository, OpptjeningRepository opptjeningRepository, VilkårResultatRepository vilkårResultatRepository, BehandlingskontrollKontekst kontekst) {
        this.opptjeningRepository = opptjeningRepository;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
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

    public void ryddOppAktiviteter(LocalDate fomDato, LocalDate tomDato) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        ryddOppVilkårsvurderinger(behandling, fomDato, tomDato);
    }

    private Optional<Vilkår> ryddOppVilkårsvurderinger(Behandling behandling, LocalDate fom, LocalDate tom) {
        Vilkårene vilkårene = hentVilkårResultat(behandling.getId());
        if (vilkårene == null) {
            return Optional.empty();
        }
        Optional<Vilkår> opptjeningVilkår = vilkårene.getVilkårene().stream()
            .filter(vilkåret -> vilkåret.getVilkårType().equals(VilkårType.OPPTJENINGSVILKÅRET))
            .findFirst();

        if (opptjeningVilkår.isPresent()) {
            VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
            final var vilkårBuilder = builder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET);
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(fom, tom)
                .medUtfall(IKKE_VURDERT));
            builder.leggTil(vilkårBuilder);
            final var nyttResultat = builder.build();
            vilkårResultatRepository.lagre(behandling.getId(), nyttResultat);
        }
        return opptjeningVilkår;
    }

    private Vilkårene hentVilkårResultat(Long behandlingId) {
        Optional<Vilkårene> vilkårResultatOpt = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        return vilkårResultatOpt.orElse(null);
    }

    private void tilbakestillOpptjenigsperiodevilkår(Behandling behandling) {
        Vilkårene vilkårene = hentVilkårResultat(behandling.getId());
        if (vilkårene == null) {
            return;
        }
        Optional<Vilkår> opptjeningPeriodeVilkår = vilkårene.getVilkårene().stream()
            .filter(vilkåret -> vilkåret.getVilkårType().equals(VilkårType.OPPTJENINGSPERIODEVILKÅR))
            .findFirst();
        if (opptjeningPeriodeVilkår.isPresent()) {
            VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
            final var vilkårBuilder = builder.hentBuilderFor(VilkårType.OPPTJENINGSPERIODEVILKÅR);
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE) // FIXME (k9) må stille tilbake de periodene under vurdering
                .medUtfall(IKKE_VURDERT));
            builder.leggTil(vilkårBuilder);
            final var nyttResultat = builder.build();
            vilkårResultatRepository.lagre(behandling.getId(), nyttResultat);
        }
    }
}
