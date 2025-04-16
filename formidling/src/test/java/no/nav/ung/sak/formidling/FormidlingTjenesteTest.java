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
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.innhold.EndringRapportertInntektInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtlederImpl;
import no.nav.ung.sak.formidling.vedtaksbrevvalg.VedtaksbrevValgRepository;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevForhåndsvisDto;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevValgRequestDto;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.UngdomsytelseSøknadsperiodeTjeneste;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ytelse.RapportertInntektMapper;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

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
        ungTestRepositories = BrevUtils.lagAlleUngTestRepositories(entityManager);
        lagBrevgenererOgVedtaksbrevRegler();
        vedtaksbrevValgRepository = new VedtaksbrevValgRepository(entityManager);
        formidlingTjeneste = new FormidlingTjeneste(
            brevGenerererTjeneste, vedtaksbrevRegler, vedtaksbrevValgRepository
        );

    }

    private void lagBrevgenererOgVedtaksbrevRegler() {
        var repositoryProvider = ungTestRepositories.repositoryProvider();
        var tilkjentYtelseRepository = ungTestRepositories.tilkjentYtelseRepository();

        var ungdomsprogramPeriodeTjeneste = new UngdomsprogramPeriodeTjeneste(ungTestRepositories.ungdomsprogramPeriodeRepository());

        var endringInnholdBygger =
            new EndringRapportertInntektInnholdBygger(tilkjentYtelseRepository,
                new RapportertInntektMapper(ungTestRepositories.abakusInMemoryInntektArbeidYtelseTjeneste(), new MånedsvisTidslinjeUtleder(ungdomsprogramPeriodeTjeneste, repositoryProvider.getBehandlingRepository()))
            );

        var detaljertResultatUtleder = new DetaljertResultatUtlederImpl(
            new ProsessTriggerPeriodeUtleder(ungTestRepositories.prosessTriggereRepository(), new UngdomsytelseSøknadsperiodeTjeneste(ungTestRepositories.ungdomsytelseStartdatoRepository(), ungdomsprogramPeriodeTjeneste, repositoryProvider.getBehandlingRepository())),
            tilkjentYtelseRepository, repositoryProvider.getVilkårResultatRepository());

        Instance<VedtaksbrevInnholdBygger> innholdByggere = new UnitTestLookupInstanceImpl<>(endringInnholdBygger);

        vedtaksbrevRegler = new VedtaksbrevRegler(repositoryProvider.getBehandlingRepository(), innholdByggere, detaljertResultatUtleder);

        brevGenerererTjeneste = new BrevGenerererTjenesteImpl(
            repositoryProvider.getBehandlingRepository(),
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient(),
            repositoryProvider.getPersonopplysningRepository(),
            vedtaksbrevRegler);
    }

//    @Test
    void test() {
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

        //Initielle valg - kun automatisk brev
        var valg = formidlingTjeneste.vedtaksbrevValg(behandling.getId());
        assertThat(valg.harBrev()).isTrue();
        assertThat(valg.enableRediger()).isTrue();
        assertThat(valg.redigert()).isFalse();
        assertThat(valg.kanOverstyreRediger()).isTrue();
        assertThat(valg.redigertBrevHtml()).isNull();

        //Forhåndsviser automatisk brev
        assertThat(forhåndsvis(behandling, false)).contains("<h1>");

        //Forhåndsviser redigert brev - skal feile da det ikke finnes noe lagret enda
        assertThatThrownBy(() -> forhåndsvis(behandling, true))
            .isInstanceOf(IllegalStateException.class);


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

        var valgEtterRedigering = formidlingTjeneste.vedtaksbrevValg(behandling.getId());
        assertThat(valgEtterRedigering.harBrev()).isTrue();
        assertThat(valgEtterRedigering.enableRediger()).isTrue();
        assertThat(valgEtterRedigering.redigert()).isTrue();
        assertThat(valgEtterRedigering.kanOverstyreRediger()).isTrue();
        assertThat(valgEtterRedigering.redigertBrevHtml()).isEqualTo(redigertHtml);


        //Forhåndsviser automatisk brev - skal fortsått gå bra
        assertThat(forhåndsvis(behandling, false)).contains("<h1>");

        //Forhåndsviser maneull brev - skal nå gå bra
        assertThat(forhåndsvis(behandling, false)).contains(redigertHtml);



    }

    private String forhåndsvis(Behandling behandling, boolean redigertVersjon) {
        return formidlingTjeneste.forhåndsvisVedtaksbrev(
            new VedtaksbrevForhåndsvisDto(behandling.getId(), redigertVersjon),
            true).dokument().html();
    }


}
