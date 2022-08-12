package no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.formidling.kontrakt.informasjonsbehov.InformasjonsbehovListeDto;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

@ApplicationScoped
public class VurderBrevTjeneste {

    private K9FormidlingKlient formidlingKlient;

    VurderBrevTjeneste() {
        // for CDI proxy
    }

    public VurderBrevTjeneste(K9FormidlingKlient formidlingKlient) {
        this.formidlingKlient = formidlingKlient;
    }

    public boolean trengerManueltBrev(Behandling behandling) {
        InformasjonsbehovListeDto informasjonsbehov = formidlingKlient.hentInformasjonsbehov(behandling.getUuid(), behandling.getFagsakYtelseType());
        return !informasjonsbehov.getInformasjonsbehov().isEmpty();
    }
}
