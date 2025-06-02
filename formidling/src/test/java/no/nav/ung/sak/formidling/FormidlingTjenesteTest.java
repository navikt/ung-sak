package no.nav.ung.sak.formidling;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.innhold.EndringRapportertInntektInnholdBygger;
import no.nav.ung.sak.formidling.innhold.ManuellVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtlederImpl;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevForhåndsvisDto;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgRequestDto;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.UngdomsytelseSøknadsperiodeTjeneste;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class FormidlingTjenesteTest {


    private BrevGenerererTjeneste brevGenerererTjeneste;
    private VedtaksbrevRegler vedtaksbrevRegler;
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;
    private UngTestRepositories ungTestRepositories;
    private FormidlingTjeneste formidlingTjeneste;

    PdlKlientFake pdlKlient = PdlKlientFake.medTilfeldigFnr();

    @Inject
    private EntityManager entityManager;

    @BeforeEach
    void setup() {
        ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        lagBrevgenererOgVedtaksbrevRegler();
        vedtaksbrevValgRepository = new VedtaksbrevValgRepository(entityManager);
        formidlingTjeneste = new FormidlingTjeneste(
            brevGenerererTjeneste, vedtaksbrevRegler, vedtaksbrevValgRepository,
            ungTestRepositories.repositoryProvider().getBehandlingRepository());

    }

    private void lagBrevgenererOgVedtaksbrevRegler() {
        var repositoryProvider = ungTestRepositories.repositoryProvider();
        var tilkjentYtelseRepository = ungTestRepositories.tilkjentYtelseRepository();

        var ungdomsprogramPeriodeTjeneste = new UngdomsprogramPeriodeTjeneste(ungTestRepositories.ungdomsprogramPeriodeRepository());

        var endringInnholdBygger = new EndringRapportertInntektInnholdBygger(tilkjentYtelseRepository);

        var detaljertResultatUtleder = new DetaljertResultatUtlederImpl(
            new ProsessTriggerPeriodeUtleder(ungTestRepositories.prosessTriggereRepository(), new UngdomsytelseSøknadsperiodeTjeneste(ungTestRepositories.ungdomsytelseStartdatoRepository(), ungdomsprogramPeriodeTjeneste, repositoryProvider.getBehandlingRepository())),
            tilkjentYtelseRepository, repositoryProvider.getVilkårResultatRepository());

        Instance<VedtaksbrevInnholdBygger> innholdByggere = new UnitTestLookupInstanceImpl<>(endringInnholdBygger);

        vedtaksbrevRegler = new VedtaksbrevRegler(repositoryProvider.getBehandlingRepository(), innholdByggere, detaljertResultatUtleder, ungTestRepositories.ungdomsprogramPeriodeRepository());

        brevGenerererTjeneste = new BrevGenerererTjenesteImpl(
            repositoryProvider.getBehandlingRepository(),
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient(),
            repositoryProvider.getPersonopplysningRepository(),
            vedtaksbrevRegler,
            ungTestRepositories.vedtaksbrevValgRepository(),
            new ManuellVedtaksbrevInnholdBygger(ungTestRepositories.vedtaksbrevValgRepository()));
    }

    @Test
    void skal_bruke_automatisk_brev_hvis_ikke_overstyrt() {
        var behandling = lagScenarioMedRedigerbarBrev();

        //Initielle valg - kun automatisk brev
        var valg = formidlingTjeneste.vedtaksbrevValg(behandling.getId());
        assertThat(valg.harBrev()).isTrue();
        assertThat(valg.enableRediger()).isTrue();
        assertThat(valg.redigert()).isFalse();
        assertThat(valg.kanOverstyreRediger()).isTrue();
        assertThat(valg.redigertBrevHtml()).isNull();

        //Forhåndsviser automatisk brev
        String automatiskBrevHtmlSnippet = "<h1>";
        assertThat(forhåndsvis(behandling, false)).contains(automatiskBrevHtmlSnippet);
        //Brevet behandlingen kommer til å bruke skal være automatisk brev
        assertThat(forhåndsvis(behandling, null)).contains(automatiskBrevHtmlSnippet);

        //Forhåndsviser redigert brev - skal feile da det ikke finnes noe lagret
        assertThatThrownBy(() -> forhåndsvis(behandling, true))
            .isInstanceOf(IllegalStateException.class);

    }

    @Test
    void skal_bruke_manuell_brev_hvis_redigert() {
        var behandling = lagScenarioMedRedigerbarBrev();
        String automatiskBrevHtmlSnippet = "<h1>";

        //Lager redigert tekst
        String redigertHtml = "<h2>Manuell skrevet brev</h2>";
        formidlingTjeneste.lagreVedtaksbrev(
            new VedtaksbrevValgRequestDto(
                behandling.getId(),
                false,
                true,
                redigertHtml
            )
        );

        var valgEtterRedigering1 = formidlingTjeneste.vedtaksbrevValg(behandling.getId());
        assertThat(valgEtterRedigering1.harBrev()).isTrue();
        assertThat(valgEtterRedigering1.enableRediger()).isTrue();
        assertThat(valgEtterRedigering1.redigert()).isTrue();
        assertThat(valgEtterRedigering1.kanOverstyreRediger()).isTrue();
        assertThat(valgEtterRedigering1.redigertBrevHtml()).isEqualTo(redigertHtml);

        //Forhåndsviser automatisk brev - skal fortsått gå bra
        assertThat(forhåndsvis(behandling, false)).contains(automatiskBrevHtmlSnippet);

        //Forhåndsviser maneull brev - skal nå gå bra
        assertThat(forhåndsvis(behandling, true)).contains(redigertHtml);

        //Brevet behandlingen kommer til å bruke skal være manuell brev
        assertThat(forhåndsvis(behandling, null)).contains(redigertHtml);
    }


    @Test
    void endrer_fra_rediger_tilbake_til_automatisk() {
        var behandling = lagScenarioMedRedigerbarBrev();
        String automatiskBrevHtmlSnippet = "<h1>";

        //Lager redigert tekst
        String redigertHtml = "<h2>Manuell skrevet brev</h2>";
        formidlingTjeneste.lagreVedtaksbrev(
            new VedtaksbrevValgRequestDto(
                behandling.getId(),
                false,
                true,
                redigertHtml
            )
        );

        //Brevet behandlingen kommer til å bruke skal være manuell brev
        assertThat(forhåndsvis(behandling, null)).contains(redigertHtml);

        //Tilbakestiller manuell brev
        formidlingTjeneste.lagreVedtaksbrev(
            new VedtaksbrevValgRequestDto(
                behandling.getId(),
                false,
                false,
                null
            )
        );

        var valgEtterRedigering2 = formidlingTjeneste.vedtaksbrevValg(behandling.getId());
        assertThat(valgEtterRedigering2.harBrev()).isTrue();
        assertThat(valgEtterRedigering2.enableRediger()).isTrue();
        assertThat(valgEtterRedigering2.redigert()).isFalse();
        assertThat(valgEtterRedigering2.kanOverstyreRediger()).isTrue();
        assertThat(valgEtterRedigering2.redigertBrevHtml()).isNull();


        //Forhåndsviser automatisk brev
        assertThat(forhåndsvis(behandling, false)).contains(automatiskBrevHtmlSnippet);

        //Forhåndsviser redigert brev - skal feile da det ikke finnes noe lagret lenger
        assertThatThrownBy(() -> forhåndsvis(behandling, true))
            .isInstanceOf(IllegalStateException.class);

        //Brevet behandlingen kommer til å bruke skal være automatisk brev
        assertThat(forhåndsvis(behandling, null)).contains(automatiskBrevHtmlSnippet);

    }

    @Test
    void skal_beholde_redigert_tekst_ved_tilbakehopp() {
        var behandling = lagScenarioMedRedigerbarBrev();
        String automatiskBrevHtmlSnippet = "<h1>";
        String redigertHtml = "<h2>Manuell skrevet brev</h2>";

        formidlingTjeneste.lagreVedtaksbrev(
            new VedtaksbrevValgRequestDto(
                behandling.getId(),
                false,
                true,
                redigertHtml
            )
        );

        //Tilbakestiller
        formidlingTjeneste.ryddVedTilbakeHopp(behandling.getId());
        var valgEtterRedigering3 = formidlingTjeneste.vedtaksbrevValg(behandling.getId());
        assertThat(valgEtterRedigering3.harBrev()).isTrue();
        assertThat(valgEtterRedigering3.enableRediger()).isTrue();
        assertThat(valgEtterRedigering3.redigert()).isFalse();
        assertThat(valgEtterRedigering3.kanOverstyreRediger()).isTrue();
        //Beholder teksten
        assertThat(valgEtterRedigering3.redigertBrevHtml()).isEqualTo(redigertHtml);

        //Brevet behandlingen kommer til å bruke skal være automatisk brev
        assertThat(forhåndsvis(behandling, null)).contains(automatiskBrevHtmlSnippet);

        //Forhåndsviser redigert brev - skal bruke den gamle teksten
        assertThat(forhåndsvis(behandling, true)).contains(redigertHtml);

    }

    @Test
    void skal_ikke_lage_brev_hvis_hindret() {
        var behandling = lagScenarioMedRedigerbarBrev();
        String automatiskBrevHtmlSnippet = "<h1>";
        String redigertHtml = "<h2>Manuell skrevet brev</h2>";

        //Lagerer hindret valget
        formidlingTjeneste.lagreVedtaksbrev(
            new VedtaksbrevValgRequestDto(
                behandling.getId(),
                true,
                true,
                redigertHtml
            )
        );

        var valgEtterRedigering1 = formidlingTjeneste.vedtaksbrevValg(behandling.getId());
        assertThat(valgEtterRedigering1.harBrev()).isTrue();
        assertThat(valgEtterRedigering1.enableHindre()).isTrue();
        assertThat(valgEtterRedigering1.hindret()).isTrue();
        assertThat(valgEtterRedigering1.kanOverstyreHindre()).isTrue();

        //Forhåndsviser automatisk brev - skal fortsått gå bra
        assertThat(forhåndsvis(behandling, false)).contains(automatiskBrevHtmlSnippet);

        //Forhåndsviser maneull brev - skal fortsatt gå bra
        assertThat(forhåndsvis(behandling, true)).contains(redigertHtml);

        //Ingen brev som brukes av behandling
        assertThat(forhåndsvis(behandling, null)).isNull();
    }


    private Behandling lagScenarioMedRedigerbarBrev() {
        UngTestScenario ungTestscenario = BrevScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));

        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestscenario);

        scenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);

        var behandling = scenarioBuilder.buildOgLagreMedUng(ungTestRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.KONTROLLER_INNTEKT);
        new AksjonspunktTestSupport().setTilUtført(aksjonspunkt, "utført");
        BehandlingRepository behandlingRepository = ungTestRepositories.repositoryProvider().getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private String forhåndsvis(Behandling behandling, Boolean redigertVersjon) {
        GenerertBrev generertBrev = formidlingTjeneste.forhåndsvisVedtaksbrev(
            new VedtaksbrevForhåndsvisDto(behandling.getId(), redigertVersjon, true)
        );
        return Optional.ofNullable(generertBrev).map(it -> it.dokument().html()).orElse(null);
    }

}
