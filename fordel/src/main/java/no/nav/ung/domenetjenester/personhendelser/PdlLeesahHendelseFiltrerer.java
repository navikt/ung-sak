package no.nav.ung.domenetjenester.personhendelser;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.ung.sak.hendelsemottak.tjenester.HendelsemottakTjeneste;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.typer.AktørId;


@Dependent
public class PdlLeesahHendelseFiltrerer {

    private final PdlLeesahOversetter oversetter;
    private final HendelsemottakTjeneste hendelsemottakTjeneste;

    @Inject
    public PdlLeesahHendelseFiltrerer(PdlLeesahOversetter oversetter, HendelsemottakTjeneste hendelsemottakTjeneste) {
        this.oversetter = oversetter;
        this.hendelsemottakTjeneste = hendelsemottakTjeneste;
    }

    Optional<Hendelse> oversettStøttetPersonhendelse(Personhendelse personhendelse) {
        return oversetter.oversettStøttetPersonhendelse(personhendelse);
    }

    List<AktørId> finnAktørerMedPåvirketUngFagsak(Hendelse hendelse) {
        var aktørIderMedPåvirketSak = hendelse.getHendelseInfo().getAktørIder().stream()
            .filter(aktørId -> {
                var påvirkedeSaker = hendelsemottakTjeneste.finnFagsakerTilVurdering(hendelse);
                return !påvirkedeSaker.isEmpty();
            })
            .collect(Collectors.toList());
        return aktørIderMedPåvirketSak;
    }

}
