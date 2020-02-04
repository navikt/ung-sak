package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjenesteImpl;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;

public class ArbeidsgiverHistorikkinnslagTjenesteImplTest {

    private static final String PRIVATPERSON_NAVN = "Mikke Mus";
    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final String ORGNR = "999888777";
    private static final String ORG_NAVN = "Andeby Bank";
    private static final Virksomhet VIRKSOMHET = lagVirksomhet(ORGNR);
    private static final LocalDate FØDSELSDATO = LocalDate.of(2000, 1, 1);
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_REF = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final String SUFFIX = ARBEIDSFORHOLD_REF.getReferanse().substring(ARBEIDSFORHOLD_REF.getReferanse().length()-4);

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    @Mock
    private PersonIdentTjeneste tpsTjeneste;

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;

    @Before
    public void setup() {
        when(tpsTjeneste.hentBrukerForAktør(any(AktørId.class))).thenReturn(Optional.of(lagPersoninfo()));

        var virksomhetTjeneste = mock(VirksomhetTjeneste.class);
       // when(virksomhetTjeneste.hentVirksomhet(any())).thenReturn(Optional.of(VIRKSOMHET));
        when(virksomhetTjeneste.hentOgLagreOrganisasjon(any())).thenReturn(VIRKSOMHET);
        ArbeidsgiverTjeneste arbeidsgiverTjeneste = new ArbeidsgiverTjenesteImpl(tpsTjeneste, virksomhetTjeneste);
        arbeidsgiverHistorikkinnslagTjeneste = new ArbeidsgiverHistorikkinnslag(arbeidsgiverTjeneste);
    }

    private Personinfo lagPersoninfo() {
        return new Personinfo.Builder()
            .medAktørId(AKTØR_ID)
            .medPersonIdent(new PersonIdent("123123123"))
            .medKjønn(NavBrukerKjønn.MANN)
            .medFødselsdato(FØDSELSDATO)
            .medNavn(PRIVATPERSON_NAVN)
            .build();
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
        VirksomhetEntitet.Builder b = new VirksomhetEntitet.Builder();
        b.medOrgnr(orgnr).medNavn(ORG_NAVN);
        return b.build();
    }

}
