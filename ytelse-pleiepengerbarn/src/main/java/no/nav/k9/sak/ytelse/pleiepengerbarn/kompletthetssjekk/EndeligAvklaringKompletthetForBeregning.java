package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.kontrakt.kompletthet.aksjonspunkt.EndeligAvklaringKompletthetForBeregningDto;
import no.nav.k9.sak.kontrakt.kompletthet.aksjonspunkt.KompletthetsPeriode;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;

@ApplicationScoped
@DtoTilServiceAdapter(dto = EndeligAvklaringKompletthetForBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class EndeligAvklaringKompletthetForBeregning implements AksjonspunktOppdaterer<EndeligAvklaringKompletthetForBeregningDto> {

    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private BeregningPerioderGrunnlagRepository grunnlagRepository;
    private Boolean benyttNyFlyt = false;

    EndeligAvklaringKompletthetForBeregning() {
        // for CDI proxy
    }

    @Inject
    public EndeligAvklaringKompletthetForBeregning(KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste,
                                                   HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                                   BeregningPerioderGrunnlagRepository grunnlagRepository,
                                                   @KonfigVerdi(value = "KOMPLETTHET_NY_FLYT", defaultVerdi = "false") Boolean benyttNyFlyt) {
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.grunnlagRepository = grunnlagRepository;
        this.benyttNyFlyt = benyttNyFlyt;
    }

    @Override
    public OppdateringResultat oppdater(EndeligAvklaringKompletthetForBeregningDto dto, AksjonspunktOppdaterParameter param) {
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
        lagreVurderinger(param.getBehandlingId(), perioderMedManglendeGrunnlag, dto);

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

    private void lagreVurderinger(Long behandlingId, Map<DatoIntervallEntitet, List<ManglendeVedlegg>> perioderMedManglendeGrunnlag,
                                  EndeligAvklaringKompletthetForBeregningDto dto) {
        var perioder = dto.getPerioder()
            .stream()
            .filter(at -> perioderMedManglendeGrunnlag.keySet()
                .stream()
                .anyMatch(it -> it.overlapper(at.getPeriode().getFom(), at.getPeriode().getTom())))
            .collect(Collectors.toList());

        var kompletthetVurderinger = perioder.stream()
            .map(it -> new KompletthetPeriode(utledVurderingstype(it), it.getPeriode().getFom(), it.getBegrunnelse()))
            .collect(Collectors.toList());

        grunnlagRepository.lagre(behandlingId, kompletthetVurderinger);
    }

    private Vurdering utledVurderingstype(KompletthetsPeriode it) {
        return it.getKanFortsette() ? Vurdering.KAN_FORTSETTE : Vurdering.MANGLENDE_GRUNNLAG;
    }

    private void lagHistorikkinnslag(AksjonspunktOppdaterParameter param, EndeligAvklaringKompletthetForBeregningDto dto) {
        historikkTjenesteAdapter.tekstBuilder()
            .medSkjermlenke(SkjermlenkeType.BEREGNING) // TODO: Sette noe fornuftig avhengig av hvor frontend plasserer dette
            .medBegrunnelse("Endelig behov for inntektsmelding avklart: " + dto.getBegrunnelse());
        historikkTjenesteAdapter.opprettHistorikkInnslag(param.getBehandlingId(), HistorikkinnslagType.FAKTA_ENDRET);
    }

}
