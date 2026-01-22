package no.nav.ung.sak.oppgave;

import no.nav.ung.sak.oppgave.varsel.*;
import no.nav.ung.sak.felles.typer.AktørId;
import no.nav.ung.sak.oppgave.varsel.oppgavedata.*;

import java.time.LocalDate;
import java.util.Set;

/**
 * Example class showing how to create and use BrukerdialogOppgaveEntitet with different OppgaveData types.
 * This demonstrates the polymorphic JSON mapping of the data field.
 */
public class OppgaveDataExample {

    /**
     * Example: Creating an oppgave with EndretStartdatoOppgaveData
     */
    public static BrukerdialogVarselEntitet createEndretStartdatoVarsel(AktørId aktørId) {
        BrukerdialogVarselEntitet entitet = new BrukerdialogVarselEntitet();

        // Create the specific data type
        EndretStartdatoOppgaveData data = new EndretStartdatoOppgaveData(
            LocalDate.of(2026, 2, 1),  // nyStartdato
            LocalDate.of(2026, 1, 15)  // forrigeStartdato
        );

        // Set the polymorphic data field
        entitet.setData(data);

        return entitet;
    }

    /**
     * Example: Creating an oppgave with KontrollerRegisterInntektOppgaveData
     */
    public static BrukerdialogVarselEntitet createKontrollerInntektVarsel(AktørId aktørId) {
        BrukerdialogVarselEntitet entitet = new BrukerdialogVarselEntitet();

        // Create register income data
        ArbeidOgFrilansRegisterInntektData arbeidsinntekt =
            new ArbeidOgFrilansRegisterInntektData(25000, "Bedrift AS");

        YtelseRegisterInntektData ytelseinntekt =
            new YtelseRegisterInntektData(15000, YtelseType.SYKEPENGER);

        RegisterinntektData registerinntekt = new RegisterinntektData(
            java.util.List.of(arbeidsinntekt),
            java.util.List.of(ytelseinntekt)
        );

        // Create the specific data type
        KontrollerRegisterInntektOppgaveData data = new KontrollerRegisterInntektOppgaveData(
            registerinntekt,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            false
        );

        entitet.setData(data);

        return entitet;
    }

    /**
     * Example: Creating an oppgave with EndretPeriodeOppgaveData
     */
    public static BrukerdialogVarselEntitet createEndretPeriodeVarsel(AktørId aktørId) {
        BrukerdialogVarselEntitet entitet = new BrukerdialogVarselEntitet();

        PeriodeDTO nyPeriode = new PeriodeDTO(
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 6, 30)
        );

        PeriodeDTO forrigePeriode = new PeriodeDTO(
            LocalDate.of(2026, 1, 15),
            LocalDate.of(2026, 5, 31)
        );

        EndretPeriodeOppgaveData data = new EndretPeriodeOppgaveData(
            nyPeriode,
            forrigePeriode,
            Set.of(PeriodeEndringType.ENDRET_STARTDATO, PeriodeEndringType.ENDRET_SLUTTDATO)
        );

        entitet.setData(data);

        return entitet;
    }

    /**
     * Example: Reading and casting the polymorphic data field
     */
    public static void processOppgaveData(BrukerdialogOppgaveEntitet entitet) {
        OppgaveData data = entitet.getData();

        // The data field will be automatically deserialized to the correct type
        // based on the "type" property in the JSON

        if (data instanceof EndretStartdatoOppgaveData startdatoData) {
            System.out.println("Ny startdato: " + startdatoData.getNyStartdato());
            System.out.println("Forrige startdato: " + startdatoData.getForrigeStartdato());
        } else if (data instanceof KontrollerRegisterInntektOppgaveData inntektData) {
            System.out.println("Periode: " + inntektData.getFomDato() + " - " + inntektData.getTomDato());
            System.out.println("Antall arbeidsgivere: " +
                inntektData.getRegisterinntekt().getArbeidOgFrilansInntekter().size());
        } else if (data instanceof EndretPeriodeOppgaveData periodeData) {
            System.out.println("Endringer: " + periodeData.getEndringer());
        }
        // ... handle other types
    }
}

