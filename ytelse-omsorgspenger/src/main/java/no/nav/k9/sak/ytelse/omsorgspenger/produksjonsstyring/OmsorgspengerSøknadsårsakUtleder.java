package no.nav.k9.sak.ytelse.omsorgspenger.produksjonsstyring;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.SøknadÅrsak;
import no.nav.k9.sak.behandling.hendelse.produksjonsstyring.søknadsårsak.SøknadsårsakUtleder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.OMSORGSPENGER)
public class OmsorgspengerSøknadsårsakUtleder implements SøknadsårsakUtleder {

    private OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository;

    public OmsorgspengerSøknadsårsakUtleder() {
        //for CDI proxy
    }

    @Inject
    public OmsorgspengerSøknadsårsakUtleder(OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository) {
        this.omsorgspengerGrunnlagRepository = omsorgspengerGrunnlagRepository;
    }

    @Override
    public List<SøknadÅrsak> utledSøknadÅrsaker(Behandling behandling) {
        Optional<OppgittFravær> oppgittFravær = omsorgspengerGrunnlagRepository.hentOppgittFraværFraSøknadHvisEksisterer(behandling.getId());
        return oppgittFravær.stream()
            .flatMap(of -> of.getPerioder().stream())
            .map(OppgittFraværPeriode::getSøknadÅrsak)
            .toList();
    }
}
