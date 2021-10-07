package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
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
    private boolean skalBrukeUtledetEndringsperiode;

    PSBSøknadDokumentValidator() {
        // CDI
    }

    @Inject
    public PSBSøknadDokumentValidator(SøknadParser søknadParser,
                                      SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                      BehandlingRepository behandlingRepository,
                                      @KonfigVerdi(value = "ENABLE_UTLEDET_ENDRINGSPERIODE", defaultVerdi = "false") boolean skalBrukeUtledetEndringsperiode) {
        this.søknadParser = søknadParser;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.skalBrukeUtledetEndringsperiode = skalBrukeUtledetEndringsperiode;
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

        if (skalBrukeUtledetEndringsperiode) {
            // Kan ikke hente behandlingId fra mottatt dokument siden dokumentet ikke er knyttet til behandlingen enda
            // Det skjer først når det er gyldig
            var endringsperioder = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(mottattDokument.getFagsakId())
                .map(this::utledEndrignsperioder)
                .orElse(List.of());

            if (!endringsperioder.isEmpty()) {
                log.info("Fant [{}] som gyldige endringsperioder ", endringsperioder);
            }

            new PleiepengerSyktBarnSøknadValidator().forsikreValidert(søknad, endringsperioder);
        } else {
            new PleiepengerSyktBarnSøknadValidator().forsikreValidert(søknad);
        }
    }

    private List<Periode> utledEndrignsperioder(Behandling behandling) {
        var endringsperioder = søknadsperiodeTjeneste.utledFullstendigPeriode(behandling.getId());
        return endringsperioder
            .stream()
            .map(d -> new Periode(d.getFomDato(), d.getTomDato()))
            .toList();
    }
}
