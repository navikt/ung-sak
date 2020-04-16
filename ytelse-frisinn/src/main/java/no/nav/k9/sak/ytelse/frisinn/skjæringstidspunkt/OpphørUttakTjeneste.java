package no.nav.k9.sak.ytelse.frisinn.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.UUID;

import no.nav.k9.kodeverk.uttak.UtfallType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.domene.uttak.uttaksplan.Uttaksplan;
import no.nav.k9.sak.domene.uttak.uttaksplan.Uttaksplanperiode;
import no.nav.k9.sak.kontrakt.uttak.Periode;

class OpphørUttakTjeneste {

    private UttakTjeneste uttakTjeneste;

    OpphørUttakTjeneste(UttakTjeneste uttakTjeneste) {
        this.uttakTjeneste = uttakTjeneste;
    }

    Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        if (!ref.getBehandlingResultat().isBehandlingsresultatOpphørt()) {
            return Optional.empty();
        }
        LocalDate skjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();
        Uttaksplan uttaksplan = hentUttakResultatFor(ref.getBehandlingUuid()).orElse(null);
        LocalDate opphørsdato = utledOpphørsdatoFraUttak(uttaksplan, skjæringstidspunkt);

        return Optional.ofNullable(opphørsdato);
    }
    
    boolean harAvslåttUttakPeriode(UUID behandlingUuid) {
        var uttaksplanOpt = hentUttakResultatFor(behandlingUuid);
        return uttaksplanOpt.map(ut -> ut.harAvslåttePerioder()).orElse(false);
    }

    private Optional<Uttaksplan> hentUttakResultatFor(UUID behandlingId) {
        return uttakTjeneste.hentUttaksplan(behandlingId);
    }

    private LocalDate utledOpphørsdatoFraUttak(Uttaksplan uttaksplan, LocalDate skjæringstidspunkt) {
        // FIXME K9 UTTAK: Mulig dette ikke trengs? Kan ha flere perioder med avslått/innvilget om hverandre?
        NavigableMap<Periode, Uttaksplanperiode> perioder = uttaksplan != null ? uttaksplan.getPerioderReversert() : Collections.emptyNavigableMap();
        // Finn fom-dato i første periode av de siste sammenhengende periodene med opphørårsaker
        LocalDate fom = null;
        for (var entry : perioder.entrySet()) {
            var periode = entry.getKey();
            var info = entry.getValue();
            if (UtfallType.AVSLÅTT.equals(info.getUtfall())) {
                fom = periode.getFom();
            } else if (fom != null && UtfallType.INNVILGET.equals(info.getUtfall())) {
                return fom;
            }
        }
        // bruk skjæringstidspunkt hvis fom = null eller tidligste periode i uttaksplan er opphørt eller avslått
        return skjæringstidspunkt;
    }

}
