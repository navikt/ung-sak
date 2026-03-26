package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.Aktivitetspenger;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.Bosteder;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.mottak.dokumentmottak.SøknadParser;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Dependent
public class ForutgåendeMedlemskapTjeneste {

    private final MottatteDokumentRepository mottatteDokumentRepository;
    private final SøknadParser søknadParser;


    @Inject
    public ForutgåendeMedlemskapTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                         SøknadParser søknadParser) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.søknadParser = søknadParser;
    }

    public Optional<Bosteder> utledForutgåendeBosteder(Long fagsakId, Long behandlingId) {
        return hentNyesteSøknad(fagsakId, behandlingId)
            .map(søknad -> (Aktivitetspenger) søknad.getYtelse())
            .map(Aktivitetspenger::getForutgåendeBosteder);
    }

    private Optional<Søknad> hentNyesteSøknad(Long fagsakId, Long behandlingId) {
        return mottatteDokumentRepository.hentMottatteDokumentForBehandling(fagsakId, behandlingId, List.of(Brevkode.AKTIVITETSPENGER_SOKNAD), false, DokumentStatus.GYLDIG)
            .stream()
            .sorted(Comparator.comparing(MottattDokument::getMottattTidspunkt).reversed())
            .collect(Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .findFirst()
            .map(søknadParser::parseSøknad);
    }
}
