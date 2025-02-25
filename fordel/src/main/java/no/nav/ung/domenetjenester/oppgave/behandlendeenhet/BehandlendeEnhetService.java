package no.nav.ung.domenetjenester.oppgave.behandlendeenhet;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.produksjonsstyring.OmrådeTema;
import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.person.Diskresjonskode;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.typer.AktørId;

@Dependent
public class BehandlendeEnhetService {

    private PersoninfoAdapter personinfoAdapter;
    private no.nav.ung.sak.produksjonsstyring.behandlingenhet.EnhetsTjeneste enhetsTjeneste;

    @Inject
    public BehandlendeEnhetService(PersoninfoAdapter personinfoAdapter, no.nav.ung.sak.produksjonsstyring.behandlingenhet.EnhetsTjeneste enhetsTjeneste) {
        this.personinfoAdapter = personinfoAdapter;
        this.enhetsTjeneste = enhetsTjeneste;
    }

    public BehandlendeEnhet hentBehandlendeEnhet(OmrådeTema tema, BehandlingTema behandlingTema, AktørId hovedAktør) {
        GeografiskTilknytning gjeldendeGeografiskTilknytning = hentGjeldendeGeografiskeTilknytning(
            hovedAktør
        );
        return finnBehandledeEnhet(gjeldendeGeografiskTilknytning, tema, behandlingTema);
    }

    BehandlendeEnhet finnBehandledeEnhet(GeografiskTilknytning geografiskTilknytning, OmrådeTema tema, BehandlingTema behandlingTema) {

        List<OrganisasjonsEnhet> aktiveEnheter = enhetsTjeneste.hentFordelingEnhetId(tema, behandlingTema, geografiskTilknytning);

        if (aktiveEnheter.isEmpty()) {
            throw new IllegalStateException("Forventet å få minst en behandlende enhet: tema=" + tema);
        }
        if (aktiveEnheter.size() != 1) {
            throw new IllegalStateException(String.format("Forventet å få én behandlende enhet for tema %s, fikk %s.", tema, aktiveEnheter.size()));
        }
        OrganisasjonsEnhet organisasjonsenhet = aktiveEnheter.get(0);
        return new BehandlendeEnhet(organisasjonsenhet.getEnhetId(), organisasjonsenhet.getEnhetNavn());
    }

    private GeografiskTilknytning hentGjeldendeGeografiskeTilknytning(AktørId hovedAktør) {
        final GeografiskTilknytning geografiskTilknytningHovedAktør;
        if (hovedAktør != null) {
            geografiskTilknytningHovedAktør = hentGeografiskTilknytning(hovedAktør);
            if (Diskresjonskode.KODE6.equals(geografiskTilknytningHovedAktør.getDiskresjonskode())) {
                return geografiskTilknytningHovedAktør;
            }
        } else {
            geografiskTilknytningHovedAktør = new GeografiskTilknytning(null, null);
        }


        return geografiskTilknytningHovedAktør;
    }


    private GeografiskTilknytning hentGeografiskTilknytning(AktørId aktørId) {
        var ident = personinfoAdapter.hentIdentForAktørId(aktørId);
        return ident
            .map(fnr -> personinfoAdapter.hentGeografiskTilknytning(fnr))
            .orElse(null);
    }

}
