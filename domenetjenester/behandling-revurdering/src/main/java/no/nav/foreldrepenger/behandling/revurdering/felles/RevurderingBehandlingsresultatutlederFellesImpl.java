package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingFeil;
import no.nav.foreldrepenger.behandling.revurdering.felles.FastsettBehandlingsresultatVedEndring.Betingelser;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Utfall;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.util.Tuple;

public abstract class RevurderingBehandlingsresultatutlederFellesImpl implements RevurderingBehandlingsresultatutlederFelles {

    private HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private MedlemTjeneste medlemTjeneste;

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private HarEtablertYtelse harEtablertYtelse;

    // FIXME K9 håndter revurdering
    @SuppressWarnings("unused")
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    protected RevurderingBehandlingsresultatutlederFellesImpl() {
        // for CDI proxy
    }

    public RevurderingBehandlingsresultatutlederFellesImpl(BehandlingRepositoryProvider repositoryProvider,
                                                           HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                           MedlemTjeneste medlemTjeneste,
                                                           HarEtablertYtelse harEtablertYtelse,
                                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {

        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.medlemTjeneste = medlemTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.harEtablertYtelse = harEtablertYtelse;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public Behandlingsresultat bestemBehandlingsresultatForRevurdering(BehandlingReferanse revurderingRef, boolean erVarselOmRevurderingSendt) {
        Behandling revurdering = behandlingRepository.hentBehandling(revurderingRef.getBehandlingId());

        Behandling originalBehandling = revurdering.getOriginalBehandling()
            .orElseThrow(() -> RevurderingFeil.FACTORY.revurderingManglerOriginalBehandling(revurdering.getId()).toException());

        return bestemBehandlingsresultatForRevurderingCore(revurderingRef, revurdering, originalBehandling, erVarselOmRevurderingSendt);
    }

    private Behandlingsresultat bestemBehandlingsresultatForRevurderingCore(BehandlingReferanse revurderingRef,
                                                                            Behandling revurdering,
                                                                            Behandling originalBehandling,
                                                                            boolean erVarselOmRevurderingSendt) {
        if (!revurdering.getType().equals(BehandlingType.REVURDERING)) {
            throw new IllegalStateException("Utviklerfeil: Skal ikke kunne havne her uten en revurderingssak");
        }
        validerReferanser(revurderingRef, revurdering.getId());
        Long behandlingId = revurderingRef.getBehandlingId();

        Optional<Behandlingsresultat> behandlingsresultatRevurdering = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        Optional<Behandlingsresultat> behandlingsresultatOriginal = finnBehandlingsresultatPåOriginalBehandling(originalBehandling);
        if (FastsettBehandlingsresultatVedAvslagPåAvslag.vurder(behandlingsresultatRevurdering, behandlingsresultatOriginal)) {
            /* 2b */
            return FastsettBehandlingsresultatVedAvslagPåAvslag.fastsett(revurdering);
        }
        final var vilkårene = vilkårResultatRepository.hent(revurdering.getId());
        if (OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt.vurder(vilkårene.getVilkårene())) {
            /* 2c */
            return OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt.fastsett(revurdering, vilkårene.getVilkårene());
        }

        Tuple<Utfall, Avslagsårsak> utfall = medlemTjeneste.utledVilkårUtfall(revurdering);
        if (!utfall.getElement1().equals(Utfall.OPPFYLT)) {
            Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
            Behandlingsresultat.Builder behandlingsresultatBuilder = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat);
            behandlingsresultatBuilder.medBehandlingResultatType(BehandlingResultatType.OPPHØR);
            behandlingsresultatBuilder.leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.YTELSE_OPPHØRER);
            behandlingsresultatBuilder.medVedtaksbrev(Vedtaksbrev.AUTOMATISK);
            behandlingsresultat.setAvslagsårsak(utfall.getElement2());
            return behandlingsresultatBuilder.buildFor(revurdering);
        }

        Optional<BeregningsgrunnlagEntitet> revurderingsGrunnlagOpt = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagForBehandling(revurdering.getId());
        Optional<BeregningsgrunnlagEntitet> originalGrunnlagOpt = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagForBehandling(originalBehandling.getId());

        boolean erEndringIBeregning = ErEndringIBeregning.vurder(revurderingsGrunnlagOpt, originalGrunnlagOpt);

        Betingelser betingelser = Betingelser.fastsett(erEndringIBeregning, erVarselOmRevurderingSendt,
            harInnvilgetIkkeOpphørtVedtak(revurdering.getFagsak()));

        return FastsettBehandlingsresultatVedEndring.fastsett(revurdering, betingelser, harEtablertYtelse);
    }

    private Optional<Behandlingsresultat> finnBehandlingsresultatPåOriginalBehandling(Behandling originalBehandling) {
        Optional<Behandlingsresultat> behandlingsresultatOriginal = behandlingsresultatRepository.hentHvisEksisterer(originalBehandling.getId());
        if (behandlingsresultatOriginal.isPresent()) {
            // Dersom originalBehandling er et beslutningsvedtak må vi lete videre etter det faktiske resultatet for å kunne vurdere avslag på avslag
            if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingsresultatOriginal.get().getBehandlingResultatType())) {
                return finnBehandlingsresultatPåOriginalBehandling(originalBehandling.getOriginalBehandling()
                    .orElseThrow(
                        () -> new IllegalStateException("Utviklerfeil: Kan ikke ha BehandlingResultatType.INGEN_ENDRING uten original behandling. BehandlingId="
                            + originalBehandling.getId())));
            } else {
                return behandlingsresultatOriginal;
            }
        }
        return Optional.empty();
    }

    private boolean harInnvilgetIkkeOpphørtVedtak(Fagsak fagsak) {
        Behandling sisteInnvilgede = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElse(null);
        if (sisteInnvilgede == null) {
            return false;
        }
        List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(fagsak.getSaksnummer());
        return behandlinger.stream()
            .filter(this::erAvsluttetRevurdering)
            .map(this::tilBehandlingvedtak)
            .filter(vedtak -> erFattetEtter(sisteInnvilgede, vedtak))
            .noneMatch(opphørvedtak());
    }

    private boolean erAvsluttetRevurdering(Behandling behandling) {
        return behandling.erRevurdering() && behandling.erSaksbehandlingAvsluttet();
    }

    private BehandlingVedtak tilBehandlingvedtak(Behandling b) {
        return behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(b.getId()).orElse(null);
    }

    private boolean erFattetEtter(Behandling sisteInnvilget, BehandlingVedtak vedtak) {
        return vedtak != null && vedtak.getVedtaksdato().isAfter(sisteInnvilget.getOriginalVedtaksDato());
    }

    private Predicate<BehandlingVedtak> opphørvedtak() {
        return vedtak -> BehandlingResultatType.OPPHØR.equals(vedtak.getBehandlingsresultat().getBehandlingResultatType());
    }

    private void validerReferanser(BehandlingReferanse ref, Long behandlingId) {
        if (!Objects.equals(ref.getBehandlingId(), behandlingId)) {
            throw new IllegalStateException(
                "BehandlingReferanse [" + ref.getBehandlingId() + "] matcher ikke forventet [" + behandlingId + "]");
        }
    }

}
