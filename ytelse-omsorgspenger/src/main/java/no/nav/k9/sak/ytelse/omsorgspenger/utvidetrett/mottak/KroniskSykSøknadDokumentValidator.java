package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.mottak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.domene.person.pdl.PersoninfoTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentGruppeRef;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValidator;
import no.nav.k9.sak.mottak.dokumentmottak.DokumentValideringException;
import no.nav.k9.sak.mottak.dokumentmottak.SøknadParser;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk.KroniskSykSøknadsperiodeUtleder;
import no.nav.k9.søknad.Søknad;
import no.nav.k9.søknad.felles.type.PersonIdent;
import no.nav.k9.søknad.ytelse.Ytelse;
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerKroniskSyktBarn;
import no.nav.k9.søknad.ytelse.omsorgspenger.utvidetrett.v1.OmsorgspengerUtvidetRett;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
@DokumentGruppeRef(Brevkode.SØKNAD_OMS_UTVIDETRETT_KS_KODE)
public class KroniskSykSøknadDokumentValidator implements DokumentValidator {

    private static final Map<Ytelse.Type, Brevkode> GYLDIGE_SØKNAD_BREVKODER = Map.of(
        Ytelse.Type.OMSORGSPENGER_UTVIDETRETT_KRONISK_SYKT_BARN, Brevkode.SØKNAD_OMS_UTVIDETRETT_KS);
    private final BehandlingRepository behandlingRepository;
    private final PersoninfoTjeneste personinfoTjeneste;
    private final AktørTjeneste aktørTjeneste;
    private final boolean validerMotFagsakperiode;


    @Inject
    public KroniskSykSøknadDokumentValidator(BehandlingRepository behandlingRepository,
                                             PersoninfoTjeneste personinfoTjeneste, AktørTjeneste aktørTjeneste,
                                             @KonfigVerdi(value = "OMP_KS_VALIDER_SOKANDSPERIODE_MOT_FAGSAKPERIODE", defaultVerdi = "false") boolean validerMotFagsakperiode) {
        this.behandlingRepository = behandlingRepository;
        this.personinfoTjeneste = personinfoTjeneste;
        this.aktørTjeneste = aktørTjeneste;
        this.validerMotFagsakperiode = validerMotFagsakperiode;
    }

    @Override
    public void validerDokumenter(Long behandlingId, Collection<MottattDokument> meldinger) {
        validerHarInnhold(meldinger);
        var mottattBrevkoder = meldinger.stream().map(MottattDokument::getType).collect(Collectors.toList());
        var søknader = new SøknadParser().parseSøknader(meldinger);

        int i = 0;
        for (Søknad søknad : søknader) {
            var brevkode = mottattBrevkoder.get(i++);
            Brevkode forventetBrevkode = GYLDIGE_SØKNAD_BREVKODER.get(søknad.getYtelse().getType());
            if (!Objects.equals(brevkode, forventetBrevkode)) {
                throw new IllegalArgumentException("Forventet brevkode: " + forventetBrevkode + ", fikk: " + brevkode);
            }
            validerInnhold(søknad);
        }

        if (validerMotFagsakperiode) {
            validerUtledetSøknadsperiodeMotFagsakperiode(behandlingId, søknader);
        }
    }

    private void validerUtledetSøknadsperiodeMotFagsakperiode(Long behandlingId, Collection<Søknad> søknader) {
        //innsending fra k9-fordel og k9-punsj skjer i to runder: Først velges/opprettes fagsak med gitt periode, deretter sendes søknad inn på fagsaken
        //når perioden på søknaden ikke stemmer med perioden på fagsaken blir det følgefeil senere i prosessen i k9-sak
        //kompliserende her er at søknadsperioden utledes (hovedsaklig fra mottatt dato), utvides og tilpasses ut fra alder på barnet

        Fagsak fagsak = behandlingRepository.hentBehandling(behandlingId).getFagsak();
        for (Søknad søknad : søknader) {
            OmsorgspengerKroniskSyktBarn ytelse = søknad.getYtelse();
            PersonIdent barnFnr = ytelse.getBarn().getPersonIdent();
            var ident = new no.nav.k9.sak.typer.PersonIdent(barnFnr.getVerdi());
            var aktørId = aktørTjeneste.hentAktørIdForPersonIdent(ident).orElseThrow(() -> new IllegalArgumentException("Ukjent person"));
            var fødselsdatoBarn = personinfoTjeneste.hentKjerneinformasjon(aktørId, ident).getFødselsdato();
            var oppgittSøknadsperiode = søknad.getYtelse().getSøknadsperiode();
            var oppgittSøknadsperiodeDatoIntervallEntitet = DatoIntervallEntitet.fraOgMedTilOgMed(oppgittSøknadsperiode.getFraOgMed(), oppgittSøknadsperiode.getTilOgMed());
            DatoIntervallEntitet faktiskSøknadsperiode = new KroniskSykSøknadsperiodeUtleder().utledFaktiskSøknadsperiode(oppgittSøknadsperiodeDatoIntervallEntitet, fødselsdatoBarn);
            if (!fagsak.getPeriode().inkluderer(faktiskSøknadsperiode.getFomDato())) {
                throw new IllegalArgumentException("Start på utledet søknadsperiode " + faktiskSøknadsperiode.getFomDato() + " ligger ikke innenfor fagsakens periode " + fagsak.getPeriode());
            }
        }
    }

    @Override
    public void validerDokument(MottattDokument mottattDokument) {
        validerDokumenter(null, Set.of(mottattDokument));
    }

    private void validerInnhold(Søknad søknad) {
        OmsorgspengerUtvidetRett ytelse = søknad.getYtelse();
        defaultValidering(ytelse);
    }

    private void defaultValidering(OmsorgspengerUtvidetRett ytelse) {
        List<no.nav.k9.søknad.felles.Feil> feil = ytelse.getValidator().valider(ytelse);
        if (!feil.isEmpty()) {
            // kaster DokumentValideringException pga håndtering i SaksbehandlingDokumentmottakTjeneste
            throw valideringsfeil(feil.stream()
                .map(f -> "kode=" + f.getFeilkode() + " for " + f.getFelt() + ": " + f.getFeilmelding())
                .reduce((a, b) -> a + "; " + b).orElseThrow());
        }
    }

    private static void validerHarInnhold(Collection<MottattDokument> dokumenter) {
        Set<JournalpostId> dokumenterUtenInnhold = dokumenter.stream()
            .filter(d -> !d.harPayload())
            .map(MottattDokument::getJournalpostId)
            .collect(Collectors.toSet());
        if (!dokumenterUtenInnhold.isEmpty()) {
            throw valideringsfeil("Mottok søknad uten innhold. Gjelder journalpostId=" + dokumenterUtenInnhold);
        }
    }

    private static DokumentValideringException valideringsfeil(String tekst) {
        return new DokumentValideringException("Feil i søknad om utbetaling av omsorgspenger: " + tekst);
    }

}
