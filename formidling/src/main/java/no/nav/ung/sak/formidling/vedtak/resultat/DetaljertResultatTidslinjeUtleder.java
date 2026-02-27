package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

public interface DetaljertResultatTidslinjeUtleder {
    LocalDateTimeline<DetaljertResultat> utledDetaljertResultat(Behandling behandling);
}
