package no.nav.ung.sak.domene.registerinnhenting.impl.behandlingårsak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@FagsakYtelseTypeRef(OMSORGSPENGER)
@FagsakYtelseTypeRef(FRISINN)
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
