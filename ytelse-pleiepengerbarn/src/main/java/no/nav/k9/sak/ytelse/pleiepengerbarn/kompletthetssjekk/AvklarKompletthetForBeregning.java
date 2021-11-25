package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagTekstBuilderFormater;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.kontrakt.kompletthet.aksjonspunkt.AvklarKompletthetForBeregningDto;
import no.nav.k9.sak.kontrakt.kompletthet.aksjonspunkt.KompletthetsPeriode;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;
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

        boolean toTrinn = kanFortsette;
        // TODO: Lagre ned de som er avklart OK for fortsettelse eller om det er varsel til AG
        if (benyttNyFlyt) {
            toTrinn = lagreVurderinger(param, perioderMedManglendeGrunnlag, dto);
        }

        if (kanFortsette && !benyttNyFlyt) {
            lagHistorikkinnslag(param, dto);

            return OppdateringResultat.utenTransisjon()
                .medTotrinn()
                .build();
        } else {
            var resultat = OppdateringResultat.utenTransisjon()
                .medTotrinnHvis(benyttNyFlyt && toTrinn)
                .build();

            resultat.skalRekjøreSteg(); // Rekjører steget for å bli sittende fast, bør håndteres med mer fornuftig logikk senere
            resultat.setSteg(BehandlingStegType.VURDER_KOMPLETTHET_BEREGNING); // TODO: Ved fjerning av toggle, endre til å alltid hoppe tilbake
            return resultat;
        }
    }

    private boolean lagreVurderinger(AksjonspunktOppdaterParameter param,
                                     Map<DatoIntervallEntitet, List<ManglendeVedlegg>> perioderMedManglendeGrunnlag,
                                     AvklarKompletthetForBeregningDto dto) {

        var perioder = dto.getPerioder()
            .stream()
            .filter(at -> perioderMedManglendeGrunnlag.entrySet()
                .stream()
                .filter(it -> !it.getValue().isEmpty())
                .anyMatch(it -> it.getKey().overlapper(at.getPeriode().getFom(), at.getPeriode().getTom())))
            .collect(Collectors.toList());

        lagHistorikkinnslag(param, perioder);

        var kompletthetVurderinger = perioder.stream()
            .map(it -> new KompletthetPeriode(utledVurderingstype(it), it.getPeriode().getFom(), getBegrunnelse(dto, it)))
            .collect(Collectors.toList());

        log.info("Lagrer {} vurderinger.", kompletthetVurderinger.size());

        grunnlagRepository.lagre(param.getBehandlingId(), kompletthetVurderinger);

        return !kompletthetVurderinger.stream().allMatch(it -> Objects.equals(it.getVurdering(), Vurdering.UDEFINERT));
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

    private void lagHistorikkinnslag(AksjonspunktOppdaterParameter param, List<KompletthetsPeriode> perioder) {
        var eksisterendeGrunnlag = grunnlagRepository.hentGrunnlag(param.getBehandlingId());

        for (KompletthetsPeriode periode : perioder) {
            var eksisterendeValg = utledEksisterendeValg(periode, eksisterendeGrunnlag);

            if (eksisterendeValg == null && !periode.getKanFortsette()) {
                continue;
            }
            historikkTjenesteAdapter.tekstBuilder()
                .medSkjermlenke(SkjermlenkeType.FAKTA_OM_INNTEKTSMELDING) // TODO: Sette noe fornuftig avhengig av hvor frontend plasserer dette
                .medEndretFelt(HistorikkEndretFeltType.KOMPLETTHET, formaterDato(periode), eksisterendeValg, utledVurderingstype(periode))
                .medBegrunnelse(periode.getBegrunnelse());
            historikkTjenesteAdapter.opprettHistorikkInnslag(param.getBehandlingId(), HistorikkinnslagType.FAKTA_ENDRET);
        }
    }

    private String formaterDato(KompletthetsPeriode periode) {
        return HistorikkinnslagTekstBuilderFormater.formatDate(periode.getPeriode().getFom());
    }

    private Vurdering utledEksisterendeValg(KompletthetsPeriode periode, Optional<BeregningsgrunnlagPerioderGrunnlag> eksisterendeGrunnlag) {
        if (eksisterendeGrunnlag.isEmpty()) {
            return null;
        }
        var vurderinger = eksisterendeGrunnlag.get().getKompletthetPerioder();

        return vurderinger.stream()
            .filter(it -> Objects.equals(it.getSkjæringstidspunkt(), periode.getPeriode().getFom()))
            .map(KompletthetPeriode::getVurdering)
            .findFirst()
            .orElse(null);
    }

}
