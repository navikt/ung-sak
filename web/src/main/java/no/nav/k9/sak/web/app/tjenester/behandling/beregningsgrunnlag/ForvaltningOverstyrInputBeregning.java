package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.prosessering.task.TilbakeTilStartBeregningTask;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningInputPeriode;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrInputForBeregningDto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputAktivitetOverstyring;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregninginput.BeregningInputHistorikkTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregninginput.BeregningInputLagreTjeneste;

@ApplicationScoped
public class ForvaltningOverstyrInputBeregning {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private BeregningInputHistorikkTjeneste beregningInputHistorikkTjeneste;
    private BeregningInputLagreTjeneste beregningInputLagreTjeneste;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private BehandlingModellRepository behandlingModellRepository;
    private BeregningPerioderGrunnlagRepository grunnlagRepository;
    private Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    public ForvaltningOverstyrInputBeregning() {
    }

    @Inject
    public ForvaltningOverstyrInputBeregning(
        HistorikkTjenesteAdapter historikkTjenesteAdapter,
        BeregningInputHistorikkTjeneste beregningInputHistorikkTjeneste,
        BeregningInputLagreTjeneste beregningInputLagreTjeneste,
        FagsakProsessTaskRepository fagsakProsessTaskRepository,
        BehandlingModellRepository behandlingModellRepository,
        BeregningPerioderGrunnlagRepository grunnlagRepository,
        @Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning,
        InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {

        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.beregningInputHistorikkTjeneste = beregningInputHistorikkTjeneste;
        this.beregningInputLagreTjeneste = beregningInputLagreTjeneste;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.behandlingModellRepository = behandlingModellRepository;
        this.grunnlagRepository = grunnlagRepository;
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void overstyrInntektsmelding(
        Behandling behandling,
        DatoIntervallEntitet vilkårsperiode,
        OverstyrBeregningInputPeriode overstyrtPeriodeDto,
        String begrunnelse) {
        if (behandling.erStatusFerdigbehandlet()) {
            throw new IllegalArgumentException("Kan ikke utføre overstyring for behandling som er ferdigbehandlet");
        }
        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        var dto = new OverstyrInputForBeregningDto(begrunnelse, List.of(overstyrtPeriodeDto));

        // Validering
        var inntektsmeldingerForSak = inntektArbeidYtelseTjeneste.hentUnikeInntektsmeldingerForSak(behandlingReferanse.getSaksnummer());
        valider(behandling, overstyrtPeriodeDto, vilkårsperiode, inntektsmeldingerForSak);

        // Lagring
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        beregningInputLagreTjeneste.lagreInputOverstyringer(behandlingReferanse, dto, iayGrunnlag, inntektsmeldingerForSak, new TreeSet<>(Set.of(vilkårsperiode)));

        // Historikk
        beregningInputHistorikkTjeneste.lagHistorikk(behandlingReferanse.getId(), begrunnelse);
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandlingReferanse.getId(), HistorikkinnslagType.FAKTA_ENDRET);

        // Tilbakehopp
        var modell = behandlingModellRepository.getModell(behandlingReferanse.getBehandlingType(), behandlingReferanse.getFagsakYtelseType());
        if (modell.erStegAFørStegB(BehandlingStegType.PRECONDITION_BEREGNING, behandling.getAktivtBehandlingSteg())) {
            ProsessTaskData tilbakeTilBeregningTask = ProsessTaskData.forProsessTask(TilbakeTilStartBeregningTask.class);
            tilbakeTilBeregningTask.setBehandling(behandlingReferanse.getFagsakId(), behandlingReferanse.getId(), behandlingReferanse.getAktørId().getId());
            fagsakProsessTaskRepository.lagreNyGruppe(tilbakeTilBeregningTask);
        }
    }

    private void valider(Behandling behandling, OverstyrBeregningInputPeriode periode, DatoIntervallEntitet vilkårsperiode, Set<Inntektsmelding> inntektsmeldingerForSak) {
        var aktivtGrunnlag = grunnlagRepository.hentGrunnlag(behandling.getId());
        var ekisterendeOverstyrteAktiviteter = aktivtGrunnlag.stream().flatMap(gr -> gr.getInputOverstyringPerioder().stream())
            .filter(p -> p.getSkjæringstidspunkt().equals(periode.getSkjaeringstidspunkt()))
            .findFirst()
            .stream()
            .flatMap(p -> p.getAktivitetOverstyringer().stream())
            .toList();


        var eksisterendeOverstyrteArbeidsgivere = ekisterendeOverstyrteAktiviteter.stream().map(InputAktivitetOverstyring::getArbeidsgiver)
            .map(Arbeidsgiver::getIdentifikator).collect(Collectors.toSet());
        var overstyrteArbeidsgivere = periode.getAktivitetliste()
            .stream().map(a -> a.getArbeidsgiverAktørId() != null ? a.getArbeidsgiverAktørId().getId() : a.getArbeidsgiverOrgnr().getId())
            .collect(Collectors.toSet());

        var harFjernetOverstyringForArbeidsgiver = eksisterendeOverstyrteArbeidsgivere.stream().anyMatch(it -> !overstyrteArbeidsgivere.contains(it));

        if (ekisterendeOverstyrteAktiviteter.size() > periode.getAktivitetliste().size() || harFjernetOverstyringForArbeidsgiver) {
            throw new IllegalArgumentException("Det finnes allerede overstyrte arbeidsgivere i periode. Det støttes ikke å fjerne overstyringer.");
        }

        var inntektsmeldingerForPeriode = InntektsmeldingerRelevantForBeregning.finnTjeneste(inntektsmeldingerRelevantForBeregning, behandling.getFagsakYtelseType())
            .begrensSakInntektsmeldinger(BehandlingReferanse.fra(behandling), inntektsmeldingerForSak, vilkårsperiode);

        var arbeidsgivereMedInntektsmelding = inntektsmeldingerForPeriode.stream().map(Inntektsmelding::getArbeidsgiver).map(Arbeidsgiver::getArbeidsgiverOrgnr).collect(Collectors.toSet());

        var overstyrtArbeidsgiverUtenIM = overstyrteArbeidsgivere.stream().filter(o -> !arbeidsgivereMedInntektsmelding.contains(o)).findFirst();

        if (overstyrtArbeidsgiverUtenIM.isPresent()) {
            throw new IllegalArgumentException("Overstyrt arbeidsgiver hadde ikke inntektsmelding til bruk for periode " + vilkårsperiode);
        }


    }

}
