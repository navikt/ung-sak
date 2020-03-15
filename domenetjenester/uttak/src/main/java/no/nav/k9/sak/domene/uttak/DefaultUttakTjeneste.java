package no.nav.k9.sak.domene.uttak;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import no.finn.unleash.Unleash;
import no.nav.k9.sak.domene.uttak.input.UttakInput;
import no.nav.k9.sak.domene.uttak.input.UttakPersonInfo;
import no.nav.k9.sak.domene.uttak.rest.UttakRestTjeneste;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.Person;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.Uttaksplan;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.UttaksplanRequest;

@ApplicationScoped
@Default
public class DefaultUttakTjeneste implements UttakTjeneste {
    private static final String FEATURE_TOGGLE = "k9.uttak.rest";
    
    private UttakRestTjeneste uttakRestTjeneste;
    private Unleash unleash;

    private UttakInMemoryTjeneste uttakInMemoryTjeneste = new UttakInMemoryTjeneste();

    protected DefaultUttakTjeneste() {
    }

    @Inject
    public DefaultUttakTjeneste(UttakRestTjeneste uttakRestTjeneste, Unleash unleash) {
        this.uttakRestTjeneste = uttakRestTjeneste;
        this.unleash = unleash;
    }

    @Override
    public Uttaksplan opprettUttaksplan(UttakInput input) {
        var ref = input.getBehandlingReferanse();

        var utReq = new UttaksplanRequest();
        utReq.setBarn(mapPerson(input.getPleietrengende()));
        utReq.setSøker(mapPerson(input.getSøker()));
        utReq.setBehandlingId(ref.getBehandlingUuid());
        utReq.setSaksnummer(ref.getSaksnummer());

        if (unleash.isEnabled(FEATURE_TOGGLE)) {
            // FIXME K9: Fjern feature toggle når uttak tjeneste er oppe
            return uttakRestTjeneste.opprettUttaksplan(utReq);
        } else {
            return uttakInMemoryTjeneste.opprettUttaksplan(input);
        }
    }

    private Person mapPerson(UttakPersonInfo uttakPerson) {
        var person = new Person();
        person.setAktørId(uttakPerson.getAktørId().getId());
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
        if (unleash.isEnabled(FEATURE_TOGGLE)) {
            // FIXME K9: Fjern feature toggle når uttak tjeneste er oppe
            return uttakRestTjeneste.hentUttaksplaner(behandlingUuid);
        } else {
            return uttakInMemoryTjeneste.hentUttaksplaner(behandlingUuid);
        }
    }
}
