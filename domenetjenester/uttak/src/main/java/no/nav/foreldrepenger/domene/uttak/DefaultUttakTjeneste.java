package no.nav.foreldrepenger.domene.uttak;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.UttakPersonInfo;
import no.nav.foreldrepenger.domene.uttak.rest.UttakRestTjeneste;
import no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt.Person;
import no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt.Uttaksplan;
import no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt.UttaksplanRequest;

@ApplicationScoped
@Default
public class DefaultUttakTjeneste implements UttakTjeneste {

    private UttakRestTjeneste uttakRestTjeneste;

    protected DefaultUttakTjeneste() {
    }

    @Inject
    public DefaultUttakTjeneste(UttakRestTjeneste uttakRestTjeneste) {
        this.uttakRestTjeneste = uttakRestTjeneste;
    }
    
    @Override
    public Uttaksplan opprettUttaksplan(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        
        var utReq = new UttaksplanRequest();
        utReq.setBarn(mapPerson(input.getPleietrengende()));
        utReq.setSøker(mapPerson(input.getSøker()));
        utReq.setBehandlingId(ref.getBehandlingUuid());
        utReq.setSaksnummer(ref.getSaksnummer());
        
        return uttakRestTjeneste.opprettUttaksplan(utReq);
    }

    private Person mapPerson(UttakPersonInfo uttakPerson) {
        var person = new Person();
        person.setAktørId(uttakPerson.getAktørId().toString());
        person.setFødselsdato(uttakPerson.getFødselsdato());
        person.setDødsdato(uttakPerson.getDødsdato());
        return person;
    }

    @Override
    public boolean harAvslåttUttakPeriode(UUID behandlingUuid) {
        var uttaksplanOpt = hentUttaksplan(behandlingUuid);
        return uttaksplanOpt.map(ut -> ut.harAvslåttePerioder()).orElse(false);
    }

    @Override
    public Optional<Uttaksplan> hentUttaksplan(UUID behandlingUuid) {
        return hentUttaksplaner(behandlingUuid).stream().findFirst();
    }

    @Override
    public List<Uttaksplan> hentUttaksplaner(UUID... behandlingUuid) {
        return uttakRestTjeneste.hentUttaksplaner(behandlingUuid);
    }
}
