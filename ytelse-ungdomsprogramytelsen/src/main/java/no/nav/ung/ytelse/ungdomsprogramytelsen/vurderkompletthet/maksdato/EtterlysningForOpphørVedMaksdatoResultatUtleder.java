package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.maksdato;

import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.MaksdatoOpphørVarslingPeriode;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.EtterlysningOgGrunnlag;

import java.time.LocalDate;

public class EtterlysningForOpphørVedMaksdatoResultatUtleder {

    static ResultatType utledResultat(EtterlysningForMaksdatoInput input) {
        if (input.tomDato.isBefore(input.maksdato)) {
            // Opphør er satt til en dato før maksdato
            // Håndteres via vanlig opphørsvarsling
            // Avbryter eventuelt eksisterende etterlysning og varsel
            return input.eksisterendeEtterlysning != null && input.eksisterendeEtterlysning.etterlysningData().status().equals(EtterlysningStatus.VENTER) ? ResultatType.AVBRYT_ETTERLYSNING : ResultatType.INGEN_ENDRING;
        }


        if (!MaksdatoOpphørVarslingPeriode.harPassertVarseldato(input.maksdato)) {
            // Vi har ikke passert varslingsdatoen. Dette betyr mest sannsynlig at maksdato har blitt flyttet etter opprettelse av behandlingen
            // Avbryter eventuelt eksisterende etterlysning og varsel
            return input.eksisterendeEtterlysning != null && input.eksisterendeEtterlysning.etterlysningData().status().equals(EtterlysningStatus.VENTER) ? ResultatType.AVBRYT_ETTERLYSNING : ResultatType.INGEN_ENDRING;
        }

        if (input.eksisterendeEtterlysning != null) {
            // Vi sjekker om eksisterende etterlysning gjelder for samme maksdato
            UngdomsprogramPeriodeGrunnlag etterlystForGrunnlag = input.eksisterendeEtterlysning.grunnlag();
            LocalDate etterlystForMaksdato = etterlystForGrunnlag.getPeriodeMaksDato().orElseThrow(() -> new IllegalStateException("Forventer maksdato"));
            if (etterlystForMaksdato.equals(input.maksdato)) {
                return ResultatType.INGEN_ENDRING;
            } else {
                EtterlysningStatus status = input.eksisterendeEtterlysning.etterlysningData().status();
                if (status.equals(EtterlysningStatus.OPPRETTET) || status.equals(EtterlysningStatus.VENTER)) {
                    return ResultatType.ERSTATT_EKSISTERENDE;
                } else {
                    return ResultatType.OPPRETT_ETTERLYSNING;
                }
            }

        } else {
            return ResultatType.OPPRETT_ETTERLYSNING;
        }
    }



    record EtterlysningForMaksdatoInput(
        LocalDate tomDato,
        LocalDate maksdato,
        LocalDate dagensDato,
        EtterlysningOgGrunnlag eksisterendeEtterlysning) {
    }


    enum ResultatType {
        OPPRETT_ETTERLYSNING,
        AVBRYT_ETTERLYSNING,
        ERSTATT_EKSISTERENDE,
        INGEN_ENDRING
    }


}
