package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.kompletthet.aksjonspunkt.AvklarKompletthetForBeregningDto;
import no.nav.k9.sak.kontrakt.kompletthet.aksjonspunkt.KompletthetsPeriode;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarKompletthetForBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarKompletthetForBeregning implements AksjonspunktOppdaterer<AvklarKompletthetForBeregningDto> {

    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    AvklarKompletthetForBeregning() {
        // for CDI proxy
    }

    @Inject
    public AvklarKompletthetForBeregning(KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste, HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
    }

    @Override
    public OppdateringResultat oppdater(AvklarKompletthetForBeregningDto dto, AksjonspunktOppdaterParameter param) {
        var perioderMedManglendeGrunnlag = kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraGrunnlag(param.getRef());

        var kanFortsette = perioderMedManglendeGrunnlag.entrySet()
            .stream()
            .filter(it -> !it.getValue().isEmpty())
            .allMatch(it -> dto.getPerioder()
                .stream()
                .filter(at -> it.getKey().overlapper(at.getPeriode().getFom(), at.getPeriode().getTom()))
                .map(KompletthetsPeriode::getKanFortsette)
                .findFirst()
                .orElse(false));
        // TODO: Lagre ned de som er avklart OK for fortsettelse

        if (kanFortsette) {
            lagHistorikkinnslag(param, dto);

            return OppdateringResultat.utenTransisjon()
                .medTotrinn()
                .build();
        } else {
            var resultat = OppdateringResultat.utenOverhopp();
            resultat.skalRekjøreSteg(); // Rekjører steget for å bli sittende fast, bør håndteres med mer fornuftig logikk senere
            resultat.setSteg(BehandlingStegType.VURDER_KOMPLETTHET_BEREGNING);
            return resultat;
        }
    }

    private void lagHistorikkinnslag(AksjonspunktOppdaterParameter param, AvklarKompletthetForBeregningDto dto) {
        historikkTjenesteAdapter.tekstBuilder()
            .medSkjermlenke(SkjermlenkeType.BEREGNING) // TODO: Sette noe fornuftig avhengig av hvor frontend plasserer dette
            .medBegrunnelse("Behov for inntektsmelding avklart: " + dto.getBegrunnelse());
        historikkTjenesteAdapter.opprettHistorikkInnslag(param.getBehandlingId(), HistorikkinnslagType.FAKTA_ENDRET);
    }

}
