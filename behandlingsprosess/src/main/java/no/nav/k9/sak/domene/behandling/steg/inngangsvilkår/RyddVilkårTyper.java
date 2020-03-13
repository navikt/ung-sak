package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår;

import static no.nav.k9.kodeverk.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.vilkår.VilkårType;

public class RyddVilkårTyper {

    static Map<VilkårType, Consumer<RyddVilkårTyper>> OPPRYDDER_FOR_AVKLARTE_DATA = new HashMap<>();

    static {
        OPPRYDDER_FOR_AVKLARTE_DATA.put(MEDLEMSKAPSVILKÅRET, r -> r.medlemskapRepository.slettAvklarteMedlemskapsdata(r.behandling.getId(), r.kontekst.getSkriveLås()));
    }

    private final Behandling behandling;
    private final BehandlingskontrollKontekst kontekst;
    private BehandlingRepository behandlingRepository;
    private MedlemskapRepository medlemskapRepository;

    public RyddVilkårTyper(@SuppressWarnings("unused") BehandlingStegModell modell,
                           BehandlingRepositoryProvider repositoryProvider,
                           Behandling behandling,
                           BehandlingskontrollKontekst kontekst) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.behandling = behandling;
        this.kontekst = kontekst;
    }

    public void ryddVedTilbakeføring() {
        nullstillVedtaksresultat();
    }

    private void nullstillVedtaksresultat() {
        if ( Objects.equals(behandling.getBehandlingResultatType(), BehandlingResultatType.IKKE_FASTSATT)) {
            return;
        }

        behandling.setBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

}
