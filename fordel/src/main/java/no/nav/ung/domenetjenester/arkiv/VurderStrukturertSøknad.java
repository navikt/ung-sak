package no.nav.ung.domenetjenester.arkiv;


import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.ung.v1.UngdomsytelseSøknadValidator;
import no.nav.ung.domenetjenester.sak.FinnEllerOpprettUngSakTask;
import no.nav.ung.fordel.handler.MottattMelding;

class VurderStrukturertSøknad {

    MottattMelding håndtertSøknad(MottattMelding dataWrapper, String payload) {
        return håndterStrukturertJsonDokument(dataWrapper, payload);
    }

    private MottattMelding håndterStrukturertJsonDokument(MottattMelding dataWrapper, String payload) {
        // kun ny søknadsformat per nå
        var søknad = JsonUtils.fromString(payload, Søknad.class);
        var ytelse = søknad.getYtelse();
        validerSøknad(søknad);

        Periode søknadsperiode = ytelse.getSøknadsperiode();
        if (søknadsperiode != null) {
            dataWrapper.setFørsteUttaksdag(søknadsperiode.getFraOgMed());
            dataWrapper.setSisteUttaksdag(søknadsperiode.getTilOgMed());
        } else {
            dataWrapper.setFørsteUttaksdag(søknad.getMottattDato().toLocalDate());
        }
        return dataWrapper.nesteSteg(FinnEllerOpprettUngSakTask.TASKTYPE);
    }

    private void validerSøknad(Søknad søknad) {
        new UngdomsytelseSøknadValidator().forsikreValidert(søknad);
    }


}
