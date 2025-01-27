package no.nav.ung.domenetjenester.personhendelser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.ung.domenetjenester.personhendelser.utils.PersonhendelseUtils;
import no.nav.ung.sak.hendelsemottak.tjenester.FinnFagsakerForAktørTjeneste;
import no.nav.ung.sak.hendelsemottak.tjenester.HendelsemottakTjeneste;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.typer.AktørId;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@ApplicationScoped
public class PdlLeesahHendelseFiltrerer {

    private HendelsemottakTjeneste hendelsemottakTjeneste;
    private FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste;

    public PdlLeesahHendelseFiltrerer() {
    }

    @Inject
    public PdlLeesahHendelseFiltrerer(HendelsemottakTjeneste hendelsemottakTjeneste, FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste) {
        this.hendelsemottakTjeneste = hendelsemottakTjeneste;
        this.finnFagsakerForAktørTjeneste = finnFagsakerForAktørTjeneste;
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

    boolean erHendelseTypeViSkalHåndtere(String key, Personhendelse personhendelse) {
        return PersonhendelseUtils.erStøttetHendelseType(personhendelse);
    }

    boolean harPåvirketUngFagsakForHendelse(String key, Personhendelse personhendelse) {
        return mapIdenterTilAktørIder(personhendelse).stream()
            .filter(Optional::isPresent)
            .anyMatch(aktørId -> finnFagsakerForAktørTjeneste.harRelevantFagsakForAktør(aktørId.get()));
    }

    private List<Optional<AktørId>> mapIdenterTilAktørIder(Personhendelse personhendelse) {
        return personhendelse.getPersonidenter().stream()
            .map(PersonhendelseUtils::mapIdentTilAktørId)
            .filter(Optional::isPresent)
            .collect(Collectors.toList());
    }

}
