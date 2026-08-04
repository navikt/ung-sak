package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.KontrollerInntektEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.maksdato.MaksdatoEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.ProgramperiodeendringEtterlysningTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UngEtterlysningOppretterTest {

    private static final Long BEHANDLING_ID = 123L;

    private final KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste = mock(KontrollerInntektEtterlysningTjeneste.class);
    private final ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste = mock(ProgramperiodeendringEtterlysningTjeneste.class);
    private final MaksdatoEtterlysningTjeneste maksdatoEtterlysningTjeneste = mock(MaksdatoEtterlysningTjeneste.class);
    private final OpphevelseAvOpphørEtterlysningHåndterer opphevelseAvOpphørEtterlysningHåndterer = mock(OpphevelseAvOpphørEtterlysningHåndterer.class);
    private final BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);

    private final Behandling behandling = mock(Behandling.class);
    private final BehandlingReferanse behandlingReferanse = mock(BehandlingReferanse.class);

    private UngEtterlysningOppretter oppretter;

    @BeforeEach
    void setUp() {
        oppretter = new UngEtterlysningOppretter(kontrollerInntektEtterlysningTjeneste, programperiodeendringEtterlysningTjeneste, maksdatoEtterlysningTjeneste,
            opphevelseAvOpphørEtterlysningHåndterer, behandlingRepository);
        when(behandlingReferanse.getBehandlingId()).thenReturn(BEHANDLING_ID);
        when(behandlingRepository.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
    }

    @Test
    void skalIkkeTriggeInntektskontrollEllerProgramperiodeendring_forRentVarselOpphørVedMaksdatoBehandlingsflyt() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO));

        oppretter.opprettEtterlysninger(behandlingReferanse);

        verify(maksdatoEtterlysningTjeneste).opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste, never()).opprettEtterlysninger(behandlingReferanse);
        verify(programperiodeendringEtterlysningTjeneste, never()).opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
    }

    @Test
    void skalTriggeAlleEtterlysninger_nårBehandlingHarAndreÅrsaker() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(
            BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO,
            BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));

        oppretter.opprettEtterlysninger(behandlingReferanse);

        verify(kontrollerInntektEtterlysningTjeneste).opprettEtterlysninger(behandlingReferanse);
        verify(programperiodeendringEtterlysningTjeneste).opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
        verify(maksdatoEtterlysningTjeneste).opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
    }

    @Test
    void skalDelegereTilOpphevelseAvOpphørHåndterer_nårBehandlingenHarOpphørOpphevetÅrsak() {
        var årsaker = List.of(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM);
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(årsaker);

        oppretter.opprettEtterlysninger(behandlingReferanse);

        verify(opphevelseAvOpphørEtterlysningHåndterer).håndter(behandlingReferanse, årsaker);
        verify(kontrollerInntektEtterlysningTjeneste, never()).opprettEtterlysninger(behandlingReferanse);
        verify(programperiodeendringEtterlysningTjeneste, never()).opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
        verify(maksdatoEtterlysningTjeneste, never()).opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
    }

    @Test
    void skalDelegereTilOpphevelseAvOpphørHåndterer_nårOpphørOpphevetErSlåttSammenMedUtdatertOpphørÅrsak() {
        // Reproduserer sammenslåing av hendelser, se OpphevelseAvOpphørEtterlysningHåndterer for detaljer.
        var årsaker = List.of(
            BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM,
            BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM);
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(årsaker);

        oppretter.opprettEtterlysninger(behandlingReferanse);

        verify(opphevelseAvOpphørEtterlysningHåndterer).håndter(behandlingReferanse, årsaker);
        verify(kontrollerInntektEtterlysningTjeneste, never()).opprettEtterlysninger(behandlingReferanse);
        verify(programperiodeendringEtterlysningTjeneste, never()).opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
        verify(maksdatoEtterlysningTjeneste, never()).opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
    }
}
