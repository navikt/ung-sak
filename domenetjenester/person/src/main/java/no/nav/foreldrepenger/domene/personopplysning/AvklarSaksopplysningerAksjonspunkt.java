package no.nav.foreldrepenger.domene.personopplysning;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.sak.typer.AktørId;

class AvklarSaksopplysningerAksjonspunkt {
    private PersonopplysningRepository personopplysningRepository;

    AvklarSaksopplysningerAksjonspunkt(PersonopplysningRepository personopplysningRepository) {
        this.personopplysningRepository = personopplysningRepository;
    }

    void oppdater(Long behandlingId, AktørId aktørId, PersonopplysningAksjonspunkt adapter) {
        PersonInformasjonBuilder builder = personopplysningRepository.opprettBuilderForOverstyring(behandlingId);

        LocalDate fom = adapter.getPersonstatusTypeKode().get().getGyldigFom();
        LocalDate tom = adapter.getPersonstatusTypeKode().get().getGyldigTom();
        DatoIntervallEntitet intervall = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);

        final PersonstatusType status = setPersonstatusType(adapter.getPersonstatusTypeKode().map(PersonopplysningAksjonspunkt.PersonstatusPeriode::getPersonstatus));
        if (status != null) {
            PersonInformasjonBuilder.PersonstatusBuilder medPersonstatus = builder.getPersonstatusBuilder(aktørId, intervall)
                .medAktørId(aktørId)
                .medPeriode(intervall)
                .medPersonstatus(status);
            builder.leggTil(medPersonstatus);

            personopplysningRepository.lagre(behandlingId, builder);
        }
    }

    private PersonstatusType setPersonstatusType(Optional<PersonstatusType> personstatus) {
        if (personstatus.isPresent()) {
            Set<PersonstatusType> personstatusType = PersonstatusType.personstatusTyperFortsattBehandling();
            var personstatusen = personstatus.get();
            for (PersonstatusType type : personstatusType) {
                if (type.equals(personstatusen)) {
                    return type;
                }
            }
        }
        return null;
    }

}
