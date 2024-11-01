package no.nav.k9.sak.ytelse.ung.søknadsfrist;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.ung.søknadsperioder.UngdomsytelseSøknadsperiode;


@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class UngdomsytelseVurdererSøknadsfristTjeneste implements VurderSøknadsfristTjeneste<UngdomsytelseSøknadsperiode> {

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
    public Map<KravDokument, List<VurdertSøktPeriode<UngdomsytelseSøknadsperiode>>> vurderSøknadsfrist(BehandlingReferanse referanse) {
        var søktePerioder = hentPerioderTilVurdering(referanse);

        return vurderSøknadsfrist(referanse.getBehandlingId(), søktePerioder);
    }

    @Override
    public Map<KravDokument, List<SøktPeriode<UngdomsytelseSøknadsperiode>>> hentPerioderTilVurdering(BehandlingReferanse referanse) {
        var result = new HashMap<KravDokument, List<SøktPeriode<Søknadsperiode>>>();
        return result;
    }


    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<UngdomsytelseSøknadsperiode>>> vurderSøknadsfrist(Long behandlingId, Map<KravDokument, List<SøktPeriode<UngdomsytelseSøknadsperiode>>> søknaderMedPerioder) {
        var result = new HashMap<KravDokument, List<VurdertSøktPeriode<UngdomsytelseSøknadsperiode>>>();
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
