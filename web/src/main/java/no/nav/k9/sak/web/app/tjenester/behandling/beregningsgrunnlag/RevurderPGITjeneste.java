package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.FastsettPGIPeriodeTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.PGIPeriode;

@ApplicationScoped
public class RevurderPGITjeneste {

    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private FagsakTjeneste fagsakTjeneste;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private FastsettPGIPeriodeTjeneste fastsettPGIPeriodeTjeneste;
    private OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;

    public RevurderPGITjeneste() {
    }

    @Inject
    public RevurderPGITjeneste(BehandlingRepository behandlingRepository,
                               InntektArbeidYtelseTjeneste iayTjeneste,
                               VilkårResultatRepository vilkårResultatRepository,
                               FagsakTjeneste fagsakTjeneste,
                               FagsakProsessTaskRepository fagsakProsessTaskRepository,
                               FastsettPGIPeriodeTjeneste fastsettPGIPeriodeTjeneste,
                               OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider,
                               BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.fagsakTjeneste = fagsakTjeneste;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.fastsettPGIPeriodeTjeneste = fastsettPGIPeriodeTjeneste;
        this.oppgittOpptjeningFilterProvider = oppgittOpptjeningFilterProvider;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
    }

    /**
     * Revurderer periode der bruker omfattes av § 8-35 og ferdilignede inntekter på dagens dato skal brukes.
     *
     * @param saksnummer         Saksnummer
     * @param skjæringstidspunkt Skjæringstidspunkt for aktuell periode
     */
    public void revurderOgInnhentPGI(Saksnummer saksnummer, LocalDate skjæringstidspunkt) {
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false).orElseThrow(() -> new IllegalArgumentException("finnes ikke fagsak med saksnummer: " + saksnummer));
        var tilRevurdering = behandlingRepository.hentSisteBehandlingForFagsakId(fagsak.getId()).orElseThrow();
        var aktuellPeriode = finnVilkårsperiode(skjæringstidspunkt, tilRevurdering);

        ProsessTaskData tilRevurderingTaskData = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_KLAGE_NY_INNH_LIGNET_INNTEKT.getKode());
        tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODE_FOM, aktuellPeriode.getFom().toString());
        tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODE_TOM, aktuellPeriode.getTom().toString());
        tilRevurderingTaskData.setBehandling(tilRevurdering.getFagsakId(), tilRevurdering.getId(), tilRevurdering.getAktørId().getId());
        fagsakProsessTaskRepository.lagreNyGruppe(tilRevurderingTaskData);
    }


    /**
     * Revurderer periode der bruker omfattes av § 8-35 og ferdilignede inntekter fra forrige skatteoppgjør
     *
     * @param saksnummer                       Saksnummer
     * @param behandlingIdForrigeSkatteoppgjør behandlingId
     * @param skjæringstidspunkt               Skjæringstidspunkt for aktuell periode
     */
    public void revurderOgBrukForrigeSkatteoppgjør(Saksnummer saksnummer,
                                                   String behandlingIdForrigeSkatteoppgjør,
                                                   LocalDate skjæringstidspunkt) {
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false).orElseThrow(() -> new IllegalArgumentException("finnes ikke fagsak med saksnummer: " + saksnummer));

        var tilRevurdering = behandlingRepository.hentSisteBehandlingForFagsakId(fagsak.getId()).orElseThrow();
        var behandlingMedRiktigSkatteoppgjør = behandlingRepository.hentBehandling(behandlingIdForrigeSkatteoppgjør);

        if (!behandlingMedRiktigSkatteoppgjør.getFagsak().getSaksnummer().equals(saksnummer.getSaksnummer())) {
            throw new IllegalArgumentException("Saksnummer for behandling med riktig skatteoppgjør matcher ikke oppgitt saksnummer");
        }

        var forrigeSkatteoppgjør = lagPGIPeriodeForForrigeSkatteoppgjør(skjæringstidspunkt, behandlingMedRiktigSkatteoppgjør);
        beregningPerioderGrunnlagRepository.lagreOgDeaktiverPGIPerioder(tilRevurdering.getId(), List.of(forrigeSkatteoppgjør), Collections.emptyList());

        var aktuellPeriode = finnVilkårsperiode(skjæringstidspunkt, tilRevurdering);

        ProsessTaskData tilRevurderingTaskData = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG.getKode());
        tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODE_FOM, aktuellPeriode.getFom().toString());
        tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODE_TOM, aktuellPeriode.getTom().toString());
        tilRevurderingTaskData.setBehandling(tilRevurdering.getFagsakId(), tilRevurdering.getId(), tilRevurdering.getAktørId().getId());
        fagsakProsessTaskRepository.lagreNyGruppe(tilRevurderingTaskData);
    }

    private PGIPeriode lagPGIPeriodeForForrigeSkatteoppgjør(LocalDate skjæringstidspunkt, Behandling behandlingMedRiktigSkatteoppgjør) {
        var iayGrunnlagMedRiktigSkatteoppgjør = iayTjeneste.hentGrunnlag(behandlingMedRiktigSkatteoppgjør.getId());
        return new PGIPeriode(iayGrunnlagMedRiktigSkatteoppgjør.getEksternReferanse(), skjæringstidspunkt);
    }

    private VilkårPeriode finnVilkårsperiode(LocalDate skjæringstidspunkt, Behandling tilRevurdering) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(tilRevurdering.getId());
        var oppgittOpptjeningFilter = oppgittOpptjeningFilterProvider.finnOpptjeningFilter(tilRevurdering.getId());
        return vilkårResultatRepository.hentHvisEksisterer(tilRevurdering.getId())
            .flatMap(it -> it.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)).stream()
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> p.getPeriode().getFomDato().equals(skjæringstidspunkt))
            .filter(p -> fastsettPGIPeriodeTjeneste.omfattesAv8_35(tilRevurdering.getId(), iayGrunnlag, oppgittOpptjeningFilter, p.getSkjæringstidspunkt()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Bruker har ingen søknadsperiode med fom-dato " + skjæringstidspunkt + " eller omfattes ikke av § 8-35 for denne perioden"));
    }


}
