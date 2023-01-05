package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_MA;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.formidling.kontrakt.informasjonsbehov.InformasjonsbehovListeDto;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.DefaultForeslåVedtakManueltUtleder;
import no.nav.k9.sak.domene.behandling.steg.foreslåvedtak.ForeslåVedtakManueltUtleder;
import no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev.K9FormidlingKlient;


@FagsakYtelseTypeRef(OMSORGSPENGER_AO)
@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
@FagsakYtelseTypeRef(OMSORGSPENGER_MA)
@ApplicationScoped
public class UtvidetRettForeslåVedtakManueltUtleder implements ForeslåVedtakManueltUtleder {

    private K9FormidlingKlient formidlingKlient;
    private DefaultForeslåVedtakManueltUtleder defaultForeslåVedtakManueltUtleder;

    UtvidetRettForeslåVedtakManueltUtleder() {
        //for CDI proxy
    }

    @Inject
    public UtvidetRettForeslåVedtakManueltUtleder(K9FormidlingKlient formidlingKlient, DefaultForeslåVedtakManueltUtleder defaultForeslåVedtakManueltUtleder) {
        this.formidlingKlient = formidlingKlient;
        this.defaultForeslåVedtakManueltUtleder = defaultForeslåVedtakManueltUtleder;
    }

    @Override
    public boolean skalOppretteForeslåVedtakManuelt(Behandling behandling) {
        return defaultForeslåVedtakManueltUtleder.skalOppretteForeslåVedtakManuelt(behandling) || trengerManueltBrev(behandling);
    }

    private boolean trengerManueltBrev(Behandling behandling) {
        InformasjonsbehovListeDto informasjonsbehov = formidlingKlient.hentInformasjonsbehov(behandling.getUuid(), behandling.getFagsakYtelseType());
        return !informasjonsbehov.getInformasjonsbehov().isEmpty();
    }

}
