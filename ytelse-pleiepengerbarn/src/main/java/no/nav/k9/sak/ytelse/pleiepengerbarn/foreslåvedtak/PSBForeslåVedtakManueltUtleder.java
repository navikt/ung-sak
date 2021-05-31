package no.nav.k9.sak.ytelse.pleiepengerbarn.foreslåvedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.formidling.kontrakt.informasjonsbehov.InformasjonsbehovListeDto;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ForeslåVedtakManueltUtleder;
import no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev.K9FormidlingKlient;


@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PSBForeslåVedtakManueltUtleder implements ForeslåVedtakManueltUtleder {

    private K9FormidlingKlient formidlingKlient;
    private Boolean lansert;

    PSBForeslåVedtakManueltUtleder() {
        //for CDI proxy
    }

    @Inject
    public PSBForeslåVedtakManueltUtleder(K9FormidlingKlient formidlingKlient,
                                          @KonfigVerdi(value = "FORMIDLING_RETUR_MALTYPER", defaultVerdi = "true") Boolean lansert) {
        this.formidlingKlient = formidlingKlient;
        this.lansert = lansert;
    }

    @Override
    public boolean skalOppretteForeslåVedtakManuelt(Behandling behandling) {
        return erManuellRevurdering(behandling) || trengerManueltBrev(behandling);
    }

    private boolean erManuellRevurdering(Behandling behandling) {
        return BehandlingType.REVURDERING == behandling.getType() && behandling.erManueltOpprettet();
    }

    private boolean trengerManueltBrev(Behandling behandling) {
        if (!lansert) {
            return false;
        }
        InformasjonsbehovListeDto informasjonsbehov = formidlingKlient.hentInformasjonsbehov(behandling.getUuid(), behandling.getFagsakYtelseType());
        return !informasjonsbehov.getInformasjonsbehov().isEmpty();
    }

}
