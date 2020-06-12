package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår;

import static no.nav.k9.kodeverk.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

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
        if (Objects.equals(behandling.getBehandlingResultatType(), BehandlingResultatType.IKKE_FASTSATT)) {
            return;
        }

        behandling.setBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

}
