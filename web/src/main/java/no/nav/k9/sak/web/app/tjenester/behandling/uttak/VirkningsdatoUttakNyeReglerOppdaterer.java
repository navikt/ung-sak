package no.nav.k9.sak.web.app.tjenester.behandling.uttak;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.uttak.UttakNyeReglerRepository;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderVirkningsdatoUttakNyeReglerDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderVirkningsdatoUttakNyeReglerDto.class, adapter = AksjonspunktOppdaterer.class)
public class VirkningsdatoUttakNyeReglerOppdaterer implements AksjonspunktOppdaterer<VurderVirkningsdatoUttakNyeReglerDto> {

    private UttakNyeReglerRepository uttakNyeReglerRepository;
    private HistorikkRepository historikkRepository;
    private boolean nyRegelEnabled;

    VirkningsdatoUttakNyeReglerOppdaterer() {
        //for CDI proxy
    }

    @Inject
    public VirkningsdatoUttakNyeReglerOppdaterer(UttakNyeReglerRepository uttakNyeReglerRepository,
                                                 HistorikkRepository historikkRepository,
                                                 @KonfigVerdi(value = "ENABLE_DATO_NY_REGEL_UTTAK", required = false, defaultVerdi = "false") boolean nyRegelEnabled) {
        this.uttakNyeReglerRepository = uttakNyeReglerRepository;
        this.historikkRepository = historikkRepository;
        this.nyRegelEnabled = nyRegelEnabled;
    }

    @Override
    public OppdateringResultat oppdater(VurderVirkningsdatoUttakNyeReglerDto dto, AksjonspunktOppdaterParameter param) {
        if (!nyRegelEnabled){
            throw new IllegalStateException("Mulighet for å sette virkningsdato for nye regler for uttak er ikke lansert.");
        }

        LocalDate eksisterendeDato = uttakNyeReglerRepository.finnDatoForNyeRegler(param.getBehandlingId()).orElse(null);
        uttakNyeReglerRepository.lagreDatoForNyeRegler(param.getBehandlingId(), dto.getVirkningsdato());
        opprettHistorikkInnslag(param.getBehandlingId(), dto.getVirkningsdato(), eksisterendeDato, dto.getBegrunnelse());
        return OppdateringResultat.builder().medTotrinn().build();
    }

    private void opprettHistorikkInnslag(Long behandlingId, LocalDate nyVirkningsdato, LocalDate eksisterendeDato, String begrunnelse) {
        Historikkinnslag innslag = new Historikkinnslag();
        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        innslag.setBehandlingId(behandlingId);
        innslag.setType(HistorikkinnslagType.VIRKNINGSDATO_UTTAK_NYE_REGLER);

        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder().medSkjermlenke(SkjermlenkeType.UTTAK);
        tekstBuilder.medHendelse(innslag.getType());
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.VIRKNINGSDATO_UTTAK_NYE_REGLER, eksisterendeDato, nyVirkningsdato);
        tekstBuilder.medBegrunnelse(begrunnelse);
        tekstBuilder.build(innslag);
        historikkRepository.lagre(innslag);
    }
}
