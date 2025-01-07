package no.nav.ung.sak.søknadsfrist;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.VurdertSøktPeriode;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;


@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class UngdomsytelseVurdererSøknadsfristTjeneste implements VurderSøknadsfristTjeneste<UngdomsytelseSøktStartdato> {

    // TODO: Denne må implementeres

    private MottatteDokumentRepository mottatteDokumentRepository;

    UngdomsytelseVurdererSøknadsfristTjeneste() {
        // CDI
    }

    @Inject
    public UngdomsytelseVurdererSøknadsfristTjeneste(MottatteDokumentRepository mottatteDokumentRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
    }


    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<UngdomsytelseSøktStartdato>>> vurderSøknadsfrist(BehandlingReferanse referanse) {
        var søktePerioder = hentPerioderTilVurdering(referanse);

        return vurderSøknadsfrist(referanse.getBehandlingId(), søktePerioder);
    }

    @Override
    public Map<KravDokument, List<SøktPeriode<UngdomsytelseSøktStartdato>>> hentPerioderTilVurdering(BehandlingReferanse referanse) {
        var result = new HashMap<KravDokument, List<SøktPeriode<UngdomsytelseSøktStartdato>>>();
        return result;
    }


    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<UngdomsytelseSøktStartdato>>> vurderSøknadsfrist(Long behandlingId, Map<KravDokument, List<SøktPeriode<UngdomsytelseSøktStartdato>>> søknaderMedPerioder) {
        var result = new HashMap<KravDokument, List<VurdertSøktPeriode<UngdomsytelseSøktStartdato>>>();
        return result;
    }

    @Override
    public Set<KravDokument> relevanteKravdokumentForBehandling(BehandlingReferanse referanse, boolean taHensynTilManuellRevurdering) {
        return mottatteDokumentRepository.hentMottatteDokumentForBehandling(referanse.getFagsakId(), referanse.getBehandlingId(), List.of(Brevkode.UNGDOMSYTELSE_SOKNAD), false, DokumentStatus.GYLDIG)
            .stream()
            .map(it -> new KravDokument(it.getJournalpostId(), it.getInnsendingstidspunkt(), KravDokumentType.SØKNAD, it.getKildesystem()))
            .collect(Collectors.toSet());
    }

}
