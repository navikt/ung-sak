package no.nav.ung.sak.formidling;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.søknad.JsonUtils;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.geografisk.Landkoder;
import no.nav.ung.kodeverk.person.PersonstatusType;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsResultat;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersonBasisTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.formidling.domene.GenerertBrev;
import no.nav.ung.sak.formidling.domene.PdlPerson;
import no.nav.ung.sak.formidling.dto.Brevbestilling;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;

/**
 * Tester at pdf blir generert riktig.
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class BrevGenerererTjenesteTest {

    private ObjectMapper objectMapper = JsonUtils.getObjectMapper();
    private BrevGenerererTjeneste brevGenerererTjeneste;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    private EntityManager entityManager;
    private BehandlingRepositoryProvider repositoryProvider;


    String navn = "Halvorsen Halvor";
    String fnr = PdlKlientFake.gyldigFnr();

    @BeforeEach
    void setup() {

        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ungdomsytelseGrunnlagRepository = new UngdomsytelseGrunnlagRepository(entityManager);
        ungdomsprogramPeriodeRepository = new UngdomsprogramPeriodeRepository(entityManager);

    }


    @Test
    void skal_lage_innvilgelsesbrev_pdf() {
        int alder = 19;
        var fødselsdato = LocalDate.now().minusYears(alder);
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad();
        var ungdom = scenario.getDefaultBrukerAktørId();
        var stp = LocalDate.of(2024, 12, 1);
        Periode periode = new Periode(stp, stp.plusYears(1));
        scenario.leggTilVilkår(VilkårType.ALDERSVILKÅR, Utfall.OPPFYLT, periode);
        scenario.leggTilVilkår(VilkårType.UNGDOMSPROGRAMVILKÅRET, Utfall.OPPFYLT, periode);


        PersonInformasjon personInformasjon = scenario
            .opprettBuilderForRegisteropplysninger()
            .medPersonas()
            .ungdom(ungdom, fødselsdato)
            .statsborgerskap(Landkoder.NOR)
            .personstatus(PersonstatusType.BOSA)
            .build();

        scenario.medRegisterOpplysninger(personInformasjon);

        var behandling = scenario.lagre(repositoryProvider);

        UngdomsytelseSatser høySats = new UngdomsytelseSatser(
            BigDecimal.valueOf(608.31), BigDecimal.valueOf(118620), BigDecimal.valueOf(1.3333), UngdomsytelseSatsType.LAV, 0, 0);
        var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(
                periode.getFom(), periode.getTom(), høySats
            )
        ));

        ungdomsytelseGrunnlagRepository.lagre(behandling.getId(), new UngdomsytelseSatsResultat(timeline, "regelInputSats", "regelSporingSats"));

        UngdomsytelseUttakPerioder uttakperioder = new UngdomsytelseUttakPerioder(
            List.of(new UngdomsytelseUttakPeriode(
                BigDecimal.valueOf(100), DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom())
            ))
        );
        uttakperioder.setRegelInput("regelInputUttak");
        uttakperioder.setRegelSporing("regelSporingUttak");
        ungdomsytelseGrunnlagRepository.lagre(behandling.getId(), uttakperioder
        );

        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(periode.getFom(), TIDENES_ENDE)));


        var pdlKlient = new PdlKlientFake("Halvor", "Halvorsen", fnr);

        brevGenerererTjeneste = new BrevGenerererTjeneste(
            repositoryProvider.getBehandlingRepository(),
            new PersonBasisTjeneste(pdlKlient),
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient(),
            ungdomsytelseGrunnlagRepository,
            ungdomsprogramPeriodeRepository
        );


        GenerertBrev generertBrev = brevGenerererTjeneste.genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.INNVILGELSE);

        assertThat(erPdf(generertBrev.dokument().pdf())).isTrue();
        if (System.getenv("LAGRE_PDF") != null) {
            PdfUtils.lagrePdf(generertBrev.dokument().pdf(), generertBrev.malType().name());
        }

        PdlPerson mottaker = generertBrev.mottaker();
        assertThat(mottaker.navn()).isEqualTo(navn);
        assertThat(mottaker.aktørId().getAktørId()).isEqualTo(ungdom.getAktørId());

        PdlPerson gjelder = generertBrev.gjelder();
        assertThat(gjelder).isEqualTo(mottaker);
        assertThat(generertBrev.malType()).isEqualTo(DokumentMalType.INNVILGELSE_DOK);

        var brevtekst = generertBrev.dokument().html();
        assertThatHtml(brevtekst).contains("Til: " + navn);

    }

    @Test //TODO vurder om denne trengs pga over
    void skal_lage_pdf_med_riktig_mottaker_navn() throws IOException {
        var ungdom = AktørId.dummy();
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad(ungdom);
        var behandling = scenarioBuilder.lagre(repositoryProvider);

        var pdlKlient = new PdlKlientFake("Halvor", "Halvorsen", fnr);

        brevGenerererTjeneste = new BrevGenerererTjeneste(
            repositoryProvider.getBehandlingRepository(),
            new PersonBasisTjeneste(pdlKlient),
            new AktørTjeneste(pdlKlient),
            new PdfGenKlient(),
            ungdomsytelseGrunnlagRepository,
            ungdomsprogramPeriodeRepository
        );

        // Lag innvilgelsesbrev
        var bestillBrevDto = new Brevbestilling(
            behandling.getId(),
            DokumentMalType.INNVILGELSE_DOK,
            behandling.getFagsak().getSaksnummer().getVerdi(),
            null,
            objectMapper.createObjectNode()
        );

        GenerertBrev generertBrev = brevGenerererTjeneste.generer(bestillBrevDto);
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.INNVILGELSE);

        assertThat(erPdf(generertBrev.dokument().pdf())).isTrue();
        if (System.getenv("LAGRE_PDF") != null) {
            PdfUtils.lagrePdf(generertBrev.dokument().pdf(), generertBrev.malType().name());
        }

        PdlPerson mottaker = generertBrev.mottaker();
        assertThat(mottaker.navn()).isEqualTo(navn);
        assertThat(mottaker.aktørId().getAktørId()).isEqualTo(ungdom.getAktørId());

        PdlPerson gjelder = generertBrev.gjelder();
        assertThat(gjelder).isEqualTo(mottaker);
        assertThat(generertBrev.malType()).isEqualTo(DokumentMalType.INNVILGELSE_DOK);

        var brevtekst = generertBrev.dokument().html();
        assertThatHtml(brevtekst).contains("Til: " + navn);

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


