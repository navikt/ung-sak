package no.nav.k9.sak.behandlingslager.pip;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Journalpost;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.sak.db.util.Repository;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class PipRepositoryTest {

    private static final JournalpostId JOURNALPOST_ID = new JournalpostId("42");

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider ;
    private BehandlingRepository behandlingRepository ;
    private PipRepository pipRepository ;
    private FagsakRepository fagsakRepository ;
    private BasicBehandlingBuilder behandlingBuilder ;


    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        pipRepository = new PipRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
    }

    private Behandling behandling;

    private void lagreBehandling(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    @Test
    public void skal_finne_behandligstatus_og_sakstatus_for_behandling() throws Exception {
        this.behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        lagreBehandling(behandling);

        Optional<PipBehandlingsData> pipBehandlingsData = pipRepository.hentDataForBehandling(behandling.getId());
        assertThat(pipBehandlingsData.get()).isNotNull();
        assertThat(pipBehandlingsData.get().getBehandligStatus()).isEqualTo(behandling.getStatus().getKode());
        assertThat(pipBehandlingsData.get().getFagsakStatus()).isEqualTo(behandling.getFagsak().getStatus().getKode());
    }

    @Test
    public void skal_returne_tomt_resultat_når_det_søkes_etter_behandling_id_som_ikke_finnes() throws Exception {
        Optional<PipBehandlingsData> pipBehandlingsData = pipRepository.hentDataForBehandling(1241L);
        assertThat(pipBehandlingsData).isNotPresent();
    }

    @Test
    public void skal_finne_alle_fagsaker_for_en_søker() throws Exception {
        Fagsak fagsak1 = behandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
        AktørId aktørId1 = fagsak1.getAktørId();
        Fagsak fagsak2 = behandlingBuilder.opprettFagsak(FagsakYtelseType.SVANGERSKAPSPENGER, aktørId1);
        @SuppressWarnings("unused")
        Fagsak fagsakAnnenAktør = new BasicBehandlingBuilder(entityManager).opprettFagsak(FagsakYtelseType.FORELDREPENGER);

        Set<Long> resultat = pipRepository.fagsakIderForSøker(Collections.singleton(aktørId1));

        assertThat(resultat).containsOnly(fagsak1.getId(), fagsak2.getId());
    }

    public void skal_finne_aktoerId_for_fagsak() throws Exception {
        AktørId aktørId1 = AktørId.dummy();
        var fagsak = behandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER, aktørId1);

        Set<AktørId> aktørIder = pipRepository.hentAktørIdKnyttetTilFagsaker(Collections.singleton(fagsak.getId()));
        assertThat(aktørIder).containsOnly(aktørId1);
    }

    @Test
    public void skal_finne_fagsakId_knyttet_til_journalpostId() throws Exception {
        Fagsak fagsak1 = behandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
        @SuppressWarnings("unused")
        Fagsak fagsak2 = behandlingBuilder.opprettFagsak(FagsakYtelseType.FORELDREPENGER);
        Journalpost journalpost1 = new Journalpost(JOURNALPOST_ID, fagsak1);
        fagsakRepository.lagre(journalpost1);
        Journalpost journalpost2 = new Journalpost(new JournalpostId("4444"), fagsak1);
        fagsakRepository.lagre(journalpost2);
        (new Repository(entityManager)).flush();

        Set<Long> fagsakId = pipRepository.fagsakIdForJournalpostId(Collections.singleton(JOURNALPOST_ID));
        assertThat(fagsakId).containsOnly(fagsak1.getId());
    }

    @Test
    public void skal_finne_aksjonspunktTyper_for_aksjonspunktKoder() throws Exception {
        Set<String> resultat1 = pipRepository.hentAksjonspunktTypeForAksjonspunktKoder(Collections.singletonList(AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNING.getKode()));
        assertThat(resultat1).containsOnly("Overstyring");

        Set<String> resultat2 = pipRepository.hentAksjonspunktTypeForAksjonspunktKoder(List.of(AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNING.getKode(), AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD.getKode()));
        assertThat(resultat2).containsOnly("Overstyring", "Manuell");
    }

}
