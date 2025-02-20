package no.nav.ung.sak.formidling.vedtak;

import java.util.Set;

public record DetaljertResultat(
    Set<DetaljertResultatType> resultatTyper
) {
}
