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
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.mottak.inntektsmelding.InntektsmeldingPersistererTjeneste;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.vedtak.konfig.KonfigVerdi;

@Dependent
public class MottatteDokumentTjeneste {

    private Period fristForInnsendingAvDokumentasjon;

    private InntektsmeldingPersistererTjeneste dokumentPersistererTjeneste;
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
                                    InntektsmeldingPersistererTjeneste dokumentPersistererTjeneste,
                                    MottatteDokumentRepository mottatteDokumentRepository,
                                    VilkårResultatRepository vilkårResultatRepository,
                                    UttakRepository uttakRepository,
                                    BehandlingRepositoryProvider behandlingRepositoryProvider) {
        this.fristForInnsendingAvDokumentasjon = fristForInnsendingAvDokumentasjon;
        this.dokumentPersistererTjeneste = dokumentPersistererTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.uttakRepository = uttakRepository;
        this.behandlingRepositoryProvider = behandlingRepositoryProvider;
    }

    public void persisterInntektsmelding(Behandling behandling, MottattDokument dokument) {
        oppdaterMottattDokumentMedBehandling(dokument, behandling.getId());
        if (dokument.harPayload()) {
            dokumentPersistererTjeneste.leggInntektsmeldingTilBehandling(behandling, dokument);
        }
    }

    Long lagreMottattDokumentPåFagsak(MottattDokument dokument) {
        MottattDokument mottattDokument = mottatteDokumentRepository.lagre(dokument);
        return mottattDokument.getId();
    }

    private void oppdaterMottattDokumentMedBehandling(MottattDokument mottattDokument, Long behandlingId) {
        mottatteDokumentRepository.oppdaterMedBehandling(mottattDokument, behandlingId);
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
