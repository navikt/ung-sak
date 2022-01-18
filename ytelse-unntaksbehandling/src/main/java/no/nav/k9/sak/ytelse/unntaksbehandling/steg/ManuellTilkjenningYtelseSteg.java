package no.nav.k9.sak.ytelse.unntaksbehandling.steg;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;

@FagsakYtelseTypeRef
@BehandlingStegRef(kode = "MANUELL_TILKJENNING_YTELSE")
@BehandlingTypeRef("BT-010")
@ApplicationScoped
public class ManuellTilkjenningYtelseSteg implements BehandlingSteg {

    private VedtakVarselRepository vedtakVarselRepository;

    protected ManuellTilkjenningYtelseSteg() {
        // for CDI proxy
    }

    @Inject
    public ManuellTilkjenningYtelseSteg(VedtakVarselRepository vedtakVarselRepository) {
        this.vedtakVarselRepository = vedtakVarselRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();

        var vedtakVarsel = vedtakVarselRepository.hentHvisEksisterer(behandlingId).orElse(new VedtakVarsel());
        vedtakVarselRepository.lagre(behandlingId, vedtakVarsel);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
