package no.nav.ung.sak.formidling;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.FørstegangsInnvilgelseInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class FørstegangsInnvilgelseTest extends AbstractVedtaksbrevInnholdByggerTest {

    private final LocalDate DAGENS_DATO = LocalDate.of(2025, 8, 15);

    FørstegangsInnvilgelseTest() {
        super(2, "Du får ungdomsprogramytelse");
    }

    @DisplayName("Innvilgelse med riktig fom dato, maks antall dager, lav sats, grunnbeløp, hjemmel")
    @Test
    void standardInnvilgelse() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        var ungTestGrunnlag = BrevScenarioer.innvilget19år(fom);

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får ungdomsprogramytelse \
                Fra 1. august 2025 får du ungdomsprogramytelse på 681 kroner per dag utenom lørdag og søndag. \
                Pengene får du utbetalt én gang i måneden før den 10. i måneden. \
                Den første utbetalingen får du måneden etter at du begynner i ungdomsprogrammet. \
                Pengene du får, blir det trukket skatt av. Hvis du har frikort, blir det ikke trukket skatt. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                """ + hvorforFårDuPleiepengerAvsnitt() + """
                Hvordan regner vi oss fram til hvor mye penger du har rett på? \
                Når Nav regner ut hvor mye penger du kan ha rett på, bruker vi en bestemt sum som heter grunnbeløpet. \
                Grunnbeløpet er bestemt av Stortinget, og det endrer seg hvert år. \
                Nå er grunnbeløpet på 130 160 kroner. \
                Når du er under 25 år, bruker vi grunnbeløpet ganger 2/3 av 2,041. \
                Det vil si 177 105 kroner i året. \
                Denne summen deler vi på 260 dager, fordi du ikke får penger for lørdager og søndager. \
                Det vil si at du har rett på 681 kroner per dag. \
                Det er dette vi kaller dagsatsen. \
                """ + meldFraTilOssHvisDuHarEndringerAvsnitt()
        );

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ungdomsprogramytelse</h1>",
                "<h2>Hvorfor får du ungdomsprogramytelsen?</h2>",
                "<h2>Hvordan regner vi oss fram til hvor mye penger du har rett på?</h2>",
                "<h2>Meld fra til oss hvis du har inntekt i tillegg til ungdomsprogramytelsen</h2>"
            );

    }

    @Test
    void høySats() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        var ungTestGrunnlag = BrevScenarioer.innvilget27år(fom);

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());

        var brevtekst = generertBrev.dokument().html();

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får ungdomsprogramytelse \
                Fra 1. august 2025 får du ungdomsprogramytelse på 1 022 kroner per dag utenom lørdag og søndag. \
                Pengene får du utbetalt én gang i måneden før den 10. i måneden. \
                Den første utbetalingen får du måneden etter at du begynner i ungdomsprogrammet. \
                Pengene du får, blir det trukket skatt av. Hvis du har frikort, blir det ikke trukket skatt. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                """ + hvorforFårDuPleiepengerAvsnitt() + """
                Hvordan regner vi oss fram til hvor mye penger du har rett på? \
                Når Nav regner ut hvor mye penger du kan ha rett på, bruker vi en bestemt sum som heter grunnbeløpet. \
                Grunnbeløpet er bestemt av Stortinget, og det endrer seg hvert år. \
                Nå er grunnbeløpet på 130 160 kroner. \
                Når du er over 25 år, bruker vi grunnbeløpet ganger 2,041. \
                Det vil si 265 657 kroner i året. \
                Denne summen deler vi på 260 dager, fordi du ikke får penger for lørdager og søndager. \
                Det vil si at du har rett på 1 022 kroner per dag. \
                Det er dette vi kaller dagsatsen. \
                """ + meldFraTilOssHvisDuHarEndringerAvsnitt());

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce("<h1>Du får ungdomsprogramytelse</h1>");
    }

    @DisplayName("blir 29 i løpet av programmet og får mindre enn maks antall dager")
    @Test
    void høySatsMaksAlder6MndIProgrammet() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        var fødselsdato = LocalDate.of(1997, 2, 15); //Blir 29 etter 6 mnd/130 dager i programmet
        var ungTestGrunnlag = BrevScenarioer.innvilget29År(fom, fødselsdato);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får ungdomsprogramytelse \
                Fra 1. august 2025 til 15. februar 2026 får du ungdomsprogramytelse på 1 022 kroner per dag utenom lørdag og søndag. \
                Pengene får du utbetalt én gang i måneden før den 10. i måneden. \
                Den første utbetalingen får du måneden etter at du begynner i ungdomsprogrammet. \
                Pengene du får, blir det trukket skatt av. Hvis du har frikort, blir det ikke trukket skatt. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                """ + hvorforFårDuPleiepengerAvsnitt() + """
                Hvordan regner vi oss fram til hvor mye penger du har rett på? \
                Når Nav regner ut hvor mye penger du kan ha rett på, bruker vi en bestemt sum som heter grunnbeløpet. \
                Grunnbeløpet er bestemt av Stortinget, og det endrer seg hvert år. \
                Nå er grunnbeløpet på 130 160 kroner. \
                Når du er over 25 år, bruker vi grunnbeløpet ganger 2,041. \
                Det vil si 265 657 kroner i året. \
                Denne summen deler vi på 260 dager, fordi du ikke får penger for lørdager og søndager. \
                Det vil si at du har rett på 1 022 kroner per dag. \
                Det er dette vi kaller dagsatsen. \
                """ + meldFraTilOssHvisDuHarEndringerAvsnitt()
        );

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ungdomsprogramytelse</h1>"
            );
    }

    @Test
    void lavOgHøySats() {
        var fom = LocalDate.of(2025, 8, 1);
        var ungTestGrunnlag = BrevScenarioer.innvilget24årBle25årførsteMåned(fom);
        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får ungdomsprogramytelse \
                Fra 1. august 2025 får du ungdomsprogramytelse på 681 kroner per dag utenom lørdag og søndag. \
                Fordi du fylte 25 år 16. august 2025, får du mer penger fra denne datoen. Da får du 1 022 kroner per dag, utenom lørdag og søndag. \
                Pengene får du utbetalt én gang i måneden før den 10. i måneden. \
                Den første utbetalingen får du måneden etter at du begynner i ungdomsprogrammet. \
                Pengene du får, blir det trukket skatt av. Hvis du har frikort, blir det ikke trukket skatt. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                """ + hvorforFårDuPleiepengerAvsnitt() + """
                Hvordan regner vi oss fram til hvor mye penger du har rett på? \
                Når Nav regner ut hvor mye penger du kan ha rett på, bruker vi en bestemt sum som heter grunnbeløpet. \
                Grunnbeløpet er bestemt av Stortinget, og det endrer seg hvert år. \
                Nå er grunnbeløpet på 130 160 kroner. \
                Når du er under 25 år, bruker vi grunnbeløpet ganger 2/3 av 2,041. \
                Det vil si 177 105 kroner i året. \
                Denne summen deler vi på 260 dager, fordi du ikke får penger for lørdager og søndager. \
                Det vil si at du har rett på 681 kroner per dag. \
                Det er dette vi kaller dagsatsen. \
                Når du er over 25 år, bruker vi grunnbeløpet ganger 2,041 som blir 265 657 kroner i året. \
                Det vil si at du har rett på 1 022 kroner per dag. \
                """ + meldFraTilOssHvisDuHarEndringerAvsnitt()
        );


        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ungdomsprogramytelse</h1>"
            );
    }
    @DisplayName("Innvilgelse med barnetillegg")
    @Test
    void barnetillegg() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        var ungTestGrunnlag = BrevScenarioer.innvilget19årMedBarn15DagerEtterStartdato(fom);

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får ungdomsprogramytelse \
                Fra 1. august 2025 får du ungdomsprogramytelse på 681 kroner per dag utenom lørdag og søndag. \
                Fordi du fikk barn 16. august 2025, får du 37 kroner mer fra denne datoen. Da får du 718 kroner per dag, utenom lørdag og søndag. \
                Pengene får du utbetalt én gang i måneden før den 10. i måneden. \
                Den første utbetalingen får du måneden etter at du begynner i ungdomsprogrammet. \
                Pengene du får, blir det trukket skatt av. Hvis du har frikort, blir det ikke trukket skatt. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                """ + hvorforFårDuPleiepengerAvsnitt() + """
                Hvordan regner vi oss fram til hvor mye penger du har rett på? \
                Når Nav regner ut hvor mye penger du kan ha rett på, bruker vi en bestemt sum som heter grunnbeløpet. \
                Grunnbeløpet er bestemt av Stortinget, og det endrer seg hvert år. \
                Nå er grunnbeløpet på 130 160 kroner. \
                Når du er under 25 år, bruker vi grunnbeløpet ganger 2/3 av 2,041. \
                Det vil si 177 105 kroner i året. \
                Denne summen deler vi på 260 dager, fordi du ikke får penger for lørdager og søndager. \
                Det vil si at du har rett på 681 kroner per dag. \
                Det er dette vi kaller dagsatsen. \
                Fordi du har 1 barn, får du i tillegg 37 kroner per dag i barnetillegg. \
                Det vil si at du til sammen får 718 kroner per dag. \
                """ + meldFraTilOssHvisDuHarEndringerAvsnitt()
        );

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ungdomsprogramytelse</h1>"
            );

    }

    @DisplayName("Innvilgelsesbrev med alle kombinasjoner: barnefødsel, barnedødsfall, overgang 25 år, etterbetaling. Sjekker om alt kommer i riktig rekkefølge")
    @Test
    void medAlleKombinasjoner() {
        var ungTestGrunnlag = BrevScenarioer.innvilget24MedAlleKombinasjonerFom21April2025();

        var behandling = lagScenario(ungTestGrunnlag);

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.INNVILGELSE);

        var brevtekst = generertBrev.dokument().html();

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            //TODO G-verdi endrer seg i perioder som teksten omtaler, men endret G-verdi er ikke nevnt
            """
                Du får ungdomsprogramytelse \
                Fra 21. april 2025 får du ungdomsprogramytelse på 649 kroner per dag utenom lørdag og søndag. \
                Fordi du fikk barn 6. mai 2025, får du 37 kroner mer per barn fra denne datoen. Da får du 755 kroner per dag, utenom lørdag og søndag. \
                Fordi du mistet barn 10. mai 2025, får du ikke barnetillegg på 37 kroner fra denne datoen. Da får du 718 kroner per dag, utenom lørdag og søndag. \
                Fordi du fylte 25 år 12. mai 2025, får du mer penger fra denne datoen. Da får du 1 059 kroner per dag, utenom lørdag og søndag. \
                Pengene får du utbetalt én gang i måneden før den 10. i måneden. \
                Den første utbetalingen får du innen en uke. \
                Pengene du får, blir det trukket skatt av. Hvis du har frikort, blir det ikke trukket skatt. \
                Du finner mer informasjon om utbetalingen hvis du logger inn på Min side på nav.no. \
                """ + hvorforFårDuPleiepengerAvsnitt() + """
                Hvordan regner vi oss fram til hvor mye penger du har rett på? \
                Når Nav regner ut hvor mye penger du kan ha rett på, bruker vi en bestemt sum som heter grunnbeløpet. \
                Grunnbeløpet er bestemt av Stortinget, og det endrer seg hvert år. \
                Nå er grunnbeløpet på 130 160 kroner. \
                Når du er under 25 år, bruker vi grunnbeløpet ganger 2/3 av 2,041. \
                Det vil si 168 761 kroner i året. \
                Denne summen deler vi på 260 dager, fordi du ikke får penger for lørdager og søndager. \
                Det vil si at du har rett på 649 kroner per dag. \
                Det er dette vi kaller dagsatsen. \
                Når du er over 25 år, bruker vi grunnbeløpet ganger 2,041 som blir 265 657 kroner i året. \
                Det vil si at du har rett på 1 022 kroner per dag. \
                Fordi du har 1 barn, får du i tillegg 37 kroner per dag i barnetillegg. \
                Det vil si at du til sammen får 1 059 kroner per dag. \
                """ + meldFraTilOssHvisDuHarEndringerAvsnitt()
        );

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får ungdomsprogramytelse</h1>"
            );

    }

    private static String meldFraTilOssHvisDuHarEndringerAvsnitt() {
        return """
            Meld fra til oss hvis du har inntekt i tillegg til ungdomsprogramytelsen \
            Hvis du har en annen inntekt i tillegg til ytelsen, er det veldig viktig at du sier fra til oss om det. \
            Du får en SMS den 1. hver måned. \
            Når du har fått SMS-en, logger du inn på Min side på nav.no og gir oss beskjed om all inntekt du har hatt måneden før. \
            Når du har en inntekt, får du mindre penger gjennom ungdomsprogramytelsen. \
            Likevel får du til sammen mer penger når du både har en inntekt og får ytelsen, enn hvis du bare får ytelsen. \
            """;
    }

    private static String hvorforFårDuPleiepengerAvsnitt() {
        return """
            Hvorfor får du ungdomsprogramytelsen? \
            Du får ytelsen fordi du er med i ungdomsprogrammet. \
            Ytelsen gir deg en inntekt mens du deltar i ungdomsprogrammet. \
            Du får penger gjennom ytelsen så lenge du er i ungdomsprogrammet, men du kan som hovedregel ikke få penger i mer enn ett år. \
            Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
            """;
    }

    private Behandling lagScenario(UngTestScenario ungTestscenario) {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestscenario);

        var behandling = scenarioBuilder.buildOgLagreMedUng(
            ungTestRepositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return behandling;
    }

    @Override
    protected VedtaksbrevInnholdBygger lagVedtaksbrevInnholdBygger() {
        var ungdomsprogramPeriodeTjeneste = new UngdomsprogramPeriodeTjeneste(ungTestRepositories.ungdomsprogramPeriodeRepository(), ungTestRepositories.ungdomsytelseStartdatoRepository());

        return  new FørstegangsInnvilgelseInnholdBygger(
            ungTestRepositories.ungdomsytelseGrunnlagRepository(),
            ungdomsprogramPeriodeTjeneste,
            ungTestRepositories.tilkjentYtelseRepository(), false, DAGENS_DATO);
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        UngTestScenario ungTestscenario = BrevScenarioer.innvilget19år(LocalDate.of(2024, 12, 1));
        return lagScenario(ungTestscenario);
    }
}


