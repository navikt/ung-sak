package no.nav.ung.sak.formidling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.innhold.FørstegangsInnvilgelseInnholdBygger;
import no.nav.ung.sak.formidling.innhold.ManueltVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.mottaker.BrevMottakerTjeneste;
import no.nav.ung.sak.formidling.mottaker.PdlPerson;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatInfo;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtlederFake;
import no.nav.ung.sak.formidling.vedtak.regler.FørstegangsInnvilgelseByggerVelger;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevRegler;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tester at pdf blir generert riktig.
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VedtaksbrevGenerererTjenesteTest {

    private UngTestRepositories ungTestRepositories;

    @Inject
    private EntityManager entityManager;
    PdlKlientFake pdlKlient = PdlKlientFake.medTilfeldigFnr();

    @BeforeEach
    void setup() {
        ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
    }


    @Test
    void skal_lage_vedtakspdf() {

        var scenario = BrevScenarioer.lagAvsluttetStandardBehandling(ungTestRepositories);
        var ungTestGrunnlag = scenario.getUngTestGrunnlag();
        var behandling = scenario.getBehandling();

        var repositoryProvider = ungTestRepositories.repositoryProvider();

        FørstegangsInnvilgelseInnholdBygger førstegangsInnvilgelseInnholdBygger = new FørstegangsInnvilgelseInnholdBygger(
            ungTestRepositories.ungdomsytelseGrunnlagRepository(),
            new UngdomsprogramPeriodeTjeneste(ungTestRepositories.ungdomsprogramPeriodeRepository(), ungTestRepositories.ungdomsytelseStartdatoRepository()),
            ungTestRepositories.tilkjentYtelseRepository(), false, null);
        VedtaksbrevGenerererTjeneste vedtaksbrevGenerererTjeneste = new VedtaksbrevGenerererTjenesteImpl(
            repositoryProvider.getBehandlingRepository(),
            new PdfGenKlient(),
            new VedtaksbrevRegler(
                repositoryProvider.getBehandlingRepository(),
                new UnitTestLookupInstanceImpl<>(førstegangsInnvilgelseInnholdBygger),
                new DetaljertResultatUtlederFake(
                    ungTestGrunnlag.ungdomsprogramvilkår().mapValue(it -> DetaljertResultat.of(DetaljertResultatInfo.of(DetaljertResultatType.INNVILGELSE_UTBETALING_NY_PERIODE), Collections.emptySet(), Collections.emptySet(), Collections.emptySet()))),
                ungTestRepositories.ungdomsprogramPeriodeRepository(), ungTestRepositories.ungdomsytelseGrunnlagRepository(), false,
                new UnitTestLookupInstanceImpl<>(new FørstegangsInnvilgelseByggerVelger(førstegangsInnvilgelseInnholdBygger))),
            ungTestRepositories.vedtaksbrevValgRepository(), new ManueltVedtaksbrevInnholdBygger(ungTestRepositories.vedtaksbrevValgRepository()),
            new BrevMottakerTjeneste(new AktørTjeneste(pdlKlient), repositoryProvider.getPersonopplysningRepository()));


        GenerertBrev generertBrev = vedtaksbrevGenerererTjeneste.genererVedtaksbrevForBehandling(behandling.getId(), false);
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.INNVILGELSE);

        assertThat(erPdf(generertBrev.dokument().pdf())).isTrue();

        PdlPerson mottaker = generertBrev.mottaker();
        assertThat(mottaker.navn()).isEqualTo(ungTestGrunnlag.navn());
        assertThat(mottaker.aktørId().getAktørId()).isEqualTo(behandling.getAktørId().getAktørId());

        PdlPerson gjelder = generertBrev.gjelder();
        assertThat(gjelder).isEqualTo(mottaker);
        assertThat(generertBrev.malType()).isEqualTo(DokumentMalType.INNVILGELSE_DOK);

        var brevtekst = generertBrev.dokument().html();
        assertThat(brevtekst).contains("Til: " + ungTestGrunnlag.navn());

    }

    public static boolean erPdf(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < 5) {
            return false; // Not enough data to check
        }

        // Bruker StandardCharsets.US_ASCII for å sikre konsekvent tolkning av PDF-magiske tall ("%PDF-"),
        // siden dette alltid er innenfor ASCII-tegnsettet, uavhengig av plattformens standard tegnsett.
        String magicNumber = new String(fileBytes, 0, 5, StandardCharsets.US_ASCII);
        return "%PDF-".equals(magicNumber);
    }


    //TODO lage hindre test
}


