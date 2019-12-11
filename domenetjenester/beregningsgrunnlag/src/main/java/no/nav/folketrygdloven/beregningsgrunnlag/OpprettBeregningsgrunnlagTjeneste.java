package no.nav.folketrygdloven.beregningsgrunnlag;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;

@ApplicationScoped
class OpprettBeregningsgrunnlagTjeneste {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private FastsettSkjæringstidspunktOgStatuser fastsettSkjæringstidspunktOgStatuser;
    private FastsettInntektskategoriFraSøknadTjeneste fastsettInntektskategoriFraSøknadTjeneste;
    private FastsettBeregningsgrunnlagPerioderTjeneste fastsettBeregningsgrunnlagPerioderTjeneste;

    protected OpprettBeregningsgrunnlagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OpprettBeregningsgrunnlagTjeneste(FastsettSkjæringstidspunktOgStatuser fastsettSkjæringstidspunktOgStatuser,
                                             FastsettInntektskategoriFraSøknadTjeneste fastsettInntektskategoriFraSøknadTjeneste,
                                             FastsettBeregningsgrunnlagPerioderTjeneste fastsettBeregningsgrunnlagPerioderTjeneste,
                                             BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.fastsettSkjæringstidspunktOgStatuser = fastsettSkjæringstidspunktOgStatuser;
        this.fastsettInntektskategoriFraSøknadTjeneste = fastsettInntektskategoriFraSøknadTjeneste;
        this.fastsettBeregningsgrunnlagPerioderTjeneste = fastsettBeregningsgrunnlagPerioderTjeneste;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    private static boolean harKunYtelse(List<BeregningsgrunnlagAktivitetStatus> aktivitetStatusList) {
        return aktivitetStatusList
            .stream()
            .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus)
            .collect(Collectors.toList())
            .equals(List.of(AktivitetStatus.KUN_YTELSE));
    }

    /**
     * Henter inn grunnlagsdata om nødvendig
     * Oppretter og bygger beregningsgrunnlag for behandlingen
     * Oppretter perioder og andeler på beregningsgrunnlag
     * Setter inntektskategori på andeler
     * Splitter perioder basert på refusjon, gradering og naturalytelse.
     *
     * @param input en {@link BeregningsgrunnlagInput}
     */
    BeregningsgrunnlagEntitet opprettOgLagreBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        var ref = input.getBehandlingReferanse();
        var grunnlag = input.getBeregningsgrunnlagGrunnlag();
        BeregningAktivitetAggregatEntitet beregningAktiviteter = grunnlag.getGjeldendeAktiviteter();

        BeregningsgrunnlagEntitet bgMedAndeler = fastsettSkjæringstidspunktOgStatuser.fastsett(ref, beregningAktiviteter, input.getIayGrunnlag());
        BehandlingReferanse refMedSkjæringstidspunkt = ref
            .medSkjæringstidspunkt(oppdaterSkjæringstidspunktForBeregning(ref, beregningAktiviteter, bgMedAndeler));
        fastsettInntektskategoriFraSøknadTjeneste.fastsettInntektskategori(bgMedAndeler, input.getIayGrunnlag());
        BeregningsgrunnlagInput newInput = input.medBehandlingReferanse(refMedSkjæringstidspunkt);
        BeregningsgrunnlagEntitet bgMedPerioder = fastsettBeregningsgrunnlagPerioderTjeneste.fastsettPerioderForNaturalytelse(newInput, bgMedAndeler);
        kopierFraGjeldendeBeregningsgrunnlag(newInput, bgMedPerioder);
        kopierFraGjeldendeBeregningsgrunnlag(input, bgMedPerioder);
        BeregningsgrunnlagVerifiserer.verifiserOppdatertBeregningsgrunnlag(bgMedPerioder);
        return bgMedPerioder;
    }

    private Skjæringstidspunkt oppdaterSkjæringstidspunktForBeregning(BehandlingReferanse ref,
                                                                      BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                                                      BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(ref.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt())
            .medSkjæringstidspunktOpptjening(beregningAktivitetAggregat.getSkjæringstidspunktOpptjening())
            .medSkjæringstidspunktBeregning(beregningsgrunnlag.getSkjæringstidspunkt()).build();
    }

    BeregningsgrunnlagEntitet fastsettSkjæringstidspunktOgStatuser(BehandlingReferanse ref, BeregningAktivitetAggregatEntitet beregningAktiviteter, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return fastsettSkjæringstidspunktOgStatuser.fastsett(ref, beregningAktiviteter, iayGrunnlag);
    }

    /**
     * Kopier informasjon fra gjeldende beregningsgrunnlag i tilfeller hvor det ikke er gjort endringer som påvirker beregningsgrunnlaget
     *
     * @param nyttBeregningsgrunnlag
     */
    private void kopierFraGjeldendeBeregningsgrunnlag(BeregningsgrunnlagInput input, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag) {
        if (harKunYtelse(nyttBeregningsgrunnlag.getAktivitetStatuser())) {
            beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
                input.getBehandlingReferanse().getBehandlingId(), input.getBehandlingReferanse().getOriginalBehandlingId(), BeregningsgrunnlagTilstand.FASTSATT)
                .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
                .filter(bg -> harKunYtelse(bg.getAktivitetStatuser()))
                .ifPresent(gjeldendeBG -> {
                    BeregningsgrunnlagEntitet kopi = KopierBeregningsgrunnlag.kopierVerdier(gjeldendeBG, nyttBeregningsgrunnlag);
                    BeregningsgrunnlagEntitet.builder(kopi).build();
                });
        }
    }
}
