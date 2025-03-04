package no.nav.ung.sak.domene.behandling.steg.varselrevurdering;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.ungdomsytelse.periodeendring.UngdomsytelsePeriodeEndringType;
import no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseBekreftetPeriodeEndring;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM;
import static no.nav.ung.kodeverk.behandling.BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;

public class VarselRevurderingAksjonspunktUtleder {

    public static Optional<AksjonspunktResultat> utledAksjonspunkt(
        List<BehandlingÅrsakType> behandlingsårsakerForBehandling,
        List<DatoIntervallEntitet> perioder,
        List<MottattDokument> gyldigeDokumenter,
        List<UngdomsytelseBekreftetPeriodeEndring> bekreftelser,
        String ventefrist,
        Optional<Aksjonspunkt> eksisterendeAksjonspunkt) {
        final var gruppertePeriodeEndringerPåEndringstype = finnBekreftelserGruppertPåEndringstype(bekreftelser);
        final var eksisterendeFrist = eksisterendeAksjonspunkt.map(Aksjonspunkt::getFristTid);

        if (behandlingsårsakerForBehandling.stream().anyMatch(RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM::equals)) {
            final var sisteBekreftetEndring = finnSisteMottatteBekreftelseForEndringstype(gruppertePeriodeEndringerPåEndringstype, gyldigeDokumenter, UngdomsytelsePeriodeEndringType.ENDRET_OPPHØRSDATO);
            if (sisteBekreftetEndring.isEmpty() || !harMatchendeSluttdato(perioder, sisteBekreftetEndring.get())) {
                return Optional.of(aksjonspunktMedFristOgVenteÅrsak(Venteårsak.VENTER_BEKREFTELSE_ENDRET_OPPHØR_UNGDOMSPROGRAM, eksisterendeFrist, ventefrist));
            }
        }

        if (behandlingsårsakerForBehandling.stream().anyMatch(RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM::equals)) {
            final var sisteBekreftetEndring = finnSisteMottatteBekreftelseForEndringstype(gruppertePeriodeEndringerPåEndringstype, gyldigeDokumenter, UngdomsytelsePeriodeEndringType.ENDRET_STARTDATO);
            if (sisteBekreftetEndring.isEmpty() || !harMatchendeStartdato(perioder, sisteBekreftetEndring.get())) {
                return Optional.of(aksjonspunktMedFristOgVenteÅrsak(Venteårsak.VENTER_BEKREFTELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM, eksisterendeFrist, ventefrist));
            }

        }

        return Optional.empty();
    }

    private static AksjonspunktResultat aksjonspunktMedFristOgVenteÅrsak(Venteårsak venterBekreftelseEndretStartdatoUngdomsprogram, Optional<LocalDateTime> eksisterendeFrist, String ventefrist) {
        return AksjonspunktResultat.opprettForAksjonspunktMedFrist(
            AUTO_SATT_PÅ_VENT_REVURDERING,
            venterBekreftelseEndretStartdatoUngdomsprogram,
            eksisterendeFrist.orElse(LocalDateTime.now().plus(Period.parse(ventefrist)).minusDays(1)));
    }


    private static Map<UngdomsytelsePeriodeEndringType, List<UngdomsytelseBekreftetPeriodeEndring>> finnBekreftelserGruppertPåEndringstype(List<UngdomsytelseBekreftetPeriodeEndring> bekreftelser) {
        return bekreftelser.stream().collect(Collectors.groupingBy(UngdomsytelseBekreftetPeriodeEndring::getEndringType));
    }

    private static boolean harMatchendeSluttdato(List<DatoIntervallEntitet> perioder, UngdomsytelseBekreftetPeriodeEndring sisteBekreftetEndring) {
        return perioder.stream().anyMatch(p -> p.getTomDato().equals(sisteBekreftetEndring.getDato()));
    }

    private static boolean harMatchendeStartdato(List<DatoIntervallEntitet> perioder, UngdomsytelseBekreftetPeriodeEndring sisteBekreftetEndring) {
        return perioder.stream().anyMatch(p -> p.getFomDato().equals(sisteBekreftetEndring.getDato()));
    }

    private static Optional<UngdomsytelseBekreftetPeriodeEndring> finnSisteMottatteBekreftelseForEndringstype(Map<UngdomsytelsePeriodeEndringType, List<UngdomsytelseBekreftetPeriodeEndring>> gruppertePeriodeEndringerPåEndringstype,
                                                                                                              List<MottattDokument> gyldigeDokumenter,
                                                                                                              UngdomsytelsePeriodeEndringType endringType) {
        final var bekreftetEndringer = gruppertePeriodeEndringerPåEndringstype.getOrDefault(endringType, List.of());
        return bekreftetEndringer.stream()
            .max(Comparator.comparing(e -> finnMottattTidspunkt(e, gyldigeDokumenter)));
    }

    private static LocalDateTime finnMottattTidspunkt(UngdomsytelseBekreftetPeriodeEndring e, List<MottattDokument> gyldigeDokumenter) {
        return gyldigeDokumenter.stream().filter(d -> d.getJournalpostId().equals(e.getJournalpostId())).map(MottattDokument::getMottattTidspunkt).findFirst().orElseThrow();
    }


}
