package no.nav.ung.sak.formidling.vedtak;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

public interface DetaljertResultatUtleder {
    LocalDateTimeline<DetaljertResultat> utledDetaljertResultat(Behandling behandling);
}
