package no.nav.ung.sak.web.app.tjenester.kravperioder;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.k9.søknad.felles.Kildesystem;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.krav.ÅrsakTilVurdering;
import no.nav.ung.sak.søknadsfrist.KravDokument;
import no.nav.ung.sak.søknadsfrist.KravDokumentType;
import no.nav.ung.sak.søknadsfrist.SøktPeriode;
import no.nav.ung.sak.trigger.ProsessTriggerFilter;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;

class UtledStatusForPerioderPåBehandlingTest {

    private static final Long ORIGINAL_BEHANDLING_ID = 999L;

    private final Behandling behandling = mock(Behandling.class);
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository = mock(UngdomsprogramPeriodeRepository.class);

    /** Simulerer at originalbehandlingen faktisk hadde en lukket sluttdato, dvs. at opphøret ble reelt vedtatt. */
    private void mockOpphørVarFaktiskIverksatt() {
        when(behandling.getOriginalBehandlingId()).thenReturn(Optional.of(ORIGINAL_BEHANDLING_ID));
        var grunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        var perioder = new UngdomsprogramPerioder(Set.of(new UngdomsprogramPeriode(LocalDate.now().minusYears(1), LocalDate.now().minusDays(1))));
        when(grunnlag.getUngdomsprogramPerioder()).thenReturn(perioder);
        when(ungdomsprogramPeriodeRepository.hentGrunnlag(ORIGINAL_BEHANDLING_ID)).thenReturn(Optional.of(grunnlag));
    }

    /** Simulerer at originalbehandlingen fortsatt hadde åpen sluttdato, dvs. at opphøret aldri ble vedtatt. */
    private void mockOpphørAldriIverksatt() {
        when(behandling.getOriginalBehandlingId()).thenReturn(Optional.of(ORIGINAL_BEHANDLING_ID));
        var grunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        var perioder = new UngdomsprogramPerioder(Set.of(new UngdomsprogramPeriode(LocalDate.now().minusYears(1), Tid.TIDENES_ENDE)));
        when(grunnlag.getUngdomsprogramPerioder()).thenReturn(perioder);
        when(ungdomsprogramPeriodeRepository.hentGrunnlag(ORIGINAL_BEHANDLING_ID)).thenReturn(Optional.of(grunnlag));
    }

    @Test
    void skal_utlede_status_fra_en_ny_søkt_periode() {
        var startdato = LocalDate.now();
        var programperiode = DatoIntervallEntitet.fraOgMedTilOgMed(startdato, TIDENES_ENDE);

        var statusForPerioderPåBehandling = UtledStatusForPerioderPåBehandling.utledStatus(
                Map.of(new KravDokument(new JournalpostId(12345L), LocalDateTime.now(), KravDokumentType.SØKNAD, Kildesystem.SØKNADSDIALOG.getKode()), List.of(new SøktPeriode<>(programperiode, null))),
            List.of(),
            behandling,
            ungdomsprogramPeriodeRepository
        );

        var perioderMedÅrsak = statusForPerioderPåBehandling.getPerioderMedÅrsak();
        assertThat(perioderMedÅrsak.size()).isEqualTo(1);
        var periode = perioderMedÅrsak.get(0);
        assertThat(periode.getPeriode()).isEqualTo(new Periode(programperiode.getFomDato(), programperiode.getTomDato()));
        assertThat(periode.getÅrsaker()).isEqualTo(Set.of(ÅrsakTilVurdering.FØRSTEGANGSVURDERING));
    }

    @Test
    void skal_utlede_status_fra_opphør_av_ungdomsprogram() {
        var startdato = LocalDate.now();
        var opphør = startdato.plusWeeks(30);
        var tomDatoFagsakPeriode = startdato.plusWeeks(52);
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(opphør.plusDays(1), tomDatoFagsakPeriode);

        var statusForPerioderPåBehandling = UtledStatusForPerioderPåBehandling.utledStatus(
                Map.of(),
            List.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, periodeTilVurdering)),
            behandling,
            ungdomsprogramPeriodeRepository
        );

        var perioderMedÅrsak = statusForPerioderPåBehandling.getPerioderMedÅrsak();
        assertThat(perioderMedÅrsak.size()).isEqualTo(1);
        var periode = perioderMedÅrsak.get(0);
        assertThat(periode.getPeriode()).isEqualTo(new Periode(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato()));
        assertThat(periode.getÅrsaker()).isEqualTo(Set.of(ÅrsakTilVurdering.OPPHØR_UNGDOMSPROGRAM));
    }

    @Test
    void skal_utlede_status_fra_forlenget_periode_av_ungdomsprogram() {
        var startdato = LocalDate.now();
        var fomNyPeriode = startdato.plusWeeks(52);
        var tomNyPeriode = fomNyPeriode.plusWeeks(8).minusDays(1);
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(fomNyPeriode, tomNyPeriode);

        var statusForPerioderPåBehandling = UtledStatusForPerioderPåBehandling.utledStatus(
            Map.of(),
            List.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM, periodeTilVurdering)),
            behandling,
            ungdomsprogramPeriodeRepository
        );

        var perioderMedÅrsak = statusForPerioderPåBehandling.getPerioderMedÅrsak();
        assertThat(perioderMedÅrsak.size()).isEqualTo(1);
        var periode = perioderMedÅrsak.get(0);
        assertThat(periode.getPeriode()).isEqualTo(new Periode(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato()));
        assertThat(periode.getÅrsaker()).isEqualTo(Set.of(ÅrsakTilVurdering.FORLENGET_PERIODE_UNGDOMSPROGRAM));
    }

    @Test
    void skal_utlede_status_fra_varsel_opphor_ved_maksdato() {
        var startdato = LocalDate.now();
        var maksdato = startdato.plusWeeks(52).minusDays(1);
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(maksdato, maksdato);

        var statusForPerioderPåBehandling = UtledStatusForPerioderPåBehandling.utledStatus(
            Map.of(),
            List.of(new Trigger(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO, periodeTilVurdering)),
            behandling,
            ungdomsprogramPeriodeRepository
        );

        var perioderMedÅrsak = statusForPerioderPåBehandling.getPerioderMedÅrsak();
        assertThat(perioderMedÅrsak.size()).isEqualTo(1);
        var periode = perioderMedÅrsak.get(0);
        assertThat(periode.getPeriode()).isEqualTo(new Periode(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato()));
        assertThat(periode.getÅrsaker()).isEqualTo(Set.of(ÅrsakTilVurdering.OPPHØR_VED_MAKSDATO));
    }

    @Test
    void skal_utlede_status_fra_opphevelse_av_opphør_av_ungdomsprogram() {
        mockOpphørVarFaktiskIverksatt();
        var startdato = LocalDate.now();
        var tidligereOpphørsdato = startdato.plusWeeks(30);
        var maksdato = startdato.plusWeeks(52).minusDays(1);
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(tidligereOpphørsdato.plusDays(1), maksdato);

        var statusForPerioderPåBehandling = UtledStatusForPerioderPåBehandling.utledStatus(
                Map.of(),
            List.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM, periodeTilVurdering)),
            behandling,
            ungdomsprogramPeriodeRepository
        );

        var perioderMedÅrsak = statusForPerioderPåBehandling.getPerioderMedÅrsak();
        assertThat(perioderMedÅrsak.size()).isEqualTo(1);
        var periode = perioderMedÅrsak.get(0);
        assertThat(periode.getPeriode()).isEqualTo(new Periode(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato()));
        assertThat(periode.getÅrsaker()).isEqualTo(Set.of(ÅrsakTilVurdering.UNGDOMSPROGRAM_OPPHØR_OPPHEVET));
    }

    @Test
    void skal_utlede_status_som_avbrutt_når_opphør_aldri_ble_iverksatt() {
        mockOpphørAldriIverksatt();
        var startdato = LocalDate.now();
        var tidligereOpphørsdato = startdato.plusWeeks(30);
        var maksdato = startdato.plusWeeks(52).minusDays(1);
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(tidligereOpphørsdato.plusDays(1), maksdato);

        var statusForPerioderPåBehandling = UtledStatusForPerioderPåBehandling.utledStatus(
                Map.of(),
            List.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM, periodeTilVurdering)),
            behandling,
            ungdomsprogramPeriodeRepository
        );

        var perioderMedÅrsak = statusForPerioderPåBehandling.getPerioderMedÅrsak();
        assertThat(perioderMedÅrsak.size()).isEqualTo(1);
        var periode = perioderMedÅrsak.get(0);
        assertThat(periode.getPeriode()).isEqualTo(new Periode(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato()));
        assertThat(periode.getÅrsaker()).isEqualTo(Set.of(ÅrsakTilVurdering.UNGDOMSPROGRAM_OPPHØR_MOTTATT_OG_AVBRUTT_I_SAMME_BEHANDLING));
    }

    @Test
    void skal_ikke_vise_opphor_ved_maksdato_nar_forlenget_periode_overstyrer_varselarsak() {
        var startdato = LocalDate.now();
        var maksdato = startdato.plusWeeks(52).minusDays(1);
        var varselPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(maksdato, maksdato);
        var forlengetPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(maksdato.plusDays(1), maksdato.plusWeeks(8));

        var statusForPerioderPåBehandling = UtledStatusForPerioderPåBehandling.utledStatus(
            Map.of(),
            ProsessTriggerFilter.forKravperioder(List.of(
                new Trigger(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO, varselPeriode),
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM, forlengetPeriode)
            )),
            behandling,
            ungdomsprogramPeriodeRepository
        );

        var unikeÅrsaker = statusForPerioderPåBehandling.getPerioderMedÅrsak().stream()
            .flatMap(periode -> periode.getÅrsaker().stream())
            .collect(Collectors.toSet());

        assertThat(unikeÅrsaker)
            .contains(ÅrsakTilVurdering.FORLENGET_PERIODE_UNGDOMSPROGRAM)
            .doesNotContain(ÅrsakTilVurdering.OPPHØR_VED_MAKSDATO);
    }

    @Test
    void skal_ikke_vise_opphor_ved_maksdato_nar_opphor_overstyrer_varselarsak() {
        var startdato = LocalDate.now();
        var maksdato = startdato.plusWeeks(52).minusDays(1);
        var varselPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(maksdato, maksdato);
        var opphørPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(maksdato.plusDays(1), maksdato.plusWeeks(4));

        var statusForPerioderPåBehandling = UtledStatusForPerioderPåBehandling.utledStatus(
            Map.of(),
            ProsessTriggerFilter.forKravperioder(List.of(
                new Trigger(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO, varselPeriode),
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, opphørPeriode)
            )),
            behandling,
            ungdomsprogramPeriodeRepository
        );

        var unikeÅrsaker = statusForPerioderPåBehandling.getPerioderMedÅrsak().stream()
            .flatMap(periode -> periode.getÅrsaker().stream())
            .collect(Collectors.toSet());

        assertThat(unikeÅrsaker)
            .contains(ÅrsakTilVurdering.OPPHØR_UNGDOMSPROGRAM)
            .doesNotContain(ÅrsakTilVurdering.OPPHØR_VED_MAKSDATO);
    }

    @Test
    void skal_ikke_vise_opphør_når_opphevelse_av_opphør_er_på_samme_behandling() {
        mockOpphørVarFaktiskIverksatt();
        var startdato = LocalDate.now();
        var tidligereOpphørsdato = startdato.plusWeeks(30);
        var maksdato = startdato.plusWeeks(52).minusDays(1);
        var opphørPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(tidligereOpphørsdato.plusDays(1), maksdato);
        var ophevelsePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(tidligereOpphørsdato.plusDays(1), maksdato);

        var statusForPerioderPåBehandling = UtledStatusForPerioderPåBehandling.utledStatus(
            Map.of(),
            List.of(
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, opphørPeriode),
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM, ophevelsePeriode)
            ),
            behandling,
            ungdomsprogramPeriodeRepository
        );

        var unikeÅrsaker = statusForPerioderPåBehandling.getPerioderMedÅrsak().stream()
            .flatMap(periode -> periode.getÅrsaker().stream())
            .collect(Collectors.toSet());

        assertThat(unikeÅrsaker)
            .contains(ÅrsakTilVurdering.UNGDOMSPROGRAM_OPPHØR_OPPHEVET)
            .doesNotContain(ÅrsakTilVurdering.OPPHØR_UNGDOMSPROGRAM);
    }

    @Test
    void skal_vise_avbrutt_når_opphør_og_opphevelse_slås_sammen_uten_at_opphøret_ble_iverksatt() {
        mockOpphørAldriIverksatt();
        var startdato = LocalDate.now();
        var tidligereOpphørsdato = startdato.plusWeeks(30);
        var maksdato = startdato.plusWeeks(52).minusDays(1);
        var opphørPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(tidligereOpphørsdato.plusDays(1), maksdato);
        var ophevelsePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(tidligereOpphørsdato.plusDays(1), maksdato);

        var statusForPerioderPåBehandling = UtledStatusForPerioderPåBehandling.utledStatus(
            Map.of(),
            List.of(
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, opphørPeriode),
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM, ophevelsePeriode)
            ),
            behandling,
            ungdomsprogramPeriodeRepository
        );

        var unikeÅrsaker = statusForPerioderPåBehandling.getPerioderMedÅrsak().stream()
            .flatMap(periode -> periode.getÅrsaker().stream())
            .collect(Collectors.toSet());

        assertThat(unikeÅrsaker)
            .contains(ÅrsakTilVurdering.UNGDOMSPROGRAM_OPPHØR_MOTTATT_OG_AVBRUTT_I_SAMME_BEHANDLING)
            .doesNotContain(ÅrsakTilVurdering.OPPHØR_UNGDOMSPROGRAM, ÅrsakTilVurdering.UNGDOMSPROGRAM_OPPHØR_OPPHEVET);
    }

}

