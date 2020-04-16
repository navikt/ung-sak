package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import static no.nav.k9.kodeverk.vilkår.Utfall.IKKE_VURDERT;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

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

    public void ryddOpp(DatoIntervallEntitet periode) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        Optional<Vilkår> vilkår = ryddOppVilkårsvurderinger(behandling, periode.getFomDato(), periode.getTomDato());
        if (vilkår.isPresent()) {
            opptjeningRepository.deaktiverOpptjeningForPeriode(behandling, periode);
            tilbakestillOpptjenigsperiodevilkår(behandling, periode);
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

    private void tilbakestillOpptjenigsperiodevilkår(Behandling behandling, DatoIntervallEntitet periode) {
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
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode.getFomDato(), periode.getTomDato())
                .medUtfall(IKKE_VURDERT));
            builder.leggTil(vilkårBuilder);
            final var nyttResultat = builder.build();
            vilkårResultatRepository.lagre(behandling.getId(), nyttResultat);
        }
    }
}
