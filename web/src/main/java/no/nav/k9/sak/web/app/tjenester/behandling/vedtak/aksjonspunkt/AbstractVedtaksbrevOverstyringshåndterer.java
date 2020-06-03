package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import no.finn.unleash.Unleash;
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
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.k9.sak.kontrakt.vedtak.VedtaksbrevOverstyringDto;

public abstract class AbstractVedtaksbrevOverstyringshåndterer {
    protected HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    VedtakVarselRepository vedtakVarselRepository;
    protected OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag;
    private VedtakTjeneste vedtakTjeneste;
    protected Unleash unleash;

    AbstractVedtaksbrevOverstyringshåndterer() {
        // for CDI proxy
    }

    AbstractVedtaksbrevOverstyringshåndterer(VedtakVarselRepository vedtakVarselRepository,
                                             HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                             OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                             VedtakTjeneste vedtakTjeneste) {
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.vedtakVarselRepository = vedtakVarselRepository;
        this.opprettToTrinnsgrunnlag = opprettToTrinnsgrunnlag;
        this.vedtakTjeneste = vedtakTjeneste;
        this.unleash = null;
    }

    AbstractVedtaksbrevOverstyringshåndterer(VedtakVarselRepository vedtakVarselRepository,
                                             HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                             OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                             VedtakTjeneste vedtakTjeneste,
                                             Unleash unleash) {
        this.vedtakVarselRepository = vedtakVarselRepository;
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.opprettToTrinnsgrunnlag = opprettToTrinnsgrunnlag;
        this.vedtakTjeneste = vedtakTjeneste;
        this.unleash = unleash;
    }

    void oppdaterVedtaksbrevForFritekst(VedtaksbrevOverstyringDto dto, AksjonspunktOppdaterParameter param, OppdateringResultat.Builder builder) {
        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            settFritekstBrev(param.getBehandlingId(), dto.getOverskrift(), dto.getFritekstBrev());

            Behandling behandling = param.getBehandling();
            opprettToTrinnsKontrollpunktForFritekstBrev(dto, behandling, builder);
            opprettAksjonspunktForFatterVedtak(builder);
            opprettToTrinnsgrunnlag.settNyttTotrinnsgrunnlag(behandling);
            opprettHistorikkinnslag(behandling);
        }
    }

    private void settFritekstBrev(Long behandlingId, String overskrift, String fritekst) {
        vedtakVarselRepository.hentHvisEksisterer(behandlingId).ifPresent(vedtakVarsel -> {
            vedtakVarsel.setOverskrift(overskrift);
            vedtakVarsel.setFritekstbrev(fritekst);
            vedtakVarsel.setVedtaksbrev(Vedtaksbrev.FRITEKST);
            vedtakVarselRepository.lagre(behandlingId, vedtakVarsel);
        });
    }

    private void opprettToTrinnsKontrollpunktForFritekstBrev(BekreftetAksjonspunktDto dto, Behandling behandling, OppdateringResultat.Builder builder) {
        if (!behandling.isToTrinnsBehandling()) {
            behandling.setToTrinnsBehandling();
        }
        builder.medTotrinn();
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(dto.getKode());
        if (!AksjonspunktDefinisjon.FORESLÅ_VEDTAK.equals(aksjonspunktDefinisjon)) {
            ekskluderOrginaltAksjonspunktFraTotrinnsVurdering(dto, behandling, builder);
            registrerNyttKontrollpunktIAksjonspunktRepo(behandling, builder);
        }
    }

    void opprettAksjonspunktForFatterVedtak(OppdateringResultat.Builder builder) {
        builder.medEkstraAksjonspunktResultat(AksjonspunktDefinisjon.FATTER_VEDTAK, AksjonspunktStatus.OPPRETTET);
    }

    private void ekskluderOrginaltAksjonspunktFraTotrinnsVurdering(BekreftetAksjonspunktDto dto, Behandling behandling, OppdateringResultat.Builder builder) {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(dto.getKode());
        behandling.getÅpentAksjonspunktMedDefinisjonOptional(aksjonspunktDefinisjon)
            .ifPresent(ap -> builder.medAvbruttAksjonspunkt());
    }

    private void registrerNyttKontrollpunktIAksjonspunktRepo(Behandling behandling, OppdateringResultat.Builder builder) {
        AksjonspunktDefinisjon foreslaVedtak = AksjonspunktDefinisjon.FORESLÅ_VEDTAK;
        AksjonspunktStatus target = behandling.getAksjonspunktMedDefinisjonOptional(foreslaVedtak)
            .map(ap -> AksjonspunktStatus.AVBRUTT.equals(ap.getStatus()) ? AksjonspunktStatus.OPPRETTET : ap.getStatus()).orElse(AksjonspunktStatus.UTFØRT);
        builder.medEkstraAksjonspunktResultat(foreslaVedtak, target);
    }

    void opprettHistorikkinnslag(Behandling behandling) {
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

    void oppdaterRedusertUtbetalingÅrsaker(VedtaksbrevOverstyringDto dto, Long behandlingId) {
        vedtakVarselRepository.hentHvisEksisterer(behandlingId).ifPresent(
            v -> v.setRedusertUtbetalingÅrsaker(dto.getRedusertUtbetalingÅrsaker())
        );
    }
}
