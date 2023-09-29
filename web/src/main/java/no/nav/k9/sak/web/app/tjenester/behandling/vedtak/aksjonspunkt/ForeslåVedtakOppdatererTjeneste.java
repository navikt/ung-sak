package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.domene.vedtak.VedtakTjeneste;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.vedtak.VedtaksbrevOverstyringDto;

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
            opprettAksjonspunktForFatterVedtak(builder);
        }
        opprettHistorikkinnslag(behandling);
    }

    private void opprettAksjonspunktForFatterVedtak(OppdateringResultat.Builder builder) {
        builder.medEkstraAksjonspunktResultat(AksjonspunktDefinisjon.FATTER_VEDTAK, AksjonspunktStatus.OPPRETTET);
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
