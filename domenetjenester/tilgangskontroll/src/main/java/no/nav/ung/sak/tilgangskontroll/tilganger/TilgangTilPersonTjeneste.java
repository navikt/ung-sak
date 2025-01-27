package no.nav.ung.sak.tilgangskontroll.tilganger;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class TilgangTilPersonTjeneste {

    private final static Logger logger = LoggerFactory.getLogger(TilgangTilPersonTjeneste.class);

    private final PersonDiskresjonskodeTjeneste personDiskresjonskodeTjeneste;

    @Inject
    public TilgangTilPersonTjeneste(PersonDiskresjonskodeTjeneste personDiskresjonskodeTjeneste) {
        this.personDiskresjonskodeTjeneste = personDiskresjonskodeTjeneste;
    }

    @WithSpan
    public Set<IkkeTilgangÅrsak> sjekkTilgangTilPersoner(TilgangerBruker tilganger, List<AktørId> aktørIder, List<PersonIdent> personIdenter) {
        try {
            return internSjekkTilgangTilPersoner(tilganger, aktørIder, personIdenter);
        } catch (Exception e) {
            logger.warn("Fikk teknisk feil i tilgangssjekk, gir derfor ikke tilgang til operasjonen", e);
            return Set.of(IkkeTilgangÅrsak.TEKNISK_FEIL);
        }
    }

    Set<IkkeTilgangÅrsak> internSjekkTilgangTilPersoner(TilgangerBruker tilganger, List<AktørId> aktørIder, List<PersonIdent> personIdenter) {
        Set<Diskresjonskode> diskresjonskoder = personDiskresjonskodeTjeneste.hentDiskresjonskoder(aktørIder, personIdenter);
        return sjekkTilgangTilDiskresjonskoder(tilganger, diskresjonskoder);
    }


    private static Set<IkkeTilgangÅrsak> sjekkTilgangTilDiskresjonskoder(TilgangerBruker tilganger, Set<Diskresjonskode> diskresjonskoderPåPersoner) {
        return diskresjonskoderPåPersoner.stream()
            .map(diskresjonskode -> switch (diskresjonskode) {
                case KODE6 -> tilganger.kanBehandleKode6() ? null : IkkeTilgangÅrsak.HAR_IKKE_TILGANG_TIL_KODE6_PERSON;
                case KODE7 -> tilganger.kanBehandleKode7() ? null : IkkeTilgangÅrsak.HAR_IKKE_TILGANG_TIL_KODE7_PERSON;
                case SKJERMET ->
                    tilganger.kanBehandleEgenAnsatt() ? null : IkkeTilgangÅrsak.HAR_IKKE_TILGANG_TIL_EGEN_ANSATT;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
}
