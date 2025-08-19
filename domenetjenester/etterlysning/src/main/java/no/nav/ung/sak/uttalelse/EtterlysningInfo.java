package no.nav.ung.sak.uttalelse;


import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;

import java.util.Objects;

public record EtterlysningInfo(EtterlysningStatus etterlysningStatus, Boolean harUttalelse) {

    public EtterlysningInfo {
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
