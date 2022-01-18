package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
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
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;
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

    void oppdaterVedtaksbrev(VedtaksbrevOverstyringDto dto, AksjonspunktOppdaterParameter param, OppdateringResultat.Builder builder) {
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(dto.getKode());
        Behandling behandling = param.getBehandling();

        if (dto.isSkalBrukeOverstyrendeFritekstBrev()) {
            settFritekstBrev(param.getBehandlingId(), dto.getOverskrift(), dto.getFritekstBrev());
        }
        if (AksjonspunktDefinisjon.FORESLÅ_VEDTAK.equals(aksjonspunktDefinisjon)) {
            opprettToTrinnsgrunnlag.settNyttTotrinnsgrunnlag(behandling);
            opprettAksjonspunktForFatterVedtak(builder);
        }
        opprettHistorikkinnslag(behandling);
    }

    void oppdaterVedtaksvarsel(VedtaksbrevOverstyringDto dto, Long behandlingId) {
        vedtakVarselRepository.hentHvisEksisterer(behandlingId).ifPresent(v -> {
            v.setRedusertUtbetalingÅrsaker(dto.getRedusertUtbetalingÅrsaker());
            if (dto.isSkalUndertrykkeBrev()) {
                v.setVedtaksbrev(Vedtaksbrev.INGEN);
            }
            vedtakVarselRepository.lagre(behandlingId, v);
        });
    }

    void oppdaterBegrunnelse(Behandling behandling, String begrunnelse) {
        vedtakVarselRepository.hentHvisEksisterer(behandling.getId()).ifPresent(behandlingsresultat -> {
            if (kreverFritekstbrev(behandling.getBehandlingResultatType())
                || begrunnelse != null
                || skalNullstilleFritekstfelt(behandling, behandlingsresultat)) {
                behandlingsresultat.setAvslagarsakFritekst(begrunnelse);
            }
        });
        behandling.setAnsvarligSaksbehandler(getCurrentUserId());
    }

    private void settFritekstBrev(Long behandlingId, String overskrift, String fritekst) {
        vedtakVarselRepository.hentHvisEksisterer(behandlingId).ifPresent(vedtakVarsel -> {
            vedtakVarsel.setOverskrift(overskrift);
            vedtakVarsel.setFritekstbrev(fritekst);
            vedtakVarsel.setVedtaksbrev(Vedtaksbrev.FRITEKST);
            vedtakVarselRepository.lagre(behandlingId, vedtakVarsel);
        });
    }

    private void opprettAksjonspunktForFatterVedtak(OppdateringResultat.Builder builder) {
        builder.medEkstraAksjonspunktResultat(AksjonspunktDefinisjon.FATTER_VEDTAK, AksjonspunktStatus.OPPRETTET);
    }

    private boolean skalNullstilleFritekstfelt(Behandling behandling, VedtakVarsel behandlingsresultat) {
        return !kreverFritekstbrev(behandling.getBehandlingResultatType())
            && behandlingsresultat.getAvslagarsakFritekst() != null;
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

    private boolean kreverFritekstbrev(BehandlingResultatType behandlingResultatType) {
        return Set.of(BehandlingResultatType.AVSLÅTT, BehandlingResultatType.OPPHØR, BehandlingResultatType.DELVIS_INNVILGET).contains(behandlingResultatType);
    }

}
