package no.nav.ung.sak.domene.behandling.steg.varselrevurdering;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.ungdomsytelse.periodeendring.UngdomsprogramPeriodeEndringType;
import no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsprogramBekreftetPeriodeEndring;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.JournalpostId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM;
import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM;
import static org.junit.jupiter.api.Assertions.*;


class VarselRevurderingAksjonspunktUtlederTest {

    @Test
    void skal_få_aksjonspunkt_og_venteårsak_for_opphør_når_vi_ikke_har_bekreftelse() {
        final var tomDato = LocalDate.now().minusDays(5);
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        var ungdomsprogramTidslinje = new LocalDateTimeline<>(LocalDate.now().minusDays(10), tomDato, true);
        List<MottattDokument> gyldigeDokumenter = List.of();
        List<UngdomsprogramBekreftetPeriodeEndring> bekreftelser = List.of();
        var ventefrist = Period.parse("P1D");
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, ungdomsprogramTidslinje, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_OPPHØR_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }

    @Test
    void skal_få_aksjonspunkt_og_venteårsak_for_startdato_når_vi_ikke_har_bekreftelse() {
        final var tomDato = LocalDate.now().minusDays(5);
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM);
        var ungdomsprogramTidslinje = new LocalDateTimeline<>(LocalDate.now().minusDays(10), tomDato, true);
        List<MottattDokument> gyldigeDokumenter = List.of();
        List<UngdomsprogramBekreftetPeriodeEndring> bekreftelser = List.of();
        var ventefrist = Period.parse("P1D");
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, ungdomsprogramTidslinje, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }

    @Test
    void skal_få_aksjonspunkt_når_vi_har_bekretelse_som_ikke_matcher_gjeldende_opphørsdato() {
        final var bekreftelseDato = LocalDate.now().minusDays(3);
        final var tomDato = LocalDate.now().minusDays(5);
        final var journalpostId = "1";

        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        var ungdomsprogramTidslinje = new LocalDateTimeline<>(LocalDate.now().minusDays(10), tomDato, true);
        List<MottattDokument> gyldigeDokumenter = List.of(lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1)));
        List<UngdomsprogramBekreftetPeriodeEndring> bekreftelser = List.of(lagBekreftelse(bekreftelseDato, journalpostId, UngdomsprogramPeriodeEndringType.ENDRET_OPPHØRSDATO));
        var ventefrist = Period.parse("P1D");
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, ungdomsprogramTidslinje, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_OPPHØR_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }

    @Test
    void skal_ikke_få_aksjonspunkt_når_vi_har_bekreftelse_som_matcher_opphørsdato() {
        final var journalpostId = "1";
        final var programperiodeTomDato = LocalDate.now().minusDays(3);

        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        var ungdomsprogramTidslinje = new LocalDateTimeline<>(LocalDate.now().minusDays(10), programperiodeTomDato, true);
        List<MottattDokument> gyldigeDokumenter = List.of(lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1)));
        List<UngdomsprogramBekreftetPeriodeEndring> bekreftelser = List.of(lagBekreftelse(programperiodeTomDato, journalpostId, UngdomsprogramPeriodeEndringType.ENDRET_OPPHØRSDATO));
        var ventefrist = Period.parse("P1D");
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, ungdomsprogramTidslinje, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertFalse(resultat.isPresent());
    }

    @Test
    void skal_få_aksjonspunkt_når_bekreftet_startdato_ikke_matcher_startdato_fra_register() {
        final var bekreftelseDato = LocalDate.now().minusDays(3);
        final var fomDato = LocalDate.now().minusDays(10);
        final var journalpostId = "1";
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM);
        var ungdomsprogramTidslinje = new LocalDateTimeline<>(fomDato, LocalDate.now().minusDays(5), true);
        List<MottattDokument> gyldigeDokumenter = List.of(lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1)));
        List<UngdomsprogramBekreftetPeriodeEndring> bekreftelser = List.of(lagBekreftelse(bekreftelseDato, journalpostId, UngdomsprogramPeriodeEndringType.ENDRET_STARTDATO));
        var ventefrist = Period.parse("P1D");
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, ungdomsprogramTidslinje, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }

    @Test
    void skal_få_aksjonspunkt_når_siste_bekreftet_startdato_ikke_matcher_startdato_fra_register() {
        final var journalpostId = "1";
        final var journalpostId2 = "2";
        final var fomDato = LocalDate.now().minusDays(10);
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM);
        var ungdomsprogramTidslinje = new LocalDateTimeline<>(fomDato, LocalDate.now().minusDays(5), true);
        List<MottattDokument> gyldigeDokumenter = List.of(
            lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1)),
            lagMottattDokument(journalpostId2, LocalDateTime.now())
        );
        List<UngdomsprogramBekreftetPeriodeEndring> bekreftelser = List.of(
            lagBekreftelse(fomDato, journalpostId, UngdomsprogramPeriodeEndringType.ENDRET_STARTDATO),
            lagBekreftelse(LocalDate.now().minusDays(3), journalpostId2, UngdomsprogramPeriodeEndringType.ENDRET_STARTDATO));
        var ventefrist = Period.parse("P1D");
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, ungdomsprogramTidslinje, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }

    @Test
    void skal_ikke_få_aksjonspunkt_når_siste_bekreftet_startdato_matcher_startdato_fra_register() {
        final var journalpostId = "1";
        final var journalpostId2 = "2";
        final var fomDato = LocalDate.now().minusDays(10);
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM);
        var ungdomsprogramTidslinje = new LocalDateTimeline<>(fomDato, LocalDate.now().minusDays(5), true);
        List<MottattDokument> gyldigeDokumenter = List.of(
            lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1)),
            lagMottattDokument(journalpostId2, LocalDateTime.now())
        );
        List<UngdomsprogramBekreftetPeriodeEndring> bekreftelser = List.of(
            lagBekreftelse(LocalDate.now().minusDays(3), journalpostId, UngdomsprogramPeriodeEndringType.ENDRET_STARTDATO),
            lagBekreftelse(fomDato, journalpostId2, UngdomsprogramPeriodeEndringType.ENDRET_STARTDATO));
        var ventefrist = Period.parse("P1D");
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, ungdomsprogramTidslinje, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertFalse(resultat.isPresent());
    }

    @Test
    void skal_ikke_få_aksjonspunkt_når_siste_bekreftet_sluttdato_matcher_sluttdato_fra_register() {
        final var journalpostId = "1";
        final var journalpostId2 = "2";
        final var fomDato = LocalDate.now().minusDays(10);
        final var tomDato = LocalDate.now().minusDays(5);
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        var ungdomsprogramTidslinje = new LocalDateTimeline<>(fomDato, tomDato, true);
        List<MottattDokument> gyldigeDokumenter = List.of(
            lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1)),
            lagMottattDokument(journalpostId2, LocalDateTime.now())
        );
        List<UngdomsprogramBekreftetPeriodeEndring> bekreftelser = List.of(
            lagBekreftelse(LocalDate.now().minusDays(3), journalpostId, UngdomsprogramPeriodeEndringType.ENDRET_OPPHØRSDATO),
            lagBekreftelse(tomDato, journalpostId2, UngdomsprogramPeriodeEndringType.ENDRET_OPPHØRSDATO));
        var ventefrist = Period.parse("P1D");
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, ungdomsprogramTidslinje, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertFalse(resultat.isPresent());
    }

    @Test
    void skal_få_aksjonspunkt_når_siste_bekreftet_sluttdato_ikke_matcher_sluttdato_fra_register() {
        final var journalpostId = "1";
        final var journalpostId2 = "2";
        final var fomDato = LocalDate.now().minusDays(10);
        final var tomDato = LocalDate.now().minusDays(5);
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        var perioder = new LocalDateTimeline<>(fomDato, tomDato, true);
        List<MottattDokument> gyldigeDokumenter = List.of(
            lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1)),
            lagMottattDokument(journalpostId2, LocalDateTime.now())
        );
        List<UngdomsprogramBekreftetPeriodeEndring> bekreftelser = List.of(
            lagBekreftelse(tomDato, journalpostId, UngdomsprogramPeriodeEndringType.ENDRET_OPPHØRSDATO),
            lagBekreftelse(LocalDate.now().minusDays(3), journalpostId2, UngdomsprogramPeriodeEndringType.ENDRET_OPPHØRSDATO));
        var ventefrist = Period.parse("P1D");
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, perioder, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_OPPHØR_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }



    @Test
    void skal_ikke_få_aksjonspunkt_når_vi_har_bekreftelse_med_matchende_startdato() {
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM);
        var ungdomsprogramTidslinje = new LocalDateTimeline<>(LocalDate.now().minusDays(3), LocalDate.now().plusDays(3), true);
        List<MottattDokument> gyldigeDokumenter = List.of(lagMottattDokument("1", LocalDateTime.now().minusDays(1)));
        List<UngdomsprogramBekreftetPeriodeEndring> bekreftelser = List.of(lagBekreftelse(LocalDate.now().minusDays(3), "1", UngdomsprogramPeriodeEndringType.ENDRET_STARTDATO));
        var ventefrist = Period.parse("P1D");
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, ungdomsprogramTidslinje, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertFalse(resultat.isPresent());
    }

    @Test
    void skal_ikke_få_aksjonspunkt_uten_behandlingsårsak_for_endring() {
        List<BehandlingÅrsakType> behandlingsårsaker = List.of();
        var ungdomsprogramTidslinje = new LocalDateTimeline<>(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), true);
        List<MottattDokument> gyldigeDokumenter = List.of(lagMottattDokument("1", LocalDateTime.now().minusDays(1)));
        List<UngdomsprogramBekreftetPeriodeEndring> bekreftelser = List.of();
        var ventefrist = Period.parse("P1D");
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, ungdomsprogramTidslinje, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertFalse(resultat.isPresent());
    }

    @Test
    void skal_få_aksjonspunkt_for_både_opphør_og_startdato_endring_med_kun_bekreftet_opphør() {
        final var journalpostId = "1";
        final var fomDato = LocalDate.now().minusDays(10);
        final var tomDato = LocalDate.now().minusDays(5);
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        var ungdomsprogramTidslinje = new LocalDateTimeline<>(fomDato, tomDato, true);
        List<MottattDokument> gyldigeDokumenter = List.of(
            lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1))
        );
        List<UngdomsprogramBekreftetPeriodeEndring> bekreftelser = List.of(
            lagBekreftelse(tomDato, journalpostId, UngdomsprogramPeriodeEndringType.ENDRET_OPPHØRSDATO));
        var ventefrist = Period.parse("P1D");
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, ungdomsprogramTidslinje, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }

    @Test
    void skal_prioritere_venteårsak_for_opphør() {
        final var fomDato = LocalDate.now().minusDays(10);
        final var tomDato = LocalDate.now().minusDays(5);
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        var ungdomsprogramTidslinje = new LocalDateTimeline<>(fomDato, tomDato, true);
        List<MottattDokument> gyldigeDokumenter = List.of();
        List<UngdomsprogramBekreftetPeriodeEndring> bekreftelser = List.of();
        var ventefrist = Period.parse("P1D");
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, ungdomsprogramTidslinje, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_OPPHØR_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }


    private static UngdomsprogramBekreftetPeriodeEndring lagBekreftelse(LocalDate endringDato, String journalpostId, UngdomsprogramPeriodeEndringType endringType) {
        return new UngdomsprogramBekreftetPeriodeEndring(endringDato, new JournalpostId(journalpostId), endringType);
    }

    private static MottattDokument lagMottattDokument(String journalpostId, LocalDateTime mottattTidspunkt) {
        return new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId(journalpostId))
            .medFagsakId(1L)
            .medMottattTidspunkt(mottattTidspunkt)
            .build();
    }
}
