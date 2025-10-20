package no.nav.ung.sak.web.app.tjenester.behandling;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.kontrakt.behandling.BehandlingDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingVisningsnavn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BehandlingDtoUtilTest {

    private OrganisasjonsEnhet organisasjonsEnhet;

    @BeforeEach
    void setUp() {
        organisasjonsEnhet = mock(OrganisasjonsEnhet.class);
        when(organisasjonsEnhet.getEnhetId()).thenReturn("4833");
        when(organisasjonsEnhet.getEnhetNavn()).thenReturn("NAV Familie- og pensjonsytelser");
    }

    @Test
    void forventer_ingen_relevant_behandlingsårsak_når_ingen_årsaker() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of());

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.INGEN_RELEVANT_BEHANDLINGÅRSAK);
    }

    @Test
    void forventer_ingen_relevant_behandlingsårsak_når_kun_uttalelse_fra_bruker() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.INGEN_RELEVANT_BEHANDLINGÅRSAK);
    }

    @Test
    void forventer_ingen_relevant_behandlingsårsak_når_kun_re_registeropplysning() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_REGISTEROPPLYSNING));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.INGEN_RELEVANT_BEHANDLINGÅRSAK);
    }

    @Test
    void forventer_kontroll_av_inntekt_for_re_kontroll_register_inntekt() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.KONTROLL_AV_INNTEKT);
    }

    @Test
    void forventer_kontroll_av_inntekt_for_re_rapportering_inntekt() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.KONTROLL_AV_INNTEKT);
    }

    @Test
    void forventer_ingen_relevant_behandlingsårsak_når_kun_re_inntektsopplysning() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_INNTEKTSOPPLYSNING));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.INGEN_RELEVANT_BEHANDLINGÅRSAK);
    }

    @Test
    void forventer_kontroll_av_inntekt_for_kombinasjon_av_inntektsårsaker() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(
            BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT
        ));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

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
        when(behandling.getBehandlendeOrganisasjonsEnhet()).thenReturn(organisasjonsEnhet);

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.KONTROLL_AV_INNTEKT);
    }

    @Test
    void forventer_beregning_av_høy_sats_for_re_trigger_beregning_høy_sats() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.BEREGNING_AV_HØY_SATS);
    }

    @Test
    void forventer_endring_av_barnetillegg_for_hendelse_fødsel() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_HENDELSE_FØDSEL));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.ENDRING_AV_BARNETILLEGG);
    }

    @Test
    void forventer_endring_av_barnetillegg_for_hendelse_død_barn() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_HENDELSE_DØD_BARN));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.ENDRING_AV_BARNETILLEGG);
    }

    @Test
    void forventer_endring_av_barnetillegg_for_kombinasjon_av_barn_hendelser() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(
            BehandlingÅrsakType.RE_HENDELSE_FØDSEL,
            BehandlingÅrsakType.RE_HENDELSE_DØD_BARN
        ));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.ENDRING_AV_BARNETILLEGG);
    }

    @Test
    void forventer_brukers_dødsfall_for_hendelse_død_forelder() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.BRUKERS_DØDSFALL);
    }

    @Test
    void forventer_ungdomsprogramendring_for_hendelse_endret_startdato_ungdomsprogram() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.UNGDOMSPROGRAMENDRING);
    }

    @Test
    void forventer_ungdomsprogramendring_for_hendelse_opphør_ungdomsprogram() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.UNGDOMSPROGRAMENDRING);
    }

    @Test
    void forventer_ungdomsprogramendring_for_kombinasjon_av_ungdomsprogram_hendelser() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(
            BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM,
            BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM
        ));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.UNGDOMSPROGRAMENDRING);
    }

    @Test
    void forventer_flere_behandlingårsaker_for_blandede_årsaker() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(
            BehandlingÅrsakType.RE_HENDELSE_FØDSEL,
            BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT
        ));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.FLERE_BEHANDLINGÅRSAKER);
    }

    @Test
    void filtrerer_bort_uttalelse_fra_bruker_og_re_registeropplysning() {
        Behandling behandling = mockBehandling();
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(
            BehandlingÅrsakType.UTTALELSE_FRA_BRUKER,
            BehandlingÅrsakType.RE_REGISTEROPPLYSNING,
            BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT
        ));

        BehandlingDto dto = new BehandlingDto();
        BehandlingDtoUtil.setStandardfelter(behandling, dto, null, false);

        assertThat(dto.getVisningsnavn()).isEqualTo(BehandlingVisningsnavn.KONTROLL_AV_INNTEKT);
    }

    private Behandling mockBehandling() {
        Behandling behandling = mock(Behandling.class);
        when(behandling.getBehandlendeOrganisasjonsEnhet()).thenReturn(organisasjonsEnhet);
        return behandling;
    }
}
