package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.vedtak.exception.FunksjonellException;

public class ArbeidsgiverValidatorTest {

    private static final String GYLDIG_ORGNR = "910909088";
    private static final String UGYLDIG_ORGNR = "123456789";
    private static final String UKJENT_ORGNR = "979312059";
    private static final String ORGNR_VIRKSOMHETSTJENESTE_FEILER = "890484832";

    private static final String AKTØRID_IDENT1 = "1234567890123";
    private static final String AKTØRID_IDENT2 = "1234567890124";

    private ArbeidsgiverValidator arbeidsgiverValidator;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    @BeforeEach
    public void setUp() {
        arbeidsgiverTjeneste = mock(ArbeidsgiverTjeneste.class);
        arbeidsgiverValidator = new ArbeidsgiverValidator(arbeidsgiverTjeneste);
    }

    @Test
    public void valider_orgnr_som_arbeidsforhold() {
        ArbeidsgiverOpplysninger opplysningerForGyldigOrgnr = new ArbeidsgiverOpplysninger(GYLDIG_ORGNR, "Bedrift 1");
        when(arbeidsgiverTjeneste.hent(eq(Arbeidsgiver.virksomhet(GYLDIG_ORGNR)))).thenReturn(opplysningerForGyldigOrgnr);
        when(arbeidsgiverTjeneste.hent(eq(Arbeidsgiver.virksomhet(UKJENT_ORGNR)))).thenReturn(null);
        when(arbeidsgiverTjeneste.hent(eq(Arbeidsgiver.virksomhet(ORGNR_VIRKSOMHETSTJENESTE_FEILER)))).thenThrow(new RuntimeException("Oppslag mot virksomhetstjeneste feilet"));

        assertDoesNotThrow(() -> arbeidsgiverValidator.validerArbeidsgiver(GYLDIG_ORGNR, null));
        assertThrows(FunksjonellException.class, () -> arbeidsgiverValidator.validerArbeidsgiver(UGYLDIG_ORGNR, null));
        assertThrows(FunksjonellException.class, () -> arbeidsgiverValidator.validerArbeidsgiver(UKJENT_ORGNR, null));
        assertThrows(FunksjonellException.class, () -> arbeidsgiverValidator.validerArbeidsgiver(ORGNR_VIRKSOMHETSTJENESTE_FEILER, null));
    }

    @Test
    public void valider_orgnr_som_aktørid() {
        AktørId fagsakAktørId = new AktørId(AKTØRID_IDENT1);

        assertDoesNotThrow(() -> arbeidsgiverValidator.validerArbeidsgiver(AKTØRID_IDENT1, fagsakAktørId));
        assertThrows(FunksjonellException.class, () -> arbeidsgiverValidator.validerArbeidsgiver(AKTØRID_IDENT2, fagsakAktørId));
    }

}
