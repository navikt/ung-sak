package no.nav.ung.sak.domene.registerinnhenting.impl.startpunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.ung.sak.domene.person.personopplysning.PersonopplysningGrunnlagDiff;
import no.nav.ung.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
@GrunnlagRef(PersonInformasjonEntitet.class)
@FagsakYtelseTypeRef
class StartpunktUtlederPersonopplysning implements EndringStartpunktUtleder {

    private final String source = this.getClass().getSimpleName();
    private PersonopplysningRepository personopplysningRepository;

    StartpunktUtlederPersonopplysning() {
        // For CDI
    }

    @Inject
    StartpunktUtlederPersonopplysning(PersonopplysningRepository personopplysningRepository) {
        this.personopplysningRepository = personopplysningRepository;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object oppdatertGrunnlagId, Object forrigeGrunnlagId) {
        PersonopplysningGrunnlagEntitet oppdatertGrunnlag = personopplysningRepository.hentPersonopplysningerPåId((Long) oppdatertGrunnlagId);
        PersonopplysningGrunnlagEntitet forrigeGrunnlag = personopplysningRepository.hentPersonopplysningerPåId((Long) forrigeGrunnlagId);
        return utled(ref, oppdatertGrunnlag, forrigeGrunnlag);
    }

    private StartpunktType utled(BehandlingReferanse ref, PersonopplysningGrunnlagEntitet oppdatertGrunnlag, PersonopplysningGrunnlagEntitet forrigeGrunnlag) {
        return hentAlleStartpunktForPersonopplysninger(ref, oppdatertGrunnlag, forrigeGrunnlag).stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    // Finn endringer per aggregat under grunnlaget og map dem mot startpunkt. Dekker bruker og TPS-relaterte personer (barn, ekte). Bør spisses der det er behov.
    private List<StartpunktType> hentAlleStartpunktForPersonopplysninger(BehandlingReferanse ref, PersonopplysningGrunnlagEntitet oppdatertGrunnlag, PersonopplysningGrunnlagEntitet forrigeGrunnlag) {
        var aktørId = ref.getAktørId();

        PersonopplysningGrunnlagDiff poDiff = new PersonopplysningGrunnlagDiff(aktørId, oppdatertGrunnlag, forrigeGrunnlag);
        boolean forelderDødEndret = poDiff.erForeldreDødsdatoEndret();

        Set<StartpunktType> startpunkter = new LinkedHashSet<>();
        if (forelderDødEndret) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.BEREGNING, "foreldres død", oppdatertGrunnlag.getId(), håndtereNull(forrigeGrunnlag));
            startpunkter.add(StartpunktType.UTTAKSVILKÅR);
        }
        if (poDiff.erSivilstandEndretForBruker()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.UTTAKSVILKÅR, "sivilstand", oppdatertGrunnlag.getId(), håndtereNull(forrigeGrunnlag));
            startpunkter.add(StartpunktType.UTTAKSVILKÅR);
        }

        if (poDiff.erBarnDødsdatoEndret()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.BEREGNING, "barnets dødsdato", oppdatertGrunnlag.getId(), håndtereNull(forrigeGrunnlag));
            startpunkter.add(StartpunktType.UTTAKSVILKÅR);
        }

        if (startpunkter.isEmpty()) {
            // Endringen som trigget utledning av startpunkt skal ikke styre startpunkt
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(source, StartpunktType.UDEFINERT, "personopplysning - andre endringer", oppdatertGrunnlag.getId(), håndtereNull(forrigeGrunnlag));
            startpunkter.add(StartpunktType.UDEFINERT);
        }
        return List.copyOf(startpunkter);
    }

    private Long håndtereNull(PersonopplysningGrunnlagEntitet forrigeGrunnlag) {
        return forrigeGrunnlag != null ? forrigeGrunnlag.getId() : null;
    }

}
