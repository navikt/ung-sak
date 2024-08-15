package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import java.time.LocalDate;
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
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.prosessering.task.TilbakeTilStartBeregningTask;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningAktivitet;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrBeregningInputPeriode;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrInputForBeregningDto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
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
    private VirksomhetTjeneste virksomhetTjeneste;

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
        InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, VirksomhetTjeneste virksomhetTjeneste) {

        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.beregningInputHistorikkTjeneste = beregningInputHistorikkTjeneste;
        this.beregningInputLagreTjeneste = beregningInputLagreTjeneste;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.behandlingModellRepository = behandlingModellRepository;
        this.grunnlagRepository = grunnlagRepository;
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    public void overstyrOpphørRefusjon(
        Behandling behandling,
        DatoIntervallEntitet vilkårsperiode,
        Arbeidsgiver arbeidsgiver,
        LocalDate opphørsdato,
        String begrunnelse) {
        var behandlingReferanse = BehandlingReferanse.fra(behandling);

        // Validering
        var inntektsmeldingerForSak = inntektArbeidYtelseTjeneste.hentUnikeInntektsmeldingerForSak(behandlingReferanse.getSaksnummer());
        valider(behandling,
            arbeidsgiver,
            vilkårsperiode,
            inntektsmeldingerForSak
        );

        // Lagring
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        oppdaterRefusjonOpphør(vilkårsperiode,
            arbeidsgiver,
            opphørsdato,
            begrunnelse,
            behandlingReferanse,
            iayGrunnlag,
            inntektsmeldingerForSak);

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

    private void oppdaterRefusjonOpphør(DatoIntervallEntitet vilkårsperiode,
                                        Arbeidsgiver arbeidsgiver,
                                        LocalDate opphørsdato,
                                        String begrunnelse,
                                        BehandlingReferanse behandlingReferanse,
                                        InntektArbeidYtelseGrunnlag iayGrunnlag,
                                        Set<Inntektsmelding> inntektsmeldingerForSak) {
        var periode = new OverstyrBeregningInputPeriode(vilkårsperiode.getFomDato(),
            List.of(new OverstyrBeregningAktivitet(new OrgNummer(arbeidsgiver.getArbeidsgiverOrgnr()), null, null, null, null, opphørsdato, true)));
        var dto = new OverstyrInputForBeregningDto(begrunnelse, List.of(periode));
        beregningInputLagreTjeneste.lagreInputOverstyringer(behandlingReferanse, dto, iayGrunnlag, inntektsmeldingerForSak, new TreeSet<>(Set.of(vilkårsperiode)));
    }

    private void valider(Behandling behandling,
                         Arbeidsgiver arbeidsgiver,
                         DatoIntervallEntitet vilkårsperiode,
                         Set<Inntektsmelding> inntektsmeldingerForSak) {
        if (behandling.erStatusFerdigbehandlet()) {
            throw new IllegalArgumentException("Kan ikke utføre overstyring for behandling som er ferdigbehandlet");
        }

        var aktivtGrunnlag = grunnlagRepository.hentGrunnlag(behandling.getId());
        var ekisterendeOverstyrteAktiviteter = aktivtGrunnlag.stream().flatMap(gr -> gr.getInputOverstyringPerioder().stream())
            .filter(p -> p.getSkjæringstidspunkt().equals(vilkårsperiode.getFomDato()))
            .findFirst()
            .stream()
            .flatMap(p -> p.getAktivitetOverstyringer().stream())
            .toList();

        var harEksisterendeOverstyringAvAnnenInfo = ekisterendeOverstyrteAktiviteter.stream()
            .anyMatch(o -> !o.getArbeidsgiver().equals(arbeidsgiver) ||
                o.getInntektPrÅr() != null ||
                o.getRefusjonPrÅr() != null);


        if (harEksisterendeOverstyringAvAnnenInfo) {
            throw new IllegalArgumentException("Det finnes allerede overstyrte arbeidsgivere i periode. Det støttes ikke å fjerne overstyringer.");
        }

        var inntektsmeldingerForPeriode = InntektsmeldingerRelevantForBeregning.finnTjeneste(inntektsmeldingerRelevantForBeregning, behandling.getFagsakYtelseType())
            .begrensSakInntektsmeldinger(BehandlingReferanse.fra(behandling), inntektsmeldingerForSak, vilkårsperiode);

        var arbeidsgivereMedInntektsmelding = inntektsmeldingerForPeriode.stream().map(Inntektsmelding::getArbeidsgiver).collect(Collectors.toSet());

        var harOverstyrtArbeidsgiverInntektsmelding = arbeidsgivereMedInntektsmelding.contains(arbeidsgiver);

        if (!harOverstyrtArbeidsgiverInntektsmelding) {
            throw new IllegalArgumentException("Overstyrt arbeidsgiver hadde ikke inntektsmelding til bruk for periode " + vilkårsperiode);
        }


        var virksomhet = virksomhetTjeneste.hentOrganisasjon(arbeidsgiver.getArbeidsgiverOrgnr());
        var idag = LocalDate.now();
        if (virksomhet.getAvslutt() == null || virksomhet.getAvslutt().isAfter(idag)) {
            throw new IllegalArgumentException("Kan ikke opphøre refusjon for en virksomhet som ikke er avsluttet.");
        }

    }

}
