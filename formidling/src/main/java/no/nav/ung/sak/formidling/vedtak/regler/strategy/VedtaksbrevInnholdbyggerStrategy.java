package no.nav.ung.sak.formidling.vedtak.regler.strategy;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;

import java.util.List;

/**
 * Regler for å bestemme riktig bygger av vedtaksbrev. Kan også avgjøre om brev IKKE skal bestilles.
 *
 * <p>En strategi er <em>frittstående</em>: {@link #evaluer} skal kun forholde seg til sin egen flyt, og aldri
 * inspisere andre strategiers resultater. En strategi som ikke er relevant for behandlingen returnerer en tom
 * liste. Hvordan resultater kombineres på tvers av strategier styres av resolveren via {@link #presedens()}.</p>
 */
public interface VedtaksbrevInnholdbyggerStrategy {

    /**
     * Evaluerer egen flyt og returnerer 0..n resultater (brev og/eller ingen-brev-årsaker). Returner tom liste
     * dersom strategien ikke er relevant for behandlingen.
     */
    List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat);

    /**
     * Presedens som styrer hvordan resolveren kombinerer denne strategien med de andre. Default {@link Presedens#NORMAL}.
     */
    default Presedens presedens() {
        return Presedens.NORMAL;
    }

}
