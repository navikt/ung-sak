package no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.domene.vedtak.VedtakTjeneste;
import no.nav.ung.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.ung.sak.kontrakt.vedtak.VedtaksbrevOverstyringDto;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnTjeneste;

import java.util.List;

@Dependent
public class ForeslåVedtakOppdatererTjeneste {
    private HistorikkinnslagRepository historikkinnslagRepository;
    private OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag;
    private TotrinnTjeneste totrinnTjeneste;
    private VedtakTjeneste vedtakTjeneste;

    @Inject
    public ForeslåVedtakOppdatererTjeneste(HistorikkinnslagRepository historikkinnslagRepository,
                                           OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag, TotrinnTjeneste totrinnTjeneste,
                                           VedtakTjeneste vedtakTjeneste) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.opprettToTrinnsgrunnlag = opprettToTrinnsgrunnlag;
        this.totrinnTjeneste = totrinnTjeneste;
        this.vedtakTjeneste = vedtakTjeneste;
    }

    void håndterTotrinnOgHistorikkinnslag(VedtaksbrevOverstyringDto dto, AksjonspunktOppdaterParameter param, OppdateringResultat.Builder builder) {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(dto.getKode());
        Behandling behandling = param.getBehandling();

        if (AksjonspunktDefinisjon.FORESLÅ_VEDTAK.equals(aksjonspunktDefinisjon)) {
            opprettToTrinnsgrunnlag.settNyttTotrinnsgrunnlag(behandling);
        }
        opprettHistorikkinnslag(behandling);
    }

    private void opprettHistorikkinnslag(Behandling behandling) {
        VedtakResultatType vedtakResultatType = vedtakTjeneste.utledVedtakResultatType(behandling);
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.VEDTAK)
            .addLinje(String.format("Vedtak foreslått og sendt til beslutter: %s", vedtakResultatType.getNavn()))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
