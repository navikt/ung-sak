package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet;


import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FagsakperiodeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

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

        LOG.info("Innhenter ungdomsprogramperioder for behandling={}: harUtvidetKvoteFraRegister={}, harUtvidetKvoteFraBehandlingsårsak={}, harUtvidetKvote={}",
            behandling.getId(), harUtvidetKvoteFraRegister, harUtvidetKvoteFraBehandlingsårsak, harUtvidetKvote);

        if (registerOpplysninger.opplysninger().isEmpty()) {
            ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(), harUtvidetKvote);
            LOG.info("Fant ingen opplysninger om ungdomsprogrammet for aktør. ");
        } else {
            var timeline = lagTimeline(registerOpplysninger);
            LOG.info("Programperiode fra register: fom={}, tom={}", timeline.getMinLocalDate(), timeline.getMaxLocalDate());
            if (harUtvidetKvote) {
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
        var erÅpen = tom.equals(LocalDate.of(9999, 12, 31));
        if (erÅpen) {
            var utvidetTom = FagsakperiodeUtleder.finnTomDato(fom, LocalDateTimeline.empty(), true);
            return timeline.intersection(new LocalDateInterval(fom, utvidetTom));
        }
        var nyFom = tom.plusDays(1);
        var utvidetTom = FagsakperiodeUtleder.finnTomDato(nyFom, timeline, true);
        if (!utvidetTom.isAfter(tom)) {
            return timeline;
        }
        var utvidelse = new LocalDateTimeline<>(nyFom, utvidetTom, true);
        return timeline.crossJoin(utvidelse);
    }

    private static LocalDateTimeline<Boolean> lagTimeline(UngdomsprogramRegisterKlient.DeltakerOpplysningerDTO registerOpplysninger) {
        var segmenter = registerOpplysninger.opplysninger().stream().map(it -> new LocalDateSegment<>(it.fraOgMed(), it.tilOgMed(), true)).toList();
        var timeline = new LocalDateTimeline<>(segmenter);
        timeline.compress();
        return timeline;
    }

    private static Collection<UngdomsprogramPeriode> mapPerioder(LocalDateTimeline<Boolean> dto) {
        return dto.stream().map(it -> new UngdomsprogramPeriode(it.getFom(), it.getTom())).toList();
    }

}
