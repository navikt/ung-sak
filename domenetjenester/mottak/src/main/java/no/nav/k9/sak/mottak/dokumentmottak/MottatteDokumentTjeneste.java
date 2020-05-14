package no.nav.k9.sak.mottak.dokumentmottak;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.mottak.inntektsmelding.InntektsmeldingParser;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.vedtak.konfig.KonfigVerdi;

@Dependent
public class MottatteDokumentTjeneste {

    private Period fristForInnsendingAvDokumentasjon;

    private InntektsmeldingParser inntektsmeldingParser;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    private VilkårResultatRepository vilkårResultatRepository;
    private UttakRepository uttakRepository;

    protected MottatteDokumentTjeneste() {
        // for CDI proxy
    }

    /**
     * 
     * @param fristForInnsendingAvDokumentasjon - Frist i uker fom siste vedtaksdato
     */
    @Inject
    public MottatteDokumentTjeneste(@KonfigVerdi(value = "sak.frist.innsending.dok", defaultVerdi = "P6W") Period fristForInnsendingAvDokumentasjon,
                                    InntektsmeldingParser inntektsmeldingParser,
                                    InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                    MottatteDokumentRepository mottatteDokumentRepository,
                                    VilkårResultatRepository vilkårResultatRepository,
                                    UttakRepository uttakRepository,
                                    BehandlingRepositoryProvider behandlingRepositoryProvider) {
        this.fristForInnsendingAvDokumentasjon = fristForInnsendingAvDokumentasjon;
        this.inntektsmeldingParser = inntektsmeldingParser;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.uttakRepository = uttakRepository;
        this.behandlingRepositoryProvider = behandlingRepositoryProvider;
    }

    public void persisterInntektsmelding(Behandling behandling, MottattDokument dokument) {
        if (dokument.harPayload()) {
            var inntektsmeldinger = inntektsmeldingParser.parseInntektsmeldinger(behandling, dokument);
            // sendte bare ett dokument her, så forventer kun et svar:
            var arbeidsgiver = inntektsmeldinger.get(0).getArbeidsgiver(); // NOSONAR
            dokument.setArbeidsgiver(arbeidsgiver.getIdentifikator());
            dokument.setBehandlingId(behandling.getId());
            mottatteDokumentRepository.lagre(dokument);// oppdaterer

            // gjør etter alle andre lagringer i db da dette medfører remote kall til abakus (bør egentlig flyttes til egen task)
            inntektsmeldingTjeneste.lagreInntektsmeldinger(behandling.getFagsak().getSaksnummer(), behandling.getId(), inntektsmeldinger);

        }
    }

    Long lagreMottattDokumentPåFagsak(MottattDokument dokument) {
        MottattDokument mottattDokument = mottatteDokumentRepository.lagre(dokument);
        return mottattDokument.getId();
    }

    Optional<MottattDokument> hentMottattDokument(Long mottattDokumentId) {
        return mottatteDokumentRepository.hentMottattDokument(mottattDokumentId);
    }

    boolean erSisteYtelsesbehandlingAvslåttPgaManglendeDokumentasjon(Fagsak sak) {
        Objects.requireNonNull(sak, "Fagsak");
        Optional<Behandling> behandling = behandlingRepositoryProvider.getBehandlingRepository().finnSisteAvsluttedeIkkeHenlagteBehandling(sak.getId());
        return behandling.map(this::erAvsluttetPgaManglendeDokumentasjon).orElse(Boolean.FALSE);
    }

    /**
     * Beregnes fra vedtaksdato
     */
    boolean harFristForInnsendingAvDokGåttUt(Fagsak sak) {
        Objects.requireNonNull(sak, "Fagsak");
        Optional<Behandling> behandlingOptional = behandlingRepositoryProvider.getBehandlingRepository().finnSisteAvsluttedeIkkeHenlagteBehandling(sak.getId());
        return behandlingOptional.flatMap(b -> behandlingRepositoryProvider.getBehandlingVedtakRepository().hentBehandlingVedtakForBehandlingId(b.getId()))
            .map(BehandlingVedtak::getVedtaksdato)
            .map(dato -> dato.isBefore(LocalDate.now().minus(fristForInnsendingAvDokumentasjon))).orElse(Boolean.FALSE);
    }

    private boolean erAvsluttetPgaManglendeDokumentasjon(Behandling behandling) {
        Objects.requireNonNull(behandling, "Behandling");
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandling.getId());
        var vilkår = vilkårResultatRepository.hentHvisEksisterer(behandling.getId());
        if (søknadsperioder.isPresent() && vilkår.isPresent()) {
            var v = vilkår.get();
            var maksPeriode = søknadsperioder.get().getMaksPeriode();
            var vt = v.getVilkårTimeline(VilkårType.SØKERSOPPLYSNINGSPLIKT, maksPeriode.getFomDato(), maksPeriode.getTomDato());
            return !vt.filterValue(p -> Objects.equals(p.getAvslagsårsak(), Avslagsårsak.MANGLENDE_DOKUMENTASJON)).isEmpty();
        } else {
            return false;
        }
    }

}
