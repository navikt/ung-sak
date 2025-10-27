package no.nav.ung.sak.kontroll;


import no.nav.ung.kodeverk.varsel.EtterlysningStatus;

import java.util.Objects;

public record InntektskontrollEtterlysningInfo(EtterlysningStatus etterlysningStatus, Boolean harUttalelse) {

    public InntektskontrollEtterlysningInfo {
        if (etterlysningStatus == EtterlysningStatus.MOTTATT_SVAR) {
            Objects.requireNonNull(harUttalelse);
        }
    }

    public boolean erBesvartOgHarUttalelse() {
        return etterlysningStatus == EtterlysningStatus.MOTTATT_SVAR && harUttalelse();
    }

    public boolean erBesvartOgHarIkkeUttalelse() {
        return etterlysningStatus == EtterlysningStatus.MOTTATT_SVAR && !harUttalelse();
    }

}
