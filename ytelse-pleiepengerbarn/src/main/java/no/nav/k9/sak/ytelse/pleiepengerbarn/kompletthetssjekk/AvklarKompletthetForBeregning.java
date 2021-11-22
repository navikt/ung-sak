package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.kompletthet.aksjonspunkt.AvklarKompletthetForBeregningDto;
import no.nav.k9.sak.kontrakt.kompletthet.aksjonspunkt.KompletthetsPeriode;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarKompletthetForBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarKompletthetForBeregning implements AksjonspunktOppdaterer<AvklarKompletthetForBeregningDto> {

    private static final Logger log = LoggerFactory.getLogger(AvklarKompletthetForBeregning.class);

    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private BeregningPerioderGrunnlagRepository grunnlagRepository;
    private Boolean benyttNyFlyt = false;

    AvklarKompletthetForBeregning() {
        // for CDI proxy
    }

    @Inject
    public AvklarKompletthetForBeregning(KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste,
                                         HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                         BeregningPerioderGrunnlagRepository grunnlagRepository,
                                         @KonfigVerdi(value = "KOMPLETTHET_NY_FLYT", defaultVerdi = "false") Boolean benyttNyFlyt) {
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.grunnlagRepository = grunnlagRepository;
        this.benyttNyFlyt = benyttNyFlyt;
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
        // TODO: Lagre ned de som er avklart OK for fortsettelse eller om det er varsel til AG
        if (benyttNyFlyt) {
            lagreVurderinger(param.getBehandlingId(), dto);
        }

        if (kanFortsette && !benyttNyFlyt) {
            lagHistorikkinnslag(param, dto);

            return OppdateringResultat.utenTransisjon()
                .medTotrinn()
                .build();
        } else {
            var resultat = OppdateringResultat.utenTransisjon()
                .medTotrinnHvis(benyttNyFlyt)
                .build();

            resultat.skalRekjøreSteg(); // Rekjører steget for å bli sittende fast, bør håndteres med mer fornuftig logikk senere
            resultat.setSteg(BehandlingStegType.VURDER_KOMPLETTHET_BEREGNING); // TODO: Ved fjerning av toggle, endre til å alltid hoppe tilbake
            return resultat;
        }
    }

    private void lagreVurderinger(Long behandlingId,
                                  AvklarKompletthetForBeregningDto dto) {

        var kompletthetVurderinger = dto.getPerioder()
            .stream()
            .map(it -> new KompletthetPeriode(utledVurderingstype(it), it.getPeriode().getFom(), getBegrunnelse(dto, it)))
            .collect(Collectors.toList());

        log.info("Lagrer {} vurderinger.", kompletthetVurderinger.size());

        grunnlagRepository.lagre(behandlingId, kompletthetVurderinger);
    }

    private String getBegrunnelse(AvklarKompletthetForBeregningDto dto, KompletthetsPeriode it) {
        if (Objects.isNull(it.getBegrunnelse())) {
            log.info("Begrunnelse på periode er null når det er forventet at denne IKKE er null.");
            return dto.getBegrunnelse();
        }

        return it.getBegrunnelse();
    }

    private Vurdering utledVurderingstype(KompletthetsPeriode it) {
        return it.getKanFortsette() ? Vurdering.KAN_FORTSETTE : Vurdering.UDEFINERT;
    }

    private void lagHistorikkinnslag(AksjonspunktOppdaterParameter param, AvklarKompletthetForBeregningDto dto) {
        historikkTjenesteAdapter.tekstBuilder()
            .medSkjermlenke(SkjermlenkeType.BEREGNING) // TODO: Sette noe fornuftig avhengig av hvor frontend plasserer dette
            .medBegrunnelse("Behov for inntektsmelding avklart: " + dto.getBegrunnelse());
        historikkTjenesteAdapter.opprettHistorikkInnslag(param.getBehandlingId(), HistorikkinnslagType.FAKTA_ENDRET);
    }

}
