package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.vedtak.BekreftVedtakUtenTotrinnskontrollDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftVedtakUtenTotrinnskontrollDto.class, adapter=AksjonspunktOppdaterer.class)
class BekreftVedtakUtenTotrinnskontrollOppdaterer extends AbstractVedtaksbrevOverstyringshåndterer implements AksjonspunktOppdaterer<BekreftVedtakUtenTotrinnskontrollDto> {

    BekreftVedtakUtenTotrinnskontrollOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftVedtakUtenTotrinnskontrollOppdaterer(VedtakVarselRepository vedtakVarselRepository,
                                                       HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                                       OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                                       VedtakTjeneste vedtakTjeneste) {
        super(vedtakVarselRepository, historikkApplikasjonTjeneste, opprettToTrinnsgrunnlag, vedtakTjeneste);
    }

    @Override
    public OppdateringResultat oppdater(BekreftVedtakUtenTotrinnskontrollDto dto, AksjonspunktOppdaterParameter param) {
        OppdateringResultat.Builder builder = OppdateringResultat.utenTransisjon();
        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            super.oppdaterVedtaksbrev(dto, param, builder);
            builder.medFremoverHopp(FellesTransisjoner.FREMHOPP_TIL_FATTE_VEDTAK);
        }
        return builder.build();
    }
}
