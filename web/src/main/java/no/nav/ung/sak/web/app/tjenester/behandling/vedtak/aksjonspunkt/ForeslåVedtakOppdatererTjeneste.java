package no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.historikk.HistorikkinnslagType;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.domene.vedtak.VedtakTjeneste;
import no.nav.ung.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.ung.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.ung.sak.kontrakt.vedtak.VedtaksbrevOverstyringDto;

@Dependent
public class ForeslåVedtakOppdatererTjeneste {
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag;
    private VedtakTjeneste vedtakTjeneste;

    @Inject
    public ForeslåVedtakOppdatererTjeneste(HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                           OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                           VedtakTjeneste vedtakTjeneste) {
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.opprettToTrinnsgrunnlag = opprettToTrinnsgrunnlag;
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

        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder()
            .medResultat(vedtakResultatType)
            .medSkjermlenke(SkjermlenkeType.VEDTAK)
            .medHendelse(HistorikkinnslagType.FORSLAG_VEDTAK);

        Historikkinnslag innslag = new Historikkinnslag();
        innslag.setType(HistorikkinnslagType.FORSLAG_VEDTAK);
        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        innslag.setBehandlingId(behandling.getId());
        tekstBuilder.build(innslag);
        historikkApplikasjonTjeneste.lagInnslag(innslag);
    }

}
