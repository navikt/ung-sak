package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.domene.vedtak.VedtakTjeneste;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.vedtak.ForeslaVedtakAksjonspunktDto;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ForeslaVedtakAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class ForeslåVedtakAksjonspunktOppdaterer extends AbstractVedtaksbrevOverstyringshåndterer implements AksjonspunktOppdaterer<ForeslaVedtakAksjonspunktDto> {

    ForeslåVedtakAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public ForeslåVedtakAksjonspunktOppdaterer(VedtakVarselRepository vedtakVarselRepository,
                                               HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                               OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                               VedtakTjeneste vedtakTjeneste) {
        super(vedtakVarselRepository, historikkApplikasjonTjeneste, opprettToTrinnsgrunnlag, vedtakTjeneste);
    }

    @Override
    public OppdateringResultat oppdater(ForeslaVedtakAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        String begrunnelse = dto.getBegrunnelse();
        Behandling behandling = param.getBehandling();
        oppdaterBegrunnelse(behandling, begrunnelse);

        OppdateringResultat.Builder builder = OppdateringResultat.utenTransisjon();
        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            super.oppdaterVedtaksbrevForFritekst(dto, param, builder);
        } else {
            opprettAksjonspunktForFatterVedtak(builder);
            opprettToTrinnsgrunnlag.settNyttTotrinnsgrunnlag(behandling);
            opprettHistorikkinnslag(behandling);
        }

        oppdaterVedtaksvarsel(dto, param.getBehandlingId());
        return builder.build();
    }

    private void oppdaterBegrunnelse(Behandling behandling, String begrunnelse) {
        vedtakVarselRepository.hentHvisEksisterer(behandling.getId()).ifPresent(behandlingsresultat -> {
            if ((behandling.getBehandlingResultatType().isBehandlingsresultatAvslåttOrOpphørt() || begrunnelse != null)
                || skalNullstilleFritekstfelt(behandling, behandlingsresultat)) {
                behandlingsresultat.setAvslagarsakFritekst(begrunnelse);
            }
        });
        behandling.setAnsvarligSaksbehandler(getCurrentUserId());
    }

    private boolean skalNullstilleFritekstfelt(Behandling behandling, VedtakVarsel behandlingsresultat) {
        return !behandling.getBehandlingResultatType().isBehandlingsresultatAvslåttOrOpphørt()
            && behandlingsresultat.getAvslagarsakFritekst() != null;
    }

    protected String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }
}
