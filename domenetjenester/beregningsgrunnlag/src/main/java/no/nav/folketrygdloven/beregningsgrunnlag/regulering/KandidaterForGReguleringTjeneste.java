package no.nav.folketrygdloven.beregningsgrunnlag.regulering;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
public class KandidaterForGReguleringTjeneste {

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    KandidaterForGReguleringTjeneste() {
    }

    @Inject
    public KandidaterForGReguleringTjeneste(BehandlingRepository behandlingRepository, VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public boolean skalGReguleres(Long fagsakId, DatoIntervallEntitet periode) {
        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElseThrow();
        var vilkårene = vilkårResultatRepository.hent(sisteBehandling.getId());

        var harOverlappendeGrunnlag = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .orElseThrow(() -> new IllegalStateException("Fagsaken(id=" + fagsakId + ") har ikke beregnignsvilkåret knyttet til siste behandling"))
            .getPerioder()
            .stream()
            .filter(it -> Utfall.OPPFYLT.equals(it.getGjeldendeUtfall()))
            .anyMatch(it -> periode.overlapper(it.getPeriode().getFomDato(), it.getFom())); // FOM må være i perioden

        // TODO: Utvide med tjeneste mot kalkulus for å sjekke om DETTE grunnlaget skal revurderes

        return harOverlappendeGrunnlag;
    }

}
