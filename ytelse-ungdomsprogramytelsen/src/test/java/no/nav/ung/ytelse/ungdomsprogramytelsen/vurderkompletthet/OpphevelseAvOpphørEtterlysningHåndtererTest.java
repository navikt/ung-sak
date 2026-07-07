package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.KontrollerInntektEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.maksdato.MaksdatoEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.ProgramperiodeendringEtterlysningTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OpphevelseAvOpphørEtterlysningHåndtererTest {

    private final KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste = mock(KontrollerInntektEtterlysningTjeneste.class);
    private final ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste = mock(ProgramperiodeendringEtterlysningTjeneste.class);
    private final MaksdatoEtterlysningTjeneste maksdatoEtterlysningTjeneste = mock(MaksdatoEtterlysningTjeneste.class);

    private final BehandlingReferanse behandlingReferanse = mock(BehandlingReferanse.class);

    private OpphevelseAvOpphørEtterlysningHåndterer håndterer;

    @BeforeEach
    void setUp() {
        håndterer = new OpphevelseAvOpphørEtterlysningHåndterer(kontrollerInntektEtterlysningTjeneste, programperiodeendringEtterlysningTjeneste, maksdatoEtterlysningTjeneste);
    }

    @Test
    void skalAvbryteVentendeEtterlysningOgIkkeOpprettteNoenNye_nårOpphørOpphevetErEnesteÅrsak() {
        håndterer.håndter(behandlingReferanse, List.of(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM));

        verify(programperiodeendringEtterlysningTjeneste).avbrytVentendeSluttdatoEtterlysninger(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste, never()).opprettEtterlysninger(behandlingReferanse);
        verify(maksdatoEtterlysningTjeneste, never()).opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
    }

    @Test
    void skalAvbryteVentendeEtterlysningOgIkkeOpprettteNoenNye_nårOpphørOpphevetErSlåttSammenMedUtdatertOpphørÅrsak() {
        // Reproduserer sammenslåing av hendelser: opphevOpphør-hendelsen slås sammen med en fortsatt åpen behandling
        // som venter på bekreftelse av det (nå opphevede) opphøret. Den utdaterte RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM
        // skal ikke føre til at det etterlyses noe, og en eventuell ventende etterlysning skal avbrytes.
        håndterer.håndter(behandlingReferanse, List.of(
            BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM,
            BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM));

        verify(programperiodeendringEtterlysningTjeneste).avbrytVentendeSluttdatoEtterlysninger(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste, never()).opprettEtterlysninger(behandlingReferanse);
        verify(maksdatoEtterlysningTjeneste, never()).opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
    }

    @Test
    void skalAvbryteVentendeEtterlysningOgTriggeInntektskontrollOgMaksdato_nårOpphørOpphevetHarTilleggsårsaker() {
        håndterer.håndter(behandlingReferanse, List.of(
            BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM,
            BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));

        verify(programperiodeendringEtterlysningTjeneste).avbrytVentendeSluttdatoEtterlysninger(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste).opprettEtterlysninger(behandlingReferanse);
        verify(maksdatoEtterlysningTjeneste).opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
    }
}
