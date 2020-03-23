package no.nav.k9.sak.domene.arbeidsforhold.impl;

import java.util.Optional;

import no.nav.k9.kodeverk.arbeidsforhold.BekreftetPermisjonStatus;
import no.nav.k9.sak.domene.iay.modell.BekreftetPermisjon;

final class UtledBrukAvPermisjonForWrapper {

    private UtledBrukAvPermisjonForWrapper() {
        // Skjuler default public constructor
    }

    static Boolean utled(Optional<BekreftetPermisjon> bekreftetPermisjonOpt){
        if (bekreftetPermisjonOpt.isPresent() && !BekreftetPermisjonStatus.UDEFINERT.equals(bekreftetPermisjonOpt.get().getStatus())) {
            return BekreftetPermisjonStatus.BRUK_PERMISJON.equals(bekreftetPermisjonOpt.get().getStatus());
        }
        return null;
    }

}
