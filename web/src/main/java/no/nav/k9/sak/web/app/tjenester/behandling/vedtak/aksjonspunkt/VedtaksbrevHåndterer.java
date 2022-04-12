package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.domene.vedtak.VedtakTjeneste;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.vedtak.VedtaksbrevOverstyringDto;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@ApplicationScoped
public class VedtaksbrevHåndterer {
    protected HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    VedtakVarselRepository vedtakVarselRepository;
    protected OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag;
    private VedtakTjeneste vedtakTjeneste;

    VedtaksbrevHåndterer() {
        //
    }

    @Inject
    public VedtaksbrevHåndterer(VedtakVarselRepository vedtakVarselRepository,
                                HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                VedtakTjeneste vedtakTjeneste) {
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.vedtakVarselRepository = vedtakVarselRepository;
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

    void oppdaterVedtaksvarsel(VedtaksbrevOverstyringDto dto, Long behandlingId, FagsakYtelseType fagsakYtelseType) {
        if (fagsakYtelseType != FagsakYtelseType.FRISINN) return;

        vedtakVarselRepository.hentHvisEksisterer(behandlingId).ifPresent(v -> {
            v.setRedusertUtbetalingÅrsaker(dto.getRedusertUtbetalingÅrsaker());
            if (dto.isSkalUndertrykkeBrev()) {
                v.setVedtaksbrev(Vedtaksbrev.INGEN);
            }
            vedtakVarselRepository.lagre(behandlingId, v);
        });
    }

    void oppdaterBegrunnelse(Behandling behandling) {
        behandling.setAnsvarligSaksbehandler(getCurrentUserId());
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

    protected String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

}
