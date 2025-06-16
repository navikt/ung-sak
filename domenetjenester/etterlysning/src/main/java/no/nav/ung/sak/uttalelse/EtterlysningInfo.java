package no.nav.ung.sak.uttalelse;


import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;

import java.util.Objects;

public record EtterlysningInfo(EtterlysningStatus etterlysningStatus, Boolean erEndringenGodkjent) {

    public EtterlysningInfo {
        if (etterlysningStatus == EtterlysningStatus.MOTTATT_SVAR) {
            Objects.requireNonNull(erEndringenGodkjent);
        }
    }

    public boolean erBesvartOgIkkeGodkjent() {
        return etterlysningStatus == EtterlysningStatus.MOTTATT_SVAR && !erEndringenGodkjent();
    }

}
