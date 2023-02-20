package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

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
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarnSøknadValidator;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.PLEIEPENGER_BARN_SOKNAD_KODE)
public class PSBSøknadDokumentValidator implements DokumentValidator {

    private static final Logger log = LoggerFactory.getLogger(PSBSøknadDokumentValidator.class);

    private SøknadParser søknadParser;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private BehandlingRepository behandlingRepository;

    PSBSøknadDokumentValidator() {
        // CDI
    }

    @Inject
    public PSBSøknadDokumentValidator(SøknadParser søknadParser,
                                      SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                      BehandlingRepository behandlingRepository) {
        this.søknadParser = søknadParser;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.behandlingRepository = behandlingRepository;
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
        if (!Objects.equals(Brevkode.PLEIEPENGER_BARN_SOKNAD, mottattDokument.getType())) {
            throw new IllegalArgumentException("Forventet brevkode: " + Brevkode.PLEIEPENGER_BARN_SOKNAD + ", fikk: " + mottattDokument.getType());
        }
        var søknad = søknadParser.parseSøknad(mottattDokument);

        // Kan ikke hente behandlingId fra mottatt dokument siden dokumentet ikke er knyttet til behandlingen enda
        // Det skjer først når det er gyldig
        var endringsperioder = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(mottattDokument.getFagsakId())
            .map(this::utledEndringsperioder)
            .orElse(List.of());

        if (!endringsperioder.isEmpty()) {
            log.info("Fant [{}] som gyldige endringsperioder ", endringsperioder);
        }

        new PleiepengerSyktBarnSøknadValidator().forsikreValidert(søknad, endringsperioder);
    }

    private List<Periode> utledEndringsperioder(Behandling behandling) {
        var endringsperioder = søknadsperiodeTjeneste.utledFullstendigPeriode(behandling.getId());
        return endringsperioder
            .stream()
            .map(d -> new Periode(d.getFomDato(), d.getTomDato()))
            .toList();
    }
}
