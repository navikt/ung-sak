package no.nav.foreldrepenger.dokumentbestiller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.dokumentbestiller.dto.BrevmalDto;
import no.nav.foreldrepenger.dokumentbestiller.klient.FormidlingRestKlient;
import no.nav.foreldrepenger.kontrakter.formidling.v1.BehandlingUuidDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentProdusertDto;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
public class DokumentBehandlingTjeneste {
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private FormidlingRestKlient formidlingRestKlient;

    public DokumentBehandlingTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentBehandlingTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                      BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                      FormidlingRestKlient formidlingRestKlient) {

        Objects.requireNonNull(repositoryProvider, "repositoryProvider");
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.formidlingRestKlient = formidlingRestKlient;
    }

    public List<BrevmalDto> hentBrevmalerFor(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        final List<no.nav.foreldrepenger.kontrakter.formidling.v1.BrevmalDto> brevmalDtos = formidlingRestKlient.hentBrevMaler(new BehandlingUuidDto(behandling.getUuid()));
        List<BrevmalDto> brevmalListe = new ArrayList<>();
        for (no.nav.foreldrepenger.kontrakter.formidling.v1.BrevmalDto brevmalDto : brevmalDtos) {
            brevmalListe.add(new BrevmalDto(brevmalDto.getKode(), brevmalDto.getNavn(), mapDokumentMalRestriksjon(brevmalDto.getRestriksjon().getKode()), brevmalDto.getTilgjengelig()));
        }
        return brevmalListe;
    }

    private DokumentMalRestriksjon mapDokumentMalRestriksjon(String restriksjon) {
        if (DokumentMalRestriksjon.ÅPEN_BEHANDLING.getKode().equals(restriksjon)) {
            return DokumentMalRestriksjon.ÅPEN_BEHANDLING;
        } else if (DokumentMalRestriksjon.ÅPEN_BEHANDLING_IKKE_SENDT.getKode().equals(restriksjon)) {
            return DokumentMalRestriksjon.ÅPEN_BEHANDLING_IKKE_SENDT;
        } else if (DokumentMalRestriksjon.REVURDERING.getKode().equals(restriksjon)) {
            return DokumentMalRestriksjon.REVURDERING;
        } else {
            return DokumentMalRestriksjon.INGEN;
        }
    }

    public boolean erDokumentProdusert(Long behandlingId, DokumentMalType dokumentMalTypeKode) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return formidlingRestKlient.erDokumentProdusert(new DokumentProdusertDto(behandling.getUuid(), dokumentMalTypeKode.getKode()));
    }

    public void settBehandlingPåVent(Long behandlingId, Venteårsak venteårsak) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        behandlingskontrollTjeneste.settBehandlingPåVentUtenSteg(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT,
            LocalDateTime.now().plusDays(14), venteårsak);
    }

    public void utvidBehandlingsfristManuelt(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        oppdaterBehandlingMedNyFrist(behandling, finnNyFristManuelt(behandling));
    }

    void oppdaterBehandlingMedNyFrist(Behandling behandling, LocalDate nyFrist) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandling.setBehandlingstidFrist(nyFrist);
        behandlingRepository.lagre(behandling, lås);
    }

    LocalDate finnNyFristManuelt(Behandling behandling) {
        return FPDateUtil.iDag().plusWeeks(behandling.getType().getBehandlingstidFristUker());
    }

    public void utvidBehandlingsfristManueltMedlemskap(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        oppdaterBehandlingMedNyFrist(behandling, utledFristMedlemskap(behandling));

    }

    LocalDate utledFristMedlemskap(Behandling behandling) {
        LocalDate vanligFrist = finnNyFristManuelt(behandling);
        return vanligFrist;
    }

}
