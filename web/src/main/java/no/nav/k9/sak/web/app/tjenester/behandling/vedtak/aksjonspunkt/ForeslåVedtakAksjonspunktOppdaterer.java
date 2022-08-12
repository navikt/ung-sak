package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev.VurderBrevTjeneste;
import no.nav.k9.sak.kontrakt.vedtak.ForeslaVedtakAksjonspunktDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ForeslaVedtakAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class ForeslåVedtakAksjonspunktOppdaterer implements AksjonspunktOppdaterer<ForeslaVedtakAksjonspunktDto> {

    private VedtaksbrevHåndterer vedtaksbrevHåndterer;
    private VurderBrevTjeneste vurderBrevTjeneste;

    ForeslåVedtakAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ForeslåVedtakAksjonspunktOppdaterer(VedtaksbrevHåndterer vedtaksbrevHåndterer, VurderBrevTjeneste vurderBrevTjeneste) {
        this.vedtaksbrevHåndterer = vedtaksbrevHåndterer;
        this.vurderBrevTjeneste = vurderBrevTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(ForeslaVedtakAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        vedtaksbrevHåndterer.oppdaterBegrunnelse(behandling);

        OppdateringResultat.Builder builder = OppdateringResultat.builder();
        if (Set.of(FagsakYtelseType.PSB, FagsakYtelseType.PPN).contains( behandling.getFagsakYtelseType())) {
            if (dto.isSkalBrukeOverstyrendeFritekstBrev() || vurderBrevTjeneste.trengerManueltBrev(behandling)) {
                builder.medTotrinn();
            }
        }

        vedtaksbrevHåndterer.håndterTotrinnOgHistorikkinnslag(dto, param, builder);

        vedtaksbrevHåndterer.oppdaterVedtaksvarsel(dto, param.getBehandlingId(), param.getRef().getFagsakYtelseType());

        return builder.build();
    }

}
