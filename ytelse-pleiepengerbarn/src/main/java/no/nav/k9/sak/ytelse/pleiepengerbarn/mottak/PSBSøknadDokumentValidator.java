package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValidator;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarnSøknadValidator;

@ApplicationScoped
@DokumentGruppeRef(Brevkode.PLEIEPENGER_BARN_SOKNAD_KODE)
public class PSBSøknadDokumentValidator implements DokumentValidator {

    private SøknadParser søknadParser;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;

    PSBSøknadDokumentValidator() {
        // CDI
    }

    @Inject
    public PSBSøknadDokumentValidator(SøknadParser søknadParser, SøknadsperiodeTjeneste søknadsperiodeTjeneste) {
        this.søknadParser = søknadParser;
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
        if (!Objects.equals(Brevkode.PLEIEPENGER_BARN_SOKNAD, mottattDokument.getType())) {
            throw new IllegalArgumentException("Forventet brevkode: " + Brevkode.PLEIEPENGER_BARN_SOKNAD + ", fikk: " + mottattDokument.getType());
        }
        var kravperioder = søknadsperiodeTjeneste.hentKravperioder(mottattDokument.getFagsakId(), mottattDokument.getBehandlingId());
        var søknad = søknadParser.parseSøknad(mottattDokument);

        (new PleiepengerSyktBarnSøknadValidator()).forsikreValidert(søknad, tilPerioder(kravperioder));
    }

    private List<Periode> tilPerioder(List<SøknadsperiodeTjeneste.Kravperiode> kravperioder) {
        return kravperioder.stream().map(this::tilPeriode).collect(Collectors.toList());
    }

    private Periode tilPeriode(SøknadsperiodeTjeneste.Kravperiode kravperiode) {
        return new Periode(kravperiode.getPeriode().getFomDato(), kravperiode.getPeriode().getTomDato());
    }
}
