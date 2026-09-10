package no.nav.ung.domenetjenester.oppgave.behandlendeenhet;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingTema;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.person.Diskresjonskode;
import no.nav.ung.kodeverk.produksjonsstyring.OmrådeTema;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.typer.AktørId;

import java.util.List;
import java.util.Objects;

@Dependent
public class BehandlendeEnhetService {

    private final PersoninfoAdapter personinfoAdapter;
    private final no.nav.ung.sak.produksjonsstyring.behandlingenhet.EnhetsTjeneste enhetsTjeneste;

    @Inject
    public BehandlendeEnhetService(PersoninfoAdapter personinfoAdapter, no.nav.ung.sak.produksjonsstyring.behandlingenhet.EnhetsTjeneste enhetsTjeneste) {
        this.personinfoAdapter = personinfoAdapter;
        this.enhetsTjeneste = enhetsTjeneste;
    }

    public BehandlendeEnhet hentBehandlendeEnhet(OmrådeTema tema, BehandlingTema behandlingTema, AktørId hovedAktør) {
        Objects.requireNonNull(behandlingTema, "behandlingTema manglet");
        // For UDEFINERT (f.eks. klager uten utledet fagsak/ytelse) gir dette FagsakYtelseType.UDEFINERT,
        // som brukes av BehandlingsnummerMapper til å velge behandlingsnummer for PDL-oppslagene.
        // Når BRUK_PDL_SPESIFIKKE_BEHANDLINGNUMRE er aktivert sendes da behandlingsnummer for alle
        // aktuelle ytelser; med flagget av (dagens produksjonsverdi) brukes uansett kun ett behandlingsnummer.
        FagsakYtelseType ytelseType = behandlingTema.getFagsakYtelseType();
        GeografiskTilknytning gjeldendeGeografiskTilknytning = hentGjeldendeGeografiskeTilknytning(hovedAktør, ytelseType);
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

    private GeografiskTilknytning hentGjeldendeGeografiskeTilknytning(AktørId hovedAktør, FagsakYtelseType ytelseType) {
        final GeografiskTilknytning geografiskTilknytningHovedAktør;
        if (hovedAktør != null) {
            geografiskTilknytningHovedAktør = hentGeografiskTilknytning(hovedAktør, ytelseType);
            if (Diskresjonskode.KODE6.equals(geografiskTilknytningHovedAktør.getDiskresjonskode())) {
                return geografiskTilknytningHovedAktør;
            }
        } else {
            geografiskTilknytningHovedAktør = new GeografiskTilknytning(null, null);
        }


        return geografiskTilknytningHovedAktør;
    }


    private GeografiskTilknytning hentGeografiskTilknytning(AktørId aktørId, FagsakYtelseType ytelseType) {
        return personinfoAdapter.hentIdentForAktørId(aktørId)
            .map(ident -> personinfoAdapter.hentGeografiskTilknytning(ident, ytelseType))
            .orElse(null);
    }

}
