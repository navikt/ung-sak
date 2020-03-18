package no.nav.k9.sak.domene.uttak;

import java.time.LocalDate;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.uttak.UtfallType;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.Periode;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.Uttaksplan;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.Uttaksplanperiode;

@ApplicationScoped
public class OpphørUttakTjeneste {

    private UttakTjeneste uttakTjeneste;

    OpphørUttakTjeneste() {
        //
    }

    @Inject
    public OpphørUttakTjeneste(UttakTjeneste uttakTjeneste) {
        this.uttakTjeneste = uttakTjeneste;
    }

    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        if (!ref.getBehandlingResultat().isBehandlingsresultatOpphørt()) {
            return Optional.empty();
        }
        LocalDate skjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();
        Uttaksplan uttaksplan = hentUttakResultatFor(ref.getBehandlingUuid());
        LocalDate opphørsdato = utledOpphørsdatoFraUttak(uttaksplan, skjæringstidspunkt);

        return Optional.ofNullable(opphørsdato);
    }

    private Uttaksplan hentUttakResultatFor(UUID behandlingId) {
        return uttakTjeneste.hentUttaksplan(behandlingId).orElse(null);
    }

    private LocalDate utledOpphørsdatoFraUttak(Uttaksplan uttaksplan, LocalDate skjæringstidspunkt) {
        // FIXME K9 UTTAK: Mulig dette ikke trengs?  Kan ha flere perioder med avslått/innvilget om hverandre?
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
