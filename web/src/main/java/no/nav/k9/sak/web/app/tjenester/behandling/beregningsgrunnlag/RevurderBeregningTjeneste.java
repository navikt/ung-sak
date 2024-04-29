package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.PGIPeriode;

@ApplicationScoped
public class RevurderBeregningTjeneste {

    public static Set<BehandlingÅrsakType> MANUELLE_BEREGNING_ÅRSAKER = Set.of(
        BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG,
        BehandlingÅrsakType.RE_KLAGE_NY_INNH_LIGNET_INNTEKT
    );

    public static Set<BehandlingÅrsakType> MANUELLE_OPPTJENING_ÅRSAKER = Set.of(
        BehandlingÅrsakType.RE_OPPLYSNINGER_OM_OPPTJENING
    );


    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private FagsakTjeneste fagsakTjeneste;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private FastsettPGIPeriodeTjeneste fastsettPGIPeriodeTjeneste;
    private OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;

    public RevurderBeregningTjeneste() {
    }

    @Inject
    public RevurderBeregningTjeneste(BehandlingRepository behandlingRepository,
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
     * Revurderer periode
     *
     * @param saksnummer          Saksnummer
     * @param skjæringstidspunkt  Skjæringstidspunkt for aktuell periode
     * @param behandlingÅrsakType Behandlingsårsaktype
     * @param nesteKjøringEtter   Neste kjøring etter for å spre tasker
     * @return ProsessTaskGruppeId
     */
    public String revurderMedÅrsak(Saksnummer saksnummer, LocalDate skjæringstidspunkt, BehandlingÅrsakType behandlingÅrsakType, Optional<LocalDateTime> nesteKjøringEtter) {
        if (!MANUELLE_BEREGNING_ÅRSAKER.contains(behandlingÅrsakType) && !MANUELLE_OPPTJENING_ÅRSAKER.contains(behandlingÅrsakType)) {
            throw new IllegalArgumentException("Ugyldig behandlingsårsak for manuell revurdering av beregning eller opptjening: " + behandlingÅrsakType.getKode());
        }

        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false).orElseThrow(() -> new IllegalArgumentException("finnes ikke fagsak med saksnummer: " + saksnummer));
        var tilRevurdering = behandlingRepository.hentSisteBehandlingForFagsakId(fagsak.getId()).orElseThrow();
        var aktuellPeriode = finnVilkårsperiode(skjæringstidspunkt, tilRevurdering, behandlingÅrsakType.equals(BehandlingÅrsakType.RE_KLAGE_NY_INNH_LIGNET_INNTEKT));


        ProsessTaskData tilRevurderingTaskData = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK, behandlingÅrsakType.getKode());
        tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODE_FOM, aktuellPeriode.getFomDato().toString());
        tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODE_TOM, aktuellPeriode.getTomDato().toString());
        tilRevurderingTaskData.setBehandling(tilRevurdering.getFagsakId(), tilRevurdering.getId(), tilRevurdering.getAktørId().getId());
        nesteKjøringEtter.ifPresent(tilRevurderingTaskData::setNesteKjøringEtter);
        return fagsakProsessTaskRepository.lagreNyGruppe(tilRevurderingTaskData);
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

        var aktuellPeriode = finnVilkårsperiode(skjæringstidspunkt, tilRevurdering, true);

        ProsessTaskData tilRevurderingTaskData = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG.getKode());
        tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODE_FOM, aktuellPeriode.getFomDato().toString());
        tilRevurderingTaskData.setProperty(OpprettRevurderingEllerOpprettDiffTask.PERIODE_TOM, aktuellPeriode.getTomDato().toString());
        tilRevurderingTaskData.setBehandling(tilRevurdering.getFagsakId(), tilRevurdering.getId(), tilRevurdering.getAktørId().getId());
        fagsakProsessTaskRepository.lagreNyGruppe(tilRevurderingTaskData);
    }

    private PGIPeriode lagPGIPeriodeForForrigeSkatteoppgjør(LocalDate skjæringstidspunkt, Behandling behandlingMedRiktigSkatteoppgjør) {
        var iayGrunnlagMedRiktigSkatteoppgjør = iayTjeneste.hentGrunnlag(behandlingMedRiktigSkatteoppgjør.getId());
        return new PGIPeriode(iayGrunnlagMedRiktigSkatteoppgjør.getEksternReferanse(), skjæringstidspunkt);
    }

    private DatoIntervallEntitet finnVilkårsperiode(LocalDate skjæringstidspunkt, Behandling tilRevurdering, boolean skalValidereMot8_35) {
        var vilkårPeriode = vilkårResultatRepository.hentHvisEksisterer(tilRevurdering.getId())
            .flatMap(it -> it.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)).stream()
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> p.getPeriode().getFomDato().equals(skjæringstidspunkt))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Bruker har ingen søknadsperiode med fom-dato " + skjæringstidspunkt));

        if (skalValidereMot8_35) {
            var iayGrunnlag = iayTjeneste.hentGrunnlag(tilRevurdering.getId());
            var oppgittOpptjeningFilter = oppgittOpptjeningFilterProvider.finnOpptjeningFilter(tilRevurdering.getId());
            if (!fastsettPGIPeriodeTjeneste.omfattesAv8_35(tilRevurdering.getId(), iayGrunnlag, oppgittOpptjeningFilter, vilkårPeriode.getSkjæringstidspunkt())) {
                throw new IllegalArgumentException("Bruker har ingen søknadsperiode som omfattes av § 8-35 for denne perioden");
            }

        }

        return vilkårPeriode.getPeriode();
    }


}
