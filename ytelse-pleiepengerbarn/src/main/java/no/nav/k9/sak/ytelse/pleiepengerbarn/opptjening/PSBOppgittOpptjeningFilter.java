package no.nav.k9.sak.ytelse.pleiepengerbarn.opptjening;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningAggregat;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
public class PSBOppgittOpptjeningFilter implements OppgittOpptjeningFilter {

    private VilkårTjeneste vilkårTjeneste;
    private BehandlingRepository behandlingRepository;
    private Instance<VurderSøknadsfristTjeneste<Søknadsperiode>> søknadsfristTjenester;

    PSBOppgittOpptjeningFilter() {
        // For CDI
    }

    @Inject
    public PSBOppgittOpptjeningFilter(VilkårTjeneste vilkårTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      @Any Instance<VurderSøknadsfristTjeneste<Søknadsperiode>> søknadsfristTjenester) {
        this.vilkårTjeneste = vilkårTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjenester = søknadsfristTjenester;
    }

    /**
     * Henter sist mottatte oppgitte opptjening som kan knyttes til vilkårsperiode (vilkårsperiode slås opp gjennom stp)
     */
    @Override
    public Optional<OppgittOpptjening> hentOppgittOpptjening(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate stp) {
        var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(behandlingId));
        var tjeneste = FagsakYtelseTypeRef.Lookup.find(søknadsfristTjenester, ref.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Har ikke " + getClass().getSimpleName() + " for ytelse=" + ref.getFagsakYtelseType()));
        Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravdokMedFravær = tjeneste.hentPerioderTilVurdering(ref);
        return finnOppgittOpptjening(iayGrunnlag, kravdokMedFravær, stp);
    }

    /**
     * Henter sist mottatte oppgitte opptjening som kan knyttes til vilkårsperiode
     */
    @Override
    public Optional<OppgittOpptjening> hentOppgittOpptjening(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode) {
        var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(behandlingId));
        var tjeneste = FagsakYtelseTypeRef.Lookup.find(søknadsfristTjenester, ref.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Har ikke " + getClass().getSimpleName() + " for ytelse=" + ref.getFagsakYtelseType()));
        Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravdokMedFravær = tjeneste.hentPerioderTilVurdering(ref);
        return finnOppgittOpptjening(iayGrunnlag, kravdokMedFravær, vilkårsperiode.getFomDato());
    }

    // Kode for lansering av ny prioriering - START
    Optional<OppgittOpptjening> finnOppgittOpptjening(InntektArbeidYtelseGrunnlag iayGrunnlag, Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravDokumenterMedFravær, LocalDate stp) {
        var oppgitteOpptjeninger = iayGrunnlag.getOppgittOpptjeningAggregat()
            .map(OppgittOpptjeningAggregat::getOppgitteOpptjeninger)
            .orElse(List.of());
        var sistMottatteSøknadNærmestStp = kravDokumenterMedFravær.entrySet()
            .stream()
            .filter(it -> !it.getValue().isEmpty())
            .filter(it -> oppgitteOpptjeninger.stream().anyMatch(jp -> jp.getJournalpostId().equals(it.getKey().getJournalpostId())))
            .min((e1, e2) -> {
                var kravdok1 = e1.getKey();
                var kravdok2 = e2.getKey();
                var søktePerioder1 = e1.getValue();
                var søktePerioder2 = e2.getValue();

                var compareStpDistanse = Long.compare(distanseTilStp(søktePerioder1, stp), distanseTilStp(søktePerioder2, stp));
                if (compareStpDistanse != 0) {
                    return compareStpDistanse;
                }
                return kravdok2.getInnsendingsTidspunkt().compareTo(kravdok1.getInnsendingsTidspunkt());
            });

        return sistMottatteSøknadNærmestStp.flatMap(sistMottatt -> {
            var journalpostId = sistMottatt.getKey().getJournalpostId();
            return oppgitteOpptjeninger.stream()
                .filter(jp -> jp.getJournalpostId().equals(journalpostId))
                .findFirst();
        });

    }

    private long distanseTilStp(List<SøktPeriode<Søknadsperiode>> søktePerioder, LocalDate skjæringstidspunkt) {
        var næresteDatoFraEksisterende = finnDatoNærmestSkjæringstidspunktet(søktePerioder, skjæringstidspunkt);
        long dist = Math.abs(ChronoUnit.DAYS.between(skjæringstidspunkt, næresteDatoFraEksisterende));
        return dist;
    }

    private LocalDate finnDatoNærmestSkjæringstidspunktet(List<SøktPeriode<Søknadsperiode>> søktePerioder, LocalDate stp) {
        var inkludert = søktePerioder.stream()
            .filter(p -> p.getPeriode().inkluderer(stp))
            .findFirst();
        if (inkludert.isPresent()) {
            return stp;
        }
        return søktePerioder.stream()
            .map(it -> it.getPeriode().getFomDato())
            .min(Comparator.comparingLong(x -> Math.abs(ChronoUnit.DAYS.between(stp, x))))
            .orElseThrow();
    }
    // Kode for lansering av prioriering - END


}
