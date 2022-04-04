package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.mottak;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValidator;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.pls.v1.PleiepengerLivetsSluttfaseSøknadValidator;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE_KODE)
public class PPNSøknadDokumentValidator implements DokumentValidator {

    private static final Logger log = LoggerFactory.getLogger(PPNSøknadDokumentValidator.class);

    private SøknadParser søknadParser;
    private BehandlingRepository behandlingRepository;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;

    PPNSøknadDokumentValidator() {
        // CDI
    }

    @Inject
    public PPNSøknadDokumentValidator(BehandlingRepository behandlingRepository,
                                      SøknadParser søknadParser,
                                      SøknadsperiodeTjeneste søknadsperiodeTjeneste) {
        this.søknadParser = søknadParser;
        this.behandlingRepository = behandlingRepository;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
    }

    @Override
    public void validerDokumenter(Long behandlingId, Collection<MottattDokument> mottatteDokumenter) {
        for (MottattDokument mottattDokument : mottatteDokumenter) {
            validerDokument(mottattDokument);
        }
    }

    @Override
    public void validerDokument(MottattDokument mottattDokument) {
        Objects.requireNonNull(mottattDokument);
        if (!Objects.equals(Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE, mottattDokument.getType())) {
            throw new IllegalArgumentException("Forventet brevkode: " + Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE + ", fikk: " + mottattDokument.getType());
        }
        var søknad = søknadParser.parseSøknad(mottattDokument);

        // Kan ikke hente behandlingId fra mottatt dokument siden dokumentet ikke er knyttet til behandlingen enda
        // Det skjer først når dokumentet får status GYLDIG
        var endringsperioder = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(mottattDokument.getFagsakId())
            .map(this::utledEndringsperioder)
            .orElse(List.of());
        if (!endringsperioder.isEmpty()) {
            log.info("Fant [{}] som gyldige endringsperioder ", endringsperioder);
        }

        new PleiepengerLivetsSluttfaseSøknadValidator().forsikreValidert(søknad, endringsperioder);
    }

    private List<Periode> utledEndringsperioder(Behandling behandling) {
        var endringsperioder = søknadsperiodeTjeneste.utledFullstendigPeriode(behandling.getId());
        return endringsperioder
            .stream()
            .map(endringsperiode -> new Periode(endringsperiode.getFomDato(), endringsperiode.getTomDato()))
            .toList();
    }
}
