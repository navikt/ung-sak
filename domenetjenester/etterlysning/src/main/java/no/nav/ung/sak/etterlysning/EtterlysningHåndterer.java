package no.nav.ung.sak.etterlysning;

import no.nav.ung.kodeverk.etterlysning.EtterlysningType;

import java.time.LocalDateTime;

public interface EtterlysningHåndterer {
    void håndterOpprettelse(long behandlingId, EtterlysningType etterlysningType);

    default LocalDateTime getFrist() {
        return LocalDateTime.now().plusDays(14);
    }
}
