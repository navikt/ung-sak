package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.KontrollerInntektEtterlysningTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.OpphørVedMaksdatoEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.ProgramperiodeendringEtterlysningTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.*;

import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM;
import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM;
import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO;
import static org.mockito.Mockito.*;

/**
 * Tester for UngEtterlysningsOrkestreringTjeneste.
 *
 * Verifiserer at riktig etterlysnings-tjeneste kalles basert på grunnlagstilstand
 * (ikke kun behandlingsårsaker). Grunnlaget er kilde til sannhet for forlenget periode og opphør.
 *
 * Scenarioer:
 * - Varsel om opphør ved maksdato alene med åpen periode → opprett varsel
 * - Varsel om opphør ved maksdato men grunnlag viser forlenget → avbryt varsel, kjør normal
 * - Varsel om opphør ved maksdato men grunnlag viser opphør → avbryt varsel, kjør normal
 * - Normal flyt (ingen varsel-årsak) → inntektskontroll + programperiodeendring
 */
class UngEtterlysningsOrkestreringTjenesteTest {

    private UngEtterlysningsOrkestreringTjeneste tjeneste;

    @Mock
    private OpphørVedMaksdatoEtterlysningTjeneste opphørVedMaksdatoEtterlysningTjeneste;

    @Mock
    private KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste;

    @Mock
    private ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste;

    @Mock
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    private BehandlingReferanse behandlingReferanse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tjeneste = new UngEtterlysningsOrkestreringTjeneste(
            opphørVedMaksdatoEtterlysningTjeneste,
            kontrollerInntektEtterlysningTjeneste,
            programperiodeendringEtterlysningTjeneste,
            ungdomsprogramPeriodeRepository
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
    void skal_opprette_opphør_ved_maksdato_etterlysning_når_varsel_årsak_og_grunnlag_viser_åpen_periode() {
        // Grunnlag: åpen periode (tom=TIDENES_ENDE), ikke forlenget
        mockGrunnlag(false, Tid.TIDENES_ENDE);

        Collection<BehandlingÅrsakType> årsaker = Arrays.asList(RE_VARSEL_OPPHOR_VED_MAKSDATO);

        tjeneste.orkestrerEtterlysninger(behandlingReferanse, årsaker);

        verify(opphørVedMaksdatoEtterlysningTjeneste).opprettEtterlysningForOpphørVedMaksdato(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste, never()).opprettEtterlysninger(behandlingReferanse);
        verify(programperiodeendringEtterlysningTjeneste, never()).opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
    }

    @Test
    void skal_avbryte_varsel_når_grunnlag_viser_forlenget_periode() {
        // Grunnlag: forlenget periode, åpen (tom=TIDENES_ENDE)
        mockGrunnlag(true, Tid.TIDENES_ENDE);

        Collection<BehandlingÅrsakType> årsaker = Arrays.asList(RE_VARSEL_OPPHOR_VED_MAKSDATO, RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM);

        tjeneste.orkestrerEtterlysninger(behandlingReferanse, årsaker);

        verify(opphørVedMaksdatoEtterlysningTjeneste).avbrytEtterlysningForOpphørVedMaksdato(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste).opprettEtterlysninger(behandlingReferanse);
        verify(programperiodeendringEtterlysningTjeneste, never()).opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
    }

    @Test
    void skal_avbryte_varsel_når_grunnlag_viser_opphør() {
        // Grunnlag: opphørt periode (tom != TIDENES_ENDE), ikke forlenget
        mockGrunnlag(false, LocalDate.now().plusDays(30));

        Collection<BehandlingÅrsakType> årsaker = Arrays.asList(RE_VARSEL_OPPHOR_VED_MAKSDATO, RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);

        tjeneste.orkestrerEtterlysninger(behandlingReferanse, årsaker);

        verify(opphørVedMaksdatoEtterlysningTjeneste).avbrytEtterlysningForOpphørVedMaksdato(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste).opprettEtterlysninger(behandlingReferanse);
        verify(programperiodeendringEtterlysningTjeneste).opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
    }

    @Test
    void skal_avbryte_varsel_basert_på_grunnlag_selv_om_årsak_kun_er_varsel() {
        // Scenario: Prosesstask-rekkefølge-problem — kun RE_VARSEL årsak, men grunnlag viser forlenget
        // Dette er kjerneproblemet vi løser: grunnlaget har allerede blitt oppdatert av en annen prosesstask
        mockGrunnlag(true, Tid.TIDENES_ENDE);

        Collection<BehandlingÅrsakType> årsaker = Arrays.asList(RE_VARSEL_OPPHOR_VED_MAKSDATO);

        tjeneste.orkestrerEtterlysninger(behandlingReferanse, årsaker);

        // Selv om årsaken sier varsel, ser grunnlaget at perioden er forlenget → avbryt
        verify(opphørVedMaksdatoEtterlysningTjeneste).avbrytEtterlysningForOpphørVedMaksdato(behandlingReferanse);
        verify(opphørVedMaksdatoEtterlysningTjeneste, never()).opprettEtterlysningForOpphørVedMaksdato(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste).opprettEtterlysninger(behandlingReferanse);
    }

    @Test
    void skal_kjøre_normal_flyt_uten_varsel_årsak() {
        // Grunnlag: åpen periode, ikke forlenget
        mockGrunnlag(false, Tid.TIDENES_ENDE);

        Collection<BehandlingÅrsakType> årsaker = List.of();

        tjeneste.orkestrerEtterlysninger(behandlingReferanse, årsaker);

        verify(opphørVedMaksdatoEtterlysningTjeneste, never()).opprettEtterlysningForOpphørVedMaksdato(behandlingReferanse);
        verify(opphørVedMaksdatoEtterlysningTjeneste, never()).avbrytEtterlysningForOpphørVedMaksdato(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste).opprettEtterlysninger(behandlingReferanse);
        verify(programperiodeendringEtterlysningTjeneste).opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
    }

    @Test
    void skal_ikke_opprette_programperiodeendring_når_grunnlag_viser_forlenget_periode() {
        // Grunnlag: forlenget, åpen periode
        mockGrunnlag(true, Tid.TIDENES_ENDE);

        Collection<BehandlingÅrsakType> årsaker = Arrays.asList(RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM);

        tjeneste.orkestrerEtterlysninger(behandlingReferanse, årsaker);

        verify(opphørVedMaksdatoEtterlysningTjeneste, never()).opprettEtterlysningForOpphørVedMaksdato(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste).opprettEtterlysninger(behandlingReferanse);
        verify(programperiodeendringEtterlysningTjeneste, never()).opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
    }

    @Test
    void skal_opprette_programperiodeendring_når_grunnlag_viser_forlenget_periode_med_opphør() {
        // Grunnlag: forlenget OG opphørt (tom != TIDENES_ENDE) — opphør innenfor forlenget periode
        mockGrunnlag(true, LocalDate.now().plusDays(30));

        Collection<BehandlingÅrsakType> årsaker = Arrays.asList(RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);

        tjeneste.orkestrerEtterlysninger(behandlingReferanse, årsaker);

        verify(opphørVedMaksdatoEtterlysningTjeneste, never()).opprettEtterlysningForOpphørVedMaksdato(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste).opprettEtterlysninger(behandlingReferanse);
        verify(programperiodeendringEtterlysningTjeneste).opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
    }

    @Test
    void skal_avbryte_varsel_når_grunnlag_viser_opphør_uten_opphør_årsak() {
        // Scenario: Kun RE_VARSEL årsak, men grunnlag viser at perioden allerede er opphørt
        mockGrunnlag(false, LocalDate.now().plusDays(30));

        Collection<BehandlingÅrsakType> årsaker = Arrays.asList(RE_VARSEL_OPPHOR_VED_MAKSDATO);

        tjeneste.orkestrerEtterlysninger(behandlingReferanse, årsaker);

        verify(opphørVedMaksdatoEtterlysningTjeneste).avbrytEtterlysningForOpphørVedMaksdato(behandlingReferanse);
        verify(opphørVedMaksdatoEtterlysningTjeneste, never()).opprettEtterlysningForOpphørVedMaksdato(behandlingReferanse);
        verify(kontrollerInntektEtterlysningTjeneste).opprettEtterlysninger(behandlingReferanse);
    }

    private void mockGrunnlag(boolean harForlengetPeriode, LocalDate periodeTom) {
        var grunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        var perioder = mock(UngdomsprogramPerioder.class);
        var periode = new UngdomsprogramPeriode(LocalDate.now(), periodeTom);

        when(grunnlag.harForlengetPeriode()).thenReturn(harForlengetPeriode);
        when(grunnlag.getUngdomsprogramPerioder()).thenReturn(perioder);
        when(perioder.getPerioder()).thenReturn(Set.of(periode));
        when(ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingReferanse.getBehandlingId()))
            .thenReturn(Optional.of(grunnlag));
    }
}
