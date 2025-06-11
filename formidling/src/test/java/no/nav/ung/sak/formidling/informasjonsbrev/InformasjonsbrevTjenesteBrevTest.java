package no.nav.ung.sak.formidling.informasjonsbrev;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.k9.prosesstask.impl.ProsessTaskTjenesteImpl;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.InformasjonsbrevMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingRepository;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingStatusType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.BrevScenarioer;
import no.nav.ung.sak.formidling.BrevTestUtils;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.PdlKlientFake;
import no.nav.ung.sak.formidling.bestilling.BrevbestillingTjeneste;
import no.nav.ung.sak.formidling.dokarkiv.DokArkivKlientFake;
import no.nav.ung.sak.formidling.mottaker.BrevMottakerTjeneste;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.GenereltFritekstBrevDto;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevBestillingDto;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class InformasjonsbrevTjenesteBrevTest {

    @Inject
    private EntityManager entityManager;

    private final PdlKlientFake pdlKlient = PdlKlientFake.medTilfeldigFnr();
    protected String fnr = pdlKlient.fnr();

    private UngTestRepositories ungTestRepositories;
    private InformasjonsbrevTjeneste informasjonsbrevTjeneste;
    private BrevbestillingRepository brevbestillingRepository;


    @BeforeEach
    void setup() {
        ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        brevbestillingRepository = new BrevbestillingRepository(entityManager);
        var brevbestillingTjeneste = new BrevbestillingTjeneste(
            brevbestillingRepository,
            new DokArkivKlientFake(),
            new ProsessTaskTjenesteImpl(new ProsessTaskRepositoryImpl(entityManager, null, null)));
        informasjonsbrevTjeneste = new InformasjonsbrevTjeneste(
            ungTestRepositories.repositoryProvider().getBehandlingRepository(),
            ungTestRepositories.repositoryProvider().getPersonopplysningRepository(),
            new InformasjonsbrevGenerererTjeneste(
                ungTestRepositories.repositoryProvider().getBehandlingRepository(),
                new PdfGenKlient(),
                new BrevMottakerTjeneste(new AktørTjeneste(pdlKlient),
                    ungTestRepositories.repositoryProvider().getPersonopplysningRepository())),
            brevbestillingTjeneste
        );
    }

    @Test
    void skal_forhåndsvise_informasjonsbrev() {
        // Given
        LocalDate fom = LocalDate.of(2024, 12, 1);
        UngTestScenario scenario = BrevScenarioer.innvilget19år(fom);
        Behandling behandling = lagStandardBehandling(scenario);

        // When
        String overskrift = "Dette er en test for forhåndsvisning av informasjonsbrev";
        String brødtekst = "Test brødtekst.";
        GenerertBrev generertBrev = informasjonsbrevTjeneste.forhåndsvis(
            new InformasjonsbrevBestillingDto(
                behandling.getId(), InformasjonsbrevMalType.GENERELT_FRITEKSTBREV, null,
                new GenereltFritekstBrevDto(overskrift, brødtekst)
                ),
            true
            );

        // Then
        var mottaker = generertBrev.mottaker();
        assertThat(mottaker.navn()).isEqualTo(scenario.navn());
        assertThat(mottaker.aktørId().getAktørId()).isEqualTo(behandling.getAktørId().getAktørId());
        assertThat(generertBrev.gjelder()).isEqualTo(mottaker);
        assertThat(generertBrev.malType()).isEqualTo(DokumentMalType.GENERELT_FRITEKSTBREV);
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.GENERELT_FRITEKSTBREV);

        var forventetFullBrev = InformasjonsbrevVerifikasjon.medHeaderOgFooter(fnr,
            overskrift + " " + brødtekst + " "
        );

        String brevtekst = generertBrev.dokument().html();
        assertThatHtml(brevtekst).asPlainTextIsEqualTo(forventetFullBrev);
        assertThatHtml(brevtekst)
            .containsHtmlSubSequenceOnce(
                "<h1>" + overskrift + "</h1>"
            );

    }


    @Test
    void skal_bestille_informasjonsbrev() {
        // Given
        LocalDate fom = LocalDate.of(2024, 12, 1);
        UngTestScenario scenario = BrevScenarioer.innvilget19år(fom);
        Behandling behandling = lagStandardBehandling(scenario);

        // When
        String overskrift = "Dette er en test for forhåndsvisning av informasjonsbrev";
        String brødtekst = "Test brødtekst.";
        informasjonsbrevTjeneste.bestill(
            new InformasjonsbrevBestillingDto(
                behandling.getId(), InformasjonsbrevMalType.GENERELT_FRITEKSTBREV, null,
                new GenereltFritekstBrevDto(overskrift, brødtekst)
            )
        );

        // Then
        var behandlingBrevbestillingEntitets = brevbestillingRepository.hentForBehandling(behandling.getId());
        assertThat(behandlingBrevbestillingEntitets).hasSize(1);

        var behandlingBestilling = behandlingBrevbestillingEntitets.getFirst();
        assertThat(behandlingBestilling.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(behandlingBestilling.isVedtaksbrev()).isFalse();

        var bestilling = behandlingBestilling.getBestilling();
        assertThat(bestilling.getBrevbestillingUuid()).isNotNull();
        assertThat(bestilling.getSaksnummer()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(bestilling.getDokumentMalType()).isEqualTo(DokumentMalType.GENERELT_FRITEKSTBREV);
        assertThat(bestilling.getTemplateType()).isEqualTo(TemplateType.GENERELT_FRITEKSTBREV);
        assertThat(bestilling.getStatus()).isEqualTo(BrevbestillingStatusType.JOURNALFØRT);
        assertThat(bestilling.getDokumentData()).isNull();
        assertThat(bestilling.getDokdistBestillingId()).isNull();
        assertThat(bestilling.getMottaker().getMottakerId()).isEqualTo(behandling.getAktørId().getAktørId());
        assertThat(bestilling.getMottaker().getMottakerIdType()).isEqualTo(IdType.AKTØRID);

    }
    private Behandling lagStandardBehandling(UngTestScenario scenario) {

        return TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medUngTestGrunnlag(scenario)
            .buildOgLagreMedUng(ungTestRepositories);
    }

}
