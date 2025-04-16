package no.nav.ung.sak.domene.vedtak.ekstern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryFeil;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.Ytelse;
import no.nav.ung.sak.domene.iay.modell.YtelseAnvist;
import no.nav.ung.sak.domene.iay.modell.YtelseFilter;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelseUtleder;
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseTilkjentYtelseUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class VurderOmArenaYtelseSkalOpphøre {
    private static final Logger log = LoggerFactory.getLogger(VurderOmArenaYtelseSkalOpphøre.class);

    private static final long HALV_MELDEKORT_PERIODE = 9;
    private static final Period MELDEKORT_PERIODE = Period.ofDays(14);

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private TilkjentYtelseUtleder tilkjentYtelseUtleder;

    VurderOmArenaYtelseSkalOpphøre() {
        // for CDI proxy
    }

    @Inject
    public VurderOmArenaYtelseSkalOpphøre(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                          BehandlingVedtakRepository behandlingVedtakRepository,
                                          OppgaveTjeneste oppgaveTjeneste,
                                          UngdomsytelseTilkjentYtelseUtleder utledTilkjentYtelse) {
        this.iayTjeneste = inntektArbeidYtelseTjeneste;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.tilkjentYtelseUtleder = utledTilkjentYtelse;
    }

    void opprettOppgaveHvisArenaytelseSkalOpphøre(Long behandlingId, AktørId aktørId, LocalDate skjæringstidspunkt) {
        BehandlingVedtak vedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandlingId)
            .orElseThrow(() -> BehandlingRepositoryFeil.FACTORY.fantIkkeBehandlingVedtak(behandlingId).toException());
        if (!VedtakResultatType.INNVILGET.equals(vedtak.getVedtakResultatType())) {
            return;
        }
        LocalDate vedtaksDato = vedtak.getVedtaksdato();
        LocalDate startdatoYtelsen = finnFørsteAnvistDato(behandlingId).orElse(skjæringstidspunkt);

        if (vurderArenaYtelserOpphøres(behandlingId, aktørId, startdatoYtelsen, vedtaksDato)) {
            String oppgaveId = oppgaveTjeneste.opprettOppgaveStopUtbetalingAvARENAYtelse(behandlingId, startdatoYtelsen);
            log.info("Oppgave opprettet i GOSYS slik at NØS kan behandle saken videre. Oppgavenummer: {}", oppgaveId);
        }
    }

    /**
     * Ved iverksetting av vedtak skal FPSAK gjøre en sjekk av om det er overlapp mellom startdato for foreldrepenger
     * og utbetalt ytelse i ARENA. FPSAK skal benytte lagrede registerdata om meldekortperioder for å vurdere om
     * startdatoen for foreldrepenger overlapper med ytelse i ARENA.
     *
     * @param behandlingId     behandling til saken i FP
     * @param førsteAnvistDato første dato for utbetaling
     * @param vedtaksDato      vedtaksdato
     * @return true hvis det finnes en overlappende ytelse i ARENA, ellers false
     */
    boolean vurderArenaYtelserOpphøres(Long behandlingId, AktørId aktørId, LocalDate førsteAnvistDato, LocalDate vedtaksDato) {
        LocalDate senesteInputDato = vedtaksDato.isAfter(førsteAnvistDato) ? vedtaksDato : førsteAnvistDato;
        var arenaYtelser = hentArenaYtelser(behandlingId, aktørId, senesteInputDato);

        // Ser både på løpende og avsluttede vedtak som overlapper første anvist dato
        if (!finnesYtelseVedtakPåEtterStartdato(arenaYtelser, førsteAnvistDato)) {
            return false;
        }

        LocalDate sisteArenaAnvistDatoFørVedtaksdato = finnSisteArenaAnvistDatoFørVedtaksdato(arenaYtelser, vedtaksDato);
        if (sisteArenaAnvistDatoFørVedtaksdato == null) {
            return false;
        }
        if (førsteAnvistDato.isBefore(sisteArenaAnvistDatoFørVedtaksdato)) {
            return true;
        }

        // sjekk frem i tid også dersom bruker er mottaker (mot meldekort)
        Optional<LocalDate> nesteArenaAnvistDatoEtterVedtaksdato = finnNesteArenaAnvistDatoEtterVedtaksdato(arenaYtelser, vedtaksDato, sisteArenaAnvistDatoFørVedtaksdato);
        return (nesteArenaAnvistDatoEtterVedtaksdato.isPresent() &&
            DatoIntervallEntitet.fraOgMedTilOgMed(sisteArenaAnvistDatoFørVedtaksdato, nesteArenaAnvistDatoEtterVedtaksdato.get()).inkluderer(førsteAnvistDato) &&
            vedtaksDato.isAfter(nesteArenaAnvistDatoEtterVedtaksdato.get().minusDays(HALV_MELDEKORT_PERIODE)));
    }


    private Collection<Ytelse> hentArenaYtelser(Long behandlingId, AktørId aktørId, LocalDate skjæringstidspunkt) {
        var ytelseFilter = iayTjeneste.finnGrunnlag(behandlingId)
            .map(it -> new YtelseFilter(it.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunkt)).orElse(YtelseFilter.EMPTY);

        return ytelseFilter
            .filter(y -> Fagsystem.ARENA.equals(y.getKilde()))
            .getFiltrertYtelser();
    }

    private Optional<LocalDate> finnFørsteAnvistDato(Long behandlingId) {
        var tidslinje = tilkjentYtelseUtleder.utledTilkjentYtelseTidslinje(behandlingId);
        return tidslinje.isEmpty() ? Optional.empty() : Optional.of(tidslinje.getMinLocalDate());
    }

    private LocalDate finnSisteArenaAnvistDatoFørVedtaksdato(Collection<Ytelse> ytelser, LocalDate vedtaksdato) {
        return ytelser.stream()
            .map(Ytelse::getYtelseAnvist)
            .flatMap(Collection::stream)
            .map(YtelseAnvist::getAnvistTOM)
            .filter(vedtaksdato::isAfter)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }

    private Optional<LocalDate> finnNesteArenaAnvistDatoEtterVedtaksdato(Collection<Ytelse> ytelser, LocalDate vedtaksdato, LocalDate sisteAnvisteDatoArena) {
        // Venter ikke egentlig treff her ettersom vedtaksdato som regel er dagens dato
        var nesteAnvistDato = ytelser.stream()
            .map(Ytelse::getYtelseAnvist)
            .flatMap(Collection::stream)
            .map(YtelseAnvist::getAnvistFOM)
            .filter(vedtaksdato::isBefore)
            .min(Comparator.naturalOrder());
        if (nesteAnvistDato.isPresent()) {
            return nesteAnvistDato;
        }
        var ytelserPåVedtaksdato = ytelser.stream()
            .filter(y -> y.getPeriode().inkluderer(vedtaksdato))
            .collect(Collectors.toList());
        if (!ytelserPåVedtaksdato.isEmpty()) {
            return Optional.of(sisteAnvisteDatoArena.plus(MELDEKORT_PERIODE));
        }
        nesteAnvistDato = ytelser.stream()
            .filter(y -> y.getPeriode().getFomDato().isAfter(vedtaksdato))
            .map(Ytelse::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .min(Comparator.naturalOrder());
        return nesteAnvistDato;
    }

    private boolean finnesYtelseVedtakPåEtterStartdato(Collection<Ytelse> ytelser, LocalDate startdato) {
        return ytelser.stream()
            .anyMatch(y -> y.getPeriode().inkluderer(startdato) || y.getPeriode().getFomDato().isAfter(startdato));
    }
}
