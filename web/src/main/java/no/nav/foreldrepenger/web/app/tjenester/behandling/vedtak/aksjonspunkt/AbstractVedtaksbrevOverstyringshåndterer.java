package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import no.finn.unleash.Unleash;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.dokumentbestiller.klient.FormidlingRestKlient;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.kontrakter.formidling.v1.TekstFraSaksbehandlerDto;

public abstract class AbstractVedtaksbrevOverstyringshåndterer {
    public static final String FPSAK_LAGRE_FRITEKST_INN_FORMIDLING = "fpsak.lagre_fritekst_inn_fpformidling";
    protected HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    BehandlingsresultatRepository behandlingsresultatRepository;
    protected OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag;
    private VedtakTjeneste vedtakTjeneste;
    FormidlingRestKlient formidlingRestKlient;
    protected Unleash unleash;

    AbstractVedtaksbrevOverstyringshåndterer() {
        // for CDI proxy
    }

    AbstractVedtaksbrevOverstyringshåndterer(BehandlingRepositoryProvider repositoryProvider,
                                             HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                             OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                             VedtakTjeneste vedtakTjeneste) {
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.opprettToTrinnsgrunnlag = opprettToTrinnsgrunnlag;
        this.vedtakTjeneste = vedtakTjeneste;
        this.unleash = null;
    }

    AbstractVedtaksbrevOverstyringshåndterer(BehandlingRepositoryProvider repositoryProvider,
                                             HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                             OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                             VedtakTjeneste vedtakTjeneste,
                                             FormidlingRestKlient formidlingRestKlient,
                                             Unleash unleash) {
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.opprettToTrinnsgrunnlag = opprettToTrinnsgrunnlag;
        this.vedtakTjeneste = vedtakTjeneste;
        this.formidlingRestKlient = formidlingRestKlient;
        this.unleash = unleash;
    }

    void oppdaterVedtaksbrev(VedtaksbrevOverstyringDto dto, AksjonspunktOppdaterParameter param, OppdateringResultat.Builder builder) {
        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            Behandling behandling = param.getBehandling();
            settFritekstBrev(behandling, dto.getOverskrift(), dto.getFritekstBrev());
            opprettToTrinnsKontrollpunktForFritekstBrev(dto, behandling, builder);
            opprettAksjonspunktForFatterVedtak(behandling, builder);
            opprettToTrinnsgrunnlag.settNyttTotrinnsgrunnlag(behandling);
            opprettHistorikkinnslag(behandling);
        }
    }

    private void settFritekstBrev(Behandling behandling, String overskrift, String fritekst) {
        behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).ifPresent(behandlingsresultat -> {
            if (unleash != null && unleash.isEnabled(FPSAK_LAGRE_FRITEKST_INN_FORMIDLING)) {
                formidlingRestKlient.lagreTekstFraSaksbehandler(mapTekstFraSaksbehandlerDto(behandling, overskrift, fritekst));
            } else {
                Behandlingsresultat.builderEndreEksisterende(behandlingsresultat)
                    .medOverskrift(overskrift)
                    .medFritekstbrev(fritekst)
                    .medVedtaksbrev(Vedtaksbrev.FRITEKST)
                    .buildFor(behandling);
            }
        });
    }

    private TekstFraSaksbehandlerDto mapTekstFraSaksbehandlerDto(Behandling behandling, String overskrift, String fritekst) {
        TekstFraSaksbehandlerDto tekstFraSaksbehandlerDto = new TekstFraSaksbehandlerDto();
        tekstFraSaksbehandlerDto.setBehandlingUuid(behandling.getUuid());
        tekstFraSaksbehandlerDto.setVedtaksbrev(no.nav.foreldrepenger.kontrakter.formidling.kodeverk.Vedtaksbrev.FRITEKST);
        tekstFraSaksbehandlerDto.setTittel(overskrift);
        tekstFraSaksbehandlerDto.setFritekst(fritekst);
        return tekstFraSaksbehandlerDto;
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

    void opprettAksjonspunktForFatterVedtak(Behandling behandling, OppdateringResultat.Builder builder) {
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
}
