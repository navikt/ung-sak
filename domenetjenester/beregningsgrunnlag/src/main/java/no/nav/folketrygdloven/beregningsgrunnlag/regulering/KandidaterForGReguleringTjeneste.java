package no.nav.folketrygdloven.beregningsgrunnlag.regulering;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Saksnummer;

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
            .orElseThrow()
            .getPerioder()
            .stream()
            .filter(it -> Utfall.OPPFYLT.equals(it.getGjeldendeUtfall()))
            .anyMatch(it -> periode.overlapper(it.getPeriode()));

        // TODO: Utvide med tjeneste mot kalkulus for å sjekke om DETTE grunnlaget skal revurderes

        return harOverlappendeGrunnlag;
    }

}
