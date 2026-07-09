package no.nav.ung.sak.web.app.tjenester.behandling;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingAnsvarlig;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import no.nav.ung.sak.kontrakt.behandling.BehandlingDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingVisningsnavn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BehandlingDtoUtilTest {

    private static final Long ORIGINAL_BEHANDLING_ID = 999L;

    private OrganisasjonsEnhet organisasjonsEnhet = new OrganisasjonsEnhet("4833", "NAV Familie- og pensjonsytelser");

    Behandling behandling = Mockito.mock(Behandling.class);
    BehandlingAnsvarlig behandlingAnsvarlig = new BehandlingAnsvarlig(1L, BehandlingDel.SENTRAL);
    Map<BehandlingDel, BehandlingAnsvarlig> behandlingAnsvarlige = Map.of(BehandlingDel.SENTRAL, behandlingAnsvarlig);
    UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository = mock(UngdomsprogramPeriodeRepository.class);


    @BeforeEach
    void setUp() {
        behandlingAnsvarlig.setBehandlendeEnhet(organisasjonsEnhet);
    }

    /** Simulerer at originalbehandlingen faktisk hadde en lukket sluttdato, dvs. at opphøret ble reelt vedtatt. */
    private void mockOpphørVarFaktiskIverksatt() {
        when(behandling.getOriginalBehandlingId()).thenReturn(Optional.of(ORIGINAL_BEHANDLING_ID));
        var grunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        var perioder = new UngdomsprogramPerioder(Set.of(new UngdomsprogramPeriode(LocalDate.now().minusYears(1), LocalDate.now().minusDays(1))));
        when(grunnlag.getUngdomsprogramPerioder()).thenReturn(perioder);
        when(ungdomsprogramPeriodeRepository.hentGrunnlag(ORIGINAL_BEHANDLING_ID)).thenReturn(Optional.of(grunnlag));
    }

    @Test
    void forventer_ingen_relevant_behandlingsårsak_når_ingen_årsaker() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of());

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.INGEN_RELEVANT_BEHANDLINGÅRSAK);
    }

    @Test
    void forventer_ingen_relevant_behandlingsårsak_når_kun_uttalelse_fra_bruker() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.INGEN_RELEVANT_BEHANDLINGÅRSAK);
    }

    @Test
    void forventer_ingen_relevant_behandlingsårsak_når_kun_re_registeropplysning() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_REGISTEROPPLYSNING));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.INGEN_RELEVANT_BEHANDLINGÅRSAK);
    }

    @Test
    void forventer_kontroll_av_inntekt_for_re_kontroll_register_inntekt() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.KONTROLL_AV_INNTEKT);
    }

    @Test
    void forventer_kontroll_av_inntekt_for_re_rapportering_inntekt() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.KONTROLL_AV_INNTEKT);
    }

    @Test
    void forventer_ingen_relevant_behandlingsårsak_når_kun_re_inntektsopplysning() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_INNTEKTSOPPLYSNING));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.INGEN_RELEVANT_BEHANDLINGÅRSAK);
    }

    @Test
    void forventer_kontroll_av_inntekt_for_kombinasjon_av_inntektsårsaker() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(
            BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT,
            BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT
        ));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.KONTROLL_AV_INNTEKT);
    }

    @Test
    void forventer_kontroll_av_inntekt_når_re_inntektsopplysning_kombineres_med_andre_inntektsårsaker() {
        Behandling behandling = mock(Behandling.class);
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(
            BehandlingÅrsakType.RE_INNTEKTSOPPLYSNING,
            BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT,
            BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT
        ));

        behandlingAnsvarlig.setBehandlendeEnhet(organisasjonsEnhet);

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.KONTROLL_AV_INNTEKT);
    }

    @Test
    void forventer_beregning_av_høy_sats_for_re_trigger_beregning_høy_sats() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.BEREGNING_AV_HØY_SATS);
    }

    @Test
    void forventer_endring_av_barnetillegg_for_hendelse_fødsel() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_HENDELSE_FØDSEL));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.ENDRING_AV_BARNETILLEGG);
    }

    @Test
    void forventer_endring_av_barnetillegg_for_hendelse_død_barn() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_HENDELSE_DØD_BARN));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.ENDRING_AV_BARNETILLEGG);
    }

    @Test
    void forventer_endring_av_barnetillegg_for_kombinasjon_av_barn_hendelser() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(
            BehandlingÅrsakType.RE_HENDELSE_FØDSEL,
            BehandlingÅrsakType.RE_HENDELSE_DØD_BARN
        ));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.ENDRING_AV_BARNETILLEGG);
    }

    @Test
    void forventer_brukers_dødsfall_for_hendelse_død_forelder() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.BRUKERS_DØDSFALL);
    }

    @Test
    void forventer_ungdomsprogramendring_for_hendelse_endret_startdato_ungdomsprogram() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.UNGDOMSPROGRAMENDRING);
    }

    @Test
    void forventer_ungdomsprogramendring_for_hendelse_opphør_ungdomsprogram() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.UNGDOMSPROGRAMENDRING);
    }

    @Test
    void forventer_ungdomsprogramendring_for_kombinasjon_av_ungdomsprogram_hendelser() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(
            BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM,
            BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM
        ));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.UNGDOMSPROGRAMENDRING);
    }

    @Test
    void forventer_opphør_ved_maksdato_for_re_varsel_opphor_ved_maksdato() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.OPPHØR_VED_MAKSDATO);
    }

    @Test
    void forventer_opphør_opphevet_for_hendelse_opphør_opphevet_ungdomsprogram() {
        mockOpphørVarFaktiskIverksatt();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.OPPHØR_OPPHEVET);
    }

    @Test
    void forventer_opphør_opphevet_når_slått_sammen_med_utdatert_opphør_årsak() {
        // Reproduserer at opphevOpphør-hendelsen kan bli slått sammen med en fortsatt åpen behandling
        // som venter på bekreftelse av det (nå opphevede) opphøret, jf. UngEtterlysningOppretter.harKunOpphørsÅrsaker.
        mockOpphørVarFaktiskIverksatt();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(
            BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM,
            BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.OPPHØR_OPPHEVET);
    }

    @Test
    void forventer_opphør_mottatt_og_opphevet_i_samme_behandling_når_opphøret_aldri_ble_iverksatt() {
        // Opphør og opphevelse slått sammen på samme, fortsatt åpne behandling — opphøret ble aldri
        // vedtatt (originalbehandlingen har fortsatt åpen sluttdato, jf. OpphørOpphevetUtleder).
        when(behandling.getOriginalBehandlingId()).thenReturn(Optional.of(ORIGINAL_BEHANDLING_ID));
        var grunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        var perioder = new UngdomsprogramPerioder(Set.of(new UngdomsprogramPeriode(LocalDate.now().minusYears(1), no.nav.k9.felles.konfigurasjon.konfig.Tid.TIDENES_ENDE)));
        when(grunnlag.getUngdomsprogramPerioder()).thenReturn(perioder);
        when(ungdomsprogramPeriodeRepository.hentGrunnlag(ORIGINAL_BEHANDLING_ID)).thenReturn(Optional.of(grunnlag));

        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(
            BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM,
            BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.OPPHØR_MOTTATT_OG_ANNULLERT_I_SAMME_BEHANDLING);
    }

    @Test
    void forventer_flere_behandlingårsaker_for_blandede_årsaker() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(
            BehandlingÅrsakType.RE_HENDELSE_FØDSEL,
            BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT
        ));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.FLERE_BEHANDLINGÅRSAKER);
    }

    @Test
    void filtrerer_bort_uttalelse_fra_bruker_og_re_registeropplysning() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(
            BehandlingÅrsakType.UTTALELSE_FRA_BRUKER,
            BehandlingÅrsakType.RE_REGISTEROPPLYSNING,
            BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT
        ));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, behandlingAnsvarlige, dto, null, false, ungdomsprogramPeriodeRepository);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.KONTROLL_AV_INNTEKT);
    }

    private Behandling mockBehandling() {
        Behandling behandling = mock(Behandling.class);
        return behandling;
    }

    private BehandlingAnsvarlig mockBehandlingAnsvarlig() {
        BehandlingAnsvarlig behandlingAnsvarlig = mock(BehandlingAnsvarlig.class);
        when(behandlingAnsvarlig.getBehandlendeOrganisasjonsEnhet()).thenReturn(organisasjonsEnhet);
        return behandlingAnsvarlig;
    }
}
