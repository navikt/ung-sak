package no.nav.k9.sak.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;

@ApplicationScoped
@GrunnlagRef("MedlemskapAggregat")
@FagsakYtelseTypeRef("PSB")
@FagsakYtelseTypeRef("PPN")
@FagsakYtelseTypeRef("OMP")
@FagsakYtelseTypeRef("FRISINN")
class BehandlingÅrsakUtlederMedlemskap implements BehandlingÅrsakUtleder {
    private static final Logger log = LoggerFactory.getLogger(BehandlingÅrsakUtlederMedlemskap.class);

    @Inject
    public BehandlingÅrsakUtlederMedlemskap() {
        //For CDI
    }

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        long grunnlag1 = (long) grunnlagId1;
        long grunnlag2 = (long) grunnlagId2;
        log.info("Setter behandlingårsak til registeropplysning, har endring i Medlemskap, grunnlagid1: {}, grunnlagid2: {}", grunnlag1, grunnlag2); //$NON-NLS-1
        return java.util.Collections.unmodifiableSet(new HashSet<>(Collections.singletonList(BehandlingÅrsakType.RE_REGISTEROPPLYSNING)));
    }
}
