package no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
import no.nav.k9.sak.kontrakt.kompletthet.aksjonspunkt.EndeligAvklaringKompletthetForBeregningDto;
import no.nav.k9.sak.kontrakt.kompletthet.aksjonspunkt.KompletthetsPeriode;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;

@ApplicationScoped
@DtoTilServiceAdapter(dto = EndeligAvklaringKompletthetForBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class EndeligAvklaringKompletthetForBeregning implements AksjonspunktOppdaterer<EndeligAvklaringKompletthetForBeregningDto> {

    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private BeregningPerioderGrunnlagRepository grunnlagRepository;

    EndeligAvklaringKompletthetForBeregning() {
        // for CDI proxy
    }

    @Inject
    public EndeligAvklaringKompletthetForBeregning(KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste,
                                                   HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                                   BeregningPerioderGrunnlagRepository grunnlagRepository) {
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.grunnlagRepository = grunnlagRepository;
    }

    @Override
    public OppdateringResultat oppdater(EndeligAvklaringKompletthetForBeregningDto dto, AksjonspunktOppdaterParameter param) {
        var perioderMedManglendeGrunnlag = kompletthetForBeregningTjeneste.utledAlleManglendeVedleggFraGrunnlag(param.getRef());

        lagreVurderinger(param, perioderMedManglendeGrunnlag, dto);

        // Rekjører steget etter løsing
        return OppdateringResultat.utenTransisjon()
            .medTotrinn()
            .build();
    }

    private void lagreVurderinger(AksjonspunktOppdaterParameter param, Map<DatoIntervallEntitet, List<ManglendeVedlegg>> perioderMedManglendeGrunnlag,
                                  EndeligAvklaringKompletthetForBeregningDto dto) {
        var perioder = dto.getPerioder()
            .stream()
            .filter(at -> perioderMedManglendeGrunnlag.keySet()
                .stream()
                .anyMatch(it -> it.overlapper(at.getPeriode().getFom(), at.getPeriode().getTom())))
            .collect(Collectors.toList());

        lagHistorikkinnslag(param, perioder);

        var kompletthetVurderinger = perioder.stream()
            .map(it -> new KompletthetPeriode(utledVurderingstype(it), it.getPeriode().getFom(), it.getBegrunnelse()))
            .collect(Collectors.toList());

        grunnlagRepository.lagre(param.getBehandlingId(), kompletthetVurderinger);
    }

    private Vurdering utledVurderingstype(KompletthetsPeriode it) {
        return it.getKanFortsette() ? Vurdering.KAN_FORTSETTE : Vurdering.MANGLENDE_GRUNNLAG;
    }

    private void lagHistorikkinnslag(AksjonspunktOppdaterParameter param, List<KompletthetsPeriode> perioder) {
        var eksisterendeGrunnlag = grunnlagRepository.hentGrunnlag(param.getBehandlingId());

        for (KompletthetsPeriode periode : perioder) {
            var eksisterendeValg = utledEksisterendeValg(periode, eksisterendeGrunnlag);
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
            .filter(it -> !Objects.equals(Vurdering.UDEFINERT, it))
            .findFirst()
            .orElse(null);
    }

}
