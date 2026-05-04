package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet;


import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramUtvidetKvote;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FagsakperiodeUtleder;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FinnForbrukteDager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

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

        // Utvidet kvote gjelder dersom registeret returnerer flagget ELLER behandlingen ble trigget av
        // utvidet kvote-hendelse. Sistnevnte håndterer tilfeller der registeret ennå ikke har oppdatert flagget.
        boolean harUtvidetKvoteFraRegister = registerOpplysninger.opplysninger().stream()
            .anyMatch(UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO::harUtvidetKvote);
        boolean harUtvidetKvoteFraBehandlingsårsak = behandling.getBehandlingÅrsakerTyper()
            .contains(BehandlingÅrsakType.RE_HENDELSE_UTVIDET_KVOTE_UNGDOMSPROGRAM);
        boolean harUtvidetKvote = harUtvidetKvoteFraRegister || harUtvidetKvoteFraBehandlingsårsak;

        // Sjekk om utvidet kvote allerede er materialisert (beregnet til konkret tom-dato) i et tidligere
        // grunnlag. Grunnlag kopieres til nye revurderinger via UngdomsprogramPeriodeRepository.kopier(),
        // så det aktive grunnlaget på behandlingen reflekterer forrige tilstand før vi skriver på nytt.
        //
        // NB: Ideelt sett burde ung-deltakelse-opplyser (registeret) ikke sende inn utvidet periode ved utvidet
        // kvote i det hele tatt – ung-sak burde i stedet beregne perioden selv basert på kvote-flagget der det er
        // nødvendig (f.eks. ved første gangs utvidelse). Inntil registeret er endret håndterer vi det her ved å
        // beregne utvidelsen kun én gang, og deretter stole på registerets perioder for å unngå at utvidelsen
        // re-deriveres ved senere innhentinger (f.eks. opphør, der register sender klippet tom – da ville en
        // re-derivering ført til to disjunkte segmenter og brutt valideringen om nøyaktig én programperiode).
        boolean alleredeUtvidetIEttTidligereGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId())
            .flatMap(UngdomsprogramPeriodeGrunnlag::getUngdomsprogramUtvidetKvote)
            .map(UngdomsprogramUtvidetKvote::isHarUtvidetKvote)
            .orElse(false);

        LOG.info("Innhenter ungdomsprogramperioder for behandling={}: harUtvidetKvoteFraRegister={}, harUtvidetKvoteFraBehandlingsårsak={}, harUtvidetKvote={}, alleredeUtvidetIEttTidligereGrunnlag={}",
            behandling.getId(), harUtvidetKvoteFraRegister, harUtvidetKvoteFraBehandlingsårsak, harUtvidetKvote, alleredeUtvidetIEttTidligereGrunnlag);

        if (registerOpplysninger.opplysninger().isEmpty()) {
            ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(), harUtvidetKvote);
            LOG.info("Fant ingen opplysninger om ungdomsprogrammet for aktør. ");
        } else {
            var timeline = lagTimeline(registerOpplysninger);
            LOG.info("Programperiode fra register: fom={}, tom={}", timeline.getMinLocalDate(), timeline.getMaxLocalDate());
            if (harUtvidetKvote && !alleredeUtvidetIEttTidligereGrunnlag) {
                // Materialiser utvidelsen én gang. Etterpå er konkret tom-dato lagret i grunnlaget,
                // og registerets perioder skal være sannhetskilden ved senere innhentinger.
                timeline = utvidProgramperiodeTilMaksKvote(timeline);
                LOG.info("Programperiode utvidet til: fom={}, tom={}", timeline.getMinLocalDate(), timeline.getMaxLocalDate());
            }
            ungdomsprogramPeriodeRepository.lagre(behandling.getId(), mapPerioder(timeline), harUtvidetKvote);
        }
    }

    /**
     * Utvider programperiode-tidslinjen til maksimalt antall virkedager ved utvidet kvote (300 virkedager).
     *
     * <p>To scenarioer håndteres kant-i-kant:
     * <ul>
     *     <li>Åpen programperiode (tom=9999-12-31, løpende): klippes til 300 virkedager fra fom.</li>
     *     <li>Klippet programperiode (opphør satt, eller 260 virkedager allerede forbrukt):
     *         legges til de resterende virkedagene (opp til 300 totalt) kant-i-kant etter eksisterende tom.</li>
     * </ul>
     *
     * <p>Prinsippet er at utvidelsen alltid skal være kant-i-kant, slik at de nye dagene kan behandles
     * som en vanlig ytelsesperiode (kontroll av inntekt, aldersovergang, g-regulering osv.).
     */
    private static LocalDateTimeline<Boolean> utvidProgramperiodeTilMaksKvote(LocalDateTimeline<Boolean> timeline) {
        var fom = timeline.getMinLocalDate();
        var tom = timeline.getMaxLocalDate();
        var erÅpen = tom.equals(TIDENES_ENDE);
        boolean harUtvidetKvote = true;
        if (erÅpen) {
            var utvidetTom = FagsakperiodeUtleder.finnTomDato(fom, LocalDateTimeline.empty(), harUtvidetKvote);
            return timeline.intersection(new LocalDateInterval(fom, utvidetTom));
        }
        // Beregn eksplisitt gjenstående virkedager for å unngå feil ved grenseverdier.
        // finnTomDato returnerer nyFom både når det gjenstår 1 virkedag og når kvoten er oppbrukt,
        var forbrukteDager = FinnForbrukteDager.finnForbrukteDager(timeline, harUtvidetKvote).forbrukteDager();
        var gjenståendeDager = FinnForbrukteDager.getMaksAntallDager(harUtvidetKvote) - forbrukteDager;
        if (gjenståendeDager <= 0) {
            return timeline;
        }
        var nyFom = tom.plusDays(1);
        var utvidetTom = FagsakperiodeUtleder.finnTomDato(nyFom, timeline, harUtvidetKvote);
        var utvidelse = new LocalDateTimeline<>(nyFom, utvidetTom, harUtvidetKvote);
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
