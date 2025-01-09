package no.nav.ung.sak.web.app.tjenester.kravperioder;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.k9.søknad.felles.Kildesystem;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.krav.PeriodeMedÅrsaker;
import no.nav.ung.sak.kontrakt.krav.StatusForPerioderPåBehandling;
import no.nav.ung.sak.kontrakt.krav.ÅrsakTilVurdering;
import no.nav.ung.sak.søknadsfrist.KravDokument;
import no.nav.ung.sak.søknadsfrist.KravDokumentType;
import no.nav.ung.sak.søknadsfrist.SøktPeriode;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;

class UtledStatusForPerioderPåBehandlingTest {

    @Test
    void skal_utlede_status_fra_en_ny_søkt_periode() {
        var startdato = LocalDate.now();
        var perioderTilVurdering = new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(startdato, startdato.plusWeeks(52))));
        var programperiode = DatoIntervallEntitet.fraOgMedTilOgMed(startdato, TIDENES_ENDE);

        var statusForPerioderPåBehandling = UtledStatusForPerioderPåBehandling.utledStatus(
            perioderTilVurdering,
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
        var perioderTilVurdering = new TreeSet<>(Set.of(periodeTilVurdering));

        var statusForPerioderPåBehandling = UtledStatusForPerioderPåBehandling.utledStatus(
            perioderTilVurdering,
            Map.of(),
            List.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, periodeTilVurdering))
        );

        var perioderMedÅrsak = statusForPerioderPåBehandling.getPerioderMedÅrsak();
        assertThat(perioderMedÅrsak.size()).isEqualTo(1);
        var periode = perioderMedÅrsak.get(0);
        assertThat(periode.getPeriode()).isEqualTo(new Periode(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato()));
        assertThat(periode.getÅrsaker()).isEqualTo(Set.of(ÅrsakTilVurdering.OPPHØR_UNGDOMSPROGRAM));
    }


}
