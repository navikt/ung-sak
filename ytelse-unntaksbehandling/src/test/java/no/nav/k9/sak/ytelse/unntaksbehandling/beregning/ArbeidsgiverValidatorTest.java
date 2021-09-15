package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.felles.exception.FunksjonellException;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.OrgNummer;

public class ArbeidsgiverValidatorTest {

    private static final OrgNummer GYLDIG_ORGNR = new OrgNummer("910909088");
    private static final OrgNummer UKJENT_ORGNR = new OrgNummer("979312059");
    private static final OrgNummer ORGNR_VIRKSOMHETSTJENESTE_FEILER = new OrgNummer("890484832");

    private static final AktørId AKTØRID_IDENT1 = new AktørId("1234567890123");
    private static final AktørId AKTØRID_IDENT2 = new AktørId("1234567890124");

    private ArbeidsgiverValidator arbeidsgiverValidator;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    @BeforeEach
    public void setUp() {
        arbeidsgiverTjeneste = mock(ArbeidsgiverTjeneste.class);
        arbeidsgiverValidator = new ArbeidsgiverValidator(arbeidsgiverTjeneste);
    }

    @Test
    public void valider_orgnr_som_arbeidsforhold() {
        ArbeidsgiverOpplysninger opplysningerForGyldigOrgnr = new ArbeidsgiverOpplysninger(GYLDIG_ORGNR.getOrgNummer(), "Bedrift 1");
        when(arbeidsgiverTjeneste.hent(eq(Arbeidsgiver.virksomhet(GYLDIG_ORGNR)))).thenReturn(opplysningerForGyldigOrgnr);
        when(arbeidsgiverTjeneste.hent(eq(Arbeidsgiver.virksomhet(UKJENT_ORGNR)))).thenReturn(null);
        when(arbeidsgiverTjeneste.hent(eq(Arbeidsgiver.virksomhet(ORGNR_VIRKSOMHETSTJENESTE_FEILER)))).thenThrow(new RuntimeException("Oppslag mot virksomhetstjeneste feilet"));

        assertDoesNotThrow(() -> arbeidsgiverValidator.validerArbeidsgiver(GYLDIG_ORGNR, null, null));
        assertThrows(FunksjonellException.class, () -> arbeidsgiverValidator.validerArbeidsgiver(UKJENT_ORGNR, null, null));
        assertThrows(FunksjonellException.class, () -> arbeidsgiverValidator.validerArbeidsgiver(ORGNR_VIRKSOMHETSTJENESTE_FEILER, null, null));
    }

    @Test
    public void valider_orgnr_som_aktørid() {
        AktørId fagsakAktørId = AKTØRID_IDENT1;

        assertDoesNotThrow(() -> arbeidsgiverValidator.validerArbeidsgiver(null, AKTØRID_IDENT2, fagsakAktørId));
        assertThrows(FunksjonellException.class, () -> arbeidsgiverValidator.validerArbeidsgiver(null, AKTØRID_IDENT1, fagsakAktørId));
    }

}
