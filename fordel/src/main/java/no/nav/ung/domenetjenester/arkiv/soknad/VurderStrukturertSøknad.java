package no.nav.ung.domenetjenester.arkiv.soknad;


import no.nav.k9.søknad.JsonUtils;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.aktivitetspenger.v1.Aktivitetspenger;
import no.nav.k9.søknad.ytelse.ung.v1.Ungdomsytelse;
import no.nav.k9.søknad.ytelse.ung.v1.UngdomsytelseSøknadValidator;
import no.nav.ung.domenetjenester.sak.FinnEllerOpprettUngSakTask;
import no.nav.ung.fordel.handler.MottattMelding;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

public class VurderStrukturertSøknad {

    public MottattMelding håndtertSøknad(MottattMelding dataWrapper, String payload) {
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
        dataWrapper.setYtelseType(utledYtelseType(søknad));
        return dataWrapper.nesteSteg(FinnEllerOpprettUngSakTask.TASKTYPE);
    }

    private void validerSøknad(Søknad søknad) {
        switch (søknad.getYtelse()) {
            case Ungdomsytelse _ -> new UngdomsytelseSøknadValidator().forsikreValidert(søknad);
            case Aktivitetspenger _ -> new Object(); //TODO legg til søknadvalidator når opprettet
            default -> throw new IllegalArgumentException("Ikke-støttet ytelse: " + søknad.getYtelse().getClass());
        }
    }

    private FagsakYtelseType utledYtelseType(Søknad søknad) {
        return switch (søknad.getYtelse()) {
            case Ungdomsytelse _ -> FagsakYtelseType.UNGDOMSYTELSE;
            case Aktivitetspenger _ -> FagsakYtelseType.AKTIVITETSPENGER;
            default -> throw new IllegalArgumentException("Ikke-støttet ytelse: " + søknad.getYtelse().getClass());
        };
    }


}
