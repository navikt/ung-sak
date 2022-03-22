package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import java.time.LocalDate;
import java.util.Arrays;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.FagsakTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.mottak.SøknadMottakTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerSøknadMottaker implements SøknadMottakTjeneste<OmsorgspengerSøknadInnsending> {

    private static final int CUT_OFF_OMP = 2020;
    private FagsakTjeneste fagsakTjeneste;
    private SaksnummerRepository saksnummerRepository;

    OmsorgspengerSøknadMottaker() {
        // proxy
    }

    @Inject
    public OmsorgspengerSøknadMottaker(SaksnummerRepository saksnummerRepository, FagsakTjeneste fagsakTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.saksnummerRepository = saksnummerRepository;
    }

    @Override
    public Fagsak finnEllerOpprettFagsak(FagsakYtelseType ytelseType, AktørId søkerAktørId, AktørId pleietrengendeAktørId, AktørId relatertPersonAktørId, LocalDate startDato, LocalDate sluttDato) {
        ytelseType.validerNøkkelParametere(pleietrengendeAktørId, relatertPersonAktørId);
        var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, søkerAktørId, null, null, startDato, sluttDato);
        if (fagsak.isPresent()) {
            return fagsak.get();
        }

        var saksnummer = new Saksnummer(saksnummerRepository.genererNyttSaksnummer());

        LocalDate idag = LocalDate.now();
        var detteÅret = DatoIntervallEntitet.fraOgMedTilOgMed(idag.withDayOfYear(1), idag.withMonth(12).withDayOfMonth(31));
        var ettÅrTilbake = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusYears(1).withDayOfYear(1), idag.minusYears(1).withMonth(12).withDayOfMonth(31));
        var toÅrTilbake = DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusYears(2).withDayOfYear(1), idag.minusYears(2).withMonth(12).withDayOfMonth(31));
        var angittPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(startDato, sluttDato);

        for (var p : Arrays.asList(detteÅret, ettÅrTilbake, toÅrTilbake)) {
            if (p.overlapper(angittPeriode)) {
                if (p.getFomDato().getYear() >= CUT_OFF_OMP) {
                    // ta utgangspunkt i året i år først, sjekk deretter fjoråret. Men ikke tillatt 2019 eller tidligere her
                    return opprettSakFor(saksnummer, søkerAktørId, pleietrengendeAktørId, ytelseType, p.getFomDato(), p.getTomDato());
                }
            }
        }

        throw new IllegalArgumentException("Kan ikke opprette " + ytelseType + " sak for periode: " + angittPeriode);
    }

    private Fagsak opprettSakFor(Saksnummer saksnummer, AktørId brukerIdent, AktørId pleietrengendeAktørId, FagsakYtelseType ytelseType, LocalDate fom, LocalDate tom) {
        var fagsak = Fagsak.opprettNy(ytelseType, brukerIdent, pleietrengendeAktørId, null, saksnummer, fom, tom);
        fagsakTjeneste.opprettFagsak(fagsak);
        return fagsak;
    }

}
