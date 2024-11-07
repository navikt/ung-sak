package no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.sak.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.PersonIdent;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ArbeidsgiverHistorikkinnslagTjenesteImplTest {

    private static final String PRIVATPERSON_NAVN = "Mikke Mus";
    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final String ORGNR = "999888777";
    private static final String ORG_NAVN = "Andeby Bank";
    private static final Virksomhet VIRKSOMHET = lagVirksomhet(ORGNR);
    private static final LocalDate FØDSELSDATO = LocalDate.of(2000, 1, 1);
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_REF = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final String SUFFIX = ARBEIDSFORHOLD_REF.getReferanse().substring(ARBEIDSFORHOLD_REF.getReferanse().length()-4);

    @Mock
    private PersonIdentTjeneste tpsTjeneste;

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;

    @BeforeEach
    public void setup() {
        when(tpsTjeneste.hentPersoninfoArbeidsgiver(any(AktørId.class))).thenReturn(Optional.of(lagPersoninfo()));

        var virksomhetTjeneste = mock(VirksomhetTjeneste.class);
        when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(VIRKSOMHET);
        ArbeidsgiverTjeneste arbeidsgiverTjeneste = new ArbeidsgiverTjeneste(tpsTjeneste, virksomhetTjeneste);
        arbeidsgiverHistorikkinnslagTjeneste = new ArbeidsgiverHistorikkinnslag(arbeidsgiverTjeneste);
    }

    private PersoninfoArbeidsgiver lagPersoninfo() {
        return new PersoninfoArbeidsgiver.Builder()
            .medAktørId(AKTØR_ID)
            .medPersonIdent(new PersonIdent("123123123"))
            .medFødselsdato(FØDSELSDATO)
            .medNavn(PRIVATPERSON_NAVN)
            .bygg();
    }

    @Test
    public void skal_lage_tekst_for_arbeidsgiver_privatperson_uten_arbref() {
        // Act
        String arbeidsgiverNavn = arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver.person(AKTØR_ID), List.of());

        // Assert
        assertThat(arbeidsgiverNavn).isEqualTo("Mikke Mus (01.01.2000)");
    }

    @Test
    public void skal_lage_tekst_for_arbeidsgiver_privatperson_med_arbref() {
        // Act
        String arbeidsgiverNavn = arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(
            Arbeidsgiver.person(AKTØR_ID), ARBEIDSFORHOLD_REF, List.of());

        // Assert
        assertThat(arbeidsgiverNavn).isEqualTo("Mikke Mus (01.01.2000) ..." + SUFFIX);
    }

    @Test
    public void skal_lage_tekst_for_arbeidsgiver_virksomhet_uten_arbref() {
        // Act
        String arbeidsgiverNavn = arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver.virksomhet(ORGNR), List.of());

        // Assert
        assertThat(arbeidsgiverNavn).isEqualTo("Andeby Bank (999888777)");
    }

    @Test
    public void skal_lage_tekst_for_arbeidsgiver_virksomhet_med_arbref() {
        // Act
        String arbeidsgiverNavn = arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(
            Arbeidsgiver.virksomhet(ORGNR), ARBEIDSFORHOLD_REF, List.of());

        // Assert
        assertThat(arbeidsgiverNavn).isEqualTo("Andeby Bank (999888777) ..." + SUFFIX);
    }

    @Test
    public void skal_lage_tekst_for_arbeidsgiver_virksomhet_med_arbref_null() {
        // Act
        String arbeidsgiverNavn = arbeidsgiverHistorikkinnslagTjeneste.lagArbeidsgiverHistorikkinnslagTekst(Arbeidsgiver.virksomhet(ORGNR),
            InternArbeidsforholdRef.nullRef(), List.of());

        // Assert
        assertThat(arbeidsgiverNavn).isEqualTo("Andeby Bank (999888777)");
    }

    private static Virksomhet lagVirksomhet(String orgnr) {
        Virksomhet.Builder b = new Virksomhet.Builder();
        b.medOrgnr(orgnr).medNavn(ORG_NAVN);
        return b.build();
    }

}
