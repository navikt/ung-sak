package no.nav.ung.sak.formidling;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import domene.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.domene.GenerertBrev;
import no.nav.ung.sak.formidling.domene.PdlPerson;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelseUtleder;
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseTilkjentYtelseUtleder;

/**
 * Tester at pdf blir generert riktig.
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BrevGenerererTjenesteTest {

    private BrevGenerererTjeneste brevGenerererTjeneste;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private TilkjentYtelseUtleder tilkjentYtelseUtleder;

    @Inject
    private EntityManager entityManager;
    private BehandlingRepositoryProvider repositoryProvider;


    PdlKlientFake pdlKlient = PdlKlientFake.medTilfeldigFnr();

    @BeforeEach
    void setup() {

        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ungdomsytelseGrunnlagRepository = new UngdomsytelseGrunnlagRepository(entityManager);
        ungdomsprogramPeriodeRepository = new UngdomsprogramPeriodeRepository(entityManager);
        tilkjentYtelseUtleder = new UngdomsytelseTilkjentYtelseUtleder(ungdomsytelseGrunnlagRepository);

    }


    @Test
    void skal_lage_vedtakspdf() {

        var scenario = BrevScenarioer.lagAvsluttetStandardBehandling(repositoryProvider, ungdomsytelseGrunnlagRepository, ungdomsprogramPeriodeRepository);
        var ungTestGrunnlag = scenario.getUngTestGrunnlag();
        var behandling = scenario.getBehandling();

        brevGenerererTjeneste = new BrevGenerererTjeneste(
            repositoryProvider.getBehandlingRepository(),
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient(),
            ungdomsytelseGrunnlagRepository,
            new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository),
            tilkjentYtelseUtleder,
            repositoryProvider.getPersonopplysningRepository());


        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.INNVILGELSE);

        assertThat(erPdf(generertBrev.dokument().pdf())).isTrue();

        PdlPerson mottaker = generertBrev.mottaker();
        assertThat(mottaker.navn()).isEqualTo(ungTestGrunnlag.navn());
        assertThat(mottaker.aktørId().getAktørId()).isEqualTo(behandling.getAktørId().getAktørId());

        PdlPerson gjelder = generertBrev.gjelder();
        assertThat(gjelder).isEqualTo(mottaker);
        assertThat(generertBrev.malType()).isEqualTo(DokumentMalType.INNVILGELSE_DOK);

        var brevtekst = generertBrev.dokument().html();
        assertThatHtml(brevtekst).containsText("Til: " + ungTestGrunnlag.navn());

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


}


