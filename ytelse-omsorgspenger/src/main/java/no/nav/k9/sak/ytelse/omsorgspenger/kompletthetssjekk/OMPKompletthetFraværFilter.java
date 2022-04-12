package no.nav.k9.sak.ytelse.omsorgspenger.kompletthetssjekk;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.KompletthetFraværFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.AktivitetTypeArbeidsgiver;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.OppgittFraværHolder;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.TrekkUtFraværTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
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
    public boolean harFraværFraArbeidetIPerioden(BehandlingReferanse ref, DatoIntervallEntitet vilkårsperiode, ManglendeVedlegg manglendeVedlegg) {
        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());

        Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> fraværPrAktivitet = trekkUtFraværTjeneste.fraværFraKravDokumenterPåFagsakMedSøknadsfristVurdering(behandling);

        LocalDateTimeline<OppgittFraværHolder> fraværTidslinjeHosArbeidsgiver = fraværPrAktivitet.get(new AktivitetTypeArbeidsgiver(UttakArbeidType.ARBEIDSTAKER, manglendeVedlegg.getArbeidsgiver()));
        return fraværTidslinjeHosArbeidsgiver != null && fraværTidslinjeHosArbeidsgiver.stream()
            .map(LocalDateSegment::getValue)
            .anyMatch(fraværHolder -> fraværHolder.søknadGjelder());
    }

}
