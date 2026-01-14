package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.util.Optional;

import static no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste.harEndretStartdatoFraOppgittStartdatoer;

public class EtterlysningForEndretProgramperiodeResultatUtlederV2 {

    /**
     * Utleder resultat som beskriver behov for etterlysning i forbindelse med endring av ungdomsprogramperiode.
     * Denne metoden utleder kun hva som skal gjøres i forhold til eksisterende etterlysning, men gjør ikke selve endringen.
     *
     * @param input               Input til utledning, inkluderer eksisterende programperiode, etterlysninger og initielle grunnlag
     * @param behandlingReferanse Behandlingreferanse
     * @return
     */
    static ResultatType finnResultat(EndretUngdomsprogramEtterlysningInput input, BehandlingReferanse behandlingReferanse) {
        validerHøystEnProgramperiode(input);
        return utledResultat(input, behandlingReferanse);
    }

    private static ResultatType utledResultat(EndretUngdomsprogramEtterlysningInput input, BehandlingReferanse behandlingReferanse) {
        if (harEksisterendeEtterlysningOgRelevantEndringIProgramperiode(input)) {
            return utledResultatForEksisterendeEtterlysning(input);
        }
        if (harIngenEtterlysningOgEndringFraInitiell(input, behandlingReferanse)) {
            return ResultatType.OPPRETT_ETTERLYSNING;
        }
        return ResultatType.INGEN_ENDRING;
    }

    private static boolean harIngenEtterlysningOgEndringFraInitiell(EndretUngdomsprogramEtterlysningInput input, BehandlingReferanse behandlingReferanse) {
        return input.gjeldendeEtterlysningOgGrunnlag().isEmpty() && harEndretPeriodeSidenInitiell(input, behandlingReferanse);
    }

    private static ResultatType utledResultatForEksisterendeEtterlysning(EndretUngdomsprogramEtterlysningInput input) {
        var gjeldendeEtterlysning = input.gjeldendeEtterlysningOgGrunnlag().orElseThrow(() -> new IllegalStateException("Utviklerfeil: Skal ikke komme hit uten å ha eksisterende etterlysning"));
        return switch (gjeldendeEtterlysning.etterlysningData().status()) {
            case VENTER, OPPRETTET -> ResultatType.ERSTATT_EKSISTERENDE_ETTERLYSNING;
            case MOTTATT_SVAR, UTLØPT -> ResultatType.OPPRETT_ETTERLYSNING;
            default ->
                throw new IllegalStateException("Ugyldig status for gjeldende etterlysning: " + gjeldendeEtterlysning.etterlysningData().status());
        };
    }

    private static boolean harEksisterendeEtterlysningOgRelevantEndringIProgramperiode(EndretUngdomsprogramEtterlysningInput input) {
        return input.gjeldendeEtterlysningOgGrunnlag().map(it -> harEndring(it.grunnlag(), input.gjeldendePeriodeGrunnlag())).orElse(false);
    }

    private static boolean harEndretPeriodeSidenInitiell(EndretUngdomsprogramEtterlysningInput input,
                                                         BehandlingReferanse behandlingReferanse) {
        var erEndringSidenInitiell = harEndring(input.initiellPeriodegrunnlag(), input.gjeldendePeriodeGrunnlag());

        if (erEndringSidenInitiell) {
            return true;
        }

        if (behandlingReferanse.getBehandlingType() == BehandlingType.FØRSTEGANGSSØKNAD) {
            // Dersom det er førstegangssøknad må vi også sjekke om det er endringer i start dato fra det som ble oppgitt da bruker sendte inn søknaden.
            return harFjernetPeriode(input) ||
                harEndretStartdatoFraOppgittStartdatoer(input.gjeldendePeriodeGrunnlag(), input.ungdomsytelseStartdatoGrunnlag()) ||
                harSattSluttdato(input.gjeldendePeriodeGrunnlag());
        }
        return false;
    }

    private static boolean harFjernetPeriode(EndretUngdomsprogramEtterlysningInput input) {
        return input.gjeldendePeriodeGrunnlag().hentForEksaktEnPeriodeDersomFinnes().isEmpty();
    }


    private static boolean harSattSluttdato(UngdomsprogramPeriodeGrunnlag gjeldendePeriodeGrunnlag) {
        // For å hindre at sluttdato kan endres uten at bruker får varsel oppretter vi alltid en etterlysning for endret sluttdato dersom den er satt i førstegangssøknad.
        var gjeldendeSluttdato = gjeldendePeriodeGrunnlag.hentForEksaktEnPeriode().getTomDato();
        var harSattSluttdato = gjeldendeSluttdato != null && !gjeldendeSluttdato.equals(AbstractLocalDateInterval.TIDENES_ENDE);
        return harSattSluttdato;
    }

    private static boolean harEndring(UngdomsprogramPeriodeGrunnlag førsteGrunnlag, UngdomsprogramPeriodeGrunnlag andreGrunnlag) {
        return !UngdomsprogramPeriodeTjeneste.finnEndretTidslinje(Optional.of(andreGrunnlag), Optional.of(førsteGrunnlag)).isEmpty();
    }

    private static void validerHøystEnProgramperiode(EndretUngdomsprogramEtterlysningInput input) {
        // Ekstra validering for å sjekke at det kun er én programperiode i grunnlaget.
        final var programperioder = input.gjeldendePeriodeGrunnlag().getUngdomsprogramPerioder().getPerioder();
        if (programperioder.size() > 1) {
            throw new IllegalStateException("Støtter ikke flere programperioder");
        }
    }

}
