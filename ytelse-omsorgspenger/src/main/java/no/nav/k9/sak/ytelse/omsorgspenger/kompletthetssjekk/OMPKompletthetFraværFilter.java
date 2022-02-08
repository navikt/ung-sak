package no.nav.k9.sak.ytelse.omsorgspenger.kompletthetssjekk;

import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.KompletthetFraværFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OMPKompletthetFraværFilter implements KompletthetFraværFilter {


    private OmsorgspengerGrunnlagRepository grunnlagRepository;

    OMPKompletthetFraværFilter() {
        // CDI
    }

    @Inject
    public OMPKompletthetFraværFilter(OmsorgspengerGrunnlagRepository grunnlagRepository) {
        this.grunnlagRepository = grunnlagRepository;
    }

    @Override
    public boolean harFraværFraArbeidetIPerioden(BehandlingReferanse ref,
                                                 DatoIntervallEntitet vilkårsperiode,
                                                 ManglendeVedlegg manglendeVedlegg) {
        var fraværFraSøknad = grunnlagRepository.hentOppgittFraværFraSøknadHvisEksisterer(ref.getBehandlingId())
            .map(OppgittFravær::getPerioder)
            .orElse(Set.of());

        return harFraværFraArbeidsgiverIPerioden(fraværFraSøknad, vilkårsperiode, manglendeVedlegg);
    }

    private boolean harFraværFraArbeidsgiverIPerioden(Set<OppgittFraværPeriode> fraværPerioder, DatoIntervallEntitet vilkårsperiode, ManglendeVedlegg at) {
        return fraværPerioder.stream()
            .filter(fp -> UttakArbeidType.ARBEIDSTAKER.equals(fp.getAktivitetType()))
            .filter(fp -> fp.getPeriode().overlapper(vilkårsperiode))
            .anyMatch(fp -> Objects.equals(at.getArbeidsgiver(), fp.getArbeidsgiver())
                && (fp.getFraværPerDag() == null || !fp.getFraværPerDag().isZero()));
    }

}
