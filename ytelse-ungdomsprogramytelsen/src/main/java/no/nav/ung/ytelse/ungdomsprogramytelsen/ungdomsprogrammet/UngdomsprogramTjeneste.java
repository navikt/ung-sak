package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet;


import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
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

        // Maks-dato sendes alltid fra registeret (260 virkedager ved normal kvote, 300 ved forlenget periode).
        LocalDate periodeMaksDato = registerOpplysninger.opplysninger().stream()
            .map(UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO::periodeMaksDato)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

        LOG.info("Innhenter ungdomsprogramperioder for behandling={}: harForlengetPeriodeFraRegister={}, harForlengetPeriodeFraBehandlingsårsak={}, harForlengetPeriode={}, periodeMaksDato={}",
            behandling.getId(), harForlengetPeriodeFraRegister, harForlengetPeriodeFraBehandlingsårsak, harForlengetPeriode, periodeMaksDato);

        if (registerOpplysninger.opplysninger().isEmpty()) {
            ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(), harForlengetPeriode, periodeMaksDato);
            LOG.info("Fant ingen opplysninger om ungdomsprogrammet for aktør.");
            return;
        }

        var timeline = lagTimeline(registerOpplysninger);
        LOG.info("Programperiode fra register: fom={}, tom={}", timeline.getMinLocalDate(), timeline.getMaxLocalDate());

        // Åpen periode fra register bevares uendret – maks-dato brukes til beregning downstream
        // via FagsakperiodeUtleder.finnTomDato.
        // Klippet periode fra register (opphør) beholdes alltid uendret.
        if (harForlengetPeriode && timeline.getMaxLocalDate().equals(TIDENES_ENDE)) {
            LOG.info("Forlenget periode er aktiv med periodeMaksDato={}. Bevarer åpen programperiode.", periodeMaksDato);
        }

        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), mapPerioder(timeline), harForlengetPeriode, periodeMaksDato);
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
