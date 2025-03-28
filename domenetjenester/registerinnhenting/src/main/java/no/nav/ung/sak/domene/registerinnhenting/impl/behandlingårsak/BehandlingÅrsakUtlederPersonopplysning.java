package no.nav.ung.sak.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.domene.person.personopplysning.PersonopplysningGrunnlagDiff;
import no.nav.ung.sak.domene.registerinnhenting.GrunnlagRef;

@ApplicationScoped
@GrunnlagRef(PersonInformasjonEntitet.class)
@FagsakYtelseTypeRef
public class BehandlingÅrsakUtlederPersonopplysning implements BehandlingÅrsakUtleder {
    private static final Logger log = LoggerFactory.getLogger(BehandlingÅrsakUtlederPersonopplysning.class);

    private PersonopplysningRepository personopplysningRepository;


    BehandlingÅrsakUtlederPersonopplysning() {
        // For CDI
    }

    @Inject
    BehandlingÅrsakUtlederPersonopplysning(PersonopplysningRepository personopplysningRepository) {
        this.personopplysningRepository = personopplysningRepository;
    }

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        Long grunnlag1 = (Long) grunnlagId1;
        Long grunnlag2 = (Long) grunnlagId2;

        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = personopplysningRepository.hentPersonopplysningerPåId(grunnlag1);
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = personopplysningRepository.hentPersonopplysningerPåId(grunnlag2);

        PersonopplysningGrunnlagDiff poDiff = new PersonopplysningGrunnlagDiff(ref.getAktørId(), personopplysningGrunnlag1, personopplysningGrunnlag2);
        boolean forelderErDødEndret = poDiff.erForeldreDødsdatoEndret();
        boolean barnetsDødsdatoEndret = poDiff.erBarnDødsdatoEndret();

        if (forelderErDødEndret || barnetsDødsdatoEndret) {
            log.info("Setter behandlingårsak til opplysning om død, grunnlagid1: {}, grunnlagid2: {}", grunnlag1, grunnlag2); //$NON-NLS-1
            return Set.copyOf(Collections.singletonList(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD));
        }
        log.info("Setter behandlingårsak til registeropplysning, grunnlagid1: {}, grunnlagid2: {}", grunnlag1, grunnlag2); //$NON-NLS-1
        return Set.copyOf(Collections.singletonList(BehandlingÅrsakType.RE_REGISTEROPPLYSNING));
    }
}
