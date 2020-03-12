package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.VarselRevurderingAksjonspunkt;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.kontrakt.behandling.revurdering.VarselRevurderingDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VarselRevurderingDto.class, adapter=AksjonspunktOppdaterer.class)
public class VarselRevurderingOppdaterer implements AksjonspunktOppdaterer<VarselRevurderingDto> {

    private DokumentBestillerTjeneste dokumentTjeneste;
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private VedtakVarselRepository vedtakVarselRepository;

    VarselRevurderingOppdaterer() {
        // CDI
    }

    @Inject
    public VarselRevurderingOppdaterer(VedtakVarselRepository vedtakVarselRepository, 
                                       DokumentBestillerTjeneste dokumentTjeneste, 
                                       HistorikkTjenesteAdapter historikkApplikasjonTjeneste) {
        this.vedtakVarselRepository = vedtakVarselRepository;
        this.dokumentTjeneste = dokumentTjeneste;
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VarselRevurderingDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        if (dto.isSendVarsel() && !harSendtVarselOmRevurdering(behandling)) {
            var adapter = new VarselRevurderingAksjonspunkt(dto.getFritekst(), dto.getBegrunnelse(), dto.getFrist(), dto.getVentearsak().getKode());
            dokumentTjeneste.håndterVarselRevurdering(behandling, adapter);
        } else if (!dto.isSendVarsel()) {
            opprettHistorikkinnslagOmIkkeSendtVarselOmRevurdering(behandling, dto, HistorikkAktør.SAKSBEHANDLER);
        }
        return OppdateringResultat.utenOveropp();
    }

    private void opprettHistorikkinnslagOmIkkeSendtVarselOmRevurdering(Behandling behandling, VarselRevurderingDto varselRevurderingDto, HistorikkAktør historikkAktør) {
        HistorikkInnslagTekstBuilder historiebygger = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.VRS_REV_IKKE_SNDT)
            .medBegrunnelse(varselRevurderingDto.getBegrunnelse());
        Historikkinnslag innslag = new Historikkinnslag();
        innslag.setAktør(historikkAktør);
        innslag.setType(HistorikkinnslagType.VRS_REV_IKKE_SNDT);
        innslag.setBehandlingId(behandling.getId());
        historiebygger.build(innslag);

        historikkApplikasjonTjeneste.lagInnslag(innslag);
    }

    private Boolean harSendtVarselOmRevurdering(Behandling behandling) {
        return vedtakVarselRepository.hentHvisEksisterer(behandling.getId()).orElse(new VedtakVarsel()).getErVarselOmRevurderingSendt();
    }

}
