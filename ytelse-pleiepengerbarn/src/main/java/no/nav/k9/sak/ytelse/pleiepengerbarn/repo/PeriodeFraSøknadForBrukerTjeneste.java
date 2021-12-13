package no.nav.k9.sak.ytelse.pleiepengerbarn.repo;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PSBVurdererSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;

@Dependent
public class PeriodeFraSøknadForBrukerTjeneste {

    private final PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste;
    private final UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;

    @Inject
    public PeriodeFraSøknadForBrukerTjeneste(@FagsakYtelseTypeRef("PSB") @FagsakYtelseTypeRef("PPN") PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste,
                                             UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository) {
        this.søknadsfristTjeneste = søknadsfristTjeneste;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
    }

    /**
     * Henter ut søknadsdata for alle gyldige dokumenter
     *
     * @param referanse behandling referanse
     * @return perioder fra søknad
     */
    public Set<PerioderFraSøknad> hentPerioderFraSøknad(BehandlingReferanse referanse) {
        var kravDokumenter = søknadsfristTjeneste.hentPerioderTilVurdering(referanse).keySet();

        if (kravDokumenter.isEmpty()) {
            return Set.of();
        }

        var uttaksPerioderGrunnlagOpt = uttakPerioderGrunnlagRepository.hentGrunnlag(referanse.getBehandlingId());
        if (uttaksPerioderGrunnlagOpt.isEmpty()) {
            return Set.of();
        }
        var grunnlag = uttaksPerioderGrunnlagOpt.get();

        return grunnlag.getOppgitteSøknadsperioder()
            .getPerioderFraSøknadene()
            .stream()
            .filter(it -> kravDokumenter.stream().anyMatch(krav -> Objects.equals(krav.getJournalpostId(), it.getJournalpostId())))
            .collect(Collectors.toSet());
    }

    /**
     * Henter ut søknadsdata for alle gyldige dokumenter
     *
     * @param referanse behandling referanse
     * @return perioder fra søknad
     */
    public Set<KravDokumentMedData> hentKravDokumentMedPerioderFraSøknad(BehandlingReferanse referanse) {
        var kravDokumenter = søknadsfristTjeneste.hentPerioderTilVurdering(referanse).keySet();

        if (kravDokumenter.isEmpty()) {
            return Set.of();
        }

        var uttaksPerioderGrunnlagOpt = uttakPerioderGrunnlagRepository.hentGrunnlag(referanse.getBehandlingId());
        if (uttaksPerioderGrunnlagOpt.isEmpty()) {
            return Set.of();
        }
        var grunnlag = uttaksPerioderGrunnlagOpt.get();

        return grunnlag.getOppgitteSøknadsperioder()
            .getPerioderFraSøknadene()
            .stream()
            .filter(it -> kravDokumenter.stream().anyMatch(krav -> Objects.equals(krav.getJournalpostId(), it.getJournalpostId())))
            .map(it -> new KravDokumentMedData(finnKravDokument(kravDokumenter, it.getJournalpostId()), it))
            .collect(Collectors.toSet());
    }

    private KravDokument finnKravDokument(Set<KravDokument> kravDokumenter, JournalpostId journalpostId) {
        var relevanteDokumenter = kravDokumenter.stream()
            .filter(it -> Objects.equals(it.getJournalpostId(), journalpostId))
            .collect(Collectors.toSet());

        if (relevanteDokumenter.isEmpty()) {
            throw new IllegalStateException("Har data fra et dokument som ikke er relevant.");
        } else if (relevanteDokumenter.size() > 1) {
            throw new IllegalStateException("Har data som er knyttet til flere kravdokumenter. " + relevanteDokumenter);
        }
        return relevanteDokumenter.iterator().next();
    }
}
