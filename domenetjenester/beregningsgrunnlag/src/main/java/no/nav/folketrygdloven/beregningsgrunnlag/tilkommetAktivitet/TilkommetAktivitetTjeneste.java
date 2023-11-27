package no.nav.folketrygdloven.beregningsgrunnlag.tilkommetAktivitet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.kalkulus.response.v1.tilkommetAktivitet.UtledetTilkommetAktivitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;

@ApplicationScoped
public class TilkommetAktivitetTjeneste {

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;

    private KalkulusTjeneste kalkulusTjeneste;

    TilkommetAktivitetTjeneste() {
    }

    @Inject
    public TilkommetAktivitetTjeneste(BehandlingRepository behandlingRepository,
                                      VilkårResultatRepository vilkårResultatRepository,
                                      BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                      KalkulusTjeneste kalkulusTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.kalkulusTjeneste = kalkulusTjeneste;
    }


    /**
     * Henter ut tilkommede aktiviteter for angitt fagsak.
     *
     * @param fagsakId           IDen til fagsaken.
     * @param virkningstidspunkt Fra-og-med-datoen man skal få tilkommede aktiviteter for.
     * @return En {@code Map} med alle tilkommede aktiviteter med tilhørende perioden den
     * den regnes å være tilkommet i.
     */
    public Map<AktivitetstatusOgArbeidsgiver, LocalDateTimeline<Boolean>> finnTilkommedeAktiviteter(Long fagsakId, LocalDate virkningstidspunkt) {
        LocalDateInterval aktuellPeriode = virkningstidspunkt != null ? new LocalDateInterval(virkningstidspunkt, LocalDateInterval.TIDENES_ENDE) : null;
        return finnTilkommedeAktiviteter(fagsakId, aktuellPeriode);
    }

    /**
     * Henter ut inntektsgradering for angitt fagsak.
     *
     * @param fagsakId IDen til fagsaken.
     * @return En {@code Map} med alle tilkommede aktiviteter med tilhørende perioden den
     * den regnes å være tilkommet i.
     */
    public LocalDateTimeline<BigDecimal> finnInntektsgradering(Long fagsakId) {
        var relevantBehandling = finnRelevantBehandling(fagsakId);
        if (relevantBehandling.isEmpty()) {
            return LocalDateTimeline.empty();
        }
        var koblingerÅSpørreMot = finnRelevanteReferanser(relevantBehandling.get());
        return kalkulusTjeneste.finnInntektsgradering(koblingerÅSpørreMot, BehandlingReferanse.fra(relevantBehandling.get()));
    }


    /**
     * Henter ut tilkommede aktiviteter for angitt fagsak.
     *
     * @param fagsakId       IDen til fagsaken.
     * @param aktuellPeriode perioden det sjekkes tilkommede aktiviteter for.
     * @return En {@code Map} med alle tilkommede aktiviteter med tilhørende perioden den
     * den regnes å være tilkommet i.
     */
    public Map<AktivitetstatusOgArbeidsgiver, LocalDateTimeline<Boolean>> finnTilkommedeAktiviteter(Long fagsakId, LocalDateInterval aktuellPeriode) {
        var relevantBehandling = finnRelevantBehandling(fagsakId);

        if (relevantBehandling.isEmpty()) {
            return Collections.emptyMap();
        }

        var koblingerÅSpørreMot = finnRelevanteReferanser(relevantBehandling.get());

        final Map<UUID, List<UtledetTilkommetAktivitet>> koblingMotAktiviteter = kalkulusTjeneste.utledTilkommetAktivitet(koblingerÅSpørreMot, BehandlingReferanse.fra(relevantBehandling.get()));

        final Map<AktivitetstatusOgArbeidsgiver, LocalDateTimeline<Boolean>> sammenslåttResultat = new HashMap<>();
        koblingMotAktiviteter.values().stream().flatMap(Collection::stream).forEach(s -> {
            final AktivitetstatusOgArbeidsgiver aktivitetstatusOgArbeidsgiver = mapTilAktivitetstatusOgArbeidsgiver(s);
            LocalDateTimeline<Boolean> periodetidslinje = sammenslåttResultat.getOrDefault(aktivitetstatusOgArbeidsgiver, LocalDateTimeline.empty());
            LocalDateTimeline<Boolean> nyePerioder = new LocalDateTimeline<>(s.getPerioder()
                .stream()
                .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), true))
                .collect(Collectors.toList())
            );
            LocalDateTimeline<Boolean> sammenslått = periodetidslinje.crossJoin(nyePerioder);
            if (aktuellPeriode != null) {
                sammenslått = sammenslått.intersection(aktuellPeriode);
            }
            if (!sammenslått.isEmpty()) {
                sammenslåttResultat.put(aktivitetstatusOgArbeidsgiver, sammenslått);
            }
        });

        return sammenslåttResultat;
    }


    private Optional<Behandling> finnRelevantBehandling(Long fagsakId) {
        var sisteBehandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);

        if (sisteBehandlingOpt.isEmpty()) {
            return Optional.empty();
        }

        var sisteBehandling = sisteBehandlingOpt.get();

        if (sisteBehandling.erHenlagt()) {
            var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
            if (behandling.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(behandling.orElseThrow());
        }

    }

    private Map<UUID, DatoIntervallEntitet> finnRelevanteReferanser(Behandling behandling) {

        Map<UUID, DatoIntervallEntitet> koblingerÅSpørreMot = new HashMap<>();
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandling.getId());

        if (vilkårene.isEmpty()) {
            return Collections.emptyMap();
        }

        var vilkår = vilkårene.get().getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        if (vilkår.isEmpty()) {
            return Collections.emptyMap();
        }

        var overlappendeGrunnlag = vilkår
            .orElseThrow(() -> new IllegalStateException("Fagsaken(id=" + behandling.getFagsakId() + ") har ikke beregningsvilkåret knyttet til siste behandling"))
            .getPerioder()
            .stream()
            .filter(it -> Utfall.OPPFYLT.equals(it.getGjeldendeUtfall()))
            .toList();

        if (overlappendeGrunnlag.isEmpty()) {
            return Collections.emptyMap();
        }

        var bg = beregningPerioderGrunnlagRepository.hentGrunnlag(behandling.getId()).orElseThrow();


        overlappendeGrunnlag.forEach(og ->
            bg.finnGrunnlagFor(og.getSkjæringstidspunkt()).ifPresent(bgp -> {
                koblingerÅSpørreMot.put(bgp.getEksternReferanse(), og.getPeriode());
            }));


        return koblingerÅSpørreMot;

    }


    private AktivitetstatusOgArbeidsgiver mapTilAktivitetstatusOgArbeidsgiver(UtledetTilkommetAktivitet s) {
        final UttakArbeidType uttakArbeidType = UttakArbeidType.fraKode(s.getAktivitetStatus().getKode());
        final Arbeidsgiver arbeidsgiver;
        if (s.getArbeidsgiver() != null) {
            if (s.getArbeidsgiver().getArbeidsgiverAktørId() != null) {
                arbeidsgiver = Arbeidsgiver.person(new AktørId(s.getArbeidsgiver().getArbeidsgiverAktørId()));
            } else {
                arbeidsgiver = Arbeidsgiver.virksomhet(s.getArbeidsgiver().getArbeidsgiverOrgnr());
            }
        } else {
            arbeidsgiver = null;
        }
        final AktivitetstatusOgArbeidsgiver aktivitetstatusOgArbeidsgiver = new AktivitetstatusOgArbeidsgiver(uttakArbeidType, arbeidsgiver);
        return aktivitetstatusOgArbeidsgiver;
    }
}
