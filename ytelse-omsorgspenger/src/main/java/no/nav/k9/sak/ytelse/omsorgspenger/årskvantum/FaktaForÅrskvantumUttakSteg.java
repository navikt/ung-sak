package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.KONTROLLER_FAKTA_UTTAK;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;

/** Samle sammen fakta for fravær. */
@ApplicationScoped
@BehandlingStegRef(stegtype = KONTROLLER_FAKTA_UTTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef(OMSORGSPENGER)
public class FaktaForÅrskvantumUttakSteg implements BehandlingSteg {

    @SuppressWarnings("unused")
    private OmsorgspengerGrunnlagRepository grunnlagRepository;

    protected FaktaForÅrskvantumUttakSteg() {
        // for proxy
    }

    @Inject
    public FaktaForÅrskvantumUttakSteg(OmsorgspengerGrunnlagRepository grunnlagRepository) {
        this.grunnlagRepository = grunnlagRepository;
    }

    @SuppressWarnings("unused")
    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        AktørId aktørId = kontekst.getAktørId();
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
