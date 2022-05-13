package no.nav.k9.sak.ytelse.omsorgspenger.registerdata;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.aarskvantum.kontrakter.Rammevedtak;
import no.nav.k9.aarskvantum.kontrakter.UtvidetRett;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.domene.registerinnhenting.YtelsesspesifikkRelasjonsFilter;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.omsorgspenger.rammevedtak.OmsorgspengerRammevedtakTjeneste;

@FagsakYtelseTypeRef(OMSORGSPENGER)
@ApplicationScoped
public class OmsorgspengerRelasjonsFilter implements YtelsesspesifikkRelasjonsFilter {

    private OmsorgspengerRammevedtakTjeneste omsorgspengerRammevedtakTjeneste;
    private AktørTjeneste aktørTjeneste;

    OmsorgspengerRelasjonsFilter() {
        //for CDI proxy
    }

    @Inject
    public OmsorgspengerRelasjonsFilter(OmsorgspengerRammevedtakTjeneste omsorgspengerRammevedtakTjeneste, AktørTjeneste aktørTjeneste) {
        this.omsorgspengerRammevedtakTjeneste = omsorgspengerRammevedtakTjeneste;
        this.aktørTjeneste = aktørTjeneste;
    }

    @Override
    public boolean hentHistorikkForRelatertePersoner() {
        return true;
    }

    @Override
    public boolean hentDeltBosted() {
        return true;
    }

    @Override
    public List<Personinfo> relasjonsFiltreringBarn(Behandling behandling, List<Personinfo> barn, Periode opplysningsperioden) {
        if (barn.stream().allMatch(barnet -> barnetRelevantForSakenPgaAlderUnder12(barnet, opplysningsperioden))) {
            //snarvei for å unngå å hente rammevedtak
            return barn;
        }
        List<Rammevedtak> rammevedtak = omsorgspengerRammevedtakTjeneste.hentRammevedtak(new BehandlingUuidDto(behandling.getUuid())).getRammevedtak();
        return barn.stream()
            .filter(barnet -> barnetRelevantForSaken(opplysningsperioden, rammevedtak, barnet))
            .toList();
    }

    private boolean barnetRelevantForSaken(Periode opplysningsperioden, List<Rammevedtak> rammevedtak, Personinfo barnet) {
        return barnetRelevantForSakenPgaAlderUnder12(barnet, opplysningsperioden) || barnetAktueltPgaRammevedtak(barnet, rammevedtak, opplysningsperioden);
    }

    private boolean barnetRelevantForSakenPgaAlderUnder12(Personinfo barnet, Periode opplysningsperioden) {
        LocalDate sluttAvÅret = opplysningsperioden.getFom().withMonth(12).withDayOfMonth(31);
        return barnet.getAlder(sluttAvÅret) <= 12;
    }

    private boolean barnetAktueltPgaRammevedtak(Personinfo barnet, List<Rammevedtak> rammevedtak, Periode opplysningsperioden) {
        LocalDate sluttAvÅret = opplysningsperioden.getFom().withMonth(12).withDayOfMonth(31);
        return barnet.getAlder(sluttAvÅret) <= 18 && harKroniskSykRammevedtak(barnet, rammevedtak, opplysningsperioden);
    }

    private boolean harKroniskSykRammevedtak(Personinfo barn, List<Rammevedtak> rammevedtak, Periode opplysningsperiode) {
        for (Rammevedtak rammevedtaket : rammevedtak) {
            if (rammevedtaket instanceof UtvidetRett utvidetRett && gjelderForBarnet(barn, utvidetRett) && overlapper(opplysningsperiode, utvidetRett)) {
                return true;
            }
        }
        return false;
    }

    private boolean overlapper(Periode opplysningsperiode, UtvidetRett utvidetRett) {
        Periode utvidetRettPeriode = new Periode(utvidetRett.gyldigFraOgMed(), utvidetRett.gyldigTilOgMed());
        return utvidetRettPeriode.overlaps(opplysningsperiode);
    }

    private boolean gjelderForBarnet(Personinfo barn, UtvidetRett utvidetRett) {
        String fnrGjelderFor = utvidetRett.getUtvidetRettFor();
        AktørId gjelderFor = aktørTjeneste.hentAktørIdForPersonIdent(new PersonIdent(fnrGjelderFor)).orElseThrow();
        return barn.getAktørId().equals(gjelderFor);
    }
}
