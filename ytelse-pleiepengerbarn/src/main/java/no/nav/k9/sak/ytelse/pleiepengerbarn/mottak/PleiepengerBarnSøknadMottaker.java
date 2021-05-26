package no.nav.k9.sak.ytelse.pleiepengerbarn.mottak;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PleiepengerBarnSøknadMottaker implements SøknadMottakTjeneste<PleiepengerBarnSøknadInnsending> {

    private SaksnummerRepository saksnummerRepository;
    private FagsakTjeneste fagsakTjeneste;

    
    protected PleiepengerBarnSøknadMottaker() {
        // for proxy
    }

    @Inject
    public PleiepengerBarnSøknadMottaker(SaksnummerRepository saksnummerRepository,FagsakTjeneste fagsakTjeneste) {
        this.saksnummerRepository = saksnummerRepository;
        this.fagsakTjeneste = fagsakTjeneste;
    }

    @Override
    public Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, AktørId pleietrengendeAktørId, AktørId relatertPersonAktørId, LocalDate startDato, LocalDate sluttDato) {
        ytelseType.validerNøkkelParametere(pleietrengendeAktørId, relatertPersonAktørId);

        if (sluttDato == null) {
            sluttDato = startDato;
        }
        
        if (sluttDato.isAfter(LocalDate.now().plusYears(2))) {
            // Hvis dette skulle bli nødvendig i fremtiden kan denne sjekken fjernes.
            throw new IllegalArgumentException("Fagsak kan ikke være mer enn 2 år inn i fremtiden.");
        }
        var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, søkerAktørId, pleietrengendeAktørId, null, Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE);
        if (fagsak.isPresent()) {
            return fagsak.get();
        }
        final Saksnummer saksnummer = new Saksnummer(saksnummerRepository.genererNyttSaksnummer());
        return opprettSakFor(saksnummer, søkerAktørId, pleietrengendeAktørId, ytelseType, startDato, sluttDato);
    }
    
    
    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, FagsakYtelseType ytelseType, LocalDate startDato, LocalDate sluttDato) {
        final Fagsak fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, null, saksnummer, startDato, sluttDato);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }
}
