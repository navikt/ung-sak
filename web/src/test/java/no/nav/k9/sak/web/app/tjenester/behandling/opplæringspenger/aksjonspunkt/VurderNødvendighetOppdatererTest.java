package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.dokument.OpplæringDokumentType;
import no.nav.k9.sak.kontrakt.opplæringspenger.vurdering.VurderNødvendighetDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument.OpplæringDokument;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument.OpplæringDokumentRepository;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertNødvendighet;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringRepository;

@CdiDbAwareTest
class VurderNødvendighetOppdatererTest {

    @Inject
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    @Inject
    private EntityManager entityManager;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private OpplæringDokumentRepository opplæringDokumentRepository;

    private VurderNødvendighetOppdaterer vurderNødvendighetOppdaterer;
    private Behandling behandling;
    private static final String brukerId = "bruker1";
    private OpplæringDokument dokument;

    @BeforeAll
    static void subjectHandlerSetup() {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker(brukerId);
    }

    @BeforeEach
    void setup() {
        BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vurderNødvendighetOppdaterer = new VurderNødvendighetOppdaterer(vurdertOpplæringRepository, behandlingRepository, opplæringDokumentRepository);
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad().medSøknadsdato(LocalDate.now());
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_NØDVENDIGHET, BehandlingStegType.VURDER_NØDVENDIGHETS_VILKÅR);
        behandling = scenario.lagre(repositoryProvider);
        scenario.getFagsak().setPleietrengende(AktørId.dummy());
        dokument = lagreNyttDokument(OpplæringDokumentType.LEGEERKLÆRING_MED_DOKUMENTASJON_AV_OPPLÆRING);
    }

    @Test
    void skalLagreNyttGrunnlag() {
        final JournalpostIdDto journalpostIdDto = new JournalpostIdDto("1337");
        final VurderNødvendighetDto dto = new VurderNødvendighetDto(journalpostIdDto, true, "", Set.of(dokument.getId().toString()));
        dto.setBegrunnelse("fordi");

        OppdateringResultat resultat = lagreGrunnlag(dto);
        assertThat(resultat).isNotNull();

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertInstitusjonHolder()).isNull();
        assertThat(grunnlag.get().getVurdertNødvendighetHolder().getVurdertNødvendighet()).hasSize(1);
        VurdertNødvendighet vurdertNødvendighet = grunnlag.get().getVurdertNødvendighetHolder().getVurdertNødvendighet().get(0);
        assertThat(vurdertNødvendighet.getNødvendigOpplæring()).isEqualTo(dto.isNødvendigOpplæring());
        assertThat(vurdertNødvendighet.getBegrunnelse()).isEqualTo(dto.getBegrunnelse());
        assertThat(vurdertNødvendighet.getVurdertAv()).isEqualTo(brukerId);
        assertThat(vurdertNødvendighet.getVurdertTidspunkt()).isNotNull();
        assertThat(vurdertNødvendighet.getDokumenter()).hasSize(1);
        assertThat(vurdertNødvendighet.getDokumenter().get(0)).isEqualTo(dokument);
    }

    @Test
    void skalOppdatereGrunnlag() {
        final JournalpostIdDto journalpostIdDto = new JournalpostIdDto("1338");
        final VurderNødvendighetDto dto1 = new VurderNødvendighetDto(journalpostIdDto, false, "", Set.of(dokument.getId().toString()));
        lagreGrunnlag(dto1);
        LocalDateTime vurdertTidspunkt1 = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId()).orElseThrow().getVurdertNødvendighetHolder().getVurdertNødvendighet().get(0).getVurdertTidspunkt();

        var kursbeskrivelse = lagreNyttDokument(OpplæringDokumentType.DOKUMENTASJON_AV_OPPLÆRING);
        final VurderNødvendighetDto dto2 = new VurderNødvendighetDto(journalpostIdDto, true, "", Set.of(kursbeskrivelse.getId().toString()));
        lagreGrunnlag(dto2);

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertNødvendighetHolder().getVurdertNødvendighet()).hasSize(1);
        var vurdertNødvendighet = grunnlag.get().getVurdertNødvendighetHolder().getVurdertNødvendighet().get(0);
        assertThat(vurdertNødvendighet.getNødvendigOpplæring()).isEqualTo(dto2.isNødvendigOpplæring());
        assertThat(vurdertNødvendighet.getVurdertTidspunkt()).isAfter(vurdertTidspunkt1);
        assertThat(vurdertNødvendighet.getDokumenter()).hasSize(1);
        assertThat(vurdertNødvendighet.getDokumenter().get(0)).isEqualTo(kursbeskrivelse);
    }

    @Test
    void skalLagreGrunnlagOgKopiereFraAktivtForAnnenJornalpostId() {
        final JournalpostIdDto journalpostIdDto1 = new JournalpostIdDto("1339");
        final VurderNødvendighetDto dto1 = new VurderNødvendighetDto(journalpostIdDto1, true, "", Set.of());
        lagreGrunnlag(dto1);

        final JournalpostIdDto journalpostIdDto2 = new JournalpostIdDto("1340");
        final VurderNødvendighetDto dto2 = new VurderNødvendighetDto(journalpostIdDto2, true, "", Set.of());
        lagreGrunnlag(dto2);

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertNødvendighetHolder().getVurdertNødvendighet()).hasSize(2);
        var vurdertNødvendighet1 = grunnlag.get().getVurdertNødvendighetHolder().getVurdertNødvendighet().stream().filter(vurdertOpplæring -> vurdertOpplæring.getJournalpostId().equals(journalpostIdDto1.getJournalpostId())).findFirst();
        assertThat(vurdertNødvendighet1).isPresent();
        var vurdertNødvendighet2 = grunnlag.get().getVurdertNødvendighetHolder().getVurdertNødvendighet().stream().filter(vurdertOpplæring -> vurdertOpplæring.getJournalpostId().equals(journalpostIdDto2.getJournalpostId())).findFirst();
        assertThat(vurdertNødvendighet2).isPresent();
        assertThat(vurdertNødvendighet1.get().getVurdertTidspunkt()).isNotEqualTo(vurdertNødvendighet2.get().getVurdertTidspunkt());
    }

    private OppdateringResultat lagreGrunnlag(VurderNødvendighetDto dto) {
        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        return vurderNødvendighetOppdaterer.oppdater(dto, param);
    }

    private OpplæringDokument lagreNyttDokument(OpplæringDokumentType type) {
        OpplæringDokument dokument = new OpplæringDokument(new JournalpostId("456"),null, type, behandling.getUuid(), LocalDate.now(), LocalDateTime.now());
        opplæringDokumentRepository.lagre(dokument);
        return dokument;
    }
}
