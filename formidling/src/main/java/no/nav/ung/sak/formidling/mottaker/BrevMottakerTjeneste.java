package no.nav.ung.sak.formidling.mottaker;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.typer.AktørId;

import java.util.Objects;

@Dependent
public class BrevMottakerTjeneste {

    private AktørTjeneste aktørTjeneste;
    private PersonopplysningRepository personopplysningRepository;

    @Inject
    public BrevMottakerTjeneste(AktørTjeneste aktørTjeneste, PersonopplysningRepository personopplysningRepository) {
        this.aktørTjeneste = aktørTjeneste;
        this.personopplysningRepository = personopplysningRepository;
    }

    public PdlPerson hentMottaker(Behandling behandling) {
        AktørId aktørId = behandling.getAktørId();
        PersonopplysningGrunnlagEntitet personopplysningGrunnlagEntitet = personopplysningRepository.hentPersonopplysninger(behandling.getId());
        PersonopplysningEntitet personopplysning = personopplysningGrunnlagEntitet.getGjeldendeVersjon().getPersonopplysning(aktørId);

        String navn = personopplysning.getNavn();

        var personIdent = aktørTjeneste.hentPersonIdentForAktørId(aktørId)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke person med aktørid"));

        String fnr = personIdent.getIdent();
        Objects.requireNonNull(fnr);

        return new PdlPerson(fnr, aktørId, navn, personopplysning.getDødsdato());
    }

}
