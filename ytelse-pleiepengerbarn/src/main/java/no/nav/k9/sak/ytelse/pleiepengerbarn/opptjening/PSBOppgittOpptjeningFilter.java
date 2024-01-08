package no.nav.k9.sak.ytelse.pleiepengerbarn.opptjening;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningAggregat;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.MapTilBrevkode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.Versjon;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
public class PSBOppgittOpptjeningFilter implements OppgittOpptjeningFilter {

    private VilkårTjeneste vilkårTjeneste;
    private BehandlingRepository behandlingRepository;
    private Instance<VurderSøknadsfristTjeneste<Søknadsperiode>> søknadsfristTjenester;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private Instance<MapTilBrevkode> brevkodeMappere;

    private boolean enableOppgittOpptjeningEndringssøknadPSB;

    PSBOppgittOpptjeningFilter() {
        // For CDI
    }

    @Inject
    public PSBOppgittOpptjeningFilter(VilkårTjeneste vilkårTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      @Any Instance<VurderSøknadsfristTjeneste<Søknadsperiode>> søknadsfristTjenester,
                                      MottatteDokumentRepository mottatteDokumentRepository,
                                      @Any Instance<MapTilBrevkode> brevkodeMappere,
                                      @KonfigVerdi(value = "ENABLE_OPPGITT_OPPTJENING_ENDRINGSSØKNAD_PSB", defaultVerdi = "false") boolean enableOppgittOpptjeningEndringssøknadPSB) {
        this.vilkårTjeneste = vilkårTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjenester = søknadsfristTjenester;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.brevkodeMappere = brevkodeMappere;
        this.enableOppgittOpptjeningEndringssøknadPSB = enableOppgittOpptjeningEndringssøknadPSB;
    }

    /**
     * Henter sist mottatte oppgitte opptjening som kan knyttes til vilkårsperiode (vilkårsperiode slås opp gjennom stp)
     */
    @Override
    public Optional<OppgittOpptjening> hentOppgittOpptjening(Long behandlingId, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate stp) {
        var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(behandlingId));
        var vilkårsperiode = finnVilkårsperiodeForOpptjening(ref, stp);
        return hentOppgittOpptjening(behandlingId, iayGrunnlag, vilkårsperiode);
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

        Brevkode brevkode = MapTilBrevkode.finnBrevkodeMapper(brevkodeMappere, behandlingRepository.hentBehandling(ref.getBehandlingId()).getFagsakYtelseType()).getBrevkode();
        Set<MottattDokument> mottatteDokumenter = mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(ref.getFagsakId())
            .stream()
            .filter(it -> brevkode.equals(it.getType()))
            .collect(Collectors.toSet());

        return finnOppgittOpptjening(iayGrunnlag, vilkårsperiode, kravdokMedFravær, mottatteDokumenter);
    }

    Optional<OppgittOpptjening> finnOppgittOpptjening(InntektArbeidYtelseGrunnlag iayGrunnlag, DatoIntervallEntitet vilkårsperiode,
                                                      Map<KravDokument, List<SøktPeriode<Søknadsperiode>>> kravDokumenterMedFravær, Set<MottattDokument> mottatteDokumenter) {
        LocalDate stp = vilkårsperiode.getFomDato();
        var oppgitteOpptjeninger = iayGrunnlag.getOppgittOpptjeningAggregat()
            .map(OppgittOpptjeningAggregat::getOppgitteOpptjeninger)
            .orElse(List.of());
        var sistMottatteSøknadNærmestStp = kravDokumenterMedFravær.entrySet()
            .stream()
            .filter(it -> !it.getValue().isEmpty())
            .filter(it -> oppgitteOpptjeninger.stream().anyMatch(jp -> jp.getJournalpostId().equals(it.getKey().getJournalpostId())))
            .filter(it -> brukOppgittOpptjening(it.getKey().getJournalpostId(), mottatteDokumenter))
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
        LocalDate næresteDatoFraEksisterende = finnDatoNærmestSkjæringstidspunktet(søktePerioder, skjæringstidspunkt);
        return Math.abs(ChronoUnit.DAYS.between(skjæringstidspunkt, næresteDatoFraEksisterende));
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

    private boolean brukOppgittOpptjening(JournalpostId journalpostId, Set<MottattDokument> mottatteDokumenter) {
        final MottattDokument mottattDokument = mottatteDokumenter.stream().filter(it -> journalpostId.equals(it.getJournalpostId())).findFirst().orElseThrow();
        final Søknad søknad = JsonUtils.fromString(mottattDokument.getPayload(), Søknad.class);
        if (enableOppgittOpptjeningEndringssøknadPSB && Objects.equals(søknad.getYtelse().getType().kode(), "PLEIEPENGER_SYKT_BARN") && søknad.getVersjon().compareTo(Versjon.of("1.0.1")) >= 0) {
            // Dersom søknaden ikke har søknadsperiode betyr det at den enten er fra endringsdialogen eller at den er punsjet uten.
            // I disse tilfellene skal oppgitt opptjening fra tidligere søknad fortsatt gjelde.
            PleiepengerSyktBarn psbSøknad = søknad.getYtelse();
            return psbSøknad.getSøknadsperiodeList() != null && !psbSøknad.getSøknadsperiodeList().isEmpty();
        }
        return true;
    }

    private DatoIntervallEntitet finnVilkårsperiodeForOpptjening(BehandlingReferanse ref, LocalDate stp) {
        var periodeTilVurdering = vilkårTjeneste.utledPerioderTilVurdering(ref, VilkårType.OPPTJENINGSVILKÅRET)
            .stream()
            .filter(di -> di.getFomDato().equals(stp))
            .findFirst();
        if (periodeTilVurdering.isPresent()) {
            return periodeTilVurdering.get();
        }

        // Ingen match for stp mot perioder under vurdering -> må da forvente at den matcher vilkårsperiode som er ferdigvurdert
        var vilkår = vilkårTjeneste.hentVilkårResultat(ref.getBehandlingId()).getVilkår(VilkårType.OPPTJENINGSVILKÅRET).orElseThrow();
        var periodeFerdigvurdert = vilkår.finnPeriodeForSkjæringstidspunkt(stp);
        if (periodeFerdigvurdert.getGjeldendeUtfall().equals(Utfall.IKKE_VURDERT)) {
            throw new IllegalStateException("Forventer at vilkårsperiode som matchet opptjening var ferdigvurdert");
        }
        return periodeFerdigvurdert.getPeriode();
    }


}
