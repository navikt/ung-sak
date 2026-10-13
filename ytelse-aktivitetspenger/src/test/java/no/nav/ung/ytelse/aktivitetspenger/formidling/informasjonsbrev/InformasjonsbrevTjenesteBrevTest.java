package no.nav.ung.ytelse.aktivitetspenger.formidling.informasjonsbrev;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingRepository;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingStatusType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.informasjonsbrev.InformasjonsbrevTjeneste;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.GenereltFritekstBrevDto;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevBestillingRequest;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevMottakerDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.BrevTestUtils;
import no.nav.ung.ytelse.aktivitetspenger.formidling.PdlKlientFake;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerFørstegangsbehandlingScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestRepositories;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class InformasjonsbrevTjenesteBrevTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private PdlKlientFake pdlKlient;
    private String fnr;
    private AktivitetspengerTestRepositories repositories;

    @Inject
    private InformasjonsbrevTjeneste informasjonsbrevTjeneste;

    private BrevbestillingRepository brevbestillingRepository;


    @BeforeEach
    void setup() {
        repositories = BrevTestUtils.lagAlleTestRepositories(entityManager);
        brevbestillingRepository = new BrevbestillingRepository(entityManager);
        fnr = pdlKlient.fnr();
    }

    @Test
    void skal_forhåndsvise_informasjonsbrev() {
        // Given
        LocalDate fom = LocalDate.of(2025, 8, 1);
        AktivitetspengerTestScenario scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilgetKunLavSats(fom);
        Behandling behandling = lagStandardBehandling(scenario);

        // When
        String overskrift = "Dette er en test for forhåndsvisning av informasjonsbrev";
        String brødtekst = "Test brødtekst.";
        String brødtekstMedMarkdown = "### " + brødtekst;

        GenerertBrev generertBrev = informasjonsbrevTjeneste.forhåndsvis(
            new InformasjonsbrevBestillingRequest(
                behandling.getId(), DokumentMalType.GENERELT_FRITEKSTBREV,
                new InformasjonsbrevMottakerDto(behandling.getAktørId().getId(), IdType.AKTØRID),
                new GenereltFritekstBrevDto(overskrift, brødtekstMedMarkdown)
            ),
            AktivitetspengerBrevScenarioerUtils.NAV_SENTRAL_IDENT, true
        );

        // Then
        var mottaker = generertBrev.mottaker();
        assertThat(mottaker.navn()).isEqualTo(scenario.navn());
        assertThat(mottaker.aktørId().getAktørId()).isEqualTo(behandling.getAktørId().getAktørId());
        assertThat(generertBrev.gjelder()).isEqualTo(mottaker);
        assertThat(generertBrev.malType()).isEqualTo(DokumentMalType.GENERELT_FRITEKSTBREV);
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_GENERELT_FRITEKSTBREV);

        var forventetFullBrev = InformasjonsbrevVerifikasjon.medHeaderOgFooter(fnr,
            overskrift + " " + brødtekst + " "
        );

        String brevtekst = generertBrev.dokument().html();
        assertThatHtml(brevtekst).asPlainTextIsEqualTo(forventetFullBrev);
        assertThatHtml(brevtekst)
            .containsHtmlSubSequenceOnce(
                "<h1>" + overskrift + "</h1>",
                "<h3>" + brødtekst + "</h3>"
            );
    }


    @Test
    void skal_bestille_informasjonsbrev() {
        // Given
        LocalDate fom = LocalDate.of(2025, 8, 1);
        AktivitetspengerTestScenario scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilgetKunLavSats(fom);
        Behandling behandling = lagStandardBehandling(scenario);

        // When
        String overskrift = "Dette er en test for forhåndsvisning av informasjonsbrev";
        String brødtekst = "Test brødtekst.";
        informasjonsbrevTjeneste.bestill(
            new InformasjonsbrevBestillingRequest(
                behandling.getId(), DokumentMalType.GENERELT_FRITEKSTBREV,
                new InformasjonsbrevMottakerDto(behandling.getAktørId().getId(), IdType.AKTØRID),
                new GenereltFritekstBrevDto(overskrift, brødtekst)
            ),
            AktivitetspengerBrevScenarioerUtils.NAV_SENTRAL_IDENT);

        // Then
        var behandlingBrevbestillingEntitets = brevbestillingRepository.hentForBehandling(behandling.getId());
        assertThat(behandlingBrevbestillingEntitets).hasSize(1);

        var bestilling = behandlingBrevbestillingEntitets.getFirst();
        assertThat(bestilling.getBrevbestillingUuid()).isNotNull();
        assertThat(bestilling.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(bestilling.getFagsakId()).isEqualTo(behandling.getFagsakId());
        assertThat(bestilling.getDokumentMalType()).isEqualTo(DokumentMalType.GENERELT_FRITEKSTBREV);
        assertThat(bestilling.getTemplateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_GENERELT_FRITEKSTBREV);
        assertThat(bestilling.getStatus()).isEqualTo(BrevbestillingStatusType.JOURNALFØRT);
        assertThat(bestilling.getDokdistBestillingId()).isNull();
        assertThat(bestilling.getMottaker().getMottakerId()).isEqualTo(behandling.getAktørId().getAktørId());
        assertThat(bestilling.getMottaker().getMottakerIdType()).isEqualTo(IdType.AKTØRID);
        assertThat(bestilling.isVedtaksbrev()).isFalse();
    }

    private Behandling lagStandardBehandling(AktivitetspengerTestScenario scenario) {
        return AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medAktivitetspengerTestGrunnlag(scenario)
            .buildOgLagreMedAktivitspenger(repositories);
    }

}
