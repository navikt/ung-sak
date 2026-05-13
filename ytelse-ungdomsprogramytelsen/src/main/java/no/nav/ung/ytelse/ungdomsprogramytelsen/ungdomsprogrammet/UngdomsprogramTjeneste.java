package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet;


import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramForlengetPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FagsakperiodeUtleder;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FinnForbrukteDager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static no.nav.k9.felles.konfigurasjon.konfig.Tid.TIDENES_ENDE;

/**
 * Tjeneste for innhenting av ungdomsprogramopplysninger fra register.
 */
@Dependent
public class UngdomsprogramTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(UngdomsprogramTjeneste.class);

    private UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public UngdomsprogramTjeneste(UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungdomsprogramRegisterKlient = ungdomsprogramRegisterKlient;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    public UngdomsprogramTjeneste() {
    }

    public void innhentOpplysninger(Behandling behandling) {
        var registerOpplysninger = ungdomsprogramRegisterKlient.hentForAktørId(behandling.getFagsak().getAktørId().getAktørId());

        // Forlenget periode gjelder dersom registeret returnerer flagget ELLER behandlingen ble trigget av
        // forlenget-periode-hendelse. Sistnevnte håndterer tilfeller der registeret ennå ikke har oppdatert flagget.
        boolean harForlengetPeriodeFraRegister = registerOpplysninger.opplysninger().stream()
            .anyMatch(UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO::harForlengetPeriode);
        boolean harForlengetPeriodeFraBehandlingsårsak = behandling.getBehandlingÅrsakerTyper()
            .contains(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM);
        boolean harForlengetPeriode = harForlengetPeriodeFraRegister || harForlengetPeriodeFraBehandlingsårsak;

        // Maks-dato fra registeret (kan være null i overgangsperioden).
        LocalDate forlengetPeriodeMaksDatoFraRegister = registerOpplysninger.opplysninger().stream()
            .map(UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO::forlengetPeriodeMaksDato)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

        // Bevar tidligere maks-dato dersom registeret ikke sender den (typisk i overgangsperioden eller
        // ved hendelses-trigget revurdering der registeret enda ikke har oppdatert opplysningene).
        LocalDate eksisterendeMaksDato = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId())
            .flatMap(UngdomsprogramPeriodeGrunnlag::getForlengetPeriodeMaksDato)
            .orElse(null);
        LocalDate forlengetPeriodeMaksDato = forlengetPeriodeMaksDatoFraRegister != null
            ? forlengetPeriodeMaksDatoFraRegister
            : eksisterendeMaksDato;

        // Sjekk om forlenget periode allerede er materialisert (beregnet til konkret tom-dato) i et tidligere
        // grunnlag. Grunnlag kopieres til nye revurderinger via UngdomsprogramPeriodeRepository.kopier(),
        // så det aktive grunnlaget på behandlingen reflekterer forrige tilstand før vi skriver på nytt.
        //
        // NB: Ideelt sett burde ung-deltakelse-opplyser (registeret) ikke sende inn forlenget periode-flagg
        // i det hele tatt – ung-sak burde i stedet beregne perioden selv basert på flagget der det er
        // nødvendig (f.eks. ved første gangs forlengelse). Inntil registeret er endret håndterer vi det her ved å
        // beregne forlengelsen kun én gang, og deretter stole på registerets perioder for å unngå at forlengelsen
        // re-deriveres ved senere innhentinger (f.eks. opphør, der register sender klippet tom – da ville en
        // re-derivering ført til to disjunkte segmenter og brutt valideringen om nøyaktig én programperiode).
        boolean alleredeForlengetIEttTidligereGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId())
            .flatMap(UngdomsprogramPeriodeGrunnlag::getUngdomsprogramForlengetPeriode)
            .map(UngdomsprogramForlengetPeriode::harForlengetPeriode)
            .orElse(false);

        LOG.info("Innhenter ungdomsprogramperioder for behandling={}: harForlengetPeriodeFraRegister={}, harForlengetPeriodeFraBehandlingsårsak={}, harForlengetPeriode={}, alleredeForlengetIEttTidligereGrunnlag={}, forlengetPeriodeMaksDato={}",
            behandling.getId(), harForlengetPeriodeFraRegister, harForlengetPeriodeFraBehandlingsårsak, harForlengetPeriode, alleredeForlengetIEttTidligereGrunnlag, forlengetPeriodeMaksDato);

        if (registerOpplysninger.opplysninger().isEmpty()) {
            ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(), harForlengetPeriode, forlengetPeriodeMaksDato);
            LOG.info("Fant ingen opplysninger om ungdomsprogrammet for aktør.");
            return;
        }

        var timeline = lagTimeline(registerOpplysninger);
        LOG.info("Programperiode fra register: fom={}, tom={}", timeline.getMinLocalDate(), timeline.getMaxLocalDate());

        boolean erÅpenPeriode = timeline.getMaxLocalDate().equals(TIDENES_ENDE);

        if (harForlengetPeriode && erÅpenPeriode) {
            if (forlengetPeriodeMaksDato != null) {
                // Maks-dato er kjent fra registeret – bevar åpen periode og bruk maks-dato til beregning downstream.
                LOG.info("Forlenget periode er aktiv med forlengetPeriodeMaksDato={}. Bevarer åpen programperiode.", forlengetPeriodeMaksDato);
            } else if (!alleredeForlengetIEttTidligereGrunnlag) {
                // Overgangsfallback: registeret har ikke sendt maks-dato ennå. Materialiser én gang slik vi
                // tidligere gjorde, og logg WARN. Skal fjernes når registeret alltid sender feltet.
                // TODO: Fjern fallback når ung-deltakelse-opplyser alltid sender forlengetPeriodeMaksDato.
                LOG.warn("Forlenget periode er aktiv for behandling={}, men registeret har ikke sendt forlengetPeriodeMaksDato. Bruker midlertidig materialisering.", behandling.getId());
                timeline = forlengProgramperiodeTilMaksdato(timeline);
                LOG.info("Programperiode forlenget (fallback) til: fom={}, tom={}", timeline.getMinLocalDate(), timeline.getMaxLocalDate());
            }
        }

        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), mapPerioder(timeline), harForlengetPeriode, forlengetPeriodeMaksDato);
    }

    /**
     * Forlenger programperiode-tidslinjen til maksimalt antall virkedager ved forlenget periode (300 virkedager).
     *
     * <p>To scenarioer håndteres kant-i-kant:
     * <ul>
     *     <li>Åpen programperiode (tom=9999-12-31, løpende): klippes til 300 virkedager fra fom.</li>
     *     <li>Klippet programperiode (opphør satt, eller 260 virkedager allerede forbrukt):
     *         legges til de resterende virkedagene (opp til 300 totalt) kant-i-kant etter eksisterende tom.</li>
     * </ul>
     *
     * <p>Prinsippet er at forlengelsen alltid skal være kant-i-kant, slik at de nye dagene kan behandles
     * som en vanlig ytelsesperiode (kontroll av inntekt, aldersovergang, g-regulering osv.).
     */
    private static LocalDateTimeline<Boolean> forlengProgramperiodeTilMaksdato(LocalDateTimeline<Boolean> timeline) {
        var fom = timeline.getMinLocalDate();
        var tom = timeline.getMaxLocalDate();
        var erÅpen = tom.equals(TIDENES_ENDE);
        boolean harForlengetPeriode = true;
        if (erÅpen) {
            var utvidetTom = FagsakperiodeUtleder.finnTomDato(fom, LocalDateTimeline.empty(), harForlengetPeriode);
            return timeline.intersection(new LocalDateInterval(fom, utvidetTom));
        }
        // Beregn eksplisitt gjenstående virkedager for å unngå feil ved grenseverdier.
        var forbrukteDager = FinnForbrukteDager.finnForbrukteDager(timeline, harForlengetPeriode).forbrukteDager();
        var gjenståendeDager = FinnForbrukteDager.getMaksAntallDager(harForlengetPeriode) - forbrukteDager;
        if (gjenståendeDager <= 0) {
            return timeline;
        }
        var nyFom = tom.plusDays(1);
        var utvidetTom = FagsakperiodeUtleder.finnTomDato(nyFom, timeline, harForlengetPeriode);
        var utvidelse = new LocalDateTimeline<>(nyFom, utvidetTom, harForlengetPeriode);
        return timeline.crossJoin(utvidelse);
    }

    private static LocalDateTimeline<Boolean> lagTimeline(UngdomsprogramRegisterKlient.DeltakerOpplysningerDTO registerOpplysninger) {
        var segmenter = registerOpplysninger.opplysninger().stream()
            .map(it -> new LocalDateSegment<>(it.fraOgMed(), it.tilOgMed(), true)).toList();
        // LocalDateTimeline.compress() returnerer ny tidslinje – returverdien må brukes.
        return new LocalDateTimeline<>(segmenter).compress();
    }

    private static Collection<UngdomsprogramPeriode> mapPerioder(LocalDateTimeline<Boolean> dto) {
        return dto.stream().map(it -> new UngdomsprogramPeriode(it.getFom(), it.getTom())).toList();
    }

}
