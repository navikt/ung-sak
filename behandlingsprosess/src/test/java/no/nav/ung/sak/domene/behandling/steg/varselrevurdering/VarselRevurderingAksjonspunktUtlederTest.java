package no.nav.ung.sak.domene.behandling.steg.varselrevurdering;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.ungdomsytelse.periodeendring.UngdomsytelsePeriodeEndringType;
import no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseBekreftetPeriodeEndring;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.JournalpostId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        List<DatoIntervallEntitet> perioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), tomDato));
        List<MottattDokument> gyldigeDokumenter = List.of();
        List<UngdomsytelseBekreftetPeriodeEndring> bekreftelser = List.of();
        String ventefrist = "P1D";
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, perioder, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_OPPHØR_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }

    @Test
    void skal_få_aksjonspunkt_og_venteårsak_for_startdato_når_vi_ikke_har_bekreftelse() {
        final var tomDato = LocalDate.now().minusDays(5);
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM);
        List<DatoIntervallEntitet> perioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), tomDato));
        List<MottattDokument> gyldigeDokumenter = List.of();
        List<UngdomsytelseBekreftetPeriodeEndring> bekreftelser = List.of();
        String ventefrist = "P1D";
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, perioder, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }

    @Test
    void skal_få_aksjonspunkt_når_vi_har_bekretelse_som_ikke_matcher_gjeldende_opphørsdato() {
        final var bekreftelseDato = LocalDate.now().minusDays(3);
        final var tomDato = LocalDate.now().minusDays(5);
        final var journalpostId = "1";

        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        List<DatoIntervallEntitet> perioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), tomDato));
        List<MottattDokument> gyldigeDokumenter = List.of(lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1)));
        List<UngdomsytelseBekreftetPeriodeEndring> bekreftelser = List.of(lagBekreftelse(bekreftelseDato, journalpostId, UngdomsytelsePeriodeEndringType.ENDRET_OPPHØRSDATO));
        String ventefrist = "P1D";
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, perioder, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_OPPHØR_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }

    @Test
    void skal_ikke_få_aksjonspunkt_når_vi_har_bekreftelse_som_matcher_opphørsdato() {
        final var journalpostId = "1";
        final var programperiodeTomDato = LocalDate.now().minusDays(3);

        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        List<DatoIntervallEntitet> perioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), programperiodeTomDato));
        List<MottattDokument> gyldigeDokumenter = List.of(lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1)));
        List<UngdomsytelseBekreftetPeriodeEndring> bekreftelser = List.of(lagBekreftelse(programperiodeTomDato, journalpostId, UngdomsytelsePeriodeEndringType.ENDRET_OPPHØRSDATO));
        String ventefrist = "P1D";
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, perioder, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertFalse(resultat.isPresent());
    }

    @Test
    void skal_få_aksjonspunkt_når_bekreftet_startdato_ikke_matcher_startdato_fra_register() {
        final var bekreftelseDato = LocalDate.now().minusDays(3);
        final var fomDato = LocalDate.now().minusDays(10);
        final var journalpostId = "1";
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM);
        List<DatoIntervallEntitet> perioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, LocalDate.now().minusDays(5)));
        List<MottattDokument> gyldigeDokumenter = List.of(lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1)));
        List<UngdomsytelseBekreftetPeriodeEndring> bekreftelser = List.of(lagBekreftelse(bekreftelseDato, journalpostId, UngdomsytelsePeriodeEndringType.ENDRET_STARTDATO));
        String ventefrist = "P1D";
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, perioder, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }

    @Test
    void skal_få_aksjonspunkt_når_siste_bekreftet_startdato_ikke_matcher_startdato_fra_register() {
        final var journalpostId = "1";
        final var journalpostId2 = "2";
        final var fomDato = LocalDate.now().minusDays(10);
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM);
        List<DatoIntervallEntitet> perioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, LocalDate.now().minusDays(5)));
        List<MottattDokument> gyldigeDokumenter = List.of(
            lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1)),
            lagMottattDokument(journalpostId2, LocalDateTime.now())
        );
        List<UngdomsytelseBekreftetPeriodeEndring> bekreftelser = List.of(
            lagBekreftelse(fomDato, journalpostId, UngdomsytelsePeriodeEndringType.ENDRET_STARTDATO),
            lagBekreftelse(LocalDate.now().minusDays(3), journalpostId2, UngdomsytelsePeriodeEndringType.ENDRET_STARTDATO));
        String ventefrist = "P1D";
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, perioder, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }

    @Test
    void skal_ikke_få_aksjonspunkt_når_siste_bekreftet_startdato_matcher_startdato_fra_register() {
        final var journalpostId = "1";
        final var journalpostId2 = "2";
        final var fomDato = LocalDate.now().minusDays(10);
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM);
        List<DatoIntervallEntitet> perioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, LocalDate.now().minusDays(5)));
        List<MottattDokument> gyldigeDokumenter = List.of(
            lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1)),
            lagMottattDokument(journalpostId2, LocalDateTime.now())
        );
        List<UngdomsytelseBekreftetPeriodeEndring> bekreftelser = List.of(
            lagBekreftelse(LocalDate.now().minusDays(3), journalpostId, UngdomsytelsePeriodeEndringType.ENDRET_STARTDATO),
            lagBekreftelse(fomDato, journalpostId2, UngdomsytelsePeriodeEndringType.ENDRET_STARTDATO));
        String ventefrist = "P1D";
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, perioder, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertFalse(resultat.isPresent());
    }

    @Test
    void skal_ikke_få_aksjonspunkt_når_siste_bekreftet_sluttdato_matcher_sluttdato_fra_register() {
        final var journalpostId = "1";
        final var journalpostId2 = "2";
        final var fomDato = LocalDate.now().minusDays(10);
        final var tomDato = LocalDate.now().minusDays(5);
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        List<DatoIntervallEntitet> perioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, tomDato));
        List<MottattDokument> gyldigeDokumenter = List.of(
            lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1)),
            lagMottattDokument(journalpostId2, LocalDateTime.now())
        );
        List<UngdomsytelseBekreftetPeriodeEndring> bekreftelser = List.of(
            lagBekreftelse(LocalDate.now().minusDays(3), journalpostId, UngdomsytelsePeriodeEndringType.ENDRET_OPPHØRSDATO),
            lagBekreftelse(tomDato, journalpostId2, UngdomsytelsePeriodeEndringType.ENDRET_OPPHØRSDATO));
        String ventefrist = "P1D";
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, perioder, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertFalse(resultat.isPresent());
    }

    @Test
    void skal_få_aksjonspunkt_når_siste_bekreftet_sluttdato_ikke_matcher_sluttdato_fra_register() {
        final var journalpostId = "1";
        final var journalpostId2 = "2";
        final var fomDato = LocalDate.now().minusDays(10);
        final var tomDato = LocalDate.now().minusDays(5);
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        List<DatoIntervallEntitet> perioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, tomDato));
        List<MottattDokument> gyldigeDokumenter = List.of(
            lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1)),
            lagMottattDokument(journalpostId2, LocalDateTime.now())
        );
        List<UngdomsytelseBekreftetPeriodeEndring> bekreftelser = List.of(
            lagBekreftelse(tomDato, journalpostId, UngdomsytelsePeriodeEndringType.ENDRET_OPPHØRSDATO),
            lagBekreftelse(LocalDate.now().minusDays(3), journalpostId2, UngdomsytelsePeriodeEndringType.ENDRET_OPPHØRSDATO));
        String ventefrist = "P1D";
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, perioder, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_OPPHØR_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }



    @Test
    void skal_ikke_få_aksjonspunkt_når_vi_har_bekreftelse_med_matchende_startdato() {
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM);
        List<DatoIntervallEntitet> perioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(3), LocalDate.now().plusDays(3)));
        List<MottattDokument> gyldigeDokumenter = List.of(lagMottattDokument("1", LocalDateTime.now().minusDays(1)));
        List<UngdomsytelseBekreftetPeriodeEndring> bekreftelser = List.of(lagBekreftelse(LocalDate.now().minusDays(3), "1", UngdomsytelsePeriodeEndringType.ENDRET_STARTDATO));
        String ventefrist = "P1D";
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, perioder, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertFalse(resultat.isPresent());
    }

    @Test
    void skal_ikke_få_aksjonspunkt_uten_behandlingsårsak_for_endring() {
        List<BehandlingÅrsakType> behandlingsårsaker = List.of();
        List<DatoIntervallEntitet> perioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5)));
        List<MottattDokument> gyldigeDokumenter = List.of(lagMottattDokument("1", LocalDateTime.now().minusDays(1)));
        List<UngdomsytelseBekreftetPeriodeEndring> bekreftelser = List.of();
        String ventefrist = "P1D";
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, perioder, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertFalse(resultat.isPresent());
    }

    @Test
    void skal_få_aksjonspunkt_for_både_opphør_og_startdato_endring_med_kun_bekreftet_opphør() {
        final var journalpostId = "1";
        final var fomDato = LocalDate.now().minusDays(10);
        final var tomDato = LocalDate.now().minusDays(5);
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        List<DatoIntervallEntitet> perioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, tomDato));
        List<MottattDokument> gyldigeDokumenter = List.of(
            lagMottattDokument(journalpostId, LocalDateTime.now().minusDays(1))
        );
        List<UngdomsytelseBekreftetPeriodeEndring> bekreftelser = List.of(
            lagBekreftelse(tomDato, journalpostId, UngdomsytelsePeriodeEndringType.ENDRET_OPPHØRSDATO));
        String ventefrist = "P1D";
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, perioder, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }

    @Test
    void skal_prioritere_venteårsak_for_opphør() {
        final var fomDato = LocalDate.now().minusDays(10);
        final var tomDato = LocalDate.now().minusDays(5);
        List<BehandlingÅrsakType> behandlingsårsaker = List.of(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
        List<DatoIntervallEntitet> perioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, tomDato));
        List<MottattDokument> gyldigeDokumenter = List.of();
        List<UngdomsytelseBekreftetPeriodeEndring> bekreftelser = List.of();
        String ventefrist = "P1D";
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt = Optional.empty();

        Optional<AksjonspunktResultat> resultat = VarselRevurderingAksjonspunktUtleder.utledAksjonspunkt(behandlingsårsaker, perioder, gyldigeDokumenter, bekreftelser, ventefrist, eksisterendeAksjonspunkt);

        assertTrue(resultat.isPresent());
        assertEquals(Venteårsak.VENTER_BEKREFTELSE_ENDRET_OPPHØR_UNGDOMSPROGRAM, resultat.get().getVenteårsak());
    }


    private static UngdomsytelseBekreftetPeriodeEndring lagBekreftelse(LocalDate endringDato, String journalpostId, UngdomsytelsePeriodeEndringType endringType) {
        return new UngdomsytelseBekreftetPeriodeEndring(endringDato, new JournalpostId(journalpostId), endringType);
    }

    private static MottattDokument lagMottattDokument(String journalpostId, LocalDateTime mottattTidspunkt) {
        return new MottattDokument.Builder()
            .medJournalPostId(new JournalpostId(journalpostId))
            .medFagsakId(1L)
            .medMottattTidspunkt(mottattTidspunkt)
            .build();
    }
}
