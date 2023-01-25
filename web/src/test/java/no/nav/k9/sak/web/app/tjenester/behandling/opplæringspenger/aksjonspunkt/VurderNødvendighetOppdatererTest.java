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
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderNødvendighetDto;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokumentInformasjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokumentRepository;

@CdiDbAwareTest
class VurderNødvendighetOppdatererTest {

    @Inject
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    @Inject
    private EntityManager entityManager;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository;

    private VurderNødvendighetOppdaterer vurderNødvendighetOppdaterer;
    private Behandling behandling;
    private static final String brukerId = "bruker1";
    private PleietrengendeSykdomDokument legeerklæring;

    @BeforeAll
    static void subjectHandlerSetup() {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker(brukerId);
    }

    @BeforeEach
    void setup() {
        BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vurderNødvendighetOppdaterer = new VurderNødvendighetOppdaterer(vurdertOpplæringRepository, behandlingRepository, pleietrengendeSykdomDokumentRepository);
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad().medSøknadsdato(LocalDate.now());
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_NØDVENDIGHET, BehandlingStegType.VURDER_NØDVENDIGHETS_VILKÅR);
        behandling = scenario.lagre(repositoryProvider);
        scenario.getFagsak().setPleietrengende(AktørId.dummy());
        legeerklæring = lagreNyttSykdomDokument(SykdomDokumentType.LEGEERKLÆRING_MED_DOKUMENTASJON_AV_OPPLÆRING);
    }

    @Test
    void skalLagreNyttGrunnlag() {
        final JournalpostIdDto journalpostIdDto = new JournalpostIdDto("1337");
        final VurderNødvendighetDto dto = new VurderNødvendighetDto(journalpostIdDto, true, "", Set.of(legeerklæring.getId().toString()));
        dto.setBegrunnelse("fordi");

        OppdateringResultat resultat = lagreGrunnlag(dto);
        assertThat(resultat).isNotNull();

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertInstitusjonHolder()).isNull();
        assertThat(grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring()).hasSize(1);
        VurdertOpplæring vurdertOpplæring = grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring().get(0);
        assertThat(vurdertOpplæring.getNødvendigOpplæring()).isEqualTo(dto.isNødvendigOpplæring());
        assertThat(vurdertOpplæring.getBegrunnelse()).isEqualTo(dto.getBegrunnelse());
        assertThat(vurdertOpplæring.getVurdertAv()).isEqualTo(brukerId);
        assertThat(vurdertOpplæring.getVurdertTidspunkt()).isNotNull();
        assertThat(vurdertOpplæring.getDokumenter()).hasSize(1);
        assertThat(vurdertOpplæring.getDokumenter().get(0)).isEqualTo(legeerklæring);
    }

    @Test
    void skalOppdatereGrunnlag() {
        final JournalpostIdDto journalpostIdDto = new JournalpostIdDto("1338");
        final VurderNødvendighetDto dto1 = new VurderNødvendighetDto(journalpostIdDto, false, "", Set.of(legeerklæring.getId().toString()));
        lagreGrunnlag(dto1);
        LocalDateTime vurdertTidspunkt1 = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId()).orElseThrow().getVurdertOpplæringHolder().getVurdertOpplæring().get(0).getVurdertTidspunkt();

        var kursbeskrivelse = lagreNyttSykdomDokument(SykdomDokumentType.DOKUMENTASJON_AV_OPPLÆRING);
        final VurderNødvendighetDto dto2 = new VurderNødvendighetDto(journalpostIdDto, true, "", Set.of(kursbeskrivelse.getId().toString()));
        lagreGrunnlag(dto2);

        Optional<VurdertOpplæringGrunnlag> grunnlag = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(behandling.getId());
        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring()).hasSize(1);
        var vurdertOpplæring = grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring().get(0);
        assertThat(vurdertOpplæring.getNødvendigOpplæring()).isEqualTo(dto2.isNødvendigOpplæring());
        assertThat(vurdertOpplæring.getVurdertTidspunkt()).isAfter(vurdertTidspunkt1);
        assertThat(vurdertOpplæring.getDokumenter()).hasSize(1);
        assertThat(vurdertOpplæring.getDokumenter().get(0)).isEqualTo(kursbeskrivelse);
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
        assertThat(grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring()).hasSize(2);
        var vurdertOpplæring1 = grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring().stream().filter(vurdertOpplæring -> vurdertOpplæring.getJournalpostId().equals(journalpostIdDto1.getJournalpostId())).findFirst();
        assertThat(vurdertOpplæring1).isPresent();
        var vurdertOpplæring2 = grunnlag.get().getVurdertOpplæringHolder().getVurdertOpplæring().stream().filter(vurdertOpplæring -> vurdertOpplæring.getJournalpostId().equals(journalpostIdDto2.getJournalpostId())).findFirst();
        assertThat(vurdertOpplæring2).isPresent();
        assertThat(vurdertOpplæring1.get().getVurdertTidspunkt()).isNotEqualTo(vurdertOpplæring2.get().getVurdertTidspunkt());
    }

    private OppdateringResultat lagreGrunnlag(VurderNødvendighetDto dto) {
        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto);

        return vurderNødvendighetOppdaterer.oppdater(dto, param);
    }

    private PleietrengendeSykdomDokument lagreNyttSykdomDokument(SykdomDokumentType type) {
        PleietrengendeSykdomDokument dokument = new PleietrengendeSykdomDokument(new JournalpostId("456"), null,
            new PleietrengendeSykdomDokumentInformasjon(type, false, LocalDate.now(), LocalDateTime.now(), 1L, "meg", LocalDateTime.now()),
            null, null, null, "meg", LocalDateTime.now());
        pleietrengendeSykdomDokumentRepository.lagre(dokument, behandling.getFagsak().getPleietrengendeAktørId());
        return dokument;
    }
}
