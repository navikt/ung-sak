package no.nav.ung.sak.domene.behandling.steg.varselrevurdering;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.ungdomsytelse.periodeendring.UngdomsprogramPeriodeEndringType;
import no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsprogramBekreftetPeriodeEndring;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM;
import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;

public class VarselRevurderingAksjonspunktUtleder {

    public static Optional<AksjonspunktResultat> utledAksjonspunkt(
        List<BehandlingÅrsakType> behandlingsårsakerForBehandling,
        LocalDateTimeline<Boolean> ungdomsprogramTidslinje,
        List<MottattDokument> gyldigeDokumenter,
        List<UngdomsprogramBekreftetPeriodeEndring> bekreftelser,
        Duration ventePeriode,
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt) {
        final var gruppertePeriodeEndringerPåEndringstype = finnBekreftelserGruppertPåEndringstype(bekreftelser);
        final var eksisterendeFrist = eksisterendeAksjonspunkt.map(Aksjonspunkt::getFristTid);

        if (behandlingsårsakerForBehandling.contains(RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM)) {
            final var sisteBekreftetEndring = finnSisteMottatteBekreftelseForEndringstype(gruppertePeriodeEndringerPåEndringstype, gyldigeDokumenter, UngdomsprogramPeriodeEndringType.ENDRET_OPPHØRSDATO);
            if (sisteBekreftetEndring.isEmpty() || !harMatchendeSluttdato(ungdomsprogramTidslinje, sisteBekreftetEndring.get())) {
                return Optional.of(aksjonspunktMedFristOgVenteÅrsak(Venteårsak.VENTER_BEKREFTELSE_ENDRET_OPPHØR_UNGDOMSPROGRAM, eksisterendeFrist, ventePeriode));
            }
        }

        if (behandlingsårsakerForBehandling.contains(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM)) {
            final var sisteBekreftetEndring = finnSisteMottatteBekreftelseForEndringstype(gruppertePeriodeEndringerPåEndringstype, gyldigeDokumenter, UngdomsprogramPeriodeEndringType.ENDRET_STARTDATO);
            if (sisteBekreftetEndring.isEmpty() || !harMatchendeStartdato(ungdomsprogramTidslinje, sisteBekreftetEndring.get())) {
                return Optional.of(aksjonspunktMedFristOgVenteÅrsak(Venteårsak.VENTER_BEKREFTELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, eksisterendeFrist, ventePeriode));
            }

        }

        return Optional.empty();
    }

    private static AksjonspunktResultat aksjonspunktMedFristOgVenteÅrsak(Venteårsak venterBekreftelseEndretStartdatoUngdomsprogram, Optional<LocalDateTime> eksisterendeFrist, Duration ventePeriode) {
        return AksjonspunktResultat.opprettForAksjonspunktMedFrist(
            AUTO_SATT_PÅ_VENT_REVURDERING,
            venterBekreftelseEndretStartdatoUngdomsprogram,
            eksisterendeFrist.filter(it -> it.isAfter(LocalDateTime.now())).orElse(LocalDateTime.now().plus(ventePeriode)));
    }


    private static Map<UngdomsprogramPeriodeEndringType, List<UngdomsprogramBekreftetPeriodeEndring>> finnBekreftelserGruppertPåEndringstype(List<UngdomsprogramBekreftetPeriodeEndring> bekreftelser) {
        return bekreftelser.stream().collect(Collectors.groupingBy(UngdomsprogramBekreftetPeriodeEndring::getEndringType));
    }

    private static boolean harMatchendeSluttdato(LocalDateTimeline<Boolean> ungdomsprogramTidslinje, UngdomsprogramBekreftetPeriodeEndring sisteBekreftetEndring) {
        return ungdomsprogramTidslinje.getLocalDateIntervals().stream().anyMatch(p -> p.getTomDato().equals(sisteBekreftetEndring.getDato()));
    }

    private static boolean harMatchendeStartdato(LocalDateTimeline<Boolean> perioder, UngdomsprogramBekreftetPeriodeEndring sisteBekreftetEndring) {
        return perioder.getLocalDateIntervals().stream().anyMatch(p -> p.getFomDato().equals(sisteBekreftetEndring.getDato()));
    }

    private static Optional<UngdomsprogramBekreftetPeriodeEndring> finnSisteMottatteBekreftelseForEndringstype(Map<UngdomsprogramPeriodeEndringType, List<UngdomsprogramBekreftetPeriodeEndring>> gruppertePeriodeEndringerPåEndringstype,
                                                                                                               List<MottattDokument> gyldigeDokumenter,
                                                                                                               UngdomsprogramPeriodeEndringType endringType) {
        final var bekreftetEndringer = gruppertePeriodeEndringerPåEndringstype.getOrDefault(endringType, List.of());
        return bekreftetEndringer.stream()
            .max(Comparator.comparing(e -> finnMottattTidspunkt(e, gyldigeDokumenter)));
    }

    private static LocalDateTime finnMottattTidspunkt(UngdomsprogramBekreftetPeriodeEndring e, List<MottattDokument> gyldigeDokumenter) {
        return gyldigeDokumenter.stream().filter(d -> d.getJournalpostId().equals(e.getJournalpostId())).map(MottattDokument::getMottattTidspunkt).findFirst().orElseThrow();
    }


}
