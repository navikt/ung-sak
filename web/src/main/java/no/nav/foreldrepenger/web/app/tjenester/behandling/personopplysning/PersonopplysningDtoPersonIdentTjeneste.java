package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.k9.kodeverk.person.Diskresjonskode;
import no.nav.k9.sak.kontrakt.person.PersonopplysningDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;

@ApplicationScoped
public class PersonopplysningDtoPersonIdentTjeneste {
    private TpsTjeneste tpsTjeneste;

    public PersonopplysningDtoPersonIdentTjeneste() {
    }

    @Inject
    public PersonopplysningDtoPersonIdentTjeneste(TpsTjeneste tpsTjeneste) {
        this.tpsTjeneste = tpsTjeneste;
    }

    // oppdater med foedselsnr
    public void oppdaterMedPersonIdent(PersonopplysningDto personopplysningDto) {
        // memoriser oppslagsfunksjoner - unngår repeterende tjeneste kall eksternt
        Function<AktørId, Optional<PersonIdent>> personInfoFinder = memoize((aktørId) -> tpsTjeneste.hentFnr(aktørId));
        Function<String, Optional<String>> diskresjonskodeFinder = memoize((fnr) -> tpsTjeneste.hentDiskresjonskodeForAktør(new PersonIdent(fnr)));

        // Sett fødselsnummer og diskresjonskodepå personopplysning for alle
        // behandlinger. Fødselsnummer og diskresjonskode lagres ikke i basen og må derfor hentes fra
        // TPS/IdentRepository// for å vises i GUI.
        if (personopplysningDto != null) {
            setFnrPaPersonopplysning(personopplysningDto,
                personInfoFinder,
                diskresjonskodeFinder);
        }

    }

    void setFnrPaPersonopplysning(PersonopplysningDto dto, Function<AktørId, Optional<PersonIdent>> tpsFnrFinder,
                                  Function<String, Optional<String>> tpsKodeFinder) {

        // Soker
        dto.setFnr(findFnr(dto.getAktørId(), tpsFnrFinder)); // forelder / soeker
        dto.setDiskresjonskode(findKode(dto.getFnr(), tpsKodeFinder));

        // Medsoker
        if (dto.getAnnenPart() != null) {
            dto.getAnnenPart().setFnr(findFnr(dto.getAnnenPart().getAktørId(), tpsFnrFinder));
            dto.getAnnenPart().setDiskresjonskode(findKode(dto.getAnnenPart().getFnr(), tpsKodeFinder));
            // Medsøkers barn
            if (!dto.getAnnenPart().getBarn().isEmpty()) {
                for (PersonopplysningDto dtoBarn : dto.getAnnenPart().getBarn()) {
                    dtoBarn.setFnr(findFnr(dtoBarn.getAktørId(), tpsFnrFinder));
                    dtoBarn.setDiskresjonskode(findKode(dtoBarn.getFnr(), tpsKodeFinder));
                }
            }
        }

        // ektefelle
        if (dto.getEktefelle() != null) {
            dto.getEktefelle().setFnr(findFnr(dto.getEktefelle().getAktørId(), tpsFnrFinder));
            dto.getEktefelle().setDiskresjonskode(findKode(dto.getEktefelle().getFnr(), tpsKodeFinder));
        }

        // Barn
        for (PersonopplysningDto dtoBarn : dto.getBarn()) {
            dtoBarn.setFnr(findFnr(dtoBarn.getAktørId(), tpsFnrFinder));
            dtoBarn.setDiskresjonskode(findKode(dtoBarn.getFnr(), tpsKodeFinder));
        }
    }

    private Diskresjonskode findKode(String fnr, Function<String, Optional<String>> tpsKodeFinder) {
        if (fnr != null) {
            Optional<String> kode = tpsKodeFinder.apply(fnr);
            if (kode.isPresent()) {
                return Diskresjonskode.fraKode(kode.get());
            }
        }
        return Diskresjonskode.UDEFINERT;
    }

    private String findFnr(AktørId aktørId, Function<AktørId, Optional<PersonIdent>> tpsFnrFinder) {
        return aktørId == null ? null : tpsFnrFinder.apply(aktørId).map(PersonIdent::getIdent).orElse(null);

    }

    /** Lag en funksjon som husker resultat av tidligere input. Nyttig for repeterende lookups */
    static <I, O> Function<I, O> memoize(Function<I, O> f) {
        ConcurrentMap<I, O> lookup = new ConcurrentHashMap<>();
        return input -> input == null ? null : lookup.computeIfAbsent(input, f);
    }
}
