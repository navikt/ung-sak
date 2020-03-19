package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingFeil;
import no.nav.foreldrepenger.behandling.revurdering.felles.FastsettResultatVedEndring.Betingelser;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.medlem.MedlemTjeneste;
import no.nav.vedtak.util.Tuple;

public abstract class RevurderingBehandlingsresultatutlederFelles {

    private BeregningTjeneste kalkulusTjeneste;
    private MedlemTjeneste medlemTjeneste;

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private VedtakVarselRepository vedtakVarselRepository;
    private HarEtablertYtelse harEtablertYtelse;

    private VilkårResultatRepository vilkårResultatRepository;

    protected RevurderingBehandlingsresultatutlederFelles() {
        // for CDI proxy
    }

    public RevurderingBehandlingsresultatutlederFelles(BehandlingRepositoryProvider repositoryProvider,
                                                           VedtakVarselRepository vedtakVarselRepository,
                                                           BeregningTjeneste kalkulusTjeneste,
                                                           MedlemTjeneste medlemTjeneste,
                                                           HarEtablertYtelse harEtablertYtelse) {

        this.kalkulusTjeneste = kalkulusTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.medlemTjeneste = medlemTjeneste;
        this.vedtakVarselRepository = vedtakVarselRepository;
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.harEtablertYtelse = harEtablertYtelse;
    }

    public VedtakVarsel bestemBehandlingsresultatForRevurdering(BehandlingReferanse revurderingRef, boolean erVarselOmRevurderingSendt) {
        Behandling revurdering = behandlingRepository.hentBehandling(revurderingRef.getBehandlingId());

        Behandling originalBehandling = revurdering.getOriginalBehandling()
            .orElseThrow(() -> RevurderingFeil.FACTORY.revurderingManglerOriginalBehandling(revurdering.getId()).toException());

        return bestemVedtakVarselRevurderingCore(revurderingRef, revurdering, originalBehandling, erVarselOmRevurderingSendt);
    }

    private VedtakVarsel bestemVedtakVarselRevurderingCore(BehandlingReferanse revurderingRef,
                                                                            Behandling revurdering,
                                                                            Behandling originalBehandling,
                                                                            boolean erVarselOmRevurderingSendt) {
        if (!revurdering.getType().equals(BehandlingType.REVURDERING)) {
            throw new IllegalStateException("Utviklerfeil: Skal ikke kunne havne her uten en revurderingssak");
        }
        validerReferanser(revurderingRef, revurdering.getId());
        final Long behandlingId = revurderingRef.getBehandlingId();

        var originalOrg = finnBehandlingsresultatPåOriginalBehandling(originalBehandling);
        VedtakVarsel vedtakVarsel = vedtakVarselRepository.hentHvisEksisterer(behandlingId).orElse(new VedtakVarsel());

        if (vurderAvslagPåAslag(Optional.of(revurdering), Optional.of(originalOrg), originalBehandling.getType())) {
            /* 2b */
            revurdering.setBehandlingResultatType(BehandlingResultatType.INGEN_ENDRING);
            vedtakVarsel.setVedtaksbrev(Vedtaksbrev.INGEN);
            return vedtakVarsel;

        }
        final var vilkårene = vilkårResultatRepository.hent(revurdering.getId());
        if (vurder(vilkårene.getVilkårene())) {
            /* 2c */
            boolean skalBeregnesIInfotrygd = harIngenBeregningsreglerILøsningen(vilkårene.getVilkårene());
            revurdering.setBehandlingResultatType(BehandlingResultatType.OPPHØR);
            vedtakVarsel.setVedtaksbrev(skalBeregnesIInfotrygd ? Vedtaksbrev.INGEN : Vedtaksbrev.AUTOMATISK);
            return vedtakVarsel;
        }

        Tuple<Utfall, Avslagsårsak> utfall = medlemTjeneste.utledVilkårUtfall(revurdering);
        if (!utfall.getElement1().equals(Utfall.OPPFYLT)) {
            revurdering.setBehandlingResultatType(BehandlingResultatType.OPPHØR);
            vedtakVarsel.setVedtaksbrev(Vedtaksbrev.AUTOMATISK);
            return vedtakVarsel;
        }

        boolean erEndringIBeregning = kalkulusTjeneste.erEndringIBeregning(revurdering.getId(), originalBehandling.getId());

        Betingelser betingelser = Betingelser.fastsett(erEndringIBeregning, erVarselOmRevurderingSendt,
            harInnvilgetIkkeOpphørtVedtak(revurdering.getFagsak()));

        return FastsettResultatVedEndring.fastsett(revurdering, vedtakVarsel, betingelser, harEtablertYtelse);
    }

    private Behandling finnBehandlingsresultatPåOriginalBehandling(Behandling behandling) {
        // Dersom originalBehandling er et beslutningsvedtak må vi lete videre etter det faktiske resultatet for å kunne vurdere avslag på avslag
        if (BehandlingResultatType.INGEN_ENDRING.equals(behandling.getBehandlingResultatType())) {
            return finnBehandlingsresultatPåOriginalBehandling(behandling.getOriginalBehandling()
                .orElseThrow(
                    () -> new IllegalStateException("Utviklerfeil: Kan ikke ha BehandlingResultatType.INGEN_ENDRING uten original behandling. BehandlingId="
                        + behandling.getId())));
        } else {
            return behandling;
        }
    }

    private boolean harInnvilgetIkkeOpphørtVedtak(Fagsak fagsak) {
        Behandling sisteInnvilgede = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElse(null);
        if (sisteInnvilgede == null) {
            return false;
        }
        var sistInnvilgedeVedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(sisteInnvilgede.getId());
        Map<Long, Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(fagsak.getSaksnummer())
            .stream().collect(Collectors.toMap(v -> v.getId(), v -> v));

        return behandlinger.values().stream()
            .filter(this::erAvsluttetRevurdering)
            .map(this::tilBehandlingvedtak)
            .filter(vedtak -> erFattetEtter(sistInnvilgedeVedtak, vedtak))
            .noneMatch(opphørvedtak(behandlinger));
    }

    private boolean erAvsluttetRevurdering(Behandling behandling) {
        return behandling.erRevurdering() && behandling.erSaksbehandlingAvsluttet();
    }

    private BehandlingVedtak tilBehandlingvedtak(Behandling b) {
        return behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(b.getId()).orElse(null);
    }

    private boolean erFattetEtter(Optional<BehandlingVedtak> sistInnvilgedeVedtak, BehandlingVedtak vedtak) {
        if (sistInnvilgedeVedtak.isEmpty()) {
            return vedtak != null;
        }
        return (vedtak != null && vedtak.getVedtaksdato().isAfter(sistInnvilgedeVedtak.get().getVedtaksdato()));
    }

    private Predicate<BehandlingVedtak> opphørvedtak(Map<Long, Behandling> behandlinger) {
        return vedtak -> {
            var behandling = behandlinger.get(vedtak.getBehandlingId());
            return BehandlingResultatType.OPPHØR.equals(behandling.getBehandlingResultatType());
        };
    }

    private void validerReferanser(BehandlingReferanse ref, Long behandlingId) {
        if (!Objects.equals(ref.getBehandlingId(), behandlingId)) {
            throw new IllegalStateException(
                "BehandlingReferanse [" + ref.getBehandlingId() + "] matcher ikke forventet [" + behandlingId + "]");
        }
    }

    private boolean vurder(List<Vilkår> vilkårene) {
        ChronoLocalDate chronoLocalDate = LocalDate.now();
        return vilkårene.stream()
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .filter(it -> it.getPeriode().inkluderer(chronoLocalDate))
            .anyMatch(v -> !Utfall.OPPFYLT.equals(v.getGjeldendeUtfall()));
    }

    private boolean harIngenBeregningsreglerILøsningen(List<Vilkår> vilkårene) {
        return vilkårene.stream()
            .filter(vilkår -> VilkårType.BEREGNINGSGRUNNLAGVILKÅR.equals(vilkår.getVilkårType()))
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .anyMatch(periode -> Avslagsårsak.INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN.equals(periode.getAvslagsårsak())
                && Utfall.IKKE_OPPFYLT.equals(periode.getGjeldendeUtfall()));
    }

    private static boolean vurderAvslagPåAslag(Optional<Behandling> resRevurdering, Optional<Behandling> resOriginal, BehandlingType originalBehandlingType) {
        if (resOriginal.isPresent() && resRevurdering.isPresent()) {
            if (BehandlingType.FØRSTEGANGSSØKNAD.equals(originalBehandlingType)) {
                return erAvslagPåAvslag(resRevurdering.get(), resOriginal.get());
            }
        }
        return false;
    }

    private static boolean erAvslagPåAvslag(Behandling resRevurdering, Behandling resOriginal) {
        return false;
    }
}
