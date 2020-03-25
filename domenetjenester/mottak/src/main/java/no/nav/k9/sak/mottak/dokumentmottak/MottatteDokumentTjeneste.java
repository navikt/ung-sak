package no.nav.k9.sak.mottak.dokumentmottak;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.dokument.DokumentKategori;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding.InntektsmeldingPersistererTjeneste;
import no.nav.k9.sak.mottak.dokumentpersiterer.inntektsmelding.MottattInntektsmeldingWrapper;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
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

    public void persisterDokumentinnhold(Behandling behandling, MottattDokument dokument, Optional<LocalDate> gjelderFra) {
        oppdaterMottattDokumentMedBehandling(dokument, behandling.getId());
        if (dokument.getPayload() != null) {
            var innhold = dokumentPersistererTjeneste.persisterDokumentinnhold(dokument, behandling, gjelderFra);
            if(!innhold.getOmsorgspengerFravær().isEmpty()) {
                
            }
        }
    }

    public Long lagreMottattDokumentPåFagsak(MottattDokument dokument) {
        MottattDokument mottattDokument = mottatteDokumentRepository.lagre(dokument);
        return mottattDokument.getId();
    }

    public List<MottattDokument> hentMottatteDokument(Long behandlingId) {
        return mottatteDokumentRepository.hentMottatteDokument(behandlingId);
    }

    public List<MottattDokument> hentMottatteDokumentFagsak(Long fagsakId) {
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId);
    }

    public boolean harMottattDokumentKat(Long behandlingId, DokumentKategori dokumentKategori) {
        return hentMottatteDokument(behandlingId).stream().anyMatch(dok -> dokumentKategori.equals(dok.getDokumentKategori()));
    }

    public void oppdaterMottattDokumentMedBehandling(MottattDokument mottattDokument, Long behandlingId) {
        mottatteDokumentRepository.oppdaterMedBehandling(mottattDokument, behandlingId);
    }

    public Optional<MottattDokument> hentMottattDokument(Long mottattDokumentId) {
        return mottatteDokumentRepository.hentMottattDokument(mottattDokumentId);
    }

    public boolean erSisteYtelsesbehandlingAvslåttPgaManglendeDokumentasjon(Fagsak sak) {
        Objects.requireNonNull(sak, "Fagsak");
        Optional<Behandling> behandling = behandlingRepositoryProvider.getBehandlingRepository().finnSisteAvsluttedeIkkeHenlagteBehandling(sak.getId());
        return behandling.map(this::erAvsluttetPgaManglendeDokumentasjon).orElse(Boolean.FALSE);
    }

    /**
     * Beregnes fra vedtaksdato
     */
    public boolean harFristForInnsendingAvDokGåttUt(Fagsak sak) {
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
