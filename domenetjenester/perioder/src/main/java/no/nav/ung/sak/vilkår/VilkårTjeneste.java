package no.nav.ung.sak.vilkår;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;

import java.util.Objects;
import java.util.Optional;

@Dependent
public class VilkårTjeneste {


    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    protected VilkårTjeneste() {
        // CDI Proxy
    }

    @Inject
    public VilkårTjeneste(BehandlingRepository behandlingRepository,
                          VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public Vilkårene hentVilkårResultat(Long behandlingId) {
        return vilkårResultatRepository.hent(behandlingId);
    }


    public void nullstillBehandlingsresultat(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = hentBehandling(behandlingId);
        if (Objects.equals(behandling.getBehandlingResultatType(), BehandlingResultatType.IKKE_FASTSATT)) {
            return;
        }
        behandling.setBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private Behandling hentBehandling(Long behandlingId) {
        return behandlingRepository.hentBehandling(behandlingId);
    }

    public Optional<Vilkårene> hentHvisEksisterer(Long behandlingId) {
        return vilkårResultatRepository.hentHvisEksisterer(behandlingId);
    }

    public LocalDateTimeline<VilkårUtfallSamlet> samletVilkårsresultat(Long behandlingId) {
        return vilkårResultatRepository.hentHvisEksisterer(behandlingId)
            .map(SamletVilkårsutfallTjeneste::samleVilkårUtfall)
            .orElseGet(LocalDateTimeline::empty);
    }
}
