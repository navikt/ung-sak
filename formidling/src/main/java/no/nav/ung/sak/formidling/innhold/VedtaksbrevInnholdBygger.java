package no.nav.ung.sak.formidling.innhold;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;

public interface VedtaksbrevInnholdBygger {

    /**
     * Bygger komplett dto for brev template
     *
     * @param behandling
     * @param detaljertResultatTidslinje - tidslinje som angir perioder som har relevante for denne behandlingen og resultat deres
     * @return
     */
    TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje);

}


