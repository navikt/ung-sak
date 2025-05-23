package no.nav.ung.sak.behandlingskontroll.impl;

import java.util.Collections;
import java.util.List;

import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;

public class TestStegKonfig {
    private final BehandlingStegType behandlingStegType;
    private final BehandlingType behandlingType;
    private final FagsakYtelseType fagsakYtelseType;
    private final BehandlingSteg steg;
    private final List<AksjonspunktDefinisjon> utgangAksjonspunkter;

    public TestStegKonfig(BehandlingStegType behandlingStegType, BehandlingType behandlingType, FagsakYtelseType fagsakYtelseType, BehandlingSteg steg, List<AksjonspunktDefinisjon> utgangAksjonspunkter) {
        this.behandlingStegType = behandlingStegType;
        this.behandlingType = behandlingType;
        this.fagsakYtelseType = fagsakYtelseType;
        this.steg = steg;
        this.utgangAksjonspunkter = utgangAksjonspunkter;
    }

    public TestStegKonfig(BehandlingStegType behandlingStegType, BehandlingType behandlingType, FagsakYtelseType fagsakYtelseType, BehandlingSteg steg) {
        this(behandlingStegType, behandlingType, fagsakYtelseType, steg, Collections.emptyList());
    }

    public BehandlingStegType getBehandlingStegType() {
        return behandlingStegType;
    }

    public BehandlingType getBehandlingType() {
        return behandlingType;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return fagsakYtelseType;
    }

    public BehandlingSteg getSteg() {
        return steg;
    }


    public List<AksjonspunktDefinisjon> getUtgangAksjonspunkter() {
        return utgangAksjonspunkter;
    }
}
