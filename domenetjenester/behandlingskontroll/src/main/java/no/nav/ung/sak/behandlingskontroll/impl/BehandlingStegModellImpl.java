package no.nav.ung.sak.behandlingskontroll.impl;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.sak.behandlingskontroll.BehandlingModell;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;

import java.util.*;

/**
 * Modellerer ett behandlingssteg, inklusiv hvilke aksjonspunkter må løses før/etter steget.
 * Dersom det ved kjøring oppdages aksjonspunkter som ikke er registrert må disse løses før utgang av et
 * behandlingssteg.
 */
class BehandlingStegModellImpl implements BehandlingStegModell {

    /**
     * Aksjonspunkter som må løses ved utgang fra behandlingsteg.
     */
    private final Set<String> aksjonpunktKoder = new LinkedHashSet<>();
    private Instance<BehandlingSteg> stegInstances;
    private BehandlingSteg steg;
    private BehandlingStegType behandlingStegType;
    /**
     * Hver steg modell må tilhøre en BehandlingModell som beskriver hvordan de henger sammen.
     */
    private BehandlingModellImpl behandlingModell;

    /**
     * Holder for å referere til en konkret, men lazy-initialisert CDI implementasjon av et {@link BehandlingSteg}.
     */
    BehandlingStegModellImpl(BehandlingModellImpl behandlingModell,
                             Instance<BehandlingSteg> bean,
                             BehandlingStegType stegType) {
        this.stegInstances = Objects.requireNonNull(bean, "bean");
        this.behandlingModell = Objects.requireNonNull(behandlingModell, "behandlingModell");
        this.behandlingStegType = Objects.requireNonNull(stegType, "stegType");
    }

    /**
     * Direkte injisering av {@link BehandlingSteg}. For testing.
     */
    BehandlingStegModellImpl(BehandlingModellImpl behandlingModell, BehandlingSteg steg, BehandlingStegType stegType) {
        this.steg = Objects.requireNonNull(steg, "steg");
        this.behandlingModell = Objects.requireNonNull(behandlingModell, "behandlingModell");
        this.behandlingStegType = Objects.requireNonNull(stegType, "stegType");
    }

    @Override
    public BehandlingModell getBehandlingModell() {
        return behandlingModell;
    }

    Set<String> getAksjonpunktKoder() {
        return Collections.unmodifiableSet(aksjonpunktKoder);
    }

    protected void initSteg() {
        if (steg == null) {
            steg = BehandlingStegRef.Lookup
                .find(BehandlingSteg.class, stegInstances, behandlingModell.getFagsakYtelseType(), behandlingModell.getBehandlingType(), behandlingStegType)
                .orElseThrow(() -> {
                    return new IllegalStateException(
                        "Mangler steg definert for stegKode=" + behandlingStegType + " [behandlingType=" //$NON-NLS-1$ //$NON-NLS-2$
                            + behandlingModell.getBehandlingType() + ", fagsakYtelseType=" + behandlingModell.getFagsakYtelseType() //$NON-NLS-1$ //$NON-NLS-2$
                            + "]");
                });
        }
    }

    protected void leggTilAksjonspunktVurderingUtgang(String kode) {
        behandlingModell.validerErIkkeAlleredeMappet(kode);
        aksjonpunktKoder.add(kode);
    }

    void destroy() {
        if (stegInstances != null && steg != null) {
            stegInstances.destroy(steg);
        }
    }

    /**
     * Type kode for dette steget.
     */
    @Override
    public BehandlingStegType getBehandlingStegType() {
        return behandlingStegType;
    }

    /**
     * Forventet status når behandling er i steget.
     */
    @Override
    public String getForventetStatus() {
        return behandlingStegType.getDefinertBehandlingStatus().getKode();
    }

    /**
     * Implementasjon av et gitt steg i behandlingen.
     */
    @Override
    public BehandlingSteg getSteg() {
        initSteg();
        return steg;
    }

    /**
     * Avleder status behandlingsteg bør settes i gitt et sett med aksjonpunkter. Tar kun hensyn til aksjonpunkter
     * som gjelder dette steget.
     */
    Optional<BehandlingStegStatus> avledStatus(Collection<String> aksjonspunkter) {

        if (!Collections.disjoint(aksjonspunkter, aksjonpunktKoder)) { // NOSONAR
            return Optional.of(BehandlingStegStatus.UTGANG);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + behandlingStegType.getKode() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "utgangAksjonspunkter=" + aksjonpunktKoder + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "impl=" + steg //$NON-NLS-1$
            + ">"; //$NON-NLS-1$
    }
}
