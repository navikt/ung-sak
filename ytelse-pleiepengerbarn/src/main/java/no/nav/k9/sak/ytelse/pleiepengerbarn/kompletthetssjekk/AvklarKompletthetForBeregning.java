package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
        lagHistorikkinnslag(param, dto);

        var perioderMedManglendeGrunnlag = kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraGrunnlag(param.getRef());

        var kanFortsette = perioderMedManglendeGrunnlag.entrySet()
            .stream()
            .filter(it -> !it.getValue().isEmpty())
            .allMatch(it -> dto.getPerioder()
                .stream()
                .filter(at -> at.getPeriode().overlaps(it.getKey().tilPeriode()))
                .map(KompletthetsPeriode::getKanFortsette)
                .findFirst()
                .orElse(false));


        if (kanFortsette) {
            return OppdateringResultat.utenOveropp();
        } else {
            var resultat = OppdateringResultat.utenOveropp();
            resultat.skalRekjøreSteg(); // Rekjører steget for å bli sittende fast, bør håndteres med mer fornuftig logikk senere

            return resultat;
        }
    }

    private void lagHistorikkinnslag(AksjonspunktOppdaterParameter param, AvklarKompletthetForBeregningDto dto) {
        historikkTjenesteAdapter.tekstBuilder()
            .medSkjermlenke(SkjermlenkeType.BEREGNING) // TODO: Sette noe fornuftig avhengig av hvor frontend plasserer dette
            .medBegrunnelse("Kompletthet avklart: " + dto.getBegrunnelse());
        historikkTjenesteAdapter.opprettHistorikkInnslag(param.getBehandlingId(), HistorikkinnslagType.FAKTA_ENDRET);
    }

}
