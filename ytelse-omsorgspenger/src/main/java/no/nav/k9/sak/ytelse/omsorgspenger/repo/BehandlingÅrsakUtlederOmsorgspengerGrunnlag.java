package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.registerinnhenting.GrunnlagRef;
import no.nav.k9.sak.domene.registerinnhenting.impl.behandlingårsak.BehandlingÅrsakUtleder;

@ApplicationScoped
@GrunnlagRef(OmsorgspengerGrunnlag.class)
@FagsakYtelseTypeRef(OMSORGSPENGER)
public class BehandlingÅrsakUtlederOmsorgspengerGrunnlag implements BehandlingÅrsakUtleder {

    private OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository;

    BehandlingÅrsakUtlederOmsorgspengerGrunnlag() {
        //for CDI proxy
    }

    @Inject
    public BehandlingÅrsakUtlederOmsorgspengerGrunnlag(OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository) {
        this.omsorgspengerGrunnlagRepository = omsorgspengerGrunnlagRepository;
    }

    @Override
    public Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        Set<BehandlingÅrsakType> resultat = EnumSet.noneOf(BehandlingÅrsakType.class);

        Long grunnlag1 = (Long) grunnlagId1;
        Long grunnlag2 = (Long) grunnlagId2;

        Optional<OmsorgspengerGrunnlag> omsorgspengerGrunnlag1 = omsorgspengerGrunnlagRepository.hentGrunnlagBasertPåId(grunnlag1);
        Optional<OmsorgspengerGrunnlag> omsorgspengerGrunnlag2 = omsorgspengerGrunnlagRepository.hentGrunnlagBasertPåId(grunnlag2);

        Set<OppgittFraværPeriode> fraværFraSøknad1 = hentFraværFraSøknad(omsorgspengerGrunnlag1);
        Set<OppgittFraværPeriode> fraværFraSøknad2 = hentFraværFraSøknad(omsorgspengerGrunnlag2);
        if (!fraværFraSøknad1.equals(fraværFraSøknad2)) {
            //ikke presist, vi kommer hit både ved endinger fra bruker, og hvis saksbehandler punsjer på vegne av bruker
            resultat.add(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        }
        Set<OppgittFraværPeriode> korrigeringIm1 = hentKorrigeringIm(omsorgspengerGrunnlag1);
        Set<OppgittFraværPeriode> korrigeringIm2 = hentKorrigeringIm(omsorgspengerGrunnlag2);
        if (!korrigeringIm1.equals(korrigeringIm2)) {
            resultat.add(BehandlingÅrsakType.RE_FRAVÆRSKORRIGERING_FRA_SAKSBEHANDLER);
        }
        return resultat;
    }

    private Set<OppgittFraværPeriode> hentFraværFraSøknad(Optional<OmsorgspengerGrunnlag> grunnlag) {
        if (grunnlag.isEmpty()) {
            return Set.of();
        }
        OppgittFravær fraværFraSøknad = grunnlag.get().getOppgittFraværFraSøknad();
        if (fraværFraSøknad == null) {
            return Set.of();
        }
        return fraværFraSøknad.getPerioder();
    }

    private Set<OppgittFraværPeriode> hentKorrigeringIm(Optional<OmsorgspengerGrunnlag> grunnlag) {
        if (grunnlag.isEmpty()) {
            return Set.of();
        }
        OppgittFravær korrigeringIm = grunnlag.get().getOppgittFraværFraKorrigeringIm();
        if (korrigeringIm == null) {
            return Set.of();
        }
        return korrigeringIm.getPerioder();
    }


}
