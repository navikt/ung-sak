package no.nav.ung.sak.behandlingskontroll;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.ung.sak.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;

/**
 * Definerer metoder for å inspisere state-machine for en gitt behandling type.
 *
 * Hver behandling type er knyttet til en egen BehandlingModell.
 */
public interface BehandlingModell {

    /** Gjelder kun steg ETTER angitt steg (eksklusv angitt steg). */
    Set<String> finnAksjonspunktDefinisjonerEtter(BehandlingStegType steg);

    /**
     * Gjelder kun steg ETTER angitt steg (inklusiv angitt steg). Dersom medInngangOgså tas også aksjonspunt som skal
     * være løst også ved Inngang med, ellers kun ved Utgang av steget
     */
    Set<String> finnAksjonspunktDefinisjonerFraOgMed(BehandlingStegType steg);

    Set<String> finnAksjonspunktDefinisjoner(BehandlingStegType stegType);

    BehandlingStegModell finnForrigeSteg(BehandlingStegType stegType);

    BehandlingStegModell finnForrigeSteg(String stegKode);

    BehandlingStegModell finnFørsteSteg(BehandlingStegType... behandlingStegTyper);

    BehandlingStegModell finnNesteSteg(BehandlingStegType stegType);

    BehandlingStegModell finnNesteSteg(String stegKode);

    BehandlingStegModell finnSteg(BehandlingStegType stegType);

    BehandlingStegModell finnSteg(String stegKode);

    Optional<BehandlingStegStatus> finnStegStatusFor(BehandlingStegType stegType, Collection<String> aksjonspunktKoder);

    BehandlingStegModell finnTidligsteStegFor(Collection<AksjonspunktDefinisjon> aksjonspunkter);

    BehandlingStegModell finnTidligsteStegFor(AksjonspunktDefinisjon aksjonspunkt);

    BehandlingStegModell finnTidligsteStegForAksjonspunktDefinisjon(Collection<String> aksjonspunktDefinisjoner);

    /** Behandling type modellen gjelder for. */
    BehandlingType getBehandlingType();

    Stream<BehandlingStegModell> hvertSteg();

    Stream<BehandlingStegModell> hvertStegEtter(BehandlingStegType stegType);

    BehandlingStegType finnBehandlingSteg(StartpunktType startpunkt);

    Stream<BehandlingStegModell> hvertStegFraOgMed(BehandlingStegType fraOgMedSteg);

    Stream<BehandlingStegModell> hvertStegFraOgMedTil(BehandlingStegType fraOgMedSteg, BehandlingStegType tilSteg, boolean inklusivTil);

    boolean erStegAFørStegB(BehandlingStegType stegA, BehandlingStegType stegB);

    /**
     * Beregn relativ forflytning mellom to steg.
     *
     * @param stegFørType
     * @param stegEtterType
     * @return 1 (normalt fremover), mindre enn 0 (tilbakeføring), større enn 1 (overhopp/framføring)
     */
    int relativStegForflytning(BehandlingStegType stegFørType, BehandlingStegType stegEtterType);

    /**
     * Kjør behandling fra angitt steg, med angitt visitor. Stopper når visitor ikke kan kjøre lenger.
     * @param startFraBehandlingStegType
     * @param visitor
     *
     * @return
     */
    BehandlingStegUtfall prosesserFra(BehandlingStegType startFraBehandlingStegType, BehandlingModellVisitor visitor);

    FagsakYtelseType getFagsakYtelseType();

    StegTransisjon finnTransisjon(TransisjonIdentifikator transisjonIdentifikator);

    List<BehandlingStegType> getAlleBehandlingStegTyper();

}
