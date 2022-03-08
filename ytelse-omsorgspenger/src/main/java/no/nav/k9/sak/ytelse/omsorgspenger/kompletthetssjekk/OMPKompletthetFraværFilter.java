package no.nav.k9.sak.ytelse.omsorgspenger.kompletthetssjekk;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.KompletthetFraværFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.TrekkUtFraværTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OMPKompletthetFraværFilter implements KompletthetFraværFilter {

    private BehandlingRepository behandlingRepository;
    private TrekkUtFraværTjeneste trekkUtFraværTjeneste;

    OMPKompletthetFraværFilter() {
        // CDI
    }

    @Inject
    public OMPKompletthetFraværFilter(BehandlingRepository behandlingRepository, TrekkUtFraværTjeneste trekkUtFraværTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.trekkUtFraværTjeneste = trekkUtFraværTjeneste;
    }

    @Override
    public boolean harFraværFraArbeidetIPerioden(BehandlingReferanse ref,
                                                 DatoIntervallEntitet vilkårsperiode,
                                                 ManglendeVedlegg manglendeVedlegg) {
        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        var fraværFraSøknad = trekkUtFraværTjeneste.fraværFraKravDokumenterPåFagsakMedSøknadsfristVurdering(behandling).stream()
            .filter(it -> it.getKravDokumentType() == KravDokumentType.SØKNAD)
            .map(it -> it.getPeriode())
            .toList();

        return harFraværFraArbeidsgiverIPerioden(fraværFraSøknad, vilkårsperiode, manglendeVedlegg);
    }

    private boolean harFraværFraArbeidsgiverIPerioden(List<OppgittFraværPeriode> fraværPerioder, DatoIntervallEntitet vilkårsperiode, ManglendeVedlegg at) {
        return fraværPerioder.stream()
            .filter(fp -> UttakArbeidType.ARBEIDSTAKER.equals(fp.getAktivitetType()))
            .filter(fp -> fp.getPeriode().overlapper(vilkårsperiode))
            .anyMatch(fp -> Objects.equals(at.getArbeidsgiver(), fp.getArbeidsgiver())
                && (fp.getFraværPerDag() == null || !fp.getFraværPerDag().isZero()));
    }

}
