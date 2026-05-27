package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.KontrollerInntektEtterlysningTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.AutomatiskOpphørEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.ProgramperiodeendringEtterlysningTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM;
import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM;
import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_VARSEL_AUTOMATISK_OPPHOR;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tester for UngEtterlysningsorkestrerserTjeneste.
 *
 * Verifiserer at riktig etterlysnings-tjeneste kalles basert på behandlingsårsaker
 * for alle fire scenarioer:
 * - Scenario 1: Varsel om automatisk opphør alene
 * - Scenario 2: Varsel overstyrt av forlenget periode
 * - Scenario 3: Varsel overstyrt av manuelt opphør
 * - Scenario 0: Normal flyt (ingen varsel-årsak)
 */
class UngEtterlysningsorkestrerserTjenesteTest {

    private UngEtterlysningsorkestrerserTjeneste tjeneste;

    @Mock
    private AutomatiskOpphørEtterlysningTjeneste automatiskOpphørEtterlysningTjeneste;

    @Mock
    private KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste;

    @Mock
    private ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste;

    private BehandlingReferanse behandlingReferanse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tjeneste = new UngEtterlysningsorkestrerserTjeneste(
            automatiskOpphørEtterlysningTjeneste,
            kontrollerInntektEtterlysningTjeneste,
            programperiodeendringEtterlysningTjeneste
        );
        behandlingReferanse = BehandlingReferanse.fra(
            FagsakYtelseType.UNGDOMSYTELSE,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.INNVILGET,
            new AktørId("1234567890123"),
            new Saksnummer("SAK123"),
            1L,
            1L,
            UUID.randomUUID(),
            Optional.empty(),
            BehandlingStatus.OPPRETTET,
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(260))
        );
    }

    @Test
    void skal_opprette_automatisk_opphør_etterlysning_når_kun_varsel_årsak() {
        // Scenario 1: Kun RE_VARSEL_AUTOMATISK_OPPHOR
        Collection<BehandlingÅrsakType> årsaker = Arrays.asList(RE_VARSEL_AUTOMATISK_OPPHOR);

        tjeneste.orkestrerEtterlysninger(behandlingReferanse, årsaker);

        verify(automatiskOpphørEtterlysningTjeneste).opprettEtterlysningForAutomatiskOpphør(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste, never()).opprettEtterlysninger(behandlingReferanse);
        verify(programperiodeendringEtterlysningTjeneste, never()).opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
    }

    @Test
    void skal_avbryte_varsel_og_kjøre_normal_flyt_når_forlenget_periode_overstyrer() {
        // Scenario 2: RE_VARSEL_AUTOMATISK_OPPHOR + RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM
        Collection<BehandlingÅrsakType> årsaker = Arrays.asList(RE_VARSEL_AUTOMATISK_OPPHOR, RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM);

        tjeneste.orkestrerEtterlysninger(behandlingReferanse, årsaker);

        verify(automatiskOpphørEtterlysningTjeneste).avbrytEtterlysningForAutomatiskOpphør(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste).opprettEtterlysninger(behandlingReferanse);
        verify(programperiodeendringEtterlysningTjeneste, never()).opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
    }

    @Test
    void skal_avbryte_varsel_og_kjøre_normal_flyt_når_manuelt_opphør_overstyrer() {
        // Scenario 3: RE_VARSEL_AUTOMATISK_OPPHOR + RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM
        Collection<BehandlingÅrsakType> årsaker = Arrays.asList(RE_VARSEL_AUTOMATISK_OPPHOR, RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);

        tjeneste.orkestrerEtterlysninger(behandlingReferanse, årsaker);

        verify(automatiskOpphørEtterlysningTjeneste).avbrytEtterlysningForAutomatiskOpphør(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste).opprettEtterlysninger(behandlingReferanse);
        verify(programperiodeendringEtterlysningTjeneste).opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
    }

    @Test
    void skal_kjøre_normal_flyt_uten_varsel_årsak() {
        // Scenario 0: Ingen varsel-årsak
        Collection<BehandlingÅrsakType> årsaker = Arrays.asList();

        tjeneste.orkestrerEtterlysninger(behandlingReferanse, årsaker);

        verify(automatiskOpphørEtterlysningTjeneste, never()).opprettEtterlysningForAutomatiskOpphør(behandlingReferanse);
        verify(automatiskOpphørEtterlysningTjeneste, never()).avbrytEtterlysningForAutomatiskOpphør(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste).opprettEtterlysninger(behandlingReferanse);
        verify(programperiodeendringEtterlysningTjeneste).opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
    }

    @Test
    void skal_ikke_opprette_programperiodeendring_når_forlenget_periode_uten_varsel_årsak() {
        // Edge case: Forlenget periode alene (ingen varsel-årsak)
        Collection<BehandlingÅrsakType> årsaker = Arrays.asList(RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM);

        tjeneste.orkestrerEtterlysninger(behandlingReferanse, årsaker);

        verify(automatiskOpphørEtterlysningTjeneste, never()).opprettEtterlysningForAutomatiskOpphør(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste).opprettEtterlysninger(behandlingReferanse);
        verify(programperiodeendringEtterlysningTjeneste, never()).opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
    }
}

