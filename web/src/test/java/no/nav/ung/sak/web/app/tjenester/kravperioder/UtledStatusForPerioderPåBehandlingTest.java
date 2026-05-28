package no.nav.ung.sak.web.app.tjenester.kravperioder;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.k9.søknad.felles.Kildesystem;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.krav.ÅrsakTilVurdering;
import no.nav.ung.sak.søknadsfrist.KravDokument;
import no.nav.ung.sak.søknadsfrist.KravDokumentType;
import no.nav.ung.sak.søknadsfrist.SøktPeriode;
import no.nav.ung.sak.trigger.ProsessTriggereNormalisering;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;

class UtledStatusForPerioderPåBehandlingTest {

    @Test
    void skal_utlede_status_fra_en_ny_søkt_periode() {
        var startdato = LocalDate.now();
        var programperiode = DatoIntervallEntitet.fraOgMedTilOgMed(startdato, TIDENES_ENDE);

        var statusForPerioderPåBehandling = UtledStatusForPerioderPåBehandling.utledStatus(
                Map.of(new KravDokument(new JournalpostId(12345L), LocalDateTime.now(), KravDokumentType.SØKNAD, Kildesystem.SØKNADSDIALOG.getKode()), List.of(new SøktPeriode<>(programperiode, null))),
            List.of()
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
            List.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, periodeTilVurdering))
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
            List.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM, periodeTilVurdering))
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
            List.of(new Trigger(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO, periodeTilVurdering))
        );

        var perioderMedÅrsak = statusForPerioderPåBehandling.getPerioderMedÅrsak();
        assertThat(perioderMedÅrsak.size()).isEqualTo(1);
        var periode = perioderMedÅrsak.get(0);
        assertThat(periode.getPeriode()).isEqualTo(new Periode(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato()));
        assertThat(periode.getÅrsaker()).isEqualTo(Set.of(ÅrsakTilVurdering.OPPHØR_VED_MAKSDATO));
    }

    @Test
    void skal_ikke_vise_opphor_ved_maksdato_nar_forlenget_periode_overstyrer_varselarsak() {
        var startdato = LocalDate.now();
        var maksdato = startdato.plusWeeks(52).minusDays(1);
        var varselPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(maksdato, maksdato);
        var forlengetPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(maksdato.plusDays(1), maksdato.plusWeeks(8));

        var statusForPerioderPåBehandling = UtledStatusForPerioderPåBehandling.utledStatus(
            Map.of(),
            ProsessTriggereNormalisering.forKravperioder(List.of(
                new Trigger(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO, varselPeriode),
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM, forlengetPeriode)
            ))
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
            ProsessTriggereNormalisering.forKravperioder(List.of(
                new Trigger(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO, varselPeriode),
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, opphørPeriode)
            ))
        );

        var unikeÅrsaker = statusForPerioderPåBehandling.getPerioderMedÅrsak().stream()
            .flatMap(periode -> periode.getÅrsaker().stream())
            .collect(Collectors.toSet());

        assertThat(unikeÅrsaker)
            .contains(ÅrsakTilVurdering.OPPHØR_UNGDOMSPROGRAM)
            .doesNotContain(ÅrsakTilVurdering.OPPHØR_VED_MAKSDATO);
    }

    @Test
    void revurdering_skal_kun_vise_triggerperiode_naar_soknadsperiode_er_filtrert_bort() {
        // Simulerer at kallstedet har filtrert bort søknadsdokumentet (som tilhører
        // førstegangsbehandlingen) via relevanteKravdokumentForBehandling. Kun trigger-perioden
        // skal vises.
        var startdato = LocalDate.now();
        var fomForlenget = startdato.plusWeeks(52);
        var tomForlenget = fomForlenget.plusWeeks(8).minusDays(1);
        var forlengetPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fomForlenget, tomForlenget);

        var statusForPerioderPåBehandling = UtledStatusForPerioderPåBehandling.utledStatus(
            Map.of(), // Søknadsdokumentet er allerede filtrert bort av kallstedet
            List.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM, forlengetPeriode))
        );

        var perioderMedÅrsak = statusForPerioderPåBehandling.getPerioderMedÅrsak();
        assertThat(perioderMedÅrsak.size()).isEqualTo(1);
        var periode = perioderMedÅrsak.get(0);
        assertThat(periode.getPeriode()).isEqualTo(new Periode(forlengetPeriode.getFomDato(), forlengetPeriode.getTomDato()));
        assertThat(periode.getÅrsaker()).isEqualTo(Set.of(ÅrsakTilVurdering.FORLENGET_PERIODE_UNGDOMSPROGRAM));
    }


}
