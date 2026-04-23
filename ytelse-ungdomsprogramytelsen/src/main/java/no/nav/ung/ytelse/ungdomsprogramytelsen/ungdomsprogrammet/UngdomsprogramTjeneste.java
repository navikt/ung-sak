package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet;


import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FagsakperiodeUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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

        boolean harUtvidetKvote = registerOpplysninger.opplysninger().stream()
            .anyMatch(UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO::harUtvidetKvote);

        if (registerOpplysninger.opplysninger().isEmpty()) {
            ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(), harUtvidetKvote);
            LOG.info("Fant ingen opplysninger om ungdomsprogrammet for aktør. ");
        } else {
            var timeline = lagTimeline(registerOpplysninger);
            if (harUtvidetKvote) {
                timeline = utvidProgramperiodeTilMaksKvote(timeline);
            }
            ungdomsprogramPeriodeRepository.lagre(behandling.getId(), mapPerioder(timeline), harUtvidetKvote);
        }
    }

    /**
     * Utvider programperiode-tidslinjen til maksimalt antall dager med utvidet kvote (300 virkedager).
     * Når NAV-veileder innvilger utvidet kvote, skal programperioden strekkes til å dekke
     * 300 virkedager fra programstart, slik at vilkårsperiodene og uttaksberegningen
     * reflekterer den utvidede kvoten.
     */
    private static LocalDateTimeline<Boolean> utvidProgramperiodeTilMaksKvote(LocalDateTimeline<Boolean> timeline) {
        var fom = timeline.getMinLocalDate();
        var utvidetTom = FagsakperiodeUtleder.finnTomDato(fom, LocalDateTimeline.empty(), true);
        var currentTom = timeline.getMaxLocalDate();
        if (!utvidetTom.isAfter(currentTom)) {
            return timeline;
        }
        // Legg til et ekstra segment for å dekke de gjenstående virkedagene
        var segments = new ArrayList<>(timeline.stream().map(s -> new LocalDateSegment<>(s.getFom(), s.getTom(), s.getValue())).toList());
        segments.add(new LocalDateSegment<>(currentTom.plusDays(1), utvidetTom, Boolean.TRUE));
        return new LocalDateTimeline<>(segments);
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
