package no.nav.k9.sak.mottak.dokumentmottak;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.dokument.Brevkode;

@ApplicationScoped
public class DokumentValidatorProvider {

    private Instance<DokumentValidator> dokumentValidatorerer;

    DokumentValidatorProvider() {
        //for CDI proxy
    }

    @Inject
    public DokumentValidatorProvider(@Any Instance<DokumentValidator> dokumentValidatorerer) {
        this.dokumentValidatorerer = dokumentValidatorerer;
    }

    public DokumentValidator finnValidator(Brevkode brevkode) {
        Instance<DokumentValidator> selected = dokumentValidatorerer.select(new DokumentGruppeRef.DokumentGruppeRefLiteral(brevkode.getKode()));
        if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Har ingen " + DokumentValidator.class.getSimpleName() + " for brevkode=" + brevkode);
        }
        if (selected.isAmbiguous()) {
            throw new IllegalArgumentException("Klarte ikke finne unik " + DokumentValidator.class.getSimpleName() + " for brevkode=" + brevkode + ", har følgende kandidater: " + selected);
        }
        return selected.get();
    }
}
